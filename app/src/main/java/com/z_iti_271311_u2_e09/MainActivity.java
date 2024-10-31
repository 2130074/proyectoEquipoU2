package com.z_iti_271311_u2_e09;

import android.Manifest;
import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CAMERA = 1001; // Constante para el código de solicitud de permiso de cámara
    private TextView timerText;
    private Button startButton, finishButton, viewResultsButton;
    private ImageView operationsImage;
    private PreviewView userCameraView;

    // Variables para el manejo del temporizador
    private Handler timerHandler = new Handler();
    private long startTime = 0L;
    private long elapsedTime = 0L;

    // Variables de estado
    private boolean isActivityStarted = false;
    private boolean isActivityFinished = false;
    private boolean isDialogShowing = false;
    private boolean isPaused = false;

    // Variables para el manejo de la cámara
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private Camera camera;
    private ProcessCameraProvider cameraProvider;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeViews();
        setupButtons();
        cameraProviderFuture = ProcessCameraProvider.getInstance(this);
    }

    private void initializeViews() {
        timerText = findViewById(R.id.timer_text);
        startButton = findViewById(R.id.start_button);
        finishButton = findViewById(R.id.finish_button);
        viewResultsButton = findViewById(R.id.view_results_button);
        operationsImage = findViewById(R.id.operations_image);
        userCameraView = findViewById(R.id.user_camera_view);
        operationsImage.setVisibility(View.GONE);
    }

    private void setupButtons() {
        startButton.setOnClickListener(view -> checkCameraPermissionAndStart());
        finishButton.setOnClickListener(view -> stopTimer());
        viewResultsButton.setOnClickListener(view -> viewResults());
    }

    @Override
    protected void onPause() {  // pausa la actividad
        super.onPause();
        if (isActivityStarted && !isActivityFinished && !isDialogShowing) {
            pauseActivity();
            showExitWarningDialog();
        }
    }

    private void pauseActivity() { //pausar la actividad cuidando los datos guardados mientras confirma
        if (!isPaused) {
            isPaused = true;
            // Guardar el tiempo transcurrido
            elapsedTime = SystemClock.uptimeMillis() - startTime;
            // Detener el cronómetro
            timerHandler.removeCallbacks(updateTimerRunnable);
            // Detener la cámara
            stopCamera();
        }
    }

    private void resumeActivity() { //reanuda la actividad ( por si no se quiso salir)
        if (isPaused) {
            isPaused = false;
            // Actualizar el tiempo de inicio considerando el tiempo transcurrido
            startTime = SystemClock.uptimeMillis() - elapsedTime;
            // Reiniciar el cronómetro
            timerHandler.postDelayed(updateTimerRunnable, 0);
            // Reiniciar la cámara
            startCamera();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        isDialogShowing = false;
    }

    private void showExitWarningDialog() {//muestra la advertencia de que si sale va valer m
        isDialogShowing = true;
        new AlertDialog.Builder(this)
                .setTitle("Warning")
                .setMessage("If you leave the application, the activity will be finished. Do you want to continue?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    stopTimer();
                    finishAffinity();
                })
                .setNegativeButton("No", (dialog, which) -> {
                    isDialogShowing = false;
                    resumeActivity();
                })
                .setOnCancelListener(dialog -> {
                    isDialogShowing = false;
                    resumeActivity();
                })
                .setCancelable(true)
                .show();
    }

    @Override
    public void onBackPressed() {//maneja el boton de retroseso
        if (isActivityStarted && !isActivityFinished) {
            pauseActivity();
            showExitWarningDialog();
        } else {
            super.onBackPressed();
        }
    }

    private void checkCameraPermissionAndStart() {  //Verifica que tenga permisos para no tener errores
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    PERMISSION_REQUEST_CAMERA);
        } else {
            startTimer();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) { // solicita el permiso SI ES LA PRIMERA VEZ QUE SE INSTALA
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CAMERA) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startTimer();
            } else {
                Toast.makeText(this, "Camera permission is required to start the activity",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    private void startTimer() { //inicia tiempo
        startTime = SystemClock.uptimeMillis();
        elapsedTime = 0L;
        isPaused = false;
        timerHandler.postDelayed(updateTimerRunnable, 0);
        operationsImage.setVisibility(View.VISIBLE);
        isActivityStarted = true;
        isActivityFinished = false;
        startCamera();// manda a llamar a que se inicie la camara
        Toast.makeText(this, "You have started the exercise. Remember that the camera is turned on and you are being observed.",
                Toast.LENGTH_LONG).show();
    }

    private void startCamera() { // inicia la camara
        cameraProviderFuture.addListener(() -> {
            try {
                if (cameraProvider != null) {
                    cameraProvider.unbindAll();
                }
                cameraProvider = cameraProviderFuture.get();
                bindPreview(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                Toast.makeText(this, "Error starting camera: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindPreview(ProcessCameraProvider cameraProvider) { //vincula la vista previa de la camara
        Preview preview = new Preview.Builder().build();
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                .build();

        preview.setSurfaceProvider(userCameraView.getSurfaceProvider());

        try {
            camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview);
        } catch (Exception e) {
            Toast.makeText(this, "Error binding camera preview: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void stopTimer() {//detiene el tiempo y finaliza la actividad
        if (isActivityStarted) {
            timerHandler.removeCallbacks(updateTimerRunnable);
            operationsImage.setVisibility(View.GONE);
            isActivityFinished = true;
            stopCamera();
            Toast.makeText(this, "You have finished the exercise and the camera has turned off.",
                    Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "You need to start the activity first.",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void stopCamera() { //detiene la camarita
        if (cameraProvider != null) {
            cameraProvider.unbindAll();
        }
    }

    private void viewResults() { //Maneja la vista de resutados (DAMARIS)
        if (isActivityStarted && isActivityFinished) { //Solo si se inicio y termino una ctividad
            Toast.makeText(this, "Results viewed.", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Please complete the activity by starting and finishing it first.",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private Runnable updateTimerRunnable = new Runnable() { //Temporizador
        @Override
        public void run() {
            if (!isPaused) {
                long currentTime = SystemClock.uptimeMillis() - startTime;
                int seconds = (int) (currentTime / 1000);
                int minutes = seconds / 60;
                seconds = seconds % 60;

                timerText.setText(String.format("%02d:%02d", minutes, seconds));
                timerHandler.postDelayed(this, 500);
            }
        }
    };

    @Override
    protected void onDestroy() { //destruye todo si quiere salir de la plicacion
        super.onDestroy();
        stopCamera();
    }
}
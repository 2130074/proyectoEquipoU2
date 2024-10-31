package com.z_iti_271311_u2_e09;

import android.Manifest;
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

    private static final int PERMISSION_REQUEST_CAMERA = 1001;
    private TextView timerText;
    private Button startButton, finishButton, viewResultsButton;
    private ImageView operationsImage;
    private PreviewView userCameraView;
    private Handler timerHandler = new Handler();
    private long startTime = 0L;
    private boolean isActivityStarted = false;
    private boolean isActivityFinished = false;

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

    private void checkCameraPermissionAndStart() {
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
                                           @NonNull int[] grantResults) {
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

    private void startTimer() {
        startTime = SystemClock.uptimeMillis();
        timerHandler.postDelayed(updateTimerRunnable, 0);
        operationsImage.setVisibility(View.VISIBLE);
        isActivityStarted = true;
        isActivityFinished = false;
        startCamera();
        Toast.makeText(this, "You have started the exercise. Remember that the camera is turned on and you are being observed.",
                Toast.LENGTH_LONG).show();
    }

    private void startCamera() {
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

    private void bindPreview(ProcessCameraProvider cameraProvider) {
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

    private void stopTimer() {
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

    private void stopCamera() {
        if (cameraProvider != null) {
            cameraProvider.unbindAll();
        }
    }

    private void viewResults() {
        if (isActivityStarted && isActivityFinished) {
            Toast.makeText(this, "Results viewed.", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Please complete the activity by starting and finishing it first.",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private Runnable updateTimerRunnable = new Runnable() {
        @Override
        public void run() {
            long elapsedMillis = SystemClock.uptimeMillis() - startTime;
            int seconds = (int) (elapsedMillis / 1000);
            int minutes = seconds / 60;
            seconds = seconds % 60;

            timerText.setText(String.format("%02d:%02d", minutes, seconds));
            timerHandler.postDelayed(this, 500);
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopCamera();
    }
}
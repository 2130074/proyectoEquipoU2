package com.z_iti_271311_u2_e09;

import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private TextView timerText;
    private Button startButton, finishButton, viewResultsButton;
    private ImageView operationsImage;
    private Handler timerHandler = new Handler();
    private long startTime = 0L;
    private boolean isActivityStarted = false; // Verifica si la actividad ha comenzado
    private boolean isActivityFinished = false; // Verifica si la actividad ha sido finalizada

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        timerText = findViewById(R.id.timer_text);
        startButton = findViewById(R.id.start_button);
        finishButton = findViewById(R.id.finish_button);
        viewResultsButton = findViewById(R.id.view_results_button);
        operationsImage = findViewById(R.id.operations_image);

        // Inicialmente ocultar la imagen
        operationsImage.setVisibility(View.GONE);

        startButton.setOnClickListener(view -> startTimer());
        finishButton.setOnClickListener(view -> stopTimer());
        viewResultsButton.setOnClickListener(view -> viewResults());
    }

    private void startTimer() {
        startTime = SystemClock.uptimeMillis();
        timerHandler.postDelayed(updateTimerRunnable, 0);

        // Mostrar la imagen y marcar que la actividad ha comenzado
        operationsImage.setVisibility(View.VISIBLE);
        isActivityStarted = true;
        isActivityFinished = false; // Reiniciar el estado de finalización

        // Mostrar el mensaje de inicio y advertencia de la cámara
        Toast.makeText(this, "You have started the exercise. Remember that the camera is turned on and you are being observed.", Toast.LENGTH_LONG).show();
    }

    private void stopTimer() {
        if (isActivityStarted) { // Solo permite finalizar si la actividad ha comenzado
            timerHandler.removeCallbacks(updateTimerRunnable);

            // Ocultar la imagen y marcar que la actividad ha finalizado
            operationsImage.setVisibility(View.GONE);
            isActivityFinished = true;

            // Mostrar el mensaje de inicio y advertencia de la cámara
            Toast.makeText(this, "You have finished the exercise and the camera has turned off..", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "You need to start the activity first.", Toast.LENGTH_SHORT).show();
        }
    }

    private void viewResults() {
        if (isActivityStarted && isActivityFinished) {
            // Mostrar los resultados solo si la actividad ha comenzado y finalizado
            Toast.makeText(this, "Results viewed.", Toast.LENGTH_SHORT).show();
        } else {
            // Mostrar advertencia si la actividad no se ha seguido en el orden correcto
            Toast.makeText(this, "Please complete the activity by starting and finishing it first.", Toast.LENGTH_SHORT).show();
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
}
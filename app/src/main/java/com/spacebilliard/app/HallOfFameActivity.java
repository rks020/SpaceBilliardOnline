package com.spacebilliard.app;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.graphics.Color;

public class HallOfFameActivity extends Activity {

    private TextView textBestLevel;
    private TextView textHighScore;
    private TextView textMaxCombo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_hall_of_fame);

        // Prevent closing by touching outside (modal behavior)
        setFinishOnTouchOutside(false);

        textBestLevel = findViewById(R.id.textBestLevel);
        textHighScore = findViewById(R.id.textHighScore);
        textMaxCombo = findViewById(R.id.textMaxCombo);

        Button btnReset = findViewById(R.id.btnReset);
        Button btnBack = findViewById(R.id.btnBack);

        loadStats();

        btnReset.setOnClickListener(v -> {
            v.animate()
                    .scaleX(0.96f)
                    .scaleY(0.96f)
                    .setDuration(80)
                    .withEndAction(() -> {
                        v.animate().scaleX(1f).scaleY(1f).setDuration(80).start();
                        resetStats();
                    })
                    .start();
        });

        btnBack.setOnClickListener(v -> {
            v.animate()
                    .scaleX(0.96f)
                    .scaleY(0.96f)
                    .setDuration(80)
                    .withEndAction(() -> {
                        v.animate().scaleX(1f).scaleY(1f).setDuration(80).start();
                        finish();
                        overridePendingTransition(0, 0); // No animation
                    })
                    .start();
        });
    }

    private void loadStats() {
        SharedPreferences prefs = getSharedPreferences("SpaceBilliard", Context.MODE_PRIVATE);
        int highLevel = prefs.getInt("highLevel", 1);
        int highScore = prefs.getInt("highScore", 0);
        int maxCombo = prefs.getInt("maxCombo", 0);

        // Format Level: Space X Level Y
        int space = ((highLevel - 1) / 50) + 1;
        int levelInSpace = ((highLevel - 1) % 50) + 1;
        String levelText = "SPACE " + space + " - LEVEL " + levelInSpace;

        textBestLevel.setText(levelText);
        textHighScore.setText(String.valueOf(highScore));
        textMaxCombo.setText(String.valueOf(maxCombo));
    }

    private void resetStats() {
        SharedPreferences prefs = getSharedPreferences("SpaceBilliard", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt("highLevel", 1);
        editor.putInt("highScore", 0);
        editor.putInt("maxCombo", 0);
        editor.putInt("maxUnlockedLevel", 1); // Also reset progress
        editor.apply();

        loadStats();
        Toast.makeText(this, "Stats Reset!", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(0, 0);
    }
}

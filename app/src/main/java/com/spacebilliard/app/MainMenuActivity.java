package com.spacebilliard.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.content.SharedPreferences;

public class MainMenuActivity extends Activity {

    private android.animation.ObjectAnimator jupiterRotation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Fullscreen
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_main_menu);

        // Listeners for buttons
        /*
         * Button btnStart = findViewById(R.id.btnStart);
         * 
         * Button btnHowTo = findViewById(R.id.btnHowTo);
         * Button btnHallOfFame = findViewById(R.id.btnHallOfFame);
         * Button btnShop = findViewById(R.id.btnShop);
         */

        /*
         * btnStart.setOnClickListener(v -> {
         * Intent intent = new Intent(MainMenuActivity.this, MainActivity.class);
         * startActivity(intent);
         * });
         * 
         * btnShop.setOnClickListener(v -> {
         * Intent intent = new Intent(MainMenuActivity.this, ShopActivity.class);
         * startActivity(intent);
         * });
         * 
         * btnHowTo.setOnClickListener(v -> {
         * Intent intent = new Intent(MainMenuActivity.this, HowToPlayActivity.class);
         * startActivity(intent);
         * });
         * 
         * btnHallOfFame.setOnClickListener(v -> {
         * Intent intent = new Intent(MainMenuActivity.this, HallOfFameActivity.class);
         * startActivity(intent);
         * });
         */

        // --- NEW BUTTONS (RANKING & GUIDE) ---
        findViewById(R.id.btnRanking).setOnClickListener(v -> {
            startActivity(new Intent(MainMenuActivity.this, HallOfFameActivity.class));
        });

        findViewById(R.id.btnGuide).setOnClickListener(v -> {
            startActivity(new Intent(MainMenuActivity.this, HowToPlayActivity.class));
        });

        // --- PLANET ANIMATION ---
        // We use our custom PlanetView which handles internal rotation.
        // We only add a floating animation here.
        View planetView = findViewById(R.id.imgJupiter);

        if (planetView != null) {
            // Floating (Up/Down)
            android.animation.ObjectAnimator floatAnim = android.animation.ObjectAnimator.ofFloat(
                    planetView, "translationY", -20f, 20f);
            floatAnim.setDuration(4000);
            floatAnim.setRepeatCount(android.animation.ValueAnimator.INFINITE);
            floatAnim.setRepeatMode(android.animation.ValueAnimator.REVERSE);
            floatAnim.start();
        }

        // Bottom Navigation Bar Buttons (5 buttons)
        findViewById(R.id.btnBottomPlay).setOnClickListener(v -> {
            Intent intent = new Intent(MainMenuActivity.this, LevelSelectActivity.class);
            startActivity(intent);
        });

        findViewById(R.id.btnBottomUpgrades).setOnClickListener(v -> {
            // TODO: Upgrades screen
            android.widget.Toast.makeText(this, "Upgrades - Coming Soon", android.widget.Toast.LENGTH_SHORT).show();
        });

        findViewById(R.id.btnBottomQuests).setOnClickListener(v -> {
            // TODO: Quests screen
            android.widget.Toast.makeText(this, "Quests - Coming Soon", android.widget.Toast.LENGTH_SHORT).show();
        });

        findViewById(R.id.btnBottomSpecial).setOnClickListener(v -> {
            // TODO: Special/Modes screen
            android.widget.Toast.makeText(this, "Special Modes - Coming Soon", android.widget.Toast.LENGTH_SHORT)
                    .show();
        });

        findViewById(R.id.btnBottomShop).setOnClickListener(v -> {
            Intent intent = new Intent(MainMenuActivity.this, ShopActivity.class);
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (jupiterRotation != null && !jupiterRotation.isStarted()) {
            jupiterRotation.resume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (jupiterRotation != null) {
            jupiterRotation.pause();
        }
    }
}

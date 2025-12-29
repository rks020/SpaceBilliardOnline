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
        Button btnStart = findViewById(R.id.btnStart);
        Button btnOnline = findViewById(R.id.btnOnline);
        Button btnHowTo = findViewById(R.id.btnHowTo);
        Button btnHallOfFame = findViewById(R.id.btnHallOfFame);
        Button btnShop = findViewById(R.id.btnShop);
        Button btnSettings = findViewById(R.id.btnSettings);

        btnStart.setOnClickListener(v -> {
            Intent intent = new Intent(MainMenuActivity.this, MainActivity.class);
            startActivity(intent);
        });

        btnOnline.setOnClickListener(v -> {
            Intent intent = new Intent(MainMenuActivity.this, OnlineActivity.class);
            startActivity(intent);
        });

        btnShop.setOnClickListener(v -> {
            Intent intent = new Intent(MainMenuActivity.this, ShopActivity.class);
            startActivity(intent);
        });

        btnSettings.setOnClickListener(v -> {
            Intent intent = new Intent(MainMenuActivity.this, SettingsActivity.class);
            startActivity(intent);
        });

        btnHowTo.setOnClickListener(v -> {
            Intent intent = new Intent(MainMenuActivity.this, MainActivity.class);
            intent.putExtra("ACTION", "SHOW_HOWTO");
            startActivity(intent);
        });

        btnHallOfFame.setOnClickListener(v -> {
            Intent intent = new Intent(MainMenuActivity.this, MainActivity.class);
            intent.putExtra("ACTION", "SHOW_HOF");
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
    }
}

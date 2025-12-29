package com.spacebilliard.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;

public class GameOverActivity extends Activity {

    public static final int RESULT_REVIVE = 101;
    public static final int RESULT_REBOOT = 102;
    public static final int RESULT_HOF = 103;
    public static final int RESULT_MAIN_MENU = 104;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_game_over);

        // Prevent closing by touching outside (modal behavior)
        setFinishOnTouchOutside(false);

        Button btnRevive = findViewById(R.id.btnRevive);
        Button btnReboot = findViewById(R.id.btnReboot);
        Button btnHallOfFame = findViewById(R.id.btnHallOfFame);
        Button btnMainMenu = findViewById(R.id.btnMainMenu);

        btnRevive.setOnClickListener(v -> {
            setResult(RESULT_REVIVE);
            finish();
            overridePendingTransition(0, 0); // No animation
        });

        btnReboot.setOnClickListener(v -> {
            setResult(RESULT_REBOOT);
            finish();
            overridePendingTransition(0, 0);
        });

        btnHallOfFame.setOnClickListener(v -> {
            setResult(RESULT_HOF);
            finish();
            overridePendingTransition(0, 0);
        });

        btnMainMenu.setOnClickListener(v -> {
            setResult(RESULT_MAIN_MENU);
            finish();
            overridePendingTransition(0, 0);
        });
    }

    @Override
    public void onBackPressed() {
        // Prevent back button from dismissing without choice, or default to Main Menu
        setResult(RESULT_MAIN_MENU);
        finish();
    }
}

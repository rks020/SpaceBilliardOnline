package com.spacebilliard.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.spacebilliard.app.ui.NeonButton;
import com.spacebilliard.app.ui.NeonInfoPanel;
import com.spacebilliard.app.ui.NeonPowerPanel;

public class MainActivity extends Activity {

    private GameView gameView;
    private NeonInfoPanel infoPanel;
    private NeonPowerPanel powerPanel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Tam ekran ayarları
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        // Oyun görünümünü oluştur
        gameView = new GameView(this);

        // Root Layout
        FrameLayout root = new FrameLayout(this);
        root.addView(gameView);

        // Top Panel Container (Horizontal layout for two panels)
        LinearLayout topPanelContainer = new LinearLayout(this);
        topPanelContainer.setOrientation(LinearLayout.HORIZONTAL);
        topPanelContainer.setGravity(Gravity.CENTER_HORIZONTAL);

        FrameLayout.LayoutParams topContainerParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        topContainerParams.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        topContainerParams.setMargins(20, 30, 20, 0);
        topPanelContainer.setLayoutParams(topContainerParams);

        // Left Panel (TIME/SCORE/COIN)
        infoPanel = new NeonInfoPanel(this);
        infoPanel.setData("TIME:", "20", "SCORE:", "0");
        infoPanel.setCoins("0");
        infoPanel.setThemeColor(Color.CYAN);

        LinearLayout.LayoutParams infoPanelParams = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        infoPanelParams.setMargins(0, 0, 10, 0);
        infoPanel.setLayoutParams(infoPanelParams);

        // Right Panel (POWER/STAGE/LIVES with Heart)
        powerPanel = new NeonPowerPanel(this);
        powerPanel.setPower(0);
        powerPanel.setStage("1/10");
        powerPanel.setLives(3);

        LinearLayout.LayoutParams powerPanelParams = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        powerPanelParams.setMargins(10, 0, 0, 0);
        powerPanel.setLayoutParams(powerPanelParams);

        topPanelContainer.addView(infoPanel);
        topPanelContainer.addView(powerPanel);

        root.addView(topPanelContainer);

        // Calculate center for button positioning
        int density = (int) getResources().getDisplayMetrics().density;

        // Shop Button (Bottom most)
        final NeonButton shopBtn = new NeonButton(this, "SHOP", Color.rgb(255, 60, 120));
        FrameLayout.LayoutParams shopParams = new FrameLayout.LayoutParams(
                (int) (getResources().getDisplayMetrics().density * 180),
                (int) (getResources().getDisplayMetrics().density * 45));
        shopParams.gravity = Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM;
        shopParams.bottomMargin = (int) (getResources().getDisplayMetrics().density * 220); // Adjusted
        shopBtn.setLayoutParams(shopParams);

        // Hall of Fame Button
        final NeonButton hallOfFameBtn = new NeonButton(this, "HALL OF FAME", Color.rgb(255, 215, 0));
        FrameLayout.LayoutParams hallParams = new FrameLayout.LayoutParams(
                (int) (getResources().getDisplayMetrics().density * 180),
                (int) (getResources().getDisplayMetrics().density * 45));
        hallParams.gravity = Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM;
        hallParams.bottomMargin = (int) (getResources().getDisplayMetrics().density * 280); // +60dp
        hallOfFameBtn.setLayoutParams(hallParams);

        // How to Play Button
        final NeonButton howToBtn = new NeonButton(this, "HOW TO PLAY", Color.MAGENTA);
        FrameLayout.LayoutParams howToParams = new FrameLayout.LayoutParams(
                (int) (getResources().getDisplayMetrics().density * 180),
                (int) (getResources().getDisplayMetrics().density * 45));
        howToParams.gravity = Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM;
        howToParams.bottomMargin = (int) (getResources().getDisplayMetrics().density * 340); // +60dp
        howToBtn.setLayoutParams(howToParams);

        // Start Button (Top most)
        final NeonButton startBtn = new NeonButton(this, "START GAME", Color.CYAN);
        FrameLayout.LayoutParams startParams = new FrameLayout.LayoutParams(
                (int) (getResources().getDisplayMetrics().density * 180),
                (int) (getResources().getDisplayMetrics().density * 45));
        startParams.gravity = Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM;
        startParams.bottomMargin = (int) (getResources().getDisplayMetrics().density * 400); // +60dp
        startBtn.setLayoutParams(startParams);

        // Initially visible
        shopBtn.setVisibility(View.VISIBLE);
        startBtn.setVisibility(View.VISIBLE);
        howToBtn.setVisibility(View.VISIBLE);
        hallOfFameBtn.setVisibility(View.VISIBLE);

        shopBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, ShopActivity.class);
                startActivity(intent);
            }
        });

        startBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                gameView.startGame();
            }
        });

        howToBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                gameView.showInstructions();
            }
        });

        hallOfFameBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                gameView.showHighScore();
            }
        });

        root.addView(shopBtn);
        root.addView(startBtn);
        root.addView(howToBtn);
        root.addView(hallOfFameBtn);

        // Give GameView references to buttons so it can manage visibility
        gameView.setMenuButtons(startBtn, howToBtn, shopBtn, hallOfFameBtn);

        setContentView(root);
    }

    @Override
    protected void onPause() {
        super.onPause();
        gameView.pause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        gameView.resume();
    }

    // Method to update panels from GameView
    public void updatePanels(int time, int score, int coins, int power, String stage, String levelInfo, int lives) {
        if (infoPanel != null) {
            infoPanel.setData("TIME:", String.valueOf(time), "SCORE:", String.valueOf(score));
            infoPanel.setCoins(String.valueOf(coins));
        }
        if (powerPanel != null) {
            powerPanel.setPower(power);
            powerPanel.setStage(stage);
            powerPanel.setLevelInfo(levelInfo);
            powerPanel.setLives(lives);
            powerPanel.setCoins(coins); // Coin'i de güncelle
        }
    }
}

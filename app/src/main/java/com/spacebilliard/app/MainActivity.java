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

import com.spacebilliard.app.ui.NeonGameOverPanel;

// AdMob imports
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.google.android.gms.ads.LoadAdError;

public class MainActivity extends Activity {

    private GameView gameView;
    private NeonInfoPanel infoPanel;
    private NeonPowerPanel powerPanel;

    // Removed UI Panels
    // private NeonMainMenuPanel mainMenuPanel; // Moved to MainMenuActivity

    private NeonGameOverPanel gameOverPanel;

    // AdMob fields
    private AdView bannerAd;
    private RewardedAd rewardedAd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Tam ekran ayarları
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        // Initialize AdMob SDK
        MobileAds.initialize(this, new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(InitializationStatus initializationStatus) {
                // SDK initialized
                loadRewardedAd();
            }
        });

        // Oyun görünümünü oluştur
        gameView = new GameView(this);

        // Root Layout
        FrameLayout root = new FrameLayout(this);
        root.addView(gameView);

        // Calculate density for layout positioning
        int density = (int) getResources().getDisplayMetrics().density;

        // Top Panel Container (Horizontal layout for two panels)
        LinearLayout topPanelContainer = new LinearLayout(this);
        topPanelContainer.setOrientation(LinearLayout.HORIZONTAL);
        topPanelContainer.setGravity(Gravity.CENTER_HORIZONTAL);

        FrameLayout.LayoutParams topContainerParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        topContainerParams.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        // Pushing panel down by 60dp (50dp banner + 10dp padding)
        topContainerParams.setMargins(20 * density, 60 * density, 20 * density, 0);
        topPanelContainer.setLayoutParams(topContainerParams);

        // Left Panel (TIME/SCORE/COIN)
        infoPanel = new NeonInfoPanel(this);
        infoPanel.setData("TIME:", "20", "SCORE:", "0");
        infoPanel.setCoins("0");
        infoPanel.setThemeColor(Color.CYAN);

        LinearLayout.LayoutParams infoPanelParams = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        infoPanelParams.setMargins(0, 0, 10 * density, 0);
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

        // Banner Ad at top
        bannerAd = new AdView(this);
        bannerAd.setAdUnitId("ca-app-pub-3940256099942544/6300978111"); // Test banner ID
        bannerAd.setAdSize(AdSize.BANNER);

        FrameLayout.LayoutParams bannerParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        bannerParams.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        bannerAd.setLayoutParams(bannerParams);

        AdRequest adRequest = new AdRequest.Builder().build();
        bannerAd.loadAd(adRequest);

        root.addView(bannerAd);

        // Game Over Panel
        gameOverPanel = new NeonGameOverPanel(this);
        FrameLayout.LayoutParams menuParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
        gameOverPanel.setLayoutParams(menuParams);
        gameOverPanel.setVisibility(View.GONE); // Initially hidden
        root.addView(gameOverPanel);

        // --- Setup Listeners ---
        // setupMainMenuListeners(); // Gone
        setupGameOverListeners();

        setContentView(root);

        // Handle incoming intent actions (HowTo, HoF)
        boolean handled = handleIntentActions();

        // Only start game if we didn't open a special panel
        if (!handled) {
            gameView.startGame();
        }
    }

    // Returns true if an action was handled (HoF or Instructions opened)
    private boolean handleIntentActions() {
        if (getIntent() != null) {
            String action = getIntent().getStringExtra("ACTION");
            if (action != null) {
                if (action.equals("SHOW_HOWTO")) {
                    gameView.showInstructions();
                    return true;
                } else if (action.equals("SHOW_HOF")) {
                    gameView.showHighScore();
                    return true;
                }
            }
        }
        return false;
    }

    private void setupGameOverListeners() {
        gameOverPanel.btnRevive.setOnClickListener(v -> {
            showRewardedAdForContinue();
        });

        gameOverPanel.btnReboot.setOnClickListener(v -> {
            gameView.rebootLevel();
            hideAllPanels();
        });

        gameOverPanel.btnHallOfFame.setOnClickListener(v -> {
            gameView.showHighScore();
            gameOverPanel.setVisibility(View.GONE);
        });

        gameOverPanel.btnMainMenu.setOnClickListener(v -> {
            gameView.resetToMainMenu();
            finish(); // Return to MainMenuActivity
        });
    }

    // UI Helpers

    // showMainMenu method removed as we use an Activity now

    public void showGameOverScreen() {
        runOnUiThread(() -> {
            gameOverPanel.setVisibility(View.VISIBLE);

            // Hide top panels in Game Over too
            if (infoPanel != null)
                infoPanel.setVisibility(View.GONE);
            if (powerPanel != null)
                powerPanel.setVisibility(View.GONE);
        });
    }

    public void hideAllPanels() {
        runOnUiThread(() -> {
            gameOverPanel.setVisibility(View.GONE);

            // Show Info/Power panels when in game
            if (infoPanel != null)
                infoPanel.setVisibility(View.VISIBLE);
            if (powerPanel != null)
                powerPanel.setVisibility(View.VISIBLE);
        });
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

        // Refresh coin from SharedPreferences and update both GameView and panel
        if (gameView != null && infoPanel != null) {
            android.content.SharedPreferences prefs = getSharedPreferences("SpaceBilliard", MODE_PRIVATE);
            int currentCoins = prefs.getInt("coins", 0);

            // Update GameView's internal coin value
            gameView.refreshCoins();

            // Update panel display
            infoPanel.setCoins(String.valueOf(currentCoins));
        }
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
        }
    }

    // Load rewarded ad
    private void loadRewardedAd() {
        AdRequest adRequest = new AdRequest.Builder().build();
        RewardedAd.load(this, "ca-app-pub-3940256099942544/5224354917", // Test rewarded ID
                adRequest, new RewardedAdLoadCallback() {
                    @Override
                    public void onAdLoaded(RewardedAd ad) {
                        rewardedAd = ad;
                    }

                    @Override
                    public void onAdFailedToLoad(LoadAdError loadAdError) {
                        rewardedAd = null;
                    }
                });
    }

    // Show rewarded ad and give the reward (continuing the game)
    public void showRewardedAdForContinue() {
        if (rewardedAd != null) {
            rewardedAd.show(this, rewardItem -> {
                // User watched ad, grant continue
                if (gameView != null) {
                    gameView.continueAfterAd();
                    hideAllPanels(); // Success
                }
                // Load next ad
                loadRewardedAd();
            });
        } else {
            // Ad not ready, just continue anyway (fallback)
            if (gameView != null) {
                gameView.continueAfterAd();
                hideAllPanels(); // Success
            }
            loadRewardedAd();
        }
    }
}

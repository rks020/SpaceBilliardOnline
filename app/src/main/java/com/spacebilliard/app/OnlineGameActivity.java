package com.spacebilliard.app;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.Toast;
import android.util.Log;

import com.spacebilliard.app.network.OnlineGameManager;
import com.spacebilliard.app.network.SupabaseManager;
import com.spacebilliard.app.ui.OnlineScorePanel;
import com.spacebilliard.app.ui.NeonButton;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
// import com.spacebilliard.app.SimpleOnlineGameView;

public class OnlineGameActivity extends Activity implements OnlineGameManager.OnGameListener {

    private static final String TAG = "OnlineGameActivity";

    private OnlineGameManager gameManager;
    private OnlineGameView gameView;
    private OnlineScorePanel scorePanel;
    private NeonButton backBtn;
    private AdView bannerAd;

    private String roomId;
    private String hostUsername;
    private String guestUsername;
    private boolean isHost;

    private int hostScore = 0;
    private int guestScore = 0;
    private int currentSet = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            android.util.Log.d("OnlineGameActivity", "onCreate started");

            // Full screen
            requestWindowFeature(Window.FEATURE_NO_TITLE);
            getWindow().setFlags(
                    WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);

            // Get intent data
            roomId = getIntent().getStringExtra("roomId");
            hostUsername = getIntent().getStringExtra("hostUsername");
            guestUsername = getIntent().getStringExtra("guestUsername");
            isHost = getIntent().getBooleanExtra("isHost", false);

            android.util.Log.d("OnlineGameActivity", "Room: " + roomId + ", Host: " + isHost);

            // Root layout
            FrameLayout root = new FrameLayout(this);

            // Use OnlineGameView instead of GameView
            android.util.Log.d("OnlineGameActivity", "Creating OnlineGameView");
            gameView = new OnlineGameView(this);
            root.addView(gameView);

            // Score panel (below banner ad at top)
            android.util.Log.d("OnlineGameActivity", "Creating score panel");
            scorePanel = new OnlineScorePanel(this);
            FrameLayout.LayoutParams scoreParams = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    (int) (getResources().getDisplayMetrics().density * 120));
            scoreParams.gravity = Gravity.TOP; // Move to top
            // Position below banner ad (50dp banner height + 10dp spacing)
            scoreParams.setMargins(20, (int) (getResources().getDisplayMetrics().density * 60), 20, 0);
            scorePanel.setLayoutParams(scoreParams);
            scorePanel.setPlayerNames(hostUsername, guestUsername);
            scorePanel.setIsHost(isHost);
            scorePanel.setCurrentSet(1, 3); // Initialize with Set 1/3
            scorePanel.setTimeLeft(30000); // Initialize with 30 seconds

            // Load and display win/loss statistics
            android.content.SharedPreferences prefs = getSharedPreferences("SpaceBilliard", MODE_PRIVATE);
            int wins = prefs.getInt("onlineWins", 0);
            int losses = prefs.getInt("onlineLosses", 0);
            scorePanel.setWinLossStats(wins, losses);

            root.addView(scorePanel);

            // Banner Ad at top
            android.util.Log.d("OnlineGameActivity", "Creating banner ad");
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

            // Back button
            android.util.Log.d("OnlineGameActivity", "Creating back button");
            backBtn = new NeonButton(this, "LEAVE GAME", Color.RED);
            FrameLayout.LayoutParams backParams = new FrameLayout.LayoutParams(
                    (int) (getResources().getDisplayMetrics().density * 150),
                    (int) (getResources().getDisplayMetrics().density * 45));
            backParams.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
            backParams.bottomMargin = 30;
            backBtn.setLayoutParams(backParams);
            backBtn.setOnClickListener(v -> leaveGame());
            root.addView(backBtn);

            setContentView(root);

            // Get game manager from application
            android.util.Log.d("OnlineGameActivity", "Getting game manager");
            gameManager = ((OnlineApplication) getApplication()).getGameManager();

            if (gameManager != null) {
                android.util.Log.d("OnlineGameActivity", "Game manager found, setting up");
                // Set this activity as game listener
                gameManager.setGameListener(this);

                // Set game manager and start
                gameView.setOnlineMode(gameManager, isHost);
                gameView.setPlayerNames(hostUsername, guestUsername);
                android.util.Log.d("OnlineGameActivity", "OnlineGameView setup complete");
            } else {
                android.util.Log.e("OnlineGameActivity", "Game manager is NULL!");
                Toast.makeText(this, "Game manager not found!", Toast.LENGTH_SHORT).show();
                finish();
            }
        } catch (Exception e) {
            android.util.Log.e("OnlineGameActivity", "Error in onCreate: " + e.getMessage(), e);
            Toast.makeText(this, "Error starting game: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @Override
    public void onPlayerJoined(String username) {
        runOnUiThread(() -> {
            Toast.makeText(this, username + " joined!", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void onPlayerLeft() {
        runOnUiThread(() -> {
            Toast.makeText(this, "Player left the game", Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    private android.app.AlertDialog matchEndDialog;

    @Override
    public void onGameStart() {
        Log.d(TAG, "onGameStart called!");
        runOnUiThread(() -> {
            Log.d(TAG, "onGameStart UI thread block executing");
            Toast.makeText(this, "Game started!", Toast.LENGTH_SHORT).show();

            // Dismiss match end dialog if showing (Rematch accepted)
            if (matchEndDialog != null && matchEndDialog.isShowing()) {
                Log.d(TAG, "Dismissing matchEndDialog");
                matchEndDialog.dismiss();
            }
            if (rematchDialog != null && rematchDialog.isShowing()) {
                Log.d(TAG, "Dismissing rematchDialog");
                rematchDialog.dismiss();
            }

            // Reset scores on game start/restart
            this.hostScore = 0;
            this.guestScore = 0;
            this.currentSet = 1;
            if (scorePanel != null) {
                scorePanel.setScores(0, 0);
                scorePanel.setBallsDestroyed(0, 0);
                scorePanel.setCurrentSet(1, 3);
            }

            // Clear ALL overlays (winner, setFinished, countdown)
            if (gameView != null) {
                Log.d(TAG, "Clearing game view overlays");
                gameView.clearAllOverlays(); // New method to clear everything
            }

            if (gameManager != null) {
                gameManager.sendReady();
            }
            Log.d(TAG, "onGameStart complete");
        });
    }

    @Override
    public void onGameStateReceived(String hostCueBallJson, String guestCueBallJson, String ballsJson) {
        // Forward ball positions to game view
        if (gameView != null) {
            gameView.updateOnlineState(hostCueBallJson, guestCueBallJson, ballsJson, 30000);
        }
    }

    @Override
    public void onGameStateUpdate(String hostCueBallJson, String guestCueBallJson, String ballsJson, long timeLeft,
            int currentSet) {
        // Forward real-time physics update to game view (60 FPS)
        if (gameView != null) {
            gameView.updateOnlineState(hostCueBallJson, guestCueBallJson, ballsJson, timeLeft);
        }

        // Update score panel timer
        if (scorePanel != null) {
            scorePanel.setTimeLeft(timeLeft);
            // We can also update current set here ensuring sync
            scorePanel.setCurrentSet(currentSet, 3);
        }

        this.currentSet = currentSet;
    }

    @Override
    public void onOpponentShot(float angle, float power) {
        // Shot handled by server sync
    }

    @Override
    public void onBallsUpdate(int hostBalls, int guestBalls) {
        runOnUiThread(() -> {
            // These store the BALL COUNT for the current set
            // this.hostScore = hostBalls; // Don't overwrite SET SCORE with BALL COUNT
            // this.guestScore = guestBalls;

            if (scorePanel != null) {
                scorePanel.setBallsDestroyed(hostBalls, guestBalls);
            }
        });
    }

    @Override
    public void onBallDestroyed(float x, float y, String colorHex) {
        runOnUiThread(() -> {
            if (gameView != null) {
                // Convert normalized coordinates (0-1) to screen coordinates
                int screenWidth = gameView.getWidth();
                int screenHeight = gameView.getHeight();
                float screenX = x * screenWidth;
                float screenY = y * screenHeight;

                // Parse color from hex string
                int color = android.graphics.Color.parseColor(colorHex);

                // Trigger explosion effect
                gameView.createExplosion(screenX, screenY, color);
            }
        });
    }

    @Override
    public void onSetEnded(String winner, int hostScore, int guestScore, int currentSet) {
        this.hostScore = hostScore;
        this.guestScore = guestScore;
        this.currentSet = currentSet;

        runOnUiThread(() -> {
            if (scorePanel != null) {
                scorePanel.setScores(hostScore, guestScore);
                scorePanel.setCurrentSet(currentSet, 3);
            }

            // Only show SET FINISHED if match is NOT over (someone didn't win 2-0)
            // If match continues to next set (scores are 1-1 or match will continue)
            boolean matchWillContinue = (hostScore < 2 && guestScore < 2);

            if (gameView != null && matchWillContinue) {
                // Show SET FINISHED with correct number
                gameView.showSetFinished("SET " + currentSet + " FINISHED");

                // Start countdown AFTER set finished text disappears (2 second delay)
                new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                    new android.os.CountDownTimer(5000, 1000) {
                        public void onTick(long millisUntilFinished) {
                            if (gameView != null) {
                                int count = (int) Math.ceil(millisUntilFinished / 1000.0);
                                gameView.showCountdown(count);
                            }
                        }

                        public void onFinish() {
                            if (gameView != null) {
                                gameView.hideCountdown();
                            }
                        }
                    }.start();
                }, 2000); // 2 second delay
            }
        });
    }

    private android.app.AlertDialog rematchDialog;

    @Override
    public void onRematchRequested(String requestingUser, boolean hostWants, boolean guestWants) {
        runOnUiThread(() -> {
            boolean amIHost = isHost;
            boolean myRequest = amIHost ? hostWants : guestWants;
            boolean oppRequest = amIHost ? guestWants : hostWants;

            Log.d(TAG, "onRematchRequested: HostWants=" + hostWants + ", GuestWants=" + guestWants + ", MyRequest="
                    + myRequest + ", OppRequest=" + oppRequest);

            if (oppRequest && !myRequest) {
                Log.d(TAG, "Opponent requested rematch. Showing dialog.");
                // Opponent wants rematch, I haven't said yes. Show Invite.
                if (rematchDialog == null || !rematchDialog.isShowing()) {
                    android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
                    builder.setTitle("Rematch Request");
                    builder.setMessage(requestingUser + " wants a rematch!");
                    builder.setPositiveButton("ACCEPT", (dialog, which) -> {
                        Toast.makeText(this, "Accepting rematch...", Toast.LENGTH_SHORT).show();
                        if (gameManager != null) {
                            gameManager.sendRematchRequest();
                        }
                    });
                    builder.setNegativeButton("DECLINE", (dialog, which) -> {
                        dialog.dismiss();
                        leaveGame();
                    });
                    builder.setCancelable(false);
                    rematchDialog = builder.show();
                }
            } else if (myRequest && !oppRequest) {
                // I already asked, waiting for them
                Toast.makeText(this, "Waiting for " + (amIHost ? guestUsername : hostUsername) + "...",
                        Toast.LENGTH_SHORT).show();
            } else if (myRequest && oppRequest) {
                // Both accepted! Game will restart via onGameStart callback
                Toast.makeText(this, "Rematch accepted! Game restarting...", Toast.LENGTH_SHORT).show();

                // Close any rematch dialog
                if (rematchDialog != null && rematchDialog.isShowing()) {
                    rematchDialog.dismiss();
                }
            }
        });
    }

    @Override
    public void onMatchEnded(String winner, String finalScore, int hostScore, int guestScore) {
        runOnUiThread(() -> {
            // Determine if current player won or lost
            boolean playerWon = (isHost && winner.equals("host")) || (!isHost && winner.equals("guest"));

            // Update Supabase stats
            SupabaseManager supabaseManager = SupabaseManager.getInstance(this);
            String deviceId = android.provider.Settings.Secure.getString(
                    getContentResolver(),
                    android.provider.Settings.Secure.ANDROID_ID);

            // Get current profile and update
            supabaseManager.getUserProfile(new SupabaseManager.ProfileCallback() {
                @Override
                public void onSuccess(SupabaseManager.UserProfile profile) {
                    int newWins = profile.wins + (playerWon ? 1 : 0);
                    int newLosses = profile.losses + (playerWon ? 0 : 1);

                    supabaseManager.updateStats(
                            profile.userId,
                            newWins,
                            newLosses,
                            new SupabaseManager.ProfileCallback() {
                                @Override
                                public void onSuccess(SupabaseManager.UserProfile updatedProfile) {
                                    android.util.Log.d("OnlineGameActivity",
                                            "Stats updated: " + newWins + "/" + newLosses);

                                    // Save last match result
                                    getSharedPreferences("game_prefs", MODE_PRIVATE)
                                            .edit()
                                            .putString("last_match_result", playerWon ? "WIN" : "LOSS")
                                            .apply();
                                }

                                @Override
                                public void onError(String error) {
                                    android.util.Log.e("OnlineGameActivity", "Error updating stats: " + error);
                                }
                            });
                }

                @Override
                public void onError(String error) {
                    android.util.Log.e("OnlineGameActivity", "Error getting profile: " + error);
                }
            });

            String winnerName = winner.equals("host") ? hostUsername : guestUsername;

            // Show winner name AFTER "SET FINISHED" disappears (3 second delay)
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                if (gameView != null) {
                    gameView.showWinner(winnerName.toUpperCase() + " WINS!");
                }

                // Show match end dialog after winner text displays (2 more seconds)
                new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                    // Create Custom Dialog
                    android.app.Dialog dialog = new android.app.Dialog(this);
                    dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                    dialog.setContentView(R.layout.dialog_match_finished);
                    dialog.getWindow().setBackgroundDrawable(
                            new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
                    dialog.setCancelable(false);

                    // Initialize Views
                    android.widget.TextView dialogMessage = dialog.findViewById(R.id.dialogMessage);
                    android.widget.TextView dialogScoreText = dialog.findViewById(R.id.dialogScore);
                    android.widget.Button btnOk = dialog.findViewById(R.id.btnOk);
                    android.widget.Button btnRematch = dialog.findViewById(R.id.btnRematch);

                    // Set Text
                    dialogMessage.setText(winnerName + " won the match!");
                    dialogScoreText.setText("Final Score: " + finalScore);

                    // Button Listeners
                    btnOk.setOnClickListener(v -> {
                        dialog.dismiss();
                        finish();
                    });

                    btnRematch.setOnClickListener(v -> {
                        if (gameManager != null) {
                            gameManager.sendRematchRequest();
                            Toast.makeText(this, "Rematch request sent!", Toast.LENGTH_SHORT).show();
                        }
                    });

                    matchEndDialog = new android.app.AlertDialog.Builder(this).create(); // Holder to prevent null
                                                                                         // checks failing elsewhere if
                                                                                         // used
                    // But wait, we are using a Dialog, not AlertDialog. Let's cast or change field
                    // type?
                    // Field is AlertDialog. Let's change field to Dialog or just keep local
                    // reference and manage it?
                    // Better: Change field type to Dialog in class if possible, OR just use local
                    // dialog but handle dismissal in onStop/Start?
                    // Re-using 'matchEndDialog' field requires changing its type or just using a
                    // separate field.
                    // For minimal diff, let's keep it local here, but we need to reference it to
                    // dismiss in onGameStart.
                    // Let's modify the field type later or simply cast/store it if possible.
                    // Actually, AlertDialog extends Dialog. So we can store it in 'matchEndDialog'
                    // IF we change declaration.
                    // But 'matchEndDialog' is likely AlertDialog.
                    // Let's check field definition: private android.app.AlertDialog matchEndDialog;
                    // I need to change that definition too if I want to store this custom dialog
                    // there.
                    // OR I can use AlertDialog.Builder.setView() with the inflated view! That's
                    // cleaner for keeping types.

                    // APPROACH 2: Use Builder.setView
                    android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
                    View dialogView = getLayoutInflater().inflate(R.layout.dialog_match_finished, null);
                    builder.setView(dialogView);
                    builder.setCancelable(false);
                    matchEndDialog = builder.create();
                    matchEndDialog.getWindow().setBackgroundDrawable(
                            new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT)); // Transparent
                                                                                                              // for
                                                                                                              // custom
                                                                                                              // rounded
                                                                                                              // bg

                    // Init Views from dialogView
                    android.widget.TextView txtMessage = dialogView.findViewById(R.id.dialogMessage);
                    android.widget.TextView txtScore = dialogView.findViewById(R.id.dialogScore);
                    android.widget.Button bOk = dialogView.findViewById(R.id.btnOk);
                    android.widget.Button bRematch = dialogView.findViewById(R.id.btnRematch);

                    txtMessage.setText(winnerName + " won the match!");
                    txtScore.setText("Final Score: " + finalScore);

                    bOk.setOnClickListener(v -> {
                        matchEndDialog.dismiss();
                        finish();
                    });

                    bRematch.setOnClickListener(v -> {
                        if (gameManager != null) {
                            gameManager.sendRematchRequest();
                            Toast.makeText(this, "Rematch request sent!", Toast.LENGTH_SHORT).show();
                        }
                    });

                    matchEndDialog.show();
                }, 2000); // 2 second delay to show winner text
            }, 3000); // 3 second delay - wait for SET FINISHED to fade
        });
    }

    private void leaveGame() {
        // Create Custom Dialog
        android.app.Dialog dialog = new android.app.Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_leave_game);
        dialog.getWindow().setBackgroundDrawable(
                new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        dialog.setCancelable(false);

        // Init Buttons
        android.widget.Button btnConfirm = dialog.findViewById(R.id.btnConfirm);
        android.widget.Button btnCancel = dialog.findViewById(R.id.btnCancel);

        btnConfirm.setOnClickListener(v -> {
            dialog.dismiss();
            if (gameManager != null) {
                gameManager.leaveRoom();
            }
            finish();
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (gameView != null) {
            gameView.pause();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (gameView != null) {
            gameView.resume();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // GameView will cleanup automatically in pause()
    }

    public void updateTimer(long timeLeft) {
        if (scorePanel != null) {
            scorePanel.setTimeLeft(timeLeft);
        }
    }
}

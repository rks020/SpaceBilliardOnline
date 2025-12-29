package com.spacebilliard.app;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.spacebilliard.app.network.SupabaseManager;
import com.spacebilliard.app.ui.WinLossBar;

public class SettingsActivity extends Activity {

    private SupabaseManager supabaseManager;
    private SupabaseManager.UserProfile userProfile;
    private EditText nicknameInput;
    private WinLossBar winLossBar;
    private TextView statsText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_settings);

        // Initialize Views
        nicknameInput = findViewById(R.id.nicknameInput);
        statsText = findViewById(R.id.statsText);

        Button btnSave = findViewById(R.id.btnSave);
        Button btnBack = findViewById(R.id.btnBack);

        // Setup WinLossBar programmatically into container
        FrameLayout winLossContainer = findViewById(R.id.winLossContainer);
        winLossBar = new WinLossBar(this);
        winLossContainer.addView(winLossBar, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        // Listeners
        btnSave.setOnClickListener(v -> saveChanges());
        btnBack.setOnClickListener(v -> finish());

        // Load profile
        supabaseManager = SupabaseManager.getInstance(this);
        loadProfile();
    }

    private void loadProfile() {
        supabaseManager.getUserProfile(new SupabaseManager.ProfileCallback() {
            @Override
            public void onSuccess(SupabaseManager.UserProfile profile) {
                runOnUiThread(() -> {
                    userProfile = profile;
                    nicknameInput.setText(profile.username);
                    winLossBar.setStats(profile.wins, profile.losses);
                    statsText.setText("Total Games: " + (profile.wins + profile.losses) +
                            " | Win Rate: " + calculateWinRate(profile.wins, profile.losses) + "%");
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    if (error.equals("NO_PROFILE")) {
                        // Auto-create a dummy profile with random nickname
                        String dummyNickname = "Player_" + (int) (Math.random() * 10000);
                        createDummyProfile(dummyNickname);
                    } else {
                        Toast.makeText(SettingsActivity.this, "Error loading profile: " + error, Toast.LENGTH_SHORT)
                                .show();
                        finish();
                    }
                });
            }
        });
    }

    private void createDummyProfile(String nickname) {
        supabaseManager.createProfile(nickname, new SupabaseManager.ProfileCallback() {
            @Override
            public void onSuccess(SupabaseManager.UserProfile profile) {
                runOnUiThread(() -> {
                    userProfile = profile;
                    nicknameInput.setText(profile.username);
                    winLossBar.setStats(0, 0);
                    statsText.setText("Total Games: 0 | Win Rate: 0%");
                    Toast.makeText(SettingsActivity.this, "Profile created! You can change your nickname.",
                            Toast.LENGTH_LONG).show();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(SettingsActivity.this, "Error creating profile: " + error, Toast.LENGTH_SHORT)
                            .show();
                    finish();
                });
            }
        });
    }

    private void saveChanges() {
        String newNickname = nicknameInput.getText().toString().trim();
        if (newNickname.isEmpty()) {
            Toast.makeText(this, "Nickname cannot be empty!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validate Username (Length and Content)
        if (!com.spacebilliard.app.utils.ProfanityFilter.isValidUsername(newNickname)) {
            nicknameInput.setTextColor(Color.RED); // Set text to RED on error
            if (newNickname.length() > 7) {
                Toast.makeText(this, "Nickname too long! Max 7 chars.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Invalid nickname! Please use appropriate language.", Toast.LENGTH_SHORT).show();
            }
            return;
        } else {
            nicknameInput.setTextColor(Color.WHITE); // Reset to WHITE on success
        }

        if (userProfile == null) {
            Toast.makeText(this, "Profile not loaded!", Toast.LENGTH_SHORT).show();
            return;
        }

        // If username hasn't changed, skip check
        if (newNickname.equals(userProfile.username)) {
            Toast.makeText(this, "No changes to save.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check if new username already exists
        supabaseManager.checkUsernameExists(newNickname, new SupabaseManager.UsernameCheckCallback() {
            @Override
            public void onResult(boolean exists) {
                runOnUiThread(() -> {
                    if (exists) {
                        Toast.makeText(SettingsActivity.this, "This username already exists!", Toast.LENGTH_SHORT)
                                .show();
                        nicknameInput.setTextColor(Color.RED);
                    } else {
                        // Proceed with update
                        updateUsername(newNickname);
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(SettingsActivity.this, "Error checking username: " + error, Toast.LENGTH_SHORT)
                            .show();
                });
            }
        });
    }

    private void updateUsername(String newNickname) {
        supabaseManager.updateUsername(userProfile.userId, newNickname, new SupabaseManager.ProfileCallback() {
            @Override
            public void onSuccess(SupabaseManager.UserProfile profile) {
                runOnUiThread(() -> {
                    userProfile = profile;
                    nicknameInput.setTextColor(Color.WHITE);
                    Toast.makeText(SettingsActivity.this, "Profile updated successfully!", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(SettingsActivity.this, "Error updating profile: " + error, Toast.LENGTH_SHORT)
                            .show();
                });
            }
        });
    }

    private int calculateWinRate(int wins, int losses) {
        int total = wins + losses;
        if (total == 0)
            return 0;
        return (int) ((wins / (float) total) * 100);
    }
}

package com.spacebilliard.app;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class LevelSelectActivity extends Activity {

    private GridLayout levelGrid;
    private TextView txtProgress;
    private TextView txtSpaceName;
    private int currentSpace = 1; // 1 = Levels 1-10, 2 = Levels 11-20
    private int maxUnlockedLevel = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_level_select);

        // Load progress (legacy variable for display, actual logic uses stars)
        SharedPreferences prefs = getSharedPreferences("SpaceBilliard", MODE_PRIVATE);
        maxUnlockedLevel = prefs.getInt("maxUnlockedLevel", 1);
        // maxUnlockedLevel = 500; // TEST MODE: SHOW ALL UNLOCKED

        levelGrid = findViewById(R.id.levelGrid);
        txtProgress = findViewById(R.id.progress);
        txtSpaceName = findViewById(R.id.txtSpaceName);
        ImageButton btnPrevSpace = findViewById(R.id.btnPrevSpace);
        ImageButton btnNextSpace = findViewById(R.id.btnNextSpace);
        Button btnBack = findViewById(R.id.btnReturn);

        btnBack.setOnClickListener(v -> {
            v.animate()
                    .scaleX(0.96f)
                    .scaleY(0.96f)
                    .setDuration(80)
                    .withEndAction(() -> {
                        v.animate().scaleX(1f).scaleY(1f).setDuration(80).start();
                        finish();
                    })
                    .start();
        });

        btnPrevSpace.setOnClickListener(v -> {
            if (currentSpace > 1) {
                currentSpace--;
                updateUI();
            }
        });

        btnNextSpace.setOnClickListener(v -> {
            if (currentSpace < 10) { // Max 10 spaces (100 levels)
                currentSpace++;
                updateUI();
            }
        });

        updateUI();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Reload progress data when returning from gameplay
        SharedPreferences prefs = getSharedPreferences("SpaceBilliard", MODE_PRIVATE);
        maxUnlockedLevel = prefs.getInt("maxUnlockedLevel", 1);
        // maxUnlockedLevel = 500; // TEST MODE: SHOW ALL UNLOCKED

        // Refresh the UI to show updated stars and unlocked levels
        updateUI();
    }

    private void updateUI() {
        // Update Space Name
        txtSpaceName.setText("SPACE " + currentSpace);

        // Update Progress Text (Using legacy maxUnlockedLevel for simplicity of
        // percentage)
        txtProgress.setText("CAMPAIGN PROGRESS: " + maxUnlockedLevel + "%");

        loadGrid();
    }

    private void loadGrid() {
        levelGrid.removeAllViews();
        int startLevel = (currentSpace - 1) * 10 + 1;
        int endLevel = startLevel + 9;

        LayoutInflater inflater = LayoutInflater.from(this);
        SharedPreferences starPrefs = getSharedPreferences("SPACE_PROGRESS", MODE_PRIVATE);

        for (int i = startLevel; i <= endLevel; i++) {
            final int levelNum = i;

            // Inflate custom layout
            View itemView = inflater.inflate(R.layout.item_level, levelGrid, false);

            // Layout params for Grid
            GridLayout.LayoutParams params = (GridLayout.LayoutParams) itemView.getLayoutParams();
            params.width = 0;
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f); // Even width
            itemView.setLayoutParams(params);

            // Find views
            View bgView = itemView.findViewById(R.id.bgLevel);
            TextView txtLevelNum = itemView.findViewById(R.id.txtLevelNum);
            TextView txtBossLabel = itemView.findViewById(R.id.txtBossLabel);
            ImageView imgLock = itemView.findViewById(R.id.imgLock);
            LinearLayout starsContainer = itemView.findViewById(R.id.starsContainer);

            // Logic: Determine if unlocked based on stars of previous level
            boolean isUnlocked;
            if (levelNum == 1) {
                isUnlocked = true;
            } else {
                int prevStars = starPrefs.getInt("level_" + (levelNum - 1) + "_stars", 0);
                isUnlocked = (prevStars == 3);
            }

            int currentStars = starPrefs.getInt("level_" + levelNum + "_stars", 0);
            boolean isCompleted = (currentStars == 3);
            boolean isBoss = (levelNum % 10 == 0);

            if (!isUnlocked) {
                // LOCKED
                if (isBoss) {
                    // Locked BOSS level - show boss background and BOSS text
                    bgView.setBackgroundResource(R.drawable.bg_level_boss);
                    txtLevelNum.setVisibility(View.VISIBLE);
                    txtLevelNum.setText("BOSS");
                    txtLevelNum.setTextSize(18);
                    txtLevelNum.setTextColor(Color.parseColor("#D500F9")); // Purple
                    txtBossLabel.setVisibility(View.VISIBLE);
                } else {
                    bgView.setBackgroundResource(R.drawable.bg_level_locked);
                    txtLevelNum.setVisibility(View.GONE);
                    txtBossLabel.setVisibility(View.GONE);
                }
                imgLock.setVisibility(View.VISIBLE);
                starsContainer.setVisibility(View.GONE);
                itemView.setClickable(false);
                itemView.setAlpha(0.3f); // User requested 0.3f
            } else {
                // UNLOCKED
                itemView.setClickable(true);
                itemView.setAlpha(1.0f);
                imgLock.setVisibility(View.GONE);
                txtLevelNum.setVisibility(View.VISIBLE);

                if (isBoss) {
                    // Boss level - show "BOSS" instead of number
                    txtLevelNum.setText("BOSS");
                    txtLevelNum.setTextSize(18); // Slightly smaller for "BOSS" text
                } else {
                    txtLevelNum.setText(String.valueOf(levelNum));
                    txtLevelNum.setTextSize(24); // Original size for numbers
                }

                if (isBoss) {
                    bgView.setBackgroundResource(R.drawable.bg_level_boss);
                    txtLevelNum.setTextColor(Color.parseColor("#D500F9")); // Purple
                    txtBossLabel.setVisibility(View.VISIBLE);
                } else {
                    bgView.setBackgroundResource(R.drawable.bg_level_active);
                    txtLevelNum.setTextColor(Color.WHITE);
                    txtBossLabel.setVisibility(View.GONE);
                }

                // Show stars if completed
                if (isCompleted) {
                    starsContainer.setVisibility(View.VISIBLE);
                } else {
                    starsContainer.setVisibility(View.GONE);
                }

                itemView.setOnClickListener(v -> openLevel(levelNum));
            }

            levelGrid.addView(itemView);
        }
    }

    private void openLevel(int level) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("LEVEL", level);
        startActivity(intent);
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }
}

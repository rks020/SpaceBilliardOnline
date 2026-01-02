package com.spacebilliard.app;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.spacebilliard.app.ui.NeonButton;

public class UpgradesActivity extends Activity {

    private SharedPreferences prefs;
    private TextView txtCoins;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_upgrades);

        prefs = getSharedPreferences("SpaceBilliard", MODE_PRIVATE);
        txtCoins = findViewById(R.id.txtCoins);
        updateCoinDisplay();

        setupUpgradeCard(R.id.cardAim, "upgrade_aim", "LASER SIGHT", "+ Aim Line", 100, 1.2f,
                R.drawable.ic_upgrade_aim_vector);
        setupUpgradeCard(R.id.cardEnergy, "upgrade_energy", "ENERGY CORE", "+ Skill Time", 200, 1.2f,
                R.drawable.ic_upgrade_energy_vector);
        setupUpgradeCard(R.id.cardLuck, "upgrade_luck", "LUCKY CHARM", "+ Coin Gain", 250, 1.3f,
                R.drawable.ic_upgrade_luck_vector);
        setupUpgradeCard(R.id.cardShield, "upgrade_shield", "SHIELD GEN", "Start Shield", 400, 1.5f,
                R.drawable.ic_upgrade_shield_vector); // Cheaper now

        // New Upgrades
        setupUpgradeCard(R.id.cardNova, "upgrade_nova", "NOVA MODULE", "+ Boom Radius", 150, 1.2f,
                R.drawable.ic_upgrade_nova_vector);
        setupUpgradeCard(R.id.cardHunter, "upgrade_hunter", "BOSS HUNTER", "+ Boss Dmg", 200, 1.2f,
                R.drawable.ic_upgrade_hunter_vector);
        setupUpgradeCard(R.id.cardImpulse, "upgrade_impulse", "IMPULSE ENG", "+ Wall Bounce", 100, 1.2f,
                R.drawable.ic_upgrade_impulse_vector);
        setupUpgradeCard(R.id.cardMidas, "upgrade_midas", "MIDAS CHIP", "Coin Drop %", 250, 1.5f,
                R.drawable.ic_upgrade_midas_vector);

        setupUpgradeCard(R.id.cardVampire, "upgrade_vampire", "VAMPIRE CORE", "+ Heal on Kill", 400, 1.3f,
                R.drawable.ic_upgrade_vampire);

        findViewById(R.id.btnBack).setOnClickListener(v -> {
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

        // Guide Panel Logic
        View helpOverlay = findViewById(R.id.helpOverlay);
        findViewById(R.id.btnGuide).setOnClickListener(v -> helpOverlay.setVisibility(View.VISIBLE));
        findViewById(R.id.btnCloseHelp).setOnClickListener(v -> {
            v.animate()
                    .scaleX(0.96f)
                    .scaleY(0.96f)
                    .setDuration(80)
                    .withEndAction(() -> {
                        v.animate().scaleX(1f).scaleY(1f).setDuration(80).start();
                        helpOverlay.setVisibility(View.GONE);
                    })
                    .start();
        });
    }

    private void updateCoinDisplay() {
        int coins = prefs.getInt("coins", 0);
        txtCoins.setText("💰 " + coins);
    }

    private void setupUpgradeCard(int cardId, String prefKey, String title, String benefit, int basePrice,
            float priceMultiplier, int iconResId) {
        View card = findViewById(cardId);

        TextView txtTitle = card.findViewById(R.id.txtTitle);
        TextView txtLevel = card.findViewById(R.id.txtLevel);
        TextView txtBoost = card.findViewById(R.id.txtCurrentBoost);
        ProgressBar prog = card.findViewById(R.id.progressLevel);
        Button btnBuy = card.findViewById(R.id.btnBuy);
        ImageView imgIcon = card.findViewById(R.id.imgIcon);
        imgIcon.setImageResource(iconResId);

        txtTitle.setText(title);

        updateCardUI(prefKey, txtLevel, txtBoost, prog, btnBuy, basePrice, priceMultiplier, benefit);

        btnBuy.setOnClickListener(v -> {
            int currentLevel = prefs.getInt(prefKey, 1);
            if (currentLevel >= 10)
                return;

            int price = (int) (basePrice * Math.pow(priceMultiplier, currentLevel - 1));
            int coins = prefs.getInt("coins", 0);

            if (coins >= price) {
                // Buy
                SharedPreferences.Editor editor = prefs.edit();
                editor.putInt("coins", coins - price);
                editor.putInt(prefKey, currentLevel + 1);
                editor.apply();

                // Sound?
                // playSound(soundCoin);

                updateCoinDisplay();
                updateCardUI(prefKey, txtLevel, txtBoost, prog, btnBuy, basePrice, priceMultiplier, benefit);
                Toast.makeText(this, "UPGRADED!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Not enough coins!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateCardUI(String prefKey, TextView txtLevel, TextView txtBoost, ProgressBar prog, Button btnBuy,
            int basePrice, float priceMultiplier, String benefit) {
        int currentLevel = prefs.getInt(prefKey, 1);
        int price = (int) (basePrice * Math.pow(priceMultiplier, currentLevel - 1));

        txtLevel.setText("Lvl " + currentLevel + "/10");
        prog.setProgress(currentLevel);

        // Calculate boost desc based on type
        if (prefKey.equals("upgrade_aim")) {
            txtBoost.setText((100 + (currentLevel * 3)) + "% Line Length");
        } else if (prefKey.equals("upgrade_energy")) {
            txtBoost.setText("+" + (currentLevel * 10) + "% Duration");
        } else if (prefKey.equals("upgrade_luck")) {
            txtBoost.setText("+" + (currentLevel * 5) + "% Income");
        } else if (prefKey.equals("upgrade_shield")) {
            txtBoost.setText((currentLevel * 10) + "% Start Chance");
        } else if (prefKey.equals("upgrade_hunter")) {
        } else if (prefKey.equals("upgrade_hunter")) {
            txtBoost.setText("+" + (currentLevel * 2) + " Boss Dmg");

        } else if (prefKey.equals("upgrade_vampire")) {
            txtBoost.setText("+" + (currentLevel * 2) + " HP Drain");
        } else {
            txtBoost.setText(benefit);
        }

        if (currentLevel >= 10) {
            btnBuy.setText("MAX");
            btnBuy.setEnabled(false);
            btnBuy.setAlpha(0.5f);
        } else {
            btnBuy.setText(price + " 💰");
            btnBuy.setEnabled(true);
            btnBuy.setAlpha(1.0f);
        }
    }
}

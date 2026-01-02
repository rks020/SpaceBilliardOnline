package com.spacebilliard.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.WindowManager;
import android.widget.Button;
import android.graphics.Color;

public class HowToPlayActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_how_to_play);

        // Prevent closing by touching outside (modal behavior)
        setFinishOnTouchOutside(false);

        Button btnGotIt = findViewById(R.id.btnGotIt);

        btnGotIt.setOnClickListener(v -> {
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

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(0, 0);
    }
}

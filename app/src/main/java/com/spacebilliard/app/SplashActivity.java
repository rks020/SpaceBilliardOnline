package com.spacebilliard.app;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.FrameLayout;

public class SplashActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Premium Splash View
        SplashView splashView = new SplashView(this);
        setContentView(splashView);

        // Transition to MainActivity after 3 seconds
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(SplashActivity.this, MainMenuActivity.class);
                startActivity(intent);
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                finish();
            }
        }, 3000);
    }

    private class SplashView extends View {
        private Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private float pulse = 1.0f;
        private boolean growing = true;
        private long startTime;
        private int screenWidth, screenHeight;

        public SplashView(Context context) {
            super(context);
            startTime = System.currentTimeMillis();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            screenWidth = getWidth();
            screenHeight = getHeight();
            float centerX = screenWidth / 2f;
            float centerY = screenHeight / 2f;

            // Background - Dark Cosmic Gradient
            RadialGradient bgGradient = new RadialGradient(centerX, centerY, screenHeight,
                    new int[] { Color.rgb(15, 15, 35), Color.rgb(5, 5, 16) },
                    null, Shader.TileMode.CLAMP);
            paint.setShader(bgGradient);
            canvas.drawRect(0, 0, screenWidth, screenHeight, paint);
            paint.setShader(null);

            // Pulsing Logic
            long elapsed = System.currentTimeMillis() - startTime;
            pulse = 1.0f + (float) Math.sin(elapsed / 500.0) * 0.05f;

            // Draw Logo/Ball Glow
            paint.setStyle(Paint.Style.FILL);
            paint.setShadowLayer(40 * pulse, 0, 0, Color.CYAN);
            paint.setColor(Color.WHITE);
            canvas.drawCircle(centerX, centerY, 150 * pulse, paint);
            paint.clearShadowLayer();

            // Draw Title Text
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTextSize(80);
            paint.setFakeBoldText(true);
            paint.setColor(Color.WHITE);
            paint.setShadowLayer(20, 0, 0, Color.MAGENTA);
            canvas.drawText("SPACE BILLIARD", centerX, centerY + 300, paint);
            paint.clearShadowLayer();

            // Draw Loading indicator
            paint.setTextSize(40);
            paint.setAlpha(150);
            canvas.drawText("INITIALIZING SYSTEMS...", centerX, screenHeight - 200, paint);
            paint.setAlpha(255);

            // Draw a glowing progress bar
            float progress = Math.min(1.0f, elapsed / 2500f);
            float barWidth = screenWidth * 0.6f;
            float barX = (screenWidth - barWidth) / 2;
            float barY = screenHeight - 150;

            paint.setColor(Color.rgb(40, 40, 60));
            canvas.drawRoundRect(barX, barY, barX + barWidth, barY + 10, 5, 5, paint);

            paint.setColor(Color.CYAN);
            paint.setShadowLayer(15, 0, 0, Color.CYAN);
            canvas.drawRoundRect(barX, barY, barX + (barWidth * progress), barY + 10, 5, 5, paint);
            paint.clearShadowLayer();

            invalidate(); // Continuous animation
        }
    }
}

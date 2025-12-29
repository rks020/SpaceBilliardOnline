package com.spacebilliard.app.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.view.View;

public class WinLossBar extends View {
    private Paint paint;
    private Paint textPaint;

    private int wins = 0;
    private int losses = 0;
    private String lastMatchResult = ""; // "WIN", "LOSS", or ""

    public WinLossBar(Context context) {
        super(context);
        init();
    }

    private void init() {
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(32);
        textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        textPaint.setTextAlign(Paint.Align.CENTER);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();

        // Calculate total and percentages
        int total = wins + losses;
        float winPercent = total > 0 ? (float) wins / total : 0.5f;

        // Create rounded pill shape path
        android.graphics.Path path = new android.graphics.Path();
        RectF fullRect = new RectF(0, 0, width, height - 40);
        float radius = 30f; // Smoother roundness
        path.addRoundRect(fullRect, radius, radius, android.graphics.Path.Direction.CW);

        canvas.save();
        canvas.clipPath(path);

        // Draw win bar (green) - left side
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.argb(255, 76, 175, 80)); // Material green
        float winWidth = width * winPercent;
        canvas.drawRect(0, 0, winWidth, height - 40, paint);

        // Draw loss bar (red) - right side
        paint.setColor(Color.argb(255, 244, 67, 54)); // Material red
        canvas.drawRect(winWidth, 0, width, height - 40, paint);

        canvas.restore();

        // Draw "Win" label on left
        textPaint.setTextAlign(Paint.Align.LEFT);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(22);
        canvas.drawText("Win", 15, 25, textPaint);

        // Draw win number (clamped to be visible)
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTextSize(36);
        textPaint.setColor(Color.WHITE);
        textPaint.setShadowLayer(3, 0, 0, Color.BLACK);
        float winTextX = winWidth / 2f;
        if (winTextX < 30)
            winTextX = 30; // Min margin from left
        if (winTextX > width - 30)
            winTextX = width - 30; // Safety

        // If bar is too small, text might bleed into other section. Add specific
        // color/stroke?
        // For now, simpler clamp is enough as long as contrast is high (White on
        // Red/Green is ok).
        canvas.drawText(String.valueOf(wins), winTextX, (height - 40) / 2f + 12, textPaint);

        // Draw "Loss" label on right
        textPaint.setTextAlign(Paint.Align.RIGHT);
        textPaint.setTextSize(22);
        textPaint.setShadowLayer(0, 0, 0, Color.TRANSPARENT);
        canvas.drawText("Loss", width - 15, 25, textPaint);

        // Draw loss number (clamped)
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTextSize(36);
        textPaint.setShadowLayer(3, 0, 0, Color.BLACK);

        float lossBarStartX = winWidth;
        float lossBarWidth = width - winWidth;
        float lossTextX = lossBarStartX + lossBarWidth / 2f;

        if (lossTextX > width - 30)
            lossTextX = width - 30; // Min margin from right
        if (lossTextX < 30)
            lossTextX = 30; // Safety

        canvas.drawText(String.valueOf(losses), lossTextX, (height - 40) / 2f + 12, textPaint);

        // Draw last match result indicator
        textPaint.setShadowLayer(0, 0, 0, Color.TRANSPARENT);
        textPaint.setTextSize(24);
        textPaint.setTextAlign(Paint.Align.CENTER);

        if (!lastMatchResult.isEmpty()) {
            String resultText = "Last Match: ";
            textPaint.setColor(Color.WHITE);
            canvas.drawText(resultText, width / 2f - 50, height - 10, textPaint);

            if (lastMatchResult.equals("WIN")) {
                textPaint.setColor(Color.GREEN);
                textPaint.setTextSize(28);
                canvas.drawText("✓", width / 2f + 50, height - 10, textPaint);
            } else if (lastMatchResult.equals("LOSS")) {
                textPaint.setColor(Color.RED);
                textPaint.setTextSize(28);
                canvas.drawText("✗", width / 2f + 50, height - 10, textPaint);
            }
        }
    }

    public void setStats(int wins, int losses) {
        this.wins = wins;
        this.losses = losses;
        invalidate();
    }

    public void setLastMatchResult(String result) {
        this.lastMatchResult = result;
        invalidate();
    }
}

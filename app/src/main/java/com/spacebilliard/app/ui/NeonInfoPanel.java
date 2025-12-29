package com.spacebilliard.app.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;

public class NeonInfoPanel extends View {

    private Paint bgPaint;
    private Paint borderPaint;
    private Paint glowPaint;
    private Paint textPaint;
    private Paint valuePaint;
    private Paint coinIconPaint;

    private String line1Label = "TIME:";
    private String line1Value = "20";
    private String line2Label = "SCORE:";
    private String line2Value = "0";
    private String coinsValue = "0";
    private int themeColor = Color.CYAN;
    private Paint coinPaint;
    private Paint coinGlowPaint;

    public NeonInfoPanel(Context context) {
        super(context);
        init();
    }

    public NeonInfoPanel(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        setLayerType(LAYER_TYPE_SOFTWARE, null);

        bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bgPaint.setStyle(Paint.Style.FILL);

        borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(2f);
        borderPaint.setColor(themeColor);

        glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        glowPaint.setStyle(Paint.Style.STROKE);
        glowPaint.setColor(themeColor);

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(themeColor);
        textPaint.setTextSize(11 * getResources().getDisplayMetrics().scaledDensity);
        textPaint.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));

        valuePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        valuePaint.setColor(themeColor);
        valuePaint.setTextSize(11 * getResources().getDisplayMetrics().scaledDensity);
        valuePaint.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));

        coinPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        coinGlowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        coinGlowPaint.setStyle(Paint.Style.FILL);
    }

    public void setData(String l1Label, String l1Value, String l2Label, String l2Value) {
        this.line1Label = l1Label;
        this.line1Value = l1Value;
        this.line2Label = l2Label;
        this.line2Value = l2Value;
        invalidate();
    }

    // Animation Fields
    private float coinTextScale = 1.0f;
    private boolean isAnimatingCoin = false;
    private long animationStartTime = 0;
    private final float MAX_COIN_SCALE = 1.5f;

    public void setCoins(String coins) {
        if (!this.coinsValue.equals(coins)) {
            // Value changed, trigger animation if increasing
            try {
                int oldVal = Integer.parseInt(this.coinsValue);
                int newVal = Integer.parseInt(coins);
                if (newVal > oldVal) {
                    isAnimatingCoin = true;
                    animationStartTime = System.currentTimeMillis();
                }
            } catch (NumberFormatException e) {
                // Ignore parsing errors
                isAnimatingCoin = true;
                animationStartTime = System.currentTimeMillis();
            }
        }
        this.coinsValue = coins;
        invalidate();
    }

    public void setThemeColor(int color) {
        this.themeColor = color;
        borderPaint.setColor(color);
        glowPaint.setColor(color);
        textPaint.setColor(color);
        valuePaint.setColor(color);
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int desiredWidth = (int) (180 * getResources().getDisplayMetrics().density);
        int desiredHeight = (int) (95 * getResources().getDisplayMetrics().density);
        setMeasuredDimension(resolveSize(desiredWidth, widthMeasureSpec),
                resolveSize(desiredHeight, heightMeasureSpec));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Update Animation
        if (isAnimatingCoin) {
            long elapsed = System.currentTimeMillis() - animationStartTime;
            if (elapsed < 200) {
                // Grow
                float ratio = elapsed / 200f;
                coinTextScale = 1.0f + (MAX_COIN_SCALE - 1.0f) * ratio;
            } else if (elapsed < 400) {
                // Shrink
                float ratio = (elapsed - 200) / 200f;
                coinTextScale = MAX_COIN_SCALE - (MAX_COIN_SCALE - 1.0f) * ratio;
            } else {
                // End
                coinTextScale = 1.0f;
                isAnimatingCoin = false;
            }
            invalidate(); // Keep animating
        }

        float w = getWidth();
        float h = getHeight();
        float density = getResources().getDisplayMetrics().density;

        float margin = 5f * density;
        float headerH = 22f * density;

        // 1. Shadow / Outer Glow
        glowPaint.setAlpha(30);
        glowPaint.setStrokeWidth(10f);
        canvas.drawRoundRect(new RectF(margin, margin, w - margin, h - margin), 12, 12, glowPaint);

        // 2. Main Body (Dark Glass)
        bgPaint.setColor(Color.argb(230, 5, 8, 25));
        canvas.drawRoundRect(new RectF(margin, margin + headerH / 2, w - margin, h - margin), 10, 10, bgPaint);

        // 3. Header Bar
        RectF headerRect = new RectF(margin + 10 * density, margin, w - margin - 10 * density, margin + headerH);
        bgPaint.setColor(Color.argb(255, 10, 30, 60)); // Darker, solid header
        canvas.drawRoundRect(headerRect, 8, 8, bgPaint);

        borderPaint.setAlpha(255);
        borderPaint.setStrokeWidth(2f);
        canvas.drawRoundRect(headerRect, 8, 8, borderPaint);

        // Header Text
        textPaint.setTextSize(10 * getResources().getDisplayMetrics().scaledDensity);
        textPaint.setColor(Color.WHITE);
        float titleW = textPaint.measureText("MISSION DATA");
        canvas.drawText("MISSION DATA", w / 2 - titleW / 2, margin + headerH * 0.7f, textPaint);
        textPaint.setColor(themeColor); // Restore

        // 4. Detailed Borders & Sides
        borderPaint.setAlpha(60);
        borderPaint.setStrokeWidth(1.5f);
        canvas.drawRoundRect(new RectF(margin, margin + headerH / 2, w - margin, h - margin), 10, 10, borderPaint);

        // Decorative Side Brackets (Gamey look)
        borderPaint.setAlpha(255);
        borderPaint.setStrokeWidth(3f);
        float brk = 12 * density;
        // Top corners of body
        canvas.drawLine(margin, margin + headerH, margin + brk, margin + headerH, borderPaint);
        canvas.drawLine(margin, margin + headerH, margin, margin + headerH + brk, borderPaint);
        canvas.drawLine(w - margin, margin + headerH, w - margin - brk, margin + headerH, borderPaint);
        canvas.drawLine(w - margin, margin + headerH, w - margin, margin + headerH + brk, borderPaint);
        // Bottom corners
        canvas.drawLine(margin, h - margin, margin + brk, h - margin, borderPaint);
        canvas.drawLine(margin, h - margin, margin, h - margin - brk, borderPaint);
        canvas.drawLine(w - margin, h - margin, w - margin - brk, h - margin, borderPaint);
        canvas.drawLine(w - margin, h - margin, w - margin, h - margin - brk, borderPaint);

        // 5. Content Rendering
        float leftContent = margin + 20f * density;
        float startY = margin + headerH + 15 * density;
        float spacing = (h - startY - margin) / 3;

        float baseTextSize = 11 * getResources().getDisplayMetrics().scaledDensity;
        textPaint.setTextSize(baseTextSize);
        valuePaint.setTextSize(baseTextSize);

        // Line 1: TIME
        canvas.drawText(line1Label, leftContent, startY, textPaint);
        canvas.drawText(line1Value, leftContent + textPaint.measureText(line1Label) + 10, startY, valuePaint);

        // Line 2: SCORE
        canvas.drawText(line2Label, leftContent, startY + spacing, textPaint);
        canvas.drawText(line2Value, leftContent + textPaint.measureText(line2Label) + 10, startY + spacing, valuePaint);

        // Line 3: COINS
        float iconSize = 18 * density;
        float iconX = leftContent;
        float iconY = startY + (spacing * 2) - iconSize / 1.5f;
        drawVectorCoin(canvas, iconX + iconSize / 2, iconY + iconSize / 2, iconSize / 2);

        // APPLY ANIMATION SCALE
        valuePaint.setTextSize(baseTextSize * coinTextScale);
        if (isAnimatingCoin) {
            valuePaint.setColor(Color.YELLOW); // Highlight color during animation
        } else {
            valuePaint.setColor(themeColor);
        }

        canvas.drawText(coinsValue, iconX + iconSize + 10, startY + spacing * 2, valuePaint);

        // Reset Paint
        valuePaint.setTextSize(baseTextSize);
        valuePaint.setColor(themeColor);

        // Technical accents: small "screws" in corners
        bgPaint.setColor(Color.argb(150, 200, 200, 200));
        float sDist = 3 * density;
        canvas.drawCircle(margin + sDist, h - margin - sDist, 1.5f * density, bgPaint);
        canvas.drawCircle(w - margin - sDist, h - margin - sDist, 1.5f * density, bgPaint);
    }

    private void drawVectorCoin(Canvas canvas, float cx, float cy, float radius) {
        // Outer glow
        coinGlowPaint.setShader(new android.graphics.RadialGradient(cx, cy, radius * 1.5f,
                new int[] { Color.argb(100, 255, 200, 0), Color.TRANSPARENT }, null,
                android.graphics.Shader.TileMode.CLAMP));
        canvas.drawCircle(cx, cy, radius * 1.5f, coinGlowPaint);

        // Gold base
        coinPaint.setStyle(Paint.Style.FILL);
        coinPaint.setColor(Color.rgb(255, 215, 0));
        canvas.drawCircle(cx, cy, radius, coinPaint);

        // Inner rim
        coinPaint.setStyle(Paint.Style.STROKE);
        coinPaint.setColor(Color.rgb(255, 165, 0));
        coinPaint.setStrokeWidth(2f);
        canvas.drawCircle(cx, cy, radius * 0.8f, coinPaint);

        // Neon 'C'
        textPaint.setTextSize(radius * 1.2f);
        textPaint.setColor(Color.BLACK);
        float tw = textPaint.measureText("C");
        canvas.drawText("C", cx - tw / 2, cy + radius * 0.4f, textPaint);

        // Restore paint size
        textPaint.setTextSize(11 * getResources().getDisplayMetrics().scaledDensity);
        textPaint.setColor(themeColor);
    }
}

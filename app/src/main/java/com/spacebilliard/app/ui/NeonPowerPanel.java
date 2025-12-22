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

public class NeonPowerPanel extends View {

    private Paint bgPaint;
    private Paint borderPaint;
    private Paint glowPaint;
    private Paint textPaint;
    private Paint powerBarBgPaint;
    private Paint powerBarFillPaint;
    private Paint stageTextPaint;
    private Paint livesTextPaint;
    private Paint heartPaint;
    private Path heartPath;

    private int power = 0; // 0-100
    private String stage = "1/5";
    private String levelInfo = "SPACE 1 - LEVEL 1";
    private int lives = 3;
    private int coins = 0; // Coin balance

    private int themeColor = Color.CYAN;
    private Paint energyCorePaint;
    private Paint energyCoreGlowPaint;

    public NeonPowerPanel(Context context) {
        super(context);
        init();
    }

    public NeonPowerPanel(Context context, AttributeSet attrs) {
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

        powerBarBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        powerBarBgPaint.setStyle(Paint.Style.FILL);
        powerBarBgPaint.setColor(Color.argb(100, 50, 50, 50));

        powerBarFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        powerBarFillPaint.setStyle(Paint.Style.FILL);
        powerBarFillPaint.setColor(themeColor);
        powerBarFillPaint.setShadowLayer(10, 0, 0, themeColor);

        stageTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        stageTextPaint.setColor(themeColor);
        stageTextPaint.setTextSize(11 * getResources().getDisplayMetrics().scaledDensity);
        stageTextPaint.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));

        livesTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        livesTextPaint.setColor(themeColor);
        livesTextPaint.setTextSize(12 * getResources().getDisplayMetrics().scaledDensity);
        livesTextPaint.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));

        energyCorePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        energyCoreGlowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        energyCoreGlowPaint.setStyle(Paint.Style.FILL);

        heartPath = new Path();
    }

    public void setPower(int power) {
        this.power = Math.max(0, Math.min(100, power));
        invalidate();
    }

    public void setStage(String stage) {
        this.stage = stage;
        invalidate();
    }

    public void setLevelInfo(String info) {
        this.levelInfo = info;
        invalidate();
    }

    public void setLives(int lives) {
        this.lives = lives;
        invalidate();
    }

    public void setThemeColor(int color) {
        this.themeColor = color;
        borderPaint.setColor(color);
        glowPaint.setColor(color);
        textPaint.setColor(color);
        powerBarFillPaint.setColor(color);
        powerBarFillPaint.setShadowLayer(10, 0, 0, color);
        stageTextPaint.setColor(color);
        livesTextPaint.setColor(color);
        invalidate();
    }

    public void setCoins(int coins) {
        this.coins = coins;
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
        bgPaint.setColor(Color.argb(255, 60, 20, 20)); // Reddish dark header for Power panel
        canvas.drawRoundRect(headerRect, 8, 8, bgPaint);

        borderPaint.setAlpha(255);
        borderPaint.setStrokeWidth(2f);
        canvas.drawRoundRect(headerRect, 8, 8, borderPaint);

        // Header Text
        textPaint.setTextSize(10 * getResources().getDisplayMetrics().scaledDensity);
        textPaint.setColor(Color.WHITE);
        float titleW = textPaint.measureText("SYSTEM CORE");
        canvas.drawText("SYSTEM CORE", w / 2 - titleW / 2, margin + headerH * 0.7f, textPaint);
        textPaint.setColor(themeColor); // Restore

        // 4. Detailed Borders & Sides
        borderPaint.setAlpha(60);
        borderPaint.setStrokeWidth(1.5f);
        canvas.drawRoundRect(new RectF(margin, margin + headerH / 2, w - margin, h - margin), 10, 10, borderPaint);

        // Decorative Side Brackets
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

        // 5. Content Layout
        float leftContent = margin + 20f * density;
        float startY = margin + headerH + 12 * density;
        float spacing = (h - startY - margin) / 4;

        textPaint.setTextSize(10 * getResources().getDisplayMetrics().scaledDensity);

        // Line 0: LEVEL
        canvas.drawText(levelInfo, leftContent, startY, textPaint);

        // Line 1: STAGE
        canvas.drawText("STAGE: ", leftContent, startY + spacing, textPaint);
        canvas.drawText(stage, leftContent + textPaint.measureText("STAGE: "), startY + spacing, stageTextPaint);

        // Line 2: POWER
        canvas.drawText("POW:", leftContent, startY + spacing * 2, textPaint);
        float barX = leftContent + textPaint.measureText("POW:") + 10;
        float barW = w - barX - margin - 35 * density;
        float barH = 5f * density;
        float barY = (startY + spacing * 2) - barH * 0.8f;

        // Bar Bg
        canvas.drawRoundRect(new RectF(barX, barY, barX + barW, barY + barH), 3 * density, 3 * density,
                powerBarBgPaint);
        // Bar Fill
        canvas.drawRoundRect(new RectF(barX, barY, barX + (barW * power / 100f), barY + barH), 3 * density, 3 * density,
                powerBarFillPaint);
        canvas.drawText(power + "%", w - margin - 30 * density, startY + spacing * 2, stageTextPaint);

        // Line 3: LIVES
        float iconSize = 16 * density;
        float iconX = leftContent;
        // Add explicit offset to separate from POW bar
        float livesOffsetY = 6 * density;
        float iconY = (startY + spacing * 3) - iconSize * 0.8f + livesOffsetY;
        drawVectorEnergyCore(canvas, iconX + iconSize / 2, iconY + iconSize / 2, iconSize / 2);
        canvas.drawText(String.valueOf(lives), iconX + iconSize + 10, startY + spacing * 3 + livesOffsetY,
                livesTextPaint);

        // COINS display (next to lives)
        float coinIconX = iconX + iconSize + 10 + livesTextPaint.measureText(String.valueOf(lives)) + 20 * density;
        drawCoinIcon(canvas, coinIconX + iconSize / 2, iconY + iconSize / 2, iconSize / 2);
        Paint coinPaint = new Paint(livesTextPaint);
        coinPaint.setColor(Color.rgb(255, 215, 0)); // Gold color
        canvas.drawText(String.valueOf(coins), coinIconX + iconSize + 10, startY + spacing * 3 + livesOffsetY,
                coinPaint);

        // Technical accents
        bgPaint.setColor(Color.argb(150, 200, 200, 200));
        float sDist = 3 * density;
        canvas.drawCircle(margin + sDist, h - margin - sDist, 1.5f * density, bgPaint);
        canvas.drawCircle(w - margin - sDist, h - margin - sDist, 1.5f * density, bgPaint);
    }

    private void drawVectorEnergyCore(Canvas canvas, float cx, float cy, float radius) {
        // Outer glow pulse
        energyCoreGlowPaint.setShader(new android.graphics.RadialGradient(cx, cy, radius * 1.5f,
                new int[] { Color.argb(120, 0, 255, 255), Color.TRANSPARENT }, null,
                android.graphics.Shader.TileMode.CLAMP));
        canvas.drawCircle(cx, cy, radius * 1.5f, energyCoreGlowPaint);

        // Core shell
        energyCorePaint.setStyle(Paint.Style.STROKE);
        energyCorePaint.setStrokeWidth(2f);
        energyCorePaint.setColor(Color.CYAN);
        canvas.drawCircle(cx, cy, radius, energyCorePaint);

        // Inner nucleus
        energyCorePaint.setStyle(Paint.Style.FILL);
        energyCorePaint.setColor(Color.WHITE);
        canvas.drawCircle(cx, cy, radius * 0.4f, energyCorePaint);

        // Orbital rings
        energyCorePaint.setStyle(Paint.Style.STROKE);
        energyCorePaint.setStrokeWidth(1f);
        energyCorePaint.setColor(Color.argb(180, 0, 255, 255));
        canvas.drawOval(new RectF(cx - radius, cy - radius * 0.3f, cx + radius, cy + radius * 0.3f), energyCorePaint);
    }

    private void drawCoinIcon(Canvas canvas, float cx, float cy, float radius) {
        // Outer glow
        Paint glowP = new Paint(Paint.ANTI_ALIAS_FLAG);
        glowP.setStyle(Paint.Style.FILL);
        glowP.setShader(new android.graphics.RadialGradient(cx, cy, radius * 1.4f,
                new int[] { Color.argb(80, 255, 215, 0), Color.TRANSPARENT }, null,
                android.graphics.Shader.TileMode.CLAMP));
        canvas.drawCircle(cx, cy, radius * 1.4f, glowP);

        // Main coin body
        Paint coinP = new Paint(Paint.ANTI_ALIAS_FLAG);
        coinP.setStyle(Paint.Style.FILL);
        coinP.setColor(Color.rgb(255, 215, 0)); // Gold
        canvas.drawCircle(cx, cy, radius, coinP);

        // Inner ring
        coinP.setStyle(Paint.Style.STROKE);
        coinP.setStrokeWidth(1.5f);
        coinP.setColor(Color.rgb(218, 165, 32)); // Dark gold
        canvas.drawCircle(cx, cy, radius * 0.8f, coinP);

        // Shine effect
        coinP.setStyle(Paint.Style.FILL);
        coinP.setColor(Color.rgb(255, 255, 200)); // Light yellow
        canvas.drawCircle(cx - radius * 0.3f, cy - radius * 0.3f, radius * 0.3f, coinP);
    }

    private void drawHeart(Canvas canvas, float x, float y, float size) {
        heartPath.reset();
        float halfSize = size / 2;
        // Simple heart shape
        heartPath.moveTo(x, y + halfSize * 0.6f);
        heartPath.cubicTo(x - halfSize * 2, y - halfSize, x - halfSize * 0.5f, y - halfSize * 1.5f, x,
                y - halfSize * 0.5f);
        heartPath.cubicTo(x + halfSize * 0.5f, y - halfSize * 1.5f, x + halfSize * 2, y - halfSize, x,
                y + halfSize * 0.6f);

        canvas.drawPath(heartPath, heartPaint);
    }
}

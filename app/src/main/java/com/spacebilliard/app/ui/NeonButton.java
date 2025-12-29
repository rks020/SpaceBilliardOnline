package com.spacebilliard.app.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class NeonButton extends View {

    private String text = "BUTTON";
    private int themeColor = Color.CYAN;
    private Paint borderPaint;
    private Paint textPaint;
    private Paint glowPaint;
    private Paint bgPaint;
    private Paint shadowPaint;
    private Paint accentPaint;
    private RectF bounds;
    private boolean isPressed = false;

    public NeonButton(Context context) {
        super(context);
        init();
    }

    public NeonButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public NeonButton(Context context, String text, int color) {
        super(context);
        this.text = text;
        this.themeColor = color;
        init();
    }

    private void init() {
        setLayerType(LAYER_TYPE_SOFTWARE, null);

        borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(3f);

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setFakeBoldText(true);

        glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        glowPaint.setStyle(Paint.Style.STROKE);

        bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bgPaint.setStyle(Paint.Style.FILL);

        shadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        shadowPaint.setStyle(Paint.Style.FILL);

        accentPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        accentPaint.setStyle(Paint.Style.FILL);

        setClickable(true);
    }

    public void setText(String text) {
        this.text = text;
        invalidate();
    }

    public void setThemeColor(int color) {
        this.themeColor = color;
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int desiredWidth = (int) (140 * getResources().getDisplayMetrics().density);
        int desiredHeight = (int) (50 * getResources().getDisplayMetrics().density);

        setMeasuredDimension(resolveSize(desiredWidth, widthMeasureSpec),
                resolveSize(desiredHeight, heightMeasureSpec));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float density = getResources().getDisplayMetrics().density;
        float w = getWidth();
        float h = getHeight();
        float margin = 4 * density;
        float radius = h / 2; // Pill shape

        if (bounds == null) {
            bounds = new RectF();
        }
        bounds.set(margin, margin, w - margin, h - margin);

        // 1. Shadow Layer (Bottom depth)
        shadowPaint.setColor(Color.BLACK);
        shadowPaint.setAlpha(isPressed ? 40 : 80);
        RectF shadowRect = new RectF(bounds.left + 2 * density, bounds.top + 3 * density,
                bounds.right + 2 * density, bounds.bottom + 3 * density);
        canvas.drawRoundRect(shadowRect, radius, radius, shadowPaint);

        // 2. Main Body with Gradient
        int topColor = isPressed ? darkenColor(themeColor, 0.4f) : lightenColor(themeColor, 0.2f);
        int bottomColor = isPressed ? darkenColor(themeColor, 0.6f) : darkenColor(themeColor, 0.3f);

        LinearGradient gradient = new LinearGradient(
                0, bounds.top, 0, bounds.bottom,
                topColor, bottomColor, Shader.TileMode.CLAMP);
        bgPaint.setShader(gradient);
        canvas.drawRoundRect(bounds, radius, radius, bgPaint);
        bgPaint.setShader(null);

        // 3. Top Accent Bar (Decorative highlight)
        float accentH = h * 0.25f;
        RectF accentRect = new RectF(bounds.left + 8 * density, bounds.top + 4 * density,
                bounds.right - 8 * density, bounds.top + accentH);
        accentPaint.setColor(Color.WHITE);
        accentPaint.setAlpha(isPressed ? 30 : 60);
        canvas.drawRoundRect(accentRect, radius * 0.6f, radius * 0.6f, accentPaint);

        // 4. Outer Glow
        glowPaint.setColor(themeColor);
        glowPaint.setAlpha(isPressed ? 60 : 100);
        glowPaint.setStrokeWidth(6f);
        canvas.drawRoundRect(bounds, radius, radius, glowPaint);

        // 5. Border
        borderPaint.setColor(lightenColor(themeColor, 0.3f));
        borderPaint.setAlpha(isPressed ? 180 : 255);
        canvas.drawRoundRect(bounds, radius, radius, borderPaint);

        // 6. Inner Border (Detail)
        borderPaint.setStrokeWidth(1.5f);
        borderPaint.setAlpha(100);
        RectF innerBorder = new RectF(bounds.left + 3 * density, bounds.top + 3 * density,
                bounds.right - 3 * density, bounds.bottom - 3 * density);
        canvas.drawRoundRect(innerBorder, radius * 0.8f, radius * 0.8f, borderPaint);

        // 7. Text
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(h * 0.4f);
        textPaint.setShadowLayer(8, 0, isPressed ? 1 : 2, Color.argb(150, 0, 0, 0));

        Paint.FontMetrics fm = textPaint.getFontMetrics();
        float textY = h / 2 - (fm.descent + fm.ascent) / 2;
        if (isPressed)
            textY += 2 * density;

        canvas.drawText(text, w / 2, textY, textPaint);
        textPaint.clearShadowLayer();
    }

    private int lightenColor(int color, float factor) {
        int r = Color.red(color);
        int g = Color.green(color);
        int b = Color.blue(color);

        r = Math.min(255, (int) (r + (255 - r) * factor));
        g = Math.min(255, (int) (g + (255 - g) * factor));
        b = Math.min(255, (int) (b + (255 - b) * factor));

        return Color.rgb(r, g, b);
    }

    private int darkenColor(int color, float factor) {
        int r = Color.red(color);
        int g = Color.green(color);
        int b = Color.blue(color);

        r = (int) (r * (1 - factor));
        g = (int) (g * (1 - factor));
        b = (int) (b * (1 - factor));

        return Color.rgb(r, g, b);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                isPressed = true;
                invalidate();
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                isPressed = false;
                invalidate();
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    performClick();
                }
                return true;
        }
        return super.onTouchEvent(event);
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }
}

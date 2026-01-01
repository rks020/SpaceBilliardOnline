package com.spacebilliard.app.ui;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

public class PlanetView extends View {

    private Paint planetPaint;
    private Paint ringPaint;
    private Paint glowPaint;

    private ValueAnimator animator;

    // Colors
    private final int COLOR_CORE = 0xFF120024; // Dark deep purple
    private final int COLOR_GLOW = 0xFF00E5FF; // Cyan Neon
    private final int COLOR_RING_1 = 0xFFD500F9; // Purple Neon
    private final int COLOR_RING_2 = 0xFF00E5FF; // Cyan Neon

    public PlanetView(Context context) {
        super(context);
        init();
    }

    public PlanetView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private long startTime;

    private void init() {
        planetPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        ringPaint.setStyle(Paint.Style.STROKE);
        ringPaint.setStrokeWidth(6f);

        glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        glowPaint.setStyle(Paint.Style.FILL);

        startTime = android.os.SystemClock.uptimeMillis();

        // Start internal rotation animation (Driver)
        // We use this just to invalidate the view continuously
        animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(1000);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setInterpolator(new LinearInterpolator());
        animator.addUpdateListener(animation -> {
            invalidate();
        });
        animator.start();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float cx = getWidth() / 2f;
        float cy = getHeight() / 2f;
        float radius = Math.min(cx, cy) * 0.5f; // Planet radius

        // Calculate dynamic angles based on time for continuous non-jumping flow
        long elapsed = android.os.SystemClock.uptimeMillis() - startTime;
        float t = elapsed / 1000f; // seconds

        float angle1 = t * 20f; // 20 degrees per second
        float angle2 = 45f - t * 15f; // -15 deg/s, start at 45
        float angle3 = 90f + t * 25f; // 25 deg/s, start at 90

        // 1. Draw Outer Glow (Backlight)
        glowPaint.setShader(new RadialGradient(
                cx, cy, radius * 1.5f,
                Color.argb(100, 0, 229, 255), // Semi-transparent Cyan
                Color.TRANSPARENT,
                Shader.TileMode.CLAMP));
        canvas.drawCircle(cx, cy, radius * 1.5f, glowPaint);

        // 2. Draw Planet Body
        // Radial gradient for 3D effect: Top-Left Light -> Bottom-Right Dark
        planetPaint.setShader(new RadialGradient(
                cx - radius * 0.3f, cy - radius * 0.3f, radius * 1.2f,
                new int[] { COLOR_GLOW, COLOR_CORE, Color.BLACK },
                new float[] { 0.1f, 0.6f, 1.0f },
                Shader.TileMode.CLAMP));
        canvas.drawCircle(cx, cy, radius, planetPaint);

        // 3. Draw Rings
        // We will draw elliptical rings rotated around the planet
        drawRing(canvas, cx, cy, radius * 1.8f, radius * 0.4f, angle1, COLOR_RING_1);
        drawRing(canvas, cx, cy, radius * 2.0f, radius * 0.3f, angle2, COLOR_RING_2);
        drawRing(canvas, cx, cy, radius * 1.6f, radius * 0.5f, angle3, 0xAAD500F9); // Semi-transparent purple
    }

    private void drawRing(Canvas canvas, float cx, float cy, float width, float height, float angle, int color) {
        canvas.save();
        canvas.rotate(angle, cx, cy);

        RectF oval = new RectF(cx - width / 2, cy - height / 2, cx + width / 2, cy + height / 2);

        // Ring Gradient for depth
        ringPaint.setColor(color);
        ringPaint.setShader(new LinearGradient(
                oval.left, oval.top, oval.right, oval.bottom,
                new int[] { color, Color.TRANSPARENT, color },
                null, Shader.TileMode.CLAMP));

        canvas.drawOval(oval, ringPaint);

        canvas.restore();
    }
}

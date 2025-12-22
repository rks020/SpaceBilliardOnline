package com.spacebilliard.app.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

public class NeonShopItem extends View {

    private String itemName;
    private String description = "";
    private String price;
    private int themeColor;
    private String quantity = "x5";
    private String priceIcon = "GEM"; // GEM, HEART, COIN, NONE
    private boolean isSelected = false;
    private String skinId = null;

    // Paints
    private Paint glowPaint;
    private Paint borderPaint;
    private Paint bgPaint;
    private Paint textPaint;
    private Paint iconPaint;
    private Paint pillPaint;

    public NeonShopItem(Context context, String name, String price, int color) {
        super(context);
        this.itemName = name;
        this.price = price;
        this.themeColor = color;
        init();
    }

    public NeonShopItem(Context context, String name, String price, int color, String qty, String pIcon, String desc) {
        super(context);
        this.itemName = name;
        this.price = price;
        this.themeColor = color;
        this.quantity = qty;
        this.priceIcon = pIcon;
        this.description = desc;
        init();
    }

    public NeonShopItem(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.itemName = "ITEM";
        this.price = "0";
        this.themeColor = Color.CYAN;
        init();
    }

    private void init() {
        setLayerType(LAYER_TYPE_SOFTWARE, null);

        glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        glowPaint.setStyle(Paint.Style.STROKE);

        borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(4f);

        bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bgPaint.setStyle(Paint.Style.FILL);

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setFakeBoldText(true);

        iconPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        iconPaint.setStyle(Paint.Style.STROKE);
        iconPaint.setStrokeWidth(5f);

        pillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        pillPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        pillPaint.setStrokeWidth(2f);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int desiredWidth = (int) (110 * getResources().getDisplayMetrics().density);
        int desiredHeight = (int) (140 * getResources().getDisplayMetrics().density);
        setMeasuredDimension(resolveSize(desiredWidth, widthMeasureSpec),
                resolveSize(desiredHeight, heightMeasureSpec));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float w = getWidth();
        float h = getHeight();
        float pad = 12;
        float cornerRadius = 35f;

        RectF rect = new RectF(pad, pad, w - pad, h - pad);

        // 1. Draw Card Background with subtle gradient
        int topColor = Color.argb(100, 10, 40, 60);
        int bottomColor = Color.argb(150, 5, 10, 20);
        bgPaint.setShader(new LinearGradient(0, 0, 0, h, topColor, bottomColor, Shader.TileMode.CLAMP));
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, bgPaint);
        bgPaint.setShader(null);

        // 2. Wavy/Stylized Neon Border (Inner)
        borderPaint.setColor(themeColor);
        borderPaint.setAlpha(255);

        // Draw 3 layers of glow for the border
        for (int i = 1; i <= 3; i++) {
            glowPaint.setColor(themeColor);
            glowPaint.setAlpha(100 / i);
            glowPaint.setStrokeWidth(i * 3);
            canvas.drawRoundRect(rect, cornerRadius, cornerRadius, glowPaint);
        }
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, borderPaint);

        // 3. Draw Icon (Placeholder shape based on itemName)
        drawItemIcon(canvas, w / 2, h * 0.4f, w * 0.25f);

        // 4. Quantity Label (x5)
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(h * 0.12f);
        textPaint.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText(quantity, w - pad - 15, h * 0.65f, textPaint);

        // 5. Price Pill at the bottom
        drawPricePill(canvas, w / 2, h * 0.82f);
    }

    private void drawItemIcon(Canvas canvas, float cx, float cy, float size) {
        iconPaint.setColor(themeColor);
        iconPaint.setShadowLayer(15, 0, 0, themeColor);

        // 1. Prioritize Skin ID Logic
        if (skinId != null && !skinId.isEmpty()) {
            if (skinId.startsWith("trail_")) {
                // Draw Trails
                float trailSize = size * 0.8f;
                Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
                p.setStyle(Paint.Style.FILL);
                p.setColor(themeColor);
                p.setShadowLayer(15, 0, 0, themeColor);

                if (skinId.contains("electric")) {
                    // Electric Trail
                    p.setStyle(Paint.Style.STROKE);
                    p.setStrokeWidth(5);
                    android.graphics.Path path = new android.graphics.Path();
                    path.moveTo(cx - size, cy + size * 0.5f);
                    path.lineTo(cx - size * 0.4f, cy - size * 0.2f);
                    path.lineTo(cx + size * 0.2f, cy + size * 0.4f);
                    path.lineTo(cx + size, cy - size * 0.5f);
                    canvas.drawPath(path, p);
                } else if (skinId.contains("rainbow")) {
                    // Rainbow Trail
                    int[] rb = { Color.RED, Color.YELLOW, Color.GREEN, Color.BLUE, Color.MAGENTA };
                    for (int i = 0; i < 5; i++) {
                        p.setColor(rb[i % rb.length]);
                        canvas.drawCircle(cx - size + i * size * 0.5f, cy + size - i * size * 0.5f, 8, p);
                    }
                } else if (skinId.contains("cosmic")) {
                    // Cosmic Trail
                    p.setColor(Color.WHITE);
                    drawStarIcon(canvas, cx - size * 0.5f, cy + size * 0.3f, size * 0.25f, p);
                    drawStarIcon(canvas, cx + size * 0.5f, cy - size * 0.3f, size * 0.2f, p);
                    p.setColor(themeColor);
                    drawStarIcon(canvas, cx, cy, size * 0.4f, p);
                } else if (skinId.contains("lava")) {
                    // Lava Trail
                    p.setColor(Color.rgb(255, 100, 0)); // Orange
                    canvas.drawCircle(cx, cy + size * 0.2f, size * 0.5f, p);
                    p.setColor(Color.RED);
                    canvas.drawCircle(cx - size * 0.5f, cy - size * 0.3f, size * 0.3f, p);
                    canvas.drawCircle(cx + size * 0.5f, cy - size * 0.4f, size * 0.25f, p);
                } else if (skinId.contains("ghost")) {
                    // Ghost Trail (Fading)
                    for (int i = 0; i < 4; i++) {
                        p.setColor(Color.WHITE);
                        p.setAlpha(100 - i * 20);
                        canvas.drawCircle(cx - i * size * 0.3f, cy, size * (0.8f - i * 0.1f), p);
                    }
                } else if (skinId.contains("bubble")) {
                    // Bubble Trail (Outlined)
                    p.setStyle(Paint.Style.STROKE);
                    p.setStrokeWidth(3);
                    p.setColor(Color.CYAN);
                    canvas.drawCircle(cx, cy, size * 0.7f, p);
                    canvas.drawCircle(cx - size * 0.5f, cy + size * 0.2f, size * 0.4f, p);
                    canvas.drawCircle(cx + size * 0.4f, cy - size * 0.3f, size * 0.3f, p);
                } else if (skinId.contains("pixel")) {
                    // Pixel Trail (Squares)
                    p.setColor(Color.GREEN);
                    canvas.drawRect(cx - size * 0.6f, cy - size * 0.6f, cx - size * 0.2f, cy - size * 0.2f, p);
                    canvas.drawRect(cx + size * 0.1f, cy, cx + size * 0.5f, cy + size * 0.4f, p);
                    canvas.drawRect(cx - size * 0.3f, cy + size * 0.3f, cx, cy + size * 0.6f, p);
                } else {
                    // Default/Color Trail
                    for (int i = 0; i < 5; i++) {
                        float tr = trailSize * (1.0f - i * 0.18f);
                        float tx = cx - i * (trailSize * 0.4f);
                        p.setColor(themeColor);
                        p.setAlpha(200 - i * 40);
                        canvas.drawCircle(tx, cy, tr, p);
                    }
                    p.setAlpha(255);
                    canvas.drawCircle(cx, cy, trailSize, p);
                }
                iconPaint.clearShadowLayer();
                return;

            } else if (skinId.startsWith("traj_")) {
                // Draw Trajectories
                Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(6);
                p.setColor(themeColor);
                p.setShadowLayer(15, 0, 0, themeColor);

                if (skinId.contains("dots")) { // Pearl/Dots
                    p.setStyle(Paint.Style.FILL);
                    canvas.drawCircle(cx - size * 0.6f, cy + size * 0.6f, 6, p);
                    canvas.drawCircle(cx, cy, 8, p);
                    canvas.drawCircle(cx + size * 0.6f, cy - size * 0.6f, 6, p);
                } else if (skinId.contains("electric")) {
                    android.graphics.Path path = new android.graphics.Path();
                    path.moveTo(cx - size, cy + size);
                    path.lineTo(cx - size * 0.2f, cy);
                    path.lineTo(cx + size * 0.2f, cy + size * 0.2f);
                    path.lineTo(cx + size, cy - size);
                    canvas.drawPath(path, p);
                } else if (skinId.contains("plasma")) {
                    p.setStyle(Paint.Style.FILL);
                    for (int i = 0; i < 3; i++) {
                        p.setAlpha(100 + i * 70);
                        canvas.drawCircle(cx - size * 0.7f + i * size * 0.7f, cy + size * 0.7f - i * size * 0.7f,
                                6 + i * 2, p);
                    }
                } else if (skinId.contains("arrow")) {
                    // Arrow Trajectory
                    p.setStyle(Paint.Style.FILL);
                    for (int i = 0; i < 3; i++) {
                        float offset = (i - 1) * size * 0.8f;
                        android.graphics.Path path = new android.graphics.Path();
                        path.moveTo(cx + offset - size * 0.3f, cy + offset + size * 0.3f); // Top Left of arrow
                        path.lineTo(cx + offset + size * 0.3f, cy + offset - size * 0.3f); // Top Right
                        path.lineTo(cx + offset, cy + offset - size * 0.3f);
                        // Simplified triangles
                        canvas.drawCircle(cx + offset, cy - offset, 6, p);
                        // Better arrow
                        path.reset();
                        path.moveTo(cx + offset - 10, cy - offset);
                        path.lineTo(cx + offset, cy - offset - 10);
                        path.lineTo(cx + offset + 10, cy - offset);
                        canvas.drawPath(path, p);
                    }
                } else if (skinId.contains("wave")) {
                    // Wave Trajectory
                    android.graphics.Path path = new android.graphics.Path();
                    path.moveTo(cx - size, cy);
                    path.quadTo(cx - size * 0.5f, cy - size * 0.8f, cx, cy);
                    path.quadTo(cx + size * 0.5f, cy + size * 0.8f, cx + size, cy);
                    canvas.drawPath(path, p);
                } else {
                    // Laser (Solid Line)
                    canvas.drawLine(cx - size, cy + size, cx + size, cy - size, p);
                }
                iconPaint.clearShadowLayer();
                return;

            } else if (skinId.startsWith("aura_")) {
                // Removed Aura drawing as requested
                iconPaint.clearShadowLayer();
                return;

            } else if (skinId.startsWith("impact_")) {
                // Effects
                Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(5);
                p.setColor(themeColor);
                p.setShadowLayer(15, 0, 0, themeColor);

                if (skinId.contains("stars")) {
                    p.setStyle(Paint.Style.FILL);
                    drawStarIcon(canvas, cx, cy, size * 0.6f, p);
                    drawStarIcon(canvas, cx - size * 0.5f, cy - size * 0.5f, size * 0.3f, p);
                    drawStarIcon(canvas, cx + size * 0.5f, cy + size * 0.5f, size * 0.3f, p);
                } else if (skinId.contains("electric")) {
                    // Electric Boom
                    for (int i = 0; i < 4; i++) {
                        float angle = (float) (i * Math.PI / 2);
                        float x1 = cx + (float) Math.cos(angle) * (size * 0.3f);
                        float y1 = cy + (float) Math.sin(angle) * (size * 0.3f);
                        float x2 = cx + (float) Math.cos(angle) * size;
                        float y2 = cy + (float) Math.sin(angle) * size;

                        // Zig zag ray
                        android.graphics.Path path = new android.graphics.Path();
                        path.moveTo(x1, y1);
                        path.lineTo((x1 + x2) / 2 + 10, (y1 + y2) / 2 - 10);
                        path.lineTo(x2, y2);
                        canvas.drawPath(path, p);
                    }
                } else if (skinId.contains("ripple")) {
                    // Ripple Impact
                    for (int i = 1; i <= 3; i++) {
                        p.setAlpha(255 - i * 60);
                        canvas.drawCircle(cx, cy, size * 0.3f * i, p);
                    }
                } else if (skinId.contains("confetti")) {
                    // Confetti Impact
                    p.setStyle(Paint.Style.FILL);
                    int[] colors = { Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW };
                    for (int i = 0; i < 8; i++) {
                        p.setColor(colors[i % 4]);
                        float angle = (float) (i * Math.PI / 4);
                        float dist = size * 0.7f;
                        float px = cx + (float) Math.cos(angle) * dist;
                        float py = cy + (float) Math.sin(angle) * dist;
                        canvas.drawRect(px - 4, py - 4, px + 4, py + 4, p);
                    }
                } else {
                    // Standard Boom
                    canvas.drawCircle(cx, cy, size * 0.5f, p);
                    canvas.drawCircle(cx, cy, size, p);
                }
                iconPaint.clearShadowLayer();
                return;
            } else if (skinId.equals("cyber_core")) {
                iconPaint.setColor(themeColor);
                iconPaint.setShadowLayer(15, 0, 0, themeColor);
                canvas.drawCircle(cx, cy, size, iconPaint);
                Paint p = new Paint(iconPaint);
                p.setStrokeWidth(3);
                canvas.drawArc(cx - size * 0.7f, cy - size * 0.7f, cx + size * 0.7f, cy + size * 0.7f, 45, 90, false,
                        p);
                canvas.drawArc(cx - size * 0.7f, cy - size * 0.7f, cx + size * 0.7f, cy + size * 0.7f, 225, 90, false,
                        p);
                canvas.drawCircle(cx, cy, size * 0.3f, p);
                iconPaint.clearShadowLayer();
                return;
            } else if (skinId.equals("solar_flare")) {
                iconPaint.setColor(themeColor);
                iconPaint.setShadowLayer(20, 0, 0, themeColor);
                canvas.drawCircle(cx, cy, size * 0.8f, iconPaint);
                for (int i = 0; i < 12; i++) {
                    float angle = (float) (i * Math.PI / 6);
                    canvas.drawLine(cx + (float) Math.cos(angle) * size * 0.9f,
                            cy + (float) Math.sin(angle) * size * 0.9f,
                            cx + (float) Math.cos(angle) * size * 1.2f, cy + (float) Math.sin(angle) * size * 1.2f,
                            iconPaint);
                }
                iconPaint.clearShadowLayer();
                return;
            } else if (skinId.equals("frost_bite")) {
                iconPaint.setColor(themeColor);
                iconPaint.setShadowLayer(15, 0, 0, Color.WHITE);
                canvas.drawCircle(cx, cy, size, iconPaint);
                canvas.drawLine(cx - size * 0.5f, cy - size * 0.5f, cx, cy, iconPaint);
                canvas.drawLine(cx + size * 0.3f, cy - size * 0.6f, cx - size * 0.1f, cy + size * 0.2f, iconPaint);
                iconPaint.clearShadowLayer();
                return;
            } else if (!skinId.equals("default") && !skinId.equals("neon_pulse") && !skinId.equals("soccer")) {
                drawFlagIcon(canvas, cx, cy, size, skinId);
                iconPaint.clearShadowLayer();
                return;
            }
        }

        // 2. Fallback to Name Checks (Legacy support)
        String name = itemName.toUpperCase();
        if (name.contains("CUE")) {
            canvas.drawLine(cx - size, cy + size, cx + size, cy - size, iconPaint);
            canvas.drawCircle(cx + size, cy - size, 5, iconPaint);
        } else if (name.contains("LIVES") || name.contains("OWNED")) {
            RectF r = new RectF(cx - size, cy - size * 0.7f, cx + size, cy + size * 0.7f);
            canvas.drawRoundRect(r, 10, 10, iconPaint);
            canvas.drawCircle(cx, cy, size * 0.3f, iconPaint);
        } else if (name.contains("SOCCER")) {
            canvas.drawCircle(cx, cy, size, iconPaint);
            for (int i = 0; i < 5; i++) {
                float angle = (float) (i * 2 * Math.PI / 5);
                canvas.drawLine(cx, cy, cx + (float) Math.cos(angle) * size, cy + (float) Math.sin(angle) * size,
                        iconPaint);
            }
        } else {
            // Default: Double circle ball
            canvas.drawCircle(cx, cy, size, iconPaint);
            canvas.drawCircle(cx, cy, size * 0.4f, iconPaint);
            canvas.drawCircle(cx + size * 0.3f, cy - size * 0.3f, 4, iconPaint);
        }

        iconPaint.clearShadowLayer();
    }

    private void drawFlagIcon(Canvas canvas, float cx, float cy, float r, String id) {
        canvas.save();
        android.graphics.Path clip = new android.graphics.Path();
        clip.addCircle(cx, cy, r, android.graphics.Path.Direction.CW);
        canvas.clipPath(clip);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setStyle(Paint.Style.FILL);

        switch (id) {
            case "tr_flag":
                p.setColor(Color.RED);
                canvas.drawCircle(cx, cy, r, p);

                // Crescent
                p.setColor(Color.WHITE);
                canvas.drawCircle(cx - r * 0.15f, cy, r * 0.55f, p);
                p.setColor(Color.RED);
                canvas.drawCircle(cx - r * 0.05f, cy, r * 0.45f, p);

                // Star
                p.setColor(Color.WHITE);
                drawStarIcon(canvas, cx + r * 0.35f, cy, r * 0.25f, p);
                break;
            case "usa":
                for (int i = 0; i < 7; i++) {
                    p.setColor(i % 2 == 0 ? Color.RED : Color.WHITE);
                    canvas.drawRect(cx - r, cy - r + (i * 2 * r / 7), cx + r, cy - r + ((i + 1) * 2 * r / 7), p);
                }
                p.setColor(Color.rgb(0, 40, 104));
                canvas.drawRect(cx - r, cy - r, cx, cy, p);
                break;
            case "germany":
                p.setColor(Color.BLACK);
                canvas.drawRect(cx - r, cy - r, cx + r, cy - r / 3, p);
                p.setColor(Color.RED);
                canvas.drawRect(cx - r, cy - r / 3, cx + r, cy + r / 3, p);
                p.setColor(Color.YELLOW);
                canvas.drawRect(cx - r, cy + r / 3, cx + r, cy + r, p);
                break;
            case "france":
                p.setColor(Color.rgb(0, 85, 164));
                canvas.drawRect(cx - r, cy - r, cx - r / 3, cy + r, p);
                p.setColor(Color.WHITE);
                canvas.drawRect(cx - r / 3, cy - r, cx + r / 3, cy + r, p);
                p.setColor(Color.rgb(239, 65, 53));
                canvas.drawRect(cx + r / 3, cy - r, cx + r, cy + r, p);
                break;
            case "italy":
                p.setColor(Color.rgb(0, 146, 70));
                canvas.drawRect(cx - r, cy - r, cx - r / 3, cy + r, p);
                p.setColor(Color.WHITE);
                canvas.drawRect(cx - r / 3, cy - r, cx + r / 3, cy + r, p);
                p.setColor(Color.rgb(206, 43, 55));
                canvas.drawRect(cx + r / 3, cy - r, cx + r, cy + r, p);
                break;
            case "uk":
                p.setColor(Color.rgb(1, 33, 105));
                canvas.drawCircle(cx, cy, r, p);
                p.setColor(Color.WHITE);
                canvas.drawRect(cx - r, cy - r * 0.2f, cx + r, cy + r * 0.2f, p);
                canvas.drawRect(cx - r * 0.2f, cy - r, cx + r * 0.2f, cy + r, p);
                p.setColor(Color.RED);
                canvas.drawRect(cx - r, cy - r * 0.1f, cx + r, cy + r * 0.1f, p);
                canvas.drawRect(cx - r * 0.1f, cy - r, cx + r * 0.1f, cy + r, p);
                break;
            case "spain":
                p.setColor(Color.rgb(170, 21, 27));
                canvas.drawRect(cx - r, cy - r, cx + r, cy - r / 3, p);
                p.setColor(Color.rgb(255, 196, 0));
                canvas.drawRect(cx - r, cy - r / 3, cx + r, cy + r / 3, p);
                p.setColor(Color.rgb(170, 21, 27));
                canvas.drawRect(cx - r, cy + r / 3, cx + r, cy + r, p);
                break;
            case "portugal":
                p.setColor(Color.rgb(0, 102, 0));
                canvas.drawRect(cx - r, cy - r, cx - r * 0.2f, cy + r, p);
                p.setColor(Color.RED);
                canvas.drawRect(cx - r * 0.2f, cy - r, cx + r, cy + r, p);
                break;
            case "netherlands":
                p.setColor(Color.rgb(174, 28, 40));
                canvas.drawRect(cx - r, cy - r, cx + r, cy - r / 3, p);
                p.setColor(Color.WHITE);
                canvas.drawRect(cx - r, cy - r / 3, cx + r, cy + r / 3, p);
                p.setColor(Color.rgb(33, 70, 139));
                canvas.drawRect(cx - r, cy + r / 3, cx + r, cy + r, p);
                break;
            case "belgium":
                p.setColor(Color.BLACK);
                canvas.drawRect(cx - r, cy - r, cx - r / 3, cy + r, p);
                p.setColor(Color.rgb(253, 218, 36));
                canvas.drawRect(cx - r / 3, cy - r, cx + r / 3, cy + r, p);
                p.setColor(Color.rgb(239, 51, 64));
                canvas.drawRect(cx + r / 3, cy - r, cx + r, cy + r, p);
                break;
            case "switzerland":
                p.setColor(Color.RED);
                canvas.drawCircle(cx, cy, r, p);
                p.setColor(Color.WHITE);
                canvas.drawRect(cx - r * 0.6f, cy - r * 0.15f, cx + r * 0.6f, cy + r * 0.15f, p);
                canvas.drawRect(cx - r * 0.15f, cy - r * 0.6f, cx + r * 0.15f, cy + r * 0.6f, p);
                break;
            case "austria":
                p.setColor(Color.RED);
                canvas.drawRect(cx - r, cy - r, cx + r, cy - r / 3, p);
                p.setColor(Color.WHITE);
                canvas.drawRect(cx - r, cy - r / 3, cx + r, cy + r / 3, p);
                p.setColor(Color.RED);
                canvas.drawRect(cx - r, cy + r / 3, cx + r, cy + r, p);
                break;
            case "sweden":
                p.setColor(Color.rgb(0, 106, 167)); // Blue
                canvas.drawRect(cx - r, cy - r, cx + r, cy + r, p);
                p.setColor(Color.rgb(254, 204, 0)); // Yellow
                canvas.drawRect(cx - r * 0.2f, cy - r, cx + r * 0.2f, cy + r, p);
                canvas.drawRect(cx - r, cy - r * 0.2f, cx + r, cy + r * 0.2f, p);
                break;
            case "norway":
                p.setColor(Color.rgb(186, 12, 47)); // Red
                canvas.drawRect(cx - r, cy - r, cx + r, cy + r, p);
                p.setColor(Color.WHITE);
                canvas.drawRect(cx - r * 0.3f, cy - r, cx + r * 0.3f, cy + r, p);
                canvas.drawRect(cx - r, cy - r * 0.3f, cx + r, cy + r * 0.3f, p);
                p.setColor(Color.rgb(0, 32, 91)); // Blue
                canvas.drawRect(cx - r * 0.15f, cy - r, cx + r * 0.15f, cy + r, p);
                canvas.drawRect(cx - r, cy - r * 0.15f, cx + r, cy + r * 0.15f, p);
                break;
            case "denmark":
                p.setColor(Color.rgb(200, 16, 46)); // Red
                canvas.drawRect(cx - r, cy - r, cx + r, cy + r, p);
                p.setColor(Color.WHITE);
                canvas.drawRect(cx - r * 0.2f, cy - r, cx + r * 0.2f, cy + r, p);
                canvas.drawRect(cx - r, cy - r * 0.2f, cx + r, cy + r * 0.2f, p);
                break;
            case "finland":
                p.setColor(Color.WHITE);
                canvas.drawRect(cx - r, cy - r, cx + r, cy + r, p);
                p.setColor(Color.rgb(0, 53, 128)); // Blue
                canvas.drawRect(cx - r * 0.2f, cy - r, cx + r * 0.2f, cy + r, p);
                canvas.drawRect(cx - r, cy - r * 0.2f, cx + r, cy + r * 0.2f, p);
                break;
            case "poland":
                p.setColor(Color.WHITE);
                canvas.drawRect(cx - r, cy - r, cx + r, cy, p);
                p.setColor(Color.rgb(220, 20, 60)); // Red
                canvas.drawRect(cx - r, cy, cx + r, cy + r, p);
                break;
            case "greece":
                p.setColor(Color.rgb(13, 94, 175)); // Blue
                canvas.drawRect(cx - r, cy - r, cx + r, cy + r, p);
                // Horizontal White Stripes (Simplified)
                p.setColor(Color.WHITE);
                canvas.drawRect(cx - r, cy - r * 0.6f, cx + r, cy - r * 0.4f, p);
                canvas.drawRect(cx - r, cy - r * 0.2f, cx + r, cy, p);
                canvas.drawRect(cx - r, cy + r * 0.2f, cx + r, cy + r * 0.4f, p);
                canvas.drawRect(cx - r, cy + r * 0.6f, cx + r, cy + r * 0.8f, p);
                // Cross canton
                p.setColor(Color.rgb(13, 94, 175));
                canvas.drawRect(cx - r, cy - r, cx, cy, p);
                p.setColor(Color.WHITE);
                canvas.drawRect(cx - r, cy - r * 0.6f, cx, cy - r * 0.4f, p);
                canvas.drawRect(cx - r * 0.6f, cy - r, cx - r * 0.4f, cy, p);
                break;
            case "ireland":
                p.setColor(Color.rgb(22, 155, 98)); // Green
                canvas.drawRect(cx - r, cy - r, cx - r / 3, cy + r, p);
                p.setColor(Color.WHITE);
                canvas.drawRect(cx - r / 3, cy - r, cx + r / 3, cy + r, p);
                p.setColor(Color.rgb(255, 136, 62)); // Orange
                canvas.drawRect(cx + r / 3, cy - r, cx + r, cy + r, p);
                break;
            case "canada":
                p.setColor(Color.RED);
                canvas.drawRect(cx - r, cy - r, cx - r / 3, cy + r, p);
                canvas.drawRect(cx + r / 3, cy - r, cx + r, cy + r, p);
                p.setColor(Color.WHITE);
                canvas.drawRect(cx - r / 3, cy - r, cx + r / 3, cy + r, p);
                p.setColor(Color.RED);
                // Maple leaf (Simplified diamond)
                android.graphics.Path leaf = new android.graphics.Path();
                leaf.moveTo(cx, cy - r * 0.4f);
                leaf.lineTo(cx + r * 0.3f, cy);
                leaf.lineTo(cx, cy + r * 0.4f);
                leaf.lineTo(cx - r * 0.3f, cy);
                leaf.close();
                canvas.drawPath(leaf, p);
                break;
            case "japan":
                p.setColor(Color.WHITE);
                canvas.drawCircle(cx, cy, r, p);
                p.setColor(Color.RED);
                canvas.drawCircle(cx, cy, r * 0.4f, p);
                break;
            case "korea":
                p.setColor(Color.WHITE);
                canvas.drawCircle(cx, cy, r, p);
                p.setColor(Color.RED);
                canvas.drawArc(cx - r * 0.5f, cy - r * 0.5f, cx + r * 0.5f, cy + r * 0.5f, -90, 180, true, p);
                p.setColor(Color.BLUE);
                canvas.drawArc(cx - r * 0.5f, cy - r * 0.5f, cx + r * 0.5f, cy + r * 0.5f, 90, 180, true, p);
                break;
            case "china":
                p.setColor(Color.RED);
                canvas.drawCircle(cx, cy, r, p);
                p.setColor(Color.YELLOW);
                drawStarIcon(canvas, cx - r * 0.3f, cy - r * 0.3f, r * 0.3f, p);
                break;
            case "russia":
                p.setColor(Color.WHITE);
                canvas.drawRect(cx - r, cy - r, cx + r, cy - r / 3, p);
                p.setColor(Color.BLUE);
                canvas.drawRect(cx - r, cy - r / 3, cx + r, cy + r / 3, p);
                p.setColor(Color.RED);
                canvas.drawRect(cx - r, cy + r / 3, cx + r, cy + r, p);
                break;
            case "india":
                p.setColor(Color.rgb(255, 153, 51)); // Saffron
                canvas.drawRect(cx - r, cy - r, cx + r, cy - r / 3, p);
                p.setColor(Color.WHITE);
                canvas.drawRect(cx - r, cy - r / 3, cx + r, cy + r / 3, p);
                p.setColor(Color.rgb(19, 136, 8)); // Green
                canvas.drawRect(cx - r, cy + r / 3, cx + r, cy + r, p);
                p.setColor(Color.BLUE);
                canvas.drawCircle(cx, cy, r * 0.15f, p);
                break;
            case "mexico":
                p.setColor(Color.rgb(0, 104, 71)); // Green
                canvas.drawRect(cx - r, cy - r, cx - r / 3, cy + r, p);
                p.setColor(Color.WHITE);
                canvas.drawRect(cx - r / 3, cy - r, cx + r / 3, cy + r, p);
                p.setColor(Color.RED);
                canvas.drawRect(cx + r / 3, cy - r, cx + r, cy + r, p);
                p.setColor(Color.rgb(139, 69, 19)); // Brown
                canvas.drawCircle(cx, cy, r * 0.15f, p);
                break;
            case "argentina":
                p.setColor(Color.rgb(117, 170, 219)); // Light Blue
                canvas.drawRect(cx - r, cy - r, cx + r, cy - r / 3, p);
                p.setColor(Color.WHITE);
                canvas.drawRect(cx - r, cy - r / 3, cx + r, cy + r / 3, p);
                p.setColor(Color.rgb(117, 170, 219));
                canvas.drawRect(cx - r, cy + r / 3, cx + r, cy + r, p);
                p.setColor(Color.YELLOW);
                canvas.drawCircle(cx, cy, r * 0.15f, p);
                break;
            case "azerbaijan":
                p.setColor(Color.rgb(0, 181, 226)); // Blue
                canvas.drawRect(cx - r, cy - r, cx + r, cy - r / 3, p);
                p.setColor(Color.RED);
                canvas.drawRect(cx - r, cy - r / 3, cx + r, cy + r / 3, p);
                p.setColor(Color.GREEN);
                canvas.drawRect(cx - r, cy + r / 3, cx + r, cy + r, p);
                p.setColor(Color.WHITE);
                canvas.drawCircle(cx, cy, r * 0.12f, p);
                break;
            case "ukraine":
                p.setColor(Color.rgb(0, 87, 183)); // Blue
                canvas.drawRect(cx - r, cy - r, cx + r, cy, p);
                p.setColor(Color.rgb(255, 215, 0)); // Yellow
                canvas.drawRect(cx - r, cy, cx + r, cy + r, p);
                break;
            case "egypt":
                p.setColor(Color.RED);
                canvas.drawRect(cx - r, cy - r, cx + r, cy - r / 3, p);
                p.setColor(Color.WHITE);
                canvas.drawRect(cx - r, cy - r / 3, cx + r, cy + r / 3, p);
                p.setColor(Color.BLACK);
                canvas.drawRect(cx - r, cy + r / 3, cx + r, cy + r, p);
                p.setColor(Color.rgb(181, 149, 0)); // Gold
                canvas.drawCircle(cx, cy, r * 0.15f, p);
                break;
            case "australia":
                // 1. Base Blue
                p.setColor(Color.rgb(1, 33, 105)); // Official Australian/Union Jack Blue
                canvas.drawCircle(cx, cy, r, p);

                // 2. Union Jack (Top Left Quadrant)
                canvas.save();
                canvas.clipRect(cx - r, cy - r, cx, cy);

                // Strokes for Union Jack
                float strokeW = r * 0.15f;
                p.setStyle(Paint.Style.STROKE);

                // White Diagonals
                p.setColor(Color.WHITE);
                p.setStrokeWidth(strokeW);
                canvas.drawLine(cx - r, cy - r, cx, cy, p);
                canvas.drawLine(cx - r, cy, cx, cy - r, p);

                // Red Diagonals (Simplified - just thin lines on top)
                p.setColor(Color.rgb(200, 16, 46));
                p.setStrokeWidth(strokeW * 0.3f);
                canvas.drawLine(cx - r, cy - r, cx, cy, p);
                canvas.drawLine(cx - r, cy, cx, cy - r, p);

                // White Cross (St George base)
                p.setColor(Color.WHITE);
                p.setStrokeWidth(strokeW * 1.5f);
                canvas.drawLine(cx - r / 2, cy - r, cx - r / 2, cy, p);
                canvas.drawLine(cx - r, cy - r / 2, cx, cy - r / 2, p);

                // Red Cross (St George top)
                p.setColor(Color.rgb(200, 16, 46));
                p.setStrokeWidth(strokeW * 0.8f);
                canvas.drawLine(cx - r / 2, cy - r, cx - r / 2, cy, p);
                canvas.drawLine(cx - r, cy - r / 2, cx, cy - r / 2, p);

                canvas.restore();
                p.setStyle(Paint.Style.FILL);

                // 3. Federation Star (Large 7-point star below Union Jack)
                p.setColor(Color.WHITE);
                drawStarIcon(canvas, cx - r * 0.25f, cy + r * 0.3f, r * 0.22f, p);

                // 4. Southern Cross (Right side)
                drawStarIcon(canvas, cx + r * 0.5f, cy + r * 0.35f, r * 0.12f, p); // Alpha
                drawStarIcon(canvas, cx + r * 0.25f, cy - r * 0.05f, r * 0.10f, p); // Beta
                drawStarIcon(canvas, cx + r * 0.5f, cy - r * 0.35f, r * 0.12f, p); // Gamma
                drawStarIcon(canvas, cx + r * 0.75f, cy - r * 0.1f, r * 0.10f, p); // Delta
                drawStarIcon(canvas, cx + r * 0.6f, cy + r * 0.1f, r * 0.07f, p); // Epsilon (smaller)

                // 5. Glossy Shine (Top Highlight)
                Paint shineP = new Paint(Paint.ANTI_ALIAS_FLAG);
                shineP.setColor(Color.WHITE);
                shineP.setAlpha(60);
                // Draw an oval gradient or simple shape for gloss
                canvas.drawOval(new RectF(cx - r * 0.7f, cy - r * 0.85f, cx + r * 0.7f, cy - r * 0.1f), shineP);
                // Extra shine spot
                shineP.setAlpha(90);
                canvas.drawCircle(cx - r * 0.3f, cy - r * 0.4f, r * 0.15f, shineP);
                break;
            case "south_africa":
                p.setColor(Color.WHITE);
                canvas.drawRect(cx - r, cy - r, cx + r, cy + r, p); // Base
                // Top Red
                p.setColor(Color.RED);
                canvas.drawRect(cx - r, cy - r, cx + r, cy - r * 0.2f, p);
                // Bottom Blue
                p.setColor(Color.BLUE);
                canvas.drawRect(cx - r, cy + r * 0.2f, cx + r, cy + r, p);
                // Green Y
                p.setColor(Color.rgb(0, 122, 77));
                canvas.drawRect(cx - r, cy - r * 0.15f, cx + r, cy + r * 0.15f, p);
                android.graphics.Path saY = new android.graphics.Path(); // Y shape triangle part
                saY.moveTo(cx - r, cy - r * 0.6f);
                saY.lineTo(cx, cy);
                saY.lineTo(cx - r, cy + r * 0.6f);
                saY.close();
                canvas.drawPath(saY, p);

                // Black Triangle at hoist
                p.setColor(Color.BLACK);
                android.graphics.Path saTri = new android.graphics.Path();
                saTri.moveTo(cx - r, cy - r * 0.4f);
                saTri.lineTo(cx - r * 0.4f, cy);
                saTri.lineTo(cx - r, cy + r * 0.4f);
                saTri.close();
                canvas.drawPath(saTri, p);
                break;
            case "saudi_arabia":
                p.setColor(Color.rgb(0, 100, 0)); // Green
                canvas.drawRect(cx - r, cy - r, cx + r, cy + r, p);
                p.setColor(Color.WHITE);
                // Sword (Horizontal)
                canvas.drawLine(cx - r * 0.6f, cy + r * 0.3f, cx + r * 0.6f, cy + r * 0.3f, p);
                p.setStrokeWidth(3);
                p.setStyle(Paint.Style.STROKE);
                // Script (Squiggle above sword)
                canvas.drawLine(cx - r * 0.4f, cy - r * 0.1f, cx + r * 0.4f, cy - r * 0.1f, p);
                canvas.drawCircle(cx, cy - r * 0.25f, 2, p);
                p.setStyle(Paint.Style.FILL);
                break;
            case "pakistan":
                p.setColor(Color.rgb(0, 64, 26)); // Dark Green
                canvas.drawRect(cx - r, cy - r, cx + r, cy + r, p);
                p.setColor(Color.WHITE);
                canvas.drawRect(cx - r, cy - r, cx - r * 0.65f, cy + r, p); // White stripe left
                // Crescent & Star (Rotated slightly)
                canvas.save();
                canvas.rotate(-45, cx, cy);
                p.setColor(Color.WHITE);
                canvas.drawCircle(cx, cy, r * 0.3f, p);
                p.setColor(Color.rgb(0, 64, 26)); // Mask
                canvas.drawCircle(cx + r * 0.1f, cy, r * 0.25f, p);
                p.setColor(Color.WHITE);
                drawStarIcon(canvas, cx + r * 0.1f, cy - r * 0.1f, r * 0.1f, p);
                canvas.restore();
                break;
            case "indonesia":
                p.setColor(Color.RED);
                canvas.drawRect(cx - r, cy - r, cx + r, cy, p);
                p.setColor(Color.WHITE);
                canvas.drawRect(cx - r, cy, cx + r, cy + r, p);
                break;

            case "brazil":
                p.setColor(Color.rgb(0, 153, 51));
                canvas.drawCircle(cx, cy, r, p);
                p.setColor(Color.YELLOW);
                android.graphics.Path diamond = new android.graphics.Path();
                diamond.moveTo(cx, cy - r * 0.8f);
                diamond.lineTo(cx + r * 0.8f, cy);
                diamond.lineTo(cx, cy + r * 0.8f);
                diamond.lineTo(cx - r * 0.8f, cy);
                diamond.close();
                canvas.drawPath(diamond, p);
                p.setColor(Color.rgb(0, 39, 118));
                canvas.drawCircle(cx, cy, r * 0.35f, p);
                break;
            default:
                p.setColor(Color.GRAY);
                canvas.drawCircle(cx, cy, r, p);
                p.setColor(Color.WHITE);
                p.setTextSize(r);
                p.setTextAlign(Paint.Align.CENTER);
                canvas.drawText("FLAG", cx, cy + r / 3, p);
                break;
        }
        canvas.restore();
    }

    private void drawPricePill(Canvas canvas, float cx, float cy) {
        float pillW = getWidth() * 0.75f;
        float pillH = getHeight() * 0.18f;
        RectF pillRect = new RectF(cx - pillW / 2, cy - pillH / 2, cx + pillW / 2, cy + pillH / 2);

        // Pill Background
        pillPaint.setStyle(Paint.Style.FILL);
        pillPaint.setColor(Color.argb(200, 0, 100, 150)); // Dark cyan
        canvas.drawRoundRect(pillRect, pillH / 2, pillH / 2, pillPaint);

        // Pill Border
        pillPaint.setStyle(Paint.Style.STROKE);
        pillPaint.setColor(Color.CYAN);
        canvas.drawRoundRect(pillRect, pillH / 2, pillH / 2, pillPaint);

        // Price Text and Icon
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTextSize(pillH * 0.55f); // Reduced from 0.7f to 0.55f
        textPaint.setColor(Color.WHITE);

        float textX = cx;
        // Removed Small Diamond/Square icon as requested by user
        /*
         * if (!priceIcon.equals("NONE")) {
         * // Draw small icon on the left
         * float iconSize = pillH * 0.4f;
         * float iconX = cx - pillW / 2 + pillH / 2 + 5;
         * drawSmallIcon(canvas, iconX, cy, iconSize, priceIcon);
         * textX += 10;
         * }
         */

        canvas.drawText(price, textX, cy + textPaint.getTextSize() / 3, textPaint);
    }

    private void drawSmallIcon(Canvas canvas, float x, float y, float size, String type) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setStyle(Paint.Style.FILL);
        if (type.equals("GEM")) {
            p.setColor(Color.rgb(0, 200, 255));
            Path path = new Path();
            path.moveTo(x, y - size);
            path.lineTo(x + size, y);
            path.lineTo(x, y + size);
            path.lineTo(x - size, y);
            path.close();
            canvas.drawPath(path, p);
        } else if (type.equals("HEART")) {
            p.setColor(Color.rgb(255, 50, 100));
            canvas.drawCircle(x, y, size, p);
        } else if (type.equals("COIN")) {
            p.setColor(Color.YELLOW);
            canvas.drawCircle(x, y, size, p);
        }
    }

    private void drawStarIcon(Canvas canvas, float cx, float cy, float r, Paint p) {
        Path path = new Path();
        for (int i = 0; i < 5; i++) {
            float angle = (float) (i * 2 * Math.PI / 5 - Math.PI / 2);
            float x = cx + (float) Math.cos(angle) * r;
            float y = cy + (float) Math.sin(angle) * r;
            if (i == 0)
                path.moveTo(x, y);
            else
                path.lineTo(x, y);

            angle += (float) (Math.PI / 5);
            x = cx + (float) Math.cos(angle) * (r * 0.4f);
            y = cy + (float) Math.sin(angle) * (r * 0.4f);
            path.lineTo(x, y);
        }
        path.close();
        canvas.drawPath(path, p);
    }

    public void setItemName(String name) {
        this.itemName = name;
        invalidate();
    }

    public void setPrice(String price) {
        this.price = price;
        invalidate();
    }

    public void setThemeColor(int color) {
        this.themeColor = color;
        invalidate();
    }

    public void setSkinId(String skinId) {
        this.skinId = skinId;
    }

    public String getSkinId() {
        return skinId;
    }

    public String getItemName() {
        return itemName;
    }
}

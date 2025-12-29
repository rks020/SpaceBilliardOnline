package com.spacebilliard.app.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.Gravity;
import android.widget.FrameLayout;

public class NeonMainMenuPanel extends FrameLayout {

    private Paint bgPaint;
    private Paint borderPaint;
    private Paint textPaint;
    private Path framePath;

    // Buttons
    public NeonButton btnStart;
    public NeonButton btnPlayOnline;
    public NeonButton btnHowTo;
    public NeonButton btnShop;
    public NeonButton btnHallOfFame;

    public NeonMainMenuPanel(Context context) {
        super(context);
        init(context);
    }

    public NeonMainMenuPanel(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        setWillNotDraw(false);
        setLayerType(LAYER_TYPE_SOFTWARE, null);

        bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bgPaint.setStyle(Paint.Style.FILL);
        bgPaint.setColor(Color.argb(220, 20, 20, 40));

        borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(6f);
        borderPaint.setColor(Color.CYAN);
        borderPaint.setShadowLayer(25, 0, 0, Color.CYAN);

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        textPaint.setColor(Color.MAGENTA);
        textPaint.setShadowLayer(30, 0, 0, Color.MAGENTA);

        // Add Buttons
        int buttonWidth = (int) (180 * getResources().getDisplayMetrics().density);
        int buttonHeight = (int) (45 * getResources().getDisplayMetrics().density);

        btnStart = new NeonButton(context, "START GAME", Color.CYAN);
        addView(btnStart, createParams(buttonWidth, buttonHeight));

        btnPlayOnline = new NeonButton(context, "PLAY ONLINE", Color.GREEN);
        addView(btnPlayOnline, createParams(buttonWidth, buttonHeight));

        btnHowTo = new NeonButton(context, "HOW TO PLAY", Color.MAGENTA);
        addView(btnHowTo, createParams(buttonWidth, buttonHeight));

        btnHallOfFame = new NeonButton(context, "HALL OF FAME", Color.rgb(255, 215, 0));
        addView(btnHallOfFame, createParams(buttonWidth, buttonHeight));

        btnShop = new NeonButton(context, "SHOP", Color.rgb(255, 60, 120));
        addView(btnShop, createParams(buttonWidth, buttonHeight));
    }

    private LayoutParams createParams(int w, int h) {
        LayoutParams params = new LayoutParams(w, h);
        params.gravity = Gravity.CENTER_HORIZONTAL | Gravity.TOP;
        return params;
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        int w = getWidth();
        int h = getHeight();

        float menuHeight = h * 0.55f;
        float menuTop = (h - menuHeight) / 2 + h * 0.03f;
        float headerCurveHeight = 60f;

        float startY = menuTop + headerCurveHeight + h * 0.12f; // Slightly higher than Game Over
        float space = h * 0.08f;

        btnStart.setY(startY);
        btnPlayOnline.setY(startY + space);
        btnHowTo.setY(startY + space * 2);
        btnHallOfFame.setY(startY + space * 3);
        btnShop.setY(startY + space * 4);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // No full screen dark overlay for Main Menu, just the frame

        int w = getWidth();
        int h = getHeight();
        float centerX = w / 2f;

        float menuWidth = w * 0.75f;
        float menuHeight = h * 0.55f;
        float menuTop = (h - menuHeight) / 2 + h * 0.03f;
        float menuBottom = menuTop + menuHeight;

        if (framePath == null)
            framePath = new Path();
        framePath.reset();

        float cornerRadius = 40f;
        float headerCurveHeight = 60f;

        framePath.moveTo(centerX - menuWidth / 2, menuTop + headerCurveHeight);
        framePath.quadTo(centerX, menuTop - headerCurveHeight * 0.2f, centerX + menuWidth / 2,
                menuTop + headerCurveHeight);
        framePath.lineTo(centerX + menuWidth / 2, menuBottom - cornerRadius);
        framePath.quadTo(centerX + menuWidth / 2, menuBottom, centerX + menuWidth / 2 - cornerRadius, menuBottom);
        framePath.lineTo(centerX - menuWidth / 2 + cornerRadius, menuBottom);
        framePath.quadTo(centerX - menuWidth / 2, menuBottom, centerX - menuWidth / 2, menuBottom - cornerRadius);
        framePath.close();

        canvas.drawPath(framePath, bgPaint);
        canvas.drawPath(framePath, borderPaint);

        // Header
        textPaint.setTextSize(w * 0.12f);
        canvas.drawText("MENU", centerX, menuTop + headerCurveHeight + 50, textPaint);
    }
}

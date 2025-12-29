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

public class NeonGameOverPanel extends FrameLayout {

    private Paint bgPaint;
    private Paint borderPaint;
    private Paint textPaint;
    private Path framePath;

    // Buttons
    public NeonButton btnRevive;
    public NeonButton btnReboot;
    public NeonButton btnHallOfFame;
    public NeonButton btnMainMenu;

    public NeonGameOverPanel(Context context) {
        super(context);
        init(context);
    }

    public NeonGameOverPanel(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        setWillNotDraw(false); // Enable onDraw
        setLayerType(LAYER_TYPE_SOFTWARE, null);

        bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bgPaint.setStyle(Paint.Style.FILL);
        bgPaint.setColor(Color.argb(220, 20, 20, 40));

        borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(6f);
        borderPaint.setColor(Color.RED);
        borderPaint.setShadowLayer(25, 0, 0, Color.RED);

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        textPaint.setColor(Color.RED);
        textPaint.setShadowLayer(30, 0, 0, Color.RED);

        // Add Buttons
        int buttonWidth = (int) (180 * getResources().getDisplayMetrics().density);
        int buttonHeight = (int) (45 * getResources().getDisplayMetrics().density);
        int margin = (int) (15 * getResources().getDisplayMetrics().density);

        // Buttons will be positioned in onLayout or simple gravity with top margins
        // But since we have a custom background shape, standard gravity might not align
        // perfectly with the "visual" center.
        // We can use a vertical LinearLayout inside, centered.

        // Actually, let's use LayoutParams on components directly for now.

        btnRevive = new NeonButton(context, "REVIVE (WATCH AD)", Color.GREEN);
        addView(btnRevive, createParams(buttonWidth, buttonHeight));

        btnReboot = new NeonButton(context, "REBOOT LEVEL", Color.CYAN);
        addView(btnReboot, createParams(buttonWidth, buttonHeight));

        btnHallOfFame = new NeonButton(context, "HALL OF FAME", Color.rgb(255, 215, 0));
        addView(btnHallOfFame, createParams(buttonWidth, buttonHeight));

        btnMainMenu = new NeonButton(context, "MAIN MENU", Color.rgb(255, 100, 100));
        addView(btnMainMenu, createParams(buttonWidth, buttonHeight));
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

        // Custom positioning to match the painted frame
        float menuHeight = h * 0.55f;
        float menuTop = (h - menuHeight) / 2 + h * 0.03f;
        float headerCurveHeight = 60f;

        float startY = menuTop + headerCurveHeight + h * 0.15f;
        float space = h * 0.09f;

        btnRevive.setY(startY);
        btnReboot.setY(startY + space);
        btnHallOfFame.setY(startY + space * 2);
        btnMainMenu.setY(startY + space * 3);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // Draw Dark Overlay
        canvas.drawColor(Color.argb(200, 0, 0, 0));

        int w = getWidth();
        int h = getHeight();
        float centerX = w / 2f;

        float menuWidth = w * 0.75f;
        float menuHeight = h * 0.55f;
        float menuTop = (h - menuHeight) / 2 + h * 0.03f;
        float menuBottom = menuTop + menuHeight;

        // Draw Frame
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
        canvas.drawText("GAME OVER", centerX, menuTop + headerCurveHeight + 50, textPaint);
    }
}

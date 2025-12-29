package com.spacebilliard.app.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.Gravity;
import android.widget.FrameLayout;

public class NeonMainMenuPanel extends FrameLayout {

    private Paint textPaint;
    private Paint labelPaint;
    private Paint decorationPaint;

    // Stats to display
    private int score = 0;
    private int credits = 0;

    // Buttons
    public NeonButton btnStart;
    public NeonButton btnPlayOnline;
    public NeonButton btnHowTo;
    public NeonButton btnHallOfFame; // Hall of Fame
    public NeonButton btnShop;
    public NeonButton btnSettings;

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

        // Header Text Paint
        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD_ITALIC));
        textPaint.setColor(Color.MAGENTA);
        textPaint.setShadowLayer(40, 0, 0, Color.MAGENTA); // Strong glow

        // Label Paint (Score/Credits)
        labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        labelPaint.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
        labelPaint.setColor(Color.LTGRAY);
        labelPaint.setTextSize(30);

        // Decoration Paint (Corners)
        decorationPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        decorationPaint.setStyle(Paint.Style.STROKE);
        decorationPaint.setStrokeWidth(4f);
        decorationPaint.setColor(Color.DKGRAY);

        // Add Buttons
        // Making buttons wider and slightly taller
        int buttonWidth = (int) (280 * getResources().getDisplayMetrics().density);
        int buttonHeight = (int) (55 * getResources().getDisplayMetrics().density);

        // 1. START GAME (Cyan/Blue)
        btnStart = new NeonButton(context, "START GAME", Color.parseColor("#00E5FF")); // Cyan
        addView(btnStart, createParams(buttonWidth, buttonHeight));

        // 2. PLAY ONLINE (Green)
        btnPlayOnline = new NeonButton(context, "PLAY ONLINE", Color.parseColor("#00C853")); // Green
        addView(btnPlayOnline, createParams(buttonWidth, buttonHeight));

        // 3. HOW TO PLAY (Purple)
        btnHowTo = new NeonButton(context, "HOW TO PLAY", Color.parseColor("#AA00FF")); // Purple
        addView(btnHowTo, createParams(buttonWidth, buttonHeight));

        // 4. HALL OF FAME (Orange/Golden)
        btnHallOfFame = new NeonButton(context, "HALL OF FAME", Color.parseColor("#FFAB00")); // Amber
        addView(btnHallOfFame, createParams(buttonWidth, buttonHeight));

        // 5. SHOP (Red/Pink)
        btnShop = new NeonButton(context, "SHOP", Color.parseColor("#FF1744")); // Red/Pink
        addView(btnShop, createParams(buttonWidth, buttonHeight));

        // 6. SETTINGS (Deep Orange)
        btnSettings = new NeonButton(context, "SETTINGS", Color.parseColor("#FF6D00")); // Deep Orange
        addView(btnSettings, createParams(buttonWidth, buttonHeight));

        refreshStats();
    }

    public void refreshStats() {
        android.content.SharedPreferences prefs = getContext().getSharedPreferences("SpaceBilliard",
                Context.MODE_PRIVATE);
        this.score = prefs.getInt("highScore", 0); // Showing High Score as 'Score'
        this.credits = prefs.getInt("coins", 0);
        invalidate();
    }

    private LayoutParams createParams(int w, int h) {
        LayoutParams params = new LayoutParams(w, h);
        params.gravity = Gravity.CENTER_HORIZONTAL | Gravity.TOP;
        return params;
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        int h = getHeight();
        float density = getResources().getDisplayMetrics().density;

        // Position buttons vertically centered, but slightly pushed down to make room
        // for header
        float startY = h * 0.25f; // Start at 25% height
        float gap = 65 * density; // Vertical spacing

        btnStart.setY(startY);
        btnPlayOnline.setY(startY + gap);
        btnHowTo.setY(startY + gap * 2);
        btnHallOfFame.setY(startY + gap * 3);
        btnShop.setY(startY + gap * 4);
        btnSettings.setY(startY + gap * 5);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int w = getWidth();
        int h = getHeight();
        float centerX = w / 2f;
        float density = getResources().getDisplayMetrics().density;

        // 1. MENU Header
        textPaint.setTextSize(60 * density);
        canvas.drawText("MENU", centerX, h * 0.15f, textPaint);

        // 2. Stats Row
        float statsY = h * 0.20f;
        float padding = 40 * density;

        // Left: SCORE
        labelPaint.setTextAlign(Paint.Align.LEFT);
        labelPaint.setColor(Color.GRAY);
        labelPaint.setTextSize(12 * density);
        canvas.drawText("SCORE", padding, statsY, labelPaint);

        labelPaint.setColor(Color.WHITE);
        labelPaint.setTextSize(20 * density);
        canvas.drawText(String.valueOf(score), padding, statsY + 25 * density, labelPaint);

        // Right: CREDITS
        labelPaint.setTextAlign(Paint.Align.RIGHT);
        labelPaint.setColor(Color.GRAY);
        labelPaint.setTextSize(12 * density);
        canvas.drawText("CREDITS", w - padding, statsY, labelPaint);

        labelPaint.setColor(Color.parseColor("#FFD700")); // Gold for credits
        labelPaint.setTextSize(20 * density);
        canvas.drawText("● " + credits, w - padding, statsY + 25 * density, labelPaint);

        // 3. Footer "SYSTEM READY"
        labelPaint.setTextAlign(Paint.Align.CENTER);
        labelPaint.setColor(Color.DKGRAY);
        labelPaint.setTextSize(14 * density);
        labelPaint.setLetterSpacing(0.5f);
        canvas.drawText("SYSTEM READY", centerX, h - 40 * density, labelPaint);

        // 4. Decoration Corners
        float cornerSize = 40 * density;
        float margin = 20 * density;

        // Top-Left
        canvas.drawLine(margin, margin + cornerSize, margin, margin, decorationPaint);
        canvas.drawLine(margin, margin, margin + cornerSize, margin, decorationPaint);

        // Bottom-Right
        canvas.drawLine(w - margin, h - margin - cornerSize, w - margin, h - margin, decorationPaint);
        canvas.drawLine(w - margin, h - margin, w - margin - cornerSize, h - margin, decorationPaint);
    }
}

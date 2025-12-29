package com.spacebilliard.app.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.view.View;

public class OnlineScorePanel extends View {
    private Paint paint;
    private Paint textPaint;
    private Paint glowPaint;

    private String player1Name = "Player 1";
    private String player2Name = "Player 2";
    private int player1Score = 0;
    private int player2Score = 0;
    private int player1BallsDestroyed = 0;
    private int player2BallsDestroyed = 0;
    private int currentSet = 1;
    private int totalSets = 3;
    private long timeLeft = 0; // milliseconds

    // Win/Loss statistics
    private int totalWins = 0;
    private int totalLosses = 0;

    private boolean isHost = true;

    public OnlineScorePanel(Context context) {
        super(context);
        init();
    }

    private void init() {
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(42);
        textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        textPaint.setTextAlign(Paint.Align.CENTER);

        glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        glowPaint.setStyle(Paint.Style.STROKE);
        glowPaint.setStrokeWidth(3);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();

        // Background with gradient
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.argb(200, 10, 0, 24));
        canvas.drawRoundRect(20, 20, width - 20, height - 20, 20, 20, paint);

        // Border glow
        glowPaint.setColor(Color.CYAN);
        canvas.drawRoundRect(20, 20, width - 20, height - 20, 20, 20, glowPaint);

        float centerY = height / 2f;

        // Player 1 (Left - Host/Cyan)
        textPaint.setTextAlign(Paint.Align.LEFT);
        textPaint.setColor(Color.CYAN);
        textPaint.setTextSize(48);
        // Show SET SCORE next to name (e.g., Atkafasi: 1)
        canvas.drawText(player1Name + ": " + player1Score, 50, 70, textPaint);

        // Player 1 Balls Destroyed (Bottom)
        textPaint.setTextSize(40);
        textPaint.setColor(Color.argb(255, 0, 255, 255));
        canvas.drawText("Balls: " + player1BallsDestroyed, 50, height - 70, textPaint);

        // SET indicator (Center)
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setColor(Color.MAGENTA);
        textPaint.setTextSize(56);
        canvas.drawText("SET " + currentSet + "/" + totalSets, width / 2f, centerY - 20, textPaint);

        // Timer (Below SET)
        textPaint.setTextSize(60);
        textPaint.setColor(Color.YELLOW);
        String timeStr = formatTime(timeLeft);
        canvas.drawText(timeStr, width / 2f, centerY + 60, textPaint);

        // Player 2 (Right - Guest/Green)
        textPaint.setTextAlign(Paint.Align.RIGHT);
        textPaint.setColor(Color.GREEN);
        textPaint.setTextSize(48);
        canvas.drawText(player2Name + ": " + player2Score, width - 50, 70, textPaint);

        // Player 2 Balls Destroyed (Bottom)
        textPaint.setTextSize(40);
        textPaint.setColor(Color.argb(255, 0, 255, 0));
        canvas.drawText("Balls: " + player2BallsDestroyed, width - 50, height - 70, textPaint);
    }

    private String formatTime(long millis) {
        int seconds = (int) (millis / 1000);
        int minutes = seconds / 60;
        seconds = seconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    public void setPlayerNames(String player1, String player2) {
        this.player1Name = player1;
        this.player2Name = player2;
        invalidate();
    }

    public void setScores(int player1, int player2) {
        this.player1Score = player1;
        this.player2Score = player2;
        invalidate();
    }

    public void setBallsDestroyed(int player1Balls, int player2Balls) {
        this.player1BallsDestroyed = player1Balls;
        this.player2BallsDestroyed = player2Balls;
        invalidate();
    }

    public void setCurrentSet(int set, int total) {
        this.currentSet = set;
        this.totalSets = total;
        invalidate();
    }

    public void setTimeLeft(long millis) {
        this.timeLeft = millis;
        invalidate();
    }

    public void setIsHost(boolean isHost) {
        this.isHost = isHost;
        invalidate();
    }

    public void setWinLossStats(int wins, int losses) {
        this.totalWins = wins;
        this.totalLosses = losses;
        invalidate();
    }
}

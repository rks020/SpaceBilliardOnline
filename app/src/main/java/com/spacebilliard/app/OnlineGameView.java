package com.spacebilliard.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import com.spacebilliard.app.network.OnlineGameManager;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Random;

public class OnlineGameView extends SurfaceView implements Runnable {

    private Thread gameThread;
    private SurfaceHolder holder;
    private boolean isPlaying;
    private Paint paint;
    private Random random;

    private int screenWidth, screenHeight;
    private float centerX, centerY, circleRadius;

    // Game objects
    private Ball hostBall, guestBall;
    private ArrayList<Ball> coloredBalls = new ArrayList<>();
    private ArrayList<Particle> particles = new ArrayList<>();
    // Object Pool for Balls to reduce GC pressure
    private ArrayList<Ball> ballPool = new ArrayList<>();

    // Online
    private OnlineGameManager gameManager;
    private boolean isHost;
    private String hostName = "Player 1";
    private String guestName = "Player 2";

    // UI state
    private long timeLeft = 0;
    private String setFinishedMessage = "";
    private int setFinishedAlpha = 0;
    private int countdownValue = 0;
    private String winnerMessage = "";
    private int winnerAlpha = 0;

    // Drag
    private boolean isDragging = false;
    private float dragStartX, dragStartY;
    private float currentTouchX, currentTouchY;
    private Ball myBall;
    private long lastShotTime = 0; // Cooldown tracker

    public OnlineGameView(Context context) {
        super(context);
        holder = getHolder();
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        random = new Random();
    }

    public void setOnlineMode(OnlineGameManager manager, boolean isHost) {
        this.gameManager = manager;
        this.isHost = isHost;
    }

    public void setPlayerNames(String hostName, String guestName) {
        this.hostName = hostName;
        this.guestName = guestName;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        screenWidth = w;
        screenHeight = h;
        centerX = w / 2f;
        centerY = h / 2f;
        int minSize = Math.min(w, h);
        circleRadius = minSize * 0.47f;

        // Update existing ball radii
        if (hostBall != null)
            hostBall.radius = circleRadius * 0.05f;
        if (guestBall != null)
            guestBall.radius = circleRadius * 0.05f;
    }

    public void updateOnlineState(String hostJson, String guestJson, String ballsJson, long timeLeft) {
        this.timeLeft = timeLeft;
        try {
            // Safe radius calculation
            float ballRadius = (circleRadius > 0) ? circleRadius * 0.05f : 30f;

            if (hostJson != null && !hostJson.equals("null")) {
                JSONObject h = new JSONObject(hostJson);
                float x = (float) h.optDouble("x", 0.5) * screenWidth;
                float y = (float) h.optDouble("y", 0.5) * screenHeight;
                int color = Color.parseColor(h.optString("color", "#888888"));
                if (hostBall == null)
                    hostBall = new Ball(x, y, ballRadius, color);

                // Direct position update (no interpolation)
                if (!(isDragging && isHost)) {
                    hostBall.x = x;
                    hostBall.y = y;
                }
                hostBall.color = color;
                hostBall.radius = ballRadius;
            }

            if (guestJson != null && !guestJson.equals("null")) {
                JSONObject g = new JSONObject(guestJson);
                float x = (float) g.optDouble("x", 0.5) * screenWidth;
                float y = (float) g.optDouble("y", 0.5) * screenHeight;
                int color = Color.parseColor(g.optString("color", "#FFFFFF"));
                if (guestBall == null)
                    guestBall = new Ball(x, y, ballRadius, color);

                // Direct position update (no interpolation)
                if (!(isDragging && !isHost)) {
                    guestBall.x = x;
                    guestBall.y = y;
                }
                guestBall.color = color;
                guestBall.radius = ballRadius;
            }

            if (ballsJson != null && !ballsJson.equals("null")) {
                JSONArray arr = new JSONArray(ballsJson);
                synchronized (coloredBalls) {
                    int newCount = arr.length();

                    // 1. Add needed balls from pool or create new ones
                    while (coloredBalls.size() < newCount) {
                        if (!ballPool.isEmpty()) {
                            coloredBalls.add(ballPool.remove(ballPool.size() - 1));
                        } else {
                            coloredBalls.add(new Ball(0, 0, 0, 0));
                        }
                    }

                    // 2. Remove excess balls to pool
                    while (coloredBalls.size() > newCount) {
                        Ball removed = coloredBalls.remove(coloredBalls.size() - 1);
                        ballPool.add(removed);
                    }

                    // 3. Update properties of active balls (direct updates, no interpolation)
                    for (int i = 0; i < newCount; i++) {
                        JSONObject b = arr.getJSONObject(i);
                        float x = (float) b.optDouble("x", 0.5) * screenWidth;
                        float y = (float) b.optDouble("y", 0.5) * screenHeight;
                        int color = Color.parseColor(b.optString("color", "#FF0055"));

                        Ball ball = coloredBalls.get(i);
                        ball.x = x;
                        ball.y = y;
                        ball.color = color;
                        ball.radius = circleRadius * 0.055f;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void createExplosion(float x, float y, int color) {
        // Create 30 particles for explosion (synchronized)
        synchronized (particles) {
            for (int i = 0; i < 30; i++) {
                float angle = random.nextFloat() * (float) (2 * Math.PI);
                float speed = random.nextFloat() * 10 + 5;
                particles.add(new Particle(x, y, angle, speed, color));
            }
        }
    }

    public void showSetFinished(String msg) {
        this.setFinishedMessage = msg;
        this.setFinishedAlpha = 255;
        postInvalidate();
    }

    public void showCountdown(int val) {
        this.countdownValue = val;
        postInvalidate();
    }

    public void hideCountdown() {
        this.countdownValue = 0;
        postInvalidate();
    }

    public void showWinner(String winnerText) {
        this.winnerMessage = winnerText;
        this.winnerAlpha = 255;
        postInvalidate();
    }

    public void clearAllOverlays() {
        this.winnerMessage = "";
        this.winnerAlpha = 0;
        this.setFinishedMessage = "";
        this.setFinishedAlpha = 0;
        this.countdownValue = 0;
        postInvalidate();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float touchX = event.getX();
        float touchY = event.getY();

        // Check cooldown (0.75s wait)
        if (System.currentTimeMillis() - lastShotTime < 750) {
            return true;
        }

        myBall = isHost ? hostBall : guestBall;
        if (myBall == null)
            return true;

        // Calculate screen zones outside switch
        float screenHeight = getHeight();
        float bottomZoneStart = screenHeight * 0.6f; // 60% down = bottom 40% zone

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                // NEW: Allow grabbing ball from anywhere in bottom 40% of screen
                // Check if touch is in bottom zone (red area)
                if (touchY >= bottomZoneStart) {
                    isDragging = true;
                    dragStartX = touchX;
                    dragStartY = touchY;
                    currentTouchX = touchX;
                    currentTouchY = touchY;

                    // Stop the ball on the server AND freeze it at current visual position
                    if (gameManager != null && myBall != null) {
                        float normX = myBall.x / screenWidth;
                        float normY = myBall.y / screenHeight;
                        gameManager.sendStopBall(normX, normY);
                    }
                }
                // ALSO Check if touching the ball directly (Classic control)
                else {
                    float dx = touchX - myBall.x;
                    float dy = touchY - myBall.y;
                    float distance = (float) Math.sqrt(dx * dx + dy * dy);

                    if (distance < myBall.radius * 3) {
                        isDragging = true;
                        dragStartX = touchX;
                        dragStartY = touchY;
                        currentTouchX = touchX;
                        currentTouchY = touchY;

                        if (gameManager != null) {
                            float normX = myBall.x / screenWidth;
                            float normY = myBall.y / screenHeight;
                            gameManager.sendStopBall(normX, normY);
                        }
                    }
                }
                break;

            case MotionEvent.ACTION_MOVE:
                if (isDragging && myBall != null) {
                    // Just update touch position for arrow display
                    // Ball stays in place!
                    currentTouchX = touchX;
                    currentTouchY = touchY;
                }
                break;

            case MotionEvent.ACTION_UP:
                if (isDragging) {
                    // Calculate throw direction (from current touch to drag start)
                    float dragDx = currentTouchX - dragStartX;
                    float dragDy = currentTouchY - dragStartY;
                    float angle = (float) Math.atan2(-dragDy, -dragDx); // Opposite direction
                    float power = Math.min((float) Math.sqrt(dragDx * dragDx + dragDy * dragDy), 200f);

                    if (power > 10 && gameManager != null) {
                        float normX = myBall.x / screenWidth;
                        float normY = myBall.y / screenHeight;
                        gameManager.sendShot(angle, power, normX, normY);

                        // Set cooldown
                        lastShotTime = System.currentTimeMillis();

                        // Create drag trail particles (synchronized)
                        synchronized (particles) {
                            for (int i = 0; i < 10; i++) {
                                particles.add(
                                        new Particle(myBall.x, myBall.y, angle + (random.nextFloat() - 0.5f) * 0.5f,
                                                random.nextFloat() * 5, Color.CYAN));
                            }
                        }
                    }
                    isDragging = false;
                }
                break;
        }
        return true;
    }

    @Override
    public void run() {
        while (isPlaying) {
            long startTime = System.currentTimeMillis();
            update();
            draw();

            long frameTime = System.currentTimeMillis() - startTime;
            if (frameTime < 16) {
                try {
                    Thread.sleep(16 - frameTime);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void update() {
        // Update particles (synchronized)
        synchronized (particles) {
            for (int i = particles.size() - 1; i >= 0; i--) {
                Particle p = particles.get(i);
                p.update();
                if (p.isDead())
                    particles.remove(i);
            }
        }
    }

    private void draw() {
        if (!holder.getSurface().isValid())
            return;
        Canvas canvas = holder.lockCanvas();

        // Background
        canvas.drawColor(Color.rgb(5, 5, 16));

        // Circle boundary
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(6);
        paint.setColor(Color.CYAN);
        paint.setShadowLayer(20, 0, 0, Color.CYAN);
        canvas.drawCircle(centerX, centerY, circleRadius, paint);
        paint.clearShadowLayer();

        // Colored balls
        synchronized (coloredBalls) {
            for (Ball ball : coloredBalls) {
                drawBall(canvas, ball);
            }
        }

        // Player balls with names
        if (hostBall != null) {
            drawBall(canvas, hostBall);
            drawPlayerName(canvas, hostBall, hostName, Color.CYAN);
        }
        if (guestBall != null) {
            drawBall(canvas, guestBall);
            drawPlayerName(canvas, guestBall, guestName, Color.GREEN);
        }

        // Particles (synchronized to prevent ConcurrentModificationException)
        synchronized (particles) {
            for (Particle p : particles) {
                p.draw(canvas, paint);
            }
        }

        // Draw Timer
        if (timeLeft > 0) {
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.WHITE);
            paint.setTextSize(60);
            paint.setTextAlign(Paint.Align.CENTER);
            // Time display removed (shown in score panel instead)
            // canvas.drawText(String.format("Time: %d", timeLeft / 1000), centerX, centerY
            // + circleRadius + 120, paint);
        }

        // Draw Set Finished Overlay
        if (setFinishedAlpha > 0) {
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.YELLOW);
            paint.setTextSize(100); // Bigger text
            paint.setAlpha(setFinishedAlpha);
            paint.setTextAlign(Paint.Align.CENTER);

            // Use precise center
            float cx = getWidth() / 2f;
            float cy = getHeight() / 2f;

            // Text shadow for better visibility
            paint.setShadowLayer(20, 0, 0, Color.RED);
            canvas.drawText(setFinishedMessage, cx, cy, paint);
            paint.clearShadowLayer();

            setFinishedAlpha -= 1; // Slower fade out (was 3)
            paint.setAlpha(255); // Reset
        }

        // Draw Countdown
        if (countdownValue > 0) {
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.WHITE);
            paint.setTextSize(250);
            paint.setTextAlign(Paint.Align.CENTER);
            float cx = getWidth() / 2f;
            float cy = getHeight() / 2f;
            paint.setShadowLayer(40, 0, 0, Color.CYAN);
            canvas.drawText(String.valueOf(countdownValue), cx, cy + 80, paint); // Slightly offset Y to center text
                                                                                 // vertically
            paint.clearShadowLayer();
        }

        // Draw Winner Overlay
        if (winnerAlpha > 0) {
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.rgb(255, 215, 0)); // Gold color
            paint.setTextSize(90);
            paint.setAlpha(winnerAlpha);
            paint.setTextAlign(Paint.Align.CENTER);

            float cx = getWidth() / 2f;
            float cy = getHeight() / 2f;

            // Text shadow for prominence
            paint.setShadowLayer(30, 0, 0, Color.RED);
            canvas.drawText(winnerMessage, cx, cy, paint);
            paint.clearShadowLayer();

            paint.setAlpha(255); // Reset
        }

        // Drag sight (like GameView)
        if (isDragging && myBall != null) {
            float dx = dragStartX - currentTouchX;
            float dy = dragStartY - currentTouchY;
            float distance = (float) Math.sqrt(dx * dx + dy * dy);

            if (distance > 10) {
                // Normalize direction
                float dirX = dx / distance;
                float dirY = dy / distance;

                // Draw arrow line
                float arrowLength = Math.min(distance * 1.5f, 300f);
                float endX = myBall.x + dirX * arrowLength;
                float endY = myBall.y + dirY * arrowLength;

                // Main line
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(6);
                paint.setColor(Color.CYAN);
                paint.setShadowLayer(15, 0, 0, Color.CYAN);
                canvas.drawLine(myBall.x, myBall.y, endX, endY, paint);

                // Arrow head
                float arrowSize = 30;
                float angle = (float) Math.atan2(dy, dx);
                float angle1 = angle - (float) Math.PI / 6;
                float angle2 = angle + (float) Math.PI / 6;

                canvas.drawLine(endX, endY,
                        endX - (float) Math.cos(angle1) * arrowSize,
                        endY - (float) Math.sin(angle1) * arrowSize, paint);
                canvas.drawLine(endX, endY,
                        endX - (float) Math.cos(angle2) * arrowSize,
                        endY - (float) Math.sin(angle2) * arrowSize, paint);

                paint.clearShadowLayer();
            }
        }

        holder.unlockCanvasAndPost(canvas);
    }

    private void drawBall(Canvas canvas, Ball ball) {
        // Ensure radius is valid to prevent RadialGradient crash
        if (ball == null || ball.radius <= 0 || circleRadius <= 0) {
            return; // Skip invalid balls
        }

        paint.setStyle(Paint.Style.FILL);

        // GPU-accelerated gradient instead of setShadowLayer
        RadialGradient gradient = new RadialGradient(
                ball.x - ball.radius / 3, ball.y - ball.radius / 3, ball.radius,
                Color.WHITE, ball.color, Shader.TileMode.CLAMP);
        paint.setShader(gradient);
        canvas.drawCircle(ball.x, ball.y, ball.radius, paint);
        paint.setShader(null);
    }

    private void drawPlayerName(Canvas canvas, Ball ball, String name, int color) {
        if (ball == null || name == null)
            return;

        paint.setStyle(Paint.Style.FILL);
        paint.setTextSize(28);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setColor(color);
        paint.setShadowLayer(8, 0, 0, color);
        canvas.drawText(name, ball.x, ball.y - ball.radius - 15, paint);
        paint.clearShadowLayer();
    }

    public void pause() {
        isPlaying = false;
        try {
            if (gameThread != null)
                gameThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void resume() {
        isPlaying = true;
        gameThread = new Thread(this);
        gameThread.start();
    }

    class Ball {
        float x, y, radius;
        float targetX, targetY; // Target positions for smooth interpolation
        int color;

        Ball(float x, float y, float radius, int color) {
            this.x = x;
            this.y = y;
            this.targetX = x;
            this.targetY = y;
            this.radius = radius;
            this.color = color;
        }
    }

    class Particle {
        float x, y, vx, vy;
        int color, life = 30, maxLife = 30;

        Particle(float x, float y, float angle, float speed, int color) {
            this.x = x;
            this.y = y;
            this.vx = (float) Math.cos(angle) * speed;
            this.vy = (float) Math.sin(angle) * speed;
            this.color = color;
        }

        void update() {
            x += vx;
            y += vy;
            vx *= 0.95f;
            vy *= 0.95f;
            life--;
        }

        boolean isDead() {
            return life <= 0;
        }

        void draw(Canvas canvas, Paint paint) {
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(color);
            float alpha = (float) life / maxLife;
            paint.setAlpha((int) (255 * alpha));
            // Removed setShadowLayer for GPU acceleration
            canvas.drawCircle(x, y, 6 * alpha, paint);
            paint.setAlpha(255);
        }
    }
}

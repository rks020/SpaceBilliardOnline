package com.example.neoncircleball;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import java.util.ArrayList;
import java.util.Iterator;
import android.media.AudioAttributes;
import android.media.SoundPool;
import java.util.Random;
import android.graphics.Typeface;
import android.content.SharedPreferences;

public class GameView extends SurfaceView implements Runnable {

    private Thread gameThread;
    private SurfaceHolder holder;
    private boolean isPlaying;
    private Canvas canvas;
    private Paint paint;
    private Random random;

    // Ekran boyutları
    private int screenWidth;
    private int screenHeight;
    private float centerX;
    private float centerY;
    private float circleRadius;

    // Oyun nesneleri
    private Ball whiteBall;
    private ArrayList<Ball> cloneBalls;
    private ArrayList<Ball> coloredBalls;
    private ArrayList<Ball> blackBalls;
    private ArrayList<SpecialBall> specialBalls;
    private ArrayList<Particle> particles;
    private ArrayList<GuidedMissile> missiles;
    private ArrayList<ElectricEffect> electricEffects;

    // Oyun durumu
    private boolean gameStarted = false;
    private boolean gameOver = false;
    private int score = 0;
    private int lives = 3;
    private int level = 1;
    private int stage = 1; // Mevcut stage (1-10)
    private long timeLeft = 20000;
    private long lastTime;
    private int comboCounter = 0;

    // Özel yetenekler
    private boolean blackHoleActive = false;
    private long blackHoleEndTime = 0;
    private boolean barrierActive = false;
    private long barrierEndTime = 0;
    private boolean freezeActive = false;
    private long freezeEndTime = 0;
    private boolean ghostModeActive = false;
    private long ghostModeEndTime = 0;
    private float originalWhiteBallRadius = 0;
    private boolean powerBoostActive = false;
    private BlastWave blastWave = null;

    // UI durumu
    private boolean showInstructions = false;
    private boolean showHighScore = false;
    private int highScore = 0;
    private int highLevel = 1;

    // Combo sistemi
    private int comboHits = 0;
    private long lastHitTime = 0;
    private final long COMBO_TIMEOUT = 2000; // 2 saniye
    private String comboText = "";
    private long comboTextEndTime = 0;
    private int maxCombo = 0; // Combo rekoru

    // Stage Cleared animasyonu
    private boolean showStageCleared = false;
    private long stageClearedTime = 0;

    // Level Seçici
    private boolean showLevelSelector = false;
    private int selectorPage = 1;
    private int maxUnlockedLevel = 1;

    // Yıldızlar
    private ArrayList<Star> stars;

    // Kamera sallanma
    private float cameraShakeX = 0;
    private float cameraShakeY = 0;
    private long shakeEndTime = 0;
    private Random shakeRandom;
    private long immuneEndTime = 0; // Dokunulmazlık süresi
    private long lastShieldSoundTime = 0; // Kalkan sesi zamanlayıcısı

    // Son fırlatma gücü
    private float lastLaunchPower = 0;

    // Sürükleme
    private boolean isDragging = false;
    private Ball draggedBall = null;
    private float dragStartX, dragStartY;
    private long dragStartTime;
    private final float MAX_DRAG_DISTANCE = 200;
    private final long MAX_DRAG_TIME = 3000;

    // Level geçiş beklemesi
    private boolean levelCompleted = false;

    private long levelCompletionTime = 0;

    // Ses Efektleri
    private SoundPool soundPool;
    private int soundLaunch, soundCollision, soundCoin, soundBlackExplosion, soundElectric, soundFreeze, soundGameOver,
            soundMissile, soundPower, soundShield;
    private boolean soundLoaded = false;

    public GameView(Context context) {
        super(context);
        holder = getHolder();
        paint = new Paint();
        paint.setAntiAlias(true);
        random = new Random();
        shakeRandom = new Random();

        cloneBalls = new ArrayList<>();
        coloredBalls = new ArrayList<>();
        blackBalls = new ArrayList<>();
        specialBalls = new ArrayList<>();
        particles = new ArrayList<>();
        missiles = new ArrayList<>();
        electricEffects = new ArrayList<>();
        stars = new ArrayList<>();

        // Kayıtlı skoru yükle
        SharedPreferences prefs = context.getSharedPreferences("SpaceBilliard", Context.MODE_PRIVATE);
        highScore = prefs.getInt("highScore", 0);
        highLevel = prefs.getInt("highLevel", 1);
        highScore = prefs.getInt("highScore", 0);
        highLevel = prefs.getInt("highLevel", 1);
        maxCombo = prefs.getInt("maxCombo", 0);
        // maxUnlockedLevel'ı yükle (ilerlemeyi kaydet)
        maxUnlockedLevel = prefs.getInt("maxUnlockedLevel", 1);

        // SoundPool Başlatma
        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();

        soundPool = new SoundPool.Builder()
                .setMaxStreams(10)
                .setAudioAttributes(audioAttributes)
                .build();

        soundPool.setOnLoadCompleteListener((soundPool, sampleId, status) -> soundLoaded = true);

        try {
            soundLaunch = soundPool.load(context, R.raw.drag_launch, 1);
            soundCollision = soundPool.load(context, R.raw.collision_sound, 1);
            soundCoin = soundPool.load(context, R.raw.coin_sound, 1);
            soundBlackExplosion = soundPool.load(context, R.raw.black_ball_explosion, 1);
            soundElectric = soundPool.load(context, R.raw.electric_ball, 1);
            soundFreeze = soundPool.load(context, R.raw.freeze, 1);
            soundGameOver = soundPool.load(context, R.raw.game_over, 1);
            soundMissile = soundPool.load(context, R.raw.guided_missile, 1);
            soundPower = soundPool.load(context, R.raw.power_boost, 1);
            soundShield = soundPool.load(context, R.raw.shield_block, 1);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Yıldızları oluştur
        for (int i = 0; i < 100; i++) {
            stars.add(new Star());
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        screenWidth = w;
        screenHeight = h;

        // Ekranın küçük tarafına göre kare alan oluştur
        int minSize = Math.min(w, h);
        centerX = w / 2f;
        centerY = h / 2f;
        circleRadius = minSize * 0.47f;

        // Beyaz topu başlat
        if (whiteBall == null) {
            whiteBall = new Ball(centerX, centerY, minSize * 0.02f, Color.WHITE);
            initLevel(1);
        } else {
            // Ekran döndürüldüğünde pozisyonları güncelle
            updatePositionsAfterResize();
        }
    }

    private void updatePositionsAfterResize() {
        // Tüm nesneleri yeni merkeze göre ölçekle
        float oldCenterX = centerX;
        float oldCenterY = centerY;

        whiteBall.x = centerX;
        whiteBall.y = centerY;

        for (Ball ball : coloredBalls) {
            ball.x = centerX + (ball.x - oldCenterX);
            ball.y = centerY + (ball.y - oldCenterY);
        }

        for (Ball ball : blackBalls) {
            ball.x = centerX + (ball.x - oldCenterX);
            ball.y = centerY + (ball.y - oldCenterY);
        }

        for (SpecialBall ball : specialBalls) {
            ball.x = centerX + (ball.x - oldCenterX);
            ball.y = centerY + (ball.y - oldCenterY);
        }
    }

    private void initLevel(int lv) {
        level = lv;
        // Zorluk Ayarı
        lives = 3;
        // Level 6'dan sonra süre artar, ama level arttıkça top sayısı da artar
        timeLeft = 20000 + (lv * 500);
        timeLeft = Math.min(timeLeft, 45000); // Max 45 sn

        lastTime = System.currentTimeMillis();
        comboCounter = 0;

        coloredBalls.clear();
        blackBalls.clear();
        specialBalls.clear();
        cloneBalls.clear();
        particles.clear();
        missiles.clear();
        electricEffects.clear();

        whiteBall.x = centerX;
        whiteBall.y = centerY;
        whiteBall.vx = 0;
        whiteBall.vy = 0;
        // Ghost modundan çıkarken top boyutunu sıfırla
        whiteBall.radius = (circleRadius / 0.47f) * 0.02f;

        blackHoleActive = false;
        barrierActive = false;
        freezeActive = false;
        ghostModeActive = false;
        powerBoostActive = false;
        blastWave = null;

        // Renkli toplar (maksimum 15)
        int ballCount = Math.min(level + 4, 15);
        int[] colors = {
                Color.rgb(255, 0, 85), // Pink-Red
                Color.rgb(0, 255, 153), // Cyan-Green
                Color.rgb(255, 255, 0), // Yellow
                Color.rgb(0, 204, 255), // Sky Blue
                Color.rgb(255, 102, 0) // Orange
        };

        for (int i = 0; i < ballCount; i++) {
            float angle = random.nextFloat() * (float) (2 * Math.PI);
            float radius = circleRadius * 0.7f;
            float x = centerX + (float) Math.cos(angle) * radius;
            float y = centerY + (float) Math.sin(angle) * radius;

            Ball ball = new Ball(x, y, whiteBall.radius, colors[random.nextInt(colors.length)]);
            ball.vx = (random.nextFloat() - 0.5f) * 8;
            ball.vy = (random.nextFloat() - 0.5f) * 8;
            coloredBalls.add(ball);
        }

        // Siyah toplar
        int blackCount = (level >= 15) ? 5 : (level >= 10) ? 3 : (level >= 5) ? 2 : 1;
        for (int i = 0; i < blackCount; i++) {
            float angle = random.nextFloat() * (float) (2 * Math.PI);
            float radius = circleRadius * 0.7f;
            float x = centerX + (float) Math.cos(angle) * radius;
            float y = centerY + (float) Math.sin(angle) * radius;

            Ball ball = new Ball(x, y, whiteBall.radius * 1.2f, Color.BLACK);
            ball.vx = (random.nextFloat() - 0.5f) * 12;
            ball.vy = (random.nextFloat() - 0.5f) * 12;
            blackBalls.add(ball);
        }
    }

    @Override
    public void run() {
        while (isPlaying) {
            long startTime = System.currentTimeMillis();

            update();
            draw();

            // Özel top spawn
            if (gameStarted && !gameOver && random.nextFloat() < 0.001f && specialBalls.size() < 3) {
                spawnSpecialBall();
            }

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

    private void spawnSpecialBall() {
        String[] types = { "blackhole", "extraTime", "powerBoost", "barrier", "electric",
                "clone", "freeze", "missile", "teleport", "boom", "ghost" };
        String type = types[random.nextInt(types.length)];

        float angle = random.nextFloat() * (float) (2 * Math.PI);
        float radius = circleRadius * 0.7f;
        float x = centerX + (float) Math.cos(angle) * radius;
        float y = centerY + (float) Math.sin(angle) * radius;

        specialBalls.add(new SpecialBall(x, y, whiteBall.radius * 1.2f, type));
    }

    private void update() {
        if (!gameStarted || gameOver)
            return;

        long currentTime = System.currentTimeMillis();
        long deltaTime = currentTime - lastTime;
        lastTime = currentTime;

        // Zaman
        timeLeft -= deltaTime;
        if (timeLeft <= 0 && coloredBalls.size() > 0) {
            gameOver = true;
            saveProgress();
            playSound(soundGameOver);
            return;
        }

        // Tüm toplar toplandı mı?
        if (coloredBalls.size() == 0 && !levelCompleted) {
            levelCompleted = true;
            showStageCleared = true;
            stageClearedTime = System.currentTimeMillis();

            // Siyah topları yok et
            blackBalls.clear();

            // Partiküller oluştur
            for (int i = 0; i < 30; i++) {
                float angle = random.nextFloat() * (float) (2 * Math.PI);
                float speed = random.nextFloat() * 8 + 4;
                particles.add(new Particle(centerX, centerY, angle, speed, Color.rgb(255, 215, 0)));
            }
        }

        // Stage Cleared animasyonu bitince bir sonraki stage'e geç
        if (levelCompleted && System.currentTimeMillis() - stageClearedTime > 3000) {
            levelCompleted = false;
            showStageCleared = false;
            level++;

            // Her 10 stage tamamlandığında yeni level aç
            // Örnek: Stage 10 bitince Level 2 açılır, Stage 20 bitince Level 3...
            int completedStages = level - 1; // Tamamlanan stage sayısı
            int unlockedLevelCount = (completedStages / 10) + 1; // Açılması gereken level sayısı

            if (unlockedLevelCount > maxUnlockedLevel) {
                maxUnlockedLevel = unlockedLevelCount;
                saveProgress();
            }

            initLevel(level);
            return;
        }

        // Özel yetenekler zaman kontrolü
        if (barrierActive && currentTime > barrierEndTime)
            barrierActive = false;
        if (freezeActive && currentTime > freezeEndTime)
            freezeActive = false;
        if (ghostModeActive && currentTime > ghostModeEndTime) {
            ghostModeActive = false;
            whiteBall.radius = originalWhiteBallRadius;
        }
        if (blackHoleActive && currentTime > blackHoleEndTime)
            blackHoleActive = false;

        // Beyaz top
        if (!isDragging || draggedBall != whiteBall) {
            whiteBall.x += whiteBall.vx;
            whiteBall.y += whiteBall.vy;
            reflectBall(whiteBall);
            whiteBall.vx *= 0.995f;
            whiteBall.vy *= 0.995f;
        }

        // Clone toplar
        for (int i = cloneBalls.size() - 1; i >= 0; i--) {
            Ball ball = cloneBalls.get(i);

            // Süre kontrolü (8 saniye)
            if (System.currentTimeMillis() - ball.creationTime > 8000) {
                cloneBalls.remove(i);
                continue;
            }

            if (!isDragging || draggedBall != ball) {
                ball.x += ball.vx;
                ball.y += ball.vy;
                reflectBall(ball);
                ball.vx *= 0.995f;
                ball.vy *= 0.995f;
            }
        }

        // Renkli toplar (freeze kontrolü)
        if (!freezeActive) {
            for (Ball ball : coloredBalls) {
                ball.x += ball.vx;
                ball.y += ball.vy;
                reflectBall(ball);
            }

            for (Ball ball : blackBalls) {
                ball.x += ball.vx;
                ball.y += ball.vy;
                reflectBall(ball);
            }

            for (SpecialBall ball : specialBalls) {
                ball.x += ball.vx;
                ball.y += ball.vy;
                reflectBall(ball);
            }
        }

        // Black hole çekimi
        if (blackHoleActive) {
            attractBallsToWhite();
        }

        // Çarpışmalar
        checkCollisions();

        // Missiles
        updateMissiles();

        // Blast wave
        if (blastWave != null) {
            blastWave.update();
            if (blastWave.isDead())
                blastWave = null;
        }

        // Electric effects
        for (int i = electricEffects.size() - 1; i >= 0; i--) {
            ElectricEffect effect = electricEffects.get(i);
            effect.update();
            if (effect.isDead())
                electricEffects.remove(i);
        }

        // Parçacıklar
        for (int i = particles.size() - 1; i >= 0; i--) {
            Particle p = particles.get(i);
            p.update();
            if (p.isDead())
                particles.remove(i);
        }

        // Yıldızları güncelle
        if (stars != null) {
            for (Star star : stars) {
                star.update(screenWidth, screenHeight);
            }
        }

        // Kamera sallanma güncelle
        if (System.currentTimeMillis() < shakeEndTime) {
            cameraShakeX = (shakeRandom.nextFloat() - 0.5f) * 20;
            cameraShakeY = (shakeRandom.nextFloat() - 0.5f) * 20;
        } else {
            cameraShakeX = 0;
            cameraShakeY = 0;
        }
    }

    private void attractBallsToWhite() {
        float attractionSpeed = 5;

        for (Ball ball : coloredBalls) {
            float dx = whiteBall.x - ball.x;
            float dy = whiteBall.y - ball.y;
            float distance = (float) Math.sqrt(dx * dx + dy * dy);
            if (distance > 5) {
                ball.x += (dx / distance) * attractionSpeed;
                ball.y += (dy / distance) * attractionSpeed;
            }
        }

        for (SpecialBall ball : specialBalls) {
            float dx = whiteBall.x - ball.x;
            float dy = whiteBall.y - ball.y;
            float distance = (float) Math.sqrt(dx * dx + dy * dy);
            if (distance > 5) {
                ball.x += (dx / distance) * attractionSpeed;
                ball.y += (dy / distance) * attractionSpeed;
            }
        }
    }

    private void checkCollisions() {
        // Drag sırasında çarpışma yok
        if (isDragging) {
            return;
        }

        ArrayList<Ball> allWhiteBalls = new ArrayList<>();
        allWhiteBalls.add(whiteBall);
        allWhiteBalls.addAll(cloneBalls);

        for (Ball wBall : allWhiteBalls) {
            // Renkli toplar
            for (int i = coloredBalls.size() - 1; i >= 0; i--) {
                Ball ball = coloredBalls.get(i);
                if (checkBallCollision(wBall, ball)) {
                    score++;
                    timeLeft += 1000;
                    comboCounter++;

                    // Combo sistemi
                    long currentTime = System.currentTimeMillis();
                    if (currentTime - lastHitTime < COMBO_TIMEOUT) {
                        comboHits++;
                        if (comboHits > maxCombo)
                            maxCombo = comboHits; // Rekor kontrolü
                        if (comboHits >= 2) {
                            comboText = "COMBO x" + (comboHits + 1);
                            comboTextEndTime = currentTime + 1500;
                        }
                    } else {
                        comboHits = 0;
                    }
                    lastHitTime = currentTime;

                    createParticles(ball.x, ball.y, ball.color);
                    coloredBalls.remove(i);
                    playSound(soundCollision);

                    // Hız artır
                    float dx = wBall.x - ball.x;
                    float dy = wBall.y - ball.y;
                    float angle = (float) Math.atan2(dy, dx);
                    float speed = (float) Math.sqrt(wBall.vx * wBall.vx + wBall.vy * wBall.vy);
                    wBall.vx = (float) Math.cos(angle) * speed * 1.05f;
                    wBall.vy = (float) Math.sin(angle) * speed * 1.05f;
                }
            }

            // Siyah toplar
            for (Ball ball : blackBalls) {
                // Dokunulmazlık kontrolü
                if (System.currentTimeMillis() < immuneEndTime)
                    continue;

                if (checkBallCollision(wBall, ball)) {
                    if (barrierActive || ghostModeActive) {
                        // Korundu
                        long currentTime = System.currentTimeMillis();
                        if (currentTime - lastShieldSoundTime > 500) {
                            playSound(soundShield);
                            lastShieldSoundTime = currentTime;
                        }
                    } else {
                        lives--;
                        comboCounter = 0;

                        // Kamera sallanma efekti
                        shakeEndTime = System.currentTimeMillis() + 500;

                        if (lives <= 0) {
                            gameOver = true;
                            saveProgress();
                            playSound(soundGameOver);
                        } else {
                            wBall.x = centerX;
                            wBall.y = centerY;
                            wBall.vx = 0;
                            wBall.vy = 0;
                            immuneEndTime = System.currentTimeMillis() + 2000; // 2 saniye koruma
                        }
                    }
                }
            }

            // Özel toplar
            for (int i = specialBalls.size() - 1; i >= 0; i--) {
                SpecialBall ball = specialBalls.get(i);
                if (checkBallCollision(wBall, ball)) {
                    activateSpecialPower(ball.type, wBall);
                    createParticles(ball.x, ball.y, ball.getColor());
                    specialBalls.remove(i);
                }
            }
        }
    }

    private boolean checkBallCollision(Ball b1, Ball b2) {
        float dx = b1.x - b2.x;
        float dy = b1.y - b2.y;
        float distance = (float) Math.sqrt(dx * dx + dy * dy);
        return distance < b1.radius + b2.radius;
    }

    private boolean checkBallCollision(Ball b1, SpecialBall b2) {
        float dx = b1.x - b2.x;
        float dy = b1.y - b2.y;
        float distance = (float) Math.sqrt(dx * dx + dy * dy);
        return distance < b1.radius + b2.radius;
    }

    private void activateSpecialPower(String type, Ball targetBall) {
        switch (type) {
            case "blackhole":
                blackHoleActive = true;
                blackHoleEndTime = System.currentTimeMillis() + 2000;
                break;
            case "extraTime":
                timeLeft += 5000;
                break;
            case "powerBoost":
                powerBoostActive = true;
                playSound(soundPower);
                break;
            case "barrier":
                barrierActive = true;
                barrierEndTime = System.currentTimeMillis() + 5000;
                playSound(soundShield);
                break;
            case "electric":
                triggerElectric();
                playSound(soundElectric);
                break;
            case "clone":
                Ball clone = new Ball(centerX, centerY, whiteBall.radius, Color.WHITE);
                cloneBalls.add(clone);
                break;
            case "freeze":
                freezeActive = true;
                freezeEndTime = System.currentTimeMillis() + 5000;
                playSound(soundFreeze);
                break;
            case "missile":
                if (blackBalls.size() > 0) {
                    missiles.add(new GuidedMissile(whiteBall.x, whiteBall.y, blackBalls.get(0)));
                    playSound(soundMissile);
                }
                break;
            case "teleport":
                float angle = random.nextFloat() * (float) (2 * Math.PI);
                float radius = circleRadius * 0.5f;
                targetBall.x = centerX + (float) Math.cos(angle) * radius;
                targetBall.y = centerY + (float) Math.sin(angle) * radius;
                createParticles(targetBall.x, targetBall.y, Color.GREEN);
                break;
            case "boom":
                blastWave = new BlastWave(targetBall.x, targetBall.y);
                break;
            case "ghost":
                if (!ghostModeActive) {
                    originalWhiteBallRadius = whiteBall.radius;
                    ghostModeActive = true;
                    ghostModeEndTime = System.currentTimeMillis() + 3000;
                    // Sadece beyaz topu büyüt, diğerleri etkilenmez
                    whiteBall.radius = originalWhiteBallRadius * 1.75f;
                }
                break;
        }
    }

    private void playSound(int soundID) {
        if (soundLoaded) {
            soundPool.play(soundID, 1, 1, 1, 0, 1f);
        }
    }

    private void triggerElectric() {
        if (coloredBalls.size() == 0)
            return;

        // İlk hedef
        Ball target1 = coloredBalls.get(random.nextInt(coloredBalls.size()));
        electricEffects.add(new ElectricEffect(whiteBall.x, whiteBall.y, target1.x, target1.y));
        createParticles(target1.x, target1.y, target1.color);
        score++;
        comboCounter++;
        coloredBalls.remove(target1);

        // İkinci hedef
        if (coloredBalls.size() > 0) {
            Ball target2 = coloredBalls.get(random.nextInt(coloredBalls.size()));
            electricEffects.add(new ElectricEffect(target1.x, target1.y, target2.x, target2.y));
            createParticles(target2.x, target2.y, target2.color);
            score++;
            comboCounter++;
            coloredBalls.remove(target2);
        }
    }

    private void updateMissiles() {
        for (int i = missiles.size() - 1; i >= 0; i--) {
            GuidedMissile missile = missiles.get(i);
            missile.update();

            // Hedef kontrolü
            for (int j = blackBalls.size() - 1; j >= 0; j--) {
                Ball ball = blackBalls.get(j);
                float dx = missile.x - ball.x;
                float dy = missile.y - ball.y;
                float distance = (float) Math.sqrt(dx * dx + dy * dy);

                if (distance < missile.radius + ball.radius) {
                    createParticles(ball.x, ball.y, Color.BLACK);
                    blackBalls.remove(j);
                    missiles.remove(i);
                    playSound(soundBlackExplosion);
                    break;
                }
            }

            // Sınır kontrolü
            float dx = missile.x - centerX;
            float dy = missile.y - centerY;
            if (Math.sqrt(dx * dx + dy * dy) > circleRadius) {
                missiles.remove(i);
            }
        }
    }

    private void reflectBall(Ball ball) {
        float dx = ball.x - centerX;
        float dy = ball.y - centerY;
        float distance = (float) Math.sqrt(dx * dx + dy * dy);

        if (distance + ball.radius > circleRadius) {
            float nx = dx / distance;
            float ny = dy / distance;

            float dot = ball.vx * nx + ball.vy * ny;
            ball.vx -= 2 * dot * nx;
            ball.vy -= 2 * dot * ny;

            float angle = (float) Math.atan2(dy, dx);
            ball.x = centerX + (float) Math.cos(angle) * (circleRadius - ball.radius);
            ball.y = centerY + (float) Math.sin(angle) * (circleRadius - ball.radius);
        }
    }

    private void reflectBall(SpecialBall ball) {
        float dx = ball.x - centerX;
        float dy = ball.y - centerY;
        float distance = (float) Math.sqrt(dx * dx + dy * dy);

        if (distance + ball.radius > circleRadius) {
            float nx = dx / distance;
            float ny = dy / distance;

            float dot = ball.vx * nx + ball.vy * ny;
            ball.vx -= 2 * dot * nx;
            ball.vy -= 2 * dot * ny;

            float angle = (float) Math.atan2(dy, dx);
            ball.x = centerX + (float) Math.cos(angle) * (circleRadius - ball.radius);
            ball.y = centerY + (float) Math.sin(angle) * (circleRadius - ball.radius);
        }
    }

    private void createParticles(float x, float y, int color) {
        for (int i = 0; i < 15; i++) {
            float angle = random.nextFloat() * (float) (2 * Math.PI);
            float speed = random.nextFloat() * 5 + 2;
            particles.add(new Particle(x, y, angle, speed, color));
        }
    }

    private void draw() {
        if (holder.getSurface().isValid()) {
            canvas = holder.lockCanvas();

            // Kamera Offset
            canvas.save();
            canvas.translate(cameraShakeX, cameraShakeY);

            // Arka plan
            canvas.drawColor(Color.rgb(5, 5, 16));

            // Yıldızları çiz
            for (Star star : stars) {
                star.draw(canvas, paint);
            }

            // Çember
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(6);
            paint.setColor(Color.rgb(0, 243, 255));
            paint.setShadowLayer(20, 0, 0, Color.rgb(0, 243, 255));
            canvas.drawCircle(centerX, centerY, circleRadius, paint);
            paint.clearShadowLayer();

            // Blast wave
            if (blastWave != null) {
                blastWave.draw(canvas, paint);
            }

            // Electric effects
            for (ElectricEffect effect : electricEffects) {
                effect.draw(canvas, paint);
            }

            // Parçacıklar
            for (Particle p : particles) {
                p.draw(canvas, paint);
            }

            // Toplar
            for (Ball ball : coloredBalls) {
                drawBall(canvas, ball);
            }

            for (Ball ball : blackBalls) {
                drawBall(canvas, ball);
            }

            for (SpecialBall ball : specialBalls) {
                drawSpecialBall(canvas, ball);
            }

            for (Ball ball : cloneBalls) {
                drawBall(canvas, ball);
            }

            // Missiles
            for (GuidedMissile missile : missiles) {
                missile.draw(canvas, paint);
            }

            // Beyaz top
            drawBall(canvas, whiteBall);

            // Barrier
            if (barrierActive) {
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(3);
                paint.setColor(Color.rgb(0, 150, 255));
                paint.setAlpha(128);
                canvas.drawCircle(whiteBall.x, whiteBall.y, whiteBall.radius * 2, paint);
                paint.setAlpha(255);
            }

            // Sürükleme
            if (isDragging && draggedBall != null) {
                float dx = draggedBall.x - dragStartX;
                float dy = draggedBall.y - dragStartY;
                float distance = Math.min((float) Math.sqrt(dx * dx + dy * dy), MAX_DRAG_DISTANCE);
                float ratio = distance / MAX_DRAG_DISTANCE;

                // Eski çizgi (drag line) yerine nişan çizgisi (trajectory)
                if (distance > 10) {
                    float launchAngle = (float) Math.atan2(-dy, -dx);

                    // Çizgi ve Ok parametreleri
                    float startDist = draggedBall.radius * 1.5f;
                    float lineLen = 400 * ratio; // Güç arttıkça uzayan çizgi

                    float startX = draggedBall.x + (float) Math.cos(launchAngle) * startDist;
                    float startY = draggedBall.y + (float) Math.sin(launchAngle) * startDist;
                    float endX = draggedBall.x + (float) Math.cos(launchAngle) * (startDist + lineLen);
                    float endY = draggedBall.y + (float) Math.sin(launchAngle) * (startDist + lineLen);

                    // Kesikli çizgi (Trajectory)
                    paint.setStyle(Paint.Style.STROKE);
                    paint.setStrokeWidth(5);
                    paint.setColor(Color.WHITE);
                    paint.setAlpha(180);
                    // android.graphics.DashPathEffect kullanarak kesikli çizgi
                    paint.setPathEffect(new android.graphics.DashPathEffect(new float[] { 20, 20 }, 0));
                    canvas.drawLine(startX, startY, endX, endY, paint);
                    paint.setPathEffect(null); // Efekti temizle
                    paint.setAlpha(255);

                    // Ok başı (V şeklinde) - Çizginin SONUNDA
                    paint.setStrokeWidth(8);
                    paint.setStrokeCap(Paint.Cap.ROUND);
                    paint.setColor(Color.WHITE);
                    paint.setShadowLayer(10, 0, 0, Color.CYAN);

                    float arrowSize = 40;
                    float wingAngle = (float) Math.toRadians(150); // 150 derece kanat açısı

                    // startX/Y yerine endX/Y kullanıyoruz
                    float wing1X = endX + (float) Math.cos(launchAngle + wingAngle) * arrowSize;
                    float wing1Y = endY + (float) Math.sin(launchAngle + wingAngle) * arrowSize;

                    float wing2X = endX + (float) Math.cos(launchAngle - wingAngle) * arrowSize;
                    float wing2Y = endY + (float) Math.sin(launchAngle - wingAngle) * arrowSize;

                    canvas.drawLine(endX, endY, wing1X, wing1Y, paint);
                    canvas.drawLine(endX, endY, wing2X, wing2Y, paint);
                    paint.clearShadowLayer();
                }

                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(3);
                int powerColor = Color.rgb((int) (255 * ratio), (int) (255 * (1 - ratio)), 0);
                paint.setColor(powerColor);
                canvas.drawCircle(draggedBall.x, draggedBall.y, draggedBall.radius * (1 + ratio), paint);

                // Güç barı - topun etrafında dairesel
                float arcRadius = draggedBall.radius * 2.5f;
                float sweepAngle = 360 * ratio;

                // Arka plan çember (gri)
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(8);
                paint.setColor(Color.rgb(60, 60, 60));
                paint.setAlpha(150);
                canvas.drawCircle(draggedBall.x, draggedBall.y, arcRadius, paint);
                paint.setAlpha(255);

                // Dolu kısım (mavi glow)
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(10);
                paint.setStrokeCap(Paint.Cap.ROUND);
                paint.setColor(Color.rgb(0, 180, 255));
                paint.setShadowLayer(20, 0, 0, Color.rgb(0, 180, 255));
                canvas.drawArc(
                        draggedBall.x - arcRadius, draggedBall.y - arcRadius,
                        draggedBall.x + arcRadius, draggedBall.y + arcRadius,
                        -90, sweepAngle, false, paint);
                paint.clearShadowLayer();
            }

            // Combo text göster
            if (System.currentTimeMillis() < comboTextEndTime && !comboText.isEmpty()) {
                paint.setStyle(Paint.Style.FILL);
                paint.setTextSize(screenWidth * 0.12f);
                paint.setTextAlign(Paint.Align.CENTER);
                paint.setColor(Color.rgb(255, 215, 0));
                paint.setShadowLayer(15, 0, 0, Color.rgb(255, 215, 0));
                canvas.drawText(comboText, centerX, centerY - screenHeight * 0.2f, paint);
                paint.clearShadowLayer();
            }

            // STAGE CLEARED animasyonu
            if (showStageCleared) {
                paint.setStyle(Paint.Style.FILL);
                paint.setTextSize(screenWidth * 0.1f);
                paint.setTextAlign(Paint.Align.CENTER);
                paint.setColor(Color.rgb(0, 255, 100));
                paint.setShadowLayer(25, 0, 0, Color.rgb(0, 255, 100));
                canvas.drawText("STAGE CLEARED!", centerX, centerY, paint);
                paint.clearShadowLayer();

                // Alt yazı
                paint.setTextSize(screenWidth * 0.045f);
                paint.setColor(Color.WHITE);
                int currentStageNum = ((level - 1) % 10) + 1;
                canvas.drawText("Stage " + currentStageNum + " Complete", centerX, centerY + screenHeight * 0.08f,
                        paint);
            }

            // UI
            drawUI(canvas);

            canvas.restore();
            holder.unlockCanvasAndPost(canvas);
        }
    }

    private void drawBall(Canvas canvas, Ball ball) {
        paint.setStyle(Paint.Style.FILL);

        RadialGradient gradient = new RadialGradient(
                ball.x - ball.radius / 3, ball.y - ball.radius / 3, ball.radius,
                Color.WHITE, ball.color, Shader.TileMode.CLAMP);
        paint.setShader(gradient);
        paint.setShadowLayer(15, 0, 0, ball.color);

        canvas.drawCircle(ball.x, ball.y, ball.radius, paint);

        paint.clearShadowLayer();
        paint.setShader(null);
    }

    private void drawSpecialBall(Canvas canvas, SpecialBall ball) {
        paint.setStyle(Paint.Style.FILL);

        RadialGradient gradient = new RadialGradient(
                ball.x - ball.radius / 3, ball.y - ball.radius / 3, ball.radius,
                Color.WHITE, ball.getColor(), Shader.TileMode.CLAMP);
        paint.setShader(gradient);
        paint.setShadowLayer(20, 0, 0, ball.getColor());

        canvas.drawCircle(ball.x, ball.y, ball.radius, paint);

        paint.clearShadowLayer();
        paint.setShader(null);

        // Harf çiz
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.WHITE);
        paint.setTextSize(ball.radius * 1.2f);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setShadowLayer(3, 0, 0, Color.BLACK);
        canvas.drawText(ball.getLetter(), ball.x, ball.y + ball.radius * 0.4f, paint);
        paint.clearShadowLayer();
    }

    private void drawUI(Canvas canvas) {
        float textSize = screenWidth * 0.04f;
        paint.setTextSize(textSize);
        paint.setStyle(Paint.Style.FILL);
        paint.setTextAlign(Paint.Align.LEFT);

        float margin = screenWidth * 0.03f;
        float lineHeight = textSize * 1.5f;

        paint.setColor(Color.YELLOW);
        canvas.drawText("TIME: " + (timeLeft / 1000), margin, lineHeight, paint);

        paint.setColor(Color.CYAN);
        canvas.drawText("SCORE: " + score, margin, lineHeight * 2, paint);

        paint.setColor(Color.MAGENTA);
        String rank = getRank();
        canvas.drawText("RANK: " + rank, margin, lineHeight * 3, paint);

        paint.setTextAlign(Paint.Align.RIGHT);

        paint.setColor(Color.rgb(255, 0, 85));
        canvas.drawText("POWER: " + (int) (lastLaunchPower * (powerBoostActive ? 200 : 100)) + "%",
                screenWidth - margin, lineHeight, paint);

        paint.setColor(Color.GREEN);
        // Stage gösterimi (1-10 arası)
        int currentStage = ((level - 1) % 10) + 1;
        canvas.drawText("STAGE: " + currentStage + "/10", screenWidth - margin, lineHeight * 2, paint);

        paint.setColor(Color.RED);
        canvas.drawText("LIVES: " + lives, screenWidth - margin, lineHeight * 3, paint);

        // Level Seçim Ekranı
        if (showLevelSelector && !gameStarted) {
            drawLevelSelector(canvas);
            return;
        }

        if (!gameStarted) {
            // Arka plan paneli (Glassmorphism)
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.argb(170, 25, 25, 50)); // Koyu mavi-mor
            float panelWidth = screenWidth * 0.85f;
            float panelHeight = screenHeight * 0.65f;
            canvas.drawRoundRect(
                    centerX - panelWidth / 2, centerY - panelHeight / 2,
                    centerX + panelWidth / 2, centerY + panelHeight / 2,
                    35, 35, paint);

            // Panel kenarlığı (Neon glow)
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(3);
            paint.setColor(Color.rgb(0, 243, 255));
            paint.setShadowLayer(18, 0, 0, Color.CYAN);
            canvas.drawRoundRect(
                    centerX - panelWidth / 2, centerY - panelHeight / 2,
                    centerX + panelWidth / 2, centerY + panelHeight / 2,
                    35, 35, paint);
            paint.clearShadowLayer();

            // Başlık
            paint.setTextAlign(Paint.Align.CENTER);

            // "NEON"
            paint.setTextSize(screenWidth * 0.15f);
            paint.setColor(Color.CYAN);
            paint.setShadowLayer(30, 0, 0, Color.CYAN);
            canvas.drawText("NEON", centerX, centerY - screenHeight * 0.25f, paint);

            // "BILLIARD"
            paint.setColor(Color.MAGENTA);
            paint.setShadowLayer(30, 0, 0, Color.MAGENTA);
            canvas.drawText("BILLIARD", centerX, centerY - screenHeight * 0.12f, paint);
            paint.clearShadowLayer();

            // Alt Başlık "SPACE EDITION"
            paint.setTextSize(screenWidth * 0.05f);
            paint.setColor(Color.WHITE);
            paint.setAlpha(150);
            canvas.drawText("SPACE EDITION", centerX, centerY - screenHeight * 0.05f, paint);
            paint.setAlpha(255);

            // Buton Parametreleri
            float buttonWidth = screenWidth * 0.5f;
            float buttonHeight = screenHeight * 0.07f;
            float buttonTextSize = buttonHeight * 0.5f;

            // START GAME Butonu
            float startY = centerY + screenHeight * 0.05f;

            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(4);
            paint.setColor(Color.rgb(0, 243, 255)); // Cyan
            paint.setShadowLayer(15, 0, 0, Color.rgb(0, 243, 255));

            canvas.drawRoundRect(
                    centerX - buttonWidth / 2, startY - buttonHeight / 2,
                    centerX + buttonWidth / 2, startY + buttonHeight / 2,
                    20, 20, paint);

            paint.setStyle(Paint.Style.FILL);
            paint.setTextSize(buttonTextSize * 0.85f);
            canvas.drawText("START GAME", centerX, startY + buttonTextSize / 3, paint);
            paint.clearShadowLayer();

            // HOW TO PLAY Butonu (Eski DATABASE yerine)
            float howToY = centerY + screenHeight * 0.15f;

            paint.setStyle(Paint.Style.STROKE);
            paint.setColor(Color.rgb(255, 60, 120)); // Pink
            paint.setShadowLayer(15, 0, 0, Color.rgb(255, 60, 120));

            canvas.drawRoundRect(
                    centerX - buttonWidth / 2, howToY - buttonHeight / 2,
                    centerX + buttonWidth / 2, howToY + buttonHeight / 2,
                    20, 20, paint);

            paint.setStyle(Paint.Style.FILL);
            paint.setTextSize(buttonTextSize * 0.85f);
            canvas.drawText("HOW TO PLAY", centerX, howToY + buttonTextSize / 3, paint);
            paint.clearShadowLayer();
        }

        if (gameOver) {
            // Arka plan paneli (Glassmorphism)
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.argb(180, 20, 20, 40)); // Koyu mavi-siyah
            float panelWidth = screenWidth * 0.85f;
            float panelHeight = screenHeight * 0.7f;
            canvas.drawRoundRect(
                    centerX - panelWidth / 2, centerY - panelHeight / 2,
                    centerX + panelWidth / 2, centerY + panelHeight / 2,
                    30, 30, paint);

            // Panel kenarlığı (Neon)
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(3);
            paint.setColor(Color.rgb(0, 243, 255));
            paint.setShadowLayer(15, 0, 0, Color.CYAN);
            canvas.drawRoundRect(
                    centerX - panelWidth / 2, centerY - panelHeight / 2,
                    centerX + panelWidth / 2, centerY + panelHeight / 2,
                    30, 30, paint);
            paint.clearShadowLayer();

            // Başlık (Panel içinde ortalanmış)
            paint.setTextAlign(Paint.Align.CENTER);

            paint.setTextSize(screenWidth * 0.12f);
            paint.setColor(Color.RED);
            paint.setShadowLayer(25, 0, 0, Color.RED);
            canvas.drawText("GAME OVER", centerX, centerY - screenHeight * 0.15f, paint);
            paint.clearShadowLayer();

            // Skorlar gösterilmiyor, sadece Hall of Fame'de görüntülenecek
            if (score > highScore)
                highScore = score;
            if (level > highLevel)
                highLevel = level;

            // Butonlar
            float buttonWidth = screenWidth * 0.45f;
            float buttonHeight = screenHeight * 0.06f;
            float buttonTextSize = buttonHeight * 0.35f;

            // REBOOT (Cyan Stroke)
            float rebootY = centerY + screenHeight * 0.05f;
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(4);
            paint.setColor(Color.rgb(0, 243, 255));
            paint.setShadowLayer(15, 0, 0, Color.rgb(0, 243, 255));
            canvas.drawRoundRect(centerX - buttonWidth / 2, rebootY - buttonHeight / 2,
                    centerX + buttonWidth / 2, rebootY + buttonHeight / 2, 20, 20, paint);
            paint.setStyle(Paint.Style.FILL);
            paint.setTextSize(buttonTextSize);
            canvas.drawText("REBOOT SYSTEM", centerX, rebootY + buttonTextSize / 3, paint);
            paint.clearShadowLayer();

            // HALL OF FAME (Gold Stroke)
            float hallY = centerY + screenHeight * 0.15f;
            paint.setStyle(Paint.Style.STROKE);
            paint.setColor(Color.rgb(255, 215, 0));
            paint.setShadowLayer(15, 0, 0, Color.rgb(255, 215, 0));
            canvas.drawRoundRect(centerX - buttonWidth / 2, hallY - buttonHeight / 2,
                    centerX + buttonWidth / 2, hallY + buttonHeight / 2, 20, 20, paint);
            paint.setStyle(Paint.Style.FILL);
            canvas.drawText("HALL OF FAME", centerX, hallY + buttonTextSize / 3, paint);
            paint.clearShadowLayer();

            // MAIN MENU (Gray Stroke)
            float menuY = centerY + screenHeight * 0.25f;
            paint.setStyle(Paint.Style.STROKE);
            paint.setColor(Color.LTGRAY);
            canvas.drawRoundRect(centerX - buttonWidth / 2, menuY - buttonHeight / 2,
                    centerX + buttonWidth / 2, menuY + buttonHeight / 2, 20, 20, paint);
            paint.setStyle(Paint.Style.FILL);
            canvas.drawText("MAIN MENU", centerX, menuY + buttonTextSize / 3, paint);
        }
        // Başlık... (Mevcut kod aşağıda kalacak, sadece if bloğunu değiştiriyorum)

        // Instructions overlay
        if (showInstructions)

        {
            drawInstructionsOverlay(canvas);
        }

        // High Score overlay
        if (showHighScore) {
            drawHighScoreOverlay(canvas);
        }
    }

    private void drawInstructionsOverlay(Canvas canvas) {
        // Yarı saydam arka plan
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.BLACK);
        paint.setAlpha(220);
        canvas.drawRect(0, 0, screenWidth, screenHeight, paint);
        paint.setAlpha(255);

        // Başlık
        paint.setTextSize(screenWidth * 0.08f);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setColor(Color.rgb(0, 243, 255));
        canvas.drawText("HOW TO PLAY", centerX, screenHeight * 0.15f, paint);

        // Talimatlar
        paint.setTextSize(screenWidth * 0.04f);
        paint.setColor(Color.WHITE);
        float y = screenHeight * 0.25f;
        float lineSpacing = screenHeight * 0.06f;

        canvas.drawText("• Drag white ball to launch", centerX, y, paint);
        y += lineSpacing;
        canvas.drawText("• Collect colored balls = +1 point", centerX, y, paint);
        y += lineSpacing;
        canvas.drawText("• Avoid black balls = -1 life", centerX, y, paint);
        y += lineSpacing;
        canvas.drawText("• Special balls give powers:", centerX, y, paint);
        y += lineSpacing;

        paint.setTextSize(screenWidth * 0.035f);
        canvas.drawText("B=BlackHole T=Time P=Power S=Shield", centerX, y, paint);
        y += lineSpacing * 0.8f;
        canvas.drawText("L=Lightning C=Clone F=Freeze M=Missile", centerX, y, paint);
        y += lineSpacing * 0.8f;
        canvas.drawText("T=Teleport X=Boom G=Ghost", centerX, y, paint);

        // Close butonu
        paint.setTextSize(screenWidth * 0.06f);
        paint.setColor(Color.rgb(255, 100, 100));
        canvas.drawText("TAP TO CLOSE", centerX, screenHeight * 0.85f, paint);
    }

    private void drawHighScoreOverlay(Canvas canvas) {
        // Yarı saydam arka plan
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.BLACK);
        paint.setAlpha(220);
        canvas.drawRect(0, 0, screenWidth, screenHeight, paint);
        paint.setAlpha(255);

        // Başlık
        paint.setTextSize(screenWidth * 0.1f);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setColor(Color.rgb(255, 215, 0));
        canvas.drawText("HIGH SCORE", centerX, screenHeight * 0.3f, paint);

        // Skorlar
        paint.setTextSize(screenWidth * 0.07f);
        paint.setColor(Color.WHITE);
        canvas.drawText("Best Score: " + highScore, centerX, screenHeight * 0.45f, paint);
        canvas.drawText("Best Level: " + highLevel, centerX, screenHeight * 0.55f, paint);
        canvas.drawText("Max Combo: " + maxCombo, centerX, screenHeight * 0.65f, paint);

        // Close butonu
        paint.setTextSize(screenWidth * 0.06f);
        paint.setColor(Color.rgb(255, 100, 100));
        canvas.drawText("TAP TO CLOSE", centerX, screenHeight * 0.75f, paint);
    }

    private String getRank() {
        if (comboCounter >= 11)
            return "SS";
        if (comboCounter >= 9)
            return "S";
        if (comboCounter >= 7)
            return "A++";
        if (comboCounter >= 5)
            return "A";
        if (comboCounter >= 3)
            return "B";
        if (comboCounter >= 1)
            return "C";
        return "-";
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float touchX = event.getX();
        float touchY = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                // Overlay kapatma
                if (showInstructions || showHighScore) {
                    showInstructions = false;
                    showHighScore = false;
                    return true;
                }

                if (showLevelSelector) {
                    // Sayfa Değiştirme (Oklar)
                    float arrowY = centerY;
                    float arrowSize = screenWidth * 0.1f;

                    // Sol Ok
                    if (touchX < arrowSize * 1.5f && Math.abs(touchY - arrowY) < arrowSize) {
                        if (selectorPage > 1) {
                            selectorPage--;
                            playSound(soundLaunch); // Tuş sesi olarak kullan
                        }
                        return true;
                    }

                    // Sağ Ok
                    if (touchX > screenWidth - arrowSize * 1.5f && Math.abs(touchY - arrowY) < arrowSize) {
                        // Max sayfa sınırı yok, level arttıkça gider
                        selectorPage++;
                        playSound(soundLaunch);
                        return true;
                    }

                    // BACK Butonu (Yeni pozisyon)
                    float backY = screenHeight * 0.85f;
                    if (touchY > backY - screenWidth * 0.05f && touchY < backY + screenWidth * 0.05f) {
                        showLevelSelector = false;
                        return true;
                    }

                    // Level Butonları
                    float gridStartX = screenWidth * 0.15f;
                    float gridStartY = screenHeight * 0.4f;
                    float cellWidth = screenWidth * 0.14f;
                    float cellHeight = screenWidth * 0.14f;
                    float gap = screenWidth * 0.02f;

                    for (int i = 0; i < 10; i++) {
                        int row = i / 5;
                        int col = i % 5;
                        float btnX = gridStartX + col * (cellWidth + gap) + cellWidth / 2;
                        float btnY = gridStartY + row * (cellHeight + gap) + cellHeight / 2;

                        if (Math.abs(touchX - btnX) < cellWidth / 2 && Math.abs(touchY - btnY) < cellHeight / 2) {
                            int selectedLv = (selectorPage - 1) * 10 + i + 1;
                            if (selectedLv <= maxUnlockedLevel) {
                                gameStarted = true;
                                // Seçilen level'in ilk stage'ini başlat
                                level = (selectedLv - 1) * 10 + 1;
                                score = 0;
                                lives = 3;
                                initLevel(level);
                                showLevelSelector = false;
                                playSound(soundLaunch);
                            } else {
                                // Locked sound?
                            }
                            return true;
                        }
                    }
                    return true;
                }

                if (!gameStarted) {
                    // START GAME butonu kontrolü
                    float startY = centerY + screenHeight * 0.05f;
                    // X ve Y ekseninde kontrol (Daha geniş alan)
                    if (Math.abs(touchY - startY) < screenHeight * 0.08f &&
                            Math.abs(touchX - centerX) < screenWidth * 0.4f) {
                        showLevelSelector = true;
                        return true;
                    }

                    // HOW TO PLAY butonu kontrolü
                    float howToY = centerY + screenHeight * 0.15f;
                    if (Math.abs(touchY - howToY) < screenHeight * 0.05f) {
                        showInstructions = true;
                        return true;
                    }
                } else if (gameOver) {
                    // REBOOT SYSTEM butonu
                    float rebootY = centerY + screenHeight * 0.05f;
                    if (Math.abs(touchY - rebootY) < screenHeight * 0.05f) {
                        gameOver = false;
                        score = 0;
                        initLevel(1);
                        return true;
                    }

                    // HALL OF FAME butonu
                    float hallOfFameY = centerY + screenHeight * 0.15f;
                    if (Math.abs(touchY - hallOfFameY) < screenHeight * 0.05f) {
                        showHighScore = true;
                        return true;
                    }

                    // MAIN MENU butonu
                    float mainMenuY = centerY + screenHeight * 0.25f;
                    if (Math.abs(touchY - mainMenuY) < screenHeight * 0.05f) {
                        gameOver = false;
                        gameStarted = false;
                        score = 0;
                        initLevel(1);
                        return true;
                    }
                } else {
                    // Hangi topa dokunuldu?
                    Ball touchedBall = null;
                    float minDist = Float.MAX_VALUE;

                    for (Ball ball : cloneBalls) {
                        float dx = touchX - ball.x;
                        float dy = touchY - ball.y;
                        float distance = (float) Math.sqrt(dx * dx + dy * dy);
                        if (distance < ball.radius * 3 && distance < minDist) {
                            minDist = distance;
                            touchedBall = ball;
                        }
                    }

                    float dx = touchX - whiteBall.x;
                    float dy = touchY - whiteBall.y;
                    float distance = (float) Math.sqrt(dx * dx + dy * dy);
                    if (distance < whiteBall.radius * 3 && distance < minDist) {
                        touchedBall = whiteBall;
                    }

                    if (touchedBall != null) {
                        isDragging = true;
                        draggedBall = touchedBall;
                        dragStartX = touchedBall.x;
                        dragStartY = touchedBall.y;
                        dragStartTime = System.currentTimeMillis();
                        touchedBall.vx = 0;
                        touchedBall.vy = 0;
                    }
                }
                break;

            case MotionEvent.ACTION_MOVE:
                if (isDragging && draggedBall != null) {
                    float dx = touchX - dragStartX;
                    float dy = touchY - dragStartY;
                    float distance = (float) Math.sqrt(dx * dx + dy * dy);

                    if (distance > MAX_DRAG_DISTANCE) {
                        float angle = (float) Math.atan2(dy, dx);
                        draggedBall.x = dragStartX + (float) Math.cos(angle) * MAX_DRAG_DISTANCE;
                        draggedBall.y = dragStartY + (float) Math.sin(angle) * MAX_DRAG_DISTANCE;
                    } else {
                        draggedBall.x = touchX;
                        draggedBall.y = touchY;
                    }
                }
                break;

            case MotionEvent.ACTION_UP:
                if (isDragging && draggedBall != null) {
                    float dx = draggedBall.x - dragStartX;
                    float dy = draggedBall.y - dragStartY;
                    float distance = Math.min((float) Math.sqrt(dx * dx + dy * dy), MAX_DRAG_DISTANCE);
                    float ratio = distance / MAX_DRAG_DISTANCE;
                    lastLaunchPower = ratio;
                    float maxSpeed = powerBoostActive ? 60 : 30;
                    float speed = ratio * maxSpeed;

                    if (distance > 5) {
                        draggedBall.vx = -dx / distance * speed;
                        draggedBall.vy = -dy / distance * speed;
                        playSound(soundLaunch);
                    }

                    isDragging = false;
                    draggedBall = null;
                    powerBoostActive = false;
                    comboCounter = 0;
                }
                break;
        }

        return true;
    }

    public void pause() {
        isPlaying = false;
        try {
            if (gameThread != null) {
                gameThread.join();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void resume() {
        isPlaying = true;
        gameThread = new Thread(this);
        gameThread.start();
    }

    // İç sınıflar
    class Ball {
        float x, y, vx, vy, radius;
        int color;
        long creationTime;

        Ball(float x, float y, float radius, int color) {
            this.x = x;
            this.y = y;
            this.radius = radius;
            this.color = color;
            this.vx = 0;
            this.vy = 0;
            this.creationTime = System.currentTimeMillis();
        }
    }

    class SpecialBall {
        float x, y, vx, vy, radius;
        String type;

        SpecialBall(float x, float y, float radius, String type) {
            this.x = x;
            this.y = y;
            this.radius = radius;
            this.type = type;
            this.vx = (random.nextFloat() - 0.5f) * 3;
            this.vy = (random.nextFloat() - 0.5f) * 3;
        }

        int getColor() {
            switch (type) {
                case "blackhole":
                    return Color.rgb(128, 0, 128);
                case "extraTime":
                    return Color.rgb(255, 165, 0);
                case "powerBoost":
                    return Color.rgb(255, 215, 0);
                case "barrier":
                    return Color.BLUE;
                case "electric":
                    return Color.CYAN;
                case "clone":
                    return Color.rgb(255, 192, 203);
                case "freeze":
                    return Color.rgb(173, 216, 230);
                case "missile":
                    return Color.RED;
                case "teleport":
                    return Color.GREEN;
                case "boom":
                    return Color.rgb(139, 0, 0);
                case "ghost":
                    return Color.rgb(211, 211, 211);
                default:
                    return Color.MAGENTA;
            }
        }

        String getLetter() {
            switch (type) {
                case "blackhole":
                    return "B";
                case "extraTime":
                    return "T";
                case "powerBoost":
                    return "P";
                case "barrier":
                    return "S";
                case "electric":
                    return "L";
                case "clone":
                    return "C";
                case "freeze":
                    return "F";
                case "missile":
                    return "M";
                case "teleport":
                    return "T";
                case "boom":
                    return "X";
                case "ghost":
                    return "G";
                default:
                    return "?";
            }
        }
    }

    private void saveProgress() {
        SharedPreferences prefs = getContext().getSharedPreferences("SpaceBilliard", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        if (score > highScore) {
            highScore = score;
            editor.putInt("highScore", highScore);
        }
        if (level > highLevel) {
            highLevel = level;
            editor.putInt("highLevel", highLevel);
        }

        // Level kilitlerini aç (SADECE level sayısını kaydet)
        // maxUnlockedLevel = açık olan level sayısı (1, 2, 3...)
        int currentLevelNumber = ((level - 1) / 10) + 1; // Hangi level'dayız?
        int currentStageInLevel = ((level - 1) % 10) + 1; // Level içinde hangi stage'deyiz?

        // Sadece bir level'ın son stage'ini tamamladıysak bir sonraki level'ı aç
        if (currentStageInLevel == 10 && currentLevelNumber >= maxUnlockedLevel) {
            maxUnlockedLevel = currentLevelNumber + 1;
            editor.putInt("maxUnlockedLevel", maxUnlockedLevel);
        }

        editor.putInt("maxCombo", maxCombo);
        editor.apply();
    }

    private void drawLevelSelector(Canvas canvas) {
        // Arka planı karart
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.argb(200, 0, 0, 0));
        canvas.drawRect(0, 0, screenWidth, screenHeight, paint);

        // Ana panel (Glassmorphism)
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.argb(160, 30, 30, 60)); // Koyu mavi-mor
        float panelWidth = screenWidth * 0.9f;
        float panelHeight = screenHeight * 0.75f;
        canvas.drawRoundRect(
                centerX - panelWidth / 2, screenHeight * 0.15f,
                centerX + panelWidth / 2, screenHeight * 0.15f + panelHeight,
                40, 40, paint);

        // Panel kenarlığı (Neon glow)
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(4);
        paint.setColor(Color.rgb(0, 243, 255));
        paint.setShadowLayer(20, 0, 0, Color.CYAN);
        canvas.drawRoundRect(
                centerX - panelWidth / 2, screenHeight * 0.15f,
                centerX + panelWidth / 2, screenHeight * 0.15f + panelHeight,
                40, 40, paint);
        paint.clearShadowLayer();

        // Başlık
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(screenWidth * 0.1f);
        paint.setColor(Color.rgb(0, 243, 255));
        paint.setShadowLayer(20, 0, 0, Color.CYAN);
        canvas.drawText("SELECT LEVEL", centerX, screenHeight * 0.2f, paint);
        paint.clearShadowLayer();

        // Sayfa Göstergesi (Örn: WORLD 1)
        paint.setTextSize(screenWidth * 0.05f);
        paint.setColor(Color.WHITE);
        canvas.drawText("WORLD " + selectorPage, centerX, screenHeight * 0.28f, paint);

        // Sayfa bilgisi (Her sayfa bir level grubu temsil eder)
        paint.setTextSize(screenWidth * 0.04f);
        paint.setColor(Color.LTGRAY);
        canvas.drawText("Each level contains 10 stages", centerX, screenHeight * 0.32f, paint);

        // Grid (Panel içinde ortalanmış)
        float totalGridWidth = 5 * screenWidth * 0.14f + 4 * screenWidth * 0.02f; // 5 kutu + 4 boşluk
        float gridStartX = centerX - totalGridWidth / 2;
        float gridStartY = screenHeight * 0.42f;
        float cellWidth = screenWidth * 0.14f;
        float cellHeight = screenWidth * 0.14f;
        float gap = screenWidth * 0.02f;

        for (int i = 0; i < 10; i++) {
            int lvNum = (selectorPage - 1) * 10 + i + 1;
            int row = i / 5;
            int col = i % 5;

            float btnX = gridStartX + col * (cellWidth + gap) + cellWidth / 2;
            float btnY = gridStartY + row * (cellHeight + gap) + cellHeight / 2;

            // Kutu
            paint.setStyle(Paint.Style.FILL);
            if (lvNum <= maxUnlockedLevel) {
                paint.setColor(Color.rgb(200, 0, 50)); // Kırmızı buton (unlocked)
            } else {
                paint.setColor(Color.rgb(100, 100, 100)); // Gri (locked)
            }

            // Neon Stroke
            paint.setStyle(Paint.Style.FILL);
            canvas.drawRoundRect(btnX - cellWidth / 2, btnY - cellHeight / 2, btnX + cellWidth / 2,
                    btnY + cellHeight / 2, 20, 20, paint);

            // Stroke
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(5);
            paint.setColor(Color.WHITE);
            canvas.drawRoundRect(btnX - cellWidth / 2, btnY - cellHeight / 2, btnX + cellWidth / 2,
                    btnY + cellHeight / 2, 20, 20, paint);

            // Metin veya Kilit
            paint.setStyle(Paint.Style.FILL);
            paint.setTextAlign(Paint.Align.CENTER);
            if (lvNum <= maxUnlockedLevel) {
                paint.setColor(Color.WHITE);
                paint.setTextSize(cellHeight * 0.5f);
                // Y offset biraz ayarlandı
                canvas.drawText(String.valueOf(lvNum), btnX, btnY + cellHeight * 0.2f, paint);

                // Yıldızlar (Basit mantık: 3 yıldız görsel)
                paint.setTextSize(cellHeight * 0.2f);
                paint.setColor(Color.YELLOW);
                canvas.drawText("★★★", btnX, btnY - cellHeight * 0.25f, paint);
            } else {
                // Kilit simgesi (Basitçe "L") veya renk
                paint.setColor(Color.LTGRAY);
                paint.setTextSize(cellHeight * 0.4f);
                canvas.drawText("🔒", btnX, btnY + cellHeight * 0.15f, paint);
            }
        }

        // Oklar
        paint.setTextSize(screenWidth * 0.1f);
        paint.setColor(Color.CYAN);
        if (selectorPage > 1)
            canvas.drawText("◄", screenWidth * 0.08f, centerY, paint);

        canvas.drawText("►", screenWidth * 0.92f, centerY, paint);

        // Back Butonu (Biraz yukarıda)
        paint.setTextSize(screenWidth * 0.05f);
        paint.setColor(Color.RED);
        canvas.drawText("BACK", centerX, screenHeight * 0.85f, paint);
    }

    class Particle {
        float x, y, vx, vy;
        int color, life = 30;

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
            life--;
        }

        boolean isDead() {
            return life <= 0;
        }

        void draw(Canvas canvas, Paint paint) {
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(color);
            paint.setAlpha((int) (255 * (life / 30f)));
            canvas.drawCircle(x, y, 4, paint);
            paint.setAlpha(255);
        }
    }

    class GuidedMissile {
        float x, y, vx, vy, radius = 8, speed = 4;
        Ball target;

        GuidedMissile(float x, float y, Ball target) {
            this.x = x;
            this.y = y;
            this.target = target;
        }

        void update() {
            float dx = target.x - x;
            float dy = target.y - y;
            float distance = (float) Math.sqrt(dx * dx + dy * dy);

            if (distance > speed) {
                vx = (dx / distance) * speed;
                vy = (dy / distance) * speed;
                x += vx;
                y += vy;
            } else {
                x = target.x;
                y = target.y;
            }

            speed = Math.min(speed + 0.5f, 10);
        }

        void draw(Canvas canvas, Paint paint) {
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.WHITE);
            paint.setShadowLayer(15, 0, 0, Color.RED);
            canvas.drawCircle(x, y, radius, paint);
            paint.clearShadowLayer();
        }
    }

    class ElectricEffect {
        float x1, y1, x2, y2;
        int life = 30;

        ElectricEffect(float x1, float y1, float x2, float y2) {
            this.x1 = x1;
            this.y1 = y1;
            this.x2 = x2;
            this.y2 = y2;
        }

        void update() {
            life--;
        }

        boolean isDead() {
            return life <= 0;
        }

        void draw(Canvas canvas, Paint paint) {
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(3);
            paint.setColor(Color.CYAN);
            paint.setAlpha((int) (255 * (life / 30f)));

            // Zigzag çizgi
            float midX = (x1 + x2) / 2 + (random.nextFloat() - 0.5f) * 20;
            float midY = (y1 + y2) / 2 + (random.nextFloat() - 0.5f) * 20;

            canvas.drawLine(x1, y1, midX, midY, paint);
            canvas.drawLine(midX, midY, x2, y2, paint);

            paint.setAlpha(255);
        }
    }

    class BlastWave {
        float x, y, radius = 0, maxRadius;
        int life = 60;

        BlastWave(float x, float y) {
            this.x = x;
            this.y = y;
            this.maxRadius = whiteBall.radius * 30;
        }

        void update() {
            radius = maxRadius * (1 - life / 60f);
            life--;

            // Çevredeki topları dalga yayıldıkça yok et
            for (int i = coloredBalls.size() - 1; i >= 0; i--) {
                Ball ball = coloredBalls.get(i);
                float dx = x - ball.x;
                float dy = y - ball.y;
                float distance = (float) Math.sqrt(dx * dx + dy * dy);
                if (distance < radius) {
                    score += 2;
                    createParticles(ball.x, ball.y, ball.color);
                    coloredBalls.remove(i);
                    playSound(soundCollision);
                }
            }

            for (int i = blackBalls.size() - 1; i >= 0; i--) {
                Ball ball = blackBalls.get(i);
                float dx = x - ball.x;
                float dy = y - ball.y;
                float distance = (float) Math.sqrt(dx * dx + dy * dy);
                if (distance < radius) {
                    score += 5;
                    createParticles(ball.x, ball.y, Color.BLACK);
                    blackBalls.remove(i);
                    playSound(soundCollision); // Siyah top patlasa da standart çarpışma sesi (kullanıcı isteği)
                }
            }
        }

        boolean isDead() {
            return life <= 0;
        }

        void draw(Canvas canvas, Paint paint) {
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(8 * (life / 60f));
            paint.setColor(Color.rgb(255, 69, 0));
            paint.setAlpha((int) (255 * (life / 60f)));
            paint.setShadowLayer(20, 0, 0, Color.RED);
            canvas.drawCircle(x, y, radius, paint);
            paint.clearShadowLayer();
            paint.setAlpha(255);
        }
    }

    // Yıldız sınıfı
    private class Star {
        float x, y;
        float vx, vy;
        float size;
        int alpha;

        Star() {
            Random r = new Random();
            x = r.nextFloat() * 10000;
            y = r.nextFloat() * 10000;
            vx = (r.nextFloat() - 0.5f) * 2;
            vy = (r.nextFloat() - 0.5f) * 2;
            size = r.nextFloat() * 3 + 1;
            alpha = r.nextInt(155) + 100;
        }

        void update(int screenW, int screenH) {
            x += vx;
            y += vy;

            if (x < 0)
                x = screenW;
            if (x > screenW)
                x = 0;
            if (y < 0)
                y = screenH;
            if (y > screenH)
                y = 0;
        }

        void draw(Canvas canvas, Paint paint) {
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.WHITE);
            paint.setAlpha(alpha);
            canvas.drawCircle(x, y, size, paint);
            paint.setAlpha(255);
        }
    }
}

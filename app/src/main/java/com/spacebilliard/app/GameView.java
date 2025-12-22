package com.spacebilliard.app;

import com.spacebilliard.app.MainActivity;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import java.util.ArrayList;
import java.util.Iterator;
import android.media.AudioAttributes;
import android.media.SoundPool;
import java.util.Random;
import android.graphics.Typeface;
import android.content.SharedPreferences;
import android.graphics.PointF;
import android.graphics.Path;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.RectF;

public class GameView extends SurfaceView implements Runnable {

    private Thread gameThread;
    private SurfaceHolder holder;
    private boolean isPlaying;
    private Canvas canvas;
    private Paint paint;
    private Shader nebula1, nebula2, nebula3; // Cached shaders for performance
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
    private ArrayList<ImpactArc> impactArcs;
    private ArrayList<GuidedMissile> missiles;
    private ArrayList<ElectricEffect> electricEffects;
    private ArrayList<Star> stars; // Static background stars
    private ArrayList<Comet> comets; // Background comets for Space 2

    // Oyun durumu
    private boolean gameStarted = false;
    private boolean gameOver = false;
    private boolean gameCompleted = false;
    private int score = 0;
    private int lives = 3;
    private Bitmap menuBgBitmap;
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
    private int maxCombo = 0; // Combo rekoru
    private ArrayList<FloatingText> floatingTexts; // Animasyonlu metinler için liste

    // Stage Cleared animasyonu
    private boolean showStageCleared = false;
    private long stageClearedTime = 0;

    // Elektrik topu ikinci sıçrama için
    private boolean electricSecondBounce = false;
    private long electricSecondBounceTime = 0;
    private float electricFirstTargetX = 0;
    private float electricFirstTargetY = 0;

    // Level Seçici
    private boolean showLevelSelector = false;
    private int selectorPage = 1;
    private int maxUnlockedLevel = 1;

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

    private int coins = 0;

    // Level geçiş beklemesi
    private boolean levelCompleted = false;

    private long levelCompletionTime = 0;

    // Ses Efektleri
    private SoundPool soundPool;
    private int soundLaunch, soundCollision, soundCoin, soundBlackExplosion, soundElectric, soundFreeze, soundGameOver,
            soundMissile, soundPower, soundShield;
    private boolean soundLoaded = false;

    // MainActivity reference for updating UI panels
    private MainActivity mainActivity;
    private android.view.View startBtn, howToBtn, shopBtn, hallOfFameBtn;
    private android.graphics.Rect startBtnBounds, howToBtnBounds, shopBtnBounds;

    public void setMenuButtons(android.view.View start, android.view.View howTo, android.view.View shop,
            android.view.View hallOfFame) {
        this.startBtn = start;
        this.howToBtn = howTo;
        this.shopBtn = shop;
        this.hallOfFameBtn = hallOfFame;
    }

    public void reloadPreferences() {
        SharedPreferences prefs = getContext().getSharedPreferences("SpaceBilliard", Context.MODE_PRIVATE);
        selectedSkin = prefs.getString("selectedSkin", "default");
        selectedTrail = prefs.getString("selectedTrail", "none");
        selectedAura = prefs.getString("selectedAura", "none");
        selectedTrajectory = prefs.getString("selectedTrajectory", "dashed");
        selectedImpact = prefs.getString("selectedImpact", "classic");
    }

    public void startGame() {
        reloadPreferences(); // Ensure prefs are fresh when game starts
        showLevelSelector = true;
        updateMenuButtonsVisibility();
    }

    public void showInstructions() {
        showInstructions = true;
        updateMenuButtonsVisibility();
    }

    public void showHighScore() {
        showHighScore = true;
        updateMenuButtonsVisibility();
    }

    private String selectedSkin = "default";
    private String selectedTrail = "none";
    private String selectedAura = "none";
    private String selectedTrajectory = "dashed";
    private String selectedImpact = "classic";
    private final int MAX_TRAIL_POINTS = 15;

    private void updateMenuButtonsVisibility() {
        if (mainActivity != null) {
            mainActivity.runOnUiThread(() -> {
                boolean show = !gameStarted && !showLevelSelector && !showInstructions && !showHighScore
                        && !gameCompleted;
                if (startBtn != null)
                    startBtn.setVisibility(show ? View.VISIBLE : View.GONE);
                if (howToBtn != null)
                    howToBtn.setVisibility(show ? View.VISIBLE : View.GONE);
                if (shopBtn != null)
                    shopBtn.setVisibility(show ? View.VISIBLE : View.GONE);
                if (hallOfFameBtn != null)
                    hallOfFameBtn.setVisibility(show ? View.VISIBLE : View.GONE);
            });
        }
    }

    public GameView(Context context) {
        super(context);
        if (context instanceof MainActivity) {
            mainActivity = (MainActivity) context;
        }
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
        impactArcs = new ArrayList<>();
        missiles = new ArrayList<>();
        electricEffects = new ArrayList<>();
        electricEffects = new ArrayList<>();
        stars = new ArrayList<>();
        floatingTexts = new ArrayList<>();

        cachedXfermode = new android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.SRC_ATOP);

        // Kayıtlı skoru yükle
        SharedPreferences prefs = context.getSharedPreferences("SpaceBilliard", Context.MODE_PRIVATE);
        highScore = prefs.getInt("highScore", 0);
        highLevel = prefs.getInt("highLevel", 1);
        highScore = prefs.getInt("highScore", 0);
        highLevel = prefs.getInt("highLevel", 1);
        maxCombo = prefs.getInt("maxCombo", 0);
        // maxUnlockedLevel'ı yükle (ilerlemeyi kaydet)
        maxUnlockedLevel = prefs.getInt("maxUnlockedLevel", 1);
        // Coinleri yükle
        coins = prefs.getInt("coins", 0);

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

        // Kuyruklu Yıldızları oluştur (Space 2 için)
        comets = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            comets.add(new Comet());
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
            updatePositionsAfterResize();
        }

        // Initialize Simple Background Shader (High Performance)
        nebula1 = new RadialGradient(centerX, centerY, Math.max(screenWidth, screenHeight),
                new int[] { Color.rgb(30, 10, 50), Color.rgb(5, 5, 10) }, null, Shader.TileMode.CLAMP);

        initGiantMeteorGraphics();
    }

    // Optimization Fields
    private android.graphics.Path cachedMeteorPath;
    private android.graphics.Path cachedMeteorCracks;
    private RadialGradient cachedMeteorAura;
    private RadialGradient cachedMeteorShading;
    private android.graphics.PorterDuffXfermode cachedXfermode;

    private void initGiantMeteorGraphics() {
        float meteorRadius = screenWidth * 0.18f;
        float meteorX = screenWidth * 0.85f;
        float meteorY = screenHeight * 0.85f;

        cachedMeteorPath = new android.graphics.Path();
        float[] irregularOffsets = { 1.0f, 0.9f, 1.05f, 0.95f, 1.1f, 0.9f, 1.0f, 0.85f, 1.05f, 0.95f, 1.0f, 1.1f };
        for (int i = 0; i < 12; i++) {
            float theta = (float) (i * 2 * Math.PI / 12);
            float r = meteorRadius * irregularOffsets[i % irregularOffsets.length];
            float px = meteorX + (float) Math.cos(theta) * r;
            float py = meteorY + (float) Math.sin(theta) * r;
            if (i == 0)
                cachedMeteorPath.moveTo(px, py);
            else
                cachedMeteorPath.lineTo(px, py);
        }
        cachedMeteorPath.close();

        cachedMeteorCracks = new android.graphics.Path();
        cachedMeteorCracks.moveTo(meteorX - meteorRadius * 0.5f, meteorY + meteorRadius * 0.1f);
        cachedMeteorCracks.lineTo(meteorX - meteorRadius * 0.2f, meteorY - meteorRadius * 0.1f);
        cachedMeteorCracks.lineTo(meteorX + meteorRadius * 0.3f, meteorY);
        cachedMeteorCracks.lineTo(meteorX + meteorRadius * 0.5f, meteorY + meteorRadius * 0.4f);

        cachedMeteorCracks.moveTo(meteorX + meteorRadius * 0.1f, meteorY - meteorRadius * 0.6f);
        cachedMeteorCracks.lineTo(meteorX, meteorY - meteorRadius * 0.3f);
        cachedMeteorCracks.lineTo(meteorX - meteorRadius * 0.1f, meteorY - meteorRadius * 0.1f);

        cachedMeteorAura = new RadialGradient(meteorX, meteorY, meteorRadius * 1.6f,
                new int[] { Color.argb(100, 255, 69, 0), Color.TRANSPARENT },
                null, Shader.TileMode.CLAMP);

        cachedMeteorShading = new RadialGradient(meteorX - meteorRadius * 0.3f, meteorY - meteorRadius * 0.3f,
                meteorRadius * 1.5f,
                Color.argb(0, 0, 0, 0), Color.argb(200, 0, 0, 0), Shader.TileMode.CLAMP);
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
        missiles.clear();
        electricEffects.clear();
        floatingTexts.clear();

        whiteBall.x = centerX;
        whiteBall.y = centerY;
        whiteBall.vx = 0;
        whiteBall.vy = 0;
        whiteBall.trail.clear();
        // Ghost modundan çıkarken top boyutunu sıfırla
        whiteBall.radius = (circleRadius / 0.47f) * 0.02f;

        blackHoleActive = false;
        barrierActive = false;
        freezeActive = false;
        ghostModeActive = false;
        powerBoostActive = false;
        blastWave = null;

        // Her level (5 stage) için top sayısını sıfırla ve yeniden başlat
        int stageInLevel = ((lv - 1) % 5) + 1;
        int ballCount = stageInLevel + 4; // Her level 5 topla başlar, her stage +1 artar

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

        // Siyah toplar (Level içindeki stage'e göre artar)
        int blackCount = (stageInLevel >= 4) ? 2 : 1;
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
            // Target ~144 FPS (approx 7ms)
            if (frameTime < 7) {
                try {
                    Thread.sleep(7 - frameTime);
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
            updateMenuButtonsVisibility();
            return;
        }

        // Tüm toplar toplandı mı?
        if (coloredBalls.size() == 0 && !levelCompleted) {
            levelCompleted = true;
            showStageCleared = true;
            stageClearedTime = System.currentTimeMillis();

            // Her stage tamamlandığında 5 coin kazan
            coins += 5;
            saveProgress(); // Coin'i kaydet

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

            // Her 5 stage tamamlandığında yeni level aç
            int completedStages = level - 1; // Tamamlanan stage sayısı
            int unlockedLevelCount = (completedStages / 5) + 1; // Açılması gereken level sayısı

            if (unlockedLevelCount > maxUnlockedLevel) {
                maxUnlockedLevel = unlockedLevelCount;
                saveProgress();

                // New Space Unlocked? (Level 11, 21, etc.)
                if (maxUnlockedLevel % 10 == 1 && maxUnlockedLevel > 1 && maxUnlockedLevel <= 100) {
                    gameStarted = false;
                    showLevelSelector = true;
                    selectorPage = (maxUnlockedLevel - 1) / 10 + 1;
                    // Stop here, don't init next level immediately
                    return;
                }
            }

            // Check for Game Completion (Level > 500 stages = 100 Levels)
            if (level > 500) {
                gameCompleted = true;
                gameStarted = false;
                updateMenuButtonsVisibility();
                return;
            }

            initLevel(level);
            return;
        }

        // Elektrik topu ikinci sıçrama kontrolü
        if (electricSecondBounce && System.currentTimeMillis() >= electricSecondBounceTime) {
            electricSecondBounce = false;
            if (coloredBalls.size() > 0) {
                Ball target2 = coloredBalls.get(random.nextInt(coloredBalls.size()));
                electricEffects
                        .add(new ElectricEffect(electricFirstTargetX, electricFirstTargetY, target2.x, target2.y));
                createImpactBurst(target2.x, target2.y, target2.color);
                score++;
                comboCounter++;
                playSound(soundElectric); // İkinci sıçrama sesi
                coloredBalls.remove(target2);
            }
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

            // Trail update
            updateBallTrail(whiteBall);
        }

        // Clone toplar
        for (int i = cloneBalls.size() - 1; i >= 0; i--) {
            Ball ball = cloneBalls.get(i);

            // Clone topu için ömür kontrolü
            if (ball.isClone && ball.lifetime > 0) {
                long elapsed = System.currentTimeMillis() - ball.creationTime;
                if (elapsed > ball.lifetime) {
                    cloneBalls.remove(i);
                    continue;
                }
            } else if (System.currentTimeMillis() - ball.creationTime > 8000) {
                // Eski clone toplar için (lifetime olmayan)
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

        for (int i = particles.size() - 1; i >= 0; i--) {
            Particle p = particles.get(i);
            p.update();
            if (p.isDead())
                particles.remove(i);
        }

        for (int i = impactArcs.size() - 1; i >= 0; i--) {
            ImpactArc arc = impactArcs.get(i);
            arc.update();
            if (arc.isDead())
                impactArcs.remove(i);
        }

        // Floating texts update
        for (int i = floatingTexts.size() - 1; i >= 0; i--) {
            FloatingText ft = floatingTexts.get(i);
            ft.update();
            if (ft.isDead())
                floatingTexts.remove(i);
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

        // Combo Fire Effekt
        if (comboCounter >= 3 && (Math.abs(whiteBall.vx) > 1 || Math.abs(whiteBall.vy) > 1)) {
            createFlame(whiteBall.x, whiteBall.y);
        }

        // Combo timeout kontrolü (Eğer başka bir yerde yapılmıyorsa)
        if (comboCounter > 0 && System.currentTimeMillis() - lastHitTime > COMBO_TIMEOUT) {
            comboCounter = 0;
        }
    }

    private void updateBallTrail(Ball ball) {
        if (!selectedTrail.equals("none") && (Math.abs(ball.vx) > 0.1 || Math.abs(ball.vy) > 0.1)) {
            ball.trail.add(0, new TrailPoint(ball.x, ball.y, ball.radius));
            if (ball.trail.size() > MAX_TRAIL_POINTS) {
                ball.trail.remove(ball.trail.size() - 1);
            }
        } else {
            if (ball.trail.size() > 0)
                ball.trail.remove(ball.trail.size() - 1);
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
        try {
            allWhiteBalls.addAll(cloneBalls);
        } catch (Exception e) {
            // ConcurrentModificationException önleme
        }

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
                        if (comboHits >= 3) {
                            // Yeni animasyonlu combo yazısı ekle
                            floatingTexts.add(new FloatingText("COMBO x" + (comboHits), centerX,
                                    centerY - screenHeight * 0.15f, Color.rgb(255, 215, 0)));
                        }
                    } else {
                        comboHits = 1;
                    }
                    lastHitTime = currentTime;

                    createImpactBurst(ball.x, ball.y, ball.color);
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

            // Siyah toplar (güvenli iterasyon)
            for (int i = blackBalls.size() - 1; i >= 0; i--) {
                if (i >= blackBalls.size())
                    continue; // Güvenlik kontrolü
                Ball ball = blackBalls.get(i);

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

                        // Siyah top patlama efekti (partikül)
                        createParticles(ball.x, ball.y, Color.BLACK);

                        // Kamera sallanma efekti
                        shakeEndTime = System.currentTimeMillis() + 500;

                        if (lives <= 0) {
                            gameOver = true;
                            saveProgress();
                            playSound(soundGameOver);
                            updateMenuButtonsVisibility();
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
                // Ghost mode aktifse orijinal boyutu kullan
                float cloneRadius = ghostModeActive ? originalWhiteBallRadius : whiteBall.radius;
                Ball clone = new Ball(centerX, centerY, cloneRadius, Color.WHITE, 5000); // 5 saniye ömür
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
        createImpactBurst(target1.x, target1.y, target1.color);
        score++;
        comboCounter++;
        playSound(soundElectric); // İlk sıçrama sesi
        coloredBalls.remove(target1);

        // İkinci hedef için gecikme ayarla (0.4 saniye)
        if (coloredBalls.size() > 0) {
            electricFirstTargetX = target1.x;
            electricFirstTargetY = target1.y;
            electricSecondBounce = true;
            electricSecondBounceTime = System.currentTimeMillis() + 400;
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

    private void createFlame(float x, float y) {
        // Spawn 1-2 flame particles each frame for continuous effect
        for (int i = 0; i < 2; i++) {
            particles.add(new Particle(x, y, 0, 0, Color.YELLOW, ParticleType.FLAME));
        }
    }

    private void createParticles(float x, float y, int color) {
        for (int i = 0; i < 15; i++) {
            float angle = random.nextFloat() * (float) (2 * Math.PI);
            float speed = random.nextFloat() * 5 + 2;
            particles.add(new Particle(x, y, angle, speed, color));
        }
    }

    private void createImpactBurst(float x, float y, int color) {
        // Standard Circle Particles (Common base)
        int particleCount = 10;
        for (int i = 0; i < particleCount; i++) {
            float angle = random.nextFloat() * (float) (2 * Math.PI);
            float speed = random.nextFloat() * 6 + 2;
            particles.add(new Particle(x, y, angle, speed, color, ParticleType.CIRCLE));
        }

        switch (selectedImpact) {
            case "stars":
                // Star Burst: Circles + Stars
                for (int i = 0; i < 10; i++) {
                    float angle = random.nextFloat() * (float) (2 * Math.PI);
                    float speed = random.nextFloat() * 4 + 3;
                    particles.add(new Particle(x, y, angle, speed, color, ParticleType.STAR));
                }
                break;
            case "electric":
                // Electric Boom: Circles + Lightning Arcs
                for (int i = 0; i < 6; i++) {
                    float angle = random.nextFloat() * (float) (2 * Math.PI);
                    float length = random.nextFloat() * 40 + 30;
                    impactArcs.add(new ImpactArc(x, y, angle, length, color));
                }
                break;
            case "ripple":
                particles.add(new Particle(x, y, 0, 0, color, ParticleType.RIPPLE));
                break;
            case "confetti":
                for (int i = 0; i < 15; i++) {
                    float angle = random.nextFloat() * (float) (2 * Math.PI);
                    float speed = random.nextFloat() * 5 + 3;
                    int confettiColor = Color.rgb(random.nextInt(256), random.nextInt(256), random.nextInt(256));
                    particles.add(new Particle(x, y, angle, speed, confettiColor, ParticleType.CONFETTI));
                }
                break;
            case "vortex":
                for (int i = 0; i < 25; i++) {
                    float angle = (float) (random.nextFloat() * Math.PI * 2);
                    float speed = random.nextFloat() * 10 + 5;
                    int pColor = Color.rgb(100 + random.nextInt(155), 0, 255); // Purple nuances
                    particles.add(new Particle(x, y, angle, speed, pColor));
                }
                break;
            case "shatter":
                for (int i = 0; i < 20; i++) {
                    float angle = (float) (random.nextFloat() * Math.PI * 2);
                    float speed = random.nextFloat() * 20 + 10;
                    int pColor = Color.rgb(200 + random.nextInt(55), 200 + random.nextInt(55), 255); // White/Blueish
                    particles.add(new Particle(x, y, angle, speed, pColor));
                }
                break;
            default:
                // Classic: Just circles (already added)
                break;
        }
    }

    private void draw() {
        if (holder.getSurface().isValid()) {
            canvas = holder.lockCanvas();

            // Kamera Offset
            canvas.save();
            canvas.translate(cameraShakeX, cameraShakeY);

            // Arka plan - 10 Space progression
            int currentSpace = ((level - 1) / 50) + 1;

            // Cycle backgrounds for Spaces 1-10
            switch (currentSpace) {
                case 1:
                case 5:
                case 8:
                    // Space 1, 5, 8: Dark + Stars
                    canvas.drawColor(Color.rgb(5, 5, 16));
                    for (Star star : stars) {
                        star.draw(canvas, paint);
                    }
                    break;

                case 2:
                case 6:
                case 9:
                    // Space 2, 6, 9: Dark + Moon + Comets
                    canvas.drawColor(Color.rgb(10, 5, 20)); // Deep dark
                    drawMoon(canvas);
                    for (Comet c : comets) {
                        c.update(screenWidth, screenHeight);
                        c.draw(canvas, paint);
                    }
                    break;

                case 3:
                case 4:
                case 10:
                    // Space 3, 4, 10: Reddish Dark + Giant Meteor + Comets
                    canvas.drawColor(Color.rgb(25, 5, 10)); // Reddish dark background
                    drawGiantMeteor(canvas);
                    for (Comet c : comets) {
                        c.update(screenWidth, screenHeight);
                        c.draw(canvas, paint);
                    }
                    break;

                case 7:
                    // Space 7: Static Aurora (no animation)
                    drawAuroraNebula(canvas);
                    for (Star star : stars) {
                        star.update(screenWidth, screenHeight);
                        star.draw(canvas, paint);
                    }
                    break;

                default:
                    // Fallback to stars for any space beyond 10
                    canvas.drawColor(Color.rgb(5, 5, 16));
                    for (Star star : stars) {
                        star.draw(canvas, paint);
                    }
                    break;
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

            for (ImpactArc arc : impactArcs) {
                arc.draw(canvas, paint);
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

                // Clone topu için geri sayım göster
                if (ball.isClone && ball.lifetime > 0) {
                    long elapsed = System.currentTimeMillis() - ball.creationTime;
                    long remaining = ball.lifetime - elapsed;
                    int seconds = (int) Math.ceil(remaining / 1000.0);

                    if (seconds > 0 && seconds <= 5) {
                        paint.setStyle(Paint.Style.FILL);
                        paint.setTextSize(ball.radius * 1.2f);
                        paint.setTextAlign(Paint.Align.CENTER);
                        paint.setColor(Color.WHITE);
                        paint.setShadowLayer(8, 0, 0, Color.BLACK);
                        canvas.drawText(String.valueOf(seconds), ball.x + ball.radius * 0.8f,
                                ball.y - ball.radius * 0.8f, paint);
                        paint.clearShadowLayer();
                    }
                }
            }

            // Missiles
            for (GuidedMissile missile : missiles) {
                missile.draw(canvas, paint);
            }

            // Trail (Only for player ball)
            if (!selectedTrail.equals("none") && whiteBall.trail.size() > 0) {
                drawCometTrail(canvas, whiteBall);
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
            Ball currentDraggedBall = draggedBall; // Race condition önleme
            if (isDragging && currentDraggedBall != null) {
                float dx = currentDraggedBall.x - dragStartX;
                float dy = currentDraggedBall.y - dragStartY;
                float distance = Math.min((float) Math.sqrt(dx * dx + dy * dy), MAX_DRAG_DISTANCE);
                float ratio = distance / MAX_DRAG_DISTANCE;

                // Eski çizgi (drag line) yerine nişan çizgisi (trajectory)
                if (distance > 10) {
                    float launchAngle = (float) Math.atan2(-dy, -dx);

                    // Çizgi ve Ok parametreleri
                    float startDist = currentDraggedBall.radius * 1.5f;
                    float lineLen = 400 * ratio; // Güç arttıkça uzayan çizgi

                    float startX = currentDraggedBall.x + (float) Math.cos(launchAngle) * startDist;
                    float startY = currentDraggedBall.y + (float) Math.sin(launchAngle) * startDist;
                    float endX = currentDraggedBall.x + (float) Math.cos(launchAngle) * (startDist + lineLen);
                    float endY = currentDraggedBall.y + (float) Math.sin(launchAngle) * (startDist + lineLen);

                    // Visual end point (slightly before the arrow tip to avoid clipping)
                    float vOffset = 15;
                    float vEndX = currentDraggedBall.x
                            + (float) Math.cos(launchAngle) * (startDist + Math.max(0, lineLen - vOffset));
                    float vEndY = currentDraggedBall.y
                            + (float) Math.sin(launchAngle) * (startDist + Math.max(0, lineLen - vOffset));

                    // Kesikli çizgi (Trajectory)
                    paint.setStyle(Paint.Style.STROKE);
                    paint.setStrokeWidth(5);
                    paint.setColor(Color.WHITE);
                    paint.setAlpha(180);

                    if (selectedTrajectory.equals("laser")) {
                        // Visionary Laser Style
                        paint.setAlpha(255);
                        paint.setShadowLayer(15, 0, 0, Color.RED);
                        paint.setStrokeWidth(8);
                        paint.setColor(Color.RED);
                        canvas.drawLine(startX, startY, vEndX, vEndY, paint);

                        paint.setColor(Color.WHITE);
                        paint.setStrokeWidth(3);
                        canvas.drawLine(startX, startY, vEndX, vEndY, paint);
                        paint.clearShadowLayer();
                    } else if (selectedTrajectory.equals("electric")) {
                        // Electric Zig-Zag
                        paint.setAlpha(255);
                        paint.setStrokeWidth(5);
                        paint.setColor(Color.CYAN);
                        paint.setShadowLayer(20, 0, 0, Color.CYAN);
                        paint.setStyle(Paint.Style.STROKE);

                        android.graphics.Path path = new android.graphics.Path();
                        path.moveTo(startX, startY);
                        int segs = 12;
                        for (int i = 1; i <= segs; i++) {
                            float px = startX + (vEndX - startX) * i / segs;
                            float py = startY + (vEndY - startY) * i / segs;
                            float offset = (float) (Math.sin(System.currentTimeMillis() * 0.05 + i) * 15);
                            float perpX = -(endY - startY) / lineLen;
                            float perpY = (endX - startX) / lineLen;
                            path.lineTo(px + perpX * offset, py + perpY * offset);
                        }
                        canvas.drawPath(path, paint);
                        paint.clearShadowLayer();
                    } else if (selectedTrajectory.equals("dots")) {
                        // Golden Pearls
                        paint.setStyle(Paint.Style.FILL);
                        paint.setColor(Color.rgb(255, 215, 0));
                        paint.setShadowLayer(15, 0, 0, Color.YELLOW);
                        int dots = 10;
                        for (int i = 0; i <= dots; i++) {
                            float px = startX + (vEndX - startX) * i / dots;
                            float py = startY + (vEndY - startY) * i / dots;
                            canvas.drawCircle(px, py, 6, paint);
                        }
                        paint.clearShadowLayer();
                    } else if (selectedTrajectory.equals("plasma")) {
                        // Fading Plasma
                        paint.setStyle(Paint.Style.FILL);
                        int dots = 15;
                        for (int i = 0; i <= dots; i++) {
                            float t = (float) i / dots;
                            float px = startX + (vEndX - startX) * t;
                            float py = startY + (vEndY - startY) * t;
                            paint.setColor(Color.MAGENTA);
                            paint.setAlpha((int) (255 * t));
                            paint.setShadowLayer(10 * t, 0, 0, Color.MAGENTA);
                            canvas.drawCircle(px, py, 8 * t, paint);
                        }
                        paint.setAlpha(255);
                        paint.clearShadowLayer();
                    } else if (selectedTrajectory.equals("arrow")) {
                        // Green Arrow
                        paint.setAlpha(255);
                        paint.setShadowLayer(15, 0, 0, Color.GREEN);
                        paint.setStrokeWidth(6);
                        paint.setColor(Color.GREEN);
                        canvas.drawLine(startX, startY, vEndX, vEndY, paint);

                        // Small arrowheads along the line
                        paint.setStyle(Paint.Style.FILL);
                        int arrows = 3;
                        for (int i = 1; i <= arrows; i++) {
                            float t = (float) i / arrows;
                            float ax = startX + (vEndX - startX) * t;
                            float ay = startY + (vEndY - startY) * t;
                            // Simple arrowhead
                            // ... (We draw the main head at the end anyway, maybe just dots or small
                            // chevrons here?)
                            // Let's just keep the main line green and rely on the big arrow at the end.
                        }
                        paint.clearShadowLayer();
                    } else if (selectedTrajectory.equals("wave")) {
                        // Cyan Wave
                        paint.setAlpha(255);
                        paint.setStrokeWidth(4);
                        paint.setColor(Color.CYAN);
                        paint.setStyle(Paint.Style.STROKE);

                        android.graphics.Path path = new android.graphics.Path();
                        path.moveTo(startX, startY);
                        int segs = 20;
                        for (int i = 1; i <= segs; i++) {
                            float t = (float) i / segs;
                            float px = startX + (vEndX - startX) * t;
                            float py = startY + (vEndY - startY) * t;

                            // Sine wave
                            float offset = (float) Math.sin(t * Math.PI * 4 - System.currentTimeMillis() * 0.01) * 20;
                            float perpX = -(endY - startY) / lineLen;
                            float perpY = (endX - startX) / lineLen;

                            path.lineTo(px + perpX * offset, py + perpY * offset);
                        }
                        canvas.drawPath(path, paint);
                    } else if (selectedTrajectory.equals("grid")) {
                        // Projected Grid Guide
                        paint.setAlpha(150);
                        paint.setStrokeWidth(2);
                        paint.setColor(Color.WHITE);
                        paint.setStyle(Paint.Style.STROKE);

                        // Draw a "ladder" or grid projection
                        int steps = 15;
                        for (int i = 0; i <= steps; i++) {
                            float t = (float) i / steps;
                            float px = startX + (vEndX - startX) * t;
                            float py = startY + (vEndY - startY) * t;

                            // Cross lines
                            float perpX = -(endY - startY) / lineLen * 10 * (1 + t); // Widens at end
                            float perpY = (endX - startX) / lineLen * 10 * (1 + t);

                            canvas.drawLine(px - perpX, py - perpY, px + perpX, py + perpY, paint);
                        }
                        // Side lines
                        canvas.drawLine(startX, startY, vEndX, vEndY, paint);

                    } else if (selectedTrajectory.equals("pulse")) {
                        // Pulsating Line
                        float pulse = (float) (Math.sin(System.currentTimeMillis() * 0.02) * 0.5 + 0.5);
                        paint.setAlpha(255);
                        paint.setStrokeWidth(4 + 4 * pulse);
                        paint.setColor(Color.rgb(255, 50, 50));
                        paint.setShadowLayer(15 * pulse, 0, 0, Color.RED);
                        canvas.drawLine(startX, startY, vEndX, vEndY, paint);
                        paint.clearShadowLayer();

                    } else {
                        // Default dashed
                        paint.setPathEffect(new android.graphics.DashPathEffect(new float[] { 20, 20 }, 0));
                        canvas.drawLine(startX, startY, vEndX, vEndY, paint);
                        paint.setPathEffect(null);
                    }
                    paint.setAlpha(255);

                    // Ok başı (V şeklinde) - Çizginin SONUNDA
                    int arrowColor = Color.WHITE;
                    int shadowColor = Color.CYAN;
                    if (selectedTrajectory.equals("laser")) {
                        arrowColor = Color.RED;
                        shadowColor = Color.RED;
                    } else if (selectedTrajectory.equals("electric")) {
                        arrowColor = Color.CYAN;
                        shadowColor = Color.CYAN;
                    } else if (selectedTrajectory.equals("dots")) {
                        arrowColor = Color.rgb(255, 215, 0);
                        shadowColor = Color.YELLOW;
                    } else if (selectedTrajectory.equals("plasma")) {
                        arrowColor = Color.MAGENTA;
                        shadowColor = Color.MAGENTA;
                    }

                    paint.setStrokeWidth(8);
                    paint.setStrokeCap(Paint.Cap.ROUND);
                    paint.setColor(arrowColor);
                    paint.setShadowLayer(10, 0, 0, shadowColor);

                    float arrowSize = (selectedTrajectory.equals("dashed")) ? 40 : 50;
                    float wingAngle = (float) Math.toRadians(150); // 150 derece kanat açısı

                    // startX/Y yerine endX/Y kullanıyoruz (Gerçek uç)
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
                canvas.drawCircle(currentDraggedBall.x, currentDraggedBall.y, currentDraggedBall.radius * (1 + ratio),
                        paint);

                // Güç barı - topun etrafında dairesel
                float arcRadius = currentDraggedBall.radius * 2.5f;
                float sweepAngle = 360 * ratio;

                // Arka plan çember (gri)
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(8);
                paint.setColor(Color.rgb(60, 60, 60));
                paint.setAlpha(150);
                canvas.drawCircle(currentDraggedBall.x, currentDraggedBall.y, arcRadius, paint);
                paint.setAlpha(255);

                // Dolu kısım (mavi glow)
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(10);
                paint.setStrokeCap(Paint.Cap.ROUND);
                paint.setColor(Color.rgb(0, 180, 255));
                paint.setShadowLayer(20, 0, 0, Color.rgb(0, 180, 255));
                canvas.drawArc(
                        currentDraggedBall.x - arcRadius, currentDraggedBall.y - arcRadius,
                        currentDraggedBall.x + arcRadius, currentDraggedBall.y + arcRadius,
                        -90, sweepAngle, false, paint);
                paint.clearShadowLayer();
            }

            // Combo text göster (Floating texts)
            for (FloatingText ft : floatingTexts) {
                ft.draw(canvas, paint);
            }

            // STAGE CLEARED animasyonu
            if (showStageCleared) {
                paint.setStyle(Paint.Style.FILL);
                paint.setTextSize(screenWidth * 0.08f); // Reduced Text Size
                int currentStageNum = ((level - 1) % 5) + 1;
                if (currentStageNum == 5) {
                    int completedLevel = ((level - 1) / 5) + 1;
                    int nextLevelToUnlock = completedLevel + 1;

                    // If game is completed (or about to be), don't show unlock text for next level
                    if (nextLevelToUnlock > 100) {
                        canvas.drawText("FINAL STAGE CLEARED!", centerX, centerY, paint);
                    } else if (nextLevelToUnlock <= maxUnlockedLevel) {
                        paint.setTextSize(screenWidth * 0.06f); // Smaller text
                        canvas.drawText("LEVEL " + nextLevelToUnlock, centerX, centerY - screenHeight * 0.04f, paint);
                        canvas.drawText("ALREADY UNLOCKED", centerX, centerY + screenHeight * 0.04f, paint);
                    } else {
                        if (nextLevelToUnlock % 10 == 1) {
                            // New Space - Split lines to fit screen
                            int spaceNum = (nextLevelToUnlock - 1) / 10 + 1;
                            canvas.drawText("SPACE " + spaceNum, centerX, centerY - screenHeight * 0.05f, paint);
                            canvas.drawText("UNLOCKED!", centerX, centerY + screenHeight * 0.05f, paint);
                        } else {
                            canvas.drawText("LEVEL " + nextLevelToUnlock, centerX, centerY - screenHeight * 0.05f,
                                    paint);
                            canvas.drawText("UNLOCKED!", centerX, centerY + screenHeight * 0.05f, paint);
                        }
                    }
                } else {
                    canvas.drawText("STAGE CLEARED!", centerX, centerY, paint);
                }
                paint.clearShadowLayer();

                // Alt yazı
                paint.setTextSize(screenWidth * 0.045f);
                paint.setColor(Color.WHITE);
                canvas.drawText("Stage " + currentStageNum + " Complete", centerX, centerY + screenHeight * 0.08f,
                        paint);
            }

            // UI
            drawUI(canvas);

            if (gameCompleted) {
                drawGameCompleted(canvas);
            }

            canvas.restore();
            holder.unlockCanvasAndPost(canvas);
        }
    }

    private void drawNebulaBackground(Canvas canvas) {
        // High Performance Background
        paint.setStyle(Paint.Style.FILL);
        paint.setShader(nebula1);
        canvas.drawRect(0, 0, screenWidth, screenHeight, paint);
        paint.setShader(null);
    }

    private void drawBall(Canvas canvas, Ball ball) {
        if (ball == whiteBall || cloneBalls.contains(ball)) {
            // Draw Aura if active
            if (selectedAura.equals("neon")) {
                drawAuraEffect(canvas, ball);
            }

            switch (selectedSkin) {
                case "tr_flag":
                    drawTRFlagBall(canvas, ball);
                    return;
                case "soccer":
                    drawSoccerBall(canvas, ball);
                    return;
                case "neon_pulse":
                    drawNeonPulseBall(canvas, ball);
                    return;
                case "usa":
                case "germany":
                case "france":
                case "italy":
                case "uk":
                case "spain":
                case "portugal":
                case "netherlands":
                case "belgium":
                case "switzerland":
                case "austria":
                case "sweden":
                case "norway":
                case "denmark":
                case "finland":
                case "poland":
                case "greece":
                case "ireland":
                case "canada":
                case "brazil":
                case "japan":
                case "korea":
                case "china":
                case "russia":
                case "india":
                case "mexico":
                case "argentina":
                case "azerbaijan":
                case "ukraine":
                case "egypt":
                case "australia":
                case "south_africa":
                case "saudi_arabia":
                case "pakistan":
                case "indonesia":
                    drawCountryFlagBall(canvas, ball, selectedSkin);
                    return;
                case "cyber_core":
                    drawCyberCoreBall(canvas, ball);
                    return;
                case "solar_flare":
                    drawSolarFlareBall(canvas, ball);
                    return;
                case "frost_bite":
                    drawFrostBiteBall(canvas, ball);
                    return;
            }
        }

        paint.setStyle(Paint.Style.FILL);

        RadialGradient gradient = new RadialGradient(
                ball.x - ball.radius / 3, ball.y - ball.radius / 3, ball.radius,
                Color.WHITE, ball.color, Shader.TileMode.CLAMP);
        paint.setShader(gradient);
        paint.setShadowLayer(15, 0, 0, ball.color);

        canvas.drawCircle(ball.x, ball.y, ball.radius, paint);

        paint.clearShadowLayer();
        paint.setShader(null);

        // 8-Ball Design (Black Ball)
        if (ball.color == Color.BLACK && ball != whiteBall) {
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.WHITE);
            canvas.drawCircle(ball.x, ball.y, ball.radius * 0.45f, paint);

            paint.setColor(Color.BLACK);
            paint.setTextSize(ball.radius * 0.6f);
            paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            paint.setTextAlign(Paint.Align.CENTER);
            float textY = ball.y - (paint.descent() + paint.ascent()) / 2;
            canvas.drawText("8", ball.x, textY, paint);
        }
    }

    private void drawTRFlagBall(Canvas canvas, Ball ball) {
        // Red Base
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.RED);
        paint.setShadowLayer(20, 0, 0, Color.RED);
        canvas.drawCircle(ball.x, ball.y, ball.radius, paint);
        paint.clearShadowLayer();

        // White Crescent
        paint.setColor(Color.WHITE);
        canvas.drawCircle(ball.x - ball.radius * 0.15f, ball.y, ball.radius * 0.55f, paint);
        paint.setColor(Color.RED);
        canvas.drawCircle(ball.x - ball.radius * 0.05f, ball.y, ball.radius * 0.45f, paint);

        // White Star
        paint.setColor(Color.WHITE);
        float starX = ball.x + ball.radius * 0.35f;
        float starY = ball.y;
        float r = ball.radius * 0.25f;
        drawStarPath(canvas, starX, starY, r);
    }

    private void drawCyberCoreBall(Canvas canvas, Ball ball) {
        // Metallic grey base
        paint.setStyle(Paint.Style.FILL);
        RadialGradient base = new RadialGradient(ball.x - ball.radius / 3, ball.y - ball.radius / 3, ball.radius * 1.5f,
                Color.rgb(80, 80, 90), Color.rgb(30, 30, 35), Shader.TileMode.CLAMP);
        paint.setShader(base);
        canvas.drawCircle(ball.x, ball.y, ball.radius, paint);
        paint.setShader(null);

        // Pulsing Cyan Circuits
        float pulse = (float) (Math.sin(System.currentTimeMillis() * 0.005) * 0.5 + 0.5);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(ball.radius * 0.15f);
        paint.setColor(Color.CYAN);
        paint.setAlpha((int) (100 + 155 * pulse));
        paint.setShadowLayer(10 * pulse, 0, 0, Color.CYAN);

        // Draw some circuit-like lines
        canvas.drawArc(ball.x - ball.radius * 0.7f, ball.y - ball.radius * 0.7f,
                ball.x + ball.radius * 0.7f, ball.y + ball.radius * 0.7f, 45, 90, false, paint);
        canvas.drawArc(ball.x - ball.radius * 0.7f, ball.y - ball.radius * 0.7f,
                ball.x + ball.radius * 0.7f, ball.y + ball.radius * 0.7f, 225, 90, false, paint);

        paint.setStrokeWidth(ball.radius * 0.1f);
        canvas.drawCircle(ball.x, ball.y, ball.radius * 0.3f, paint);

        paint.clearShadowLayer();
        paint.setAlpha(255);
    }

    private void drawSolarFlareBall(Canvas canvas, Ball ball) {
        // Inner pulsing core
        float pulse = (float) (Math.sin(System.currentTimeMillis() * 0.01) * 0.2 + 0.8);
        paint.setStyle(Paint.Style.FILL);
        RadialGradient sun = new RadialGradient(ball.x, ball.y, ball.radius * 1.5f,
                new int[] { Color.WHITE, Color.YELLOW, Color.rgb(255, 100, 0), Color.TRANSPARENT },
                new float[] { 0, 0.3f, 0.7f, 1f }, Shader.TileMode.CLAMP);
        paint.setShader(sun);
        paint.setShadowLayer(25 * pulse, 0, 0, Color.YELLOW);
        canvas.drawCircle(ball.x, ball.y, ball.radius * pulse, paint);

        // Flare/Corona effect
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(ball.radius * 0.3f);
        paint.setColor(Color.rgb(255, 60, 0));
        paint.setAlpha((int) (150 * (1 - pulse + 0.2)));
        canvas.drawCircle(ball.x, ball.y, ball.radius * 1.2f, paint);

        paint.clearShadowLayer();
        paint.setShader(null);
        paint.setAlpha(255);
    }

    private void drawFrostBiteBall(Canvas canvas, Ball ball) {
        // Icy Crystal Base
        paint.setStyle(Paint.Style.FILL);
        RadialGradient ice = new RadialGradient(ball.x - ball.radius / 3, ball.y - ball.radius / 3, ball.radius,
                new int[] { Color.WHITE, Color.rgb(200, 240, 255), Color.rgb(100, 180, 255) },
                null, Shader.TileMode.CLAMP);
        paint.setShader(ice);
        paint.setShadowLayer(20, 0, 0, Color.rgb(173, 216, 230));
        canvas.drawCircle(ball.x, ball.y, ball.radius, paint);

        // Cracks/Crystals
        paint.setShader(null);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2);
        paint.setColor(Color.WHITE);
        paint.setAlpha(180);

        // Simulating ice cracks
        canvas.drawLine(ball.x - ball.radius * 0.5f, ball.y - ball.radius * 0.5f, ball.x, ball.y, paint);
        canvas.drawLine(ball.x + ball.radius * 0.3f, ball.y - ball.radius * 0.6f, ball.x - ball.radius * 0.1f,
                ball.y + ball.radius * 0.2f, paint);

        // Mist/Cold Aura
        float time = System.currentTimeMillis() * 0.002f;
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(ball.radius * 0.4f);
        paint.setAlpha((int) (80 + 40 * Math.sin(time)));
        canvas.drawCircle(ball.x, ball.y, ball.radius * 1.3f, paint);

        paint.clearShadowLayer();
        paint.setAlpha(255);
    }

    private void drawStarPath(Canvas canvas, float cx, float cy, float r) {
        android.graphics.Path path = new android.graphics.Path();
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
        canvas.drawPath(path, paint);
    }

    private void drawAuraEffect(Canvas canvas, Ball ball) {
        float speed = (float) Math.sqrt(ball.vx * ball.vx + ball.vy * ball.vy);
        float pulse = (float) (Math.sin(System.currentTimeMillis() * 0.01) * 0.15 + 0.85);
        float auraSize = ball.radius * (1.3f + (speed * 0.02f) * pulse);

        int auraColor = Color.CYAN; // Default
        if (selectedSkin.equals("tr_flag"))
            auraColor = Color.RED;
        else if (selectedSkin.equals("soccer"))
            auraColor = Color.WHITE;
        else if (selectedSkin.equals("neon_pulse"))
            auraColor = Color.rgb(0, 255, 255);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(5 + (speed * 0.1f));
        paint.setColor(auraColor);
        paint.setAlpha((int) (150 * pulse));
        paint.setShadowLayer(25 * pulse + speed * 0.5f, 0, 0, auraColor);
        canvas.drawCircle(ball.x, ball.y, auraSize, paint);
        paint.clearShadowLayer();
        paint.setAlpha(255);
    }

    private void drawCometTrail(Canvas canvas, Ball ball) {
        if (selectedTrail.equals("cosmic")) {
            drawCosmicTrail(canvas, ball);
            return;
        } else if (selectedTrail.equals("lava")) {
            drawLavaTrail(canvas, ball);
            return;
        } else if (selectedTrail.equals("electric")) {
            drawElectricTrail(canvas, ball);
            return;
        } else if (selectedTrail.equals("rainbow")) {
            drawRainbowTrail(canvas, ball);
            return;
        } else if (selectedTrail.equals("ghost")) {
            drawGhostTrail(canvas, ball);
            return;
        } else if (selectedTrail.equals("bubble")) {
            drawBubbleTrail(canvas, ball);
            return;
        } else if (selectedTrail.equals("pixel")) {
            drawPixelTrail(canvas, ball);
            return;
        } else if (selectedTrail.equals("dna")) {
            drawDNATrail(canvas, ball);
            return;
        } else if (selectedTrail.equals("sparkle")) {
            drawSparkleTrail(canvas, ball);
            return;
        }

        paint.setStyle(Paint.Style.FILL);
        int trailColor = Color.CYAN; // Default

        switch (selectedTrail) {
            case "red":
                trailColor = Color.RED;
                break;
            case "blue":
                trailColor = Color.BLUE;
                break;
            case "green":
                trailColor = Color.GREEN;
                break;
            case "gold":
                trailColor = Color.rgb(255, 215, 0);
                break;
            case "purple":
                trailColor = Color.rgb(160, 32, 240);
                break;
            case "pink":
                trailColor = Color.rgb(255, 105, 180);
                break;
            case "neon":
                trailColor = Color.CYAN;
                break;
            case "comet":
                trailColor = Color.CYAN;
                break;
        }

        for (int i = 0; i < ball.trail.size(); i++) {
            TrailPoint p = ball.trail.get(i);
            float ratio = 1.0f - (float) i / MAX_TRAIL_POINTS;
            int alpha = (int) (180 * ratio);
            float size = p.radius * ratio;

            paint.setColor(trailColor);
            paint.setAlpha(alpha);
            paint.setShadowLayer(10 * ratio, 0, 0, trailColor);
            canvas.drawCircle(p.x, p.y, size, paint);
        }
        paint.setAlpha(255);
        paint.clearShadowLayer();
    }

    private void drawDNATrail(Canvas canvas, Ball ball) {
        paint.setStyle(Paint.Style.FILL);
        for (int i = 0; i < ball.trail.size(); i++) {
            TrailPoint p = ball.trail.get(i);
            float ratio = 1.0f - (float) i / MAX_TRAIL_POINTS;

            // DNA Helix structure
            float offset = (float) Math.sin(i * 0.5 + System.currentTimeMillis() * 0.005) * 20;

            paint.setColor(Color.MAGENTA);
            paint.setAlpha((int) (200 * ratio));
            canvas.drawCircle(p.x + offset, p.y + offset, 4 * ratio, paint);

            paint.setColor(Color.CYAN);
            canvas.drawCircle(p.x - offset, p.y - offset, 4 * ratio, paint);

            // Connector
            if (i % 2 == 0) {
                paint.setColor(Color.WHITE);
                paint.setAlpha((int) (100 * ratio));
                paint.setStrokeWidth(2);
                canvas.drawLine(p.x + offset, p.y + offset, p.x - offset, p.y - offset, paint);
                paint.setStyle(Paint.Style.FILL); // Reset
            }
        }
    }

    private void drawSparkleTrail(Canvas canvas, Ball ball) {
        paint.setStyle(Paint.Style.FILL);
        for (int i = 0; i < ball.trail.size(); i++) {
            TrailPoint p = ball.trail.get(i);
            float ratio = 1.0f - (float) i / MAX_TRAIL_POINTS;

            if (random.nextFloat() > 0.3f) {
                paint.setColor(Color.YELLOW);
                paint.setAlpha((int) (255 * ratio));
                float size = 5 * ratio * (random.nextFloat() + 0.5f);
                drawStarPath(canvas, p.x + (random.nextFloat() - 0.5f) * 30, p.y + (random.nextFloat() - 0.5f) * 30,
                        size);
            }
        }
    }

    private void drawCosmicTrail(Canvas canvas, Ball ball) {
        paint.setStyle(Paint.Style.FILL);
        for (int i = 0; i < ball.trail.size(); i++) {
            TrailPoint p = ball.trail.get(i);
            float ratio = 1.0f - (float) i / MAX_TRAIL_POINTS;

            // Sparkling particles
            if (random.nextFloat() > 0.4f) {
                paint.setColor(random.nextBoolean() ? Color.WHITE : Color.rgb(200, 220, 255));
                paint.setAlpha((int) (255 * ratio));
                canvas.drawCircle(p.x + (random.nextFloat() - 0.5f) * 20, p.y + (random.nextFloat() - 0.5f) * 20,
                        3 * ratio, paint);
            }

            // Dust cloud
            paint.setColor(Color.rgb(100, 100, 255));
            paint.setAlpha((int) (100 * ratio));
            canvas.drawCircle(p.x, p.y, p.radius * (1.0f + ratio), paint);
        }
    }

    private void drawLavaTrail(Canvas canvas, Ball ball) {
        for (int i = 0; i < ball.trail.size(); i++) {
            TrailPoint p = ball.trail.get(i);
            float ratio = 1.0f - (float) i / MAX_TRAIL_POINTS;

            // Lava core
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.rgb(255, 69, 0)); // Orange-Red
            paint.setAlpha((int) (200 * ratio));
            canvas.drawCircle(p.x, p.y, p.radius * ratio, paint);

            // Fire sparks
            if (random.nextFloat() > 0.7f) {
                paint.setColor(Color.YELLOW);
                canvas.drawCircle(p.x + (random.nextFloat() - 0.5f) * 30, p.y - ratio * 40, 4 * ratio, paint);
            }

            // Smoke
            paint.setColor(Color.DKGRAY);
            paint.setAlpha((int) (120 * ratio));
            canvas.drawCircle(p.x, p.y - ratio * 20, p.radius * 1.5f * ratio, paint);
        }
    }

    private void drawElectricTrail(Canvas canvas, Ball ball) {
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(3);
        paint.setColor(Color.CYAN);
        paint.setShadowLayer(15, 0, 0, Color.CYAN);

        for (int i = 1; i < ball.trail.size(); i++) {
            TrailPoint p1 = ball.trail.get(i - 1);
            TrailPoint p2 = ball.trail.get(i);
            float ratio = 1.0f - (float) i / MAX_TRAIL_POINTS;
            paint.setAlpha((int) (255 * ratio));

            // Zig-zag electricity
            float offsetX = (random.nextFloat() - 0.5f) * 30;
            float offsetY = (random.nextFloat() - 0.5f) * 30;
            canvas.drawLine(p1.x, p1.y, p2.x + offsetX, p2.y + offsetY, paint);
            canvas.drawLine(p2.x + offsetX, p2.y + offsetY, p2.x, p2.y, paint);
        }
        paint.clearShadowLayer();
    }

    private void drawRainbowTrail(Canvas canvas, Ball ball) {
        paint.setStyle(Paint.Style.FILL);
        int[] rainbow = { Color.RED, Color.YELLOW, Color.GREEN, Color.CYAN, Color.MAGENTA };

        for (int i = 0; i < ball.trail.size(); i++) {
            TrailPoint p = ball.trail.get(i);
            float ratio = 1.0f - (float) i / MAX_TRAIL_POINTS;

            paint.setColor(rainbow[i % rainbow.length]);
            paint.setAlpha((int) (180 * ratio));
            canvas.drawCircle(p.x, p.y, p.radius * (1.2f - ratio * 0.5f), paint);
        }
    }

    private void drawGhostTrail(Canvas canvas, Ball ball) {
        paint.setStyle(Paint.Style.FILL);
        // fill
        for (int i = 0; i < ball.trail.size(); i++) {
            TrailPoint p = ball.trail.get(i);
            float ratio = 1.0f - (float) i / MAX_TRAIL_POINTS;

            // Draw a ghost ball at each point
            paint.setColor(Color.WHITE);
            paint.setAlpha((int) (120 * ratio));
            canvas.drawCircle(p.x, p.y, p.radius * ratio, paint);
        }
    }

    private void drawBubbleTrail(Canvas canvas, Ball ball) {
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(3);
        paint.setColor(Color.CYAN);

        for (int i = 0; i < ball.trail.size(); i++) {
            TrailPoint p = ball.trail.get(i);
            float ratio = 1.0f - (float) i / MAX_TRAIL_POINTS;

            paint.setAlpha((int) (200 * ratio));
            canvas.drawCircle(p.x, p.y, p.radius * ratio, paint);

            // Shine spot
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.WHITE);
            paint.setAlpha((int) (150 * ratio));
            canvas.drawCircle(p.x - p.radius * ratio * 0.3f, p.y - p.radius * ratio * 0.3f, p.radius * ratio * 0.3f,
                    paint);

            paint.setStyle(Paint.Style.STROKE);
            paint.setColor(Color.CYAN);
        }
        paint.setStyle(Paint.Style.FILL);
    }

    private void drawPixelTrail(Canvas canvas, Ball ball) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.GREEN);

        for (int i = 0; i < ball.trail.size(); i++) {
            TrailPoint p = ball.trail.get(i);
            float ratio = 1.0f - (float) i / MAX_TRAIL_POINTS;
            float r = p.radius * ratio;

            paint.setAlpha((int) (180 * ratio));
            canvas.drawRect(p.x - r, p.y - r, p.x + r, p.y + r, paint);
        }
    }

    private void drawSoccerBall(Canvas canvas, Ball ball) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.WHITE);
        canvas.drawCircle(ball.x, ball.y, ball.radius, paint);

        paint.setColor(Color.BLACK);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2);

        // Draw some simple pentagons/lines to resemble soccer ball
        for (int i = 0; i < 5; i++) {
            float angle = (float) (i * 2 * Math.PI / 5);
            float x1 = ball.x + (float) Math.cos(angle) * (ball.radius * 0.4f);
            float y1 = ball.y + (float) Math.sin(angle) * (ball.radius * 0.4f);
            float x2 = ball.x + (float) Math.cos(angle) * ball.radius;
            float y2 = ball.y + (float) Math.sin(angle) * ball.radius;
            canvas.drawLine(x1, y1, x2, y2, paint);

            float angleNext = (float) ((i + 1) * 2 * Math.PI / 5);
            float xNext = ball.x + (float) Math.cos(angleNext) * (ball.radius * 0.4f);
            float yNext = ball.y + (float) Math.sin(angleNext) * (ball.radius * 0.4f);
            canvas.drawLine(x1, y1, xNext, yNext, paint);
        }
    }

    private void drawNeonPulseBall(Canvas canvas, Ball ball) {
        float pulse = (float) (Math.sin(System.currentTimeMillis() * 0.01) * 0.2 + 0.8);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.WHITE);
        canvas.drawCircle(ball.x, ball.y, ball.radius, paint);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(4);
        paint.setColor(Color.CYAN);
        paint.setShadowLayer(20 * pulse, 0, 0, Color.CYAN);
        canvas.drawCircle(ball.x, ball.y, ball.radius, paint);
        paint.clearShadowLayer();
    }

    private void drawCountryFlagBall(Canvas canvas, Ball ball, String country) {
        canvas.save();
        android.graphics.Path clipPath = new android.graphics.Path();
        clipPath.addCircle(ball.x, ball.y, ball.radius, android.graphics.Path.Direction.CW);
        canvas.clipPath(clipPath);

        float bx = ball.x;
        float by = ball.y;
        float r = ball.radius;

        switch (country) {
            case "usa":
                for (int i = 0; i < 7; i++) {
                    paint.setColor(i % 2 == 0 ? Color.RED : Color.WHITE);
                    canvas.drawRect(bx - r, by - r + (i * 2 * r / 7), bx + r, by - r + ((i + 1) * 2 * r / 7), paint);
                }
                paint.setColor(Color.rgb(0, 40, 104));
                canvas.drawRect(bx - r, by - r, bx, by, paint);
                paint.setColor(Color.WHITE);
                canvas.drawCircle(bx - r * 0.5f, by - r * 0.5f, r * 0.15f, paint);
                break;
            case "germany":
                drawHorizStripes(canvas, bx, by, r, Color.BLACK, Color.RED, Color.rgb(255, 204, 0));
                break;
            case "france":
                drawVertStripes(canvas, bx, by, r, Color.rgb(0, 85, 164), Color.WHITE, Color.rgb(239, 65, 53));
                break;
            case "italy":
                drawVertStripes(canvas, bx, by, r, Color.rgb(0, 146, 70), Color.WHITE, Color.rgb(206, 43, 55));
                break;
            case "uk":
                paint.setColor(Color.rgb(1, 33, 105));
                canvas.drawCircle(bx, by, r, paint);
                paint.setColor(Color.WHITE);
                canvas.drawRect(bx - r, by - r * 0.3f, bx + r, by + r * 0.3f, paint);
                canvas.drawRect(bx - r * 0.3f, by - r, bx + r * 0.3f, by + r, paint);
                paint.setColor(Color.RED);
                canvas.drawRect(bx - r, by - r * 0.15f, bx + r, by + r * 0.15f, paint);
                canvas.drawRect(bx - r * 0.15f, by - r, bx + r * 0.15f, by + r, paint);
                break;
            case "spain":
                paint.setColor(Color.RED);
                canvas.drawRect(bx - r, by - r, bx + r, by - r * 0.5f, paint);
                canvas.drawRect(bx - r, by + r * 0.5f, bx + r, by + r, paint);
                paint.setColor(Color.rgb(255, 196, 0));
                canvas.drawRect(bx - r, by - r * 0.5f, bx + r, by + r * 0.5f, paint);
                break;
            case "portugal":
                paint.setColor(Color.rgb(0, 102, 0));
                canvas.drawRect(bx - r, by - r, bx - r * 0.2f, by + r, paint);
                paint.setColor(Color.RED);
                canvas.drawRect(bx - r * 0.2f, by - r, bx + r, by + r, paint);
                break;
            case "netherlands":
                drawHorizStripes(canvas, bx, by, r, Color.rgb(174, 28, 40), Color.WHITE, Color.rgb(33, 70, 139));
                break;
            case "belgium":
                drawVertStripes(canvas, bx, by, r, Color.BLACK, Color.rgb(253, 218, 36), Color.rgb(239, 51, 64));
                break;
            case "switzerland":
                paint.setColor(Color.RED);
                canvas.drawCircle(bx, by, r, paint);
                paint.setColor(Color.WHITE);
                canvas.drawRect(bx - r * 0.55f, by - r * 0.15f, bx + r * 0.55f, by + r * 0.15f, paint);
                canvas.drawRect(bx - r * 0.15f, by - r * 0.55f, bx + r * 0.15f, by + r * 0.55f, paint);
                break;
            case "austria":
                drawHorizStripes(canvas, bx, by, r, Color.RED, Color.WHITE, Color.RED);
                break;
            case "sweden":
                drawNordicCross(canvas, bx, by, r, Color.rgb(0, 107, 168), Color.rgb(254, 204, 2));
                break;
            case "norway":
                drawNordicCross(canvas, bx, by, r, Color.rgb(186, 12, 47), Color.rgb(0, 32, 91));
                break;
            case "denmark":
                drawNordicCross(canvas, bx, by, r, Color.rgb(198, 12, 48), Color.WHITE);
                break;
            case "finland":
                drawNordicCross(canvas, bx, by, r, Color.WHITE, Color.rgb(0, 53, 128));
                break;
            case "poland":
                drawHorizStripes(canvas, bx, by, r, Color.WHITE, Color.rgb(220, 20, 60));
                break;
            case "greece":
                paint.setColor(Color.rgb(13, 94, 175));
                drawHorizStripes(canvas, bx, by, r, Color.rgb(13, 94, 175), Color.WHITE, Color.rgb(13, 94, 175),
                        Color.WHITE, Color.rgb(13, 94, 175));
                canvas.drawRect(bx - r, by - r, bx, by + r * 0.2f, paint);
                paint.setColor(Color.WHITE);
                canvas.drawRect(bx - r, by - r * 0.5f, bx, by - r * 0.3f, paint);
                canvas.drawRect(bx - r * 0.6f, by - r, bx - r * 0.4f, by + r * 0.2f, paint);
                break;
            case "ireland":
                drawVertStripes(canvas, bx, by, r, Color.rgb(22, 155, 98), Color.WHITE, Color.rgb(255, 136, 62));
                break;
            case "canada":
                paint.setColor(Color.RED);
                canvas.drawRect(bx - r, by - r, bx - r * 0.4f, by + r, paint);
                canvas.drawRect(bx + r * 0.4f, by - r, bx + r, by + r, paint);
                paint.setColor(Color.WHITE);
                canvas.drawRect(bx - r * 0.4f, by - r, bx + r * 0.4f, by + r, paint);
                paint.setColor(Color.RED);
                canvas.drawCircle(bx, by, r * 0.3f, paint); // Leaf representation
                break;
            case "brazil":
                paint.setColor(Color.rgb(0, 153, 51));
                canvas.drawCircle(bx, by, r, paint);
                paint.setColor(Color.YELLOW);
                android.graphics.Path diamond = new android.graphics.Path();
                diamond.moveTo(bx, by - r * 0.8f);
                diamond.lineTo(bx + r * 0.8f, by);
                diamond.lineTo(bx, by + r * 0.8f);
                diamond.lineTo(bx - r * 0.8f, by);
                diamond.close();
                canvas.drawPath(diamond, paint);
                paint.setColor(Color.rgb(0, 39, 118));
                canvas.drawCircle(bx, by, r * 0.35f, paint);
                break;
            case "japan":
                paint.setColor(Color.WHITE);
                canvas.drawCircle(bx, by, r, paint);
                paint.setColor(Color.RED);
                canvas.drawCircle(bx, by, r * 0.4f, paint);
                break;
            case "korea":
                paint.setColor(Color.WHITE);
                canvas.drawCircle(bx, by, r, paint);
                paint.setColor(Color.RED);
                canvas.drawArc(bx - r * 0.5f, by - r * 0.5f, bx + r * 0.5f, by + r * 0.5f, -90, 180, true, paint);
                paint.setColor(Color.BLUE);
                canvas.drawArc(bx - r * 0.5f, by - r * 0.5f, bx + r * 0.5f, by + r * 0.5f, 90, 180, true, paint);
                break;
            case "china":
                paint.setColor(Color.RED);
                canvas.drawCircle(bx, by, r, paint);
                paint.setColor(Color.YELLOW);
                drawStarPath(canvas, bx - r * 0.2f, by - r * 0.2f, r * 0.25f);
                break;
            case "russia":
                drawHorizStripes(canvas, bx, by, r, Color.WHITE, Color.BLUE, Color.RED);
                break;
            case "india":
                drawHorizStripes(canvas, bx, by, r, Color.rgb(255, 153, 51), Color.WHITE, Color.rgb(19, 136, 8));
                paint.setColor(Color.BLUE);
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(2);
                canvas.drawCircle(bx, by, r * 0.15f, paint);
                break;
            case "mexico":
                drawVertStripes(canvas, bx, by, r, Color.rgb(0, 104, 71), Color.WHITE, Color.RED);
                paint.setColor(Color.rgb(139, 69, 19));
                paint.setStyle(Paint.Style.FILL);
                canvas.drawCircle(bx, by, r * 0.15f, paint);
                break;
            case "argentina":
                drawHorizStripes(canvas, bx, by, r, Color.rgb(117, 170, 219), Color.WHITE, Color.rgb(117, 170, 219));
                paint.setColor(Color.YELLOW);
                canvas.drawCircle(bx, by, r * 0.15f, paint);
                break;
            case "azerbaijan":
                drawHorizStripes(canvas, bx, by, r, Color.rgb(0, 181, 226), Color.RED, Color.GREEN);
                paint.setColor(Color.WHITE);
                paint.setStyle(Paint.Style.FILL);
                canvas.drawCircle(bx, by, r * 0.12f, paint);
                break;
            case "ukraine":
                drawHorizStripes(canvas, bx, by, r, Color.rgb(0, 87, 183), Color.rgb(255, 215, 0));
                break;
            case "egypt":
                drawHorizStripes(canvas, bx, by, r, Color.RED, Color.WHITE, Color.BLACK);
                paint.setColor(Color.rgb(192, 147, 0)); // Gold
                canvas.drawCircle(bx, by, r * 0.15f, paint);
                break;
            case "australia":
                paint.setColor(Color.rgb(0, 0, 139)); // Blue
                canvas.drawCircle(bx, by, r, paint);
                // Union Jack (simplified)
                paint.setColor(Color.RED);
                canvas.drawRect(bx - r, by - r, bx, by, paint); // Red base for corner
                paint.setColor(Color.WHITE);
                canvas.drawLine(bx - r, by - r, bx, by, paint); // Cross
                canvas.drawLine(bx - r, by, bx, by - r, paint);
                // Stars
                paint.setColor(Color.WHITE);
                drawStarPath(canvas, bx, by + r * 0.6f, r * 0.2f); // Commonwealth
                drawStarPath(canvas, bx + r * 0.6f, by, r * 0.15f); // Southern Cross main
                break;
            case "south_africa":
                paint.setColor(Color.WHITE);
                canvas.drawCircle(bx, by, r, paint); // White base for borders

                // Green Y
                paint.setColor(Color.rgb(0, 122, 77));
                android.graphics.Path yPath = new android.graphics.Path();
                yPath.moveTo(bx - r, by - r * 0.2f);
                yPath.lineTo(bx, by);
                yPath.lineTo(bx + r, by);
                yPath.lineTo(bx + r, by - 0.2f); // Thickness hack
                // Actually simple shapes:
                canvas.drawRect(bx, by - r * 0.15f, bx + r, by + r * 0.15f, paint); // Middle bar
                // Triangles for Y arms? Complex. Let's simplify:
                // Green Horizontal
                canvas.drawRect(bx - r, by - r * 0.15f, bx + r, by + r * 0.15f, paint);
                // Black Triangle Left
                paint.setColor(Color.BLACK);
                android.graphics.Path tri = new android.graphics.Path();
                tri.moveTo(bx - r, by - r * 0.5f);
                tri.lineTo(bx - r * 0.3f, by);
                tri.lineTo(bx - r, by + r * 0.5f);
                tri.close();
                canvas.drawPath(tri, paint);
                // Yellow Border
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(5);
                paint.setColor(Color.YELLOW);
                canvas.drawPath(tri, paint);
                paint.setStyle(Paint.Style.FILL);

                // Red Top
                paint.setColor(Color.RED);
                canvas.drawRect(bx - r, by - r, bx + r, by - r * 0.25f, paint);
                // Blue Bottom
                paint.setColor(Color.BLUE);
                canvas.drawRect(bx - r, by + r * 0.25f, bx + r, by + r, paint);
                break;
            case "saudi_arabia":
                paint.setColor(Color.rgb(0, 100, 0)); // Green
                canvas.drawCircle(bx, by, r, paint);
                paint.setColor(Color.WHITE);
                // Sword
                canvas.drawLine(bx - r * 0.6f, by + r * 0.3f, bx + r * 0.6f, by + r * 0.3f, paint);
                // Text squiggle
                paint.setStrokeWidth(3);
                canvas.drawLine(bx - r * 0.4f, by - r * 0.1f, bx + r * 0.4f, by - r * 0.1f, paint);
                canvas.drawCircle(bx, by - r * 0.2f, 2, paint);
                break;
            case "pakistan":
                paint.setColor(Color.rgb(0, 64, 26)); // Dark Green
                canvas.drawCircle(bx, by, r, paint);
                paint.setColor(Color.WHITE);
                // White stripe left
                canvas.drawRect(bx - r, by - r, bx - r * 0.5f, by + r, paint);
                // Crescent
                paint.setColor(Color.WHITE);
                canvas.drawCircle(bx + r * 0.1f, by, r * 0.35f, paint);
                paint.setColor(Color.rgb(0, 64, 26)); // Mask
                canvas.drawCircle(bx + r * 0.18f, by - r * 0.05f, r * 0.3f, paint);
                // Star
                paint.setColor(Color.WHITE);
                drawStarPath(canvas, bx + r * 0.3f, by - r * 0.2f, r * 0.1f);
                break;
            case "indonesia":
                drawHorizStripes(canvas, bx, by, r, Color.RED, Color.WHITE);
                break;
        }
        canvas.restore();
    }

    private void drawHorizStripes(Canvas canvas, float x, float y, float r, int... colors) {
        float h = 2 * r / colors.length;
        paint.setStyle(Paint.Style.FILL);
        for (int i = 0; i < colors.length; i++) {
            paint.setColor(colors[i]);
            canvas.drawRect(x - r, y - r + i * h, x + r, y - r + (i + 1) * h, paint);
        }
    }

    private void drawVertStripes(Canvas canvas, float x, float y, float r, int... colors) {
        float w = 2 * r / colors.length;
        paint.setStyle(Paint.Style.FILL);
        for (int i = 0; i < colors.length; i++) {
            paint.setColor(colors[i]);
            canvas.drawRect(x - r + i * w, y - r, x - r + (i + 1) * w, y + r, paint);
        }
    }

    private void drawNordicCross(Canvas canvas, float x, float y, float r, int bgColor, int crossColor) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(bgColor);
        canvas.drawCircle(x, y, r, paint);
        paint.setColor(crossColor);
        canvas.drawRect(x - r, y - r * 0.2f, x + r, y + r * 0.2f, paint);
        canvas.drawRect(x - r * 0.5f, y - r, x - r * 0.1f, y + r, paint);
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
        // UI artık MainActivity'deki custom panellerde gösteriliyor
        // Eski text-based UI kaldırıldı

        // MainActivity'deki panelleri güncelle
        updateMainActivityPanels();

        // Level Seçim Ekranı
        if (showLevelSelector && !gameStarted) {
            drawLevelSelector(canvas);
            return;
        }

        if (!gameStarted) {
            float menuWidth = screenWidth * 0.75f;
            float menuHeight = screenHeight * 0.55f; // Sığması için küçültüldü
            // Dikey ortalama, üstte biraz boşluk bırak (dümen için)
            float menuTop = (screenHeight - menuHeight) / 2 + screenHeight * 0.03f;
            float menuBottom = menuTop + menuHeight;

            // 1. Draw Black Hole (Top Center, appearing behind the frame)
            drawBlackHole(canvas, centerX, menuTop, screenWidth * 0.15f);

            // 2. Draw Main Menu Container (Complex Shape with Curves)
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.argb(220, 20, 20, 40)); // Dark Blue/Purple background

            Path framePath = new Path();
            float cornerRadius = 40f;
            float headerCurveHeight = 60f;

            // Top Arch (Curved inwards)
            framePath.moveTo(centerX - menuWidth / 2, menuTop + headerCurveHeight);
            framePath.quadTo(centerX, menuTop - headerCurveHeight * 0.2f, centerX + menuWidth / 2,
                    menuTop + headerCurveHeight);

            // Sides and Bottom (Curved bottom corners)
            framePath.lineTo(centerX + menuWidth / 2, menuBottom - cornerRadius);
            framePath.quadTo(centerX + menuWidth / 2, menuBottom, centerX + menuWidth / 2 - cornerRadius, menuBottom);
            framePath.lineTo(centerX - menuWidth / 2 + cornerRadius, menuBottom);
            framePath.quadTo(centerX - menuWidth / 2, menuBottom, centerX - menuWidth / 2, menuBottom - cornerRadius);
            framePath.close();

            canvas.drawPath(framePath, paint);

            // 3. Neon Border for Container
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(6f);
            paint.setColor(Color.CYAN);
            paint.setShadowLayer(25, 0, 0, Color.CYAN);
            canvas.drawPath(framePath, paint);
            paint.clearShadowLayer();

            // 4. "MENU" Header Text Area
            // Draw a separator line arc below "MENU"
            Path headerLine = new Path();
            float headerLineY = menuTop + headerCurveHeight + 80;
            headerLine.moveTo(centerX - menuWidth / 2 + 20, headerLineY);
            headerLine.quadTo(centerX, headerLineY + 20, centerX + menuWidth / 2 - 20, headerLineY);

            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(3f);
            paint.setColor(Color.MAGENTA);
            paint.setShadowLayer(10, 0, 0, Color.MAGENTA);
            canvas.drawPath(headerLine, paint);
            paint.clearShadowLayer();

            // "MENU" Text
            paint.setStyle(Paint.Style.FILL);
            paint.setTextSize(screenWidth * 0.12f);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            paint.setColor(Color.MAGENTA);
            paint.setShadowLayer(30, 0, 0, Color.MAGENTA);
            // Position well visible in the header area
            canvas.drawText("MENU", centerX, menuTop + headerCurveHeight + 50, paint);
            paint.clearShadowLayer();
            paint.setTypeface(Typeface.DEFAULT);

            // 5. Decorative Hanging Lanterns/Hearts
            float lanternY = menuBottom;
            float ropeLength = screenHeight * 0.08f;

            // Left Lantern
            drawHangingDecoration(canvas, centerX - menuWidth * 0.25f, lanternY, ropeLength);
            // Right Lantern
            drawHangingDecoration(canvas, centerX + menuWidth * 0.25f, lanternY, ropeLength);
        }

        if (gameOver) {
            // Dark overlay
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.BLACK);
            paint.setAlpha(200);
            canvas.drawRect(0, 0, screenWidth, screenHeight, paint);
            paint.setAlpha(255);

            float menuWidth = screenWidth * 0.75f;
            float menuHeight = screenHeight * 0.55f;
            float menuTop = (screenHeight - menuHeight) / 2 + screenHeight * 0.03f;
            float menuBottom = menuTop + menuHeight;

            // 1. Draw Black Hole (Same as Main Menu)
            drawBlackHole(canvas, centerX, menuTop, screenWidth * 0.15f);

            // 2. Draw Game Over Container
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.argb(220, 20, 20, 40));

            Path framePath = new Path();
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

            canvas.drawPath(framePath, paint);

            // 3. Neon Border (Red for Game Over)
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(6f);
            paint.setColor(Color.RED);
            paint.setShadowLayer(25, 0, 0, Color.RED);
            canvas.drawPath(framePath, paint);
            paint.clearShadowLayer();

            // 4. Header "GAME OVER"
            paint.setStyle(Paint.Style.FILL);
            paint.setTextSize(screenWidth * 0.12f);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            paint.setColor(Color.RED);
            paint.setShadowLayer(30, 0, 0, Color.RED);
            canvas.drawText("GAME OVER", centerX, menuTop + headerCurveHeight + 50, paint);
            paint.clearShadowLayer();
            paint.setTypeface(Typeface.DEFAULT);

            // 5. Statistics
            paint.setTextSize(screenWidth * 0.06f);
            paint.setColor(Color.WHITE);
            // Dynamic Y positioning
            float scoreY = menuTop + headerCurveHeight + screenHeight * 0.12f;
            canvas.drawText("SCORE: " + score, centerX, scoreY, paint);

            if (score > highScore)
                highScore = score;
            if (level > highLevel)
                highLevel = level;

            // Buttons
            float btnW = menuWidth * 0.7f;
            float btnH = screenHeight * 0.07f;

            // REBOOT LEVEL
            float rebootY = scoreY + screenHeight * 0.1f;
            drawNeonButton(canvas, "REBOOT LEVEL", centerX, rebootY, btnW, btnH, Color.CYAN);

            // HALL OF FAME
            float hallY = rebootY + screenHeight * 0.11f;
            drawNeonButton(canvas, "HALL OF FAME", centerX, hallY, btnW, btnH, Color.rgb(255, 215, 0));

            // MAIN MENU
            float menuY = hallY + screenHeight * 0.11f;
            drawNeonButton(canvas, "MAIN MENU", centerX, menuY, btnW, btnH, Color.rgb(255, 100, 100));
        }

        // Draw In-Game Menu Icon logic
        if (gameStarted && !gameOver) {
            drawInGameMenuIcon(canvas);
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
        paint.setAlpha(230);
        canvas.drawRect(0, 0, screenWidth, screenHeight, paint);
        paint.setAlpha(255);

        // Ana panel (Glassmorphism)
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.argb(220, 20, 20, 40)); // Koyu arka plan

        float panelWidth = screenWidth * 0.85f;
        float panelHeight = screenHeight * 0.7f; // Biraz daha kompakt
        float panelTop = centerY - panelHeight / 2;
        float panelBottom = centerY + panelHeight / 2;

        canvas.drawRoundRect(
                centerX - panelWidth / 2, panelTop,
                centerX + panelWidth / 2, panelBottom,
                40, 40, paint);

        // Panel kenarlığı (Neon glow)
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(5);
        paint.setColor(Color.rgb(0, 255, 255)); // Cyan
        paint.setShadowLayer(20, 0, 0, Color.CYAN);
        canvas.drawRoundRect(
                centerX - panelWidth / 2, panelTop,
                centerX + panelWidth / 2, panelBottom,
                40, 40, paint);
        paint.clearShadowLayer();

        // Başlık
        paint.setStyle(Paint.Style.FILL);
        paint.setTextSize(screenWidth * 0.09f);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setColor(Color.rgb(0, 255, 255));
        paint.setShadowLayer(15, 0, 0, Color.CYAN);
        canvas.drawText("HOW TO PLAY", centerX, panelTop + panelHeight * 0.12f, paint);
        paint.clearShadowLayer();

        // Separator
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2);
        paint.setColor(Color.MAGENTA);
        canvas.drawLine(centerX - panelWidth * 0.3f, panelTop + panelHeight * 0.15f,
                centerX + panelWidth * 0.3f, panelTop + panelHeight * 0.15f, paint);

        // Talimatlar - Renkli toplar ve açıklamalar
        paint.setStyle(Paint.Style.FILL);
        paint.setTextSize(screenWidth * 0.038f); // Biraz daha okunaklı boyut
        paint.setTextAlign(Paint.Align.LEFT);
        paint.setShadowLayer(0, 0, 0, 0);

        float startY = panelTop + panelHeight * 0.22f;
        float lineSpacing = panelHeight * 0.075f; // Panele göre orantılı aralık
        float ballSize = screenWidth * 0.035f;
        float ballX = centerX - panelWidth * 0.35f;
        float textX = ballX + ballSize * 2.5f;

        // Liste verileri
        int[] colors = {
                Color.rgb(255, 165, 0), Color.rgb(255, 215, 0), Color.BLUE, Color.CYAN,
                Color.rgb(255, 192, 203), Color.rgb(173, 216, 230), Color.RED, Color.GREEN, Color.rgb(139, 0, 0)
        };
        String[] descs = {
                "Extra Time: +5 Seconds", "Power Boost: Strong shot", "Barrier: Shield protection",
                "Electric: Chain reaction", "Clone: Duplicate ball", "Freeze: Stop movement",
                "Missile: Homing attack", "Teleport: Instant jump", "Boom: Area explosion"
        };

        for (int i = 0; i < colors.length; i++) {
            float y = startY + i * lineSpacing;

            // Top
            paint.setColor(colors[i]);
            canvas.drawCircle(ballX, y - ballSize / 2, ballSize, paint);

            // Neon glow efektli top kenarı
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(2);
            paint.setColor(Color.WHITE);
            paint.setAlpha(100);
            canvas.drawCircle(ballX, y - ballSize / 2, ballSize, paint);
            paint.setAlpha(255);
            paint.setStyle(Paint.Style.FILL);

            // Yazı
            paint.setColor(Color.WHITE);
            canvas.drawText(descs[i], textX, y, paint);
        }

        // Close butonu
        paint.setTextSize(screenWidth * 0.05f);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setColor(Color.rgb(255, 80, 80));
        // Panelin altına yakın ama içinde
        canvas.drawText("TAP TO CLOSE", centerX, panelBottom - panelHeight * 0.05f, paint);
    }

    private void drawHighScoreOverlay(Canvas canvas) {
        // Yarı saydam arka plan
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.BLACK);
        paint.setAlpha(220);
        canvas.drawRect(0, 0, screenWidth, screenHeight, paint);
        paint.setAlpha(255);

        // Ana panel (Glassmorphism)
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.argb(170, 25, 25, 50)); // Koyu mavi-mor
        float panelWidth = screenWidth * 0.85f;
        float panelHeight = screenHeight * 0.6f;
        canvas.drawRoundRect(
                centerX - panelWidth / 2, centerY - panelHeight / 2,
                centerX + panelWidth / 2, centerY + panelHeight / 2,
                35, 35, paint);

        // Panel kenarlığı (Neon glow)
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(3);
        paint.setColor(Color.rgb(255, 215, 0));
        paint.setShadowLayer(18, 0, 0, Color.rgb(255, 215, 0));
        canvas.drawRoundRect(
                centerX - panelWidth / 2, centerY - panelHeight / 2,
                centerX + panelWidth / 2, centerY + panelHeight / 2,
                35, 35, paint);
        paint.clearShadowLayer();

        // Başlık
        paint.setStyle(Paint.Style.FILL);
        paint.setTextSize(screenWidth * 0.1f);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setColor(Color.rgb(255, 215, 0));
        canvas.drawText("HALL OF FAME", centerX, centerY - screenHeight * 0.2f, paint);

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

    private void drawNeonButton(Canvas canvas, String text, float cx, float cy, float w, float h, int color) {
        float density = getResources().getDisplayMetrics().density;
        android.graphics.RectF rect = new android.graphics.RectF(cx - w / 2, cy - h / 2, cx + w / 2, cy + h / 2);
        float radius = 12 * density;

        // 1. Shadow Layer (Bottom depth)
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.BLACK);
        paint.setAlpha(80);
        android.graphics.RectF shadowRect = new android.graphics.RectF(rect.left + 2 * density, rect.top + 3 * density,
                rect.right + 2 * density, rect.bottom + 3 * density);
        canvas.drawRoundRect(shadowRect, radius, radius, paint);

        // 2. Main Body with Gradient
        int topColor = lightenColor(color, 0.2f);
        int bottomColor = darkenColor(color, 0.3f);
        android.graphics.Shader gradient = new android.graphics.LinearGradient(
                0, rect.top, 0, rect.bottom,
                topColor, bottomColor, android.graphics.Shader.TileMode.CLAMP);
        paint.setShader(gradient);
        paint.setAlpha(255);
        canvas.drawRoundRect(rect, radius, radius, paint);
        paint.setShader(null);

        // 3. Top Accent Bar
        float accentH = h * 0.25f;
        android.graphics.RectF accentRect = new android.graphics.RectF(rect.left + 8 * density, rect.top + 4 * density,
                rect.right - 8 * density, rect.top + accentH);
        paint.setColor(Color.WHITE);
        paint.setAlpha(60);
        canvas.drawRoundRect(accentRect, radius * 0.6f, radius * 0.6f, paint);

        // 4. Outer Glow
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(color);
        paint.setAlpha(100);
        paint.setStrokeWidth(6f);
        canvas.drawRoundRect(rect, radius, radius, paint);

        // 5. Border
        paint.setColor(lightenColor(color, 0.3f));
        paint.setAlpha(255);
        paint.setStrokeWidth(3f);
        canvas.drawRoundRect(rect, radius, radius, paint);

        // 6. Text
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.WHITE);
        paint.setTextSize(h * 0.4f);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setFakeBoldText(true);
        paint.setShadowLayer(8, 0, 2, Color.argb(150, 0, 0, 0));

        Paint.FontMetrics fm = paint.getFontMetrics();
        float textY = cy - (fm.descent + fm.ascent) / 2;

        canvas.drawText(text, cx, textY, paint);
        paint.clearShadowLayer();
        paint.setFakeBoldText(false);
    }

    private void drawBlackHole(Canvas canvas, float cx, float cy, float radius) {
        paint.setStyle(Paint.Style.STROKE);

        // Outer glow
        paint.setStrokeWidth(radius * 0.1f);
        paint.setColor(Color.rgb(138, 43, 226)); // Purple
        paint.setShadowLayer(30, 0, 0, Color.MAGENTA);
        canvas.drawCircle(cx, cy, radius * 1.1f, paint);

        // Inner rings
        paint.setStrokeWidth(radius * 0.08f);
        paint.setColor(Color.rgb(75, 0, 130)); // Indigo
        canvas.drawCircle(cx, cy, radius * 0.9f, paint);

        paint.setStrokeWidth(radius * 0.05f);
        paint.setColor(Color.rgb(148, 0, 211)); // Dark Violet
        canvas.drawCircle(cx, cy, radius * 0.7f, paint);
        paint.clearShadowLayer();

        // Event Horizon
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(4f);
        paint.setColor(Color.WHITE);
        paint.setShadowLayer(15, 0, 0, Color.WHITE);
        canvas.drawCircle(cx, cy, radius * 0.5f, paint);
        paint.clearShadowLayer();

        // The Void
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.BLACK);
        canvas.drawCircle(cx, cy, radius * 0.48f, paint);

        // Accretion Disk (Elliptical)
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(3f);
        paint.setColor(Color.rgb(255, 100, 50));
        paint.setShadowLayer(8, 0, 0, Color.RED);
        android.graphics.RectF diskRect = new android.graphics.RectF(cx - radius * 1.4f, cy - radius * 0.3f,
                cx + radius * 1.4f, cy + radius * 0.3f);

        canvas.save();
        canvas.rotate(-20, cx, cy); // Tilted
        canvas.drawOval(diskRect, paint);
        canvas.restore();
        paint.clearShadowLayer();
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
                    updateMenuButtonsVisibility();
                    return true;
                }

                if (showLevelSelector) {
                    // Arrow positions (below the grid)
                    float arrowRadius = screenWidth * 0.08f;
                    float gridStartY = screenHeight * 0.42f;
                    float cellHeight = screenWidth * 0.14f;
                    float gap = screenWidth * 0.02f;
                    float gridEndY = gridStartY + 2 * (cellHeight + gap);
                    float arrowY = gridEndY + arrowRadius + 30 * getResources().getDisplayMetrics().density;

                    // Left Arrow
                    float leftX = screenWidth * 0.25f;
                    float distLeft = (float) Math
                            .sqrt((touchX - leftX) * (touchX - leftX) + (touchY - arrowY) * (touchY - arrowY));
                    if (distLeft < arrowRadius * 1.5f && selectorPage > 1) {
                        selectorPage--;
                        playSound(soundLaunch);
                        return true;
                    }

                    // Right Arrow
                    float rightX = screenWidth * 0.75f;
                    float distRight = (float) Math
                            .sqrt((touchX - rightX) * (touchX - rightX) + (touchY - arrowY) * (touchY - arrowY));
                    if (distRight < arrowRadius * 1.5f && selectorPage < 10) { // Limit to 10 pages
                        selectorPage++;
                        playSound(soundLaunch);
                        return true;
                    }

                    // BACK Button (updated position)
                    float backBtnW = screenWidth * 0.5f;
                    float backBtnH = screenHeight * 0.065f;
                    float backBtnY = screenHeight * 0.80f;
                    if (touchX > centerX - backBtnW / 2 && touchX < centerX + backBtnW / 2 &&
                            touchY > backBtnY - backBtnH / 2 && touchY < backBtnY + backBtnH / 2) {
                        showLevelSelector = false;
                        updateMenuButtonsVisibility();
                        return true;
                    }

                    // Level Buttons (centered grid)
                    float totalGridWidth = 5 * screenWidth * 0.14f + 4 * screenWidth * 0.02f;
                    float gridStartX = centerX - totalGridWidth / 2;
                    float cellWidth = screenWidth * 0.14f;

                    for (int i = 0; i < 10; i++) {
                        int row = i / 5;
                        int col = i % 5;
                        float btnX = gridStartX + col * (cellWidth + gap) + cellWidth / 2;
                        float btnY = gridStartY + row * (cellHeight + gap) + cellHeight / 2;

                        if (Math.abs(touchX - btnX) < cellWidth / 2 && Math.abs(touchY - btnY) < cellHeight / 2) {
                            int selectedLv = (selectorPage - 1) * 10 + i + 1;
                            if (selectedLv <= maxUnlockedLevel) {
                                gameStarted = true;
                                updateMenuButtonsVisibility();
                                // Seçilen level'in ilk stage'ini başlat
                                level = (selectedLv - 1) * 5 + 1;
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
                    // START GAME butonu kontrolü - removed as it's a NeonButton now

                    // HOW TO PLAY butonu kontrolü - removed as it's a NeonButton now
                } else if (gameOver) {
                    float menuHeight = screenHeight * 0.55f;
                    float menuTop = (screenHeight - menuHeight) / 2 + screenHeight * 0.03f;
                    float headerCurveHeight = 60f;
                    float menuWidth = screenWidth * 0.75f;
                    float btnW = menuWidth * 0.7f;
                    float btnH = screenHeight * 0.07f;
                    float scoreY = menuTop + headerCurveHeight + screenHeight * 0.12f;

                    // REBOOT LEVEL (Restart current Level)
                    float rebootY = scoreY + screenHeight * 0.1f;
                    if (Math.abs(touchX - centerX) < btnW / 2 && Math.abs(touchY - rebootY) < btnH / 2) {
                        gameOver = false;
                        lives = 3;
                        initLevel(level); // Restart current level
                        updateMenuButtonsVisibility();
                        return true;
                    }

                    // HALL OF FAME
                    float hallY = rebootY + screenHeight * 0.11f;
                    if (Math.abs(touchX - centerX) < btnW / 2 && Math.abs(touchY - hallY) < btnH / 2) {
                        showHighScore = true;
                        updateMenuButtonsVisibility();
                        return true;
                    }

                    // MAIN MENU
                    float menuY = hallY + screenHeight * 0.11f;
                    if (Math.abs(touchX - centerX) < btnW / 2 && Math.abs(touchY - menuY) < btnH / 2) {
                        gameOver = false;
                        gameStarted = false;
                        score = 0;
                        initLevel(1);
                        updateMenuButtonsVisibility();
                        return true;
                    }

                } else if (gameCompleted) {
                    float btnW = screenWidth * 0.5f;
                    float btnH = screenHeight * 0.07f;
                    float btnY = screenHeight * 0.75f;

                    if (Math.abs(touchX - centerX) < btnW / 2 && Math.abs(touchY - btnY) < btnH / 2) {
                        gameCompleted = false;
                        gameStarted = false;
                        score = 0;
                        initLevel(1);
                        maxUnlockedLevel = Math.min(maxUnlockedLevel, 100); // just safety
                        updateMenuButtonsVisibility();
                        playSound(soundLaunch);
                        return true;
                    }
                } else {
                    // Check In-Game Menu Icon touch only when game is running and not paused/over
                    float density = getResources().getDisplayMetrics().density;
                    float iconSize = screenWidth * 0.1f;
                    float iconX = screenWidth - iconSize * 0.8f;
                    float iconY = screenHeight * 0.18f; // Adjusted position (slightly lower)

                    if (Math.abs(touchX - iconX) < iconSize / 2 && Math.abs(touchY - iconY) < iconSize / 2) {
                        gameStarted = false; // Go back to main menu
                        updateMenuButtonsVisibility();
                        playSound(soundLaunch);
                        return true;
                    }

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
                        touchedBall.trail.clear(); // Drag başladığında eski izi temizle
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
        reloadPreferences();
        isPlaying = true;
        gameThread = new Thread(this);
        gameThread.start();
    }

    private void drawInGameMenuIcon(Canvas canvas) {
        float density = getResources().getDisplayMetrics().density;
        float iconSize = screenWidth * 0.1f;
        float x = screenWidth - iconSize * 0.8f;
        float y = screenHeight * 0.18f;

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.argb(150, 40, 40, 60)); // Dark semi-transparent bg
        canvas.drawCircle(x, y, iconSize / 2, paint);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(3f);
        paint.setColor(Color.CYAN);
        paint.setShadowLayer(10, 0, 0, Color.CYAN);
        canvas.drawCircle(x, y, iconSize / 2, paint);
        paint.clearShadowLayer();

        // Home Icon (simple house shape)
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(4f);
        paint.setColor(Color.WHITE);

        Path homePath = new Path();
        float w = iconSize * 0.5f;
        // Roof
        homePath.moveTo(x - w / 2, y + w * 0.1f); // Left base textY
        homePath.lineTo(x, y - w * 0.3f); // Top
        homePath.lineTo(x + w / 2, y + w * 0.1f); // Right base
        // Body
        homePath.moveTo(x - w / 3, y + w * 0.1f);
        homePath.lineTo(x - w / 3, y + w * 0.4f);
        homePath.lineTo(x + w / 3, y + w * 0.4f);
        homePath.lineTo(x + w / 3, y + w * 0.1f);

        canvas.drawPath(homePath, paint);
    }

    // İç sınıflar
    class Ball {
        float x, y, vx, vy, radius;
        int color;
        long creationTime;
        long lifetime; // Clone topları için yaşam süresi (ms)
        boolean isClone; // Clone topu mu?
        ArrayList<TrailPoint> trail = new ArrayList<>();

        Ball(float x, float y, float radius, int color) {
            this.x = x;
            this.y = y;
            this.radius = radius;
            this.color = color;
            this.vx = 0;
            this.vy = 0;
            this.creationTime = System.currentTimeMillis();
            this.lifetime = 0;
            this.isClone = false;
        }

        Ball(float x, float y, float radius, int color, long lifetime) {
            this(x, y, radius, color);
            this.lifetime = lifetime;
            this.isClone = true;
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

        // Persist maxUnlockedLevel
        editor.putInt("maxUnlockedLevel", maxUnlockedLevel);

        editor.putInt("maxCombo", maxCombo);

        // Coin'leri kaydet
        editor.putInt("coins", coins);

        editor.apply();
    }

    public int getCoins() {
        return coins;
    }

    public void refreshCoins() {
        SharedPreferences prefs = getContext().getSharedPreferences("SpaceBilliard", Context.MODE_PRIVATE);
        coins = prefs.getInt("coins", 0);
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

        // Sayfa Göstergesi (Örn: SPACE 1)
        paint.setTextSize(screenWidth * 0.05f);
        paint.setColor(Color.WHITE);
        canvas.drawText("SPACE " + selectorPage, centerX, screenHeight * 0.28f, paint);

        // Sayfa bilgisi (Her sayfa bir level grubu temsil eder)
        paint.setTextSize(screenWidth * 0.04f);
        paint.setColor(Color.LTGRAY);
        canvas.drawText("Each level contains 5 stages", centerX, screenHeight * 0.32f, paint);

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

        // Calculate grid end position
        float gridEndY = gridStartY + 2 * (cellHeight + gap);

        // Premium Navigation Arrows (Below the grid)
        float arrowRadius = screenWidth * 0.08f;
        float arrowY = gridEndY + arrowRadius + 30 * getResources().getDisplayMetrics().density;

        // Left Arrow (if not on first page)
        if (selectorPage > 1) {
            float leftX = screenWidth * 0.25f;
            drawPremiumArrowButton(canvas, leftX, arrowY, arrowRadius, true);
        }

        // Right Arrow (Limit to 10 pages)
        if (selectorPage < 10) {
            float rightX = screenWidth * 0.75f;
            drawPremiumArrowButton(canvas, rightX, arrowY, arrowRadius, false);
        }

        // Premium BACK Button (Neon style matching reference)
        float backBtnW = screenWidth * 0.5f;
        float backBtnH = screenHeight * 0.065f;
        float backBtnY = screenHeight * 0.80f;
        float backRadius = backBtnH / 2f;

        // Shadow
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.BLACK);
        paint.setAlpha(100);
        canvas.drawRoundRect(centerX - backBtnW / 2 + 3, backBtnY - backBtnH / 2 + 3,
                centerX + backBtnW / 2 + 3, backBtnY + backBtnH / 2 + 3,
                backRadius, backRadius, paint);
        paint.setAlpha(255);

        // Outer glow
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(8f);
        paint.setColor(Color.RED);
        paint.setAlpha(80);
        canvas.drawRoundRect(centerX - backBtnW / 2, backBtnY - backBtnH / 2,
                centerX + backBtnW / 2, backBtnY + backBtnH / 2,
                backRadius, backRadius, paint);
        paint.setAlpha(255);

        // Gradient background (dark to light red)
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.rgb(120, 20, 30));
        canvas.drawRoundRect(centerX - backBtnW / 2, backBtnY - backBtnH / 2,
                centerX + backBtnW / 2, backBtnY + backBtnH / 2,
                backRadius, backRadius, paint);

        // Inner highlight
        paint.setColor(Color.rgb(180, 40, 50));
        RectF innerRect = new RectF(centerX - backBtnW / 2 + 8, backBtnY - backBtnH / 2 + 8,
                centerX + backBtnW / 2 - 8, backBtnY + backBtnH / 2 - 8);
        canvas.drawRoundRect(innerRect, backRadius * 0.8f, backRadius * 0.8f, paint);

        // Neon border
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(3f);
        paint.setColor(Color.rgb(255, 80, 100));
        paint.setShadowLayer(15, 0, 0, Color.RED);
        canvas.drawRoundRect(centerX - backBtnW / 2, backBtnY - backBtnH / 2,
                centerX + backBtnW / 2, backBtnY + backBtnH / 2,
                backRadius, backRadius, paint);
        paint.clearShadowLayer();

        // Inner neon line
        paint.setStrokeWidth(1.5f);
        paint.setColor(Color.rgb(255, 150, 150));
        paint.setAlpha(150);
        canvas.drawRoundRect(innerRect, backRadius * 0.8f, backRadius * 0.8f, paint);
        paint.setAlpha(255);

        // Text with glow
        paint.setStyle(Paint.Style.FILL);
        paint.setTextSize(screenWidth * 0.05f);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setColor(Color.WHITE);
        paint.setShadowLayer(10, 0, 0, Color.RED);
        canvas.drawText("BACK", centerX, backBtnY + screenWidth * 0.018f, paint);
        paint.clearShadowLayer();
    }

    private void drawPremiumArrowButton(Canvas canvas, float cx, float cy, float radius, boolean isLeft) {
        float triangleSize = radius * 1.2f;

        // Create triangle path
        Path trianglePath = new Path();

        if (isLeft) {
            // Left-pointing triangle
            trianglePath.moveTo(cx - triangleSize * 0.6f, cy); // Left point
            trianglePath.lineTo(cx + triangleSize * 0.4f, cy - triangleSize * 0.7f); // Top right
            trianglePath.lineTo(cx + triangleSize * 0.4f, cy + triangleSize * 0.7f); // Bottom right
            trianglePath.close();
        } else {
            // Right-pointing triangle
            trianglePath.moveTo(cx + triangleSize * 0.6f, cy); // Right point
            trianglePath.lineTo(cx - triangleSize * 0.4f, cy - triangleSize * 0.7f); // Top left
            trianglePath.lineTo(cx - triangleSize * 0.4f, cy + triangleSize * 0.7f); // Bottom left
            trianglePath.close();
        }

        // Shadow layer
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.BLACK);
        paint.setAlpha(120);
        canvas.save();
        canvas.translate(4, 4);
        canvas.drawPath(trianglePath, paint);
        canvas.restore();
        paint.setAlpha(255);

        // Outer glow (multiple layers for intensity)
        paint.setStyle(Paint.Style.STROKE);
        for (int i = 3; i >= 1; i--) {
            paint.setStrokeWidth(i * 8f);
            paint.setColor(Color.CYAN);
            paint.setAlpha(60 / i);
            canvas.drawPath(trianglePath, paint);
        }
        paint.setAlpha(255);

        // Main fill with gradient effect (dark to light blue)
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.rgb(10, 40, 80));
        canvas.drawPath(trianglePath, paint);

        // Inner bright fill (smaller triangle for depth)
        Path innerPath = new Path();
        float innerScale = 0.7f;

        if (isLeft) {
            innerPath.moveTo(cx - triangleSize * 0.6f * innerScale, cy);
            innerPath.lineTo(cx + triangleSize * 0.4f * innerScale, cy - triangleSize * 0.7f * innerScale);
            innerPath.lineTo(cx + triangleSize * 0.4f * innerScale, cy + triangleSize * 0.7f * innerScale);
        } else {
            innerPath.moveTo(cx + triangleSize * 0.6f * innerScale, cy);
            innerPath.lineTo(cx - triangleSize * 0.4f * innerScale, cy - triangleSize * 0.7f * innerScale);
            innerPath.lineTo(cx - triangleSize * 0.4f * innerScale, cy + triangleSize * 0.7f * innerScale);
        }
        innerPath.close();

        paint.setColor(Color.rgb(30, 120, 180));
        canvas.drawPath(innerPath, paint);

        // Neon border (bright cyan)
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(4f);
        paint.setColor(Color.CYAN);
        paint.setShadowLayer(15, 0, 0, Color.CYAN);
        canvas.drawPath(trianglePath, paint);
        paint.clearShadowLayer();

        // Inner neon highlight
        paint.setStrokeWidth(2f);
        paint.setColor(Color.rgb(100, 220, 255));
        paint.setShadowLayer(10, 0, 0, Color.rgb(100, 220, 255));
        canvas.drawPath(innerPath, paint);
        paint.clearShadowLayer();

        // Extra bright edge highlight (top edge)
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(3f);
        paint.setColor(Color.WHITE);
        paint.setAlpha(180);

        Path highlightPath = new Path();
        if (isLeft) {
            highlightPath.moveTo(cx - triangleSize * 0.6f, cy);
            highlightPath.lineTo(cx + triangleSize * 0.4f, cy - triangleSize * 0.7f);
        } else {
            highlightPath.moveTo(cx + triangleSize * 0.6f, cy);
            highlightPath.lineTo(cx - triangleSize * 0.4f, cy - triangleSize * 0.7f);
        }

        paint.setShadowLayer(8, 0, 0, Color.WHITE);
        canvas.drawPath(highlightPath, paint);
        paint.clearShadowLayer();
        paint.setAlpha(255);
    }

    private void drawHangingDecoration(Canvas canvas, float x, float y, float length) {
        // Rope
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(4f);
        paint.setColor(Color.parseColor("#E066FF")); // Pinkish rope

        Path rope = new Path();
        rope.moveTo(x, y);

        // Twisted rope effect (sine wave)
        for (float i = 0; i < length; i += 5) {
            float twist = (float) Math.sin(i * 0.2f) * 3;
            rope.lineTo(x + twist, y + i);
        }
        canvas.drawPath(rope, paint);

        // Jewel/Heart at bottom
        float jewelY = y + length + 15;
        float size = 15f;

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.MAGENTA); // Outer glow
        paint.setShadowLayer(15, 0, 0, Color.MAGENTA);

        Path heart = new Path();
        heart.moveTo(x, jewelY - size / 2);
        heart.quadTo(x - size, jewelY - size, x - size, jewelY + size / 2);
        heart.lineTo(x, jewelY + size * 1.5f);
        heart.lineTo(x + size, jewelY + size / 2);
        heart.quadTo(x + size, jewelY - size, x, jewelY - size / 2);
        heart.close();

        canvas.drawPath(heart, paint);

        // Inner white shine
        paint.setColor(Color.WHITE);
        paint.clearShadowLayer();
        paint.setAlpha(200);
        canvas.drawCircle(x, jewelY, 4, paint);
        paint.setAlpha(255);
    }

    private void drawShipWheel(Canvas canvas, float cx, float cy, float radius) {
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(Color.parseColor("#FFD700")); // Gold
        paint.setStrokeWidth(radius * 0.15f);
        paint.setShadowLayer(15, 0, 0, Color.parseColor("#FFA500")); // Orange glow

        // Outer ring
        canvas.drawCircle(cx, cy, radius, paint);

        // Inner ring
        paint.setStrokeWidth(radius * 0.08f);
        canvas.drawCircle(cx, cy, radius * 0.4f, paint);

        // Spokes
        for (int i = 0; i < 8; i++) {
            double angle = Math.toRadians(i * 45);
            float startX = cx + (float) Math.cos(angle) * radius * 0.4f;
            float startY = cy + (float) Math.sin(angle) * radius * 0.4f;
            float endX = cx + (float) Math.cos(angle) * (radius * 1.3f); // Extend beyond ring regarding handles
            float endY = cy + (float) Math.sin(angle) * (radius * 1.3f);

            // Draw handle knob
            canvas.drawLine(startX, startY, endX, endY, paint);

            paint.setStyle(Paint.Style.FILL);
            canvas.drawCircle(endX, endY, radius * 0.12f, paint);
            paint.setStyle(Paint.Style.STROKE); // Revert to stroke
        }

        // Center hub
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.parseColor("#FF8C00")); // Darker orange center
        canvas.drawCircle(cx, cy, radius * 0.25f, paint);

        // Reflection
        paint.setColor(Color.WHITE);
        paint.setAlpha(150);
        paint.clearShadowLayer();
        canvas.drawArc(new RectF(cx - radius * 0.15f, cy - radius * 0.15f,
                cx + radius * 0.15f, cy + radius * 0.15f),
                200, 90, false, paint);
        paint.setAlpha(255);
    }

    enum ParticleType {
        CIRCLE, STAR, FLAME, CONFETTI, RIPPLE
    }

    class Particle {
        float x, y, vx, vy;
        int color, life = 30, maxLife = 30;
        ParticleType type = ParticleType.CIRCLE;
        float rotation = 0, vRotation = 0;

        Particle(float x, float y, float angle, float speed, int color) {
            this.x = x;
            this.y = y;
            this.vx = (float) Math.cos(angle) * speed;
            this.vy = (float) Math.sin(angle) * speed;
            this.color = color;
        }

        Particle(float x, float y, float angle, float speed, int color, ParticleType type) {
            this(x, y, angle, speed, color);
            this.type = type;
            if (type == ParticleType.STAR) {
                this.vRotation = (random.nextFloat() - 0.5f) * 0.2f;
                this.maxLife = 40;
                this.life = 40;
            } else if (type == ParticleType.FLAME) {
                this.maxLife = 20 + random.nextInt(15);
                this.life = this.maxLife;
                // Flames move mostly up
                this.vx = (random.nextFloat() - 0.5f) * 2f;
                this.vy = -random.nextFloat() * 4f - 2f;
            } else if (type == ParticleType.CONFETTI) {
                this.vRotation = (random.nextFloat() - 0.5f) * 0.5f;
                this.maxLife = 50;
                this.life = 50;
                // Confetti slows down
                this.vx *= 0.8f;
                this.vy *= 0.8f;
            } else if (type == ParticleType.RIPPLE) {
                this.vx = 0;
                this.vy = 0; // Ripples stay in place
                this.maxLife = 40;
                this.life = 40;
            }
        }

        void update() {
            x += vx;
            y += vy;
            rotation += vRotation;
            life--;

            if (type == ParticleType.CONFETTI) {
                // Gravityish/Air resistance
                vx *= 0.95f;
                vy *= 0.95f;
                vy += 0.2f; // Gravity
            }
        }

        boolean isDead() {
            return life <= 0;
        }

        void draw(Canvas canvas, Paint paint) {
            paint.setStyle(Paint.Style.FILL);
            paint.setAlpha((int) (255 * (life / (float) maxLife)));

            if (type == ParticleType.STAR) {
                paint.setColor(color);
                drawStar(canvas, x, y, 8 * (life / (float) maxLife), rotation, paint);
            } else if (type == ParticleType.FLAME) {
                // Gradient fire color based on life
                float ratio = (float) life / maxLife;
                int r = 255;
                int g = (int) (255 * ratio);
                int b = (int) (100 * (ratio * ratio));
                paint.setColor(Color.rgb(r, g, b));
                paint.setShadowLayer(15 * ratio, 0, 0, Color.rgb(r, g, b));
                canvas.drawCircle(x, y, 12 * ratio, paint);
                paint.clearShadowLayer();
            } else if (type == ParticleType.CONFETTI) {
                paint.setColor(color);
                canvas.save();
                canvas.rotate((float) Math.toDegrees(rotation), x, y);
                canvas.drawRect(x - 5, y - 5, x + 5, y + 5, paint);
                canvas.restore();
            } else if (type == ParticleType.RIPPLE) {
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(5 * (life / (float) maxLife));
                paint.setColor(color);
                float radius = 100 * (1.0f - life / (float) maxLife);
                canvas.drawCircle(x, y, radius, paint);
                // Second ripple
                if (life < maxLife - 10) {
                    float radius2 = 100 * (1.0f - (life + 10) / (float) maxLife);
                    canvas.drawCircle(x, y, radius2 * 0.7f, paint);
                }
            } else {
                paint.setColor(color);
                canvas.drawCircle(x, y, 4, paint);
            }
            paint.setAlpha(255);
        }

        private void drawStar(Canvas canvas, float cx, float cy, float r, float rot, Paint p) {
            android.graphics.Path path = new android.graphics.Path();
            for (int i = 0; i < 5; i++) {
                float angle = (float) (i * 2 * Math.PI / 5 + rot - Math.PI / 2);
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
    }

    class ImpactArc {
        float x1, y1, x2, y2;
        int color, life = 15, maxLife = 15;
        java.util.ArrayList<PointF> points = new java.util.ArrayList<>();

        ImpactArc(float x, float y, float angle, float length, int color) {
            this.color = color;
            float endX = x + (float) Math.cos(angle) * length;
            float endY = y + (float) Math.sin(angle) * length;

            points.add(new PointF(x, y));
            int segments = 3;
            for (int i = 1; i < segments; i++) {
                float px = x + (endX - x) * i / segments + (random.nextFloat() - 0.5f) * 20;
                float py = y + (endY - y) * i / segments + (random.nextFloat() - 0.5f) * 20;
                points.add(new PointF(px, py));
            }
            points.add(new PointF(endX, endY));
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
            paint.setColor(color);
            paint.setAlpha((int) (200 * (life / (float) maxLife)));
            paint.setShadowLayer(10, 0, 0, color);

            for (int i = 0; i < points.size() - 1; i++) {
                canvas.drawLine(points.get(i).x, points.get(i).y, points.get(i + 1).x, points.get(i + 1).y, paint);
            }
            paint.clearShadowLayer();
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
                    createImpactBurst(ball.x, ball.y, ball.color);
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

    // Meteor sınıfı (Space 2 için)
    private class Meteor {
        float x, y, size, vx, vy;
        int color;

        Meteor() {
            Random r = new Random();
            x = r.nextFloat() * 2000;
            y = r.nextFloat() * 3000;
            size = r.nextFloat() * 5 + 3; // Biraz daha büyük
            int shade = r.nextInt(100) + 50; // Koyu gri tonları
            color = Color.rgb(shade, shade, shade + 20); // Hafif mavimsi gri
            // Movement logic
            vx = (r.nextFloat() - 0.5f) * 4; // Horizontal drift
            vy = r.nextFloat() * 5 + 2; // Vertical falling
        }

        void update(int width, int height) {
            x += vx;
            y += vy;

            // Wrap around screen
            if (x < -50)
                x = width + 50;
            if (x > width + 50)
                x = -50;
            if (y > height + 50)
                y = -50;
        }

        void draw(Canvas canvas, Paint paint) {
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(color);
            canvas.drawCircle(x, y, size, paint);

            // Kuyruk efekti (basit)
            paint.setAlpha(100);
            canvas.drawCircle(x - size, y - size, size * 0.8f, paint);
            paint.setAlpha(255);
        }
    }

    private void drawMoon(Canvas canvas) {
        float moonRadius = screenWidth * 0.15f;
        float moonX = screenWidth * 0.85f;
        float moonY = screenHeight * 0.85f; // Bottom Right

        // Rotation Animation
        float rotation = (System.currentTimeMillis() % 60000) * 0.006f * 360f / 60f; // Smoother rotation? No, just
                                                                                     // linear is fine.
        // Simplified: (time % period) / period * 360
        float angle = (System.currentTimeMillis() % 20000) / 20000f * 360f; // Full circle in 20s

        canvas.save();
        canvas.rotate(angle, moonX, moonY);

        // Glow
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.WHITE);
        paint.setAlpha(30);
        canvas.drawCircle(moonX, moonY, moonRadius * 1.2f, paint);

        // Main Body
        paint.setColor(Color.rgb(220, 220, 230)); // Light Gray
        paint.setAlpha(255);
        canvas.drawCircle(moonX, moonY, moonRadius, paint);

        // Craters (Simple)
        paint.setColor(Color.rgb(180, 180, 190)); // Darker Gray
        canvas.drawCircle(moonX - moonRadius * 0.3f, moonY - moonRadius * 0.2f, moonRadius * 0.2f, paint);
        canvas.drawCircle(moonX + moonRadius * 0.4f, moonY + moonRadius * 0.3f, moonRadius * 0.15f, paint);
        canvas.drawCircle(moonX - moonRadius * 0.1f, moonY + moonRadius * 0.5f, moonRadius * 0.1f, paint);

        canvas.restore();
    }

    private void drawGiantMeteor(Canvas canvas) {
        float meteorRadius = screenWidth * 0.18f;
        float meteorX = screenWidth * 0.85f;
        float meteorY = screenHeight * 0.85f;

        // Rotation
        float angle = (System.currentTimeMillis() % 25000) / 25000f * 360f;

        canvas.save();
        canvas.rotate(angle, meteorX, meteorY);

        // 1. Fiery Aura/Tail effect behind
        paint.setStyle(Paint.Style.FILL);
        if (cachedMeteorAura != null)
            paint.setShader(cachedMeteorAura);
        canvas.drawCircle(meteorX, meteorY, meteorRadius * 1.6f, paint);
        paint.setShader(null);

        // 2. Main Rock Shape (Irregular Polygon)
        // Fill Rock
        paint.setColor(Color.rgb(60, 50, 45)); // Dark Grey-Brown
        paint.setShadowLayer(20, 0, 0, Color.BLACK);
        if (cachedMeteorPath != null)
            canvas.drawPath(cachedMeteorPath, paint);
        paint.clearShadowLayer();

        // 3. Shading (Gradient overlay for 3D effect)
        paint.setStyle(Paint.Style.FILL);
        if (cachedMeteorShading != null)
            paint.setShader(cachedMeteorShading);
        if (cachedXfermode != null)
            paint.setXfermode(cachedXfermode);
        if (cachedMeteorPath != null)
            canvas.drawPath(cachedMeteorPath, paint);
        paint.setXfermode(null);
        paint.setShader(null);

        // 4. Craters (Darker spots)
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.rgb(40, 30, 25));
        canvas.drawCircle(meteorX - meteorRadius * 0.4f, meteorY - meteorRadius * 0.2f, meteorRadius * 0.25f, paint);
        canvas.drawCircle(meteorX + meteorRadius * 0.3f, meteorY + meteorRadius * 0.4f, meteorRadius * 0.2f, paint);
        canvas.drawCircle(meteorX + meteorRadius * 0.1f, meteorY - meteorRadius * 0.5f, meteorRadius * 0.15f, paint);
        canvas.drawCircle(meteorX - meteorRadius * 0.2f, meteorY + meteorRadius * 0.3f, meteorRadius * 0.1f, paint);

        // 5. Molten Cracks (Glowing Orange/Yellow)
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeWidth(meteorRadius * 0.05f);
        paint.setColor(Color.rgb(255, 140, 0)); // Intense Orange
        paint.setShadowLayer(15, 0, 0, Color.RED);

        if (cachedMeteorCracks != null)
            canvas.drawPath(cachedMeteorCracks, paint);
        paint.clearShadowLayer();
        canvas.restore();
    }

    private void drawAuroraNebula(Canvas canvas) {
        // Ultra-lightweight static background for Space 7 (no animation)
        canvas.drawColor(Color.rgb(15, 8, 25)); // Deep purple-blue
    }

    // Kuyruklu Yıldız Sınıfı
    private class Comet {
        float x, y, vx, vy, size;
        int color;
        // Tail
        float[] tailX = new float[10];
        float[] tailY = new float[10];

        Comet() {
            reset(true);
        }

        void reset(boolean randomPos) {
            Random r = new Random();
            if (randomPos) {
                x = r.nextFloat() * 2000;
                y = r.nextFloat() * 3000;
            } else {
                // Determine spawn side
                if (r.nextBoolean()) {
                    x = -50; // Left
                    y = r.nextFloat() * 2000;
                } else {
                    x = r.nextFloat() * 2000;
                    y = -50; // Top
                }
            }

            vx = r.nextFloat() * 8 + 4; // Faster than meteors
            vy = r.nextFloat() * 8 + 4;
            size = r.nextFloat() * 4 + 4;
            color = Color.rgb(200, 240, 255); // Cyan-ish white

            // Init tail
            for (int i = 0; i < 10; i++) {
                tailX[i] = x;
                tailY[i] = y;
            }
        }

        void update(int width, int height) {
            // Shift tail
            for (int i = 9; i > 0; i--) {
                tailX[i] = tailX[i - 1];
                tailY[i] = tailY[i - 1];
            }
            tailX[0] = x;
            tailY[0] = y;

            x += vx;
            y += vy;

            // Reset if out of bounds (far out)
            if (x > width + 200 || y > height + 200) {
                reset(false);
            }
        }

        void draw(Canvas canvas, Paint paint) {
            // Draw Tail
            for (int i = 0; i < 10; i++) {
                float ratio = 1f - (float) i / 10f;
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(color);
                paint.setAlpha((int) (150 * ratio));
                float tSize = size * ratio;
                canvas.drawCircle(tailX[i], tailY[i], tSize, paint);
            }

            // Head
            paint.setAlpha(255);
            paint.setColor(Color.WHITE);
            paint.setShadowLayer(10, 0, 0, color);
            canvas.drawCircle(x, y, size, paint);
            paint.clearShadowLayer();
        }
    }

    private void updateMainActivityPanels() {
        if (mainActivity != null && gameStarted && !gameOver) {
            int currentStage = ((level - 1) % 5) + 1;
            int currentSpace = ((level - 1) / 50) + 1;
            int currentLevelInSpace = ((level - 1) / 5) + 1;

            String levelText = "SPACE " + currentSpace + " - LEVEL " + currentLevelInSpace;
            int power = (int) (lastLaunchPower * (powerBoostActive ? 200 : 100));

            mainActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mainActivity.updatePanels(
                            (int) (timeLeft / 1000),
                            score,
                            coins, // Add coins
                            power,
                            currentStage + "/5",
                            levelText,
                            lives);
                }
            });
        }
    }

    class TrailPoint {
        float x, y, radius;

        TrailPoint(float x, float y, float radius) {
            this.x = x;
            this.y = y;
            this.radius = radius;
        }
    }

    private void drawNeonMenuButton(Canvas canvas, float cx, float cy, float width, float height,
            String text, int color) {
        float radius = height / 2f;
        RectF btnRect = new RectF(cx - width / 2, cy - height / 2, cx + width / 2, cy + height / 2);

        // Shadow
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.BLACK);
        paint.setAlpha(100);
        RectF shadowRect = new RectF(btnRect.left + 3, btnRect.top + 3,
                btnRect.right + 3, btnRect.bottom + 3);
        canvas.drawRoundRect(shadowRect, radius, radius, paint);
        paint.setAlpha(255);

        // Outer glow
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(6f);
        paint.setColor(color);
        paint.setAlpha(80);
        canvas.drawRoundRect(btnRect, radius, radius, paint);
        paint.setAlpha(255);

        // Button background (darker version of color)
        paint.setStyle(Paint.Style.FILL);
        int r = Color.red(color) / 3;
        int g = Color.green(color) / 3;
        int b = Color.blue(color) / 3;
        paint.setColor(Color.rgb(r, g, b));
        canvas.drawRoundRect(btnRect, radius, radius, paint);

        // Inner highlight
        RectF innerRect = new RectF(btnRect.left + 6, btnRect.top + 6,
                btnRect.right - 6, btnRect.bottom - 6);
        r = Color.red(color) / 2;
        g = Color.green(color) / 2;
        b = Color.blue(color) / 2;
        paint.setColor(Color.rgb(r, g, b));
        canvas.drawRoundRect(innerRect, radius * 0.8f, radius * 0.8f, paint);

        // Neon border
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(3f);
        paint.setColor(color);
        paint.setShadowLayer(12, 0, 0, color);
        canvas.drawRoundRect(btnRect, radius, radius, paint);
        paint.clearShadowLayer();

        // Text
        paint.setStyle(Paint.Style.FILL);
        paint.setTextSize(height * 0.5f);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setColor(Color.WHITE);
        paint.setShadowLayer(8, 0, 0, color);
        canvas.drawText(text, cx, cy + height * 0.18f, paint);
        paint.clearShadowLayer();
    }

    // Floating Text Class for animated UI labels
    class FloatingText {
        String text;
        float x, y;
        int color;
        int life = 60; // Frames
        int maxLife = 60;
        float floatSpeed = 3f;

        FloatingText(String text, float x, float y, int color) {
            this.text = text;
            this.x = x;
            this.y = y;
            this.color = color;
        }

        void update() {
            y -= floatSpeed; // Move up
            life--;
        }

        boolean isDead() {
            return life <= 0;
        }

        void draw(Canvas canvas, Paint paint) {
            paint.setStyle(Paint.Style.FILL);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD_ITALIC));

            // Calculate sizes and alpha
            float ratio = (float) life / maxLife;
            paint.setTextSize(screenWidth * 0.12f * (0.8f + 0.2f * ratio)); // Shrink slightly as it fades? Or grow?
                                                                            // Let's stay mostly constant or subtle pop
            paint.setAlpha((int) (255 * ratio)); // Fade out

            // Text Color
            paint.setColor(color);
            paint.setShadowLayer(10, 0, 0, color);

            canvas.drawText(text, x, y, paint);

            paint.clearShadowLayer();
            paint.setAlpha(255);
            paint.setTypeface(Typeface.DEFAULT);
        }
    }

    private void drawGameCompleted(Canvas canvas) {
        // Darken background
        canvas.drawColor(Color.argb(200, 0, 0, 0));

        float centerX = screenWidth / 2f;
        float centerY = screenHeight / 2f;

        // Pulsing Text Effect
        float pulse = (float) (Math.sin(System.currentTimeMillis() * 0.005) * 0.1 + 1.0);

        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        // "CONGRATULATIONS"
        paint.setTextSize(screenWidth * 0.10f * pulse); // Reduced from 0.12f
        paint.setColor(Color.rgb(255, 215, 0)); // Gold
        paint.setShadowLayer(20, 0, 0, Color.RED);
        canvas.drawText("CONGRATULATIONS", centerX, centerY - screenHeight * 0.1f, paint);
        paint.clearShadowLayer();

        // "YOU MASTERED THE GALAXY"
        paint.setTextSize(screenWidth * 0.05f);
        paint.setColor(Color.WHITE);
        canvas.drawText("YOU MASTERED THE GALAXY", centerX, centerY + screenHeight * 0.05f, paint);

        // "Space 10 - Level 100 Completed"
        paint.setTextSize(screenWidth * 0.04f);
        paint.setColor(Color.CYAN);
        canvas.drawText("Level 100 Completed", centerX, centerY + screenHeight * 0.12f, paint);

        // Restart info ?
        // Restart info ?
        /*
         * paint.setTextSize(screenWidth * 0.035f);
         * paint.setColor(Color.LTGRAY);
         * paint.setTypeface(Typeface.DEFAULT);
         * canvas.drawText("(Restart App to Play Again)", centerX, centerY +
         * screenHeight * 0.25f, paint);
         */

        // BACK TO MENU Button
        float btnW = screenWidth * 0.5f;
        float btnH = screenHeight * 0.07f;
        float btnY = screenHeight * 0.75f;
        drawNeonButton(canvas, "BACK TO MENU", centerX, btnY, btnW, btnH, Color.rgb(255, 100, 100));
    }
}

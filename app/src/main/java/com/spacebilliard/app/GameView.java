package com.spacebilliard.app;

import com.spacebilliard.app.MainActivity;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.LinearGradient;
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

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.Random;

import android.graphics.Typeface;
import android.content.SharedPreferences;
import android.graphics.PointF;
import android.graphics.Path;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.RectF;

public class GameView extends SurfaceView implements Runnable {

    private final long COMBO_TIMEOUT = 2000; // 2 saniye
    private final float MAX_DRAG_DISTANCE = 200;
    private final long MAX_DRAG_TIME = 3000;
    private final long SKILL_COOLDOWN = 10000; // 10 seconds generic cooldown
    private final int MAX_INVENTORY_SIZE = 3;
    private final int MAX_TRAIL_POINTS = 15;
    private Thread gameThread;
    private final SurfaceHolder holder;
    private boolean isPlaying;
    private Canvas canvas;
    private final Paint paint;
    private Shader nebula1, nebula2, nebula3; // Cached shaders for performance
    private final Random random;
    // Ekran boyutları
    private int screenWidth;
    private int screenHeight;
    private float centerX;
    private float centerY;
    private float circleRadius;
    // Oyun nesneleri
    private Ball whiteBall;
    private final ArrayList<Ball> cloneBalls;
    private final ArrayList<Ball> coloredBalls;
    private final ArrayList<Ball> blackBalls;
    private final ArrayList<SpecialBall> specialBalls;
    private final ArrayList<Particle> particles;
    // PERFORMANCE: Changed from CopyOnWriteArrayList to ArrayList
    private final ArrayList<ImpactArc> impactArcs;
    private final ArrayList<GuidedMissile> missiles;
    private ArrayList<ElectricEffect> electricEffects;
    private final ArrayList<Star> stars; // Static background stars
    private final ArrayList<Comet> comets; // Background comets for Space 2

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
    private long electricModeEndTime = 0; // Electric Power-up for Boss
    private float originalWhiteBallRadius = 0;
    private boolean powerBoostActive = false;
    private BlastWave blastWave = null;
    // New Special Balls Fields
    private Ufo activeUfo = null;

    // New sounds
    private int soundRetroLaser, soundLaserGun;
    // UI durumu
    private boolean showInstructions = false;
    private boolean showHighScore = false;
    private int highScore = 0;
    private int highLevel = 1;
    // Combo sistemi
    private int comboHits = 0;
    private long lastHitTime = 0;
    private int maxCombo = 0; // Combo rekoru
    private final ArrayList<FloatingText> floatingTexts; // Animasyonlu metinler için liste
    // Stage Cleared animasyonu
    private boolean showStageCleared = false;
    private long stageClearedTime = 0;
    // Elektrik topu ikinci sıçrama için
    private boolean electricSecondBounce = false;
    private long electricSecondBounceTime = 0;
    private float electricFirstTargetX = 0;
    private float electricFirstTargetY = 0;
    private int pendingLightningStrikes = 0;
    private long lastLightningStrikeTime = 0;
    private boolean showBossDefeated = false;
    private long bossDefeatedTime = 0;

    // PASSIVE ABILITY SYSTEM
    private String activePassivePower = "none"; // "none", "teleport", "split_save", "vortex"
    private Vortex activeVortex = null; // Active vortex instance
    // Split Save State
    private boolean isSplitSaveActive = false;
    private long cloneExpirationTime = 0;
    // Teleport Animation State
    private boolean isTeleporting = false;
    private long teleportEndTime = 0;

    private boolean showPlayerDefeated = false; // New field
    private long playerDefeatedTime = 0; // New field
    // Level Seçici
    private boolean showLevelSelector = false;
    private int selectorPage = 1;
    private int maxUnlockedLevel = 1;
    // Kamera sallanma
    private float cameraShakeX = 0;
    private float cameraShakeY = 0;
    private long shakeEndTime = 0;
    private final Random shakeRandom;
    private long immuneEndTime = 0; // Dokunulmazlık süresi
    private long lastShieldSoundTime = 0; // Kalkan sesi zamanlayıcısı
    // Son fırlatma gücü
    private float lastLaunchPower = 0;
    // Sürükleme
    private boolean isDragging = false;
    private Ball draggedBall = null;
    private float dragStartX, dragStartY;
    private long dragStartTime;
    private float currentTouchX, currentTouchY;
    private long lastLaunchTime = 0; // Cooldown for grabbing ball
    // Skill System
    private final String activeSkill = "None";
    private long lastSkillUseTime = 0;
    private boolean isSkillActive = false;
    private float skillBtnX, skillBtnY, skillBtnRadius;

    // Boss System
    private Boss currentBoss = null;

    // Inventory System
    private final ArrayList<String> inventory = new ArrayList<>();
    private float inventorySlotSize;
    private float inventoryX, inventoryY;
    // New Skills
    private boolean multiBallActive = false;
    private boolean magmaTrailActive = false;
    private long magmaTrailEndTime = 0;
    private final ArrayList<MagmaPatch> magmaPatches = new ArrayList<>();
    // PERFORMANCE: Changed from CopyOnWriteArrayList (copies entire list on every
    // add/remove!)
    private final ArrayList<Ball> bossProjectiles = new ArrayList<>();
    private long lastImpactSoundTime = 0; // Throttle impact sounds
    private float playerHp = 1000, playerMaxHp = 1000;

    // CRYO-STASIS Freeze Mechanic
    private long freezeProximityStartTime = 0;
    private boolean isFrozen = false;
    private long frozenEndTime = 0;

    private int coins = 0;
    private int pendingColoredBalls = 0; // Toplar yavaş yavaş gelecek
    private boolean extraTimeActive = false; // Added missing variable
    // Level geçiş beklemesi
    private boolean levelCompleted = false;
    private int lastCoinAwardedLevel = -1; // Start at -1 to ensure first stage awards coins
    private int lastCoinAwardedStage = -1;

    private final long levelCompletionTime = 0;
    // Ses Efektleri
    private final SoundPool soundPool;
    private int soundLaunch, soundCollision, soundCoin, soundBlackExplosion, soundElectric, soundFreeze, soundGameOver,
            soundMissile, soundPower, soundShield, soundTeleport;
    private boolean soundLoaded = false;
    // MainActivity reference for updating UI panels
    private MainActivity mainActivity;
    private QuestManager questManager;

    // Quest tracking fields
    private int ballsHitThisShot = 0; // Quest 3: Multi-hit
    private long bossSpawnTime = 0; // Quest 16: Speed demon
    private int playerHpAtBossStart = 0; // Quest 14: Flawless victory
    private boolean tookDamageInBossFight = false; // Quest 14
    private boolean wasFrozenDuringCryoFight = false; // Quest 17: Anti-freeze
    private int consecutiveLevelWins = 0; // Quest 29: Unstoppable
    private boolean tookDamageInLevel = false; // Quest 28: Perfection
    private long levelStartTime = 0; // Quest 36: Endurance (5 min survival)
    private long totalPlayTimeSeconds = 0; // Quest 37: Marathon
    private long lastDamageTime = 0; // Quest 38: Untouchable
    private long timeAtLowHp = 0; // Quest 39: Edge of death

    // New Quest Tracking (41-50)
    private int currentComboCount = 0; // Quest 41: Sharpshooter
    private long stageStartTime = 0; // Quest 43: Speed Clearer
    private int ballsDestroyedInLast5Seconds = 0; // Quest 44: Lightning Reflexes
    private long last5SecondWindowStart = 0; // Quest 44
    private java.util.Set<String> skillsUsedThisLevel = new java.util.HashSet<>(); // Quest 46
    private int[] stagesPerSpace = new int[10]; // Quest 47: Space Traveler

    // Removed old button references
    private android.graphics.Rect startBtnBounds, howToBtnBounds, shopBtnBounds;
    private String selectedSkin = "default";
    private String selectedTrail = "none";
    private String selectedAura = "none";
    private String selectedTrajectory = "dashed";
    private String selectedImpact = "classic";
    // Optimization Fields
    private android.graphics.Path cachedMeteorPath;
    private android.graphics.Path cachedMeteorCracks;
    private RadialGradient cachedMeteorAura;
    private RadialGradient cachedMeteorShading;
    private final android.graphics.PorterDuffXfermode cachedXfermode;
    private Bitmap cachedBossBackground; // NEW: Performance optimization for 60FPS

    // Level Info Overlay
    private String levelInfoText = "";
    private long levelInfoEndTime = 0;

    // PERFORMANCE: Cached polygon paths (created once, reused every frame)
    private Path cachedPolygonPath = null;
    private int cachedPolygonSides = -1;
    private float cachedPolygonScale = -1f;

    // PERFORMANCE: Object pools to reduce GC pressure
    private ParticlePool particlePool;
    private FloatingTextPool floatingTextPool;

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
        // PERFORMANCE: Changed from CopyOnWriteArrayList
        impactArcs = new ArrayList<>();
        missiles = new ArrayList<>();
        electricEffects = new ArrayList<>();
        electricEffects = new ArrayList<>();
        stars = new ArrayList<>();
        floatingTexts = new ArrayList<>();

        // PERFORMANCE: Object pooling to reduce GC pressure
        particlePool = new ParticlePool(this);
        floatingTextPool = new FloatingTextPool(this);

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
        // maxUnlockedLevel = 500; // TEST MODE: Unlock all levels
        // Coinleri yükle
        coins = prefs.getInt("coins", 0);
        // TEST MODE: Grant 10,000 coins for testing
        /*
         * if (coins < 10000) {
         * coins = 10000;
         * SharedPreferences.Editor editor = prefs.edit();
         * editor.putInt("coins", coins);
         * editor.apply();
         * }
         */

        // Initialize Quest Manager
        questManager = QuestManager.getInstance(context);

        // SoundPool Başlatma
        AudioAttributes audioAttributes = new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build();

        soundPool = new SoundPool.Builder().setMaxStreams(10).setAudioAttributes(audioAttributes).build();

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
            soundRetroLaser = soundPool.load(context, R.raw.retro_laser, 1);
            soundLaserGun = soundPool.load(context, R.raw.laser_gun, 1);
            soundTeleport = soundPool.load(context, R.raw.retro_laser, 1); // Fallback
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

    // Removed setMenuButtons method as MainActivity handles buttons now

    // Upgrades
    private int upgradeAim = 1;
    private int upgradeEnergy = 1;
    private int upgradeLuck = 1;
    private int upgradeShield = 1;
    // New Upgrades
    private int upgradeNova = 1;
    private int upgradeHunter = 1;
    private int upgradeImpulse = 1;
    private int upgradeMidas = 1;
    // New Advanced Upgrades
    private int upgradeRailgun = 0; // Starts at 0 (Unlocked via Shop)
    private int upgradeVampire = 0;

    private void loadUpgrades() {
        SharedPreferences prefs = getContext().getSharedPreferences("SpaceBilliard", Context.MODE_PRIVATE);
        upgradeAim = prefs.getInt("upgrade_aim", 1);
        upgradeEnergy = prefs.getInt("upgrade_energy", 1);
        upgradeLuck = prefs.getInt("upgrade_luck", 1);
        upgradeShield = prefs.getInt("upgrade_shield", 1);
        upgradeNova = prefs.getInt("upgrade_nova", 1);
        upgradeHunter = prefs.getInt("upgrade_hunter", 1);
        upgradeImpulse = prefs.getInt("upgrade_impulse", 1);
        upgradeMidas = prefs.getInt("upgrade_midas", 1);
        upgradeRailgun = prefs.getInt("upgrade_railgun", 0);
        upgradeVampire = prefs.getInt("upgrade_vampire", 0);
    }

    public void reloadPreferences() {
        loadUpgrades();
        SharedPreferences prefs = getContext().getSharedPreferences("SpaceBilliard", Context.MODE_PRIVATE);
        selectedSkin = prefs.getString("selectedSkin", "default");
        selectedTrail = prefs.getString("selectedTrail", "none");
        selectedAura = prefs.getString("selectedAura", "none");
        selectedTrajectory = prefs.getString("selectedTrajectory", "dashed");
        selectedImpact = prefs.getString("selectedImpact", "classic");
    }

    public void startGame() {
        reloadPreferences(); // Ensure prefs are fresh when game starts
        showLevelSelector = false; // Disable old selector
        updateUIPanels();
    }

    public void startGameAtLevel(int startLevel) {
        reloadPreferences();
        showLevelSelector = false;

        level = startLevel;
        stage = 1;

        if (screenWidth > 0 && screenHeight > 0) {
            // View is ready, verify whiteBall exists
            if (whiteBall == null) {
                // Initialize physics objects if missing (should be in onSizeChanged but safe
                // fallback)
                int minSize = Math.min(screenWidth, screenHeight);
                centerX = screenWidth / 2f;
                centerY = screenHeight / 2f;
                circleRadius = minSize * 0.47f;
                whiteBall = new Ball(centerX, centerY, minSize * 0.02f, Color.WHITE);
            }
            initLevel(level);
            gameStarted = true;
            gameOver = false;
            updateUIPanels();
        } else {
            // View not ready. We set the level variable, and onSizeChanged will pick it up.
            // Do NOT set gameStarted = true here, as physics objects (whiteBall) are
            // missing.
            // onSizeChanged will initialize whiteBall and then set gameStarted = true.
        }
    }

    public void showInstructions() {
        showInstructions = true;
        updateUIPanels();
    }

    public void showHighScore() {
        showHighScore = true;
        updateUIPanels();
    }

    // Called when user watches rewarded ad to continue from current stage
    public void continueAfterAd() {
        if (gameOver) {
            // CRITICAL: Reset defeat flags to prevent loop
            showPlayerDefeated = false;
            showBossDefeated = false;
            playerHp = playerMaxHp; // Restore full HP

            if (lives <= 0) {
                lives = 3; // Give FULL lives back
            }

            // Reset to level's initial time (same formula as initLevel) - ALWAYS on Revive
            timeLeft = 20000 + (level * 500L);
            timeLeft = Math.min(timeLeft, 45000); // Max 45 sn

            // Restore player ball
            if (whiteBall != null) {
                whiteBall.x = centerX;
                whiteBall.y = centerY + circleRadius * 0.5f;
                whiteBall.vx = 0;
                whiteBall.vy = 0;
            }

            // RESTORE BOSS HP ON REVIVE (Reduced recovery)
            if (currentBoss != null) {
                // Reduced from 30% to 10% HP recovery
                currentBoss.hp = Math.min(currentBoss.hp + (currentBoss.maxHp * 0.1f), currentBoss.maxHp);

                // Reset boss position to starting point
                currentBoss.x = centerX;
                currentBoss.y = centerY - circleRadius * 0.6f;

                floatingTexts.add(new FloatingText("BOSS RECOVERED!", centerX, centerY - 100, Color.RED));
            }

            // CRITICAL: Set gameOver = false BEFORE calling updateUIPanels to prevent loop
            gameStarted = true; // Ensure loop continues
            gameOver = false; // MUST be false before updateUIPanels call
            updateUIPanels(); // Now safe to call - won't trigger showGameOverScreen
            lastTime = System.currentTimeMillis(); // ABSOLUTE LAST - prevent deltaTime loss during UI update
        }
    }

    // New Helper Methods for UI interactions
    public void rebootLevel() {
        gameOver = false;
        lives = 3;
        score = 0;

        // RESET GAME STATE FLAGS TO PREVENT LOOP
        playerHp = playerMaxHp;
        showPlayerDefeated = false;
        showBossDefeated = false;

        // Reset to stage 1 of current level
        stage = 1;

        // CLEAR BOSS PROJECTILES TO PREVENT PERSISTENCE
        if (bossProjectiles != null) {
            bossProjectiles.clear();
        }

        initLevel(level);
        updateUIPanels();
        playSound(soundLaunch);
        gameStarted = true;
    }

    public void resetToMainMenu() {
        gameOver = false;
        gameStarted = false;
        score = 0;
        lives = 3;

        // RESET GAME STATE FLAGS TO PREVENT LOOP
        playerHp = playerMaxHp;
        showPlayerDefeated = false;
        showBossDefeated = false;

        // CLEAR BOSS PROJECTILES
        if (bossProjectiles != null) {
            bossProjectiles.clear();
        }

        // initLevel(1);
        updateUIPanels();
    }

    private void updateUIPanels() {
        if (mainActivity != null) {
            if (showInstructions) {
                mainActivity.hideAllPanels();
            } else if (showHighScore) {
                mainActivity.hideAllPanels();
            } else if (showLevelSelector) {
                mainActivity.hideAllPanels();
            } else if (gameOver) {
                mainActivity.showGameOverScreen();
            } else if (!gameStarted && !gameCompleted) {
                // Main Menu
                mainActivity.finish();
            } else {
                // In Game
                mainActivity.hideAllPanels();
            }
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

        circleRadius = minSize * 0.47f;

        // Skill Button Positions
        float density = getResources().getDisplayMetrics().density;
        skillBtnRadius = 35 * density;
        skillBtnX = screenWidth - 50 * density;
        skillBtnY = screenHeight - 120 * density;

        // Beyaz topu başlat
        if (whiteBall == null) {
            whiteBall = new Ball(centerX, centerY, minSize * 0.02f, Color.WHITE);
            // If startGameAtLevel was called, 'level' is already set to target.
            // If normal start, 'level' defaults to 1.
            // So just use 'level' here instead of hardcoding 1.

            // Reset stage to 1 for fresh level start
            stage = 1;

            initLevel(level);

            // Now that objects are created, we can safely start the game loop logic
            gameStarted = true;
            gameOver = false;
        } else {
            updatePositionsAfterResize();
        }

        // Initialize Simple Background Shader (High Performance)
        nebula1 = new RadialGradient(centerX, centerY, Math.max(screenWidth, screenHeight),
                new int[] { Color.rgb(30, 10, 50), Color.rgb(5, 5, 10) }, null, Shader.TileMode.CLAMP);

        // PERFORMANCE: Lazy-load meteor graphics when Space 2 is actually reached
        // (prevents 100-300ms startup freeze)
        // initGiantMeteorGraphics();
    }

    private void generateBossBackground(String bossName) {
        // Skip for LUNAR and VOID (Use existing dynamic backgrounds)
        if (bossName.equals("LUNAR CONSTRUCT") || bossName.equals("VOID TITAN")) {
            if (cachedBossBackground != null) {
                cachedBossBackground.recycle();
                cachedBossBackground = null;
            }
            return;
        }

        if (cachedBossBackground != null && !cachedBossBackground.isRecycled()) {
            cachedBossBackground.recycle();
        }
        cachedBossBackground = Bitmap.createBitmap(screenWidth, screenHeight, Bitmap.Config.ARGB_8888);
        Canvas bgCanvas = new Canvas(cachedBossBackground);
        Paint bgPaint = new Paint();
        bgPaint.setAntiAlias(false); // Disable for performance

        // SIMPLIFIED: Single gradient only
        if (bossName.equals("SOLARION")) {
            bgPaint.setShader(new RadialGradient(centerX, centerY, screenWidth * 0.8f, Color.rgb(80, 20, 0),
                    Color.rgb(20, 5, 0), Shader.TileMode.CLAMP));
            bgCanvas.drawRect(0, 0, screenWidth, screenHeight, bgPaint);
        } else if (bossName.equals("NEBULON")) {
            bgPaint.setShader(new RadialGradient(centerX, centerY, screenWidth * 0.8f, Color.rgb(60, 20, 100),
                    Color.rgb(10, 0, 30), Shader.TileMode.CLAMP));
            bgCanvas.drawRect(0, 0, screenWidth, screenHeight, bgPaint);
        } else if (bossName.equals("GRAVITON")) {
            bgPaint.setShader(new RadialGradient(centerX, centerY, screenWidth * 0.8f, Color.rgb(20, 0, 80),
                    Color.BLACK, Shader.TileMode.CLAMP));
            bgCanvas.drawRect(0, 0, screenWidth, screenHeight, bgPaint);
        } else if (bossName.equals("MECHA-CORE")) {
            bgPaint.setShader(new RadialGradient(centerX, centerY, screenWidth * 0.8f, Color.rgb(0, 40, 50),
                    Color.rgb(5, 10, 15), Shader.TileMode.CLAMP));
            bgCanvas.drawRect(0, 0, screenWidth, screenHeight, bgPaint);
        } else if (bossName.equals("CRYO-STASIS")) {
            bgPaint.setShader(new RadialGradient(centerX, centerY, screenWidth * 0.8f, Color.rgb(0, 60, 100),
                    Color.rgb(0, 10, 30), Shader.TileMode.CLAMP));
            bgCanvas.drawRect(0, 0, screenWidth, screenHeight, bgPaint);
        } else if (bossName.equals("GEO-BREAKER")) {
            bgPaint.setShader(new RadialGradient(centerX, centerY, screenWidth * 0.8f, Color.rgb(60, 20, 0),
                    Color.rgb(15, 5, 0), Shader.TileMode.CLAMP));
            bgCanvas.drawRect(0, 0, screenWidth, screenHeight, bgPaint);
        } else if (bossName.equals("BIO-HAZARD")) {
            bgPaint.setShader(new RadialGradient(centerX, centerY, screenWidth * 0.8f, Color.rgb(0, 80, 20),
                    Color.rgb(0, 15, 5), Shader.TileMode.CLAMP));
            bgCanvas.drawRect(0, 0, screenWidth, screenHeight, bgPaint);
        } else if (bossName.equals("CHRONO-SHIFTER")) {
            bgPaint.setShader(new RadialGradient(centerX, centerY, screenWidth * 0.8f, Color.rgb(80, 60, 0),
                    Color.rgb(20, 15, 0), Shader.TileMode.CLAMP));
            bgCanvas.drawRect(0, 0, screenWidth, screenHeight, bgPaint);
        } else {
            // Fallback
            bgCanvas.drawColor(Color.rgb(10, 10, 10));
        }
    }

    private void initGiantMeteorGraphics() {
        // Keeps cached bitmaps for meteor
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
                new int[] { Color.argb(100, 255, 69, 0), Color.TRANSPARENT }, null, Shader.TileMode.CLAMP);

        cachedMeteorShading = new RadialGradient(meteorX - meteorRadius * 0.3f, meteorY - meteorRadius * 0.3f,
                meteorRadius * 1.5f, Color.argb(0, 0, 0, 0), Color.argb(200, 0, 0, 0), Shader.TileMode.CLAMP);
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

        // Level Info Screen logic - ONLY SHOW ON STAGE 1 & START OF SPACE (Level 1, 11,
        // 21...)
        /*
         * DISABLED REQ:
         * "her spacein ilk bölümünde ponel çıkıyor level 61 71 gibi onları kaldır tamamen"
         * if (stage == 1 && (lv % 10 == 1)) {
         * levelInfoText = "LEVEL " + lv;
         * 
         * levelInfoEndTime = System.currentTimeMillis() + 6000;
         * }
         */
        // Zorluk Ayarı
        lives = 3;
        // Level 6'dan sonra süre artar, ama level arttıkça top sayısı da artar
        timeLeft = 20000 + (lv * 500L);
        timeLeft = Math.min(timeLeft, 45000); // Max 45 sn

        lastTime = System.currentTimeMillis();
        levelStartTime = lastTime; // Quest 36: Endurance tracking
        comboCounter = 0;

        coloredBalls.clear();
        blackBalls.clear();
        specialBalls.clear();
        cloneBalls.clear();
        particles.clear();
        magmaPatches.clear();
        missiles.clear();
        electricEffects.clear();
        floatingTexts.clear();
        inventory.clear(); // Clear inventory on level start/restart

        // New Quest Tracking Initialization
        stageStartTime = System.currentTimeMillis(); // Quest 43: Speed Clearer
        skillsUsedThisLevel.clear(); // Quest 46: Ability Addict
        currentComboCount = 0; // Quest 41: Sharpshooter
        ballsDestroyedInLast5Seconds = 0; // Quest 44
        last5SecondWindowStart = System.currentTimeMillis(); // Quest 44

        // Safety Reset for flags
        showPlayerDefeated = false;
        showBossDefeated = false;

        // CLEAR BOSS PROJECTILES
        if (bossProjectiles != null) {
            bossProjectiles.clear();
        }

        whiteBall.x = centerX;
        whiteBall.y = centerY;
        whiteBall.vx = 0;
        whiteBall.vy = 0;
        whiteBall.trail.clear();
        // Ghost modundan çıkarken top boyutunu sıfırla
        whiteBall.radius = (circleRadius / 0.47f) * 0.022f; // Slightly smaller (2.2%)
        originalWhiteBallRadius = whiteBall.radius; // Initialize default radius

        blackHoleActive = false;
        // Shield Battery Upgrade
        if (upgradeShield > 1 && random.nextInt(100) < (upgradeShield * 10)) {
            barrierActive = true;
            barrierEndTime = System.currentTimeMillis() + 10000; // Start with 10s shield
            floatingTexts.add(new FloatingText("AUTO SHIELD!", centerX, centerY - 200, Color.CYAN));
        } else {
            barrierActive = false;
        }
        freezeActive = false;
        ghostModeActive = false;
        powerBoostActive = false;
        extraTimeActive = false; // Reset extra time

        // Reset special ball inventory effects
        magmaTrailActive = false;
        magmaTrailEndTime = 0;
        multiBallActive = false;

        // CLEAR PERSISTENT UFO
        activeUfo = null;

        // Reset special ball effects
        if (whiteBall != null) {
            whiteBall.radius = originalWhiteBallRadius;
            whiteBall.color = Color.WHITE;
        }
        blastWave = null;

        // Initialize Boss (Every 50th stage = End of every 10th Level)
        currentBoss = null;

        // SPACE-BASED AREA ADJUSTMENTS
        float minSize = Math.min(screenWidth, screenHeight);
        int currentSpace = ((lv - 1) / 10) + 1;

        if (currentSpace == 2) {
            // Space 2: Square play area (larger)
            circleRadius = minSize * 0.49f;
        } else if (currentSpace == 3) {
            // Space 3: Even larger area
            circleRadius = minSize * 0.49f;
        } else {
            // Default area for other spaces
            circleRadius = minSize * 0.47f;
        }

        // Boss only appears at Level X0 Stage 5 (e.g., Level 10 Stage 5, Level 20 Stage
        // 5)
        if (lv % 10 == 0 && stage == 5) {
            spawnBoss(lv);
            return; // Skip normal ball layout
        }

        // Her level (5 stage) için top sayısını sıfırla ve yeniden başlat
        int stageInLevel = stage; // Use explicit stage variable
        int ballCount = 10 + stageInLevel; // Start higher (11 balls at stage 1)

        // Spawn 4 balls initially
        int initialSpawn = 4;
        pendingColoredBalls = ballCount - initialSpawn;

        int[] colors = { Color.rgb(255, 0, 85), // Pink-Red
                Color.rgb(0, 255, 153), // Cyan-Green
                Color.rgb(255, 255, 0), // Yellow
                Color.rgb(0, 204, 255), // Sky Blue
                Color.rgb(255, 102, 0) // Orange
        };

        for (int i = 0; i < initialSpawn; i++) {
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
        int blackCount;
        if (stageInLevel <= 3)
            blackCount = 2; // Stages 1, 2, 3 -> 2 balls
        else
            blackCount = 3; // Stages 4, 5 -> 3 balls

        for (int i = 0; i < blackCount; i++) {
            float angle = random.nextFloat() * (float) (2 * Math.PI);
            float radius = circleRadius * 0.7f;
            float x = centerX + (float) Math.cos(angle) * radius;
            float y = centerY + (float) Math.sin(angle) * radius;

            Ball ball = new Ball(x, y, whiteBall.radius * 1.2f, Color.BLACK);
            // First ball is static, others are moving
            if (i == 0) {
                ball.vx = 0;
                ball.vy = 0;
            } else {
                ball.vx = (random.nextFloat() - 0.5f) * 12;
                ball.vy = (random.nextFloat() - 0.5f) * 12;
            }
            blackBalls.add(ball);
        }
    }

    private void spawnBoss(int lv) {
        String name = "Boss";
        float hp = 2000 + (lv * 200); // Scaling HP
        int color = Color.RED;
        long duration = 180000; // 3 mins for boss battles

        // Boss assignment based on level
        // Boss assignment based on global stage count (50 stages = 10 Levels)
        if (lv == 10) { // Level 10 Stage 5 - 1st Boss
            name = "VOID TITAN";
            hp = 2000;
            color = Color.rgb(75, 0, 130);
        } else if (lv == 20) { // Level 20 Stage 5 - 2nd Boss
            name = "LUNAR CONSTRUCT";
            hp = 2000;
            color = Color.rgb(100, 100, 100);
        } else if (lv == 30) { // Level 30
            name = "SOLARION";
            hp = 2000;
            color = Color.rgb(255, 165, 0);
        } else if (lv == 40) { // Level 40
            name = "NEBULON";
            hp = 2000;
            color = Color.rgb(218, 112, 214);
        } else if (lv == 50) { // Level 50
            name = "GRAVITON";
            hp = 2000;
            color = Color.rgb(0, 0, 139);
        } else if (lv == 60) { // Level 60
            name = "MECHA-CORE";
            hp = 2000;
            color = Color.rgb(192, 192, 192);
        } else if (lv == 70) { // Level 70
            name = "CRYO-STASIS";
            hp = 2000;
            color = Color.CYAN;
        } else if (lv == 80) { // Level 80
            name = "GEO-BREAKER";
            hp = 2000;
            color = Color.rgb(139, 69, 19);
        } else if (lv == 90) { // Level 90
            name = "BIO-HAZARD";
            hp = 2000;
            color = Color.rgb(50, 205, 50);
        } else if (lv == 100) { // Level 100
            name = "CHRONO-SHIFTER";
            hp = 2000;
            color = Color.rgb(255, 215, 0);
        }

        // generateBossBackground(name); // REMOVED: Causes FPS drops

        timeLeft = duration;
        currentBoss = new Boss(name, hp, color);

        // Boss Info Screen - Detailed descriptions for all bosses
        if (name.equals("VOID TITAN")) {
            levelInfoText = "WARNING: VOID TITAN\nHOMING MISSILES\nSTAY MOBILE!";
        } else if (name.equals("LUNAR CONSTRUCT")) {
            levelInfoText = "BOSS: LUNAR CONSTRUCT\nSUMMONS UFO ALLIES\nDESTROY UFOs FAST";
        } else if (name.equals("SOLARION")) {
            levelInfoText = "WARNING: SOLARION\nSOLAR FLARES\nDON'T GET TOO CLOSE!";
        } else if (name.equals("NEBULON")) {
            levelInfoText = "BOSS: NEBULON\nHIGH DENSITY FIELD\nINCREASED FRICTION";
        } else if (name.equals("GRAVITON")) {
            levelInfoText = "BOSS: GRAVITON\nEVENT HORIZON ATTACK\nAVOID CENTER - HP REGEN";
        } else if (name.equals("MECHA-CORE")) {
            levelInfoText = "WARNING: MECHA-CORE\nELECTRIC PULSES\nRAPID FIRE ATTACKS";
        } else if (name.equals("CRYO-STASIS")) {
            levelInfoText = "BOSS: CRYO-STASIS\nICE BARRAGE\nSTAY CLOSE = FREEZE";
        } else if (name.equals("GEO-BREAKER")) {
            levelInfoText = "WARNING: GEO-BREAKER\nMAGMA PATCHES\nAVOID LAVA POOLS!";
        } else if (name.equals("BIO-HAZARD")) {
            levelInfoText = "BOSS: BIO-HAZARD\nTOXIC CLOUDS\nCONTAMINATION SPREADS";
        } else if (name.equals("CHRONO-SHIFTER")) {
            levelInfoText = "WARNING: CHRONO-SHIFTER\nTIME DISTORTION\nFINAL BOSS!";
        } else {
            levelInfoText = "WARNING: BOSS ENCOUNTER\n" + name;
        }

        levelInfoEndTime = System.currentTimeMillis() + 7000;

        // Player HP Setup
        playerMaxHp = 1000;
        playerHp = playerMaxHp;
        bossProjectiles.clear();

        // Reposition Player for battle
        if (whiteBall != null) {
            whiteBall.x = centerX;
            whiteBall.y = centerY + circleRadius * 0.6f; // Start below
            whiteBall.vx = 0;
            whiteBall.vy = 0;
        }
    }

    @Override
    public void run() {
        while (isPlaying) {
            long startTime = System.currentTimeMillis();

            update();
            draw();

            // Next Level Transition Logic - REMOVED (Handled in update())
            /*
             * if (showStageCleared) { ... }
             */

            // Special Ball Spawn logic (Higher chance in Boss Fight)
            float spawnChance = (currentBoss != null) ? 0.003f : 0.0025f; // Reduced: 0.2% boss, 0.15% normal
            // normal
            if (gameStarted && !gameOver && random.nextFloat() < spawnChance && specialBalls.size() < 8) { // Increased
                // limit
                spawnSpecialBall();
            }

            // Pending Colored Balls Spawn (Random Intervals)
            // Boss Fight: NO Colored Balls
            if (gameStarted && !gameOver && pendingColoredBalls > 0 && coloredBalls.size() < 8
                    && random.nextFloat() < 0.01f && currentBoss == null) {
                spawnRandomColoredBall();
                pendingColoredBalls--;
            }

            long frameTime = System.currentTimeMillis() - startTime;
            // Target 60 FPS (16ms per frame) - optimized for 90% of Android devices
            if (frameTime < 16) {
                try {
                    Thread.sleep(16 - frameTime);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void performLightningStrike() {
        if (currentBoss != null) {
            electricEffects.add(new ElectricEffect(currentBoss.x, -200, currentBoss.x, currentBoss.y, 1));
            createImpactBurst(currentBoss.x, currentBoss.y, Color.RED);
            currentBoss.hp -= 50;
            floatingTexts.add(new FloatingText("-50", currentBoss.x, currentBoss.y - 50, Color.RED));
            playSound(soundBlackExplosion);
        } else if (blackBalls.size() > 0) {
            Ball b = blackBalls.get(0); // Always target first available
            electricEffects.add(new ElectricEffect(b.x, -200, b.x, b.y, 1));
            createImpactBurst(b.x, b.y, Color.BLACK);
            blackBalls.remove(0);
            // Quest 2: Dark Matter & Quest 5: Destroyer II
            if (questManager != null) {
                questManager.incrementQuestProgress(2, 1);
                questManager.incrementQuestProgress(5, 1);
            }
            playSound(soundBlackExplosion);
        } else if (coloredBalls.size() > 0) {
            Ball b = coloredBalls.get(random.nextInt(coloredBalls.size()));
            electricEffects.add(new ElectricEffect(b.x, -200, b.x, b.y, 1));
            createImpactBurst(b.x, b.y, b.color);
            score += 5;
            coloredBalls.remove(b);
            // Quest progress: Destroyer I (Quest 1)
            if (questManager != null) {
                android.util.Log.d("QUEST_DEBUG", "Colored ball destroyed - incrementing quest 1 and 5");
                questManager.incrementQuestProgress(1, 1); // Destroy 100 colored balls
                questManager.incrementQuestProgress(5, 1); // Destroy 200 balls total
            }
            playSound(soundBlackExplosion);
        }
    }

    private void spawnSpecialBall() {
        String[] types;

        // UNLOCKED: All balls available in all modes (User Request)
        types = new String[] { "blackhole", "blackhole", "extraTime", "powerBoost",
                "barrier", "barrier", "electric", "electric", "electric", "freeze", "freeze", "missile", "missile",
                "missile",
                "missile", "teleport",
                "teleport", "teleport", "split_save", "split_save", "split_save",
                "vortex", "vortex", "vortex",
                "boom", "boom", "ghost", "ghost", "multiball", "multiball", "magma", "magma", "magma", "lightning",
                "lightning", "lightning",
                "ufo", "ufo", "repulsor", "repulsor", "alchemy", "alchemy" };

        String type = types[random.nextInt(types.length)];

        float angle = random.nextFloat() * (float) (2 * Math.PI);
        float radius = circleRadius * 0.7f;
        float x = centerX + (float) Math.cos(angle) * radius;
        float y = centerY + (float) Math.sin(angle) * radius;

        specialBalls.add(new SpecialBall(x, y, (circleRadius / 0.47f) * 0.02f, type));
    }

    private void spawnRandomColoredBall() {
        int[] colors = { Color.rgb(255, 0, 85), Color.rgb(0, 255, 153), Color.rgb(255, 255, 0), Color.rgb(0, 204, 255),
                Color.rgb(255, 102, 0) };
        float angle = random.nextFloat() * (float) (2 * Math.PI);
        float dist = random.nextFloat() * (circleRadius * 0.8f); // Slightly wider spread
        float x = centerX + (float) Math.cos(angle) * dist;
        float y = centerY + (float) Math.sin(angle) * dist;

        Ball b = new Ball(x, y, (circleRadius / 0.47f) * 0.02f, colors[random.nextInt(colors.length)]);
        // Set Velocity
        b.vx = (random.nextFloat() - 0.5f) * 12 + ((random.nextFloat() > 0.5) ? 2 : -2);
        b.vy = (random.nextFloat() - 0.5f) * 12 + ((random.nextFloat() > 0.5) ? 2 : -2);
        coloredBalls.add(b);

        // Visual Spawn Effect - REMOVED as requested
        // createImpactBurst(x, y, Color.WHITE);
    }

    private void update() {

        // PAUSE GAME IF INFO SCREEN IS ACTIVE
        if (System.currentTimeMillis() < levelInfoEndTime) {
            return;
        }

        // Offline Logic: Lightning Sequencer
        if (pendingLightningStrikes > 0) {
            long now = System.currentTimeMillis();
            if (now - lastLightningStrikeTime >= 1000) {
                lastLightningStrikeTime = now;
                pendingLightningStrikes--;
                performLightningStrike();
            }
        }

        // Boss Defeated Sequence Transition
        if (showBossDefeated && System.currentTimeMillis() - bossDefeatedTime > 3000) {
            showBossDefeated = false;
            if (coloredBalls.isEmpty() && pendingColoredBalls == 0) {
                // Grant Reward for Boss Defeat - 100 Coins
                int oldCoins = coins;
                coins += 100; // Reward 100 coins
                // Quest 31-32, 49: Collect coins
                if (questManager != null) {
                    questManager.incrementQuestProgress(31, 100);
                    questManager.incrementQuestProgress(32, 100);
                    questManager.incrementQuestProgress(49, 100); // Quest 49: Coin Millionaire
                }
                lastCoinAwardedLevel = level; // Mark level as rewarded to prevent double reward

                saveProgress(); // Save coins

                // Force UI update to trigger animation
                if (mainActivity != null) {
                    mainActivity.runOnUiThread(() -> {

                        updateMainActivityPanels();
                    });
                }

                // Show a floating text for reward
                floatingTexts.add(new FloatingText("+100 COINS!", centerX, centerY, Color.YELLOW));
                playSound(soundPower);

                levelCompleted = true;
                showStageCleared = true;
                stageClearedTime = System.currentTimeMillis();

                // Stage Cleared: Explode all Black Balls (ensure cleanup)
                for (Ball b : blackBalls) {
                    createImpactBurst(b.x, b.y, Color.BLACK);
                }
                blackBalls.clear();

                // CLEAR ALL PROJECTILES ON BOSS VICTORY
                bossProjectiles.clear();
                coloredBalls.clear();
            }
        }

        if (!gameStarted || gameOver)
            return;

        long currentTime = System.currentTimeMillis();
        long deltaTime = currentTime - lastTime;
        deltaTime = Math.min(deltaTime, 50); // Clamp to 50ms max to prevent physics explosions during GC
        lastTime = currentTime;

        // QUEST TRACKING - Survival/Time-Based Quests
        if (questManager != null && currentBoss == null) {
            // Quest 36: Endurance - Survive 20 minutes in one level
            if (levelStartTime > 0) {
                long levelDuration = (currentTime - levelStartTime) / 1000; // seconds
                if (levelDuration >= 1200) { // 20 minutes = 1200 seconds
                    questManager.incrementQuestProgress(36, 1);
                    levelStartTime = 0; // Reset to prevent re-trigger
                }
            }

            // Quest 37: Marathon - Play for 120 minutes total
            if (deltaTime > 0) {
                totalPlayTimeSeconds += deltaTime / 1000;
                if (totalPlayTimeSeconds % 60 == 0) { // Update every minute
                    questManager.updateQuestProgress(37, (int) totalPlayTimeSeconds);
                }
            }

            // Quest 38: Untouchable - No damage for 8 minutes
            if (lastDamageTime > 0) {
                long noDamageDuration = (currentTime - lastDamageTime) / 1000; // seconds
                if (noDamageDuration >= 480) { // 8 minutes = 480 seconds
                    questManager.incrementQuestProgress(38, 1);
                    lastDamageTime = 0; // Reset to prevent re-trigger
                }
            } else if (lastDamageTime == 0 && levelStartTime > 0) {
                // Initialize no-damage timer at level start
                lastDamageTime = levelStartTime;
            }

            // Quest 39: Edge of Death - Survive with 1 HP for 120s
            if (playerHp <= 1 && playerHp > 0) {
                if (timeAtLowHp == 0) {
                    timeAtLowHp = currentTime; // Start tracking
                } else {
                    long lowHpDuration = (currentTime - timeAtLowHp) / 1000; // seconds
                    if (lowHpDuration >= 120) { // 120 seconds
                        questManager.incrementQuestProgress(39, 1);
                        timeAtLowHp = 0; // Reset to prevent re-trigger
                    }
                }
            } else {
                timeAtLowHp = 0; // Reset if HP increases
            }
        }

        // Zaman
        timeLeft -= deltaTime;

        // Quest 40: Last Second - Complete with 0 seconds remaining
        if (timeLeft <= 1000 && timeLeft > 0 && coloredBalls.isEmpty() && pendingColoredBalls == 0) {
            if (questManager != null) {
                questManager.incrementQuestProgress(40, 1);
            }
        }

        // Space 3 Background Effect: Rising Flames
        int currentEffectsSpace = ((level - 1) / 10) + 1;
        if (currentEffectsSpace == 3 && !showBossDefeated && !showPlayerDefeated) {
            // Spawn rising flames from bottom
            if (random.nextFloat() < 0.3f) { // 30% chance per frame
                float x = random.nextFloat() * screenWidth;
                float y = screenHeight + 20; // Start below screen
                float speed = 5 + random.nextFloat() * 5; // Slow rising
                float angle = (float) (Math.PI * 1.5); // Upwards (~270 degrees)
                // Fire colors: Red -> Orange -> Yellow
                int r = 255;
                int g = random.nextInt(140);
                int b = 0;
                // Use simple particle constructor
                particles.add(new Particle(x, y, angle, speed, Color.rgb(r, g, b)));
            }
        }

        // Boss Projectiles Update & Collision with Player Balls
        // Prevent updates if Boss or Player is defeated (Game Paused Visual)
        if (!showBossDefeated && !showPlayerDefeated) {
            for (int i = bossProjectiles.size() - 1; i >= 0; i--) {
                Ball p = bossProjectiles.get(i);
                p.x += p.vx;
                p.y += p.vy;

                // REMOVED: Collision check here was bypassing passive abilities!
                // Collision is now ONLY handled in checkCollisions() where passives are
                // checked.

                // Remove if out of bounds
                if (p.x < -100 || p.x > screenWidth + 100 || p.y < -100 || p.y > screenHeight + 100) {
                    bossProjectiles.remove(i);
                }
            }
        }

        // Game Over Check for HP
        /*
         * REMOVED to prevent immediate Game Over.
         * Now handled by 'showPlayerDefeated' sequence at lines 1004+.
         * if (currentBoss != null && playerHp <= 0) {
         * gameOver = true;
         * playSound(soundGameOver);
         * updateUIPanels();
         * return;
         * }
         */

        // Magma Update
        if (magmaTrailActive) {
            if (System.currentTimeMillis() > magmaTrailEndTime) {
                magmaTrailActive = false;
            } else if (whiteBall != null && (Math.abs(whiteBall.vx) > 1 || Math.abs(whiteBall.vy) > 1)) {
                if (random.nextFloat() < 0.2f) {
                    magmaPatches.add(new MagmaPatch(whiteBall.x, whiteBall.y, whiteBall.radius * 0.8f));
                }
            }
        }
        for (int i = magmaPatches.size() - 1; i >= 0; i--) {
            magmaPatches.get(i).update();
            if (magmaPatches.get(i).isDead())
                magmaPatches.remove(i);
        }

        // Feature: Magma Patches destroy Boss Projectiles (except Meteors)
        if (!magmaPatches.isEmpty() && !bossProjectiles.isEmpty()) {
            for (int i = bossProjectiles.size() - 1; i >= 0; i--) {
                Ball proj = bossProjectiles.get(i);
                // Meteors are immune (Ghost-like)
                if (proj instanceof MeteorProjectile)
                    continue;

                for (MagmaPatch mp : magmaPatches) {
                    float dx = proj.x - mp.x;
                    float dy = proj.y - mp.y;
                    float dist = (float) Math.sqrt(dx * dx + dy * dy);
                    if (dist < proj.radius + mp.radius) {
                        bossProjectiles.remove(i);
                        createImpactBurst(proj.x, proj.y, Color.rgb(255, 100, 0)); // Magma color
                        playSound(soundBlackExplosion);
                        break; // Stop checking this projectile
                    }
                }
            }
        }
        if (timeLeft <= 0 && (coloredBalls.size() > 0 || currentBoss != null)) {
            gameOver = true;
            saveProgress();
            playSound(soundGameOver);
            updateUIPanels();
            return;
        }

        // TELEPORT LOGIC
        if (isTeleporting) {
            if (System.currentTimeMillis() > teleportEndTime) {
                isTeleporting = false;

                // Reappear at random safe location
                float angle = random.nextFloat() * (float) (2 * Math.PI);
                float dist = circleRadius * 0.4f; // Inner circle
                whiteBall.x = centerX + (float) Math.cos(angle) * dist;
                whiteBall.y = centerY + (float) Math.sin(angle) * dist;
                whiteBall.vx = 0;
                whiteBall.vy = 0;

                immuneEndTime = System.currentTimeMillis() + 2000; // 2s Immunity
                createImpactBurst(whiteBall.x, whiteBall.y, Color.GREEN);
                playSound(soundTeleport);
                floatingTexts.add(new FloatingText("SAVED!", whiteBall.x, whiteBall.y - 100, Color.GREEN));
            }
        }

        // Tüm toplar toplandı mı? (Ve Boss yoksa)
        // DEBUG LOGGING - Track condition state (DISABLED - too spammy)
        // if (coloredBalls.size() == 0) {
        // android.util.Log.d("GameView", "All colored balls cleared! pendingBalls=" +
        // pendingColoredBalls +
        // ", boss=" + (currentBoss != null ? "EXISTS" : "null") +
        // ", levelCompleted=" + levelCompleted +
        // ", showBossDefeated=" + showBossDefeated);
        // }

        // Tüm toplar toplandı mı? (Ve Boss yoksa)
        // Stage completion check

        if (coloredBalls.size() == 0 && pendingColoredBalls == 0 && currentBoss == null && !showBossDefeated
                && !levelCompleted) {

            // Award 5 coins for stage completion
            int oldCoins = coins;
            coins += (5 + (upgradeLuck - 1)); // Upgrade: +1 coin per level
            // Quest 31-32, 49: Collect coins
            if (questManager != null) {
                questManager.incrementQuestProgress(31, 5);
                questManager.incrementQuestProgress(32, 5);
                questManager.incrementQuestProgress(49, 5); // Quest 49: Coin Millionaire
            }
            lastCoinAwardedLevel = level;
            lastCoinAwardedStage = stage; // Track stage too
            android.util.Log.d("GameView",
                    "✅ COIN AWARDED! Level " + level + " Stage " + stage + " cleared! Coins: " + oldCoins + " -> "
                            + coins);
            saveProgress(); // Coin'i kaydet

            // Force UI update to trigger animation
            if (mainActivity != null) {
                mainActivity.runOnUiThread(() -> {
                    updateMainActivityPanels();
                });
            }

            // Now trigger level completion animation
            if (!levelCompleted) {
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
        }

        // Stage Cleared animasyonu bitince bir sonraki stage'e geç
        if (levelCompleted && System.currentTimeMillis() - stageClearedTime > 3000) {
            levelCompleted = false;
            showStageCleared = false;

            // ✨ AWARD COINS FOR STAGE COMPLETION - MOVED HERE TO ENSURE IT WORKS
            // Award coins BEFORE incrementing stage
            // Simplified: Just check if we haven't given coins for this specific
            // level+stage combo
            boolean shouldAwardCoins = !(level == lastCoinAwardedLevel && stage == lastCoinAwardedStage);

            if (shouldAwardCoins) {
                int oldCoins = coins;
                coins += (5 + (upgradeLuck - 1)); // 5 coins + luck bonus

                // Quest tracking
                if (questManager != null) {
                    questManager.incrementQuestProgress(31, 5);
                    questManager.incrementQuestProgress(32, 5);
                    questManager.incrementQuestProgress(49, 5);
                }

                lastCoinAwardedLevel = level;
                lastCoinAwardedStage = stage;

                android.util.Log.d("GameView", "✅ COINS AWARDED! Level " + level + " Stage " + stage +
                        ": " + oldCoins + " -> " + coins);

                saveProgress();

                // Update UI
                if (mainActivity != null) {
                    mainActivity.runOnUiThread(() -> updateMainActivityPanels());
                }
            } else {
                android.util.Log.d("GameView", "⚠️ Coins already awarded for Level " + level + " Stage " + stage);
            }

            stage++;

            if (stage > 5) {
                stage = 1;

                // Log Debug
                android.util.Log.d("GameView", "Level Completed! Saving stars for Level: " + level);

                // Save 3 stars for the completed level
                android.content.Context ctx = getContext();
                if (ctx != null) {
                    android.content.SharedPreferences prefs = ctx.getSharedPreferences("SPACE_PROGRESS",
                            android.content.Context.MODE_PRIVATE);
                    String key = "level_" + level + "_stars";
                    android.util.Log.d("GameView", "Attempting to save: " + key + " = 3");

                    android.content.SharedPreferences.Editor editor = prefs.edit();
                    editor.putInt(key, 3);
                    boolean success = editor.commit(); // Use commit() instead of apply() for immediate save

                    android.util.Log.d("GameView", "Save result for " + key + ": " + (success ? "SUCCESS" : "FAILED"));

                    // Verify the save by reading it back
                    int savedStars = prefs.getInt(key, 0);
                    android.util.Log.d("GameView", "Verification read: " + key + " = " + savedStars);
                } else {
                    android.util.Log.e("GameView", "ERROR: Context is null, cannot save stars!");
                }

                // Show "LEVEL COMPLETE" text
                floatingTexts.add(new FloatingText("LEVEL COMPLETE!", centerX, centerY - 100, Color.GREEN));
                playSound(soundPower); // Victory sound

                level++; // Go to next level
                // Quest 21-23, 30: Reach specific levels
                // Quest 24: Century (100 stages)
                if (questManager != null) {
                    questManager.incrementQuestProgress(24, 1); // 100 stages
                    if (level >= 10)
                        questManager.incrementQuestProgress(21, level);
                    if (level >= 30)
                        questManager.incrementQuestProgress(22, level);
                    if (level >= 50)
                        questManager.incrementQuestProgress(23, level);
                    if (level >= 100)
                        questManager.incrementQuestProgress(30, level);
                    // Quest 25-27: Clear spaces
                    int space = ((level - 1) / 10) + 1;
                    if (space >= 3)
                        questManager.incrementQuestProgress(25, space);
                    if (space >= 5)
                        questManager.incrementQuestProgress(26, space);
                    if (space >= 10)
                        questManager.incrementQuestProgress(27, space);
                }
                android.util.Log.d("GameView", "Advancing to Next Level: " + level);

                // Quest tracking: Level completion
                if (questManager != null) {
                    // Quest 28: Perfection (no damage)
                    if (!tookDamageInLevel)
                        questManager.incrementQuestProgress(28, 1);
                    // Quest 29: Unstoppable (10 consecutive)
                    consecutiveLevelWins++;
                    if (consecutiveLevelWins >= 10)
                        questManager.incrementQuestProgress(29, consecutiveLevelWins);
                }
                tookDamageInLevel = false; // Reset for next level

                if (level > maxUnlockedLevel) {
                    maxUnlockedLevel = level;
                    saveProgress(); // This saves 'maxUnlockedLevel' to 'SpaceBilliard' prefs
                    android.util.Log.d("GameView", "New Max Unlocked Level: " + maxUnlockedLevel);

                    // Show "LEVEL UNLOCKED" text
                    floatingTexts
                            .add(new FloatingText("LEVEL " + level + " UNLOCKED!", centerX, centerY + 50, Color.CYAN));
                }
            } else {
                // Intermediate stage completed (stages 1-4), show stage cleared text
                int completedStage = stage - 1; // We already incremented, so -1 to get completed stage
                android.util.Log.d("GameView",
                        "⭐ STAGE CLEARED! Level " + level + " Stage " + completedStage + ". Next Stage: " + stage);
                android.util.Log.d("GameView",
                        "💵 Coin Status: coins=" + coins + ", lastCoinLevel=" + lastCoinAwardedLevel
                                + ", lastCoinStage=" + lastCoinAwardedStage);
                floatingTexts
                        .add(new FloatingText("STAGE " + completedStage + " CLEARED!", centerX, centerY, Color.YELLOW));
                playSound(soundCoin); // Lighter sound for stage clear
            }
            // else: Just continue to next stage within same level

            // Check for Game Completion (Level > 500 stages = 100 Levels)
            if (level > 500)

            {
                gameCompleted = true;
                gameStarted = false;
                updateUIPanels();
                return;
            }

            initLevel(level);
            return;
        }

        // Elektrik topu ikinci sıçrama kontrolü
        if (electricSecondBounce && System.currentTimeMillis() >= electricSecondBounceTime)

        {
            electricSecondBounce = false;
            if (currentBoss != null) {
                // Boss varsa ona sek
                electricEffects.add(new ElectricEffect(electricFirstTargetX, electricFirstTargetY, currentBoss.x,
                        currentBoss.y, 0));
                createImpactBurst(currentBoss.x, currentBoss.y, currentBoss.color);
                currentBoss.hp -= 150; // Electric damage
                playSound(soundElectric);
            } else if (coloredBalls.size() > 0) {
                Ball target2 = coloredBalls.get(random.nextInt(coloredBalls.size()));
                electricEffects
                        .add(new ElectricEffect(electricFirstTargetX, electricFirstTargetY, target2.x, target2.y, 0));
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
        if (whiteBall == null)
            return; // Safety check initialization

        // FROZEN STATE: Check if freeze time expired
        if (isFrozen) {
            if (System.currentTimeMillis() > frozenEndTime) {
                isFrozen = false;
                floatingTexts.add(new FloatingText("ICE MELTED", whiteBall.x, whiteBall.y - 80, Color.WHITE));
            }
        }

        // Player ball movement (SKIP if frozen)
        if (!isFrozen && (!isDragging || draggedBall != whiteBall)) {
            whiteBall.x += whiteBall.vx;
            whiteBall.y += whiteBall.vy;

            whiteBall.vx *= 0.998f;
            whiteBall.vy *= 0.998f;

            // Trail update
            updateBallTrail(whiteBall);

            // Quest 3: Multi-Hit Logic (Check when ball stops)
            if (ballsHitThisShot > 0 && Math.abs(whiteBall.vx) < 0.1 && Math.abs(whiteBall.vy) < 0.1) {
                if (ballsHitThisShot >= 2 && questManager != null) {
                    questManager.incrementQuestProgress(3, 1);
                }
                ballsHitThisShot = 0;
            }
        } else if (isFrozen) {
            // Frozen - allow movement from knockback, but don't lock if ball has velocity
            float speed = (float) Math.sqrt(whiteBall.vx * whiteBall.vx + whiteBall.vy * whiteBall.vy);
            if (speed > 2.0f) {
                // Being knocked back - allow movement
                whiteBall.x += whiteBall.vx;
                whiteBall.y += whiteBall.vy;
                whiteBall.vx *= 0.95f; // Slow down knockback
                whiteBall.vy *= 0.95f;
            } else {
                // Not moving - lock in place
                whiteBall.vx = 0;
                whiteBall.vy = 0;
            }
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

                ball.vx *= 0.995f;
                ball.vy *= 0.995f;
            }
        }

        // Boss Update
        // Stop Boss movement if defeated logic is active
        if (currentBoss != null && !showBossDefeated) {
            currentBoss.update();
        }

        // Ambient / Boss Particles (Run for Bosses OR specific Levels)
        String ambience = "";
        if (currentBoss != null && !showBossDefeated) {
            ambience = currentBoss.name;
        } else if (level > 150 && level <= 200) {
            ambience = "NEBULON"; // Space 4 Ambience
        }

        if (!ambience.isEmpty() && particles.size() < 150 && random.nextFloat() < 0.01f) { // 1% per frame, capped at
                                                                                           // 150
            float x = random.nextFloat() * screenWidth;
            float y = random.nextFloat() * screenHeight; // Spawn anywhere
            float angle = (float) (Math.PI / 2); // Upward
            float speed = 2 + random.nextFloat() * 3;

            if (ambience.equals("SOLARION")) {
                particles.add(new Particle(x, y, angle, speed, Color.rgb(255, 100 + random.nextInt(100), 0),
                        ParticleType.FLAME));
            } else if (ambience.equals("NEBULON")) {
                particles.add(new Particle(x, y, angle, speed, Color.rgb(138, 43, 226), ParticleType.CIRCLE));
            } else if (ambience.equals("GRAVITON")) {
                particles.add(new Particle(x, y, angle, speed, Color.rgb(50, 0, 200), ParticleType.STAR));
            } else if (ambience.equals("MECHA-CORE")) {
                particles.add(new Particle(x, y, angle, speed, Color.CYAN, ParticleType.CIRCLE));
            } else if (ambience.equals("CRYO-STASIS")) {
                particles.add(new Particle(x, y, angle, speed, Color.rgb(200, 230, 255), ParticleType.STAR));
            } else if (ambience.equals("GEO-BREAKER")) {
                particles.add(new Particle(x, y, angle, speed, Color.rgb(139, 69, 19), ParticleType.CIRCLE));
            } else if (ambience.equals("BIO-HAZARD")) {
                particles.add(new Particle(x, y, angle, speed, Color.rgb(50, 205, 50), ParticleType.CIRCLE));
            } else if (ambience.equals("CHRONO-SHIFTER")) {
                particles.add(new Particle(x, y, angle, speed, Color.rgb(255, 215, 0), ParticleType.STAR));
            }
        }

        // Renkli toplar (freeze kontrolü)
        // PERFORMANCE: Indexed loops (no Iterator)
        if (!freezeActive) {
            for (int i = 0; i < coloredBalls.size(); i++) {
                Ball ball = coloredBalls.get(i);
                ball.x += ball.vx;
                ball.y += ball.vy;
            }
            for (int i = 0; i < blackBalls.size(); i++) {
                Ball ball = blackBalls.get(i);
                ball.x += ball.vx;
                ball.y += ball.vy;
            }
            for (int i = 0; i < specialBalls.size(); i++) {
                SpecialBall ball = specialBalls.get(i);
                ball.x += ball.vx;
                ball.y += ball.vy;
            }
        }

        // GIANT MODE
        // UFO UPDATE
        if (activeUfo != null) {
            activeUfo.update();
        }

        // SLOWMO MODE

        // Black hole çekimi
        if (blackHoleActive) {
            attractBallsToWhite();
        }

        // Çarpışmalar
        checkCollisions();

        // Missiles
        updateMissiles();

        // Blast wave
        if (blastWave != null)

        {
            blastWave.update();
            if (blastWave.isDead())
                blastWave = null;
        }

        // Vortex
        if (activeVortex != null) {
            activeVortex.update();
            if (activeVortex.isDead()) {
                activeVortex = null;
            }
        }

        // Electric effects
        for (int i = electricEffects.size() - 1; i >= 0; i--) {
            ElectricEffect effect = electricEffects.get(i);
            effect.update();
            if (effect.isDead()) // Only remove dead effects, update handles lifetime/fade
                electricEffects.remove(i);
        }

        for (int i = particles.size() - 1; i >= 0; i--) {
            Particle p = particles.get(i);
            p.update();
            if (p.isDead()) {
                // Swap & pop pattern for O(1) removal instead of O(n)
                int lastIdx = particles.size() - 1;
                if (i != lastIdx) {
                    particles.set(i, particles.get(lastIdx));
                }
                particles.remove(lastIdx);
            }
        }

        for (int i = impactArcs.size() - 1; i >= 0; i--) {
            ImpactArc arc = impactArcs.get(i);
            arc.update();
            if (arc.isDead()) {
                // Swap & pop pattern for O(1) removal
                int lastIdx = impactArcs.size() - 1;
                if (i != lastIdx) {
                    impactArcs.set(i, impactArcs.get(lastIdx));
                }
                impactArcs.remove(lastIdx);
            }
        }

        // Floating texts update
        for (int i = floatingTexts.size() - 1; i >= 0; i--) {
            FloatingText ft = floatingTexts.get(i);
            ft.update();
            if (ft.isDead()) {
                // Swap & pop pattern for O(1) removal
                int lastIdx = floatingTexts.size() - 1;
                if (i != lastIdx) {
                    floatingTexts.set(i, floatingTexts.get(lastIdx));
                }
                floatingTexts.remove(lastIdx);
            }
        }

        // Yıldızları güncelle
        if (stars != null) {
            for (Star star : stars) {
                star.update(screenWidth, screenHeight);
            }
        }

        // Comet updates (PERFORMANCE: moved from draw loop)
        if (comets != null) {
            for (Comet c : comets) {
                c.update(screenWidth, screenHeight);
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

        // Player Boss Death Check (removed currentBoss null check)
        if (playerHp <= 0 && !gameOver && !showPlayerDefeated) {
            playerHp = 0; // Clamp to prevent retrigger from additional damage
            showPlayerDefeated = true;
            playerDefeatedTime = System.currentTimeMillis();
            playerDefeatedTime = System.currentTimeMillis();
            playSound(soundGameOver);

            // Clear all balls on defeat
            if (coloredBalls != null)
                coloredBalls.clear();
            if (blackBalls != null)
                blackBalls.clear();
            if (specialBalls != null)
                specialBalls.clear();
            if (bossProjectiles != null)
                bossProjectiles.clear();
            if (electricEffects != null)
                electricEffects.clear();

            // Player ball explosion visual
            for (int k = 0; k < 8; k++) {
                createImpactBurst(whiteBall.x + (random.nextFloat() - 0.5f) * 100,
                        whiteBall.y + (random.nextFloat() - 0.5f) * 100, Color.WHITE);
            }
            playSound(soundBlackExplosion);
        }

        if (showPlayerDefeated && System.currentTimeMillis() - playerDefeatedTime > 3000) {
            gameOver = true;
            showPlayerDefeated = false;
            updateUIPanels();
        }

        if (showBossDefeated) {
            // Clear all balls on boss defeat
            if (coloredBalls != null && !coloredBalls.isEmpty())
                coloredBalls.clear();
            if (blackBalls != null && !blackBalls.isEmpty())
                blackBalls.clear();
            if (specialBalls != null && !specialBalls.isEmpty())
                specialBalls.clear();
            if (bossProjectiles != null && !bossProjectiles.isEmpty())
                bossProjectiles.clear();
            if (electricEffects != null && !electricEffects.isEmpty())
                electricEffects.clear();

            if (System.currentTimeMillis() - bossDefeatedTime > 3000) {
                showBossDefeated = false;
                levelCompleted = true; // Trigger next level
            }
        }
        // --- SPLIT SAVE CLONE TIMEOUT ---
        if (isSplitSaveActive && System.currentTimeMillis() > cloneExpirationTime) {
            // Time up for clones
            isSplitSaveActive = false;

            // Clones vanish with particle effects
            for (Ball c : cloneBalls) {
                createImpactBurst(c.x, c.y, Color.MAGENTA);
            }
            cloneBalls.clear();

            // REMOVED: Teleport to center after immunity
            // Player ball should stay where it is and continue moving
        }

    }

    private void updateBallTrail(Ball ball) {
        if (!selectedTrail.equals("none") && (Math.abs(ball.vx) > 0.1 || Math.abs(ball.vy) > 0.1)) {
            ball.trail.add(0, new TrailPoint(ball.x, ball.y, ball.radius));
            if (ball.trail.size() > MAX_TRAIL_POINTS) {
                // Safety check: ensure index is valid before removing
                if (ball.trail.size() > 0) {
                    ball.trail.remove(ball.trail.size() - 1);
                }
            }
        } else {
            if (ball.trail.size() > 0)
                ball.trail.remove(ball.trail.size() - 1);
        }
    }

    private void attractBallsToWhite() {
        float attractionSpeed = 5;

        // PERFORMANCE: Indexed loop instead of enhanced for (no Iterator allocation)
        for (int i = 0; i < coloredBalls.size(); i++) {
            Ball ball = coloredBalls.get(i);
            float dx = whiteBall.x - ball.x;
            float dy = whiteBall.y - ball.y;
            float distSq = dx * dx + dy * dy;
            if (distSq > 25) { // 5 * 5
                float distance = (float) Math.sqrt(distSq);
                ball.x += (dx / distance) * attractionSpeed;
                ball.y += (dy / distance) * attractionSpeed;
            }
        }

        for (int i = 0; i < specialBalls.size(); i++) {
            SpecialBall ball = specialBalls.get(i);
            float dx = whiteBall.x - ball.x;
            float dy = whiteBall.y - ball.y;
            float distSq = dx * dx + dy * dy;
            if (distSq > 25) { // 5 * 5
                float distance = (float) Math.sqrt(distSq);
                ball.x += (dx / distance) * attractionSpeed;
                ball.y += (dy / distance) * attractionSpeed;
            }
        }
    }

    private boolean checkBallCollision(Ball b1, Ball b2) {
        float dx = b1.x - b2.x;
        float dy = b1.y - b2.y;
        float distSq = dx * dx + dy * dy;
        float radSum = b1.radius + b2.radius;
        return distSq < radSum * radSum;
    }

    private boolean checkBallCollision(Ball b1, SpecialBall b2) {
        float dx = b1.x - b2.x;
        float dy = b1.y - b2.y;
        float distance = (float) Math.sqrt(dx * dx + dy * dy);
        return distance < b1.radius + b2.radius;
    }

    private void activateSpecialPower(String type, Ball targetBall) {
        // Inventory Collection Logic
        // ONLY specific types go to inventory: magma, lightning, multiball, ufo,
        // repulsor, alchemy, swarm
        if (type.equals("magma") || type.equals("lightning") || type.equals("multiball") || type.equals("ufo")
                || type.equals("repulsor") || type.equals("alchemy") || type.equals("swarm")) {

            if (inventory.size() >= MAX_INVENTORY_SIZE) {
                // FIFO Replacement: Remove first slot (oldest) which is index 0
                inventory.remove(0);
            }
            inventory.add(type);
            playSound(soundCoin);
            floatingTexts.add(new FloatingText("COLLECTED!", targetBall.x, targetBall.y, Color.GREEN, 0.5f));

            // Quest 50: Arsenal Master - Hold 3 power-ups simultaneously
            if (questManager != null && inventory.size() >= 3) {
                questManager.incrementQuestProgress(50, 1);
            }

            // Quest 46: Ability Addict - Use different skills in one level
            if (questManager != null) {
                skillsUsedThisLevel.add(type);
                if (skillsUsedThisLevel.size() >= 10) {
                    questManager.incrementQuestProgress(46, 1);
                }
            }

            return; // Do not activate immediately
        }

        activateSkillEffect(type, targetBall);

        // Quest 46: Track skill usage
        if (questManager != null) {
            skillsUsedThisLevel.add(type);
            if (skillsUsedThisLevel.size() >= 10) {
                questManager.incrementQuestProgress(46, 1);
            }
        }
    }

    private void activateSkillEffect(String type, Ball targetBall) {
        switch (type) {
            case "blackhole":
                blackHoleActive = true;
                blackHoleEndTime = System.currentTimeMillis() + 2000;
                break;
            case "extraTime":
                timeLeft += (long) (5000 * (1.0f + (upgradeEnergy - 1) * 0.1f));
                break;
            case "powerBoost":
                powerBoostActive = true;
                playSound(soundPower);
                break;
            case "barrier":
                barrierActive = true;
                barrierEndTime = System.currentTimeMillis() + (long) (5000 * (1.0f + (upgradeEnergy - 1) * 0.1f));
                playSound(soundShield);
                // Quest 9: Shielded (20 barrier uses)
                if (questManager != null) {
                    questManager.incrementQuestProgress(9, 1);
                }
                break;
            case "multiball":
                multiBallActive = true;
                playSound(soundPower);
                break;
            case "magma":
                magmaTrailActive = true;
                magmaTrailEndTime = System.currentTimeMillis() + (long) (10000 * (1.0f + (upgradeEnergy - 1) * 0.1f));
                playSound(soundPower); // Using power sound
                break;
            case "electric":
                triggerElectric();
                playSound(soundElectric);
                electricModeEndTime = System.currentTimeMillis() + (long) (10000 * (1.0f + (upgradeEnergy - 1) * 0.1f)); // Upgradeable
                // Quest 7: Electric Storm (10 electric uses)
                if (questManager != null) {
                    questManager.incrementQuestProgress(7, 1);
                }
                break;
            case "clone":
                // Ghost mode aktifse orijinal boyutu kullan
                float cloneRadius = ghostModeActive ? originalWhiteBallRadius : whiteBall.radius;
                Ball clone = new Ball(centerX, centerY, cloneRadius, Color.WHITE, 5000); // 5 saniye ömür
                cloneBalls.add(clone);
                break;
            case "freeze":
                freezeActive = true;
                freezeEndTime = System.currentTimeMillis() + (long) (5000 * (1.0f + (upgradeEnergy - 1) * 0.1f));
                // Quest 8: Ice Age (5 freeze uses)
                if (questManager != null) {
                    questManager.incrementQuestProgress(8, 1);
                }
                playSound(soundFreeze);
                break;
            case "missile":
                // Fire missile - Priority: Black Balls > Colored Balls > Boss
                Ball target = null;
                if (blackBalls.size() > 0) {
                    target = blackBalls.get(0);
                } else if (coloredBalls.size() > 0) {
                    target = coloredBalls.get(0);
                }
                missiles.add(new GuidedMissile(whiteBall.x, whiteBall.y, target));
                playSound(soundMissile);
                break;
            case "teleport":
                // Charge passive teleport ability
                if (!activePassivePower.equals("teleport")) {
                    activePassivePower = "teleport";
                    playSound(soundPower);
                    floatingTexts.add(new FloatingText("PASSIVE READY!", targetBall.x, targetBall.y, Color.GREEN));
                    createParticles(targetBall.x, targetBall.y, Color.GREEN);
                } else {
                    // Already charged
                    playSound(soundCoin);
                    floatingTexts.add(new FloatingText("ALREADY CHARGED", targetBall.x, targetBall.y, Color.LTGRAY));
                }
                break;
            case "split_save":
                // Charge passive split ability
                if (!activePassivePower.equals("split_save")) {
                    activePassivePower = "split_save";
                    playSound(soundPower);
                    floatingTexts.add(new FloatingText("SPLIT READY!", targetBall.x, targetBall.y, Color.MAGENTA));
                    createParticles(targetBall.x, targetBall.y, Color.MAGENTA);
                } else {
                    // Already charged
                    playSound(soundCoin);
                    floatingTexts.add(new FloatingText("ALREADY CHARGED", targetBall.x, targetBall.y, Color.LTGRAY));
                }
                break;
            case "vortex":
                // Charge passive vortex ability
                if (!activePassivePower.equals("vortex")) {
                    activePassivePower = "vortex";
                    playSound(soundPower);
                    floatingTexts.add(new FloatingText("VORTEX READY!", targetBall.x, targetBall.y, Color.CYAN));
                    createParticles(targetBall.x, targetBall.y, Color.CYAN);
                } else {
                    // Already charged
                    playSound(soundCoin);
                    floatingTexts.add(new FloatingText("ALREADY CHARGED", targetBall.x, targetBall.y, Color.LTGRAY));
                }
                break;
            case "lightning":
                // Trigger sequenced lightning (3 strikes, 1s interval)
                pendingLightningStrikes = 3;
                lastLightningStrikeTime = System.currentTimeMillis() - 1000; // Force first strike immediately
                playSound(soundElectric);
                createImpactBurst(centerX, centerY, Color.YELLOW);
                break;
            case "boom":
                blastWave = new BlastWave(targetBall.x, targetBall.y);
                break;
            case "ghost":
                // G topu sadece oyuncu topuna etki etmeli (özellikli topları büyütmemeli)
                if (!ghostModeActive && targetBall == whiteBall) {
                    originalWhiteBallRadius = whiteBall.radius;
                    ghostModeActive = true;
                    ghostModeEndTime = System.currentTimeMillis() + 3000;
                    // Sadece beyaz topu büyüt
                    whiteBall.radius = originalWhiteBallRadius * 1.75f;
                }
                break;
            case "repulsor":
                // PUSH AWAY
                createImpactBurst(targetBall.x, targetBall.y, Color.CYAN);
                playSound(soundBlackExplosion);
                for (Ball b : coloredBalls) {
                    float dx = b.x - targetBall.x;
                    float dy = b.y - targetBall.y;
                    float d = (float) Math.sqrt(dx * dx + dy * dy);
                    if (d > 0) {
                        b.vx += (dx / d) * 30;
                        b.vy += (dy / d) * 30;
                    }
                }
                for (Ball b : blackBalls) {
                    float dx = b.x - targetBall.x;
                    float dy = b.y - targetBall.y;
                    float d = (float) Math.sqrt(dx * dx + dy * dy);
                    if (d > 0) {
                        b.vx += (dx / d) * 50;
                        b.vy += (dy / d) * 50;
                    }
                }

                // REFLECT BOSS PROJECTILES (User Request)
                if (currentBoss != null && !bossProjectiles.isEmpty()) {
                    for (Ball proj : bossProjectiles) {
                        float dx = proj.x - targetBall.x;
                        float dy = proj.y - targetBall.y;
                        float d = (float) Math.sqrt(dx * dx + dy * dy);
                        if (d > 0 && d < 300) { // Within repulsor range
                            // Reverse velocity to send back to boss
                            proj.vx = -proj.vx * 1.5f;
                            proj.vy = -proj.vy * 1.5f;

                            // Check if projectile will hit boss
                            float projDx = currentBoss.x - proj.x;
                            float projDy = currentBoss.y - proj.y;
                            float projDist = (float) Math.sqrt(projDx * projDx + projDy * projDy);
                            if (projDist < currentBoss.radius + proj.radius + 100) {
                                // Deal damage to boss
                                currentBoss.hp -= 15;
                                createImpactBurst(proj.x, proj.y, Color.CYAN);
                                floatingTexts.add(new FloatingText("-15 BOSS!", proj.x, proj.y, Color.CYAN));
                            }
                        }
                    }
                }
                break;
            case "alchemy":
                // Convert Black to Color
                playSound(soundCoin);
                for (Ball b : blackBalls) {
                    createParticles(b.x, b.y, Color.rgb(255, 215, 0));
                    // Spawn colored ball at position
                    Ball newB = new Ball(b.x, b.y, (circleRadius / 0.47f) * 0.02f, Color.YELLOW);
                    newB.vx = b.vx;
                    newB.vy = b.vy;
                    coloredBalls.add(newB);
                }
                blackBalls.clear();

                // TRANSFORM BOSS PROJECTILES TO COLORED BALLS (User Request)
                if (currentBoss != null && !bossProjectiles.isEmpty()) {
                    for (Ball proj : bossProjectiles) {
                        createParticles(proj.x, proj.y, Color.rgb(255, 215, 0));
                        // Spawn colored ball at position
                        Ball newB = new Ball(proj.x, proj.y, (circleRadius / 0.47f) * 0.02f, Color.YELLOW);
                        newB.vx = proj.vx * 0.5f; // Reduce velocity
                        newB.vy = proj.vy * 0.5f;
                        coloredBalls.add(newB);
                    }
                    bossProjectiles.clear();
                    floatingTexts.add(
                            new FloatingText("ALCHEMIZED!", targetBall.x, targetBall.y - 50, Color.rgb(255, 215, 0)));
                }

                floatingTexts.add(new FloatingText("ALCHEMY!", targetBall.x, targetBall.y, Color.rgb(255, 215, 0)));
                break;
            case "ufo":
                activeUfo = new Ufo();
                playSound(soundPower);
                floatingTexts.add(new FloatingText("UFO ARRIVED!", targetBall.x, targetBall.y, Color.GREEN));
                break;

            case "swarm":
                playSound(soundMissile);
                int count = 5;
                for (int k = 0; k < count; k++) {
                    Ball swarmTarget = (blackBalls.size() > 0) ? blackBalls.get(random.nextInt(blackBalls.size()))
                            : null;
                    GuidedMissile m = new GuidedMissile(targetBall.x, targetBall.y, swarmTarget);
                    m.vx = (random.nextFloat() - 0.5f) * 10;
                    m.vy = (random.nextFloat() - 0.5f) * 10;
                    missiles.add(m);
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
        // Standard Electric (Type 0)
        electricEffects.add(new ElectricEffect(whiteBall.x, whiteBall.y, target1.x, target1.y, 0));
        createImpactBurst(target1.x, target1.y, target1.color);
        score++;
        comboCounter++;
        playSound(soundElectric); // İlk sıçrama sesi
        coloredBalls.remove(target1);

        // İkinci hedef için gecikme ayarla (0.4 saniye)
        if (currentBoss != null || coloredBalls.size() > 0) {
            electricFirstTargetX = target1.x;
            electricFirstTargetY = target1.y;
            electricSecondBounce = true;
            electricSecondBounceTime = System.currentTimeMillis() + 400;
        }
    }

    private void updateMissiles() {
        for (int i = missiles.size() - 1; i >= 0; i--) {
            GuidedMissile missile = missiles.get(i);

            // SMOKE TRAIL (User Request)
            if (random.nextInt(3) == 0) { // Frequent smoke
                particles.add(new Particle(missile.x, missile.y, (float) (random.nextFloat() * 2 * Math.PI), 2,
                        Color.DKGRAY));
            }

            // TARGET VALIDATION & PRIORITY (Black > Colored > None)
            // Ensure we have a valid target or switch to high priority
            // TARGET VALIDATION & PRIORITY (BOSS > Black > Colored > None)
            if (currentBoss != null) {
                missile.target = null; // Let GuidedMissile handle boss targeting internally
            } else if (missile.target == null
                    || (!blackBalls.contains(missile.target) && !coloredBalls.contains(missile.target))) {
                if (blackBalls.size() > 0)
                    missile.target = blackBalls.get(0);
                else if (coloredBalls.size() > 0)
                    missile.target = coloredBalls.get(0);
                else
                    missile.target = null;
            }

            // MOVEMENT
            if (missile.target != null || currentBoss != null) {
                missile.update(); // Homing
            } else {
                // FLY AWAY LOGIC (No targets)
                float dx = missile.x - centerX;
                float dy = missile.y - centerY;
                float ang = (float) Math.atan2(dy, dx);
                missile.x += Math.cos(ang) * 15; // Fast exit
                missile.y += Math.sin(ang) * 15;
            }

            // REMOVEL IF OFF SCREEN (Generic for both states)
            float distFromCenterSq = (missile.x - centerX) * (missile.x - centerX)
                    + (missile.y - centerY) * (missile.y - centerY);
            if (distFromCenterSq > (circleRadius * 2.5f) * (circleRadius * 2.5f)) {
                missiles.remove(i);
                continue;
            }

            // Hedef kontrolü
            boolean hit = false;
            // Black Ball Collision
            for (int j = blackBalls.size() - 1; j >= 0; j--) {
                Ball ball = blackBalls.get(j);
                float dx = missile.x - ball.x;
                float dy = missile.y - ball.y;
                float distSq = dx * dx + dy * dy;
                float radSum = missile.radius + ball.radius;

                if (distSq < radSum * radSum) {
                    createParticles(ball.x, ball.y, Color.BLACK);
                    blackBalls.remove(j);
                    // Quest 2: Dark Matter & Quest 5: Destroyer II
                    if (questManager != null) {
                        questManager.incrementQuestProgress(2, 1);
                        questManager.incrementQuestProgress(5, 1);
                    }
                    missiles.remove(i);
                    playSound(soundBlackExplosion);
                    hit = true;
                    break;
                }
            }
            if (hit)
                continue;

            // Colored Ball Collision (FIX: Now destroys colored balls)
            for (int j = coloredBalls.size() - 1; j >= 0; j--) {
                Ball ball = coloredBalls.get(j);
                float dx = missile.x - ball.x;
                float dy = missile.y - ball.y;
                float distSq = dx * dx + dy * dy;
                float radSum = missile.radius + ball.radius;

                if (distSq < radSum * radSum) {
                    createParticles(ball.x, ball.y, ball.color);
                    coloredBalls.remove(j);
                    // Quest 1 & 5: Colored ball destruction
                    if (questManager != null) {
                        questManager.incrementQuestProgress(1, 1);
                        questManager.incrementQuestProgress(5, 1);
                    }
                    missiles.remove(i);
                    playSound(soundBlackExplosion);
                    hit = true;
                    break;
                }
            }
            if (hit)
                continue;

            // Boss Collision
            if (currentBoss != null) {
                float dx = missile.x - currentBoss.x;
                float dy = missile.y - currentBoss.y;
                float distSq = dx * dx + dy * dy;
                float radSum = missile.radius + currentBoss.radius;

                if (distSq < radSum * radSum) {
                    currentBoss.hp -= 100;
                    createParticles(missile.x, missile.y, Color.MAGENTA);
                    playSound(soundBlackExplosion);
                    missiles.remove(i);

                    if (currentBoss.hp <= 0 && !showBossDefeated) {
                        currentBoss = null;
                        showBossDefeated = true;
                        bossDefeatedTime = System.currentTimeMillis();
                        for (int k = 0; k < 5; k++)
                            createImpactBurst(centerX + (random.nextFloat() - 0.5f) * 300,
                                    centerY + (random.nextFloat() - 0.5f) * 300, Color.RED);
                        playSound(soundBlackExplosion);
                    }
                    continue;
                }
            }
        }
    }

    private void checkCollisions() {
        // --- BOSS PROJECTILES & MAGMA (Restored Logic) ---
        // Boss Projectiles vs Player
        for (int i = bossProjectiles.size() - 1; i >= 0; i--) {
            Ball p = bossProjectiles.get(i);
            if (whiteBall != null) {
                float dx = p.x - whiteBall.x;
                float dy = p.y - whiteBall.y;
                if (Math.sqrt(dx * dx + dy * dy) < p.radius + whiteBall.radius) {
                    // ICE SHATTER: If player is frozen, this attack shatters ice and knocks back
                    if (isFrozen) {
                        isFrozen = false;
                        frozenEndTime = 0;

                        // Ice shatter particle burst
                        for (int k = 0; k < 40; k++) {
                            float angle = random.nextFloat() * (float) (2 * Math.PI);
                            float speed = 5 + random.nextFloat() * 8;
                            particles.add(new Particle(
                                    whiteBall.x, whiteBall.y,
                                    angle, speed,
                                    Color.rgb(150, 200, 255),
                                    ParticleType.STAR));
                        }

                        // Massive knockback away from boss
                        if (currentBoss != null) {
                            float bossToPlayerX = whiteBall.x - currentBoss.x;
                            float bossToPlayerY = whiteBall.y - currentBoss.y;
                            float dist = (float) Math
                                    .sqrt(bossToPlayerX * bossToPlayerX + bossToPlayerY * bossToPlayerY);
                            if (dist > 0) {
                                whiteBall.vx = (bossToPlayerX / dist) * 45; // Very strong knockback
                                whiteBall.vy = (bossToPlayerY / dist) * 45;
                            }
                        }

                        floatingTexts
                                .add(new FloatingText("ICE SHATTERED!", whiteBall.x, whiteBall.y - 100, Color.WHITE));
                        playSound(soundBlackExplosion);
                        createImpactBurst(whiteBall.x, whiteBall.y, Color.CYAN);

                        bossProjectiles.remove(i);
                        continue;
                    }

                    // Check for Passive Teleport Save
                    if (activePassivePower.equals("teleport")) {
                        activePassivePower = "none";

                        // Teleport save effects (Green)
                        createImpactBurst(whiteBall.x, whiteBall.y, Color.GREEN);
                        for (int particleIdx = 0; particleIdx < 20; particleIdx++) {
                            particles.add(
                                    new Particle(whiteBall.x, whiteBall.y, random.nextFloat() * (float) (2 * Math.PI),
                                            random.nextFloat() * 8 + 4, Color.GREEN));
                        }

                        // Teleport
                        float angle = random.nextFloat() * (float) (2 * Math.PI);
                        float dist = circleRadius * 0.5f;
                        whiteBall.x = centerX + (float) Math.cos(angle) * dist;
                        whiteBall.y = centerY + (float) Math.sin(angle) * dist;
                        whiteBall.vx = 0;
                        whiteBall.vy = 0;

                        createImpactBurst(whiteBall.x, whiteBall.y, Color.GREEN);
                        playSound(soundShield);
                        floatingTexts.add(new FloatingText("SAVED!", whiteBall.x, whiteBall.y - 100, Color.GREEN));
                        immuneEndTime = System.currentTimeMillis() + 3000;

                        bossProjectiles.remove(i);
                        continue;

                    } else if (activePassivePower.equals("split_save")) {
                        // PASSIVE SPLIT SAVE vs BOSS PROJECTILE
                        activePassivePower = "none";

                        createImpactBurst(whiteBall.x, whiteBall.y, Color.MAGENTA);
                        playSound(soundPower);
                        floatingTexts.add(new FloatingText("SPLIT SAVE!", centerX, centerY, Color.MAGENTA));

                        // Spawn 3 Mini Clones
                        for (int k = 0; k < 3; k++) {
                            Ball clone = new Ball(whiteBall.x, whiteBall.y, whiteBall.radius * 0.6f, Color.MAGENTA,
                                    4000);
                            float angle = (float) (k * (2 * Math.PI / 3));
                            clone.vx = (float) Math.cos(angle) * 15;
                            clone.vy = (float) Math.sin(angle) * 15;
                            cloneBalls.add(clone);
                        }

                        // MODIFIED: Do NOT hide/teleport the main ball.
                        // Instead, make it immune and let it continue moving.
                        immuneEndTime = System.currentTimeMillis() + 4000; // 4s Immunity

                        isSplitSaveActive = true;
                        cloneExpirationTime = System.currentTimeMillis() + 4000;

                        bossProjectiles.remove(i);
                        continue;
                    }

                    // Check for Passive Vortex
                    if (activePassivePower.equals("vortex")) {
                        activePassivePower = "none";

                        // Create vortex
                        activeVortex = new Vortex(whiteBall.x, whiteBall.y);

                        // Visual feedback
                        createImpactBurst(whiteBall.x, whiteBall.y, Color.CYAN);
                        for (int pIdx = 0; pIdx < 30; pIdx++) {
                            float pAngle = random.nextFloat() * (float) (2 * Math.PI);
                            float pSpeed = random.nextFloat() * 10 + 5;
                            particles.add(new Particle(whiteBall.x, whiteBall.y, pAngle, pSpeed, Color.CYAN));
                        }
                        playSound(soundPower);
                        floatingTexts.add(new FloatingText("VORTEX!", whiteBall.x, whiteBall.y - 50, Color.CYAN));

                        // Give immunity during vortex
                        immuneEndTime = System.currentTimeMillis() + 2500;

                        bossProjectiles.remove(i);
                        continue;
                    }

                    if (barrierActive) {
                        createImpactBurst(p.x, p.y, Color.CYAN); // Block effect
                        playSound(soundShield);
                        // Vampire Core Healing
                        if (upgradeVampire > 0) {
                            int heal = 2 * upgradeVampire; // 2 HP per level
                            playerHp = Math.min(playerHp + heal, playerMaxHp);
                            floatingTexts.add(new FloatingText("+" + heal, whiteBall.x, whiteBall.y, Color.GREEN));
                        }
                        bossProjectiles.remove(i);
                        continue;
                    }

                    // VORTEX IMMUNITY: If a vortex is actively swirling, player is safe
                    if (activeVortex != null) {
                        createImpactBurst(p.x, p.y, Color.CYAN); // Block effect
                        // Vampire Core Healing
                        if (upgradeVampire > 0) {
                            int heal = 2 * upgradeVampire;
                            playerHp = Math.min(playerHp + heal, playerMaxHp);
                            floatingTexts.add(new FloatingText("+" + heal, whiteBall.x, whiteBall.y, Color.GREEN));
                        }
                        bossProjectiles.remove(i);
                        // Optional: Play a sound or show text?
                        continue;
                    }

                    playerHp -= 100; // Damage
                    createImpactBurst(whiteBall.x, whiteBall.y, Color.RED);
                    bossProjectiles.remove(i);
                    // Shake
                    cameraShakeX = 20;
                    shakeEndTime = System.currentTimeMillis() + 300;
                    if (System.currentTimeMillis() - lastImpactSoundTime > 50) {
                        playSound(soundCollision);
                        lastImpactSoundTime = System.currentTimeMillis();
                    }
                }
            }
        }

        // Magma Collisions
        for (

                int m = magmaPatches.size() - 1; m >= 0; m--) {
            MagmaPatch p = magmaPatches.get(m);
            if (currentBoss != null) {
                float dx = p.x - currentBoss.x;
                float dy = p.y - currentBoss.y;
                if (Math.sqrt(dx * dx + dy * dy) < p.radius + currentBoss.radius) {
                    currentBoss.hp -= 0.1f; // Magma damage reduced to 0.1
                }
            }
            // Vs Colored Balls
            for (int i = coloredBalls.size() - 1; i >= 0; i--) {
                Ball b = coloredBalls.get(i);
                float dx = p.x - b.x;
                float dy = p.y - b.y;
                if (Math.sqrt(dx * dx + dy * dy) < p.radius * 2.0 + b.radius) { // Radius increased for easier hit
                    createImpactBurst(b.x, b.y, b.color);
                    coloredBalls.remove(i);

                    // Quest 44: Lightning Reflexes - Destroy 10 balls in 5 seconds
                    if (questManager != null) {
                        long currentTime = System.currentTimeMillis();
                        if (currentTime - last5SecondWindowStart > 5000) {
                            // Reset window
                            last5SecondWindowStart = currentTime;
                            ballsDestroyedInLast5Seconds = 0;
                        }
                        ballsDestroyedInLast5Seconds++;
                        if (ballsDestroyedInLast5Seconds >= 10) {
                            questManager.incrementQuestProgress(44, 1);
                            ballsDestroyedInLast5Seconds = 0; // Reset
                            last5SecondWindowStart = currentTime;
                        }
                    }

                    playSound(soundCollision);
                }
            }
        }

        // --- WALL COLLISIONS (Geometric Shapes) ---
        // Must run even during drag to keep balls inside new shapes
        int space = ((level - 1) / 10) + 1; // Corrected from 50 to 10 to match draw() logic
        float damping = 0.9f + (upgradeImpulse > 1 ? (upgradeImpulse - 1) * 0.02f : 0);

        ArrayList<Ball> allBalls = new ArrayList<>();
        if (whiteBall != null)
            allBalls.add(whiteBall);
        allBalls.addAll(coloredBalls);
        allBalls.addAll(blackBalls);
        allBalls.addAll(specialBalls);
        try {
            allBalls.addAll(cloneBalls);
        } catch (Exception e) {
        }

        if (space == 2) {
            // --- SPACE 2: SQUARE (Stages 11-20) ---
            float boundary = circleRadius;
            float left = centerX - boundary;
            float right = centerX + boundary;
            // Match visual fix: top is lower (0.85f) to avoid UI overlap
            float top = centerY - boundary * 0.85f;
            float bottom = centerY + boundary;

            if (whiteBall != null)
                handleBoxCollision(whiteBall, left, right, top, bottom, damping);
            for (Ball ball : coloredBalls)
                handleBoxCollision(ball, left, right, top, bottom, damping);
            for (Ball ball : blackBalls)
                handleBoxCollision(ball, left, right, top, bottom, damping);
            for (SpecialBall ball : specialBalls)
                handleBoxCollision(ball, left, right, top, bottom, damping);
            for (Ball ball : cloneBalls)
                handleBoxCollision(ball, left, right, top, bottom, damping);

        } else if (space == 3) {
            // --- SPACE 3: RECTANGLE (Stages 101-150) ---
            float halfW = circleRadius;
            float halfH = circleRadius * 0.7f;
            float left = centerX - halfW;
            float right = centerX + halfW;
            float top = centerY - halfH;
            float bottom = centerY + halfH;

            if (whiteBall != null)
                handleBoxCollision(whiteBall, left, right, top, bottom, damping);
            for (Ball ball : coloredBalls)
                handleBoxCollision(ball, left, right, top, bottom, damping);
            for (Ball ball : blackBalls)
                handleBoxCollision(ball, left, right, top, bottom, damping);
            for (SpecialBall ball : specialBalls)
                handleBoxCollision(ball, left, right, top, bottom, damping);
            for (Ball ball : cloneBalls)
                handleBoxCollision(ball, left, right, top, bottom, damping);

        } else if (space >= 4 && space <= 10) {
            // --- POLYGON SPACES (Stages 151-500) ---
            int sides = 5;
            float scale = 1.15f;

            switch (space) {
                case 4:
                    sides = 5;
                    scale = 1.15f;
                    break; // Pentagon
                case 5:
                    sides = 6;
                    scale = 1.15f;
                    break; // Hexagon
                case 6:
                    sides = 3;
                    scale = 1.3f;
                    break; // Triangle
                case 7:
                    sides = 8;
                    scale = 1.1f;
                    break; // Octagon
                case 8:
                    sides = 4;
                    scale = 1.35f;
                    break; // Diamond
                case 9:
                    sides = 7;
                    scale = 1.15f;
                    break; // Heptagon
                case 10:
                    sides = 10;
                    scale = 1.05f;
                    break; // Decagon
            }

            float polyRadius = circleRadius * scale;
            if (whiteBall != null)
                handlePolygonCollision(whiteBall, sides, polyRadius, damping);
            for (Ball ball : coloredBalls)
                handlePolygonCollision(ball, sides, polyRadius, damping);
            for (Ball ball : blackBalls)
                handlePolygonCollision(ball, sides, polyRadius, damping);
            for (SpecialBall ball : specialBalls)
                handlePolygonCollision(ball, sides, polyRadius, damping);
            for (Ball ball : cloneBalls)
                handlePolygonCollision(ball, sides, polyRadius, damping);

        } else {
            // --- DEFAULT: CIRCLE (Space 1 & Others) ---
            if (whiteBall != null)
                handleCircleCollision(whiteBall, damping);
            for (Ball ball : coloredBalls)
                handleCircleCollision(ball, damping);
            for (Ball ball : blackBalls)
                handleCircleCollision(ball, damping);
            for (SpecialBall ball : specialBalls)
                handleCircleCollision(ball, damping);
            for (Ball ball : cloneBalls)
                handleCircleCollision(ball, damping);
        }

        // Drag sırasında top-top çarpışması yok
        if (isDragging) {
            return;
        }

        // --- BALL & BOSS INTERACTIONS (Restored Logic) ---
        ArrayList<Ball> allWhiteBalls = new ArrayList<>();
        allWhiteBalls.add(whiteBall);
        try {
            allWhiteBalls.addAll(cloneBalls);
        } catch (Exception e) {
        }

        for (Ball wBall : allWhiteBalls) {
            // Boss Interaction
            if (currentBoss != null) {
                float dx = wBall.x - currentBoss.x;
                float dy = wBall.y - currentBoss.y;
                float dist = (float) Math.sqrt(dx * dx + dy * dy);

                if (dist < wBall.radius + currentBoss.radius) {
                    boolean bossShielded = (currentBoss.state == 1 || currentBoss.dashing);
                    if (bossShielded) {
                        playSound(soundShield);
                        createImpactBurst(wBall.x, wBall.y, Color.CYAN);
                        floatingTexts.add(new FloatingText("BLOCKED", wBall.x, wBall.y - 50, Color.CYAN));

                        if (currentBoss.dashing && !currentBoss.charging && wBall == whiteBall) {
                            if (barrierActive || ghostModeActive) {
                                if (barrierActive) {
                                    barrierActive = false;
                                    floatingTexts.add(new FloatingText("BARRIER BROKEN!", whiteBall.x, whiteBall.y + 50,
                                            Color.YELLOW));
                                    playSound(soundCollision);
                                }
                            } else {
                                playerHp -= 40;
                                floatingTexts.add(new FloatingText("-40", whiteBall.x, whiteBall.y - 50, Color.RED));
                                createImpactBurst(whiteBall.x, whiteBall.y, Color.RED);
                                cameraShakeX = 30;
                                shakeEndTime = System.currentTimeMillis() + 400;
                                playSound(soundCollision);
                            }
                        }
                    } else {
                        long now = System.currentTimeMillis();
                        if (now < electricModeEndTime) {
                            int dmg = 40 + (upgradeHunter - 1) * 2;
                            currentBoss.hp -= dmg;
                            // Quest 18: Heavy Hitter (5000 damage)
                            if (questManager != null) {
                                questManager.incrementQuestProgress(18, 40);
                            }
                            floatingTexts.add(new FloatingText("-" + dmg, wBall.x, wBall.y - 50, Color.CYAN));
                            electricEffects.add(new ElectricEffect(wBall.x, wBall.y, currentBoss.x, currentBoss.y, 0));
                            createImpactBurst(wBall.x, wBall.y, Color.CYAN);
                            createParticles(wBall.x, wBall.y, Color.CYAN);
                            playSound(soundBlackExplosion);
                        } else {
                            int dmg = 25 + (upgradeHunter - 1) * 2;
                            currentBoss.hp -= dmg;
                            // Quest 18: Heavy Hitter
                            if (questManager != null) {
                                questManager.incrementQuestProgress(18, 25);
                            }
                            createParticles(wBall.x, wBall.y, currentBoss.color);
                            playSound(soundCollision);
                            floatingTexts.add(new FloatingText("-" + dmg, wBall.x, wBall.y - 50, Color.WHITE));
                        }
                    }

                    // Reflect Ball
                    float angle = (float) Math.atan2(dy, dx);
                    float speed = (float) Math.sqrt(wBall.vx * wBall.vx + wBall.vy * wBall.vy);
                    speed = Math.min(speed * 1.1f, 60f);
                    wBall.vx = (float) Math.cos(angle) * speed;
                    wBall.vy = (float) Math.sin(angle) * speed;
                    float overlap = (wBall.radius + currentBoss.radius) - dist + 2;
                    wBall.x += Math.cos(angle) * overlap;
                    wBall.y += Math.sin(angle) * overlap;

                    if (currentBoss.hp <= 0) {
                        // Boss defeated - track quests
                        String bossName = currentBoss.name;
                        if (questManager != null) {
                            // Quest 11-13, 19: Specific boss defeats
                            if (bossName.equals("VOID TITAN"))
                                questManager.incrementQuestProgress(11, 1);
                            else if (bossName.equals("SOLARION"))
                                questManager.incrementQuestProgress(12, 1);
                            else if (bossName.equals("GRAVITON"))
                                questManager.incrementQuestProgress(13, 1);
                            else if (bossName.equals("CHRONO-SHIFTER"))
                                questManager.incrementQuestProgress(19, 1);
                            // Quest 15: Boss Hunter (3 bosses)
                            // Quest 20: Ultimate Champion (10 bosses)
                            questManager.incrementQuestProgress(15, 1);
                            questManager.incrementQuestProgress(20, 1);

                            // Quest 45: Glass Cannon - Defeat boss with 50 HP or less
                            if (playerHp <= 50) {
                                questManager.incrementQuestProgress(45, 1);
                            }
                        }
                        currentBoss = null;
                        showBossDefeated = true;
                        bossDefeatedTime = System.currentTimeMillis();
                        for (int k = 0; k < 5; k++)
                            createImpactBurst(centerX + (random.nextFloat() - 0.5f) * 300,
                                    centerY + (random.nextFloat() - 0.5f) * 300, Color.RED);
                        playSound(soundBlackExplosion);
                    }
                }
            }

            // Colored Balls (Scoring)
            for (int i = coloredBalls.size() - 1; i >= 0; i--) {
                Ball ball = coloredBalls.get(i);
                if (checkBallCollision(wBall, ball)) {
                    score++;
                    timeLeft += 1000;
                    comboCounter++;

                    // Combo Logic
                    long currentTime = System.currentTimeMillis();
                    if (currentTime - lastHitTime < 2000) { // COMBO_TIMEOUT hardcoded as 2000 here or check constant
                        comboHits++;
                        if (comboHits > maxCombo)
                            maxCombo = comboHits;
                        // Quest 4: Combo Master (20 combos)
                        if (questManager != null && comboHits >= 3) {
                            questManager.incrementQuestProgress(4, 1);
                        }
                        // Quest 41: Sharpshooter - Hit 5 balls in one combo
                        if (questManager != null && comboHits >= 5) {
                            questManager.incrementQuestProgress(41, 1);
                        }
                        if (comboHits >= 3) {
                            floatingTexts.add(new FloatingText("COMBO x" + (comboHits), centerX,
                                    centerY - screenHeight * 0.15f, Color.rgb(255, 215, 0)));
                        }
                    } else {
                        comboHits = 1;
                    }
                    lastHitTime = currentTime;

                    // Quest 42: Ricochet Master - Track hits after wall bounce
                    // Simplified: Increment when hitting ball with high velocity (suggests bounce)
                    if (questManager != null && (Math.abs(wBall.vx) > 15 || Math.abs(wBall.vy) > 15)) {
                        questManager.incrementQuestProgress(42, 1);
                    }

                    createImpactBurst(ball.x, ball.y, ball.color);
                    coloredBalls.remove(i);
                    // Midas Chip Logic
                    if (upgradeMidas > 1) {
                        if (Math.random() < (upgradeMidas - 1) * 0.05f) {
                            coins++;
                            floatingTexts.add(new FloatingText("+1 💰", ball.x, ball.y, Color.YELLOW));
                        }
                    }
                    // Vampire Core Healing (Boss Fight Only)
                    if (upgradeVampire > 0 && currentBoss != null) {
                        int heal = 2 * upgradeVampire;
                        playerHp = Math.min(playerHp + heal, playerMaxHp);
                        floatingTexts.add(new FloatingText("+" + heal, wBall.x, wBall.y, Color.GREEN));
                    }
                    ballsHitThisShot++; // Quest 3: Count balls hit
                    // QUEST TRACKING - MAIN GAMEPLAY COLORED BALL DESTRUCTION!
                    if (questManager != null) {
                        android.util.Log.d("QUEST_DEBUG", "BALL HIT! Colored ball destroyed in main collision!");
                        questManager.incrementQuestProgress(1, 1); // Destroy 100 colored balls
                        questManager.incrementQuestProgress(5, 1); // Destroy 200 balls total
                    }
                    playSound(soundCollision);

                    if (coloredBalls.isEmpty() && pendingColoredBalls == 0 && currentBoss == null
                            && !showBossDefeated) {
                        levelCompleted = true;
                        showStageCleared = true;
                        stageClearedTime = System.currentTimeMillis();
                        for (Ball b : blackBalls)
                            createImpactBurst(b.x, b.y, Color.BLACK);
                        blackBalls.clear();
                    }

                    // Physics bounce
                    float dx = wBall.x - ball.x;
                    float dy = wBall.y - ball.y;
                    float angle = (float) Math.atan2(dy, dx);
                    float speed = (float) Math.sqrt(wBall.vx * wBall.vx + wBall.vy * wBall.vy);
                    wBall.vx = (float) Math.cos(angle) * speed * 1.05f;
                    wBall.vy = (float) Math.sin(angle) * speed * 1.05f;
                }
            }

            // Black Balls (Damage)
            for (int i = blackBalls.size() - 1; i >= 0; i--) {
                Ball ball = blackBalls.get(i);
                float dx = wBall.x - ball.x;
                float dy = wBall.y - ball.y;
                if (dx * dx + dy * dy < (wBall.radius + ball.radius) * (wBall.radius + ball.radius)) {
                    // Check immunity or teleport state
                    if (System.currentTimeMillis() < immuneEndTime || isTeleporting)
                        continue;
                    if (wBall != whiteBall)
                        continue; // Clones are immune

                    if (barrierActive || ghostModeActive) {
                        long currentTime = System.currentTimeMillis();
                        if (currentTime - lastShieldSoundTime > 500) {
                            playSound(soundShield);
                            lastShieldSoundTime = currentTime;
                        }
                    } else if (activePassivePower.equals("teleport")) {
                        // PASSIVE TELEPORT SAVE triggers
                        activePassivePower = "none"; // Consume passive

                        // Start Teleport Sequence (Disappear)
                        isTeleporting = true;
                        teleportEndTime = System.currentTimeMillis() + 500; // 500ms delay

                        // Vanish Particles at old location
                        createImpactBurst(wBall.x, wBall.y, Color.GREEN);
                        for (int p = 0; p < 30; p++) {
                            float pAngle = random.nextFloat() * (float) (2 * Math.PI);
                            float pSpeed = random.nextFloat() * 10 + 5;
                            particles.add(new Particle(wBall.x, wBall.y, pAngle, pSpeed, Color.GREEN));
                        }

                        // Stop movement immediately
                        wBall.vx = 0;
                        wBall.vy = 0;
                        // Note: Reappear logic is in update()
                    } else if (activePassivePower.equals("split_save")) {
                        // PASSIVE SPLIT SAVE
                        activePassivePower = "none";

                        createImpactBurst(wBall.x, wBall.y, Color.MAGENTA);
                        playSound(soundPower);
                        floatingTexts.add(new FloatingText("SPLIT SAVE!", centerX, centerY, Color.MAGENTA));

                        // Spawn 3 Mini Clones
                        for (int k = 0; k < 3; k++) {
                            // Spawn smaller clones with 4s lifetime
                            Ball clone = new Ball(wBall.x, wBall.y, wBall.radius * 0.6f, Color.MAGENTA, 4000);
                            float angle = (float) (k * (2 * Math.PI / 3));
                            clone.vx = (float) Math.cos(angle) * 15;
                            clone.vy = (float) Math.sin(angle) * 15;
                            cloneBalls.add(clone);
                        }

                        // MODIFIED: Do NOT hide/teleport the main ball.
                        // Instead, make it immune and let it continue moving.
                        immuneEndTime = System.currentTimeMillis() + 4000; // 4s Immunity

                        // We do NOT reset vx/vy so it continues its trajectory.
                        // We do NOT move x/y to -9999.

                        isSplitSaveActive = true;
                        cloneExpirationTime = System.currentTimeMillis() + 4000; // 4s

                    } else if (activePassivePower.equals("vortex")) {
                        // PASSIVE VORTEX SAVE vs BLACK BALL
                        activePassivePower = "none";

                        // Create vortex
                        activeVortex = new Vortex(wBall.x, wBall.y);

                        // Visual feedback
                        createImpactBurst(wBall.x, wBall.y, Color.CYAN);
                        for (int pIdx = 0; pIdx < 30; pIdx++) {
                            float pAngle = random.nextFloat() * (float) (2 * Math.PI);
                            float pSpeed = random.nextFloat() * 10 + 5;
                            particles.add(new Particle(wBall.x, wBall.y, pAngle, pSpeed, Color.CYAN));
                        }
                        playSound(soundPower);
                        floatingTexts.add(new FloatingText("VORTEX!", wBall.x, wBall.y - 50, Color.CYAN));

                        // Give immunity during vortex
                        immuneEndTime = System.currentTimeMillis() + 2500;

                    } else {
                        lives--;
                        comboCounter = 0;
                        createParticles(ball.x, ball.y, Color.BLACK);
                        shakeEndTime = System.currentTimeMillis() + 500;
                        if (lives <= 0) {
                            gameOver = true;
                            saveProgress();
                            playSound(soundGameOver);
                            updateUIPanels();
                        } else {
                            wBall.x = centerX;
                            wBall.y = centerY;
                            wBall.vx = 0;
                            wBall.vy = 0;
                            immuneEndTime = System.currentTimeMillis() + 2000;
                        }
                    }
                }
            }

            // Special Balls
            for (int i = specialBalls.size() - 1; i >= 0; i--) {
                SpecialBall ball = specialBalls.get(i);
                if (checkBallCollision(wBall, ball)) {
                    activateSpecialPower(ball.type, wBall);
                    createParticles(ball.x, ball.y, ball.getColor());
                    specialBalls.remove(i);
                    // Quest 6: Special Hunter (5 special balls)
                    // Quest 5: Destroyer II (200 balls total)
                    if (questManager != null) {
                        questManager.incrementQuestProgress(6, 1);
                        questManager.incrementQuestProgress(5, 1);
                    }
                }
            }

        }

    }

    private void createFlame(float x, float y) {
        // Spawn 1-2 flame particles each frame for continuous effect
        for (int i = 0; i < 2; i++) {
            particles.add(new Particle(x, y, 0, 0, Color.YELLOW, ParticleType.FLAME));
        }
    }

    private void createParticles(float x, float y, int color) {
        for (int i = 0; i < 10; i++) { // Reduced from 15 to 10 for performance
            float angle = random.nextFloat() * (float) (2 * Math.PI);
            float speed = random.nextFloat() * 5 + 2;
            particles.add(new Particle(x, y, angle, speed, color));
        }
    }

    private void createImpactBurst(float x, float y, int color) {
        // Quest 10: Demolition (50 explosions)
        if (questManager != null) {
            questManager.incrementQuestProgress(10, 1);
        }
        // Always add some base particles
        // Nova Upgrade: Larger explosion radius (speed) and more particles
        float novaSpeedMult = 1.0f + (upgradeNova > 1 ? (upgradeNova - 1) * 0.25f : 0);
        int particleCount = 6 + (upgradeNova > 1 ? (upgradeNova - 1) * 2 : 0);

        for (int i = 0; i < particleCount; i++) {
            float angle = random.nextFloat() * (float) (2 * Math.PI);
            float speed = (random.nextFloat() * 6 + 2) * novaSpeedMult;

            // BLACK BALL FIX: Use gray-white particles for better visibility
            int particleColor = color;
            if (color == Color.BLACK) {
                int shade = 150 + random.nextInt(106); // 150-255 range (gray to white)
                particleColor = Color.rgb(shade, shade, shade);
            }

            particles.add(new Particle(x, y, angle, speed, particleColor, ParticleType.CIRCLE));
        }

        switch (selectedImpact) {
            case "pixel":
                for (int i = 0; i < 15; i++) {
                    float angle = random.nextFloat() * (float) (2 * Math.PI);
                    float speed = random.nextFloat() * 8 + 3;
                    particles.add(new Particle(x, y, angle, speed, Color.GREEN, ParticleType.PIXEL));
                }
                break;
            case "vortex":
                for (int i = 0; i < 20; i++) {
                    float angle = (float) (random.nextFloat() * Math.PI * 2);
                    float speed = random.nextFloat() * 5 + 2;
                    // Spiraling logic handled in update? particle currently just moves linear.
                    // We'll simulate vortex look by sheer number and color
                    int pColor = Color.rgb(100 + random.nextInt(155), 0, 255);
                    particles.add(new Particle(x, y, angle, speed, pColor, ParticleType.CIRCLE));
                }
                break;
            case "sparks":
                for (int i = 0; i < 20; i++) {
                    float angle = random.nextFloat() * (float) (2 * Math.PI);
                    float speed = random.nextFloat() * 15 + 10; // Fast
                    particles.add(new Particle(x, y, angle, speed, Color.YELLOW, ParticleType.STAR)); // Reusing Star
                    // for spark look
                }
                break;
            case "hearts":
                for (int i = 0; i < 8; i++) {
                    float angle = random.nextFloat() * (float) (2 * Math.PI);
                    float speed = random.nextFloat() * 4 + 2;
                    particles.add(new Particle(x, y, angle, speed, Color.RED, ParticleType.HEART));
                }
                break;
            case "skull":
                for (int i = 0; i < 5; i++) {
                    float angle = random.nextFloat() * (float) (2 * Math.PI);
                    float speed = random.nextFloat() * 3 + 1;
                    particles.add(new Particle(x, y, angle, speed, Color.LTGRAY, ParticleType.SKULL));
                }
                break;
            case "music":
                int[] noteColors = { Color.CYAN, Color.MAGENTA, Color.YELLOW, Color.WHITE };
                for (int i = 0; i < 8; i++) {
                    float angle = random.nextFloat() * (float) (2 * Math.PI);
                    float speed = random.nextFloat() * 5 + 3;
                    particles.add(new Particle(x, y, angle, speed, noteColors[i % 4], ParticleType.NOTE));
                }
                break;
            case "lightning":
                for (int i = 0; i < 6; i++) {
                    float angle = random.nextFloat() * (float) (2 * Math.PI);
                    float length = random.nextFloat() * 40 + 30;
                    impactArcs.add(new ImpactArc(x, y, angle, length, Color.CYAN));
                }
                break;
            case "confetti":
                for (int i = 0; i < 15; i++) {
                    float angle = random.nextFloat() * (float) (2 * Math.PI);
                    float speed = random.nextFloat() * 5 + 3;
                    int confettiColor = Color.rgb(random.nextInt(256), random.nextInt(256), random.nextInt(256));
                    particles.add(new Particle(x, y, angle, speed, confettiColor, ParticleType.CONFETTI));
                }
                break;
            case "ghost":
                for (int i = 0; i < 5; i++) {
                    float angle = random.nextFloat() * (float) (2 * Math.PI);
                    float speed = random.nextFloat() * 2 + 1;
                    particles.add(new Particle(x, y, angle, speed, Color.WHITE, ParticleType.GHOST));
                }
                break;
            // "shockwave" uses default circles above
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

            // Arka plan - 10 Space progression (every 10 levels = 1 space)
            int currentSpace = ((level - 1) / 10) + 1;

            // DRAW CACHED BOSS BACKGROUND (Performance Optimization)
            if (currentBoss != null && cachedBossBackground != null) {
                canvas.drawBitmap(cachedBossBackground, 0, 0, null);
                // Also draw stars on top for depth if needed, but keep it simple for perf
            } else if (level > 100 && level <= 200) {
                // SPACE 3 & 4 (Solarion & Nebulon Style): Black background
                canvas.drawColor(Color.BLACK);
            } else if (currentBoss != null && (currentBoss.name.equals("NEBULON") || currentBoss.name.equals("GRAVITON")
                    || currentBoss.name.equals("MECHA-CORE") || currentBoss.name.equals("CRYO-STASIS")
                    || currentBoss.name.equals("GEO-BREAKER") || currentBoss.name.equals("BIO-HAZARD")
                    || currentBoss.name.equals("CHRONO-SHIFTER"))) {
                // Other bosses: Black background with themed particles
                canvas.drawColor(Color.BLACK);
            } else if (currentBoss != null && currentBoss.name.equals("LUNAR CONSTRUCT")) {
                // Force Moon Background for Lunar Construct
                canvas.drawColor(Color.BLACK);
                drawMoon(canvas);
                // Comets (already updated in update() loop)
                for (Comet c : comets) {
                    c.draw(canvas, paint);
                }
            } else {
                // Normal Level Backgrounds
                switch (currentSpace) {
                    case 1:
                    case 5:
                    case 8:
                        // Space 1, 5, 8: Pure black background
                        canvas.drawColor(Color.BLACK);
                        for (Star star : stars) {
                            star.draw(canvas, paint);
                        }
                        break;

                    case 2:
                    case 6:
                    case 9:
                        // Space 2, 6, 9: Pure black background
                        canvas.drawColor(Color.BLACK);
                        drawMoon(canvas);
                        for (Comet c : comets) {
                            c.draw(canvas, paint); // Already updated in update()
                        }
                        break;

                    case 3:
                    case 4:
                    case 10:
                        // Space 3: Black + Rising Flames (Restored)
                        // Space 4, 10: Keep dark (or shared logic)
                        if (currentSpace == 3) {
                            canvas.drawColor(Color.BLACK);
                            // NO COMETS for Space 3
                        } else {
                            canvas.drawColor(Color.BLACK);
                            for (Comet c : comets) {
                                c.draw(canvas, paint);
                            }
                        }
                        break;

                    case 7:
                        // Space 7: Static Aurora (no animation)
                        drawAuroraNebula(canvas);
                        for (Star star : stars) {
                            star.draw(canvas, paint);
                        }
                        break;

                    default:
                        // Fallback: Pure black
                        canvas.drawColor(Color.BLACK);
                        for (Star star : stars) {
                            star.draw(canvas, paint);
                        }
                        break;
                }
            } // End of else block for normal backgrounds

            // Draw Magma
            for (MagmaPatch p : magmaPatches)
                p.draw(canvas);

            // Draw Vortex
            if (activeVortex != null) {
                activeVortex.draw(canvas, paint);
            }

            // Draw Boss Projectiles
            for (Ball p : bossProjectiles) {
                drawBall(canvas, p);
            }

            // Draw Boss
            if (currentBoss != null) {
                currentBoss.draw(canvas);
            }

            // Draw Player HP Bar (if Boss active)
            if (currentBoss != null) {
                float barW = screenWidth * 0.5f;
                float barH = 20;
                float barX = (screenWidth - barW) / 2;
                float barY = screenHeight - 250; // Slightly above bottom

                // Background
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(Color.DKGRAY);
                canvas.drawRect(barX, barY, barX + barW, barY + barH, paint);

                // HP
                paint.setColor(Color.GREEN);
                float ratio = Math.max(0, playerHp / playerMaxHp);
                if (ratio < 0.3f)
                    paint.setColor(Color.RED);
                canvas.drawRect(barX, barY, barX + barW * ratio, barY + barH, paint);

                // Border
                paint.setStyle(Paint.Style.STROKE);
                paint.setColor(Color.WHITE);
                paint.setStrokeWidth(2);
                canvas.drawRect(barX, barY, barX + barW, barY + barH, paint);

                paint.setStyle(Paint.Style.FILL);
                paint.setColor(Color.WHITE);
                paint.setTextSize(30);
                paint.setTextAlign(Paint.Align.CENTER);
                canvas.drawText("PLAYER HP", centerX, barY - 10, paint);
            }

            // Draw Game Borders
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(10);
            paint.setColor(Color.WHITE);
            paint.setShadowLayer(20, 0, 0, Color.CYAN);

            // Reuse currentSpace from line 2650 (performance: avoid duplicate calculation)

            if (currentSpace == 2) {
                // SPACE 2: SQUARE (Full Circle Radius)
                float boundary = circleRadius;
                // Modified top (0.85f) to avoid overlapping with Top UI (Home/Passive)
                canvas.drawRect(centerX - boundary, centerY - boundary * 0.85f, centerX + boundary, centerY + boundary,
                        paint);
            } else if (currentSpace == 3) {
                // SPACE 3: RECTANGLE
                float halfW = circleRadius;
                float halfH = circleRadius * 0.7f;
                canvas.drawRect(centerX - halfW, centerY - halfH, centerX + halfW, centerY + halfH, paint);
            } else if (currentSpace >= 4 && currentSpace <= 10) {
                // --- POLYGON SPACES ---
                int sides = 5;
                float scale = 1.15f;

                switch (currentSpace) {
                    case 4:
                        sides = 5;
                        scale = 1.15f;
                        break;
                    case 5:
                        sides = 6;
                        scale = 1.15f;
                        break;
                    case 6:
                        sides = 3;
                        scale = 1.3f; // Reduced from 1.6f to fit screen
                        break;
                    case 7:
                        sides = 8;
                        scale = 1.1f;
                        break;
                    case 8:
                        sides = 4;
                        scale = 1.35f;
                        break;
                    case 9:
                        sides = 7;
                        scale = 1.15f;
                        break;
                    case 10:
                        sides = 10;
                        scale = 1.05f;
                        break;
                }

                // PERFORMANCE: Use cached path if parameters haven't changed
                if (cachedPolygonPath == null || cachedPolygonSides != sides || cachedPolygonScale != scale) {
                    cachedPolygonPath = new Path();
                    for (int i = 0; i < sides; i++) {
                        float angle = (float) (i * 2 * Math.PI / sides - Math.PI / 2);
                        float x = centerX + (float) Math.cos(angle) * circleRadius * scale;
                        float y = centerY + (float) Math.sin(angle) * circleRadius * scale;
                        if (i == 0)
                            cachedPolygonPath.moveTo(x, y);
                        else
                            cachedPolygonPath.lineTo(x, y);
                    }
                    cachedPolygonPath.close();
                    cachedPolygonSides = sides;
                    cachedPolygonScale = scale;
                }
                canvas.drawPath(cachedPolygonPath, paint);
            } else {
                // DEFAULT: CIRCLE
                canvas.drawCircle(centerX, centerY, circleRadius, paint);
            }

            paint.setStyle(Paint.Style.FILL); // Reset
            paint.clearShadowLayer();

            // Blast wave
            if (blastWave != null) {
                blastWave.draw(canvas, paint);
            }
            // ... (rest of draw)

            // Electric effects
            for (

                    int i = 0; i < electricEffects.size(); i++) {
                ElectricEffect effect = electricEffects.get(i);
                effect.draw(canvas, paint);
            }

            // Parçacıklar
            for (int i = 0; i < particles.size(); i++) {
                Particle p = particles.get(i);
                p.draw(canvas, paint);
            }

            for (ImpactArc arc : impactArcs) {
                arc.draw(canvas, paint);
            }

            // Draw Boss
            if (currentBoss != null) {
                currentBoss.draw(canvas);
            }

            // Toplar
            for (int i = 0; i < coloredBalls.size(); i++) {
                Ball ball = coloredBalls.get(i);
                drawBall(canvas, ball);
            }

            for (int i = 0; i < blackBalls.size(); i++) {
                Ball ball = blackBalls.get(i);
                drawBall(canvas, ball);
            }

            for (int i = 0; i < specialBalls.size(); i++) {
                SpecialBall ball = specialBalls.get(i);
                drawSpecialBall(canvas, ball);
            }

            for (int i = 0; i < cloneBalls.size(); i++) {
                Ball ball = cloneBalls.get(i);
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
            for (int i = 0; i < missiles.size(); i++) {
                GuidedMissile missile = missiles.get(i);
                missile.draw(canvas, paint);
            }

            // UFO Draw
            if (activeUfo != null) {
                activeUfo.draw(canvas);
            }

            // Trail (Only for player ball)
            if (!selectedTrail.equals("none") && whiteBall.trail.size() > 0 && !showPlayerDefeated) {
                drawCometTrail(canvas, whiteBall);
            }

            // Beyaz top (Sadece offline modda çiziyoruz, online modda hostBall/guestBall
            // kullanılıyor)
            if (!showPlayerDefeated) {
                drawBall(canvas, whiteBall);

                // ICE VISUAL: Draw ice ring if frozen
                if (isFrozen) {
                    paint.setStyle(Paint.Style.STROKE);
                    paint.setStrokeWidth(6);
                    paint.setColor(Color.rgb(150, 200, 255));
                    paint.setAlpha(200);

                    // Pulsing ice rings
                    long iceTime = System.currentTimeMillis() % 1000;
                    float pulse = 1.0f + 0.2f * (float) Math.sin(iceTime * 0.01f);

                    canvas.drawCircle(whiteBall.x, whiteBall.y, whiteBall.radius * 2.0f * pulse, paint);
                    canvas.drawCircle(whiteBall.x, whiteBall.y, whiteBall.radius * 2.5f * pulse, paint);

                    // Ice crystals
                    for (int i = 0; i < 8; i++) {
                        float angle = i * (float) (Math.PI / 4);
                        float x1 = whiteBall.x + (float) Math.cos(angle) * whiteBall.radius * 2;
                        float y1 = whiteBall.y + (float) Math.sin(angle) * whiteBall.radius * 2;
                        float x2 = whiteBall.x + (float) Math.cos(angle) * whiteBall.radius * 3;
                        float y2 = whiteBall.y + (float) Math.sin(angle) * whiteBall.radius * 3;
                        canvas.drawLine(x1, y1, x2, y2, paint);
                    }

                    paint.setAlpha(255);
                }
            }

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
                float dx, dy;
                boolean isBottomDrag = dragStartY >= screenHeight * 0.6f;

                if (isBottomDrag) {
                    // Sling shot: Vector is Start - Current (Pull back)
                    dx = dragStartX - currentTouchX;
                    dy = dragStartY - currentTouchY;
                } else {
                    // Classic Drag: Vector is Center - DraggedPos (Spring back)
                    dx = dragStartX - currentDraggedBall.x;
                    dy = dragStartY - currentDraggedBall.y;
                }

                float distance = Math.min((float) Math.sqrt(dx * dx + dy * dy), MAX_DRAG_DISTANCE);
                float ratio = distance / MAX_DRAG_DISTANCE;

                // Launch Angle (Direction of shoot)
                float launchAngle = (float) Math.atan2(dy, dx);

                // Eski çizgi (drag line) yerine nişan çizgisi (trajectory)
                if (distance > 10) {
                    // Multi-Ball Sight: 3 Lines if active
                    drawTrajectory(canvas, launchAngle, ratio, currentDraggedBall);
                    if (multiBallActive) {
                        drawTrajectory(canvas, launchAngle + 0.3f, ratio, currentDraggedBall);
                        drawTrajectory(canvas, launchAngle - 0.3f, ratio, currentDraggedBall);
                    }
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
                canvas.drawArc(currentDraggedBall.x - arcRadius, currentDraggedBall.y - arcRadius,
                        currentDraggedBall.x + arcRadius, currentDraggedBall.y + arcRadius, -90, sweepAngle, false,
                        paint);
                paint.clearShadowLayer();
            }

            // Electric Effects (NOW VISIBLE)
            for (ElectricEffect effect : electricEffects) {
                effect.draw(canvas, paint);
            }

            // Combo text göster (Floating texts) - Use index to avoid
            // ConcurrentModificationException
            for (int i = 0; i < floatingTexts.size(); i++) {
                if (i < floatingTexts.size()) { // Safety check
                    floatingTexts.get(i).draw(canvas, paint);
                }
            }

            // STAGE CLEARED animasyonu
            if (showStageCleared) {
                paint.setStyle(Paint.Style.FILL);
                paint.setTextSize(screenWidth * 0.08f); // Reduced Text Size

                // At this point, stage and level have NOT been incremented yet
                // We're showing what was just completed
                int completedStage = stage; // Current stage is the one that was just completed

                if (stage == 5) {
                    // We just completed stage 5, will unlock next level
                    int nextLevelToUnlock = level + 1;

                    // If game is completed (or about to be), don't show unlock text for next level
                    if (nextLevelToUnlock > 100) {
                        canvas.drawText("FINAL LEVEL CLEARED!", centerX, centerY, paint);
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
                    // Intermediate stage (1-4) cleared, show stage number
                    canvas.drawText("STAGE " + completedStage + " CLEARED!", centerX, centerY, paint);
                }
                paint.clearShadowLayer();

                // Alt yazı
                paint.setTextSize(screenWidth * 0.045f);
                paint.setColor(Color.WHITE);
                canvas.drawText("Stage " + completedStage + " Complete", centerX, centerY + screenHeight * 0.08f,
                        paint);
            }

            // UI
            drawUI(canvas);

            if (gameCompleted) {
                drawGameCompleted(canvas);
            }

            // --- DRAW DEFEATED TEXT (Restored) ---
            if (showPlayerDefeated && !gameOver) {
                paint.setColor(Color.RED);
                paint.setTextSize(screenWidth * 0.1f);
                paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
                paint.setTextAlign(Paint.Align.CENTER);
                paint.setShadowLayer(50, 0, 0, Color.BLACK);

                // Only show "DEFEATED BY BOSS" - simpler and clearer
                String text = "DEFEATED BY BOSS";
                canvas.drawText(text, centerX, centerY, paint);

                paint.clearShadowLayer();
                paint.setTypeface(Typeface.DEFAULT);
            }

            // --- INFO SCREEN OVERLAY ---
            if (System.currentTimeMillis() < levelInfoEndTime) {
                // Dim Text
                paint.setColor(Color.argb(100, 0, 0, 0));
                paint.setStyle(Paint.Style.FILL);
                canvas.drawRect(0, 0, screenWidth, screenHeight, paint);

                // Panel Box
                float panelW = screenWidth * 0.8f;
                float panelH = screenHeight * 0.3f;
                float panelL = centerX - panelW / 2;
                float panelT = centerY - panelH / 2;
                float panelR = centerX + panelW / 2;
                float panelB = centerY + panelH / 2;

                RectF panelRect = new RectF(panelL, panelT, panelR, panelB);

                // Background
                paint.setColor(Color.argb(230, 20, 20, 30)); // Dark Blue-Grey
                paint.setStyle(Paint.Style.FILL);
                canvas.drawRoundRect(panelRect, 40, 40, paint);

                // Border
                paint.setColor(Color.CYAN);
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(6);
                canvas.drawRoundRect(panelRect, 40, 40, paint);

                // Text
                paint.setColor(Color.WHITE);
                paint.setStyle(Paint.Style.FILL);
                paint.setTextSize(screenWidth * 0.035f); // Smaller text
                paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
                paint.setTextAlign(Paint.Align.CENTER);

                String[] lines = levelInfoText.split("\n");
                // Calculate total text height to center vertically in box
                float lineHeight = screenHeight * 0.06f;
                float totalTextH = lines.length * lineHeight;
                float textStartY = centerY - (totalTextH / 2) + (lineHeight / 2);

                for (int i = 0; i < lines.length; i++) {
                    // Title (first line) is larger? Maybe not, keep uniform for now.
                    if (i == 0)
                        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD_ITALIC));
                    else
                        paint.setTypeface(Typeface.DEFAULT);

                    canvas.drawText(lines[i], centerX, textStartY + (i * lineHeight), paint);
                }

                paint.setTypeface(Typeface.DEFAULT);
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
        // Eğer top yoksa veya yarıçapı 0 ise (veya çok küçükse) çizimi iptal et.
        // Bu satır çökme (Crash) sorununu %100 çözecektir.
        if (ball == null || ball.radius <= 0.1f || Float.isNaN(ball.radius)) {
            return;
        }

        // Blinking Effect for Immunity (Split Save / Revive)
        if (ball == whiteBall && System.currentTimeMillis() < immuneEndTime) {
            // Blink every 100ms
            if ((System.currentTimeMillis() / 100) % 2 == 0) {
                return; // Skip drawing this frame
            }
        }

        if (ball instanceof MoonRock) {
            drawMoonRock(canvas, (MoonRock) ball);
            return;
        }

        if (ball instanceof VoidProjectile) {
            drawVoidProjectile(canvas, (VoidProjectile) ball);
            return;
        }

        // --- Unique Projectile Dispatch ---
        if (ball instanceof PlasmaBullet) {
            drawPlasmaBullet(canvas, (PlasmaBullet) ball);
            return;
        }
        if (ball instanceof SolarBolt) {
            drawSolarBolt(canvas, (SolarBolt) ball);
            return;
        }
        if (ball instanceof MistShard) {
            drawMistShard(canvas, (MistShard) ball);
            return;
        }
        if (ball instanceof GravityOrb) {
            drawGravityOrb(canvas, (GravityOrb) ball);
            return;
        }
        if (ball instanceof IceSpike) {
            drawIceSpike(canvas, (IceSpike) ball);
            return;
        }
        if (ball instanceof GeoRock) {
            drawGeoRock(canvas, (GeoRock) ball);
            return;
        }
        if (ball instanceof AcidBlob) {
            drawAcidBlob(canvas, (AcidBlob) ball);
            return;
        }
        if (ball instanceof ClockGear) {
            drawClockGear(canvas, (ClockGear) ball);
            return;
        }

        if (ball instanceof MeteorProjectile) {
            drawMeteor(canvas, (MeteorProjectile) ball);
            return;
        }

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
                    // --- TURKEY (Super Lig) ---
                case "team_galatasaray":
                case "team_fenerbahce":
                case "team_besiktas":
                case "team_trabzon":
                case "team_basaksehir":
                case "team_adana":
                case "team_samsun":
                case "team_goztepe":
                case "team_sivas":
                case "team_konya":
                    // --- ENGLAND ---
                case "team_man_city":
                case "team_arsenal":
                case "team_liverpool":
                case "team_aston_villa":
                case "team_tottenham":
                case "team_chelsea":
                case "team_newcastle":
                case "team_man_utd":
                case "team_westham":
                case "team_brighton":
                    // --- SPAIN ---
                case "team_real_madrid":
                case "team_girona":
                case "team_barcelona":
                case "team_atletico":
                case "team_bilbao":
                case "team_sociedad":
                case "team_betis":
                case "team_valencia":
                case "team_villarreal":
                case "team_sevilla":
                    // --- GERMANY ---
                case "team_leverkusen":
                case "team_bayern":
                case "team_stuttgart":
                case "team_leipzig":
                case "team_dortmund":
                case "team_frankfurt":
                case "team_hoffenheim":
                case "team_bremen":
                case "team_freiburg":
                case "team_augsburg":
                    // --- FRANCE ---
                case "team_psg":
                case "team_monaco":
                case "team_brest":
                case "team_lille":
                case "team_nice":
                case "team_lens":
                case "team_marseille":
                case "team_lyon":
                case "team_rennes":
                case "team_reims":
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

        RadialGradient gradient = new RadialGradient(ball.x - ball.radius / 3, ball.y - ball.radius / 3, ball.radius,
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
        canvas.drawArc(ball.x - ball.radius * 0.7f, ball.y - ball.radius * 0.7f, ball.x + ball.radius * 0.7f,
                ball.y + ball.radius * 0.7f, 45, 90, false, paint);
        canvas.drawArc(ball.x - ball.radius * 0.7f, ball.y - ball.radius * 0.7f, ball.x + ball.radius * 0.7f,
                ball.y + ball.radius * 0.7f, 225, 90, false, paint);

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
                new int[] { Color.WHITE, Color.rgb(200, 240, 255), Color.rgb(100, 180, 255) }, null,
                Shader.TileMode.CLAMP);
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

    private void drawMoonRock(Canvas canvas, MoonRock rock) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.rgb(100, 100, 100)); // Fixed Dark Gray Base
        paint.setShadowLayer(10, 0, 0, Color.BLACK);
        canvas.drawCircle(rock.x, rock.y, rock.radius, paint);
        paint.clearShadowLayer();

        // Craters (High Contrast Black)
        paint.setColor(Color.BLACK);
        paint.setAlpha(180);
        float r = rock.radius;
        // Crater 1
        canvas.drawCircle(rock.x - r * 0.3f, rock.y - r * 0.3f, r * 0.3f, paint);
        // Crater 2
        canvas.drawCircle(rock.x + r * 0.4f, rock.y + r * 0.2f, r * 0.2f, paint);
        // Crater 3
        canvas.drawCircle(rock.x, rock.y + r * 0.5f, r * 0.15f, paint);
        paint.setAlpha(255);

        // Highlight
        paint.setColor(Color.WHITE);
        paint.setAlpha(50);
        canvas.drawCircle(rock.x - r * 0.3f, rock.y - r * 0.3f, r * 0.2f, paint);
        paint.setAlpha(255);
    }

    private void drawMeteor(Canvas canvas, MeteorProjectile m) {
        // Trail
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(m.radius); // Trail width = ball size
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setColor(m.color);
        paint.setAlpha(100); // Semi-transparent tail
        // Draw line opposite to velocity
        canvas.drawLine(m.x, m.y, m.x - m.vx * m.trailLengthFactor, m.y - m.vy * m.trailLengthFactor, paint);

        // Head
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(m.color);
        paint.setAlpha(255);
        canvas.drawCircle(m.x, m.y, m.radius, paint);

        // Core (Hot white center)
        paint.setColor(Color.WHITE);
        paint.setAlpha(200);
        canvas.drawCircle(m.x, m.y, m.radius * 0.4f, paint);
        paint.setAlpha(255);
    }

    // --- Unique Projectile Draw Methods ---

    private void drawPlasmaBullet(Canvas canvas, PlasmaBullet b) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(b.color);
        paint.setShadowLayer(25, 0, 0, b.color);
        float speed = (float) Math.sqrt(b.vx * b.vx + b.vy * b.vy);
        if (speed == 0)
            speed = 1;
        float angle = (float) Math.atan2(b.vy, b.vx);
        canvas.save();
        canvas.rotate((float) Math.toDegrees(angle), b.x, b.y);

        // Elongated core with glow
        RectF rect = new RectF(b.x - b.radius * 2, b.y - b.radius * 0.6f, b.x + b.radius * 2, b.y + b.radius * 0.6f);
        canvas.drawRoundRect(rect, 10, 10, paint);

        // Inner white hot core
        paint.setColor(Color.WHITE);
        paint.clearShadowLayer();
        RectF core = new RectF(b.x - b.radius * 1.5f, b.y - b.radius * 0.3f, b.x + b.radius * 1.5f,
                b.y + b.radius * 0.3f);
        canvas.drawRoundRect(core, 5, 5, paint);

        canvas.restore();
        paint.setColor(b.color);
    }

    private void drawSolarBolt(Canvas canvas, SolarBolt b) {
        // Core
        paint.setColor(Color.rgb(255, 69, 0)); // Red-Orange
        paint.setShadowLayer(30, 0, 0, Color.YELLOW);
        float pulse = (float) (1.0 + Math.sin(System.currentTimeMillis() * 0.02) * 0.2);
        canvas.drawCircle(b.x, b.y, b.radius * pulse, paint);

        // Inner
        paint.setColor(Color.YELLOW);
        canvas.drawCircle(b.x, b.y, b.radius * 0.6f, paint);
        paint.clearShadowLayer();

        // Trailing Flares
        paint.setColor(Color.rgb(255, 140, 0));
        paint.setAlpha(150);
        for (int i = 1; i <= 3; i++) {
            float tx = b.x - b.vx * i * 0.2f;
            float ty = b.y - b.vy * i * 0.2f;
            canvas.drawCircle(tx, ty, b.radius * (1.0f - i * 0.25f), paint);
        }
        paint.setAlpha(255);
    }

    private void drawMistShard(Canvas canvas, MistShard b) {
        // Wispy Effect
        paint.setColor(b.color);
        paint.setAlpha(100);

        // 3 overlapping circles
        canvas.drawCircle(b.x, b.y, b.radius, paint);
        canvas.drawCircle(b.x - b.vx * 0.5f, b.y - b.vy * 0.5f, b.radius * 0.8f, paint);
        canvas.drawCircle(b.x - b.vx, b.y - b.vy, b.radius * 0.6f, paint);

        paint.setAlpha(255);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2);
        paint.setColor(Color.WHITE);
        canvas.drawCircle(b.x, b.y, b.radius * 0.5f, paint);
        paint.setStyle(Paint.Style.FILL);
    }

    private void drawGravityOrb(Canvas canvas, GravityOrb b) {
        paint.setColor(Color.BLACK);
        paint.setShadowLayer(25, 0, 0, Color.rgb(138, 43, 226));
        canvas.drawCircle(b.x, b.y, b.radius, paint);
        paint.clearShadowLayer();

        // Swirling Ring
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(Color.MAGENTA);
        paint.setStrokeWidth(4);
        float rot = (System.currentTimeMillis() * 0.5f) % 360;
        RectF oval = new RectF(b.x - b.radius * 1.5f, b.y - b.radius * 1.5f, b.x + b.radius * 1.5f,
                b.y + b.radius * 1.5f);
        canvas.drawArc(oval, rot, 100, false, paint);
        canvas.drawArc(oval, rot + 180, 100, false, paint);
        paint.setStyle(Paint.Style.FILL);
    }

    private void drawIceSpike(Canvas canvas, IceSpike b) {
        paint.setColor(Color.CYAN);
        paint.setShadowLayer(20, 0, 0, Color.WHITE);

        float angle = (float) Math.atan2(b.vy, b.vx);
        canvas.save();
        canvas.rotate((float) Math.toDegrees(angle) + 90, b.x, b.y);

        Path path = new Path();
        path.moveTo(b.x, b.y - b.radius * 2); // Sharp Tip
        path.lineTo(b.x + b.radius * 0.8f, b.y);
        path.lineTo(b.x, b.y + b.radius * 0.8f);
        path.lineTo(b.x - b.radius * 0.8f, b.y);
        path.close();

        canvas.drawPath(path, paint);
        paint.clearShadowLayer();

        // Glint
        paint.setColor(Color.WHITE);
        canvas.drawCircle(b.x, b.y - b.radius * 0.5f, 3, paint);

        canvas.restore();
    }

    private void drawGeoRock(Canvas canvas, GeoRock b) {
        paint.setColor(Color.rgb(100, 70, 50)); // Brown
        // Irregular Rock Shape (Simulated)
        Path rock = new Path();
        rock.moveTo(b.x - b.radius, b.y);
        rock.lineTo(b.x - b.radius * 0.5f, b.y - b.radius * 0.9f);
        rock.lineTo(b.x + b.radius * 0.5f, b.y - b.radius * 0.8f);
        rock.lineTo(b.x + b.radius, b.y);
        rock.lineTo(b.x + b.radius * 0.6f, b.y + b.radius * 0.8f);
        rock.lineTo(b.x - b.radius * 0.6f, b.y + b.radius * 0.9f);
        rock.close();

        canvas.drawPath(rock, paint);

        // Crack
        paint.setColor(Color.BLACK);
        paint.setStrokeWidth(3);
        paint.setStyle(Paint.Style.STROKE);
        canvas.drawLine(b.x - b.radius * 0.5f, b.y, b.x + b.radius * 0.5f, b.y, paint);
        paint.setStyle(Paint.Style.FILL);
    }

    private void drawAcidBlob(Canvas canvas, AcidBlob b) {
        paint.setColor(Color.rgb(50, 205, 50)); // Lime Green
        float wobble = (float) Math.sin(System.currentTimeMillis() * 0.015) * 4;

        // Main Blob
        canvas.drawOval(b.x - b.radius + wobble, b.y - b.radius - wobble, b.x + b.radius - wobble,
                b.y + b.radius + wobble, paint);

        // Dripping bits
        paint.setAlpha(150);
        canvas.drawCircle(b.x - b.vx * 1.5f, b.y - b.vy * 1.5f, b.radius * 0.4f, paint);
        paint.setAlpha(255);

        // Highlight
        paint.setColor(Color.WHITE);
        paint.setAlpha(100);
        canvas.drawCircle(b.x - b.radius * 0.3f, b.y - b.radius * 0.3f, 5, paint);
        paint.setAlpha(255);
    }

    private void drawClockGear(Canvas canvas, ClockGear b) {
        paint.setColor(Color.rgb(218, 165, 32)); // Gold

        // Gear Teeth
        float angleSep = (float) (Math.PI * 2 / 8);
        for (int i = 0; i < 8; i++) {
            float ang = i * angleSep + (System.currentTimeMillis() * 0.005f);
            float tx = b.x + (float) Math.cos(ang) * b.radius * 1.2f;
            float ty = b.y + (float) Math.sin(ang) * b.radius * 1.2f;
            canvas.drawCircle(tx, ty, b.radius * 0.3f, paint);
        }

        canvas.drawCircle(b.x, b.y, b.radius, paint);

        paint.setColor(Color.BLACK);
        canvas.drawCircle(b.x, b.y, b.radius * 0.5f, paint);
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

    private void drawVoidProjectile(Canvas canvas, VoidProjectile b) {
        // Dark Core with Pulsing Violet Aura
        float pulse = (float) (1.0 + Math.sin(System.currentTimeMillis() * 0.01) * 0.2);

        // Aura
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.rgb(75, 0, 130)); // Indigo
        paint.setAlpha(100);
        paint.setShadowLayer(30, 0, 0, Color.MAGENTA);
        canvas.drawCircle(b.x, b.y, b.radius * (1.2f * pulse), paint);
        paint.clearShadowLayer();

        // Core
        paint.setAlpha(255);
        paint.setColor(Color.BLACK);
        canvas.drawCircle(b.x, b.y, b.radius, paint);

        // Inner Light
        paint.setColor(Color.rgb(138, 43, 226)); // Blue Violet
        canvas.drawCircle(b.x, b.y, b.radius * 0.4f, paint);

        // Trail Particles (Simple logic)
        if (random.nextFloat() < 0.3f) {
            // Add a temporary particle method or just rely on global particles?
            // For now, let's just draw a simple trail tail
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(b.radius * 0.5f);
            paint.setColor(Color.MAGENTA);
            paint.setAlpha(100);
            canvas.drawLine(b.x, b.y, b.x - b.vx * 3, b.y - b.vy * 3, paint);
            paint.setAlpha(255);
        }
    }

    private void drawCometTrail(Canvas canvas, Ball ball) {
        if (selectedTrail.contains("cosmic")) {
            drawCosmicTrail(canvas, ball);
            return;
        } else if (selectedTrail.contains("lava")) {
            drawLavaTrail(canvas, ball);
            return;
        } else if (selectedTrail.contains("electric")) {
            drawElectricTrail(canvas, ball);
            return;
        } else if (selectedTrail.contains("rainbow")) {
            drawRainbowTrail(canvas, ball);
            return;
        } else if (selectedTrail.contains("ghost")) {
            drawGhostTrail(canvas, ball);
            return;
        } else if (selectedTrail.contains("bubble")) {
            drawBubbleTrail(canvas, ball);
            return;
        } else if (selectedTrail.contains("pixel")) {
            drawPixelTrail(canvas, ball);
            return;
        } else if (selectedTrail.contains("dna")) {
            drawDNATrail(canvas, ball);
            return;
        } else if (selectedTrail.contains("sparkle")) {
            drawSparkleTrail(canvas, ball);
            return;
        } else if (selectedTrail.contains("matrix")) {
            drawMatrixTrail(canvas, ball);
            return;
        } else if (selectedTrail.contains("sakura")) {
            drawSakuraTrail(canvas, ball);
            return;
        } else if (selectedTrail.contains("void")) {
            drawVoidTrail(canvas, ball);
            return;
        } else if (selectedTrail.contains("crystal")) {
            drawCrystalTrail(canvas, ball);
            return;
        } else if (selectedTrail.contains("music")) {
            drawMusicTrail(canvas, ball);
            return;
        } else if (selectedTrail.contains("heartbeat")) {
            drawHeartbeatTrail(canvas, ball);
            return;
        } else if (selectedTrail.contains("comet")) {
            drawCometTrailImpl(canvas, ball);
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
        }
    }

    private void drawMatrixTrail(Canvas canvas, Ball ball) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.GREEN);
        paint.setTextSize(ball.radius * 1.5f);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTypeface(Typeface.MONOSPACE);

        String[] chars = { "0", "1", "X", "Y", "Z" };

        for (int i = 0; i < ball.trail.size(); i++) {
            TrailPoint p = ball.trail.get(i);
            float ratio = 1.0f - (float) i / MAX_TRAIL_POINTS;

            if (i % 2 == 0) { // Optimize density
                paint.setAlpha((int) (255 * ratio));
                String c = chars[(i + (int) System.currentTimeMillis() / 100) % chars.length];
                canvas.drawText(c, p.x, p.y + ball.radius * 0.5f, paint);
            }
        }
    }

    private void drawSakuraTrail(Canvas canvas, Ball ball) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.rgb(255, 183, 197)); // Pink

        for (int i = 0; i < ball.trail.size(); i++) {
            TrailPoint p = ball.trail.get(i);
            float ratio = 1.0f - (float) i / MAX_TRAIL_POINTS;

            if (i % 3 == 0) { // Petals scattered
                paint.setAlpha((int) (200 * ratio));
                float offset = (random.nextFloat() - 0.5f) * 20 * ratio;
                float angle = ratio * 360;

                canvas.save();
                canvas.rotate(angle, p.x + offset, p.y + offset);
                canvas.drawOval(p.x + offset - 5 * ratio, p.y + offset - 8 * ratio, p.x + offset + 5 * ratio,
                        p.y + offset + 8 * ratio, paint);
                canvas.restore();
            }
        }
    }

    private void drawVoidTrail(Canvas canvas, Ball ball) {
        paint.setStyle(Paint.Style.FILL);
        for (int i = 0; i < ball.trail.size(); i++) {
            TrailPoint p = ball.trail.get(i);
            float ratio = 1.0f - (float) i / MAX_TRAIL_POINTS;
            float size = p.radius * ratio;

            // White Rim
            paint.setColor(Color.WHITE);
            paint.setAlpha((int) (100 * ratio));
            canvas.drawCircle(p.x, p.y, size, paint);

            // Black Core
            paint.setColor(Color.BLACK);
            paint.setAlpha(255);
            canvas.drawCircle(p.x, p.y, size * 0.7f, paint);
        }
    }

    private void drawCrystalTrail(Canvas canvas, Ball ball) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.CYAN);

        Path crystal = new Path();
        for (int i = 0; i < ball.trail.size(); i += 2) {
            TrailPoint p = ball.trail.get(i);
            float ratio = 1.0f - (float) i / MAX_TRAIL_POINTS;
            float size = p.radius * ratio;

            paint.setAlpha((int) (180 * ratio));

            crystal.reset();
            crystal.moveTo(p.x, p.y - size);
            crystal.lineTo(p.x + size * 0.6f, p.y);
            crystal.lineTo(p.x, p.y + size);
            crystal.lineTo(p.x - size * 0.6f, p.y);
            crystal.close();

            canvas.drawPath(crystal, paint);
        }
    }

    private void drawMusicTrail(Canvas canvas, Ball ball) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.WHITE);
        paint.setStrokeWidth(3);

        for (int i = 0; i < ball.trail.size(); i += 3) {
            TrailPoint p = ball.trail.get(i);
            float ratio = 1.0f - (float) i / MAX_TRAIL_POINTS;
            float size = p.radius * ratio * 0.5f;

            paint.setAlpha((int) (255 * ratio));

            // Note head
            canvas.drawCircle(p.x - size, p.y + size, size, paint);
            // Stem
            canvas.drawLine(p.x, p.y + size, p.x, p.y - size * 2, paint);
            // Flag
            canvas.drawLine(p.x, p.y - size * 2, p.x + size * 2, p.y - size, paint);
        }
    }

    private void drawHeartbeatTrail(Canvas canvas, Ball ball) {
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(4);
        paint.setColor(Color.RED);
        paint.setShadowLayer(10, 0, 0, Color.RED);

        if (ball.trail.size() > 2) {
            Path pulse = new Path();
            pulse.moveTo(ball.trail.get(0).x, ball.trail.get(0).y);

            for (int i = 1; i < ball.trail.size(); i++) {
                TrailPoint p = ball.trail.get(i);
                float offset = 0;
                // Add jagged pulse every few points
                if (i % 5 == 2)
                    offset = -15;
                else if (i % 5 == 3)
                    offset = 15;

                pulse.lineTo(p.x, p.y + offset);
            }
            canvas.drawPath(pulse, paint);
        }
        paint.clearShadowLayer();
    }

    private void drawCometTrailImpl(Canvas canvas, Ball ball) {
        // Fireball comet
        paint.setStyle(Paint.Style.FILL);
        for (int i = 0; i < ball.trail.size(); i++) {
            TrailPoint p = ball.trail.get(i);
            float ratio = 1.0f - (float) i / MAX_TRAIL_POINTS;

            // Core from Orange to Yellow
            int r = 255;
            int g = (int) (255 * (1 - ratio)) + 100;
            if (g > 255)
                g = 255;

            paint.setColor(Color.rgb(r, g, 0));
            paint.setAlpha((int) (200 * ratio));
            canvas.drawCircle(p.x, p.y, p.radius * ratio, paint);

            // Outer glow
            if (i < 5) {
                paint.setColor(Color.RED);
                paint.setAlpha(100);
                canvas.drawCircle(p.x, p.y, p.radius * 2 * ratio, paint);
            }
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
            // --- TURKEY (Super Lig) ---
            case "team_galatasaray":
                drawBadgeIcon(canvas, bx, by, r, Color.rgb(169, 4, 50), Color.rgb(253, 185, 19), Color.WHITE, "GS");
                break;
            case "team_fenerbahce":
                drawBadgeIcon(canvas, bx, by, r, Color.rgb(0, 32, 91), Color.YELLOW, Color.rgb(0, 32, 91), "FB");
                break;
            case "team_besiktas":
                drawBadgeIcon(canvas, bx, by, r, Color.BLACK, Color.WHITE, Color.BLACK, "BJK");
                break;
            case "team_trabzon":
                drawBadgeIcon(canvas, bx, by, r, Color.rgb(125, 0, 48), Color.rgb(105, 190, 221), Color.WHITE, "TS");
                break;
            case "team_basaksehir":
                drawBadgeIcon(canvas, bx, by, r, Color.rgb(22, 57, 100), Color.rgb(235, 96, 11), Color.WHITE, "IBFK");
                break;
            case "team_adana":
                drawBadgeIcon(canvas, bx, by, r, Color.rgb(40, 93, 161), Color.rgb(40, 93, 161), Color.WHITE, "ADS");
                break;
            case "team_samsun":
                drawBadgeIcon(canvas, bx, by, r, Color.RED, Color.WHITE, Color.RED, "SAM");
                break;
            case "team_goztepe":
                drawBadgeIcon(canvas, bx, by, r, Color.RED, Color.YELLOW, Color.RED, "GOZ");
                break;
            case "team_sivas":
                drawBadgeIcon(canvas, bx, by, r, Color.RED, Color.WHITE, Color.RED, "YGS");
                break;
            case "team_konya":
                drawBadgeIcon(canvas, bx, by, r, Color.rgb(0, 98, 65), Color.WHITE, Color.BLACK, "KON");
                break;

            // --- ENGLAND (Premier League) ---
            case "team_man_city":
                drawBadgeIcon(canvas, bx, by, r, Color.rgb(108, 171, 221), Color.WHITE, Color.rgb(108, 171, 221),
                        "MCI");
                break;
            case "team_arsenal":
                drawBadgeIcon(canvas, bx, by, r, Color.rgb(239, 1, 7), Color.WHITE, Color.rgb(239, 1, 7), "ARS");
                break;
            case "team_liverpool":
                drawBadgeIcon(canvas, bx, by, r, Color.rgb(200, 16, 46), Color.WHITE, Color.rgb(200, 16, 46), "LIV");
                break;
            case "team_aston_villa":
                drawBadgeIcon(canvas, bx, by, r, Color.rgb(149, 191, 229), Color.rgb(103, 14, 54), Color.YELLOW, "AVL");
                break;
            case "team_tottenham":
                drawBadgeIcon(canvas, bx, by, r, Color.rgb(19, 34, 87), Color.WHITE, Color.rgb(19, 34, 87), "TOT");
                break;
            case "team_chelsea":
                drawBadgeIcon(canvas, bx, by, r, Color.rgb(3, 70, 148), Color.WHITE, Color.rgb(3, 70, 148), "CHE");
                break;
            case "team_newcastle":
                drawBadgeIcon(canvas, bx, by, r, Color.BLACK, Color.WHITE, Color.BLACK, "NEW");
                break;
            case "team_man_utd":
                drawBadgeIcon(canvas, bx, by, r, Color.rgb(218, 41, 28), Color.BLACK, Color.YELLOW, "MUN");
                break;
            case "team_westham":
                drawBadgeIcon(canvas, bx, by, r, Color.rgb(122, 38, 58), Color.rgb(27, 177, 231), Color.WHITE, "WHU");
                break;
            case "team_brighton":
                drawBadgeIcon(canvas, bx, by, r, Color.rgb(0, 87, 184), Color.WHITE, Color.rgb(0, 87, 184), "BHA");
                break;

            // --- SPAIN (La Liga) ---
            case "team_real_madrid":
                drawBadgeIcon(canvas, bx, by, r, Color.WHITE, Color.rgb(254, 190, 16), Color.BLACK, "RM");
                break;
            case "team_girona":
                drawBadgeIcon(canvas, bx, by, r, Color.RED, Color.WHITE, Color.RED, "GIR");
                break;
            case "team_barcelona":
                drawBadgeIcon(canvas, bx, by, r, Color.rgb(0, 77, 152), Color.rgb(165, 0, 68), Color.YELLOW, "BAR");
                break;
            case "team_atletico":
                drawBadgeIcon(canvas, bx, by, r, Color.rgb(203, 53, 36), Color.WHITE, Color.BLUE, "ATM");
                break;
            case "team_bilbao":
                drawBadgeIcon(canvas, bx, by, r, Color.RED, Color.BLACK, Color.WHITE, "ATH");
                break;
            case "team_sociedad":
                drawBadgeIcon(canvas, bx, by, r, Color.BLUE, Color.WHITE, Color.BLUE, "RSO");
                break;
            case "team_betis":
                drawBadgeIcon(canvas, bx, by, r, Color.rgb(0, 150, 69), Color.WHITE, Color.BLACK, "BET");
                break;
            case "team_valencia":
                drawBadgeIcon(canvas, bx, by, r, Color.WHITE, Color.BLACK, Color.BLACK, "VAL");
                break;
            case "team_villarreal":
                drawBadgeIcon(canvas, bx, by, r, Color.YELLOW, Color.YELLOW, Color.BLUE, "VIL");
                break;
            case "team_sevilla":
                drawBadgeIcon(canvas, bx, by, r, Color.WHITE, Color.RED, Color.BLACK, "SEV");
                break;

            // --- GERMANY (Bundesliga) ---
            case "team_leverkusen":
                drawBadgeIcon(canvas, bx, by, r, Color.BLACK, Color.RED, Color.WHITE, "B04");
                break;
            case "team_bayern":
                drawBadgeIcon(canvas, bx, by, r, Color.rgb(220, 5, 45), Color.WHITE, Color.rgb(220, 5, 45), "FCB");
                break;
            case "team_stuttgart":
                drawBadgeIcon(canvas, bx, by, r, Color.WHITE, Color.RED, Color.BLACK, "VFB");
                break;
            case "team_leipzig":
                drawBadgeIcon(canvas, bx, by, r, Color.WHITE, Color.RED, Color.rgb(23, 23, 50), "RBL");
                break;
            case "team_dortmund":
                drawBadgeIcon(canvas, bx, by, r, Color.rgb(253, 225, 0), Color.BLACK, Color.rgb(253, 225, 0), "BVB");
                break;
            case "team_frankfurt":
                drawBadgeIcon(canvas, bx, by, r, Color.RED, Color.BLACK, Color.WHITE, "SGE");
                break;
            case "team_hoffenheim":
                drawBadgeIcon(canvas, bx, by, r, Color.BLUE, Color.WHITE, Color.BLUE, "TSG");
                break;
            case "team_bremen":
                drawBadgeIcon(canvas, bx, by, r, Color.rgb(0, 150, 69), Color.WHITE, Color.rgb(0, 150, 69), "SVW");
                break;
            case "team_freiburg":
                drawBadgeIcon(canvas, bx, by, r, Color.BLACK, Color.WHITE, Color.BLACK, "SCF");
                break;
            case "team_augsburg":
                drawBadgeIcon(canvas, bx, by, r, Color.WHITE, Color.rgb(186, 55, 51), Color.GREEN, "FCA");
                break;

            // --- FRANCE (Ligue 1) ---
            case "team_psg":
                drawBadgeIcon(canvas, bx, by, r, Color.rgb(0, 65, 112), Color.RED, Color.WHITE, "PSG");
                break;
            case "team_monaco":
                drawBadgeIcon(canvas, bx, by, r, Color.RED, Color.WHITE, Color.rgb(203, 161, 53), "ASM");
                break;
            case "team_brest":
                drawBadgeIcon(canvas, bx, by, r, Color.RED, Color.WHITE, Color.RED, "SB29");
                break;
            case "team_lille":
                drawBadgeIcon(canvas, bx, by, r, Color.RED, Color.rgb(23, 23, 50), Color.WHITE, "LOSC");
                break;
            case "team_nice":
                drawBadgeIcon(canvas, bx, by, r, Color.BLACK, Color.RED, Color.WHITE, "OGC");
                break;
            case "team_lens":
                drawBadgeIcon(canvas, bx, by, r, Color.rgb(255, 215, 0), Color.RED, Color.BLACK, "RCL");
                break;
            case "team_marseille":
                drawBadgeIcon(canvas, bx, by, r, Color.WHITE, Color.rgb(137, 203, 235), Color.rgb(137, 203, 235), "OM");
                break;
            case "team_lyon":
                drawBadgeIcon(canvas, bx, by, r, Color.WHITE, Color.rgb(23, 23, 50), Color.rgb(218, 41, 28), "OL");
                break;
            case "team_rennes":
                drawBadgeIcon(canvas, bx, by, r, Color.RED, Color.BLACK, Color.WHITE, "SRFC");
                break;
            case "team_reims":
                drawBadgeIcon(canvas, bx, by, r, Color.RED, Color.WHITE, Color.RED, "SDR");
                break;

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

        RadialGradient gradient = new RadialGradient(ball.x - ball.radius / 3, ball.y - ball.radius / 3, ball.radius,
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
        if (showBossDefeated) {
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.RED);
            paint.setTextSize(screenWidth * 0.10f); // Reduced from 0.15f to prevent overflow
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setShadowLayer(50, 0, 0, Color.BLACK);
            canvas.drawText("BOSS DEFEATED", centerX, centerY, paint);
            paint.clearShadowLayer();
            return;
        }

        if (showPlayerDefeated && !gameOver) {
            return; // Handled in main draw loop
        }
        // UI artık MainActivity'deki custom panellerde gösteriliyor
        // Eski text-based UI kaldırıldı

        // MainActivity'deki panelleri güncelle
        updateMainActivityPanels();

        // Level Seçim Ekranı
        if (showLevelSelector && !gameStarted) {
            drawLevelSelector(canvas);
            // MainMenuPanel should be hidden via updateUIPanels called in toggle or start
            return;
        }

        if (!gameStarted) {
            // Main Menu logic is now in MainActivity panel.
            // We only draw the Black Hole background element here if desired,
            // but previously it was part of the menu frame.
            // Let's keep drawing the black hole if we want it "behind" the UI panel,
            // OR let the UI panel handle it.
            // The UI panel has a background color but is transparentish.

            // Let's rely on the game loop continuing to draw the space background
            // underneath.
            // We can return early to avoid drawing any game objects on main menu if
            // desired,
            // but keeping them is nice (animated space background).
        }

        // Game Over logic moved to MainActivity
        // if (gameOver) { ... } REMOVED

        // Draw In-Game Menu Icon logic
        if (gameStarted && !gameOver) {
            drawSkillButton(canvas);
            drawInventory(canvas);
            drawPassiveSlot(canvas, screenWidth * 0.1f);
            drawInGameMenuIcon(canvas); // Added Home Button
        }
        // Başlık... (Mevcut kod aşağıda kalacak, sadece if bloğunu değiştiriyorum)

        // Instructions overlay
        if (showInstructions) {
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

        canvas.drawRoundRect(centerX - panelWidth / 2, panelTop, centerX + panelWidth / 2, panelBottom, 40, 40, paint);

        // Panel kenarlığı (Neon glow)
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(5);
        paint.setColor(Color.rgb(0, 255, 255)); // Cyan
        paint.setShadowLayer(20, 0, 0, Color.CYAN);
        canvas.drawRoundRect(centerX - panelWidth / 2, panelTop, centerX + panelWidth / 2, panelBottom, 40, 40, paint);
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
        canvas.drawLine(centerX - panelWidth * 0.3f, panelTop + panelHeight * 0.15f, centerX + panelWidth * 0.3f,
                panelTop + panelHeight * 0.15f, paint);

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
        int[] colors = { Color.rgb(255, 165, 0), Color.rgb(255, 215, 0), Color.BLUE, Color.CYAN,
                Color.rgb(255, 192, 203), Color.rgb(173, 216, 230), Color.RED, Color.GREEN, Color.rgb(139, 0, 0),
                Color.YELLOW };
        String[] descs = { "Extra Time: +5 Seconds", "Power Boost: Strong shot", "Barrier: Shield protection",
                "Electric: Chain reaction", "Clone: Duplicate ball", "Freeze: Stop movement", "Missile: Homing attack",
                "Teleport: Instant jump", "Boom: Area explosion", "Lightning: Strike black balls" };

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

        // Ana panel (Moved down slightly)
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.argb(170, 25, 25, 50)); // Koyu mavi-mor

        float panelWidth = screenWidth * 0.85f;
        float panelHeight = screenHeight * 0.6f;
        float cy = centerY + screenHeight * 0.05f; // Moved DOWN by 5%

        canvas.drawRoundRect(centerX - panelWidth / 2, cy - panelHeight / 2, centerX + panelWidth / 2,
                cy + panelHeight / 2, 35, 35, paint);

        // Panel kenarlığı (Neon glow)
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(3);
        paint.setColor(Color.rgb(255, 215, 0));
        paint.setShadowLayer(18, 0, 0, Color.rgb(255, 215, 0));
        canvas.drawRoundRect(centerX - panelWidth / 2, cy - panelHeight / 2, centerX + panelWidth / 2,
                cy + panelHeight / 2, 35, 35, paint);
        paint.clearShadowLayer();

        // Başlık
        paint.setStyle(Paint.Style.FILL);
        paint.setTextSize(screenWidth * 0.09f); // Reduced (was 0.1f)
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setColor(Color.rgb(255, 215, 0));
        // text coordinates relative to shifted cy
        canvas.drawText("HALL OF FAME", centerX, cy - panelHeight * 0.35f, paint);

        // Skorlar
        paint.setTextSize(screenWidth * 0.06f); // Reduced (was 0.07f)
        paint.setColor(Color.WHITE);
        canvas.drawText("Best Score: " + highScore, centerX, cy - panelHeight * 0.05f, paint);

        int bestSpace = ((highLevel - 1) / 10) + 1;
        canvas.drawText("Best Level: Space " + bestSpace + " Level " + highLevel, centerX,
                cy + panelHeight * 0.15f, paint);

        canvas.drawText("Max Combo: " + maxCombo, centerX, cy + panelHeight * 0.35f, paint);

        // Close butonu
        paint.setTextSize(screenWidth * 0.05f); // Reduced
        paint.setColor(Color.rgb(255, 100, 100));
        canvas.drawText("TAP TO CLOSE", centerX, cy + panelHeight * 0.45f, paint);
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
        android.graphics.Shader gradient = new android.graphics.LinearGradient(0, rect.top, 0, rect.bottom, topColor,
                bottomColor, android.graphics.Shader.TileMode.CLAMP);
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
                    updateUIPanels();
                    return true;
                }

                if (showLevelSelector) {
                    // Arrow positions (below the grid)
                    float arrowRadius = screenWidth * 0.08f;
                    float gridStartY = screenHeight * 0.52f; // Moved down (was 0.42f)
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
                    float backBtnY = screenHeight * 0.92f; // Moved down to avoid overlap (was 0.80f)
                    if (touchX > centerX - backBtnW / 2 && touchX < centerX + backBtnW / 2
                            && touchY > backBtnY - backBtnH / 2 && touchY < backBtnY + backBtnH / 2) {
                        showLevelSelector = false;
                        updateUIPanels();
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
                                updateUIPanels();
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
                    // Main Menu touches are now handled by NeonMainMenuPanel in MainActivity
                    return true;
                } else if (gameOver) {
                    // Game Over touches are now handled by NeonGameOverPanel in MainActivity
                    return true;

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
                        gameCompleted = false;
                        gameStarted = false;
                        score = 0;
                        initLevel(1);
                        maxUnlockedLevel = Math.min(maxUnlockedLevel, 100); // just safety
                        updateUIPanels();
                        playSound(soundLaunch);
                        return true;
                    }
                } else {
                    int action = event.getAction();
                    // Check In-Game Menu Icon touch only when game is running and not paused/over
                    float density = getResources().getDisplayMetrics().density;
                    float iconSize = screenWidth * 0.1f;
                    float iconX = screenWidth - iconSize * 0.8f;
                    float iconY = screenHeight * 0.25f; // Adjusted position (moved down to avoid overlap)

                    if (Math.abs(touchX - iconX) < iconSize / 2 && Math.abs(touchY - iconY) < iconSize / 2) {
                        resetToMainMenu();
                        playSound(soundLaunch);
                        return true;
                    }

                    // Skill Button Check
                    float dSkill = (float) Math.sqrt(Math.pow(touchX - skillBtnX, 2) + Math.pow(touchY - skillBtnY, 2));
                    long currentTime = System.currentTimeMillis();
                    boolean isOnCooldown = (currentTime - lastSkillUseTime < SKILL_COOLDOWN);

                    if (dSkill < skillBtnRadius) {
                        if (!activeSkill.equals("None") && !isOnCooldown) {
                            lastSkillUseTime = currentTime;
                            isSkillActive = true;
                            if (activeSkill.equals("Ghost Ball")) {
                                ghostModeActive = true;
                                ghostModeEndTime = currentTime + 5000;
                                floatingTexts
                                        .add(new FloatingText("GHOST MODE!", whiteBall.x, whiteBall.y, Color.CYAN));
                                playSound(soundPower);
                            }
                        }
                    }

                    // Inventory Check
                    for (int i = 0; i < inventory.size(); i++) {
                        float slotX = inventoryX + i * (inventorySlotSize * 1.2f);
                        float dInv = (float) Math.sqrt(Math.pow(touchX - slotX, 2) + Math.pow(touchY - inventoryY, 2));
                        if (dInv < inventorySlotSize * 0.45f) {
                            if (whiteBall != null) {
                                activateSkillEffect(inventory.get(i), whiteBall);
                                inventory.remove(i);
                            }
                            return true;
                        }
                    }
                }

                // Classic Ball Touch Logic
                if (gameStarted && !gameOver) {
                    Ball touchedBall = null;
                    float minDist = Float.MAX_VALUE;

                    float screenHeight = getHeight();
                    float bottomZoneStart = screenHeight * 0.6f;

                    // Cooldown Check: Prevent spamming click to stop ball
                    if (System.currentTimeMillis() - lastLaunchTime < 750) {
                        return true; // Ignore touch if within 0.75 second of last shot
                    }

                    if (touchY >= bottomZoneStart && whiteBall != null) {
                        // User touched bottom area -> control white ball
                        touchedBall = whiteBall;

                        isDragging = true;
                        draggedBall = touchedBall;
                        // For bottom zone control, we drag FROM THE TOUCH POINT, not moving the ball
                        // itself visually yet
                        dragStartX = touchX;
                        dragStartY = touchY;
                        dragStartTime = System.currentTimeMillis();
                        touchedBall.vx = 0;
                        touchedBall.vy = 0;
                        touchedBall.trail.clear();

                        // Önemli: Bottom zone kontrolü olduğunu işaretlemek için belki bir flag
                        // kullanılabilir
                        // Veya ACTION_MOVE'da logic buna göre düzenlenmeli.
                        // Mevcut logic draggedBall.x = touchX yapıyor ki bu bottom zone için İSTENMEZ.
                        // Bottom zone = "Sling shot" (sadece ok göster, top hareket etmesin)
                    } else {
                        // Classic Control logic check
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
                            // Prevent dragging if frozen
                            if (touchedBall == whiteBall && isFrozen) {
                                floatingTexts
                                        .add(new FloatingText("FROZEN!", whiteBall.x, whiteBall.y - 60, Color.CYAN));
                            } else {
                                isDragging = true;
                                draggedBall = touchedBall;
                                // Classic behavior: drag starts at Ball's center
                                dragStartX = touchedBall.x;
                                dragStartY = touchedBall.y;
                                dragStartTime = System.currentTimeMillis();
                                touchedBall.vx = 0;
                                touchedBall.vy = 0;
                                touchedBall.trail.clear();
                            }
                        }
                    }
                }
                break;

            case MotionEvent.ACTION_MOVE:
                if (isDragging && draggedBall != null) {
                    currentTouchX = touchX;
                    currentTouchY = touchY;

                    // Check if this is a bottom zone drag (sling shot) or classic drag
                    float screenH = getHeight();
                    boolean isBottomDrag = dragStartY >= screenH * 0.6f;

                    if (isBottomDrag) {
                        // Sling shot mode: DO NOT MOVE THE BALL
                        // Just update internal state if needed (or do nothing visual yet)
                        // Maybe later add an arrow visualization
                    } else {
                        // Classic Drag Mode: Move the ball with finger
                        // But prevent if frozen
                        if (draggedBall == whiteBall && isFrozen) {
                            // Don't move frozen ball
                            return true;
                        }

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
                }
                break;

            case MotionEvent.ACTION_UP:
                if (isDragging && draggedBall != null) {
                    // Prevent launch if frozen
                    if (draggedBall == whiteBall && isFrozen) {
                        isDragging = false;
                        draggedBall = null;
                        return true;
                    }

                    float screenH = getHeight();
                    boolean isBottomDrag = dragStartY >= screenH * 0.6f;

                    float dx, dy;

                    if (isBottomDrag) {
                        // Sling Shot Logic (Bottom Zone)
                        // Vector is from Current Touch (release) TO Start Touch
                        // Pull down (End Y > Start Y) -> Vector Y should be negative (Up)
                        dx = dragStartX - touchX;
                        dy = dragStartY - touchY;
                    } else {
                        // Classic Logic (Ball Drag)
                        // Ball was moved to draggedBall.x/y. Start was original center.
                        // Force is towards center (spring effect)
                        dx = dragStartX - draggedBall.x; // Backwards: Center - Current
                        dy = dragStartY - draggedBall.y;

                        // Reset ball visual position to center instantly before shooting
                        draggedBall.x = dragStartX;
                        draggedBall.y = dragStartY;
                    }

                    float distance = Math.min((float) Math.sqrt(dx * dx + dy * dy), MAX_DRAG_DISTANCE);
                    float ratio = distance / MAX_DRAG_DISTANCE;
                    lastLaunchPower = ratio;
                    float maxSpeed = powerBoostActive ? 60 : 40; // Increased base speed slightly
                    float speed = ratio * maxSpeed;

                    if (distance > 10) { // Slight threshold
                        // Direction normalized
                        float distNorm = (float) Math.sqrt(dx * dx + dy * dy);
                        draggedBall.vx = (dx / distNorm) * speed;
                        draggedBall.vy = (dy / distNorm) * speed;

                        playSound(soundLaunch);

                        lastLaunchTime = System.currentTimeMillis();

                        if (multiBallActive) {
                            spawnClones(draggedBall);
                            multiBallActive = false;
                        }
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
        // Railgun Button removed (Moved to Top-Left Slot)

        // Draw Home Button
        float density = getResources().getDisplayMetrics().density;
        float iconSize = screenWidth * 0.1f;
        float x = screenWidth - iconSize * 0.8f;
        float y = screenHeight * 0.25f; // Moved down to avoid overlap

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

    private void drawTrajectory(Canvas canvas, float launchAngle, float ratio, Ball currentDraggedBall) {
        // Çizgi ve Ok parametreleri
        float startDist = currentDraggedBall.radius * 1.5f;
        float lineLen = (250 + (upgradeAim - 1) * 8) * ratio; // Scaling: Max ~130% of base (250 -> 322)

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

        } else if (selectedTrajectory.equals("sniper")) {
            // Sniper Crosshairs
            paint.setStrokeWidth(2);
            paint.setColor(Color.RED);
            canvas.drawLine(startX, startY, vEndX, vEndY, paint);
            int crosses = 4;
            for (int i = 1; i <= crosses; i++) {
                float t = (float) i / (crosses + 1);
                float px = startX + (vEndX - startX) * t;
                float py = startY + (vEndY - startY) * t;
                paint.setStrokeWidth(4);
                float size = 15;
                canvas.drawLine(px - size, py, px + size, py, paint);
                canvas.drawLine(px, py - size, px, py + size, paint);
            }
        } else if (selectedTrajectory.equals("double")) {
            // Double Lines
            paint.setStrokeWidth(3);
            paint.setColor(Color.CYAN);
            paint.setShadowLayer(10, 0, 0, Color.CYAN);
            // Offset perpendicular to line
            float perpX = -(endY - startY) / lineLen * 10;
            float perpY = (endX - startX) / lineLen * 10;
            canvas.drawLine(startX + perpX, startY + perpY, vEndX + perpX, vEndY + perpY, paint);
            canvas.drawLine(startX - perpX, startY - perpY, vEndX - perpX, vEndY - perpY, paint);
            paint.clearShadowLayer();
        } else if (selectedTrajectory.equals("rainbow")) {
            // Rainbow
            int[] colors = { Color.RED, Color.YELLOW, Color.GREEN, Color.CYAN, Color.BLUE, Color.MAGENTA };
            LinearGradient shader = new LinearGradient(startX, startY, vEndX, vEndY, colors, null,
                    Shader.TileMode.REPEAT);
            paint.setShader(shader);
            paint.setStrokeWidth(6);
            paint.setAlpha(200);
            canvas.drawLine(startX, startY, vEndX, vEndY, paint);
            paint.setShader(null);
        } else if (selectedTrajectory.equals("dashdot")) {
            // Dash Dot
            paint.setPathEffect(new android.graphics.DashPathEffect(new float[] { 40, 20, 10, 20 }, 0));
            paint.setStrokeWidth(4);
            paint.setColor(Color.WHITE);
            canvas.drawLine(startX, startY, vEndX, vEndY, paint);
            paint.setPathEffect(null);
        } else if (selectedTrajectory.equals("stars")) {
            // Stars
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.YELLOW);
            int stars = 6;
            for (int i = 1; i <= stars; i++) {
                float t = (float) i / (stars + 1);
                float px = startX + (vEndX - startX) * t;
                float py = startY + (vEndY - startY) * t;
                drawStarPath(canvas, px, py, 12); // Reusing existing helper
            }
        } else if (selectedTrajectory.equals("hearts")) {
            // Hearts
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.rgb(255, 105, 180));
            int hearts = 6;
            for (int i = 1; i <= hearts; i++) {
                float t = (float) i / (hearts + 1);
                float px = startX + (vEndX - startX) * t;
                float py = startY + (vEndY - startY) * t;
                // Simple heart shape (circle for now to keep it fast)
                canvas.drawCircle(px, py, 10, paint);
            }
        } else if (selectedTrajectory.equals("tech")) {
            // Tech Circuit
            paint.setColor(Color.GREEN);
            paint.setStrokeWidth(2);
            canvas.drawLine(startX, startY, vEndX, vEndY, paint);
            int nodes = 5;
            paint.setStyle(Paint.Style.FILL);
            for (int i = 0; i <= nodes; i++) {
                float t = (float) i / nodes;
                float px = startX + (vEndX - startX) * t;
                float py = startY + (vEndY - startY) * t;
                canvas.drawCircle(px, py, 6, paint);
            }
        } else if (selectedTrajectory.equals("snake")) {
            // Snake S-Curve
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(4);
            paint.setColor(Color.GREEN);
            android.graphics.Path path = new android.graphics.Path();
            path.moveTo(startX, startY);
            int segs = 15;
            for (int i = 1; i <= segs; i++) {
                float t = (float) i / segs;
                float px = startX + (vEndX - startX) * t;
                float py = startY + (vEndY - startY) * t;
                // Faster frequency for snake
                float offset = (float) Math.sin(t * Math.PI * 6) * 15;
                float perpX = -(endY - startY) / lineLen;
                float perpY = (endX - startX) / lineLen;
                path.lineTo(px + perpX * offset, py + perpY * offset);
            }
            canvas.drawPath(path, paint);
        } else if (selectedTrajectory.equals("chevron")) {
            // Chevrons
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(4);
            paint.setColor(Color.rgb(255, 165, 0)); // Orange
            int chevs = 8;
            // Vector direction
            float dx = (vEndX - startX) / lineLen;
            float dy = (vEndY - startY) / lineLen;
            // Perpendicular
            float px = -dy * 15;
            float py = dx * 15;

            // Animate offset
            float animOffset = (System.currentTimeMillis() % 1000) / 1000.0f;

            for (int i = 0; i < chevs; i++) {
                float t = ((float) i / chevs + animOffset) % 1.0f;
                if (t < 0.1f)
                    continue; // Fade in at start

                float cx = startX + (vEndX - startX) * t;
                float cy = startY + (vEndY - startY) * t;

                android.graphics.Path p = new android.graphics.Path();
                p.moveTo(cx - dx * 10 + px, cy - dy * 10 + py);
                p.lineTo(cx, cy);
                p.lineTo(cx - dx * 10 - px, cy - dy * 10 - py);
                canvas.drawPath(p, paint);
            }
        } else if (selectedTrajectory.equals("fire")) {
            // Fire
            paint.setStyle(Paint.Style.FILL);
            int particles = 20;
            for (int i = 0; i < particles; i++) {
                float t = (float) i / particles;
                float px = startX + (vEndX - startX) * t;
                float py = startY + (vEndY - startY) * t;

                float size = 15 * (1 - t) * (0.8f + random.nextFloat() * 0.4f);
                int alpha = (int) (255 * (1 - t));

                // Jitter
                px += (random.nextFloat() - 0.5f) * 10;
                py += (random.nextFloat() - 0.5f) * 10;

                paint.setColor(Color.rgb(255, 69 + random.nextInt(100), 0));
                paint.setAlpha(alpha);
                canvas.drawCircle(px, py, size, paint);
            }
            paint.setAlpha(255);

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
        float panelTop = screenHeight * 0.25f; // Moved down (was 0.15f)

        canvas.drawRoundRect(centerX - panelWidth / 2, panelTop, centerX + panelWidth / 2, panelTop + panelHeight, 40,
                40, paint);

        // Panel kenarlığı (Neon glow)
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(4);
        paint.setColor(Color.rgb(0, 243, 255));
        paint.setShadowLayer(20, 0, 0, Color.CYAN);
        canvas.drawRoundRect(centerX - panelWidth / 2, panelTop, centerX + panelWidth / 2, panelTop + panelHeight, 40,
                40, paint);
        paint.clearShadowLayer();

        // Başlık
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(screenWidth * 0.1f);
        paint.setColor(Color.rgb(0, 243, 255));
        paint.setShadowLayer(20, 0, 0, Color.CYAN);
        canvas.drawText("SELECT LEVEL", centerX, screenHeight * 0.30f, paint); // Moved down (was 0.20f)
        paint.clearShadowLayer();

        // Sayfa Göstergesi (Örn: SPACE 1)
        paint.setTextSize(screenWidth * 0.05f);
        paint.setColor(Color.WHITE);
        canvas.drawText("SPACE " + selectorPage, centerX, screenHeight * 0.36f, paint);

        // Sayfa bilgisi (Her sayfa bir level grubu temsil eder)
        paint.setTextSize(screenWidth * 0.04f);
        paint.setColor(Color.LTGRAY);
        canvas.drawText("Each level contains 5 stages", centerX, screenHeight * 0.40f, paint);

        // Grid (Panel içinde ortalanmış)
        float totalGridWidth = 5 * screenWidth * 0.14f + 4 * screenWidth * 0.02f; // 5 kutu + 4 boşluk
        float gridStartX = centerX - totalGridWidth / 2;
        float gridStartY = screenHeight * 0.52f; // Sync with touch logic (was 0.42f)
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

                // BOSS LEVEL Indicator
                if (lvNum % 10 == 0) {
                    paint.setTextSize(cellHeight * 0.15f);
                    paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
                    paint.setColor(Color.RED); // Pure Red
                    canvas.drawText("BOSS LEVEL", btnX, btnY + cellHeight * 0.42f, paint);
                }

                // Yıldızlar (Basit mantık: 3 yıldız görsel)
                paint.setTextSize(cellHeight * 0.2f);
                paint.setColor(Color.YELLOW);
                canvas.drawText("★★★", btnX, btnY - cellHeight * 0.25f, paint);
            } else {
                // Kilit simgesi (Basitçe "L") veya renk
                paint.setColor(Color.LTGRAY);
                paint.setTextSize(cellHeight * 0.4f);
                canvas.drawText("🔒", btnX, btnY + cellHeight * 0.15f, paint);

                // Show BOSS LEVEL text even if locked
                if (lvNum % 10 == 0) {
                    paint.setTextSize(cellHeight * 0.15f);
                    paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
                    paint.setColor(Color.RED); // Pure Red
                    canvas.drawText("BOSS LEVEL", btnX, btnY + cellHeight * 0.42f, paint);
                }
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
        float backBtnY = screenHeight * 0.92f; // Moved down (was 0.80f)
        float backRadius = backBtnH / 2f;

        // Shadow
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.BLACK);
        paint.setAlpha(100);
        canvas.drawRoundRect(centerX - backBtnW / 2 + 3, backBtnY - backBtnH / 2 + 3, centerX + backBtnW / 2 + 3,
                backBtnY + backBtnH / 2 + 3, backRadius, backRadius, paint);
        paint.setAlpha(255);

        // Outer glow
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(8f);
        paint.setColor(Color.RED);
        paint.setAlpha(80);
        canvas.drawRoundRect(centerX - backBtnW / 2, backBtnY - backBtnH / 2, centerX + backBtnW / 2,
                backBtnY + backBtnH / 2, backRadius, backRadius, paint);
        paint.setAlpha(255);

        // Gradient background (dark to light red)
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.rgb(120, 20, 30));
        canvas.drawRoundRect(centerX - backBtnW / 2, backBtnY - backBtnH / 2, centerX + backBtnW / 2,
                backBtnY + backBtnH / 2, backRadius, backRadius, paint);

        // Inner highlight
        paint.setColor(Color.rgb(180, 40, 50));
        RectF innerRect = new RectF(centerX - backBtnW / 2 + 8, backBtnY - backBtnH / 2 + 8, centerX + backBtnW / 2 - 8,
                backBtnY + backBtnH / 2 - 8);
        canvas.drawRoundRect(innerRect, backRadius * 0.8f, backRadius * 0.8f, paint);

        // Neon border
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(3f);
        paint.setColor(Color.rgb(255, 80, 100));
        paint.setShadowLayer(15, 0, 0, Color.RED);
        canvas.drawRoundRect(centerX - backBtnW / 2, backBtnY - backBtnH / 2, centerX + backBtnW / 2,
                backBtnY + backBtnH / 2, backRadius, backRadius, paint);
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
        canvas.drawArc(new RectF(cx - radius * 0.15f, cy - radius * 0.15f, cx + radius * 0.15f, cy + radius * 0.15f),
                200, 90, false, paint);
        paint.setAlpha(255);
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

    private void updateMainActivityPanels() {
        if (mainActivity != null && gameStarted && !gameOver) {
            int currentStage = stage;
            int currentLevelInSpace = level;

            int remaining = coloredBalls.size() + pendingColoredBalls;
            String levelText = "Level " + currentLevelInSpace + " - Ball: " + remaining;
            int power = (int) (lastLaunchPower * (powerBoostActive ? 200 : 100));

            mainActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mainActivity.updatePanels((int) (timeLeft / 1000), score, coins, // Add coins
                            power, currentStage + "/5", levelText, lives);
                }
            });
        }
    }

    private void drawNeonMenuButton(Canvas canvas, float cx, float cy, float width, float height, String text,
            int color) {
        float radius = height / 2f;
        RectF btnRect = new RectF(cx - width / 2, cy - height / 2, cx + width / 2, cy + height / 2);

        // Shadow
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.BLACK);
        paint.setAlpha(100);
        RectF shadowRect = new RectF(btnRect.left + 3, btnRect.top + 3, btnRect.right + 3, btnRect.bottom + 3);
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
        RectF innerRect = new RectF(btnRect.left + 6, btnRect.top + 6, btnRect.right - 6, btnRect.bottom - 6);
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

    public void startGameWithLevel(int levelNum) {
        this.level = levelNum;
        this.stage = 1;

        startGame();
    }

    public int getCurrentLevel() {
        return level;
    }

    private void drawSkillButton(Canvas canvas) {
        // Position at bottom right (Safe area)
        skillBtnRadius = screenWidth * 0.08f;
        skillBtnX = screenWidth - skillBtnRadius * 1.5f;
        skillBtnY = screenHeight - skillBtnRadius * 1.5f;

        // Inventory Layout
        inventorySlotSize = screenWidth * 0.12f;
        inventoryX = screenWidth * 0.05f + inventorySlotSize / 2;
        inventoryY = screenHeight - inventorySlotSize * 0.8f;

        // Background
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.argb(190, 30, 30, 50));

        long currentTime = System.currentTimeMillis();
        boolean isOnCooldown = (currentTime - lastSkillUseTime < SKILL_COOLDOWN);

        // Hide skill button if None and not on Cooldown
        if (activeSkill.equals("None") && !isOnCooldown)
            return;

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.argb(190, 30, 30, 50));
        canvas.drawCircle(skillBtnX, skillBtnY, skillBtnRadius, paint);

        // Remove redundant declaration below (moved up)
        // long currentTime = System.currentTimeMillis();
        // boolean isOnCooldown = (currentTime - lastSkillUseTime < SKILL_COOLDOWN);

        // Border
        paint.setStyle(Paint.Style.STROKE);
        if (isOnCooldown) {
            paint.setStrokeWidth(5);
            paint.setColor(Color.GRAY);
            paint.setShadowLayer(0, 0, 0, 0);
        } else if (activeSkill.equals("None")) {
            paint.setStrokeWidth(5);
            paint.setColor(Color.DKGRAY);
            paint.setShadowLayer(0, 0, 0, 0);
        } else {
            // Ready to use
            paint.setStrokeWidth(8);
            paint.setColor(Color.CYAN);
            paint.setShadowLayer(20, 0, 0, Color.CYAN);
        }
        canvas.drawCircle(skillBtnX, skillBtnY, skillBtnRadius, paint);
        paint.clearShadowLayer();

        // Icon / Text
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.WHITE);
        paint.setTextAlign(Paint.Align.CENTER);

        float textY = skillBtnY - (paint.descent() + paint.ascent()) / 2;

        if (isOnCooldown) {
            float remaining = (SKILL_COOLDOWN - (currentTime - lastSkillUseTime)) / 1000f;
            paint.setTextSize(skillBtnRadius * 0.5f);
            canvas.drawText(String.format("%.1f", remaining), skillBtnX, textY, paint);
        } else {
            paint.setTextSize(skillBtnRadius * 0.35f);
            String label = activeSkill.equals("None") ? "EMPTY" : activeSkill;
            if (activeSkill.equals("Ghost Ball"))
                label = "GHOST";

            // Draw label
            canvas.drawText(label, skillBtnX, textY, paint);
        }
    }

    private void drawInventory(Canvas canvas) {
        for (int i = 0; i < MAX_INVENTORY_SIZE; i++) {
            float slotX = inventoryX + i * (inventorySlotSize * 1.2f);

            // Draw Slot Background
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.argb(100, 50, 50, 50));
            canvas.drawCircle(slotX, inventoryY, inventorySlotSize * 0.5f, paint);

            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(3);
            paint.setColor(Color.GRAY);
            canvas.drawCircle(slotX, inventoryY, inventorySlotSize * 0.5f, paint);

            // Draw Item if exists
            if (i < inventory.size()) {
                String skill = inventory.get(i);

                // Determine Color/Letter based on skill
                int color = Color.WHITE;
                String letter = "?";

                switch (skill) {
                    case "ghost":
                        color = Color.CYAN;
                        letter = "G";
                        break;
                    case "missile":
                        color = Color.RED;
                        letter = "M";
                        break;
                    case "barrier":
                        color = Color.BLUE;
                        letter = "B";
                        break;
                    case "freeze":
                        color = Color.rgb(0, 200, 255);
                        letter = "F";
                        break;
                    case "clone":
                        color = Color.GREEN;
                        letter = "C";
                        break;
                    case "blackhole":
                        color = Color.DKGRAY;
                        letter = "H";
                        break;
                    case "multiball":
                        color = Color.rgb(255, 165, 0); // Orange
                        letter = "M";
                        break;
                    case "magma":
                        color = Color.rgb(255, 69, 0); // Red-Orange
                        letter = "F";
                        break;
                    case "lightning":
                        color = Color.rgb(255, 255, 0); // Yellow
                        letter = "L";
                        break;
                    case "ufo":
                        color = Color.GREEN;
                        letter = "U";
                        break;
                    case "repulsor":
                        color = Color.CYAN;
                        letter = "R";
                        break;
                    case "alchemy":
                        color = Color.rgb(255, 215, 0);
                        letter = "A";
                        break;
                    case "swarm":
                        color = Color.RED;
                        letter = "S";
                        break;
                    case "slowmo":
                        color = Color.BLUE;
                        letter = "SL";
                        break;
                }

                // Draw Icon (Mini Special Ball)
                paint.setStyle(Paint.Style.FILL);
                RadialGradient gradient = new RadialGradient(slotX - inventorySlotSize * 0.15f,
                        inventoryY - inventorySlotSize * 0.15f, inventorySlotSize * 0.4f, Color.WHITE, color,
                        Shader.TileMode.CLAMP);
                paint.setShader(gradient);
                canvas.drawCircle(slotX, inventoryY, inventorySlotSize * 0.4f, paint);
                paint.setShader(null);

                // Letter
                paint.setColor(Color.WHITE);
                paint.setTextSize(inventorySlotSize * 0.4f);
                paint.setTextAlign(Paint.Align.CENTER);
                canvas.drawText(letter, slotX, inventoryY + inventorySlotSize * 0.15f, paint);
            } else {
                // Empty Slot
                // Draw "EMPTY" Label
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(Color.DKGRAY);
                paint.setTextSize(inventorySlotSize * 0.25f);
                paint.setTextAlign(Paint.Align.CENTER);
                canvas.drawText("EMPTY", slotX, inventoryY + inventorySlotSize * 0.1f, paint);
            }
        }
    }

    private void spawnClones(Ball parent) {
        for (int i = 0; i < 2; i++) {
            Ball clone = new Ball(parent.x, parent.y, parent.radius, Color.rgb(255, 100, 0));
            float angleOffset = (i == 0) ? 0.3f : -0.3f; // ~17 degrees
            float vx = parent.vx;
            float vy = parent.vy;
            float newVx = (float) (vx * Math.cos(angleOffset) - vy * Math.sin(angleOffset));
            float newVy = (float) (vx * Math.sin(angleOffset) + vy * Math.cos(angleOffset));
            clone.vx = newVx;
            clone.vy = newVy;
            // Clone color set in properties
            cloneBalls.add(clone);
        }
    }

    private void handleBoxCollision(Ball ball, float left, float right, float top, float bottom, float damping) {
        if (ball.x < left + ball.radius) {
            ball.x = left + ball.radius;
            ball.vx = Math.abs(ball.vx) * damping;
        } else if (ball.x > right - ball.radius) {
            ball.x = right - ball.radius;
            ball.vx = -Math.abs(ball.vx) * damping;
        }

        if (ball.y < top + ball.radius) {
            ball.y = top + ball.radius;
            ball.vy = Math.abs(ball.vy) * damping;
        } else if (ball.y > bottom - ball.radius) {
            ball.y = bottom - ball.radius;
            ball.vy = -Math.abs(ball.vy) * damping;
        }
    }

    private void handleCircleCollision(Ball ball, float damping) {
        float dx = ball.x - centerX;
        float dy = ball.y - centerY;
        float dist = (float) Math.sqrt(dx * dx + dy * dy);

        if (dist > circleRadius - ball.radius) {
            float angle = (float) Math.atan2(dy, dx);
            ball.x = centerX + (float) Math.cos(angle) * (circleRadius - ball.radius);
            ball.y = centerY + (float) Math.sin(angle) * (circleRadius - ball.radius);

            float nx = (float) Math.cos(angle);
            float ny = (float) Math.sin(angle);

            // Reflected vector: v' = v - 2(v.n)n
            float dot = ball.vx * nx + ball.vy * ny;
            ball.vx = (ball.vx - 2 * dot * nx) * damping;
            ball.vy = (ball.vy - 2 * dot * ny) * damping;
        }
    }

    private void handlePolygonCollision(Ball ball, int sides, float radius, float damping) {
        // Enforce boundary for Regular Polygon centered at (centerX, centerY)
        // Check distance to each edge.
        // A regular polygon can be defined by 'sides' planes.
        // Distance from center to edge (apothem) r = R * cos(PI/sides)
        // But since we want the ball INSIDE, we check if it crosses any edge plane.

        float angleStep = (float) (2 * Math.PI / sides);
        float startAngle = (float) -Math.PI / 2; // Matching draw rotation

        boolean collided = false;

        // Check against all edges
        for (int i = 0; i < sides; i++) {
            float a1 = startAngle + i * angleStep;
            float a2 = startAngle + (i + 1) * angleStep;

            // Vertex 1
            float x1 = centerX + (float) Math.cos(a1) * radius;
            float y1 = centerY + (float) Math.sin(a1) * radius;

            // Vertex 2
            float x2 = centerX + (float) Math.cos(a2) * radius;
            float y2 = centerY + (float) Math.sin(a2) * radius;

            // Edge Normal (pointing OUTWARD from the shape center)
            float normalAngle = (a1 + a2) / 2;
            float nx = (float) Math.cos(normalAngle);
            float ny = (float) Math.sin(normalAngle);

            // Distance of ball center along this normal (relative to shape center)
            float distAlongNormal = (ball.x - centerX) * nx + (ball.y - centerY) * ny;

            // Max allowed distance (Apothem) minus ball radius
            float apothem = radius * (float) Math.cos(Math.PI / sides);
            float maxDist = apothem - ball.radius;

            if (distAlongNormal > maxDist) {
                // Collision!
                // Push back
                float overlap = distAlongNormal - maxDist;
                ball.x -= nx * overlap;
                ball.y -= ny * overlap;

                // Reflect velocity: V' = V - 2(V.N)N
                float dot = ball.vx * nx + ball.vy * ny;

                // Only reflect if moving towards wall (dot > 0 because Normal points OUT)
                if (dot > 0) {
                    ball.vx = (ball.vx - 2 * dot * nx) * damping;
                    ball.vy = (ball.vy - 2 * dot * ny) * damping;
                }
                collided = true;
            }
        }
    }

    enum ParticleType {
        CIRCLE, STAR, FLAME, CONFETTI, RIPPLE, HEART, NOTE, SKULL, PIXEL, GHOST
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

    class MoonRock extends Ball {
        MoonRock(float x, float y, float radius, int color) {
            super(x, y, radius, color);
        }
    }

    // ========== ONLINE MODE METHODS ==========

    class MeteorProjectile extends Ball {
        float trailLengthFactor;

        MeteorProjectile(float x, float y, float radius) {
            super(x, y, radius, Color.rgb(255, 100 + new Random().nextInt(155), 50));
            this.trailLengthFactor = 5f; // Adjust based on speed
        }
    }

    class SpecialBall extends Ball {
        String type;
        int bossHits = 0; // Track hits on boss
        long lastBossHitTime = 0; // Debounce

        SpecialBall(float x, float y, float radius, String type) {
            super(x, y, radius, 0); // Color set below
            this.type = type;
            this.color = getColor(); // Use the inherited 'color' field
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
                    return Color.GREEN; // Brown
                case "split_save":
                    return Color.MAGENTA;
                case "boom":
                    return Color.rgb(139, 0, 0);
                case "ghost":
                    return Color.rgb(211, 211, 211);
                case "lightning":
                    return Color.MAGENTA;
                case "vortex":
                    return Color.CYAN;
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
                case "split_save":
                    return "SS";

                case "electric":
                    return "L";
                case "clone":
                    return "C";
                case "freeze":
                    return "F";
                case "missile":
                    return "M";
                case "teleport":
                    return "TP";
                case "boom":
                    return "X";
                case "ghost":
                    return "G";
                case "lightning":
                    return "?";
                case "vortex":
                    return "VX";
                default:
                    return "?";
            }
        }
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

        // Reset methods for object pooling
        Particle reset(float x, float y, float angle, float speed, int color) {
            this.x = x;
            this.y = y;
            this.vx = (float) Math.cos(angle) * speed;
            this.vy = (float) Math.sin(angle) * speed;
            this.color = color;
            this.life = 30;
            this.maxLife = 30;
            this.type = ParticleType.CIRCLE;
            this.rotation = 0;
            this.vRotation = 0;
            return this;
        }

        Particle reset(float x, float y, float angle, float speed, int color, ParticleType type) {
            reset(x, y, angle, speed, color);
            this.type = type;
            if (type == ParticleType.STAR) {
                this.vRotation = (random.nextFloat() - 0.5f) * 0.2f;
                this.maxLife = 40;
                this.life = 40;
            } else if (type == ParticleType.FLAME) {
                this.maxLife = 20 + random.nextInt(15);
                this.life = this.maxLife;
                this.vx = (random.nextFloat() - 0.5f) * 2f;
                this.vy = -random.nextFloat() * 4f - 2f;
            } else if (type == ParticleType.CONFETTI) {
                this.vRotation = (random.nextFloat() - 0.5f) * 0.5f;
                this.maxLife = 50;
                this.life = 50;
                this.vx *= 0.8f;
                this.vy *= 0.8f;
            } else if (type == ParticleType.RIPPLE) {
                this.vx = 0;
                this.vy = 0;
                this.maxLife = 40;
                this.life = 40;
            }
            return this;
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
            } else if (type == ParticleType.HEART) {
                paint.setColor(color);
                drawHeart(canvas, x, y, 10 * (life / (float) maxLife), paint);
            } else if (type == ParticleType.NOTE) {
                paint.setColor(color);
                drawNote(canvas, x, y, 12 * (life / (float) maxLife), paint);
            } else if (type == ParticleType.SKULL) {
                paint.setColor(color);
                drawSkull(canvas, x, y, 12 * (life / (float) maxLife), paint);
            } else if (type == ParticleType.PIXEL) {
                paint.setColor(color);
                float s = 8 * (life / (float) maxLife);
                canvas.drawRect(x - s, y - s, x + s, y + s, paint);
            } else if (type == ParticleType.GHOST) {
                paint.setColor(color);
                float s = 10 * (life / (float) maxLife);
                canvas.drawCircle(x, y, s, paint);
                // Eyes
                paint.setColor(Color.BLACK);
                canvas.drawCircle(x - s * 0.3f, y - s * 0.2f, s * 0.25f, paint);
                canvas.drawCircle(x + s * 0.3f, y - s * 0.2f, s * 0.25f, paint);
            } else {
                paint.setColor(color);
                canvas.drawCircle(x, y, 4, paint);
            }
            paint.setAlpha(255);
        }

        private void drawHeart(Canvas canvas, float cx, float cy, float size, Paint p) {
            android.graphics.Path path = new android.graphics.Path();
            // Simple heart shape
            path.moveTo(cx, cy + size * 0.5f);
            path.cubicTo(cx - size, cy - size * 0.5f, cx - size, cy - size * 1.5f, cx, cy - size * 0.5f);
            path.cubicTo(cx + size, cy - size * 1.5f, cx + size, cy - size * 0.5f, cx, cy + size * 0.5f);
            canvas.drawPath(path, p);
        }

        private void drawNote(Canvas canvas, float cx, float cy, float size, Paint p) {
            // Eighth note
            canvas.drawCircle(cx - size * 0.4f, cy + size * 0.4f, size * 0.3f, p); // Head
            p.setStrokeWidth(size * 0.15f);
            canvas.drawLine(cx - size * 0.1f, cy + size * 0.4f, cx - size * 0.1f, cy - size * 0.8f, p); // Stem
            canvas.drawLine(cx - size * 0.1f, cy - size * 0.8f, cx + size * 0.5f, cy - size * 0.4f, p); // Flag
            p.setStrokeWidth(0); // Reset
        }

        private void drawSkull(Canvas canvas, float cx, float cy, float size, Paint p) {
            canvas.drawCircle(cx, cy - size * 0.2f, size * 0.6f, p); // Cranium
            canvas.drawRect(cx - size * 0.3f, cy, cx + size * 0.3f, cy + size * 0.6f, p); // Jaw
            // Eyes (Negative space logic handled by drawing black on top, but Paint color
            // is set.
            // Ideally we use a path, but drawing black circles on top works if background
            // isn't black... wait background IS black)
            // So we draw eyes with Color.BLACK.
            int oldColor = p.getColor();
            p.setColor(Color.BLACK);
            canvas.drawCircle(cx - size * 0.25f, cy - size * 0.1f, size * 0.15f, p);
            canvas.drawCircle(cx + size * 0.25f, cy - size * 0.1f, size * 0.15f, p);
            p.setColor(oldColor);
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
        boolean dead = false;
        float angle = 0; // Angle for rotation

        GuidedMissile(float x, float y, Ball target) {
            this.x = x;
            this.y = y;
            this.target = target;
        }

        void update() {
            if (dead)
                return;

            float tx = 0, ty = 0;
            boolean hasTarget = false;

            // Priority: Assigned Target -> Boss -> Random Black/Color
            if (target != null && (coloredBalls.contains(target) || blackBalls.contains(target))) {
                tx = target.x;
                ty = target.y;
                hasTarget = true;
            } else if (currentBoss != null) {
                tx = currentBoss.x;
                ty = currentBoss.y;
                hasTarget = true;
            }

            if (hasTarget) {
                float dx = tx - x;
                float dy = ty - y;
                float distance = (float) Math.sqrt(dx * dx + dy * dy);

                // Calculate angle for rotation
                angle = (float) Math.atan2(dy, dx);

                // Hit Check Boss - REMOVED (Handled in updateMissiles)
                // if (currentBoss != null && distance < currentBoss.radius + radius) { ... }

                if (distance > speed) {
                    vx = (dx / distance) * speed;
                    vy = (dy / distance) * speed;
                    x += vx;
                    y += vy;
                } else {
                    x = tx;
                    y = ty;
                    // Hit logic for ball handled elsewhere or assume hit
                    dead = true;
                }
                speed = Math.min(speed + 0.5f, 15);
            } else {
                dead = true;
            }
        }

        void draw(Canvas canvas, Paint paint) {
            canvas.save();
            canvas.translate(x, y);
            canvas.rotate((float) Math.toDegrees(angle));

            // Flame trail (behind the missile) - ENHANCED with more smoke
            paint.setStyle(Paint.Style.FILL);

            // Main flame trail (5 particles instead of 3)
            for (int i = 0; i < 5; i++) {
                float flameOffset = -radius * 1.5f - i * 6;
                float flameSize = radius * (1.3f - i * 0.2f);

                // Mix of orange-red flame and gray smoke
                int flameColor;
                if (i < 2) {
                    // Hot flame (orange-red)
                    flameColor = i == 0 ? Color.rgb(255, 80, 0) : Color.rgb(255, 120, 20);
                } else {
                    // Cooler smoke (gray with red tint)
                    int grayTone = 80 + (i - 2) * 40; // 80, 120, 160
                    flameColor = Color.rgb(grayTone + 40, grayTone, grayTone); // Reddish gray
                }

                paint.setColor(flameColor);
                Path flamePath = new Path();
                flamePath.moveTo(flameOffset, 0);
                flamePath.lineTo(flameOffset - flameSize, -flameSize * 0.6f);
                flamePath.lineTo(flameOffset - flameSize, flameSize * 0.6f);
                flamePath.close();
                canvas.drawPath(flamePath, paint);
            }

            // Missile body
            paint.setColor(Color.rgb(80, 80, 80)); // Dark gray body
            RectF body = new RectF(-radius * 1.5f, -radius * 0.6f, radius * 1.5f, radius * 0.6f);
            canvas.drawRoundRect(body, radius * 0.3f, radius * 0.3f, paint);

            // Missile nose cone (warhead)
            paint.setColor(Color.rgb(200, 50, 50)); // Red warhead
            Path noseCone = new Path();
            noseCone.moveTo(radius * 1.5f, 0);
            noseCone.lineTo(radius * 0.5f, -radius * 0.6f);
            noseCone.lineTo(radius * 0.5f, radius * 0.6f);
            noseCone.close();
            canvas.drawPath(noseCone, paint);

            // Fins
            paint.setColor(Color.rgb(120, 120, 120)); // Light gray fins
            Path topFin = new Path();
            topFin.moveTo(-radius, -radius * 0.6f);
            topFin.lineTo(-radius * 1.5f, -radius * 1.2f);
            topFin.lineTo(-radius * 0.5f, -radius * 0.6f);
            topFin.close();
            canvas.drawPath(topFin, paint);

            Path bottomFin = new Path();
            bottomFin.moveTo(-radius, radius * 0.6f);
            bottomFin.lineTo(-radius * 1.5f, radius * 1.2f);
            bottomFin.lineTo(-radius * 0.5f, radius * 0.6f);
            bottomFin.close();
            canvas.drawPath(bottomFin, paint);

            // Highlight stripe
            paint.setColor(Color.WHITE);
            paint.setStrokeWidth(2);
            canvas.drawLine(0, -radius * 0.4f, 0, radius * 0.4f, paint);

            canvas.restore();
        }
    }

    class ElectricEffect {
        float startX, startY, endX, endY;
        // Optimization: Use Path instead of List<float[]> for faster rendering
        Path lightningPath = new Path();
        float opacity = 1.0f;
        float maxDisplacement = 60f;
        int frameCounter = 0;

        ElectricEffect(float x1, float y1, float x2, float y2, int type) {
            this.startX = x1;
            this.startY = y1;
            this.endX = x2;
            this.endY = y2;

            float dist = (float) Math.hypot(x2 - x1, y2 - y1);
            this.maxDisplacement = Math.max(30f, dist / 8f);

            generateSegments();
        }

        private void generateSegments() {
            lightningPath.reset();
            lightningPath.moveTo(startX, startY);
            createElectricLine(startX, startY, endX, endY, maxDisplacement);
        }

        private void createElectricLine(float x1, float y1, float x2, float y2, float disp) {
            // PERFORMANCE: Increased threshold from 15 to 25 to reduce segment count
            if (disp < 25) {
                lightningPath.lineTo(x2, y2);
            } else {
                float midX = (x1 + x2) / 2 + (random.nextFloat() - 0.5f) * disp;
                float midY = (y1 + y2) / 2 + (random.nextFloat() - 0.5f) * disp;
                createElectricLine(x1, y1, midX, midY, disp / 2);
                createElectricLine(midX, midY, x2, y2, disp / 2);
            }
        }

        void update() {
            frameCounter++;
            // PERFORMANCE: Reduced frequency from every 4 frames to every 6 frames
            if (frameCounter % 6 == 0) {
                generateSegments();
            }
            opacity -= 0.05f; // Faster fade for quicker cleanup
        }

        boolean isDead() {
            return opacity <= 0;
        }

        void draw(Canvas canvas, Paint paint) {
            if (opacity <= 0 || lightningPath.isEmpty())
                return;

            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setStrokeJoin(Paint.Join.ROUND);

            // PERFORMANCE: Removed setShadowLayer (kills FPS)
            // Simple two-layer rendering without shadow

            // 1. Outer Glow (no shadow, just thicker line with alpha)
            paint.setStrokeWidth(6.0f);
            int glowAlpha = (int) (opacity * 120); // Reduced from 150
            if (glowAlpha > 255)
                glowAlpha = 255;
            if (glowAlpha < 0)
                glowAlpha = 0;

            paint.setColor(Color.argb(glowAlpha, 0, 200, 255));
            canvas.drawPath(lightningPath, paint);

            // 2. Inner Core
            paint.setStrokeWidth(2.0f);
            int coreAlpha = (int) (opacity * 255);
            if (coreAlpha > 255)
                coreAlpha = 255;

            paint.setColor(Color.argb(coreAlpha, 255, 255, 255));
            canvas.drawPath(lightningPath, paint);
        }
    }

    class BlastWave {
        float x, y, radius = 0, maxRadius;
        int life = 60;
        boolean bossHit = false;

        BlastWave(float x, float y) {
            this.x = x;
            this.y = y;
            this.maxRadius = whiteBall.radius * 30;
        }

        void update() {
            radius = maxRadius * (1 - life / 60f);
            life--;

            // Track if anything was destroyed this frame
            boolean ballsDestroyed = false;
            boolean projectilesDestroyed = false;

            // Boss Check (Squared Distance Optimization)
            if (!bossHit && currentBoss != null) {
                float dx = currentBoss.x - x;
                float dy = currentBoss.y - y;
                float distSq = dx * dx + dy * dy;
                float hitDist = radius + currentBoss.radius;
                if (distSq < hitDist * hitDist) {
                    currentBoss.hp -= 30;
                    floatingTexts.add(new FloatingText("-30", currentBoss.x, currentBoss.y, Color.rgb(255, 165, 0)));
                    bossHit = true;
                }
            }

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
                    ballsDestroyed = true;
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
                    // Quest 2: Dark Matter (50 black balls)
                    // Quest 5: Destroyer II (200 balls total)
                    if (questManager != null) {
                        questManager.incrementQuestProgress(2, 1);
                        questManager.incrementQuestProgress(5, 1);
                    }
                    ballsDestroyed = true;
                }
            }

            // Feature: Blast Wave Destroys Boss Projectiles
            if (!bossProjectiles.isEmpty()) {
                for (int i = bossProjectiles.size() - 1; i >= 0; i--) {
                    Ball proj = bossProjectiles.get(i);
                    float dx = x - proj.x;
                    float dy = y - proj.y;
                    float distance = (float) Math.sqrt(dx * dx + dy * dy);

                    if (distance < radius) {
                        bossProjectiles.remove(i);
                        // Visual feedback
                        createImpactBurst(proj.x, proj.y, Color.rgb(255, 69, 0));
                        projectilesDestroyed = true;
                    }
                }
            }

            // PERFORMANCE FIX: Play sounds once per frame instead of per ball
            if (ballsDestroyed) {
                playSound(soundCollision);
            }
            if (projectilesDestroyed) {
                playSound(soundBlackExplosion);
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
            // paint.setShadowLayer(20, 0, 0, Color.RED); // Removed for performance (Lag
            // fix)
            canvas.drawCircle(x, y, radius, paint);
            // paint.clearShadowLayer();
            paint.setAlpha(255);
        }
    }

    // VORTEX PASSIVE ABILITY - Pulls in and destroys projectiles and black balls
    class Vortex {
        float x, y;
        float pullRadius = 0;
        float maxRadius = 200;
        int life = 200; // ~2.5 seconds at 60fps
        float rotationAngle = 0;

        Vortex(float x, float y) {
            this.x = x;
            this.y = y;
        }

        void update() {
            // Expand radius over time - FIXED: starts at maxRadius, shrinks as life
            // decreases
            pullRadius = maxRadius * (1 - (life / 200f) * 0.3f); // Starts at 100%, goes to 70%
            life--;
            rotationAngle += 10; // Rotate spiral

            // Follow player position
            if (whiteBall != null) {
                x = whiteBall.x;
                y = whiteBall.y;
            }

            // Track if anything was pulled
            boolean soundPlayed = false;

            // Pull and destroy boss projectiles
            for (int i = bossProjectiles.size() - 1; i >= 0; i--) {
                Ball proj = bossProjectiles.get(i);
                float dx = x - proj.x;
                float dy = y - proj.y;
                float distance = (float) Math.sqrt(dx * dx + dy * dy);

                if (distance < pullRadius) {
                    // Pull toward center with increasing strength
                    float pullStrength = 20f + (150 - life) * 0.3f; // Gets stronger over time
                    float angle = (float) Math.atan2(dy, dx);
                    proj.vx = (float) Math.cos(angle) * pullStrength;
                    proj.vy = (float) Math.sin(angle) * pullStrength;

                    // Destroy if close to center
                    if (distance < 50) {
                        bossProjectiles.remove(i);
                        createImpactBurst(proj.x, proj.y, Color.CYAN);
                        if (!soundPlayed) {
                            playSound(soundBlackExplosion);
                            soundPlayed = true;
                        }
                    }
                }
            }

            // Pull and destroy black balls with SHRINKING EFFECT
            for (int i = blackBalls.size() - 1; i >= 0; i--) {
                Ball ball = blackBalls.get(i);
                float dx = x - ball.x;
                float dy = y - ball.y;
                float distance = (float) Math.sqrt(dx * dx + dy * dy);

                if (distance < pullRadius) {
                    // Pull toward center with strong force
                    float pullStrength = 18f + (150 - life) * 0.4f;
                    float angle = (float) Math.atan2(dy, dx);
                    ball.vx = (float) Math.cos(angle) * pullStrength;
                    ball.vy = (float) Math.sin(angle) * pullStrength;

                    // SHRINKING EFFECT: Ball gets smaller as it gets closer
                    float shrinkFactor = distance / pullRadius; // 1.0 at edge, 0.0 at center
                    float originalRadius = whiteBall.radius * 1.2f; // Black ball default size
                    ball.radius = originalRadius * (0.3f + shrinkFactor * 0.7f); // Shrinks to 30% size

                    // Destroy if very close to center OR very small
                    if (distance < 60 || ball.radius < originalRadius * 0.4f) {
                        blackBalls.remove(i);
                        score += 5;
                        // Quest 2: Dark Matter & Quest 5: Destroyer II
                        if (questManager != null) {
                            questManager.incrementQuestProgress(2, 1);
                            questManager.incrementQuestProgress(5, 1);
                        }
                        createParticles(ball.x, ball.y, Color.BLACK);
                        createImpactBurst(ball.x, ball.y, Color.CYAN);
                        if (!soundPlayed) {
                            playSound(soundCollision);
                            soundPlayed = true;
                        }
                    }
                }
            }
        }

        boolean isDead() {
            return life <= 0;
        }

        void draw(Canvas canvas, Paint paint) {
            float alpha = life / 150f;

            // Draw spiral vortex effect
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(6);
            paint.setColor(Color.CYAN);
            paint.setAlpha((int) (200 * alpha));

            // Draw multiple spiral rings
            for (int i = 0; i < 3; i++) {
                float ringRadius = pullRadius * (0.3f + i * 0.35f);
                int segments = 20;
                Path spiral = new Path();

                for (int j = 0; j <= segments; j++) {
                    float angle = (float) ((j / (float) segments) * Math.PI * 2
                            + Math.toRadians(rotationAngle + i * 120));
                    float spiralX = x + (float) Math.cos(angle) * ringRadius;
                    float spiralY = y + (float) Math.sin(angle) * ringRadius;

                    if (j == 0) {
                        spiral.moveTo(spiralX, spiralY);
                    } else {
                        spiral.lineTo(spiralX, spiralY);
                    }
                }
                canvas.drawPath(spiral, paint);
            }

            // Draw center glow
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.CYAN);
            paint.setAlpha((int) (100 * alpha));
            canvas.drawCircle(x, y, 20, paint);

            // Draw outer ring
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(4);
            paint.setColor(Color.CYAN);
            paint.setAlpha((int) (150 * alpha));
            canvas.drawCircle(x, y, pullRadius, paint);

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

    class TrailPoint {
        float x, y, radius;

        TrailPoint(float x, float y, float radius) {
            this.x = x;
            this.y = y;
            this.radius = radius;
        }
    }

    // Floating Text Class for animated UI labels
    class FloatingText {
        String text;
        float x, y;
        int color;
        int life = 60; // Frames
        int maxLife = 60;
        float floatSpeed = 3f;
        float sizeScale = 1.0f;

        FloatingText(String text, float x, float y, int color) {
            this(text, x, y, color, 1.0f);
        }

        FloatingText(String text, float x, float y, int color, float sizeScale) {
            this.text = text;
            this.x = x;
            this.y = y;
            this.color = color;
            this.sizeScale = sizeScale;
        }

        void update() {
            y -= floatSpeed; // Move up
            life--;
        }

        boolean isDead() {
            return life <= 0;
        }

        // Reset methods for object pooling
        FloatingText reset(String text, float x, float y, int color) {
            return reset(text, x, y, color, 1.0f);
        }

        FloatingText reset(String text, float x, float y, int color, float sizeScale) {
            this.text = text;
            this.x = x;
            this.y = y;
            this.color = color;
            this.sizeScale = sizeScale;
            this.life = 60;
            this.maxLife = 60;
            this.floatSpeed = 3f;
            return this;
        }

        void draw(Canvas canvas, Paint paint) {
            paint.setStyle(Paint.Style.FILL);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD_ITALIC));

            // Calculate sizes and alpha
            float ratio = (float) life / maxLife;
            paint.setTextSize(screenWidth * 0.12f * sizeScale * (0.8f + 0.2f * ratio)); // Shrink slightly as it fades?
            // Or grow?
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

    // --- Unique Projectile Classes ---
    class PlasmaBullet extends Ball {
        public PlasmaBullet(float x, float y, float r, int color) {
            super(x, y, r, color);
        }
    }

    class SolarBolt extends Ball {
        public SolarBolt(float x, float y, float r, int color) {
            super(x, y, r, color);
        }
    }

    class MistShard extends Ball {
        public MistShard(float x, float y, float r, int color) {
            super(x, y, r, color);
        }
    }

    class GravityOrb extends Ball {
        public GravityOrb(float x, float y, float r, int color) {
            super(x, y, r, color);
        }
    }

    class IceSpike extends Ball {
        public IceSpike(float x, float y, float r, int color) {
            super(x, y, r, color);
        }
    }

    // --- COLLISION HELPER METHODS ---

    class GeoRock extends Ball {
        public GeoRock(float x, float y, float r, int color) {
            super(x, y, r, color);
        }
    }

    class AcidBlob extends Ball {
        public AcidBlob(float x, float y, float r, int color) {
            super(x, y, r, color);
        }
    }

    class ClockGear extends Ball {
        public ClockGear(float x, float y, float r, int color) {
            super(x, y, r, color);
        }
    }

    class MagmaPatch {
        float x, y, radius;
        long createTime;
        long duration = 3000;

        MagmaPatch(float x, float y, float r) {
            this.x = x;
            this.y = y;
            this.radius = r;
            this.createTime = System.currentTimeMillis();
        }

        void update() {
            // Generate Flame Particles (Combo-like effect)
            if (random.nextFloat() < 0.2f) {
                createFlame(x + (random.nextFloat() - 0.5f) * radius, y + (random.nextFloat() - 0.5f) * radius);
            }
        }

        boolean isDead() {
            return System.currentTimeMillis() - createTime > duration;
        }

        void draw(Canvas canvas) {
            // Drawn via particles mostly, but keep a faint glow base
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.rgb(255, 50, 0));
            paint.setAlpha(30);
            canvas.drawCircle(x, y, radius, paint);
            paint.setAlpha(255);
        }
    }

    class Boss {
        String name;
        float x, y, radius;
        float vx, vy; // Velocity fields re-added
        float hp, maxHp;
        int color;
        long lastStateChangeTime;
        long lastEventHorizonTime = 0; // New timer for periodic abilities
        int state = 0; // 0: Idle/Shoot, 1: Dash, 2: Burst
        long lastAttackTime;
        float dashTargetX, dashTargetY;
        boolean dashing = false;
        boolean charging = false;
        long chargeStartTime;
        long chargeDuration = 1000; // 1 second charging time
        float hoverCenterX, hoverCenterY;

        long lastMoveTime = 0;
        float moveTargetX, moveTargetY;

        Boss(String name, float maxHp, int color) {
            this.name = name;
            this.maxHp = maxHp;
            this.hp = maxHp;
            this.color = color;
            this.x = centerX;
            this.y = centerY - circleRadius * 0.65f;
            this.hoverCenterX = x;
            this.hoverCenterY = y;
            // Initialize movement target to current position
            this.moveTargetX = x;
            this.moveTargetY = y;

            this.radius = circleRadius * 0.25f;
            this.lastStateChangeTime = System.currentTimeMillis();
            this.lastAttackTime = System.currentTimeMillis(); // CRITICAL: Initialize attack timer

        }

        void update() {
            long now = System.currentTimeMillis();

            // Generic Charging Logic - Only for bosses that rely on default behaviors or
            // strictly use this flag
            // We DO NOT return early here because specific bosses (Lunar, Void) need their
            // update methods for state machines.
            if (charging) {
                if (now - chargeStartTime > chargeDuration) {
                    // Check if this is a generic charge (not managed by specific boss state)
                    // If boss state is 0, we can fire default.
                    // But Void Titan & Lunar manage their own states.
                    if (!name.equals("VOID TITAN") && !name.equals("LUNAR CONSTRUCT")) {
                        charging = false;
                        shootDefaultProjectile();
                        lastAttackTime = now;
                    }
                }
            }

            // Default Projectile Attack Trigger (For bosses without complex state machines)
            // Void Titan & Lunar have their own triggers.
            if (!name.equals("VOID TITAN") && !name.equals("LUNAR CONSTRUCT")) {
                if (now - lastAttackTime > 3000 && !charging) {
                    charging = true;
                    chargeStartTime = now;
                }
            }

            // State Machine based on Boss Type
            if (name.equals("VOID TITAN") || name.equals("Boss")) {
                updateVoidTitan(now);
            } else if (name.equals("LUNAR CONSTRUCT")) {
                updateLunarConstruct(now);
            } else if (name.equals("SOLARION")) {
                updateSolarion(now);
            } else if (name.equals("NEBULON")) {
                updateNebulon(now);
            } else if (name.equals("GRAVITON")) {
                updateGraviton(now);
            } else if (name.equals("MECHA-CORE")) {
                updateMechaCore(now);
            } else if (name.equals("CRYO-STASIS")) {
                updateCryoStasis(now);
            } else if (name.equals("GEO-BREAKER")) {
                updateGeoBreaker(now);
            } else if (name.equals("BIO-HAZARD")) {
                updateBioHazard(now);
            } else if (name.equals("CHRONO-SHIFTER")) {
                updateChronoShifter(now);
            }
        }

        void shootDefaultProjectile() {
            if (whiteBall == null)
                return;
            // Logic per boss to choose proj type
            if (name.equals("SOLARION"))
                shootSolarBolt();
            else if (name.equals("NEBULON"))
                shootMistShard();
            else if (name.equals("GRAVITON"))
                shootGravityWell();
            else if (name.equals("MECHA-CORE"))
                shootPlasmaBullet();
            else if (name.equals("CRYO-STASIS"))
                shootIceSpike();
            else if (name.equals("GEO-BREAKER"))
                shootRockThrow();
            else if (name.equals("BIO-HAZARD"))
                shootAcidBlob();
            else if (name.equals("CHRONO-SHIFTER"))
                shootClockHand();
            else
                shootVoidProjectile(); // Default fallback
        }

        // --- NEW BOSS UPDATE METHODS ---

        private void updateSolarion(long now) {
            // Hover logic: Figure-8 Movement
            x = hoverCenterX + (float) Math.cos(now * 0.0008) * 60; // Wide Horizontal
            y = hoverCenterY + (float) Math.sin(now * 0.0012) * 30; // Vertical Bob

            // --- PASSIVE: HEAT AURA (Burns balls near it) ---
            // Slow damage if player is too close
            if (whiteBall != null) {
                float dist = (float) Math.sqrt(Math.pow(x - whiteBall.x, 2) + Math.pow(y - whiteBall.y, 2));
                if (dist < radius * 2.5f) {
                    // Heat Damage
                    if (now % 60 == 0) { // Approx once per second
                        playerHp -= 8;
                        createParticles(whiteBall.x, whiteBall.y, Color.rgb(255, 100, 0));
                        floatingTexts
                                .add(new FloatingText("BURN", whiteBall.x, whiteBall.y - 20, Color.rgb(255, 69, 0)));
                    }
                }
            }

            // Phases based on HP
            int phase = 1;
            if (hp < maxHp * 0.3)
                phase = 3;
            else if (hp < maxHp * 0.7)
                phase = 2;

            if (phase == 2) {
                // Solar Flare Ring every 5s
                if (now - lastStateChangeTime > 3000 + random.nextInt(3000)) { // Random 3-6s
                    doSolarFlare();
                    lastStateChangeTime = now;
                }
            } else if (phase == 3) {
                // Supernova - Erratic movement
                x = hoverCenterX + (random.nextFloat() - 0.5f) * 20;
                y = hoverCenterY + (random.nextFloat() - 0.5f) * 20;
                if (now - lastStateChangeTime > 1500 + random.nextInt(1500)) { // Random 1.5-3s
                    doSupernova();
                    lastStateChangeTime = now;
                }
            }
        }

        private void updateNebulon(long now) {
            int phase = 1;
            if (hp < maxHp * 0.3)
                phase = 3;
            else if (hp < maxHp * 0.7)
                phase = 2;

            // Phase 1: Drifting Mist
            if (phase == 1) {
                float driftSpeed = 0.5f;
                x += (random.nextFloat() - 0.5f) * driftSpeed;
                y += (random.nextFloat() - 0.5f) * driftSpeed;
            }

            // --- PASSIVE: MIST FRICTION (Slows all balls) ---
            // Simulates dense nebula
            float friction = 0.99f; // Standard
            friction = 0.96f; // Higher drag

            if (whiteBall != null) {
                whiteBall.vx *= friction;
                whiteBall.vy *= friction;
            }
            for (Ball b : coloredBalls) {
                b.vx *= friction;
                b.vy *= friction;
            }

            // Phase 2 & 3: Teleport
            long teleportInterval = (phase == 3) ? 1500 : 4000;
            if (now - lastStateChangeTime > teleportInterval) {
                // Teleport
                float angle = random.nextFloat() * 6.28f;
                float dist = random.nextFloat() * (circleRadius * 0.6f);
                x = centerX + (float) Math.cos(angle) * dist;
                y = centerY + (float) Math.sin(angle) * dist;

                // Surprise Shot
                shootMistShard();

                lastStateChangeTime = now;
                playSound(soundTeleport); // Assuming soundTeleport exists or fallback
            }
        }

        private void updateGraviton(long now) {
            // Movement: Random Directions (User Request)
            if (now - lastMoveTime > 2000) { // Change direction every 2 seconds
                // Random target within 80% of radius
                float angle = random.nextFloat() * 6.28f;
                float dist = random.nextFloat() * (circleRadius * 0.8f);
                moveTargetX = centerX + (float) Math.cos(angle) * dist;
                moveTargetY = centerY + (float) Math.sin(angle) * dist;
                lastMoveTime = now;
            }
            // Smooth movement towards target
            float moveDx = moveTargetX - x;
            float moveDy = moveTargetY - y;
            x += moveDx * 0.02f;
            y += moveDy * 0.02f;

            // Phase Logic
            int phase = 1;
            if (hp < maxHp * 0.3)
                phase = 3;
            else if (hp < maxHp * 0.7)
                phase = 2;

            // Passive: Pull White Ball
            float pullStrength = 0.05f + (phase * 0.03f); // Increases with phase
            if (whiteBall != null) {
                float dx = x - whiteBall.x;
                float dy = y - whiteBall.y;
                float dist = (float) Math.sqrt(dx * dx + dy * dy);
                if (dist > 10) {
                    whiteBall.vx += (dx / dist) * pullStrength;
                    whiteBall.vy += (dy / dist) * pullStrength;
                }
            }

            // Phase 2: Active Repulsion Bursts
            if (phase >= 2 && now - lastAttackTime > 4000) {
                // Push all balls AWAY strongly
                for (Ball b : coloredBalls) {
                    float dx = b.x - x;
                    float dy = b.y - y;
                    float dist = (float) Math.sqrt(dx * dx + dy * dy);
                    if (dist < 300) {
                        b.vx += (dx / dist) * 15;
                        b.vy += (dy / dist) * 15;
                    }
                }
                createImpactBurst(x, y, Color.MAGENTA);
                lastAttackTime = now;
            }

            // Phase 3: Spaghettification (High speed straight beam) & Event Horizon
            // Expansion
            if (phase == 3) {
                // TIMING LOGIC: 5 seconds cycle (2.5s Active, 2.5s Cooldown)
                if (lastEventHorizonTime == 0)
                    lastEventHorizonTime = now;

                boolean isEventHorizonActive = (now - lastEventHorizonTime) < 2500; // Active for first 2.5s

                if (now - lastEventHorizonTime > 5000) {
                    lastEventHorizonTime = now; // Reset cycle
                    isEventHorizonActive = true;
                    playSound(soundPower); // Sound cue for activation
                }

                if (isEventHorizonActive) {
                    // 1. Expand Event Horizon (Visual + Logic)
                    float eventHorizonRadius = radius * 3.5f; // Expanded radius

                    // Visual Warning (Red Circle expanding/pulsing)
                    createParticles(x, y, Color.RED); // Simple visual feedback

                    // 2. Strong Pull Player
                    if (whiteBall != null) {
                        float dx = x - whiteBall.x;
                        float dy = y - whiteBall.y;
                        float dist = (float) Math.sqrt(dx * dx + dy * dy);

                        // Stronger pull in Phase 3 (User requested "much faster")
                        if (dist > 10) {
                            float pullForce = 0.8f; // DRASTICALLY INCREASED from 0.25f
                            whiteBall.vx += (dx / dist) * pullForce;
                            whiteBall.vy += (dy / dist) * pullForce;
                        }

                        // 3. Event Horizon Damage & Friction
                        if (dist < eventHorizonRadius) {
                            // High Friction (Slows down escape)
                            whiteBall.vx *= 0.9f;
                            whiteBall.vy *= 0.9f;

                            if (now % 20 == 0) { // Fast Damage tick (approx 3/sec)
                                playerHp -= 15; // Increased damage
                                createParticles(whiteBall.x, whiteBall.y, Color.RED);
                                floatingTexts.add(new FloatingText("-15", whiteBall.x, whiteBall.y - 50, Color.RED));
                            }
                        }
                    }
                }

                if (now - lastStateChangeTime > 2000 + random.nextInt(2000)) { // Random 2-4s
                    shootGravityWell(); // Fires double/triple
                    shootGravityWell();
                    lastStateChangeTime = now;
                }
            }

            // --- PASSIVE: Black Hole Pull (Consumes Special Balls) ---
            if (specialBalls != null) {
                for (int i = specialBalls.size() - 1; i >= 0; i--) {
                    SpecialBall sb = specialBalls.get(i);
                    float dx = x - sb.x;
                    float dy = y - sb.y;
                    float dist = (float) Math.sqrt(dx * dx + dy * dy);

                    if (dist < radius) {
                        // Consume logic
                        specialBalls.remove(i);
                        // Heal boss
                        if (hp < maxHp) {
                            int healAmount = 50;
                            hp = Math.min(hp + healAmount, maxHp);
                            floatingTexts.add(new FloatingText("+" + healAmount, x, y - 50, Color.GREEN)); // Heal
                            // Visual
                        }
                        continue;
                    }

                    // Strong Pull
                    if (dist < circleRadius * 0.8f) {
                        float force = 15.0f / (dist + 1); // Inverse square-ish
                        sb.vx += (dx / dist) * force;
                        sb.vy += (dy / dist) * force;
                    }
                }
            }
        }

        private void updateMechaCore(long now) {
            int phase = 1;
            if (hp < maxHp * 0.3)
                phase = 3;
            else if (hp < maxHp * 0.7)
                phase = 2;

            // Movement
            if (phase >= 1) {
                // Robotic/Grid Movement
                if (Math.abs(x - moveTargetX) < 10 && Math.abs(y - moveTargetY) < 10) {
                    // Snap to grid point
                    float gridSize = circleRadius * 0.4f;
                    int gridX = random.nextInt(3) - 1; // -1, 0, 1
                    int gridY = random.nextInt(3) - 1;
                    moveTargetX = centerX + gridX * gridSize;
                    moveTargetY = centerY + gridY * gridSize;
                }
                // Linear robotic lerp (constant speed)
                float dx = moveTargetX - x;
                float dy = moveTargetY - y;
                float dist = (float) Math.sqrt(dx * dx + dy * dy);
                if (dist > 0) {
                    float speed = 2.0f; // Slow heavy mech
                    x += (dx / dist) * speed;
                    y += (dy / dist) * speed;
                }
            }

            // --- PASSIVE: MAGNETIC FIELD (Repels balls slightly) ---
            if (whiteBall != null) {
                float dist = (float) Math.sqrt(Math.pow(x - whiteBall.x, 2) + Math.pow(y - whiteBall.y, 2));
                if (dist < radius * 2.0f) {
                    // Magnetic push
                    float force = 0.5f;
                    float dx = whiteBall.x - x;
                    float dy = whiteBall.y - y;
                    whiteBall.vx += (dx / dist) * force;
                    whiteBall.vy += (dy / dist) * force;
                }
            }

            // Attacks
            if (phase == 2 && now - lastStateChangeTime > 2000 + random.nextInt(2000)) { // Random 2-4s
                shootPlasmaBullet();
                shootPlasmaBullet();
                shootPlasmaBullet();
                lastStateChangeTime = now;
            } else if (phase == 3 && now - lastStateChangeTime > 1000 + random.nextInt(1500)) { // Random 1-2.5s
                // Homing Swarm
                for (int i = 0; i < 3; i++)
                    shootPlasmaBullet();
                lastStateChangeTime = now;
            }
        }

        // --- NEW BOSS UPDATES (GROUP 2) ---

        private void updateCryoStasis(long now) {
            // Enhanced Movement (Figure 8 Pattern or drifting)
            x = hoverCenterX + (float) Math.sin(now * 0.001) * 150; // Wide horizontal
            y = hoverCenterY + (float) Math.cos(now * 0.002) * 50; // Gentle vertical wave

            int phase = 1;
            if (hp < maxHp * 0.3)
                phase = 3;
            else if (hp < maxHp * 0.7)
                phase = 2;

            if (phase >= 2) {
                // Freezing Breath (Cone)
                if (now - lastStateChangeTime > 3000 + random.nextInt(2000)) { // Random 3-5s
                    doFreezingBreath();
                    lastStateChangeTime = now;
                }
            }
            if (phase == 3) {
                // Blizzard (Simulated by many small projectiles)
                if (random.nextInt(100) < 5) { // 5% chance per frame
                    float r = random.nextFloat() * circleRadius;
                    float a = random.nextFloat() * 6.28f;
                    Ball snow = new Ball(centerX + (float) Math.cos(a) * r, -50, 5, Color.WHITE);
                    snow.vy = 5;
                    bossProjectiles.add(snow);
                }
            }

            // --- PROXIMITY FREEZE MECHANIC ---
            if (whiteBall != null && !isFrozen) {
                float dist = (float) Math.sqrt(Math.pow(x - whiteBall.x, 2) + Math.pow(y - whiteBall.y, 2));
                float freezeRange = radius * 3.0f; // Proximity freeze range

                if (dist < freezeRange) {
                    // Player is in freeze zone
                    if (freezeProximityStartTime == 0) {
                        // Start proximity timer
                        freezeProximityStartTime = now;
                        floatingTexts.add(new FloatingText("FREEZING...", whiteBall.x, whiteBall.y - 80, Color.CYAN));
                    }

                    // Check if 2 seconds passed
                    if (now - freezeProximityStartTime > 2000) {
                        // FREEZE PLAYER
                        isFrozen = true;
                        frozenEndTime = now + 2000; // Frozen for 2 seconds
                        whiteBall.vx = 0;
                        whiteBall.vy = 0;

                        // Ice encasement effect
                        for (int i = 0; i < 30; i++) {
                            float angle = i * (2f * (float) Math.PI / 30);
                            particles.add(new Particle(
                                    whiteBall.x + (float) Math.cos(angle) * whiteBall.radius * 2,
                                    whiteBall.y + (float) Math.sin(angle) * whiteBall.radius * 2,
                                    angle, 0.5f, Color.CYAN, ParticleType.STAR));
                        }
                        floatingTexts.add(
                                new FloatingText("FROZEN!", whiteBall.x, whiteBall.y - 100, Color.rgb(100, 200, 255)));
                        playSound(soundFreeze);
                        freezeProximityStartTime = 0; // Reset
                    } else {
                        // Still freezing - slow player
                        whiteBall.vx *= 0.90f;
                        whiteBall.vy *= 0.90f;

                        // Visual frost buildup
                        if (random.nextFloat() < 0.4f) {
                            createParticles(whiteBall.x, whiteBall.y, Color.CYAN);
                        } else {
                            // Normal collisions
                            // Bounce logic...
                        }
                    }
                } else {
                    // Out of range - reset timer
                    if (freezeProximityStartTime != 0) {
                        freezeProximityStartTime = 0;
                    }
                }
            }
        }

        private void updateGeoBreaker(long now) {
            // Heavy Stomps: Moves in jumps
            if (now - lastStateChangeTime > 1500 + random.nextInt(1500)) { // Random 1.5-3s
                // Jump to new location
                float angle = random.nextFloat() * 6.28f;
                float dist = random.nextFloat() * (circleRadius * 0.5f);
                moveTargetX = centerX + (float) Math.cos(angle) * dist;
                moveTargetY = centerY + (float) Math.sin(angle) * dist;

                // "Stomp" - Instant move with screen shake
                x = moveTargetX;
                y = moveTargetY;
                shakeEndTime = System.currentTimeMillis() + 300; // Screen Shake

                // Attack on landing
                shootRockThrow();

                lastStateChangeTime = now;
            }

            // Phase Logic for Attacks
            int phase = 1;
            if (hp < maxHp * 0.3)
                phase = 3;
            else if (hp < maxHp * 0.7)
                phase = 2;

            if (phase >= 2 && now - lastAttackTime > 5000) { // Separate timer for fracture
                doFracture();
                lastAttackTime = now;
            }

            // --- PASSIVE: Magma Leaks (Spawns magma patches passively) ---
            if (now % 200 == 0 && random.nextFloat() < 0.1f) {
                // Rare magma spawn under boss
                magmaPatches.add(new MagmaPatch(x, y, radius * 0.5f));
            }
        }

        private void updateBioHazard(long now) {
            // Erratic Blob Movement
            float wobbleX = (float) Math.sin(now * 0.005) * 50;
            float wobbleY = (float) Math.cos(now * 0.007) * 50;

            // Slide towards random points smoothly but unevenly
            if (random.nextFloat() < 0.02f) {
                moveTargetX = centerX + (random.nextFloat() - 0.5f) * circleRadius;
                moveTargetY = centerY + (random.nextFloat() - 0.5f) * circleRadius;
            }

            x += (moveTargetX - x) * 0.02f + wobbleX * 0.01f;
            y += (moveTargetY - y) * 0.02f + wobbleY * 0.01f;

            // --- PASSIVE: TOXIC TRAIL ---
            if (now % 20 == 0) {
                // Buffed: Larger blobs, higher damage
                AcidBlob trail = new AcidBlob(x, y, 15, Color.GREEN); // Size 15
                trail.vx = 0;
                trail.vy = 0;
                bossProjectiles.add(trail);
            }

            int phase = 1;
            if (hp < maxHp * 0.3)
                phase = 3;
            else if (hp < maxHp * 0.7)
                phase = 2;

            if (phase >= 2 && now - lastStateChangeTime > 4000 + random.nextInt(3000)) { // Random 4-7s
                // Toxic Pool (Cluster of blobs)
                for (int i = 0; i < 5; i++)
                    shootAcidBlob();
                lastStateChangeTime = now;
            }
            if (phase == 3) {
                // Plague Spray
                if (now % 20 == 0) { // Frequent spray
                    float angle = (now * 0.005f) % 6.28f;
                    Ball proj = new Ball(x, y, 10, Color.GREEN);
                    proj.vx = (float) Math.cos(angle) * 8;
                    proj.vy = (float) Math.sin(angle) * 8;
                    bossProjectiles.add(proj);
                }
            }
        }

        private void updateChronoShifter(long now) {
            // Ticks: Teleports short distances every second (Tick-Tock)
            if (now - lastStateChangeTime > 800 + random.nextInt(700)) { // Random 0.8-1.5s
                float angle = (float) Math.atan2(y - centerY, x - centerX);
                angle += (Math.PI / 6); // Move 30 degrees (1 hour on clock)
                float dist = circleRadius * 0.6f;

                x = centerX + (float) Math.cos(angle) * dist;
                y = centerY + (float) Math.sin(angle) * dist;

                shootClockHand();
                lastStateChangeTime = now;
            }

            // --- PASSIVE: PHASE SHIFT (Chance to teleport when hit) ---
            if (now % 100 == 0 && random.nextFloat() < 0.05f) {
                // Mini blink
                x += (random.nextFloat() - 0.5f) * 20;
                y += (random.nextFloat() - 0.5f) * 20;
            }

            int phase = 1;
            if (hp < maxHp * 0.3)
                phase = 3;
            else if (hp < maxHp * 0.7)
                phase = 2;

            if (phase >= 2 && now - lastAttackTime > 5000) {
                // Echo Projectiles
                doEchoShot();
                lastAttackTime = now;
            }
        }

        // --- NEW BOSS ATTACK METHODS ---

        void shootVoidProjectile() {
            if (whiteBall != null) {
                float angle = (float) Math.atan2(whiteBall.y - y, whiteBall.x - x);
                shootVoidProjectile(Math.toDegrees(angle));
            }
        }

        void shootVoidProjectile(double angleDeg) {
            float angle = (float) Math.toRadians(angleDeg);
            float speed = 12;
            VoidProjectile proj = new VoidProjectile(x, y, 25);
            proj.vx = (float) Math.cos(angle) * speed;
            proj.vy = (float) Math.sin(angle) * speed;
            bossProjectiles.add(proj);
            playSound(soundRetroLaser); // Or explicit void sound
        }

        void shootSolarBolt() {
            if (whiteBall == null)
                return;
            // fireProjectile(whiteBall, 15, Color.rgb(255, 140, 0), 20);
            float angle = (float) Math.atan2(whiteBall.y - y, whiteBall.x - x);
            float speed = 15;
            Ball proj = new SolarBolt(x, y, 20, Color.rgb(255, 140, 0));
            proj.vx = (float) Math.cos(angle) * speed;
            proj.vy = (float) Math.sin(angle) * speed;
            bossProjectiles.add(proj);
            playSound(soundRetroLaser);
        }

        void doSolarFlare() {
            for (int i = 0; i < 12; i++) {
                float angle = (float) (i * Math.PI / 6);
                float px = x + (float) Math.cos(angle) * (radius * 1.2f);
                float py = y + (float) Math.sin(angle) * (radius * 1.2f);
                Ball proj = new SolarBolt(px, py, 15, Color.rgb(255, 69, 0));
                proj.vx = (float) Math.cos(angle) * 8;
                proj.vy = (float) Math.sin(angle) * 8;
                bossProjectiles.add(proj);
            }
            playSound(soundBlackExplosion);
        }

        void doSupernova() {
            for (int i = 0; i < 8; i++) {
                float angle = random.nextFloat() * 6.28f;
                float px = x + (float) Math.cos(angle) * (radius * 1.2f);
                float py = y + (float) Math.sin(angle) * (radius * 1.2f);
                Ball proj = new SolarBolt(px, py, 30, Color.RED);
                proj.vx = (random.nextFloat() - 0.5f) * 20;
                proj.vy = (random.nextFloat() - 0.5f) * 20;
                bossProjectiles.add(proj);
            }
        }

        void shootMistShard() {
            if (whiteBall == null)
                return;
            float angle = (float) Math.atan2(whiteBall.y - y, whiteBall.x - x);
            float speed = 12;
            Ball proj = new MistShard(x, y, 15, Color.rgb(221, 160, 221));
            proj.vx = (float) Math.cos(angle) * speed;
            proj.vy = (float) Math.sin(angle) * speed;
            bossProjectiles.add(proj);
        }

        void shootGravityWell() {
            if (whiteBall == null)
                return;
            float angle = (float) Math.atan2(whiteBall.y - y, whiteBall.x - x);
            float speed = 10;
            Ball proj = new GravityOrb(x, y, 35, Color.rgb(25, 25, 112));
            proj.vx = (float) Math.cos(angle) * speed;
            proj.vy = (float) Math.sin(angle) * speed;
            bossProjectiles.add(proj);
        }

        void shootPlasmaBullet() {
            if (whiteBall == null)
                return;
            float angle = (float) Math.atan2(whiteBall.y - y, whiteBall.x - x);
            float speed = 15;
            Ball proj = new PlasmaBullet(x, y, 20, Color.CYAN);
            proj.vx = (float) Math.cos(angle) * speed;
            proj.vy = (float) Math.sin(angle) * speed;
            bossProjectiles.add(proj);
            playSound(soundRetroLaser);
        }

        void shootIceSpike() {
            if (whiteBall == null)
                return;
            float angle = (float) Math.atan2(whiteBall.y - y, whiteBall.x - x);
            float speed = 15;
            Ball proj = new IceSpike(x, y, 15, Color.CYAN);
            proj.vx = (float) Math.cos(angle) * speed;
            proj.vy = (float) Math.sin(angle) * speed;
            bossProjectiles.add(proj);
            playSound(soundFreeze);
        }

        void doFreezingBreath() {
            for (int i = -2; i <= 2; i++) {
                if (whiteBall == null)
                    break;
                float angle = (float) Math.atan2(whiteBall.y - y, whiteBall.x - x);
                angle += i * 0.1f;
                Ball proj = new IceSpike(x, y, 10, Color.WHITE);
                proj.vx = (float) Math.cos(angle) * 15;
                proj.vy = (float) Math.sin(angle) * 15;
                bossProjectiles.add(proj);
            }
            playSound(soundFreeze);
        }

        void shootRockThrow() {
            if (whiteBall != null) {
                float angle = (float) Math.atan2(whiteBall.y - y, whiteBall.x - x);
                float speed = 10;
                Ball proj = new GeoRock(x, y, 30, Color.DKGRAY);
                proj.vx = (float) Math.cos(angle) * speed;
                proj.vy = (float) Math.sin(angle) * speed;
                bossProjectiles.add(proj);
            } else {
                Ball proj = new GeoRock(x, y, 30, Color.DKGRAY);
                proj.vx = (random.nextFloat() - 0.5f) * 10;
                proj.vy = 10;
                bossProjectiles.add(proj);
            }
        }

        void doFracture() {
            for (int i = 0; i < 6; i++) {
                shootRockThrow();
            }
            playSound(soundBlackExplosion);
        }

        void shootAcidBlob() {
            if (whiteBall == null)
                return;
            float angle = (float) Math.atan2(whiteBall.y - y, whiteBall.x - x);
            float speed = 10;
            Ball proj = new AcidBlob(x, y, 20, Color.GREEN);
            proj.vx = (float) Math.cos(angle) * speed;
            proj.vy = (float) Math.sin(angle) * speed;
            bossProjectiles.add(proj);
        }

        void shootClockHand() {
            if (whiteBall == null)
                return;
            float angle = (float) Math.atan2(whiteBall.y - y, whiteBall.x - x);
            float speed = 15;
            Ball proj = new ClockGear(x, y, 18, Color.YELLOW);
            proj.vx = (float) Math.cos(angle) * speed;
            proj.vy = (float) Math.sin(angle) * speed;
            bossProjectiles.add(proj);
        }

        void doEchoShot() {
            for (int i = 0; i < 4; i++) {
                if (whiteBall == null)
                    break;
                float angle = (float) (i * Math.PI / 2);
                Ball proj = new ClockGear(x, y, 15, Color.rgb(255, 215, 0));
                proj.vx = (float) Math.cos(angle) * 10;
                proj.vy = (float) Math.sin(angle) * 10;
                bossProjectiles.add(proj);
            }
        }

        // Helper
        void fireProjectile(Ball target, float speed, int color, float size) {
            float angle = (float) Math.atan2(target.y - y, target.x - x);
            Ball proj = new Ball(x, y, size, color);
            proj.vx = (float) Math.cos(angle) * speed;
            proj.vy = (float) Math.sin(angle) * speed;
            bossProjectiles.add(proj);
            playSound(soundRetroLaser);
        }

        private void updateVoidTitan(long now) {
            // State Machine Checks (Only if in Idle State 0)
            if (state == 0) {
                if (now - lastStateChangeTime > 2000 + random.nextInt(2000)) { // Random 2-4s delay
                    // Select next state based on HP
                    if (hp <= maxHp / 2) {
                        // HP <= 50%: Randomly choose Dash (1) or Burst (2)
                        state = random.nextBoolean() ? 1 : 2;
                    } else {
                        // HP > 50%: Always Dash (1)
                        state = 1;
                    }

                    lastStateChangeTime = now;
                    dashing = false;

                    if (state == 1) {
                        // Prepare Dash
                        charging = true;
                        chargeStartTime = now;
                        playSound(soundPower); // Replaced soundLaunch
                    } else if (state == 2) {
                        // Prepare Burst - Charging Phase
                        charging = true;
                        chargeStartTime = now;
                        playSound(soundPower); // Warning sound
                    }
                }
            }

            if (state == 0) { // Hover Locally
                y = hoverCenterY + (float) Math.sin(now * 0.002) * 30;
                x = hoverCenterX + (float) Math.cos(now * 0.0013) * 20;

                if (now - lastAttackTime > 1500) {
                    shootVoidProjectile(); // Replaced shootVoidBall
                    lastAttackTime = now;
                }
            } else if (state == 1) { // Charge & Dash
                if (!dashing) {
                    // Start Charge Phase
                    dashing = true;
                    charging = true;
                    chargeStartTime = now;
                    playSound(soundPower); // Warning
                }

                if (charging) {
                    // Wait 2 seconds (Red Eyes)
                    if (now - chargeStartTime > 2000) {
                        charging = false;
                        // Lock Target logic
                        if (whiteBall != null) {
                            dashTargetX = whiteBall.x;
                            dashTargetY = whiteBall.y;
                        } else {
                            dashTargetX = centerX;
                            dashTargetY = centerY;
                        }
                    }
                } else {
                    // Moving Phase
                    // PERFORMANCE: Use distance squared
                    float dx = dashTargetX - x;
                    float dy = dashTargetY - y;
                    float distSq = dx * dx + dy * dy;

                    // Boundary Check (PERFORMANCE: distance squared)
                    float dcx = x - centerX;
                    float dcy = y - centerY;
                    float distFromCenterSq = dcx * dcx + dcy * dcy;
                    float radiusLimit = circleRadius * 0.95f - radius;

                    if (distFromCenterSq > radiusLimit * radiusLimit) {
                        // Hit wall, stop here but PUSH INWARDS to prevent getting stuck
                        state = 0;
                        dashing = false;
                        charging = false;
                        lastStateChangeTime = now;

                        // Push inwards (Stronger push to 0.6f)
                        float angle = (float) Math.atan2(y - centerY, x - centerX);
                        hoverCenterX = centerX + (float) Math.cos(angle) * (circleRadius * 0.6f);
                        hoverCenterY = centerY + (float) Math.sin(angle) * (circleRadius * 0.6f);
                        x = hoverCenterX;
                        y = hoverCenterY;
                        return;
                    }

                    if (distSq > 625) { // 25 * 25
                        float dist = (float) Math.sqrt(distSq); // Only calc sqrt when needed
                        float moveSpeed = 25;
                        vx = (dx / dist) * moveSpeed;
                        vy = (dy / dist) * moveSpeed;
                        x += vx;
                        y += vy;
                    } else {
                        // Reached target
                        state = 0;
                        dashing = false;
                        charging = false;
                        lastStateChangeTime = now;

                        // New hover center is where we stopped
                        hoverCenterX = x;
                        hoverCenterY = y;
                    }
                }
            } else if (state == 2) { // Burst Mode (Static, shoots everywhere)
                if (charging) {
                    if (now - chargeStartTime > 2000) {
                        charging = false;
                        // Fire Burst (PERFORMANCE: simple indexed loop)
                        for (int i = 0; i < 12; i++) {
                            // Inline logic to avoid sound spam from shootVoidProjectile
                            float angle = (float) Math.toRadians(i * 30);
                            float speed = 12;
                            VoidProjectile proj = new VoidProjectile(x, y, 25);
                            proj.vx = (float) Math.cos(angle) * speed;
                            proj.vy = (float) Math.sin(angle) * speed;
                            bossProjectiles.add(proj);
                        }
                        playSound(soundBlackExplosion);
                        state = 0; // Back to idle
                        lastStateChangeTime = now;
                    }
                }
            }
        }

        private void updateLunarConstruct(long now) {
            // Movement Logic: Pick new random point periodically
            if (now - lastMoveTime > 4000) { // Every 4 seconds
                lastMoveTime = now;
                // Pick a random spot in the upper circle area (y < centerY)
                // Restrict to keep within bounds
                float angle = random.nextFloat() * (float) Math.PI + (float) Math.PI; // PI to 2PI (Top Ref is actually
                // bottom in standard math but y
                // increases down? Wait.
                // Standard Android Canvas: y=0 is top. centerY is middle.
                // We want Top area: y < centerY.
                // Circle math: x = cx + r*cos(a), y = cy + r*sin(a)
                // Angle PI (180) is Left, 0 is Right, 3PI/2 (270) is Top.
                // Let's just use random X/Y rejection sampling or constrained polar
                // coordinates.
                // Top half: Angle from PI (180) to 2PI (360) -> Math.PI to 2*Math.PI.

                float moveAngle = (float) (Math.PI + random.nextFloat() * Math.PI); // 180 to 360 degrees
                float moveDist = random.nextFloat() * (circleRadius * 0.7f); // Stay somewhat inside

                moveTargetX = centerX + (float) Math.cos(moveAngle) * moveDist;
                moveTargetY = centerY + (float) Math.sin(moveAngle) * moveDist;
            }

            // Smoothly move hoverCenter towards moveTarget
            float dx = moveTargetX - hoverCenterX;
            float dy = moveTargetY - hoverCenterY;
            hoverCenterX += dx * 0.02f; // Smooth lerp
            hoverCenterY += dy * 0.02f;

            // Hover logic (Sine wave on top of movement)
            y = hoverCenterY + (float) Math.sin(now * 0.001) * 20;
            x = hoverCenterX; // X follows directly for now (or add slight horizontal sway if desired)

            // State Machine
            if (state == 0) { // Idle (Moon Rock)
                if (now - lastAttackTime > 2500) { // Every 2.5s
                    shootMoonRock();
                    lastAttackTime = now;
                }

                // Dynamic cooldown: Faster transitions when HP is low (enrage)
                long stateChangeCooldown = (hp > maxHp / 2) ? 5000 : 3000; // 5s normal, 3s enraged
                if (now - lastStateChangeTime > stateChangeCooldown) {
                    android.util.Log.d("BOSS_DEBUG", "STATE CHANGE TRIGGERED! HP: " + hp + "/" + maxHp);
                    if (hp > maxHp / 2) {
                        state = 1; // Scatter Shot (Phase 1)
                    } else {
                        state = 2; // Meteor Shower (Phase 2 - Enrage)
                    }
                    charging = true;
                    chargeStartTime = now;
                    playSound(soundPower); // Charging Up
                    lastStateChangeTime = now;
                    android.util.Log.d("BOSS_DEBUG", "New state: " + state + " charging: " + charging);
                }
            } else if (state == 1) { // Scatter Shot
                if (charging && now - chargeStartTime > 1500) {
                    doScatterShot();
                    charging = false;
                    state = 0; // Return to idle
                    lastStateChangeTime = now;
                }
            } else if (state == 2) { // Meteor Shower
                if (charging && now - chargeStartTime > 1500) {
                    doMeteorShower();
                    charging = false;
                    state = 0;
                    lastStateChangeTime = now;
                }
            }
        }

        void shootMoonRock() {
            if (whiteBall != null) {
                android.util.Log.d("BOSS_DEBUG", "shootMoonRock called");
                float angle = (float) Math.atan2(whiteBall.y - y, whiteBall.x - x);
                float speed = 8; // Slow
                Ball proj = new MoonRock(x, y, 40, Color.LTGRAY); // Big Rock
                android.util.Log.d("BOSS_DEBUG", "Created MoonRock: " + proj.getClass().getName());
                proj.vx = (float) Math.cos(angle) * speed;
                proj.vy = (float) Math.sin(angle) * speed;
                bossProjectiles.add(proj);
                playSound(soundRetroLaser); // Reuse sound for now
            }
        }

        void doMeteorShower() {
            android.util.Log.d("BOSS_DEBUG", "doMeteorShower called");
            for (int i = 0; i < 20; i++) {
                float startX = (random.nextFloat() * screenWidth);
                float startY = -50 - random.nextFloat() * 200; // Spawn closer to top (visible sooner)
                Ball proj = new MeteorProjectile(startX, startY, 25); // Slightly larger

                // Target player position
                if (whiteBall != null) {
                    float targetDx = whiteBall.x - startX;
                    float targetDy = whiteBall.y - startY;
                    float targetDist = (float) Math.sqrt(targetDx * targetDx + targetDy * targetDy);
                    float speed = 10 + random.nextFloat() * 8; // Random speed 10-18

                    proj.vx = (targetDx / targetDist) * speed;
                    proj.vy = (targetDy / targetDist) * speed;
                } else {
                    proj.vx = (random.nextFloat() - 0.5f) * 6;
                    proj.vy = 12 + random.nextFloat() * 8;
                }
                bossProjectiles.add(proj);
            }
            android.util.Log.d("BOSS_DEBUG", "Meteor shower spawned 20 Meteors");
            playSound(soundBlackExplosion); // Rumble sound
        }

        void shootVoidBall() {
            if (whiteBall != null) {
                float angle = (float) Math.atan2(whiteBall.y - y, whiteBall.x - x);
                float speed = 12;
                Ball proj = new Ball(x, y, 25, Color.rgb(75, 0, 130));
                proj.vx = (float) Math.cos(angle) * speed;
                proj.vy = (float) Math.sin(angle) * speed;
                bossProjectiles.add(proj);
                playSound(soundRetroLaser); // Retro laser sound
            }
        }

        void shootVoidBall(float angleDegrees) {
            float angle = (float) Math.toRadians(angleDegrees);
            float speed = 12;
            Ball proj = new Ball(x, y, 25, Color.rgb(75, 0, 130));
            proj.vx = (float) Math.cos(angle) * speed;
            proj.vy = (float) Math.sin(angle) * speed;
            bossProjectiles.add(proj);
        }

        void doBurstAttack() {
            playSound(soundLaserGun); // Laser Gun sound for burst attack
            // PERFORMANCE: Reduced from 20 to 16 projectiles to prevent freeze
            // 16 still provides excellent visual coverage with 22.5° spacing
            for (int i = 0; i < 16; i++) {
                float angle = (float) (i * (2 * Math.PI / 16));
                float speed = 10;
                Ball proj = new Ball(x, y, 20, Color.rgb(100, 0, 150));
                proj.vx = (float) Math.cos(angle) * speed;
                proj.vy = (float) Math.sin(angle) * speed;
                bossProjectiles.add(proj);
            }
        }

        void doScatterShot() {
            playSound(soundRetroLaser);
            // Triple Scatter Shot (Standard Phase)
            android.util.Log.d("BOSS_DEBUG", "doScatterShot called");
            for (int i = -1; i <= 1; i++) {
                if (whiteBall == null)
                    break;
                float angle = (float) Math.atan2(whiteBall.y - y, whiteBall.x - x);
                angle += i * 0.2f; // Spread by ~11 degrees
                float speed = 10;
                Ball proj = new MoonRock(x, y, 30, Color.LTGRAY);
                proj.vx = (float) Math.cos(angle) * speed;
                proj.vy = (float) Math.sin(angle) * speed;
                bossProjectiles.add(proj);
            }
        }

        void draw(Canvas canvas) {
            // Charging Telegraph Effect
            if (charging) {
                float progress = (System.currentTimeMillis() - chargeStartTime) / (float) chargeDuration;
                progress = Math.min(progress, 1.0f);

                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(5 + 10 * progress);
                paint.setColor(Color.WHITE);
                paint.setAlpha((int) (150 * progress));

                // Specific shapes for telegraphs? For now circle is readable.
                canvas.drawCircle(x, y, radius * (2.0f - progress), paint);

                paint.setAlpha(255);
                paint.setStyle(Paint.Style.FILL); // Reset
            }

            paint.setStyle(Paint.Style.FILL);
            paint.setColor(color);

            // UNIQUE BOSS SHAPES
            // UNIQUE BOSS SHAPES
            // --- BOSS ATTACK GLOW (Synced with Animation) ---
            if (charging || state == 1) { // Only glow during attack preparation
                paint.setStyle(Paint.Style.FILL);

                // Color based on Phase/Danger
                int glowColor = Color.YELLOW;
                if (hp < maxHp * 0.3)
                    glowColor = Color.RED;

                // Pulse Alpha (NO shadowLayer for 60 FPS)
                long time = System.currentTimeMillis();
                int alpha = (int) (100 + 50 * Math.sin(time * 0.01)); // Pulse 100-150

                paint.setColor(glowColor);
                paint.setAlpha(alpha);
                // REMOVED: paint.setShadowLayer(50, 0, 0, glowColor); // Causes FPS drop

                // Draw slightly larger than boss
                canvas.drawCircle(x, y, radius * 1.3f, paint);

                paint.setAlpha(255);
            }

            if (name.equals("VOID TITAN")) {
                // Spiky Star Shape for Void Titan
                paint.setColor(Color.rgb(30, 0, 50));

                Path voidPath = new Path();
                int spikes = 12;
                float outerR = radius * 1.2f;
                float innerR = radius * 0.8f;
                float angleStep = (float) (Math.PI * 2 / spikes);

                // Rotate logic
                float rot = (System.currentTimeMillis() % 2000) / 2000f * (float) Math.PI * 2;

                for (int i = 0; i < spikes; i++) {
                    float a = i * angleStep + rot;
                    float px = x + (float) Math.cos(a) * outerR;
                    float py = y + (float) Math.sin(a) * outerR;
                    if (i == 0)
                        voidPath.moveTo(px, py);
                    else
                        voidPath.lineTo(px, py);

                    a += angleStep / 2;
                    px = x + (float) Math.cos(a) * innerR;
                    py = y + (float) Math.sin(a) * innerR;
                    voidPath.lineTo(px, py);
                }
                voidPath.close();

                // REMOVED shadowLayer for 60 FPS
                // paint.setShadowLayer(40, 0, 0, Color.MAGENTA);
                canvas.drawPath(voidPath, paint);
                // paint.clearShadowLayer();

                // Core Eye
                paint.setColor(Color.BLACK);
                canvas.drawCircle(x, y, radius * 0.5f, paint);

                // Pupil
                int pupilColor = (state == 1) ? Color.RED : (state == 2 ? Color.CYAN : Color.rgb(138, 43, 226));
                paint.setColor(pupilColor);
                canvas.drawCircle(x, y, radius * 0.25f, paint);

            } else if (name.equals("SOLARION")) {
                // Sun with Coronas
                paint.setColor(Color.rgb(255, 140, 0)); // Dark Orange
                paint.setShadowLayer(50, 0, 0, Color.RED);
                canvas.drawCircle(x, y, radius * 0.9f, paint);
                paint.clearShadowLayer();

                // Surrounding balls removed by user request
                // Flares and Mini-Sun logic deleted

            } else if (name.equals("NEBULON")) {
                // Deep Space Cloud
                paint.setColor(Color.rgb(72, 61, 139)); // Dark Slate Blue
                canvas.drawCircle(x, y, radius, paint);

                // Mist Layers
                paint.setColor(Color.MAGENTA);
                paint.setAlpha(50);
                for (int i = 0; i < 3; i++) {
                    float offset = (float) Math.sin(System.currentTimeMillis() * 0.001 + i) * 10;
                    canvas.drawCircle(x + offset, y - offset, radius * (0.8f + i * 0.1f), paint);
                }

                // Stars inside
                paint.setColor(Color.WHITE);
                paint.setAlpha(200);
                if (random.nextFloat() < 0.1) {
                    float sx = x + (random.nextFloat() - 0.5f) * radius;
                    float sy = y + (random.nextFloat() - 0.5f) * radius;
                    canvas.drawCircle(sx, sy, 3, paint);
                }
                paint.setAlpha(255);

            } else if (name.equals("GRAVITON")) {
                // Black Hole Event Horizon
                paint.setColor(Color.BLACK);
                paint.setShadowLayer(50, 0, 0, Color.rgb(100, 0, 255));
                canvas.drawCircle(x, y, radius, paint);
                paint.clearShadowLayer();

                // Accretion Disk (Swirling)
                paint.setStyle(Paint.Style.STROKE);
                for (int i = 0; i < 3; i++) {
                    paint.setColor(Color.rgb(50 + i * 50, 0, 150 + i * 30));
                    paint.setStrokeWidth(6 - i);
                    float startA = (System.currentTimeMillis() * (0.2f - i * 0.05f)) % 360;
                    canvas.drawArc(x - radius * 1.2f, y - radius * 1.2f, x + radius * 1.2f, y + radius * 1.2f, startA,
                            200, false, paint);
                }

                // PHASE 3: EVENT HORIZON LINES (Synced with Active State)
                if (hp < maxHp * 0.3) {
                    long now = System.currentTimeMillis();
                    boolean isActive = (now - lastEventHorizonTime) < 2500;

                    if (isActive) {
                        float eventHorizonRadius = radius * 3.5f;
                        paint.setStyle(Paint.Style.STROKE);
                        paint.setStrokeWidth(3);
                        paint.setColor(Color.RED);
                        paint.setAlpha(150);

                        // Draw concentric pulsing rings
                        long phase = System.currentTimeMillis() % 1000;
                        float pulse = phase / 1000f; // 0 to 1

                        // Ring 1 (Fixed Limit)
                        canvas.drawCircle(x, y, eventHorizonRadius, paint);

                        // Ring 2 (Pulsing Inwards)
                        float r2 = eventHorizonRadius * (1.0f - pulse * 0.5f);
                        paint.setAlpha(100);
                        canvas.drawCircle(x, y, r2, paint);
                    }
                    paint.setAlpha(255);
                    paint.setStyle(Paint.Style.FILL);
                }

                paint.setStyle(Paint.Style.FILL);

            } else if (name.equals("MECHA-CORE")) {
                // Robotic Core - Detailed
                // Outer Ring (Rotating)
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(Math.max(2, radius * 0.15f));
                paint.setColor(Color.DKGRAY);
                canvas.drawCircle(x, y, radius, paint);

                paint.setColor(Color.CYAN);
                paint.setStrokeWidth(4);
                float rot = (System.currentTimeMillis() * 0.1f) % 360;
                RectF arcRect = new RectF(x - radius * 0.9f, y - radius * 0.9f, x + radius * 0.9f, y + radius * 0.9f);
                canvas.drawArc(arcRect, rot, 90, false, paint);
                canvas.drawArc(arcRect, rot + 180, 90, false, paint);

                // Inner Body
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(Color.rgb(50, 50, 60)); // Steel Grey
                canvas.drawCircle(x, y, radius * 0.7f, paint);

                // Glowing Core Pulse
                float pulse = (float) (Math.sin(System.currentTimeMillis() * 0.008) * 0.3 + 0.7);
                paint.setColor(Color.rgb(0, 255, 255)); // Cyan
                paint.setAlpha((int) (200 * pulse));
                paint.setShadowLayer(20, 0, 0, Color.BLUE);
                canvas.drawCircle(x, y, radius * 0.3f, paint);
                paint.clearShadowLayer();
                paint.setAlpha(255);

                // Red "Camera Eye" dot
                paint.setColor(Color.RED);
                float eyeOffset = radius * 0.2f; // Move eye slightly
                canvas.drawCircle(x + eyeOffset, y - eyeOffset, radius * 0.1f, paint);

            } else if (name.equals("CRYO-STASIS")) {
                // Crystal Shard
                Path crystalPath = new Path();
                crystalPath.moveTo(x, y - radius * 1.3f); // Top
                crystalPath.lineTo(x + radius, y);
                crystalPath.lineTo(x, y + radius * 1.3f); // Bottom
                crystalPath.lineTo(x - radius, y);
                crystalPath.close();

                paint.setColor(Color.CYAN);
                paint.setShadowLayer(30, 0, 0, Color.WHITE);
                canvas.drawPath(crystalPath, paint);
                paint.clearShadowLayer();

                // Inner Detail
                paint.setStyle(Paint.Style.STROKE);
                paint.setColor(Color.WHITE);
                paint.setStrokeWidth(3);
                canvas.drawPath(crystalPath, paint);
                paint.setStyle(Paint.Style.FILL);

            } else if (name.equals("GEO-BREAKER")) {
                // Jagged Rock with Lava Cracks
                paint.setColor(Color.rgb(60, 40, 30)); // Darker Rock
                canvas.drawCircle(x, y, radius, paint); // Base circle instead of rect for rotation safety

                // Magma cracks
                paint.setColor(Color.rgb(255, 69, 0)); // Red Orange
                paint.setStrokeWidth(4);
                paint.setStyle(Paint.Style.STROKE);
                canvas.drawLine(x - radius * 0.5f, y - radius * 0.5f, x + radius * 0.5f, y + radius * 0.5f, paint);
                canvas.drawLine(x + radius * 0.5f, y - radius * 0.3f, x - radius * 0.2f, y + radius * 0.6f, paint);
                paint.setStyle(Paint.Style.FILL);

            } else if (name.equals("BIO-HAZARD")) {
                // Green Sludge
                paint.setColor(Color.rgb(0, 100, 0));
                canvas.drawCircle(x, y, radius, paint);

                // Bubbles
                paint.setColor(Color.rgb(50, 205, 50)); // Lime
                float b1 = (float) Math.sin(System.currentTimeMillis() * 0.003) * radius * 0.4f;
                float b2 = (float) Math.cos(System.currentTimeMillis() * 0.004) * radius * 0.3f;
                canvas.drawCircle(x + b1, y + b2, radius * 0.3f, paint);
                canvas.drawCircle(x - b2, y - b1, radius * 0.25f, paint);

                // Toxic Symbol center (Simulated)
                paint.setColor(Color.BLACK);
                canvas.drawCircle(x, y, radius * 0.15f, paint);

            } else if (name.equals("CHRONO-SHIFTER")) {
                // Golden Clock
                paint.setColor(Color.rgb(218, 165, 32)); // Goldenrod
                paint.setShadowLayer(20, 0, 0, Color.YELLOW);
                canvas.drawCircle(x, y, radius, paint);
                paint.clearShadowLayer();

                paint.setColor(Color.BLACK);
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(3);
                canvas.drawCircle(x, y, radius * 0.8f, paint);

                // Moving Hands
                float secAngle = (System.currentTimeMillis() % 60000) / 60000f * 360;
                float minAngle = (System.currentTimeMillis() % 3600000) / 3600000f * 360;

                canvas.save();
                canvas.rotate(secAngle, x, y);
                canvas.drawLine(x, y, x, y - radius * 0.7f, paint); // Second Hand
                canvas.restore();

                paint.setStrokeWidth(5);
                canvas.save();
                canvas.rotate(minAngle, x, y);
                canvas.drawLine(x, y, x, y - radius * 0.5f, paint); // Hour Hand
                canvas.restore();

                paint.setStyle(Paint.Style.FILL);
            } else if (name.equals("LUNAR CONSTRUCT")) {
                // Moon
                paint.setColor(Color.LTGRAY);
                canvas.drawCircle(x, y, radius, paint);
                paint.setColor(Color.DKGRAY);
                canvas.drawCircle(x - radius * 0.3f, y - radius * 0.2f, radius * 0.2f, paint);
                canvas.drawCircle(x + radius * 0.4f, y + radius * 0.4f, radius * 0.15f, paint);
            } else {
                // Default Circular Shape for others
                paint.setColor(color);
                paint.setShadowLayer(30, 0, 0, color);
                canvas.drawCircle(x, y, radius, paint);
                paint.clearShadowLayer();
            }

            // --- UNIVERSAL SHIELD VISUAL (Drawn Last) ---
            if (state == 1 || dashing) {
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(5);
                paint.setColor(Color.CYAN);
                paint.setShadowLayer(10, 0, 0, Color.BLUE);
                int alpha = (int) (200 + Math.sin(System.currentTimeMillis() * 0.01) * 55);
                paint.setAlpha(alpha);

                float rot = (System.currentTimeMillis() * 0.2f);
                RectF shieldRect = new RectF(x - radius * 1.3f, y - radius * 1.3f, x + radius * 1.3f,
                        y + radius * 1.3f);
                for (int i = 0; i < 3; i++) {
                    canvas.drawArc(shieldRect, rot + i * 120, 80, false, paint);
                }

                paint.clearShadowLayer();
                paint.setAlpha(255);
                paint.setStyle(Paint.Style.FILL);
            }

            // HP Bar (Above Boss)
            float barW = radius * 2.5f;
            float barH = 25;
            float barX = x - barW / 2;
            float barY = y - radius * 1.4f; // Moved up slightly

            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.DKGRAY);
            canvas.drawRect(barX, barY, barX + barW, barY + barH, paint);

            paint.setColor(Color.RED);
            float hpRatio = Math.max(0, hp / maxHp);
            canvas.drawRect(barX, barY, barX + barW * hpRatio, barY + barH, paint);

            paint.setStyle(Paint.Style.STROKE);
            paint.setColor(Color.WHITE);
            paint.setStrokeWidth(2);
            canvas.drawRect(barX, barY, barX + barW, barY + barH, paint);

            // Name
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.WHITE);
            paint.setTextSize(40);
            paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            paint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText(name, x, barY - 15, paint);
            paint.setTypeface(Typeface.DEFAULT);
            paint.setTypeface(Typeface.DEFAULT);
        }
    } // This closes the draw method.

    private class VoidProjectile extends Ball {
        public VoidProjectile(float x, float y, float r) {
            super(x, y, r, Color.BLACK);
        }
    }

    class Ufo {
        float x, y;
        long startTime;
        boolean leaving = false;
        Ball target1 = null;
        Ball target2 = null;
        long lastLaserTime = 0;
        boolean chargingLaser = false;
        long laserChargeStart = 0;
        String attackMode = "default"; // "default" or "burning"

        Ufo() {
            x = centerX;
            y = -200; // Start above screen
            startTime = System.currentTimeMillis();

            // Determine attack mode based on presence of black balls
            if (blackBalls == null || blackBalls.isEmpty()) {
                attackMode = "burning"; // Orange UFO for colored ball attacks
            } else {
                attackMode = "default"; // Gray UFO for black ball mode
            }
        }

        void update() {
            // Orange UFO (Burning Mode) - Maps wide descent
            float targetY;
            if (attackMode.equals("burning")) {
                targetY = screenHeight * 0.9f; // Go effectively to bottom area
            } else {
                targetY = centerY - 400; // Default hover for Gray UFO
            }

            if (!leaving) {
                if (y < targetY)
                    y += 5; // Sustain entrance
                else {
                    // If burning mode, stay at bottom
                    if (attackMode.equals("burning")) {
                        y = targetY + (float) Math.sin(System.currentTimeMillis() / 500.0) * 50; // Wider hover at
                        // bottom
                    } else {
                        y = targetY + (float) Math.sin(System.currentTimeMillis() / 500.0) * 20; // Hover top
                    }
                }

                // Boss attack logic - KAMIKAZE STRIKES
                if (currentBoss != null) {
                    // Fly directly at boss
                    float dx = currentBoss.x - x;
                    float dy = currentBoss.y - y;
                    float dist = (float) Math.sqrt(dx * dx + dy * dy);

                    // Move fast toward boss
                    x += (dx / dist) * 10; // Fast kamikaze speed
                    y += (dy / dist) * 10;

                    // Check collision
                    if (dist < 70) {
                        // Kamikaze strike!
                        createImpactBurst(x, y, Color.RED);
                        createParticles(x, y, Color.RED);
                        currentBoss.hp -= 5; // 25 damage
                        playSound(soundBlackExplosion);

                        // Bounce back after strike
                        x = currentBoss.x + (random.nextFloat() - 0.5f) * 200;
                        y = currentBoss.y - 150;
                        lastLaserTime = System.currentTimeMillis();
                    }

                    // After 3 strikes, leave
                    if (System.currentTimeMillis() - startTime > 6000) { // ~3 strikes in 6s
                        leaving = true;
                    }
                } else {
                    // Check if there are any black balls
                    if (blackBalls != null && !blackBalls.isEmpty()) {
                        // Find 2 nearest black balls
                        java.util.ArrayList<Ball> toRemove = new java.util.ArrayList<>();
                        target1 = null;
                        target2 = null;
                        float minDist1 = Float.MAX_VALUE;
                        float minDist2 = Float.MAX_VALUE;

                        for (Ball b : blackBalls) {
                            float dx = x - b.x;
                            float dy = y - b.y;
                            float d = (float) Math.sqrt(dx * dx + dy * dy);

                            if (d < minDist1) {
                                minDist1 = d;
                                target1 = b;
                            }
                        }
                        target2 = null; // Explicitly enforce single target

                        // Move towards target1 (Boosted speed)
                        if (target1 != null) {
                            float dx = target1.x - x;
                            float dy = target1.y - y;
                            float d = (float) Math.sqrt(dx * dx + dy * dy);
                            if (d > 100) { // Don't get too close
                                x += (dx / d) * 3; // Reduced Speed from 5 to 3
                                y += (dy / d) * 3;
                            }
                        }

                        // Pull both targets logic (Boosted Range & Strength)
                        float pullRange = 10000; // Boosted to infinite for map-wide pull
                        float pullStrength = 8; // Reduced from 30 for sequential pulling

                        if (target1 != null) {
                            float dx = x - target1.x;
                            float dy = y - target1.y;
                            float d = (float) Math.sqrt(dx * dx + dy * dy);
                            if (d < pullRange) {
                                target1.x += (dx / d) * pullStrength;
                                target1.y += (dy / d) * pullStrength;
                                if (d < 80) {
                                    createParticles(target1.x, target1.y, Color.BLACK);
                                    toRemove.add(target1);
                                    playSound(soundBlackExplosion);
                                }
                            }
                        }

                        // Quest tracking for black ball destruction
                        if (questManager != null && toRemove.size() > 0) {
                            questManager.incrementQuestProgress(2, toRemove.size());
                            questManager.incrementQuestProgress(5, toRemove.size());
                        }
                        blackBalls.removeAll(toRemove);
                    } else {
                        // NO BLACK BALLS - DIRECT ATTACK ON COLORED BALLS (Kamikaze style)
                        if (coloredBalls != null && !coloredBalls.isEmpty()) {
                            // Find nearest colored ball (OPTIMIZED - no sorting)
                            Ball nearestTarget = null;
                            float minDist = Float.MAX_VALUE;

                            for (Ball b : coloredBalls) {
                                float dx = b.x - x;
                                float dy = b.y - y;
                                float dist = dx * dx + dy * dy; // Skip sqrt for speed
                                if (dist < minDist) {
                                    minDist = dist;
                                    nearestTarget = b;
                                }
                            }

                            // Fly toward nearest target
                            if (nearestTarget != null) {
                                float dx = nearestTarget.x - x;
                                float dy = nearestTarget.y - y;
                                float dist = (float) Math.sqrt(dx * dx + dy * dy);

                                // Move fast toward target
                                x += (dx / dist) * 8; // Fast kamikaze speed
                                y += (dy / dist) * 8;

                                // Check collision with nearby balls (within radius)
                                java.util.ArrayList<Ball> toDestroy = new java.util.ArrayList<>();
                                int hitCount = 0;

                                for (Ball target : coloredBalls) {
                                    if (hitCount >= 3)
                                        break; // Max 3

                                    float tdx = x - target.x;
                                    float tdy = y - target.y;
                                    float tdist = (float) Math.sqrt(tdx * tdx + tdy * tdy);

                                    if (tdist < 60) { // Collision radius
                                        // Minimal explosion effect for performance
                                        createImpactBurst(target.x, target.y, target.color);
                                        toDestroy.add(target);
                                        score += 10;
                                        playSound(soundBlackExplosion);
                                        hitCount++;
                                    }
                                }

                                // Remove destroyed balls
                                // Quest tracking for batch destruction
                                if (questManager != null && toDestroy.size() > 0) {
                                    android.util.Log.d("QUEST_DEBUG",
                                            "Teleport destroyed " + toDestroy.size() + " colored balls!");
                                    questManager.incrementQuestProgress(1, toDestroy.size());
                                    questManager.incrementQuestProgress(5, toDestroy.size());
                                }
                                coloredBalls.removeAll(toDestroy);

                                // If we hit 3 or no more balls, leave
                                if (hitCount >= 3 || coloredBalls.isEmpty()) {
                                    leaving = true;
                                }
                            }
                        }
                    }
                }

                if (System.currentTimeMillis() - startTime > 12000) // Extended from 8000 to 12000
                    leaving = true;
            } else {
                y -= 15;
                if (y < -300)
                    activeUfo = null;
            }
        }

        void fireLaserAtBoss() {
            if (currentBoss == null)
                return;

            // Create laser beam effect
            electricEffects.add(new ElectricEffect(x, y + 20, currentBoss.x, currentBoss.y, 1));
            // Red Impact
            createImpactBurst(currentBoss.x, currentBoss.y, Color.RED);
            currentBoss.hp -= 100; // Damage
            playSound(soundElectric);

            // Visual feedback (Red Text)
            floatingTexts.add(new FloatingText("UFO LASER!", x, y - 40, Color.RED));
        }

        void fireLaserAtColoredBalls() {
            if (coloredBalls == null || coloredBalls.isEmpty())
                return;

            // Find up to 2 nearest colored balls
            java.util.ArrayList<Ball> targets = new java.util.ArrayList<>();
            // Simple nearest search (optimize if needed)
            coloredBalls.sort((b1, b2) -> Float.compare((float) Math.hypot(b1.x - x, b1.y - y),
                    (float) Math.hypot(b2.x - x, b2.y - y)));

            for (int i = 0; i < Math.min(2, coloredBalls.size()); i++) {
                targets.add(coloredBalls.get(i));
            }

            for (Ball target : targets) {
                // Red Laser Visual
                electricEffects.add(new ElectricEffect(x, y + 20, target.x, target.y, 1));
                createImpactBurst(target.x, target.y, Color.RED);

                // Destroy Ball
                coloredBalls.remove(target);
                score += 10; // Bonus points
                playSound(soundElectric);
            }
        }

        void draw(Canvas canvas) {
            // Draw UFO (color varies by attack mode)
            paint.setStyle(Paint.Style.FILL);

            if (attackMode.equals("burning")) {
                // Burning mode: Orange/Yellow UFO
                paint.setColor(Color.rgb(255, 140, 0)); // Dark Orange body
                canvas.drawOval(x - 60, y - 20, x + 60, y + 20, paint);
                paint.setColor(Color.rgb(255, 215, 0)); // Gold dome
                canvas.drawArc(x - 24, y - 30, x + 24, y + 5, 180, 180, true, paint);
            } else {
                // Default mode: Gray UFO
                paint.setColor(Color.DKGRAY);
                canvas.drawOval(x - 60, y - 20, x + 60, y + 20, paint); // Body
                paint.setColor(Color.GREEN);
                canvas.drawArc(x - 24, y - 30, x + 24, y + 5, 180, 180, true, paint); // Dome
            }

            // Lights
            paint.setColor(Color.RED);
            long t = System.currentTimeMillis();
            if ((t / 200) % 2 == 0)
                canvas.drawCircle(x - 40, y, 5, paint);
            if ((t / 200) % 2 != 0)
                canvas.drawCircle(x + 40, y, 5, paint);

            // Charging laser effect (color varies by mode)
            if (chargingLaser) {
                long elapsed = System.currentTimeMillis() - laserChargeStart;
                float chargeProgress = Math.min(elapsed / 500f, 1f);

                if (attackMode.equals("burning")) {
                    // Orange/Yellow charging for burning mode
                    paint.setColor(Color.argb((int) (200 * chargeProgress), 255, 140, 0)); // Orange
                } else {
                    // Red charging for default mode
                    paint.setColor(Color.argb((int) (200 * chargeProgress), 255, 0, 0)); // RED
                }
                canvas.drawCircle(x, y + 20, 30 * chargeProgress, paint);
            }

            // Firing Laser Beam (Thick Red Line for boss)
            if (!chargingLaser && System.currentTimeMillis() - lastLaserTime < 500 && currentBoss != null) {
                paint.setStrokeWidth(15);
                paint.setColor(Color.RED);
                canvas.drawLine(x, y + 20, currentBoss.x, currentBoss.y, paint);
                // Glow core
                paint.setStrokeWidth(5);
                paint.setColor(Color.WHITE);
                canvas.drawLine(x, y + 20, currentBoss.x, currentBoss.y, paint);
            }

            // Burning Laser Beam for Colored Balls (Orange/Yellow)
            if (!chargingLaser && System.currentTimeMillis() - lastLaserTime < 500 && attackMode.equals("burning")
                    && (coloredBalls != null && !coloredBalls.isEmpty())) {
                // Draw lasers to nearest colored balls
                java.util.ArrayList<Ball> nearestBalls = new java.util.ArrayList<>();
                for (int i = 0; i < Math.min(2, coloredBalls.size()); i++) {
                    nearestBalls.add(coloredBalls.get(i));
                }

                for (Ball target : nearestBalls) {
                    // Thick burning beam (outer layer - orange)
                    paint.setStrokeWidth(15);
                    paint.setColor(Color.rgb(255, 140, 0)); // Orange
                    canvas.drawLine(x, y + 20, target.x, target.y, paint);
                    // Middle layer (yellow)
                    paint.setStrokeWidth(8);
                    paint.setColor(Color.rgb(255, 215, 0)); // Gold
                    canvas.drawLine(x, y + 20, target.x, target.y, paint);
                    // Core (bright white)
                    paint.setStrokeWidth(3);
                    paint.setColor(Color.WHITE);
                    canvas.drawLine(x, y + 20, target.x, target.y, paint);
                }
            }

            // Tractor Beam (only for black balls, not boss)
            // Only draw if we have black ball targets
            if (!leaving && currentBoss == null && blackBalls != null && !blackBalls.isEmpty()) {
                paint.setColor(Color.argb(40, 50, 255, 50));
                android.graphics.Path p = new android.graphics.Path();
                p.moveTo(x - 20, y + 20);
                p.lineTo(x + 20, y + 20);
                p.lineTo(x + 150, y + 800);
                p.lineTo(x - 150, y + 800);
                p.close();
                canvas.drawPath(p, paint);

                // Draw beam lines to targets
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(3);
                paint.setColor(Color.argb(100, 0, 255, 0));
                if (target1 != null) {
                    canvas.drawLine(x, y + 20, target1.x, target1.y, paint);
                }
            }
        }

    }

    private void drawBadgeIcon(Canvas canvas, float cx, float cy, float r, int outerColor, int innerColor,
            int textColor, String text) {
        // Outer Board (Primary)
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(outerColor);
        canvas.drawCircle(cx, cy, r, paint);

        // Inner Circle (Secondary)
        paint.setColor(innerColor);
        canvas.drawCircle(cx, cy, r * 0.75f, paint);

        // Inner Ring (Outer Color again for contrast)
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(r * 0.1f);
        paint.setColor(outerColor);
        canvas.drawCircle(cx, cy, r * 0.70f, paint);

        // Text
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(textColor);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setFakeBoldText(true);
        // Adjust text size based on length
        paint.setTextSize(text.length() > 2 ? r * 0.6f : r * 0.8f);

        // Center text vertically
        Paint.FontMetrics fm = paint.getFontMetrics();
        float textY = cy - (fm.descent + fm.ascent) / 2;
        canvas.drawText(text, cx, textY, paint);
    }

    private void drawPassiveSlot(Canvas canvas, float originalSlotSize) {
        float iconSize = screenWidth * 0.1f; // Match Home button size
        float slotSize = iconSize;
        float slotX = iconSize * 0.8f; // Left side symmetry (Home is at width - 0.8*size)
        float slotY = screenHeight * 0.25f; // Match Home button Y-coordinate

        // Draw Slot Background
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(5);
        paint.setColor(Color.DKGRAY);
        canvas.drawCircle(slotX, slotY, slotSize / 2, paint);

        // Draw "PASSIVE" Label
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.LTGRAY);
        paint.setTextSize(slotSize * 0.25f);
        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("PASSIVE", slotX, slotY - slotSize * 0.6f, paint);

        // Draw Active Passive Icon
        if (activePassivePower != null && !activePassivePower.equals("none")) {
            int color = Color.GRAY;
            String letter = "?";

            if (activePassivePower.equals("teleport")) {
                color = Color.GREEN;
                letter = "TP";
            } else if (activePassivePower.equals("split_save")) {
                color = Color.MAGENTA;
                letter = "SS";
            } else if (activePassivePower.equals("vortex")) {
                color = Color.CYAN;
                letter = "VX";
            }

            drawBadgeIcon(canvas, slotX, slotY, slotSize * 0.4f, color, Color.BLACK, Color.WHITE, letter);
        } else {
            // Empty placeholder
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.DKGRAY);
            paint.setTextSize(slotSize * 0.2f);
            paint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("EMPTY", slotX, slotY + slotSize * 0.1f, paint);
            canvas.drawText("EMPTY", slotX, slotY + slotSize * 0.1f, paint);
        }
    }
}
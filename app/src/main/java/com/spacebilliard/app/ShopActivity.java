package com.spacebilliard.app;

import android.app.Activity;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.spacebilliard.app.ui.NeonButton;
import com.spacebilliard.app.ui.NeonShopItem;

public class ShopActivity extends Activity {

        private int currentCategory = 0; // 0: SKINS, 1: TRAILS, 2: SIGHTS, 3: EFFECTS
        private GridLayout grid;
        private TextView descriptionText;
        private TextView coinText; // Coin balance display
        private NeonButton btnSkins, btnTrails, btnSights, btnEffects;

        // Selected item for purchase
        private String selectedItemId = null;
        private int selectedItemPrice = 0;
        private boolean selectedItemOwned = false;

        @Override
        protected void onCreate(Bundle savedInstanceState) {
                super.onCreate(savedInstanceState);

                requestWindowFeature(Window.FEATURE_NO_TITLE);
                getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                                WindowManager.LayoutParams.FLAG_FULLSCREEN);

                // 1. Root Container with Space Background
                FrameLayout root = new FrameLayout(this);
                root.setBackgroundColor(Color.rgb(5, 5, 20)); // Deep space blue

                // 2. Main Shop Panel (The one with the neon border)
                LinearLayout mainPanel = new LinearLayout(this);
                mainPanel.setOrientation(LinearLayout.VERTICAL);

                FrameLayout.LayoutParams panelParams = new FrameLayout.LayoutParams(
                                (int) (getResources().getDisplayMetrics().widthPixels * 0.9f),
                                (int) (getResources().getDisplayMetrics().heightPixels * 0.9f)); // Slightly
                                                                                                 // taller
                panelParams.gravity = Gravity.CENTER;
                mainPanel.setLayoutParams(panelParams);

                // Custom Background
                GradientDrawable panelBg = new GradientDrawable();
                panelBg.setColor(Color.argb(230, 10, 20, 40));
                panelBg.setCornerRadius(80f);
                panelBg.setStroke(6, Color.rgb(0, 255, 255)); // Neon
                                                              // Cyan
                                                              // border
                mainPanel.setBackground(panelBg);

                // 3. Header View
                setupHeader(mainPanel);

                // 4. Awning View
                View awning = new View(this) {
                        @Override
                        protected void onDraw(Canvas canvas) {
                                float w = getWidth();
                                float h = getHeight();
                                int stripes = 8;
                                float stripeW = w / stripes;
                                Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);

                                for (int i = 0; i < stripes; i++) {
                                        p.setColor(i % 2 == 0 ? Color.WHITE : Color.rgb(0, 180, 200));
                                        canvas.drawRect(i * stripeW, 0, (i + 1) * stripeW, h - 20, p);
                                        canvas.drawCircle(i * stripeW + stripeW / 2, h - 20, stripeW / 2, p);
                                }
                        }
                };
                LinearLayout.LayoutParams awningParams = new LinearLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                (int) (50 * getResources().getDisplayMetrics().density));
                mainPanel.addView(awning, awningParams);

                // 5. Category Tabs
                LinearLayout tabsLayout = new LinearLayout(this);
                tabsLayout.setOrientation(LinearLayout.HORIZONTAL);
                tabsLayout.setGravity(Gravity.CENTER);
                tabsLayout.setPadding(0, 20, 0, 10);

                btnSkins = createTabButton("SKINS", Color.CYAN);
                btnTrails = createTabButton("TRAILS", Color.GRAY);
                btnSights = createTabButton("SIGHTS", Color.GRAY);
                btnEffects = createTabButton("EFFECTS", Color.GRAY);

                btnSkins.setOnClickListener(v -> setCategory(0));
                btnTrails.setOnClickListener(v -> setCategory(1));
                btnSights.setOnClickListener(v -> setCategory(2));
                btnEffects.setOnClickListener(v -> setCategory(3));

                tabsLayout.addView(btnSkins);
                tabsLayout.addView(btnTrails);
                tabsLayout.addView(btnSights);
                tabsLayout.addView(btnEffects);
                mainPanel.addView(tabsLayout);

                // 6. Description / Preview Panel
                LinearLayout infoPanel = new LinearLayout(this);
                infoPanel.setOrientation(LinearLayout.VERTICAL);
                infoPanel.setGravity(Gravity.CENTER);
                infoPanel.setPadding(20, 10, 20, 10);

                descriptionText = new TextView(this);
                descriptionText.setText("SELECT AN ITEM TO PREVIEW");
                descriptionText.setTextColor(Color.YELLOW);
                descriptionText.setTextSize(16);
                descriptionText.setTypeface(Typeface.SERIF, Typeface.ITALIC);
                descriptionText.setGravity(Gravity.CENTER);
                descriptionText.setLines(3); // Fixed height to prevent jumping
                descriptionText.setEllipsize(android.text.TextUtils.TruncateAt.END);
                infoPanel.addView(descriptionText);
                mainPanel.addView(infoPanel);

                // 7. Scrollable Grid Area
                ScrollView scrollView = new ScrollView(this);
                scrollView.setPadding(30, 10, 30, 20);
                scrollView.setVerticalScrollBarEnabled(false);
                LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
                mainPanel.addView(scrollView, scrollParams);

                grid = new GridLayout(this);
                grid.setColumnCount(3);
                grid.setAlignmentMode(GridLayout.ALIGN_BOUNDS);
                scrollView.addView(grid);

                // Initial Load
                setCategory(0);

                // 8. Footer
                setupFooter(mainPanel);

                root.addView(mainPanel);
                setContentView(root);
        }

        private NeonButton createTabButton(String text, int color) {
                NeonButton btn = new NeonButton(this, text, color);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                                (int) (70 * getResources().getDisplayMetrics().density), // Reduced width
                                (int) (35 * getResources().getDisplayMetrics().density)); // Reduced height
                params.setMargins(5, 0, 5, 0);
                btn.setLayoutParams(params);
                return btn;
        }

        private void setCategory(int cat) {
                this.currentCategory = cat;
                // Update styling
                btnSkins.setThemeColor(cat == 0 ? Color.CYAN : Color.GRAY);
                btnTrails.setThemeColor(cat == 1 ? Color.GREEN : Color.GRAY);
                btnSights.setThemeColor(cat == 2 ? Color.WHITE : Color.GRAY);
                btnEffects.setThemeColor(cat == 3 ? Color.rgb(255, 100, 100) : Color.GRAY);

                btnSkins.postInvalidate();
                btnTrails.postInvalidate();
                btnSights.postInvalidate();
                btnEffects.postInvalidate();

                // Update Description with Category Info
                if (descriptionText != null) {
                        switch (cat) {
                                case 0:
                                        descriptionText.setText("Change the appearance of your ball.");
                                        descriptionText.setTextColor(Color.CYAN);
                                        break;
                                case 1:
                                        descriptionText.setText("A trail follows the ball as it moves.");
                                        descriptionText.setTextColor(Color.GREEN);
                                        break;
                                case 2:
                                        descriptionText.setText("Change the aiming guide style.");
                                        descriptionText.setTextColor(Color.WHITE);
                                        break;
                                case 3:
                                        descriptionText.setText("Explosion effects when hitting balls.");
                                        descriptionText.setTextColor(Color.rgb(255, 100, 100));
                                        break;
                        }
                }

                refreshGrid();
        }

        private void setupHeader(LinearLayout parent) {
                FrameLayout header = new FrameLayout(this);
                LinearLayout.LayoutParams headerParams = new LinearLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                (int) (60 * getResources().getDisplayMetrics().density));
                header.setLayoutParams(headerParams);

                // Header Background
                GradientDrawable headerBg = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                                new int[] { Color.rgb(100, 50, 255), Color.rgb(50, 200, 255) });
                headerBg.setCornerRadii(new float[] { 80f, 80f, 80f, 80f, 0, 0, 0, 0 });
                header.setBackground(headerBg);

                TextView title = new TextView(this);
                title.setText("NEON SHOP");
                title.setTextColor(Color.WHITE);
                title.setTextSize(24);
                title.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
                FrameLayout.LayoutParams titleParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT);
                titleParams.gravity = Gravity.CENTER;
                header.addView(title, titleParams);

                // Coin Display (Left side)
                android.content.SharedPreferences prefs = getSharedPreferences("SpaceBilliard",
                                android.content.Context.MODE_PRIVATE);
                int coins = prefs.getInt("coins", 0);

                coinText = new TextView(this);
                coinText.setText("💰 " + coins);
                coinText.setTextColor(Color.rgb(255, 215, 0)); // Gold
                coinText.setTextSize(18);
                coinText.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
                FrameLayout.LayoutParams coinParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT);
                coinParams.gravity = Gravity.START | Gravity.CENTER_VERTICAL;
                coinParams.leftMargin = 40;
                header.addView(coinText, coinParams);

                // Close Button (X)
                TextView closeBtn = new TextView(this);
                closeBtn.setText("X");
                closeBtn.setTextColor(Color.argb(150, 0, 0, 0));
                closeBtn.setTextSize(24);
                closeBtn.setTypeface(Typeface.DEFAULT_BOLD);
                FrameLayout.LayoutParams closeParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT);
                closeParams.gravity = Gravity.END | Gravity.CENTER_VERTICAL;
                closeParams.rightMargin = 40;
                closeBtn.setOnClickListener(v -> finish());
                header.addView(closeBtn, closeParams);

                parent.addView(header);
        }

        private void refreshGrid() {
                grid.removeAllViews();
                android.content.SharedPreferences prefs = getSharedPreferences("SpaceBilliard",
                                android.content.Context.MODE_PRIVATE);

                if (currentCategory == 0) { // SKINS
                        String current = prefs.getString("selectedSkin", "default");
                        addItem(grid, "DEFAULT", current.equals("default"), Color.WHITE, "Classic White Ball",
                                        "default");
                        addItem(grid, "TR FLAG", current.equals("tr_flag"), Color.RED, "Turkish Flag Skin", "tr_flag");
                        addItem(grid, "SOCCER", current.equals("soccer"), Color.LTGRAY, "Classic Football Design",
                                        "soccer");
                        addItem(grid, "NEON", current.equals("neon_pulse"), Color.CYAN, "Pulsing Neon Light",
                                        "neon_pulse");
                        // Countries
                        addItem(grid, "USA", current.equals("usa"), Color.BLUE, "USA Flag", "usa");
                        addItem(grid, "GERMANY", current.equals("germany"), Color.YELLOW, "Germany Flag", "germany");
                        addItem(grid, "FRANCE", current.equals("france"), Color.BLUE, "France Flag", "france");
                        addItem(grid, "ITALY", current.equals("italy"), Color.GREEN, "Italy Flag", "italy");
                        addItem(grid, "UK", current.equals("uk"), Color.BLUE, "United Kingdom Flag", "uk");
                        addItem(grid, "BRAZIL", current.equals("brazil"), Color.GREEN, "Brazil Flag", "brazil");
                        addItem(grid, "SPAIN", current.equals("spain"), Color.RED, "Spain Flag", "spain");
                        addItem(grid, "PORTUGAL", current.equals("portugal"), Color.GREEN, "Portugal Flag", "portugal");
                        addItem(grid, "NETHERLANDS", current.equals("netherlands"), Color.RED, "Netherlands Flag",
                                        "netherlands");
                        addItem(grid, "BELGIUM", current.equals("belgium"), Color.YELLOW, "Belgium Flag", "belgium");
                        addItem(grid, "SWITZERLAND", current.equals("switzerland"), Color.RED, "Switzerland Flag",
                                        "switzerland");
                        addItem(grid, "AUSTRIA", current.equals("austria"), Color.RED, "Austria Flag", "austria");
                        addItem(grid, "SWEDEN", current.equals("sweden"), Color.BLUE, "Sweden Flag", "sweden");
                        addItem(grid, "NORWAY", current.equals("norway"), Color.RED, "Norway Flag", "norway");
                        addItem(grid, "DENMARK", current.equals("denmark"), Color.RED, "Denmark Flag", "denmark");
                        addItem(grid, "FINLAND", current.equals("finland"), Color.BLUE, "Finland Flag", "finland");
                        addItem(grid, "POLAND", current.equals("poland"), Color.RED, "Poland Flag", "poland");
                        addItem(grid, "GREECE", current.equals("greece"), Color.BLUE, "Greece Flag", "greece");
                        addItem(grid, "IRELAND", current.equals("ireland"), Color.GREEN, "Ireland Flag", "ireland");
                        addItem(grid, "CANADA", current.equals("canada"), Color.RED, "Canada Flag", "canada");
                        addItem(grid, "JAPAN", current.equals("japan"), Color.RED, "Japan Flag", "japan");
                        addItem(grid, "KOREA", current.equals("korea"), Color.BLUE, "South Korea Flag", "korea");
                        addItem(grid, "CHINA", current.equals("china"), Color.RED, "China Flag", "china");
                        addItem(grid, "RUSSIA", current.equals("russia"), Color.BLUE, "Russia Flag", "russia");
                        addItem(grid, "INDIA", current.equals("india"), Color.rgb(255, 153, 51), "India Flag", "india");
                        addItem(grid, "MEXICO", current.equals("mexico"), Color.GREEN, "Mexico Flag", "mexico");
                        addItem(grid, "ARGENTINA", current.equals("argentina"), Color.CYAN, "Argentina Flag",
                                        "argentina");
                        addItem(grid, "AZERBAIJAN", current.equals("azerbaijan"), Color.CYAN, "Azerbaijan Flag",
                                        "azerbaijan");
                        addItem(grid, "UKRAINE", current.equals("ukraine"), Color.YELLOW, "Ukraine Flag", "ukraine");
                        addItem(grid, "EGYPT", current.equals("egypt"), Color.RED, "Egypt Flag", "egypt");

                        // New Countries
                        addItem(grid, "AUSTRALIA", current.equals("australia"), Color.BLUE, "Australia Flag",
                                        "australia");
                        addItem(grid, "S.AFRICA", current.equals("south_africa"), Color.GREEN, "South Africa Flag",
                                        "south_africa");
                        addItem(grid, "S.ARABIA", current.equals("saudi_arabia"), Color.GREEN, "Saudi Arabia Flag",
                                        "saudi_arabia");
                        addItem(grid, "PAKISTAN", current.equals("pakistan"), Color.GREEN, "Pakistan Flag", "pakistan");
                        addItem(grid, "INDONESIA", current.equals("indonesia"), Color.RED, "Indonesia Flag",
                                        "indonesia");

                        // Soccer Teams
                        // --- TURKEY (Super Lig) ---
                        addItem(grid, "GALATASARAY", current.equals("team_galatasaray"), Color.rgb(169, 4, 50),
                                        "Turkish Lions", "team_galatasaray");
                        addItem(grid, "FENERBAHCE", current.equals("team_fenerbahce"), Color.rgb(0, 32, 91),
                                        "Turkish Canaries", "team_fenerbahce");
                        addItem(grid, "BESIKTAS", current.equals("team_besiktas"), Color.WHITE, "Turkish Eagles",
                                        "team_besiktas");
                        addItem(grid, "TRABZON", current.equals("team_trabzon"), Color.rgb(125, 0, 48), "Turkish Storm",
                                        "team_trabzon");
                        addItem(grid, "BASAKSEHIR", current.equals("team_basaksehir"), Color.rgb(235, 96, 11), "Owls",
                                        "team_basaksehir");
                        addItem(grid, "ADANA DEMIR", current.equals("team_adana"), Color.rgb(40, 93, 161),
                                        "Blue Lightning", "team_adana");
                        addItem(grid, "SAMSUN", current.equals("team_samsun"), Color.RED, "Red Lightning",
                                        "team_samsun");
                        addItem(grid, "GOZTEPE", current.equals("team_goztepe"), Color.YELLOW, "Goz Goz",
                                        "team_goztepe");
                        addItem(grid, "SIVAS", current.equals("team_sivas"), Color.RED, "Yigidos", "team_sivas");
                        addItem(grid, "KONYA", current.equals("team_konya"), Color.GREEN, "Eagles", "team_konya");

                        // --- ENGLAND (Premier League) ---
                        addItem(grid, "MAN CITY", current.equals("team_man_city"), Color.CYAN, "Cityzens",
                                        "team_man_city");
                        addItem(grid, "ARSENAL", current.equals("team_arsenal"), Color.RED, "Gunners", "team_arsenal");
                        addItem(grid, "LIVERPOOL", current.equals("team_liverpool"), Color.RED, "The Reds",
                                        "team_liverpool");
                        addItem(grid, "ASTON VILLA", current.equals("team_aston_villa"), Color.rgb(103, 14, 54),
                                        "Villans", "team_aston_villa");
                        addItem(grid, "TOTTENHAM", current.equals("team_tottenham"), Color.WHITE, "Spurs",
                                        "team_tottenham");
                        addItem(grid, "CHELSEA", current.equals("team_chelsea"), Color.BLUE, "The Blues",
                                        "team_chelsea");
                        addItem(grid, "NEWCASTLE", current.equals("team_newcastle"), Color.BLACK, "Magpies",
                                        "team_newcastle");
                        addItem(grid, "MAN UTD", current.equals("team_man_utd"), Color.RED, "Red Devils",
                                        "team_man_utd");
                        addItem(grid, "WEST HAM", current.equals("team_westham"), Color.rgb(122, 38, 58), "Hammers",
                                        "team_westham");
                        addItem(grid, "BRIGHTON", current.equals("team_brighton"), Color.BLUE, "Seagulls",
                                        "team_brighton");

                        // --- SPAIN (La Liga) ---
                        addItem(grid, "R.MADRID", current.equals("team_real_madrid"), Color.WHITE, "Los Blancos",
                                        "team_real_madrid");
                        addItem(grid, "GIRONA", current.equals("team_girona"), Color.RED, "Gironistes", "team_girona");
                        addItem(grid, "BARCELONA", current.equals("team_barcelona"), Color.rgb(165, 0, 68), "Blaugrana",
                                        "team_barcelona");
                        addItem(grid, "ATLETICO", current.equals("team_atletico"), Color.RED, "Colchoneros",
                                        "team_atletico");
                        addItem(grid, "ATH.BILBAO", current.equals("team_bilbao"), Color.RED, "Lions", "team_bilbao");
                        addItem(grid, "SOCIEDAD", current.equals("team_sociedad"), Color.BLUE, "White & Blue",
                                        "team_sociedad");
                        addItem(grid, "BETIS", current.equals("team_betis"), Color.GREEN, "Beticos", "team_betis");
                        addItem(grid, "VALENCIA", current.equals("team_valencia"), Color.WHITE, "The Bats",
                                        "team_valencia");
                        addItem(grid, "VILLARREAL", current.equals("team_villarreal"), Color.YELLOW, "Yellow Sub",
                                        "team_villarreal");
                        addItem(grid, "SEVILLA", current.equals("team_sevilla"), Color.WHITE, "Sevillistas",
                                        "team_sevilla");

                        // --- GERMANY (Bundesliga) ---
                        addItem(grid, "LEVERKUSEN", current.equals("team_leverkusen"), Color.BLACK, "Die Werkself",
                                        "team_leverkusen");
                        addItem(grid, "BAYERN", current.equals("team_bayern"), Color.RED, "The Bavarians",
                                        "team_bayern");
                        addItem(grid, "STUTTGART", current.equals("team_stuttgart"), Color.WHITE, "Die Schwaben",
                                        "team_stuttgart");
                        addItem(grid, "LEIPZIG", current.equals("team_leipzig"), Color.WHITE, "Red Bulls",
                                        "team_leipzig");
                        addItem(grid, "DORTMUND", current.equals("team_dortmund"), Color.YELLOW, "Black & Yellow",
                                        "team_dortmund");
                        addItem(grid, "FRANKFURT", current.equals("team_frankfurt"), Color.RED, "The Eagles",
                                        "team_frankfurt");
                        addItem(grid, "HOFFENHEIM", current.equals("team_hoffenheim"), Color.BLUE, "Die Kraichgauer",
                                        "team_hoffenheim");
                        addItem(grid, "BREMEN", current.equals("team_bremen"), Color.GREEN, "Werder", "team_bremen");
                        addItem(grid, "FREIBURG", current.equals("team_freiburg"), Color.BLACK, "Breisgau",
                                        "team_freiburg");
                        addItem(grid, "AUGSBURG", current.equals("team_augsburg"), Color.WHITE, "Fugger",
                                        "team_augsburg");

                        // --- FRANCE (Ligue 1) ---
                        addItem(grid, "PSG", current.equals("team_psg"), Color.BLUE, "Les Parisiens", "team_psg");
                        addItem(grid, "MONACO", current.equals("team_monaco"), Color.RED, "Les Monegasques",
                                        "team_monaco");
                        addItem(grid, "BREST", current.equals("team_brest"), Color.RED, "Les Pirates", "team_brest");
                        addItem(grid, "LILLE", current.equals("team_lille"), Color.RED, "Les Dogues", "team_lille");
                        addItem(grid, "NICE", current.equals("team_nice"), Color.BLACK, "Les Aiglons", "team_nice");
                        addItem(grid, "LENS", current.equals("team_lens"), Color.YELLOW, "Sang et Or", "team_lens");
                        addItem(grid, "MARSEILLE", current.equals("team_marseille"), Color.CYAN, "Les Phoceens",
                                        "team_marseille");
                        addItem(grid, "LYON", current.equals("team_lyon"), Color.WHITE, "Les Gones", "team_lyon");
                        addItem(grid, "RENNES", current.equals("team_rennes"), Color.RED, "Les Rouges", "team_rennes");
                        addItem(grid, "REIMS", current.equals("team_reims"), Color.RED, "Les Rouges", "team_reims");

                        // Premium
                        addItem(grid, "CYBER", current.equals("cyber_core"), Color.CYAN, "Metallic Cyber Core",
                                        "cyber_core");
                        addItem(grid, "SOLAR", current.equals("solar_flare"), Color.YELLOW, "Burning Solar Surface",
                                        "solar_flare");
                        addItem(grid, "FROST", current.equals("frost_bite"), Color.rgb(200, 240, 255),
                                        "Icy Frozen Core", "frost_bite");

                } else if (currentCategory == 1) { // TRAILS
                        String currentTrail = prefs.getString("selectedTrail", "none");
                        addItem(grid, "RED", currentTrail.contains("red"), Color.RED, "Basic Red Path", "trail_red");
                        addItem(grid, "BLUE", currentTrail.contains("blue"), Color.BLUE, "Basic Blue Path",
                                        "trail_blue");
                        addItem(grid, "GREEN", currentTrail.contains("green"), Color.GREEN, "Basic Green Path",
                                        "trail_green");
                        addItem(grid, "GOLD", currentTrail.contains("gold"), Color.rgb(255, 215, 0),
                                        "Golden Luxury Path", "trail_gold");
                        addItem(grid, "NEON", currentTrail.contains("neon"), Color.CYAN, "Bright Neon Light",
                                        "trail_neon");
                        addItem(grid, "COSMIC", currentTrail.contains("cosmic"), Color.MAGENTA, "Cosmic Dust",
                                        "trail_cosmic");
                        addItem(grid, "LAVA", currentTrail.contains("lava"), Color.rgb(255, 69, 0), "Lava Trail",
                                        "trail_lava");
                        addItem(grid, "ELECTRIC", currentTrail.contains("electric"), Color.CYAN, "Electric Spark Path",
                                        "trail_electric");
                        addItem(grid, "RAINBOW", currentTrail.contains("rainbow"), Color.WHITE, "Rainbow Path",
                                        "trail_rainbow");
                        addItem(grid, "GHOST", currentTrail.contains("ghost"), Color.WHITE, "Ghost Fade Path",
                                        "trail_ghost");
                        addItem(grid, "BUBBLE", currentTrail.contains("bubble"), Color.CYAN, "Blue Bubble Path",
                                        "trail_bubble");
                        addItem(grid, "PIXEL", currentTrail.contains("pixel"), Color.GREEN, "Green Pixel Path",
                                        "trail_pixel");
                        addItem(grid, "DNA", currentTrail.contains("dna"), Color.MAGENTA, "Double Helix Trails",
                                        "trail_dna");
                        addItem(grid, "SPARKLE", currentTrail.contains("sparkle"), Color.YELLOW, "Glittering Sparkles",
                                        "trail_sparkle");
                        // New Trails
                        addItem(grid, "MATRIX", currentTrail.contains("matrix"), Color.GREEN, "Digital Rain Path",
                                        "trail_matrix");
                        addItem(grid, "SAKURA", currentTrail.contains("sakura"), Color.rgb(255, 183, 197),
                                        "Cherry Blossom", "trail_sakura");
                        addItem(grid, "VOID", currentTrail.contains("void"), Color.LTGRAY, "Abyssal Void Path",
                                        "trail_void");
                        addItem(grid, "CRYSTAL", currentTrail.contains("crystal"), Color.CYAN, "Crystal Shards",
                                        "trail_crystal");
                        addItem(grid, "MUSIC", currentTrail.contains("music"), Color.WHITE, "Melodic Notes",
                                        "trail_music");
                        addItem(grid, "HEARTBEAT", currentTrail.contains("heartbeat"), Color.RED, "Cardio Pulse",
                                        "trail_heartbeat");
                        addItem(grid, "COMET", currentTrail.contains("comet"), Color.rgb(255, 140, 0),
                                        "Cosmic Fireball", "trail_comet");

                } else if (currentCategory == 2) { // SIGHTS
                        String currentTraj = prefs.getString("selectedTrajectory", "dashed");
                        addItem(grid, "LASER SIGHT", currentTraj.equals("laser"), Color.RED, "Solid Red Laser Guide",
                                        "traj_laser");
                        addItem(grid, "ELEC SIGHT", currentTraj.equals("electric"), Color.CYAN, "Electric Guide Line",
                                        "traj_electric");
                        addItem(grid, "PEARL SIGHT", currentTraj.equals("dots"), Color.YELLOW, "Dotted Pearl Guide",
                                        "traj_dots");
                        addItem(grid, "PLASMA SIGHT", currentTraj.equals("plasma"), Color.MAGENTA,
                                        "Glowing Plasma Guide", "traj_plasma");

                        // New Unique Sights
                        addItem(grid, "SNIPER", currentTraj.equals("sniper"), Color.RED, "Crosshair Markers",
                                        "traj_sniper");
                        addItem(grid, "DOUBLE", currentTraj.equals("double"), Color.CYAN, "Twin Parallel Lines",
                                        "traj_double");
                        addItem(grid, "RAINBOW", currentTraj.equals("rainbow"), Color.YELLOW, "Color Shifting Path",
                                        "traj_rainbow");
                        addItem(grid, "DASHDOT", currentTraj.equals("dashdot"), Color.WHITE, "Morse Code Style",
                                        "traj_dashdot");
                        addItem(grid, "STARS", currentTraj.equals("stars"), Color.YELLOW, "Starry Path",
                                        "traj_stars");
                        addItem(grid, "HEARTS", currentTraj.equals("hearts"), Color.rgb(255, 105, 180),
                                        "Floating Hearts",
                                        "traj_hearts");
                        addItem(grid, "TECH", currentTraj.equals("tech"), Color.rgb(0, 255, 0), "Circuit Board Path",
                                        "traj_tech");
                        addItem(grid, "SNAKE", currentTraj.equals("snake"), Color.GREEN, "Winding S-Curve",
                                        "traj_snake");
                        addItem(grid, "CHEVRON", currentTraj.equals("chevron"), Color.rgb(255, 165, 0),
                                        "Forward Arrows",
                                        "traj_chevron");
                        addItem(grid, "FIRE", currentTraj.equals("fire"), Color.rgb(255, 69, 0), "Burning Flame Path",
                                        "traj_fire");
                        addItem(grid, "ARROW SIGHT", currentTraj.equals("arrow"), Color.GREEN, "Arrow Guide",
                                        "traj_arrow");
                        addItem(grid, "WAVE SIGHT", currentTraj.equals("wave"), Color.CYAN, "Sine Wave Guide",
                                        "traj_wave");
                        // New Sights
                        addItem(grid, "GRID SIGHT", currentTraj.equals("grid"), Color.WHITE, "Projected Grid Guide",
                                        "traj_grid");
                        addItem(grid, "PULSE SIGHT", currentTraj.equals("pulse"), Color.RED, "Pulsating Line Guide",
                                        "traj_pulse");

                } else if (currentCategory == 3) { // EFFECTS
                        String currentBoom = prefs.getString("selectedImpact", "classic");
                        // 1. Classic (Shockwave)
                        addItem(grid, "SHOCKWAVE", currentBoom.equals("classic"), Color.WHITE, "Classic Shockwave",
                                        "classic");
                        // 2. Pixel
                        addItem(grid, "PIXEL", currentBoom.equals("pixel"), Color.GREEN, "Retro Pixel Burst",
                                        "impact_pixel");
                        // 3. Vortex
                        addItem(grid, "VORTEX", currentBoom.equals("vortex"), Color.rgb(100, 0, 255), "Swirling Vortex",
                                        "impact_vortex");
                        // 4. Sparks
                        addItem(grid, "SPARKS", currentBoom.equals("sparks"), Color.YELLOW, "Flying Sparks",
                                        "impact_sparks");
                        // 5. Hearts
                        addItem(grid, "HEARTS", currentBoom.equals("hearts"), Color.RED, "Lovely Hearts",
                                        "impact_hearts");
                        // 6. Skull
                        addItem(grid, "SKULL", currentBoom.equals("skull"), Color.LTGRAY, "Deadly Skulls",
                                        "impact_skull");
                        // 7. Music
                        addItem(grid, "MUSIC", currentBoom.equals("music"), Color.CYAN, "Musical Notes",
                                        "impact_music");
                        // 8. Lightning
                        addItem(grid, "LIGHTNING", currentBoom.equals("lightning"), Color.CYAN, "Electric Storm",
                                        "impact_lightning");
                        // 9. Confetti
                        addItem(grid, "CONFETTI", currentBoom.equals("confetti"), Color.MAGENTA, "Party Confetti",
                                        "impact_confetti");
                        // 10. Ghost
                        addItem(grid, "GHOST", currentBoom.equals("ghost"), Color.WHITE, "Spooky Ghosts",
                                        "impact_ghost");
                }
        }

        private void addItem(GridLayout grid, String name, boolean equipped, int color, String description,
                        String skinId) {
                // Determine price based on category
                int price = 0;
                boolean isDefaultItem = skinId.equals("default");

                if (!isDefaultItem) {
                        if (currentCategory == 0) { // Skins (Flags & Premium)
                                if (skinId.equals("tr_flag")) {
                                        price = 50;
                                } else if (skinId.equals("cyber_core") || skinId.equals("solar_flare")
                                                || skinId.equals("frost_bite")) {
                                        price = 60; // Premiums
                                } else {
                                        // Random-like distribution for other flags (10, 20, 30)
                                        int hash = Math.abs(skinId.hashCode());
                                        int variant = hash % 3;
                                        if (variant == 0)
                                                price = 10;
                                        else if (variant == 1)
                                                price = 20;
                                        else
                                                price = 30;
                                }
                        } else if (currentCategory == 1) { // Trails
                                // Base price 30, max 40 based on "newness"
                                // Higher hash/order implies newer? No, let's map manually or loosely based on
                                // string length/content
                                // Or explicitly list them.

                                // Explicit pricing for "newest" feeling
                                if (skinId.contains("dna") || skinId.contains("sparkle")
                                                || skinId.contains("pixel") || skinId.contains("matrix")
                                                || skinId.contains("sakura")
                                                || skinId.contains("void") || skinId.contains("crystal")
                                                || skinId.contains("music")
                                                || skinId.contains("heartbeat") || skinId.contains("comet"))
                                        price = 40;
                                else if (skinId.equals("trail_ghost") || skinId.equals("trail_bubble")
                                                || skinId.equals("trail_rainbow"))
                                        price = 38;
                                else if (skinId.equals("trail_lava") || skinId.equals("trail_cosmic")
                                                || skinId.equals("trail_electric"))
                                        price = 35;
                                else
                                        price = 30; // Basic trails
                                price = 30; // Basic trails
                        } else if (currentCategory == 2) {
                                if (skinId.equals("traj_sniper") || skinId.equals("traj_double")
                                                || skinId.equals("traj_rainbow") ||
                                                skinId.equals("traj_dashdot") || skinId.equals("traj_stars")
                                                || skinId.equals("traj_hearts") ||
                                                skinId.equals("traj_tech") || skinId.equals("traj_snake")
                                                || skinId.equals("traj_chevron") ||
                                                skinId.equals("traj_fire")) {
                                        price = 25; // Premium Sights
                                } else {
                                        price = 5; // Basic Sights
                                }
                        } else if (currentCategory == 3) {
                                price = 50; // Effects Price
                        }
                }

                // Check if already owned
                android.content.SharedPreferences prefs = getSharedPreferences("SpaceBilliard",
                                android.content.Context.MODE_PRIVATE);
                boolean isOwned = prefs.getBoolean("owned_" + skinId, isDefaultItem);

                String statusText = equipped ? "EQUIPPED" : (isOwned ? "SELECT" : price + " 💰");

                NeonShopItem item = new NeonShopItem(this, name, statusText, color, "x1", "GEM",
                                description);
                item.setSkinId(skinId);

                // Make variables final for lambda
                final int finalPrice = price;
                final boolean finalIsDefaultItem = isDefaultItem;
                final String finalSkinId = skinId;

                item.setOnClickListener(v -> {
                        descriptionText.setText(description);
                        descriptionText.setTextColor(color);

                        // Get current ownership status
                        android.content.SharedPreferences currentPrefs = getSharedPreferences("SpaceBilliard",
                                        android.content.Context.MODE_PRIVATE);
                        boolean owned = currentPrefs.getBoolean("owned_" + finalSkinId, finalIsDefaultItem);

                        if (owned) {
                                // ALREADY OWNED -> EQUIP IMMEDIATELY
                                equipItem(finalSkinId, currentPrefs);
                                refreshGrid();

                                descriptionText.setText(description + "\n\n✓ EQUIPPED");

                                // Clear selection for purchase since we just equipped
                                selectedItemId = null;
                                selectedItemPrice = 0;
                                selectedItemOwned = true;
                        } else {
                                // NOT OWNED -> SELECT FOR PURCHASE
                                if (finalSkinId.equals(selectedItemId)) {
                                        // SECOND TAP -> BUY
                                        handlePurchase();
                                } else {
                                        // FIRST TAP -> SELECT
                                        selectedItemId = finalSkinId;
                                        selectedItemPrice = finalPrice;
                                        selectedItemOwned = false;

                                        descriptionText.setText(description + "\n\n💰 " + finalPrice
                                                        + " coins - Tap again to BUY");
                                }
                        }
                });

                GridLayout.LayoutParams params = new GridLayout.LayoutParams();
                params.setMargins(10, 10, 10, 10);
                params.width = (int) (95 * getResources().getDisplayMetrics().density);
                params.height = (int) (130 * getResources().getDisplayMetrics().density);
                item.setLayoutParams(params);
                grid.addView(item);
        }

        private void equipItem(String skinId, android.content.SharedPreferences prefs) {
                if (skinId.startsWith("trail_")) {
                        String type = skinId.substring(6);
                        prefs.edit().putString("selectedTrail", type).apply();
                } else if (skinId.startsWith("aura_")) {
                        String type = skinId.substring(5);
                        prefs.edit().putString("selectedAura", type).apply();
                } else if (skinId.startsWith("traj_")) {
                        String type = skinId.substring(5);
                        prefs.edit().putString("selectedTrajectory", type).apply();
                } else if (skinId.startsWith("impact_")) {
                        String type = skinId.substring(7);
                        prefs.edit().putString("selectedImpact", type).apply();
                } else {
                        prefs.edit().putString("selectedSkin", skinId).apply();
                }
        }

        private void setupFooter(LinearLayout parent) {
                LinearLayout footer = new LinearLayout(this);
                footer.setOrientation(LinearLayout.HORIZONTAL);
                footer.setGravity(Gravity.CENTER);
                footer.setPadding(0, 20, 0, 40);

                // BUY BUTTON
                NeonButton buyBtn = new NeonButton(this, "BUY", Color.rgb(0, 255, 100));
                buyBtn.setOnClickListener(v -> handlePurchase());

                // BACK BUTTON
                NeonButton back = new NeonButton(this, "BACK", Color.rgb(255, 50, 255));
                back.setOnClickListener(v -> finish());

                LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
                                (int) (120 * getResources().getDisplayMetrics().density),
                                (int) (45 * getResources().getDisplayMetrics().density));
                btnParams.setMargins(20, 0, 20, 0);

                footer.addView(buyBtn, btnParams);
                footer.addView(back, btnParams);
                parent.addView(footer);
        }

        private void handlePurchase() {
                if (selectedItemId == null) {
                        descriptionText.setText("⚠️ Please select an item first!");
                        descriptionText.setTextColor(Color.YELLOW);
                        return;
                }

                if (selectedItemOwned) {
                        // Already owned, just equip
                        android.content.SharedPreferences prefs = getSharedPreferences("SpaceBilliard",
                                        android.content.Context.MODE_PRIVATE);
                        equipItem(selectedItemId, prefs);
                        descriptionText.setText("✓ Item equipped!");
                        descriptionText.setTextColor(Color.GREEN);
                        refreshGrid();
                        return;
                }

                // Need to purchase
                android.content.SharedPreferences prefs = getSharedPreferences("SpaceBilliard",
                                android.content.Context.MODE_PRIVATE);
                int currentCoins = prefs.getInt("coins", 0);

                if (currentCoins >= selectedItemPrice) {
                        // Purchase successful
                        prefs.edit()
                                        .putBoolean("owned_" + selectedItemId, true)
                                        .putInt("coins", currentCoins - selectedItemPrice)
                                        .apply();

                        // Update coin display
                        if (coinText != null) {
                                coinText.setText("💰 " + (currentCoins - selectedItemPrice));
                        }

                        // Equip the item
                        equipItem(selectedItemId, prefs);
                        descriptionText.setText("✓ Purchase successful!");
                        descriptionText.setTextColor(Color.GREEN);
                        refreshGrid();
                } else {
                        // Insufficient balance
                        descriptionText.setText("⚠️ INSUFFICIENT COINS! Need " + selectedItemPrice + " coins.");
                        descriptionText.setTextColor(Color.RED);
                }
        }
}

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
        private NeonButton btnSkins, btnTrails, btnSights, btnEffects;

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

                        // Premium
                        addItem(grid, "CYBER", current.equals("cyber_core"), Color.CYAN, "Metallic Cyber Core",
                                        "cyber_core");
                        addItem(grid, "SOLAR", current.equals("solar_flare"), Color.YELLOW, "Burning Solar Surface",
                                        "solar_flare");
                        addItem(grid, "FROST", current.equals("frost_bite"), Color.rgb(200, 240, 255),
                                        "Icy Frozen Core", "frost_bite");

                } else if (currentCategory == 1) { // TRAILS
                        String currentTrail = prefs.getString("selectedTrail", "none");
                        addItem(grid, "RED TRAIL", currentTrail.equals("red"), Color.RED, "Basic Red Path",
                                        "trail_red");
                        addItem(grid, "BLUE TRAIL", currentTrail.equals("blue"), Color.BLUE, "Basic Blue Path",
                                        "trail_blue");
                        addItem(grid, "GREEN TRAIL", currentTrail.equals("green"), Color.GREEN, "Basic Green Path",
                                        "trail_green");
                        addItem(grid, "GOLD TRAIL", currentTrail.equals("gold"), Color.rgb(255, 215, 0),
                                        "Golden Luxury Path", "trail_gold");
                        addItem(grid, "NEON TRAIL", currentTrail.equals("neon"), Color.CYAN, "Bright Neon Light",
                                        "trail_neon");
                        addItem(grid, "COSMIC", currentTrail.equals("cosmic"), Color.MAGENTA, "Cosmic Dust",
                                        "trail_cosmic");
                        addItem(grid, "LAVA", currentTrail.equals("lava"), Color.rgb(255, 69, 0), "Lava Trail",
                                        "trail_lava");
                        addItem(grid, "ELECTRIC", currentTrail.equals("electric"), Color.CYAN, "Electric Spark Path",
                                        "trail_electric");
                        addItem(grid, "RAINBOW", currentTrail.equals("rainbow"), Color.WHITE, "Rainbow Path",
                                        "trail_rainbow");
                        addItem(grid, "GHOST", currentTrail.equals("ghost"), Color.WHITE, "Ghost Fade Path",
                                        "trail_ghost");
                        addItem(grid, "BUBBLE", currentTrail.equals("bubble"), Color.CYAN, "Blue Bubble Path",
                                        "trail_bubble");
                        addItem(grid, "PIXEL", currentTrail.equals("pixel"), Color.GREEN, "Green Pixel Path",
                                        "trail_pixel");
                        // New Trails
                        addItem(grid, "DNA", currentTrail.equals("dna"), Color.MAGENTA, "Double Helix Trails",
                                        "trail_dna");
                        addItem(grid, "SPARKLE", currentTrail.equals("sparkle"), Color.YELLOW, "Glittering Sparkles",
                                        "trail_sparkle");

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
                        addItem(grid, "STAR BURST", currentBoom.equals("stars"), Color.YELLOW, "Explode into Stars!",
                                        "impact_stars");
                        addItem(grid, "ELEC BOOM", currentBoom.equals("electric"), Color.CYAN, "Electric Shockwave!",
                                        "impact_electric");
                        addItem(grid, "RIPPLE", currentBoom.equals("ripple"), Color.BLUE, "Water Ripple",
                                        "impact_ripple");
                        addItem(grid, "CONFETTI", currentBoom.equals("confetti"), Color.MAGENTA, "Party Confetti",
                                        "impact_confetti");
                        // New Effects
                        addItem(grid, "VORTEX", currentBoom.equals("vortex"), Color.rgb(100, 0, 255), "Swirling Vortex",
                                        "impact_vortex");
                        addItem(grid, "SHATTER", currentBoom.equals("shatter"), Color.WHITE, "Glass Shatter Effect",
                                        "impact_shatter");
                }
        }

        private void addItem(GridLayout grid, String name, boolean equipped, int color, String description,
                        String skinId) {
                // Price is hidden or default for now since we are focusing on categories
                NeonShopItem item = new NeonShopItem(this, name, equipped ? "EQUIPPED" : "SELECT", color, "x1", "GEM",
                                description);
                item.setSkinId(skinId);

                item.setOnClickListener(v -> {
                        descriptionText.setText(description);
                        descriptionText.setTextColor(color);

                        // Equip Logic
                        if (skinId != null) {
                                android.content.SharedPreferences prefs = getSharedPreferences("SpaceBilliard",
                                                android.content.Context.MODE_PRIVATE);
                                if (skinId.startsWith("trail_")) {
                                        String type = skinId.substring(6);
                                        if (prefs.getString("selectedTrail", "none").equals(type)) {
                                                // Keep equipped
                                        } else {
                                                prefs.edit().putString("selectedTrail", type).apply();
                                        }
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
                                // Refresh grid to show new equipped status
                                refreshGrid();

                                // Add visual feedback like small shake or highlighting (handled by grid refresh
                                // opacity maybe)
                        }
                });

                GridLayout.LayoutParams params = new GridLayout.LayoutParams();
                params.setMargins(10, 10, 10, 10);
                params.width = (int) (95 * getResources().getDisplayMetrics().density);
                params.height = (int) (130 * getResources().getDisplayMetrics().density);
                item.setLayoutParams(params);
                grid.addView(item);
        }

        private void setupFooter(LinearLayout parent) {
                LinearLayout footer = new LinearLayout(this);
                footer.setOrientation(LinearLayout.HORIZONTAL);
                footer.setGravity(Gravity.CENTER);
                footer.setPadding(0, 20, 0, 40);

                NeonButton back = new NeonButton(this, "BACK", Color.rgb(255, 50, 255));
                back.setOnClickListener(v -> finish());

                LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
                                (int) (120 * getResources().getDisplayMetrics().density),
                                (int) (45 * getResources().getDisplayMetrics().density));
                btnParams.setMargins(20, 0, 20, 0);

                footer.addView(back, btnParams);
                parent.addView(footer);
        }
}

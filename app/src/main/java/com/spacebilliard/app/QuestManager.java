package com.spacebilliard.app;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.ArrayList;
import java.util.List;

public class QuestManager {
    private static QuestManager instance;
    private List<Quest> allQuests;
    private SharedPreferences prefs;
    private static final String PREFS_NAME = "QuestPrefs";

    private QuestManager(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        initializeQuests();
        loadQuestProgress();
    }

    public static QuestManager getInstance(Context context) {
        if (instance == null) {
            instance = new QuestManager(context);
        }
        return instance;
    }

    private void initializeQuests() {
        allQuests = new ArrayList<>();

        // COMBAT QUESTS (1-10)
        allQuests.add(new Quest(1, "Destroyer I", "Destroy 1000 colored balls", 1000, 50, Quest.QuestType.COMBAT));
        allQuests.add(new Quest(2, "Dark Matter", "Destroy 500 black balls", 500, 75, Quest.QuestType.COMBAT));
        allQuests.add(new Quest(3, "Multi-Hit", "Hit 10 balls with one shot", 10, 100, Quest.QuestType.COMBAT));
        allQuests.add(new Quest(4, "Combo Master", "Get 180 combo hits", 180, 60, Quest.QuestType.COMBAT));
        allQuests.add(new Quest(5, "Destroyer II", "Destroy 2000 balls total", 2000, 100, Quest.QuestType.COMBAT));
        allQuests.add(new Quest(6, "Special Hunter", "Hit 700 special balls", 700, 80, Quest.QuestType.COMBAT));
        allQuests.add(new Quest(7, "Electric Storm", "Use Electric skill 40 times", 40, 70, Quest.QuestType.COMBAT));
        allQuests.add(new Quest(8, "Ice Age", "Activate Freeze 20 times", 20, 60, Quest.QuestType.COMBAT));
        allQuests.add(new Quest(9, "Shielded", "Use Barrier skill 80 times", 80, 90, Quest.QuestType.COMBAT));
        allQuests.add(new Quest(10, "Demolition", "Create 400 explosions", 400, 85, Quest.QuestType.COMBAT));

        // BOSS QUESTS (11-20)
        allQuests.add(new Quest(11, "Void Victor", "Defeat VOID TITAN", 1, 150, Quest.QuestType.BOSS));
        allQuests.add(new Quest(12, "Solar Slayer", "Defeat SOLARION", 1, 150, Quest.QuestType.BOSS));
        allQuests.add(new Quest(13, "Gravity Crusher", "Defeat GRAVITON", 1, 150, Quest.QuestType.BOSS));
        allQuests
                .add(new Quest(14, "Flawless Victory", "Defeat any boss without damage", 1, 200, Quest.QuestType.BOSS));
        allQuests.add(new Quest(15, "Boss Hunter", "Defeat 12 bosses", 12, 180, Quest.QuestType.BOSS));
        allQuests.add(new Quest(16, "Speed Demon", "Win boss fight in under 60s", 1, 250, Quest.QuestType.BOSS));
        allQuests.add(new Quest(17, "Anti-Freeze", "Defeat CRYO-STASIS without freeze", 1, 200, Quest.QuestType.BOSS));
        allQuests.add(new Quest(18, "Heavy Hitter", "Deal 200000 damage to bosses", 200000, 120, Quest.QuestType.BOSS));
        allQuests.add(new Quest(19, "Time Lord", "Defeat CHRONO-SHIFTER", 1, 300, Quest.QuestType.BOSS));
        allQuests.add(new Quest(20, "Ultimate Champion", "Complete all 40 boss fights", 40, 500, Quest.QuestType.BOSS));

        // LEVEL PROGRESS QUESTS (21-30)
        allQuests.add(new Quest(21, "Rookie", "Reach Level 40", 40, 40, Quest.QuestType.LEVEL));
        allQuests.add(new Quest(22, "Veteran", "Reach Level 120", 120, 100, Quest.QuestType.LEVEL));
        allQuests.add(new Quest(23, "Expert", "Reach Level 200", 200, 200, Quest.QuestType.LEVEL));
        allQuests.add(new Quest(24, "Century", "Complete 400 stages", 400, 150, Quest.QuestType.LEVEL));
        allQuests.add(new Quest(25, "Space Explorer I", "Clear Space 3", 30, 80, Quest.QuestType.LEVEL));
        allQuests.add(new Quest(26, "Space Explorer II", "Clear Space 5", 50, 120, Quest.QuestType.LEVEL));
        allQuests.add(new Quest(27, "Galaxy Master", "Unlock all 10 spaces", 100, 300, Quest.QuestType.LEVEL));
        allQuests.add(new Quest(28, "Perfection", "Complete level without losing life", 4, 100, Quest.QuestType.LEVEL));
        allQuests.add(new Quest(29, "Unstoppable", "Complete 40 levels in a row", 40, 150, Quest.QuestType.LEVEL));
        allQuests.add(new Quest(30, "Legend", "Reach Level 400", 400, 500, Quest.QuestType.LEVEL));

        // COLLECTION QUESTS (31-35)
        allQuests.add(new Quest(31, "Wealth I", "Collect 1000 coins", 1000, 100, Quest.QuestType.COLLECTION));
        allQuests.add(new Quest(32, "Wealth II", "Collect 2000 coins", 2000, 150, Quest.QuestType.COLLECTION));
        allQuests.add(new Quest(33, "Shopaholic", "Buy 20 items from shop", 20, 80, Quest.QuestType.COLLECTION));
        allQuests.add(new Quest(34, "Fashionista", "Unlock 40 different skins", 40, 120, Quest.QuestType.COLLECTION));
        allQuests.add(new Quest(35, "Trail Collector", "Buy all trail effects", 8, 200, Quest.QuestType.COLLECTION));

        // SURVIVAL QUESTS (36-40)
        allQuests.add(
                new Quest(36, "Endurance", "Survive 20 minutes in one level", 1200, 100, Quest.QuestType.SURVIVAL));
        allQuests.add(new Quest(37, "Marathon", "Play for 120 minutes total", 7200, 150, Quest.QuestType.SURVIVAL));
        allQuests.add(new Quest(38, "Untouchable", "No damage for 8 minutes", 480, 120, Quest.QuestType.SURVIVAL));
        allQuests.add(new Quest(39, "Edge of Death", "Survive with 1 HP for 120s", 120, 200, Quest.QuestType.SURVIVAL));
        allQuests.add(
                new Quest(40, "Last Second", "Complete with 0 seconds remaining", 4, 150, Quest.QuestType.SURVIVAL));

        // NEW QUESTS (41-50)
        // PRECISION & SKILL
        allQuests.add(new Quest(41, "Sharpshooter", "Hit 5 balls in one combo", 15, 120, Quest.QuestType.COMBAT));
        allQuests.add(
                new Quest(42, "Ricochet Master", "Hit 3+ balls after wall bounce", 50, 100, Quest.QuestType.COMBAT));

        // SPEED & AGILITY
        allQuests.add(
                new Quest(43, "Speed Clearer", "Complete stage in under 10 seconds", 5, 150, Quest.QuestType.LEVEL));
        allQuests.add(
                new Quest(44, "Lightning Reflexes", "Destroy 10 balls in 5 seconds", 20, 130, Quest.QuestType.COMBAT));

        // POWER & DOMINATION
        allQuests.add(new Quest(45, "Glass Cannon", "Defeat boss with 50 HP or less", 3, 250, Quest.QuestType.BOSS));
        allQuests.add(new Quest(46, "Ability Addict", "Use 10 different skills in one level", 15, 140,
                Quest.QuestType.COMBAT));

        // EXPLORER
        allQuests.add(
                new Quest(47, "Space Traveler", "Complete 50 stages in each space", 500, 300, Quest.QuestType.LEVEL));
        allQuests.add(
                new Quest(48, "Comeback King", "Complete level with only 1 life", 10, 160, Quest.QuestType.SURVIVAL));

        // COLLECTOR
        allQuests.add(new Quest(49, "Coin Millionaire", "Collect 10,000 coins total", 10000, 500,
                Quest.QuestType.COLLECTION));
        allQuests.add(new Quest(50, "Arsenal Master", "Hold 3 power-ups simultaneously", 25, 180,
                Quest.QuestType.COLLECTION));
    }

    public List<Quest> getAllQuests() {
        return allQuests;
    }

    public Quest getQuestById(int id) {
        for (Quest quest : allQuests) {
            if (quest.getId() == id) {
                return quest;
            }
        }
        return null;
    }

    public void updateQuestProgress(int questId, int progress) {
        Quest quest = getQuestById(questId);
        if (quest != null && !quest.isCompleted()) {
            quest.setCurrentProgress(progress);
            saveQuestProgress();
        }
    }

    public void incrementQuestProgress(int questId, int amount) {
        Quest quest = getQuestById(questId);
        if (quest != null && !quest.isCompleted()) {
            int oldProgress = quest.getCurrentProgress();
            quest.incrementProgress(amount);
            int newProgress = quest.getCurrentProgress();
            android.util.Log.d("QUEST_DEBUG", "Quest " + questId + ": " + oldProgress + " -> " + newProgress);
            saveQuestProgress();
            android.util.Log.d("QUEST_DEBUG", "Quest progress saved to SharedPreferences");
        } else if (quest == null) {
            android.util.Log.e("QUEST_DEBUG", "Quest " + questId + " is null!");
        } else {
            android.util.Log.d("QUEST_DEBUG", "Quest " + questId + " already completed");
        }
    }

    public void saveQuestProgress() {
        SharedPreferences.Editor editor = prefs.edit();
        for (Quest quest : allQuests) {
            editor.putInt("quest_" + quest.getId() + "_progress", quest.getCurrentProgress());
            editor.putBoolean("quest_" + quest.getId() + "_completed", quest.isCompleted());
            editor.putBoolean("quest_" + quest.getId() + "_claimed", quest.isClaimed());
        }
        editor.apply();
    }

    public void loadQuestProgress() {
        for (Quest quest : allQuests) {
            int progress = prefs.getInt("quest_" + quest.getId() + "_progress", 0);
            boolean completed = prefs.getBoolean("quest_" + quest.getId() + "_completed", false);
            boolean claimed = prefs.getBoolean("quest_" + quest.getId() + "_claimed", false);
            quest.setCurrentProgress(progress);
            quest.setCompleted(completed);
            quest.setClaimed(claimed);
        }
    }

    public int getCompletedQuestsCount() {
        int count = 0;
        for (Quest quest : allQuests) {
            if (quest.isCompleted())
                count++;
        }
        return count;
    }
}

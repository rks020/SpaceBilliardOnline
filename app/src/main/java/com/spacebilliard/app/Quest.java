package com.spacebilliard.app;

public class Quest {
    private int id;
    private String title;
    private String description;
    private int currentProgress;
    private int targetProgress;
    private int coinReward;
    private boolean isCompleted;
    private boolean isClaimed; // For reward claiming
    private QuestType type;

    public enum QuestType {
        COMBAT, BOSS, LEVEL, COLLECTION, SURVIVAL
    }

    public Quest(int id, String title, String description, int targetProgress, int coinReward, QuestType type) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.targetProgress = targetProgress;
        this.coinReward = coinReward;
        this.type = type;
        this.currentProgress = 0;
        this.isCompleted = false;
    }

    // Getters
    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public int getCurrentProgress() {
        return currentProgress;
    }

    public int getTargetProgress() {
        return targetProgress;
    }

    public int getCoinReward() {
        return coinReward;
    }

    public boolean isCompleted() {
        return isCompleted;
    }

    public QuestType getType() {
        return type;
    }

    // Setters
    public void setCurrentProgress(int progress) {
        this.currentProgress = progress;
        if (this.currentProgress >= this.targetProgress) {
            this.isCompleted = true;
        }
    }

    public void incrementProgress(int amount) {
        this.currentProgress += amount;
        if (this.currentProgress >= this.targetProgress) {
            this.isCompleted = true;
        }
    }

    public void setCompleted(boolean completed) {
        this.isCompleted = completed;
    }

    public float getProgressPercentage() {
        if (targetProgress == 0)
            return 0;
        return Math.min(100f, (currentProgress * 100f) / targetProgress);
    }

    public boolean isClaimed() {
        return isClaimed;
    }

    public void setClaimed(boolean claimed) {
        this.isClaimed = claimed;
    }
}

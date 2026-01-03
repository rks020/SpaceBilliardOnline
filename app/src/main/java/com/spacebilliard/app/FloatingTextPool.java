package com.spacebilliard.app;

import java.util.Stack;

/**
 * Object pool for FloatingText to reduce GC pressure
 */
public class FloatingTextPool {
    private final Stack<GameView.FloatingText> pool = new Stack<>();
    private final GameView gameView;

    public FloatingTextPool(GameView gameView) {
        this.gameView = gameView;
    }

    public GameView.FloatingText obtain(String text, float x, float y, int color) {
        if (pool.isEmpty()) {
            return gameView.new FloatingText(text, x, y, color);
        }
        return pool.pop().reset(text, x, y, color);
    }

    public GameView.FloatingText obtain(String text, float x, float y, int color, float sizeScale) {
        if (pool.isEmpty()) {
            return gameView.new FloatingText(text, x, y, color, sizeScale);
        }
        return pool.pop().reset(text, x, y, color, sizeScale);
    }

    public void free(GameView.FloatingText text) {
        if (text != null && pool.size() < 20) {
            pool.push(text);
        }
    }

    public void clear() {
        pool.clear();
    }
}

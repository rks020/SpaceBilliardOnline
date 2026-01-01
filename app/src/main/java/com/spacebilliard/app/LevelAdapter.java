package com.spacebilliard.app;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class LevelAdapter extends RecyclerView.Adapter<LevelAdapter.LevelViewHolder> {

    private final Context context;
    private final int maxUnlockedLevel;
    private final int totalLevels = 100; // Example total

    public LevelAdapter(Context context, int maxUnlockedLevel) {
        this.context = context;
        this.maxUnlockedLevel = maxUnlockedLevel;
    }

    @NonNull
    @Override
    public LevelViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_level, parent, false);
        return new LevelViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LevelViewHolder holder, int position) {
        int level = position + 1;
        boolean isLocked = level > maxUnlockedLevel;
        boolean isBoss = (level % 10 == 0); // Every 10th level is a boss
        boolean isCompleted = level < maxUnlockedLevel; // Simple logic for now

        if (isLocked) {
            holder.bgView.setBackgroundResource(R.drawable.bg_level_locked);
            holder.txtLevelNum.setVisibility(View.GONE);
            holder.imgLock.setVisibility(View.VISIBLE);
            holder.txtBossLabel.setVisibility(View.GONE);
            holder.starsContainer.setVisibility(View.GONE);
            holder.itemView.setClickable(false);
            holder.itemView.setAlpha(0.5f);
        } else {
            // Unlocked
            holder.itemView.setClickable(true);
            holder.itemView.setAlpha(1.0f);
            holder.imgLock.setVisibility(View.GONE);
            holder.txtLevelNum.setVisibility(View.VISIBLE);
            holder.txtLevelNum.setText(String.valueOf(level));

            if (isBoss) {
                holder.bgView.setBackgroundResource(R.drawable.bg_level_boss);
                holder.txtLevelNum.setTextColor(android.graphics.Color.parseColor("#D500F9")); // Purple text
                holder.txtBossLabel.setVisibility(View.VISIBLE);
            } else {
                holder.bgView.setBackgroundResource(R.drawable.bg_level_active);
                holder.txtLevelNum.setTextColor(android.graphics.Color.WHITE);
                holder.txtBossLabel.setVisibility(View.GONE);
            }

            // Stars (Fake logic: if completed, show 3 stars. In real app, check detailed
            // progress)
            if (isCompleted) {
                holder.starsContainer.setVisibility(View.VISIBLE);
            } else {
                holder.starsContainer.setVisibility(View.GONE);
            }

            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(context, MainActivity.class);
                intent.putExtra("LEVEL", level);
                context.startActivity(intent);
            });
        }
    }

    @Override
    public int getItemCount() {
        return totalLevels;
    }

    static class LevelViewHolder extends RecyclerView.ViewHolder {
        View bgView;
        TextView txtLevelNum;
        ImageView imgLock;
        TextView txtBossLabel;
        LinearLayout starsContainer;

        public LevelViewHolder(@NonNull View itemView) {
            super(itemView);
            bgView = itemView.findViewById(R.id.bgLevel);
            txtLevelNum = itemView.findViewById(R.id.txtLevelNum);
            imgLock = itemView.findViewById(R.id.imgLock);
            txtBossLabel = itemView.findViewById(R.id.txtBossLabel);
            starsContainer = itemView.findViewById(R.id.starsContainer);
        }
    }
}

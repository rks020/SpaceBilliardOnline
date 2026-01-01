package com.spacebilliard.app;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class QuestAdapter extends RecyclerView.Adapter<QuestAdapter.QuestViewHolder> {

    private List<Quest> quests;

    public QuestAdapter(List<Quest> quests) {
        this.quests = quests;
    }

    @NonNull
    @Override
    public QuestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.quest_card, parent, false);
        return new QuestViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull QuestViewHolder holder, int position) {
        Quest quest = quests.get(position);

        holder.tvTitle.setText(quest.getTitle());
        holder.tvDescription.setText(quest.getDescription());
        holder.tvCoinReward.setText("💰 " + quest.getCoinReward());
        holder.tvProgress.setText(quest.getCurrentProgress() + "/" + quest.getTargetProgress());
        holder.progressBar.setProgress((int) quest.getProgressPercentage());

        // Grayed out if not completed
        if (quest.isCompleted()) {
            holder.cardView.setAlpha(1.0f);
            holder.tvCompleted.setVisibility(View.VISIBLE);
            holder.progressBar.setVisibility(View.GONE);
            holder.tvProgress.setVisibility(View.GONE);
            holder.tvTitle.setTextColor(Color.parseColor("#00FF00"));

            // Show claim button if not yet claimed
            if (!quest.isClaimed()) {
                holder.btnClaimReward.setVisibility(View.VISIBLE);
                holder.btnClaimReward.setOnClickListener(v -> {
                    // Claim reward
                    quest.setClaimed(true);

                    // Add coins
                    android.content.SharedPreferences prefs = holder.itemView.getContext()
                            .getSharedPreferences("SpaceBilliard", android.content.Context.MODE_PRIVATE);
                    int currentCoins = prefs.getInt("coins", 0);
                    prefs.edit().putInt("coins", currentCoins + quest.getCoinReward()).apply();

                    // Save claimed status
                    QuestManager.getInstance(holder.itemView.getContext()).saveQuestProgress();

                    // Hide button
                    holder.btnClaimReward.setVisibility(View.GONE);

                    // Show toast
                    android.widget.Toast.makeText(holder.itemView.getContext(),
                            "+" + quest.getCoinReward() + " coins!",
                            android.widget.Toast.LENGTH_SHORT).show();
                });
            } else {
                holder.btnClaimReward.setVisibility(View.GONE);
            }
        } else {
            holder.cardView.setAlpha(0.6f);
            holder.tvCompleted.setVisibility(View.GONE);
            holder.progressBar.setVisibility(View.VISIBLE);
            holder.tvProgress.setVisibility(View.VISIBLE);
            holder.tvTitle.setTextColor(Color.parseColor("#FFFFFF"));
            holder.btnClaimReward.setVisibility(View.GONE);
        }

        // Type-based color coding
        String typeColor = "#FFFFFF";
        switch (quest.getType()) {
            case COMBAT:
                typeColor = "#FF6B6B"; // Red
                break;
            case BOSS:
                typeColor = "#FF4500"; // Orange-Red
                break;
            case LEVEL:
                typeColor = "#4169E1"; // Royal Blue
                break;
            case COLLECTION:
                typeColor = "#FFD700"; // Gold
                break;
            case SURVIVAL:
                typeColor = "#32CD32"; // Lime Green
                break;
        }
        holder.tvDescription.setTextColor(Color.parseColor(typeColor));
    }

    @Override
    public int getItemCount() {
        return quests.size();
    }

    public void updateQuests(List<Quest> newQuests) {
        this.quests = newQuests;
        notifyDataSetChanged();
    }

    static class QuestViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        TextView tvTitle, tvDescription, tvCoinReward, tvProgress, tvCompleted;
        ProgressBar progressBar;
        android.widget.Button btnClaimReward;

        public QuestViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.questCard);
            tvTitle = itemView.findViewById(R.id.tvQuestTitle);
            tvDescription = itemView.findViewById(R.id.tvQuestDescription);
            tvCoinReward = itemView.findViewById(R.id.tvCoinReward);
            tvProgress = itemView.findViewById(R.id.tvProgress);
            tvCompleted = itemView.findViewById(R.id.tvCompleted);
            progressBar = itemView.findViewById(R.id.progressBar);
            btnClaimReward = itemView.findViewById(R.id.btnClaimReward);
        }
    }
}

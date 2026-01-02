package com.spacebilliard.app;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class QuestsActivity extends AppCompatActivity {

    private RecyclerView rvQuests;
    private QuestAdapter adapter;
    private QuestManager questManager;
    private TextView tvQuestCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quests);

        // Initialize Quest Manager
        questManager = QuestManager.getInstance(this);

        // Setup UI
        tvQuestCount = findViewById(R.id.tvQuestCount);
        rvQuests = findViewById(R.id.rvQuests);
        Button btnBack = findViewById(R.id.btnBack);

        // Setup RecyclerView
        rvQuests.setLayoutManager(new LinearLayoutManager(this));
        List<Quest> quests = questManager.getAllQuests();
        adapter = new QuestAdapter(quests);
        rvQuests.setAdapter(adapter);

        // Update quest count
        updateQuestCount();

        // Back button
        btnBack.setOnClickListener(v -> {
            v.animate()
                    .scaleX(0.96f)
                    .scaleY(0.96f)
                    .setDuration(80)
                    .withEndAction(() -> {
                        v.animate().scaleX(1f).scaleY(1f).setDuration(80).start();
                        finish();
                    })
                    .start();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh quest progress when returning to this screen
        questManager.loadQuestProgress();
        adapter.updateQuests(questManager.getAllQuests());
        updateQuestCount();
    }

    private void updateQuestCount() {
        int completed = questManager.getCompletedQuestsCount();
        tvQuestCount.setText(completed + "/40");
    }
}

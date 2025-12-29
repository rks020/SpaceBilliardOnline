package com.spacebilliard.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.spacebilliard.app.network.OnlineGameManager;
import com.spacebilliard.app.network.SupabaseManager;

import java.util.List;

public class OnlineActivity extends Activity {

    private OnlineGameManager gameManager;
    private EditText usernameInput;
    private EditText roomNameInput;
    private LinearLayout roomListContainer;
    private Button createRoomBtn;
    private Button refreshBtn;
    private Button backBtn;

    // New Stats Views
    private TextView textWins;
    private TextView textLosses;

    private String currentUsername;
    private boolean isConnected = false;
    public SupabaseManager supabaseManager;
    public SupabaseManager.UserProfile userProfile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Full screen settings
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        // Set XML layout
        setContentView(R.layout.activity_online);

        // Find views
        usernameInput = findViewById(R.id.editCommanderName);
        roomNameInput = findViewById(R.id.editRoomName);
        createRoomBtn = findViewById(R.id.btnCreateRoom);
        refreshBtn = findViewById(R.id.btnRefresh);
        backBtn = findViewById(R.id.btnBack);
        roomListContainer = findViewById(R.id.roomListContainer);

        // Find Stats Views
        textWins = findViewById(R.id.textWins);
        textLosses = findViewById(R.id.textLosses);

        // Set button listeners immediately
        createRoomBtn.setOnClickListener(v -> createRoom());
        createRoomBtn.setClickable(true);
        refreshBtn.setOnClickListener(v -> refreshRooms());
        refreshBtn.setClickable(true);
        backBtn.setOnClickListener(v -> finish());
        backBtn.setClickable(true);

        // Initialize game manager
        gameManager = new OnlineGameManager(this);
        ((OnlineApplication) getApplication()).setGameManager(gameManager);

        // Initialize Supabase
        supabaseManager = SupabaseManager.getInstance(this);

        // Load profile and auto-connect
        loadUserProfile();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (createRoomBtn != null)
            createRoomBtn.setEnabled(true); // Safety reset
        if (refreshBtn != null)
            refreshBtn.setEnabled(true);
        // Reload stats when returning from match
        if (supabaseManager != null) {
            loadUserProfile();
        }
    }

    public void startGame(String roomId, String hostUsername, String guestUsername, boolean isHost) {
        runOnUiThread(() -> {
            android.content.Intent intent = new android.content.Intent(this, OnlineGameActivity.class);
            intent.putExtra("roomId", roomId);
            intent.putExtra("hostUsername", hostUsername);
            intent.putExtra("guestUsername", guestUsername);
            intent.putExtra("isHost", isHost);
            startActivity(intent);
        });
    }

    private void loadUserProfile() {
        if (supabaseManager == null)
            return;

        supabaseManager.getUserProfile(new SupabaseManager.ProfileCallback() {
            @Override
            public void onSuccess(SupabaseManager.UserProfile profile) {
                runOnUiThread(() -> {
                    userProfile = profile;
                    currentUsername = profile.username;
                    usernameInput.setText(profile.username);
                    usernameInput.setEnabled(false);

                    // Update new Stats Views
                    textWins.setText(String.valueOf(profile.wins));
                    textLosses.setText(String.valueOf(profile.losses));

                    // Auto-connect if not connected
                    if (!isConnected) {
                        connectToServer();
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    if (error.equals("NO_PROFILE")) {
                        showNicknameDialog();
                    } else {
                        // Silent fail or toast
                        // Toast.makeText(OnlineActivity.this, "Profile Error: " + error,
                        // Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    private void showNicknameDialog() {
        // Create Custom Dialog
        android.app.Dialog dialog = new android.app.Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_nickname);
        dialog.getWindow().setBackgroundDrawable(
                new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        dialog.setCancelable(false);

        // Init Views
        android.widget.EditText nicknameInput = dialog.findViewById(R.id.nicknameInput);
        android.widget.Button btnCreate = dialog.findViewById(R.id.btnCreate);

        btnCreate.setOnClickListener(v -> {
            String nickname = nicknameInput.getText().toString().trim();
            if (nickname.isEmpty()) {
                Toast.makeText(OnlineActivity.this, "Nickname cannot be empty!", Toast.LENGTH_SHORT).show();
            } else if (!com.spacebilliard.app.utils.ProfanityFilter.isValidUsername(nickname)) {
                Toast.makeText(OnlineActivity.this, "Invalid nickname! Please choose another.", Toast.LENGTH_SHORT)
                        .show();
            } else {
                // Check if username exists
                supabaseManager.checkUsernameExists(nickname, new SupabaseManager.UsernameCheckCallback() {
                    @Override
                    public void onResult(boolean exists) {
                        runOnUiThread(() -> {
                            if (exists) {
                                Toast.makeText(OnlineActivity.this, "This username already exists!", Toast.LENGTH_SHORT)
                                        .show();
                            } else {
                                dialog.dismiss();
                                createUserProfile(nickname);
                            }
                        });
                    }

                    @Override
                    public void onError(String error) {
                        runOnUiThread(() -> {
                            Toast.makeText(OnlineActivity.this, "Error checking username: " + error, Toast.LENGTH_SHORT)
                                    .show();
                        });
                    }
                });
            }
        });

        dialog.show();
    }

    private void createUserProfile(String nickname) {
        supabaseManager.createProfile(nickname, new SupabaseManager.ProfileCallback() {
            @Override
            public void onSuccess(SupabaseManager.UserProfile profile) {
                runOnUiThread(() -> {
                    loadUserProfile(); // Reload to set UI and connect
                    Toast.makeText(OnlineActivity.this, "Profile created!", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(OnlineActivity.this, "Error: " + error, Toast.LENGTH_SHORT).show();
                    showNicknameDialog();
                });
            }
        });
    }

    private void connectToServer() {
        String username = usernameInput.getText().toString().trim();
        if (username.isEmpty())
            return;

        currentUsername = username;
        gameManager.connect(username, new OnlineGameManager.OnConnectionListener() {
            @Override
            public void onConnected() {
                isConnected = true;
                runOnUiThread(() -> {
                    // Toast.makeText(OnlineActivity.this, "Connected", Toast.LENGTH_SHORT).show();
                    // // Removed to reduce spam
                    refreshRooms();
                });
            }

            @Override
            public void onDisconnected() {
                isConnected = false;
                // runOnUiThread(() -> Toast.makeText(OnlineActivity.this, "Disconnected",
                // Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> Toast
                        .makeText(OnlineActivity.this, "Conn Error: " + error, Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void createRoom() {
        android.util.Log.d("OnlineActivity", "CREATE BUTTON CLICKED!");
        createRoomBtn.setEnabled(false);

        // Safety: Re-enable button after 5 seconds if nothing happens
        new android.os.Handler().postDelayed(() -> {
            if (createRoomBtn != null && !createRoomBtn.isEnabled()) {
                android.util.Log.w("OnlineActivity", "Safety: Re-enabling create button");
                createRoomBtn.setEnabled(true);
            }
        }, 5000);

        String roomName = roomNameInput.getText().toString().trim();
        android.util.Log.d("OnlineActivity", "Room name: " + roomName);
        if (roomName.isEmpty()) {
            Toast.makeText(this, "Enter room name", Toast.LENGTH_SHORT).show();
            createRoomBtn.setEnabled(true);
            return;
        }

        if (com.spacebilliard.app.utils.ProfanityFilter.containsProfanity(roomName)) {
            Toast.makeText(this, "Room name contains inappropriate words!", Toast.LENGTH_SHORT).show();
            createRoomBtn.setEnabled(true);
            return;
        }

        android.util.Log.d("OnlineActivity", "isConnected: " + isConnected);
        if (!isConnected) {
            android.util.Log.d("OnlineActivity", "Not connected, connecting first...");
            // Connect and then create room
            String username = usernameInput.getText().toString().trim();
            if (username.isEmpty()) {
                Toast.makeText(this, "Enter username", Toast.LENGTH_SHORT).show();
                createRoomBtn.setEnabled(true);
                return;
            }

            currentUsername = username;
            gameManager.connect(username, new OnlineGameManager.OnConnectionListener() {
                @Override
                public void onConnected() {
                    isConnected = true;
                    android.util.Log.d("OnlineActivity", "Connected! Now creating room...");
                    runOnUiThread(() -> {
                        // Now create the room
                        doCreateRoom(roomName);
                    });
                }

                @Override
                public void onDisconnected() {
                    isConnected = false;
                }

                @Override
                public void onError(String error) {
                    runOnUiThread(() -> {
                        Toast.makeText(OnlineActivity.this, "Connection error: " + error, Toast.LENGTH_SHORT).show();
                        createRoomBtn.setEnabled(true);
                    });
                }
            });
            return;
        }

        doCreateRoom(roomName);
    }

    private void doCreateRoom(String roomName) {
        android.util.Log.d("OnlineActivity", "doCreateRoom: " + roomName);
        gameManager.createRoom(roomName, new OnlineGameManager.OnRoomListener() {
            @Override
            public void onRoomCreated(String roomId) {
                android.util.Log.d("OnlineActivity", "Room created! ID: " + roomId);
                runOnUiThread(() -> {
                    Toast.makeText(OnlineActivity.this, "Room created!", Toast.LENGTH_SHORT).show();
                    refreshRooms();
                    createRoomBtn.setEnabled(true);
                    roomNameInput.setText("");
                });
            }

            @Override
            public void onError(String error) {
                android.util.Log.e("OnlineActivity", "Room error: " + error);
                runOnUiThread(() -> {
                    Toast.makeText(OnlineActivity.this, "Error: " + error, Toast.LENGTH_SHORT).show();
                    createRoomBtn.setEnabled(true);
                });
            }
        });
    }

    private void refreshRooms() {
        if (!isConnected) {
            // Don't toast on auto-refresh if not connected yet, but button click should
            // toast
            // But here we can just try to connect
            if (currentUsername != null)
                connectToServer();
            return;
        }

        gameManager.getRooms(new OnlineGameManager.OnRoomsListener() {
            @Override
            public void onRoomsReceived(List<OnlineGameManager.RoomInfo> rooms) {
                runOnUiThread(() -> {
                    roomListContainer.removeAllViews();
                    for (OnlineGameManager.RoomInfo room : rooms) {
                        addRoomItem(room);
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> Toast.makeText(OnlineActivity.this, "Refresh Error: " + error, Toast.LENGTH_SHORT)
                        .show());
            }
        });
    }

    private void addRoomItem(OnlineGameManager.RoomInfo room) {
        View roomView = LayoutInflater.from(this).inflate(R.layout.item_room, roomListContainer, false);

        TextView nameText = roomView.findViewById(R.id.textRoomName);
        TextView hostText = roomView.findViewById(R.id.textHostName);
        Button joinBtn = roomView.findViewById(R.id.btnJoin);

        nameText.setText(room.name);
        hostText.setText("HOST: " + room.host + " (" + room.players + "/" + room.maxPlayers + ")");

        joinBtn.setOnClickListener(v -> joinRoom(room.id));

        // Add margin programmatically if needed, or rely on XML
        roomListContainer.addView(roomView);
    }

    private void joinRoom(String roomId) {
        gameManager.joinRoom(roomId, new OnlineGameManager.OnRoomListener() {
            @Override
            public void onRoomCreated(String id) {
                // Not used
            }

            @Override
            public void onError(String error) {
                runOnUiThread(
                        () -> Toast.makeText(OnlineActivity.this, "Join Error: " + error, Toast.LENGTH_SHORT).show());
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (gameManager != null) {
            gameManager.disconnect();
        }
    }
}

package com.spacebilliard.app.network;

import android.util.Log;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import com.spacebilliard.app.OnlineActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class OnlineGameManager {
    private static final String TAG = "OnlineGameManager";
    private static final String SERVER_URL = "wss://3865e024b1d3.ngrok-free.app";

    private WebSocket webSocket;
    private OkHttpClient client;
    private Gson gson;
    private OnConnectionListener connectionListener;
    private OnGameListener gameListener;
    private OnRoomsListener roomsListener;
    private OnRoomListener roomListener; // Added this line
    private OnlineActivity activity;

    private String clientId;
    private String username;
    private String currentRoomId;
    private boolean isHost;

    public OnlineGameManager(OnlineActivity activity) {
        this.activity = activity;
        this.gson = new Gson();

        this.client = new OkHttpClient.Builder()
                .readTimeout(0, TimeUnit.MILLISECONDS)
                .build();
    }

    public interface OnConnectionListener {
        void onConnected();

        void onDisconnected();

        void onError(String error);
    }

    public interface OnRoomListener {
        void onRoomCreated(String roomId);

        void onError(String error);
    }

    public interface OnRoomsListener {
        void onRoomsReceived(java.util.List<RoomInfo> rooms);

        void onError(String error);
    }

    public interface OnGameListener {
        void onPlayerJoined(String username);

        void onPlayerLeft();

        void onGameStart();

        void onGameStateReceived(String hostCueBallJson, String guestCueBallJson, String ballsJson); // Initial
                                                                                                     // positions

        void onGameStateUpdate(String hostCueBallJson, String guestCueBallJson, String ballsJson, long timeLeft,
                int currentSet);

        void onOpponentShot(float angle, float power);

        void onBallsUpdate(int hostBalls, int guestBalls);

        void onBallDestroyed(float x, float y, String colorHex); // For explosion effects

        void onSetEnded(String winner, int hostScore, int guestScore, int currentSet);

        void onRematchRequested(String requestingUser, boolean hostWants, boolean guestWants);

        void onMatchEnded(String winner, String finalScore, int hostScore, int guestScore);
    }

    public void setGameListener(OnGameListener listener) {
        this.gameListener = listener;
    }

    public static class RoomInfo {
        public String id;
        public String name;
        public String host;
        public int players;
        public int maxPlayers;
    }

    public void connect(String username, OnConnectionListener listener) {
        this.username = username;
        this.connectionListener = listener;

        if (webSocket != null) {
            // Already connected check?
            // If we have clientId, we are good
            if (clientId != null) {
                Log.d(TAG, "Already connected, firing callback immediately");
                if (listener != null)
                    listener.onConnected();
                return;
            }
        }

        Request request = new Request.Builder()
                .url(SERVER_URL)
                .build();

        webSocket = client.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                Log.d(TAG, "WebSocket connected");
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                handleMessage(text);
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                Log.e(TAG, "WebSocket Connection FAILED: " + t.getMessage(), t);
                if (connectionListener != null) {
                    connectionListener.onError("Conn Fail: " + t.getMessage());
                }
            }

            @Override
            public void onClosed(WebSocket webSocket, int code, String reason) {
                Log.d(TAG, "WebSocket closed: " + reason);
                if (connectionListener != null) {
                    connectionListener.onDisconnected();
                }
            }
        });
    }

    private void handleMessage(String message) {
        try {
            JsonObject json = gson.fromJson(message, JsonObject.class);

            if (json == null || !json.has("type")) {
                Log.w(TAG,
                        "Invalid message format (no type): " + message.substring(0, Math.min(100, message.length())));
                return;
            }

            String type = json.get("type").getAsString();

            if (!type.equals("game_state_update")) {
                Log.d(TAG, "Received message: " + type);
            }

            switch (type) {
                case "connected":
                    clientId = json.get("clientId").getAsString();
                    Log.d(TAG, "Client ID: " + clientId);

                    // Send username
                    sendMessage("set_username", jsonObj -> {
                        jsonObj.addProperty("username", username);
                        return jsonObj;
                    });
                    break;

                case "username_set":
                    Log.d(TAG, "Username set confirmed by server");
                    // NOW we're truly connected and ready
                    if (connectionListener != null) {
                        Log.d(TAG, "Calling connectionListener.onConnected()");
                        connectionListener.onConnected();
                    } else {
                        Log.w(TAG, "connectionListener is NULL!");
                    }
                    break;

                case "room_list":
                    handleRoomList(json);
                    break;

                case "roomCreated":
                    handleRoomCreated(json);
                    break;

                case "player_joined":
                    handlePlayerJoined(json);
                    break;

                case "rematch_request_ack":
                    boolean hostWants = json.has("hostRequested") ? json.get("hostRequested").getAsBoolean() : false;
                    boolean guestWants = json.has("guestRequested") ? json.get("guestRequested").getAsBoolean() : false;
                    String requestingUser = json.has("requestingUser") ? json.get("requestingUser").getAsString() : "";
                    if (gameListener != null) {
                        gameListener.onRematchRequested(requestingUser, hostWants, guestWants);
                    }
                    break;

                case "game_start":
                    // Game starting or restarting (after rematch)
                    Log.d(TAG, "Game start received!");
                    if (gameListener != null) {
                        gameListener.onGameStart();
                    }
                    break;

                case "game_state":
                    // Initial game state with ball positions
                    if (gameListener != null && json.has("balls")) {
                        String hostCueBall = json.has("hostCueBall") ? json.get("hostCueBall").toString() : "null";
                        String guestCueBall = json.has("guestCueBall") ? json.get("guestCueBall").toString() : "null";
                        String balls = json.get("balls").toString();
                        gameListener.onGameStateReceived(hostCueBall, guestCueBall, balls);
                    }
                    break;

                case "game_state_update":
                    // Real-time physics update from server (60 FPS)
                    if (gameListener != null && json.has("balls")) {
                        String hostCueBall = json.has("hostCueBall") ? json.get("hostCueBall").toString() : "null";
                        String guestCueBall = json.has("guestCueBall") ? json.get("guestCueBall").toString() : "null";
                        String balls = json.get("balls").toString();

                        long timeLeft = json.has("timeLeft") ? json.get("timeLeft").getAsLong() : 0;
                        int currentSet = json.has("currentSet") ? json.get("currentSet").getAsInt() : 1;

                        gameListener.onGameStateUpdate(hostCueBall, guestCueBall, balls, timeLeft, currentSet);

                        if (json.has("hostBallsDestroyed") && json.has("guestBallsDestroyed")) {
                            int hostBalls = json.get("hostBallsDestroyed").getAsInt();
                            int guestBalls = json.get("guestBallsDestroyed").getAsInt();
                            gameListener.onBallsUpdate(hostBalls, guestBalls);
                        }
                    }
                    break;

                case "player_left":
                    Log.d(TAG, "Player left the room");
                    if (gameListener != null) {
                        gameListener.onPlayerLeft();
                    }
                    break;

                case "room_closed":
                    String reason = json.get("reason").getAsString();
                    Log.d(TAG, "Room closed: " + reason);
                    currentRoomId = null;
                    isHost = false;
                    if (gameListener != null) {
                        gameListener.onPlayerLeft(); // Treat as player left
                    }
                    break;

                case "opponent_shot":
                    if (gameListener != null && json.has("angle") && json.has("power")) {
                        float angle = json.get("angle").getAsFloat();
                        float power = json.get("power").getAsFloat();
                        gameListener.onOpponentShot(angle, power);
                    }
                    break;

                case "score_update":
                    if (gameListener != null && json.has("hostScore") && json.has("guestScore")) {
                        int hostScore = json.get("hostScore").getAsInt();
                        int guestScore = json.get("guestScore").getAsInt();

                        // Trigger explosion effect if ball was destroyed
                        if (json.has("destroyedBall")) {
                            JsonObject ball = json.getAsJsonObject("destroyedBall");
                            float x = ball.get("x").getAsFloat();
                            float y = ball.get("y").getAsFloat();
                            String colorHex = ball.get("color").getAsString();
                            gameListener.onBallDestroyed(x, y, colorHex);
                        }

                        gameListener.onBallsUpdate(hostScore, guestScore);
                    }
                    break;

                case "set_ended":
                    if (gameListener != null) {
                        String winner = json.has("winner") ? json.get("winner").getAsString() : "draw";
                        int hostScore = json.has("hostScore") ? json.get("hostScore").getAsInt() : 0;
                        int guestScore = json.has("guestScore") ? json.get("guestScore").getAsInt() : 0;
                        int currentSet = json.has("currentSet") ? json.get("currentSet").getAsInt() : 1;
                        gameListener.onSetEnded(winner, hostScore, guestScore, currentSet);
                    }
                    break;

                case "match_ended":
                    if (gameListener != null) {
                        String winner = json.get("winner").getAsString();
                        String finalScore = json.get("finalScore").getAsString();
                        int hostScore = json.get("hostScore").getAsInt();
                        int guestScore = json.get("guestScore").getAsInt();
                        gameListener.onMatchEnded(winner, finalScore, hostScore, guestScore);
                    }
                    break;

                case "error":
                    String errorMsg = json.get("message").getAsString();
                    Log.e(TAG, "Server error: " + errorMsg);
                    if (connectionListener != null) {
                        connectionListener.onError(errorMsg);
                    }
                    break;
            }
        } catch (

        Exception e) {
            Log.e(TAG, "Error parsing message: " + e.getMessage());
        }
    }

    private void handleRoomList(JsonObject json) {
        List<RoomInfo> rooms = new ArrayList<>();

        if (json.has("rooms") && json.get("rooms").isJsonArray()) {
            json.getAsJsonArray("rooms").forEach(element -> {
                JsonObject roomJson = element.getAsJsonObject();
                RoomInfo room = new RoomInfo();
                room.id = roomJson.get("id").getAsString();
                room.name = roomJson.get("name").getAsString();
                room.host = roomJson.get("host").getAsString();
                room.players = roomJson.get("players").getAsInt();
                room.maxPlayers = roomJson.get("maxPlayers").getAsInt();
                rooms.add(room);
            });
        }

        // Use callback instead of activity method
        if (roomsListener != null) {
            roomsListener.onRoomsReceived(rooms);
        }
    }

    private void handleRoomCreated(JsonObject json) {
        Log.d(TAG, "handleRoomCreated() called with JSON: " + json.toString());
        Log.d(TAG, "roomListener state: " + (roomListener != null ? "SET" : "NULL"));

        if (roomListener == null) {
            Log.w(TAG, "Room created but no listener!");
            return;
        }

        if (json.has("success") && json.get("success").getAsBoolean()) {
            String roomId = json.has("roomId") ? json.get("roomId").getAsString() : "unknown";
            Log.d(TAG, "Room created successfully: " + roomId);
            currentRoomId = roomId; // Update currentRoomId
            isHost = true; // Set isHost to true
            roomListener.onRoomCreated(roomId);
        } else {
            String error = json.has("error") ? json.get("error").getAsString() : "Unknown error";
            Log.e(TAG, "Room creation failed: " + error);
            roomListener.onError(error);
        }
    }

    private void handlePlayerJoined(JsonObject json) {
        String hostUsername = json.get("hostUsername").getAsString();
        String guestUsername = json.get("guestUsername").getAsString();
        String roomId = json.get("roomId").getAsString();
        String status = json.get("status").getAsString();

        Log.d(TAG, "Game ready! Host: " + hostUsername + ", Guest: " + guestUsername);

        // Notify game listener
        if (gameListener != null) {
            gameListener.onPlayerJoined(isHost ? guestUsername : hostUsername);
            gameListener.onGameStart();
        }

        // Notify activity to start game screen
        if (activity != null) {
            activity.startGame(roomId, hostUsername, guestUsername, isHost);
        }
    }

    public void createRoom(String roomName) {
        sendMessage("create_room", jsonObj -> {
            jsonObj.addProperty("roomName", roomName);
            return jsonObj;
        });
    }

    public void joinRoom(String roomId) {
        sendMessage("join_room", jsonObj -> {
            jsonObj.addProperty("roomId", roomId);
            return jsonObj;
        });
    }

    public void requestRoomList() {
        sendMessage("list_rooms", jsonObj -> jsonObj);
    }

    public void leaveRoom() {
        sendMessage("leave_room", jsonObj -> jsonObj);
    }

    public void createRoom(String roomName, OnRoomListener listener) {
        Log.d(TAG, "createRoom() called with roomName: " + roomName);
        Log.d(TAG, "WebSocket state: " + (webSocket != null ? "connected" : "NULL"));

        if (webSocket == null) {
            Log.e(TAG, "WebSocket is NULL! Cannot create room.");
            listener.onError("Not connected to server");
            return;
        }

        this.roomListener = listener;
        Log.d(TAG, "Room listener stored");

        try {
            JsonObject message = new JsonObject();
            message.addProperty("type", "createRoom");

            JsonObject data = new JsonObject();
            data.addProperty("roomName", roomName);
            data.addProperty("username", username);

            message.add("data", data);

            String jsonMessage = gson.toJson(message);
            Log.d(TAG, "Sending createRoom message: " + jsonMessage);
            webSocket.send(jsonMessage);
            Log.d(TAG, "Message sent successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error creating room", e);
            listener.onError(e.getMessage());
        }
    }

    public void getRooms(OnRoomsListener listener) {
        if (webSocket == null) {
            listener.onError("Not connected to server");
            return;
        }

        this.roomsListener = listener;

        JsonObject message = new JsonObject();
        message.addProperty("type", "getRooms");

        String jsonMessage = gson.toJson(message);
        Log.d(TAG, "Sending getRooms: " + jsonMessage);
        webSocket.send(jsonMessage);
    }

    public void joinRoom(String roomId, OnRoomListener listener) {
        if (webSocket == null) {
            listener.onError("Not connected to server");
            return;
        }

        try {
            JsonObject message = new JsonObject();
            message.addProperty("type", "joinRoom");

            JsonObject data = new JsonObject();
            data.addProperty("roomId", roomId);
            data.addProperty("username", username);

            message.add("data", data);

            String jsonMessage = gson.toJson(message);
            Log.d(TAG, "Sending joinRoom: " + jsonMessage);
            webSocket.send(jsonMessage);
        } catch (Exception e) {
            Log.e(TAG, "Error joining room", e);
            listener.onError(e.getMessage());
        }
    }

    public void sendShot(float angle, float power, float x, float y) {
        sendMessage("shot", jsonObj -> {
            jsonObj.addProperty("angle", angle);
            jsonObj.addProperty("power", power);
            jsonObj.addProperty("x", x);
            jsonObj.addProperty("y", y);
            return jsonObj;
        });
    }

    public void sendBallDestroyed() {
        sendMessage("ball_destroyed", jsonObj -> jsonObj);
    }

    public void sendSetEnded() {
        sendMessage("set_ended", jsonObj -> jsonObj);
    }

    public void sendReady() {
        sendMessage("ready", jsonObj -> jsonObj);
    }

    public void sendStopBall(float x, float y) {
        sendMessage("stop_ball", jsonObj -> {
            jsonObj.addProperty("x", x);
            jsonObj.addProperty("y", y);
            return jsonObj;
        });
    }

    public void sendRematchRequest() {
        sendMessage("rematch_request", jsonObj -> jsonObj);
    }

    private void sendMessage(String type, MessageBuilder builder) {
        if (webSocket == null) {
            Log.e(TAG, "WebSocket not connected");
            return;
        }

        try {
            JsonObject json = new JsonObject();
            json.addProperty("type", type);
            json = builder.build(json);

            String message = gson.toJson(json);
            webSocket.send(message);
            Log.d(TAG, "Sent message: " + type);
        } catch (Exception e) {
            Log.e(TAG, "Error sending message: " + e.getMessage());
        }
    }

    private interface MessageBuilder {
        JsonObject build(JsonObject json);
    }

    public void disconnect() {
        if (webSocket != null) {
            webSocket.close(1000, "Client closing");
            webSocket = null;
        }
    }

    public boolean isConnected() {
        return webSocket != null;
    }

    public boolean isInRoom() {
        return currentRoomId != null;
    }

    public boolean isHost() {
        return isHost;
    }

    public String getCurrentRoomId() {
        return currentRoomId;
    }
}

package com.spacebilliard.app.network;

import android.content.Context;
import android.provider.Settings;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.UUID;

public class SupabaseManager {
    private static final String TAG = "SupabaseManager";
    private static final String SUPABASE_URL = "https://hrywsorgjitwedsnlbyp.supabase.co";
    private static final String SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImhyeXdzb3Jnaml0d2Vkc25sYnlwIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjY0MDAyODMsImV4cCI6MjA4MTk3NjI4M30.PVjY7BSJz1UBm7UOvr9r9qcupshnprdZ-BL7rTkcaRc";

    private static SupabaseManager instance;
    private String deviceId;

    private SupabaseManager(Context context) {
        deviceId = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
    }

    public static synchronized SupabaseManager getInstance(Context context) {
        if (instance == null) {
            instance = new SupabaseManager(context.getApplicationContext());
        }
        return instance;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public interface ProfileCallback {
        void onSuccess(UserProfile profile);

        void onError(String error);
    }

    // Get user profile by device ID
    public void getUserProfile(ProfileCallback callback) {
        new Thread(() -> {
            try {
                URL url = new URL(SUPABASE_URL + "/rest/v1/user_profiles?device_id=eq." + deviceId + "&select=*");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("apikey", SUPABASE_ANON_KEY);
                conn.setRequestProperty("Authorization", "Bearer " + SUPABASE_ANON_KEY);

                int responseCode = conn.getResponseCode();
                if (responseCode == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();

                    JSONArray array = new JSONArray(response.toString());
                    if (array.length() > 0) {
                        JSONObject json = array.getJSONObject(0);
                        UserProfile profile = new UserProfile(
                                json.getString("user_id"),
                                json.getString("device_id"),
                                json.getString("username"),
                                json.optInt("total_wins", 0),
                                json.optInt("total_losses", 0));
                        callback.onSuccess(profile);
                    } else {
                        callback.onError("NO_PROFILE");
                    }
                } else {
                    callback.onError("HTTP_ERROR_" + responseCode);
                }
                conn.disconnect();
            } catch (Exception e) {
                Log.e(TAG, "Error getting profile", e);
                callback.onError(e.getMessage());
            }
        }).start();
    }

    // Create new user profile
    public void createProfile(String username, ProfileCallback callback) {
        new Thread(() -> {
            try {
                JSONObject data = new JSONObject();
                data.put("user_id", UUID.randomUUID().toString());
                data.put("device_id", deviceId);
                data.put("username", username);
                data.put("total_wins", 0);
                data.put("total_losses", 0);

                URL url = new URL(SUPABASE_URL + "/rest/v1/user_profiles");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("apikey", SUPABASE_ANON_KEY);
                conn.setRequestProperty("Authorization", "Bearer " + SUPABASE_ANON_KEY);
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Prefer", "return=representation");
                conn.setDoOutput(true);

                OutputStream os = conn.getOutputStream();
                os.write(data.toString().getBytes());
                os.flush();
                os.close();

                int responseCode = conn.getResponseCode();
                if (responseCode == 201) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();

                    JSONArray array = new JSONArray(response.toString());
                    JSONObject json = array.getJSONObject(0);
                    UserProfile profile = new UserProfile(
                            json.getString("user_id"),
                            json.getString("device_id"),
                            json.getString("username"),
                            0,
                            0);
                    callback.onSuccess(profile);
                } else {
                    callback.onError("HTTP_ERROR_" + responseCode);
                }
                conn.disconnect();
            } catch (Exception e) {
                Log.e(TAG, "Error creating profile", e);
                callback.onError(e.getMessage());
            }
        }).start();
    }

    // Update username
    public void updateUsername(String userId, String newUsername, ProfileCallback callback) {
        new Thread(() -> {
            try {
                JSONObject data = new JSONObject();
                data.put("username", newUsername);

                URL url = new URL(SUPABASE_URL + "/rest/v1/user_profiles?user_id=eq." + userId);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("PATCH");
                conn.setRequestProperty("apikey", SUPABASE_ANON_KEY);
                conn.setRequestProperty("Authorization", "Bearer " + SUPABASE_ANON_KEY);
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                OutputStream os = conn.getOutputStream();
                os.write(data.toString().getBytes());
                os.flush();
                os.close();

                conn.getResponseCode();
                conn.disconnect();

                getUserProfile(callback);
            } catch (Exception e) {
                Log.e(TAG, "Error updating username", e);
                callback.onError(e.getMessage());
            }
        }).start();
    }

    // Update win/loss stats
    public void updateStats(String userId, int wins, int losses, ProfileCallback callback) {
        new Thread(() -> {
            try {
                JSONObject data = new JSONObject();
                data.put("total_wins", wins);
                data.put("total_losses", losses);

                URL url = new URL(SUPABASE_URL + "/rest/v1/user_profiles?user_id=eq." + userId);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("PATCH");
                conn.setRequestProperty("apikey", SUPABASE_ANON_KEY);
                conn.setRequestProperty("Authorization", "Bearer " + SUPABASE_ANON_KEY);
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                OutputStream os = conn.getOutputStream();
                os.write(data.toString().getBytes());
                os.flush();
                os.close();

                conn.getResponseCode();
                conn.disconnect();

                getUserProfile(callback);
            } catch (Exception e) {
                Log.e(TAG, "Error updating stats", e);
                callback.onError(e.getMessage());
            }
        }).start();
    }

    public static class UserProfile {
        public String userId;
        public String deviceId;
        public String username;
        public int wins;
        public int losses;

        public UserProfile(String userId, String deviceId, String username, int wins, int losses) {
            this.userId = userId;
            this.deviceId = deviceId;
            this.username = username;
            this.wins = wins;
            this.losses = losses;
        }
    }

    public interface UsernameCheckCallback {
        void onResult(boolean exists);

        void onError(String error);
    }

    // Check if username already exists
    public void checkUsernameExists(String username, UsernameCheckCallback callback) {
        new Thread(() -> {
            try {
                // Query standard REST API for username match
                URL url = new URL(SUPABASE_URL + "/rest/v1/user_profiles?username=eq." + username + "&select=username");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("apikey", SUPABASE_ANON_KEY);
                conn.setRequestProperty("Authorization", "Bearer " + SUPABASE_ANON_KEY);

                int responseCode = conn.getResponseCode();
                if (responseCode == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();

                    JSONArray array = new JSONArray(response.toString());
                    callback.onResult(array.length() > 0);
                } else {
                    callback.onError("HTTP_ERROR_" + responseCode);
                }
                conn.disconnect();
            } catch (Exception e) {
                Log.e(TAG, "Error checking username", e);
                callback.onError(e.getMessage());
            }
        }).start();
    }
}

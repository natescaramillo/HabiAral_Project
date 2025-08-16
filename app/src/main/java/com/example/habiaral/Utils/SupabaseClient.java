package com.example.habiaral.Utils;  // 👈 make sure this matches the folder name

import okhttp3.*;

public class SupabaseClient {
    // Keep sensitive values private
    private static final String SUPABASE_URL = "https://ubxiwtxuswedwfdcqfja.supabase.co"; // replace
    private static final String SUPABASE_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InVieGl3dHh1c3dlZHdmZGNxZmphIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NTUzMjE5NTcsImV4cCI6MjA3MDg5Nzk1N30.P8zLNosS_i9LxIEJMQoJhc40Y20m7puBxK1_-gdxmmg"; // replace
    private static final OkHttpClient client = new OkHttpClient();

    // ✅ Safe getters
    public static String getSupabaseUrl() {
        return SUPABASE_URL;
    }

    public static String getSupabaseKey() {
        return SUPABASE_KEY;
    }

    // ======================
    // SELECT DATA
    // ======================
    public static void getData(String tableName, Callback callback) {
        String url = SUPABASE_URL + "/rest/v1/" + tableName + "?select=*";

        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", SUPABASE_KEY)
                .addHeader("Authorization", "Bearer " + SUPABASE_KEY)
                .build();

        client.newCall(request).enqueue(callback);
    }

    // ======================
    // INSERT DATA
    // ======================
    public static void insertData(String tableName, String jsonBody, Callback callback) {
        String url = SUPABASE_URL + "/rest/v1/" + tableName;

        RequestBody body = RequestBody.create(
                jsonBody,
                MediaType.parse("application/json")
        );

        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .addHeader("apikey", SUPABASE_KEY)
                .addHeader("Authorization", "Bearer " + SUPABASE_KEY)
                .addHeader("Content-Type", "application/json")
                .build();

        client.newCall(request).enqueue(callback);
    }


    // ======================
    // GET SIGNED URL (Storage)
    // ======================
    public static void getSignedUrl(String bucketName, String filePath, int expiresIn, Callback callback) {
        String url = SUPABASE_URL + "/storage/v1/object/sign/" + bucketName + "/" + filePath;

        RequestBody body = RequestBody.create(
                "{\"expiresIn\":" + expiresIn + "}",
                MediaType.parse("application/json")
        );

        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .addHeader("apikey", SUPABASE_KEY)
                .addHeader("Authorization", "Bearer " + SUPABASE_KEY)
                .addHeader("Content-Type", "application/json")
                .build();

        client.newCall(request).enqueue(callback);
    }
}

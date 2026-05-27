package com.lotato.pro.api;

import android.os.Handler;
import android.os.Looper;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.lotato.pro.models.Draw;
import com.lotato.pro.models.Ticket;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ApiService {
    private static final String BASE_URL = "https://lotato1.onrender.com/api";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static ApiService instance;
    private final OkHttpClient client;
    private final Gson gson;
    private final ExecutorService executor;
    private final Handler mainHandler;
    private String authToken;

    private ApiService() {
        client = new OkHttpClient.Builder()
            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .build();
        gson = new Gson();
        executor = Executors.newCachedThreadPool();
        mainHandler = new Handler(Looper.getMainLooper());
    }

    public static ApiService getInstance() {
        if (instance == null) instance = new ApiService();
        return instance;
    }

    public void setAuthToken(String token) { this.authToken = token; }

    private Request.Builder buildRequest(String path) {
        Request.Builder builder = new Request.Builder()
            .url(BASE_URL + path);
        if (authToken != null) builder.addHeader("Authorization", "Bearer " + authToken);
        return builder;
    }

    public interface Callback<T> {
        void onSuccess(T result);
        void onError(String error);
    }

    public void login(String username, String password, String role, Callback<JsonObject> cb) {
        executor.execute(() -> {
            try {
                JsonObject body = new JsonObject();
                body.addProperty("username", username);
                body.addProperty("password", password);
                body.addProperty("role", role);
                RequestBody rb = RequestBody.create(body.toString(), JSON);
                Request req = new Request.Builder().url(BASE_URL + "/auth/login").post(rb).build();
                try (Response res = client.newCall(req).execute()) {
                    String bodyStr = res.body() != null ? res.body().string() : "{}";
                    if (res.isSuccessful()) {
                        JsonObject result = gson.fromJson(bodyStr, JsonObject.class);
                        mainHandler.post(() -> cb.onSuccess(result));
                    } else {
                        JsonObject err = gson.fromJson(bodyStr, JsonObject.class);
                        String msg = err.has("error") ? err.get("error").getAsString() : "Erreur " + res.code();
                        mainHandler.post(() -> cb.onError(msg));
                    }
                }
            } catch (IOException e) {
                mainHandler.post(() -> cb.onError("Erreur réseau: " + e.getMessage()));
            }
        });
    }

    public void getDraws(Callback<List<Draw>> cb) {
        executor.execute(() -> {
            try {
                Request req = buildRequest("/draws").get().build();
                try (Response res = client.newCall(req).execute()) {
                    String bodyStr = res.body() != null ? res.body().string() : "{}";
                    if (res.isSuccessful()) {
                        JsonObject obj = gson.fromJson(bodyStr, JsonObject.class);
                        JsonArray arr = obj.has("draws") ? obj.getAsJsonArray("draws") : new JsonArray();
                        List<Draw> draws = new ArrayList<>();
                        for (int i = 0; i < arr.size(); i++) {
                            draws.add(gson.fromJson(arr.get(i), Draw.class));
                        }
                        mainHandler.post(() -> cb.onSuccess(draws));
                    } else {
                        mainHandler.post(() -> cb.onError("Erreur " + res.code()));
                    }
                }
            } catch (IOException e) {
                mainHandler.post(() -> cb.onError(e.getMessage()));
            }
        });
    }

    public void saveTicket(JsonObject ticketData, Callback<JsonObject> cb) {
        executor.execute(() -> {
            try {
                RequestBody rb = RequestBody.create(ticketData.toString(), JSON);
                Request req = buildRequest("/tickets/save").post(rb).build();
                try (Response res = client.newCall(req).execute()) {
                    String bodyStr = res.body() != null ? res.body().string() : "{}";
                    if (res.isSuccessful()) {
                        JsonObject result = gson.fromJson(bodyStr, JsonObject.class);
                        mainHandler.post(() -> cb.onSuccess(result));
                    } else {
                        JsonObject err = gson.fromJson(bodyStr, JsonObject.class);
                        String msg = err.has("error") ? err.get("error").getAsString() : "Erreur " + res.code();
                        mainHandler.post(() -> cb.onError(msg));
                    }
                }
            } catch (IOException e) {
                mainHandler.post(() -> cb.onError(e.getMessage()));
            }
        });
    }

    public void getTickets(String agentId, Callback<List<Ticket>> cb) {
        executor.execute(() -> {
            try {
                Request req = buildRequest("/tickets?agentId=" + agentId).get().build();
                try (Response res = client.newCall(req).execute()) {
                    String bodyStr = res.body() != null ? res.body().string() : "{}";
                    if (res.isSuccessful()) {
                        JsonObject obj = gson.fromJson(bodyStr, JsonObject.class);
                        JsonArray arr = obj.has("tickets") ? obj.getAsJsonArray("tickets") : new JsonArray();
                        List<Ticket> tickets = new ArrayList<>();
                        for (int i = 0; i < arr.size(); i++) {
                            tickets.add(gson.fromJson(arr.get(i), Ticket.class));
                        }
                        mainHandler.post(() -> cb.onSuccess(tickets));
                    } else {
                        mainHandler.post(() -> cb.onError("Erreur " + res.code()));
                    }
                }
            } catch (IOException e) {
                mainHandler.post(() -> cb.onError(e.getMessage()));
            }
        });
    }

    public void getReports(String agentId, Callback<JsonObject> cb) {
        executor.execute(() -> {
            try {
                Request req = buildRequest("/reports?agentId=" + agentId).get().build();
                try (Response res = client.newCall(req).execute()) {
                    String bodyStr = res.body() != null ? res.body().string() : "{}";
                    if (res.isSuccessful()) {
                        mainHandler.post(() -> cb.onSuccess(gson.fromJson(bodyStr, JsonObject.class)));
                    } else {
                        mainHandler.post(() -> cb.onError("Erreur " + res.code()));
                    }
                }
            } catch (IOException e) {
                mainHandler.post(() -> cb.onError(e.getMessage()));
            }
        });
    }

    public void getWinners(String agentId, Callback<JsonObject> cb) {
        executor.execute(() -> {
            try {
                Request req = buildRequest("/winners?agentId=" + agentId).get().build();
                try (Response res = client.newCall(req).execute()) {
                    String bodyStr = res.body() != null ? res.body().string() : "{}";
                    if (res.isSuccessful()) {
                        mainHandler.post(() -> cb.onSuccess(gson.fromJson(bodyStr, JsonObject.class)));
                    } else {
                        mainHandler.post(() -> cb.onError("Erreur " + res.code()));
                    }
                }
            } catch (IOException e) {
                mainHandler.post(() -> cb.onError(e.getMessage()));
            }
        });
    }

    public void getNumberLimits(Callback<JsonObject> cb) {
        executor.execute(() -> {
            try {
                Request req = buildRequest("/number-limits").get().build();
                try (Response res = client.newCall(req).execute()) {
                    String bodyStr = res.body() != null ? res.body().string() : "{}";
                    if (res.isSuccessful()) {
                        mainHandler.post(() -> cb.onSuccess(gson.fromJson(bodyStr, JsonObject.class)));
                    } else {
                        mainHandler.post(() -> cb.onError("Erreur " + res.code()));
                    }
                }
            } catch (IOException e) {
                mainHandler.post(() -> cb.onError(e.getMessage()));
            }
        });
    }

    public void deleteTicket(String ticketId, Callback<JsonObject> cb) {
        executor.execute(() -> {
            try {
                Request req = buildRequest("/tickets/" + ticketId).delete().build();
                try (Response res = client.newCall(req).execute()) {
                    String bodyStr = res.body() != null ? res.body().string() : "{}";
                    if (res.isSuccessful()) {
                        mainHandler.post(() -> cb.onSuccess(gson.fromJson(bodyStr, JsonObject.class)));
                    } else {
                        mainHandler.post(() -> cb.onError("Erreur " + res.code()));
                    }
                }
            } catch (IOException e) {
                mainHandler.post(() -> cb.onError(e.getMessage()));
            }
        });
    }
}


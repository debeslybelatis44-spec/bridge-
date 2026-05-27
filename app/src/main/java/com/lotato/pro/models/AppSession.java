package com.lotato.pro.models;

import android.content.Context;
import android.content.SharedPreferences;

public class AppSession {
    private static final String PREF_NAME = "lotato_session";
    private static final String KEY_TOKEN = "auth_token";
    private static final String KEY_AGENT_ID = "agent_id";
    private static final String KEY_AGENT_NAME = "agent_name";
    private static final String KEY_OWNER_ID = "owner_id";
    private static final String KEY_ROLE = "role";
    private static AppSession instance;
    private SharedPreferences prefs;

    private AppSession(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static AppSession getInstance(Context context) {
        if (instance == null) instance = new AppSession(context);
        return instance;
    }

    public void saveLogin(String token, String agentId, String agentName, String ownerId, String role) {
        prefs.edit()
            .putString(KEY_TOKEN, token)
            .putString(KEY_AGENT_ID, agentId)
            .putString(KEY_AGENT_NAME, agentName)
            .putString(KEY_OWNER_ID, ownerId)
            .putString(KEY_ROLE, role)
            .apply();
    }

    public void logout() {
        prefs.edit().clear().apply();
    }

    public String getToken() { return prefs.getString(KEY_TOKEN, null); }
    public String getAgentId() { return prefs.getString(KEY_AGENT_ID, null); }
    public String getAgentName() { return prefs.getString(KEY_AGENT_NAME, "Agent"); }
    public String getOwnerId() { return prefs.getString(KEY_OWNER_ID, null); }
    public String getRole() { return prefs.getString(KEY_ROLE, null); }
    public boolean isLoggedIn() { return getToken() != null; }
}


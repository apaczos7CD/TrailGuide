package pl.trailguide.app.auth;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import java.io.IOException;
import java.security.GeneralSecurityException;

public class TokenStorage {
    private static final String PREFS_NAME = "auth";
    private static final String TOKEN_KEY = "jwt";

    private final SharedPreferences preferences;

    public TokenStorage(Context context) throws GeneralSecurityException, IOException {
        MasterKey masterKey = new MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build();

        preferences = EncryptedSharedPreferences.create(
                context,
                PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM);
    }

    public String getToken() {
        return preferences.getString(TOKEN_KEY, null);
    }

    public void saveToken(String token) {
        preferences.edit().putString(TOKEN_KEY, token).apply();
    }

    public void clear() {
        preferences.edit().remove(TOKEN_KEY).apply();
    }
}

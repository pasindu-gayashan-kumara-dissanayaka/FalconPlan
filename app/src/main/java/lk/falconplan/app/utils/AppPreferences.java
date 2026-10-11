package lk.falconplan.app.utils;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;

public class AppPreferences {

    private static final String PREF_NAME = "falconplan_prefs";
    private static final String KEY_ONBOARDING_COMPLETED = "onboarding_completed";
    private static final String KEY_LANGUAGE_SELECTED = "language_selected";
    private static final String KEY_SELECTED_LANGUAGE = "selected_language";
    private static final String KEY_THEME_MODE = "theme_mode";

    private static AppPreferences instance;
    private final SharedPreferences preferences;

    private AppPreferences(Context context) {
        this.preferences = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static synchronized AppPreferences getInstance(Context context) {
        if (instance == null) {
            instance = new AppPreferences(context);
        }
        return instance;
    }

    public boolean isOnboardingCompleted() {
        return preferences.getBoolean(KEY_ONBOARDING_COMPLETED, false);
    }

    public void setOnboardingCompleted(boolean completed) {
        preferences.edit().putBoolean(KEY_ONBOARDING_COMPLETED, completed).apply();
    }

    public boolean isLanguageSelected() {
        return preferences.getBoolean(KEY_LANGUAGE_SELECTED, false);
    }

    public void setLanguageSelected(boolean selected) {
        preferences.edit().putBoolean(KEY_LANGUAGE_SELECTED, selected).apply();
    }

    public String getSelectedLanguage() {
        return preferences.getString(KEY_SELECTED_LANGUAGE, "en");
    }

    public void setSelectedLanguage(String langCode) {
        preferences.edit()
                .putString(KEY_SELECTED_LANGUAGE, langCode)
                .putBoolean(KEY_LANGUAGE_SELECTED, true)
                .apply();
    }

    public int getThemeMode() {
        return preferences.getInt(KEY_THEME_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
    }

    public void setThemeMode(int themeMode) {
        preferences.edit().putInt(KEY_THEME_MODE, themeMode).apply();
    }
}
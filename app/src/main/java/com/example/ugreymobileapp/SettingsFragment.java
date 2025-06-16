package com.example.ugreymobileapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import com.google.android.material.switchmaterial.SwitchMaterial;

public class SettingsFragment extends Fragment {
    private static final String PREFS_NAME = "AppSettings";
    private static final String THEME_KEY = "night_mode";
    private static final String NOTIFICATIONS_KEY = "notifications_enabled";

    private SwitchMaterial themeSwitch;
    private SwitchMaterial notificationsSwitch;
    private SharedPreferences sharedPreferences;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        sharedPreferences = requireActivity().getSharedPreferences(PREFS_NAME, 0);

        themeSwitch = view.findViewById(R.id.theme_switch);
        notificationsSwitch = view.findViewById(R.id.notifications_switch);

        loadSettings();

        setupThemeSwitch();
        setupNotificationsSwitch();

        view.findViewById(R.id.logout_button).setOnClickListener(v -> logout());

        return view;
    }

    private void loadSettings() {
        boolean isNightMode = sharedPreferences.getBoolean(THEME_KEY, false);
        themeSwitch.setChecked(isNightMode);

        boolean notificationsEnabled = sharedPreferences.getBoolean(NOTIFICATIONS_KEY, true);
        notificationsSwitch.setChecked(notificationsEnabled);
    }

    private void setupThemeSwitch() {
        themeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean(THEME_KEY, isChecked);
            editor.apply();

            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            }

            requireActivity().recreate();
        });
    }

    private void setupNotificationsSwitch() {
        notificationsSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean(NOTIFICATIONS_KEY, isChecked);
            editor.apply();
        });
    }

    private void logout() {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();

        Intent intent = new Intent(getActivity(), AuthActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        requireActivity().finish();
    }
}
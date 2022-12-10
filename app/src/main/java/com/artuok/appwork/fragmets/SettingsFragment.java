package com.artuok.appwork.fragmets;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Switch;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;

import com.artuok.appwork.R;

public class SettingsFragment extends Fragment {


    Switch darkTheme;
    SharedPreferences sharedPreferences;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.settings_fragment, container, false);

        darkTheme = root.findViewById(R.id.change_theme);
        sharedPreferences = requireActivity().getSharedPreferences("settings", Context.MODE_PRIVATE);

        LinearLayout version = root.findViewById(R.id.version);

        darkThemeSetter();

        return root;
    }

    void darkThemeSetter() {
        boolean a = sharedPreferences.getBoolean("DarkMode", false);

        darkTheme.setChecked(a);
        darkTheme.setOnCheckedChangeListener((compoundButton, b) -> {

            if (b) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            }

            SharedPreferences.Editor editor = sharedPreferences.edit();

            editor.putBoolean("DarkMode", b);
            editor.apply();
        });
    }

}
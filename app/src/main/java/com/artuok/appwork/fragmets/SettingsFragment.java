package com.artuok.appwork.fragmets;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
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
import com.artuok.appwork.db.DbChat;
import com.google.firebase.auth.FirebaseAuth;
import com.thekhaeng.pushdownanim.PushDownAnim;

public class SettingsFragment extends Fragment {


    Switch darkTheme;
    SharedPreferences sharedPreferences;
    LinearLayout session, conversation;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.settings_fragment, container, false);

        darkTheme = root.findViewById(R.id.change_theme);
        sharedPreferences = requireActivity().getSharedPreferences("settings", Context.MODE_PRIVATE);

        LinearLayout version = root.findViewById(R.id.version);
        session = root.findViewById(R.id.closephonesession);
        conversation = root.findViewById(R.id.deleteconversations);

        PushDownAnim.setPushDownAnimTo(session)
                .setDurationPush(100)
                .setScale(PushDownAnim.MODE_SCALE, 0.98f)
                .setOnClickListener(view -> {
                    SharedPreferences preferences = requireActivity().getSharedPreferences("chat", Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = preferences.edit();

                    editor.putBoolean("logged", false);

                    editor.apply();

                    FirebaseAuth auth = FirebaseAuth.getInstance();
                    auth.signOut();
                });
        PushDownAnim.setPushDownAnimTo(conversation)
                .setDurationPush(100)
                .setScale(PushDownAnim.MODE_SCALE, 0.98f)
                .setOnClickListener(view -> {
                    deleteAllConversations();
                });

        darkThemeSetter();

        return root;
    }

    void deleteAllConversations() {
        DbChat dbChat = new DbChat(requireActivity());
        SQLiteDatabase db = dbChat.getWritableDatabase();

        db.delete(DbChat.T_CHATS_MSG, "", null);
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
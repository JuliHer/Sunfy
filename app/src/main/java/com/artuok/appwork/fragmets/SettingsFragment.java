package com.artuok.appwork.fragmets;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;

import com.artuok.appwork.R;
import com.artuok.appwork.dialogs.PermissionDialog;
import com.artuok.appwork.services.AlarmWorkManager;

import java.util.Calendar;

public class SettingsFragment extends Fragment {


    Switch darkTheme, timeToDoHomework, alarmset;
    LinearLayout timer;
    TextView timeTDH;
    SharedPreferences sharedPreferences;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.settings_fragment, container, false);

        darkTheme = root.findViewById(R.id.change_theme);
        timeToDoHomework = root.findViewById(R.id.enable_timerSet);
        timer = root.findViewById(R.id.timeSet);
        timeTDH = root.findViewById(R.id.timeTDH);
        alarmset = root.findViewById(R.id.alarmset);
        sharedPreferences = requireActivity().getSharedPreferences("settings", Context.MODE_PRIVATE);

        LinearLayout version = root.findViewById(R.id.version);

        darkThemeSetter();
        TTDHSetter();
        AlarmDisplay();

        return root;
    }

    void AlarmDisplay() {
        boolean as = sharedPreferences.getBoolean("AlarmSet", false);
        alarmset.setChecked(as);
        alarmset.setOnCheckedChangeListener((compoundButton, b) -> {
            if (b) {
                setAlarmset();

            } else {
                int h = Integer.parseInt(sharedPreferences.getString("timeTDH", "11:00").split(":")[0]);
                int m = Integer.parseInt(sharedPreferences.getString("timeTDH", "11:00").split(":")[1]);
                setAlarm(h, m, false);
            }
            SharedPreferences.Editor editor = sharedPreferences.edit();

            editor.putBoolean("AlarmSet", b);
            editor.apply();
        });
    }

    void showUIEducative() {
        PermissionDialog dialog = new PermissionDialog();
        dialog.setTitleDialog(requireActivity().getString(R.string.required_permissions));
        dialog.setTextDialog(requireActivity().getString(R.string.permissions_description));
        dialog.setDrawable(R.drawable.ic_check_circle);
        dialog.setPositive((view, which) -> requestLaucher.launch(Manifest.permission.START_FOREGROUND_SERVICES_FROM_BACKGROUND));
        dialog.setNegative((view, which) -> permissionDenied((view1, which1) -> {
            view1.dismiss();
            int h = Integer.parseInt(sharedPreferences.getString("timeTDH", "11:00").split(":")[0]);
            int m = Integer.parseInt(sharedPreferences.getString("timeTDH", "11:00").split(":")[1]);
            setAlarm(h, m, false);
            alarmset.setChecked(false);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean("AlarmSet", false);
            editor.apply();
        }));
        dialog.show(requireActivity().getSupportFragmentManager(), "Permissions");
    }

    void permissionDenied(PermissionDialog.onResponseListener listener) {
        PermissionDialog dialog = new PermissionDialog();
        dialog.setTitleDialog(requireActivity().getString(R.string.permissions_denied));
        dialog.setTextDialog(requireActivity().getString(R.string.permissions_denied_description));
        dialog.setDrawable(R.drawable.ic_x_circle);
        dialog.setPositive(listener);
        dialog.show(requireActivity().getSupportFragmentManager(), "Permissions");
    }

    private final ActivityResultLauncher<String> requestLaucher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
        if (isGranted) {
            setAlarmset();
        } else {
            int h = Integer.parseInt(sharedPreferences.getString("timeTDH", "11:00").split(":")[0]);
            int m = Integer.parseInt(sharedPreferences.getString("timeTDH", "11:00").split(":")[1]);
            setAlarm(h, m, false);
            alarmset.setChecked(false);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean("AlarmSet", false);
            editor.apply();
            permissionDenied((view, which) -> {
                view.dismiss();
            });

        }
    });

    void setAlarmset() {
        cancelAlarm();
        int h = Integer.parseInt(sharedPreferences.getString("timeTDH", "11:00").split(":")[0]);
        int m = Integer.parseInt(sharedPreferences.getString("timeTDH", "11:00").split(":")[1]);
        setAlarm(h, m, true);
    }

    void TTDHSetter() {
        boolean a = sharedPreferences.getBoolean("setTimeToDoHomeWork", false);
        String tt = sharedPreferences.getString("timeTDH", "11:00");
        timeToDoHomework.setChecked(a);

        timer.setVisibility(a ? View.VISIBLE : View.GONE);
        timeTDH.setText(tt);

        timeToDoHomework.setOnCheckedChangeListener((compoundButton, b) -> {
            if (b) {
                timer.setVisibility(View.VISIBLE);
                boolean as = sharedPreferences.getBoolean("AlarmSet", false);
                int h = Integer.parseInt(sharedPreferences.getString("timeTDH", "11:00").split(":")[0]);
                int m = Integer.parseInt(sharedPreferences.getString("timeTDH", "11:00").split(":")[1]);
                setAlarm(h, m, as);
            } else {
                timer.setVisibility(View.GONE);
                cancelAlarm();
            }

            SharedPreferences.Editor editor = sharedPreferences.edit();

            editor.putBoolean("setTimeToDoHomeWork", b);
            editor.apply();
        });

        final int hh = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        final int mm = Calendar.getInstance().get(Calendar.MINUTE);


        timer.setOnClickListener((view) -> {


            TimePickerDialog dialog = new TimePickerDialog(requireActivity(), (timePicker, i, i1) -> {
                String h = i1 < 10 ? "0" + i1 : i1 + "";
                String m = i < 10 ? "0" + i : i + "";
                boolean as = sharedPreferences.getBoolean("AlarmSet", false);
                setAlarm(i, i1, as);
                String g = m + ":" + h;
                timeTDH.setText(g);

                SharedPreferences.Editor editor = sharedPreferences.edit();

                editor.putString("timeTDH", g);
                editor.apply();
            }, hh, mm + 1, false);

            dialog.show();
        });
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

    void cancelAlarm() {
        AlarmManager manager = (AlarmManager) requireActivity().getSystemService(Context.ALARM_SERVICE);
        Intent notify = new Intent(requireActivity(), AlarmWorkManager.class)
                .setAction(AlarmWorkManager.ACTION_TIME_TO_DO_HOMEWORK);
        PendingIntent pendingNotify = PendingIntent.getBroadcast(
                requireActivity(),
                0, notify,
                0);
        manager.cancel(pendingNotify);
    }

    void setAlarm(int hour, int minute, boolean alarm) {
        final Calendar c = Calendar.getInstance();
        int day = 1000 * 60 * 60 * 24;
        AlarmManager manager = (AlarmManager) requireActivity().getSystemService(Context.ALARM_SERVICE);
        c.set(Calendar.HOUR_OF_DAY, hour);
        c.set(Calendar.MINUTE, minute);
        c.set(Calendar.SECOND, 0);
        long whe = c.getTimeInMillis() <= Calendar.getInstance().getTimeInMillis() ? c.getTimeInMillis() + day : c.getTimeInMillis();
        Intent notify = new Intent(requireActivity(), AlarmWorkManager.class)
                .setAction(AlarmWorkManager.ACTION_TIME_TO_DO_HOMEWORK);
        notify.putExtra("time", whe);
        Log.d("say", "alarmset " + alarm);
        if (alarm) {
            notify.putExtra("alarm", 1);

        }
        PendingIntent pendingNotify = PendingIntent.getBroadcast(
                requireActivity(),
                0, notify,
                0);
        manager.cancel(pendingNotify);

        manager.setExact(AlarmManager.RTC_WAKEUP, whe, pendingNotify);
    }
}
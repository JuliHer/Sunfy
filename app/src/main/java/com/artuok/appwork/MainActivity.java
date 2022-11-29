package com.artuok.appwork;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.artuok.appwork.fragmets.AveragesFragment;
import com.artuok.appwork.fragmets.AwaitingFragment;
import com.artuok.appwork.fragmets.CalendarFragment;
import com.artuok.appwork.fragmets.SettingsFragment;
import com.artuok.appwork.fragmets.SubjectsFragment;
import com.artuok.appwork.fragmets.homeFragment;
import com.artuok.appwork.services.AlarmWorkManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationBarView;
import com.google.android.material.navigation.NavigationView;
import com.thekhaeng.pushdownanim.PushDownAnim;

import java.util.Calendar;
import java.util.Random;

public class MainActivity extends AppCompatActivity {


    //Navigation
    private BottomNavigationView navigation;
    private DrawerLayout drawerLayout;
    public Fragment currentFragment;
    private NavigationView navigationView;
    //fragments
    homeFragment homefragment = new homeFragment();
    AwaitingFragment awaitingFragment = new AwaitingFragment();
    CalendarFragment calendarFragment = new CalendarFragment();
    SubjectsFragment subjectsFragment = new SubjectsFragment();
    AveragesFragment averagesFragment = new AveragesFragment();


    SettingsFragment settingsFragment = new SettingsFragment();

    Fragment firstCurrentFragment = homefragment;
    Fragment secondCurrentFragment = awaitingFragment;
    Fragment thirdCurrentFragment = calendarFragment;


    //floating button
    FloatingActionButton actionButton;

    //creating Task
    String subject;

    //Subjects
    String[] subjects;

    //Toolbar
    Toolbar toolbar;

    //Dialog
    Dialog dialog;
    int dd, mm, aaaa, hh, mn;
    String datetime, dateText;
    TextView date;
    EditText title, description;
    Calendar alarmset;

    private static MainActivity instance;

    private final ActivityResultLauncher<String> requestPermissionsLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
        if (isGranted) {

        } else {

        }
    });

    int position = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        navigation = findViewById(R.id.bottom_navigation);

        drawerLayout = findViewById(R.id.drawerLayout);
        navigationView = findViewById(R.id.navigationView);

        toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("");
        setSupportActionBar(toolbar);
        startFragment(homefragment);

        instance = this;

        actionButton = findViewById(R.id.floating_button);

        alarmset = Calendar.getInstance();
        PushDownAnim.setPushDownAnimTo(actionButton)
                .setScale(PushDownAnim.MODE_SCALE, 0.98f)
                .setDurationPush(100)
                .setOnClickListener(view -> {
                    Intent i = new Intent(this, CreateAwaitingActivity.class);
                    i.getIntExtra("requestCode", 2);

                    resultLauncher.launch(i);
                });

        navigation.setOnItemSelectedListener(mOnNavigationItemSelectedListener);
        navigationView.setNavigationItemSelectedListener(mOnItemSelectedListener);

        if (getIntent().getExtras() != null)
            if (getIntent().getStringExtra("task").equals("do tasks"))
                navigation.setSelectedItemId(R.id.awaiting_fragment);
    }


    @Override
    protected void onResume() {
        navigateTo(position - 1);
        Preferences();
        super.onResume();
    }

    public void navigateTo(int n) {
        switch (n) {
            case 0:
                navigation.setSelectedItemId(R.id.homefragment);
                break;
            case 1:
                navigation.setSelectedItemId(R.id.awaiting_fragment);
                break;
            case 3:
                navigation.setSelectedItemId(R.id.calendar_fragment);
                break;
            case 2:
                navigationView.setCheckedItem(R.id.nav_subject);
                break;
        }
    }

    ActivityResultLauncher<Intent> resultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Intent data = result.getData();
                    if (data.getIntExtra("requestCode", 0) == 3) {
                        navigateTo(2);
                    } else if (data.getIntExtra("requestCode", 0) == 2) {
                        updateWidget();
                        notifyAllChanged();
                    }
                }
            }
    );

    public void updateWidget() {

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        int id = item.getItemId();

        return true;
    }

    boolean changesFromDrawer = false;

    NavigationView.OnNavigationItemSelectedListener mOnItemSelectedListener = item -> {
        drawerLayout.close();
        changesFromDrawer = true;
        navigation.setSelectedItemId(R.id.homefragment);
        switch (item.getItemId()) {
            case R.id.nav_subject:
                LoadFragment(subjectsFragment);
                return true;
            case R.id.nav_averages:
                LoadFragment(averagesFragment);
                return true;
            case R.id.nav_settings:
                LoadTextFragment(settingsFragment, MainActivity.this.getString(R.string.settings_menu));
                return true;
        }

        return false;
    };

    NavigationBarView.OnItemSelectedListener mOnNavigationItemSelectedListener = item -> {
        if (!changesFromDrawer && item.getItemId() != R.id.nav_menu) {
            navigationView.setCheckedItem(R.id.nav_subject);
        }
        changesFromDrawer = false;
        switch (item.getItemId()) {
            case R.id.homefragment:
                position = 1;
                LoadFragment(homefragment);
                return true;
            case R.id.awaiting_fragment:
                position = 2;
                LoadFragment(awaitingFragment);
                return true;
            case R.id.calendar_fragment:
                position = 3;
                LoadFragment(calendarFragment);
                return true;
            case R.id.nav_menu:
                drawerLayout.openDrawer(GravityCompat.START);
                return false;
        }

        return false;
    };

    private void startFragment(Fragment fragment) {
        FragmentTransaction transaction =
                getSupportFragmentManager()
                        .beginTransaction();
        transaction.replace(R.id.frameLayoutMain, fragment);
        if (position == 1) {
            firstCurrentFragment = fragment;
        } else if (position == 2) {
            secondCurrentFragment = fragment;
        } else if (position == 3) {
            thirdCurrentFragment = fragment;
        }
        currentFragment = fragment;
        transaction.commit();
    }

    void LoadTextFragment(Fragment fragment, String title) {
        FragmentTransaction transaction =
                getSupportFragmentManager()
                        .beginTransaction();
        if (fragment.isAdded()) {
            transaction
                    .hide(currentFragment)
                    .show(fragment);

        } else {
            transaction
                    .hide(currentFragment)
                    .add(R.id.frameLayoutMain, fragment);
        }

        currentFragment = fragment;
        transaction.commit();
        ((TextView) toolbar.findViewById(R.id.title)).setText(title);
        toolbar.setNavigationIcon(getDrawable(R.drawable.ic_arrow_left));
        toolbar.setNavigationOnClickListener(view -> {
            Fragment f = getFragmentPosition(position);
            if (f != null) {
                LoadFragment(f);
            }
        });

    }


    public void LoadFragment(Fragment fragment) {
        FragmentTransaction transaction =
                getSupportFragmentManager()
                        .beginTransaction();
        if (fragment.isAdded()) {
            transaction
                    .hide(currentFragment)
                    .show(fragment);

        } else {
            transaction
                    .hide(currentFragment)
                    .add(R.id.frameLayoutMain, fragment);
        }

        currentFragment = fragment;
        transaction.commit();

        ((TextView) toolbar.findViewById(R.id.title)).setText(MainActivity.this.getString(R.string.app_name));
        toolbar.setNavigationIcon(null);

    }

    Fragment getFragmentPosition(int pos) {

        switch (pos) {
            case 1:
                return homefragment;
            case 2:
                return awaitingFragment;
            case 3:
                return subjectsFragment;
            case 4:
                return calendarFragment;
        }

        return null;
    }

    public void notifyAllChanged() {
        if (homefragment.isAdded()) {
            homefragment.NotifyDataAdd();
        }
        if (awaitingFragment.isAdded()) {
            awaitingFragment.NotifyChanged();
        }
        if (calendarFragment.isAdded()) {
            calendarFragment.NotifyChanged();
        }
        if (averagesFragment.isAdded()) {
            averagesFragment.notifyDataChanged();
        }
    }

    public void notifyChanged(int pos) {
        if (homefragment.isAdded() && position != 1) {
            homefragment.NotifyDataChanged(pos);
        }
        if (awaitingFragment.isAdded() && position != 2) {
            awaitingFragment.NotifyChanged();
        }
        if (calendarFragment.isAdded() && position != 3) {
            calendarFragment.NotifyChanged();
        }
        if (averagesFragment.isAdded()) {
            averagesFragment.notifyDataChanged();
        }
    }

    void Preferences() {
        SharedPreferences sharedPreferences = getSharedPreferences("settings", MODE_PRIVATE);
        boolean b = sharedPreferences.getBoolean("DarkMode", false);

        if (b) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }

    void setAlarmAt(String name, String description, Calendar c, boolean Alarm) {
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);

        int AlarmType = Alarm ? AlarmManager.RTC_WAKEUP : AlarmManager.RTC;

        Intent notify = new Intent(this, AlarmWorkManager.class).setAction(AlarmWorkManager.ACTION_NOTIFY);
        notify.putExtra("title", name);
        notify.putExtra("desc", description);

        Random random = new Random();
        int r = random.nextInt();
        while (r == 0 || r == 1) {
            r = random.nextInt();
        }
        PendingIntent pendingNotify = PendingIntent.getBroadcast(this, r, notify, 0);

        alarmManager.setExact(AlarmType, c.getTimeInMillis(), pendingNotify);
    }


    public static MainActivity getInstance() {
        return instance;
    }

    public void notifyCalendar() {
        calendarFragment.NotifyChanged();
    }
}
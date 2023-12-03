package com.artuok.appwork;

import static android.appwidget.AppWidgetManager.ACTION_APPWIDGET_UPDATE;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.artuok.appwork.dialogs.AnnouncementDialog;
import com.artuok.appwork.dialogs.CreateTaskDialog;
import com.artuok.appwork.dialogs.ScheduleMakerDialog;
import com.artuok.appwork.fragmets.AlarmsFragment;
import com.artuok.appwork.fragmets.AveragesFragment;
import com.artuok.appwork.fragmets.TasksFragment;
import com.artuok.appwork.fragmets.BackupsFragment;
import com.artuok.appwork.fragmets.CalendarFragment;
import com.artuok.appwork.fragmets.ChatFragment;
import com.artuok.appwork.fragmets.SettingsFragment;
import com.artuok.appwork.fragmets.SocialFragment;
import com.artuok.appwork.fragmets.HomeFragment;
import com.artuok.appwork.library.CalendarWeekView;
import com.artuok.appwork.library.Constants;
import com.artuok.appwork.widgets.TodayTaskWidget;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationBarView;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;
import com.thekhaeng.pushdownanim.PushDownAnim;

import java.util.Calendar;
import java.util.Random;

public class MainActivity extends AppCompatActivity {
    public BottomNavigationView navigation;
    public Fragment currentFragment;
    private FloatingActionButton mainFAB;
    private FloatingActionButton subFAB1;
    private FloatingActionButton subFAB2;
    private View backgroundOverlay;
    private boolean isFABsExpanded = false;

    //fragments
    HomeFragment homefragment = new HomeFragment();
    TasksFragment tasksFragment = new TasksFragment();
    CalendarFragment calendarFragment = new CalendarFragment();


    public ChatFragment chatFragment = new ChatFragment();
    public SocialFragment socialFragment = new SocialFragment();
    public AveragesFragment averagesFragment = new AveragesFragment();
    public AlarmsFragment alarmsFragment = new AlarmsFragment();
    public SettingsFragment settingsFragment = new SettingsFragment();
    public BackupsFragment backupsFragment = new BackupsFragment();


    Fragment firstCurrentFragment = homefragment;
    Fragment secondCurrentFragment = tasksFragment;
    Fragment thirdCurrentFragment = calendarFragment;
    Fragment fourCurrentFragment = socialFragment;

    //Dialog
    Calendar alarmset;

    private static MainActivity instance;

    int position = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        navigation = findViewById(R.id.bottom_navigation);
        NavigationView navigationView = findViewById(R.id.navigationView);

        if (savedInstanceState != null)
            position = savedInstanceState.getInt("position", 1);

        if (position == 1) {
            startFragment(homefragment);
        } else if (position == 2) {
            startFragment(tasksFragment);
        } else if (position == 3) {
            startFragment(calendarFragment);
        } else if (position == 4) {
            startFragment(socialFragment);
        } else {
            startFragment(homefragment);
        }

        instance = this;

        mainFAB = findViewById(R.id.floating_button);
        subFAB1 = findViewById(R.id.sub_floating_first);
        subFAB2 = findViewById(R.id.sub_floating_second);
        backgroundOverlay = findViewById(R.id.backgroundOverlay);
        backgroundOverlay.setOnClickListener(view -> {});
        alarmset = Calendar.getInstance();

        mainFAB.setOnClickListener(view -> {
            if(calendarFragment.isAdded()){
                if(calendarFragment.isVisible()){
                    if(calendarFragment.isScheduleVisible()){
                        Calendar calendar = Calendar.getInstance();
                        int day = calendar.get(Calendar.DAY_OF_WEEK)-1;
                        long hour = 43200;
                        long duration = 3600;

                        ScheduleMakerDialog dialog = new ScheduleMakerDialog(day, hour);
                        dialog.setOnCreateScheduleListener(() -> calendarFragment.NotifyChanged());
                        dialog.show(getSupportFragmentManager(), "Schedule Maker");
                        return;
                    }
                }
            }
            CreateTaskDialog create = new CreateTaskDialog(0);
            create.setOnCheckListener ((views, id) ->{
                create.dismiss();
                notifyAllChanged();
                updateWidget();
            });
            create.show(getSupportFragmentManager(), "Create Task");
        });
        PushDownAnim.setPushDownAnimTo(subFAB1)
                .setScale(PushDownAnim.MODE_SCALE, 0.78f)
                .setDurationPush(100)
                .setOnClickListener(view -> {

                    CreateTaskDialog create = new CreateTaskDialog(0);
                    create.setOnCheckListener ((views, id) ->{
                        create.dismiss();
                        notifyAllChanged();
                        updateWidget();
                    });
                    create.show(getSupportFragmentManager(), "Create Task");
                });

        if (navigation != null)
            navigation.setOnItemSelectedListener(mOnNavigationItemSelectedListener);
        if (navigation == null)
            navigationView.setNavigationItemSelectedListener(mOnItemSelectedListener);

        if (getIntent().getExtras() != null) {
            Bundle extras = getIntent().getExtras();
            if (extras.getString("task", "").equals("new task")) {
                CreateTaskDialog create = new CreateTaskDialog(0);
                create.setOnCheckListener ((views, id) ->{
                    create.dismiss();
                    notifyAllChanged();
                    updateWidget();
                });
                create.show(getSupportFragmentManager(), "Create Task");
            }
        }

        if (checkVersion()) {
            showAnnouncement();
            setVersion(Constants.VERSION);
        }

        if (!SettingsFragment.isLogged(this)) {
            showAnnouncementChat();
        }

        OnBackPressedCallback onBackPressedCallback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (position != 0) {
                    navigateTo(0);
                }
            }
        };

        getOnBackPressedDispatcher().addCallback(this, onBackPressedCallback);
    }

    @SuppressLint("RestrictedApi")
    public void showSnackbar(String text) {
        CoordinatorLayout layout = (CoordinatorLayout) findViewById(R.id.coordinadorers);
        Snackbar snackbar = Snackbar.make(layout, "", BaseTransientBottomBar.LENGTH_LONG);
        @SuppressLint("InflateParams") View customSnack = getLayoutInflater().inflate(R.layout.snack_notification_layout, null);
        snackbar.getView().setBackgroundColor(Color.TRANSPARENT);

        Snackbar.SnackbarLayout snackbarLayout = (Snackbar.SnackbarLayout) snackbar.getView();

        snackbarLayout.setPadding(0, 0, 0, 0);
        CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) snackbarLayout.getLayoutParams();
        params.setMargins(0, 0, 0, 185);
        snackbarLayout.setLayoutParams(params);
        TextView dialog = customSnack.findViewById(R.id.textdialog);
        dialog.setText(text);
        snackbarLayout.addView(customSnack, 0);

        snackbar.show();
    }

    public void setVersion(int version){
        SharedPreferences s = getSharedPreferences("settings", Context.MODE_PRIVATE);
        SharedPreferences.Editor se = s.edit();
        se.putInt("version", version);
        se.apply();
    }

    public boolean checkVersion() {
        SharedPreferences s = getSharedPreferences("settings", Context.MODE_PRIVATE);
        return s.getInt("version", 0) != Constants.VERSION;
    }

    public int getVersion() {
        SharedPreferences s = getSharedPreferences("settings", Context.MODE_PRIVATE);
        return s.getInt("version", 0);
    }

    public ActivityResultLauncher<Intent> resultLaunchers = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            it -> {
                if (it.getResultCode() == RESULT_OK) {
                    String path = getTempImg();
                    Intent i = new Intent(this, PhotoSelectActivity.class);
                    i.putExtra("path", path);
                    i.putExtra("icon", true);
                    i.putExtra("from", "camera");
                    startActivity(i);
                }
            });

    private String getTempImg() {
        SharedPreferences sr = getSharedPreferences("images", Context.MODE_PRIVATE);
        return sr.getString("TempImg", "");
    }

    @Override
    protected void onResume() {
        navigateTo(position - 1);
        Preferences();
        super.onResume();
    }

    public void navigateTo(int n) {
        if (navigation != null)
            switch (n) {
                case 0:
                    navigation.setSelectedItemId(R.id.homefragment);
                    break;
                case 1:
                    navigation.setSelectedItemId(R.id.awaiting_fragment);
                    break;
                case 2:
                    navigation.setSelectedItemId(R.id.calendar_fragment);
                    break;
                case 3:
                    navigation.setSelectedItemId(R.id.nav_subject);
                    break;
                case 4:
                    navigation.setSelectedItemId(R.id.homefragment);
                    position = 5;
                    LoadTextFragment(socialFragment, getString(R.string.chat));
                    break;
                case 5:
                    navigation.setSelectedItemId(R.id.homefragment);
                    position = 6;
                    LoadFragment(averagesFragment);
                    break;
                case 6:
                    navigation.setSelectedItemId(R.id.homefragment);
                    position = 7;
                    LoadFragment(alarmsFragment);
                    break;
                case 7:
                    navigation.setSelectedItemId(R.id.homefragment);
                    position = 8;
                    LoadTextFragment(settingsFragment, getString(R.string.settings_menu));
                    break;
                case 8:
                    navigation.setSelectedItemId(R.id.homefragment);
                    position = 9;
                    LoadTextFragment(backupsFragment, getString(R.string.backup));
                    break;
            }
        else
            switch (n) {
                case 0:
                    position = 1;
                    LoadFragment(homefragment);
                    break;
                case 1:
                    position = 2;
                    LoadFragment(tasksFragment);
                    break;
                case 2:
                    position = 3;
                    LoadFragment(calendarFragment);
                    break;
                case 3:
                    position = 4;
                    LoadTextFragment(socialFragment, getString(R.string.search));
                    break;
                case 4:
                    position = 5;
                    LoadTextFragment(socialFragment, getString(R.string.chat));
                    break;
                case 5:
                    position = 6;
                    LoadFragment(averagesFragment);
                    break;
                case 6:
                    position = 7;
                    LoadFragment(alarmsFragment);
                    break;
                case 7:
                    position = 8;
                    LoadTextFragment(settingsFragment, getString(R.string.settings_menu));
                    break;
                case 8:
                    position = 9;
                    LoadTextFragment(backupsFragment, getString(R.string.backup));
                    break;
            }
    }



    public void updateWidget() {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
        int[] appWidgetsIds = appWidgetManager.getAppWidgetIds(new ComponentName(this, TodayTaskWidget.class));
        Intent uw = new Intent(ACTION_APPWIDGET_UPDATE)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetsIds);
        sendBroadcast(uw);
    }

    public void showAnnouncementChat() {
        AnnouncementDialog dialog = new AnnouncementDialog();
        dialog.setTitle("Sunfy chat");
        dialog.setText("Únete a la comunidad de Sunfy y chatea con tu equipo y amigos.");

        TypedArray a = obtainStyledAttributes(R.styleable.AppCustomAttrs);
        int color = a.getColor(R.styleable.AppCustomAttrs_backgroundDialog, Color.WHITE);
        int textColor = a.getColor(R.styleable.AppCustomAttrs_backgroundBorder, Color.WHITE);

        a.recycle();

        dialog.setBackgroundCOlor(color);
        dialog.setTextColor(textColor);

        dialog.setImage(R.mipmap.ad_chat);

        dialog.setAgree(true);

        dialog.setOnPositiveClickListener(getString(R.string.Accept_M), view -> {
            Intent i = new Intent(MainActivity.this, LoginActivity.class);

            startActivity(i);
        });

        dialog.setOnNegativeClickListener(getString(R.string.dismiss), view -> {
            dialog.dismiss();
        });

        dialog.show(getSupportFragmentManager(), "Chat Announcement");
    }

    public void showAnnouncement() {
        AnnouncementDialog dialog = new AnnouncementDialog();
        dialog.setTitle(getString(R.string.version) + " " + Constants.VERSION_CODE);
        dialog.setText(getString(R.string.version_changes));

        TypedArray a = obtainStyledAttributes(R.styleable.AppCustomAttrs);
        int color = a.getColor(R.styleable.AppCustomAttrs_backgroundDialog, Color.WHITE);
        int textColor = a.getColor(R.styleable.AppCustomAttrs_backgroundBorder, Color.WHITE);

        a.recycle();

        dialog.setBackgroundCOlor(color);
        dialog.setTextColor(textColor);


        int[] banners = new int[9];
        banners[0] = R.mipmap.banner_10;
        banners[1] = R.mipmap.banner_2;
        banners[2] = R.mipmap.banner_3;
        banners[3] = R.mipmap.banner_4;
        banners[4] = R.mipmap.banner_5;
        banners[5] = R.mipmap.banner_6;
        banners[6] = R.mipmap.banner_7;
        banners[7] = R.mipmap.banner_8;
        banners[8] = R.mipmap.banner_9;

        Random r = new Random();
        int set = Math.abs(r.nextInt()) % 9;

        int banner = banners[set];

        dialog.setImage(banner);

        dialog.setAgree(true);

        dialog.setOnPositiveClickListener(getString(R.string.Accept_M), view -> {
            dialog.dismiss();
        });

        dialog.setOnNegativeClickListener(getString(R.string.dismiss), view -> {
            dialog.dismiss();
        });

        dialog.show(getSupportFragmentManager(), "Error Announcement");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        return true;
    }

    boolean changesFromDrawer = false;


    @SuppressLint("NonConstantResourceId")
    NavigationView.OnNavigationItemSelectedListener mOnItemSelectedListener = item -> {
        switch (item.getItemId()) {
            case R.id.homefragment:
                position = 1;
                LoadFragment(homefragment);
                return true;
            case R.id.awaiting_fragment:
                position = 2;
                LoadFragment(tasksFragment);
                return true;
            case R.id.calendar_fragment:
                position = 3;
                LoadFragment(calendarFragment);
                return true;
            case R.id.nav_subject:
                position = 4;
                LoadFragment(socialFragment);
                return true;
        }

        return false;
    };

    @SuppressLint("NonConstantResourceId")
    NavigationBarView.OnItemSelectedListener mOnNavigationItemSelectedListener = item -> {
        if (!changesFromDrawer) {
            switch (item.getItemId()) {
                case R.id.homefragment:
                    position = 1;
                    LoadFragment(homefragment);
                    return true;
                case R.id.awaiting_fragment:
                    position = 2;
                    LoadFragment(tasksFragment);
                    return true;
                case R.id.calendar_fragment:
                    position = 3;
                    LoadFragment(calendarFragment);
                    return true;
                case R.id.nav_subject:
                    position = 4;
                    LoadTextFragment(socialFragment, getString(R.string.search));
                    return true;
            }
        } else {
            changesFromDrawer = false;
            position = 1;
            return true;
        }

        return false;
    };

    private void startFragment(Fragment fragment) {
        FragmentTransaction transaction =
                getSupportFragmentManager()
                        .beginTransaction();
        transaction.replace(R.id.frameLayoutMain, fragment);

        switch (position){
            case 2: secondCurrentFragment = fragment;
                break;
            case 3: thirdCurrentFragment = fragment;
                break;
            case 4: fourCurrentFragment = fragment;
                break;
            default: firstCurrentFragment = fragment;
                break;
        }

        currentFragment = fragment;
        transaction.commit();
    }

    void LoadTextFragment(Fragment fragment, String title) {
        FragmentTransaction transaction =
                getSupportFragmentManager()
                        .beginTransaction();


        if (fragment.isAdded()) {
            return;
        } else {
            transaction.remove(currentFragment)
                    .add(R.id.frameLayoutMain, fragment);
        }




        currentFragment = fragment;
        transaction.commit();

    }

    public void loadExternalFragment(Fragment fragment, String name) {
        FragmentTransaction transaction =
                getSupportFragmentManager()
                        .beginTransaction();

        if (fragment.isAdded()) {
            return;
        } else {
            transaction.remove(currentFragment)
                    .add(R.id.frameLayoutMain, fragment);
        }

        if (navigation != null) {
            changesFromDrawer = true;
            navigation.setSelectedItemId(R.id.homefragment);
        }
        position = 1;

        currentFragment = fragment;
        transaction.commit();
    }

    public void LoadFragment(Fragment fragment) {
        FragmentTransaction transaction =
                getSupportFragmentManager()
                        .beginTransaction();

        if (fragment.isAdded()) {
            return;
        } else {
            transaction.remove(currentFragment)
                    .add(R.id.frameLayoutMain, fragment, "tag");
        }

        currentFragment = fragment;
        transaction.commit();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putInt("position", position);
        super.onSaveInstanceState(outState);
    }

    public void notifyAllChanged() {
        if (homefragment.isAdded()) {
            homefragment.NotifyDataAdd();
        }
        if (tasksFragment.isAdded()) {
            tasksFragment.NotifyChanged();
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
        if (tasksFragment.isAdded() && position != 2) {
            tasksFragment.NotifyChanged();
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

    public static MainActivity getInstance() {
        return instance;
    }

    private void toggleFABs() {
        if (isFABsExpanded) {
            hideFABs();
        } else {
            showFABs();
        }
    }

    private void showFABs() {
        isFABsExpanded = true;
        backgroundOverlay.setVisibility(View.VISIBLE);
        backgroundOverlay.setAlpha(0f);

        int translate = mainFAB.getHeight();
        float margin = translate*0.1f;
        mainFAB.animate().rotation(45f);
        subFAB1.animate().translationY((-translate)-margin);
        //subFAB2.animate().translationY((-translate*2)-margin);

        backgroundOverlay.animate().alpha(1f);
    }

    private void hideFABs() {
        isFABsExpanded = false;
        mainFAB.animate().rotation(0f);
        subFAB1.animate().translationY(0f);
        subFAB2.animate().translationY(0f);

        backgroundOverlay.animate().alpha(0f).withEndAction(() -> backgroundOverlay.setVisibility(View.GONE));
    }

}
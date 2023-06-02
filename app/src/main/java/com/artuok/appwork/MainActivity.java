package com.artuok.appwork;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.os.Bundle;
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
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.artuok.appwork.dialogs.AnnouncementDialog;
import com.artuok.appwork.dialogs.PermissionDialog;
import com.artuok.appwork.fragmets.AlarmsFragment;
import com.artuok.appwork.fragmets.AveragesFragment;
import com.artuok.appwork.fragmets.AwaitingFragment;
import com.artuok.appwork.fragmets.BackupsFragment;
import com.artuok.appwork.fragmets.CalendarFragment;
import com.artuok.appwork.fragmets.ChatFragment;
import com.artuok.appwork.fragmets.SettingsFragment;
import com.artuok.appwork.fragmets.SubjectsFragment;
import com.artuok.appwork.fragmets.homeFragment;
import com.artuok.appwork.library.CalendarWeekView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.RequestConfiguration;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationBarView;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;
import com.thekhaeng.pushdownanim.PushDownAnim;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Random;

public class MainActivity extends AppCompatActivity {


    //Navigation
    public BottomNavigationView navigation;
    public Fragment currentFragment;
    private NavigationView navigationView;
    public static int CURRENT_VERSION = 21;


    //fragments
    homeFragment homefragment = new homeFragment();
    AwaitingFragment awaitingFragment = new AwaitingFragment();
    CalendarFragment calendarFragment = new CalendarFragment();
    SubjectsFragment subjectsFragment = new SubjectsFragment();

    public ChatFragment chatFragment = new ChatFragment();
    public AveragesFragment averagesFragment = new AveragesFragment();
    public AlarmsFragment alarmsFragment = new AlarmsFragment();
    public SettingsFragment settingsFragment = new SettingsFragment();
    public BackupsFragment backupsFragment = new BackupsFragment();


    Fragment firstCurrentFragment = homefragment;
    Fragment secondCurrentFragment = awaitingFragment;
    Fragment thirdCurrentFragment = calendarFragment;
    Fragment fourCurrentFragment = subjectsFragment;

    //floating button
    FloatingActionButton actionButton;

    CollapsingToolbarLayout collapsingToolbar;
    //Toolbar
    Toolbar toolbar;

    //Dialog
    Calendar alarmset;

    private OnBackPressedCallback onBackPressedCallback;

    private static MainActivity instance;

    int position = 1;

    ActivityResultLauncher<Intent> resultLauncherSelect = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Intent data = result.getData();
                    if (data.getIntExtra("requestCode", 0) == 3) {
                    } else if (data.getIntExtra("requestCode", 0) == 2) {
                        loadChatFragment();
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        navigation = findViewById(R.id.bottom_navigation);

        navigationView = findViewById(R.id.navigationView);

        collapsingToolbar = findViewById(R.id.collapsingToolbar);
        toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("");

        setSupportActionBar(toolbar);

        MobileAds.initialize(this);
        RequestConfiguration configuration = new RequestConfiguration.Builder().setTestDeviceIds(Arrays.asList("1C6196DE1539B306778414AEE133E09B")).build();
        MobileAds.setRequestConfiguration(configuration);
        if (savedInstanceState != null)
            position = savedInstanceState.getInt("position", 1);

        if (position == 1) {
            startFragment(homefragment);
        } else if (position == 2) {
            startFragment(awaitingFragment);
        } else if (position == 3) {
            startFragment(calendarFragment);
        } else if (position == 4) {
            startFragment(subjectsFragment);
        } else {
            startFragment(homefragment);
        }

        instance = this;

        actionButton = findViewById(R.id.floating_button);

        alarmset = Calendar.getInstance();
        PushDownAnim.setPushDownAnimTo(actionButton)
                .setScale(PushDownAnim.MODE_SCALE, 0.98f)
                .setDurationPush(100)
                .setOnClickListener(view -> {
                    if(calendarFragment.isAdded()){
                        if(calendarFragment.isVisible()){
                            if(calendarFragment.isScheduleVisible()){
                                Calendar calendar = Calendar.getInstance();
                                int day = calendar.get(Calendar.DAY_OF_WEEK)-1;
                                long hour = 43200;
                                long duration = 3600;
                                calendarFragment.startCreateActivity(new CalendarWeekView.EventsTask(day, hour, duration, 0, ""));
                                return;
                            }
                        }
                    }
                    Intent i = new Intent(this, CreateAwaitingActivity.class);
                    i.getIntExtra("requestCode", 2);

                    resultLauncher.launch(i);
                });

        if (navigation != null)
            navigation.setOnItemSelectedListener(mOnNavigationItemSelectedListener);
        if (navigation == null)
            navigationView.setNavigationItemSelectedListener(mOnItemSelectedListener);

        if (getIntent().getExtras() != null) {
            Bundle extras = getIntent().getExtras();
            if (extras.getString("task", "").equals("new task")) {
                Intent i = new Intent(this, CreateAwaitingActivity.class);
                i.getIntExtra("requestCode", 2);
                startActivity(i);
            }
        }


        if (isWarning() || !isActualVersion()) {
            //showErrorAnnouncement();
        }


        if (!isActualVersion()) {
            showAnnouncement();
            //setWarning(false);
            setVersion(CURRENT_VERSION);
        }

        if (!SettingsFragment.isLogged(this)) {
            showAnnouncementChat();
        }

        onBackPressedCallback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (position != 0) {
                    navigateTo(0);
                }
            }
        };

        // Registra el callback
        getOnBackPressedDispatcher().addCallback(this, onBackPressedCallback);
    }


    @SuppressLint("RestrictedApi")
    public void showSnackbar(String text) {
        CoordinatorLayout layout = (CoordinatorLayout) findViewById(R.id.coordinadorers);
        Snackbar snackbar = Snackbar.make(layout, "", BaseTransientBottomBar.LENGTH_LONG);

        View customSnack = getLayoutInflater().inflate(R.layout.snack_notification_layout, null);

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

    public void setWarning(boolean warning){
        SharedPreferences s = getSharedPreferences("settings", Context.MODE_PRIVATE);
        SharedPreferences.Editor se = s.edit();
        se.putBoolean("acceptWarning", warning);
        se.apply();
    }

    public boolean isWarning() {
        SharedPreferences s = getSharedPreferences("settings", Context.MODE_PRIVATE);
        return !s.getBoolean("acceptWarning", false);
    }

    public boolean isActualVersion() {
        SharedPreferences s = getSharedPreferences("settings", Context.MODE_PRIVATE);
        return s.getInt("version", 0) == CURRENT_VERSION;
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
                    LoadTextFragment(chatFragment, getString(R.string.chat));
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
                    LoadFragment(awaitingFragment);
                    break;
                case 2:
                    position = 3;
                    LoadFragment(calendarFragment);
                    break;
                case 3:
                    position = 4;
                    LoadFragment(subjectsFragment);
                    break;
                case 4:
                    position = 5;
                    LoadTextFragment(chatFragment, getString(R.string.chat));
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
                    navigation.setSelectedItemId(R.id.homefragment);
                    position = 9;
                    LoadTextFragment(backupsFragment, getString(R.string.backup));
                    break;
            }
    }

    ActivityResultLauncher<Intent> resultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Intent data = result.getData();
                    if (data.getIntExtra("requestCode", 0) == 3) {
                    } else if (data.getIntExtra("requestCode", 0) == 2) {
                        updateWidget();
                        notifyAllChanged();
                    }

                    if(data.getIntExtra("requestCode2", 0) == 8){

                    }
                }
            }
    );


    public void loadChatFragment() {
        if (chatFragment.isAdded()) {
            chatFragment.loadChatsMessage();
        }
    }

    public void updateWidget() {

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
        dialog.setTitle(getString(R.string.version) + " " + getString(R.string.version_code));
        dialog.setText(getString(R.string.version_changes));

        TypedArray a = obtainStyledAttributes(R.styleable.AppCustomAttrs);
        int color = a.getColor(R.styleable.AppCustomAttrs_backgroundDialog, Color.WHITE);
        int textColor = a.getColor(R.styleable.AppCustomAttrs_backgroundBorder, Color.WHITE);

        a.recycle();

        dialog.setBackgroundCOlor(color);
        dialog.setTextColor(textColor);


        int[] banners = new int[9];
        banners[0] = R.mipmap.banner;
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

    public void showErrorAnnouncement() {
        AnnouncementDialog dialog = new AnnouncementDialog();
        dialog.setTitle(getString(R.string.v_unstable));
        dialog.setText(getString(R.string.contact_with_progr_unstable_v));
        dialog.setBackgroundCOlor(getColor(R.color.yellow_700));
        dialog.setDrawable(R.drawable.alert_octagon);
        dialog.setAgree(true);
        dialog.setOnNegativeClickListener(getString(R.string.dismiss), view -> {
            setWarning(false);
            finish();
        });
        dialog.setOnPositiveClickListener(getString(R.string.Accept_M), view -> {
            setWarning(true);
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

        int id = item.getItemId();

        return true;
    }

    boolean changesFromDrawer = false;


    NavigationView.OnNavigationItemSelectedListener mOnItemSelectedListener = item -> {


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
            case R.id.nav_subject:
                position = 4;
                LoadFragment(subjectsFragment);
                return true;
        }

        return false;
    };

    NavigationBarView.OnItemSelectedListener mOnNavigationItemSelectedListener = item -> {


        if (!changesFromDrawer) {
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
                case R.id.nav_subject:
                    position = 4;
                    LoadFragment(subjectsFragment);
                    return true;
            }
        } else {
            changesFromDrawer = false;
            position = 1;
            return true;
        }

        return false;
    };

    public void addMessage(){
        if(chatFragment.isAdded()){
            chatFragment.loadChatsMessage();
        }
    }

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
        } else if (position == 4) {
            fourCurrentFragment = fragment;
        } else {
            firstCurrentFragment = fragment;
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

        if (title == getString(R.string.chat)) {
            actionButton.setImageResource(R.drawable.message_circle);
            PushDownAnim.setPushDownAnimTo(actionButton)
                    .setScale(PushDownAnim.MODE_SCALE, 0.98f)
                    .setDurationPush(100)
                    .setOnClickListener(view -> {
                        SharedPreferences sharedPreferences = getSharedPreferences("chat", MODE_PRIVATE);
                        boolean b = sharedPreferences.getBoolean("logged", false);
                        if (b) {
                            Intent i = new Intent(this, SelectActivity.class);
                            if (ContextCompat.checkSelfPermission(this,
                                    Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
                                resultLauncherSelect.launch(i);
                            } else if (shouldShowRequestPermissionRationale(Manifest.permission.READ_CONTACTS)) {
                                showOnContextUI();
                            } else {
                                requestPermissionLauncher.launch(Manifest.permission.READ_CONTACTS);
                            }
                        } else {
                            showSnackbar( getString(R.string.login_able_chat));
                        }
                    });
        } else {
            actionButton.setImageResource(R.drawable.ic_baseline_add_24);
            PushDownAnim.setPushDownAnimTo(actionButton)
                    .setScale(PushDownAnim.MODE_SCALE, 0.98f)
                    .setDurationPush(100)
                    .setOnClickListener(view -> {

                        if(calendarFragment.isAdded()){
                            if(calendarFragment.isVisible()){
                                if(calendarFragment.isScheduleVisible()){
                                    Calendar calendar = Calendar.getInstance();
                                    int day = calendar.get(Calendar.DAY_OF_WEEK)-1;
                                    long hour = 43200;
                                    long duration = 3600;
                                    calendarFragment.startCreateActivity(new CalendarWeekView.EventsTask(day, hour, duration, 0, ""));
                                    return;
                                }
                            }
                        }

                        Intent i = new Intent(this, CreateAwaitingActivity.class);
                        i.getIntExtra("requestCode", 2);
                        resultLauncher.launch(i);
                    });
        }


        currentFragment = fragment;
        transaction.commit();

        ((TextView) toolbar.findViewById(R.id.title)).setText(title);
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

        if (name == getString(R.string.chat)) {
            actionButton.setImageResource(R.drawable.message_circle);
            PushDownAnim.setPushDownAnimTo(actionButton)
                    .setScale(PushDownAnim.MODE_SCALE, 0.98f)
                    .setDurationPush(100)
                    .setOnClickListener(view -> {
                        SharedPreferences sharedPreferences = getSharedPreferences("chat", MODE_PRIVATE);
                        boolean b = sharedPreferences.getBoolean("logged", false);
                        if (b) {
                            Intent i = new Intent(this, SelectActivity.class);
                            if (ContextCompat.checkSelfPermission(this,
                                    Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
                                resultLauncherSelect.launch(i);
                            } else if (shouldShowRequestPermissionRationale(Manifest.permission.READ_CONTACTS)) {
                                showOnContextUI();
                            } else {
                                requestPermissionLauncher.launch(Manifest.permission.READ_CONTACTS);
                            }
                        } else {
                            showSnackbar(getString(R.string.login_able_chat));
                        }
                    });
        } else {
            actionButton.setImageResource(R.drawable.ic_baseline_add_24);
            PushDownAnim.setPushDownAnimTo(actionButton)
                    .setScale(PushDownAnim.MODE_SCALE, 0.98f)
                    .setDurationPush(100)
                    .setOnClickListener(view -> {

                        if (calendarFragment.isAdded()) {
                            if (calendarFragment.isVisible()) {
                                if (calendarFragment.isScheduleVisible()) {
                                    Calendar calendar = Calendar.getInstance();
                                    int day = calendar.get(Calendar.DAY_OF_WEEK) - 1;
                                    long hour = 43200;
                                    long duration = 3600;
                                    calendarFragment.startCreateActivity(new CalendarWeekView.EventsTask(day, hour, duration, 0, ""));
                                    return;
                                }
                            }
                        }

                        Intent i = new Intent(this, CreateAwaitingActivity.class);
                        i.getIntExtra("requestCode", 2);
                        resultLauncher.launch(i);
                    });
        }

        currentFragment = fragment;
        transaction.commit();


        ((TextView) toolbar.findViewById(R.id.title)).setText(name);
    }

    public void LoadFragment(Fragment fragment) {
        FragmentTransaction transaction =
                getSupportFragmentManager()
                        .beginTransaction();

//        if (fragment.isAdded()) {
//            transaction
//                    .hide(currentFragment)
//                    .show(fragment);
//        } else {
//            transaction
//                    .hide(currentFragment)
//                    .add(R.id.frameLayoutMain, fragment);
//        }


        if (fragment.isAdded()) {
            return;
        } else {
            transaction.remove(currentFragment)
                    .add(R.id.frameLayoutMain, fragment);
        }


        actionButton.setImageResource(R.drawable.ic_baseline_add_24);
        PushDownAnim.setPushDownAnimTo(actionButton)
                .setScale(PushDownAnim.MODE_SCALE, 0.98f)
                .setDurationPush(100)
                .setOnClickListener(view -> {
                    if (calendarFragment.isAdded()) {
                        if (calendarFragment.isVisible()) {
                            if(calendarFragment.isScheduleVisible()){
                                Calendar calendar = Calendar.getInstance();
                                int day = calendar.get(Calendar.DAY_OF_WEEK)-1;
                                long hour = 43200;
                                long duration = 3600;
                                calendarFragment.startCreateActivity(new CalendarWeekView.EventsTask(day, hour, duration, 0, ""));
                                return;
                            }
                        }
                    }
                    Intent i = new Intent(this, CreateAwaitingActivity.class);
                    i.getIntExtra("requestCode", 2);

                    resultLauncher.launch(i);
                });

        currentFragment = fragment;
        transaction.commit();


        ((TextView) toolbar.findViewById(R.id.title)).setText(MainActivity.this.getString(R.string.app_name));
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
        if (awaitingFragment.isAdded()) {
            awaitingFragment.NotifyChanged();
        }
        if (calendarFragment.isAdded()) {
            calendarFragment.NotifyChanged();
        }
        if (averagesFragment.isAdded()) {
            averagesFragment.notifyDataChanged();
        }

        if(subjectsFragment.isAdded()){
            subjectsFragment.onNotifyDataChanged();
        }
    }

    public void notifyToChatChanged(){
        if(chatFragment.isAdded()){
            chatFragment.loadChatsMessage();
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

        if(subjectsFragment.isAdded() && position != 4){
            subjectsFragment.onNotifyDataChanged();
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

    private void showOnContextUI() {
        PermissionDialog dialog = new PermissionDialog();
        dialog.setTitleDialog(getString(R.string.required_permissions));
        dialog.setTextDialog(getString(R.string.permissions_read_contacts_description));
        dialog.setPositive((view, which) -> requestPermissionLauncher.launch(Manifest.permission.READ_CONTACTS));

        dialog.setNegative((view, which) -> {
            dialog.dismiss();
        });

        dialog.setDrawable(R.drawable.ic_users);
        dialog.show(getSupportFragmentManager(), "permissions");
    }




    private ActivityResultLauncher<String> requestPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                if (isGranted) {
                    Intent i = new Intent(this, SelectActivity.class);
                    resultLauncherSelect.launch(i);
                }
            });

    public static MainActivity getInstance() {
        return instance;
    }

    public void notifyCalendar() {
        calendarFragment.NotifyChanged();
    }


}
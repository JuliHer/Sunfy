package com.artuok.appwork;

import android.app.AlarmManager;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RemoteViews;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.artuok.appwork.adapters.SubjectAdapter;
import com.artuok.appwork.db.DbHelper;
import com.artuok.appwork.fragmets.AwaitingFragment;
import com.artuok.appwork.fragmets.CalendarFragment;
import com.artuok.appwork.fragmets.SettingsFragment;
import com.artuok.appwork.fragmets.SubjectsFragment;
import com.artuok.appwork.fragmets.homeFragment;
import com.artuok.appwork.objects.ItemSubjectElement;
import com.artuok.appwork.objects.SubjectElement;
import com.artuok.appwork.services.AlarmWorkManager;
import com.artuok.appwork.services.RemotesService;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationBarView;
import com.thekhaeng.pushdownanim.PushDownAnim;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Random;

public class MainActivity extends AppCompatActivity {


    //Navigation
    private BottomNavigationView navigation;
    public Fragment currentFragment;
    //fragments
    homeFragment homefragment = new homeFragment();
    AwaitingFragment awaitingFragment = new AwaitingFragment();
    CalendarFragment calendarFragment = new CalendarFragment();
    SubjectsFragment subjectsFragment = new SubjectsFragment();


    SettingsFragment settingsFragment = new SettingsFragment();

    Fragment firstCurrentFragment = homefragment;
    Fragment secondCurrentFragment = awaitingFragment;
    Fragment thirdCurrentFragment = calendarFragment;
    Fragment fourthCurrentFragment = subjectsFragment;


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

        toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("");
        setSupportActionBar(toolbar);
        startFragment(homefragment);

        setDialog();
        actionButton = findViewById(R.id.floating_button);

        alarmset = Calendar.getInstance();
        PushDownAnim.setPushDownAnimTo(actionButton)
                .setScale(PushDownAnim.MODE_SCALE, 0.98f)
                .setDurationPush(100)
                .setOnClickListener(view -> showAwaitingCreator());

        navigation.setOnItemSelectedListener(mOnNavigationItemSelectedListener);

        if (getIntent().getExtras() != null)
            if (getIntent().getStringExtra("task").equals("do tasks"))
                navigation.setSelectedItemId(R.id.awaiting_fragment);
    }


    @Override
    protected void onResume() {
        super.onResume();
        LoadFragment(currentFragment);
        Preferences();
    }

    public void navigateTo(int n) {
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
                navigation.setSelectedItemId(R.id.subjects_fragment);
                break;
        }
    }

    private void setDialog() {
        dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.bottom_task_layout);
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.getWindow().getAttributes().windowAnimations = R.style.DialogAnimation;
        dialog.getWindow().setGravity(Gravity.BOTTOM);
    }

    private void setSelectSubject(TextView a) {
        Dialog subjectDialog = new Dialog(this);
        subjectDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        subjectDialog.setContentView(R.layout.bottom_sheet_layout);
        subjectDialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        subjectDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        subjectDialog.getWindow().getAttributes().windowAnimations = R.style.DialogAnimation;
        subjectDialog.getWindow().setGravity(Gravity.BOTTOM);

        RecyclerView recyclerView = subjectDialog.findViewById(R.id.subjects_recycler);
        LinearLayoutManager manager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        final List<ItemSubjectElement> elements = getSubjects();
        SubjectAdapter adapter = new SubjectAdapter(this, elements, (view, position) -> {
            subject = ((SubjectElement) elements.get(position).getObject()).getName();
            subjectDialog.dismiss();
            a.setText(subject);
        });

        LinearLayout add = subjectDialog.findViewById(R.id.add_subject);
        add.setOnClickListener(view -> {
            subjectDialog.dismiss();
            dialog.dismiss();
            navigateTo(3);
        });

        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(manager);
        recyclerView.setAdapter(adapter);
        subjectDialog.show();
    }

    private List<ItemSubjectElement> getSubjects() {
        DbHelper dbHelper = new DbHelper(this);
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        List<ItemSubjectElement> elements = new ArrayList<>();

        Cursor cursor = db.rawQuery("SELECT * FROM " + DbHelper.t_subjects + " ORDER BY name DESC", null);
        if (cursor.moveToFirst()) {
            do {
                elements.add(new ItemSubjectElement(new SubjectElement(cursor.getString(1)), 2));
            } while (cursor.moveToNext());
        }

        db.close();

        return elements;
    }

    private void showAwaitingCreator() {
        String[] items = subjects();


        TextView chooseSubject = dialog.findViewById(R.id.choose_subject);
        chooseSubject.setText(getString(R.string.select_subject));

        chooseSubject.setOnClickListener(view -> {
            setSelectSubject(chooseSubject);
        });

        Button cancel, accept;

        cancel = dialog.findViewById(R.id.cancel_awaiting);
        accept = dialog.findViewById(R.id.accept_awaiting);
        title = dialog.findViewById(R.id.title_task);
        title.setText("");
        description = dialog.findViewById(R.id.description_task);
        description.setText("");
        date = dialog.findViewById(R.id.datepicker);
        date.setText(R.string.Date_string);


        date.setOnClickListener(view -> {
            final Calendar calendar = Calendar.getInstance();
            dd = calendar.get(Calendar.DAY_OF_MONTH);
            mm = calendar.get(Calendar.MONTH);
            aaaa = calendar.get(Calendar.YEAR);

            final TimePickerDialog timePicker = new TimePickerDialog(MainActivity.this, (timePicker1, i, i1) -> {


                String a = i1 < 10 ? "0" + i1 : i1 + "";
                String e = i < 10 ? "0" + i : i + "";
                datetime += " " + e + ":" + a + ":00";


                if (i > 12) {
                    int b = i - 12;
                    dateText += " " + b + ":" + a + " PM";
                } else {
                    dateText += " " + i + ":" + a + " AM";
                }

                alarmset.set(Calendar.HOUR_OF_DAY, i);
                alarmset.set(Calendar.MINUTE, i1);
                alarmset.set(Calendar.SECOND, 0);
                date.setText(dateText);
            }, hh, mn, false);

            DatePickerDialog datePicker = new DatePickerDialog(MainActivity.this, (datePicker1, i, i1, i2) -> {
                int m = i1 + 1;
                String e = m < 10 ? "0" + m : m + "";
                String a = i2 < 10 ? "0" + i2 : i2 + "";
                datetime = i + "-" + e + "-" + a;

                Calendar c = Calendar.getInstance();
                c.set(i, i1, i2, 12, 0, 0);
                int day = c.get(Calendar.DAY_OF_WEEK);
                dateText = homeFragment.getDayOfWeek(this, day) + " " + i2 + ", " + homeFragment.getMonthMinor(this, i1) + " " + i;

                alarmset.set(i, i1, i2);
                timePicker.show();
            }, dd, mm, aaaa);

            Calendar c = Calendar.getInstance();
            c.set(Calendar.YEAR, aaaa);
            c.set(Calendar.MONTH, mm);
            c.set(Calendar.DAY_OF_MONTH, dd);

            datePicker.getDatePicker().setMinDate(c.getTimeInMillis());
            datePicker.show();
        });

        PushDownAnim.setPushDownAnimTo(cancel)
                .setScale(PushDownAnim.MODE_SCALE, 0.98f)
                .setDurationPush(100)
                .setOnClickListener(view -> dialog.dismiss());
        PushDownAnim.setPushDownAnimTo(accept)
                .setScale(PushDownAnim.MODE_SCALE, 0.98f)
                .setDurationPush(100)
                .setOnClickListener(view -> {
                    if (!title.getText().toString().isEmpty()) {
                        if (!datetime.isEmpty()) {
                            if (!subject.isEmpty()) {
                                dialog.dismiss();
                                insertAwaiting(title.getText().toString(), datetime, subject, description.getText().toString());

                                setAlarmAt(title.getText().toString(), description.getText().toString(), alarmset, false);
                            } else {
                                Toast.makeText(MainActivity.this, "Elige una asignatura", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                });
        dialog.show();
    }

    private String[] subjects() {
        DbHelper dbHelper = new DbHelper(MainActivity.this);
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + DbHelper.t_subjects + " ORDER BY name DESC", null);

        if (subjects == null || subjects.length != cursor.getCount() || subjects.length == 0) {

            List<String> a = new ArrayList<>();
            if (cursor.moveToFirst()) {
                do {
                    a.add(cursor.getString(1));
                } while (cursor.moveToNext());
            }

            subjects = new String[a.size()];

            for (int c = 0; c < a.size(); c++) {
                subjects[c] = a.get(c);
            }
        }

        cursor.close();
        return subjects;
    }

    private void insertAwaiting(String name, String date, String subject, String description) {
        DbHelper dbHelper = new DbHelper(MainActivity.this);
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        Calendar c = Calendar.getInstance();

        String now = c.get(Calendar.YEAR) + "-" + c.get(Calendar.MONTH) + "-" + c.get(Calendar.DAY_OF_MONTH) + " " + c.get(Calendar.HOUR_OF_DAY) + ":" + c.get(Calendar.MINUTE) + ":" + c.get(Calendar.SECOND);
        values.put("date", now);
        values.put("title", name);
        values.put("end_date", date);
        values.put("subject", subject);
        values.put("description", description);
        values.put("status", false);

        db.insert(DbHelper.t_task, null, values);
        updateWidget(this);
        notifyAllChanged();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        int id = item.getItemId();

        if (id == R.id.settings) {
            LoadTextFragment(settingsFragment, MainActivity.this.getString(R.string.settings_menu));
        }

        return true;
    }

    NavigationBarView.OnItemSelectedListener mOnNavigationItemSelectedListener = item -> {
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
            case R.id.subjects_fragment:
                position = 4;
                LoadFragment(subjectsFragment);
                return true;
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
        } else if (position == 4) {
            fourthCurrentFragment = fragment;
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
        homefragment.NotifyDataAdd();
        awaitingFragment.NotifyChanged();
        calendarFragment.NotifyChanged();
    }

    public void notifyChanged() {
        if (position != 1) {
            homefragment.NotifyDataChanged();
        }
        if (position != 2) {
            awaitingFragment.NotifyChanged();
        }
        if (position != 3) {
            calendarFragment.NotifyChanged();
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

    public static void updateWidget(Context contex) {
        Context context = contex;
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.awaiting_widget);
        ComponentName thisWidget = new ComponentName(context, AwaitingWidget.class);
        remoteViews.setRemoteAdapter(R.id.list_widget, new Intent(context, RemotesService.class));
        appWidgetManager.updateAppWidget(thisWidget, remoteViews);
    }

    public void notifyChangedInHome(String d) {
        //homefragment.notifyInData(d);
    }
}
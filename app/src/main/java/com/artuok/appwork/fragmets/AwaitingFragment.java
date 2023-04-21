package com.artuok.appwork.fragmets;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Canvas;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.artuok.appwork.MainActivity;
import com.artuok.appwork.R;
import com.artuok.appwork.ViewActivity;
import com.artuok.appwork.adapters.AwaitingAdapter;
import com.artuok.appwork.db.DbHelper;
import com.artuok.appwork.dialogs.AnnouncementDialog;
import com.artuok.appwork.dialogs.PermissionDialog;
import com.artuok.appwork.objects.AnnouncesElement;
import com.artuok.appwork.objects.AwaitElement;
import com.artuok.appwork.objects.Item;
import com.artuok.appwork.objects.TextElement;
import com.artuok.appwork.widgets.TodayTaskWidget;
import com.faltenreich.skeletonlayout.Skeleton;
import com.faltenreich.skeletonlayout.SkeletonLayoutUtils;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.nativead.NativeAd;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Random;

import it.xabaras.android.recyclerview.swipedecorator.RecyclerViewSwipeDecorator;

public class AwaitingFragment extends Fragment {

    //recyclerView
    RecyclerView recyclerView;
    List<Item> elements;
    AwaitingAdapter adapter;
    LinearLayoutManager manager;
    TextView done, onHold, lose;
    AwaitingAdapter.OnClickListener listener;
    LinearLayout empty;

    int advirments = 0;

    Skeleton skeleton;

    ItemTouchHelper.SimpleCallback touchHelper = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
        @Override
        public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
            return false;
        }

        @Override
        public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {

            int position = viewHolder.getLayoutPosition();
            if(elements.get(position).getType() == 0){
                switch (direction) {
                    case ItemTouchHelper.LEFT:
                        removeTask(position);
                        break;
                    case ItemTouchHelper.RIGHT:

                        checkTask(position);
                        break;
                }

                ((MainActivity) requireActivity()).updateWidget();
                statistics();
            }

        }

        @Override
        public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
            int position = viewHolder.getLayoutPosition();
            if (elements.get(position).getType() == 0 || elements.get(position).getType() == 3) {
                if(!((AwaitElement)elements.get(position).getObject()).isDone()){
                    new RecyclerViewSwipeDecorator.Builder(requireActivity(), c, recyclerView, viewHolder, dX, dY, actionState,
                            isCurrentlyActive)
                            .addSwipeLeftBackgroundColor(requireActivity().getColor(R.color.red_500))
                            .addSwipeLeftLabel(requireActivity().getString(R.string.delete))
                            .addSwipeLeftActionIcon(R.drawable.ic_trash)
                            .setSwipeLeftActionIconTint(requireActivity().getColor(R.color.white))
                            .setSwipeLeftLabelColor(requireActivity().getColor(R.color.white))
                            .addSwipeRightBackgroundColor(requireActivity().getColor(R.color.blue_400))
                            .addSwipeRightLabel(requireActivity().getString(R.string.check))
                            .addSwipeRightActionIcon(R.drawable.ic_check_circle)
                            .setSwipeRightActionIconTint(requireActivity().getColor(R.color.white))
                            .setSwipeRightLabelColor(requireActivity().getColor(R.color.white))
                            .create()
                            .decorate();
                }else{
                    new RecyclerViewSwipeDecorator.Builder(requireActivity(), c, recyclerView, viewHolder, dX, dY, actionState,
                            isCurrentlyActive)
                            .addSwipeLeftBackgroundColor(requireActivity().getColor(R.color.red_500))
                            .addSwipeLeftLabel(requireActivity().getString(R.string.delete))
                            .addSwipeLeftActionIcon(R.drawable.ic_trash)
                            .setSwipeLeftActionIconTint(requireActivity().getColor(R.color.white))
                            .setSwipeLeftLabelColor(requireActivity().getColor(R.color.white))

                            .addSwipeRightBackgroundColor(requireActivity().getColor(R.color.blue_400))
                            .addSwipeRightLabel(requireActivity().getString(R.string.uncheck))
                            .addSwipeRightActionIcon(R.drawable.ic_x_circle)
                            .setSwipeRightActionIconTint(requireActivity().getColor(R.color.white))
                            .setSwipeRightLabelColor(requireActivity().getColor(R.color.white))
                            .create()
                            .decorate();
                }

                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
            }
        }
    };

    ActivityResultLauncher<Intent> resultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Intent data = result.getData();
                    if (data.getIntExtra("requestCode", 0) == 3) {
                    } else if (data.getIntExtra("requestCode", 0) == 2) {
                        NotifyChanged();
                        int i = data.getIntExtra("taskModify", 0);
                        notifyGlobalChanged(i);
                        DbHelper dbHelper = new DbHelper(requireActivity());
                        SQLiteDatabase db = dbHelper.getReadableDatabase();
                        Cursor pendingTasks = db.rawQuery("SELECT * FROM " + DbHelper.T_TASK + " WHERE status = '0'", null);
                        if (pendingTasks.getCount() < 1) {
                            showCongratulations();
                        }
                    }

                    if(data.getIntExtra("shareCode", 0) == 2){
                        ((MainActivity)requireActivity()).notifyToChatChanged();
                    }
                }
            }
    );

    private void setOnResultListener(){
        resultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Intent data = result.getData();
                        if (data.getIntExtra("requestCode", 0) == 3) {
                        } else if (data.getIntExtra("requestCode", 0) == 2) {
                            NotifyChanged();
                            int i = data.getIntExtra("taskModify", 0);
                            notifyGlobalChanged(i);
                            DbHelper dbHelper = new DbHelper(requireActivity());
                            SQLiteDatabase db = dbHelper.getReadableDatabase();
                            Cursor pendingTasks = db.rawQuery("SELECT * FROM " + DbHelper.T_TASK + " WHERE status = '0'", null);
                            if (pendingTasks.getCount() < 1) {
                                showCongratulations();
                            }
                        }

                        if(data.getIntExtra("shareCode", 0) == 2){
                            ((MainActivity)requireActivity()).notifyToChatChanged();
                        }
                    }
                }
        );
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.awaiting_fragment, container, false);

        setOnResultListener();
        setListener();
        elements = new ArrayList<>();
        adapter = new AwaitingAdapter(requireActivity(), elements);
        adapter.setOnClickListener(listener);
        manager = new LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false);
        recyclerView = root.findViewById(R.id.awaiting_recycler);
        done = root.findViewById(R.id.done_txt);
        onHold = root.findViewById(R.id.onHold_txt);
        lose = root.findViewById(R.id.losed_txt);
        empty = root.findViewById(R.id.empty_tasks);

        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(manager);
        recyclerView.setAdapter(adapter);

        skeleton = SkeletonLayoutUtils.applySkeleton(recyclerView, R.layout.skeleton_awaiting_layout, 12);
        TypedArray a = requireActivity().obtainStyledAttributes(R.styleable.AppCustomAttrs);

        int shimmerColor = a.getColor(R.styleable.AppCustomAttrs_shimmerSkeleton, Color.GRAY);
        int maskColor = a.getColor(R.styleable.AppCustomAttrs_maskSkeleton, Color.LTGRAY);

        skeleton.setMaskColor(maskColor);
        skeleton.setShimmerColor(shimmerColor);
        a.recycle();

        skeleton.showSkeleton();

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(touchHelper);
        itemTouchHelper.attachToRecyclerView(recyclerView);

        statistics();

        new AverageAsync(new AverageAsync.ListenerOnEvent() {
            @Override
            public void onPreExecute() {

            }

            @Override
            public void onExecute(boolean b) {
                loadAwaitings();

            }

            @Override
            public void onPostExecute(boolean b) {
                adapter.notifyDataSetChanged();
                skeleton.showOriginal();
                if (elements.size() != 0) {
                    empty.setVisibility(View.GONE);
                } else {
                    empty.setVisibility(View.VISIBLE);
                }
            }
        }).exec(true);


        SharedPreferences s = requireActivity().getSharedPreferences("settings", Context.MODE_PRIVATE);
        boolean firstTime = s.getBoolean("firstAwaitingOpens", true);
        if(elements.size() > 0 && firstTime) {
            ((MainActivity)requireActivity()).showSnackbar("Swipe left to check");
            SharedPreferences.Editor se = s.edit();
            se.putBoolean("firstAwaitingOpens", false);
            se.apply();
        }

        return root;
    }

    void localUpdate(){
        statistics();
    }

    void setListener() {
        listener = (view, p) -> {
            if (elements.get(p).getType() == 0) {
                int id = ((AwaitElement)elements.get(p).getObject()).getId();
                Intent i = new Intent(requireActivity(), ViewActivity.class);

                i.getIntExtra("requestCode", 2);
                i.putExtra("id", id);
                resultLauncher.launch(i);
            }
        };
    }

    void statistics() {
        DbHelper dbHelper = new DbHelper(requireActivity().getApplicationContext());
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + DbHelper.T_TASK + " WHERE status = '1'", null);

        Calendar c = Calendar.getInstance();

        long dat = c.getTimeInMillis();

        Cursor hold = db.rawQuery("SELECT * FROM " + DbHelper.T_TASK + " WHERE status = '0' AND end_date >= '" + dat + "'", null);
        Cursor lose = db.rawQuery("SELECT * FROM " + DbHelper.T_TASK + " WHERE status = '0' AND end_date < '" + dat + "'", null);


        String d = cursor.getCount() + "";
        String l = lose.getCount() + "";
        String h = requireActivity().getString(R.string.on_hold_string) + ": " + hold.getCount() + "";


        done.setText(d);
        onHold.setText(h);
        this.lose.setText(l);

        cursor.close();
        hold.close();
        lose.close();
    }

    public void showCongratulations() {
        MediaPlayer mp = MediaPlayer.create(requireActivity(), R.raw.completed);
        mp.start();

        AnnouncementDialog dialog = new AnnouncementDialog();
        dialog.setTitle(getString(R.string.completed_tasks));
        dialog.setText(getString(R.string.congratulations_1));
        dialog.setDrawable(R.drawable.ic_check_circle);
        dialog.setBackgroundCOlor(requireActivity().getColor(R.color.blue_400));
        dialog.setOnPositiveClickListener(requireActivity().getString(R.string.Accept_M), view -> {
            dialog.dismiss();
        });

        dialog.setOnNegativeClickListener(requireActivity().getString(R.string.dismiss), view -> {
            dialog.dismiss();
        });

        dialog.show(requireActivity().getSupportFragmentManager(), "Congratulations to user");
    }

    public void NotifyChanged() {
        if (elements != null) {
            elements.clear();
            statistics();
            loadAwaitings();
        }
    }

    public void notifyGlobalChanged(int id){
        int i = 0;
        try {
            i = getPositionOfId(requireActivity(), id);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        if (i >= 0) {
            ((MainActivity) requireActivity()).notifyChanged(i);
        } else {
            ((MainActivity) requireActivity()).notifyAllChanged();
        }
    }

    void loadAwaitings() {
        if(!isAdded())
            return;

        DbHelper dbHelper = new DbHelper(requireActivity().getApplicationContext());
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Calendar ti = Calendar.getInstance();
        long min = ti.getTimeInMillis();

        Cursor cursor = db.rawQuery("SELECT * FROM " + DbHelper.T_TASK + " WHERE status = '0' AND end_date > '" + min + "' ORDER BY end_date ASC", null);

        if (cursor.moveToFirst()) {
            TextElement el = new TextElement(requireActivity().getString(R.string.pending_activities));
            elements.add(new Item(el, 2));

            do {
                Calendar c = Calendar.getInstance();
                boolean e = true;
                long date = cursor.getLong(2);
                c.setTimeInMillis(date);
                int day = c.get(Calendar.DAY_OF_MONTH);
                int month = c.get(Calendar.MONTH);
                int year = c.get(Calendar.YEAR);
                boolean hourFormat = DateFormat.is24HourFormat(requireActivity());
                int hour = c.get(Calendar.HOUR_OF_DAY);
                if(!hourFormat)
                    hour = c.get(Calendar.HOUR) == 0 ? 12 : c.get(Calendar.HOUR);

                int minute = c.get(Calendar.MINUTE);

                int inApp = new Random().nextInt() % 5;
                if(inApp == 8){
                    setAnnounce(elements.size());
                }
                String dd = day < 10 ? "0" + day : "" + day;
                if(!isAdded())
                    return;
                String dates = dd + " " + homeFragment.getMonthMinor(requireActivity(), (month)) + " " + year + " ";
                String mn = minute < 10 ? "0" + minute : "" + minute;
                String times = hour +":"+mn;

                if(!hourFormat){
                    times += c.get(Calendar.AM_PM) == Calendar.AM ?  " a. m." : " p. m.";
                }

                boolean done = cursor.getInt(5) == 1;
                boolean liked = cursor.getInt(7) == 1;
                int subjectName = cursor.getInt(3);

                Cursor s = db.rawQuery("SELECT * FROM " + DbHelper.t_subjects + " WHERE id = " + subjectName, null);
                int colors = 0;
                String subject = "";
                if (s.moveToFirst()) {
                    colors = s.getInt(2);
                    subject = s.getString(1);
                }

                s.close();

                int id = cursor.getInt(0);
                String title = cursor.getString(4);
                String status = daysLeft(true, date);
                int statusColor = statusColor(false, date);

                AwaitElement eb = new AwaitElement(id, title, status, dates, times, colors, statusColor);
                eb.setDone(done);
                eb.setSubject(subject);
                eb.setLiked(liked);
                elements.add(new Item(eb, 0));
            } while (cursor.moveToNext());
        }
        if(!isAdded())
            return;
        loadLate();
        if(!isAdded())
            return;
        loadDone();
        cursor.close();

        updateWidget();
    }

    private int statusColor(boolean isClosed, long time){
        TypedArray ta = requireActivity().obtainStyledAttributes(R.styleable.AppCustomAttrs);
        int colorB = ta.getColor(R.styleable.AppCustomAttrs_subTextColor, requireActivity().getColor(R.color.yellow_700));
        ta.recycle();
        Calendar today = Calendar.getInstance();
        long tod = today.getTimeInMillis();

        long rest = (time - tod) / 86400000;
        if (isClosed) {
            return colorB;
        } else {
            if (rest > 2) {
                return requireActivity().getColor(R.color.green_500);
            } else {
                return requireActivity().getColor(R.color.yellow_700);
            }
        }
    }

    private String daysLeft(boolean isOpen, long time){
        String d = "";
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(time);

        Calendar today = Calendar.getInstance();

        long tod = today.getTimeInMillis();

        if(isOpen) {
            long rest = (time - tod) / 86400000;

            if (tod < time) {
                int toin = today.get(Calendar.DAY_OF_YEAR);
                int awin = c.get(Calendar.DAY_OF_YEAR);
                if (awin < toin) {
                    rest = awin + 364 - toin;
                } else {
                    rest = awin - toin;
                }
            }

            int dow = c.get(Calendar.DAY_OF_WEEK);

            if (rest == 1) {
                d = requireActivity().getString(R.string.tomorrow);
            } else if (rest == 0) {
                d = requireActivity().getString(R.string.today);
            } else if (rest < 7) {
                if (dow == 1) {
                    d = requireActivity().getString(R.string.sunday);
                } else if (dow == 2) {
                    d = requireActivity().getString(R.string.monday);
                } else if (dow == 3) {
                    d = requireActivity().getString(R.string.tuesday);
                } else if (dow == 4) {
                    d = requireActivity().getString(R.string.wednesday);
                } else if (dow == 5) {
                    d = requireActivity().getString(R.string.thursday);
                } else if (dow == 6) {
                    d = requireActivity().getString(R.string.friday);
                } else if (dow == 7) {
                    d = requireActivity().getString(R.string.saturday);
                }
            } else {
                d = rest + " " + requireActivity().getString(R.string.day_left);
            }
        }
        return d;
    }

    private void setAnnounce(int pos){
        int finalPos = pos + advirments;
        advirments++;
        AdLoader adLoader = new AdLoader.Builder(requireActivity(), "ca-app-pub-3940256099942544/2247696110")
                .forNativeAd(nativeAd -> {
                    String title = nativeAd.getHeadline();
                    String body = nativeAd.getBody();
                    String advertiser = nativeAd.getAdvertiser();
                    String price = nativeAd.getPrice();
                    List<NativeAd.Image> images = nativeAd.getImages();
                    NativeAd.Image icon = nativeAd.getIcon();

                    advirments--;
                    AnnouncesElement element = new AnnouncesElement(title, body, advertiser, images, icon);
                    element.setAction(nativeAd.getCallToAction());
                    element.setPrice(price);
                    elements.add(finalPos, new Item(element, 12));
                    adapter.notifyItemInserted(finalPos);
                    Log.d("CattoAwaiting", "AD loaded");
                }).withAdListener(new AdListener() {
                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        super.onAdFailedToLoad(loadAdError);
                    }

                    @Override
                    public void onAdLoaded() {
                        super.onAdLoaded();
                    }
                }).build();
        adLoader.loadAd(new AdRequest.Builder().build());
    }

    private void updateWidget(){
        if(!isAdded())
            return;
        Intent i = new Intent(requireActivity(), TodayTaskWidget.class);
        i.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);

        int[] ids = AppWidgetManager.getInstance(requireActivity()).getAppWidgetIds(new ComponentName(requireActivity(), TodayTaskWidget.class));
        i.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
        requireActivity().sendBroadcast(i);
    }

    void loadLate() {
        DbHelper dbHelper = new DbHelper(requireActivity().getApplicationContext());
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Calendar ti = Calendar.getInstance();
        long min = ti.getTimeInMillis();
        Cursor cursor = db.rawQuery("SELECT * FROM " + DbHelper.T_TASK + " WHERE status = '0' AND end_date <= '" + min + "' ORDER BY end_date ASC", null);

        if (cursor.moveToFirst()) {
            TextElement el = new TextElement(requireActivity().getString(R.string.overdue_tasks));
            elements.add(new Item(el, 2));
            do {
                Calendar c = Calendar.getInstance();
                boolean e = true;
                long date = cursor.getLong(2);
                c.setTimeInMillis(date);
                int day = c.get(Calendar.DAY_OF_MONTH);
                int month = c.get(Calendar.MONTH);
                int year = c.get(Calendar.YEAR);
                int inApp = new Random().nextInt() % 10;
                if(inApp == 8){
                    setAnnounce(elements.size());
                }
                boolean hourFormat = DateFormat.is24HourFormat(requireActivity());
                int hour = c.get(Calendar.HOUR_OF_DAY);
                if(!hourFormat)
                    hour = c.get(Calendar.HOUR) == 0 ? 12 : c.get(Calendar.HOUR);

                int minute = c.get(Calendar.MINUTE);

                String dd = day < 10 ? "0" + day : "" + day;
                String dates = dd + " " + homeFragment.getMonthMinor(requireActivity(), (month)) + " " + year + " ";
                String mn = minute < 10 ? "0" + minute : "" + minute;
                String times = hour +":"+mn;

                if(!hourFormat)
                    times += c.get(Calendar.AM_PM) == Calendar.AM ?  " a. m." : " p. m.";

                boolean done = cursor.getInt(5) == 1;
                boolean liked = cursor.getInt(7) == 1;
                int subjectName = cursor.getInt(3);

                Cursor s = db.rawQuery("SELECT * FROM " + DbHelper.t_subjects + " WHERE id = " + subjectName, null);
                int colors = 0;
                String subject = "";
                if (s.moveToFirst()) {
                    colors = s.getInt(2);
                    subject = s.getString(1);
                }

                s.close();
                int id = cursor.getInt(0);
                String title = cursor.getString(4);
                String status = requireActivity().getString(R.string.overdue);
                int statusColor = statusColor(true, date);

                AwaitElement eb = new AwaitElement(id, title, status, dates, times, colors, statusColor);
                eb.setDone(done);
                eb.setLiked(liked);
                eb.setSubject(subject);
                elements.add(new Item(eb, 0));
            } while (cursor.moveToNext());
        }
        cursor.close();
    }

    void loadDone() {
        DbHelper dbHelper = new DbHelper(requireActivity().getApplicationContext());
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + DbHelper.T_TASK + " WHERE status = '1' ORDER BY end_date DESC", null);
        if (cursor.moveToFirst()) {

            TextElement el = new TextElement(requireActivity().getString(R.string.completed_tasks));
            elements.add(new Item(el, 2));
            do {
                Calendar c = Calendar.getInstance();
                boolean e = true;
                long date = cursor.getLong(2);
                c.setTimeInMillis(date);
                int day = c.get(Calendar.DAY_OF_MONTH);
                int month = c.get(Calendar.MONTH);
                int year = c.get(Calendar.YEAR);
                boolean hourFormat = DateFormat.is24HourFormat(requireActivity());
                int hour = c.get(Calendar.HOUR_OF_DAY);
                if(!hourFormat)
                    hour = c.get(Calendar.HOUR) == 0 ? 12 : c.get(Calendar.HOUR);
                int minute = c.get(Calendar.MINUTE);
                int inApp = new Random().nextInt() % 5;
                if(inApp == 8){
                    setAnnounce(elements.size());
                }
                String dd = day < 10 ? "0" + day : "" + day;
                if(!isAdded())
                    return;
                String dates = dd + " " + homeFragment.getMonthMinor(requireActivity(), (month)) + " " + year + " ";
                String mn = minute < 10 ? "0" + minute : "" + minute;
                String times = hour +":"+mn;

                if(!hourFormat)
                    times += c.get(Calendar.AM_PM) == Calendar.AM ?  " a. m." : " p. m.";

                boolean done = cursor.getInt(5) == 1;
                boolean liked = cursor.getInt(7) == 1;
                int subjectName = cursor.getInt(3);

                Cursor s = db.rawQuery("SELECT * FROM " + DbHelper.t_subjects + " WHERE id = " + subjectName, null);
                int colors = 0;
                String subject = "";
                if (s.moveToFirst()) {
                    colors = s.getInt(2);
                    subject = s.getString(1);
                }

                s.close();
                int id = cursor.getInt(0);
                String title = cursor.getString(4);
                String status = requireActivity().getString(R.string.done_string);
                int statusColor = statusColor(true, date);

                AwaitElement eb = new AwaitElement(id, title, status, dates, times, colors, statusColor);
                eb.setDone(done);
                eb.setLiked(liked);
                eb.setSubject(subject);
                elements.add(new Item(eb, 0));
            } while (cursor.moveToNext());
        }
        cursor.close();
    }

    void removeTask(int position) {
        if(elements.get(position).getType() == 0) {
            int id = ((AwaitElement) elements.get(position).getObject()).getId();
            PermissionDialog dialog = new PermissionDialog();
            dialog.setTitleDialog(requireActivity().getString(R.string.remove));
            dialog.setTextDialog(requireActivity().getString(R.string.remove_task));
            dialog.setDrawable(R.drawable.ic_trash);
            adapter.notifyItemChanged(position);
            dialog.setNegative((view, which) -> {
                adapter.notifyItemChanged(position);
                dialog.dismiss();
            });


            dialog.setPositive((view, which) -> {
                DbHelper dbHelper = new DbHelper(requireActivity());
                SQLiteDatabase db = dbHelper.getReadableDatabase();
                int i = 0;
                try {
                    i = getPositionOfId(requireActivity(), id);
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                Cursor cursor = db.rawQuery("SELECT * FROM " + DbHelper.T_TASK + " WHERE id = '" + id + "'", null);
                if (cursor.moveToFirst()) {
                    SQLiteDatabase db2 = dbHelper.getWritableDatabase();
                    db2.delete(DbHelper.T_TASK, " id = '" + id + "'", null);
                    db2.delete(DbHelper.T_PHOTOS, " awaiting = '" + id + "'", null);
                }

                elements.remove(position);
                adapter.notifyItemRemoved(position);
                cursor.close();


                if (i >= 0) {
                    ((MainActivity) requireActivity()).notifyChanged(i);
                } else {
                    ((MainActivity) requireActivity()).notifyAllChanged();
                }
                dialog.dismiss();
                updateWidget();
                localUpdate();
            });

            dialog.show(requireActivity().getSupportFragmentManager(), "Remove");
        }
    }

    void checkTask(int position) {
        DbHelper dbHelper = new DbHelper(requireActivity());
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        if(elements.get(position).getType() == 0) {
            int id = ((AwaitElement) elements.get(position).getObject()).getId();


            Cursor cursor = db.rawQuery("SELECT * FROM " + DbHelper.T_TASK + " WHERE id = '" + id + "'", null);
            if (cursor.moveToFirst() && cursor.getCount() == 1) {
                boolean s = cursor.getInt(5) > 0;
                ContentValues values = new ContentValues();
                if (s) {
                    PermissionDialog permissionDialog = new PermissionDialog();


                    permissionDialog.setTitleDialog(requireActivity().getString(R.string.uncheck));
                    permissionDialog.setTextDialog(requireActivity().getString(R.string.uncheck_task));
                    permissionDialog.setDrawable(R.drawable.ic_check_circle);
                    permissionDialog.setNegative((view, which) -> {
                        permissionDialog.dismiss();
                        cursor.close();
                    });

                    permissionDialog.setPositive((view, which) -> {
                        ((AwaitElement) elements.get(position).getObject()).setDone(false);

                        long dat = Calendar.getInstance().getTimeInMillis();

                        values.put("date", dat);
                        values.put("status", false);
                        SQLiteDatabase db2 = dbHelper.getWritableDatabase();
                        db2.update(DbHelper.T_TASK, values, " id = '" + id + "'", null);
                        permissionDialog.dismiss();

                        adapter.notifyItemChanged(position);
                        int i = 0;
                        try {
                            i = getPositionOfId(requireActivity(), id);
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }

                        cursor.close();

                        if (i >= 0) {
                            ((MainActivity) requireActivity()).notifyChanged(i);
                        } else {
                            ((MainActivity) requireActivity()).notifyAllChanged();
                        }
                        updateWidget();
                        localUpdate();
                    });

                    permissionDialog.show(requireActivity().getSupportFragmentManager(), "C");
                } else {
                    ((AwaitElement) elements.get(position).getObject()).setDone(true);

                    long dat = Calendar.getInstance().getTimeInMillis();

                    values.put("date", dat);
                    values.put("status", true);
                    SQLiteDatabase db2 = dbHelper.getWritableDatabase();
                    db2.update(DbHelper.T_TASK, values, " id = '" + id + "'", null);

                    adapter.notifyItemChanged(position);
                    int i = 0;
                    try {
                        i = getPositionOfId(requireActivity(), id);
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }

                    cursor.close();

                    if (i >= 0) {
                        ((MainActivity) requireActivity()).notifyChanged(i);
                    } else {
                        ((MainActivity) requireActivity()).notifyAllChanged();
                    }
                    Cursor pendingTasks = db.rawQuery("SELECT * FROM " + DbHelper.T_TASK + " WHERE status = '0'", null);
                    if (pendingTasks.getCount() < 1) {
                        showCongratulations();
                    }
                    pendingTasks.close();
                    updateWidget();
                }
                localUpdate();
            }

            cursor.close();

            adapter.notifyItemChanged(position);
        }
    }

    private static int getPositionOfId(Context context, int id) throws ParseException {
        DbHelper dbHelper = new DbHelper(context);
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Cursor i = db.rawQuery("SELECT * FROM " + DbHelper.T_TASK + " WHERE id = '" + id + "'", null);

        int pos = -1;
        if (i.moveToFirst()) {
            long date = i.getLong(2);


            Calendar c = Calendar.getInstance();
            int today = c.get(Calendar.DAY_OF_WEEK) - 1;
            c.setTimeInMillis(date);
            for (int j = 0; j < 7; j++) {
                if ((c.get(Calendar.DAY_OF_WEEK) - 1) == (today + j) % 7) {
                    pos = j;
                    break;
                }
            }
        }

        i.close();
        return pos;
    }



}
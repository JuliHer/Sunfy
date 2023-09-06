package com.artuok.appwork.fragmets;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.artuok.appwork.CreateTaskActivity;
import com.artuok.appwork.MainActivity;
import com.artuok.appwork.R;
import com.artuok.appwork.adapters.TasksAdapter;
import com.artuok.appwork.db.DbHelper;
import com.artuok.appwork.library.Constants;
import com.artuok.appwork.library.LineChart;
import com.artuok.appwork.objects.AnnouncesElement;
import com.artuok.appwork.objects.CountElement;
import com.artuok.appwork.objects.Item;
import com.artuok.appwork.objects.LineChartElement;
import com.artuok.appwork.objects.TaskElement;
import com.artuok.appwork.objects.TasksElement;
import com.faltenreich.skeletonlayout.Skeleton;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.gms.ads.nativead.NativeAdOptions;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Random;

public class HomeFragment extends Fragment {

    //recyclerView
    private Skeleton skeleton;
    private TasksAdapter adapter;
    private List<Item> elements;
    private TasksAdapter.OnRecyclerListener listener;
    int advirments = 0;

    ActivityResultLauncher<Intent> resultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Intent data = result.getData();
                    assert data != null;
                    if (data.getIntExtra("requestCode", 0) == 2) {
                        ((MainActivity) requireActivity()).notifyAllChanged();
                    }
                }
            }
    );

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_home, container, false);

        setListener();
        restartResultLauncher();
        initRecyclerView(root);

        initDashboard(-1);

        return root;
    }

    private void initDashboard(int pos){
        new AverageAsync(new AverageAsync.ListenerOnEvent() {
            @Override
            public void onPreExecute() {

            }

            @Override
            public void onExecute(boolean b) {
                loadTasks(pos);
            }

            @Override
            public void onPostExecute(boolean b) {
                adapter.notifyDataSetChanged();
            }
        }).exec(true);
    }

    private void initRecyclerView(View root){
        elements = new ArrayList<>();
        adapter = new TasksAdapter(requireActivity(), elements, listener);
        adapter.setAddEventListener((view, pos) -> {
            String d = ((TasksElement) elements.get(pos).getObject()).getDate();

            SimpleDateFormat format = new SimpleDateFormat("dd MMM yyyy");
            Date date = new Date();
            try {
                date = format.parse(d);
            } catch (ParseException e) {
                e.printStackTrace();
            }

            assert date != null;
            long b = date.getTime();

            Intent a = new Intent(requireActivity(), CreateTaskActivity.class);
            a.putExtra("deadline", b);
            a.getIntExtra("requestCode", 2);

            resultLauncher.launch(a);
        });
        LinearLayoutManager manager = new LinearLayoutManager(requireActivity(), LinearLayoutManager.VERTICAL, false);
        RecyclerView recyclerView = root.findViewById(R.id.home_recyclerView);


        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(manager);
        recyclerView.setAdapter(adapter);
    }

    void restartResultLauncher() {
        resultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Intent data = result.getData();
                        assert data != null;
                        if (data.getIntExtra("requestCode", 0) == 3) {
                            ((MainActivity) requireActivity()).navigateTo(2);
                        } else if (data.getIntExtra("requestCode", 0) == 2) {
                            ((MainActivity) requireActivity()).notifyAllChanged();
                        }
                    }
                }
        );
    }

    void setListener() {
        listener = (view, position) -> ((MainActivity) requireActivity()).navigateTo(1);
    }

    private String getTimeDay() {
        Calendar c = Calendar.getInstance();

        int hour = c.get(Calendar.HOUR_OF_DAY);
        if (hour >= 19) {
            return requireActivity().getString(R.string.good_night);
        } else if (hour >= 12) {
            return requireActivity().getString(R.string.good_afternoon);
        } else if (hour < 4) {
            return requireActivity().getString(R.string.good_night);
        } else {
            return requireActivity().getString(R.string.good_morning);
        }
    }

    void loadTasks(int pos) {
        if (!isAdded())
            return;

        CountElement resume = new CountElement(getTimeDay(),
                view -> ((MainActivity) requireActivity()).loadExternalFragment(((MainActivity) requireActivity()).chatFragment, requireActivity().getString(R.string.chat)),
                view -> ((MainActivity) requireActivity()).loadExternalFragment(((MainActivity) requireActivity()).settingsFragment, requireActivity().getString(R.string.settings_menu)));

        if (!SettingsFragment.isLogged(requireActivity())) {
            resume.setChatVisible(false);
        }

        elements.add(new Item(resume, 1));

        boolean changedData = pos >= 0;
        Calendar c = Calendar.getInstance();
        long d = c.getTimeInMillis();
        long day = 86400000;
        int dayWeek = c.get(Calendar.DAY_OF_WEEK) - 1;
        if (!changedData) {
            for (int i = 0; i < 7; i++) {
                long today = d + (day * i);
                if (!isAdded())
                    return;
                List<TaskElement> task = getTaskDay(today);
                int dow = ((dayWeek + i) % 7) + 1;

                if(!isAdded())
                    return;
                String title = dow - 1 == dayWeek ? requireActivity().getString(R.string.today) : Constants.getDayOfWeek(requireActivity(), dow);
                title = dow - 1 == (dayWeek + 1) % 7 ? requireActivity().getString(R.string.tomorrow) : title;

                int inApp = new Random().nextInt() % 10;
                if (inApp == 4 && i > 1) {
                    setAnnounce(elements.size());
                }

                if (i == 2) {
                    LineChartElement element = new LineChartElement(getWeeklyProgress(), view ->
                            ((MainActivity) requireActivity()).loadExternalFragment(((MainActivity) requireActivity()).averagesFragment, requireActivity().getString(R.string.average_fragment_menu))
                    );

                    elements.add(new Item(element, 2));
                }
                Date date = new Date();
                date.setTime(today);
                SimpleDateFormat format = new SimpleDateFormat("dd MMM yyyy");
                String time = format.format(date).toUpperCase();

                elements.add(new Item(new TasksElement(title, time, i, task), 0));
            }
        } else {
            long today = d + (day * pos);
            List<TaskElement> task = getTaskDay(today);
            ((TasksElement) elements.get(pos).getObject()).setData(task);

        }
    }

    private void setAnnounce(int pos) {
        int finalPos = pos + advirments;
        advirments++;
        AdLoader adLoader = new AdLoader.Builder(requireActivity(), "ca-app-pub-5838551368289900/1451662327")
                .forNativeAd(nativeAd -> {
                    int adpos = Math.min(finalPos, elements.size());
                    String title = nativeAd.getHeadline();
                    String body = nativeAd.getBody();
                    String advertiser = nativeAd.getAdvertiser();
                    String price = nativeAd.getPrice();
                    List<NativeAd.Image> images = nativeAd.getImages();
                    NativeAd.Image icon = nativeAd.getIcon();
                    advirments--;
                    AnnouncesElement element = new AnnouncesElement(nativeAd, title, body, advertiser, images, icon);
                    element.setAction(nativeAd.getCallToAction());
                    element.setPrice(price);
                    elements.add(adpos, new Item(element, 12));
                    adapter.notifyItemInserted(adpos);
                }).withAdListener(new AdListener() {
                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        super.onAdFailedToLoad(loadAdError);
                    }

                    @Override
                    public void onAdLoaded() {
                        super.onAdLoaded();
                    }
                }).withNativeAdOptions(new NativeAdOptions.Builder()
                        .setMediaAspectRatio(NativeAdOptions.NATIVE_MEDIA_ASPECT_RATIO_LANDSCAPE)
                        .setRequestMultipleImages(false)
                        .setAdChoicesPlacement(NativeAdOptions.ADCHOICES_BOTTOM_LEFT)
                        .build()).build();
        adLoader.loadAd(new AdRequest.Builder().build());
    }

    public List<TaskElement> getTaskDay(long aday) {

        DbHelper dbHelper = new DbHelper(requireActivity().getApplicationContext());
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        List<TaskElement> tasks = new ArrayList<>();
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(aday);
        int y = c.get(Calendar.YEAR);
        int mm = c.get(Calendar.MONTH);
        int dd = c.get(Calendar.DAY_OF_MONTH);

        c.set(y, mm, dd, 0, 0, 0);
        long td = c.getTimeInMillis();
        c.set(y, mm, dd, 23, 59, 59);
        long tm = c.getTimeInMillis();

        Cursor cursor = db.rawQuery("SELECT * FROM " + DbHelper.T_TASK + " WHERE end_date BETWEEN '" + td + "' AND '" + tm + "' ORDER BY end_date ASC", null);
        if (cursor.moveToFirst()) {
            do {
                boolean check = Integer.parseInt(cursor.getString(5)) > 1;
                String title = cursor.getString(4);
                long t = cursor.getLong(2);
                Calendar m = Calendar.getInstance();
                m.setTimeInMillis(t);
                int minute = m.get(Calendar.MINUTE);
                boolean hourFormat = DateFormat.is24HourFormat(requireActivity());
                int hour = m.get(Calendar.HOUR_OF_DAY);
                if (!hourFormat) {
                    hour = m.get(Calendar.HOUR) == 0 ? 12 : m.get(Calendar.HOUR);
                }

                String mn = minute < 10 ? "0" + minute : "" + minute;

                String time = hour + ":" + mn;
                if (!hourFormat) {
                    time += m.get(Calendar.AM_PM) == Calendar.AM ? " a. m." : " p. m.";
                }
                int subject = cursor.getInt(3);
                Cursor q = db.rawQuery("SELECT * FROM "+DbHelper.t_subjects+" WHERE id = ?", new String[]{subject+""});
                int color = requireActivity().getColor(R.color.gray_400);
                if(q.moveToFirst()){
                    color = q.getInt(2);
                }
                q.close();

                tasks.add(new TaskElement(check, title, time, color, m.getTimeInMillis()));
            } while (cursor.moveToNext());
        }
        cursor.close();

        return tasks;
    }

    public void NotifyDataAdd() {
        elements.clear();
        initDashboard(-1);
    }

    public void NotifyDataChanged(int pos) {
        initDashboard(pos);
    }




    ArrayList<LineChart.LineChartData> getWeeklyProgress() {

        ArrayList<LineChart.LineChartData> data = new ArrayList<>();

        TypedArray ta = requireActivity().obtainStyledAttributes(R.styleable.AppCustomAttrs);
        int color = ta.getColor(R.styleable.AppCustomAttrs_iMainColor, requireActivity().getColor(R.color.green_500));
        ta.recycle();

        data.add(
                new LineChart.LineChartData(
                        requireActivity().getString(R.string.completed_tasks),
                        getLineChartDataSet(),
                        color
                )
        );
        data.add(
                new LineChart.LineChartData(
                        requireActivity().getString(R.string.pending_tasks),
                        getPendingLineChartDataSet(),
                        requireActivity().getColor(R.color.red_500)
                )
        );

        return data;
    }

    ArrayList<LineChart.LineChartDataSet> getLineChartDataSet() {
        DbHelper dbHelper = new DbHelper(requireActivity());
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        ArrayList<LineChart.LineChartDataSet> data = new ArrayList<>();
        Calendar calendar = Calendar.getInstance();
        for (int i = 0; i <= 6; i++) {
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.DAY_OF_WEEK, i + 1);


            long date1 = calendar.getTimeInMillis();
            calendar.set(Calendar.HOUR_OF_DAY, 23);
            calendar.set(Calendar.MINUTE, 59);
            calendar.set(Calendar.SECOND, 59);
            long date2 = calendar.getTimeInMillis();


            Cursor cursor = db.rawQuery(
                    "SELECT * FROM " + DbHelper.T_TASK + " WHERE status = '2' AND completed_date > '" + date1 + "' AND completed_date <= '" + date2 + "'",
                    null
            );

            if (cursor.moveToFirst()) {
                data.add(new LineChart.LineChartDataSet(Constants.getMinDayOfWeek(requireActivity(), i), cursor.getCount()));
            } else {
                data.add(new LineChart.LineChartDataSet(Constants.getMinDayOfWeek(requireActivity(), i), 0));
            }
            cursor.close();

        }

        return data;
    }

    ArrayList<LineChart.LineChartDataSet> getPendingLineChartDataSet() {
        DbHelper dbHelper = new DbHelper(requireActivity());
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        ArrayList<LineChart.LineChartDataSet> data = new ArrayList<>();
        Calendar calendar = Calendar.getInstance();
        for (int i = 0; i <= 6; i++) {

            calendar.set(Calendar.DAY_OF_WEEK, i + 1);
            int yyyy = calendar.get(Calendar.YEAR);
            int mm = calendar.get(Calendar.MONTH);
            int dd = calendar.get(Calendar.DAY_OF_MONTH);
            calendar.set(yyyy, mm, dd, 0, 0, 0);
            long date1 = calendar.getTimeInMillis();
            calendar.set(yyyy, mm, dd, 23, 59, 59);
            long date2 = calendar.getTimeInMillis();


            Cursor cursor = db.rawQuery(
                    "SELECT * FROM " + DbHelper.T_TASK + " WHERE status < '2' AND end_date > '" + date1 + "' AND end_date <= '" + date2 + "'",
                    null
            );

            if (cursor.moveToFirst()) {
                data.add(new LineChart.LineChartDataSet(Constants.getMinDayOfWeek(requireActivity(), i), cursor.getCount()));
            } else {
                data.add(new LineChart.LineChartDataSet(Constants.getMinDayOfWeek(requireActivity(), i), 0));
            }
            cursor.close();
        }


        return data;
    }
}
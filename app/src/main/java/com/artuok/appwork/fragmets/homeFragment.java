package com.artuok.appwork.fragmets;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
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

import com.artuok.appwork.CreateAwaitingActivity;
import com.artuok.appwork.MainActivity;
import com.artuok.appwork.R;
import com.artuok.appwork.adapters.TasksAdapter;
import com.artuok.appwork.db.DbHelper;
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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Random;

public class homeFragment extends Fragment {

    //recyclerView
    private Skeleton skeleton;
    private LinearLayoutManager manager;
    private RecyclerView recyclerView;
    private TasksAdapter adapter;
    private List<Item> elements;
    private ArrayList<Integer> history;
    private TasksAdapter.OnRecyclerListener listener;
    int advirments = 0;

    ActivityResultLauncher<Intent> resultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Intent data = result.getData();
                    if (data.getIntExtra("requestCode", 0) == 3) {
                        ((MainActivity) requireActivity()).navigateTo(2);
                    } else if (data.getIntExtra("requestCode", 0) == 2) {
                        ((MainActivity) requireActivity()).notifyAllChanged();
                    }
                }
            }
    );

    //elementExp
    int posExp = -1;
    int min = 0;
    int total = 0;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.home_fragment, container, false);

        setListener();
        restartResultLauncher();
        history = new ArrayList<>();

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

            long b = date.getTime();

            Intent a = new Intent(requireActivity(), CreateAwaitingActivity.class);
            a.putExtra("deadline", b);
            a.getIntExtra("requestCode", 2);

            resultLauncher.launch(a);
        });
        manager = new LinearLayoutManager(requireActivity(), LinearLayoutManager.VERTICAL, false);
        recyclerView = root.findViewById(R.id.home_recyclerView);


        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(manager);
        recyclerView.setAdapter(adapter);
        loadCount();

        new AverageAsync(new AverageAsync.ListenerOnEvent() {
            @Override
            public void onPreExecute() {

            }

            @Override
            public void onExecute(boolean b) {
                loadTasks(-1);
            }

            @Override
            public void onPostExecute(boolean b) {
                adapter.notifyDataSetChanged();
            }
        }).exec(true);

        return root;
    }

    void restartResultLauncher() {
        resultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Intent data = result.getData();
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

    void loadCount() {
        DbHelper dbHelper = new DbHelper(requireActivity());
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Cursor onHold = db.rawQuery("SELECT * FROM " + DbHelper.T_TASK + " WHERE status = '0'", null);

        int holding = onHold.getCount();
        onHold.close();


        String txt = "";
        String sTxt = "";
        if (holding == 0) {
            txt += "Good Morning!";
            sTxt = "";
        } else {
            if (holding < 10) {
                txt += "0";
            }
            txt += "" + holding;
            sTxt = requireActivity().getString(R.string.pending_tasks);
        }
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

                String title = dow - 1 == dayWeek ? requireActivity().getString(R.string.today) : getDayOfWeek(requireActivity(), dow);
                title = dow - 1 == (dayWeek + 1) % 7 ? requireActivity().getString(R.string.tomorrow) : title;

                int inApp = new Random().nextInt() % 15;
                if (inApp == 8 && i > 1) {
                    setAnnounce(elements.size());
                }

                if (i == 2) {
                    LineChartElement element = new LineChartElement(getWeeklyProgress(), new LineChartElement.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            ((MainActivity) requireActivity()).loadExternalFragment(((MainActivity) requireActivity()).averagesFragment, requireActivity().getString(R.string.average_fragment_menu));
                        }
                    });

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
                boolean check = Integer.parseInt(cursor.getString(5)) > 0;
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

                tasks.add(new TaskElement(check, title, time, m.getTimeInMillis()));
            } while (cursor.moveToNext());
        }
        cursor.close();

        return tasks;
    }

    public void NotifyDataAdd() {
        elements.clear();
        loadTasks(-1);
    }

    public void NotifyDataChanged(int pos) {
        loadTasks(pos);
    }

    public static String getMonthMinor(Context context, int MM) {
        switch (MM) {
            case 0:
                return context.getString(R.string.m_january);
            case 1:
                return context.getString(R.string.m_february);
            case 2:
                return context.getString(R.string.m_march);
            case 3:
                return context.getString(R.string.m_april);
            case 4:
                return context.getString(R.string.m_may);
            case 5:
                return context.getString(R.string.m_june);
            case 6:
                return context.getString(R.string.m_july);
            case 7:
                return context.getString(R.string.m_august);
            case 8:
                return context.getString(R.string.m_september);
            case 9:
                return context.getString(R.string.m_october);
            case 10:
                return context.getString(R.string.m_november);
            case 11:
                return context.getString(R.string.m_december);
            default:
                return "";
        }
    }

    public static String getDayOfWeek(Context context, int dd) {
        switch (dd) {
            case 1:
                return context.getString(R.string.sunday);
            case 2:
                return context.getString(R.string.monday);
            case 3:
                return context.getString(R.string.tuesday);
            case 4:
                return context.getString(R.string.wednesday);
            case 5:
                return context.getString(R.string.thursday);
            case 6:
                return context.getString(R.string.friday);
            case 7:
                return context.getString(R.string.saturday);
            default:
                return "";
        }
    }

    ArrayList<LineChart.LineChartData> getWeeklyProgress() {

        ArrayList<LineChart.LineChartData> data = new ArrayList();

        data.add(
                new LineChart.LineChartData(
                        requireActivity().getString(R.string.completed_tasks),
                        getLineChartDataSet(),
                        requireActivity().getColor(R.color.green_500)
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

        ArrayList<LineChart.LineChartDataSet> data = new ArrayList();
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
                    "SELECT * FROM " + DbHelper.T_TASK + " WHERE status = '1' AND date > '" + date1 + "' AND date <= '" + date2 + "'",
                    null
            );

            if (cursor.moveToFirst()) {
                data.add(new LineChart.LineChartDataSet(getMinDayOfWeek(i), cursor.getCount()));
            } else {
                data.add(new LineChart.LineChartDataSet(getMinDayOfWeek(i), 0));
            }

        }

        return data;
    }

    ArrayList<LineChart.LineChartDataSet> getPendingLineChartDataSet() {
        DbHelper dbHelper = new DbHelper(requireActivity());
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        ArrayList<LineChart.LineChartDataSet> data = new ArrayList();
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
                    "SELECT * FROM " + DbHelper.T_TASK + " WHERE status = '0' AND end_date > '" + date1 + "' AND end_date <= '" + date2 + "'",
                    null
            );

            if (cursor.moveToFirst()) {
                data.add(new LineChart.LineChartDataSet(getMinDayOfWeek(i), cursor.getCount()));
            } else {
                data.add(new LineChart.LineChartDataSet(getMinDayOfWeek(i), 0));
            }
            cursor.close();
        }


        return data;
    }

    private String getMinDayOfWeek(int dayOfWeek) {
        switch (dayOfWeek) {
            case 0:
                return requireContext().getString(R.string.min_sunday);
            case 1:
                return requireContext().getString(R.string.min_monday);
            case 2:
                return requireContext().getString(R.string.min_tuesday);
            case 3:
                return requireContext().getString(R.string.min_wednesday);
            case 4:
                return requireContext().getString(R.string.min_thursday);
            case 5:
                return requireContext().getString(R.string.min_friday);
            case 6:
                return requireContext().getString(R.string.min_saturday);
        }
        return "";
    }

    long getStartEndOFWeek(int enterWeek, int enterYear, boolean start) {
        Calendar calendar = Calendar.getInstance();
        calendar.clear();
        calendar.set(Calendar.WEEK_OF_YEAR, enterWeek);
        calendar.set(Calendar.YEAR, enterYear);
        long startDateInStr = calendar.getTimeInMillis();
        calendar.add(Calendar.DATE, 6);
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        long endDaString = calendar.getTimeInMillis();

        if (start) {
            return startDateInStr;
        } else {
            return endDaString;
        }
    }
}
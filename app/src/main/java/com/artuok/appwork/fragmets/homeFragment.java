package com.artuok.appwork.fragmets;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.artuok.appwork.MainActivity;
import com.artuok.appwork.R;
import com.artuok.appwork.adapters.TasksAdapter;
import com.artuok.appwork.db.DbHelper;
import com.artuok.appwork.objects.Item;
import com.artuok.appwork.objects.TaskElement;
import com.artuok.appwork.objects.TasksElement;
import com.faltenreich.skeletonlayout.Skeleton;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class homeFragment extends Fragment {

    //recyclerView
    private Skeleton skeleton;
    private LinearLayoutManager manager;
    private RecyclerView recyclerView;
    private TasksAdapter adapter;
    private List<Item> elements;
    private ArrayList<Integer> history;
    private TasksAdapter.OnRecyclerListener listener;


    //elementExp
    int posExp = -1;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.home_fragment, container, false);

        setListener();

        history = new ArrayList<>();

        elements = new ArrayList<>();
        adapter = new TasksAdapter(requireActivity(), elements, listener);
        manager = new LinearLayoutManager(requireActivity(), LinearLayoutManager.VERTICAL, false);
        recyclerView = root.findViewById(R.id.home_recyclerView);


        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(manager);


        loadTaksDates(true, false);
        return root;
    }

    void setListener() {
        listener = (view, position) -> ((MainActivity) requireActivity()).navigateTo(1);
    }

    void loadTaksDates(boolean firstTime, boolean dataChanged) {

        if (!firstTime && !dataChanged) {
            changeView();
        } else {
            Calendar c = Calendar.getInstance();
            int year = c.get(Calendar.YEAR);
            int month = c.get(Calendar.MONTH);
            int day = c.get(Calendar.DAY_OF_MONTH);
            int hour = 0;
            for (int i = 0; i < 7; i++) {
                c.set(year, month, day, hour, 0, 0);
                long dayTM = 86400000;
                long timeInMillis = c.getTimeInMillis() + (dayTM * i);
                c.setTimeInMillis(timeInMillis);

                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                String dat1 = format.format(c.getTime());

                c.set(year, month, day, hour, 0, 0);

                timeInMillis = c.getTimeInMillis() + (dayTM * (i + 1));

                c.setTimeInMillis(timeInMillis);


                int Hdif = (24 - c.get(Calendar.HOUR_OF_DAY)) % 24;
                if (Hdif != 0) {
                    long hdif = timeInMillis + (60L * 60 * Hdif * 1000);
                    c.setTimeInMillis(hdif);
                    hour++;
                }

                String dat2 = format.format(c.getTime());

                List<TaskElement> element = getTaskInterval(dat1, dat2);

                history.add(element.size());

                c.set(year, month, day, hour, 0, 0);
                timeInMillis = c.getTimeInMillis() + (dayTM * i);
                long today = c.getTimeInMillis();
                c.setTimeInMillis(timeInMillis);

                String title = "";

                int dif = (int) ((c.getTimeInMillis() - today) / 86400000);
                if (dif == 1) {
                    title += requireActivity().getString(R.string.tomorrow);
                } else if (dif == 0) {
                    title += requireActivity().getString(R.string.today);
                } else {

                    title += getDayOfWeek(requireActivity(), c.get(Calendar.DAY_OF_WEEK));
                }

                String date = c.get(Calendar.DAY_OF_MONTH) + " " + getMonthMinor(requireActivity(), c.get(Calendar.MONTH)) + " " + c.get(Calendar.YEAR);

                elements.add(new Item(new TasksElement(title, date, element), 0));
            }

        }

        if (firstTime) {
            recyclerView.setAdapter(adapter);
        } else if (dataChanged) {
            adapter.notifyDataSetChanged();
        }
    }

    void changeView() {
        Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);
        int hour = 0;
        for (int i = 0; i < 7; i++) {
            c.set(year, month, day, hour, 0, 0);
            long dayTM = 86400000;
            long timeInMillis = c.getTimeInMillis() + (dayTM * i);
            c.setTimeInMillis(timeInMillis);

            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String dat1 = format.format(c.getTime());

            c.set(year, month, day, hour, 0, 0);

            timeInMillis = c.getTimeInMillis() + (dayTM * (i + 1));

            c.setTimeInMillis(timeInMillis);


            int Hdif = (24 - c.get(Calendar.HOUR_OF_DAY)) % 24;
            if (Hdif != 0) {
                long hdif = timeInMillis + (60L * 60 * Hdif * 1000);
                c.setTimeInMillis(hdif);
                hour++;
            }

            String dat2 = format.format(c.getTime());

            List<TaskElement> element = getTaskInterval(dat1, dat2);

            if (history.get(i) != element.size()) {
                if (elements.get(i).getType() == 0) {
                    ((TasksElement) elements.get(i).getObject()).setData(element);
                }
                adapter.notifyItemChanged(i);
                history.add(i, element.size());
            }
        }
    }


    public List<TaskElement> getTaskInterval(String date1, String date2) {
        DbHelper dbHelper = new DbHelper(requireActivity().getApplicationContext());
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        List<TaskElement> tasks = new ArrayList<>();

        Cursor cursor = db.rawQuery("SELECT * FROM " + DbHelper.t_task + " WHERE end_date BETWEEN '" + date1 + "' AND '" + date2 + "' ORDER BY end_date ASC", null);
        if (cursor.moveToFirst()) {
            do {
                boolean check = Integer.parseInt(cursor.getString(6)) > 0;
                String title = cursor.getString(2);
                String[] t = cursor.getString(3).split(" ");

                String[] date = t[0].split("-");
                int year = Integer.parseInt(date[0]);
                int month = Integer.parseInt(date[1]);
                int day = Integer.parseInt(date[2]);
                String[] timed = t[1].split(":");
                int hour = Integer.parseInt(timed[0]);
                int minute = Integer.parseInt(timed[1]);

                Calendar m = Calendar.getInstance();
                m.set(year, (month - 1), day, hour, minute, 0);

                String time = "";
                String mn = minute < 10 ? "0" + minute : "" + minute;
                if (hour > 12) {
                    hour = hour - 12;
                    time += hour + ":" + mn + " PM";
                } else {
                    time += hour + ":" + mn;

                    if (hour == 12) {
                        time += " PM";
                    } else {
                        time += " AM";
                    }
                }

                tasks.add(new TaskElement(check, title, time, m.getTimeInMillis()));
            } while (cursor.moveToNext());
        }
        cursor.close();

        return tasks;
    }

    public void NotifyDataAdd() {
        loadTaksDates(false, false);
    }

    public void NotifyDataChanged() {
        loadTaksDates(false, true);
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

    public void notifyInData(String d) {
        DbHelper dbHelper = new DbHelper(requireActivity().getApplicationContext());
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);
        int hour = 0;
        int pos = -1;

        List<TaskElement> element = new ArrayList<>();

        for (int i = 0; i < 7; i++) {
            c.set(year, month, day, hour, 0, 0);
            long dayTM = 86400000;
            long timeInMillis = c.getTimeInMillis() + (dayTM * i);
            c.setTimeInMillis(timeInMillis);

            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String dat1 = format.format(c.getTime());

            c.set(year, month, day, hour, 0, 0);

            timeInMillis = c.getTimeInMillis() + (dayTM * (i + 1));

            c.setTimeInMillis(timeInMillis);


            int Hdif = (24 - c.get(Calendar.HOUR_OF_DAY)) % 24;
            if (Hdif != 0) {
                long hdif = timeInMillis + (60L * 60 * Hdif * 1000);
                c.setTimeInMillis(hdif);
                hour++;
            }

            String dat2 = format.format(c.getTime());


            if (dat1.equals(d)) {
                pos = i;
                element = getTaskInterval(dat1, dat2);
                break;
            }
        }

        if (pos >= 0) {
            ((TasksElement) elements.get(pos).getObject()).setData(element);
            adapter.notifyItemChanged(pos);
        }
    }
}
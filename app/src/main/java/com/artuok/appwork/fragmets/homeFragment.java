package com.artuok.appwork.fragmets;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
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
import com.artuok.appwork.objects.Item;
import com.artuok.appwork.objects.TaskElement;
import com.artuok.appwork.objects.TasksElement;
import com.faltenreich.skeletonlayout.Skeleton;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
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


    int min = 0;
    int total = 0;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.home_fragment, container, false);

        setListener();

        history = new ArrayList<>();

        elements = new ArrayList<>();
        adapter = new TasksAdapter(requireActivity(), elements, listener);
        adapter.setAddEventListener((view, pos) -> {
            String d = ((TasksElement) elements.get(pos).getObject()).getDate();

            SimpleDateFormat format = new SimpleDateFormat("MMMM dd, yyyy");
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
        loadCount();
        loadTasks(-1);
        return root;
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

    void loadTasks(int pos) {
        boolean changedData = pos >= 0;
        Calendar c = Calendar.getInstance();
        long d = c.getTimeInMillis();
        long day = 86400000;
        int dayWeek = c.get(Calendar.DAY_OF_WEEK) - 1;
        if (!changedData) {
            for (int i = 0; i < 7; i++) {
                long today = d + (day * i);
                List<TaskElement> task = getTaskDay(today);
                int dow = ((dayWeek + i) % 7) + 1;

                String title = dow - 1 == dayWeek ? requireActivity().getString(R.string.today) : getDayOfWeek(requireActivity(), dow);
                title = dow - 1 == (dayWeek + 1) % 7 ? requireActivity().getString(R.string.tomorrow) : title;

                Date date = new Date();
                date.setTime(today);
                SimpleDateFormat format = new SimpleDateFormat("MMMM dd, yyyy");
                String time = format.format(date);

                elements.add(new Item(new TasksElement(title, time, i, task), 0));
            }
        } else {
            long today = d + (day * pos);
            List<TaskElement> task = getTaskDay(today);
            ((TasksElement) elements.get(pos).getObject()).setData(task);
            adapter.notifyItemChanged(pos);
        }
        if (!changedData) {
            recyclerView.setAdapter(adapter);
        }

    }

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
                int hour = m.get(Calendar.HOUR) == 0 ? 12 : m.get(Calendar.HOUR);

                String mn = minute < 10 ? "0" + minute : "" + minute;

                String time = hour +":"+mn;
                time += m.get(Calendar.AM_PM) == Calendar.AM ?  " a. m." : " p. m.";

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
}
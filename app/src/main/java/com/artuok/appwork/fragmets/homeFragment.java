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

        loadTasks(-1);

        return root;
    }

    void setListener() {
        listener = (view, position) -> ((MainActivity) requireActivity()).navigateTo(1);
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


                elements.add(new Item(new TasksElement(title, time, task), 0));
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

        Date datet = new Date();
        datet.setTime(aday);
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd 00:00:00");
        String td = format.format(datet);

        long tomorrow = aday + 86400000;
        datet.setTime(tomorrow);
        String tm = format.format(datet);

        Cursor cursor = db.rawQuery("SELECT * FROM " + DbHelper.t_task + " WHERE end_date BETWEEN '" + td + "' AND '" + tm + "' ORDER BY end_date ASC", null);
        if (cursor.moveToFirst()) {
            do {
                boolean check = Integer.parseInt(cursor.getString(6)) > 0;
                String title = cursor.getString(5);
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

                    int fh = hour;
                    if (hour == 0) {
                        fh = 12;
                    }
                    time += fh + ":" + mn;

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
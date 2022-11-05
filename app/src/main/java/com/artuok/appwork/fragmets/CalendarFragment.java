package com.artuok.appwork.fragmets;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;

import com.artuok.appwork.CreateActivity;
import com.artuok.appwork.R;
import com.artuok.appwork.db.DbHelper;
import com.artuok.appwork.library.WeekView;

import java.util.ArrayList;
import java.util.List;

public class CalendarFragment extends Fragment {

    private List<WeekView.EventsTasks> elements;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_calendar, container, false);

        elements = new ArrayList<>();

        WeekView weekView = root.findViewById(R.id.weekly);


        setEvents();
        weekView.setEvents(elements);
        weekView.setSelectListener((d) -> startCreateActivity(d));

        return root;
    }

    public void startCreateActivity(WeekView.EventsTasks e) {
        Intent intent = new Intent(requireActivity(), CreateActivity.class);
        if (e != null) {
            intent.putExtra("day", e.getDay());
            intent.putExtra("hour", e.getHour());
            intent.putExtra("duration", e.getDuration());
        }

        startActivity(intent);
    }

    public void NotifyChanged() {

    }

    public void setEvents() {
        DbHelper dbHelper = new DbHelper(requireActivity());
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Cursor cursor = db.rawQuery("SELECT * FROM " + DbHelper.t_event, null);
        if (cursor.moveToFirst()) {
            do {
                String title = cursor.getString(1);
                int day = cursor.getInt(2);
                long time = cursor.getLong(3);
                long duration = cursor.getLong(4);
                int type = cursor.getInt(5);
                elements.add(new WeekView.EventsTasks(title, day, time, duration, type));
            } while (cursor.moveToNext());
        }
        cursor.close();
    }
}
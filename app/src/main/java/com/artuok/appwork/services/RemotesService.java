package com.artuok.appwork.services;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.widget.RemoteViewsService;

import com.artuok.appwork.R;
import com.artuok.appwork.adapters.WidgetAdapter;
import com.artuok.appwork.db.DbHelper;
import com.artuok.appwork.objects.WidgetElement;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

public class RemotesService extends RemoteViewsService {
    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        ArrayList<WidgetElement> a = getAwaitings(this);

        return new WidgetAdapter(this, a);
    }

    static public ArrayList<WidgetElement> getAwaitings(Context context) {
        ArrayList<WidgetElement> elements = new ArrayList<>();
        DbHelper dbHelper = new DbHelper(context);
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Calendar c = Calendar.getInstance();
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String dat = format.format(c.getTime());

        Cursor awaitings = db.rawQuery("SELECT * FROM " + DbHelper.t_task + " WHERE status = '0' AND end_date >= '" + dat + "' ORDER BY end_date ASC", null);

        if (awaitings.moveToFirst()) {
            do {
                String[] t = awaitings.getString(3).split(" ");

                String[] date = t[0].split("-");
                int year = Integer.parseInt(date[0]);
                int month = Integer.parseInt(date[1]);
                int day = Integer.parseInt(date[2]);

                String[] time = t[1].split(":");
                int hour = Integer.parseInt(time[0]);
                int minute = Integer.parseInt(time[1]);

                c.set(year, (month - 1), day, hour, minute);
                String status = ((c.getTimeInMillis() - Calendar.getInstance().getTimeInMillis()) / 86400000) + "";
                String d = status + " " + context.getString(R.string.day_left);

                if (Integer.parseInt(status) == 1) {
                    d = context.getString(R.string.tomorrow);
                } else if (Integer.parseInt(status) == 0) {
                    d = context.getString(R.string.today);
                }
                elements.add(new WidgetElement(awaitings.getString(2), awaitings.getString(5), d));
            } while (awaitings.moveToNext());
        }


        awaitings.close();
        return elements;
    }


}

package com.artuok.appwork.widgets;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.artuok.appwork.R;
import com.artuok.appwork.db.DbHelper;
import com.artuok.appwork.fragmets.HomeFragment;
import com.artuok.appwork.library.Constants;
import com.artuok.appwork.objects.AwaitElement;
import com.artuok.appwork.objects.AwaitingElement;

import org.checkerframework.checker.units.qual.A;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class RemoteTodayTaskWidget extends RemoteViewsService {
    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new RemoteTodayTaskWidgetFactory(this.getApplicationContext(), intent);
    }
}

class RemoteTodayTaskWidgetFactory implements RemoteViewsService.RemoteViewsFactory {
    Context context;
    Intent intent;
    List<AwaitingElement> mData;

    public RemoteTodayTaskWidgetFactory(Context context, Intent intent) {
        this.context = context;
        this.intent = intent;
    }

    @Override
    public void onCreate() {
        this.mData = getTodayTask(context);
    }

    @Override
    public void onDataSetChanged() {
        this.mData = getTodayTask(context);
    }

    @Override
    public void onDestroy() {
        mData.clear();
    }

    @Override
    public int getCount() {
        return mData.size();
    }

    @Override
    public RemoteViews getViewAt(int i) {
        AwaitingElement awaiting = mData.get(i);
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.item_widget_tt_list);
        views.setTextViewText(R.id.title_card, awaiting.getTitle());
        views.setTextViewText(R.id.day_card, awaiting.getSubject());
        views.setTextViewText(R.id.date_card, awaiting.getDate());
        views.setTextViewText(R.id.time_card, awaiting.getTime());
        views.setTextColor(R.id.day_card, awaiting.getColorSubject());
        return views;
    }

    @Override
    public RemoteViews getLoadingView() {
        return null;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    public List<AwaitingElement> getTodayTask(Context context){
        DbHelper helper = new DbHelper(context);
        SQLiteDatabase db = helper.getReadableDatabase();

        List<AwaitingElement> element = new ArrayList<>();
        long d = Calendar.getInstance().getTimeInMillis();

        String query = "SELECT t.*, e.name AS name, e.color AS color " +
                "FROM "+DbHelper.T_TASK+" AS t " +
                "JOIN "+DbHelper.T_TAG+" AS e ON t.subject = e.id " +
                "JOIN "+DbHelper.T_PROJECTS+" AS p ON e.proyect = p.id " +
                "WHERE t.status < ? ORDER BY t.deadline ASC;";
        Cursor q = db.rawQuery(query, new String[]{"2"});

        if(q.moveToFirst()){
            do{
                long date = q.getLong(5);
                String dates = Constants.getDateString(context, date);
                String times = Constants.getTimeString(context, date);
                int done = q.getInt(7);
                String subject = q.getString(10);
                int color = q.getInt(11);

                int id = q.getInt(0);
                String title = q.getString(1);
                AwaitingElement awaitingElement = new AwaitingElement(id, title, subject, dates, times, done);
                awaitingElement.setColorSubject(color);
                element.add(awaitingElement);
            }while (q.moveToNext());
        }
        q.close();

        return element;
    }
}

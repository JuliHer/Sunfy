package com.artuok.appwork.widgets;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.artuok.appwork.R;
import com.artuok.appwork.db.DbHelper;
import com.artuok.appwork.fragmets.homeFragment;
import com.artuok.appwork.objects.AwaitingElement;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class RemoteTodayTaskWidget extends RemoteViewsService {
    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new RemoteTodayTaskWidgetFactory(this.getApplicationContext(), intent);
    }

    static class RemoteTodayTaskWidgetFactory implements RemoteViewsFactory{
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
            Cursor q = db.rawQuery("SELECT * FROM "+DbHelper.T_TASK+" WHERE end_date > '"+d+"' AND status = '0' ORDER BY end_date ASC", null);

            if(q.moveToFirst()){
                do{
                    int id = q.getInt(0);
                    String title = q.getString(4);
                    long status = q.getLong(2);

                    Calendar c = Calendar.getInstance();
                    c.setTimeInMillis(status);
                    int day = c.get(Calendar.DAY_OF_MONTH);
                    int month = c.get(Calendar.MONTH);
                    int hour = c.get(Calendar.HOUR) == 0 ? 12 : c.get(Calendar.HOUR);
                    int minute = c.get(Calendar.MINUTE);

                    String dd = day < 10 ? "0" + day : "" + day;
                    String dates = dd + " " + homeFragment.getMonthMinor(context, (month));
                    String mn = minute < 10 ? "0" + minute : "" + minute;
                    String times = hour +":"+mn;
                    times += c.get(Calendar.AM_PM) == Calendar.AM ?  " a. m." : " p. m.";

                    String m = homeFragment.getDayOfWeek(context, c.get(Calendar.DAY_OF_WEEK));

                    AwaitingElement awaiting = new AwaitingElement(id, title, m, dates, times, status);
                    element.add(awaiting);
                }while (q.moveToNext());
            }
            q.close();

            return element;
        }
    }
}

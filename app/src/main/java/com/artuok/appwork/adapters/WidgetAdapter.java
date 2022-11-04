package com.artuok.appwork.adapters;


import android.content.Context;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.artuok.appwork.R;
import com.artuok.appwork.objects.WidgetElement;

import java.util.ArrayList;

public class WidgetAdapter implements RemoteViewsService.RemoteViewsFactory {
    ArrayList<WidgetElement> mData;
    Context context;

    public WidgetAdapter(Context context, ArrayList<WidgetElement> data) {
        this.mData = data;
        this.context = context;
    }


    @Override
    public void onCreate() {

    }

    @Override
    public void onDataSetChanged() {

    }

    @Override
    public void onDestroy() {

    }

    @Override
    public int getCount() {
        return mData.size();
    }

    @Override
    public RemoteViews getViewAt(int i) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_list);
        views.setTextViewText(R.id.widget_title, mData.get(i).getSubject());
        views.setTextViewText(R.id.widget_date, mData.get(i).getDate());
        views.setTextViewText(R.id.widget_desc, mData.get(i).getDesc());
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
}

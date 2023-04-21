package com.artuok.appwork.objects;

import android.view.View;

import com.artuok.appwork.library.LineChart;

import java.util.ArrayList;

public class LineChartElement {
    ArrayList<LineChart.LineChartData> data;
    OnClickListener viewMore;

    public LineChartElement(ArrayList<LineChart.LineChartData> data, OnClickListener viewMore) {
        this.data = data;
        this.viewMore = viewMore;
    }

    public OnClickListener getViewMore() {
        return viewMore;
    }

    public ArrayList<LineChart.LineChartData> getData() {
        return data;
    }

    public interface OnClickListener {
        void onClick(View view);
    }
}

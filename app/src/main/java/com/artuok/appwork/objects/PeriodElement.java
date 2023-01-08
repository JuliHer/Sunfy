package com.artuok.appwork.objects;

import java.util.Calendar;

public class PeriodElement {
    String title;
    int color;
    Calendar start;
    Calendar end;

    public PeriodElement(String title, Calendar start, Calendar end, int color) {
        this.title = title;
        this.color = color;
        this.start = start;
        this.end = end;
    }

    public String getTitle() {
        return title;
    }

    public int getColor() {
        return color;
    }

    public Calendar getStart() {
        return start;
    }

    public Calendar getEnd() {
        return end;
    }
}

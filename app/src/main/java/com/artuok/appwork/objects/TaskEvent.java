package com.artuok.appwork.objects;

public class TaskEvent {
    String title;
    String hour;
    long timeInMillis;
    int color;

    public TaskEvent(String title, String hour, long timeInMillis, int color) {
        this.title = title;
        this.hour = hour;
        this.timeInMillis = timeInMillis;
        this.color = color;
    }

    public String getTitle() {
        return title;
    }

    public String getHour() {
        return hour;
    }

    public long getTimeInMillis() {
        return timeInMillis;
    }

    public int getColor() {
        return color;
    }
}

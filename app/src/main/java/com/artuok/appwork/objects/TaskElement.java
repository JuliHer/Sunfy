package com.artuok.appwork.objects;

public class TaskElement {

    boolean check;
    String title;
    String date;
    long millisSeconds;
    int color;

    public TaskElement(boolean check, String title, String date, int color, long millisSeconds) {
        this.check = check;
        this.title = title;
        this.date = date;
        this.millisSeconds = millisSeconds;
        this.color = color;
    }

    public int getColor() {
        return color;
    }

    public boolean isCheck() {
        return check;
    }

    public String getTitle() {
        return title;
    }

    public String getDate() {
        return date;
    }

    public long getMillisSeconds() {
        return millisSeconds;
    }
}

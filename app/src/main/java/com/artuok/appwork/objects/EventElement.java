package com.artuok.appwork.objects;

public class EventElement {

    private long id;
    private String title;
    private int dayOfWeek;
    private long time;
    private long duration;
    private int type;
    private int subject;

    public EventElement() {

    }

    public EventElement(long id, String title, int dayOfWeek, long time, long duration, int type, int subject) {
        this.id = id;
        this.title = title;
        this.dayOfWeek = dayOfWeek;
        this.time = time;
        this.duration = duration;
        this.type = type;
        this.subject = subject;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getDayOfWeek() {
        return dayOfWeek;
    }

    public void setDayOfWeek(int dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getSubject() {
        return subject;
    }

    public void setSubject(int subject) {
        this.subject = subject;
    }
}

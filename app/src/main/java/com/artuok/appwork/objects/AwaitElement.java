package com.artuok.appwork.objects;

public class AwaitElement {
    private int id;
    private String title;
    private String status;
    private String date;
    private String time;
    private String subject;
    private int taskColor;
    private int statusColor;
    private boolean done = false;
    private boolean liked = false;
    private boolean mine = true;


    public AwaitElement(int id, String title, String status, String date, String time, int taskColor, int statusColor, boolean mine) {
        this.id = id;
        this.title = title;
        this.status = status;
        this.date = date;
        this.time = time;
        this.taskColor = taskColor;
        this.statusColor = statusColor;
        this.mine = mine;
    }

    public boolean isMine() {
        return mine;
    }

    public void setMine(boolean mine) {
        this.mine = mine;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public boolean isLiked() {
        return liked;
    }

    public void setLiked(boolean liked) {
        this.liked = liked;
    }

    public boolean isDone() {
        return done;
    }

    public void setDone(boolean done) {
        this.done = done;
    }

    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getStatus() {
        return status;
    }

    public String getDate() {
        return date;
    }

    public String getTime() {
        return time;
    }

    public int getTaskColor() {
        return taskColor;
    }

    public int getStatusColor() {
        return statusColor;
    }

    public void setStatusColor(int statusColor) {
        this.statusColor = statusColor;
    }
}

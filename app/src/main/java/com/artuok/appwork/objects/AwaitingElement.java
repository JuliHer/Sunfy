package com.artuok.appwork.objects;

public class AwaitingElement {
    int id;
    String title;
    String subject;
    String date;
    String time;
    long status;
    boolean done = false;
    boolean open = true;
    int colorSubject;

    public AwaitingElement(int id, String title, String subject, String date, String time, long status){
        this.id = id;
        this.title = title;
        this.subject = subject;
        this.date = date;
        this.time = time;
        this.status = status;
    }

    public String getTime() {
        return time;
    }

    public int getColorSubject() {
        return colorSubject;
    }

    public void setColorSubject(int colorSubject) {
        this.colorSubject = colorSubject;
    }

    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getSubject() {
        return subject;
    }

    public String getDate() {
        return date;
    }

    public long getStatus() {
        return status;
    }

    public boolean isDone() {
        return done;
    }

    public void setDone(boolean done) {
        this.done = done;
    }

    public boolean isOpen() {
        return open;
    }

    public void setOpen(boolean open) {
        this.open = open;
    }
}

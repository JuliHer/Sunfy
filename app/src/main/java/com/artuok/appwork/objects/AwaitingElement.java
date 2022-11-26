package com.artuok.appwork.objects;

public class AwaitingElement {
    int id;
    String title;
    String subject;
    String date;
    String status;
    String description;
    boolean statusB;
    boolean open;
    int colorSubject;

    public AwaitingElement(int id, String title, String subject, String date, String status, String description, boolean statusB, boolean open) {
        this.id = id;
        this.title = title;
        this.subject = subject;
        this.date = date;
        this.status = status;
        this.description = description;
        this.statusB = statusB;
        this.open = open;
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

    public String getStatus() {
        return status;
    }

    public String getDescription() {
        return description;
    }

    public boolean isStatusB() {
        return statusB;
    }

    public boolean isOpen() {
        return open;
    }

    public void setStatusB(boolean statusB) {
        this.statusB = statusB;
    }
}

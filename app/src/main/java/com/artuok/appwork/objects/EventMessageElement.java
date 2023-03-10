package com.artuok.appwork.objects;

public class EventMessageElement {
    String id;
    String title;
    String userId;
    long endDate;
    long date;
    long message;

    boolean added = false;



    public EventMessageElement(String id, String title, String userId, long date, long endDate) {
        this.id = id;
        this.title = title;
        this.userId = userId;
        this.endDate = endDate;
    }

    public long getMessage() {
        return message;
    }

    public void setMessage(long message) {
        this.message = message;
    }

    public long getDate() {
        return date;
    }

    public boolean isAdded() {
        return added;
    }

    public void setAdded(boolean added) {
        this.added = added;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getUserId() {
        return userId;
    }

    public long getEndDate() {
        return endDate;
    }
}

package com.artuok.appwork.objects;

public class EventMessageElement {
    private long deadline;
    private String description;
    private String user;

    public EventMessageElement(long deadline, String description, String user) {

        this.deadline = deadline;
        this.description = description;
        this.user = user;
    }


    public long getDeadline() {
        return deadline;
    }

    public String getDescription() {
        return description;
    }

    public String getUser() {
        return user;
    }
}

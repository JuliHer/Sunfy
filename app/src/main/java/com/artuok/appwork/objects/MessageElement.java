package com.artuok.appwork.objects;

public class MessageElement {

    private String id;
    private String message;
    private long timestamp;
    private int user;
    private String name;
    private int status;
    private boolean select;
    private MessageElement reply;

    private EventMessageElement task = null;

    public MessageElement(String id, String message, long timestamp, int user, String name, int status) {
        this.id = id;
        this.message = message;
        this.timestamp = timestamp;
        this.user = user;
        this.name = name;
        this.status = status;
    }

    public MessageElement getReply() {
        return reply;
    }

    public void setReply(MessageElement reply) {
        this.reply = reply;
    }

    public boolean isSelect() {
        return select;
    }

    public void setSelect(boolean select) {
        this.select = select;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public int getUser() {
        return user;
    }

    public void setUser(int user) {
        this.user = user;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public EventMessageElement getTask() {
        return task;
    }

    public void setTask(EventMessageElement task) {
        this.task = task;
    }
}

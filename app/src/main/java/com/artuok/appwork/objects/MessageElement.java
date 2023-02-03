package com.artuok.appwork.objects;

public class MessageElement {

    String message;
    int mine;
    int type;
    String id;
    long timestamp;
    int status;
    int reaction;

    public MessageElement(String id, String message, int mine, long timestamp, int status, int reaction) {
        this.message = message;
        this.mine = mine;
        this.id = id;
        this.timestamp = timestamp;
        this.status = status;
        this.reaction = reaction;
    }

    public String getId() {
        return id;
    }

    public String getMessage() {
        return message;
    }

    public int getMine() {
        return mine;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public int getStatus() {
        return status;
    }

    public int getReaction() {
        return reaction;
    }
}

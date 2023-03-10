package com.artuok.appwork.objects;

public class MessageElement {

    String message;
    int mine;
    int type;
    String id;
    long timestamp;
    int status;
    int reaction;
    String theirName;
    String messageReplyed;
    EventMessageElement event;
    boolean select = false;

    public MessageElement(String id, String message, int mine, long timestamp, int status, int reaction, String theirName) {
        this.message = message;
        this.mine = mine;
        this.id = id;
        this.timestamp = timestamp;
        this.status = status;
        this.reaction = reaction;
        this.theirName = theirName;
    }

    public boolean isSelect() {
        return select;
    }

    public void setSelect(boolean select) {
        this.select = select;
    }

    public void addEvent(EventMessageElement element){
        this.event = element;
    }

    public EventMessageElement getEvent(){
        return this.event;
    }

    public void setTheirName(String theirName) {
        this.theirName = theirName;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getTheirName() {
        return theirName;
    }

    public String getMessageReplyed() {
        return messageReplyed;
    }

    public void setMessageReplyed(String messageReplyed) {
        this.messageReplyed = messageReplyed;
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

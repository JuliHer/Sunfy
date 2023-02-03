package com.artuok.appwork.objects;

public class ChatElement {
    String id;
    String name;
    String number;
    String chat;
    String desc;
    String numberInternational;
    String image;
    boolean Log;

    public ChatElement(String id, String name, String desc, String chat, String number, String ISO, String image, boolean hasLog) {
        this.id = id;
        this.name = name;
        this.chat = chat;
        this.desc = desc;
        this.number = number;
        this.numberInternational = numberInternational;
        this.image = image;
        this.Log = hasLog;
    }

    public String getChat() {
        return chat;
    }

    public void setChat(String chat) {
        this.chat = chat;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public boolean isLog() {
        return Log;
    }

    public void setLog(boolean hasLog) {
        this.Log = hasLog;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNumberInternational() {
        return numberInternational;
    }

    public void setNumberInternational(String numberInternational) {
        this.numberInternational = numberInternational;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }
}

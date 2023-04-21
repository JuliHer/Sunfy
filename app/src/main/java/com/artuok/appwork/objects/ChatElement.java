package com.artuok.appwork.objects;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;

import androidx.annotation.Nullable;

public class ChatElement {
    String id;
    String name;
    String number;
    String chat;
    String desc;
    String numberInternational;
    Bitmap image;
    boolean Log;
    String publicKey;
    long timestamp;
    boolean group;
    Drawable contentIcon;
    int status = -1;

    public ChatElement(String id, String name, String desc, String chat, String number, String ISO, boolean hasLog, long timestamp) {
        this.id = id;
        this.name = name;
        this.chat = chat;
        this.desc = desc;
        this.number = number;
        this.numberInternational = ISO;
        this.Log = hasLog;
        this.timestamp = timestamp;
    }

    public Drawable getContentIcon() {
        return contentIcon;
    }

    public void setContentIcon(Drawable contentIcon) {
        this.contentIcon = contentIcon;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public boolean isGroup() {
        return this.group;
    }

    public void setGroup(boolean group) {
        this.group = group;
    }

    public void setImage(Bitmap image) {
        this.image = image;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getPublicKey() {
        return publicKey;
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

    public Bitmap getImage() {
        return image;
    }


    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }
}

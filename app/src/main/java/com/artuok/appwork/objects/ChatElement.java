package com.artuok.appwork.objects;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;

import androidx.annotation.Nullable;

public class ChatElement {
    private String name;
    private String desc;
    private String chatId;
    private String publicKey;
    private String pictureName;
    private Bitmap picture;
    private int status = -1;
    private long timestamp = 0;


    public ChatElement(String name, String desc, String chatId, String publicKey, String pictureName, Bitmap picture, int status, long timestamp) {
        this.name = name;
        this.desc = desc;
        this.chatId = chatId;
        this.publicKey = publicKey;
        this.picture = picture;
        this.pictureName = pictureName;
        this.status = status;
        this.timestamp = timestamp;
    }

    public String getPictureName() {
        return pictureName;
    }

    public void setPictureName(String pictureName) {
        this.pictureName = pictureName;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getName() {
        return name;
    }

    public String getDesc() {
        return desc;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getChatId() {
        return chatId;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public Bitmap getPicture() {
        return picture;
    }

    public int getStatus() {
        return status;
    }
}

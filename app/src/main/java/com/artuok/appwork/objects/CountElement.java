package com.artuok.appwork.objects;

import android.view.View;

public class CountElement {
    private String text;
    private OnIconClickListener chatListener, settingsListener;
    private boolean chatVisible = true, settingsVisible = true;

    public CountElement(String text, OnIconClickListener chatListener, OnIconClickListener settingsListener) {
        this.text = text;
        this.chatListener = chatListener;
        this.settingsListener = settingsListener;
    }

    public boolean isChatVisible() {
        return chatVisible;
    }

    public void setChatVisible(boolean chatVisible) {
        this.chatVisible = chatVisible;
    }

    public boolean isSettingsVisible() {
        return settingsVisible;
    }

    public void setSettingsVisible(boolean settingsVisible) {
        this.settingsVisible = settingsVisible;
    }

    public String getText() {
        return text;
    }

    public OnIconClickListener getChatListener() {
        return chatListener;
    }

    public OnIconClickListener getSettingsListener() {
        return settingsListener;
    }

    public interface OnIconClickListener {
        void onClick(View view);
    }
}

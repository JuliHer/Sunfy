package com.artuok.appwork.objects;

public class CountElement {
    private String count;
    private String text;

    public CountElement(String count, String text) {
        this.count = count;
        this.text = text;
    }

    public String getCount() {
        return count;
    }

    public void setCount(String count) {
        this.count = count;
    }

    public String getText() {
        return text;
    }
}

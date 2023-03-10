package com.artuok.appwork.objects;

public class TextElement {
    String text;
    int textSize = -1;
    int color = -1;

    public TextElement(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }

    public void setTextSize(int textSize) {
        this.textSize = textSize;
    }

    public void setColor(int color) {
        this.color = color;
    }

    public int getTextSize() {
        return textSize;
    }

    public int getColor() {
        return color;
    }
}

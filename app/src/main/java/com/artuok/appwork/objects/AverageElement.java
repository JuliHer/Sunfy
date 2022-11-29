package com.artuok.appwork.objects;

public class AverageElement {
    String subject;
    int color;
    int progress;
    int max;

    public AverageElement(String subject, int color, int progress, int max) {
        this.subject = subject;
        this.color = color;
        this.progress = progress;
        this.max = max;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public int getColor() {
        return color;
    }

    public void setStatus(int color) {
        this.color = color;
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    public int getMax() {
        return max;
    }

    public void setMax(int max) {
        this.max = max;
    }
}

package com.artuok.appwork.objects;

public class AverageElement {
    String subject;
    String status;
    int progress;
    int max;

    public AverageElement(String subject, String status, int progress, int max) {
        this.subject = subject;
        this.status = status;
        this.progress = progress;
        this.max = max;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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

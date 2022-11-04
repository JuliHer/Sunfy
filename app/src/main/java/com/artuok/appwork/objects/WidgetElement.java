package com.artuok.appwork.objects;

public class WidgetElement {
    String subject;
    String desc;
    String date;

    public WidgetElement(String subject, String desc, String date) {
        this.subject = subject;
        this.desc = desc;
        this.date = date;
    }

    public String getSubject() {
        return subject;
    }

    public String getDesc() {
        return desc;
    }

    public String getDate() {
        return date;
    }
}

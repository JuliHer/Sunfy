package com.artuok.appwork.objects;

import java.util.List;

public class TasksElement {

    String title;
    String date;
    String dateTime;
    List<TaskElement> data;
    int day;

    public TasksElement(String title, String date, int day, List<TaskElement> data) {
        this.title = title;
        this.date = date;
        this.data = data;
        this.day = day;
    }

    public int getDay() {
        return day;
    }

    public String getDateTime() {
        return dateTime;
    }

    public void setDateTime(String dateTime) {
        this.dateTime = dateTime;
    }

    public String getTitle() {
        return title;
    }

    public String getDate() {
        return date;
    }

    public List<TaskElement> getData() {
        return data;
    }

    public void setData(List<TaskElement> data) {
        this.data = data;
    }
}

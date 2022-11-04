package com.artuok.appwork.objects;

import java.util.List;

public class TasksElement {

    String title;
    String date;
    List<TaskElement> data;

    public TasksElement(String title, String date, List<TaskElement> data) {
        this.title = title;
        this.date = date;
        this.data = data;
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

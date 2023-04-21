package com.artuok.appwork.objects;

public class SubjectElement {
    String name;
    String desc;
    int color;
    int id;

    public SubjectElement(String name, String desc, int color) {
        this.name = name;
        this.desc = desc;
        this.color = color;
    }

    public SubjectElement(int id, String name, String desc, int color) {
        this.name = name;
        this.color = color;
        this.desc = desc;
        this.id = id;
    }

    public String getDesc() {
        return desc;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getColor() {
        return color;
    }
}

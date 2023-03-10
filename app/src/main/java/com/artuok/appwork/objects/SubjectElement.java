package com.artuok.appwork.objects;

public class SubjectElement {
    String name;
    int color;
    int id;

    public SubjectElement(String name, int color) {
        this.name = name;
        this.color = color;
    }

    public SubjectElement(int id, String name, int color) {
        this.name = name;
        this.color = color;
        this.id = id;
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

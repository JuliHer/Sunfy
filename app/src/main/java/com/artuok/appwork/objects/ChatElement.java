package com.artuok.appwork.objects;

public class ChatElement {
    int id;
    String name;
    String number;
    String image;

    public ChatElement(int id, String name, String number, String image) {
        this.id = id;
        this.name = name;
        this.number = number;
        this.image = image;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }
}

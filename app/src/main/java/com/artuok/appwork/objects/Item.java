package com.artuok.appwork.objects;

public class Item {
    private Object object;
    private int type;

    public Item(Object object, int type) {
        this.object = object;
        this.type = type;
    }

    public Object getObject() {
        return object;
    }

    public int getType() {
        return type;
    }
}

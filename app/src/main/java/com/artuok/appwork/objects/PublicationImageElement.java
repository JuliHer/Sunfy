package com.artuok.appwork.objects;

import android.graphics.Bitmap;

public class PublicationImageElement {
    Bitmap map;
    boolean last;

    public PublicationImageElement(Bitmap map) {
        this.map = map;
        this.last = last;
    }

    public Bitmap getMap() {
        return map;
    }

    public boolean isLast() {
        return last;
    }

    public void setLast(boolean last) {
        this.last = last;
    }
}

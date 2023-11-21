package com.artuok.appwork.objects;

import android.graphics.Bitmap;

public class ProyectElement {
    private long id;
    private String pId;
    private String name;
    private int image = -1;


    public ProyectElement(long id, String pId, String name, int image) {
        this.id = id;
        this.pId = pId;
        this.name = name;
        this.image = image;
    }

    public long getId() {
        return id;
    }

    public String getpId() {
        return pId;
    }

    public String getName() {
        return name;
    }

    public int getImage() {
        return image;
    }
}

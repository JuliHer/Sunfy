package com.artuok.appwork.objects;

import android.graphics.Bitmap;

public class ProyectElement {
    private long id;
    private String pId;
    private String name;
    private Bitmap image;


    public ProyectElement(long id, String pId, String name, Bitmap image) {
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

    public Bitmap getImage() {
        return image;
    }
}

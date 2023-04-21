package com.artuok.appwork.objects;

import com.google.android.gms.ads.nativead.NativeAd.Image;

import java.util.List;

public class AnnouncesElement {
    private String title;
    private String body;
    private String announser;
    private String action;
    private String price;
    private List<Image> images;
    private Image icon;

    public AnnouncesElement(String title, String body, String announser, List<Image> images, Image icon) {
        this.title = title;
        this.body = body;
        this.announser = announser;
        this.images = images;
        this.icon = icon;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getPrice() {
        return price;
    }

    public void setPrice(String price) {
        this.price = price;
    }

    public String getTitle() {
        return title;
    }

    public String getBody() {
        return body;
    }

    public String getAnnounser() {
        return announser;
    }

    public List<Image> getImages() {
        return images;
    }

    public Image getIcon() {
        return icon;
    }
}

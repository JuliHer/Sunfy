package com.artuok.appwork.objects;

import com.google.android.gms.ads.MediaContent;
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.gms.ads.nativead.NativeAd.Image;

public class AnnouncesElement {
    private String title;
    private String body;
    private String announser;
    private String action;
    private String price;
    private MediaContent content;
    private Image icon;
    private NativeAd nativeAd;
    private int position;

    public AnnouncesElement(NativeAd nativeAd, String title, String body, String announser, MediaContent content, Image icon) {
        this.nativeAd = nativeAd;
        this.title = title;
        this.body = body;
        this.announser = announser;
        this.content = content;
        this.icon = icon;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
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

    public MediaContent getContent() {
        return content;
    }

    public Image getIcon() {
        return icon;
    }

    public NativeAd getNativeAd() {
        return nativeAd;
    }
}

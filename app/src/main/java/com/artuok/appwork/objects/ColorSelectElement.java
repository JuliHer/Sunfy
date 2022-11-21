package com.artuok.appwork.objects;

public class ColorSelectElement {
    private String name;
    private int colorVibrant;
    private int colorDark;

    public ColorSelectElement(String name, int colorVibrant, int colorDark) {
        this.name = name;
        this.colorVibrant = colorVibrant;
        this.colorDark = colorDark;
    }

    public String getName() {
        return name;
    }

    public int getColorVibrant() {
        return colorVibrant;
    }

    public int getColorDark() {
        return colorDark;
    }
}

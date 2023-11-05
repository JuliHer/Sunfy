package com.artuok.appwork.objects;

import android.view.View;

import java.util.List;

public class ProyectsElement {

    private List<Item> elements;
    private OnProyectOpenListener listener;


    public ProyectsElement(List<Item> elements, OnProyectOpenListener listener) {
        this.elements = elements;
        this.listener = listener;
    }

    public List<Item> getElements() {
        return elements;
    }

    public OnProyectOpenListener getListener() {
        return listener;
    }

    public interface OnProyectOpenListener{
        public void onProyectOpen(View view, int position);
    }
}

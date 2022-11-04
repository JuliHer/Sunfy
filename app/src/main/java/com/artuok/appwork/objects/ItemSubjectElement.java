package com.artuok.appwork.objects;

import com.artuok.appwork.adapters.SubjectAdapter;

public class ItemSubjectElement {
    private Object object;
    private int type;
    private SubjectAdapter.SubjectClickListener listener;

    public ItemSubjectElement(Object object, int type) {
        this.object = object;
        this.type = type;
    }

    public ItemSubjectElement(int type, SubjectAdapter.SubjectClickListener listener) {
        this.type = type;
        this.listener = listener;
    }

    public SubjectAdapter.SubjectClickListener getListener() {
        return listener;
    }

    public Object getObject() {
        return object;
    }

    public int getType() {
        return type;
    }
}

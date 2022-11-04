package com.artuok.appwork.objects;

public class StatisticsElement {
    int done;
    int onHold;
    int losed;

    public StatisticsElement(int done, int onHold, int losed) {
        this.done = done;
        this.onHold = onHold;
        this.losed = losed;
    }

    public int getDone() {
        return done;
    }

    public int getOnHold() {
        return onHold;
    }

    public int getLosed() {
        return losed;
    }
}

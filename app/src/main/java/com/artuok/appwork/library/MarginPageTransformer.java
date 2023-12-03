package com.artuok.appwork.library;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.viewpager2.widget.ViewPager2;

public class MarginPageTransformer implements ViewPager2.PageTransformer {
    private final int marginPx;

    public MarginPageTransformer(int marginPx) {
        this.marginPx = marginPx;
    }

    @Override
    public void transformPage(@NonNull View page, float position) {
        int viewPagerWidth = ((View) page.getParent()).getWidth();
        int itemWidth = page.getWidth();
        float offset = position * -(2 * marginPx + itemWidth) / 3;

        if (position < -1) {
            page.setTranslationX(offset - marginPx);
        } else if (position <= 1) {
            page.setTranslationX(offset);
        } else {
            page.setTranslationX(offset + marginPx);
        }
    }
}


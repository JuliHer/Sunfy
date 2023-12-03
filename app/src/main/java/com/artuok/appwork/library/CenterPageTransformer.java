package com.artuok.appwork.library;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.viewpager2.widget.ViewPager2;

public class CenterPageTransformer implements ViewPager2.PageTransformer {

    @Override
    public void transformPage(@NonNull View page, float position) {
        float scaleFactor = 0.6f; // Ajusta según tu preferencia de escala

        if (position < -1) {
            page.setScaleX(scaleFactor);
            page.setScaleY(scaleFactor);
        } else if (position <= 1) {
            float scaleFactorAdjusted = Math.max(scaleFactor, 1 - Math.abs(position) * 0.2f);
            page.setScaleX(scaleFactorAdjusted);
            page.setScaleY(scaleFactorAdjusted);
            page.setAlpha(1 - Math.abs(position));
        } else {
            page.setScaleX(scaleFactor);
            page.setScaleY(scaleFactor);
        }
    }
}


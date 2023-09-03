package com.artuok.appwork.library;

import androidx.recyclerview.widget.RecyclerView;

import com.faltenreich.skeletonlayout.Skeleton;
import com.faltenreich.skeletonlayout.SkeletonLayoutUtils;

public class FalseSkeleton {

    public FalseSkeleton(){

    }

    public static Skeleton applySkeleton(RecyclerView recyclerView, int listItemLayoutResId, int itemCount){
        return SkeletonLayoutUtils.applySkeleton(recyclerView, listItemLayoutResId, itemCount);
    }
}

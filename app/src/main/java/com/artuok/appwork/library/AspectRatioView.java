package com.artuok.appwork.library;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.Log;

import androidx.cardview.widget.CardView;

import com.artuok.appwork.R;

public class AspectRatioView extends CardView {

    private int ORIENTATION = 0;
    private final int VERTICAL = 1;
    private final int HORIZONTAL = 0;
    private String RATIO;
    private TypedArray a;

    public AspectRatioView(Context context) {
        super(context);
    }

    public AspectRatioView(Context context, AttributeSet attrs) {
        super(context, attrs);

        a = context.obtainStyledAttributes(attrs, R.styleable.AspectRatioView, 0, 0);
        ORIENTATION = a.getInt(R.styleable.AspectRatioView_orientationRatio, 0);
        RATIO = a.getString(R.styleable.AspectRatioView_aspectRatio);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        int ratioWidth = widthSize;
        int ratioHeight = heightSize;

        if(RATIO != null && RATIO != ""){
            if(ORIENTATION == VERTICAL){
                String[] s = RATIO.split(":");
                int w = Integer.parseInt(s[0]);
                int h = Integer.parseInt(s[1]);
                ratioHeight = ratioWidth / w * h;


            }else if(ORIENTATION == HORIZONTAL){
                String[] s = RATIO.split(":");
                int h = Integer.parseInt(s[0]);
                int w = Integer.parseInt(s[1]);

                ratioWidth = ratioHeight / h * w;
            }
        }
        this.setMeasuredDimension(ratioWidth, ratioHeight);
        super.measureChildren(MeasureSpec.makeMeasureSpec(ratioWidth, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(ratioHeight, MeasureSpec.EXACTLY));
    }
}

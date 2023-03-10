package com.artuok.appwork.library;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.artuok.appwork.R;

public class MsgFlexboxLayout extends RelativeLayout {

    private TextView viewMain;
    private View viewSlave;
    private int viewWidth = 0;

    private TypedArray a;

    private RelativeLayout.LayoutParams viewPartMainLayoutParams;
    private int viewPartMainWidth;
    private int viewPartMainHeight;

    private RelativeLayout.LayoutParams viewPartSlaveLayoutParams;
    private int viewPartSlaveWidth;
    private int viewPartSlaveHeight;
    
    public MsgFlexboxLayout(Context context) {
        super(context);
    }

    public MsgFlexboxLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        
        a = context.obtainStyledAttributes(attrs, R.styleable.MsgFlexboxLayout, 0, 0);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        try {
            viewMain = (TextView) this.findViewById(a.getResourceId(R.styleable.MsgFlexboxLayout_viewMain, -1));
            viewSlave = this.findViewById(a.getResourceId(R.styleable.MsgFlexboxLayout_viewSlave, -1));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        if (viewMain == null || viewSlave == null || widthSize <= 0) {
            return;
        }

        float ds = viewMain.getLineHeight();
        int viewWidthMax = (int) ((viewMain.getMaxEms()) * ds);



        int availableWidth = Math.min(widthSize - getPaddingLeft() - getPaddingRight(), viewWidthMax);
        int availableHeight = heightSize - getPaddingTop() - getPaddingBottom();

        viewPartMainLayoutParams = (LayoutParams) viewMain.getLayoutParams();
        viewPartMainWidth = viewMain.getMeasuredWidth() + viewPartMainLayoutParams.leftMargin + viewPartMainLayoutParams.rightMargin;
        viewPartMainHeight = viewMain.getMeasuredHeight() + viewPartMainLayoutParams.topMargin + viewPartMainLayoutParams.bottomMargin;

        viewPartSlaveLayoutParams = (LayoutParams) viewSlave.getLayoutParams();
        viewPartSlaveWidth = viewSlave.getMeasuredWidth() + viewPartSlaveLayoutParams.leftMargin + viewPartSlaveLayoutParams.rightMargin;
        viewPartSlaveHeight = viewSlave.getMeasuredHeight() + viewPartSlaveLayoutParams.topMargin + viewPartSlaveLayoutParams.bottomMargin;

        int viewPartMainLineCount = viewMain.getLineCount();
        float viewPartMainLastLineWitdh = viewPartMainLineCount > 0 ? viewMain.getLayout().getLineWidth(viewPartMainLineCount - 1) : 0;



        widthSize = getPaddingLeft() + getPaddingRight();
        heightSize = getPaddingTop() + getPaddingBottom();

        if (viewPartMainLineCount > 1 && !(viewPartMainLastLineWitdh + viewPartSlaveWidth >= viewMain.getMeasuredWidth())) {
            widthSize += viewPartMainWidth;
            heightSize += viewPartMainHeight;
        } else if (viewPartMainLineCount > 1 && (viewPartMainLastLineWitdh + viewPartSlaveWidth >= availableWidth)) {
            widthSize += viewPartMainWidth;
            heightSize += viewPartMainHeight * 2;
        } else if (viewPartMainLineCount == 1 && (viewPartMainWidth + viewPartSlaveWidth >= availableWidth)) {
            if(viewPartMainWidth >= availableWidth - (ds/2)){
                widthSize += availableWidth;
            }else{
                widthSize += viewPartMainWidth;
            }

            heightSize += viewPartMainHeight * 2;
        }else if(viewPartMainLineCount == 1 && (viewPartMainWidth >= availableWidth - viewPartSlaveWidth - ds)){
            widthSize += availableWidth;
            heightSize += viewPartMainHeight;
        }else {
            widthSize += viewPartMainWidth + viewPartSlaveWidth;
            heightSize += viewPartMainHeight;
        }

        this.setMeasuredDimension(widthSize, heightSize);
        super.onMeasure(MeasureSpec.makeMeasureSpec(widthSize, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(heightSize, MeasureSpec.EXACTLY));
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        if (viewMain == null || viewSlave == null) {
            return;
        }

        viewMain.layout(
                getPaddingLeft(),
                getPaddingTop(),
                viewMain.getWidth() + getPaddingLeft(),
                viewMain.getHeight() + getPaddingTop());

        viewSlave.layout(
                right - left - viewPartSlaveWidth - getPaddingRight(),
                bottom - top - getPaddingBottom() - viewPartSlaveHeight,
                right - left - getPaddingRight(),
                bottom - top - getPaddingBottom());
    }
}

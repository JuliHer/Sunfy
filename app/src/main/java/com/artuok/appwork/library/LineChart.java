package com.artuok.appwork.library;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import androidx.annotation.Nullable;

import com.artuok.appwork.R;

import java.util.ArrayList;
import java.util.Collections;

public class LineChart extends View {

    private ArrayList<LineChartData> mData;

    //paints
    private Paint hintPaint;
    private TextPaint mTextTitlePaint;
    private TextPaint mTextHintPaint;

    //colors
    private int hintColor;
    private int mTextTitleColor;
    private int mTextHintColor;

    //sizes
    private int xSizes;
    private int ySizes;
    private int padding = 0;
    private int height = 0;
    private int width = 0;
    private int heightValues = 0;
    private int heightWorkZone = 0;

    public LineChart(Context context) {
        super(context);
        init(null);
    }

    public LineChart(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public LineChart(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    public LineChart(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(attrs);
    }

    private void init(@Nullable AttributeSet attrs) {
        hintPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mTextTitlePaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        mTextHintPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);

        if (attrs == null)
            return;

        TypedArray ta = getContext().obtainStyledAttributes(attrs, R.styleable.LineChart);
        hintColor = ta.getColor(R.styleable.LineChart_hintColors, Color.GRAY);
        mTextHintColor = ta.getColor(R.styleable.LineChart_textValuesColor, Color.GRAY);
        mTextTitleColor = ta.getColor(R.styleable.LineChart_textTitleColor, Color.GRAY);
        padding = ta.getDimensionPixelSize(R.styleable.LineChart_padding, 0);
        int textTitleSize = ta.getDimensionPixelSize(R.styleable.LineChart_textTitleSize, convertToSpToPx(16));
        int textValuesSize = ta.getDimensionPixelSize(R.styleable.LineChart_textValuesSize, convertToSpToPx(12));

        hintPaint.setColor(hintColor);
        mTextHintPaint.setColor(mTextHintColor);
        mTextHintPaint.setTextSize(textValuesSize);
        mTextTitlePaint.setColor(mTextTitleColor);
        mTextTitlePaint.setTextSize(textTitleSize);
        ta.recycle();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        drawing(canvas);
    }

    private void drawing(Canvas canvas) {
        setValues();

        if (mData == null) {
            drawNoDataSet(canvas);
        } else {
            heightValues = drawValues(canvas, mData);
            drawLines(canvas);
            drawEvents(canvas);
        }

    }

    private void drawNoDataSet(Canvas canvas) {
        int x = (width + padding) / 2;
        int y = (height + padding) / 2;

        Rect textBounds = new Rect();
        String txt = "No Data Available";
        mTextTitlePaint.getTextBounds(txt, 0, txt.length(), textBounds);
        y = (int) (y - textBounds.exactCenterY());
        x = (int) (x - textBounds.exactCenterX());

        canvas.drawText(txt, x, y, mTextTitlePaint);
    }

    private void setValues() {
        height = getHeight() - padding;
        width = getWidth() - padding;
        if (mData != null) {
            if (mData.size() > 0) {
                xSizes = ((width - padding)) / (getMaxX() - 1);
            }
        }

    }

    private void drawLines(Canvas canvas) {
        LineChartData d = getMaxLCDX();
        Rect txtBounds = new Rect();
        String text = d.getData().get(d.getData().size() - 1).getXS();
        mTextHintPaint.getTextBounds(text, 0, text.length(), txtBounds);

        int interpolate = ((width - padding) - txtBounds.width()) / (getMaxX() - 1);

        int heightMX = 0;
        for (int i = 0; i < getMaxX(); i++) {
            Rect textBounds = new Rect();
            String txt = d.getData().get(i).getXS();
            mTextHintPaint.getTextBounds(txt, 0, txt.length(), textBounds);

            heightMX = Math.max(heightMX, textBounds.height());
        }

        heightMX += convertToDpToPx(5);

        for (int i = 0; i < getMaxX(); i++) {
            int y = height - heightValues - convertToDpToPx(5);
            int x = padding + ((interpolate) * i);

            Rect textBounds = new Rect();
            String txt = d.getData().get(i).getXS();
            mTextHintPaint.getTextBounds(txt, 0, txt.length(), textBounds);

            y = (int) (y - textBounds.exactCenterY());

            canvas.drawText(txt, x, y, mTextHintPaint);

            int fx = padding + (xSizes * i);
            RectF rectF = new RectF(fx, padding, fx + convertToDpToPx(1), y - heightMX - convertToDpToPx(4));
            canvas.drawRect(rectF, hintPaint);
        }

        int posY = height - heightValues - heightMX - convertToDpToPx(5);
        RectF rectF = new RectF(padding, posY, width, posY + convertToDpToPx(2));
        canvas.drawRect(rectF, hintPaint);
        ySizes = interpolate;
        heightWorkZone = posY;
    }

    private void drawEvents(Canvas canvas) {
        LineChartData da = getMaxLCDX();
        int point = (heightWorkZone - padding) / getMaxY();


        Rect txtBounds = new Rect();
        String text = da.getData().get(da.getData().size() - 1).getXS();
        mTextHintPaint.getTextBounds(text, 0, text.length(), txtBounds);
        for (LineChartData data : mData) {
            int color = data.getColor();
            Paint bcolor = new Paint(Paint.ANTI_ALIAS_FLAG);
            bcolor.setColor(color);
            int pos = 0;
            Path a = new Path();
            Path bg = new Path();
            int lx = 0;
            int ly = 0;
            int startx = 0;
            int finalx = 0;
            for (LineChartDataSet d : data.getData()) {
                int x = padding + (xSizes * pos);
                int y = heightWorkZone - (point * d.getY());

                if (pos == 0) {
                    a.moveTo(x, y);
                    bg.moveTo(x, y);
                    startx = x;
                } else {
                    a.cubicTo(lx + ((x - lx) / 2), ly, lx + ((x - lx) / 2), y, x, y);
                    bg.cubicTo(lx + ((x - lx) / 2), ly, lx + ((x - lx) / 2), y, x, y);
                    finalx = x;
                }


                pos++;
                lx = x;
                ly = y;
            }

            bg.lineTo(finalx, heightWorkZone);
            bg.lineTo(startx, heightWorkZone);
            bg.close();
            canvas.save();

            Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            bgPaint.setColor(color);
            bgPaint.setAlpha(255 / 8);


            canvas.clipPath(bg);
            canvas.drawRect(0, 0, width, height, bgPaint);

            canvas.restore();


            Paint pincel = new Paint(Paint.ANTI_ALIAS_FLAG);
            pincel.setColor(color);
            pincel.setStrokeWidth(8);
            pincel.setStyle(Paint.Style.STROKE);
            canvas.drawPath(a, pincel);
            pos = 0;
            for (LineChartDataSet d : data.getData()) {
                int x = padding + (xSizes * pos);
                int y = heightWorkZone - (point * d.getY());

                canvas.drawCircle(x, y, convertToDpToPx(5), bcolor);
                pos++;
            }

        }
    }

    private int drawValues(Canvas canvas, ArrayList<LineChartData> data) {
        int x = padding;
        int y = height;

        int drawed = 0;

        int heightMx = 0;
        int heightMn = 1000;

        for (LineChartData d : data) {
            Rect textBounds = new Rect();
            String txt = d.getTitle();
            mTextTitlePaint.getTextBounds(txt, 0, txt.length(), textBounds);
            heightMx = Math.max(heightMx, textBounds.height());
            heightMn = Math.min(heightMx, textBounds.height());
        }

        for (LineChartData d : data) {
            Rect textBounds = new Rect();
            String txt = d.getTitle();
            mTextTitlePaint.getTextBounds(txt, 0, txt.length(), textBounds);


            int fx = x + (heightMx + convertToDpToPx(5));
            int fy = (y - ((heightMx + convertToDpToPx(5)) * drawed)) - convertToDpToPx(2);

            Paint circle = new Paint(Paint.ANTI_ALIAS_FLAG);
            circle.setColor(d.getColor());

            canvas.drawCircle(x + (heightMx / 2), (fy + textBounds.exactCenterY()), (heightMn / 2), circle);

            canvas.drawText(txt, fx, fy, mTextTitlePaint);
            drawed++;
        }

        int height = ((heightMx + convertToDpToPx(5)) * drawed) + (convertToDpToPx(2) * 2);

        return height;
    }

    private void drawMargins(Canvas canvas) {
        int pixelsMargin = convertToDpToPx(5);
        int stroke = convertToDpToPx(1);

        RectF lineX = new RectF();
        lineX.set(pixelsMargin, getHeight() - pixelsMargin - stroke, getWidth() - pixelsMargin, getHeight() - pixelsMargin);
        canvas.drawRect(lineX, hintPaint);
    }

    //Convert methods

    private int convertToDpToPx(int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getContext().getResources().getDisplayMetrics());
    }

    private int convertToSpToPx(int sp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, getContext().getResources().getDisplayMetrics());
    }

    private int getMaxX() {
        int maxX = 0;

        for (LineChartData d : mData) {
            maxX = Math.max(maxX, d.getMaxX());
        }

        return maxX;
    }

    private int getMaxY() {
        int maxY = 1;
        for (LineChartData d : mData) {
            maxY = Math.max(maxY, d.getMaxY());
        }

        return maxY;
    }

    private LineChartData getMaxLCDY() {
        LineChartData data = null;
        int maxY = 0;
        for (LineChartData d : mData) {
            if (maxY < d.getMaxY()) {
                data = d;
                maxY = d.getMaxY();
            }
        }

        return data;
    }

    private LineChartData getMaxLCDX() {
        LineChartData data = null;
        int maxX = 0;
        for (LineChartData d : mData) {
            if (maxX < d.getMaxX()) {
                data = d;
                maxX = d.getMaxX();
            }
        }

        return data;
    }

    //public methods
    public void setData(ArrayList<LineChartData> data) {
        this.mData = data;
        Collections.reverse(data);
        postInvalidate();
    }

    //public classs

    public static class LineChartData {
        private int color;
        private String title;
        private ArrayList<LineChartDataSet> data;
        private int maxX;
        private int maxY;

        public LineChartData(String title, ArrayList<LineChartDataSet> data, int color) {
            this.title = title;
            this.data = data;
            this.color = color;
            organized();
        }

        public ArrayList<LineChartDataSet> getData() {
            return data;
        }

        public int count() {
            return data.size();
        }

        public String getTitle() {
            return title;
        }

        public int getColor() {
            return color;
        }

        public void organized() {
            int maxX = data.size();
            int maxY = 0;
            for (LineChartDataSet d : data) {
                maxY = Math.max(d.getY(), maxY);
            }
            this.maxX = maxX;
            this.maxY = maxY;
        }

        public int getMaxX() {
            return maxX;
        }

        public int getMaxY() {
            return maxY;
        }
    }

    public static class LineChartDataSet {
        private boolean isText;
        private int xIntData;
        private int yIntData;
        private String xStringData;

        public LineChartDataSet(int x, int y) {
            this.xIntData = x;
            this.yIntData = y;
            isText = false;
        }

        public LineChartDataSet(String x, int y) {
            this.xStringData = x;
            this.yIntData = y;
            isText = true;
        }

        public void setX(String x) {
            this.xStringData = x;
            isText = true;
        }

        public void setX(int x) {
            this.xIntData = x;
            isText = false;
        }

        public int getX() {
            if (!isText) {
                return xIntData;
            } else {
                return -1;
            }
        }

        public String getXS() {
            if (isText) {
                return xStringData;
            } else {
                return "";
            }
        }

        public int getY() {
            return yIntData;
        }
    }
}

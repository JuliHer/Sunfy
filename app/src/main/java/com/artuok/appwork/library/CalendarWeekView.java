package com.artuok.appwork.library;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Scroller;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.artuok.appwork.R;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class CalendarWeekView extends View {

    //scroll values
    private int offsetY = 0;
    private int offsetX = 0;
    private ValueAnimator animator = ValueAnimator.ofFloat(0, 1);
    private Scroller mScroller = new Scroller(getContext());

    //Paints
    private Paint mPaintGrid;
    private Paint mPaintHour;
    private Paint mPaintDoW;
    private Paint mPaintHighlight;

    //Colors
    private int mColorGrid;
    private int mColorHour;
    private int mColorDay;
    private int mColorWeekendDay;
    private int mColorHighlightTxt;
    private int mColorHighlight;

    //Sizes
    private int mHalfSpacingOfDays = 0;
    private int mSpacingOfDays = 0;
    private int mSpacingOfHours = 0;
    private int mSpacingOfHalfHours = 0;
    private int mSpacingOfTopBar = 0;
    private int mTextSize = 0;
    private int mHeightMax = 0;
    private int mPadding = 0;

    //Calendar
    private Calendar calendar;

    //Events
    private boolean isSelect = false;
    private int sX = 0;
    private int sY = 0;

    private List<Cell> mData;

    //listeners
    OnSelectListener selectListener;


    public CalendarWeekView(Context context) {
        super(context);
        init(null);
    }

    public CalendarWeekView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public CalendarWeekView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    public CalendarWeekView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(attrs);
    }

    //init method
    private void init(@Nullable AttributeSet attr) {
        calendar = Calendar.getInstance();
        mPaintGrid = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaintHour = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaintDoW = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaintHighlight = new Paint(Paint.ANTI_ALIAS_FLAG);

        mData = new ArrayList<>();

        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(@NonNull ValueAnimator valueAnimator) {
                if (!mScroller.isFinished()) {
                    mScroller.computeScrollOffset();
                    scrollTo(0, mScroller.getCurrY());
                    postInvalidate();
                } else {
                    animator.cancel();
                }
            }
        });
        if (attr == null)
            return;

        //setAttrs
        TypedArray ta = getContext().obtainStyledAttributes(attr, R.styleable.WeekView);

        mColorGrid = ta.getColor(R.styleable.WeekView_gridColor, Color.GRAY);
        mColorHour = ta.getColor(R.styleable.WeekView_textHourColor, Color.GRAY);
        mColorDay = ta.getColor(R.styleable.WeekView_textDayColor, Color.GRAY);
        mColorWeekendDay = ta.getColor(R.styleable.WeekView_textDayWeekendColor, Color.GRAY);
        mTextSize = ta.getDimensionPixelSize(R.styleable.WeekView_textSize, convertToSpToPx(12));
        mColorHighlightTxt = ta.getColor(R.styleable.WeekView_textHighLightColor, Color.WHITE);
        mColorHighlight = ta.getColor(R.styleable.WeekView_highLightColor, Color.BLUE);
        mPadding = ta.getDimensionPixelOffset(R.styleable.WeekView_weekPaddingBottom, 0);

        mPaintHighlight.setColor(mColorHighlight);
        mPaintGrid.setColor(mColorGrid);
        mPaintHour.setColor(mColorHour);
        mPaintDoW.setColor(mColorDay);

        mPaintHour.setTextSize(mTextSize);
        mPaintHour.setTextAlign(Paint.Align.CENTER);
        mPaintDoW.setTextSize(mTextSize);
        mPaintDoW.setTextAlign(Paint.Align.CENTER);

        ta.recycle();
    }

    //Drawing methods
    @Override
    public void onDraw(Canvas canvas) {
        drawing(canvas, offsetY);
    }

    private void drawing(Canvas canvas, int y) {
        setSizeValues(canvas);
        y = offsetY;
        drawGrid(canvas, y);
        drawHours(canvas, y);
        if (isSelect) {
            drawSelect(canvas, y);
        }
        drawTopBar(canvas, y);

    }

    private void setSizeValues(Canvas canvas) {
        if (offsetY < 0) {
            offsetY = 0;
        }

        mHalfSpacingOfDays = getWidth() / 15;
        mSpacingOfDays = mHalfSpacingOfDays * 2;
        mSpacingOfHours = (int) (mSpacingOfDays * 1.75f);
        mSpacingOfHalfHours = mSpacingOfHours / 2;
        mSpacingOfTopBar = (int) (mSpacingOfHours * 0.75f);
        mHeightMax = mSpacingOfTopBar + (mSpacingOfHours * 24) + mPadding;

        if ((offsetY + getHeight()) > mHeightMax) {
            offsetY = mHeightMax - getHeight();
        }
        canvas.save();
        RectF rectF = new RectF();
        rectF.set(0, 0, getWidth(), mSpacingOfTopBar);
        canvas.clipOutRect(rectF);
    }

    private void drawGrid(Canvas canvas, int y) {
        for (int i = 0; i < 7; i++) {
            if (i > 0) {
                int spacingOfDays = mHalfSpacingOfDays + (mSpacingOfDays * i);
                int height = mSpacingOfTopBar + (mSpacingOfHours * 24);
                canvas.drawRect(spacingOfDays, mSpacingOfTopBar - y, spacingOfDays + 2, height - y, mPaintGrid);
            }
        }

        for (int i = 0; i < 47; i++) {
            int stroke = i % 2 == 1 ? 2 : 1;
            int spacingOfHours = mSpacingOfTopBar + mSpacingOfHalfHours + (mSpacingOfHalfHours * i);
            canvas.drawRect(mHalfSpacingOfDays, spacingOfHours - y, getWidth(), spacingOfHours + stroke - y, mPaintGrid);
        }
    }

    private void drawHours(Canvas canvas, int y) {
        for (int i = 0; i < 23; i++) {
            int hour = i + 1;
            String tm = hour >= 12 ? "pm" : "am";
            hour = hour > 12 ? hour - 12 : hour;

            int spacingOfHour = mSpacingOfTopBar + mSpacingOfHours + (mSpacingOfHours * i);
            canvas.drawText(hour + "", (mHalfSpacingOfDays / 2), spacingOfHour - y, mPaintHour);
            canvas.drawText(tm, (mHalfSpacingOfDays / 2), spacingOfHour - y + mTextSize, mPaintHour);
        }
    }

    private void drawTopBar(Canvas canvas, int y) {
        canvas.restore();
        int textDaySize = (int) (mTextSize * 0.66f);


        for (int i = 0; i < 7; i++) {
            mPaintDoW.setTextSize(textDaySize);
            if (i == 0 || i == 6) {
                mPaintDoW.setColor(mColorWeekendDay);
            }

            int spacingOfDaysTxt = mSpacingOfDays + (mSpacingOfDays * i);
            int spacingOfHeightDayTxt = mSpacingOfTopBar / 4;
            canvas.drawText(getMinDayOfWeek(i), spacingOfDaysTxt, spacingOfHeightDayTxt, mPaintDoW);

            if (calendar.get(Calendar.DAY_OF_WEEK) - 1 == i) {
                mPaintDoW.setColor(mColorHighlightTxt);

                canvas.drawCircle(spacingOfDaysTxt, (spacingOfHeightDayTxt * 3), spacingOfHeightDayTxt * 0.90f, mPaintHighlight);
            }
            String num = getDayOfWeek(i);
            mPaintDoW.setTextSize(mTextSize);
            int circleY = (int) ((mPaintDoW.descent() + mPaintDoW.ascent()) / 2);
            canvas.drawText(num, spacingOfDaysTxt, (spacingOfHeightDayTxt * 3) - circleY, mPaintDoW);

            mPaintDoW.setColor(mColorDay);
        }

        mPaintDoW.setTextSize(mTextSize);
    }

    private void drawSelect(Canvas canvas, int fy) {
        RectF rectF = new RectF();
        RectF rectM = new RectF();

        int padding = convertToDpToPx(2);
        float x = mHalfSpacingOfDays + (mSpacingOfDays * sX);
        float y = mSpacingOfTopBar + (mSpacingOfHalfHours * sY) - fy;
        float width = x + mSpacingOfDays - padding;
        float height = y + mSpacingOfHours - padding;

        if (sY == 47) {
            height = y + mSpacingOfHalfHours;
        }

        rectF.set(x, y, width, height);
        int paddingDp = convertToDpToPx(3);
        rectM.set(x + paddingDp, y + paddingDp, width - paddingDp, height - paddingDp);
        Paint translucent = mPaintHighlight;

        translucent.setAlpha(255 / 8);

        canvas.drawRoundRect(rectF, 15, 15, translucent);
        translucent.setAlpha(255);
        canvas.save();

        Path path = new Path();

        path.addRoundRect(rectM, 15, 15, Path.Direction.CCW);
        canvas.clipOutPath(path);
        canvas.drawRoundRect(rectF, 20, 20, mPaintHighlight);

        canvas.restore();
    }

    //scrolling methods

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return mGd.onTouchEvent(event);
    }

    GestureDetector mGd = new GestureDetector(getContext(), new GestureDetector.SimpleOnGestureListener() {
        @Override
        public boolean onScroll(@NonNull MotionEvent e1, @NonNull MotionEvent e2, float distanceX, float distanceY) {
            scrollBy((int) distanceX, (int) distanceY);
            return true;
        }

        @Override
        public boolean onFling(@NonNull MotionEvent e1, @NonNull MotionEvent e2, float velocityX, float velocityY) {
            mScroller.fling(offsetX, offsetY, -(int) velocityX, -(int) velocityY, 0, 0, -getHeight(), mHeightMax);

            animator.setDuration(mScroller.getDuration());
            animator.start();
            return true;
        }

        @Override
        public boolean onDown(@NonNull MotionEvent e) {
            if (!mScroller.isFinished()) { // is flinging
                mScroller.forceFinished(true); // to stop flinging on touch
            }
            return true;
        }

        @Override
        public boolean onSingleTapConfirmed(@NonNull MotionEvent e) {
            Cell a = getPositionAt((int) (e.getX() - mSpacingOfHalfHours), (int) (e.getY() - mSpacingOfTopBar) + offsetY);
            if (a != null) {

            } else {
                if (isSelect && isInSelect((int) (e.getX() - mSpacingOfHalfHours), (int) (e.getY() - mSpacingOfTopBar) + offsetY)) {
                    if (selectListener != null) {
                        selectListener.onClick(posToEvent(sX, sY));
                    }
                } else {
                    isSelect = !isSelect;
                    select((int) (e.getX() - mSpacingOfHalfHours), (int) (e.getY() - mSpacingOfTopBar) + offsetY);
                }
            }
            return true;
        }
    });

    @Override
    public void scrollBy(int x, int y) {
        offsetX += x;
        offsetY += y;
        postInvalidate();
    }

    @Override
    public void scrollTo(int x, int y) {
        offsetX = x;
        offsetY = y;
    }

    //Convert methods
    private int convertToDpToPx(int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getContext().getResources().getDisplayMetrics());
    }

    private int convertToSpToPx(int sp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, getContext().getResources().getDisplayMetrics());
    }

    private String getMinDayOfWeek(int dayOfWeek) {

        switch (dayOfWeek) {
            case 0:
                return getContext().getString(R.string.min_sunday);
            case 1:
                return getContext().getString(R.string.min_monday);
            case 2:
                return getContext().getString(R.string.min_tuesday);
            case 3:
                return getContext().getString(R.string.min_wednesday);
            case 4:
                return getContext().getString(R.string.min_thursday);
            case 5:
                return getContext().getString(R.string.min_friday);
            case 6:
                return getContext().getString(R.string.min_saturday);
        }

        return "";
    }

    private String getDayOfWeek(int dayOfWeek) {
        int DoW = calendar.get(Calendar.DAY_OF_WEEK) - 1;
        int MonthDays = getDaysInMoth(calendar.get(Calendar.MONTH), calendar.get(Calendar.YEAR));
        int DoM = calendar.get(Calendar.DAY_OF_MONTH);

        int diffDays = dayOfWeek - DoW;

        int dayOfMonth = DoM + diffDays;


        dayOfMonth = dayOfMonth < 1 ? getDaysInMoth(calendar.get(Calendar.MONTH) - 1, calendar.get(Calendar.YEAR)) + dayOfMonth : dayOfMonth % MonthDays;

        return dayOfMonth + "";
    }

    private int getDaysInMoth(int month, int yyyy) {
        month = month % 12;
        switch (month) {
            case 0:
            case 2:
            case 4:
            case 6:
            case 7:
            case 9:
            case 11:
                return 31;
            case 1:
                if ((yyyy % 4 == 0 && yyyy % 100 != 0) || (yyyy % 100 == 0 && yyyy % 400 == 0)) {
                    return 29;
                }
                return 28;
            case 3:
            case 5:
            case 8:
            case 10:
                return 30;
        }
        return 30;
    }

    private Cell getPositionAt(int x, int y) {
        Pos p = new Pos();
        p.getCell(x, y, mSpacingOfDays, mSpacingOfHalfHours);

        for (Cell c : mData) {
            if (p.getX() == c.getDay() && (p.getY() >= c.getHourMin() && p.getY() <= c.getHourMax())) {
                return c;
            }
        }

        return null;
    }

    private boolean isInSelect(int x, int y) {
        Pos p = new Pos();
        p.getCell(x, y, mSpacingOfDays, mSpacingOfHalfHours);

        if (sX == p.getX() && (sY == p.getY() || sY + 1 == p.getY())) {
            return true;
        }

        return false;
    }

    private void select(int x, int y) {
        Pos p = new Pos();
        p.getCell(x, y, mSpacingOfDays, mSpacingOfHalfHours);

        sX = p.getX();
        sY = p.getY();

        postInvalidate();
    }

    private EventsTask posToEvent(int x, int y) {
        long hour = 1800L * y;
        long duration = 3600;
        if (y == 47) {
            duration = 1800;
        }
        EventsTask e = new EventsTask(x, hour, duration, 0, "");

        return e;
    }

    //function methods


    //Class'

    private class Cell {
        private int hourMin;
        private int hourMax;
        private int day;
        private EventsTask event;

        public Cell(int hourMin, int hourMax, int day) {
            this.hourMin = hourMin;
            this.hourMax = hourMax;
            this.day = day;
        }

        public Cell(EventsTask event) {
            setEvent(event);
        }

        public Cell() {
            this.hourMin = 0;
            this.hourMax = 0;
            this.day = 0;
            this.event = null;
        }

        public void setEvent(EventsTask event) {
            long hour = event.getHour();
            long duration = event.getDuration();
            int start = (int) (hour / 1800);
            int end = (int) (hour + duration) / 1800;

            this.hourMin = start;
            this.hourMax = end;
            this.day = event.getDay();
            this.event = event;
        }

        public int getHourMin() {
            return hourMin;
        }

        public int getHourMax() {
            return hourMax;
        }

        public int getDay() {
            return day;
        }

        public EventsTask getEvent() {
            return event;
        }
    }

    private class Pos {
        private int x;
        private int y;

        public Pos() {
            this.x = 0;
            this.y = 0;
        }

        public void getCell(int x, int y, int cellSizeX, int cellSizeY) {
            this.x = x / cellSizeX;
            this.y = y / cellSizeY;
        }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }
    }

    private class Intersects {
        private int min = 0;
        private int max = 0;
        private int day = 0;
        private int count = 0;
        private List<EventsTask> events;

        public Intersects(List<EventsTask> events) {
            this.events = events;
            reorganization();
        }

        public Intersects() {
            this.min = 0;
            this.max = 0;
            this.count = 0;
            this.events = new ArrayList<>();
        }

        public void addEvent(EventsTask event) {
            this.events.add(event);
            reorganization();
        }

        private void reorganization() {
            count = events.size();

            day = events.get(0).getDay();
            events.sort((t, t1) -> (int) (t.getHour() - t1.getHour()));

            min = new Cell(events.get(0)).getHourMin();
            max = new Cell(events.get(count - 1)).getHourMax();
        }

        public boolean enterInIntersect(EventsTask event) {
            long duration = event.getHour() + event.getDuration();
            if (event.getDay() == day) {
                return (event.getHour() >= min && event.getHour() <= max) || (duration >= min && duration <= max);
            }

            return false;
        }

        public int getCount() {
            return count;
        }

        public EventsTask getEvent(int i) {
            return events.get(i);
        }
    }

    private class EventsTask {
        private int day;
        private long hour;
        private long duration;
        private int color;
        private String title;

        public EventsTask(int day, long hour, long duration, int color, String title) {
            this.day = day;
            this.hour = hour;
            this.duration = duration;
            this.color = color;
            this.title = title;
        }

        public int getDay() {
            return day;
        }

        public long getHour() {
            return hour;
        }

        public long getDuration() {
            return duration;
        }

        public int getColor() {
            return color;
        }

        public String getTitle() {
            return title;
        }
    }

    //interfaces

    public interface OnSelectListener {
        void onClick(EventsTask eventsTask);
    }
}

package com.artuok.appwork.library;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;
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
import java.util.Collections;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

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
    private TextPaint mPaintEvent;

    //Colors
    private int mColorGrid;
    private int mColorHour;
    private int mColorDay;
    private int mColorWeekendDay;
    private int mColorHighlightTxt;
    private int mColorHighlight;
    private int mColorTextEvent;

    //Sizes
    private int mHalfSpacingOfDays = 0;
    private int mSpacingOfDays = 0;
    private int mSpacingOfHours = 0;
    private int mSpacingOfHalfHours = 0;
    private int mSpacingOfTopBar = 0;
    private int mTextSize = 0;
    private int mHeightMax = 0;
    private int mPadding = 0;
    private int mTextEventSize = 0;

    //Calendar
    private Calendar calendar;
    private int mMinutes = 0;
    private Timer mTimer;

    //Events
    private boolean isSelect = false;
    private int sX = 0;
    private int sY = 0;

    private List<Cell> mData;
    private List<EventsTask> mTempData;
    private List<Intersects> mIntersects;

    private boolean isViewRegister = false;

    //listeners
    OnSelectListener selectListener;
    OnDateListener dateListener;
    OnViewRegister viewRegister;


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

    //init method
    private void init(@Nullable AttributeSet attr) {
        calendar = Calendar.getInstance();
        mTimer = new Timer();
        mTimer.scheduleAtFixedRate(new UpdateTimeTask(), 0, 2000);
        mPaintGrid = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaintHour = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaintDoW = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaintHighlight = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaintEvent = new TextPaint(Paint.ANTI_ALIAS_FLAG);

        mData = new ArrayList<>();

        animator.addUpdateListener(valueAnimator -> {
            if (!mScroller.isFinished()) {
                mScroller.computeScrollOffset();
                scrollTo(0, mScroller.getCurrY());
                postInvalidate();
            } else {
                animator.cancel();
            }
        });
        if (attr == null)
            return;

        //setAttrs
        TypedArray ta = getContext().obtainStyledAttributes(attr, R.styleable.CalendarWeekView);

        mColorGrid = ta.getColor(R.styleable.CalendarWeekView_gridColor, Color.GRAY);
        mColorHour = ta.getColor(R.styleable.CalendarWeekView_textHourColor, Color.GRAY);
        mColorDay = ta.getColor(R.styleable.CalendarWeekView_textDayColor, Color.GRAY);
        mColorWeekendDay = ta.getColor(R.styleable.CalendarWeekView_textDayWeekendColor, Color.GRAY);
        mTextSize = ta.getDimensionPixelSize(R.styleable.CalendarWeekView_textSize, convertToSpToPx(12));
        mColorHighlightTxt = ta.getColor(R.styleable.CalendarWeekView_textHighLightColor, Color.WHITE);
        mColorHighlight = ta.getColor(R.styleable.CalendarWeekView_highLightColor, Color.BLUE);
        mPadding = ta.getDimensionPixelOffset(R.styleable.CalendarWeekView_weekPaddingBottom, 0);
        mColorTextEvent = ta.getColor(R.styleable.CalendarWeekView_textColor, Color.WHITE);
        mTextEventSize = ta.getDimensionPixelSize(R.styleable.CalendarWeekView_textEventSize, convertToSpToPx(11));

        mPaintHighlight.setColor(mColorHighlight);
        mPaintGrid.setColor(mColorGrid);
        mPaintHour.setColor(mColorHour);
        mPaintDoW.setColor(mColorDay);

        mPaintHour.setTextSize(mTextSize);
        mPaintHour.setTextAlign(Paint.Align.CENTER);
        mPaintDoW.setTextSize(mTextSize);
        mPaintDoW.setTextAlign(Paint.Align.CENTER);

        mPaintEvent.setColor(mColorTextEvent);
        mPaintEvent.setTextSize(mTextEventSize);
        mPaintEvent.setFakeBoldText(true);

        ta.recycle();
    }

    //Drawing methods
    @Override
    public void onDraw(Canvas canvas) {
        drawing(canvas);
    }

    private void drawing(Canvas canvas) {
        setSizeValues(canvas);
        int y = offsetY;
        drawGrid(canvas, y);
        drawHours(canvas, y);
        onViewRegister();
        drawEvents(canvas, y);
        if (isSelect) {
            drawSelect(canvas, y);
        }
        drawIndicator(canvas, y);
        drawTopBar(canvas);
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

            int center = (int) (mHalfSpacingOfDays / 2);

            int spacingOfHour = mSpacingOfTopBar + mSpacingOfHours + (mSpacingOfHours * i);
            canvas.drawText(hour + "", center, spacingOfHour - y, mPaintHour);
            canvas.drawText(tm, center, spacingOfHour - y + mTextSize, mPaintHour);
        }
    }

    private void drawTopBar(@NonNull Canvas canvas) {
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

        canvas.drawRoundRect(rectF, 10, 10, translucent);
        translucent.setAlpha(255);
        canvas.save();

        Path path = new Path();

        path.addRoundRect(rectM, 10, 10, Path.Direction.CCW);
        canvas.clipOutPath(path);
        canvas.drawRoundRect(rectF, 15, 15, mPaintHighlight);

        canvas.restore();
    }

    private void drawEvents(Canvas canvas, int y) {
        for (Intersects i : mIntersects) {
            drawEvent(canvas, i, y);
        }
    }

    private void drawEvent(@NonNull Canvas canvas, @NonNull Intersects i, int y) {
        int count = i.getCount();
        int width = i.getWidth();
        int spacingOfEvent = mSpacingOfDays / width;
        float height = (mSpacingOfHours * 24);
        float day = 86400;

        List<Cell> draws = new ArrayList<>();

        for (int j = 0; j < count; j++) {
            RectF event = new RectF();
            EventsTask e = i.getEvent(j);
            float end = e.getDuration();
            float start = e.getHour();

            Cell c = new Cell(e);

            int lefts = getDrawsPos(draws, c);

            i.getEvent(j).setLeftPos(lefts);
            draws.add(c);

            int b = i.intersects(e) - width;

            int left = mHalfSpacingOfDays + (mSpacingOfDays * e.getDay()) + (spacingOfEvent * lefts);
            float top = (int) (mSpacingOfTopBar + (height / day * start) + 1) - y;
            int right = left + spacingOfEvent - convertToDpToPx(2);
            float bottom = (int) (top + (height / day * end) - convertToDpToPx(2));

            event.set(left, top, right, bottom);

            if (e.getColor() != 0)
                mPaintHighlight.setColor(e.getColor());

            canvas.drawRoundRect(event, 10, 10, mPaintHighlight);
            mPaintHighlight.setColor(mColorHighlight);

            if (width <= 2) {
                int padding = convertToDpToPx(3);
                canvas.save();
                canvas.clipRect(event);

                float heightE = bottom - top;

                Paint.FontMetrics fm = mPaintEvent.getFontMetrics();
                int txtHeight = (int) (fm.descent - fm.ascent);
                int maxLines = (int) (heightE / txtHeight);
                StaticLayout staticLayout = StaticLayout.Builder
                        .obtain(e.getTitle(), 0, e.getTitle().length(), mPaintEvent, mSpacingOfDays - (padding * 2))
                        .setMaxLines(maxLines)
                        .setEllipsize(TextUtils.TruncateAt.END)
                        .build();

                canvas.translate(left + padding, top + padding);
                staticLayout.draw(canvas);

                canvas.restore();
            }
        }
    }

    private int getDrawsPos(List<Cell> draws, Cell d) {
        int count = 0;
        for (int i = 0; i < draws.size(); i++) {
            Cell b = draws.get(i);
            for (int j = d.getHourMin(); j < (d.getHourMax() + 1); j++) {
                if (j >= b.getHourMin() && j <= b.getHourMax()) {
                    count++;
                    break;
                }
            }
        }
        return count;
    }


    private void drawIndicator(@NonNull Canvas canvas, int fy) {
        int h = calendar.get(Calendar.HOUR_OF_DAY);
        int m = calendar.get(Calendar.MINUTE);
        int d = calendar.get(Calendar.DAY_OF_WEEK) - 1;
        int spacing = mHalfSpacingOfDays + (mSpacingOfDays * d);

        float y = mSpacingOfTopBar + ((h * mSpacingOfHours) + ((1f / 60f * m) * mSpacingOfHours)) - fy;

        float x = mHalfSpacingOfDays;

        canvas.drawRect(x, y - 1, getWidth(), y + 1, mPaintHighlight);
        canvas.drawCircle(spacing, y, convertToDpToPx(5), mPaintHighlight);
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
            if (e.getX() > mHalfSpacingOfDays && e.getY() > mSpacingOfTopBar && (e.getY() + offsetY) < (mHeightMax - mPadding)) {
                Cell a = getPositionAt((int) (e.getX() - mHalfSpacingOfDays), (int) (e.getY() - mSpacingOfTopBar) + offsetY);
                if (a != null) {
                    if (dateListener != null) {
                        dateListener.onClick(a);
                    }
                } else {
                    if (isSelect && isInSelect((int) (e.getX() - mHalfSpacingOfDays), (int) (e.getY() - mSpacingOfTopBar) + offsetY)) {
                        if (selectListener != null) {
                            selectListener.onClick(posToEvent(sX, sY));
                        }
                    } else {
                        isSelect = !isSelect;
                        select((int) (e.getX() - mHalfSpacingOfDays), (int) (e.getY() - mSpacingOfTopBar) + offsetY);
                    }
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

    //function methods

    private Cell getPositionAt(int x, int y) {
        Pos p = new Pos();
        p.getCell(x, y, mSpacingOfDays, mSpacingOfHalfHours);
        for (Intersects i : mIntersects) {
            int spacingEvent = mSpacingOfDays / i.getWidth();
            if (i.getDay() == p.getX()) {
                for (int j = 0; j < i.getCount(); j++) {
                    EventsTask e = i.getEvent(j);
                    Cell c = new Cell(e);
                    if (p.getY() >= c.getHourMin() && p.getY() <= c.getHourMax()) {
                        int dx = x - (mSpacingOfDays * p.getX());
                        int de = dx / spacingEvent;
                        if (de == e.getLeftPos()) {
                            return c;
                        }
                    }
                }

            }
        }

        return null;
    }

    private boolean isInSelect(int x, int y) {
        Pos p = new Pos();
        p.getCell(x, y, mSpacingOfDays, mSpacingOfHalfHours);

        return sX == p.getX() && (sY == p.getY() || sY + 1 == p.getY());
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

        return new EventsTask(x, hour, duration, 0, "");
    }

    private void onViewRegister() {
        if (!isViewRegister) {

            List<Cell> c = new ArrayList<>();

            for (EventsTask e : mTempData) {
                Cell c1 = new Cell(e);

                c.add(c1);
            }

            mData = c;
            getIntersects();
            if (viewRegister != null) {
                viewRegister.onRegister();
            }
            isViewRegister = !isViewRegister;
        }
    }

    private void getIntersects() {
        mIntersects = new ArrayList<>();
        for (Cell c : mData) {
            EventsTask e = c.getEvent();
            if (mIntersects.size() == 0) {
                Intersects i = new Intersects();
                i.addEvent(e);
                mIntersects.add(i);
            } else {
                int count = mIntersects.size();
                boolean isIntersected = false;
                for (int i = 0; i < count; i++) {
                    Intersects b = mIntersects.get(i);
                    if (b.enterInIntersect(e)) {
                        mIntersects.get(i).addEvent(e);
                        isIntersected = true;
                        break;
                    }
                }
                if (!isIntersected) {
                    Intersects i = new Intersects();
                    i.addEvent(e);
                    mIntersects.add(i);
                }
            }
        }
    }

    private EventsTask getEventAt(int x, int y) {
        for (Intersects i : mIntersects) {
            int count = i.getCount();
            int width = i.getWidth();
            int spacingOfEvent = mSpacingOfDays / width;
            float height = (mSpacingOfHours * 24);
            float day = 86400;
            for (int j = 0; j < count; j++) {
                EventsTask e = i.getEvent(j);
                float end = e.getDuration();
                float start = e.getHour();
                int b = i.intersects(e) - width;

                int left = mHalfSpacingOfDays + (mSpacingOfDays * e.getDay()) + (spacingOfEvent * b);
                int top = (int) (mSpacingOfTopBar + (height / day * start) + 1) - offsetY;
                int right = left + spacingOfEvent - convertToDpToPx(2 / width);
                float bottom = (int) (top + (height / day * end) - convertToDpToPx(2));
                if ((x >= left && x <= right) && (y >= top && y <= bottom)) {
                    return e;
                }
            }
        }
        return null;
    }

    //public methods
    public void setEvents(List<EventsTask> events) {
        this.mTempData = events;
        if (mHalfSpacingOfDays != 0) {
            isViewRegister = false;
            onViewRegister();
        }
    }

    public void setSelectListener(OnSelectListener listener) {
        this.selectListener = listener;
    }

    public void setViewRegisterListener(OnViewRegister listener) {
        this.viewRegister = listener;
    }

    public void setDateListener(OnDateListener listener) {
        this.dateListener = listener;
    }

    public void scrollAt(long timeInMillis) {
        float height = (mSpacingOfHours * 24);
        float day = 86400;

        float pos = height / day * timeInMillis;

        offsetY = (int) pos - ((getHeight() - mPadding) / 2);
        postInvalidate();
    }

    //Classes
    public static class Cell {
        private int hourMin;
        private int hourMax;
        private int day;
        private EventsTask event;

        public Cell(EventsTask event) {
            setEvent(event);
        }

        public void setEvent(EventsTask event) {
            long hour = event.getHour();
            long duration = event.getDuration();
            int start = (int) (hour / 1800);
            int end = (int) (hour + duration - 1) / 1800;

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

    private static class Pos {
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

    private static class Intersects {
        private int min = 0;
        private int max = 0;
        private int widthMax = 0;
        private int day = 0;
        private int count = 0;
        private final List<EventsTask> events;


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

            events.sort((t, t1) -> (int) (t.getDuration() - t1.getDuration()));


            max = new Cell(events.get(count - 1)).getHourMax();

            Collections.reverse(events);

            int diff = max - min;
            int count = events.size();
            for (int i = 0; i < diff; i++) {
                int p = min + i;

                int curr = 0;
                for (int j = 0; j < count; j++) {
                    Cell c = new Cell(events.get(j));
                    if (p >= c.getHourMin() && p <= c.getHourMax()) {
                        curr++;
                    }
                }

                widthMax = Math.max(curr, widthMax);
            }
        }

        public boolean enterInIntersect(EventsTask event) {
            if (event.getDay() == day) {
                Cell c = new Cell(event);
                return (c.getHourMin() >= min && c.getHourMin() <= max) || (c.getHourMax() >= min && c.getHourMax() <= max);
            }

            return false;
        }

        public int intersects(EventsTask event) {
            Cell c = new Cell(event);
            int count = 0;
            for (EventsTask e : events) {
                Cell curr = new Cell(e);
                if ((c.getHourMin() >= curr.getHourMin() && c.getHourMin() <= curr.getHourMax()) || (c.getHourMax() >= curr.getHourMin() && c.getHourMax() <= curr.getHourMax())) {
                    count++;
                }
            }

            return count;
        }

        public int getCount() {
            return count;
        }

        public EventsTask getEvent(int i) {
            return events.get(i);
        }

        public int getWidth() {
            return widthMax;
        }

        public int getDay() {
            return day;
        }
    }

    public static class EventsTask {
        private int day;
        private long hour;
        private long duration;
        private int color;
        private final String title;
        private int id;
        private int leftPos;

        public EventsTask(int day, long hour, long duration, int color, String title) {
            this.day = day;
            this.hour = hour;
            this.duration = duration;
            this.color = color;
            this.title = title;
        }

        public EventsTask(int id, int day, long hour, long duration, int color, String title) {
            this.id = id;
            this.day = day;
            this.hour = hour;
            this.duration = duration;
            this.color = color;
            this.title = title;
        }

        public int getId() {
            return id;
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

        public void setColor(int color) {
            this.color = color;
        }

        public void setDay(int day) {
            this.day = day;
        }

        public void setHour(long hour) {
            this.hour = hour;
        }

        public void setDuration(long duration) {
            this.duration = duration;
        }

        public int getLeftPos() {
            return leftPos;
        }

        public void setLeftPos(int leftPos) {
            this.leftPos = leftPos;
        }
    }

    public class UpdateTimeTask extends TimerTask {

        @Override
        public void run() {
            calendar = Calendar.getInstance();
            if (mMinutes != calendar.get(Calendar.MINUTE)) {
                postInvalidate();
                mMinutes = calendar.get(Calendar.MINUTE);
            }
        }
    }

    //interfaces
    public interface OnSelectListener {
        void onClick(EventsTask eventsTask);
    }

    public interface OnDateListener {
        void onClick(Cell c);
    }

    public interface OnViewRegister {
        void onRegister();
    }
}

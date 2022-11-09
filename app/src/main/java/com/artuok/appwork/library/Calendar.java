package com.artuok.appwork.library;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.artuok.appwork.R;
import com.artuok.appwork.objects.TaskEvent;

import java.util.ArrayList;
import java.util.List;

public class Calendar extends View {

    //Paints
    private TextPaint mTextPaintDays;
    private TextPaint mTextPaintDaysInGrid;
    private TextPaint mTextPaintWeekendDays;
    private TextPaint mTextPaintWeekendDaysInGrid;
    private TextPaint mTextPaintToday;
    private TextPaint mTextPaintHighlight;
    private Paint mPaintHighlight;
    private TextPaint mTextPaintMonth;
    private TextPaint mTextPaintOutMonth;

    //Colors
    private int mColorDays;
    private int mColorDaysInGrid;
    private int mColorWeekendDays;
    private int mColorWeekendDaysInGrid;
    private int mColorToday;
    private int mColorTextHighlight;
    private int mColorHighlight;
    private int mColorMonth;
    private int mColorOutMonth;

    //Calendar
    private java.util.Calendar calendar;
    private java.util.Calendar calendarR;
    private int month = 0;
    private int monthTemp = 0;
    private int year = 0;

    //Sizes
    private int mTextSize = 0;
    private int mSpacingOfDays = 0;
    private int mHeightSpacingOfDays = 0;
    //swipe
    private int offsetX = 0;
    private int to = 0;
    private int df = 0;
    private ValueAnimator animator = ValueAnimator.ofFloat(0, 1);

    //click
    private OnDateClickListener clickListener;
    private int[] daySelected = new int[3];

    //Event Data
    private List<TaskEvent> mData;


    public Calendar(Context context) {
        super(context);
        init(null);
    }

    public Calendar(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public Calendar(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    //init method
    private void init(@Nullable AttributeSet attr) {
        calendar = java.util.Calendar.getInstance();
        calendarR = java.util.Calendar.getInstance();

        daySelected[0] = calendarR.get(java.util.Calendar.DAY_OF_MONTH) + 1;
        daySelected[1] = calendarR.get(java.util.Calendar.MONTH);
        daySelected[2] = calendarR.get(java.util.Calendar.YEAR);

        month = calendar.get(java.util.Calendar.MONTH);
        year = calendar.get(java.util.Calendar.YEAR);

        mData = new ArrayList<>();

        //setAttrs
        mTextPaintDays = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        mTextPaintDaysInGrid = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        mTextPaintWeekendDays = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        mTextPaintWeekendDaysInGrid = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        mTextPaintToday = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        mTextPaintHighlight = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        mPaintHighlight = new Paint(Paint.ANTI_ALIAS_FLAG);
        mTextPaintMonth = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        mTextPaintOutMonth = new TextPaint(Paint.ANTI_ALIAS_FLAG);

        animator.addUpdateListener(valueAnimator -> {
            float p = (float) valueAnimator.getAnimatedValue();
            offsetX += (int) ((to - offsetX) * p);
            if (offsetX == to) {
                offsetX = 0;
                month += monthTemp;
            }
            postInvalidate();
        });


        if (attr == null)
            return;

        TypedArray ta = getContext().obtainStyledAttributes(attr, R.styleable.Calendar);
        mColorDays = ta.getColor(R.styleable.Calendar_ColorDays, Color.GRAY);
        mColorDaysInGrid = ta.getColor(R.styleable.Calendar_ColorDaysInGrid, Color.GRAY);
        mColorWeekendDays = ta.getColor(R.styleable.Calendar_ColorWeekendDays, Color.GRAY);
        mColorWeekendDaysInGrid = ta.getColor(R.styleable.Calendar_ColorWeekendDaysInGrid, Color.GRAY);
        mColorToday = ta.getColor(R.styleable.Calendar_ColorToday, Color.BLUE);
        mColorTextHighlight = ta.getColor(R.styleable.Calendar_ColorTextHighlight, Color.WHITE);
        mColorHighlight = ta.getColor(R.styleable.Calendar_ColorHighlight, Color.BLUE);
        mColorMonth = ta.getColor(R.styleable.Calendar_ColorMonth, Color.BLACK);
        mColorOutMonth = ta.getColor(R.styleable.Calendar_ColorOutMonth, Color.GRAY);
        mTextSize = ta.getDimensionPixelSize(R.styleable.CalendarWeekView_textSize, convertToSpToPx(13));

        mTextPaintDays.setColor(mColorDays);
        mTextPaintDaysInGrid.setColor(mColorDaysInGrid);
        mTextPaintWeekendDays.setColor(mColorWeekendDays);
        mTextPaintWeekendDaysInGrid.setColor(mColorWeekendDaysInGrid);
        mTextPaintToday.setColor(mColorToday);
        mTextPaintHighlight.setColor(mColorTextHighlight);
        mPaintHighlight.setColor(mColorHighlight);
        mTextPaintMonth.setColor(mColorMonth);
        mTextPaintOutMonth.setColor(mColorOutMonth);

        mTextPaintDays.setTextSize(mTextSize);
        mTextPaintDaysInGrid.setTextSize(mTextSize);
        mTextPaintWeekendDays.setTextSize(mTextSize);
        mTextPaintWeekendDaysInGrid.setTextSize(mTextSize);
        mTextPaintToday.setTextSize(mTextSize);
        mTextPaintHighlight.setTextSize(mTextSize);
        mTextPaintMonth.setTextSize(mTextSize * 2);
        mTextPaintOutMonth.setTextSize(mTextSize);

        mTextPaintDays.setTextAlign(Paint.Align.CENTER);
        mTextPaintDaysInGrid.setTextAlign(Paint.Align.CENTER);
        mTextPaintWeekendDays.setTextAlign(Paint.Align.CENTER);
        mTextPaintWeekendDaysInGrid.setTextAlign(Paint.Align.CENTER);
        mTextPaintToday.setTextAlign(Paint.Align.CENTER);
        mTextPaintHighlight.setTextAlign(Paint.Align.CENTER);
        mTextPaintMonth.setFakeBoldText(true);
        mTextPaintOutMonth.setTextAlign(Paint.Align.CENTER);

        ta.recycle();
    }

    //draw methods

    @Override
    protected void onDraw(Canvas canvas) {
        drawing(canvas);
    }

    private void drawing(Canvas canvas) {

        setValues();

        int[] date = new int[2];

        for (int i = -1; i < 2; i++) {
            if (i == -1) {
                date[0] = (month - 1) < 0 ? 11 : (month - 1) % 12;
                date[1] = (month - 1) < 0 ? year - 1 : year;
            } else if (i == 0) {
                date[0] = month;
                date[1] = year;
            } else {
                date[0] = month + 1 > 11 ? 0 : month + 1;
                date[1] = month + 1 > 11 ? year + 1 : year;
            }
            drawMonths(canvas, date, getWidth() * i);
            drawWeekDays(canvas, getWidth() * i);
            drawDay(canvas, date, getWidth() * i);
        }
    }

    private void setValues() {
        mSpacingOfDays = getWidth() / 7;
        mHeightSpacingOfDays = (int) (mSpacingOfDays * 1f);
        year = calendar.get(java.util.Calendar.YEAR);
        year = month < 0 ? year - 1 + (month / 12) : year + (month / 12);

        month = month < 0 ? 11 : month % 12;
        calendar.set(java.util.Calendar.MONTH, month);
        calendar.set(java.util.Calendar.YEAR, year);
    }

    private void drawMonths(Canvas canvas, int[] date, int distance) {
        int month = date[0];
        int year = date[1];
        int x = (mSpacingOfDays / 2) + (offsetX + distance);
        int y = mSpacingOfDays / 2;

        Rect textBounds = new Rect();
        String txt = getMonth(month) + " " + year;
        mTextPaintMonth.getTextBounds(txt, 0, txt.length(), textBounds);
        y = (int) (y - textBounds.exactCenterY());

        mTextPaintDays.getTextBounds("d", 0, 1, textBounds);

        x = (int) (x - textBounds.exactCenterX());

        canvas.drawText(txt, x, y, mTextPaintMonth);
    }

    private void drawWeekDays(Canvas canvas, int min) {
        int y = (mSpacingOfDays / 2) + mSpacingOfDays;

        for (int i = 0; i < 7; i++) {
            int x = (mSpacingOfDays * i) + (mSpacingOfDays / 2) + offsetX + min;
            Rect textBounds = new Rect();
            String txt = getMinDayOfWeek(i);

            if (i == 0 || i == 6) {
                mTextPaintWeekendDays.getTextBounds(txt, 0, txt.length(), textBounds);
                int fy = (int) (y - textBounds.exactCenterY());
                canvas.drawText(txt, x, fy, mTextPaintWeekendDays);
            } else {
                mTextPaintDays.getTextBounds(txt, 0, txt.length(), textBounds);
                int fy = (int) (y - textBounds.exactCenterY());
                canvas.drawText(txt, x, fy, mTextPaintDays);
            }
        }
    }

    private void drawDay(Canvas canvas, int[] date, int distance) {
        boolean endOfMonth = false;
        int week = 0;

        int month = date[0];
        int year = date[1];
        int dayOfWeek = 0;
        while ((!endOfMonth || dayOfWeek != 6) && week < 6) {
            int dayOfCalendar = (7 * week);
            int spacingHeight = (mSpacingOfDays * 2) + (mSpacingOfDays / 4) + (mHeightSpacingOfDays * week);

            for (int i = 0; i < 7; i++) {
                int m = getDayOfMonth(dayOfCalendar + i, date);
                int spacingWidth = (mSpacingOfDays / 2) + (mSpacingOfDays * i) + offsetX + distance;
                Rect textBounds = new Rect();
                String txt = m + "";
                mTextPaintWeekendDaysInGrid.getTextBounds(txt, 0, txt.length(), textBounds);
                if (isInMonth(dayOfCalendar + i, date)) {
                    if (mData != null && mData.size() != 0) {
                        List<TaskEvent> events = new ArrayList<>();
                        for (TaskEvent e : mData) {
                            java.util.Calendar c = java.util.Calendar.getInstance();

                            c.setTimeInMillis(e.getTimeInMillis());

                            if (c.get(java.util.Calendar.DAY_OF_MONTH) + 1 == dayOfCalendar + i &&
                                    c.get(java.util.Calendar.MONTH) == month &&
                                    c.get(java.util.Calendar.YEAR) == year) {
                                events.add(e);
                            }
                        }

                        int count = Math.min(3, events.size());
                        if (count > 0) {
                            for (int j = 0; j < count; j++) {
                                int spacingOfTask = mSpacingOfDays / 2 / count;
                                int spacing = (spacingOfTask * count) + (spacingOfTask * j) - ((spacingOfTask / 2) * (count - 1));
                                int spacingx = spacingWidth - (mSpacingOfDays / 2) + spacing;
                                int height = (int) ((mSpacingOfDays * 2) + (mSpacingOfDays * 0.75f) + (mHeightSpacingOfDays * week));

                                mPaintHighlight.setColor(events.get(j).getColor());
                                canvas.drawCircle(spacingx, height, mSpacingOfDays / 27, mPaintHighlight);
                                mPaintHighlight.setColor(mColorHighlight);
                            }
                        }

                    }
                    if (daySelected[0] == dayOfCalendar + i &&
                            daySelected[1] == month &&
                            daySelected[2] == year) {
                        canvas.drawCircle(spacingWidth, spacingHeight, mSpacingOfDays * 0.25f, mPaintHighlight);
                        canvas.drawText(txt, spacingWidth, spacingHeight - textBounds.exactCenterY(), mTextPaintHighlight);
                    } else {
                        if (calendarR.get(java.util.Calendar.DAY_OF_MONTH) + 1 == dayOfCalendar + i &&
                                calendarR.get(java.util.Calendar.MONTH) == month &&
                                calendarR.get(java.util.Calendar.YEAR) == year) {
                            canvas.drawText(txt, spacingWidth, spacingHeight - textBounds.exactCenterY(), mTextPaintToday);
                        } else {
                            if (i == 0 || i == 6) {
                                canvas.drawText(txt, spacingWidth, spacingHeight - textBounds.exactCenterY(), mTextPaintWeekendDaysInGrid);
                            } else {
                                canvas.drawText(txt, spacingWidth, spacingHeight - textBounds.exactCenterY(), mTextPaintDaysInGrid);
                            }
                        }
                    }
                } else {
                    canvas.drawText(txt, spacingWidth, spacingHeight - textBounds.exactCenterY(), mTextPaintOutMonth);
                }

                if (m >= getDaysInMoth(month, year) && week != 0) {
                    endOfMonth = true;

                }

                dayOfWeek = i;
            }
            week++;
        }
    }

    //function methods
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

    private int getDayOfMonth(int dayOfMonth, int[] date) {
        java.util.Calendar b = java.util.Calendar.getInstance();
        b.set(java.util.Calendar.YEAR, date[1]);
        b.set(java.util.Calendar.MONTH, date[0]);
        b.set(java.util.Calendar.DAY_OF_MONTH, 1);

        int DoW = b.get(java.util.Calendar.DAY_OF_WEEK) - 1;
        int MonthDays = getDaysInMoth(date[0], date[1]);
        int DoM = -DoW;

        DoM = DoM + dayOfMonth;

        DoM = DoM < 0 ? getDaysInMoth(date[0] - 1, date[1]) + DoM : DoM % MonthDays;

        return DoM + 1;
    }

    private boolean isInMonth(int dayOfMonth, int[] date) {
        java.util.Calendar b = java.util.Calendar.getInstance();
        b.set(java.util.Calendar.YEAR, date[1]);
        b.set(java.util.Calendar.MONTH, date[0]);
        b.set(java.util.Calendar.DAY_OF_MONTH, 1);

        int DoW = b.get(java.util.Calendar.DAY_OF_WEEK) - 1;
        int MonthDays = getDaysInMoth(date[0], date[1]);

        int DoM = -DoW;

        DoM = DoM + dayOfMonth;

        return DoM >= 0 && DoM < MonthDays;
    }

    private String getDayOfWeek(int dayOfWeek) {
        int DoW = calendar.get(java.util.Calendar.DAY_OF_WEEK) - 1;
        int MonthDays = getDaysInMoth(calendar.get(java.util.Calendar.MONTH), calendar.get(java.util.Calendar.YEAR));
        int DoM = calendar.get(java.util.Calendar.DAY_OF_MONTH);

        int diffDays = dayOfWeek - DoW;

        int dayOfMonth = DoM + diffDays;


        dayOfMonth = dayOfMonth < 1 ? getDaysInMoth(calendar.get(java.util.Calendar.MONTH) - 1, calendar.get(java.util.Calendar.YEAR)) + dayOfMonth : dayOfMonth % MonthDays;

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

    private String getMonth(int month) {
        switch (month) {
            case 1:
                return getContext().getString(R.string.february);
            case 2:
                return getContext().getString(R.string.march);
            case 3:
                return getContext().getString(R.string.april);
            case 4:
                return getContext().getString(R.string.may);
            case 5:
                return getContext().getString(R.string.june);
            case 6:
                return getContext().getString(R.string.july);
            case 7:
                return getContext().getString(R.string.august);
            case 8:
                return getContext().getString(R.string.september);
            case 9:
                return getContext().getString(R.string.october);
            case 10:
                return getContext().getString(R.string.november);
            case 11:
                return getContext().getString(R.string.december);
            default:
                return getContext().getString(R.string.january);
        }
    }

    //Convert methods
    private int convertToDpToPx(int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getContext().getResources().getDisplayMetrics());
    }

    private int convertToSpToPx(int sp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, getContext().getResources().getDisplayMetrics());
    }

    //public methods

    public void addOnDateClickListener(OnDateClickListener listener) {
        this.clickListener = listener;
    }

    public void setEvents(List<TaskEvent> data) {
        this.mData = data;
        postInvalidate();
    }

    //swipe methods

    int x1 = 0;
    int y1 = 0;
    int swipe = 0;
    int dfY = 0;

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                x1 = (int) event.getX();
                y1 = (int) event.getY();
                break;

            case MotionEvent.ACTION_MOVE:
                df = (int) (event.getX() - x1);
                dfY = (int) (event.getY() - y1);
                scrollBy(df, dfY);
                if (Math.abs(df) > Math.abs(dfY)) {
                    if (df > 0 && offsetX > 0) {
                        swipe = -1;
                    } else if (df < 0 && offsetX < 0) {
                        swipe = 1;
                    }

                    if (df < 0 && offsetX > 0) {
                        swipe = 0;
                    } else if (df > 0 && offsetX < 0) {
                        swipe = 0;
                    }
                }
                x1 = (int) event.getX();
                y1 = (int) event.getY();

                break;
            case MotionEvent.ACTION_UP:
                if (swipe < 0) {
                    monthTemp = -1;
                    scrolling(getWidth());
                } else if (swipe > 0) {
                    monthTemp = 1;
                    scrolling(-getWidth());
                } else {
                    monthTemp = 0;
                    scrolling(0);
                }
                swipe = 0;
                postInvalidate();
                break;
        }

        return mGD.onTouchEvent(event);
    }

    GestureDetector mGD = new GestureDetector(getContext(), new GestureDetector.SimpleOnGestureListener() {
        @Override
        public boolean onDown(@NonNull MotionEvent e) {
            return true;
        }

        @Override
        public boolean onSingleTapConfirmed(@NonNull MotionEvent e) {
            if (e.getY() > mSpacingOfDays * 2 && e.getY() < getHeight()) {
                int x = (int) (e.getX() / mSpacingOfDays);
                int y = (int) ((e.getY() - (mSpacingOfDays * 2)) / mSpacingOfDays);

                java.util.Calendar c = java.util.Calendar.getInstance();

                c.set(java.util.Calendar.YEAR, year);
                c.set(java.util.Calendar.MONTH, month);
                c.set(java.util.Calendar.DAY_OF_MONTH, 1);

                int firstDayOfWeek = c.get(java.util.Calendar.DAY_OF_WEEK) - 2;
                int days = (y * 7) + x - firstDayOfWeek;

                daySelected[0] = days + firstDayOfWeek;
                daySelected[1] = month;
                daySelected[2] = year;

                int[] date = new int[2];

                date[0] = month;
                date[1] = year;
                if (isInMonth(daySelected[0], date)) {
                    if (clickListener != null) {
                        clickListener.onClick(days, month, year);
                    }
                }


                postInvalidate();
            }
            return true;
        }
    });

    @Override
    public void scrollBy(int x, int y) {
        offsetX += x;
        postInvalidate();
    }

    public void scrolling(int x) {
        int dur = 300 / getWidth() * Math.abs(offsetX);
        if (swipe != 0) {
            dur = 300 - (300 / getWidth() * Math.abs(offsetX));
        }

        to = x;
        animator.setDuration(dur);
        animator.start();
    }

    public void scrollTo(int x, int y) {
        offsetX = x;
    }

    //interface
    public interface OnDateClickListener {
        void onClick(int d, int m, int y);
    }
}

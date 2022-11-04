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

import androidx.annotation.Nullable;

import com.artuok.appwork.R;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class WeekView extends View {


    private int mSpacingOfEachLineX = 0;
    private int barDayHeight = 0;
    private int spacingOfDays = 0;
    private int spacingOfHour = 0;
    private Timer timer;
    private int seconds;

    private Paint mPaintLines, mPaintText, mPaintTextDoW, mPaintTxt, mBackgroundColorPaint;
    private int mTextSize;
    private int mTextEventSize;
    private Paint highLightColorPaint;
    private Calendar calendar;
    private OnDateListener dateListener;
    private OnSelectListener onSelectListener;

    private int gridColor;
    private int txtHourColor;
    private int txtColor;
    private int txtDayColor;
    private int txtDayWeekendColor;
    private int highLightColor;
    private int txtHighLightColor;
    private int backgroundColor;
    private List<Pos> mData;
    private Dictionary<Pos, EventsTasks> mEvents = new Hashtable<>();
    private ValueAnimator animator = ValueAnimator.ofFloat(0, 1);

    private int sX = 0;
    private int sY = 0;

    private boolean isSelected = false;

    private int mMaxHeight = 0;
    private Scroller mScroller = new Scroller(getContext());
    private int offsetX = 0, offsetY = 0;

    private int mPadding = 0;

    public WeekView(Context context) {
        super(context);
        init(null);
    }

    public WeekView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public WeekView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    public WeekView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(attrs);
    }

    private void init(@Nullable AttributeSet attr) {
        calendar = Calendar.getInstance();

        timer = new Timer();
        timer.scheduleAtFixedRate(new UpdateViewTask(), 0, 2000);

        mData = new ArrayList<>();
        mPaintLines = new Paint(Paint.ANTI_ALIAS_FLAG);

        mPaintText = new Paint(Paint.ANTI_ALIAS_FLAG);

        mBackgroundColorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        mPaintText.setTextAlign(Paint.Align.CENTER);

        mPaintTxt = new Paint(Paint.ANTI_ALIAS_FLAG);

        mPaintTextDoW = new Paint(Paint.ANTI_ALIAS_FLAG);

        mPaintTextDoW.setTextAlign(Paint.Align.CENTER);
        mPaintTextDoW.setFakeBoldText(true);

        highLightColorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);


        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
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

        TypedArray ta = getContext().obtainStyledAttributes(attr, R.styleable.WeekView);

        mTextSize = ta.getDimensionPixelSize(R.styleable.WeekView_textSize, convertToSpToPx(12));
        mTextEventSize = ta.getDimensionPixelSize(R.styleable.WeekView_textEventSize, convertToSpToPx(11));
        mPadding = ta.getDimensionPixelOffset(R.styleable.WeekView_weekPaddingBottom, 0);
        txtHourColor = ta.getColor(R.styleable.WeekView_textHourColor, Color.GRAY);
        txtDayColor = ta.getColor(R.styleable.WeekView_textDayColor, Color.GRAY);
        txtDayWeekendColor = ta.getColor(R.styleable.WeekView_textDayWeekendColor, Color.GRAY);
        txtHighLightColor = ta.getColor(R.styleable.WeekView_textHighLightColor, Color.WHITE);
        highLightColor = ta.getColor(R.styleable.WeekView_highLightColor, Color.BLUE);
        gridColor = ta.getColor(R.styleable.WeekView_gridColor, Color.GRAY);
        txtColor = ta.getColor(R.styleable.WeekView_textColor, Color.WHITE);
        backgroundColor = ta.getColor(R.styleable.WeekView_backgroundColor, Color.WHITE);

        mPaintLines.setColor(gridColor);
        highLightColorPaint.setColor(highLightColor);
        mBackgroundColorPaint.setColor(backgroundColor);


        mPaintText.setColor(txtHourColor);
        mPaintText.setTextSize(mTextSize);
        mPaintTxt.setColor(txtColor);
        mPaintTxt.setTextSize(mTextEventSize);
        mPaintTxt.setFakeBoldText(true);

        mPaintTextDoW.setColor(txtDayColor);
        mPaintTextDoW.setTextSize(mTextSize);

        ta.recycle();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        drawing(canvas, offsetX, offsetY);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        return mGd.onTouchEvent(event);
    }

    GestureDetector mGd = new GestureDetector(getContext(), new GestureDetector.SimpleOnGestureListener() {
        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            scrollBy((int) distanceX, (int) distanceY);
            return true;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {

            mScroller.fling(offsetX, offsetY,
                    -(int) velocityX, -(int) velocityY, 0, 0, 0 - getHeight(), mMaxHeight);

            animator.setDuration(mScroller.getDuration());
            animator.start();
            return true;
        }

        @Override
        public boolean onDown(MotionEvent e) {
            if (!mScroller.isFinished()) { // is flinging
                mScroller.forceFinished(true); // to stop flinging on touch
            }
            return true;
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            EventsTasks a = getEventAt((int) e.getX(), (int) e.getY());
            if (a != null) {
                if (dateListener != null) {
                    dateListener.onClick(a);
                }
            } else {
                if (isSelected && isInSelected((int) e.getX(), (int) e.getY())) {
                    if (onSelectListener != null) {
                        long duration = 60 * 60;
                        onSelectListener.onClick(sX, sY, duration);
                    }
                } else {
                    isSelected = !isSelected;
                    getDayAndHourAt((int) e.getX(), (int) e.getY());
                }
            }
            return true;
        }
    });

    @Override
    public void scrollBy(int x, int y) {
        offsetY += y;
        offsetX += x;
        postInvalidate();
        //super.scrollBy(x, y);
    }

    @Override
    public void scrollTo(int x, int y) {
        offsetY = y;
        offsetX = x;
    }

    public void getDayAndHourAt(int x, int y) {
        if (x < mSpacingOfEachLineX) {
            return;
        }

        if (y < barDayHeight) {
            return;
        }
        int daySpacing = spacingOfDays;
        int halfHourSpacing = (int) (spacingOfHour) / 2;

        sY = ((y + offsetY) - barDayHeight) / halfHourSpacing;
        sX = (x - mSpacingOfEachLineX) / daySpacing;

        postInvalidate();
    }

    public boolean isInSelected(int x, int y) {
        if (x < mSpacingOfEachLineX) {
            return false;
        }

        if (y < barDayHeight) {
            return false;
        }

        int daySpacing = spacingOfDays;
        int halfHourSpacing = (int) (spacingOfHour) / 2;

        int halfHourPosition = ((y + offsetY) - barDayHeight) / halfHourSpacing;
        int dayPosition = (x - mSpacingOfEachLineX) / daySpacing;

        if (dayPosition == sX) {
            return halfHourPosition == sY || halfHourPosition == sY + 1;
        }
        return false;
    }

    private void drawSelected(Canvas canvas, int d, int h, int fy) {
        RectF rectF = new RectF();
        RectF rectM = new RectF();

        float heightOfHour = (spacingOfHour) / 2;
        float x = mSpacingOfEachLineX + (spacingOfDays * d);
        float y = barDayHeight + (heightOfHour * h) - fy;
        float width = x + spacingOfDays;
        float height = y + (heightOfHour * 2);

        rectF.set(x, y, width, height);
        int paddingDp = convertToDpToPx(3);
        rectM.set(x + paddingDp, y + paddingDp, width - paddingDp, height - paddingDp);

        Paint translucent = highLightColorPaint;

        translucent.setAlpha(255 / 8);

        canvas.drawRoundRect(rectF, 15, 15, translucent);
        translucent.setAlpha(255);
        canvas.save();

        Path path = new Path();

        path.addRoundRect(rectM, 15, 15, Path.Direction.CCW);
        canvas.clipOutPath(path);
        canvas.drawRoundRect(rectF, 20, 20, highLightColorPaint);


        canvas.restore();


    }

    void drawing(Canvas canvas, int x, int y) {

        if (y < 0) {
            y = 0;
            offsetY = y;
        }

        canvas.save();
        canvas.clipOutRect(0, 0, getWidth(), barDayHeight);
        mSpacingOfEachLineX = getWidth() / 15;
        spacingOfDays = mSpacingOfEachLineX * 2;


        int pos = 0;
        spacingOfHour = (int) (spacingOfDays * 1.75f);
        mMaxHeight = (barDayHeight + (spacingOfHour * 24) + mPadding);
        barDayHeight = spacingOfHour / 3 * 2;

        if ((y + getHeight()) > barDayHeight + (spacingOfHour * 24) + mPadding) {
            y = (barDayHeight + (spacingOfHour * 24) + mPadding) - getHeight();
            offsetY = y;
        }

        for (int i = 0; i < 15; i++) {
            if (i == 3) {
                canvas.drawRect((mSpacingOfEachLineX * i), barDayHeight - y, ((mSpacingOfEachLineX * i) + 2), barDayHeight + (spacingOfHour * 24) - y, mPaintLines);
                pos += mSpacingOfEachLineX * i;

            }
            if (i > 3 && i < 9) {
                canvas.drawRect((pos + spacingOfDays), barDayHeight - y, ((pos + spacingOfDays) + 2), barDayHeight + (spacingOfHour * 24) - y, mPaintLines);
                pos += spacingOfDays;
            }
        }

        for (int i = 0; i < 24; i++) {
            canvas.drawRect(mSpacingOfEachLineX, barDayHeight + ((spacingOfHour * i) + (spacingOfHour / 2)) - y, getWidth(), barDayHeight + ((spacingOfHour * i) + (spacingOfHour / 2) + 1) - y, mPaintLines);
            if (i > 0) {
                canvas.drawRect(mSpacingOfEachLineX, barDayHeight + (spacingOfHour * i) - y, getWidth(), barDayHeight + ((spacingOfHour * i) + 2) - y, mPaintLines);


                int hour = i;
                String tv = "am";
                if (hour >= 12) {
                    if (hour > 12) {
                        hour = hour - 12;
                    }

                    tv = "pm";
                }

                canvas.drawText(hour + "", mSpacingOfEachLineX / 2, (barDayHeight + (spacingOfHour * i) - y), mPaintText);
                canvas.drawText(tv, mSpacingOfEachLineX / 2, (barDayHeight + (spacingOfHour * i) - y) + mTextSize, mPaintText);
            }
        }
        /*EventsTasks e = new EventsTasks("Homewor asdsad asd asd asda sdas d asd ask", 1, 46800, 3600, Color.GRAY);
        drawEvents(canvas, e, offsetY);*/
        drawEvents(canvas, mData);
        updateHour(canvas);
        if (isSelected) {
            drawSelected(canvas, sX, sY, offsetY);
        }
        drawTop(canvas);
    }

    private void drawTop(Canvas canvas) {


        canvas.restore();
        Paint curr = mPaintTextDoW;
        int cy = (int) (((barDayHeight / 2)) - ((mPaintTextDoW.descent() + mPaintTextDoW.ascent()) / 2));

        for (int i = 0; i < 7; i++) {


            if ((calendar.get(Calendar.DAY_OF_WEEK) - 1) == i) {
                curr.setColor(txtHighLightColor);
                canvas.drawCircle((spacingOfDays * i) + spacingOfDays, (barDayHeight / 2), (barDayHeight / 2 * 0.75f), highLightColorPaint);
            } else {
                if (i == 0 || i == 6) {
                    curr.setColor(txtDayWeekendColor);
                } else {
                    curr.setColor(txtDayColor);
                }
            }


            canvas.drawText(getMinDayOfWeek(i), (spacingOfDays * i) + spacingOfDays, cy, curr);
        }
    }

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

    private void drawEvents(Canvas canvas, List<Pos> data) {
        for (Pos e : data) {
            drawEvent(canvas, e, offsetY);
        }
    }

    private void drawEvent(Canvas canvas, Pos p, int y) {
        RectF event = new RectF();

        EventsTasks a = mEvents.get(p);

        int left = p.getX();
        float top = p.getY();
        int right = p.getWidth();
        float bottom = p.getHeight();
        event.set(left, top, right, bottom);

        float heightE = bottom - top;

        int padding = convertToDpToPx(3);

        int cy = (int) (top + padding);
        canvas.drawRoundRect(event, 15, 15, highLightColorPaint);
        canvas.save();
        canvas.clipRect(event);
        //canvas.drawText(task.getTitle(), left + padding, cy + (padding*2), mPaintTxt);
        TextPaint paint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        Paint.FontMetrics fm = mPaintTxt.getFontMetrics();
        int txtHeight = (int) (fm.descent - fm.ascent);

        int maxLines = (int) (heightE / txtHeight);

        paint.setColor(txtColor);
        paint.setTextSize(mTextEventSize);
        paint.setFakeBoldText(true);
        StaticLayout staticLayout = StaticLayout.Builder
                .obtain(a.getTitle(), 0, a.getTitle().length(), paint, spacingOfDays - (padding * 2))
                .setMaxLines(maxLines)
                .setEllipsize(TextUtils.TruncateAt.END)
                .build();


        canvas.translate(left + padding, cy);
        staticLayout.draw(canvas);

        canvas.restore();
    }

    public Pos getPositionAt(int d, long h, long dur) {

        float height = (spacingOfHour * 24);
        float day = 86400;
        float hour = h;
        float duration = dur;

        int left = (spacingOfDays * d) + mSpacingOfEachLineX + 1;
        float top = barDayHeight + (height / day * hour) + 1;
        int right = left + spacingOfDays;
        float bottom = top + (height / day * duration);

        Pos p = new Pos(left, (int) top, right, (int) bottom);

        return p;
    }

    public EventsTasks getEventAt(int x, long y) {
        EventsTasks r = null;
        for (int i = 0; i < mData.size(); i++) {
            Pos e = mData.get(i);

            if (x > e.getX() && x < e.getHeight()) {
                if (y > e.getY() && y < e.height) {
                    r = mEvents.get(e);
                    break;
                }
            }
        }

        return r;
    }


    static public class EventsTasks {
        private int day;
        private long hour;
        private long duration;
        private int color;
        private String title;

        public EventsTasks(String title, int day, long hour, long duration, int color) {
            this.title = title;
            this.day = day;
            this.hour = hour;
            this.duration = duration;
            this.color = color;
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

        public String getTitle() {
            return title;
        }

        public int getColor() {
            return color;
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

        public void setColor(int color) {
            this.color = color;
        }

        public void setTitle(String title) {
            this.title = title;
        }
    }

    public class Pos {
        int x;
        int y;
        int width;
        int height;

        public Pos(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }

        public int getWidth() {
            return width;
        }

        public int getHeight() {
            return height;
        }

        public void setX(int x) {
            this.x = x;
        }

        public void setY(int y) {
            this.y = y;
        }

        public void setWidth(int width) {
            this.width = width;
        }

        public void setHeight(int height) {
            this.height = height;
        }

        public boolean isInPos(int x, int y) {
            if (x > this.x && x < this.width) {
                if (y > this.y && y < this.height) {
                    return true;
                }
            }
            return false;
        }
    }

    public void show() {
        postInvalidate();
    }

    private class UpdateViewTask extends TimerTask {

        @Override
        public void run() {
            calendar = Calendar.getInstance();
            if (seconds != calendar.get(Calendar.MINUTE)) {
                postInvalidate();
                seconds = calendar.get(Calendar.MINUTE);
            }
        }
    }

    public void setEvents(List<EventsTasks> es) {
        List<Pos> p = new ArrayList<>();
        mEvents = new Hashtable<>();
        for (EventsTasks e : es) {
            p.add(getPositionAt(e.getDay(), e.getHour(), e.getDuration()));

            mEvents.put(p.get(p.size() - 1), e);
        }

        this.mData = p;
    }

    public void addEvents(List<EventsTasks> es) {
        List<Pos> p = new ArrayList<>();

        for (EventsTasks e : es) {
            p.add(getPositionAt(e.getDay(), e.getHour(), e.getDuration()));

            mEvents.put(p.get(p.size() - 1), e);
        }

        this.mData.addAll(p);
    }

    public void addEvent(EventsTasks e) {
        Pos p = getPositionAt(e.getDay(), e.getHour(), e.getDuration());
        this.mData.add(p);

        mEvents.put(p, e);
    }

    public void setDateListener(OnDateListener dateListener) {
        this.dateListener = dateListener;
    }

    public void setSelectListener(OnSelectListener onSelectListener) {
        this.onSelectListener = onSelectListener;
    }

    public interface OnDateListener {
        void onClick(EventsTasks e);
    }

    public interface OnSelectListener {
        void onClick(int d, long h, long duration);
    }

    private void updateHour(Canvas canvas) {
        int h = calendar.get(Calendar.HOUR_OF_DAY);
        int m = calendar.get(Calendar.MINUTE);
        int d = calendar.get(Calendar.DAY_OF_WEEK) - 1;
        int spacing = mSpacingOfEachLineX + (spacingOfDays * d);

        float y = barDayHeight + ((h * spacingOfHour) + ((1f / 60f * m) * spacingOfHour)) - offsetY;

        float x = mSpacingOfEachLineX;

        canvas.drawRect(x, y - 1, getWidth(), y + 1, highLightColorPaint);
        canvas.drawCircle(spacing, y, convertToDpToPx(5), highLightColorPaint);
    }
}

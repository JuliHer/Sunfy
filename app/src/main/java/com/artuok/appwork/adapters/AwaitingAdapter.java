package com.artuok.appwork.adapters;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.artuok.appwork.R;
import com.artuok.appwork.objects.AwaitingElement;
import com.artuok.appwork.objects.Item;
import com.artuok.appwork.objects.StatisticsElement;
import com.artuok.appwork.objects.TextElement;
import com.thekhaeng.pushdownanim.PushDownAnim;

import java.util.Calendar;
import java.util.List;

public class AwaitingAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    List<Item> mData;
    LayoutInflater mInflater;
    OnClickListener listener;

    public AwaitingAdapter(Context context, List<Item> mData) {
        this.mData = mData;
        this.mInflater = LayoutInflater.from(context);

    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == 0) {
            View view = mInflater.inflate(R.layout.item_awaiting_layout, parent, false);

            return new AwaitingViewHolder(view);
        } else if (viewType == 1) {
            View view = mInflater.inflate(R.layout.recurrence_layout, parent, false);
            return new StatisticViewHolder(view);
        } else if (viewType == 2) {
            View view = mInflater.inflate(R.layout.item_text_layout, parent, false);
            return new TextViewHolder(view);
        }
        return null;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        int type = getItemViewType(position);
        if (type == 0) {
            AwaitingElement element = (AwaitingElement) mData.get(position).getObject();
            ((AwaitingViewHolder) holder).onBindData(element);
        } else if (type == 1) {
            StatisticsElement element = (StatisticsElement) mData.get(position).getObject();
            ((StatisticViewHolder) holder).onBindData(element);
        } else if (type == 2) {
            TextElement element = (TextElement) mData.get(position).getObject();
            ((TextViewHolder) holder).onBindData(element);
        }
    }

    @Override
    public int getItemCount() {
        return mData.size();
    }

    public void setOnClickListener(OnClickListener listener) {
        this.listener = listener;
    }

    @Override
    public int getItemViewType(int position) {
        return mData.get(position).getType();
    }

    class TextViewHolder extends RecyclerView.ViewHolder {

        TextView textView;

        public TextViewHolder(@NonNull View itemView) {
            super(itemView);

            textView = itemView.findViewById(R.id.text);
        }

        public void onBindData(TextElement element) {
            textView.setText(element.getText());

        }
    }

    class AwaitingViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        TextView title, status, date, time;

        LinearLayout subject;

        public AwaitingViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.title_card);
            subject = itemView.findViewById(R.id.subject_color);
            status = itemView.findViewById(R.id.status_card);
            date = itemView.findViewById(R.id.date_card);
            time = itemView.findViewById(R.id.time);
            PushDownAnim.setPushDownAnimTo(itemView)
                    .setDurationPush(100)
                    .setScale(PushDownAnim.MODE_SCALE, 0.98f)
                    .setOnClickListener(this);
        }

        void onBindData(AwaitingElement element) {
            String t = element.getTitle();
            t = t.substring(0, 1).toUpperCase() + t.substring(1).toLowerCase();
            title.setText(t);
            title.setPaintFlags(title.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
            subject.setBackgroundColor(element.getColorSubject());

            String d = "";

            Calendar c = Calendar.getInstance();
            c.setTimeInMillis(element.getStatus());

            Calendar today = Calendar.getInstance();

            long tod = today.getTimeInMillis();

            long rest = (element.getStatus() - tod) / 86400000;

            if(tod < element.getStatus()){
                int toin = today.get(Calendar.DAY_OF_YEAR);
                int awin = c.get(Calendar.DAY_OF_YEAR);
                if(awin < toin){
                    rest = awin + 364 - toin;
                }else{
                    rest = awin - toin;
                }
            }

            int dow = c.get(Calendar.DAY_OF_WEEK);



            if (element.isOpen()) {
                if (rest == 1) {
                    d = mInflater.getContext().getString(R.string.tomorrow);
                } else if (rest == 0) {
                    d = mInflater.getContext().getString(R.string.today);
                }
                else if(rest < 7){
                    if(dow == 1){
                        d = mInflater.getContext().getString(R.string.sunday);
                    }
                    else if(dow == 2){
                        d = mInflater.getContext().getString(R.string.monday);
                    }
                    else if(dow == 3){
                        d = mInflater.getContext().getString(R.string.tuesday);
                    }
                    else if(dow == 4){
                        d = mInflater.getContext().getString(R.string.wednesday);
                    }
                    else if(dow == 5){
                        d = mInflater.getContext().getString(R.string.thursday);
                    }
                    else if(dow == 6){
                        d = mInflater.getContext().getString(R.string.friday);
                    }
                    else if(dow == 7){
                        d = mInflater.getContext().getString(R.string.saturday);
                    }
                }else{
                    d = rest+" "+mInflater.getContext().getString(R.string.day_left);
                }
            }
            status.setText(d);
            date.setText(element.getDate());
            time.setText(element.getTime());
            TypedArray ta = mInflater.getContext().obtainStyledAttributes(R.styleable.AppCustomAttrs);

            int color = ta.getColor(R.styleable.AppCustomAttrs_backgroundBorder, Color.WHITE);


            title.setTextColor(color);

            TypedArray a = mInflater.getContext().obtainStyledAttributes(R.styleable.AppCustomAttrs);
            int colorB = a.getColor(R.styleable.AppCustomAttrs_subTextColor, mInflater.getContext().getColor(R.color.yellow_700));
            int colorT = a.getColor(R.styleable.AppCustomAttrs_backgroundDialog, Color.WHITE);
            a.recycle();

            if (element.isDone()) {
                status.setText(R.string.done_string);
                status.setTextColor(colorT);
                status.setBackgroundColor(colorB);

                title.setPaintFlags(title.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);

                color = ta.getColor(R.styleable.AppCustomAttrs_subTextColor, Color.WHITE);


                title.setTextColor(color);
            } else {
                status.setTextColor(colorT);
                if (element.isOpen()) {
                    if (rest > 2) {
                        status.setBackgroundColor(mInflater.getContext().getColor(R.color.green_500));
                    } else {
                        status.setBackgroundColor(mInflater.getContext().getColor(R.color.yellow_700));
                    }
                    status.setText(d);
                } else {
                    status.setText(R.string.task_is_close);
                    status.setBackgroundColor(colorB);
                }
            }

            ta.recycle();
        }

        @Override
        public void onClick(View view) {
            listener.onClick(view, getLayoutPosition());
        }
    }

    class StatisticViewHolder extends RecyclerView.ViewHolder {

        TextView done, onHold, losed;

        public StatisticViewHolder(@NonNull View itemView) {
            super(itemView);
            done = itemView.findViewById(R.id.done_txt);
            onHold = itemView.findViewById(R.id.onHold_txt);
            losed = itemView.findViewById(R.id.losed_txt);
        }

        void onBindData(StatisticsElement element) {
            String hold = mInflater.getContext().getString(R.string.on_hold_string);
            String d = "" + element.getDone();
            done.setText(d);
            String o = hold + ": " + element.getOnHold();
            onHold.setText(o);
            String l = "" + element.getLosed();
            losed.setText(l);
        }
    }

    public interface OnClickListener {
        void onClick(View view, int position);
    }
}

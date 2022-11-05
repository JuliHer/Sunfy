package com.artuok.appwork.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.artuok.appwork.R;
import com.artuok.appwork.fragmets.homeFragment;
import com.artuok.appwork.library.WeekView;
import com.thekhaeng.pushdownanim.PushDownAnim;

import java.util.List;

public class ScheduleAdapter extends RecyclerView.Adapter<ScheduleAdapter.Recurrence> {

    OnClickListener listener;
    OnClickListener removeListener;
    LayoutInflater mInflater;
    List<WeekView.EventsTasks> mData;

    public ScheduleAdapter(Context context, List<WeekView.EventsTasks> mData, OnClickListener listener, OnClickListener removeListener) {
        this.mInflater = LayoutInflater.from(context);
        this.mData = mData;
        this.listener = listener;
        this.removeListener = removeListener;
    }

    @NonNull
    @Override
    public Recurrence onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.recurrence_layout, parent, false);
        return new Recurrence(view);
    }

    @Override
    public void onBindViewHolder(@NonNull Recurrence holder, int position) {
        holder.onBindServices(mData.get(position), position);
    }

    @Override
    public int getItemCount() {
        return mData.size();
    }

    public class Recurrence extends RecyclerView.ViewHolder implements View.OnClickListener {

        TextView day, time;
        ImageView delete;

        public Recurrence(@NonNull View itemView) {
            super(itemView);
            day = itemView.findViewById(R.id.day_date);
            time = itemView.findViewById(R.id.time_date);
            delete = itemView.findViewById(R.id.remove_action);

            PushDownAnim.setPushDownAnimTo(delete).setDurationPush(100)
                    .setScale(PushDownAnim.MODE_SCALE, 0.98f)
                    .setOnClickListener(view -> removeListener.onClick(view, getLayoutPosition()));

            PushDownAnim.setPushDownAnimTo(itemView)
                    .setDurationPush(100)
                    .setScale(PushDownAnim.MODE_SCALE, 0.98f)
                    .setOnClickListener(this);
        }

        void onBindServices(WeekView.EventsTasks element, int pos) {
            String mon = homeFragment.getDayOfWeek(mInflater.getContext(), element.getDay() + 1);
            day.setText(mon);
            long hourStartMillis = element.getHour();
            int hour = (int) (hourStartMillis / 3600);
            int minute = (int) (hourStartMillis / 60) % 60;
            hour = hour % 24;
            String tm = hour > 11 ? "PM" : "AM";
            hour = hour > 12 ? hour - 12 : hour;
            if (hour == 0) {
                hour = 12;
            }
            String min = minute < 10 ? "0" + minute : minute + "";

            String desc = hour + ":" + min + " " + tm + " -> ";

            long hourEndMillis = element.getDuration() + element.getHour();
            hour = (int) (hourEndMillis / 3600);
            minute = (int) (hourEndMillis / 60) % 60;
            hour = hour % 24;
            tm = hour > 11 ? "PM" : "AM";
            hour = hour > 12 ? hour - 12 : hour;
            if (hour == 0) {
                hour = 12;
            }

            min = minute < 10 ? "0" + minute : minute + "";

            desc += hour + ":" + min + " " + tm;
            time.setText(desc);

            if (pos == 0) {
                delete.setVisibility(View.GONE);
            }

        }

        @Override
        public void onClick(View view) {
            listener.onClick(view, getLayoutPosition());
        }
    }

    public interface OnClickListener {
        void onClick(View view, int pos);
    }
}

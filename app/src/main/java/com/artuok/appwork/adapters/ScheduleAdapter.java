package com.artuok.appwork.adapters;

import android.content.Context;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.artuok.appwork.R;
import com.artuok.appwork.library.CalendarWeekView;
import com.artuok.appwork.library.Constants;
import com.thekhaeng.pushdownanim.PushDownAnim;

import java.util.List;

public class ScheduleAdapter extends RecyclerView.Adapter<ScheduleAdapter.ScheduleViewHolder> {

    OnClickListener listener;
    OnClickListener removeListener;
    LayoutInflater mInflater;
    List<CalendarWeekView.EventsTask> mData;

    public ScheduleAdapter(Context context, List<CalendarWeekView.EventsTask> mData, OnClickListener listener, OnClickListener removeListener) {
        this.mInflater = LayoutInflater.from(context);
        this.mData = mData;
        this.listener = listener;
        this.removeListener = removeListener;
    }

    @NonNull
    @Override
    public ScheduleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.item_schedule_layout, parent, false);
        return new ScheduleViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ScheduleViewHolder holder, int position) {
        holder.onBindData(mData.get(position));
    }

    @Override
    public int getItemCount() {
        return mData.size();
    }

    public class ScheduleViewHolder extends RecyclerView.ViewHolder {

        TextView duration, day, hour;
        public ScheduleViewHolder(@NonNull View itemView) {
            super(itemView);

            duration = itemView.findViewById(R.id.duration);
            day = itemView.findViewById(R.id.day);
            hour = itemView.findViewById(R.id.hour);

            PushDownAnim.setPushDownAnimTo(itemView)
                    .setOnClickListener(view -> listener.onClick(view, getLayoutPosition()));
        }

        public void onBindData(CalendarWeekView.EventsTask element){
            String d = parseMiliToString(element.getDuration());
            duration.setText(d);

            String mon = Constants.getDayOfWeek(mInflater.getContext(), element.getDay() + 1);
            String dayS = mon.toUpperCase().substring(0, 3);
            day.setText(dayS);
            String times = getTimeString(element.getHour());
            hour.setText(times);
        }

        public String parseMiliToString(long milisegundos) {
            long segundos = milisegundos;

            if (segundos < 3600) {
                long minutos = segundos / 60;

                return minutos + "m";
            } else {
                long horas = segundos / 3600;
                long minutosRestantes = (segundos % 3600) / 60;
                if (minutosRestantes == 0) {
                    return horas + "h";
                } else {
                    return horas + "h+";
                }
            }
        }

        String getTimeString(long hourMillis){
            int hours = (int) (hourMillis / 3600);
            int minutes = (int) (hourMillis / 60) % 60;
            hours = hours % 24;
            boolean is24H = DateFormat.is24HourFormat(mInflater.getContext());
            String tm = "";
            if(!is24H){
                tm += hours > 11 ? "\npm" : "\nam";
            }

            if(!is24H){
                hours = hours > 12 ? hours - 12 : hours;
                if(hours == 0) hours = 12;
            }

            String minute = minutes < 10 ? "0"+minutes:minutes+"";

            return hours +":"+minute+tm;
        }
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

        void onBindServices(CalendarWeekView.EventsTask element, int pos) {
            String mon = Constants.getDayOfWeek(mInflater.getContext(), element.getDay() + 1);
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

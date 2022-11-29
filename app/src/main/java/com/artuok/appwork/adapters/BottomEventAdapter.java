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
import com.artuok.appwork.objects.TaskEvent;

import java.util.Calendar;
import java.util.List;

public class BottomEventAdapter extends RecyclerView.Adapter<BottomEventAdapter.EventViewHolder> {
    List<TaskEvent> mData;
    LayoutInflater mInflater;

    public BottomEventAdapter(Context context, List<TaskEvent> mData) {
        this.mData = mData;
        this.mInflater = LayoutInflater.from(context);
    }

    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.item_event_layout, parent, false);

        return new EventViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        holder.onBindData(mData.get(position));
    }

    @Override
    public int getItemCount() {
        return mData.size();
    }

    public class EventViewHolder extends RecyclerView.ViewHolder {

        TextView title, hours;
        ImageView color;

        public EventViewHolder(@NonNull View itemView) {
            super(itemView);

            title = itemView.findViewById(R.id.text);
            hours = itemView.findViewById(R.id.time);
            color = itemView.findViewById(R.id.subject_color);
        }

        void onBindData(TaskEvent event) {
            title.setText(event.getTitle());
            color.setColorFilter(event.getColor());

            Calendar c = Calendar.getInstance();

            long timeinMillis = event.getTimeInMillis();

            c.setTimeInMillis(timeinMillis);

            int hour = c.get(Calendar.HOUR_OF_DAY);
            int minute = c.get(Calendar.MINUTE);

            String min = minute < 10 ? "0" + minute : minute + "";
            hour = hour > 12 ? hour - 12 : hour;


            String time = hour + ":" + min + " ";

            time += (c.get(Calendar.AM_PM) == Calendar.AM) ? "AM" : "PM";

            hours.setText(time);
        }
    }
}

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
import com.artuok.appwork.objects.Item;
import com.artuok.appwork.objects.TaskEvent;
import com.artuok.appwork.objects.TextElement;

import java.util.Calendar;
import java.util.List;

public class BottomEventAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    List<Item> mData;
    LayoutInflater mInflater;

    public BottomEventAdapter(Context context, List<Item> mData) {
        this.mData = mData;
        this.mInflater = LayoutInflater.from(context);
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == 0) {
            View view = mInflater.inflate(R.layout.item_event_layout, parent, false);

            return new EventViewHolder(view);
        } else if (viewType == 1) {
            View view = mInflater.inflate(R.layout.item_text_layout, parent, false);

            return new TextViewHolder(view);
        }


        return null;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        int type = getItemViewType(position);
        if (type == 0) {
            ((EventViewHolder) holder).onBindData((TaskEvent) mData.get(position).getObject());
        } else if (type == 1) {
            ((TextViewHolder) holder).onBindData((TextElement) mData.get(position).getObject());
        }

    }

    @Override
    public int getItemCount() {
        return mData.size();
    }

    @Override
    public int getItemViewType(int position) {
        return mData.get(position).getType();
    }

    public class TextViewHolder extends RecyclerView.ViewHolder {
        TextView textView;


        public TextViewHolder(@NonNull View itemView) {
            super(itemView);

            textView = itemView.findViewById(R.id.text);
        }

        void onBindData(TextElement element) {
            textView.setText(element.getText());
            if(element.getTextSize() != -1){
                textView.setTextSize(element.getTextSize());
            }

            if(element.getColor() != -1){
                textView.setTextColor(element.getColor());
            }
        }
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


            time += (c.get(Calendar.AM_PM) == Calendar.AM) ? "a. m." : "p. m.";
            hours.setText(time);
        }
    }
}

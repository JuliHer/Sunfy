package com.artuok.appwork.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.artuok.appwork.R;
import com.artuok.appwork.objects.Item;
import com.artuok.appwork.objects.MessageElement;

import java.util.Calendar;
import java.util.List;

public class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    LayoutInflater inflater;
    List<Item> mData;

    public MessageAdapter(Context context, List<Item> mData) {
        this.inflater = LayoutInflater.from(context);
        this.mData = mData;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        if (viewType == 0) {
            View view = inflater.inflate(R.layout.item_my_message_layout, parent, false);
            return new MyMessageViewHolder(view);
        } else if (viewType == 1) {
            View view = inflater.inflate(R.layout.item_their_message_layout, parent, false);
            return new TheirMessageViewHolder(view);
        }
        return null;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        int type = getItemViewType(position);
        if (type == 0) {
            MessageElement element = (MessageElement) mData.get(position).getObject();
            ((MyMessageViewHolder) holder).onBindData(element);
        } else if (type == 1) {
            MessageElement element = (MessageElement) mData.get(position).getObject();
            ((TheirMessageViewHolder) holder).onBindData(element);
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

    class MyMessageViewHolder extends RecyclerView.ViewHolder {

        TextView message, time;

        public MyMessageViewHolder(@NonNull View itemView) {
            super(itemView);

            message = itemView.findViewById(R.id.mainMessage);
            time = itemView.findViewById(R.id.timestamp);
        }

        public void onBindData(MessageElement element) {
            message.setText(element.getMessage());
            Calendar c = Calendar.getInstance();
            c.setTimeInMillis(element.getTimestamp());
            int h = c.get(Calendar.HOUR_OF_DAY);
            int m = c.get(Calendar.MINUTE);
            String minute = m < 10 ? "0" + m : "" + m;
            String tm = c.get(Calendar.AM_PM) == Calendar.AM ? "AM" : "PM";

            String hour = (h % 12) + "";
            hour = (h == 12) ? "12" : hour;

            String timestamp = hour + ":" + minute + " " + tm;
            time.setText(timestamp);
        }
    }

    class TheirMessageViewHolder extends RecyclerView.ViewHolder {
        TextView message, time;

        public TheirMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            message = itemView.findViewById(R.id.mainMessage);
            time = itemView.findViewById(R.id.timestamp);
        }

        public void onBindData(MessageElement element) {
            message.setText(element.getMessage());
            Calendar c = Calendar.getInstance();
            c.setTimeInMillis(element.getTimestamp());
            int h = c.get(Calendar.HOUR_OF_DAY);
            int m = c.get(Calendar.MINUTE);
            String minute = m < 10 ? "0" + m : "" + m;
            String tm = c.get(Calendar.AM_PM) == Calendar.AM ? "AM" : "PM";

            String hour = (h % 12) + "";
            hour = (h == 12) ? "12" : hour;

            String timestamp = hour + ":" + minute + " " + tm;
            time.setText(timestamp);
        }
    }
}

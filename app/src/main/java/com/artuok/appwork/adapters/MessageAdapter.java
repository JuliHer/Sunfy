package com.artuok.appwork.adapters;

import android.content.Context;
import android.text.format.DateFormat;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.artuok.appwork.R;
import com.artuok.appwork.fragmets.HomeFragment;
import com.artuok.appwork.library.Constants;
import com.artuok.appwork.objects.Item;
import com.artuok.appwork.objects.MessageElement;
import com.artuok.appwork.objects.TextElement;
import com.thekhaeng.pushdownanim.PushDownAnim;

import java.util.Calendar;
import java.util.List;

public class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    LayoutInflater inflater;
    List<Item> mData;
    OnAddEventListener addEventListener;
    OnLongClickListener onLongClickListener;
    OnClickListener onClickListener;

    public MessageAdapter(Context context, List<Item> mData) {
        this.inflater = LayoutInflater.from(context);
        this.mData = mData;
    }

    public void setOnAddEventListener(OnAddEventListener listener){
        this.addEventListener = listener;
    }

    public void setOnLongClickListener(OnLongClickListener onLongClickListener) {
        this.onLongClickListener = onLongClickListener;
    }

    public void setOnClickListener(OnClickListener onClickListener) {
        this.onClickListener = onClickListener;
    }

    public void addMessage(Item element){
        mData.add(0, element);
    }

    public void changeMessage(List<Item> list){
        mData = list;
    }

    public void modifyStatus(int pos, int status){
        ((MessageElement)mData.get(pos).getObject()).setStatus(status);
    }

    public List<Item> getData(){
        return mData;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        if (viewType == 0) {
            View view = inflater.inflate(R.layout.item_message_layout, parent, false);
            return new MessageViewHolder(view);
        }else if(viewType == 1){
            View view = inflater.inflate(R.layout.item_message_reply_layout, parent, false);
            return new MessageReplyViewHolder(view);
        }else if(viewType == 2){
            View view = inflater.inflate(R.layout.item_message_task_layout, parent, false);
            return new MessageTaskViewHolder(view);
        }else if(viewType == 3){
            View view = inflater.inflate(R.layout.item_message_event_layout, parent, false);
            return new EventViewHolder(view);
        }
        return null;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        int type = getItemViewType(position);
        if (type == 0) {
            MessageElement element = (MessageElement) mData.get(position).getObject();
            ((MessageViewHolder) holder).onBindData(element);
        }else if(type == 1){
            MessageElement element = (MessageElement) mData.get(position).getObject();
            ((MessageReplyViewHolder) holder).onBindData(element);
        }else if(type == 2){
            MessageElement element = (MessageElement) mData.get(position).getObject();
            ((MessageTaskViewHolder) holder).onBindData(element);
        }else if(type == 3){
            TextElement element = (TextElement) mData.get(position).getObject();
            ((EventViewHolder) holder).onBindData(element);
        }

    }

    public Item getItem(int pos){ return mData.get(pos); }

    @Override
    public int getItemCount() {
        return mData.size();
    }

    @Override
    public int getItemViewType(int position) {
        return mData.get(position).getType();
    }


    class MessageViewHolder extends RecyclerView.ViewHolder{

        TextView text, texty, time, timey;
        ImageView stat;

        LinearLayout my, your;
        View total;
        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            text = itemView.findViewById(R.id.main);
            texty = itemView.findViewById(R.id.mainy);
            time = itemView.findViewById(R.id.timestamp);
            timey = itemView.findViewById(R.id.timestampy);
            stat = itemView.findViewById(R.id.status);
            my = itemView.findViewById(R.id.my);
            your = itemView.findViewById(R.id.your);
            total = itemView;
        }

        void onBindData(MessageElement element){
            Calendar c = Calendar.getInstance();
            c.setTimeInMillis(element.getTimestamp());
            int h = c.get(Calendar.HOUR_OF_DAY);
            int m = c.get(Calendar.MINUTE);
            String minute = m < 10 ? "0" + m : "" + m;
            String tm = c.get(Calendar.AM_PM) == Calendar.AM ? "a. m." : "p. m.";

            String hour = (h % 12) + "";
            hour = (h == 12) ? "12" : hour;

            boolean hourFormat = DateFormat.is24HourFormat(inflater.getContext());
            String timestamp = "";
            if (!hourFormat) {
                timestamp = hour + ":" + minute + " " + tm;
            } else {
                timestamp = h + ":" + minute;
            }
            if(element.getUser() == 0){
                your.setVisibility(View.GONE);
                my.setVisibility(View.VISIBLE);
                text.setText(element.getMessage());
                time.setText(timestamp);

                stat.setColorFilter(null);
                if (element.getStatus() == 0) {
                    stat.setImageDrawable(inflater.getContext().getDrawable(R.drawable.ic_clock));
                } else if (element.getStatus() == 1) {
                    stat.setImageDrawable(inflater.getContext().getDrawable(R.drawable.ic_check));
                } else if (element.getStatus() == 2) {
                    stat.setImageDrawable(inflater.getContext().getDrawable(R.drawable.ic_check_circle));
                } else if (element.getStatus() == 3) {
                    stat.setImageDrawable(inflater.getContext().getDrawable(R.drawable.ic_check_circle));
                    stat.setColorFilter(inflater.getContext().getColor(R.color.blue_500));
                }
            }else{
                your.setVisibility(View.VISIBLE);
                my.setVisibility(View.GONE);
                texty.setText(element.getMessage());
                timey.setText(timestamp);
            }

            if(getLayoutPosition() + 1 < mData.size()){
                RecyclerView.LayoutParams p = (RecyclerView.LayoutParams) total.getLayoutParams();
                if(mData.get(getLayoutPosition() + 1).getType() != 3){
                    if (((MessageElement) mData.get(getLayoutPosition() + 1).getObject()).getUser() != ((MessageElement) mData.get(getLayoutPosition()).getObject()).getUser()) {
                        p.setMargins(0, convertToDpToPx(10), 0, 0);
                    } else {
                        p.setMargins(0, 0, 0, 0);
                    }
                }

                total.setLayoutParams(p);
                total.requestLayout();
            }else if(getLayoutPosition() + 1 == mData.size()){
                RecyclerView.LayoutParams p = (RecyclerView.LayoutParams) total.getLayoutParams();
                p.setMargins(0, convertToDpToPx(10), 0, 0);
                total.setLayoutParams(p);
                total.requestLayout();
            }
        }
    }

    class MessageReplyViewHolder extends RecyclerView.ViewHolder{

        TextView text, texty, time, timey, name, namey, reply, replyy;
        ImageView stat;

        LinearLayout my, your;
        View total;
        public MessageReplyViewHolder(@NonNull View itemView) {
            super(itemView);
            text = itemView.findViewById(R.id.main);
            texty = itemView.findViewById(R.id.mainy);
            time = itemView.findViewById(R.id.timestamp);
            timey = itemView.findViewById(R.id.timestampy);
            stat = itemView.findViewById(R.id.status);
            my = itemView.findViewById(R.id.my);
            your = itemView.findViewById(R.id.your);
            reply = itemView.findViewById(R.id.reply);
            replyy = itemView.findViewById(R.id.replyy);
            name = itemView.findViewById(R.id.name);
            namey = itemView.findViewById(R.id.namey);
            total = itemView;
        }

        void onBindData(MessageElement element){
            Calendar c = Calendar.getInstance();
            c.setTimeInMillis(element.getTimestamp());
            int h = c.get(Calendar.HOUR_OF_DAY);
            int m = c.get(Calendar.MINUTE);
            String minute = m < 10 ? "0" + m : "" + m;
            String tm = c.get(Calendar.AM_PM) == Calendar.AM ? "a. m." : "p. m.";

            String hour = (h % 12) + "";
            hour = (h == 12) ? "12" : hour;

            boolean hourFormat = DateFormat.is24HourFormat(inflater.getContext());
            String timestamp = "";
            if (!hourFormat) {
                timestamp = hour + ":" + minute + " " + tm;
            } else {
                timestamp = h + ":" + minute;
            }


            MessageElement msg = element.getReply();
            if(element.getUser() == 0){

                your.setVisibility(View.GONE);
                my.setVisibility(View.VISIBLE);
                text.setText(element.getMessage());
                time.setText(timestamp);
                reply.setText(msg.getMessage());
                name.setText(msg.getUser() == 0 ? inflater.getContext().getString(R.string.you) : msg.getName());

                stat.setColorFilter(null);
                if (element.getStatus() == 0) {
                    stat.setImageDrawable(inflater.getContext().getDrawable(R.drawable.ic_clock));
                } else if (element.getStatus() == 1) {
                    stat.setImageDrawable(inflater.getContext().getDrawable(R.drawable.ic_check));
                } else if (element.getStatus() == 2) {
                    stat.setImageDrawable(inflater.getContext().getDrawable(R.drawable.ic_check_circle));
                } else if (element.getStatus() == 3) {
                    stat.setImageDrawable(inflater.getContext().getDrawable(R.drawable.ic_check_circle));
                    stat.setColorFilter(inflater.getContext().getColor(R.color.blue_500));
                }
            }else{

                your.setVisibility(View.VISIBLE);
                my.setVisibility(View.GONE);
                texty.setText(element.getMessage());
                timey.setText(timestamp);
                replyy.setText(msg.getMessage());
                namey.setText(msg.getUser() == 0 ? inflater.getContext().getString(R.string.you) : msg.getName());
            }

            if(getLayoutPosition() + 1 < mData.size()){
                RecyclerView.LayoutParams p = (RecyclerView.LayoutParams) total.getLayoutParams();
                if(mData.get(getLayoutPosition() + 1).getType() != 3){
                    if (((MessageElement) mData.get(getLayoutPosition() + 1).getObject()).getUser() != ((MessageElement) mData.get(getLayoutPosition()).getObject()).getUser()) {
                        p.setMargins(0, convertToDpToPx(10), 0, 0);
                    } else {
                        p.setMargins(0, 0, 0, 0);
                    }
                }
                total.setLayoutParams(p);
                total.requestLayout();
            }
        }
    }


    class MessageTaskViewHolder extends RecyclerView.ViewHolder{

        TextView text, texty, time, timey, date, datey, task, tasky;
        ImageView stat;

        LinearLayout my, your, add, addy;
        View total;
        public MessageTaskViewHolder(@NonNull View itemView) {
            super(itemView);
            text = itemView.findViewById(R.id.main);
            texty = itemView.findViewById(R.id.mainy);
            time = itemView.findViewById(R.id.timestamp);
            timey = itemView.findViewById(R.id.timestampy);
            stat = itemView.findViewById(R.id.status);
            my = itemView.findViewById(R.id.my);
            your = itemView.findViewById(R.id.your);

            task = itemView.findViewById(R.id.task);
            tasky = itemView.findViewById(R.id.tasky);
            date = itemView.findViewById(R.id.date);
            datey = itemView.findViewById(R.id.datey);
            add = itemView.findViewById(R.id.add_btn);
            addy = itemView.findViewById(R.id.add_btn_y);
            total = itemView;
            PushDownAnim.setPushDownAnimTo(add)
                    .setOnClickListener(view -> {
                        if(addEventListener != null){
                            addEventListener.onAddEvent(view, getLayoutPosition());
                        }
                    });
            PushDownAnim.setPushDownAnimTo(addy)
                    .setOnClickListener(view -> {
                        if(addEventListener != null){
                            addEventListener.onAddEvent(view, getLayoutPosition());
                        }
                    });
        }

        void onBindData(MessageElement element){
            Calendar c = Calendar.getInstance();
            c.setTimeInMillis(element.getTimestamp());
            int h = c.get(Calendar.HOUR_OF_DAY);
            int m = c.get(Calendar.MINUTE);
            String minute = m < 10 ? "0" + m : "" + m;
            String tm = c.get(Calendar.AM_PM) == Calendar.AM ? "a. m." : "p. m.";

            String hour = (h % 12) + "";
            hour = (h == 12) ? "12" : hour;

            boolean hourFormat = DateFormat.is24HourFormat(inflater.getContext());
            String timestamp = "";
            if (!hourFormat) {
                timestamp = hour + ":" + minute + " " + tm;
            } else {
                timestamp = h + ":" + minute;
            }


            if(element.getUser() == 0){

                your.setVisibility(View.GONE);
                my.setVisibility(View.VISIBLE);
                text.setText(element.getMessage());
                time.setText(timestamp);
                task.setText(element.getTask().getDescription());
                date.setText(dow(element.getTask().getDeadline()));

                stat.setColorFilter(null);
                if (element.getStatus() == 0) {
                    stat.setImageDrawable(inflater.getContext().getDrawable(R.drawable.ic_clock));
                } else if (element.getStatus() == 1) {
                    stat.setImageDrawable(inflater.getContext().getDrawable(R.drawable.ic_check));
                } else if (element.getStatus() == 2) {
                    stat.setImageDrawable(inflater.getContext().getDrawable(R.drawable.ic_check_circle));
                } else if (element.getStatus() == 3) {
                    stat.setImageDrawable(inflater.getContext().getDrawable(R.drawable.ic_check_circle));
                    stat.setColorFilter(inflater.getContext().getColor(R.color.blue_500));
                }
            }else{

                your.setVisibility(View.VISIBLE);
                my.setVisibility(View.GONE);
                texty.setText(element.getMessage());
                timey.setText(timestamp);
                tasky.setText(element.getTask().getDescription());
                datey.setText(dow(element.getTask().getDeadline()));
            }

            if(getLayoutPosition() + 1 < mData.size()){
                RecyclerView.LayoutParams p = (RecyclerView.LayoutParams) total.getLayoutParams();
                if(mData.get(getLayoutPosition() + 1).getType() != 3){
                    if (((MessageElement) mData.get(getLayoutPosition() + 1).getObject()).getUser() != ((MessageElement) mData.get(getLayoutPosition()).getObject()).getUser()) {
                        p.setMargins(0, convertToDpToPx(10), 0, 0);
                    } else {
                        p.setMargins(0, 0, 0, 0);
                    }
                }

                total.setLayoutParams(p);
                total.requestLayout();
            }
        }

        private String dow(long tim) {
            String d;
            Calendar c = Calendar.getInstance();
            c.setTimeInMillis(tim);
            int day = c.get(Calendar.DAY_OF_MONTH);
            int month = c.get(Calendar.MONTH);
            int year = c.get(Calendar.YEAR);
            int hour = c.get(Calendar.HOUR) == 0 ? 12 : c.get(Calendar.HOUR);
            int minute = c.get(Calendar.MINUTE);

            String dd = day < 10 ? "0" + day : String.valueOf(day);
            String datetime = dd + " " + Constants.getMonthMinor(inflater.getContext(), month) + " " + year + " ";
            String mn = minute < 10 ? "0" + minute : String.valueOf(minute);
            datetime += hour + ":" + mn;
            datetime += c.get(Calendar.AM_PM) == Calendar.AM ? " a. m." : " p. m.";
            d = datetime;
            return d;
        }
    }

    //Convert methods
    private int convertToDpToPx(int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, inflater.getContext().getResources().getDisplayMetrics());
    }

    public interface OnAddEventListener{
        void onAddEvent(View view, int pos);
    }

    public interface OnClickListener{
        void onClick(View view, int pos);
    }

    public interface OnLongClickListener{
        void onLong(View view, int pos);
    }

    class EventViewHolder extends RecyclerView.ViewHolder{
        TextView text;
        public EventViewHolder(@NonNull View itemView) {
            super(itemView);

            text = itemView.findViewById(R.id.text);
        }

        void onBindData(TextElement element){

            text.setText(element.getText());
        }
    }
}

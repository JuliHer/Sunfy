package com.artuok.appwork.adapters;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.artuok.appwork.R;
import com.artuok.appwork.fragmets.homeFragment;
import com.artuok.appwork.objects.EventMessageElement;
import com.artuok.appwork.objects.Item;
import com.artuok.appwork.objects.MessageElement;
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

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        if (viewType == 0) {
            View view = inflater.inflate(R.layout.item_my_message_layout, parent, false);
            return new MyMessageViewHolder(view);
        } else if (viewType == 1) {
            View view = inflater.inflate(R.layout.item_their_message_layout, parent, false);
            return new TheirMessageViewHolder(view);
        } else if (viewType == 2){
            View view = inflater.inflate(R.layout.item_my_replymessage_layout, parent, false);
            return new MyReplyMessageViewHolder(view);
        }else if(viewType == 3){
            View view = inflater.inflate(R.layout.item_their_replymessage_layout, parent, false);
            return new TheirReplyMessageViewHolder(view);
        }else if (viewType == 4){
            View view = inflater.inflate(R.layout.item_myevent_message_layout, parent, false);
            return new MyEventMessageViewHolder(view);
        }else if(viewType == 5){
            View view = inflater.inflate(R.layout.item_theirevent_message_layout, parent, false);
            return new TheirEventMessageViewHolder(view);
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
        else if (type == 2) {
            MessageElement element = (MessageElement) mData.get(position).getObject();
            ((MyReplyMessageViewHolder) holder).onBindData(element);
        }else if (type == 3) {
            MessageElement element = (MessageElement) mData.get(position).getObject();
            ((TheirReplyMessageViewHolder) holder).onBindData(element);
        }else if (type == 4){
            MessageElement element = (MessageElement) mData.get(position).getObject();
            ((MyEventMessageViewHolder) holder).onBindData(element);
        }else if (type == 5){
            MessageElement element = (MessageElement) mData.get(position).getObject();
            ((TheirEventMessageViewHolder) holder).onBindData(element);
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
        ImageView stat;
        View total;

        public MyMessageViewHolder(@NonNull View itemView) {
            super(itemView);

            message = itemView.findViewById(R.id.mainMessage);
            time = itemView.findViewById(R.id.timestamp);
            stat = itemView.findViewById(R.id.status);
            total = itemView;
            PushDownAnim.setPushDownAnimTo(itemView)
                    .setDurationPush(100)
                    .setScale(PushDownAnim.MODE_SCALE, 0.98f)
                    .setOnClickListener(view -> {
                        if(onClickListener != null) {
                            onClickListener.onClick(view, getLayoutPosition());
                        }
                    })
                    .setOnLongClickListener(view -> {
                        if(onLongClickListener != null){
                            onLongClickListener.onLong(view, getLayoutPosition());
                            return true;
                        }
                        return false;
                    });
        }

        public void onBindData(MessageElement element) {
            message.setText(element.getMessage());

            message.setLinksClickable(true);
            Calendar c = Calendar.getInstance();
            c.setTimeInMillis(element.getTimestamp());
            int h = c.get(Calendar.HOUR_OF_DAY);
            int m = c.get(Calendar.MINUTE);
            String minute = m < 10 ? "0" + m : "" + m;
            String tm = c.get(Calendar.AM_PM) == Calendar.AM ? "a. m." : "p. m.";

            String hour = (h % 12) + "";
            hour = (h == 12) ? "12" : hour;

            String timestamp = hour + ":" + minute + " " + tm;

            if(element.getStatus() == 0){
                stat.setImageDrawable(inflater.getContext().getDrawable(R.drawable.ic_clock));
            }else if(element.getStatus() == 1){
                stat.setImageDrawable(inflater.getContext().getDrawable(R.drawable.check_sended));
            }else if(element.getStatus() == 2){
                stat.setImageDrawable(inflater.getContext().getDrawable(R.drawable.ic_check_circle));
            }else if(element.getStatus() == 3){
                stat.setImageDrawable(inflater.getContext().getDrawable(R.drawable.ic_check_circle));
                stat.setColorFilter(inflater.getContext().getColor(R.color.green_500));
            }


            if(mData.size() > getLayoutPosition() + 1){
                if(((MessageElement)mData.get(getLayoutPosition() + 1).getObject()).getMine() != ((MessageElement)mData.get(getLayoutPosition()).getObject()).getMine()){
                    RecyclerView.LayoutParams p = (RecyclerView.LayoutParams) total.getLayoutParams();
                    p.setMargins(0, convertToDpToPx(10), 0, 0);
                    total.setLayoutParams(p);
                    total.requestLayout();
                }
            }

            if(element.isSelect()){
                TypedArray a = inflater.getContext().obtainStyledAttributes(R.styleable.AppCustomAttrs);
                int color = a.getColor(R.styleable.AppCustomAttrs_backgroundLighting, inflater.getContext().getColor(R.color.black_transparent_500));
                a.recycle();
                total.setBackgroundColor(color);
            }else{
                total.setBackgroundColor(Color.parseColor("#00000000"));
            }

            time.setText(timestamp);
        }
    }

    class TheirMessageViewHolder extends RecyclerView.ViewHolder {
        TextView message, time;
        View total;
        public TheirMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            message = itemView.findViewById(R.id.mainMessage);
            time = itemView.findViewById(R.id.timestamp);
            total = itemView;
            PushDownAnim.setPushDownAnimTo(itemView)
                    .setDurationPush(100)
                    .setScale(PushDownAnim.MODE_SCALE, 0.98f)
                    .setOnClickListener(view -> {
                        if(onClickListener != null) {
                            onClickListener.onClick(view, getLayoutPosition());
                        }
                    })
                    .setOnLongClickListener(view -> {
                        if(onLongClickListener != null){
                            onLongClickListener.onLong(view, getLayoutPosition());
                            return true;
                        }
                        return false;
                    });
        }

        public void onBindData(MessageElement element) {
            message.setText(element.getMessage());
            message.setLinksClickable(true);
            Calendar c = Calendar.getInstance();
            c.setTimeInMillis(element.getTimestamp());
            int h = c.get(Calendar.HOUR_OF_DAY);
            int m = c.get(Calendar.MINUTE);
            String minute = m < 10 ? "0" + m : "" + m;
            String tm = c.get(Calendar.AM_PM) == Calendar.AM ? "a. m." : "p. m.";

            String hour = (h % 12) + "";
            hour = (h == 12) ? "12" : hour;

            String timestamp = hour + ":" + minute + " " + tm;

            if(mData.size() > getLayoutPosition() + 1){
                if(((MessageElement)mData.get(getLayoutPosition() + 1).getObject()).getMine() != ((MessageElement)mData.get(getLayoutPosition()).getObject()).getMine()){
                    RecyclerView.LayoutParams p = (RecyclerView.LayoutParams) total.getLayoutParams();
                    p.setMargins(0, convertToDpToPx(10), 0, 0);
                    total.setLayoutParams(p);
                    total.requestLayout();
                }
            }

            if(element.isSelect()){
                TypedArray a = inflater.getContext().obtainStyledAttributes(R.styleable.AppCustomAttrs);
                int color = a.getColor(R.styleable.AppCustomAttrs_backgroundLighting, inflater.getContext().getColor(R.color.black_transparent_500));
                a.recycle();
                total.setBackgroundColor(color);
            }else{
                total.setBackgroundColor(Color.parseColor("#00000000"));
            }

            time.setText(timestamp);
        }
    }

    class MyReplyMessageViewHolder extends RecyclerView.ViewHolder{
        TextView message, time, reply, name;
        ImageView stat;
        View total;

        public MyReplyMessageViewHolder(@NonNull View itemView) {
            super(itemView);

            message = itemView.findViewById(R.id.mainMessage);
            time = itemView.findViewById(R.id.timestamp);
            reply = itemView.findViewById(R.id.reply_message);
            name = itemView.findViewById(R.id.name_of_they);
            stat = itemView.findViewById(R.id.status);
            total = itemView;

            PushDownAnim.setPushDownAnimTo(itemView)
                    .setDurationPush(100)
                    .setScale(PushDownAnim.MODE_SCALE, 0.98f)
                    .setOnClickListener(view -> {
                        if(onClickListener != null) {
                            onClickListener.onClick(view, getLayoutPosition());
                        }
                    })
                    .setOnLongClickListener(view -> {
                        if(onLongClickListener != null){
                            onLongClickListener.onLong(view, getLayoutPosition());
                            return true;
                        }
                        return false;
                    });
        }

        public void onBindData(MessageElement element){
            message.setText(element.getMessage());
            Calendar c = Calendar.getInstance();
            c.setTimeInMillis(element.getTimestamp());
            int h = c.get(Calendar.HOUR_OF_DAY);
            int m = c.get(Calendar.MINUTE);
            String minute = m < 10 ? "0" + m : "" + m;
            String tm = c.get(Calendar.AM_PM) == Calendar.AM ? "a. m." : "p. m.";

            String hour = (h % 12) + "";
            hour = (h == 12) ? "12" : hour;

            reply.setText(element.getMessageReplyed());
            name.setText(element.getTheirName());

            String timestamp = hour + ":" + minute + " " + tm;

            if(element.getStatus() == 0){
                stat.setImageDrawable(inflater.getContext().getDrawable(R.drawable.ic_clock));
            }else if(element.getStatus() == 1){
                stat.setImageDrawable(inflater.getContext().getDrawable(R.drawable.check_sended));
            }else if(element.getStatus() == 2){
                stat.setImageDrawable(inflater.getContext().getDrawable(R.drawable.ic_check_circle));
            }else if(element.getStatus() == 3){
                stat.setImageDrawable(inflater.getContext().getDrawable(R.drawable.ic_check_circle));
                stat.setColorFilter(inflater.getContext().getColor(R.color.green_500));
            }

            if(mData.size() > getLayoutPosition() + 1){
                if(((MessageElement)mData.get(getLayoutPosition() + 1).getObject()).getMine() != ((MessageElement)mData.get(getLayoutPosition()).getObject()).getMine()){
                    RecyclerView.LayoutParams p = (RecyclerView.LayoutParams) total.getLayoutParams();
                    p.setMargins(0, convertToDpToPx(10), 0, 0);
                    total.setLayoutParams(p);
                    total.requestLayout();
                }
            }

            if(element.isSelect()){
                TypedArray a = inflater.getContext().obtainStyledAttributes(R.styleable.AppCustomAttrs);
                int color = a.getColor(R.styleable.AppCustomAttrs_backgroundLighting, inflater.getContext().getColor(R.color.black_transparent_500));
                a.recycle();
                total.setBackgroundColor(color);
            }else{
                total.setBackgroundColor(Color.parseColor("#00000000"));
            }

            time.setText(timestamp);
        }
    }

    class TheirReplyMessageViewHolder extends RecyclerView.ViewHolder{
        TextView message, time, reply, name;
        View total;
        public TheirReplyMessageViewHolder(@NonNull View itemView) {
            super(itemView);

            message = itemView.findViewById(R.id.mainMessage);
            time = itemView.findViewById(R.id.timestamp);
            reply = itemView.findViewById(R.id.reply_message);
            name = itemView.findViewById(R.id.name_of_they);
            total = itemView;

            PushDownAnim.setPushDownAnimTo(itemView)
                    .setDurationPush(100)
                    .setScale(PushDownAnim.MODE_SCALE, 0.98f)
                    .setOnClickListener(view -> {
                        if(onClickListener != null) {
                            onClickListener.onClick(view, getLayoutPosition());
                        }
                    })
                    .setOnLongClickListener(view -> {
                        if(onLongClickListener != null){
                            onLongClickListener.onLong(view, getLayoutPosition());
                            return true;
                        }
                        return false;
                    });
        }

        public void onBindData(MessageElement element){
            message.setText(element.getMessage());
            Calendar c = Calendar.getInstance();
            c.setTimeInMillis(element.getTimestamp());
            int h = c.get(Calendar.HOUR_OF_DAY);
            int m = c.get(Calendar.MINUTE);
            String minute = m < 10 ? "0" + m : "" + m;
            String tm = c.get(Calendar.AM_PM) == Calendar.AM ? "a. m." : "p. m.";

            String hour = (h % 12) + "";
            hour = (h == 12) ? "12" : hour;

            String timestamp = hour + ":" + minute + " " + tm;
            reply.setText(element.getMessageReplyed());
            name.setText(element.getTheirName());

            if(mData.size() > getLayoutPosition() + 1){
                if(((MessageElement)mData.get(getLayoutPosition() + 1).getObject()).getMine() != ((MessageElement)mData.get(getLayoutPosition()).getObject()).getMine()){
                    RecyclerView.LayoutParams p = (RecyclerView.LayoutParams) total.getLayoutParams();
                    p.setMargins(0, convertToDpToPx(10), 0, 0);
                    total.setLayoutParams(p);
                    total.requestLayout();
                }
            }

            if(element.isSelect()){
                TypedArray a = inflater.getContext().obtainStyledAttributes(R.styleable.AppCustomAttrs);
                int color = a.getColor(R.styleable.AppCustomAttrs_backgroundLighting, inflater.getContext().getColor(R.color.black_transparent_500));
                a.recycle();
                total.setBackgroundColor(color);
            }else{
                total.setBackgroundColor(Color.parseColor("#00000000"));
            }

            time.setText(timestamp);
        }
    }

    class MyEventMessageViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView message, time;
        TextView eTitle, eUser, eDate, eTime;
        ImageView stat, usericon;
        View total;
        CardView task;

        public MyEventMessageViewHolder(@NonNull View itemView) {
            super(itemView);

            task = itemView.findViewById(R.id.task);
            message = itemView.findViewById(R.id.mainMessage);
            time = itemView.findViewById(R.id.timestamp);
            stat = itemView.findViewById(R.id.status);
            eTitle = itemView.findViewById(R.id.eventtitle);
            eUser = itemView.findViewById(R.id.username);
            eDate = itemView.findViewById(R.id.endDate);
            eTime = itemView.findViewById(R.id.endTime);
            PushDownAnim.setPushDownAnimTo(task)
                    .setDurationPush(100)
                    .setScale(PushDownAnim.MODE_SCALE, 0.98f)
                    .setOnClickListener(this);
            total = itemView;

            PushDownAnim.setPushDownAnimTo(itemView)
                    .setDurationPush(100)
                    .setScale(PushDownAnim.MODE_SCALE, 0.98f)
                    .setOnClickListener(view -> {
                        if(onClickListener != null) {
                            onClickListener.onClick(view, getLayoutPosition());
                        }
                    })
                    .setOnLongClickListener(view -> {
                        if(onLongClickListener != null){
                            onLongClickListener.onLong(view, getLayoutPosition());
                            return true;
                        }
                        return false;
                    });
        }

        public void onBindData(MessageElement element) {
            message.setText(element.getMessage());
            if(element.getMessage().equals(" 1"))
                message.setText(inflater.getContext().getString(R.string.task));
            Calendar c = Calendar.getInstance();
            c.setTimeInMillis(element.getTimestamp());
            int h = c.get(Calendar.HOUR_OF_DAY);
            int m = c.get(Calendar.MINUTE);
            String minute = m < 10 ? "0" + m : "" + m;
            String tm = c.get(Calendar.AM_PM) == Calendar.AM ? "a. m." : "p. m.";

            String hour = (h % 12) + "";
            hour = (h == 12) ? "12" : hour;

            String timestamp = hour + ":" + minute + " " + tm;

            if(element.getStatus() == 0){
                stat.setImageDrawable(inflater.getContext().getDrawable(R.drawable.ic_clock));
            }else if(element.getStatus() == 1){
                stat.setImageDrawable(inflater.getContext().getDrawable(R.drawable.check_sended));
            }else if(element.getStatus() == 2){
                stat.setImageDrawable(inflater.getContext().getDrawable(R.drawable.ic_check_circle));
            }else if(element.getStatus() == 3){
                stat.setImageDrawable(inflater.getContext().getDrawable(R.drawable.ic_check_circle));
                stat.setColorFilter(inflater.getContext().getColor(R.color.green_500));
            }

            if(element.isSelect()){
                TypedArray a = inflater.getContext().obtainStyledAttributes(R.styleable.AppCustomAttrs);
                int color = a.getColor(R.styleable.AppCustomAttrs_backgroundLighting, inflater.getContext().getColor(R.color.black_transparent_500));
                a.recycle();
                total.setBackgroundColor(color);
            }else{
                total.setBackgroundColor(Color.parseColor("#00000000"));
            }

            EventMessageElement e = element.getEvent();
            eTitle.setText(e.getTitle());

            c.setTimeInMillis(e.getEndDate());
            int day = c.get(Calendar.DAY_OF_MONTH);
            int month = c.get(Calendar.MONTH);
            int year = c.get(Calendar.YEAR);
            int hours = c.get(Calendar.HOUR) == 0 ? 12 : c.get(Calendar.HOUR);
            int minutes = c.get(Calendar.MINUTE);

            String dd = day < 10 ? "0" + day : "" + day;
            String dates = dd + " " + homeFragment.getMonthMinor(inflater.getContext(), (month)) + " " + year + " ";
            String mn = minutes < 10 ? "0" + minutes : "" + minutes;
            String times = hours +":"+mn;
            times += c.get(Calendar.AM_PM) == Calendar.AM ?  " a. m." : " p. m.";

            eDate.setText(dates);
            eTime.setText(times);

            if(mData.size() > getLayoutPosition() + 1){
                if(((MessageElement)mData.get(getLayoutPosition() + 1).getObject()).getMine() != ((MessageElement)mData.get(getLayoutPosition()).getObject()).getMine()){
                    RecyclerView.LayoutParams p = (RecyclerView.LayoutParams) total.getLayoutParams();
                    p.setMargins(0, convertToDpToPx(10), 0, 0);
                    total.setLayoutParams(p);
                    total.requestLayout();
                }
            }

            time.setText(timestamp);
        }

        @Override
        public void onClick(View view) {
            if(addEventListener != null){
                addEventListener.onAddEvent(view, getLayoutPosition());
            }
        }
    }

    class TheirEventMessageViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView message, time;
        TextView eTitle, eUser, eDate, eTime;
        ImageView usericon;
        View total;
        CardView task;

        public TheirEventMessageViewHolder(@NonNull View itemView) {
            super(itemView);

            task = itemView.findViewById(R.id.task);
            message = itemView.findViewById(R.id.mainMessage);
            time = itemView.findViewById(R.id.timestamp);
            eTitle = itemView.findViewById(R.id.eventtitle);
            eUser = itemView.findViewById(R.id.username);
            eDate = itemView.findViewById(R.id.endDate);
            eTime = itemView.findViewById(R.id.endTime);
            PushDownAnim.setPushDownAnimTo(task)
                    .setDurationPush(100)
                    .setScale(PushDownAnim.MODE_SCALE, 0.98f)
                    .setOnClickListener(this);

            PushDownAnim.setPushDownAnimTo(itemView)
                    .setDurationPush(100)
                    .setScale(PushDownAnim.MODE_SCALE, 0.98f)
                    .setOnClickListener(view -> {
                        if(onClickListener != null) {
                            onClickListener.onClick(view, getLayoutPosition());
                        }
                    })
                    .setOnLongClickListener(view -> {
                        if(onLongClickListener != null){
                            onLongClickListener.onLong(view, getLayoutPosition());
                            return true;
                        }
                        return false;
                    });
            total = itemView;
        }

        public void onBindData(MessageElement element) {
            message.setText(element.getMessage());
            if(element.getMessage().equals(" 1")) {
                String s = inflater.getContext().getString(R.string.task);
                message.setText(s);
            }

            Calendar c = Calendar.getInstance();
            c.setTimeInMillis(element.getTimestamp());
            int h = c.get(Calendar.HOUR_OF_DAY);
            int m = c.get(Calendar.MINUTE);
            String minute = m < 10 ? "0" + m : "" + m;
            String tm = c.get(Calendar.AM_PM) == Calendar.AM ? "a. m." : "p. m.";

            String hour = (h % 12) + "";
            hour = (h == 12) ? "12" : hour;

            String timestamp = hour + ":" + minute + " " + tm;

            EventMessageElement e = element.getEvent();

            eTitle.setText(e.getTitle());

            c.setTimeInMillis(e.getEndDate());
            int day = c.get(Calendar.DAY_OF_MONTH);
            int month = c.get(Calendar.MONTH);
            int year = c.get(Calendar.YEAR);
            int hours = c.get(Calendar.HOUR) == 0 ? 12 : c.get(Calendar.HOUR);
            int minutes = c.get(Calendar.MINUTE);

            String dd = day < 10 ? "0" + day : "" + day;
            String dates = dd + " " + homeFragment.getMonthMinor(inflater.getContext(), (month)) + " " + year + " ";
            String mn = minutes < 10 ? "0" + minutes : "" + minutes;
            String times = hours +":"+mn;
            times += c.get(Calendar.AM_PM) == Calendar.AM ?  " a. m." : " p. m.";

            eDate.setText(dates);
            eTime.setText(times);

            if(mData.size() > getLayoutPosition() + 1){
                if(((MessageElement)mData.get(getLayoutPosition() + 1).getObject()).getMine() != ((MessageElement)mData.get(getLayoutPosition()).getObject()).getMine()){
                    RecyclerView.LayoutParams p = (RecyclerView.LayoutParams) total.getLayoutParams();
                    p.setMargins(0, convertToDpToPx(10), 0, 0);
                    total.setLayoutParams(p);
                    total.requestLayout();
                }
            }

            if(element.isSelect()){
                TypedArray a = inflater.getContext().obtainStyledAttributes(R.styleable.AppCustomAttrs);
                int color = a.getColor(R.styleable.AppCustomAttrs_backgroundLighting, inflater.getContext().getColor(R.color.black_transparent_500));
                a.recycle();
                total.setBackgroundColor(color);
            }else{
                total.setBackgroundColor(Color.parseColor("#00000000"));
            }

            time.setText(timestamp);
        }

        @Override
        public void onClick(View view) {
            if(addEventListener != null){
                addEventListener.onAddEvent(view, getLayoutPosition());
            }
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
}

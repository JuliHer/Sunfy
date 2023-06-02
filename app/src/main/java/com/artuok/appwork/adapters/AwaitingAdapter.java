package com.artuok.appwork.adapters;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.artuok.appwork.R;
import com.artuok.appwork.objects.AnnouncesElement;
import com.artuok.appwork.objects.AwaitElement;
import com.artuok.appwork.objects.Item;
import com.artuok.appwork.objects.StatisticsElement;
import com.artuok.appwork.objects.TextElement;
import com.google.android.gms.ads.nativead.NativeAdView;
import com.thekhaeng.pushdownanim.PushDownAnim;

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
        }else if(viewType == 3){
            View view = mInflater.inflate(R.layout.item_awaiting_grid_layout, parent, false);
            return new AwaitingGridViewHolder(view);
        } else if(viewType == 12){
            View view = mInflater.inflate(R.layout.item_ad_awaiting_layout, parent, false);
            return new AwaitingAdViewHolder(view);
        }
        return null;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        int type = getItemViewType(position);
        if (type == 0) {
            AwaitElement element = (AwaitElement) mData.get(position).getObject();
            ((AwaitingViewHolder) holder).onBindData(element);
        } else if (type == 1) {
            StatisticsElement element = (StatisticsElement) mData.get(position).getObject();
            ((StatisticViewHolder) holder).onBindData(element);
        } else if (type == 2) {
            TextElement element = (TextElement) mData.get(position).getObject();
            ((TextViewHolder) holder).onBindData(element);
        } else if (type == 3) {
            AwaitElement element = (AwaitElement) mData.get(position).getObject();
            ((AwaitingGridViewHolder) holder).onBindData(element);
        } else if(type == 12){
            AnnouncesElement element = (AnnouncesElement) mData.get(position).getObject();
            ((AwaitingAdViewHolder) holder).onBindData(element);
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

    class AwaitingGridViewHolder extends RecyclerView.ViewHolder{
        TextView title, status, date, time, subject;
        ImageView subjectIcon;

        public AwaitingGridViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.title_card);
            subjectIcon = itemView.findViewById(R.id.subject_color);
            subject = itemView.findViewById(R.id.subject);
            status = itemView.findViewById(R.id.status_card);
            date = itemView.findViewById(R.id.date_card);
            time = itemView.findViewById(R.id.time);

        }

        void onBindData(AwaitElement element){
            TypedArray ta = mInflater.getContext().obtainStyledAttributes(R.styleable.AppCustomAttrs);
            int color = ta.getColor(R.styleable.AppCustomAttrs_backgroundBorder, Color.WHITE);
            int colorSub = ta.getColor(R.styleable.AppCustomAttrs_subTextColor, Color.WHITE);
            title.setText(element.getTitle());
            title.setPaintFlags(title.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
            title.setTextColor(color);

            subjectIcon.setColorFilter(element.getTaskColor());

            status.setText(element.getStatus());
            status.setBackgroundColor(element.getStatusColor());

            if(element.isDone()){
                title.setPaintFlags(title.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                title.setTextColor(colorSub);
            }

            subject.setText(element.getSubject());

            date.setText(element.getDate());
            time.setText(element.getTime());
            ta.recycle();
        }
    }

    class AwaitingViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        TextView title, status, date, time;

        LinearLayout subject;
        ImageView liked;

        public AwaitingViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.title_card);
            subject = itemView.findViewById(R.id.subject_color);
            status = itemView.findViewById(R.id.status_card);
            date = itemView.findViewById(R.id.date_card);
            time = itemView.findViewById(R.id.time);
            liked = itemView.findViewById(R.id.task_liked);
            PushDownAnim.setPushDownAnimTo(itemView)
                    .setDurationPush(100)
                    .setScale(PushDownAnim.MODE_SCALE, 0.98f)
                    .setOnClickListener(this);
        }

        void onBindData(AwaitElement element){
            TypedArray ta = mInflater.getContext().obtainStyledAttributes(R.styleable.AppCustomAttrs);
            int color = ta.getColor(R.styleable.AppCustomAttrs_backgroundBorder, Color.WHITE);
            int colorSub = ta.getColor(R.styleable.AppCustomAttrs_subTextColor, Color.WHITE);
            title.setText(element.getTitle());
            title.setPaintFlags(title.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
            title.setTextColor(color);

            subject.setBackgroundColor(element.getTaskColor());

            status.setText(element.getStatus());
            status.setBackgroundColor(element.getStatusColor());

            if(element.isDone()){
                title.setPaintFlags(title.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                title.setTextColor(colorSub);
            }

            if(element.isLiked()){
                liked.setVisibility(View.VISIBLE);
            }else{
                liked.setVisibility(View.GONE);
            }

            date.setText(element.getDate());
            time.setText(element.getTime());
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

    class AwaitingAdViewHolder extends RecyclerView.ViewHolder{

        TextView title, body, announser, price, action;
        ImageView content, icon;
        NativeAdView adView;
        public AwaitingAdViewHolder(@NonNull View itemView) {
            super(itemView);

            title = itemView.findViewById(R.id.title_card);
            body = itemView.findViewById(R.id.body_card);
            announser = itemView.findViewById(R.id.announser_card);
            price = itemView.findViewById(R.id.price_card);
            content = itemView.findViewById(R.id.image_content);
            icon = itemView.findViewById(R.id.icon);
            action = itemView.findViewById(R.id.action);
            adView = itemView.findViewById(R.id.adView);
        }

        public void onBindData(AnnouncesElement element){
            title.setText(element.getTitle());
            body.setText(element.getBody());
            announser.setText(element.getAnnounser());
            price.setText(element.getPrice());
            action.setText(element.getAction());

            if (element.getIcon() != null) {
                icon.setImageDrawable(element.getIcon().getDrawable());
            }

            if (element.getImages() != null)
                content.setImageDrawable(element.getImages().get(0).getDrawable());

            adView.setHeadlineView(title);
            adView.setAdvertiserView(announser);
            adView.setBodyView(body);
            adView.setIconView(icon);
            adView.setPriceView(price);
            adView.setCallToActionView(action);
            adView.setImageView(content);

            adView.setNativeAd(element.getNativeAd());
        }
    }

    public interface OnClickListener {
        void onClick(View view, int position);
    }
}

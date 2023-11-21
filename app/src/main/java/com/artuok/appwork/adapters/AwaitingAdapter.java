package com.artuok.appwork.adapters;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.artuok.appwork.R;
import com.artuok.appwork.objects.AnnouncesElement;
import com.artuok.appwork.objects.AwaitElement;
import com.artuok.appwork.objects.Item;
import com.artuok.appwork.objects.TextElement;
import com.google.android.gms.ads.MediaContent;
import com.google.android.gms.ads.nativead.MediaView;
import com.google.android.gms.ads.nativead.NativeAdView;
import com.thekhaeng.pushdownanim.PushDownAnim;

import java.util.List;

public class AwaitingAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    List<Item> mData;
    LayoutInflater mInflater;
    OnClickListener listener;
    OnMoveListener moveListener;

    public AwaitingAdapter(Context context, List<Item> mData) {
        this.mData = mData;
        this.mInflater = LayoutInflater.from(context);

    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == 0) {
            View view = mInflater.inflate(R.layout.item_awaiting_grid_layout, parent, false);
            return new AwaitingGridViewHolder(view);
        } else if (viewType == 2) {
            View view = mInflater.inflate(R.layout.item_title_layout, parent, false);
            return new TextViewHolder(view);
        }else if(viewType == 3){
            View view = mInflater.inflate(R.layout.item_awaiting_grid_layout, parent, false);
            return new AwaitingGridViewHolder(view);
        } else if(viewType == 4){
            View view = mInflater.inflate(R.layout.item_task_layout, parent, false);
            return new TaskViewHolder(view);
        } else if(viewType == 5){
            View view = mInflater.inflate(R.layout.item_new_task_layout, parent, false);
            return new NewTaskViewHolder(view);
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
            ((AwaitingGridViewHolder) holder).onBindData(element);
        } else if (type == 2) {
            TextElement element = (TextElement) mData.get(position).getObject();
            ((TextViewHolder) holder).onBindData(element);
        } else if (type == 3) {
            AwaitElement element = (AwaitElement) mData.get(position).getObject();
            ((AwaitingGridViewHolder) holder).onBindData(element);
        } else if(type == 4){
            AwaitElement element = (AwaitElement) mData.get(position).getObject();
            ((TaskViewHolder) holder).onBindData(element);
        } else if(type == 5){

        } else if(type == 12){
            AnnouncesElement element = (AnnouncesElement) mData.get(position).getObject();
            ((AwaitingAdViewHolder) holder).onBindData(element);
        }
    }

    @Override
    public int getItemCount() {
        return mData.size();
    }

    public void setOnClickListener(@Nullable OnClickListener listener) {
        this.listener = listener;
    }
    public void setOnMoveListener(@Nullable OnMoveListener listener) {
        this.moveListener = listener;
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

    class AwaitingGridViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
        TextView title, status, date, time, subject, project;
        ImageView arrowLeft, arrowRight;

        public AwaitingGridViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.title_card);
            subject = itemView.findViewById(R.id.subject);
            project = itemView.findViewById(R.id.project_name);
            status = itemView.findViewById(R.id.status_card);
            date = itemView.findViewById(R.id.date_card);
            time = itemView.findViewById(R.id.time);
            arrowLeft = itemView.findViewById(R.id.arrow_left);
            arrowRight = itemView.findViewById(R.id.arrow_right);
            if(listener != null){
                PushDownAnim.setPushDownAnimTo(itemView)
                        .setDurationPush(100)
                        .setScale(PushDownAnim.MODE_SCALE, 0.98f)
                        .setOnClickListener(this);
            }
            if(moveListener != null){
                PushDownAnim.setPushDownAnimTo(arrowLeft)
                        .setDurationPush(100)
                        .setScale(PushDownAnim.MODE_SCALE, 0.95f)
                        .setOnClickListener(view -> moveListener.onMoveLeft(view, getLayoutPosition()));
                PushDownAnim.setPushDownAnimTo(arrowRight)
                        .setDurationPush(100)
                        .setScale(PushDownAnim.MODE_SCALE, 0.95f)
                        .setOnClickListener(view -> moveListener.onMoveRight(view, getLayoutPosition()));
            }
        }

        void onBindData(AwaitElement element){
            TypedArray ta = mInflater.getContext().obtainStyledAttributes(R.styleable.AppCustomAttrs);
            int color = ta.getColor(R.styleable.AppCustomAttrs_backgroundBorder, Color.WHITE);
            int colorSub = ta.getColor(R.styleable.AppCustomAttrs_subTextColor, Color.WHITE);
            title.setText(element.getTitle());
            title.setPaintFlags(title.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
            title.setTextColor(color);

            status.setText(element.getStatus());
            status.setBackgroundColor(element.getStatusColor());

            if(element.isDone()){
                title.setPaintFlags(title.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                title.setTextColor(colorSub);
            }
            if(element.isMine()){
                arrowRight.setVisibility(View.VISIBLE);
                arrowLeft.setVisibility(View.VISIBLE);
            }else{
                arrowRight.setVisibility(View.GONE);
                arrowLeft.setVisibility(View.GONE);
            }

            if(element.getProjectName() != null)
                project.setText(element.getProjectName());

            subject.setTextColor(element.getTaskColor());
            subject.setText(element.getSubject());
            date.setText(element.getDate());
            time.setText(element.getTime());
            ta.recycle();
        }

        @Override
        public void onClick(View view) {
            listener.onClick(view, getLayoutPosition());
        }
    }

    class AwaitingAdViewHolder extends RecyclerView.ViewHolder{

        TextView title, body, announser, price, action;
        ImageView icon;
        MediaView content;
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

            content.setMediaContent(element.getContent());
            content.setImageScaleType(ImageView.ScaleType.CENTER_CROP);

            adView.setHeadlineView(title);
            adView.setAdvertiserView(announser);
            adView.setBodyView(body);
            adView.setIconView(icon);
            adView.setPriceView(price);
            adView.setCallToActionView(action);
            adView.setMediaView(content);

            adView.setNativeAd(element.getNativeAd());
        }
    }

    class TaskViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{

        TextView title, status, date, time, subject, inProcess;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.title_card);
            subject = itemView.findViewById(R.id.subject);
            status = itemView.findViewById(R.id.status_card);
            date = itemView.findViewById(R.id.date_card);
            time = itemView.findViewById(R.id.time);
            inProcess = itemView.findViewById(R.id.in_process);

            PushDownAnim.setPushDownAnimTo(itemView)
                    .setDurationPush(100)
                    .setScale(PushDownAnim.MODE_SCALE, 0.95f)
                    .setOnClickListener(this);
        }

        void onBindData(AwaitElement element){
            TypedArray ta = mInflater.getContext().obtainStyledAttributes(R.styleable.AppCustomAttrs);
            int color = ta.getColor(R.styleable.AppCustomAttrs_backgroundBorder, Color.WHITE);
            int colorSub = ta.getColor(R.styleable.AppCustomAttrs_subTextColor, Color.WHITE);
            title.setText(element.getTitle());
            title.setPaintFlags(title.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
            title.setTextColor(color);
            inProcess.setVisibility(View.GONE);

            status.setText(element.getStatus());
            status.setBackgroundColor(element.getStatusColor());

            if(element.isDone()){
                title.setPaintFlags(title.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                title.setTextColor(colorSub);
                status.setText(mInflater.getContext().getString(R.string.done_string));
            }

            if(element.isMine()){
                inProcess.setVisibility(View.VISIBLE);
            }

            subject.setTextColor(element.getTaskColor());
            subject.setText(element.getSubject());
            date.setText(element.getDate());
            time.setText(element.getTime());
            ta.recycle();
        }

        @Override
        public void onClick(View view) {
            listener.onClick(view, getLayoutPosition());
        }
    }

    class NewTaskViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{

        public NewTaskViewHolder(@NonNull View itemView) {
            super(itemView);
            PushDownAnim.setPushDownAnimTo(itemView)
                    .setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            listener.onClick(view, getLayoutPosition());
        }
    }


    public interface OnLongClickListener{
        boolean onLongClick(View view, int position);
    }

    public interface OnClickListener {
        void onClick(View view, int position);
    }

    public interface OnMoveListener{
        void onMoveLeft(View view, int position);
        void onMoveRight(View view, int position);
    }
}

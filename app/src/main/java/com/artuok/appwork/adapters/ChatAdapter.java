package com.artuok.appwork.adapters;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.recyclerview.widget.RecyclerView;

import com.artuok.appwork.R;
import com.artuok.appwork.objects.ChatElement;
import com.artuok.appwork.objects.Item;
import com.thekhaeng.pushdownanim.PushDownAnim;

import java.util.Calendar;
import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    List<Item> mData;
    LayoutInflater mInflater;
    OnChatClickListener listener;

    public ChatAdapter(Context context, List<Item> mData, OnChatClickListener listener) {
        this.mData = mData;
        this.mInflater = LayoutInflater.from(context);
        this.listener = listener;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == 0) {
            View view = mInflater.inflate(R.layout.item_chat_layout, parent, false);
            return new ChatViewHolder(view);
        }else if (viewType == 1) {
            View view = mInflater.inflate(R.layout.item_group_chat_layout, parent, false);
            return new GroupViewHolder(view);
        }else if(viewType == 2){
            View view = mInflater.inflate(R.layout.item_add_group_chat_layout, parent, false);
            return new AddGroupViewHolder(view);
        }

        return null;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {

        int viewType = getItemViewType(position);
        if (viewType == 0) {
            ChatElement element = (ChatElement) mData.get(position).getObject();
            ((ChatViewHolder) holder).onBindData(element);
        }else if(viewType == 1){
            ChatElement element = (ChatElement) mData.get(position).getObject();
            ((GroupViewHolder) holder).onBindData(element);
        }else if(viewType == 2){
            ChatElement element = (ChatElement) mData.get(position).getObject();
            ((AddGroupViewHolder) holder).onBindData(element);
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

    class ChatViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        TextView name, content, inviteBtn, time;
        ImageView perfilPhoto, statusIcon, contentIcon;

        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.chat_name);
            content = itemView.findViewById(R.id.chat_content);
            inviteBtn = itemView.findViewById(R.id.inviteBtn);
            statusIcon = itemView.findViewById(R.id.status_icon);
            contentIcon = itemView.findViewById(R.id.content_icon);
            time = itemView.findViewById(R.id.timestamp);
            PushDownAnim.setPushDownAnimTo(inviteBtn)
                    .setDurationPush(100)
                    .setScale(PushDownAnim.MODE_SCALE, 0.98f);

            itemView.setOnClickListener(this);
        }

        void onBindData(ChatElement element) {
            inviteBtn.setVisibility(View.GONE);
            String chat_name = element.getName();
            String chat_number = element.getDesc();
            name.setText(chat_name);
            content.setText(chat_number);

            Calendar c = Calendar.getInstance();
            c.setTimeInMillis(element.getTimestamp());
            int h = c.get(Calendar.HOUR_OF_DAY);
            int m = c.get(Calendar.MINUTE);
            String minute = m < 10 ? "0" + m : "" + m;
            String tm = c.get(Calendar.AM_PM) == Calendar.AM ? "a. m." : "p. m.";

            String hour = (h % 12) + "";
            hour = (h == 12) ? "12" : hour;

            String timestamp = hour + ":" + minute + " " + tm;

            if (!element.isLog()) {
                inviteBtn.setVisibility(View.VISIBLE);
            }else{
                inviteBtn.setVisibility(View.GONE);
            }

            if(element.getTimestamp() != 0){
                time.setVisibility(View.VISIBLE);
            }else{
                time.setVisibility(View.GONE);
            }

            if(element.getContentIcon() != null){
                contentIcon.setVisibility(View.VISIBLE);
                contentIcon.setImageDrawable(element.getContentIcon());
            }else{
                contentIcon.setVisibility(View.GONE);
            }

            if (element.getStatus() > -1){
                statusIcon.setVisibility(View.VISIBLE);
                int status = element.getStatus();
                Drawable state = AppCompatResources.getDrawable(mInflater.getContext(), R.drawable.ic_clock);
                if(status == 1){
                    state = AppCompatResources.getDrawable(mInflater.getContext(), R.drawable.check_sended);
                }else if(status == 2){
                    state = AppCompatResources.getDrawable(mInflater.getContext(), R.drawable.ic_check_circle);
                }else if(status == 3){
                    state = AppCompatResources.getDrawable(mInflater.getContext(), R.drawable.ic_check_circle);
                    statusIcon.setColorFilter(mInflater.getContext().getColor(R.color.blue_400));
                }

                statusIcon.setImageDrawable(state);
            }else{
                statusIcon.setVisibility(View.GONE);
            }

            time.setText(timestamp);
        }

        @Override
        public void onClick(View view) {
            listener.onClick(view, getLayoutPosition());
        }
    }

    static class GroupViewHolder extends RecyclerView.ViewHolder{

        TextView name, time, content;
        ImageView image;
        public GroupViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.chat_name);
            time = itemView.findViewById(R.id.timestamp);
            content = itemView.findViewById(R.id.chat_content);
            image = itemView.findViewById(R.id.chat_icon);
        }

        public void onBindData(ChatElement element) {
            if(element.getImage() != null)
                image.setImageBitmap(element.getImage());

            name.setText(element.getName());
            content.setText(element.getDesc());
            Calendar c = Calendar.getInstance();
            c.setTimeInMillis(element.getTimestamp());
            int h = c.get(Calendar.HOUR_OF_DAY);
            int m = c.get(Calendar.MINUTE);
            String minute = m < 10 ? "0" + m : "" + m;
            String tm = c.get(Calendar.AM_PM) == Calendar.AM ? "a. m." : "p. m.";

            String hour = (h % 12) + "";
            hour = (h == 12) ? "12" : hour;
            String timestamp = hour + ":" + minute + " " + tm;
            time.setText(timestamp);
        }
    }

    static class AddGroupViewHolder extends RecyclerView.ViewHolder{

        TextView name;
        ImageView image;
        public AddGroupViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.buttontext);
            image = itemView.findViewById(R.id.buttonicon);
        }

        public void onBindData(ChatElement element) {
            if(element.getDesc() != null && !element.getDesc().isEmpty())
                image.setImageBitmap(element.getImage());

            name.setText(element.getName());
        }
    }

    public interface OnChatClickListener {
        void onClick(View view, int pos);
    }
}

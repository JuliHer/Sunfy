package com.artuok.appwork.adapters;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.media.Image;
import android.text.format.DateFormat;
import android.util.Log;
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

import org.w3c.dom.Text;

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
        }else if (viewType == 2) {
            View view = mInflater.inflate(R.layout.item_add_group_chat_layout, parent, false);
            return new AddGroupViewHolder(view);
        } else if (viewType == 3) {
            View view = mInflater.inflate(R.layout.item_user_select, parent, false);
            return new ChatSelectViewHolder(view);
        } else if (viewType == 4) {
            View view = mInflater.inflate(R.layout.item_share_layout, parent, false);
            return new ShareViewHolder(view);
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
        }else if (viewType == 2) {
            ChatElement element = (ChatElement) mData.get(position).getObject();
            ((AddGroupViewHolder) holder).onBindData(element);
        } else if (viewType == 3) {
            ChatElement element = (ChatElement) mData.get(position).getObject();
            ((ChatSelectViewHolder) holder).onBindData(element);
        } else if (viewType == 4) {
            ChatElement element = (ChatElement) mData.get(position).getObject();
            ((ShareViewHolder) holder).onBindData(element);
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

        TextView name, content, time;
        ImageView perfilPhoto, statusIcon, contentIcon;

        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.chat_name);
            content = itemView.findViewById(R.id.chat_content);
            statusIcon = itemView.findViewById(R.id.status_icon);
            contentIcon = itemView.findViewById(R.id.content_icon);
            time = itemView.findViewById(R.id.timestamp);
            perfilPhoto = itemView.findViewById(R.id.usericon);

            itemView.setOnClickListener(this);
        }

        void onBindData(ChatElement element) {
            String chat_name = element.getName();
            String chat_number = element.getDesc();
            name.setText(chat_name);
            content.setText(chat_number);

            if (element.getPicture() != null)
                perfilPhoto.setImageBitmap(element.getPicture());

            Calendar c = Calendar.getInstance();
            c.setTimeInMillis(element.getTimestamp());
            int h = c.get(Calendar.HOUR_OF_DAY);
            int m = c.get(Calendar.MINUTE);
            String minute = m < 10 ? "0" + m : "" + m;
            String tm = c.get(Calendar.AM_PM) == Calendar.AM ? "a. m." : "p. m.";

            String hour = (h % 12) + "";
            boolean hourFormat = DateFormat.is24HourFormat(mInflater.getContext());
            String timestamp = "";
            if (!hourFormat) {
                hour = (h == 12) ? "12" : hour;
                timestamp = hour + ":" + minute + " " + tm;
            } else {
                hour = h + "";
                timestamp = hour + ":" + minute;
            }

            if (element.getTimestamp() != 0) {
                time.setVisibility(View.VISIBLE);
            }else{
                time.setVisibility(View.GONE);
            }



            contentIcon.setVisibility(View.GONE);
            if (element.getStatus() > -1){
                statusIcon.setVisibility(View.VISIBLE);
                int status = element.getStatus();
                Drawable state = AppCompatResources.getDrawable(mInflater.getContext(), R.drawable.ic_clock);
                if(status == 1){
                    state = AppCompatResources.getDrawable(mInflater.getContext(), R.drawable.ic_check);
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

    class ChatSelectViewHolder extends RecyclerView.ViewHolder {
        TextView name;
        ImageView image, check;


        public ChatSelectViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.chat_name);
            image = itemView.findViewById(R.id.image);
            check = itemView.findViewById(R.id.check);
            itemView.setOnClickListener(view -> {
                listener.onClick(view, getLayoutPosition());
            });
        }

        void onBindData(ChatElement element) {
            name.setText(element.getName());


        }
    }

    class GroupViewHolder extends RecyclerView.ViewHolder {

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
            if(element.getPicture() != null)
                image.setImageBitmap(element.getPicture());

            name.setText(element.getName());
            content.setText(element.getDesc());
            Calendar c = Calendar.getInstance();
            c.setTimeInMillis(element.getTimestamp());
            int h = c.get(Calendar.HOUR_OF_DAY);
            int m = c.get(Calendar.MINUTE);
            String minute = m < 10 ? "0" + m : "" + m;
            String tm = c.get(Calendar.AM_PM) == Calendar.AM ? "a. m." : "p. m.";

            String hour = (h % 12) + "";
            boolean hourFormat = DateFormat.is24HourFormat(mInflater.getContext());
            String timestamp = "";
            if (!hourFormat) {
                hour = (h == 12) ? "12" : hour;
                timestamp = hour + ":" + minute + " " + tm;
            } else {
                hour = h + "";
                timestamp = hour + ":" + minute;
            }

            time.setText(timestamp);
        }
    }

    class AddGroupViewHolder extends RecyclerView.ViewHolder {

        TextView name;
        ImageView image;

        public AddGroupViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.buttontext);
            image = itemView.findViewById(R.id.buttonicon);
            itemView.setOnClickListener(view -> listener.onClick(view, getLayoutPosition()));
        }

        public void onBindData(ChatElement element) {
            if(element.getDesc() != null && !element.getDesc().isEmpty())
                image.setImageBitmap(element.getPicture());

            name.setText(element.getName());
        }
    }

    class ShareViewHolder extends RecyclerView.ViewHolder{
        ImageView userIcon;
        TextView name;
        View total;
        public ShareViewHolder(@NonNull View itemView) {
            super(itemView);
            userIcon = itemView.findViewById(R.id.usericon);
            name = itemView.findViewById(R.id.username);
            total = itemView;
            PushDownAnim.setPushDownAnimTo(itemView)
                    .setOnClickListener(view -> listener.onClick(view, getLayoutPosition()));
        }

        void onBindData(ChatElement element){
            if(element.getPicture() != null)
                userIcon.setImageBitmap(element.getPicture());
            name.setText(element.getName());
        }
    }

    public interface OnChatClickListener {
        void onClick(View view, int pos);
    }
}

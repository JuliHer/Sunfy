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
import com.artuok.appwork.objects.ChatElement;
import com.artuok.appwork.objects.Item;
import com.thekhaeng.pushdownanim.PushDownAnim;

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
        }

        return null;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {

        int viewType = getItemViewType(position);
        if (viewType == 0) {
            ChatElement element = (ChatElement) mData.get(position).getObject();
            ((ChatViewHolder) holder).onBindData(element);
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

        TextView name, content;
        ImageView perfilPhoto;

        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.chat_name);
            content = itemView.findViewById(R.id.chat_content);
            PushDownAnim.setPushDownAnimTo(itemView)
                    .setDurationPush(100)
                    .setScale(PushDownAnim.MODE_SCALE, 0.98f)
                    .setOnClickListener(this);
        }

        void onBindData(ChatElement element) {
            String chat_name = element.getName();
            String chat_number = element.getNumber();
            name.setText(chat_name);
            content.setText(chat_number);
        }

        @Override
        public void onClick(View view) {
            listener.onClick(view, getLayoutPosition());
        }
    }

    public interface OnChatClickListener {
        void onClick(View view, int pos);
    }
}

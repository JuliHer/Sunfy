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

public class ShareAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    LayoutInflater mInfater;
    List<Item> mData;
    OnClickListener listener;

    public ShareAdapter(Context context, List<Item> mData) {
        this.mInfater = LayoutInflater.from(context);
        this.mData = mData;
    }

    public void setOnClickListener(OnClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if(viewType == 0){
            View view = mInfater.inflate(R.layout.item_share_layout, parent, false);

            return new ShareViewHolder(view);
        }
        return null;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        int viewType = getItemViewType(position);
        if(viewType == 0){
            ChatElement element = (ChatElement) mData.get(position).getObject();
            ((ShareViewHolder)holder).onBindData(element);
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

    class ShareViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        TextView username;
        ImageView usericon;
        public ShareViewHolder(@NonNull View itemView) {
            super(itemView);

            username = itemView.findViewById(R.id.username);
            usericon = itemView.findViewById(R.id.usericon);

            if(listener != null){
                PushDownAnim.setPushDownAnimTo(itemView)
                        .setDurationPush(100)
                        .setScale(PushDownAnim.MODE_SCALE, 0.98f)
                        .setOnClickListener(this);
            }
        }

        void onBindData(ChatElement element){
            username.setText(element.getName());
            if(element.getImage() != null){
                usericon.setImageBitmap(element.getImage());
            }
        }

        @Override
        public void onClick(View view) {
            listener.onClick(view, getLayoutPosition());
        }
    }

    public interface OnClickListener{
        void onClick(View view, int pos);
    }
}

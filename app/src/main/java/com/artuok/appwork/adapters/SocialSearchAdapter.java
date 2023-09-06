package com.artuok.appwork.adapters;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
import com.artuok.appwork.objects.UserSearchElement;
import com.google.android.gms.ads.nativead.NativeAdView;
import com.thekhaeng.pushdownanim.PushDownAnim;

import java.util.List;
import java.util.Objects;

public class SocialSearchAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    List<Item> mData;
    LayoutInflater mInflater;
    OnClickListener listener;

    public SocialSearchAdapter(Context context, List<Item> mData) {
        this.mData = mData;
        this.mInflater = LayoutInflater.from(context);
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == 0) {
            View view = mInflater.inflate(R.layout.item_usercard_layout, parent, false);
            return new UserSearchViewHolder(view);
        }
        return null;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        int type = getItemViewType(position);
        if (type == 0) {
            UserSearchElement element = (UserSearchElement) mData.get(position).getObject();
            ((UserSearchViewHolder) holder).onBindData(element);
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

    public void clearResults(){
        mData.clear();
    }

    public void addResult(Item a){
        mData.add(a);
    }

    public void changeImage(int n, Bitmap map){
        ((UserSearchElement)mData.get(n).getObject()).setImage(map);
    }

    public class UserSearchViewHolder extends RecyclerView.ViewHolder{

        ImageView image;
        TextView name, desc, code;

        public UserSearchViewHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.usericon);
            name = itemView.findViewById(R.id.username);
            desc = itemView.findViewById(R.id.aditional_info);
            code = itemView.findViewById(R.id.usercode);

            if(listener != null)
                PushDownAnim.setPushDownAnimTo(itemView)
                    .setDurationPush(100)
                    .setScale(PushDownAnim.MODE_SCALE, 0.98f)
                    .setOnClickListener(view -> listener.onClick(view, getLayoutPosition()));
        }

        public void onBindData(UserSearchElement element){

            if(element.getImage() != null)
                image.setImageBitmap(element.getImage());
            else{
                Bitmap map = BitmapFactory.decodeResource(mInflater.getContext().getResources(), R.mipmap.usericon);
                image.setImageBitmap(map);
            }
            name.setText(element.getName());

            desc.setVisibility(View.VISIBLE);

            desc.setText("");
            if(element.getDesc().isEmpty())
                desc.setVisibility(View.GONE);
            desc.setText(element.getDesc());
            code.setText("#"+element.getCode());
        }
    }

    public interface OnClickListener {
        void onClick(View view, int position);
    }
}

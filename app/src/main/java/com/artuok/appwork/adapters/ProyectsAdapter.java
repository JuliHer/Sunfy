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
import com.artuok.appwork.objects.Item;
import com.artuok.appwork.objects.ProyectElement;
import com.artuok.appwork.objects.ProyectsElement;
import com.thekhaeng.pushdownanim.PushDownAnim;

import java.util.ArrayList;
import java.util.List;

public class ProyectsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private Context mContext;
    private LayoutInflater mInflater;
    private List<Item> mData;
    private ProyectsElement.OnProyectOpenListener listener;

    public ProyectsAdapter(Context context, List<Item> data){
        this.mContext = context;
        this.mData = data;
        this.mInflater = LayoutInflater.from(context);
    }

    public ProyectsAdapter(Context context, List<Item> data, ProyectsElement.OnProyectOpenListener listener){
        this.mContext = context;
        this.mData = data;
        this.mInflater = LayoutInflater.from(context);
        this.listener = listener;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == 0) {
            View view = mInflater.inflate(R.layout.item_proyect_layout, parent, false);
            return new ProyectViewHolder(view);
        }
        return null;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        int viewType = getItemViewType(position);
        if (viewType == 0) {
            ProyectElement element = (ProyectElement) mData.get(position).getObject();
            ((ProyectViewHolder) holder).onBindData(element);
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

    private class ProyectViewHolder extends RecyclerView.ViewHolder{
        ImageView imageView;
        TextView name;
        public ProyectViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.icon);
            name = itemView.findViewById(R.id.name);
            if(listener != null)
                PushDownAnim.setPushDownAnimTo(itemView)
                        .setOnClickListener(view -> listener.onProyectOpen(view, getLayoutPosition()));
        }

        void onBindData(ProyectElement element){
            if(element.getImage() != null)
                imageView.setImageBitmap(element.getImage());
            name.setText(element.getName());
        }
    }
}

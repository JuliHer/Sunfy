package com.artuok.appwork.adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.artuok.appwork.R;

import java.util.List;

public class ImageAdapter extends RecyclerView.Adapter<ImageAdapter.ImageViewHolder> {
    private List<String> mData;
    private LayoutInflater inflater;
    private OnDeleteListener deleteListener;

    public ImageAdapter(Context context, List<String> data, OnDeleteListener deleteListener) {
        this.inflater = LayoutInflater.from(context);
        this.mData = data;
        this.deleteListener = deleteListener;
    }

    @NonNull
    @Override
    public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        if(viewType == 0){
            View view = inflater.inflate(R.layout.item_imagecaptured_layout, parent, false);
            return new ImageViewHolder(view);
        }
        return null;
    }

    @Override
    public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
        holder.onBindData(mData.get(position));
    }

    @Override
    public int getItemCount() {
        return mData.size();
    }


    class ImageViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{

        ImageView img, delete;
        public ImageViewHolder(@NonNull View itemView) {
            super(itemView);
            img = itemView.findViewById(R.id.imageView);
            delete = itemView.findViewById(R.id.delete);

            delete.setOnClickListener(this);
        }

        public void onBindData(String s) {
            byte [] encodeByte = Base64.decode(s,Base64.DEFAULT);

            try {
            Bitmap map = BitmapFactory.decodeByteArray(encodeByte, 0, encodeByte.length);
                img.setImageBitmap(map);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onClick(View view) {
            deleteListener.onDelete(view, getLayoutPosition());
        }
    }


    public interface OnDeleteListener{
        void onDelete(View view, int pos);
    }
}

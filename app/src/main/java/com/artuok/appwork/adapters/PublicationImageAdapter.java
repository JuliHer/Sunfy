package com.artuok.appwork.adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.artuok.appwork.R;
import com.artuok.appwork.objects.PublicationImageElement;

import java.util.List;

public class PublicationImageAdapter extends RecyclerView.Adapter<PublicationImageAdapter.PublicationImageViewHolder>{
    private List<PublicationImageElement> mData;
    private LayoutInflater inflater;
    private OnClickListener clickListener;
    private int size;

    public PublicationImageAdapter(Context context, List<PublicationImageElement> data, OnClickListener deleteListener) {
        this.inflater = LayoutInflater.from(context);
        this.mData = data;
        this.clickListener = deleteListener;
    }

    @NonNull
    @Override
    public PublicationImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        if(viewType == 0){
            View view = inflater.inflate(R.layout.item_image_publication_layout, parent, false);
            return new PublicationImageViewHolder(view);
        }
        return null;
    }

    @Override
    public void onBindViewHolder(@NonNull PublicationImageViewHolder holder, int position) {
        holder.onBindData(mData.get(position));
    }

    @Override
    public int getItemCount() {
        return mData.size();
    }


    class PublicationImageViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{

        ImageView img;
        LinearLayout plus;
        public PublicationImageViewHolder(@NonNull View itemView) {
            super(itemView);
            img = itemView.findViewById(R.id.images);
            plus = itemView.findViewById(R.id.plus);
        }

        public void onBindData(PublicationImageElement element) {

            Bitmap map = element.getMap();

            Drawable t = inflater.getContext().getDrawable(R.drawable.ic_trash);
            t.setColorFilter(inflater.getContext().getColor(R.color.gray_500), PorterDuff.Mode.MULTIPLY);
            Drawable c = inflater.getContext().getDrawable(R.drawable.ic_trash);
            c.setColorFilter(inflater.getContext().getColor(R.color.gray_500), PorterDuff.Mode.MULTIPLY);

            if(element.isLast()){
                plus.setVisibility(View.VISIBLE);
            }else{
                plus.setVisibility(View.GONE);
            }

            img.setImageBitmap(map);
        }

        @Override
        public void onClick(View view) {
            clickListener.onClick(view, getLayoutPosition());
        }
    }


    public interface OnClickListener{
        void onClick(View view, int pos);
    }
}

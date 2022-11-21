package com.artuok.appwork.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.artuok.appwork.R;
import com.artuok.appwork.objects.ColorSelectElement;
import com.thekhaeng.pushdownanim.PushDownAnim;

import java.util.List;

public class ColorSelectAdapter extends RecyclerView.Adapter<ColorSelectAdapter.ViewHolder> {
    List<ColorSelectElement> mData;
    LayoutInflater mInflater;
    OnClickListener listener;

    public ColorSelectAdapter(Context context, List<ColorSelectElement> mData, OnClickListener listener) {
        this.mData = mData;
        this.mInflater = LayoutInflater.from(context);
        this.listener = listener;
    }

    @NonNull
    @Override
    public ColorSelectAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.color_select_layout, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ColorSelectAdapter.ViewHolder holder, int position) {
        holder.onBindData(mData.get(position));
    }

    @Override
    public int getItemCount() {
        return mData.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        LinearLayout color;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            color = itemView.findViewById(R.id.color);
            PushDownAnim.setPushDownAnimTo(itemView)
                    .setDurationPush(100)
                    .setScale(PushDownAnim.MODE_SCALE, 0.95f)
                    .setOnClickListener(this);
        }

        void onBindData(ColorSelectElement element) {
            color.setBackgroundColor(element.getColorVibrant());
        }

        @Override
        public void onClick(View view) {
            listener.onClick(view, getLayoutPosition());
        }
    }

    public interface OnClickListener {
        void onClick(View view, int position);
    }
}

package com.artuok.appwork.adapters;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.artuok.appwork.R;
import com.artuok.appwork.objects.AverageElement;
import com.artuok.appwork.objects.Item;

import java.util.List;

public class AverageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    List<Item> mData;
    LayoutInflater mInflater;

    public AverageAdapter(Context context, List<Item> mData) {
        this.mData = mData;
        this.mInflater = LayoutInflater.from(context);
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == 0) {
            View view = mInflater.inflate(R.layout.item_average_layout, parent, false);

            return new AverageViewHolder(view);
        }
        return null;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (getItemViewType(position) == 0) {
            AverageElement element = (AverageElement) mData.get(position).getObject();
            ((AverageViewHolder) holder).onBindData(element);
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

    class AverageViewHolder extends RecyclerView.ViewHolder {
        TextView title;
        ProgressBar progressBar;
        CardView card;
        ImageView imageView;

        public AverageViewHolder(@NonNull View itemView) {
            super(itemView);

            title = itemView.findViewById(R.id.title_subject);
            progressBar = itemView.findViewById(R.id.progress_circular);
            card = itemView.findViewById(R.id.cardColor);
            imageView = itemView.findViewById(R.id.status_subject);

        }

        void onBindData(AverageElement element) {
            title.setText(element.getSubject());
            card.setCardBackgroundColor(element.getColor());

            if (element.getMax() == 0) {
                progressBar.setProgressDrawable(mInflater.getContext().getDrawable(R.drawable.circle_progress_green));
                progressBar.setMax(1);
                progressBar.setProgress(1);
                imageView.setImageDrawable(mInflater.getContext().getDrawable(R.drawable.ic_check_circle));
                imageView.setColorFilter(mInflater.getContext().getColor(R.color.green_500));
            } else {
                Log.d("cattoPercent", element.getProgress() + "/" + element.getMax());

                int progress = 100 / element.getMax() * element.getProgress();

                if (progress < 60) {
                    imageView.setImageDrawable(mInflater.getContext().getDrawable(R.drawable.ic_x_circle));
                    imageView.setColorFilter(mInflater.getContext().getColor(R.color.red_500));
                    progressBar.setProgressDrawable(mInflater.getContext().getDrawable(R.drawable.circle_progress_red));
                } else if (progress < 80) {
                    progressBar.setProgressDrawable(mInflater.getContext().getDrawable(R.drawable.circle_progress_yellow));
                    imageView.setImageDrawable(mInflater.getContext().getDrawable(R.drawable.ic_check_circle));
                    imageView.setColorFilter(mInflater.getContext().getColor(R.color.yellow_700));
                } else {
                    progressBar.setProgressDrawable(mInflater.getContext().getDrawable(R.drawable.circle_progress_green));
                    imageView.setImageDrawable(mInflater.getContext().getDrawable(R.drawable.ic_check_circle));
                    imageView.setColorFilter(mInflater.getContext().getColor(R.color.green_500));
                }

                progressBar.setMax(element.getMax());
                progressBar.setProgress(element.getProgress());
            }
        }
    }
}

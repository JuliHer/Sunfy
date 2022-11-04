package com.artuok.appwork.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
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
        TextView title, status;
        ProgressBar progressBar;

        public AverageViewHolder(@NonNull View itemView) {
            super(itemView);

            title = itemView.findViewById(R.id.subject_average);
            status = itemView.findViewById(R.id.progress_txt);
            progressBar = itemView.findViewById(R.id.progress_bar);
        }

        void onBindData(AverageElement element) {
            title.setText(element.getSubject());
            status.setText(element.getStatus());

            progressBar.setProgress(0, true);
            progressBar.setMax(element.getMax());
            progressBar.setProgress(element.getProgress(), true);


            int p = 0;
            if (element.getMax() != 0) {
                p = 100 / element.getMax() * element.getProgress();
            }
            if (p < 60) {
                progressBar.setProgressDrawable(mInflater.getContext().getDrawable(R.drawable.circle_progress_red));
            } else if (p < 85) {
                progressBar.setProgressDrawable(mInflater.getContext().getDrawable(R.drawable.circle_progress_yellow));
            } else {
                progressBar.setProgressDrawable(mInflater.getContext().getDrawable(R.drawable.circle_progress_green));
            }

        }
    }
}

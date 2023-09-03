package com.artuok.appwork.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.artuok.appwork.R;
import com.artuok.appwork.objects.AverageElement;
import com.artuok.appwork.objects.Item;
import com.artuok.appwork.objects.ItemSubjectElement;
import com.thekhaeng.pushdownanim.PushDownAnim;

import java.util.List;

public class AverageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    List<Item> mData;
    LayoutInflater mInflater;
    SubjectAdapter.SubjectClickListener listener;

    public AverageAdapter(Context context, List<Item> mData) {
        this.mData = mData;
        this.mInflater = LayoutInflater.from(context);
    }

    public AverageAdapter(Context context, List<Item> mData, SubjectAdapter.SubjectClickListener listener) {
        this.mData = mData;
        this.mInflater = LayoutInflater.from(context);
        this.listener = listener;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == 0) {
            View view = mInflater.inflate(R.layout.item_average_layout, parent, false);

            return new AverageViewHolder(view);
        }else if (viewType == 1){
            View view = mInflater.inflate(R.layout.button_subject_layout, parent, false);
            return new ButtonViewHolder(view);
        }
        return null;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (getItemViewType(position) == 0) {
            AverageElement element = (AverageElement) mData.get(position).getObject();
            ((AverageViewHolder) holder).onBindData(element);
        } else if (getItemViewType(position) == 1) {
            SubjectAdapter.SubjectClickListener listener = ((ItemSubjectElement)mData.get(position).getObject()).getListener();
            ((ButtonViewHolder) holder).onBindData(listener);
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

    class ButtonViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        SubjectAdapter.SubjectClickListener listenert;

        public ButtonViewHolder(@NonNull View itemView) {
            super(itemView);

            PushDownAnim.setPushDownAnimTo(itemView)
                    .setScale(PushDownAnim.MODE_SCALE, 0.95f)
                    .setDurationPush(100)
                    .setOnClickListener(this);
        }

        void onBindData(SubjectAdapter.SubjectClickListener listener) {
            this.listenert = listener;
        }

        @Override
        public void onClick(View view) {
            listenert.onClick(view, getLayoutPosition());
        }
    }

    class AverageViewHolder extends RecyclerView.ViewHolder {
        TextView title;
        ProgressBar progressBar;
        ImageView imageView, bookmark;

        public AverageViewHolder(@NonNull View itemView) {
            super(itemView);

            title = itemView.findViewById(R.id.title_subject);
            progressBar = itemView.findViewById(R.id.progress_circular);
            bookmark = itemView.findViewById(R.id.bookmark);
            imageView = itemView.findViewById(R.id.status_subject);
            PushDownAnim.setPushDownAnimTo(itemView)
                    .setScale(PushDownAnim.MODE_SCALE, 0.98f)
                    .setDurationPush(100)
                    .setOnLongClickListener(view -> {
                        listener.onClick(view, getLayoutPosition());
                        return true;
                    });
        }

        void onBindData(AverageElement element) {
            title.setText(element.getSubject());
            bookmark.setColorFilter(element.getColor());

            if (element.getMax() == 0) {
                progressBar.setProgressDrawable(mInflater.getContext().getDrawable(R.drawable.circle_progress_green));
                progressBar.setMax(1);
                progressBar.setProgress(1);
                imageView.setImageDrawable(mInflater.getContext().getDrawable(R.drawable.ic_check_circle));
                imageView.setColorFilter(mInflater.getContext().getColor(R.color.green_500));
            } else {

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

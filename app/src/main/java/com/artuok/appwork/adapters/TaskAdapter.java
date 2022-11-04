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
import com.artuok.appwork.objects.TaskElement;

import java.util.Calendar;
import java.util.List;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {

    LayoutInflater mInflater;
    List<TaskElement> mData;

    public TaskAdapter(Context context, List<TaskElement> mData) {
        this.mInflater = LayoutInflater.from(context);
        this.mData = mData;
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.task_item_layout, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        holder.onBindData(mData.get(position));
    }

    @Override
    public int getItemCount() {
        return mData.size();
    }

    public class TaskViewHolder extends RecyclerView.ViewHolder {

        TextView title, date;
        ImageView status;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.title);
            date = itemView.findViewById(R.id.date);
            status = itemView.findViewById(R.id.status_item_task);
        }

        void onBindData(TaskElement element) {
            title.setText(element.getTitle());
            date.setText(element.getDate());


            Calendar c = Calendar.getInstance();

            long timeIM = element.getMillisSeconds();

            if (element.isCheck()) {
                status.setColorFilter(mInflater.getContext().getColor(R.color.blue_400));
            } else {
                if (c.getTimeInMillis() < timeIM) {

                    status.setColorFilter(mInflater.getContext().getColor(R.color.green_500));
                } else {
                    status.setColorFilter(mInflater.getContext().getColor(R.color.red_500));
                }

            }

        }
    }
}

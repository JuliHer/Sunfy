package com.artuok.appwork.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.artuok.appwork.R;
import com.artuok.appwork.objects.CountElement;
import com.artuok.appwork.objects.Item;
import com.artuok.appwork.objects.TasksElement;
import com.thekhaeng.pushdownanim.PushDownAnim;

import java.util.List;

public class TasksAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    LayoutInflater mInflater;
    List<Item> mData;
    OnRecyclerListener listener;
    OnAddEventListener aListener;

    public TasksAdapter(Context context, List<Item> mData, OnRecyclerListener listener) {
        mInflater = LayoutInflater.from(context);
        this.mData = mData;
        this.listener = listener;
    }

    public void setAddEventListener(OnAddEventListener aListener) {
        this.aListener = aListener;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == 0) {
            View view = mInflater.inflate(R.layout.item_tasks_layout, parent, false);
            return new TasksViewHolder(view);
        } else if (viewType == 1) {
            View view = mInflater.inflate(R.layout.item_resume_layout, parent, false);
            return new ResumeViewHolder(view);
        }

        return null;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        int view = getItemViewType(position);
        if (view == 0) {
            TasksElement element = (TasksElement) mData.get(position).getObject();
            ((TasksViewHolder) holder).onBindData(element);
        } else if (view == 1) {
            CountElement element = (CountElement) mData.get(position).getObject();
            ((ResumeViewHolder) holder).onBindData(element);
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

    class ResumeViewHolder extends RecyclerView.ViewHolder {

        TextView count;
        TextView txt;

        public ResumeViewHolder(@NonNull View itemView) {
            super(itemView);

            count = itemView.findViewById(R.id.task_count);
            txt = itemView.findViewById(R.id.pendingTaskTxt);
        }

        void onBindData(CountElement element) {
            count.setText(element.getCount());

            txt.setVisibility(View.VISIBLE);
            if (!element.getText().equals("") && !element.getText().isEmpty()) {
                txt.setText(element.getText());
            } else {
                txt.setVisibility(View.GONE);
            }
        }


    }

    class TasksViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        RecyclerView recyclerView;
        TextView date_title, date_txt, date;
        CardView display_card;
        LinearLayout linearLayout, addTask;

        public TasksViewHolder(@NonNull View itemView) {
            super(itemView);
            recyclerView = itemView.findViewById(R.id.tasks_recycler);
            recyclerView.setHasFixedSize(true);
            LinearLayoutManager manager = new LinearLayoutManager(mInflater.getContext(), RecyclerView.VERTICAL, false);
            recyclerView.setLayoutManager(manager);
            date_title = itemView.findViewById(R.id.date_title);
            date_txt = itemView.findViewById(R.id.date_txt);
            display_card = itemView.findViewById(R.id.display_card);
            linearLayout = itemView.findViewById(R.id.empty_tasks);
            addTask = itemView.findViewById(R.id.add_task);
            date = itemView.findViewById(R.id.date);
            PushDownAnim.setPushDownAnimTo(addTask)
                    .setScale(PushDownAnim.MODE_SCALE, 0.95f)
                    .setDurationPush(100)
                    .setOnClickListener(view -> {
                        if (aListener != null) {
                            aListener.onClick(view, getLayoutPosition());
                        }
                    });
            PushDownAnim.setPushDownAnimTo(display_card)
                    .setScale(PushDownAnim.MODE_SCALE, 0.98f)
                    .setDurationPush(100)
                    .setOnClickListener(this);
        }

        void onBindData(TasksElement element) {
            display_card.setVisibility(View.VISIBLE);
            date_title.setText(element.getTitle());
            date_txt.setText(element.getDate());
            linearLayout.setVisibility(View.GONE);
            date.setText(element.getTitle());

            if (element.getData().size() == 0 &&
                    element.getDay() != 0 &&
                    element.getDay() != 1) {
                display_card.setVisibility(View.GONE);
                date.setVisibility(View.VISIBLE);
            } else {
                TaskAdapter adapter = new TaskAdapter(mInflater.getContext(), element.getData());
                recyclerView.setAdapter(adapter);
                display_card.setVisibility(View.VISIBLE);
                date.setVisibility(View.GONE);
            }

            if (element.getData().size() == 0) {
                linearLayout.setVisibility(View.VISIBLE);
            }


        }

        @Override
        public void onClick(View view) {
            listener.onClick(view, getLayoutPosition());
        }
    }

    public interface OnAddEventListener {
        void onClick(View view, int pos);
    }

    public interface OnRecyclerListener {
        void onClick(View view, int position);
    }
}

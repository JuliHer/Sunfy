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
import com.artuok.appwork.objects.Item;
import com.artuok.appwork.objects.TasksElement;
import com.thekhaeng.pushdownanim.PushDownAnim;

import java.util.List;

public class TasksAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    LayoutInflater mInflater;
    List<Item> mData;
    OnRecyclerListener listener;

    public TasksAdapter(Context context, List<Item> mData, OnRecyclerListener listener) {
        mInflater = LayoutInflater.from(context);
        this.mData = mData;
        this.listener = listener;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == 0) {
            View view = mInflater.inflate(R.layout.item_tasks_layout, parent, false);
            return new TasksViewHolder(view);
        }

        return null;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (getItemViewType(position) == 0) {
            TasksElement element = (TasksElement) mData.get(position).getObject();
            ((TasksViewHolder) holder).onBindData(element);
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


    class TasksViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        RecyclerView recyclerView;
        TextView date_title, date_txt;
        CardView display_card;
        LinearLayout linearLayout;

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
            PushDownAnim.setPushDownAnimTo(display_card)
                    .setScale(PushDownAnim.MODE_SCALE, 0.98f)
                    .setDurationPush(100)
                    .setOnClickListener(this);
        }

        void onBindData(TasksElement element) {
            date_title.setText(element.getTitle());
            date_txt.setText(element.getDate());

            if (element.getData().size() == 0 &&
                    !element.getTitle().equals(mInflater.getContext().getString(R.string.today)) &&
                    !element.getTitle().equals(mInflater.getContext().getString(R.string.tomorrow))) {
                display_card.setVisibility(View.GONE);

            } else {
                TaskAdapter adapter = new TaskAdapter(mInflater.getContext(), element.getData());
                recyclerView.setAdapter(adapter);
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

    public interface OnRecyclerListener {
        void onClick(View view, int position);
    }
}

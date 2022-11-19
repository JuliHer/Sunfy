package com.artuok.appwork.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.artuok.appwork.R;
import com.artuok.appwork.objects.AwaitingElement;
import com.artuok.appwork.objects.Item;
import com.artuok.appwork.objects.StatisticsElement;
import com.thekhaeng.pushdownanim.PushDownAnim;

import java.util.List;

public class AwaitingAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    List<Item> mData;
    LayoutInflater mInflater;
    OnClickListener listener;

    public AwaitingAdapter(Context context, List<Item> mData) {
        this.mData = mData;
        this.mInflater = LayoutInflater.from(context);

    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == 0) {
            View view = mInflater.inflate(R.layout.item_awaiting_layout, parent, false);

            return new AwaitingViewHolder(view);
        } else if (viewType == 1) {
            View view = mInflater.inflate(R.layout.recurrence_layout, parent, false);
            return new StatisticViewHolder(view);
        }
        return null;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (getItemViewType(position) == 0) {
            AwaitingElement element = (AwaitingElement) mData.get(position).getObject();
            ((AwaitingViewHolder) holder).onBindData(element);
        } else if (getItemViewType(position) == 1) {
            StatisticsElement element = (StatisticsElement) mData.get(position).getObject();
            ((StatisticViewHolder) holder).onBindData(element);
        }
    }

    @Override
    public int getItemCount() {
        return mData.size();
    }

    public void setOnClickListener(OnClickListener listener) {
        this.listener = listener;
    }

    @Override
    public int getItemViewType(int position) {
        return mData.get(position).getType();
    }

    class AwaitingViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        TextView title, description, subject, status, date;

        public AwaitingViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.title_card);
            description = itemView.findViewById(R.id.description_card);
            subject = itemView.findViewById(R.id.subjects_card);
            status = itemView.findViewById(R.id.status_card);
            date = itemView.findViewById(R.id.date_card);
            PushDownAnim.setPushDownAnimTo(itemView)
                    .setDurationPush(100)
                    .setScale(PushDownAnim.MODE_SCALE, 0.98f)
                    .setOnClickListener(this);
        }

        void onBindData(AwaitingElement element) {
            title.setText(element.getSubject());
            String t = element.getTitle().equals("") ? element.getDescription() : element.getTitle() + ": " + element.getDescription();
            description.setText(t);
            subject.setText("");

            String d = element.getStatus() + " " + mInflater.getContext().getString(R.string.day_left);

            if (element.isOpen()) {
                if (Integer.parseInt(element.getStatus()) == 1) {
                    d = mInflater.getContext().getString(R.string.tomorrow);
                } else if (Integer.parseInt(element.getStatus()) == 0) {
                    d = mInflater.getContext().getString(R.string.today);
                }
            }
            status.setText(d);
            date.setText(element.getDate());

            if (element.isStatusB()) {
                status.setText(R.string.done_string);
                status.setBackgroundColor(mInflater.getContext().getColor(R.color.blue_400));
            } else {
                if (element.isOpen()) {
                    if (Integer.parseInt(element.getStatus()) > 2) {
                        status.setBackgroundColor(mInflater.getContext().getColor(R.color.green_500));
                    } else {
                        status.setBackgroundColor(mInflater.getContext().getColor(R.color.yellow_700));
                    }
                    status.setText(d);
                } else {
                    status.setText(R.string.task_is_close);
                    status.setBackgroundColor(mInflater.getContext().getColor(R.color.red_500));
                }
            }
        }

        @Override
        public void onClick(View view) {
            listener.onClick(view, getLayoutPosition());
        }
    }

    class StatisticViewHolder extends RecyclerView.ViewHolder {

        TextView done, onHold, losed;

        public StatisticViewHolder(@NonNull View itemView) {
            super(itemView);
            done = itemView.findViewById(R.id.done_txt);
            onHold = itemView.findViewById(R.id.onHold_txt);
            losed = itemView.findViewById(R.id.losed_txt);
        }

        void onBindData(StatisticsElement element) {
            String hold = mInflater.getContext().getString(R.string.on_hold_string);
            String d = "" + element.getDone();
            done.setText(d);
            String o = hold + ": " + element.getOnHold();
            onHold.setText(o);
            String l = "" + element.getLosed();
            losed.setText(l);
        }
    }

    public interface OnClickListener {
        void onClick(View view, int position);
    }
}

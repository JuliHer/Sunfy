package com.artuok.appwork.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.artuok.appwork.R;
import com.artuok.appwork.library.LineChart;
import com.artuok.appwork.objects.AnnouncesElement;
import com.artuok.appwork.objects.CountElement;
import com.artuok.appwork.objects.Item;
import com.artuok.appwork.objects.LineChartElement;
import com.artuok.appwork.objects.TasksElement;
import com.google.android.gms.ads.nativead.NativeAd;
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
            return new PresentationViewHolder(view);
        } else if (viewType == 2) {
            View view = mInflater.inflate(R.layout.item_weekly_layout, parent, false);
            return new WeeklySummaryViewHolder(view);
        } else if (viewType == 12) {
            View view = mInflater.inflate(R.layout.item_ad_tasks_layout, parent, false);
            return new TasksAdViewHolder(view);
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
            ((PresentationViewHolder) holder).onBindData(element);
        } else if (view == 2) {
            LineChartElement element = (LineChartElement) mData.get(position).getObject();
            ((WeeklySummaryViewHolder) holder).onBindData(element);
        } else if (view == 12) {
            AnnouncesElement element = (AnnouncesElement) mData.get(position).getObject();
            ((TasksAdViewHolder) holder).onBindData(element);
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

    class PresentationViewHolder extends RecyclerView.ViewHolder {

        TextView hello;
        ImageView chat, settings;

        CountElement.OnIconClickListener chatListener;
        CountElement.OnIconClickListener settingsListener;

        public PresentationViewHolder(@NonNull View itemView) {
            super(itemView);

            hello = itemView.findViewById(R.id.task_count);
            chat = itemView.findViewById(R.id.chat_icon);
            settings = itemView.findViewById(R.id.settings_icon);
        }

        void onBindData(CountElement element) {
            hello.setText(element.getText());
            chatListener = element.getChatListener();
            settingsListener = element.getSettingsListener();

            if (!element.isChatVisible()) {
                chat.setVisibility(View.GONE);
            } else {
                chat.setVisibility(View.VISIBLE);
            }
            if (!element.isSettingsVisible()) {
                settings.setVisibility(View.GONE);
            } else {
                settings.setVisibility(View.VISIBLE);
            }


            chat.setOnClickListener(view -> chatListener.onClick(view));
            settings.setOnClickListener(view -> settingsListener.onClick(view));


        }
    }

    class WeeklySummaryViewHolder extends RecyclerView.ViewHolder {
        LineChart lineChart;
        CardView btn;
        LineChartElement.OnClickListener viewMoreListener;

        public WeeklySummaryViewHolder(@NonNull View itemView) {
            super(itemView);
            lineChart = itemView.findViewById(R.id.line_chart);
            btn = itemView.findViewById(R.id.view_more_btn);
        }


        public void onBindData(LineChartElement element) {
            lineChart.setData(element.getData());
            lineChart.invalidate();

            viewMoreListener = element.getViewMore();

            PushDownAnim.setPushDownAnimTo(btn)
                    .setDurationPush(100)
                    .setScale(PushDownAnim.MODE_SCALE, 0.98f)
                    .setOnClickListener(view -> viewMoreListener.onClick(view));
        }
    }

    class TasksViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        RecyclerView recyclerView;
        TextView date_title, date_txt, date;
        CardView display_card;
        LinearLayout linearLayout, addTask, dayNoData;

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
            dayNoData = itemView.findViewById(R.id.day_no_data);
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
                dayNoData.setVisibility(View.VISIBLE);
            } else {
                TaskAdapter adapter = new TaskAdapter(mInflater.getContext(), element.getData());
                recyclerView.setAdapter(adapter);
                display_card.setVisibility(View.VISIBLE);
                dayNoData.setVisibility(View.GONE);
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

    class TasksAdViewHolder extends RecyclerView.ViewHolder {
        TextView title, body, announser, price, cta;
        ImageView content1, content2, content3, icon;
        LinearLayout content;

        public TasksAdViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.title_card);
            body = itemView.findViewById(R.id.body_card);
            announser = itemView.findViewById(R.id.announser_card);
            price = itemView.findViewById(R.id.price_card);
            content1 = itemView.findViewById(R.id.image_content);
            content2 = itemView.findViewById(R.id.content_second);
            content3 = itemView.findViewById(R.id.content_third);
            content = itemView.findViewById(R.id.images_content);
            cta = itemView.findViewById(R.id.call_to_action);
            icon = itemView.findViewById(R.id.icon);
        }

        public void onBindData(AnnouncesElement element) {
            title.setText(element.getTitle());
            body.setText(element.getBody());
            announser.setText(element.getAnnounser());
            price.setText(element.getPrice());
            cta.setText(element.getAction());

            if (element.getIcon() != null)
                icon.setImageDrawable(element.getIcon().getDrawable());

            List<NativeAd.Image> images = element.getImages();
            if (images != null) {
                if (images.size() >= 3) {
                    content1.setImageDrawable(images.get(0).getDrawable());
                    content2.setImageDrawable(images.get(1).getDrawable());
                    content3.setImageDrawable(images.get(2).getDrawable());
                    content.setVisibility(View.VISIBLE);
                    content3.setVisibility(View.VISIBLE);
                } else if (images.size() == 2) {
                    content1.setImageDrawable(images.get(0).getDrawable());
                    content2.setImageDrawable(images.get(1).getDrawable());
                    content3.setVisibility(View.GONE);
                    content.setVisibility(View.VISIBLE);
                } else if (images.size() == 1) {
                    content1.setImageDrawable(images.get(0).getDrawable());
                    content.setVisibility(View.GONE);
                }
            }
        }
    }

    public interface OnAddEventListener {
        void onClick(View view, int pos);
    }

    public interface OnRecyclerListener {
        void onClick(View view, int position);
    }
}

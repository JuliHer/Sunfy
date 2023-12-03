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
import com.artuok.appwork.objects.ProjectsElement;
import com.artuok.appwork.objects.TasksElement;
import com.google.android.gms.ads.nativead.MediaView;
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.gms.ads.nativead.NativeAdView;
import com.thekhaeng.pushdownanim.PushDownAnim;

import java.util.List;
import java.util.Random;

public class TasksAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    LayoutInflater mInflater;
    Context mContext;
    List<Item> mData;
    OnRecyclerListener listener;
    OnAddEventListener aListener;

    public TasksAdapter(Context context, List<Item> mData, OnRecyclerListener listener) {
        mInflater = LayoutInflater.from(context);
        this.mData = mData;
        this.mContext = context;
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
        } else if (viewType == 3) {
            View view = mInflater.inflate(R.layout.item_projects_layout, parent, false);
            return new ProjectsViewHolder(view);
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
        } else if (view == 3) {
            ProjectsElement element = (ProjectsElement) mData.get(position).getObject();
            ((ProjectsViewHolder) holder).onBindData(element);
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
        if(mData.size() > position)
            return mData.get(position).getType();
        else
            return 0;
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

            Random rand = new Random();

            int numero = rand.nextInt(10);

            String tip = "";
            if (numero == 0) {
                tip = mContext.getString(R.string.a_note1);
            } else if (numero == 1) {
                tip = mContext.getString(R.string.a_note2);
            } else if (numero == 2) {
                tip = mContext.getString(R.string.a_note3);
            } else if (numero == 3) {
                tip = mContext.getString(R.string.a_note4);
            } else if (numero == 4) {
                tip = mContext.getString(R.string.a_note5);
            } else if (numero == 5) {
                tip = mContext.getString(R.string.a_note6);
            } else if (numero == 6) {
                tip = mContext.getString(R.string.a_note7);
            } else if (numero == 7) {
                tip = mContext.getString(R.string.a_note8);
            } else if (numero == 8) {
                tip = mContext.getString(R.string.a_note9);
            } else {
                tip = mContext.getString(R.string.a_note10);
            }

            String tiptip = mContext.getString(R.string.tip) + ": " + tip;

            PushDownAnim.setPushDownAnimTo(btn)
                    .setDurationPush(100)
                    .setScale(PushDownAnim.MODE_SCALE, 0.98f)
                    .setOnClickListener(view -> viewMoreListener.onClick(view));
        }
    }

    class TasksViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        RecyclerView recyclerView;
        TextView dateTitle, taskTile;
        LinearLayout linearLayout, add;

        public TasksViewHolder(@NonNull View itemView) {
            super(itemView);
            recyclerView = itemView.findViewById(R.id.tasks_recycler);
            LinearLayoutManager manager = new LinearLayoutManager(mContext, RecyclerView.VERTICAL, false);
            recyclerView.setLayoutManager(manager);

            taskTile = itemView.findViewById(R.id.task_title);
            dateTitle = itemView.findViewById(R.id.date_title);
            linearLayout = itemView.findViewById(R.id.empty_tasks);
            add = itemView.findViewById(R.id.add);
            recyclerView.setOnClickListener(this);
            itemView.setOnClickListener(this);
            PushDownAnim.setPushDownAnimTo(add)
                    .setOnClickListener(view -> aListener.onClick(view, getLayoutPosition()));
        }

        void onBindData(TasksElement element) {
            dateTitle.setText(element.getTitle());
            linearLayout.setVisibility(View.GONE);

            if(!element.getDate().isEmpty()){
                taskTile.setText(element.getDate());
            }else {
                taskTile.setText(mInflater.getContext().getString(R.string.pending_activities));
            }

            if (element.getData().size() == 0){
                linearLayout.setVisibility(View.VISIBLE);
            } else {
                TaskAdapter adapter = new TaskAdapter(mContext, element.getData());
                recyclerView.setAdapter(adapter);
                linearLayout.setVisibility(View.GONE);
            }

            if(element.getDay() == 1){
                add.setVisibility(View.GONE);
            }else {
                add.setVisibility(View.VISIBLE);
            }


        }

        @Override
        public void onClick(View view) {
            listener.onClick(view, getLayoutPosition());
        }
    }

    class TasksAdViewHolder extends RecyclerView.ViewHolder {
        TextView title, body, announser, price, cta;
        ImageView icon;
        MediaView content;
        NativeAdView adView;

        public TasksAdViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.title_card);
            body = itemView.findViewById(R.id.body_card);
            announser = itemView.findViewById(R.id.announser_card);
            price = itemView.findViewById(R.id.price_card);
            content = itemView.findViewById(R.id.image_content);
            cta = itemView.findViewById(R.id.call_to_action);
            icon = itemView.findViewById(R.id.icon);
            adView = itemView.findViewById(R.id.nativeAd);
        }

        public void onBindData(AnnouncesElement element) {
            title.setText(element.getTitle());
            body.setText(element.getBody());
            announser.setText(element.getAnnounser());
            price.setText(element.getPrice());
            cta.setText(element.getAction());

            if (element.getIcon() != null)
                icon.setImageDrawable(element.getIcon().getDrawable());

            content.setMediaContent(element.getContent());
            content.setImageScaleType(ImageView.ScaleType.CENTER_CROP);

            adView.setHeadlineView(title);
            adView.setAdvertiserView(announser);
            adView.setBodyView(body);
            adView.setIconView(icon);
            adView.setPriceView(price);
            adView.setCallToActionView(cta);
            adView.setMediaView(content);
            adView.setNativeAd(element.getNativeAd());

        }
    }

    private class ProjectsViewHolder extends RecyclerView.ViewHolder {
        RecyclerView recyclerView;
        public ProjectsViewHolder(@NonNull View itemView) {
            super(itemView);
            recyclerView = itemView.findViewById(R.id.recycler);
            recyclerView.setLayoutManager(new LinearLayoutManager(mContext, RecyclerView.HORIZONTAL, false));
        }

        public void onBindData(ProjectsElement element){

            ProjectAdapter adapter = new ProjectAdapter(mContext, element.getElements(), element.getListener());


            recyclerView.setAdapter(adapter);
        }
    }

    public interface OnAddEventListener {
        void onClick(View view, int pos);
    }

    public interface OnRecyclerListener {
        void onClick(View view, int position);
    }
}

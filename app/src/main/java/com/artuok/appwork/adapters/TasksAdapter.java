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
import com.artuok.appwork.objects.ProyectsElement;
import com.artuok.appwork.objects.TasksElement;
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.gms.ads.nativead.NativeAdView;
import com.thekhaeng.pushdownanim.PushDownAnim;

import java.util.ArrayList;
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
            View view = mInflater.inflate(R.layout.item_proyects_layout, parent, false);
            return new ProyectsViewHolder(view);
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
            ProyectsElement element = (ProyectsElement) mData.get(position).getObject();
            ((ProyectsViewHolder) holder).onBindData(element);
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
        TextView tipText;

        public WeeklySummaryViewHolder(@NonNull View itemView) {
            super(itemView);
            lineChart = itemView.findViewById(R.id.line_chart);
            btn = itemView.findViewById(R.id.view_more_btn);
            tipText = itemView.findViewById(R.id.tip);
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
            tipText.setText(tiptip);

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
        LinearLayout linearLayout, dayNoData, add;

        public TasksViewHolder(@NonNull View itemView) {
            super(itemView);
            recyclerView = itemView.findViewById(R.id.tasks_recycler);
            recyclerView.setHasFixedSize(true);
            LinearLayoutManager manager = new LinearLayoutManager(mContext, RecyclerView.VERTICAL, false);
            recyclerView.setLayoutManager(manager);
            date_title = itemView.findViewById(R.id.date_title);
            date_txt = itemView.findViewById(R.id.date_txt);
            display_card = itemView.findViewById(R.id.display_card);
            linearLayout = itemView.findViewById(R.id.empty_tasks);
            date = itemView.findViewById(R.id.date);
            add = itemView.findViewById(R.id.add);
            dayNoData = itemView.findViewById(R.id.day_no_data);
            recyclerView.setOnClickListener(this);
            itemView.setOnClickListener(this);
            PushDownAnim.setPushDownAnimTo(add)
                    .setOnClickListener(view -> aListener.onClick(view, getLayoutPosition()));
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
                TaskAdapter adapter = new TaskAdapter(mContext, element.getData());
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
        ImageView content, icon;
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

            List<NativeAd.Image> images = element.getImages();
            if (images != null)
                content.setImageDrawable(images.get(0).getDrawable());

            adView.setHeadlineView(title);
            adView.setAdvertiserView(announser);
            adView.setBodyView(body);
            adView.setIconView(icon);
            adView.setPriceView(price);
            adView.setCallToActionView(cta);
            adView.setImageView(content);
            adView.setNativeAd(element.getNativeAd());

        }
    }

    private class ProyectsViewHolder extends RecyclerView.ViewHolder {
        RecyclerView recyclerView;
        ProyectsAdapter adapter;
        List<Item> proyects = new ArrayList<>();
        public ProyectsViewHolder(@NonNull View itemView) {
            super(itemView);

            recyclerView = itemView.findViewById(R.id.recycler);

        }

        public void onBindData(ProyectsElement element){
            adapter = new ProyectsAdapter(mContext, proyects, element.getListener());

            recyclerView.setLayoutManager(new LinearLayoutManager(mContext, RecyclerView.HORIZONTAL, false));
            proyects.addAll(element.getElements());

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

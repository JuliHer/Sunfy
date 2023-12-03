package com.artuok.appwork.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.compose.material3.CardColors;
import androidx.recyclerview.widget.RecyclerView;

import com.artuok.appwork.R;
import com.artuok.appwork.objects.ItemSubjectElement;
import com.artuok.appwork.objects.SubjectElement;
import com.thekhaeng.pushdownanim.PushDownAnim;

import java.util.List;

public class SubjectAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    List<ItemSubjectElement> mData;
    LayoutInflater mInflater;
    SubjectClickListener listener;

    public SubjectAdapter(Context context, List<ItemSubjectElement> mData) {
        this.mData = mData;
        this.mInflater = LayoutInflater.from(context);
    }

    public SubjectAdapter(Context context, List<ItemSubjectElement> mData, SubjectClickListener listener) {
        this.mData = mData;
        this.mInflater = LayoutInflater.from(context);
        this.listener = listener;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        if (viewType == 0) {
            View view = mInflater.inflate(R.layout.item_subject_layout, parent, false);
            return new SubjectViewHolder(view);
        } else if (viewType == 1) {
            View view = mInflater.inflate(R.layout.button_subject_layout, parent, false);
            return new ButtonViewHolder(view);
        } else if (viewType == 2) {
            View view = mInflater.inflate(R.layout.list_item, parent, false);
            return new SubjectSelectViewHolder(view);
        }

        return null;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (getItemViewType(position) == 0) {
            SubjectElement element = (SubjectElement) mData.get(position).getObject();
            ((SubjectViewHolder) holder).onBindData(element);
        } else if (getItemViewType(position) == 1) {
            SubjectClickListener listener = mData.get(position).getListener();
            ((ButtonViewHolder) holder).onBindData(listener);
        } else if (getItemViewType(position) == 2) {
            SubjectElement element = (SubjectElement) mData.get(position).getObject();
            ((SubjectSelectViewHolder) holder).onBindData(element);
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

    class SubjectViewHolder extends RecyclerView.ViewHolder {

        TextView title;
        TextView desc;
        View backgroud;

        public SubjectViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.subject_name);
            desc = itemView.findViewById(R.id.subject_statistics);
            backgroud = itemView.findViewById(R.id.background);
            PushDownAnim.setPushDownAnimTo(itemView)
                    .setScale(PushDownAnim.MODE_SCALE, 0.98f)
                    .setDurationPush(100)
                    .setOnLongClickListener(view -> {
                        listener.onClick(view, getLayoutPosition());
                        return true;
                    });
        }

        void onBindData(SubjectElement element) {
            title.setText(element.getName());
            desc.setText(element.getDesc());
            if (backgroud != null) {
                backgroud.setBackgroundColor(element.getColor());
            }
        }

    }

    class ButtonViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        SubjectClickListener listenert;

        public ButtonViewHolder(@NonNull View itemView) {
            super(itemView);

            PushDownAnim.setPushDownAnimTo(itemView)
                    .setScale(PushDownAnim.MODE_SCALE, 0.95f)
                    .setDurationPush(100)
                    .setOnClickListener(this);
        }

        void onBindData(SubjectClickListener listener) {
            this.listenert = listener;
        }

        @Override
        public void onClick(View view) {
            listenert.onClick(view, getLayoutPosition());
        }
    }

    class SubjectSelectViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        TextView title;
        CardView color;

        public SubjectSelectViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.title_subject);
            color = itemView.findViewById(R.id.cardColor);
            PushDownAnim.setPushDownAnimTo(itemView)
                    .setScale(PushDownAnim.MODE_SCALE, 0.98f)
                    .setDurationPush(100)
                    .setOnClickListener(this);
        }

        void onBindData(SubjectElement element) {
            title.setText(element.getName());
            color.setCardBackgroundColor(element.getColor());
        }


        @Override
        public void onClick(View view) {
            listener.onClick(view, getLayoutPosition());
        }
    }

    public interface SubjectClickListener {
        void onClick(View view, int position);
    }


}

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
import com.artuok.appwork.library.Constants;
import com.artuok.appwork.objects.Item;
import com.artuok.appwork.objects.ProyectElement;
import com.artuok.appwork.objects.ProjectsElement;
import com.thekhaeng.pushdownanim.PushDownAnim;

import java.util.List;

public class ProjectAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private Context mContext;
    private LayoutInflater mInflater;
    private List<Item> mData;
    private ProjectsElement.OnProyectOpenListener listener;

    public ProjectAdapter(Context context, List<Item> data){
        this.mContext = context;
        this.mData = data;
        this.mInflater = LayoutInflater.from(context);
    }

    public ProjectAdapter(Context context, List<Item> data, ProjectsElement.OnProyectOpenListener listener){
        this.mContext = context;
        this.mData = data;
        this.mInflater = LayoutInflater.from(context);
        this.listener = listener;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == 0) {
            View view = mInflater.inflate(R.layout.item_project_layout, parent, false);
            return new ProjectViewHolder(view);
        } else if (viewType == 1){
            View view = mInflater.inflate(R.layout.item_icon_project, parent, false);
            return new IconProjectViewHolder(view);
        } else if(viewType == 2){
            View view = mInflater.inflate(R.layout.item_new_project_layout, parent, false);
            return new NewProjectViewHolder(view);
        }
        return null;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        int viewType = getItemViewType(position);
        if (viewType == 0) {
            ProyectElement element = (ProyectElement) mData.get(position).getObject();
            ((ProjectViewHolder) holder).onBindData(element);
        }else if(viewType == 1){
            int id = (Integer) mData.get(position).getObject();
            ((IconProjectViewHolder) holder).onBindData(id);
        }else if(viewType == 2){

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

    private class IconProjectViewHolder extends RecyclerView.ViewHolder{
        ImageView imageView;
        public IconProjectViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.icon_project);
        }

        void onBindData(int id){
            imageView.setImageResource(id);
        }
    }

    private class NewProjectViewHolder extends RecyclerView.ViewHolder{

        public NewProjectViewHolder(@NonNull View itemView) {
            super(itemView);
            PushDownAnim.setPushDownAnimTo(itemView)
                    .setOnClickListener(view -> listener.onProyectOpen(view, getLayoutPosition()));
        }
    }

    private class ProjectViewHolder extends RecyclerView.ViewHolder{
        ImageView imageView;
        TextView name;
        public ProjectViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.icon);
            name = itemView.findViewById(R.id.project_name);
            if(listener != null)
                PushDownAnim.setPushDownAnimTo(itemView)
                        .setOnClickListener(view -> listener.onProyectOpen(view, getLayoutPosition()));
        }

        void onBindData(ProyectElement element){
            if(element.getImage() != -1){
                if(element.getImage() >= Constants.ICON_LIST.length){
                    imageView.setImageResource(R.drawable.ic_eagle_emblem);
                }else{
                    imageView.setImageResource(Constants.ICON_LIST[element.getImage()]);
                }

            }

            name.setText(element.getName());
        }
    }
}

package com.artuok.appwork.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.viewpager2.widget.CompositePageTransformer;
import androidx.viewpager2.widget.MarginPageTransformer;
import androidx.viewpager2.widget.ViewPager2;

import com.artuok.appwork.R;
import com.artuok.appwork.adapters.ProjectAdapter;
import com.artuok.appwork.db.DbHelper;
import com.artuok.appwork.library.CenterPageTransformer;
import com.artuok.appwork.objects.Item;
import com.thekhaeng.pushdownanim.PushDownAnim;

import java.util.Arrays;
import java.util.List;

public class NewProjectDialog extends DialogFragment {

    ViewPager2 chooser;
    ImageView left, right;
    TextView createBtn;
    EditText projectName;
    List<Item> iconList = Arrays.asList(
            new Item(R.drawable.ic_centaur_heart, 1),
            new Item(R.drawable.ic_discussion, 1),
            new Item(R.drawable.ic_eagle_emblem, 1),
            new Item(R.drawable.ic_graduate_cap, 1),
            new Item(R.drawable.ic_spanner, 1),
            new Item(R.drawable.ic_light_bulb, 1)
    );
    OnCreateProjectListener onCreateProjectListener;

    public void setOnCreateProjectListener(OnCreateProjectListener onCreateProjectListener) {
        this.onCreateProjectListener = onCreateProjectListener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View root = inflater.inflate(R.layout.dialog_new_project_layout, null);
        initVariables(root);
        startViewPager();

        PushDownAnim.setPushDownAnimTo(createBtn)
                .setOnClickListener(view -> {
                    String name = projectName.getText().toString();
                    if(name.length() < 255){
                        insertProject(name, chooser.getCurrentItem());
                        dismiss();
                    }else{
                        Toast.makeText(requireActivity(), "project's name is too long", Toast.LENGTH_SHORT).show();
                    }
                    if(onCreateProjectListener != null){
                        onCreateProjectListener.onCreateProject(view);
                    }
                });

        builder.setView(root);
        return builder.create();
    }

    void insertProject(String name, int resource){
        DbHelper helper = new DbHelper(requireActivity());
        SQLiteDatabase db = helper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put("name", name);
        values.put("icon", resource);

        db.insert(DbHelper.T_PROJECTS, null, values);
    }

    void initVariables(View root){
        left = root.findViewById(R.id.chevron_left);
        right = root.findViewById(R.id.chevron_right);
        chooser = root.findViewById(R.id.project_image);
        createBtn = root.findViewById(R.id.button_create);
        projectName = root.findViewById(R.id.project_name);
    }

    void startViewPager(){


        chooser.setClipToPadding(false);
        chooser.setClipChildren(false);
        chooser.setOffscreenPageLimit(3);

        CompositePageTransformer compositePageTransformer = new CompositePageTransformer();
        compositePageTransformer.addTransformer(new CenterPageTransformer());
        compositePageTransformer.addTransformer(new MarginPageTransformer(40));
        chooser.setPageTransformer(compositePageTransformer);

        ProjectAdapter adapter = new ProjectAdapter(requireActivity(), iconList);
        chooser.setAdapter(adapter);

        chooser.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                super.onPageScrolled(position, positionOffset, positionOffsetPixels);
            }

            @Override
            public void onPageSelected(int position) {
                if(position == 0){
                    left.setVisibility(View.GONE);
                }else{
                    left.setVisibility(View.VISIBLE);
                }

                if(position == iconList.size() - 1){
                    right.setVisibility(View.GONE);
                }else {
                    right.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                super.onPageScrollStateChanged(state);
            }
        });

        left.setOnClickListener(view -> chooser.setCurrentItem(chooser.getCurrentItem()-1, true));
        right.setOnClickListener(view -> chooser.setCurrentItem(chooser.getCurrentItem()+1, true));
    }


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        getDialog().getWindow().setBackgroundDrawable(requireActivity().getDrawable(R.drawable.transparent_background));
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    public interface OnCreateProjectListener{
        void onCreateProject(View view);
    }
}

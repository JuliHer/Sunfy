package com.artuok.appwork.fragmets;

import android.app.Dialog;
import android.content.ContentValues;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.artuok.appwork.R;
import com.artuok.appwork.adapters.SubjectAdapter;
import com.artuok.appwork.db.DbHelper;
import com.artuok.appwork.objects.ItemSubjectElement;
import com.artuok.appwork.objects.SubjectElement;
import com.thekhaeng.pushdownanim.PushDownAnim;

import java.util.ArrayList;
import java.util.List;

public class SubjectsFragment extends Fragment {

    //recyclerView
    private RecyclerView recyclerView;
    private SubjectAdapter adapter;
    private LinearLayoutManager manager;
    private List<ItemSubjectElement> elements;
    private SubjectAdapter.SubjectClickListener listener;

    ImageView colorD;
    int color = 0;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.subjects_fragment, container, false);

        elements = new ArrayList<>();
        adapter = new SubjectAdapter(requireActivity(), elements);
        manager = new LinearLayoutManager(requireActivity().getApplicationContext(), LinearLayoutManager.VERTICAL, false);
        recyclerView = root.findViewById(R.id.subject_recycler);

        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(manager);


        loadSubjects(true);

        return root;
    }

    public void showSubjectCreator() {
        final Dialog dialog = new Dialog(requireContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.bottom_subject_creator_layout);

        final TextView title = dialog.findViewById(R.id.title_subject);
        Button cancel = dialog.findViewById(R.id.cancel_subject);
        Button accept = dialog.findViewById(R.id.accept_subject);
        colorD = dialog.findViewById(R.id.color_select);
        TypedArray ta = requireActivity().getTheme().obtainStyledAttributes(R.styleable.AppWidgetAttrs);
        color = ta.getColor(R.styleable.AppWidgetAttrs_palette_yellow, 0);
        colorD.setColorFilter(color);
        ta.recycle();
        LinearLayout color = dialog.findViewById(R.id.color_picker);

        color.setOnClickListener(view -> {
            showColorPicker();
        });

        cancel.setOnClickListener(view -> {
            dialog.dismiss();
        });

        accept.setOnClickListener(view -> {
            if (!title.getText().toString().isEmpty() || !title.getText().toString().equals("")) {
                dialog.dismiss();
                insertSubject(title.getText().toString());
            } else {
                Toast.makeText(requireContext(), R.string.name_is_empty, Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.getWindow().getAttributes().windowAnimations = R.style.DialogAnimation;
        dialog.getWindow().setGravity(Gravity.BOTTOM);
    }

    public void insertSubject(String name) {
        DbHelper dbHelper = new DbHelper(requireActivity().getApplicationContext());
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("name", name);
        values.put("color", color);

        db.insert(DbHelper.t_subjects, null, values);
        loadSubjects(false);
    }

    void loadSubjects(boolean first) {
        elements.clear();
        elements.add(new ItemSubjectElement(1, (view, position) -> {
            showSubjectCreator();
        }));
        DbHelper helper = new DbHelper(requireActivity().getApplicationContext());
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + DbHelper.t_subjects + " ORDER BY name DESC", null);
        if (cursor.moveToFirst()) {
            do {
                elements.add(new ItemSubjectElement(new SubjectElement(cursor.getString(1), cursor.getInt(2)), 0));
            } while (cursor.moveToNext());
        }

        if (first) {
            recyclerView.setAdapter(adapter);
        } else {
            adapter.notifyDataSetChanged();
        }
    }

    private void showColorPicker() {
        Dialog colorSelector = new Dialog(requireActivity());
        colorSelector.requestWindowFeature(Window.FEATURE_NO_TITLE);
        colorSelector.setContentView(R.layout.bottom_recurrence_layout);
        colorSelector.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        colorSelector.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        colorSelector.getWindow().getAttributes().windowAnimations = R.style.DialogAnimation;
        colorSelector.getWindow().setGravity(Gravity.BOTTOM);

        LinearLayout edi = colorSelector.findViewById(R.id.color_selecting);
        edi.setVisibility(View.VISIBLE);


        TypedArray ta = requireActivity().getTheme().obtainStyledAttributes(R.styleable.AppWidgetAttrs);


        LinearLayout blue = colorSelector.findViewById(R.id.color_blue);
        PushDownAnim.setPushDownAnimTo(blue)
                .setDurationPush(100)
                .setScale(0.98f)
                .setOnClickListener(view -> {
                    color = ta.getColor(R.styleable.AppWidgetAttrs_palette_blue, 0);
                    colorD.setColorFilter(color);
                    ta.recycle();
                    colorSelector.dismiss();
                });

        LinearLayout green = colorSelector.findViewById(R.id.color_green);
        PushDownAnim.setPushDownAnimTo(green)
                .setDurationPush(100)
                .setScale(0.98f)
                .setOnClickListener(view -> {
                    color = ta.getColor(R.styleable.AppWidgetAttrs_palette_green, 0);
                    colorD.setColorFilter(color);
                    ta.recycle();
                    colorSelector.dismiss();
                });

        LinearLayout yellow = colorSelector.findViewById(R.id.color_yellow);
        PushDownAnim.setPushDownAnimTo(yellow)
                .setDurationPush(100)
                .setScale(0.98f)
                .setOnClickListener(view -> {
                    color = ta.getColor(R.styleable.AppWidgetAttrs_palette_yellow, 0);
                    colorD.setColorFilter(color);
                    ta.recycle();
                    colorSelector.dismiss();
                });

        LinearLayout red = colorSelector.findViewById(R.id.color_red);
        PushDownAnim.setPushDownAnimTo(red)
                .setDurationPush(100)
                .setScale(0.98f)
                .setOnClickListener(view -> {
                    color = ta.getColor(R.styleable.AppWidgetAttrs_palette_red, 0);
                    colorD.setColorFilter(color);
                    ta.recycle();
                    colorSelector.dismiss();
                });

        LinearLayout purple = colorSelector.findViewById(R.id.color_purple);
        PushDownAnim.setPushDownAnimTo(purple)
                .setDurationPush(100)
                .setScale(0.98f)
                .setOnClickListener(view -> {
                    color = ta.getColor(R.styleable.AppWidgetAttrs_palette_purple, 0);
                    colorD.setColorFilter(color);
                    ta.recycle();
                    colorSelector.dismiss();
                });

        colorSelector.show();
    }

}
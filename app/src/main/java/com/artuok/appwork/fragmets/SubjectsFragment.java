package com.artuok.appwork.fragmets;

import android.app.Dialog;
import android.content.ContentValues;
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

import java.util.ArrayList;
import java.util.List;

public class SubjectsFragment extends Fragment {

    //recyclerView
    private RecyclerView recyclerView;
    private SubjectAdapter adapter;
    private LinearLayoutManager manager;
    private List<ItemSubjectElement> elements;
    private SubjectAdapter.SubjectClickListener listener;

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
                elements.add(new ItemSubjectElement(new SubjectElement(cursor.getString(1)), 0));
            } while (cursor.moveToNext());
        }

        if (first) {
            recyclerView.setAdapter(adapter);
        } else {
            adapter.notifyDataSetChanged();
        }
    }

}
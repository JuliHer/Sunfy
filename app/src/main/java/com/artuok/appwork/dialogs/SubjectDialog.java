package com.artuok.appwork.dialogs;

import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.artuok.appwork.R;
import com.artuok.appwork.adapters.SubjectAdapter;
import com.artuok.appwork.db.DbHelper;
import com.artuok.appwork.library.CalendarWeekView;
import com.artuok.appwork.library.Constants;
import com.artuok.appwork.objects.Item;
import com.artuok.appwork.objects.ItemSubjectElement;
import com.artuok.appwork.objects.SubjectElement;
import com.artuok.appwork.services.AlarmWorkManager;
import com.thekhaeng.pushdownanim.PushDownAnim;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Objects;

public class SubjectDialog extends DialogFragment {


    List<ItemSubjectElement> elements = new ArrayList<>();
    SubjectAdapter adapter;
    ImageView addSubject;

    OnSubjectListener listener;

    int colorNewSubject = Color.parseColor("#ffeb3c");

    public void setOnSubjectListener(OnSubjectListener listener){
        this.listener = listener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());

        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View root = inflater.inflate(R.layout.dialog_subject_layout, null);

        RecyclerView recyclerView = root.findViewById(R.id.recycler);
        addSubject = root.findViewById(R.id.add_subject);

        elements.addAll(getSubjects());

        adapter = new SubjectAdapter(requireActivity(), elements, (view, position) -> {
            if(elements.get(position).getType() == 2){
                SubjectElement subject = (SubjectElement) elements.get(position).getObject();
                if(listener != null){
                    listener.onSubject(subject);
                }
            }
        });

        PushDownAnim.setPushDownAnimTo(addSubject)
                        .setOnClickListener(view -> UtilitiesDialog.showSubjectCreator(requireActivity(), new UtilitiesDialog.OnResponseListener() {
                            @Override
                            public void onAccept(View view, TextView title) {
                                String name = Constants.parseText(title.getText().toString());
                                if (!name.equals("")) {
                                    int id =Integer.parseInt(insertSubject(name, colorNewSubject)+"");
                                    SubjectElement subject = new SubjectElement(id, name, "", colorNewSubject);
                                    if(listener != null){
                                        listener.onSubject(subject);
                                    }
                                }
                            }

                            @Override
                            public void onDismiss(View view) {

                            }

                            @Override
                            public void onChangeColor(int color) {
                                colorNewSubject = color;
                            }
                        }));
        recyclerView.setLayoutManager(new LinearLayoutManager(requireActivity(), RecyclerView.HORIZONTAL, false));
        recyclerView.setAdapter(adapter);
        builder.setView(root);
        return builder.create();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        getDialog().getWindow().setBackgroundDrawable(requireActivity().getDrawable(R.drawable.transparent_background));
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    public long insertSubject(String name, int color) {
        DbHelper dbHelper = new DbHelper(requireActivity());
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("name", name);
        values.put("color", color);
        return db.insert(DbHelper.T_TAG, null, values);
    }



    private List<ItemSubjectElement> getSubjects() {
        DbHelper dbHelper = new DbHelper(requireActivity());
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        List<ItemSubjectElement> elements = new ArrayList<>();
        Cursor cursor = db.rawQuery("SELECT * FROM " + DbHelper.T_TAG + " ORDER BY name DESC", null);
        if (cursor.moveToFirst()) {
            do {
                elements.add(new ItemSubjectElement(new SubjectElement(cursor.getInt(0), cursor.getString(1), "", cursor.getInt(2)), 2));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return elements;
    }



    public interface OnSubjectListener{
        void onSubject(SubjectElement subject);
    }
}

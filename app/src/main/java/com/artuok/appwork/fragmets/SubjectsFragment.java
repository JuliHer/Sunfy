package com.artuok.appwork.fragmets;

import android.app.Dialog;
import android.content.ContentValues;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.artuok.appwork.MainActivity;
import com.artuok.appwork.R;
import com.artuok.appwork.adapters.ColorSelectAdapter;
import com.artuok.appwork.adapters.SubjectAdapter;
import com.artuok.appwork.db.DbHelper;
import com.artuok.appwork.dialogs.PermissionDialog;
import com.artuok.appwork.objects.ColorSelectElement;
import com.artuok.appwork.objects.ItemSubjectElement;
import com.artuok.appwork.objects.SubjectElement;
import com.faltenreich.skeletonlayout.Skeleton;
import com.faltenreich.skeletonlayout.SkeletonLayoutUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class SubjectsFragment extends Fragment {

    //recyclerView
    private RecyclerView recyclerView;
    private SubjectAdapter adapter;
    private LinearLayoutManager manager;
    private List<ItemSubjectElement> elements;
    private SubjectAdapter.SubjectClickListener listener;
    private SwipeRefreshLayout refreshLayout;

    private Skeleton skeleton;

    ImageView colorD;
    int color = 0;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.subjects_fragment, container, false);

        elements = new ArrayList<>();
        adapter = new SubjectAdapter(requireActivity(), elements, (view, pos) -> {
            notifyDelete(((SubjectElement) elements.get(pos).getObject()).getName());
        });
        manager = new LinearLayoutManager(requireActivity().getApplicationContext(), LinearLayoutManager.VERTICAL, false);
        recyclerView = root.findViewById(R.id.subject_recycler);
        refreshLayout = root.findViewById(R.id.refreshLayout);

        TypedArray a = requireActivity().obtainStyledAttributes(R.styleable.AppCustomAttrs);

        refreshLayout.setProgressBackgroundColorSchemeColor(a.getColor(R.styleable.AppCustomAttrs_backgroundDialog, 0));


        refreshLayout.setColorSchemeColors(a.getColor(R.styleable.AppCustomAttrs_iMainColor, 0));

        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(manager);
        recyclerView.setAdapter(adapter);

        refreshLayout.setOnRefreshListener(() -> new TaskAsinc().execute(false));

        skeleton = SkeletonLayoutUtils.applySkeleton(recyclerView, R.layout.skeleton_subject_layout, 12);

        int shimmerColor = a.getColor(R.styleable.AppCustomAttrs_shimmerSkeleton, Color.GRAY);
        int maskColor = a.getColor(R.styleable.AppCustomAttrs_maskSkeleton, Color.LTGRAY);

        skeleton.setMaskColor(maskColor);
        skeleton.setShimmerColor(shimmerColor);
        a.recycle();
        new TaskAsinc().execute(true);


        return root;
    }

    public void showSubjectCreator() {
        final Dialog dialog = new Dialog(requireContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.bottom_subject_creator_layout);

        final TextView title = dialog.findViewById(R.id.title_subject);
        Button accept = dialog.findViewById(R.id.accept_subject);
        colorD = dialog.findViewById(R.id.color_select);
        TypedArray ta = requireActivity().getTheme().obtainStyledAttributes(R.styleable.AppCustomAttrs);
        color = ta.getColor(R.styleable.AppCustomAttrs_palette_yellow, 0);
        colorD.setColorFilter(color);
        ta.recycle();
        LinearLayout color = dialog.findViewById(R.id.color_picker);

        color.setOnClickListener(view -> showColorPicker());
        accept.setOnClickListener(view -> {
            String msg = checkMessage(title.getText().toString());
            if (!msg.isEmpty()) {
                dialog.dismiss();
                insertSubject(msg);
            } else {
                dialog.dismiss();
                ((MainActivity) requireActivity()).showSnackbar(getString(R.string.name_is_empty));
            }
        });

        dialog.show();
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.getWindow().getAttributes().windowAnimations = R.style.DialogAnimation;
        dialog.getWindow().setGravity(Gravity.BOTTOM);
    }

    private String checkMessage(String text) {
        int length = text.length();
        int start = 0;
        if (length > 0) {
            String last = text.substring(length - 1, length);
            while (last.equals(" ") && length > 0) {
                length--;
                last = text.substring(length - 1, length);
            }
            String first = text.substring(start, start+1);
            while(first.equals(" ") && (length - start) > 0){
                start++;
                first = text.substring(start, start+1);
            }

            if ((length - start) > 0) {
                return text.substring(start, length);
            }
        }
        return "";
    }

    public void insertSubject(String name) {
        DbHelper dbHelper = new DbHelper(requireActivity().getApplicationContext());
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("name", name);
        values.put("color", color);

        db.insert(DbHelper.t_subjects, null, values);
        new TaskAsinc().execute(false);
    }

    public void onNotifyDataChanged() {
        new TaskAsinc().execute(false);
    }

    void loadSubjects() {
        elements.clear();
        elements.add(new ItemSubjectElement(1, (view, position) -> {
            showSubjectCreator();
        }));
        DbHelper helper = new DbHelper(requireActivity().getApplicationContext());
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + DbHelper.t_subjects + " ORDER BY name COLLATE NOCASE ASC", null);
        if (cursor.moveToFirst()) {
            do {
                int subject = cursor.getInt(0);
                String statistic = getStatisticsSubject(subject);
                elements.add(new ItemSubjectElement(new SubjectElement(cursor.getString(1), statistic, cursor.getInt(2)), 0));
            } while (cursor.moveToNext());
        }


        cursor.close();
    }

    private String getStatisticsSubject(int id) {
        String status = "";
        if (isAdded()) {
            DbHelper helper = new DbHelper(requireActivity().getApplicationContext());
            SQLiteDatabase db = helper.getReadableDatabase();
            Cursor cursor = db.rawQuery("SELECT * FROM " + DbHelper.T_TASK + " WHERE subject = '" + id + "'", null);
            status = "0 " + requireActivity().getString(R.string.pending) + " • 0 " + requireActivity().getString(R.string.overdue) + " • 0 " + requireActivity().getString(R.string.done_string);
            int pending = 0, overdue = 0, done = 0;
            if (cursor.moveToFirst()) {
                do {
                    boolean taskDone = cursor.getInt(5) == 1;
                    if (taskDone) {
                        done++;
                    } else {
                        long today = Calendar.getInstance().getTimeInMillis();
                        boolean taskPending = cursor.getLong(2) > today;
                        if (taskPending) {
                            pending++;
                        } else {
                            overdue++;
                        }
                    }
                } while (cursor.moveToNext());
                status = pending + " " + requireActivity().getString(R.string.pending) + " • " + overdue + " " + requireActivity().getString(R.string.overdue) + " • " + done + " " + requireActivity().getString(R.string.done_string);
            }
            cursor.close();
        }
        return status;
    }

    ColorSelectAdapter adapterC;
    List<ColorSelectElement> elementsC;

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

        RecyclerView r = colorSelector.findViewById(R.id.recycler);
        LinearLayoutManager m = new LinearLayoutManager(requireActivity().getApplicationContext(), RecyclerView.VERTICAL, false);
        elementsC = getColors();
        adapterC = new ColorSelectAdapter(requireActivity(), elementsC, (view, position) -> {
            color = elementsC.get(position).getColorVibrant();
            colorD.setColorFilter(color);
            colorSelector.dismiss();
        });

        r.setLayoutManager(m);
        r.setHasFixedSize(true);
        r.setAdapter(adapterC);

        colorSelector.show();
    }


    public List<ColorSelectElement> getColors() {
        List<ColorSelectElement> e = new ArrayList<>();

        e.add(new ColorSelectElement("red", Color.parseColor("#f44236"), Color.parseColor("#b90005")));
        e.add(new ColorSelectElement("rose", Color.parseColor("#ea1e63"), Color.parseColor("#af0039")));
        e.add(new ColorSelectElement("purple", Color.parseColor("#9c28b1"), Color.parseColor("#6a0080")));
        e.add(new ColorSelectElement("purblue", Color.parseColor("#673bb7"), Color.parseColor("#320c86")));
        e.add(new ColorSelectElement("blue", Color.parseColor("#3f51b5"), Color.parseColor("#002983")));
        e.add(new ColorSelectElement("blueCyan", Color.parseColor("#2196f3"), Color.parseColor("#006ac0")));
        e.add(new ColorSelectElement("cyan", Color.parseColor("#03a9f5"), Color.parseColor("#007bc1")));
        e.add(new ColorSelectElement("turques", Color.parseColor("#008ba2"), Color.parseColor("#008ba2")));
        e.add(new ColorSelectElement("bluegreen", Color.parseColor("#009788"), Color.parseColor("#00685a")));
        e.add(new ColorSelectElement("green", Color.parseColor("#4cb050"), Color.parseColor("#087f23")));
        e.add(new ColorSelectElement("greenYellow", Color.parseColor("#8bc24a"), Color.parseColor("#5a9215")));
        e.add(new ColorSelectElement("yellowGreen", Color.parseColor("#cddc39"), Color.parseColor("#99ab01")));
        e.add(new ColorSelectElement("yellow", Color.parseColor("#ffeb3c"), Color.parseColor("#c8b800")));
        e.add(new ColorSelectElement("yellowOrange", Color.parseColor("#fec107"), Color.parseColor("#c89100")));
        e.add(new ColorSelectElement("Orangeyellow", Color.parseColor("#ff9700"), Color.parseColor("#c66901")));
        e.add(new ColorSelectElement("orange", Color.parseColor("#fe5722"), Color.parseColor("#c41c01")));
        e.add(new ColorSelectElement("gray", Color.parseColor("#9e9e9e"), Color.parseColor("#707070")));
        e.add(new ColorSelectElement("grayb", Color.parseColor("#607d8b"), Color.parseColor("#34525d")));
        e.add(new ColorSelectElement("brown", Color.parseColor("#795547"), Color.parseColor("#4a2c21")));

        return e;
    }

    private void deleteSubject(String subject) {
        DbHelper dbHelper = new DbHelper(requireActivity());
        SQLiteDatabase dbr = dbHelper.getReadableDatabase();
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        subject = DatabaseUtils.sqlEscapeString(subject);

        Cursor c = dbr.rawQuery("SELECT id FROM " + DbHelper.t_subjects + " WHERE name = " + subject, null);
        int idSubject = -1;
        if (c.moveToFirst()) {
            idSubject = c.getInt(0);
        }
        if (idSubject >= 0) {
            db.delete(DbHelper.T_TASK, "subject = " + subject, null);
            db.delete(DbHelper.t_event, "subject = '" + idSubject + "'", null);
            db.delete(DbHelper.t_subjects, "name = " + subject, null);
            ((MainActivity) requireActivity()).notifyAllChanged();
            new TaskAsinc().execute(false);
        }
        c.close();
    }

    public void notifyDelete(String subject) {
        PermissionDialog dialog = new PermissionDialog();
        dialog.setTitleDialog(requireActivity().getString(R.string.delete)+" "+subject);
        dialog.setTextDialog(subject + " " + requireActivity().getString(R.string.subject_delete));
        dialog.setDrawable(R.drawable.bookmark);
        dialog.setPositive((view, which) -> {
            deleteSubject(subject);
            dialog.dismiss();
        });

        dialog.setNegative((view, which) -> dialog.dismiss());

        dialog.show(requireActivity().getSupportFragmentManager(), "A");


    }

    private class TaskAsinc extends AsyncTask<Boolean, Boolean, Boolean> {
        @Override
        protected void onPreExecute() {
            skeleton.showSkeleton();
            super.onPreExecute();
        }

        @Override
        protected Boolean doInBackground(Boolean... booleans) {
            loadSubjects();
            return booleans[0];
        }

        @Override
        protected void onPostExecute(Boolean b) {
            adapter.notifyDataSetChanged();
            skeleton.showOriginal();
            refreshLayout.setRefreshing(false);
        }
    }
}
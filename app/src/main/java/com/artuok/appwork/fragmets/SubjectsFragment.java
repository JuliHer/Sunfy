package com.artuok.appwork.fragmets;

import android.app.Dialog;
import android.content.ContentValues;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.database.DatabaseUtils;
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

import com.artuok.appwork.MainActivity;
import com.artuok.appwork.R;
import com.artuok.appwork.adapters.ColorSelectAdapter;
import com.artuok.appwork.adapters.ScheduleAdapter;
import com.artuok.appwork.adapters.SubjectAdapter;
import com.artuok.appwork.db.DbHelper;
import com.artuok.appwork.dialogs.PermissionDialog;
import com.artuok.appwork.library.CalendarWeekView;
import com.artuok.appwork.objects.ColorSelectElement;
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
        adapter = new SubjectAdapter(requireActivity(), elements, (view, pos) -> {
            showSubjectInfo(((SubjectElement) elements.get(pos).getObject()).getName());
        });
        manager = new LinearLayoutManager(requireActivity().getApplicationContext(), LinearLayoutManager.VERTICAL, false);
        recyclerView = root.findViewById(R.id.subject_recycler);

        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(manager);


        loadSubjects(true);

        return root;
    }

    ScheduleAdapter adapterS;
    ScheduleAdapter.OnClickListener n;
    List<CalendarWeekView.EventsTask> element;

    private void setListener() {
        n = (view, pos) -> {
            removeEvent(element.get(pos).getId());
            element.remove(pos);
            adapterS.notifyItemRemoved(pos);
            ((MainActivity) requireActivity()).notifyAllChanged();
        };
    }

    private void removeEvent(int id) {
        DbHelper dbHelper = new DbHelper(requireActivity());
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        db.delete(DbHelper.t_event, "id = '" + id + "'", null);
    }

    private void showSubjectInfo(String subject) {
        final Dialog dialog = new Dialog(requireContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.bottom_task_layout);


        TextView title = dialog.findViewById(R.id.title_subject);
        title.setText(subject);
        LinearLayout delete = dialog.findViewById(R.id.deleteSubject);

        PushDownAnim.setPushDownAnimTo(delete)
                .setDurationPush(100)
                .setScale(PushDownAnim.MODE_SCALE, 0.95f)
                .setOnClickListener(view -> {
                    notifyDelete(subject);
                    dialog.dismiss();
                });

        RecyclerView r = dialog.findViewById(R.id.recycler);
        setListener();
        element = new ArrayList<>();

        element = getSSchedule(subject);
        adapterS = new ScheduleAdapter(
                requireActivity(),
                element,
                (view, pos) -> {

                },
                n);

        LinearLayoutManager m = new LinearLayoutManager(requireActivity().getApplicationContext(), RecyclerView.VERTICAL, false);
        r.setLayoutManager(m);
        r.setHasFixedSize(false);

        if (element != null)
            r.setAdapter(adapterS);

        dialog.show();
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.getWindow().getAttributes().windowAnimations = R.style.DialogAnimation;
        dialog.getWindow().setGravity(Gravity.BOTTOM);
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
            db.delete(DbHelper.t_task, "subject = " + subject, null);
            db.delete(DbHelper.t_event, "subject = '" + idSubject + "'", null);
            db.delete(DbHelper.t_subjects, "name = " + subject, null);
            ((MainActivity) requireActivity()).notifyAllChanged();
            loadSubjects(false);
        }
        c.close();
    }

    private List<CalendarWeekView.EventsTask> getSSchedule(String n) {
        List<CalendarWeekView.EventsTask> a = new ArrayList<>();
        DbHelper dbHelper = new DbHelper(requireActivity());
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        n = DatabaseUtils.sqlEscapeString(n);
        Cursor c = db.rawQuery("SELECT * FROM " + DbHelper.t_subjects + " WHERE name = " + n + "", null);
        if (c.moveToFirst()) {
            int id = c.getInt(0);
            Cursor s = db.rawQuery("SELECT * FROM " + DbHelper.t_event + " WHERE subject = '" + id + "' ORDER BY day_of_week ASC, time ASC", null);

            if (s.moveToFirst()) {
                do {
                    int ids = s.getInt(0);
                    int dd = s.getInt(2);
                    long h = s.getLong(3);
                    long d = s.getLong(4);
                    int t = s.getInt(5);
                    String tt = s.getString(1);

                    a.add(new CalendarWeekView.EventsTask(ids, dd, h, d, t, tt));
                } while (s.moveToNext());
            }
            s.close();
        }

        c.close();

        return a;
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
        cursor.close();
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

    public void notifyDelete(String subject) {
        PermissionDialog dialog = new PermissionDialog();
        dialog.setTitleDialog("Delete Subject");
        dialog.setTextDialog(subject + " will be deleted, along with its task and schedules");
        dialog.setDrawable(R.drawable.ic_trash);
        dialog.setPositive((view, which) -> {
            deleteSubject(subject);
            dialog.dismiss();
        });

        dialog.setNegative((view, which) -> dialog.dismiss());

        dialog.show(requireActivity().getSupportFragmentManager(), "A");
    }
}
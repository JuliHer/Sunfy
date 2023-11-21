package com.artuok.appwork.dialogs;

import android.Manifest;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.artuok.appwork.R;
import com.artuok.appwork.adapters.ScheduleAdapter;
import com.artuok.appwork.db.DbHelper;
import com.artuok.appwork.library.CalendarWeekView;
import com.artuok.appwork.objects.EventElement;
import com.artuok.appwork.objects.SubjectElement;
import com.artuok.appwork.services.AlarmWorkManager;
import com.thekhaeng.pushdownanim.PushDownAnim;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Objects;

public class ScheduleMakerDialog extends DialogFragment {
    long time = 0;
    int dayofweek = 0;

    ImageView newSchedule;
    LinearLayout subject;
    TextView subjectName, doneBtn;
    List<CalendarWeekView.EventsTask> events = new ArrayList<>();
    ScheduleAdapter adapter;
    SubjectElement subjectElement;
    DbHelper dbHelper;
    OnCreateScheduleListener onCreateScheduleListener;

    boolean info = false;
    CalendarWeekView.EventsTask infoEvent;

    public void setOnCreateScheduleListener(OnCreateScheduleListener onCreateScheduleListener) {
        this.onCreateScheduleListener = onCreateScheduleListener;
    }

    public ScheduleMakerDialog(int dayofweek, long time){
        this.time = time;
        this.dayofweek = dayofweek;
    }

    public ScheduleMakerDialog(CalendarWeekView.EventsTask event){
        info = true;
        infoEvent = event;
    }



    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());

        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View root = inflater.inflate(R.layout.dialog_schedule_maker_layout, null);
        newSchedule = root.findViewById(R.id.addSchedule);
        subject = root.findViewById(R.id.edit_schedule);
        subjectName = root.findViewById(R.id.subject_name);
        doneBtn = root.findViewById(R.id.done_btn);
        dbHelper = new DbHelper(requireActivity());

        PushDownAnim.setPushDownAnimTo(newSchedule)
                .setOnClickListener(view -> {
                    ScheduleConfigDialog scheduleConfigDialog = new ScheduleConfigDialog();
                    scheduleConfigDialog.setOnDateListener(new ScheduleConfigDialog.OnDateListener() {
                        @Override
                        public void onDate(int day, long hour, long duration) {
                            events.add(new CalendarWeekView.EventsTask(day, hour, duration, 0, "Schedule"));
                            adapter.notifyItemInserted(events.size()-1);
                            scheduleConfigDialog.dismiss();
                        }

                        @Override
                        public void onDelete() {
                            scheduleConfigDialog.dismiss();
                        }
                    });
                    scheduleConfigDialog.show(requireActivity().getSupportFragmentManager(), "Config Schedule");
                });
        if(!info){
            PushDownAnim.setPushDownAnimTo(subject)
                    .setOnClickListener(view -> {
                        SubjectDialog subjectDialog = new SubjectDialog();
                        subjectDialog.setOnSubjectListener(subject -> {
                            subjectName.setText(subject.getName());
                            subjectName.setTextColor(subject.getColor());
                            subjectElement = subject;
                            subjectDialog.dismiss();
                        });

                        subjectDialog.show(requireActivity().getSupportFragmentManager(), "Select Subject");
                    });
        }

        PushDownAnim.setPushDownAnimTo(doneBtn)
                .setOnClickListener(view -> {
                    setEvents();
                    dismiss();
                });

        RecyclerView recyclerView = root.findViewById(R.id.recycler);

        if(info){
            getSubjectByName();
            subjectName.setTextColor(infoEvent.getColor());
            subjectName.setText(infoEvent.getTitle());
            setAllSchedules();
        }else{
            events.add(new CalendarWeekView.EventsTask(dayofweek, time,3600,0, "Schedule"));
        }

        adapter = new ScheduleAdapter(requireActivity(), events, (view, pos) -> {
            int dayD = events.get(pos).getDay();
            long hourD = events.get(pos).getHour();
            long durationD = events.get(pos).getDuration();
            ScheduleConfigDialog scheduleConfigDialog = new ScheduleConfigDialog(pos, dayD, hourD, durationD);
            scheduleConfigDialog.setOnDateListener(new ScheduleConfigDialog.OnDateListener() {
                @Override
                public void onDate(int day, long hour, long duration) {
                    events.remove(pos);
                    events.add(pos, new CalendarWeekView.EventsTask(day, hour, duration, 0, "Schedule"));
                    adapter.notifyItemChanged(pos);
                    scheduleConfigDialog.dismiss();
                }

                @Override
                public void onDelete() {
                    events.remove(pos);
                    adapter.notifyItemRemoved(pos);
                    scheduleConfigDialog.dismiss();
                }
            });
            scheduleConfigDialog.show(requireActivity().getSupportFragmentManager(), "Config Schedule");
        }, (view, pos) -> {

        });

        recyclerView.setLayoutManager(new LinearLayoutManager(requireActivity(), RecyclerView.HORIZONTAL, false));
        recyclerView.setAdapter(adapter);
        recyclerView.setHasFixedSize(false);

        adapter.notifyDataSetChanged();

        builder.setView(root);
        return builder.create();
    }

    public void getSubjectByName(){
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT * FROM "+DbHelper.T_TAG+" WHERE name = ?", new String[]{infoEvent.getTitle()});
        if(c.moveToFirst()){
            subjectElement = new SubjectElement(c.getInt(0), c.getString(1), "", c.getInt(2));
        }
        c.close();
    }

    public void setAllSchedules(){
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor s = db.rawQuery("SELECT * FROM " + DbHelper.t_event + " WHERE subject = ? ORDER BY day_of_week ASC, time ASC", new String[]{subjectElement.getId()+""});
        if (s.moveToFirst()) {
            do {
                int ids = s.getInt(0);
                int dd = s.getInt(2);
                long h = s.getLong(3);
                long d = s.getLong(4);
                int t = s.getInt(5);
                String tt = s.getString(1);

                events.add(new CalendarWeekView.EventsTask(ids, dd, h, d, t, tt));
            } while (s.moveToNext());
        }
        s.close();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        getDialog().getWindow().setBackgroundDrawable(requireActivity().getDrawable(R.drawable.transparent_background));
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    private void setEvents(){
        if(info) {
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            db.delete(DbHelper.t_event, "subject = ?", new String[]{subjectElement.getId()+""});
        }
        if(subjectElement == null) return;
        if (!Objects.equals(subjectElement.getName(), "")) {
            for (CalendarWeekView.EventsTask e : events) {
                EventElement newEvent = new EventElement(0, subjectElement.getName(), e.getDay(), e.getHour(), e.getDuration(), subjectElement.getColor(), subjectElement.getId());
                mergeEventsWithSameSubject(requireActivity(), newEvent);
            }
            setAlarmSchedule();
        }

        if(onCreateScheduleListener != null){
            onCreateScheduleListener.onCreateSchedule();
        }
    }

    public void mergeEventsWithSameSubject(Context context, EventElement newEvent) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        // Buscar eventos con el mismo 'subject'
        String selectQuery = "SELECT * FROM "+DbHelper.t_event+" WHERE subject = " + newEvent.getSubject();
        Cursor cursor = db.rawQuery(selectQuery, null);

        boolean isNewEventMerged = false;
        if (cursor != null && cursor.moveToFirst()) {
            do {
                EventElement existingEvent = new EventElement();
                existingEvent.setId(cursor.getInt(0));
                existingEvent.setTitle(cursor.getString(1));
                existingEvent.setDayOfWeek(cursor.getInt(2));
                existingEvent.setTime(cursor.getLong(3));
                existingEvent.setDuration(cursor.getLong(4));
                existingEvent.setType(cursor.getInt(5));
                existingEvent.setSubject(cursor.getInt(6));

                // Verificar si los eventos se superponen
                if (eventsOverlap(existingEvent, newEvent)) {
                    // Fusionar los eventos
                    EventElement mergedEvent = mergeEvents(existingEvent, newEvent);

                    // Eliminar el evento existente
                    String deleteQuery = "DELETE FROM event WHERE id = " + existingEvent.getId();
                    db.execSQL(deleteQuery);

                    // Insertar el evento fusionado en la base de datos
                    ContentValues values = new ContentValues();
                    values.put("title", mergedEvent.getTitle());
                    values.put("day_of_week", mergedEvent.getDayOfWeek());
                    values.put("time", mergedEvent.getTime());
                    values.put("duration", mergedEvent.getDuration());
                    values.put("type", mergedEvent.getType());
                    values.put("subject", mergedEvent.getSubject());
                    db.insert("event", null, values);
                    isNewEventMerged =true;
                }
            } while (cursor.moveToNext());
            cursor.close();
        }

        if (!isNewEventMerged) {
            ContentValues values = new ContentValues();
            values.put("title", newEvent.getTitle());
            values.put("day_of_week", newEvent.getDayOfWeek());
            values.put("time", newEvent.getTime());
            values.put("duration", newEvent.getDuration());
            values.put("type", newEvent.getType());
            values.put("subject", newEvent.getSubject());
            db.insert("event", null, values);
        }

        db.close();
    }

    private boolean eventsOverlap(EventElement event1, EventElement event2) {
        long start1 = event1.getTime();
        long end1 = event1.getTime() + event1.getDuration();
        long start2 = event2.getTime();
        long end2 = event2.getTime() + event2.getDuration();

        return (start1 < end2 && end1 > start2) && event1.getDayOfWeek() == event2.getDayOfWeek();
    }

    private EventElement mergeEvents(EventElement event1, EventElement event2) {
        EventElement mergedEvent = new EventElement();
        mergedEvent.setTitle(event1.getTitle());
        mergedEvent.setDayOfWeek(event1.getDayOfWeek());
        mergedEvent.setTime(Math.min(event1.getTime(), event2.getTime()));
        mergedEvent.setDuration(Math.max(event1.getTime() + event1.getDuration(), event2.getTime() + event2.getDuration()) - mergedEvent.getTime());
        mergedEvent.setType(event1.getType());
        mergedEvent.setSubject(event1.getSubject());

        return mergedEvent;
    }


    void setAlarmSchedule() {
        DbHelper dbHelper = new DbHelper(requireActivity());
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor v = db.rawQuery("SELECT * FROM " + DbHelper.t_event + " ORDER BY day_of_week ASC, time ASC", null);
        Calendar c = Calendar.getInstance();
        long time = 0;
        long duration = 0;
        int day = -1;
        String name = "";
        long hour = (60 * 60 * c.get(Calendar.HOUR_OF_DAY)) + (60 * c.get(Calendar.MINUTE));
        int dow = c.get(Calendar.DAY_OF_WEEK) - 1;
        if (v.moveToFirst()) {
            do {
                if (v.getLong(3) > (hour + (60 * 5)) && dow == v.getInt(2)) {
                    time = v.getLong(3) * 1000;
                    day = v.getInt(2);
                    time = time - (hour * 1000);
                    duration = v.getLong(4) * 1000;
                    name = v.getString(1);
                    break;
                } else if (dow < v.getInt(2)) {
                    time = v.getLong(3) * 1000;
                    day = v.getInt(2);
                    int r = (day + 1) - (dow + 1);
                    time = (r * 86400000L) + (time) - (hour * 1000);
                    duration = v.getLong(4) * 1000;
                    name = v.getString(1);
                    break;
                }
            } while (v.moveToNext());
            if (!(!name.equals("") && time != 0 && duration != 0)) {
                v.moveToFirst();
                do {
                    time = v.getLong(3) * 1000;
                    day = v.getInt(2);
                    int r = 7 - (dow + 1) + (day + 1);
                    time = (r * 86400000L) + (time) - (hour * 1000);
                    duration = v.getLong(4) * 1000;
                    name = v.getString(1);
                    if (duration != 0) {
                        break;
                    }
                } while (v.moveToNext());
            }
        }
        if (!name.equals("") && time != 0 && duration != 0) {
            setNotify(name, time, duration);
        }
        v.close();
    }

    void setNotify(String name, long diff, long duration) {
        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.TIRAMISU){
            if(requireActivity().checkSelfPermission(Manifest.permission.SCHEDULE_EXACT_ALARM) != PackageManager.PERMISSION_GRANTED){
                return;
            }
        }
        long start = Calendar.getInstance().getTimeInMillis() + diff;

        Intent notify = new Intent(requireActivity(), AlarmWorkManager.class)
                .setAction(AlarmWorkManager.ACTION_EVENT);

        notify.putExtra("name", name);
        notify.putExtra("time", start);
        notify.putExtra("duration", duration);
        PendingIntent pendingNotify = PendingIntent.getBroadcast(
                requireActivity(),
                1, notify,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);


        AlarmManager manager = (AlarmManager) requireActivity().getSystemService(Context.ALARM_SERVICE);
        manager.cancel(pendingNotify);
        manager.setExact(AlarmManager.RTC_WAKEUP, start - (60 * 5 * 1000), pendingNotify);
    }

    public interface OnCreateScheduleListener{
        void onCreateSchedule();
    }
}

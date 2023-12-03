package com.artuok.appwork.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.NumberPicker;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.artuok.appwork.R;
import com.thekhaeng.pushdownanim.PushDownAnim;

public class ScheduleConfigDialog extends DialogFragment {

    NumberPicker hours, minutes, tm, duration;

    OnDateListener listener;
    RadioGroup week;

    int dayD = -1;
    long hourD, durationD;

    int dow = 0;

    public void setOnDateListener(OnDateListener onDateListener){
        this.listener = onDateListener;
    }

    public ScheduleConfigDialog(){

    }
    public ScheduleConfigDialog(int pos, int day, long hour, long duration){
        this.dayD = day;
        this.hourD = hour;
        this.durationD = duration;
    }


    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());

        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View root = inflater.inflate(R.layout.dialog_schedule_config_layout, null);
        hours = root.findViewById(R.id.hours);
        minutes = root.findViewById(R.id.minutes);
        tm = root.findViewById(R.id.tm);
        duration = root.findViewById(R.id.duration);
        week = root.findViewById(R.id.weekly);
        TextView doneBtn = root.findViewById(R.id.done_btn);
        TextView deleteBtn = root.findViewById(R.id.delete_btn);

        hours.setMinValue(1);
        hours.setMaxValue(12);

        minutes.setMinValue(00);
        minutes.setMaxValue(59);
        minutes.setFormatter(new NumberPicker.Formatter() {
            @Override
            public String format(int i) {

                if(i < 10){
                    return "0"+i;
                }
                return ""+i;
            }
        });


        String[] tmValues = new String[]{"a.m.", "p.m."};
        tm.setDisplayedValues(tmValues);
        tm.setMinValue(0);
        tm.setMaxValue(tmValues.length - 1);

        String[] durationValues = new String[]{"45m", "1h", "2h", "3h", "4h", "5h", "6h", "7h", "8h", "9h" , "10h"};
        duration.setDisplayedValues(durationValues);
        duration.setMinValue(0);
        duration.setMaxValue(durationValues.length - 1);

        initPref();

        week.setOnCheckedChangeListener((radioGroup, i) -> {
            switch (i){
                case R.id.su:
                    dow = 0;
                    break;
                case R.id.mo:
                    dow = 1;
                    break;
                case R.id.tu:
                    dow = 2;
                    break;
                case R.id.we:
                    dow = 3;
                    break;
                case R.id.th:
                    dow = 4;
                    break;
                case R.id.fr:
                    dow = 5;
                    break;
                case R.id.sa:
                    dow = 6;
                    break;
                default:
                    dow = 0;
                    break;
            }
        });
        PushDownAnim.setPushDownAnimTo(deleteBtn)
                .setOnClickListener(view -> {
                    if(listener != null){
                        listener.onDelete();
                    }
                });
        PushDownAnim.setPushDownAnimTo(doneBtn)
                        .setOnClickListener(view -> {
                            int tmV = tm.getValue();
                            int h = (hours.getValue()+(12 * tmV));
                            h = hours.getValue() == 12 ? h - 12 : h;
                            String hour = h+"h "+minutes.getValue()+"m ";

                            int d = duration.getValue();
                            long durat = 0;
                            if(d < 1){
                                durat = ((d+1)*45*60);
                            }else{
                                durat = (d*60*60);
                            }
                            if(listener != null){
                                int startat = ((h*60*60)+(minutes.getValue()*60));
                                Log.d("CattoOnDate", startat+"");
                                listener.onDate(dow, startat, durat);
                            }

                        });

        builder.setView(root);
        return builder.create();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        getDialog().getWindow().setBackgroundDrawable(requireActivity().getDrawable(R.drawable.transparent_background));
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    private void initPref(){
        if(dayD >= 0){
            switch (dayD){
                case 1:
                    week.check(R.id.mo);
                    break;
                case 2:
                    week.check(R.id.tu);
                    break;
                case 3:
                    week.check(R.id.we);
                    break;
                case 4:
                    week.check(R.id.th);
                    break;
                case 5:
                    week.check(R.id.fr);
                    break;
                case 6:
                    week.check(R.id.sa);
                    break;
                default:
                    week.check(R.id.su);
                    break;
            }

            dow = dayD;
            int horas = (int) hourD / 3600;
            int tm = horas >= 12 ? 1 : 0;
            horas = horas >= 12 ? horas-12:horas;
            int minutos = (int) (hourD % 3600) / 60;

            hours.setValue(horas);
            minutes.setValue(minutos);
            this.tm.setValue(tm);

            long durat = durationD;
            int d;

            if (durat < 0) {
                throw new IllegalArgumentException("La duración debe ser mayor o igual a cero.");
            }

            d = (int) (durat / (60 * 60));

            if(d >= 11) d = 10;

            duration.setValue(d);
        }
    }

    public interface OnDateListener{
        void onDate(int day, long hour, long duration);
        void onDelete();
    }
}

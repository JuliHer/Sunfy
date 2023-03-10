package com.artuok.appwork.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.artuok.appwork.R;

import java.util.Calendar;

public class VerifyDialog extends DialogFragment {

    private OnResponseListener positive;
    private EditText codeInput;
    private TextView positiveBtn, timeOutString;
    private long timeOut = 0;
    private long timeInMillis = 0;


    public void setTimeOut(Long timeOut) {
        this.timeOut = timeOut - Calendar.getInstance().getTimeInMillis();
        this.timeInMillis = timeOut;
    }



    public void startTimeOut() {
        if(timeInMillis != 0){
            timeOutString.setVisibility(View.VISIBLE);
        }

        timeOut = timeInMillis - Calendar.getInstance().getTimeInMillis();
        long seconds = timeOut / 1000;
        int minutes = (int) (seconds / 60 % 60);
        int second = (int) (seconds % 60);

        String secondString = second < 10 ? "0" + second : "" + second;
        String timeString = minutes + ":" + secondString;
        timeOutString.setText(timeString);

        if(timeOut >= 0){
            timeOutString.setText(timeString);
        }else if(timeOut == 0){
            timeOutString.setText("FINISH");
        }
    }

    public void setOnPositiveResponeseListener(OnResponseListener positive) {
        this.positive = positive;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View root = inflater.inflate(R.layout.dialog_verify_layout, null);
        codeInput = root.findViewById(R.id.codeHiden);
        positiveBtn = root.findViewById(R.id.positive);
        timeOutString = root.findViewById(R.id.timeout);
        builder.setView(root);


        if (positive != null) {
            positiveBtn.setOnClickListener(view -> {
                String code = codeInput.getText().toString();
                positive.onResponse(view, code);
            });
        }


        return builder.create();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = super.onCreateView(inflater, container, savedInstanceState);
        getDialog().getWindow().setBackgroundDrawableResource(R.drawable.dialog_background);
        startTimeOut();
        return root;
    }

    public interface OnResponseListener {
        void onResponse(View view, String code);
    }
}

package com.artuok.appwork.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.artuok.appwork.R;

public class PermissionDialog extends DialogFragment {

    private int drawable = -1;
    private String textDialog = "";
    private String titleDialog = "";
    private onResponseListener negative;
    private onResponseListener positive;
    private onResponseListener neutral;
    private String positiveText = "";
    private String negativeText = "";
    private String neutralText = "Neutral";

    public void setTextDialog(String txt) {
        this.textDialog = txt;
    }

    public void setTitleDialog(String titleDialog) {
        this.titleDialog = titleDialog;
    }

    public void setDrawable(int drawable) {
        this.drawable = drawable;
    }

    public void setNegative(onResponseListener negative) {
        this.negative = negative;
    }

    public void setPositive(onResponseListener positive) {
        this.positive = positive;
    }

    public void setNeutral(onResponseListener neutral) {
        this.neutral = neutral;
    }

    public void setPositiveText(String positiveText) {
        this.positiveText = positiveText;
    }

    public void setNegativeText(String negativeText) {
        this.negativeText = negativeText;
    }

    public void setNeutralText(String neutralText) {
        this.neutralText = neutralText;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());

        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View root = inflater.inflate(R.layout.permission_dialog_layout, null);
        TextView title = root.findViewById(R.id.title_dialog);
        TextView text = root.findViewById(R.id.txt_dialog);

        title.setText(titleDialog);
        text.setText(textDialog);


        if(negativeText.isEmpty()){
            negativeText = requireActivity().getString(R.string.Cancel_M);
        }

        if(positiveText.isEmpty()){
            positiveText = requireActivity().getString(R.string.Accept_M);
        }

        ImageView draw = root.findViewById(R.id.drawable_dialog);
        draw.setImageDrawable(requireActivity().getDrawable(drawable));

        builder.setView(root);
        if (negative != null) {
            builder.setNegativeButton(negativeText, (dialogInterface, i) -> negative.onClick(dialogInterface, i));
        }

        if (positive != null) {
            builder.setPositiveButton(positiveText, (dialogInterface, i) -> positive.onClick(dialogInterface, i));
        }

        if(neutral != null){
            builder.setNeutralButton(neutralText,(dialogInterface, i) -> neutral.onClick(dialogInterface, i) );
        }

        return builder.create();

    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = super.onCreateView(inflater, container, savedInstanceState);
        getDialog().getWindow().setBackgroundDrawableResource(R.drawable.dialog_background);
        return root;
    }

    public interface onResponseListener {
        void onClick(DialogInterface view, int which);
    }
}

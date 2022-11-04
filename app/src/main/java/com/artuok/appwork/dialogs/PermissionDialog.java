package com.artuok.appwork.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
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

        ImageView draw = root.findViewById(R.id.drawable_dialog);
        draw.setImageDrawable(requireActivity().getDrawable(drawable));

        builder.setView(root);
        if (negative != null) {
            builder.setNegativeButton(requireActivity().getString(R.string.Accept_M), (dialogInterface, i) -> negative.onClick(dialogInterface, i));
        }

        if (positive != null) {
            builder.setPositiveButton(requireActivity().getString(R.string.Cancel_M), (dialogInterface, i) -> positive.onClick(dialogInterface, i));
        }

        return builder.create();

    }

    public interface onResponseListener {
        void onClick(DialogInterface view, int which);
    }
}

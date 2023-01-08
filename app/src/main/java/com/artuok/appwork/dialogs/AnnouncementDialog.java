package com.artuok.appwork.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.artuok.appwork.R;
import com.thekhaeng.pushdownanim.PushDownAnim;

public class AnnouncementDialog extends DialogFragment {

    int drawable = R.drawable.ic_pen;
    int backgroundCOlor = -23;
    String title = "title";
    String text = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Curabitur pharetra tincidunt est, quis sollicitudin turpis viverra a. Phasellus in orci id erat luctus rhoncus ut a nunc. In ut tempor lacus, vulputate varius felis. Sed.";

    private OnPositiveClickListener onPositiveClickListener = null;
    private OnNegativeClickListener onNegativeClickListener = null;
    private String positiveText;
    private String negativeText;

    public void setDrawable(int drawable) {
        this.drawable = drawable;
    }

    public void setBackgroundCOlor(int backgroundCOlor) {
        this.backgroundCOlor = backgroundCOlor;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setText(String text) {
        this.text = text;
    }

    public void setOnPositiveClickListener(String positiveText, OnPositiveClickListener onPositiveClickListener) {
        this.onPositiveClickListener = onPositiveClickListener;
        this.positiveText = positiveText;
    }

    public void setOnNegativeClickListener(String negativeText, OnNegativeClickListener onNegativeClickListener) {
        this.onNegativeClickListener = onNegativeClickListener;
        this.negativeText = negativeText;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());

        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View root = inflater.inflate(R.layout.announcement_dialog_layout, null);

        TextView title = root.findViewById(R.id.title);
        TextView text = root.findViewById(R.id.text);

        ImageView image = root.findViewById(R.id.image);
        LinearLayout bg = root.findViewById(R.id.bg);

        if (backgroundCOlor != -23) {
            ColorStateList myColorStateList = new ColorStateList(
                    new int[][]{
                            new int[]{}
                    },
                    new int[]{
                            backgroundCOlor,
                    }
            );
            bg.setBackgroundTintList(myColorStateList);
        }

        if (onNegativeClickListener != null) {
            TextView dismiss = root.findViewById(R.id.negative);
            dismiss.setText(negativeText);
            PushDownAnim.setPushDownAnimTo(dismiss)
                    .setDurationPush(100)
                    .setScale(PushDownAnim.MODE_SCALE, 0.95f)
                    .setOnClickListener(view -> onNegativeClickListener.onClick(view));
        }

        if (onPositiveClickListener != null) {
            Button accept = root.findViewById(R.id.positive);
            accept.setText(positiveText);
            PushDownAnim.setPushDownAnimTo(accept)
                    .setDurationPush(100)
                    .setScale(PushDownAnim.MODE_SCALE, 0.95f)
                    .setOnClickListener(view -> onPositiveClickListener.onClick(view));
        }

        title.setText(this.title);
        text.setText(this.text);

        image.setImageDrawable(requireActivity().getDrawable(drawable));

        builder.setView(root);
        return builder.create();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = super.onCreateView(inflater, container, savedInstanceState);


        getDialog().getWindow().setBackgroundDrawableResource(R.drawable.transparent_background);
        return root;
    }

    public interface OnPositiveClickListener {
        void onClick(View view);
    }

    public interface OnNegativeClickListener {
        void onClick(View view);
    }
}

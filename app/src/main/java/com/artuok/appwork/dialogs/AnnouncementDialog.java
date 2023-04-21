package com.artuok.appwork.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.DialogFragment;

import com.artuok.appwork.R;
import com.thekhaeng.pushdownanim.PushDownAnim;

public class AnnouncementDialog extends DialogFragment {

    int imageId = R.drawable.ic_pen;
    int backgroundColor = -23;
    String title = "title";
    String text = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Curabitur pharetra tincidunt est, quis sollicitudin turpis viverra a. Phasellus in orci id erat luctus rhoncus ut a nunc. In ut tempor lacus, vulputate varius felis. Sed.";
    boolean isImage = false;
    boolean isAgree = false;
    int textColor = -23;

    private OnPositiveClickListener onPositiveClickListener = null;
    private OnNegativeClickListener onNegativeClickListener = null;
    private String positiveText;
    private String negativeText;

    public void setDrawable(int drawable) {
        isImage = false;
        this.imageId = drawable;
    }

    public void setImage(int mipmap){
        isImage = true;
        this.imageId = mipmap;
    }

    public void setAgree(boolean isAgree){
        this.isAgree = isAgree;
    }

    public void setTextColor(int textColor) {
        this.textColor = textColor;
    }

    public void setBackgroundCOlor(int backgroundColor) {
        this.backgroundColor = backgroundColor;
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
        ImageView close = root.findViewById(R.id.close_x);
        CardView bg = root.findViewById(R.id.bg);

        if (backgroundColor != -23) {
            ColorStateList myColorStateList = new ColorStateList(
                    new int[][]{
                            new int[]{}
                    },
                    new int[]{
                            backgroundColor,
                    }
            );
            bg.setBackgroundTintList(myColorStateList);
        }

        if(textColor != -23){
            title.setTextColor(textColor);
            text.setTextColor(textColor);
        }

        if(isAgree){
            close.setVisibility(View.GONE);
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
        }else{
            Button accept = root.findViewById(R.id.positive);
            TextView dismiss = root.findViewById(R.id.negative);

            accept.setVisibility(View.GONE);
            dismiss.setVisibility(View.GONE);

            PushDownAnim.setPushDownAnimTo(close)
                    .setDurationPush(100)
                    .setScale(PushDownAnim.MODE_SCALE, 0.98f)
                    .setOnClickListener(view -> onPositiveClickListener.onClick(view));
        }


        title.setText(this.title);
        text.setText(this.text);

        if(isImage){
            image.setImageDrawable(requireActivity().getDrawable(imageId));
            CardView cd = root.findViewById(R.id.cardView);
            cd.setCardElevation(2);
        }else{
            image.setImageDrawable(requireActivity().getDrawable(imageId));
            image.setColorFilter(Color.WHITE);
        }


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

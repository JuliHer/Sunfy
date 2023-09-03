package com.artuok.appwork.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.artuok.appwork.R;
import com.artuok.appwork.adapters.ColorSelectAdapter;
import com.artuok.appwork.objects.ColorSelectElement;

import java.util.ArrayList;
import java.util.List;

public class UtilitiesDialog {




    public static void showSubjectCreator(Context context, OnResponseListener onResponseListener){
        Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.bottom_subject_creator_layout);
        TextView title = dialog.findViewById(R.id.title_subject);
        Button accept = dialog.findViewById(R.id.accept_subject);
        ImageView color = dialog.findViewById(R.id.color_select);
        TypedArray ta  =
                context.getTheme().obtainStyledAttributes(R.styleable.AppCustomAttrs);
        int colorPallette = ta.getColor(R.styleable.AppCustomAttrs_palette_yellow, 0);
        color.setColorFilter(colorPallette);
        ta.recycle();
        LinearLayout colorPicker = dialog.findViewById(R.id.color_picker);
                colorPicker.setOnClickListener(view -> showColorPicker(context, color, onResponseListener));

        accept.setOnClickListener(view ->{
            onResponseListener.onAccept(view, title);
            dialog.dismiss();
        });
        dialog.show();
        dialog.getWindow().setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.getWindow().getAttributes().windowAnimations = R.style.DialogAnimation;
        dialog.getWindow().setGravity(Gravity.BOTTOM);
    }

    private static void showColorPicker(Context context, ImageView color, OnResponseListener responseListener) {
        Dialog colorSelector = new Dialog(context);
        colorSelector.requestWindowFeature(Window.FEATURE_NO_TITLE);
        colorSelector.setContentView(R.layout.bottom_recurrence_layout);
        colorSelector.getWindow().setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        colorSelector.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        colorSelector.getWindow().getAttributes().windowAnimations = R.style.DialogAnimation;
        colorSelector.getWindow().setGravity(Gravity.BOTTOM);
        LinearLayout edi = colorSelector.findViewById(R.id.color_selecting);
                edi.setVisibility(View.VISIBLE);
        RecyclerView r = colorSelector.findViewById(R.id.recycler);
        LinearLayoutManager m = new LinearLayoutManager(
                context,
                RecyclerView.VERTICAL,
                false
        );
        ColorSelectAdapter adapterC;
        List<ColorSelectElement> elementsC;
        elementsC = getColors();
        adapterC = new ColorSelectAdapter(context, elementsC, (view, position) -> {
            int colorS = elementsC.get(position).getColorVibrant();
            color.setColorFilter(colorS);
            responseListener.onChangeColor(colorS);
            colorSelector.dismiss();
        }) ;


        r.setLayoutManager(m);
        r.setHasFixedSize(true);
        r.setAdapter(adapterC);
        colorSelector.show();
    }

    public interface OnResponseListener{
        void onAccept(View view, TextView title);
        void onDismiss(View view);

        void onChangeColor(int color);
    }

    private static List<ColorSelectElement> getColors()  {
        List<ColorSelectElement> e = new ArrayList();
        e.add(new ColorSelectElement("red", Color.parseColor("#f44236"), Color.parseColor("#b90005")));
        e.add(new ColorSelectElement("rose", Color.parseColor("#ea1e63"), Color.parseColor("#af0039")));
        e.add(new
                ColorSelectElement(
                        "purple",
                        Color.parseColor("#9c28b1"),
                        Color.parseColor("#6a0080")
                )
        );
        e.add(new
                ColorSelectElement(
                        "purblue",
                        Color.parseColor("#673bb7"),
                        Color.parseColor("#320c86")
                )
        );
        e.add(new ColorSelectElement("blue", Color.parseColor("#3f51b5"), Color.parseColor("#002983")));
        e.add(new
                ColorSelectElement(
                        "blueCyan",
                        Color.parseColor("#2196f3"),
                        Color.parseColor("#006ac0")
                )
        );
        e.add(new ColorSelectElement("cyan", Color.parseColor("#03a9f5"), Color.parseColor("#007bc1")));
        e.add(new
                ColorSelectElement(
                        "turques",
                        Color.parseColor("#008ba2"),
                        Color.parseColor("#008ba2")
                )
        );
        e.add(new
                ColorSelectElement(
                        "bluegreen",
                        Color.parseColor("#009788"),
                        Color.parseColor("#00685a")
                )
        );
        e.add(new ColorSelectElement("green", Color.parseColor("#4cb050"), Color.parseColor("#087f23")));
        e.add(new
                ColorSelectElement(
                        "greenYellow",
                        Color.parseColor("#8bc24a"),
                        Color.parseColor("#5a9215")
                )
        );
        e.add(new
                ColorSelectElement(
                        "yellowGreen",
                        Color.parseColor("#cddc39"),
                        Color.parseColor("#99ab01")
                )
        );
        e.add(new
                ColorSelectElement(
                        "yellow",
                        Color.parseColor("#ffeb3c"),
                        Color.parseColor("#c8b800")
                )
        );
        e.add(new
                ColorSelectElement(
                        "yellowOrange",
                        Color.parseColor("#fec107"),
                        Color.parseColor("#c89100")
                )
        );
        e.add(new
                ColorSelectElement(
                        "Orangeyellow",
                        Color.parseColor("#ff9700"),
                        Color.parseColor("#c66901")
                )
        );
        e.add(new
                ColorSelectElement(
                        "orange",
                        Color.parseColor("#fe5722"),
                        Color.parseColor("#c41c01")
                )
        );
        e.add(new ColorSelectElement("gray", Color.parseColor("#9e9e9e"), Color.parseColor("#707070")));
        e.add(new ColorSelectElement("grayb", Color.parseColor("#607d8b"), Color.parseColor("#34525d")));
        e.add(new ColorSelectElement("brown", Color.parseColor("#795547"), Color.parseColor("#4a2c21")));
        return e;
    }

}

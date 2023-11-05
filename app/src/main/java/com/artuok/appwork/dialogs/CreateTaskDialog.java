package com.artuok.appwork.dialogs;

import static android.app.Activity.RESULT_OK;

import android.Manifest;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.appwidget.AppWidgetManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.artuok.appwork.R;
import com.artuok.appwork.adapters.SubjectAdapter;
import com.artuok.appwork.db.DbHelper;
import com.artuok.appwork.fragmets.SettingsFragment;
import com.artuok.appwork.library.Constants;
import com.artuok.appwork.objects.ItemSubjectElement;
import com.artuok.appwork.objects.SubjectElement;
import com.artuok.appwork.widgets.TodayTaskWidget;
import com.google.firebase.auth.FirebaseAuth;
import com.thekhaeng.pushdownanim.PushDownAnim;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class CreateTaskDialog extends DialogFragment {

    LinearLayout tag, date, time;
    TextView tagText, timeText, dateText;
    EditText text;
    ImageView image, check, imageTemp;
    private Bitmap img;

    int subject = -1;
    int color = -1;
    int status = 0;

    boolean is24HourFormat = false;
    long datetime = -1, dateMillis = -1, timeMillis = -1;
    private String tempImage = "";
    private Boolean isTag = false, isText = false, isDate = false, isTime = false;

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(),
                    isGranted -> {

                    });

    private OnCheckListener onCheckListener;

    public void setOnCheckListener(OnCheckListener onCheckListener){
        this.onCheckListener = onCheckListener;
    }

    public CreateTaskDialog(int status){
        this.status = status;
    }

    public CreateTaskDialog(){
        this.status = 0;
    }

    public CreateTaskDialog(long deadline){
        datetime = deadline;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());

        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View root = inflater.inflate(R.layout.dialog_create_task, null);
        tag = root.findViewById(R.id.tag);
        tagText = root.findViewById(R.id.tag_text);
        date = root.findViewById(R.id.date);
        time = root.findViewById(R.id.time);
        text = root.findViewById(R.id.text);
        image = root.findViewById(R.id.image);
        imageTemp = root.findViewById(R.id.image_temp);
        check = root.findViewById(R.id.check);
        dateText = root.findViewById(R.id.date_text);
        timeText = root.findViewById(R.id.time_text);

        PushDownAnim.setPushDownAnimTo(tag)
                .setOnClickListener(view -> {
                    setSelectSubject(tagText);
                });

        PushDownAnim.setPushDownAnimTo(date)
                .setOnClickListener(view -> {
                    Calendar calendar = Calendar.getInstance();
                    int dd = calendar.get(Calendar.DAY_OF_MONTH);
                    int mm = calendar.get(Calendar.MONTH);
                    int aaaa = calendar.get(Calendar.YEAR);

                    TimePickerDialog.OnTimeSetListener timeSetListener = (timePicker, hourOfDay, minute) -> {
                        String a = (minute < 10) ? "0" + minute : String.valueOf(minute);
                        String e = (hourOfDay < 10) ? "0" + hourOfDay : String.valueOf(hourOfDay);
                        long timeMillis = ((hourOfDay * 60 * 60L) + (minute * 60L)) * 1000L;
                        datetime = dateMillis + timeMillis;
                        String s;
                        if (is24HourFormat) {
                            s = e + ":" + a;
                        } else {
                            if (hourOfDay >= 12) {
                                int b = (hourOfDay == 12) ? hourOfDay : hourOfDay - 12;
                                s = b + ":" + a + " p. m.";
                            } else {
                                s = e + ":" + a + " a. m.";
                            }
                        }
                        timeText.setText(s);
                        isTime = true;
                        checkIfIsComplete();
                    };

                    DatePickerDialog.OnDateSetListener dateSetListener = (datePicker, year, month, dayOfMonth) -> {
                        int m = month + 1;
                        String e = (m < 10) ? "0" + m : String.valueOf(m);
                        String a = (dayOfMonth < 10) ? "0" + dayOfMonth : String.valueOf(dayOfMonth);

                        Calendar c = Calendar.getInstance();
                        c.set(year, month, dayOfMonth, 0, 0, 0);
                        dateMillis = c.getTimeInMillis();
                        datetime = dateMillis + timeMillis;

                        String t = a + "/" + e + "/" + year;
                        dateText.setText(t);
                        TimePickerDialog timePicker = new TimePickerDialog(requireActivity(), timeSetListener, 12, 0, is24HourFormat);
                        timePicker.show();
                        isDate = true;
                        checkIfIsComplete();
                    };

                    Calendar c = Calendar.getInstance();
                    c.set(Calendar.YEAR, aaaa);
                    c.set(Calendar.MONTH, mm);
                    c.set(Calendar.DAY_OF_MONTH, dd);

                    DatePickerDialog datePicker = new DatePickerDialog(requireActivity(), dateSetListener, aaaa, mm, dd);
                    datePicker.getDatePicker().setMinDate(c.getTimeInMillis());
                    datePicker.show();
                });

        PushDownAnim.setPushDownAnimTo(time)
                .setOnClickListener(view -> {
                    TimePickerDialog.OnTimeSetListener timeSetListener = (timePicker, hourOfDay, minute) -> {
                        String a = (minute < 10) ? "0" + minute : String.valueOf(minute);
                        String e = (hourOfDay < 10) ? "0" + hourOfDay : String.valueOf(hourOfDay);
                        long timeMillis = (((long) hourOfDay * 60 * 60) + (minute * 60L)) * 1000L;
                        datetime = dateMillis + timeMillis;
                        String s;
                        if (is24HourFormat) {
                            s = e + ":" + a;
                        } else {
                            if (hourOfDay >= 12) {
                                int b = (hourOfDay == 12) ? hourOfDay : hourOfDay - 12;
                                s = b + ":" + a + " p. m.";
                            } else {
                                s = e + ":" + a + " a. m.";
                            }
                        }
                        timeText.setText(s);
                        isTime = true;
                        checkIfIsComplete();
                    };

                    TimePickerDialog timePicker = new TimePickerDialog(requireActivity(), timeSetListener, 12, 0, is24HourFormat);
                    timePicker.show();

                });

        PushDownAnim.setPushDownAnimTo(image)
                .setOnClickListener(view -> {
                    openSelectImage();
                });

        text.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                isText = !charSequence.toString().isEmpty();
                checkIfIsComplete();
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        PushDownAnim.setPushDownAnimTo(check)
                .setOnClickListener(view -> {
                    if(checkIfIsComplete()){
                        long id = insertAwaiting(text.getText().toString(), datetime, subject);
                        saveImagesInDevice(tempImage, id);
                        if(onCheckListener != null){
                            onCheckListener.onCheck(view, id);

                        }
                    }
                });

        if(datetime != -1){
            Calendar c = Calendar.getInstance();
            c.setTimeInMillis(datetime);
            dateMillis = c.getTimeInMillis();

            int month = c.get(Calendar.MONTH);
            int dayOfMonth = c.get(Calendar.DAY_OF_MONTH);
            int year = c.get(Calendar.YEAR);
            int m = month + 1;
            String e = (m < 10) ? "0" + m : String.valueOf(m);
            String a = (dayOfMonth < 10) ? "0" + dayOfMonth : String.valueOf(dayOfMonth);
            String t = a + "/" + e + "/" + year;
            dateText.setText(t);

            int minute = 0;
            int hourOfDay = 12;
            a = (minute < 10) ? "0" + minute : String.valueOf(minute);
            e = (hourOfDay < 10) ? "0" + hourOfDay : String.valueOf(hourOfDay);
            long timeMillis = ((hourOfDay * 60 * 60L) + (minute * 60L)) * 1000L;
            datetime = dateMillis + timeMillis;
            String s;
            if (is24HourFormat) {
                s = e + ":" + a;
            } else {
                if (hourOfDay >= 12) {
                    int b = (hourOfDay == 12) ? hourOfDay : hourOfDay - 12;
                    s = b + ":" + a + " p. m.";
                } else {
                    s = e + ":" + a + " a. m.";
                }
            }
            timeText.setText(s);
            isTime = true;
            isDate = true;
        }
        checkIfIsComplete();
        builder.setView(root);
        return builder.create();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = super.onCreateView(inflater, container, savedInstanceState);
        getDialog().getWindow().setBackgroundDrawable(requireActivity().getDrawable(R.drawable.transparent_background));

        return root;
    }


    private void setSelectSubject(TextView a) {
        SubjectDialog dialog = new SubjectDialog();
        dialog.setOnSubjectListener(subjectE -> {
            subject = subjectE.getId();
            a.setText(subjectE.getName());
            isTag = true;
            checkIfIsComplete();
            dialog.dismiss();
        });
        dialog.show(requireActivity().getSupportFragmentManager(), "Select Subject");
    }

    private List<ItemSubjectElement> getSubjects() {
        DbHelper dbHelper = new DbHelper(requireActivity());
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        List<ItemSubjectElement> elements = new ArrayList<>();
        Cursor cursor = db.rawQuery("SELECT * FROM " + DbHelper.T_TAG + " ORDER BY name DESC", null);
        if (cursor.moveToFirst()) {
            do {
                elements.add(new ItemSubjectElement(new SubjectElement(cursor.getInt(0), cursor.getString(1), "", cursor.getInt(2)), 2));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return elements;
    }

    public long insertSubject(String name, int color) {
        DbHelper dbHelper = new DbHelper(requireActivity());
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("name", name);
        values.put("color", color);
        return db.insert(DbHelper.T_TAG, null, values);
    }

    private void openSelectImage() {
        Dialog dialog = new Dialog(requireActivity());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.bottom_selectimage_layout);

        LinearLayout openCamera = dialog.findViewById(R.id.captureImage);
        LinearLayout openGallery = dialog.findViewById(R.id.obtainImage);

        openCamera.setOnClickListener(view -> {
            if (checkPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                openCamera();
            } else {
                requestPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, 0);
            }
            dialog.dismiss();
        });

        openGallery.setOnClickListener(view -> {
            if (checkPermission(Manifest.permission.READ_EXTERNAL_STORAGE)) {
                getPictureGallery();
            } else {
                requestPermission(Manifest.permission.READ_EXTERNAL_STORAGE, 1);
            }
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

    private boolean checkPermission(String permission) {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ||
                requireActivity().checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermission(String permission, int requestCode) {
        if (shouldShowRequestPermissionRationale(permission)) {
            showInContextUI(requestCode);
        } else {
            requestPermissionLauncher.launch(permission);
        }
    }

    private void showInContextUI(int i) {
        PermissionDialog dialog = new PermissionDialog();
        dialog.setTitleDialog(getString(R.string.required_permissions));
        dialog.setDrawable(R.drawable.smartphone);

        if (i == 0) {
            dialog.setTextDialog(getString(R.string.permissions_wres));
            dialog.setPositive((dialog1, which) -> {
                requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            });
        } else if (i == 1) {
            dialog.setTextDialog(getString(R.string.permissions_wres));
            dialog.setPositive((dialog1, which) -> {
                requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
            });
        } else if (i == 2) {
            dialog.setDrawable(R.drawable.camera);
            dialog.setTextDialog(getString(R.string.permissions_camera));
            dialog.setPositive((dialog1, which) -> {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA);
            });
        } else if (i == 3) {
            dialog.setTextDialog(getString(R.string.permissions_wres));
            dialog.setPositive((dialog1, which) -> {
                requestPermissionLauncher.launch(Manifest.permission.MANAGE_EXTERNAL_STORAGE);
            });
        }

        dialog.setNegative((view, which) -> {
            dialog.dismiss();
        });

        dialog.show(requireActivity().getSupportFragmentManager(), "permissions");
    }

    private void getPictureGallery() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        resultForGalleryPicture.launch(intent);
    }

    private void openCamera() {
        if (requireActivity().checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, getOutputFile());
            resultLauncher.launch(intent);
        } else if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
            showInContextUI(2);
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    ActivityResultLauncher<Intent> resultForGalleryPicture = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    Uri data = result.getData().getData();
                    Cursor c = requireActivity().getContentResolver().query(data, null, null, null, null);

                    if (c != null && c.moveToFirst()) {
                        try {
                            InputStream s = new BufferedInputStream(requireActivity().getContentResolver().openInputStream(data));
                            img = BitmapFactory.decodeStream(s);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    img.compress(Bitmap.CompressFormat.JPEG, 100, stream);
                    byte[] by = stream.toByteArray();
                    tempImage = Base64.encodeToString(by, Base64.DEFAULT);
                    imageTemp.setImageBitmap(img);
                    imageTemp.setVisibility(View.VISIBLE);
                }
            }
    );

    ActivityResultLauncher<Intent> resultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    if (tempImage.equals("")) {
                        tempImage = getTempImg();
                    }
                    img = BitmapFactory.decodeFile(tempImage);
                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    img.compress(Bitmap.CompressFormat.JPEG, 100, stream);
                    byte[] by = stream.toByteArray();
                    tempImage = Base64.encodeToString(by, Base64.DEFAULT);
                    imageTemp.setImageBitmap(img);
                    imageTemp.setVisibility(View.VISIBLE);
                }
            }
    );

    public String getTempImg() {
        return getSharedPreferencesData("TempImg", "");
    }

    public void saveTempImg(String temp) {
        saveSharedPreferencesData("TempImg", temp);
    }

    public void deleteTemp(){
        saveSharedPreferencesData("TempImg", "");
    }

    private String getSharedPreferencesData(String key, String defaultValue) {
        SharedPreferences sr = requireActivity().getSharedPreferences("images", Context.MODE_PRIVATE);
        return sr.getString(key, defaultValue);
    }

    private void saveSharedPreferencesData(String key, String value) {
        SharedPreferences sr = requireActivity().getSharedPreferences("images", Context.MODE_PRIVATE);
        SharedPreferences.Editor se = sr.edit();
        se.putString(key, value);
        se.apply();
    }
    private Uri getOutputFile() {
        File root = requireActivity().getExternalFilesDir("Media");
        String appname = getString(R.string.app_name);
        File myDir = new File(root, appname + " Temp");

        if (!myDir.exists()) {
            if (myDir.mkdirs()) {
                File nomedia = new File(myDir, ".nomedia");
                try {
                    nomedia.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        String fname = "TEMP-";
        File file;
        try {
            file = File.createTempFile(fname, "-" + appname.toUpperCase() + ".jpg", myDir);
            tempImage = file.getPath();
            saveTempImg(tempImage);
            return FileProvider.getUriForFile(requireActivity(), "com.artuok.android.fileprovider", file);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private boolean checkIfIsComplete(){
        boolean ableToFinish = isTag && isText && isDate && isTime;

        check.setAlpha(ableToFinish ? 1f : 0.5f);

        return ableToFinish;
    }

    public interface OnCheckListener{
        void onCheck(View view, long id);
    }

    private long insertAwaiting(String name, long date, int subject) {
        DbHelper dbHelper = new DbHelper(requireActivity());
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        long now = Calendar.getInstance().getTimeInMillis();

        ContentValues values = new ContentValues();
        values.put("date", now);
        values.put("complete_date", now);
        values.put("deadline", date);
        values.put("subject", subject);
        values.put("description", name);
        values.put("status", status);
        values.put("favorite", 0);
        values.put("process_date", 0);
        values.put("user", SettingsFragment.isLogged(requireActivity()) ? FirebaseAuth.getInstance().getCurrentUser().getUid() : "noUser");

        return db.insert(DbHelper.T_TASK, null, values);
    }

    private void saveImagesInDevice(String temp, long id) {
        byte[] encodeByte = Base64.decode(temp, Base64.DEFAULT);
        try {
            Bitmap map = BitmapFactory.decodeByteArray(encodeByte, 0 , encodeByte.length);
            saveImageInDevice(map, id);
        } catch (java.lang.Exception e) {
            e.printStackTrace();
        }

        deleteTemp();
    }

    private void saveImageInDevice(Bitmap image, long id) {
        File root = requireActivity().getExternalFilesDir("Media");
        String appname = getString(R.string.app_name);
        File myDir = new File(root, appname + " Images");

        if (!myDir.exists()) {
            myDir.mkdirs();
        }

        Calendar c = Calendar.getInstance();
        int y = c.get(Calendar.YEAR);
        int m = c.get(Calendar.MONTH);
        int d = c.get(Calendar.DAY_OF_MONTH);

        String mo = (m + 1 < 10) ? "0" + (m + 1) : String.valueOf(m + 1);
        String da = (d < 10) ? "0" + d : String.valueOf(d);

        int i = 0;
        String si = String.valueOf(i);
        String t = "0000".substring(0, 4 - si.length()) + si;
        String fname = appname.toUpperCase() + "-" + y + mo + da + "-IMG" + t + ".jpg";
        File file = new File(myDir, fname);

        while (file.exists()) {
            si = String.valueOf(i);
            t = "0000".substring(0, 4 - si.length()) + si;
            fname = appname.toUpperCase() + "-" + y + mo + da + "-IMG" + t + ".jpg";
            file = new File(myDir, fname);
            i++;
        }

        if (file.exists()) {
            file.delete();
        }

        try {
            FileOutputStream out = new FileOutputStream(file);
            image.compress(Bitmap.CompressFormat.JPEG, 100, out);
            out.flush();
            out.close();

            saveImageInDataBase(id, file);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void saveImageInDataBase(long id, File file) {
        DbHelper dbHelper = new DbHelper(requireActivity());
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        long time = Calendar.getInstance().getTimeInMillis();
        cv.put("awaiting", id);
        cv.put("name", file.getName());
        cv.put("path", file.getPath());
        cv.put("timestamp", time);
        db.insert(DbHelper.T_PHOTOS, null, cv);
    }


}

package com.artuok.appwork.fragmets;

import static android.app.Activity.RESULT_OK;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import com.artuok.appwork.LoginActivity;
import com.artuok.appwork.MainActivity;
import com.artuok.appwork.PhotoSelectActivity;
import com.artuok.appwork.R;
import com.artuok.appwork.db.DbChat;
import com.artuok.appwork.db.DbHelper;
import com.artuok.appwork.dialogs.ImagePreviewDialog;
import com.artuok.appwork.dialogs.PermissionDialog;
import com.artuok.appwork.ProfileActivity;
import com.artuok.appwork.library.Constants;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.thekhaeng.pushdownanim.PushDownAnim;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import io.michaelrocks.libphonenumber.android.NumberParseException;
import io.michaelrocks.libphonenumber.android.PhoneNumberUtil;
import io.michaelrocks.libphonenumber.android.Phonenumber;

public class SettingsFragment extends Fragment {
    Switch darkTheme, savermode;
    SharedPreferences sharedPreferences;
    LinearLayout session, conversation, userSession, notifications, backup, deleteAll, donate;
    TextView logint;
    ImageView photo;
    FirebaseAuth auth = FirebaseAuth.getInstance();
    private List<String> images = new ArrayList<>();
    private List<String> hashimages = new ArrayList<>();
    private String tempImage = "";
    Bitmap map;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_settings, container, false);

        darkTheme = root.findViewById(R.id.change_theme);
        savermode = root.findViewById(R.id.saver_mode);
        sharedPreferences = requireActivity().getSharedPreferences("settings", Context.MODE_PRIVATE);

        restartLaunchers();
        LinearLayout version = root.findViewById(R.id.version);
        session = root.findViewById(R.id.closephonesession);
        conversation = root.findViewById(R.id.deleteconversations);
        logint = root.findViewById(R.id.login);
        userSession = root.findViewById(R.id.loginSetting);
        photo = root.findViewById(R.id.photo);
        notifications = root.findViewById(R.id.notifications_layout);
        backup = root.findViewById(R.id.backup_layout);
        deleteAll = root.findViewById(R.id.delete_all);
        donate = root.findViewById(R.id.donate);
        ((TextView)root.findViewById(R.id.version_code)).setText(Constants.VERSION_CODE);

        photo.setOnClickListener(view -> {
            if (isLogged(requireActivity())) {
                previewImage(photo);
            }
        });

        userSession.setOnClickListener(view -> {
            if (isLogged(requireActivity())) {
                Intent i = new Intent(requireActivity(), ProfileActivity.class);
                i.putExtra("name", "UsuarioPromedio");
                i.putExtra("id", auth.getCurrentUser().getUid());
                startActivity(i);
            } else {
                Intent i = new Intent(requireActivity(), LoginActivity.class);
                startActivity(i);
            }
        });

        PushDownAnim.setPushDownAnimTo(donate)
                        .setOnClickListener(view -> {
                            String url = "https://www.paypal.com/paypalme/ArtworkStudiosDev";
                            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                            startActivity(intent);
                        });

        PushDownAnim
                .setPushDownAnimTo(deleteAll)
                        .setOnClickListener(view -> {
                            PermissionDialog dialog = new PermissionDialog();
                            dialog.setTitleDialog(getString(R.string.delete));
                            dialog.setTextDialog(getString(R.string.ask_delete_all));
                            dialog.setDrawable(R.drawable.ic_trash);
                            dialog.setPositiveText(getString(R.string.Accept_M));
                            dialog.setNegativeText(getString(R.string.Cancel_M));
                            dialog.setPositive((view13, which) -> {
                                DbHelper dbHelper = new DbHelper(requireActivity());
                                SQLiteDatabase db = dbHelper.getWritableDatabase();
                                db.delete(DbHelper.t_subjects, null, null);
                                db.delete(DbHelper.t_event, null, null);
                                db.delete(DbHelper.t_alarm, null, null);
                                db.delete(DbHelper.T_PHOTOS, null, null);
                                db.delete(DbHelper.T_TASK, null, null);
                                DbChat dbChat = new DbChat(requireActivity());
                                SQLiteDatabase dbc = dbChat.getWritableDatabase();
                                dbc.delete(DbChat.T_CHATS, null, null);
                                dbc.delete(DbChat.T_CHATS_EVENT, null, null);
                                dbc.delete(DbChat.T_CHATS_MSG, null, null);
                                Toast.makeText(requireActivity(), getString(R.string.deleted), Toast.LENGTH_SHORT).show();
                            });
                            dialog.setNegative((view14, which) -> dialog.dismiss());
                            dialog.show(requireActivity().getSupportFragmentManager(), getString(R.string.delete));
                        });

        PushDownAnim
                .setPushDownAnimTo(backup)
                .setDurationPush(100)
                .setScale(PushDownAnim.MODE_SCALE, 0.98f)
                .setOnClickListener(view -> {
                    ((MainActivity) requireActivity()).loadExternalFragment(((MainActivity) requireActivity()).backupsFragment, requireActivity().getString(R.string.backup));
                });
        PushDownAnim
                .setPushDownAnimTo(notifications)
                .setDurationPush(100)
                .setScale(PushDownAnim.MODE_SCALE, 0.98f)
                .setOnClickListener(view -> {
                    ((MainActivity) requireActivity()).loadExternalFragment(((MainActivity) requireActivity()).alarmsFragment, requireActivity().getString(R.string.notifications));
                });

        PushDownAnim.setPushDownAnimTo(session)
                .setDurationPush(100)
                .setScale(PushDownAnim.MODE_SCALE, 0.98f)
                .setOnClickListener(view -> {
                    PermissionDialog dialog = new PermissionDialog();

                    dialog.setTitleDialog(requireActivity().getString(R.string.log_out));
                    dialog.setTextDialog(requireActivity().getString(R.string.sure_log_out_text));
                    dialog.setDrawable(R.drawable.power);
                    dialog.setPositive((view12, which) -> {
                        setLogged(requireActivity(), false);

                        FirebaseAuth auth = FirebaseAuth.getInstance();
                        auth.signOut();
                        notifyChatChanged();
                    });
                    dialog.setNegative((view1, which) -> dialog.dismiss());
                    dialog.show(requireActivity().getSupportFragmentManager(), "Session");
                });
        PushDownAnim.setPushDownAnimTo(conversation)
                .setDurationPush(100)
                .setScale(PushDownAnim.MODE_SCALE, 0.98f)
                .setOnClickListener(view -> {
                    PermissionDialog dialog = new PermissionDialog();

                    dialog.setTitleDialog(requireActivity().getString(R.string.del_conv));
                    dialog.setTextDialog(requireActivity().getString(R.string.sure_del_conv));
                    dialog.setDrawable(R.drawable.ic_trash);
                    dialog.setPositive((view12, which) -> {
                        deleteAllConversations();
                    });
                    dialog.setNegative((view1, which) -> dialog.dismiss());
                    dialog.show(requireActivity().getSupportFragmentManager(), getString(R.string.delete));
                    notifyChatChanged();
                });

        boolean s = isLogged(requireActivity());

        if(!s){
            session.setVisibility(View.GONE);
            logint.setText(requireActivity().getString(R.string.Log_In));
        }else{
            session.setVisibility(View.VISIBLE);
            logint.setText(auth.getCurrentUser().getEmail());
        }

        darkThemeSetter();
        saverModeSetter();
        setPhoto();
        return root;
    }


    private static void setLogged(Context context, boolean lg){
        SharedPreferences preferences = context.getSharedPreferences("chat", Context.MODE_PRIVATE);
        SharedPreferences.Editor edit = preferences.edit();
        edit.putBoolean("logged", lg);
        edit.apply();
    }

    public static boolean isLogged(Context context) {
        SharedPreferences preferences = context.getSharedPreferences("chat", Context.MODE_PRIVATE);
        boolean lg = preferences.getBoolean("logged", false);

        return lg;
    }

    public void previewImage(ImageView view){
        ImagePreviewDialog dialog = new ImagePreviewDialog();
        dialog.setDuration(500);
        dialog.setStartPosition(view.getX(), view.getY());

        int windowWidth = requireActivity().getWindow().getAttributes().width;
        int windowHeight = requireActivity().getWindow().getAttributes().height;

        dialog.setEndPosition(windowWidth/2f, windowHeight/2f);

        dialog.setText(requireActivity().getString(R.string.you));
        dialog.setImage(map);

        dialog.show(requireActivity().getSupportFragmentManager(), "imagepreview");
    }

    public void setPhoto(){
        if(auth.getCurrentUser() != null){
            String root = requireActivity().getExternalFilesDir("Media").toString();
            String appname = getString(R.string.app_name);
            File myDir = new File(root, appname+" Profile");

            if (myDir.exists()) {
                String fname = appname.toUpperCase() + "-USER-IMG.jpg";

                File file = new File(myDir, fname);

                if (file.exists()) {
                    map = BitmapFactory.decodeFile(file.getPath());
                    photo.setImageBitmap(map);
                }
            }


            updatedPhotoProfile(requireActivity());
        }
    }

    public static boolean isPhotoProfile(Context context) {
        SharedPreferences s = context.getSharedPreferences("settings", Context.MODE_PRIVATE);
        return s.getBoolean("isPhotoProfile", false);
    }

    public static void setPhotoProfile(Context context, boolean has) {
        SharedPreferences s = context.getSharedPreferences("settings", Context.MODE_PRIVATE);
        SharedPreferences.Editor se = s.edit();
        se.putBoolean("isPhotoProfile", has).apply();
    }



    public static void deletePhoto(Context context, String u) {
        File root = context.getExternalFilesDir("Media");
        String appname = context.getString(R.string.app_name).toUpperCase();
        File myDir = new File(root, ".Profiles");
        if (myDir.exists()) {
            String fname = "CHAT-" + u + "-" + appname + ".jpg";
            File file = new File(myDir, fname);
            if (file.exists()) {
                file.delete();
            }
        }
    }

    public static boolean isSaverModeActive(Context context) {
        SharedPreferences s = context.getSharedPreferences("settings", Context.MODE_PRIVATE);
        return s.getBoolean("datasaver", true);
    }

    public static boolean isMobileData(Context context) {
        boolean mobileDataEnable = false;
        try {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            Class<?> cmClass = Class.forName(cm.getClass().getName());
            Method method = cmClass.getDeclaredMethod("getMobileDataEnabled");
            method.setAccessible(true);
            mobileDataEnable = (boolean) method.invoke(cm);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return mobileDataEnable;
    }

    public void selectPhoto() {
        Dialog dialog = new Dialog(requireActivity());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.bottom_selectimage_layout);

        LinearLayout openCamera = dialog.findViewById(R.id.captureImage);
        LinearLayout openGallery = dialog.findViewById(R.id.obtainImage);

        openCamera.setOnClickListener (view -> {
            if(requireActivity().checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED){
                openCamera();
            }else if (shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                showInContextUI(0);
            } else{
                requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            }
            dialog.dismiss();
        });

        openGallery.setOnClickListener(view -> {
            if(requireActivity().checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED){
                getPictureGallery();
            }else if (shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE)) {
                showInContextUI(1);
            } else{
                requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
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

    private void showInContextUI(int i) {

        PermissionDialog dialog = new PermissionDialog();
        dialog.setTitleDialog(getString(R.string.required_permissions));
        dialog.setDrawable(R.drawable.smartphone);

        if(i == 0){
            dialog.setTextDialog(getString(R.string.permissions_wres));
            dialog.setPositive((view, which) -> requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE));
        }else if(i == 1){
            dialog.setTextDialog(getString(R.string.permissions_wres));
            dialog.setPositive((view, which) ->
                    requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
            );
        }else if( i== 2){
            dialog.setDrawable(R.drawable.camera);
            dialog.setTextDialog(getString(R.string.permissions_camera));
            dialog.setPositive((view, which) ->
                    requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            );
        }


        dialog.setNegative ((view, which) ->
                dialog.dismiss()
        );

        dialog.show(requireActivity().getSupportFragmentManager(), "permissions");
    }

    private ActivityResultLauncher requestPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), result -> {

    });

    private void restartLaunchers() {
        requestPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), result -> {

        });
        resultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Intent data = result.getData();
                        if (data.getIntExtra("requestCode", 0) == 3) {
                        } else if (data.getIntExtra("requestCode", 0) == 2) {
                            setPhoto();
                        }
                    }
                }
        );
        resultForGalleryPicture = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                it -> {
                    if (it.getResultCode() == RESULT_OK) {
                        Uri data = it.getData().getData();
                        Intent i = new Intent(requireActivity(), PhotoSelectActivity.class);
                        i.setData(data);
                        i.putExtra("from", "gallery");
                        i.putExtra("icon", true);
                        i.getIntExtra("requestCode", 2);
                        resultLauncher.launch(i);
                    }
                });
    }

    private void getPictureGallery() {
        saveImages();
        Intent i = new Intent(Intent.ACTION_GET_CONTENT);
        i.setType("image/*");
        resultForGalleryPicture.launch(i);
    }

    private void openCamera() {
        if (requireActivity().checkSelfPermission(Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED){
            saveImages();
            Intent i = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            i.putExtra(MediaStore.EXTRA_OUTPUT, getOutputFile());
            i.putExtra("PathOf", tempImage);
            ((MainActivity)requireActivity()).resultLaunchers.launch(i);
        }else if(shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)){
            showInContextUI(2);
        }else{
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }
    ActivityResultLauncher<Intent> resultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Intent data = result.getData();
                    if (data.getIntExtra("requestCode", 0) == 3) {
                    } else if (data.getIntExtra("requestCode", 0) == 2) {
                        setPhoto();
                    }
                }
            }
    );

    private ActivityResultLauncher<Intent> resultForGalleryPicture = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            it -> {
                if (it.getResultCode() == RESULT_OK) {
                Uri data = it.getData().getData();
                Intent i = new Intent(requireActivity(), PhotoSelectActivity.class);
                i.setData(data);
                i.putExtra("from", "gallery");
                i.putExtra("icon", true);
                i.getIntExtra("requestCode", 2);
                resultLauncher.launch(i);
            }
        });



    String numberPhoneSetter(String number) throws NumberParseException {
        SharedPreferences shared =
                requireActivity().getSharedPreferences("chat", Context.MODE_PRIVATE);
        String code = shared.getString("regionCode", "ZZ");
        PhoneNumberUtil phoneUtil = PhoneNumberUtil.createInstance(requireContext());

        Phonenumber.PhoneNumber phone = phoneUtil.parse(number, code);
        return phoneUtil.format(
                phone,
                PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL
            );
    }


    void notifyChatChanged(){
        ((MainActivity)requireActivity()).notifyToChatChanged();
    }

    void deleteAllConversations() {
        DbChat dbChat = new DbChat(requireActivity());
        SQLiteDatabase db = dbChat.getWritableDatabase();
        db.delete(DbChat.T_CHATS_MSG, "", null);
        db.delete(DbChat.T_CHATS_EVENT, "", null);
        db.delete(DbChat.T_CHATS, "", null);
    }

    void saverModeSetter(){
        boolean a = sharedPreferences.getBoolean("datasaver", true);

        savermode.setChecked(a);
        savermode.setOnCheckedChangeListener((compoundButton, b) -> {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean("datasaver", b);
            editor.apply();
        });
    }

    void darkThemeSetter() {
        boolean a = sharedPreferences.getBoolean("DarkMode", false);

        darkTheme.setChecked(a);
        darkTheme.setOnCheckedChangeListener((compoundButton, b) -> {
            if (b) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            }

            SharedPreferences.Editor editor = sharedPreferences.edit();

            editor.putBoolean("DarkMode", b);
            editor.apply();
        });

    }
        //Images functions start
    private String getTempImg() {
        SharedPreferences sr = requireActivity().getSharedPreferences("images", Context.MODE_PRIVATE);
        return sr.getString("TempImg", "");
    }

    void saveTempImg(String temp){
        SharedPreferences sr = requireActivity().getSharedPreferences("images", Context.MODE_PRIVATE);
        SharedPreferences.Editor se = sr.edit();

        se.putString("TempImg", temp);
        se.apply();
    }

    void resave(){
        SharedPreferences sr = requireActivity().getSharedPreferences("images", Context.MODE_PRIVATE);
        SharedPreferences.Editor se = sr.edit();

        int i = 0;
        for (String x : images){
            String name = "Images$i";
            se.putString(name, x);
            i++;
        }

        for(int x = i; x < 4; x++){
            String name = "Images$x";
            se.putString(name, "");
        }

        se.apply();
    }

    private void saveImage(String s, String temp){
        SharedPreferences sr = requireActivity().getSharedPreferences("images", Context.MODE_PRIVATE);
        SharedPreferences.Editor se = sr.edit();
        se.putString("Images0", s);
        se.putString("ImagesTemp0", temp);
        se.apply();
    }

    private void saveImages(){
        SharedPreferences sr = requireActivity().getSharedPreferences("images", Context.MODE_PRIVATE);
        SharedPreferences.Editor se = sr.edit();

        int i = 1;
        for (String x : images){
            String name = "Images$i";
            se.putString(name, x);
            String tmpname = "ImagesTemp$i";
            se.putString(tmpname, hashimages.get(i-1));
            i++;
        }

        for(int x = i; x < 4; x++){
            String name = "Images$x";
            se.putString(name, "");
            String tmpname = "ImagesTemp$i";
            se.putString(tmpname, "");
        }
        se.apply();
    }
    private Uri getOutputFile(){
        String root = requireActivity().getExternalFilesDir("Media").toString();
        String appname = getString(R.string.app_name);
        File myDir = new File(root, appname+" Temp");
        if(!myDir.exists()){
            boolean m = myDir.mkdirs();
            if(m){
                File nomedia = new File(myDir, ".nomedia");
                try {
                    nomedia.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        String fname = "TEMP-";
        File file = null;
        try {
            file = File.createTempFile(fname, "-"+appname.toUpperCase()+".jpg", myDir);
        } catch (IOException e) {
            e.printStackTrace();
        }
        tempImage = file.getPath();
        saveTempImg(tempImage);
        return FileProvider.getUriForFile(requireActivity(), "com.artuok.android.fileprovider", file);
    }
    //modern functions
    public static void updatedPhotoProfile(Context context) {
        if(FirebaseAuth.getInstance().getCurrentUser() == null) return;
        String user = FirebaseAuth.getInstance().getCurrentUser().getUid();

        FirebaseDatabase.getInstance().getReference().child("user").child(user).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists()){
                    if(snapshot.child("photo").exists()){
                        String photo = snapshot.child("photo").getValue().toString();
                        setPicture(context, user, photo, false);

                        SettingsFragment.setPhotoProfile(context, true);
                    }else{
                        deleteBitmapPicture(context);
                        SettingsFragment.setPhotoProfile(context, false);

                    }

                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private static void deleteBitmapPicture(Context context){
        File root = context.getExternalFilesDir("Media");
        String appName = context.getString(R.string.app_name).toUpperCase();
        File myDir = new File(root, appName + " Profile");

        if(myDir.exists()){
            String fname = appName + "-USER-IMG.jpg";
            File file = new File(myDir, fname);
            if(file.exists())
                file.delete();
        }
    }

    private static void setPicture(Context context, String user, String name, boolean cache){
        File file = null;

        if(cache){
            file = new File(context.getCacheDir(), name+".jpg");
        }else{
            String appName = context.getString(R.string.app_name).toUpperCase();
            File media = context.getExternalFilesDir("Media");
            File root = new File(media, appName+" Profile");
            if(!root.exists()) {
                if (root.mkdirs()) {
                    file = new File(root, appName + "-USER-IMG.jpg");
                }
            } else {
                file = new File(root, appName+"-USER-IMG.jpg");
            }
        }

        if(file != null)
            if(!file.exists()){
                try {
                    file.createNewFile();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            FirebaseStorage.getInstance().getReference().child("chats").child(user).child(name+".jpg")
                .getFile(file).addOnCompleteListener(task -> {
                    if(task.isSuccessful()){

                    }
                });
    }

    private static Bitmap getBitmapPicture(Context context){
        File root = context.getExternalFilesDir("Media");
        String appName = context.getString(R.string.app_name).toUpperCase();
        File myDir = new File(root, appName + " Profile");

        if(myDir.exists()){
            String fname = appName + "-USER-IMG.jpg";
            File file = new File(myDir, fname);
            if(file.exists())
                return BitmapFactory.decodeFile(file.getPath());
        }
        return null;
    }
}
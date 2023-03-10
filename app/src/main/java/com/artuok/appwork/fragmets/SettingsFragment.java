package com.artuok.appwork.fragmets;

import static android.app.Activity.RESULT_OK;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import com.artuok.appwork.MainActivity;
import com.artuok.appwork.PhotoSelectActivity;
import com.artuok.appwork.R;
import com.artuok.appwork.db.DbChat;
import com.artuok.appwork.dialogs.ImagePreviewDialog;
import com.artuok.appwork.dialogs.PermissionDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import com.thekhaeng.pushdownanim.PushDownAnim;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SettingsFragment extends Fragment {


    Switch darkTheme, savermode;
    SharedPreferences sharedPreferences;
    LinearLayout session, conversation, userSession;
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
        View root = inflater.inflate(R.layout.settings_fragment, container, false);

        darkTheme = root.findViewById(R.id.change_theme);
        savermode = root.findViewById(R.id.saver_mode);
        sharedPreferences = requireActivity().getSharedPreferences("settings", Context.MODE_PRIVATE);

        LinearLayout version = root.findViewById(R.id.version);
        session = root.findViewById(R.id.closephonesession);
        conversation = root.findViewById(R.id.deleteconversations);
        logint = root.findViewById(R.id.login);
        userSession = root.findViewById(R.id.loginSetting);
        photo = root.findViewById(R.id.photo);

        photo.setOnClickListener(view -> {
            if(isLogged(requireActivity())) {
                previewImage(photo);
            }
        });

        userSession.setOnClickListener(view -> {
            if(isLogged(requireActivity())){
                selectPhoto();
            }
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
                    dialog.show(requireActivity().getSupportFragmentManager(), "Conversation");
                    notifyChatChanged();
                });

        boolean s = isLogged(requireActivity());

        if(!s){
            session.setVisibility(View.GONE);
            logint.setText(requireActivity().getString(R.string.Log_In));
        }else{
            session.setVisibility(View.VISIBLE);
            try {
                logint.setText(numberPhoneSetter(auth.getCurrentUser().getPhoneNumber()));
            } catch (NumberParseException e) {
                e.printStackTrace();
            }
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

    private static boolean isLogged(Context context){
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

            if(myDir.exists()){
                String fname = appname.toUpperCase()+"-USER-IMG.jpg";

                File file = new File(myDir, fname);

                if(file.exists()){
                    map = BitmapFactory.decodeFile(file.getPath());
                    photo.setImageBitmap(map);
                }
            }
        }
    }

    public void selectPhoto(){
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

    private void getPictureGallery(){
        saveImages();
        Intent i = new Intent(Intent.ACTION_GET_CONTENT);
        i.setType("image/*");
        resultForGalleryPicture.launch(i);
    }

    private void openCamera() {
        if(requireActivity().checkSelfPermission(Manifest.permission.CAMERA)
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
        PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();

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
    }
    void deleteAllContacts() {
        DbChat dbChat = new DbChat(requireActivity());
        SQLiteDatabase db = dbChat.getWritableDatabase();

        db.delete(DbChat.T_CHATS_LOGGED, "", null);
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
    //Images functions end
}
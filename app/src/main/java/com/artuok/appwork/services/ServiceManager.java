package com.artuok.appwork.services;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.Person;
import androidx.core.graphics.drawable.IconCompat;

import com.artuok.appwork.InActivity;
import com.artuok.appwork.R;
import com.artuok.appwork.db.DbChat;
import com.artuok.appwork.db.DbHelper;
import com.artuok.appwork.fragmets.SettingsFragment;
import com.artuok.appwork.library.Constants;
import com.artuok.appwork.library.MessageControler;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class ServiceManager extends Service {
    private final Binder mBinder = new ServiceManagerBinder();

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        foregroundNotify();
    }

    public void foregroundNotify() {
        Notification foreground = new NotificationCompat.Builder(this, InActivity.CHANNEL_ID_2)
                .setSmallIcon(R.drawable.ic_stat_name)
                .setPriority(NotificationManager.IMPORTANCE_MIN)
                .setSilent(true)
                .setAutoCancel(true)
                .build();
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        assert manager != null;
        manager.notify(3, foreground);
        startForeground(3, foreground);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent.getAction() != null) {
            switch (intent.getAction()) {
                case AlarmWorkManager.ACTION_MESSAGES:
                    refreshMessages();
                    destroy();
                    break;
                case AlarmWorkManager.ACTION_SET_BACKUP:
                    createCSV();
                    destroy();
                    break;
                case AlarmWorkManager.ACTION_RESTORE_BACKUP:
                    try {
                        downloadFromFirestore();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    destroy();
                    break;
            }
        }

        return START_STICKY;
    }

    public void destroy() {
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        if (manager != null) {
            stopForeground(true);
            manager.cancel(3);
        }
        stopSelf();
    }

    public class ServiceManagerBinder extends Binder {
        public ServiceManager getServices() {
            return ServiceManager.this;
        }
    }

    private void setNotification(int progress, int max, String content) {
        Notification notification = new NotificationCompat.Builder(this, InActivity.CHANNEL_ID_1)
                .setSmallIcon(R.drawable.ic_stat_name)
                .setColor(Color.parseColor("#1982C4"))
                .setContentTitle("Backup")
                .setContentText(content)
                .setShowWhen(true)
                .setOnlyAlertOnce(false)
                .setAutoCancel(true)
                .setProgress(max, progress, false)
                .setGroup(InActivity.GROUP_SUBJECTS)
                .build();

        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        assert manager != null;
        manager.notify(600000, notification);
    }

    private void readCSV(){
        File carpet = new File(getExternalFilesDir("Media"), "Sunfy Backup");
        if (carpet.exists())
            try {
                File backupFile = new File(carpet, "sunfy_backup.csv");
                if (!backupFile.exists()) {
                    Toast.makeText(this, "No backup found.", Toast.LENGTH_SHORT).show();
                    return;
                }
                BufferedReader reader = new BufferedReader(new FileReader(backupFile));
                String line;
                DbHelper helper = new DbHelper(this);
                SQLiteDatabase db = helper.getWritableDatabase();
                db.execSQL("DROP TABLE " + DbHelper.t_subjects);
                db.execSQL("DROP TABLE " + DbHelper.T_TASK);
                DbHelper.createTables(db);
                int i = 0;
                boolean isTasks = false;
                while ((line = reader.readLine()) != null) {
                    if(line.equals("../../../..")){
                        isTasks = true;
                        i = 0;
                    }
                    if(isTasks){
                        if (i > 1) {
                            String[] taskData = line.split(",");
                            int id = Integer.parseInt(taskData[0]);
                            long date = Long.parseLong(taskData[1]);
                            long end_date = Long.parseLong(taskData[2]);
                            int subject = Integer.parseInt(taskData[3]);
                            String desc = taskData[4];
                            int status = Integer.parseInt(taskData[5]);
                            String user = taskData[6];
                            int favorite = Integer.parseInt(taskData[7]);
                            long completed_date = Long.parseLong(taskData[8]);
                            ContentValues values = new ContentValues();
                            values.put("id", id);
                            values.put("date", date);
                            values.put("end_date", end_date);
                            values.put("subject", subject);
                            values.put("description", desc);
                            values.put("status", status);
                            values.put("user", user);
                            values.put("favorite", favorite);
                            values.put("completed_date", completed_date);
                            db.insert(DbHelper.T_TASK, null, values);
                        }
                    }else{
                        if(i != 0){
                            String[] subjectData = line.split(",");
                            int id = Integer.parseInt(subjectData[0]);
                            String name = subjectData[1];
                            int color = Integer.parseInt(subjectData[2]);
                            ContentValues values = new ContentValues();
                            values.put("id", id);
                            values.put("name", name);
                            values.put("color", color);
                            db.insert(DbHelper.t_subjects, null, values);
                        }
                    }


                    i++;
                }
                db.close();
                reader.close();
                Toast.makeText(this, getString(R.string.tasks_restored_successfully), Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "Task restoration failed.", Toast.LENGTH_SHORT).show();
            }
        else
            Toast.makeText(this, "No backup found.", Toast.LENGTH_SHORT).show();
    }

    private void uploadToFirestore(File file) throws FileNotFoundException {
        if (SettingsFragment.isLogged(this)) {
            FirebaseAuth auth = FirebaseAuth.getInstance();
            FirebaseStorage ref = FirebaseStorage.getInstance();
            StorageReference reference = ref.getReference().child("chats/" + auth.getCurrentUser().getUid() + "/backups/sunfy_backup.csv");
            FileInputStream stream = new FileInputStream(file);
            UploadTask uploading = reference.putStream(stream);
            uploading.addOnSuccessListener(taskSnapshot -> {
                NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                if (manager != null) {
                    manager.cancel(600000);
                }
                Toast.makeText(ServiceManager.this, "Backup uploaded successfully!", Toast.LENGTH_SHORT).show();
            }).addOnProgressListener(snapshot -> setNotification((int) (100 / snapshot.getTotalByteCount() * snapshot.getBytesTransferred()), 100, "Uploading backup..."));
        }
    }

    private void downloadFromFirestore() throws IOException {
        if (SettingsFragment.isLogged(this) && Constants.isInternetAvailable(this)) {
            File carpet = new File(getExternalFilesDir("Media"), "Sunfy Backup");

            if (!carpet.exists())
                if (!carpet.mkdirs())
                    return;

            File backupFile = new File(carpet, "sunfy_backup.csv");
            if (!backupFile.exists())
                if (!backupFile.createNewFile())
                    return;


            FirebaseAuth auth = FirebaseAuth.getInstance();
            FirebaseStorage ref = FirebaseStorage.getInstance();
            StorageReference reference = ref.getReference().child("chats").child(auth.getCurrentUser().getUid())
                    .child("backups").child("sunfy_backup.csv");


            reference.getFile(backupFile)
                    .addOnCompleteListener(task -> {
                        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                        if (manager != null) {
                            manager.cancel(600000);
                        }
                        if(task.isSuccessful()){
                            readCSV();
                        }
                    })
                    .addOnProgressListener(snapshot -> setNotification((int) (100 / snapshot.getTotalByteCount() * snapshot.getBytesTransferred()), 100, "Downloading backup..."));
        } else {
            readCSV();
        }
    }

    private long checkSubject(String name) {
        DbHelper helper = new DbHelper(this);
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT * FROM " + DbHelper.t_subjects + " WHERE name = '" + name + "'", null);
        if (c.moveToFirst()) {
            int w = c.getInt(0);
            c.close();
            return w;
        } else {
            SQLiteDatabase dbw = helper.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put("name", name);
            values.put("color", 0XEA1E63);
            return dbw.insert(DbHelper.t_subjects, null, values);
        }
    }

    private void createCSV(){
        try {
            File carpet = new File(getExternalFilesDir("Media"), "Sunfy Backup");
            if (!carpet.exists())
                if (!carpet.mkdirs())
                    return;

            File backupFile = new File(carpet, "sunfy_backup.csv");
            if (!backupFile.exists())
                if (!backupFile.createNewFile())
                    return;

            DbHelper helper = new DbHelper(this);
            SQLiteDatabase db = helper.getReadableDatabase();
            Cursor tasks = db.rawQuery("SELECT * FROM " + DbHelper.T_TASK, null);
            Cursor subjects = db.rawQuery("SELECT * FROM " + DbHelper.t_subjects, null);


            FileWriter writer = new FileWriter(backupFile);
            if(tasks.moveToFirst() && subjects.moveToFirst()){
                int numColumns = subjects.getColumnCount();
                for (int i = 1; i < numColumns; i++) {
                    writer.write(subjects.getColumnName(i) + ",");
                }
                writer.write("\n");

                int max = tasks.getCount() + subjects.getCount();
                int i = 0;
                do {
                    int id = subjects.getInt(0);
                    String name = subjects.getString(1);
                    int color = subjects.getInt(2);
                    String subjectData = id+","+name+","+color+"\n";
                    setNotification(i, max, getString(R.string.creating_backup));
                    writer.write(subjectData);
                }while(subjects.moveToNext());
                writer.write("../../../..\n");
                numColumns = subjects.getColumnCount();
                for (int j = 1; j < numColumns; j++) {
                    writer.write(subjects.getColumnName(j) + ",");
                }
                writer.write("\n");
                do {
                    int id = tasks.getInt(0);
                    long date = tasks.getLong(1);
                    long end_date = tasks.getLong(2);
                    int subjectId = tasks.getInt(3);

                    String desc = tasks.getString(4);
                    int status = tasks.getInt(5);
                    String user = tasks.getString(6);
                    int favorite = tasks.getInt(7);
                    long completed_date = tasks.getLong(8);
                    String taskData = id + "," + date + "," + end_date + "," + subjectId + "," + desc + "," + status + "," + user + "," + favorite + "," + completed_date + "\n";
                    setNotification(i, max, getString(R.string.creating_backup));
                    writer.write(taskData);
                }while(tasks.moveToNext());
                NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                if (manager != null) {
                    manager.cancel(600000);
                }
                uploadToFirestore(backupFile);
                tasks.close();
                subjects.close();
                writer.close();
                Toast.makeText(this, "Backup created successfully!", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void refreshMessages() {
        SharedPreferences sharedPreferences =
                getSharedPreferences("chat", Context.MODE_PRIVATE);
        boolean login = sharedPreferences.getBoolean("logged", false);

        if (!login)
            return;

        MessageControler.restateUserChat(this, () -> {
            notifyUnreadedMessages();
        });
    }

    private void notifyUnreadedMessages() {
        DbChat dbChat = new DbChat(this);
        SQLiteDatabase db = dbChat.getReadableDatabase();
        Cursor chat = db.rawQuery("SELECT * FROM " + DbChat.T_CHATS_MSG + " WHERE status < '3' AND type = '1' GROUP BY chat", null);

        if (chat.moveToFirst()) {
            Notification notification = new NotificationCompat.Builder(this, InActivity.CHANNEL_ID_6)
                    .setSmallIcon(R.drawable.ic_stat_name)
                    .setColor(Color.parseColor("#1982C4"))
                    .setGroup(InActivity.GROUP_MESSAGES)
                    .setGroupSummary(true)
                    .build();
            int ni = 500000;
            NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            assert manager != null;
            manager.notify(ni, notification);
            int ic = 0;
            do {
                ic++;
                int idChat = chat.getInt(7);
                Cursor dataChat = db.rawQuery("SELECT * FROM " + DbChat.T_CHATS + " WHERE id = '" + idChat + "'", null);
                if (dataChat.moveToFirst()) {
                    String name = dataChat.getString(1);
                    if(!name.isEmpty()){
                        Bitmap iconB = BitmapFactory.decodeResource(getResources(), R.mipmap.usericon);
                        IconCompat icon = IconCompat.createWithBitmap(getCroppedBitMap(iconB));
                        Person person = new Person.Builder()
                                .setName(name)
                                .setIcon(icon)
                                .build();
                        NotificationCompat.MessagingStyle m = new NotificationCompat.MessagingStyle(person);
                        Cursor messages = db.rawQuery("SELECT * FROM " + DbChat.T_CHATS_MSG + " WHERE chat = '" + idChat + "' AND status < '3' AND type = '1'", null);
                        if (messages.moveToFirst()) {
                            do {
                                String message = messages.getString(1);
                                long time = messages.getLong(3);

                                if (message.equals(" 1"))
                                    message = getString(R.string.task);
                                m.addMessage(message, time, person);
                            } while (messages.moveToNext());
                        }
                        Notification notificationMessage = new NotificationCompat.Builder(this, InActivity.CHANNEL_ID_6)
                                .setSmallIcon(R.drawable.ic_stat_name)
                                .setColor(Color.parseColor("#1982C4"))
                                .setGroup(InActivity.GROUP_MESSAGES)
                                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                                .setStyle(m)
                                .build();

                        manager.notify(ni + ic, notificationMessage);
                        messages.close();
                    }

                }
                dataChat.close();
            } while (chat.moveToNext());
        }
        chat.close();
    }

    private Bitmap getCroppedBitMap(Bitmap bitmap) {
        Bitmap output = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        final int color = 0xff424242;
        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());

        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(color);
        canvas.drawCircle(bitmap.getWidth() / 2, bitmap.getHeight() / 2, bitmap.getWidth() / 2, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);
        return output;
    }
}

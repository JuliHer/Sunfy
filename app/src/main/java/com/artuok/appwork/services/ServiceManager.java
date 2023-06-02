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
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.Person;
import androidx.core.graphics.drawable.IconCompat;

import com.artuok.appwork.InActivity;
import com.artuok.appwork.R;
import com.artuok.appwork.db.DbChat;
import com.artuok.appwork.db.DbHelper;
import com.artuok.appwork.fragmets.SettingsFragment;
import com.artuok.appwork.library.MessageSender;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseError;
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
                    exportCSV();
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

    private void importCSV() {
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
                db.execSQL("DROP TABLE " + DbHelper.T_TASK);
                DbHelper.createTables(db);
                int i = 0;
                while ((line = reader.readLine()) != null) {
                    if (i != 0) {
                        String[] taskData = line.split(",");
                        long date = Long.parseLong(taskData[0]);
                        long end_date = Long.parseLong(taskData[1]);
                        int subject = (int) checkSubject(taskData[2]);
                        String desc = taskData[3];
                        int status = Integer.parseInt(taskData[4]);
                        String user = taskData[5];
                        int favorite = Integer.parseInt(taskData[6]);
                        long completed_date = Long.parseLong(taskData[7]);
                        ContentValues values = new ContentValues();
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
                    i++;
                }
                db.close();
                reader.close();
                Toast.makeText(this, "Tasks restored successfully!", Toast.LENGTH_SHORT).show();
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
        if (SettingsFragment.isLogged(this)) {
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
            StorageReference reference = ref.getReference().child("chats/" + auth.getCurrentUser().getUid() + "/backups/sunfy_backup.csv");
            reference.getFile(backupFile)
                    .addOnSuccessListener(taskSnapshot -> {
                        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                        if (manager != null) {
                            manager.cancel(600000);
                        }
                        importCSV();
                    })
                    .addOnProgressListener(snapshot -> setNotification((int) (100 / snapshot.getTotalByteCount() * snapshot.getBytesTransferred()), 100, "Downloading backup..."));
        } else {
            importCSV();
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

    private void exportCSV() {
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
            Cursor cursor = db.rawQuery("SELECT * FROM " + DbHelper.T_TASK, null);

            FileWriter writer = new FileWriter(backupFile);

            // Escribir los datos en el archivo CSV
            int numColumns = cursor.getColumnCount();
            for (int i = 1; i < numColumns; i++) {
                writer.write(cursor.getColumnName(i) + ",");
            }
            writer.write("\n");

            int i = 0;
            int max = cursor.getCount();

            while (cursor.moveToNext()) {
                long date = cursor.getLong(cursor.getColumnIndexOrThrow("date"));
                long end_date = cursor.getLong(cursor.getColumnIndexOrThrow("end_date"));
                int subjectId = cursor.getInt(cursor.getColumnIndexOrThrow("subject"));
                String subjectT = "";
                Cursor c = db.rawQuery("SELECT * FROM " + DbHelper.t_subjects + " WHERE id = '" + subjectId + "'", null);
                if (c.moveToFirst()) {
                    subjectT = c.getString(c.getColumnIndexOrThrow("name"));
                }
                c.close();
                String desc = cursor.getString(cursor.getColumnIndexOrThrow("description"));
                int status = cursor.getInt(cursor.getColumnIndexOrThrow("status"));
                String user = cursor.getString(cursor.getColumnIndexOrThrow("user"));
                int favorite = cursor.getInt(cursor.getColumnIndexOrThrow("favorite"));
                long completed_date = cursor.getLong(cursor.getColumnIndexOrThrow("completed_date"));
                String taskData = date + "," + end_date + "," + subjectT + "," + desc + "," + status + "," + user + "," + favorite + "," + completed_date + "\n";
                setNotification(i, max, "Creating backup...");
                writer.write(taskData);
            }
            NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (manager != null) {
                manager.cancel(600000);
            }
            uploadToFirestore(backupFile);
            cursor.close();
            writer.close();
            Toast.makeText(this, "Backup created successfully!", Toast.LENGTH_SHORT).show();
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

        MessageSender ms = new MessageSender(this);
        ms.loadGlobalChats();

        ms.loadGlobalMessages(new MessageSender.OnLoadMessagesListener() {
            @Override
            public void onLoadMessages(boolean newMessages) {
                if (newMessages) {
                    notifyUnreadedMessages();
                }
            }

            @Override
            public void onFailure(DatabaseError databaseError) {

            }
        });
    }

    private void notifyUnreadedMessages() {
        DbChat dbChat = new DbChat(this);
        SQLiteDatabase db = dbChat.getReadableDatabase();
        Cursor chat = db.rawQuery("SELECT * FROM " + DbChat.T_CHATS_MSG + " WHERE status < '3' AND me = '1' GROUP BY chat", null);

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
                int idChat = chat.getInt(8);
                Cursor dataChat = db.rawQuery("SELECT * FROM " + DbChat.T_CHATS + " WHERE id = '" + idChat + "'", null);
                if (dataChat.moveToFirst()) {
                    String name = dataChat.getString(1);

                    Bitmap iconB = BitmapFactory.decodeResource(getResources(), R.mipmap.usericon);
                    IconCompat icon = IconCompat.createWithBitmap(getCroppedBitMap(iconB));
                    Person person = new Person.Builder()
                            .setName(name)
                            .setIcon(icon)
                            .build();
                    NotificationCompat.MessagingStyle m = new NotificationCompat.MessagingStyle(person);
                    Cursor messages = db.rawQuery("SELECT * FROM " + DbChat.T_CHATS_MSG + " WHERE chat = '" + idChat + "' AND status < '3' AND me = '1'", null);
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

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
import com.artuok.appwork.library.Constants;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import org.checkerframework.checker.units.qual.C;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class ServiceManager extends Service {
    private final Binder mBinder = new ServiceManagerBinder();
    private static final String CSV_COLUMN_SEPARATOR = ",~~";
    private static final String CSV_TABLE_SEPARATOR = "../../../..";
    private static final String[] tablesToBackup = new String[]{DbHelper.T_PROJECTS, DbHelper.T_TAG, DbHelper.T_TASK, DbHelper.t_event};

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
                    createBackupCSV();
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

                updateTables(reader, tablesToBackup);
                Toast.makeText(this, getString(R.string.tasks_restored_successfully), Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
    }

    private void updateTables(BufferedReader reader, String... tables) throws IOException {
        DbHelper helper = new DbHelper(this);
        SQLiteDatabase db = helper.getWritableDatabase();
        for (String table : tables) {
            db.execSQL("DROP TABLE " + table);
        }
        DbHelper.createTables(db);
        String line;
        int i = 0;
        int t = 0;
        String[] columnsName = new String[0];
        while ((line = reader.readLine()) != null){
            if(line.equals(CSV_TABLE_SEPARATOR)){
                i = 0;
                t++;
            }else {
                if(i == 0){
                    columnsName = line.split(CSV_COLUMN_SEPARATOR);
                }else{
                    String[] data = line.split(CSV_COLUMN_SEPARATOR);
                    ContentValues values = new ContentValues();
                    for (int j = 0; j < columnsName.length; j++) {
                        values.put(columnsName[j], data[j]);
                    }
                    db.insert(tables[t], null, values);
                }
                i++;
            }
        }
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

    private void createBackupCSV(){
        try{
            File carpet = new File(getExternalFilesDir("Media"), "Sunfy Backup");
            if (!carpet.exists())
                if (!carpet.mkdirs())
                    return;
            File backupFile = new File(carpet, "sunfy_backup.csv");
            if (!backupFile.exists())
                if (!backupFile.createNewFile())
                    return;

            FileWriter writer = new FileWriter(backupFile);

            writeTableBackup(writer, tablesToBackup);

            uploadToFirestore(backupFile);
            writer.flush();
            writer.close();
            Toast.makeText(this, getString(R.string.backup_created_successfully), Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private FileWriter writeTableBackup(FileWriter writer, String... tableName) throws IOException {
        DbHelper helper = new DbHelper(this);
        SQLiteDatabase db = helper.getReadableDatabase();
        setNotification(0, 100, getString(R.string.creating_backup));
        for (int j = 0; j < tableName.length; j++) {
            Cursor table = db.rawQuery("SELECT * FROM " + tableName[j], null);
            if(table.moveToFirst()){
                int numColumns = table.getColumnCount();
                for (int i = 0; i < numColumns; i++) {
                    writer.append(table.getColumnName(i));
                    if(i < numColumns-1)
                        writer.append(CSV_COLUMN_SEPARATOR);
                }
                writer.write("\n");

                do{
                    for (int i = 0; i < numColumns; i++) {
                        writer.append(table.getString(i));
                        if(i < numColumns-1)
                            writer.append(CSV_COLUMN_SEPARATOR);
                    }
                    writer.append("\n");
                }while (table.moveToNext());
            }

            if(j < tableName.length-1){
                writer.append("../../../..");
                writer.append("\n");
            }

            setNotification(100/tableName.length*j, 100, getString(R.string.creating_backup));
            table.close();
        }
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.cancel(600000);
        }
        return writer;
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
            Cursor subjects = db.rawQuery("SELECT * FROM " + DbHelper.T_TAG, null);


            FileWriter writer = new FileWriter(backupFile);
            if(tasks.moveToFirst() && subjects.moveToFirst()){
                int numColumns = subjects.getColumnCount();
                for (int i = 1; i < numColumns; i++) {
                    writer.write(subjects.getColumnName(i) + ",~~");
                }
                writer.write("\n");

                int max = tasks.getCount() + subjects.getCount();
                int i = 0;
                do {
                    int id = subjects.getInt(0);
                    String name = subjects.getString(1);
                    int color = subjects.getInt(2);
                    String subjectData = id+",~~"+name+",~~"+color+"\n";
                    setNotification(i, max, getString(R.string.creating_backup));
                    writer.write(subjectData);
                }while(subjects.moveToNext());
                writer.write("../../../..\n");
                numColumns = tasks.getColumnCount();
                for (int j = 1; j < numColumns; j++) {
                    writer.write(tasks.getColumnName(j) + ",~~");
                }
                writer.write("\n");
                do {
                    int id = tasks.getInt(0);
                    long date = tasks.getLong(2);
                    long end_date = tasks.getLong(5);
                    int subjectId = tasks.getInt(6);

                    String desc = tasks.getString(1);
                    int status = tasks.getInt(7);
                    String user = tasks.getString(8);
                    int favorite = tasks.getInt(9);
                    long completed_date = tasks.getLong(4);
                    String taskData = id + ",~~" + date + ",~~" + end_date + ",~~" + subjectId + ",~~" + desc + ",~~" + status + ",~~" + user + ",~~" + favorite + ",~~" + completed_date + "\n";
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
                Toast.makeText(this, getString(R.string.backup_created_successfully), Toast.LENGTH_SHORT).show();
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

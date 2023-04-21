package com.artuok.appwork.services;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
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

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.Person;
import androidx.core.graphics.drawable.IconCompat;

import com.artuok.appwork.InActivity;
import com.artuok.appwork.R;
import com.artuok.appwork.db.DbChat;
import com.artuok.appwork.library.MessageSender;
import com.google.firebase.database.DatabaseError;

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
        startForeground(1, foreground);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent.getAction() != null) {
            switch (intent.getAction()) {
                case AlarmWorkManager.ACTION_MESSAGES:
                    refreshMessages();
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

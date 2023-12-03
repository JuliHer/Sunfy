package com.artuok.appwork.services;

import android.app.Notification;
import android.app.NotificationManager;
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
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.Person;
import androidx.core.graphics.drawable.IconCompat;

import com.artuok.appwork.InActivity;
import com.artuok.appwork.R;
import com.artuok.appwork.db.DbChat;
import com.artuok.appwork.fragmets.SettingsFragment;
import com.artuok.appwork.library.ChatControler;
import com.artuok.appwork.library.Constants;
import com.artuok.appwork.library.Message;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.google.firebase.storage.FirebaseStorage;

import java.io.File;
import java.util.Calendar;
import java.util.Objects;

public class FirebaseMessageService extends FirebaseMessagingService {

    FirebaseDatabase database = FirebaseDatabase.getInstance();
    FirebaseAuth auth = FirebaseAuth.getInstance();
    @Override
    public void onMessageReceived(@NonNull RemoteMessage message) {
        if(SettingsFragment.isLogged(this)){
            String chat = message.getData().get("chat");
            String name = message.getData().get("name");
            String chatCode = message.getData().get("code");
            String photo = message.getData().get("photo");
            String user = message.getData().get("user");
            String said = message.getData().get("message");
            String messageId = message.getData().get("id");
            String messageReply = message.getData().get("reply");
            if(!Objects.equals(user, FirebaseAuth.getInstance().getCurrentUser().getUid()))
                return;
            NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            assert manager != null;

            int id = -1;
            if(!ChatControler.Companion.existChat(this, chat)){
                id = (int) ChatControler.Companion.createChat(name, chat, chatCode, photo, "", this);
            }else{
                ChatControler.Companion.changeChatName(this, chat, name);
                id = ChatControler.Companion.getChatByString(this, chat);
            }
            ChatControler.Companion.updateMessage(this, chat, messageId, 2);

            Message.Builder msg = new Message.Builder(said)
                    .setId(messageId)
                    .setStatus(2)
                    .setUser(user);

            if (messageReply != null && !messageReply.isEmpty()) {
                msg.setReplyId(messageReply);
                said = getString(R.string.answered)+": "+said;
            }

            boolean alreadySaved = ChatControler.Companion.checkIfExists(this, messageId);
            ChatControler.Companion.saveNotify(this, msg.build(), id);
            long num = id;

            Bitmap pc;

            String say = said;

            if (!TextUtils.isEmpty(photo)) {
                pc = checkAndSetPicture(photo);

                if (pc == null) {
                    String root = getExternalCacheDir().toString();
                    String fName = photo + ".jpg";
                    File file = new File(root, fName);

                    FirebaseStorage.getInstance().getReference()
                            .child("chats").child(user).child(fName)
                            .getFile(file)
                            .addOnCompleteListener(task -> {
                                Bitmap pc1 = checkAndSetPicture(photo);

                                if (pc1 != null) {
                                    if(!alreadySaved)
                                        processNotification(manager, pc1, name, user, messageId, say, (int) num);
                                }
                            });
                }else{
                    if(!alreadySaved)
                        processNotification(manager, pc, name, user, messageId, say, (int) num);
                }
            }else{
                pc = BitmapFactory.decodeResource(getResources(), R.mipmap.usericon);
                if(!alreadySaved)
                    processNotification(manager, pc, name, user, messageId, say, (int) num);
            }
        }
    }

    private void processNotification(NotificationManager manager, Bitmap pc, String name, String user, String mKey, String said, int num) {
        IconCompat icon = IconCompat.createWithBitmap(getCroppedBitMap(pc));
        Person person = new Person.Builder()
                .setName(name)
                .setIcon(icon)
                .setKey(user)
                .build();

        long time = Calendar.getInstance().getTimeInMillis();
        NotificationCompat.MessagingStyle m = new NotificationCompat.MessagingStyle(person);
        DbChat dbChat = new DbChat(this);
        SQLiteDatabase dbw = dbChat.getReadableDatabase();
        Cursor c = dbw.rawQuery("SELECT * FROM "+DbChat.T_CHATS_MSG+" WHERE chat = ? AND mid != ? AND type != 0 AND status < 3", new String[]{num+"", mKey});
        if(c.moveToFirst()){
            do{
                String message = c.getString(1);
                String reply = c.getString(6);
                if(reply != null && !reply.isEmpty()){
                    message = getString(R.string.answered)+": "+message;
                }
                m.addMessage(message, time, person);
            }while (c.moveToNext());
        }
        c.close();
        m.addMessage(said, time, person);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, InActivity.CHANNEL_ID_6)
                .setSmallIcon(R.drawable.ic_stat_name)
                .setColor(Color.parseColor("#1982C4"))
                .setGroup(InActivity.GROUP_MESSAGES)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setStyle(m);

        manager.notify((int) (8000+num), builder.build());
    }

    private Bitmap checkAndSetPicture(String picture){
        String root = getExternalCacheDir().toString();
        String fName = picture+".jpg";
        File file = new File(root, fName);
        if (file.exists()) {
            return BitmapFactory.decodeFile(file.getPath());
        }
        return null;
    }

    @Override
    public void onNewToken(@NonNull String token) {
        if(SettingsFragment.isLogged(this)){
            String userId = auth.getCurrentUser().getUid();
            String device = Constants.getDeviceName();
            database.getReference().child("user")
                    .child(userId)
                    .child("device")
                    .child(device)
                    .setValue(token);
        }
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

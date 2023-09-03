package com.artuok.appwork.library;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.artuok.appwork.db.DbChat;
import com.artuok.appwork.fragmets.homeFragment;
import com.artuok.appwork.objects.ChatElement;
import com.artuok.appwork.objects.EventMessageElement;
import com.artuok.appwork.objects.Item;
import com.artuok.appwork.objects.MessageElement;
import com.artuok.appwork.objects.TextElement;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.messaging.Constants;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.RemoteMessage;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Objects;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MessageControler {
    private String id;
    private String uId;
    private Context context;
    private ChatType type;

    private String chatCode;

    private String name;


    public MessageControler(Context context, String id, String name, ChatType type){
        this.id = id;
        this.context = context;
        this.type = type;
        this.uId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        this.name = name;

        if(type == ChatType.SEARCH || type == ChatType.CONTACT){
            loadChat(id, uId);
        }else if(type == ChatType.GROUP){
            this.chatCode = id;
        }
    }


    private void  loadChat(String id, String uId){
        if(type == ChatType.SEARCH) {
            String[] usersArr = new String[]{
                     id, uId
             };
            Arrays.sort(usersArr);
            this.chatCode = usersArr[0] + usersArr[1];
        }else{
            this.chatCode = id;
        }
    }

    public String getChatCode() {
        return chatCode;
    }

    public static void send(Context context, String chat, Message msg){
        long chatId = chatByKey(chat, context);
        if(chatId> 0){
            saveMessage(context, chatId, msg);
            sendMessage(chat, msg);
        }
    }
    public void send(Message msg){
        if(type == ChatType.SEARCH || type == ChatType.CONTACT){
            loadChat(id, uId, msg);
        }else{
            send(context, id, msg);
        }
    }

    public ArrayList<Item> loadMessages(){
        DbChat dbChat = new DbChat(context);
        SQLiteDatabase db = dbChat.getReadableDatabase();
        long chat = chatByCode(chatCode);
        ArrayList<Item> elements = new ArrayList<>();
        long time = 0;
        if(chat >= 0){
            Cursor c = db.rawQuery("SELECT * FROM "+DbChat.T_CHATS_MSG+" WHERE chat = '"+chat+"' ORDER BY mid ASC", null);
            if(c.moveToFirst()){
                do{
                    long id = c.getLong(0);
                    String mid = c.getString(4);
                    String message = c.getString(1);
                    long timestamp = c.getLong(3);
                    int type = c.getInt(2);
                    int status = c.getInt(5);
                    String replyId = c.getString(6);

                    if(type != 0){
                        if(status < 3){
                            updateMessage(mid, 3);
                        }
                    }
                    EventMessageElement task = getTaskByMessage(db, id);

                    if(time != 0){
                        Calendar cal = Calendar.getInstance();
                        cal.setTimeInMillis(timestamp);
                        int daytm = cal.get(Calendar.DAY_OF_YEAR);
                        cal.setTimeInMillis(time);
                        int daylm = cal.get(Calendar.DAY_OF_YEAR);

                        if(daytm != daylm){
                            TextElement te = new TextElement(getDate(timestamp));
                            elements.add(0, new Item(te, 3));
                        }
                    }else if(c.isFirst()){
                        TextElement te = new TextElement(getDate(timestamp));
                        elements.add(0, new Item(te, 3));
                    }

                    time = timestamp;

                    MessageElement msg = new MessageElement(mid, message, timestamp, type, "", status);

                    msg.setTask(task);
                    if(task == null)
                        if(replyId != null && !replyId.isEmpty()){
                            MessageElement reply = getMsgById(replyId);
                            msg.setReply(reply);
                            elements.add(0, new Item(msg, 1));
                        }else{
                            elements.add(0, new Item(msg, 0));
                        }
                    else
                        elements.add(0, new Item(msg, 2));


                }while (c.moveToNext());
            }
            c.close();
        }

        return elements;
    }

    private String getDate(long time){
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(time);

        String date = c.get(Calendar.DAY_OF_MONTH) + " " + homeFragment.getMonthMinor(context, c.get(Calendar.MONTH))+" "+c.get(Calendar.YEAR);

        return date;
    }

    private EventMessageElement getTaskByMessage(SQLiteDatabase db, long id){
        Cursor c = db.rawQuery("SELECT * FROM "+DbChat.T_CHATS_EVENT+" WHERE message = '"+id+"'", null);
        if(c.moveToFirst()){
            String desc = c.getString(3);
            String user = c.getString(4);
            long deadline = c.getLong(1);
            c.close();
            return new EventMessageElement(deadline, desc, user);
        }
        c.close();
        return null;
    }

    @Nullable
    private MessageElement getMsgById(String id){
        DbChat chat = new DbChat(context);
        SQLiteDatabase db = chat.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT * FROM "+DbChat.T_CHATS_MSG+" WHERE mid = '"+id+"'", null);
        if(c.moveToFirst()){
            String mid = c.getString(4);
            String message = c.getString(1);
            long timestamp = c.getLong(3);
            int type = c.getInt(2);
            int status = c.getInt(5);

            MessageElement msg = new MessageElement(mid, message, timestamp, type, name, status);
            return msg;
        }

        return null;
    }

    private void updateMessage(String key, int status){
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference().child("chat");
                ref.orderByChild("code").equalTo(chatCode)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if(snapshot.exists()){
                            for(DataSnapshot chat : snapshot.getChildren()){
                                if(chat.child("messages").child(key).exists()){
                                    chat.child("messages").child(key).getRef().child("status").setValue(status);
                                }
                                updateMessageInDatabase(key, status);
                                break;
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }

    private void updateMessageInDatabase(String key, int status){
        DbChat dbChat = new DbChat(context);
        SQLiteDatabase db = dbChat.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("status", status);
        db.update(DbChat.T_CHATS_MSG, values, "mid = ?", new String[]{key});
    }

    public void loadChat(String id, String uId, Message msg){
        DatabaseReference database = FirebaseDatabase.getInstance().getReference();
        String tid = id;
        database.child("chat").orderByChild("code").equalTo(chatCode)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String key = "";
                        long chat = -1;
                        if(snapshot.exists()){
                            String id = tid;
                            if(id == chatCode){
                                for(DataSnapshot users : snapshot.child("users").getChildren()){
                                    if(users.getKey() != uId){
                                        id = users.getKey();
                                        MessageControler.this.id = id;
                                    }
                                }
                            }
                            for (DataSnapshot child : snapshot.getChildren()) {
                                key = child.getKey();
                                chat = chatByKey(key, context);

                                if(chat < 0){
                                    chat = createChat("", key, chatCode, id, "", context);
                                    setNameChat(context, key);
                                }
                                break;
                            }
                        }else{
                            if(type == ChatType.SEARCH){
                                HashMap<String, Object> chatHash = new HashMap<>();
                                chatHash.put("code", chatCode);
                                chatHash.put("messages", false);
                                chatHash.put("type", 0);
                                HashMap<String, Object> userHash = new HashMap<>();
                                userHash.put(id, true);
                                userHash.put(uId, true);
                                chatHash.put("users", userHash);

                                key = database.child("chat").push().getKey();
                                chat = createChat("", key, chatCode, id, "", context);
                                setNameChat(context, key);
                                database.child("chat").child(key).updateChildren(chatHash);

                                database.child("user").child(id).child("chat").child(key).setValue(true);
                                database.child("user").child(uId).child("chat").child(key).setValue(true);
                            }

                        }

                        saveMessage(context, chat, msg);
                        sendMessage(key, msg);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }



    public void updateStatus(String key, int status){
        DbChat dbChat = new DbChat(context);
        SQLiteDatabase db = dbChat.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put("status", status);

        db.update(DbChat.T_CHATS_MSG, values, "mid = ?", new String[]{key});
    }

    public void insertMessage(Message msg){
        DbChat dbChat = new DbChat(context);
        SQLiteDatabase db = dbChat.getWritableDatabase();
        ContentValues values = new ContentValues();

        long chat = chatByCode(chatCode);
        String message = TextUtils.htmlEncode(msg.getMessage());
        values.put("message", message);
        values.put("type",  1);
        values.put("timestamp", msg.getTimestamp());
        values.put("mid", msg.getId());
        values.put("status", msg.getStatus());
        values.put("reply", msg.getReplyId());
        values.put("chat", chat);

        long i = db.insert(DbChat.T_CHATS_MSG, null, values);
        if(msg.getTask() != null){
            ContentValues task = new ContentValues();
            task.put("end_date", msg.getTask().getDeadline());
            task.put("message", i);
            task.put("description", msg.getTask().getDescription());
            task.put("user", msg.getTask().getUser());
            db.insert(DbChat.T_CHATS_EVENT, null, task);
        }
    }

    public boolean checkIfExists(String key){
        DbChat dbChat = new DbChat(context);
        SQLiteDatabase db = dbChat.getReadableDatabase();

        Cursor c = db.rawQuery("SELECT * FROM "+DbChat.T_CHATS_MSG+" WHERE mid = ?", new String[]{key});
        if(c.moveToFirst()){
            c.close();
            return true;
        }
        c.close();
        return false;
    }

    public void removeMessagesViewed(){
        DatabaseReference database = FirebaseDatabase.getInstance().getReference();
        database.child("chat").orderByChild("code").equalTo(chatCode)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String key;

                        if(snapshot.exists()){
                            for (DataSnapshot child : snapshot.getChildren()) {
                                key = child.getKey();
                                if(child.child("messages").exists()){
                                    for(DataSnapshot message : child.child("messages").getChildren()){
                                        String user = "";
                                        if(message.child("userId").exists())
                                            user = message.child("userId").getValue().toString();
                                        int status = 0;
                                        if(message.child("status").exists())
                                            status = Integer.parseInt(message.child("status").getValue().toString());
                                        if(status >= 3 && user.equals(uId)){
                                            message.getRef().removeValue();

                                        }

                                    }
                                }

                                break;
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });

    }

    private long chatByCode(String code){
        DbChat dbChat = new DbChat(context);
        SQLiteDatabase db = dbChat.getReadableDatabase();

        Cursor c = db.rawQuery("SELECT id FROM "+DbChat.T_CHATS+" WHERE code = ?", new String[]{code});

        if(c.moveToFirst()){
            long id = c.getLong(0);
            c.close();
            return id;
        }
        c.close();
        return -1;
    }

    private static long chatByKey(String key, Context context){
        DbChat dbChat = new DbChat(context);
        SQLiteDatabase db = dbChat.getReadableDatabase();

        Cursor c = db.rawQuery("SELECT id FROM "+DbChat.T_CHATS+" WHERE chat = ?", new String[]{key});

        if(c.moveToFirst()){
            long id = c.getLong(0);
            c.close();
            return id;
        }
        c.close();
        return -1;
    }

    private static long createChat(String name, String id, String code, String image, String publicKey, Context context){
        java.util.Calendar calendar = java.util.Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        long currentTimeMillis = calendar.getTimeInMillis();
        DbChat dbChat = new DbChat(context);
        SQLiteDatabase db = dbChat.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put("name", name);
        values.put("type", 0);
        values.put("chat", id);
        values.put("code", code);
        values.put("image", image);
        values.put("publicKey", publicKey);
        values.put("updated", currentTimeMillis);

        return db.insert(DbChat.T_CHATS, null, values);
    }

    private static void saveMessage(Context context, long chat, Message msg){
        DbChat dbChat = new DbChat(context);
        SQLiteDatabase db = dbChat.getWritableDatabase();
        ContentValues values = new ContentValues();

        String message = TextUtils.htmlEncode(msg.getMessage());
        values.put("message", message);
        values.put("type",  0);
        values.put("timestamp", msg.getTimestamp());
        values.put("mid", msg.getId());
        values.put("status", 0);
        values.put("reply", msg.getReplyId());
        values.put("chat", chat);

        long i = db.insert(DbChat.T_CHATS_MSG, null, values);
        if(msg.getTask() != null){
            ContentValues task = new ContentValues();
            task.put("end_date", msg.getTask().getDeadline());
            task.put("message", i);
            task.put("description", msg.getTask().getDescription());
            task.put("user", msg.getTask().getUser());
            db.insert(DbChat.T_CHATS_EVENT, null, task);
        }
    }

    private static void sendMessage(String key, Message msg){
        DatabaseReference database = FirebaseDatabase.getInstance().getReference().child("chat");
        String user = FirebaseAuth.getInstance().getCurrentUser().getUid();
        HashMap<String, Object> message = new HashMap<>();
        String purger = msg.getMessage();
        message.put("message", purger);
        message.put("timestamp", msg.getTimestamp());
        message.put("status", 1);
        message.put("userId", user);
        if(msg.getReplyId() != null && !msg.getReplyId().isEmpty()){
            message.put("reply", msg.getReplyId());
        }

        message.put("task", msg.getTask());

        String msgKey = msg.getId();
        database.child(key).child("messages").child(msgKey).updateChildren(message);
    }





    public static void restateUserChat(Context context, OnRestateListener listener){

        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseDatabase.getInstance().getReference().child("user")
                .child(userId).child("chat")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if(snapshot.exists()){
                            int chats = (int) snapshot.getChildrenCount();
                            int x = 0;
                            for (DataSnapshot chat: snapshot.getChildren()) {
                                restartChat(chat.getKey(), userId, x == chats-1, listener, context);
                                x++;
                            }

                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }

    private static void setNameChat(Context context,String chatId){
        String me = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference();
        ref.child("chat")
                .child(chatId)
                .child("users")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if(snapshot.exists()){
                            for (DataSnapshot user: snapshot.getChildren()) {
                                if(!Objects.equals(user.getKey(), me)){
                                    ref.child("user").child(user.getKey()).addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                                            if(snapshot.exists()){
                                                String name = snapshot.child("name").getValue().toString();
                                                name = name.isEmpty() ? "Anonimo" : name;
                                                changeChatName(context, chatId, name);
                                            }
                                        }

                                        @Override
                                        public void onCancelled(@NonNull DatabaseError error) {

                                        }
                                    });
                                }
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }

    private static void changeChatName(Context context, String chatId, String newName){
        DbChat dbChat = new DbChat(context);
        SQLiteDatabase db = dbChat.getReadableDatabase();
        ContentValues values = new ContentValues();

        values.put("name", newName);
        db.update(DbChat.T_CHATS, values, "chat = ?", new String[]{chatId});
    }

    private static void restartChat(String chatId, String userId, boolean last, OnRestateListener listener, Context context){
        DatabaseReference chat = FirebaseDatabase.getInstance().getReference().child("chat")
                .child(chatId);
        chat.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if(snapshot.exists()){
                            int type = Integer.parseInt(snapshot.child("type").getValue().toString());
                            if(type == 0){
                                long chatI=  chatByKey(chatId, context);
                                if(chatI < 0){
                                    String name = "";
                                    if(snapshot.child("name").getValue() != null)
                                        name = snapshot.child("name").getValue().toString();
                                    String code = snapshot.child("code").getValue().toString();
                                    createChat(name, snapshot.getKey(), code, snapshot.getKey(), "", context);
                                    setNameChat(context, chatId);
                                }
                                for(DataSnapshot messages : snapshot.child("messages").getChildren()){
                                    if(!messages.child("userId").exists()){
                                        messages.getRef().removeValue();
                                    }else {
                                        String user = messages.child("userId").getValue().toString();
                                        if (!user.equals(userId)) {
                                            int status = Integer.parseInt(messages.child("status").getValue().toString());
                                            if (status < 2) {
                                                String msg = messages.child("message").getValue().toString();
                                                String key = messages.getKey();
                                                String reply = "";
                                                if (messages.child("reply").getValue() != null)
                                                    reply = messages.child("reply").getValue().toString();

                                                long timestamp = Calendar.getInstance().getTimeInMillis();
                                                Message.Builder message = new Message.Builder(msg)
                                                        .setId(key)
                                                        .setStatus(++status)
                                                        .setReplyId(reply)
                                                        .setTimestamp(timestamp)
                                                        .setUser(user);
                                                EventMessageElement event = null;
                                                if (messages.child("task").exists()) {
                                                    String desc = messages.child("task").child("description").getValue().toString();
                                                    String userT = messages.child("task").child("user").getValue().toString();
                                                    long deadline = Long.parseLong(messages.child("task").child("deadline").getValue().toString());

                                                    event = new EventMessageElement(deadline, desc, userT);
                                                    message.setTask(event);

                                                }
                                                chat.child("messages").child(key).child("status")
                                                        .setValue(status);
                                                receiveMessage(message.build(), chatId, context);
                                            }
                                        }
                                    }
                                }

                                if(last && listener != null){
                                    listener.onRestate();
                                }
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }



    public static void receiveMessage(Message msg, String chatKey, Context context){
        DbChat dbChat = new DbChat(context);
        SQLiteDatabase db = dbChat.getWritableDatabase();

        int type = msg.getUser() != FirebaseAuth.getInstance().getCurrentUser().getUid() ? 1 : 0;

        long id = chatByKey(chatKey, context);
        ContentValues values = new ContentValues();
        values.put("message", msg.getMessage());
        values.put("type", type);
        values.put("timestamp", msg.getTimestamp());
        values.put("mid", msg.getId());
        values.put("status", msg.getStatus());
        values.put("reply", msg.getReplyId());
        values.put("chat", id);
        long i = db.insert(DbChat.T_CHATS_MSG, null, values);
        if(msg.getTask() != null){
            ContentValues task = new ContentValues();
            task.put("end_date", msg.getTask().getDeadline());
            task.put("message", i);
            task.put("description", msg.getTask().getDescription());
            task.put("user", msg.getTask().getUser());
            db.insert(DbChat.T_CHATS_EVENT, null, task);
        }
    }

    public static class Message {
        private String msg;
        private long timestamp;
        private String id;
        private String user;
        private int status;

        private String replyId;

        private EventMessageElement task;

        public Message(String msg){
            this.msg = msg;
            java.util.Calendar calendar = java.util.Calendar.getInstance(TimeZone.getTimeZone("GMT"));
            timestamp = calendar.getTimeInMillis();
            user = FirebaseAuth.getInstance().getCurrentUser().getUid();
            id = FirebaseDatabase.getInstance().getReference().push().getKey();
        }
        private Message(String id, String msg){
            this.msg = msg;
            java.util.Calendar calendar = java.util.Calendar.getInstance();
            timestamp = calendar.getTimeInMillis();
            user = FirebaseAuth.getInstance().getCurrentUser().getUid();
            this.id = id;
        }
        public Message(String id, String user, String msg, long timestamp, int status, String replyId, @Nullable EventMessageElement task){
            if(id != null && !id.isEmpty()){
                this.id = id;
            }else{
                this.id = FirebaseDatabase.getInstance().getReference().push().getKey();
            }

            if(status >= 0){
                this.status = status;
            }else{
                this.status = -1;
            }

            if(user != null && !user.isEmpty()){
                this.user = user;
            }else{
                this.user = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();
            }

            this.msg = msg;

            if(timestamp > 0){
                this.timestamp = timestamp;
            }else{
                java.util.Calendar calendar = java.util.Calendar.getInstance(TimeZone.getTimeZone("GMT"));
                this.timestamp = calendar.getTimeInMillis();
            }

            if(replyId != null && !replyId.isEmpty()){
                this.replyId = replyId;
            }else{
                this.replyId = "";
            }

            this.task = task;
        }

        public void setMessage(String msg) {
            this.msg = msg;
        }

        public String getReplyId() {
            return replyId;
        }

        public void setReplyId(String replyId) {
            this.replyId = replyId;
        }

        public int getStatus() {
            return status;
        }

        public String getMessage() {
            return msg;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public String getId() {
            return id;
        }

        public String getUser() {
            return user;
        }

        public EventMessageElement getTask() {
            return task;
        }

        public static class Builder {
            private final String msg;
            private long timestamp = 0;
            private String id = "";
            private String user = "";
            private int status = -1;
            private EventMessageElement task = null;

            private String replyId;

            public Builder(String msg){
                this.msg = msg;
            }

            public Builder setTask(EventMessageElement task) {
                this.task = task;
                return this;
            }

            public Builder setTimestamp(long timestamp) {
                this.timestamp = timestamp;
                return this;
            }

            public Builder setReplyId(String id){
                this.replyId = id;
                return this;
            }

            public Builder setId(String id) {
                this.id = id;
                return this;
            }

            public Builder setUser(String user) {
                this.user = user;
                return this;
            }

            public Builder setStatus(int status){
                this.status = status;
                return this;
            }

            public Message build(){

                return new Message(id, user, msg, timestamp, status, replyId, task);
            }
        }
    }

    public enum ChatType{
        SEARCH,
        CONTACT,
        GROUP
    }
    public interface OnRestateListener{
        void onRestate();
    }
}

package com.artuok.appwork.library;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.ArrayMap;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.artuok.appwork.R;
import com.artuok.appwork.db.DbChat;
import com.artuok.appwork.fragmets.SettingsFragment;
import com.artuok.appwork.objects.EventMessageElement;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Objects;

public class MessageSender {
    private final Context context;
    private int chatLocal = -1;
    private String phone;
    private String chat;
    private OnFailureListener failureListener;
    private OnSuccessListener onSuccessListener;
    private boolean group = false;

    public void addOnSuccessListener(OnSuccessListener onSuccessListener) {
        this.onSuccessListener = onSuccessListener;
    }

    public void addOnFailureListener(OnFailureListener failureListener) {
        this.failureListener = failureListener;
    }

    public MessageSender(Context context, String phone) {
        this.context = context;
        this.phone = phone;
        this.chat = searchChatByPhone(phone);
        this.chatLocal = searchChatIdByPhone(phone);
    }

    public MessageSender(Context context, String chat, int s) {
        this.context = context;
        this.chat = chat;
        this.chatLocal = searchContactByChat(chat);
        this.phone = checkIfHaveNumber(chat);
    }

    public String checkIfHaveNumber(String chat) {
        DbChat dbChat = new DbChat(context);
        SQLiteDatabase db = dbChat.getReadableDatabase();
        Cursor query = db.rawQuery("SELECT * FROM " + DbChat.T_CHATS + " WHERE chat = '" + chat + "'", null);
        if (query.moveToFirst()) {
            String chatId = query.getString(3);
            query.close();
            return chatId;
        }
        return "";
    }

    public MessageSender(Context context) {
        this.context = context;
    }

    public void setGroup(boolean group) {
        this.group = group;
    }

    private int searchChatIdByPhone(String num) {
        DbChat dbChat = new DbChat(context);
        SQLiteDatabase db = dbChat.getReadableDatabase();
        Cursor query = db.rawQuery("SELECT * FROM " + DbChat.T_CHATS_LOGGED + " WHERE number = '" + num + "'", null);

        if (query.moveToFirst()) {
            int contact = query.getInt(0);
            query.close();
            Cursor cursor = db.rawQuery("SELECT * FROM " + DbChat.T_CHATS + " WHERE contact = '" + contact + "'", null);
            if (cursor.moveToFirst()) {
                int chat = cursor.getInt(0);
                cursor.close();
                return chat;
            }
        } else {
            query.close();
        }
        return -1;
    }

    private String searchChatByPhone(String num) {
        DbChat dbChat = new DbChat(context);
        SQLiteDatabase db = dbChat.getReadableDatabase();
        Cursor query = db.rawQuery("SELECT * FROM " + DbChat.T_CHATS_LOGGED + " WHERE number = '" + num + "'", null);
        if (query.moveToFirst()) {
            int contact = query.getInt(0);
            query.close();
            Cursor cursor = db.rawQuery("SELECT * FROM " + DbChat.T_CHATS + " WHERE contact = '" + contact + "'", null);
            if (cursor.moveToFirst()) {
                String chat = cursor.getString(4);
                cursor.close();
                return chat;
            }
        } else {
            query.close();
        }
        return "";
    }

    private int searchContactByChat(String chat) {
        DbChat dbChat = new DbChat(context);
        SQLiteDatabase db = dbChat.getReadableDatabase();
        Cursor query = db.rawQuery("SELECT * FROM " + DbChat.T_CHATS + " WHERE chat = '" + chat + "'", null);
        if (query.moveToFirst()) {
            int chatId = query.getInt(0);
            query.close();
            return chatId;
        }
        return -1;
    }

    public long uploadLocalMessage(Message msg) {
        if (chatLocal < 0) {
            if (failureListener != null)
                failureListener.onFailure(1, "ChatLocal not attached");
            return -1;
        }

        DbChat dbChat = new DbChat(context);
        SQLiteDatabase db = dbChat.getWritableDatabase();
        ContentValues content = new ContentValues();
        content.put("message", msg.getMessage());
        content.put("me", 0);
        content.put("timeSend", msg.getTimestamp());
        content.put("mid", msg.getKey());
        content.put("status", msg.getStatus());
        content.put("reply", msg.getReply());
        content.put("number", msg.getNumber());
        content.put("chat", chatLocal);

        return db.insert(DbChat.T_CHATS_MSG, null, content);
    }

    private void sendMessage(String chat, Message msg) {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference().child("chat").child(chat).child("messages").child(msg.getKey());
        ArrayMap<String, Object> message = new ArrayMap<>();

        message.put("message", msg.getMessage());
        message.put("reply", msg.getReply());
        message.put("event", msg.getEvent());
        message.put("status", 1);
        message.put("number", msg.getNumber());
        message.put("userId", msg.getUser());
        message.put("timestamp", msg.getTimestamp());
        ref.updateChildren(message);
    }

    public void sendMsg(Message msg, OnCompleteListener onSuccessListener) {
        if (group) {
            sendToChat(msg, onSuccessListener);
        } else {
            sendToUser(msg, onSuccessListener);
        }
    }

    private void sendToChat(Message msg, OnCompleteListener onSuccessListener) {
        FirebaseDatabase.getInstance().getReference().child("chat").child(chat)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (onSuccessListener != null) {
                            onSuccessListener.onComplete(snapshot, null, null, group);
                        }

                        sendMessage(chat, msg);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }

    public void receivingMessage(DataSnapshot snapshot, OnReceiveMessageListener listener) {
        FirebaseDatabase.getInstance().getReference().child("chat").child(chat)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot s) {
                        int membersCount = (int) s.child("users").getChildrenCount();
                        if (snapshot.exists()) {
                            if (snapshot.child("userId").getValue() == null)
                                return;
                            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                            String key = snapshot.getKey();
                            String currentUser = snapshot.child("userId").getValue().toString();

                            String messageText = "";
                            long timestamp = 0L;
                            String reply = "";
                            int status = 1;
                            String number = "";

                            //event
                            String eId;
                            String eTitle;
                            String eUserId;
                            long eDate;
                            long eEndDate;
                            EventMessageElement event = new EventMessageElement("", "", "", 0, 0);

                            if (snapshot.child("message").getValue() != null)
                                messageText = snapshot.child("message").getValue().toString();
                            if (snapshot.child("timestamp").getValue() != null)
                                timestamp = Long.parseLong(snapshot.child("timestamp").getValue().toString());

                            if (!group) {
                                if (snapshot.child("status").getValue() != null)
                                    status = (int) ((long) snapshot.child("status").getValue());
                            } else {
                                if (membersCount == getViewed(snapshot)) {
                                    status = 3;
                                } else if (membersCount == getNoViewed(snapshot)) {
                                    status = 2;
                                }
                            }


                            if (snapshot.child("number").getValue() != null)
                                number = snapshot.child("number").getValue().toString();

                            boolean isReply = snapshot.child("reply").getValue() != null && !snapshot.child("reply").getValue().toString().isEmpty();
                            boolean isEvent = snapshot.child("event").getValue() != null;

                            if (isReply) {
                                reply = snapshot.child("reply").getValue().toString();
                            }

                            if (isEvent) {
                                eId = snapshot.child("event").child("eventId").getValue().toString();
                                eTitle = snapshot.child("event").child("eventTitle").getValue().toString();
                                eUserId = snapshot.child("event").child("eventUserId").getValue().toString();
                                eDate = (long) snapshot.child("event").child("eventDate").getValue();
                                eEndDate = (long) snapshot.child("event").child("eventEndDate").getValue();
                                event = new EventMessageElement(eId, eTitle, eUserId, eDate, eEndDate);
                            }

                            int type = 0;


                            if (currentUser != user.getUid()) {
                                type = 1;
                            }

                            boolean isInDb = isReceived(key);

                            Message ms = new Message.Builder()
                                    .setMessage(messageText)
                                    .setKey(key)
                                    .setTimestamp(timestamp)
                                    .setReply(reply)
                                    .setStatus(3)
                                    .setNumber(number)
                                    .build();

                            if (isInDb) {
                                if (type == 0) {
                                    updateStatus(key, status);
                                    if (status == 3) {
                                        deleteMessageFromServer(chat, key);
                                    }
                                }
                            } else if (type == 1) {
                                long d = receiveMessage(chat, ms);

                                if (isEvent) {
                                    uploadEvent(d, event, chatLocal);
                                }

                                if (!group) {
                                    if (status < 3) {
                                        DatabaseReference db = FirebaseDatabase.getInstance().getReference().child("chat")
                                                .child(chat).child("messages").child(key);
                                        db.child("status").setValue(3);
                                    }
                                }
                            } else {
                                if (status == 3) {
                                    deleteMessageFromServer(chat, key);
                                }
                            }

                            listener.onReceiveMessageListener(ms, currentUser);

                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });

    }

    private void sendToUser(Message msg, OnCompleteListener onSuccessListener) {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference();
        ref.child("user").orderByChild("phone").equalTo(phone)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            for (DataSnapshot user : snapshot.getChildren()) {
                                String name = user.getKey();

                                FirebaseUser userMe = FirebaseAuth.getInstance().getCurrentUser();

                                String[] usersArr = {
                                        name, userMe.getUid()
                                };

                                Arrays.sort(usersArr);
                                String users = usersArr[0] + usersArr[1];
                                ref.child("chat").orderByChild("code").equalTo(users)
                                        .addListenerForSingleValueEvent(new ValueEventListener() {
                                            @Override
                                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                                if (onSuccessListener != null) {
                                                    onSuccessListener.onComplete(snapshot, users, name, group);
                                                }
                                                Log.d("CattoSend", "Sending: " + users);
                                                sendMessage(chat, msg);
                                            }

                                            @Override
                                            public void onCancelled(@NonNull DatabaseError error) {

                                            }
                                        });
                                return;
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }

    public void loadChat(OnLoadChatListener onLoadChatListener) {
        if (group) {
            DatabaseReference db = FirebaseDatabase.getInstance().getReference().child("chat");
            DatabaseReference query = db.child(chat);
            query.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    onLoadChatListener.onLoadChat(snapshot);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    onLoadChatListener.onFailure(error);
                }
            });
        } else {
            DatabaseReference db = FirebaseDatabase.getInstance().getReference().child("user");
            Query query = db.orderByChild("phone").equalTo(phone);
            query.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        String name = "";
                        for (DataSnapshot user : snapshot.getChildren()) {
                            name = user.getKey();

                            String[] usersArr = {
                                    name,
                                    FirebaseAuth.getInstance().getCurrentUser().getUid()
                            };

                            Arrays.sort(usersArr);

                            String users = usersArr[0] + usersArr[1];

                            Query nochat = FirebaseDatabase.getInstance().getReference().child("chat")
                                    .orderByChild("code").equalTo(users);
                            nochat.addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                    onLoadChatListener.onLoadChat(snapshot);
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {
                                    onLoadChatListener.onFailure(error);
                                }
                            });

                            return;
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    onLoadChatListener.onFailure(error);
                }
            });
        }
    }

    public void loadGlobalChats() {
        DatabaseReference db = FirebaseDatabase.getInstance().getReference();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null)
            return;

        Log.d("CattoChat", "number");
        db.child("user").child(user.getUid()).child("chat")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {

                            for (DataSnapshot child : snapshot.getChildren()) {
                                String chat = child.getKey();
                                Log.d("CattoChat", chat);
                                db.child("chat").child(chat).addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                                        if (snapshot.exists()) {
                                            int type = Integer.parseInt(Objects.requireNonNull(snapshot.child("type").getValue()).toString());

                                            if (type == 0) {
                                                String number = "";
                                                for (DataSnapshot numbers : snapshot.child("users").getChildren()) {
                                                    if (numbers.getValue() != null)
                                                        if (!Objects.equals(numbers.getKey(), user.getUid()))
                                                            number = numbers.getValue().toString();
                                                }
                                                final String phone = number;
                                                int s = loadChatLocal(snapshot.getKey(), number);

                                                db.child("user").orderByChild("phone").equalTo(number)
                                                        .addListenerForSingleValueEvent(new ValueEventListener() {
                                                            @Override
                                                            public void onDataChange(@NonNull DataSnapshot snapshotn) {
                                                                if (snapshotn.exists()) {
                                                                    for (DataSnapshot childn : snapshotn.getChildren()) {
                                                                        String namei = childn.getKey();
                                                                        String publiKey = childn.child("publicKey").getValue() != null ? childn.child("publicKey").getValue().toString() : "";
                                                                        if (s < 0) {
                                                                            createNewUnregistedContact(phone);
                                                                            createNewChatLocal(phone, 0, phone, namei, publiKey);
                                                                        }
                                                                        long updated = Long.parseLong(Objects.requireNonNull(childn.child("updated").getValue()).toString());
                                                                        if (updateUser(namei, updated)) {
                                                                            updateImage(namei, updated, 0);
                                                                        }
                                                                        break;
                                                                    }
                                                                }
                                                            }

                                                            @Override
                                                            public void onCancelled(@NonNull DatabaseError error) {

                                                            }
                                                        });

                                            } else {
                                                int s = loadChatLocal(snapshot.getKey(), "");

                                                String name = Objects.requireNonNull(snapshot.child("name").getValue()).toString();
                                                if (s < 0) {
                                                    createNewChatLocal(name, type, "", snapshot.getKey(), "");
                                                } else {
                                                    updateChatToGroup(chat);
                                                }
                                                long updated = Long.parseLong(Objects.requireNonNull(snapshot.child("updated").getValue()).toString());

                                                if (updateChat(chat, updated)) {
                                                    updateChatName(chat, name);
                                                    updateImage(chat, updated, 1);
                                                }
                                            }
                                        }


                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {

                                    }
                                });
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }

    public void loadGlobalMessages(OnLoadMessagesListener onLoadMessagesListener) {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null)
            return;
        String currentUser = user.getUid();
        ref.child("user").child(currentUser).child("chat")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            for (DataSnapshot child : snapshot.getChildren()) {
                                if (child.exists()) {
                                    String chat = child.getKey();
                                    if (chat == null)
                                        break;
                                    ref.child("chat").child(chat).addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                                            if (snapshot.exists()) {
                                                int type = Integer.parseInt(snapshot.child("type").getValue().toString());
                                                int membersCount = (int) snapshot.child("users").getChildrenCount();
                                                boolean newMessages = false;
                                                if (snapshot.child("messages").exists()) {
                                                    for (DataSnapshot messages : snapshot.child("messages").getChildren()) {
                                                        newMessages = generateMessage(messages, type, membersCount, user, chat, newMessages);
                                                    }
                                                } else {
                                                    ref.child("user").child(currentUser).child(chat).removeValue();
                                                }

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
                        onLoadMessagesListener.onFailure(error);
                    }
                });
    }

    public boolean generateMessage(DataSnapshot message, int typed, long totalUsers, FirebaseUser user, String chat, boolean newMessages) {
        if (message.child("userId").getValue() == null)
            return newMessages;

        String key = message.getKey();
        String currentUser = message.child("userId").getValue().toString();

        String messageText = "";
        long timestamp = 0L;
        String reply = "";
        int status = 1;
        String number = "";

        String eId;
        String eTitle;
        String eUserId;
        long eDate;
        long eEndDate;
        EventMessageElement event = new EventMessageElement("", "", "", 0, 0);

        if (message.child("message").getValue() != null)
            messageText = message.child("message").getValue().toString();
        if (message.child("timestamp").getValue() != null)
            timestamp = (long) message.child("timestamp").getValue();
        if (typed == 0)
            if (message.child("status").getValue() != null)
                status = Integer.parseInt(message.child("status").getValue().toString());
        if (message.child("number").getValue() != null)
            number = message.child("number").getValue().toString();

        boolean isReply = message.child("reply").getValue() != null;
        boolean isEvent = message.child("event").getValue() != null;

        if (isReply) {
            reply = message.child("reply").getValue().toString();
        }

        if (isEvent) {
            eId = message.child("event").child("eventId").getValue().toString();
            eTitle = message.child("event").child("eventTitle").getValue().toString();
            eUserId = message.child("event").child("eventUserId").getValue().toString();
            eDate = (long) message.child("event").child("eventDate").getValue();
            eEndDate = (long) message.child("event").child("eventEndDate").getValue();
            event = new EventMessageElement(eId, eTitle, eUserId, eDate, eEndDate);
        }

        int type = 0;
        if (!currentUser.equals(user.getUid())) {
            type = 1;
        }


        if (typed == 0) {
            if (type == 0) {
                if (status == 3) {
                    deleteMessageFromServer(chat, key);
                }
            }
        } else {
            if (type == 0) {
                int st = 1;
                if (totalUsers == getViewed(message)) {
                    st = 3;
                } else if (totalUsers == getNoViewed(message)) {
                    st = 2;
                }

                if (st == 3) {
                    deleteMessageFromServer(chat, key);
                }
            }
        }

        boolean received = isReceived(key);

        if (typed == 0) {
            if (received) {
                updateStatus(key, status);
            } else if (type == 1) {
                Message message1 = new Message.Builder()
                        .setMessage(messageText)
                        .setKey(key)
                        .setTimestamp(timestamp)
                        .setReply(reply)
                        .setStatus(2)
                        .setNumber(number)
                        .build();

                long d = receiveMessage(chat, message1);
                if (isEvent) {
                    long dc = getChatById(chat);
                    uploadEvent(d, event, dc);
                }

                if (status < 2) {
                    DatabaseReference db = FirebaseDatabase.getInstance().getReference().child("chat")
                            .child(chat).child("messages").child(key);
                    db.child("status").setValue(2);
                }

                return true;
            }
        } else {
            if (received) {
                if (type == 0) {
                    int st = 1;
                    if (totalUsers == getViewed(message)) {
                        st = 3;
                    } else if (totalUsers == getNoViewed(message)) {
                        st = 2;
                    }
                    updateStatus(key, st);
                } else {
                    updateStatus(key, 2);
                }
            } else if (type == 1) {
                Message message1 = new Message.Builder()
                        .setMessage(messageText)
                        .setKey(key)
                        .setTimestamp(timestamp)
                        .setReply(reply)
                        .setStatus(2)
                        .setNumber(number)
                        .build();
                long d = receiveMessage(chat, message1);
                if (isEvent) {
                    long dc = getChatById(chat);
                    uploadEvent(d, event, dc);
                }
            }
        }

        return newMessages;
    }

    private long getNoViewed(DataSnapshot message) {
        long i = 0;
        for (DataSnapshot users : message.child("status").getChildren()) {
            i++;
        }

        return i;
    }

    private long getViewed(DataSnapshot message) {
        long i = 0;
        for (DataSnapshot users : message.child("status").getChildren()) {
            boolean di = (Boolean) users.getValue();
            if (di) {
                i++;
            }
        }

        return i;
    }

    private long receiveMessage(String chat, Message message) {
        DbChat dbChat = new DbChat(context);
        SQLiteDatabase db = dbChat.getWritableDatabase();

        ContentValues content = new ContentValues();

        long idChat = getChatById(chat);

        content.put("message", message.getMessage());
        content.put("me", 1);
        content.put("timeSend", message.getTimestamp());
        content.put("mid", message.getKey());
        content.put("status", message.getStatus());
        content.put("reply", message.getReply());
        content.put("number", message.getNumber());
        content.put("chat", idChat);

        return db.insert(DbChat.T_CHATS_MSG, null, content);
    }

    private long getIdContactByNumber(String number) {
        DbChat dbChat = new DbChat(context);
        SQLiteDatabase dbr = dbChat.getReadableDatabase();
        Cursor query = dbr.rawQuery("SELECT * FROM " + DbChat.T_CHATS_LOGGED + " WHERE number = '" + number + "'", null);

        if (query.moveToFirst()) {
            int id = query.getInt(0);
            query.close();
            return id;
        }


        query.close();
        return createNewUnregistedContact(number);
    }

    private int getChatById(String cid) {
        DbChat dbChat = new DbChat(context);
        SQLiteDatabase db = dbChat.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT * FROM " + DbChat.T_CHATS + " WHERE chat = '" + cid + "'", null);
        if (c.moveToFirst()) {
            int id = c.getInt(0);
            c.close();
            return id;
        }

        c.close();
        return -1;
    }

    private void deleteMessageFromServer(String chat, String id) {
        FirebaseDatabase db = FirebaseDatabase.getInstance();
        db.getReference().child("chat").child(chat).child("messages").child(id).removeValue();
    }

    private void updateChatToGroup(String key) {
        DbChat dbChat = new DbChat(context);
        SQLiteDatabase db = dbChat.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("type", 1);
        db.update(DbChat.T_CHATS, values, "chat = '" + key + "'", null);
    }

    private void updateStatus(String key, int status) {
        DbChat dbChat = new DbChat(context);
        SQLiteDatabase db = dbChat.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("status", status);
        db.update(DbChat.T_CHATS_MSG, values, "mid = '" + key + "' AND status < '" + status + "'", null);
    }

    private void uploadEvent(long n, EventMessageElement event, long chat) {
        DbChat dbChat = new DbChat(context);
        SQLiteDatabase db = dbChat.getWritableDatabase();

        ContentValues events = new ContentValues();
        events.put("chat", chat);
        events.put("date", event.getDate());
        events.put("end_date", event.getEndDate());
        events.put("message", n);
        events.put("description", event.getTitle());
        events.put("user", event.getUserId());
        events.put("added", 1);
        db.insert(DbChat.T_CHATS_EVENT, null, events);
    }

    private boolean isReceived(String msg) {
        DbChat dbChat = new DbChat(context);
        SQLiteDatabase db = dbChat.getReadableDatabase();

        Cursor q = db.rawQuery("SELECT * FROM " + DbChat.T_CHATS_MSG + " WHERE mid = '" + msg + "'", null);

        if (q.moveToFirst()) {
            return true;
        }

        q.close();

        return false;
    }

    private boolean updateChat(String chat, long lastUpdate) {
        DbChat dbChat = new DbChat(context);
        SQLiteDatabase dbw = dbChat.getReadableDatabase();
        Cursor query = dbw.rawQuery("SELECT * FROM " + DbChat.T_CHATS + " WHERE chat = '" + chat + "'", null);
        if (query.moveToFirst()) {
            long updated = query.getLong(7);
            if (updated < lastUpdate) {
                query.close();
                return true;
            }
        }

        query.close();
        return false;
    }

    private boolean updateUser(String user, long lastUpdate) {
        DbChat dbChat = new DbChat(context);
        SQLiteDatabase dbw = dbChat.getReadableDatabase();
        Cursor query = dbw.rawQuery("SELECT * FROM " + DbChat.T_CHATS_LOGGED + " WHERE userId = '" + user + "'", null);
        if (query.moveToFirst()) {
            long updated = query.getLong(8);

            if (updated < lastUpdate) {
                return true;
            }
        }

        query.close();
        return false;
    }

    private void updateChatName(String chat, String name) {
        DbChat dbChat = new DbChat(context);
        SQLiteDatabase dbw = dbChat.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("name", name);

        dbw.update(DbChat.T_CHATS, values, "chat = '" + chat + "'", null);
    }

    private void updateImage(String name, long updated, int type) {
        StorageReference storage = FirebaseStorage.getInstance().getReference();
        StorageReference image = storage.child("chats").child(name).child("profile-photo.jpg");

        File root = context.getExternalFilesDir("Media");
        String appName = context.getString(R.string.app_name).toUpperCase();
        File myDir = new File(root, ".Profiles");
        if (!myDir.exists()) {
            boolean m = myDir.mkdirs();
            if (m) {
                File nomedia = new File(myDir, ".nomedia");
                try {
                    nomedia.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        String fname = "CHAT-" + name + "-" + appName + ".jpg";
        File file = new File(myDir, fname);
        try {
            file.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (file.exists()) {
            Log.d("CattoChat", "Exist");
            image.getFile(file).addOnSuccessListener(taskSnapshot -> {
                updateChat(name, updated, type);
                Log.d("CattoChat", "Updated");
            }).addOnFailureListener(it -> {
                it.printStackTrace();
                SettingsFragment.deletePhoto(context, name);
            });
        }
    }

    private void updateChat(String chat, long updated, int type) {
        DbChat dbChat = new DbChat(context);
        SQLiteDatabase dbw = dbChat.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("updated", updated);
        values.put("image", chat);

        if (type == 0) {
            dbw.update(DbChat.T_CHATS_LOGGED, values, "userId = '" + chat + "'", null);
        } else {
            dbw.update(DbChat.T_CHATS, values, "chat = '" + chat + "'", null);
        }
    }

    private int loadChatLocal(String key, String number) {
        DbChat dbChat = new DbChat(context);
        SQLiteDatabase dbr = dbChat.getReadableDatabase();

        Cursor cursor = dbr.rawQuery("SELECT * FROM " + DbChat.T_CHATS + " WHERE chat = '" + key + "'", null);
        if (cursor.moveToFirst()) {
            return cursor.getInt(0);
        }
        cursor.close();
        cursor = dbr.rawQuery("SELECT * FROM " + DbChat.T_CHATS_LOGGED + " WHERE number = '" + number + "'", null);
        if (cursor.moveToFirst()) {
            String name = cursor.getString(1);
            String image = cursor.getString(5);
            String publicKey = cursor.getString(7);
            cursor.close();
            return (int) createNewChatLocal(name, 0, number, image, publicKey);
        } else {
            cursor.close();
            return -1;
        }
    }

    private long createNewUnregistedContact(String number) {
        DbChat dbHelper = new DbChat(context);
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues values = new ContentValues();

        long now = Calendar.getInstance().getTimeInMillis();

        values.put("name", number);
        values.put("number", number);
        values.put("ISO", "zz");
        values.put("image", "");
        values.put("log", true);
        values.put("publicKey", "noKey");
        values.put("userId", "noUser");
        values.put("updated", now);
        values.put("added", false);
        return db.insert(DbChat.T_CHATS_LOGGED, null, values);
    }

    private long createNewChatLocal(String name, int type, String number, String image, String publicKey) {
        DbChat dbChat = new DbChat(context);
        SQLiteDatabase db = dbChat.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put("name", name);
        values.put("type", type);
        values.put("contact", number);
        values.put("chat", image);
        values.put("image", image);
        values.put("publicKey", publicKey);

        return db.insert(DbChat.T_CHATS, null, values);
    }

    public static class Message {
        private int id;
        private String key;
        private final String message;
        private final int status;
        private final String reply;
        private String number;
        private long timestamp;
        private EventMessageElement event;
        private String user;

        public Message(String message, int status, String reply) {
            this.message = message;
            this.status = status;
            this.reply = reply;
            generateMessage();
        }

        public Message(String message, int status, String reply, EventMessageElement event) {
            this.message = message;
            this.status = status;
            this.reply = reply;
            this.event = event;
            generateMessage();
        }

        private void setTimestamp(long timestamp) {
            this.timestamp = timestamp;
        }

        private void generateMessage() {
            FirebaseDatabase db = FirebaseDatabase.getInstance();
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            this.key = db.getReference().child("chat").push().getKey();
            timestamp = Calendar.getInstance().getTimeInMillis();
            number = user.getPhoneNumber();
            this.user = user.getUid();
        }

        public void setNumber(String number) {
            this.number = number;
        }

        private void setId(int id) {
            this.id = id;
        }

        public String getUser() {
            return user;
        }

        public EventMessageElement getEvent() {
            return event;
        }

        public void setEvent(EventMessageElement event) {
            this.event = event;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public String getKey() {
            return key;
        }

        public String getMessage() {
            return message;
        }

        public int getStatus() {
            return status;
        }

        public String getReply() {
            return reply;
        }

        public String getNumber() {
            return number;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public static class Builder {
            private String message = "";
            private int status = 0;
            private String reply = "";
            private EventMessageElement event;
            private String key;
            private long timestamp = -1;
            private String number;

            public Builder() {
            }

            public Builder setMessage(String message) {
                this.message = message;
                return this;
            }

            public Builder setStatus(int status) {
                this.status = status;
                return this;
            }

            public Builder setTimestamp(long timestamp) {
                this.timestamp = timestamp;
                return this;
            }

            public Builder setReply(String reply) {
                this.reply = reply;
                return this;
            }

            public Builder setEvent(EventMessageElement event) {
                this.event = event;
                return this;
            }

            public Builder setKey(String key) {
                this.key = key;
                return this;
            }

            public Builder setNumber(String number) {
                this.number = number;
                return this;
            }

            public Message build() {
                if (key == null && timestamp == -1 && number == null)
                    return new Message(message, status, reply, event);
                Message msg = new Message(message, status, reply, event);
                if (key != null)
                    msg.setKey(key);
                if (number != null)
                    msg.setNumber(number);
                if (timestamp >= 0)
                    msg.setTimestamp(timestamp);
                return msg;
            }
        }
    }

    public interface OnFailureListener {
        void onFailure(int code, String msg);
    }

    public interface OnLoadChatListener {
        void onLoadChat(DataSnapshot snapshot);

        void onFailure(DatabaseError databaseError);
    }

    public interface OnSuccessListener {
        void onSuccess();


    }

    public interface OnLoadMessagesListener {
        void onLoadMessages(boolean newMessages);

        void onFailure(DatabaseError databaseError);
    }

    public interface OnReceiveMessageListener {
        void onReceiveMessageListener(Message msg, String user);
    }

    public interface OnCompleteListener {
        void onComplete(@NonNull DataSnapshot reference, @Nullable String code, @Nullable String user, boolean isGroup);
    }
}

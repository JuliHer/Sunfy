package com.artuok.appwork.library;

import androidx.annotation.Nullable;

import com.artuok.appwork.objects.EventMessageElement;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Objects;
import java.util.TimeZone;

public class Message {
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

    public enum ChatType{
        SEARCH,
        CONTACT,
        GROUP
    }
    public interface OnRestateListener{
        void onRestate();
    }
}




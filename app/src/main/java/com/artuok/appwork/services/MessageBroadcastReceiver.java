package com.artuok.appwork.services;

import androidx.annotation.NonNull;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class MessageBroadcastReceiver extends FirebaseMessagingService {
    public static final String ACTION_MESSAGE = "MESSAGE";
    @Override
    public void onMessageReceived(@NonNull RemoteMessage message) {
        super.onMessageReceived(message);
    }
}

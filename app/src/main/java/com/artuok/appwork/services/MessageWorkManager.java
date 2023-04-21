package com.artuok.appwork.services;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class MessageWorkManager extends Worker {
    public MessageWorkManager(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Intent i = new Intent(getApplicationContext(), AlarmWorkManager.class);
        i.setAction(AlarmWorkManager.ACTION_MESSAGES);
        getApplicationContext().sendBroadcast(i);
        return Result.success();
    }
}

package com.venkee.keelah;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import androidx.annotation.Nullable;

public class BackgroundService extends Service {


    @Nullable
    @Override
    // Not needed so it return null.
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

    }

    @Override
    public void onDestroy() {

    }
}

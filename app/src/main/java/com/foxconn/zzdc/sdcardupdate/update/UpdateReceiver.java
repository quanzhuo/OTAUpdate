package com.foxconn.zzdc.sdcardupdate.update;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class UpdateReceiver extends BroadcastReceiver {
    public static final String TAG = "OTAUpdate";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "received intent: " + intent);
        String action = intent.getAction();
        Intent service;
        if (action.equals(Intent.ACTION_MEDIA_MOUNTED)) {
            service = new Intent(context, SDCardUpdateService.class);
        } else {
            Log.d(TAG, "unknown action");
            return;
        }

        // context.startForegroundService(service);
    }
}

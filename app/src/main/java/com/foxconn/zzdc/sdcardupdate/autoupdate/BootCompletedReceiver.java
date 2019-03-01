package com.foxconn.zzdc.sdcardupdate.autoupdate;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import com.foxconn.zzdc.sdcardupdate.R;

import static com.foxconn.zzdc.sdcardupdate.autoupdate.SettingActivity.AUTO_DOWNLOAD;

public class BootCompletedReceiver extends BroadcastReceiver {
    private static final String TAG = "BootCompletedReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "onReceive: " + intent);

        SharedPreferences sharedPref = context.getSharedPreferences(
                context.getString(R.string.preference_file_name), Context.MODE_PRIVATE);
        Boolean autoDownload = sharedPref.getBoolean(AUTO_DOWNLOAD, true);

        if (autoDownload) {
            Intent service = new Intent(context, AutoUpdateService.class);
            context.startForegroundService(service);
        }

    }
}

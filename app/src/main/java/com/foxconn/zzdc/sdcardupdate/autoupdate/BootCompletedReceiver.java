package com.foxconn.zzdc.sdcardupdate.autoupdate;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.SystemClock;

import com.foxconn.zzdc.sdcardupdate.R;

import static android.content.Context.ALARM_SERVICE;
import static com.foxconn.zzdc.sdcardupdate.autoupdate.SettingActivity.AUTO_DOWNLOAD;
import static com.foxconn.zzdc.sdcardupdate.autoupdate.SettingActivity.AUTO_INSTALL;

public class BootCompletedReceiver extends BroadcastReceiver {

    public static final int INTERVAL = 30 * 60 * 1000;
    public static final int REQUEST_CODE_CHECK_UPDATE = 0;



    @Override
    public void onReceive(Context context, Intent intent) {
//        Log.d(TAG, "received " + intent);
//        SharedPreferences sharedPref = context.getSharedPreferences(
//                context.getString(R.string.preference_file_name), Context.MODE_PRIVATE);
//        String curVersion = Build.VERSION.INCREMENTAL;
//        String oldVersion = sharedPref.getString(context.getString(R.string.old_version), curVersion);
//        if (! oldVersion.equals(curVersion)) {
//            Log.d(TAG, "OTA Successfully, delete ota package !");
//            String otaFile = "update-" + oldVersion.substring(5, 6) + oldVersion.substring(7)
//                    + "-" +curVersion.substring(5, 6) + curVersion.substring(7) + ".zip";
//            File otaPath = new File(OTA_DIR, otaFile);
//            if (otaPath.exists()) {
//                otaPath.delete();
//            }
//
//            SharedPreferences.Editor editor = sharedPref.edit();
//            editor.putString(context.getString(R.string.old_version), curVersion);
//            editor.commit();
//        }

        SharedPreferences sharedPref = context.getSharedPreferences(
                context.getString(R.string.preference_file_name), Context.MODE_PRIVATE);
        Boolean autoDownload = sharedPref.getBoolean(AUTO_DOWNLOAD, true);
        Boolean autoInstall = sharedPref.getBoolean(AUTO_INSTALL, true);

        if (autoDownload) {
//            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
//            long triggerAtTime = SystemClock.elapsedRealtime() + INTERVAL;
//            Intent service = new Intent(context, AutoUpdateService.class);
//            PendingIntent pendingIntent = PendingIntent.getService(context,
//                    REQUEST_CODE_CHECK_UPDATE, service, 0);
//            alarmManager.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAtTime, pendingIntent);
            setHourlyTask(context);
        }

    }

    public static void setHourlyTask(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(ALARM_SERVICE);
        long triggerAtTime = SystemClock.elapsedRealtime() + INTERVAL;
        Intent service = new Intent(context, AutoUpdateService.class);
        PendingIntent pendingIntent = PendingIntent.getService(context,
                REQUEST_CODE_CHECK_UPDATE, service, 0);
        alarmManager.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAtTime, pendingIntent);
    }
}

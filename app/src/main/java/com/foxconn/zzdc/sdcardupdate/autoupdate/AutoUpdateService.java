package com.foxconn.zzdc.sdcardupdate.autoupdate;

import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.BitmapFactory;
import android.os.IBinder;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.foxconn.zzdc.sdcardupdate.R;
import com.foxconn.zzdc.sdcardupdate.download.DownloadService;
import com.foxconn.zzdc.sdcardupdate.tool.OTAApplication;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import static com.foxconn.zzdc.sdcardupdate.tool.Util.checkUpdate;
import static com.foxconn.zzdc.sdcardupdate.tool.Util.isNetworkConnected;

public class AutoUpdateService extends IntentService {
    public static final int INTERVAL = 12 * 60 * 60 * 1000;
    private static final String TAG = "AutoUpdateService";
    private static final String CHANNEL_ID = "com.foxconn.zzdc.sdcardupdate.autoupdateservice";

    private OTAApplication mApp;
    public static final int REQUEST_CODE_CHECK_UPDATE = 0;

    private DownloadService.DownloadBinder mDownloadBinder;

    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mDownloadBinder = (DownloadService.DownloadBinder) service;
            mDownloadBinder.startDownload(mApp.getDownloadUrl());
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };

    public AutoUpdateService() {
        super("AutoUpdateService");
    }

    public AutoUpdateService(String name) {
        super(name);
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        Log.d(TAG, "onHandleIntent: " + intent);
        mApp = (OTAApplication) getApplication();
        startForeground(1, createNotification());
        if (isNetworkConnected(this)) {
            checkUpdate(this, updatable -> {
                if (updatable) {
                    Intent service = new Intent(AutoUpdateService.this, DownloadService.class);
                    startService(service);
                    bindService(service, mServiceConnection, BIND_AUTO_CREATE);
                }
                runRepeatly(this);
            });
        } else {
            runRepeatly(this);
        }
    }

    public static void runRepeatly(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(ALARM_SERVICE);
        long triggerAtTime = SystemClock.elapsedRealtime() + INTERVAL;
        long rtcTime = System.currentTimeMillis() + INTERVAL;
        String time = new SimpleDateFormat("yyyy.MM.dd G 'at' HH:mm:ss z").format(new Date(rtcTime));
        Log.d(TAG, "runRepeatly: next run at " + time);

        Intent service = new Intent(context, AutoUpdateService.class);
        PendingIntent pendingIntent = PendingIntent.getForegroundService(context,
                REQUEST_CODE_CHECK_UPDATE, service, 0);
        alarmManager.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAtTime, pendingIntent);
    }

    private Notification createNotification() {
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "autoupdate",
                NotificationManager.IMPORTANCE_HIGH);
        channel.setDescription("check ota udpate automatic");
        channel.enableVibration(false);
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        nm.createNotificationChannel(channel);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        builder.setChannelId(CHANNEL_ID)
                .setContentText("check update")
                .setContentTitle("check")
                .setSmallIcon(R.drawable.btn_bg)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(),
                        android.R.drawable.alert_dark_frame));
        return builder.build();
    }

    /**
     * @param context
     * @param serviceName: packagename + service class name
     * @return true: is running; false: not running
     */
    public boolean isServiceRunning(Context context, String serviceName) {
        boolean isRunning = false;
        ActivityManager am = (ActivityManager) context.getSystemService(ACTIVITY_SERVICE);
        List<ActivityManager.RunningServiceInfo> runningServiceInfos = am.getRunningServices(40);
        if (runningServiceInfos.size() <= 0) {
            return false;
        }

        for (int i = 0; i < runningServiceInfos.size(); i++) {
            String name = runningServiceInfos.get(i).service.getClassName().toString();
            if (name.equals(serviceName)) {
                isRunning = true;
                break;
            }
        }

        return isRunning;
    }
}

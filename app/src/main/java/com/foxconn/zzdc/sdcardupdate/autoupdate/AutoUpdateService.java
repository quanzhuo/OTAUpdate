package com.foxconn.zzdc.sdcardupdate.autoupdate;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.SystemClock;
import android.support.annotation.Nullable;

import com.foxconn.zzdc.sdcardupdate.CheckUpdateActivity;
import com.foxconn.zzdc.sdcardupdate.download.DownloadService;

import static com.foxconn.zzdc.sdcardupdate.autoupdate.BootCompletedReceiver.INTERVAL;
import static com.foxconn.zzdc.sdcardupdate.autoupdate.BootCompletedReceiver.REQUEST_CODE_CHECK_UPDATE;
import static com.foxconn.zzdc.sdcardupdate.CheckUpdateActivity.mDownloadURL;
import static com.foxconn.zzdc.sdcardupdate.CheckUpdateActivity.mOTA;
import static com.foxconn.zzdc.sdcardupdate.CheckUpdateActivity.mReleaseNote;
import static com.foxconn.zzdc.sdcardupdate.autoupdate.BootCompletedReceiver.setHourlyTask;

public class AutoUpdateService extends IntentService {

    private DownloadService.DownloadBinder mDownloadBinder;

    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mDownloadBinder = (DownloadService.DownloadBinder) service;
            mDownloadBinder.startDownload(mDownloadURL);
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
        CheckUpdateActivity.checkUpdate(updatable -> {
            if (updatable) {
                Intent service = DownloadService.newIntent(this, mReleaseNote, mOTA,
                        mDownloadURL, 0);
                startService(service);
                bindService(service, mServiceConnection, BIND_AUTO_CREATE);
                if (mDownloadBinder == null) {
                    return;
                } else {
                    mDownloadBinder.startDownload(mDownloadURL);
                }
            }
            setHourlyTask(this);
        });
    }
}

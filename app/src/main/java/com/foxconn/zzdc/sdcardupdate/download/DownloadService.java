package com.foxconn.zzdc.sdcardupdate.download;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Binder;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;

import com.foxconn.zzdc.sdcardupdate.R;
import com.foxconn.zzdc.sdcardupdate.UpdateUI;

import java.io.File;

import static android.app.NotificationManager.IMPORTANCE_HIGH;
import static android.app.NotificationManager.IMPORTANCE_LOW;
import static com.foxconn.zzdc.sdcardupdate.download.DownloadStatus.CANCELED;
import static com.foxconn.zzdc.sdcardupdate.download.DownloadStatus.DOWNLOADING;
import static com.foxconn.zzdc.sdcardupdate.download.DownloadStatus.FAILED;
import static com.foxconn.zzdc.sdcardupdate.download.DownloadStatus.FINISHED;
import static com.foxconn.zzdc.sdcardupdate.download.DownloadStatus.NOT_START;
import static com.foxconn.zzdc.sdcardupdate.download.DownloadStatus.PAUSED;

public class DownloadService extends Service {

    private static final String channelIDSound = "com.foxconn.zzdc.sdcardupdate.sound";
    private static final String channelIDNoSound = "com.foxconn.zzdc.sdcardupdate.nosound";
    private static final int NOTIFY_ID_DOWNLOAD_PROGRESS = 1;

    private String mDownloadUrl;
    private String mReleaseNote;
    private String mOta;
    private int mCurrentPercent;

    private DownloadStatus mDownloadStatus = NOT_START;

    private UpdateUI mUpdateUI;
    private NotificationManager mNM;
    private DownloadTask mDownloadTask;
    private DownloadListener mListener = new DownloadListener() {
        @Override
        public void onProgress(int progress) {
            mCurrentPercent = progress;
            mDownloadStatus = DOWNLOADING;
            mNM.notify(NOTIFY_ID_DOWNLOAD_PROGRESS,
                    getNotification(getString(R.string.notify_title_downloading),
                            progress, channelIDNoSound));
            mUpdateUI.updateProgress(progress);
        }

        @Override
        public void onSuccess() {
            mDownloadTask = null;
            mDownloadStatus = FINISHED;
            stopForeground(true);
            mNM.notify(NOTIFY_ID_DOWNLOAD_PROGRESS,
                    getNotification(getString(R.string.notify_title_success), -1,
                            channelIDSound));
            mUpdateUI.updateButton();
            mUpdateUI.updateProgress(100);
        }

        @Override
        public void onFailed() {
            mDownloadTask = null;
            mDownloadStatus = FAILED;
            stopForeground(true);
            mNM.notify(NOTIFY_ID_DOWNLOAD_PROGRESS,
                    getNotification(getString(R.string.notify_title_failed), -1, channelIDSound));
        }

        @Override
        public void onPaused() {
            mDownloadTask = null;
            mDownloadStatus = PAUSED;
            mNM.notify(NOTIFY_ID_DOWNLOAD_PROGRESS,
                    getNotification(getString(R.string.notify_title_pause), mCurrentPercent, channelIDSound));
        }

        @Override
        public void onCanceled() {
            mDownloadTask = null;
            mDownloadStatus = CANCELED;
            stopForeground(true);
        }
    };
    private DownloadBinder mBinder = new DownloadBinder();

    public DownloadService() {

    }

    public void setUpdateUI(UpdateUI updateUI) {
        mUpdateUI = updateUI;
    }

    @Override
    public IBinder onBind(Intent intent) {
        mReleaseNote = intent.getStringExtra("releaseNote");
        mDownloadUrl = intent.getStringExtra("url");
        mOta = intent.getStringExtra("ota");

        mNM = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        createNotificationChannel(channelIDNoSound);
        createNotificationChannel(channelIDSound);
        return mBinder;
    }

    public class DownloadBinder extends Binder {

        public void startDownload(String url) {
            if (mDownloadTask == null) {
                mDownloadUrl = url;
                mDownloadTask = new DownloadTask(mListener);
                mDownloadTask.execute(mDownloadUrl);
                startForeground(1, getNotification(
                        getString(R.string.notify_title_start_download), 0, channelIDNoSound));
            }
        }

        public void pauseDownload() {
            if (mDownloadTask != null) {
                mDownloadTask.pauseDownload();
            }
        }

        public void cancelDownload() {
            if (mDownloadTask != null) {
                mDownloadTask.cancelDownload();
            } else {
                if (mDownloadUrl != null) {
                    String fileName = mDownloadUrl.substring(mDownloadUrl.lastIndexOf("/"));
                    String directory = Environment.getExternalStoragePublicDirectory(
                            Environment.DIRECTORY_DOWNLOADS).getPath();
                    File file = new File(directory + fileName);
                    if (file.exists()) {
                        file.delete();
                    }
                    mNM.cancel(1);
                    stopForeground(true);
                }
            }

        }

        public DownloadService getService() {
            return DownloadService.this;
        }

        public DownloadStatus getStatus() {
            return mDownloadStatus;
        }

        public void setStatus(DownloadStatus status) {
            mDownloadStatus = status;
        }

        public int getProgress() {
            return mCurrentPercent;
        }
    }

    private void createNotificationChannel(String channelID) {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O) {
            int importance = channelID.equals(channelIDSound) ? IMPORTANCE_HIGH : IMPORTANCE_LOW;
            NotificationChannel channel = new NotificationChannel(channelID,
                    getString(R.string.notify_channel_name), importance);
            channel.setDescription(getString(R.string.notify_channel_des));
            channel.enableVibration(false);

            if (channelID.equals(channelIDSound)) {
                channel.enableVibration(true);
            } else {
                channel.enableVibration(false);
            }
            mNM.createNotificationChannel(channel);
        }
    }

    private Notification getNotification(String title, int percent, String channelID) {
        Intent intent = DownloadDetailsActivity.newIntent(
                this, mReleaseNote, mDownloadUrl, mOta, mCurrentPercent);
        PendingIntent pi = PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        builder.setSmallIcon(android.R.drawable.sym_def_app_icon)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(),
                        android.R.drawable.alert_dark_frame))
                .setContentIntent(pi)
                .setContentTitle(title)
                .setChannelId(channelID);
        if (percent >= 0) {
            builder.setContentText(percent + "%")
                    .setProgress(100, percent, false);
        }

        return builder.build();
    }

    public static Intent newIntent(Context context, String note, String ota, String url, int percent) {
        Intent service = new Intent(context, DownloadService.class);
        service.putExtra("releaseNote", note);
        service.putExtra("ota", ota);
        service.putExtra("url", url);
        service.putExtra("percent", percent);

        return service;
    }
}

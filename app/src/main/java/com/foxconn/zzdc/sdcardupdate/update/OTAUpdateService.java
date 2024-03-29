package com.foxconn.zzdc.sdcardupdate.update;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.PowerManager;
import android.os.RecoverySystem;
import android.support.annotation.Nullable;
import android.util.Log;

import com.foxconn.zzdc.sdcardupdate.R;
import com.liulishuo.filedownloader.BaseDownloadTask;
import com.liulishuo.filedownloader.FileDownloadLargeFileListener;
import com.liulishuo.filedownloader.FileDownloader;

import java.io.File;
import java.io.IOException;

import static com.foxconn.zzdc.sdcardupdate.tool.Util.fileToMD5;
import static com.foxconn.zzdc.sdcardupdate.update.UpdateReceiver.TAG;

public class OTAUpdateService extends IntentService {
    public static final String OTA_DIR = "/data/ota_package";

    public OTAUpdateService() {
        super("OTAUpdateService");
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        // Version format: 0000_0_450
        // Version code format: 0450
        final String newVersion = intent.getStringExtra("version");
        final String newVerCode = newVersion.substring(5, 6) + newVersion.substring(7);
        final String curVersion = Build.VERSION.INCREMENTAL; //00WW_5_580
        final String curVerCode = curVersion.substring(5, 6) + curVersion.substring(7);
        final String md5 = intent.getStringExtra("md5");

        if (curVerCode.compareTo(newVerCode) >= 0) {
            Log.d(TAG, "Version: " + newVerCode + " is not newer than " + curVerCode);
            return;
        }

        String url = intent.getStringExtra("otaurl");
        Log.d(TAG, "otaurl: " + url);

        FileDownloader.setup(this);
        BaseDownloadTask task = FileDownloader.getImpl().create(url);
        task.setPath(OTA_DIR, true)
                .setCallbackProgressMinInterval(5 * 1000)
                .setAutoRetryTimes(3)
                .setSyncCallback(true)
                .setListener(new FileDownloadLargeFileListener() {
                    @Override
                    protected void pending(BaseDownloadTask task, long soFarBytes, long totalBytes) {
                        Log.d(TAG, "pending: " + soFarBytes + "/" + totalBytes);
                    }

                    @Override
                    protected void progress(BaseDownloadTask task, long soFarBytes, long totalBytes) {
                        if (totalBytes == 0)
                            totalBytes = 1;
                        Log.d(TAG, "downloading: " + soFarBytes + "/" + totalBytes + " = "
                                + (float) soFarBytes / totalBytes);
                    }

                    @Override
                    protected void completed(BaseDownloadTask task) {
                        Log.d(TAG, "Download completed !");
                        final String otaPkgPath = task.getTargetFilePath();
                        final String calculatedMD5 = fileToMD5(otaPkgPath);
                        if ((md5 != null) && (!md5.equals(calculatedMD5))) {
                            Log.e(TAG, "miss matched md5, calculated: " + calculatedMD5 + ", passed:" + md5);
                            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
                            pm.reboot("OTA FAILED !");
                            return;
                        }

                        install(OTAUpdateService.this, new File(otaPkgPath));
                    }

                    @Override
                    protected void paused(BaseDownloadTask task, long soFarBytes, long totalBytes) {
                        Log.d(TAG, "paused");
                    }

                    @Override
                    protected void error(BaseDownloadTask task, Throwable e) {
                        Log.d(TAG, "error: " + e);
                    }

                    @Override
                    protected void warn(BaseDownloadTask task) {
                        Log.d(TAG, "warn");
                    }
                }).start();
    }

    public static void install(Context context, File path) {
        try {
            Log.d(TAG, "verifying ota packing...");
            RecoverySystem.verifyPackage(path, null, null);
        } catch (Exception e) {
            Log.e(TAG, "package " + path + " verify failed", e);
            return;
        }

        try {
            Log.d(TAG, "installing...");
            SharedPreferences sharedPref = context.getSharedPreferences(
                    context.getString(R.string.preference_file_name), MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putString(context.getString(R.string.old_version), Build.VERSION.INCREMENTAL);
            editor.commit();
            RecoverySystem.installPackage(context, path);
        } catch (IOException e) {
            Log.e(TAG, e + "");
        }
    }
}

package com.foxconn.zzdc.sdcardupdate;

import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.os.storage.StorageManager;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.regex.Pattern;

import static com.foxconn.zzdc.sdcardupdate.OTAUpdateService.OTA_DIR;
import static com.foxconn.zzdc.sdcardupdate.OTAUpdateService.fileToMD5;
import static com.foxconn.zzdc.sdcardupdate.OTAUpdateService.install;
import static com.foxconn.zzdc.sdcardupdate.UpdateReceiver.TAG;

public class SDCardUpdateService extends Service {

    public SDCardUpdateService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        doUpdate();

        return Service.START_STICKY;
    }

    private String getSecondaryStoragePath() {
        try {
            StorageManager sm = (StorageManager) getSystemService(STORAGE_SERVICE);
            Method getVolumePathsMethod = StorageManager.class.getMethod("getVolumePaths", null);
            String[] paths = (String[]) getVolumePathsMethod.invoke(sm, null);
            return paths.length <= 1 ? null : paths[1];
        } catch (Exception e) {
            Log.e(TAG, "getSecondaryStoragePath() failed", e);
        }

        return null;
    }

    private void doUpdate() {
        final String sdcardPath = getSecondaryStoragePath();
        if (sdcardPath == null) {
            Log.d(TAG, "No External storage found, return");
            return;
        }

        final String curVersion = Build.VERSION.INCREMENTAL; //00WW_5_580
        final String curVerCode = curVersion.substring(5, 6) + curVersion.substring(7);

        File dir = new File(sdcardPath);
        String[] pkgLists = dir.list(new FilenameFilter() {

            private Pattern pattern = Pattern.compile("update-"
                    + curVerCode + "-\\d{4}\\.zip");

            @Override
            public boolean accept(File dir, String name) {
                return pattern.matcher(name).matches()
                        && new File(dir, name).isFile();
            }
        });

        if (pkgLists == null || pkgLists.length == 0) {
            return;
        }

        Arrays.sort(pkgLists);
        String newVerCode = pkgLists[pkgLists.length - 1].substring(12, 16);
        Log.d(TAG, "Current version: " + curVerCode + ", new Version: " + newVerCode);

        if (curVerCode.compareTo(newVerCode) >= 0) {
            return;
        }


        String srcPath = sdcardPath + "/" + pkgLists[pkgLists.length - 1];
        String dstPath = OTA_DIR + "/" + pkgLists[pkgLists.length - 1];
        // copy update package to /data/ota_package
        // TODO: using system api to copy file
        copyFile(srcPath, dstPath);

        install(this, new File(dstPath));
    }

    private void copyFile(String srcPath, String dstPath) {
        File src = new File(srcPath);
        File dst = new File(dstPath);

        if (dst.exists() && dst.isFile()) {
            final String dstMD5 = fileToMD5(dstPath);
            final String srcMD5 = fileToMD5(srcPath);
            if (dstMD5.equals(srcMD5)) {
                Log.d(TAG, dstPath + " is the same with " + srcPath);
                return;
            } else {
                dst.delete();
            }
        }

        Log.d(TAG, "Copy " + src + " to " + dst);
        BufferedInputStream in = null;
        BufferedOutputStream out = null;
        try {
            in = new BufferedInputStream(new FileInputStream(src));
            out = new BufferedOutputStream(new FileOutputStream(dst));
            int count;
            byte[] buffer = new byte[512];

            while ((count = in.read(buffer)) != -1) {
                out.write(buffer, 0, count);
            }
        } catch (IOException e) {
            Log.e(TAG, e + "");
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
                if (out != null) {
                    out.close();
                }
            } catch (IOException e) {
                Log.e(TAG, e + "");
            }
        }
    }
}

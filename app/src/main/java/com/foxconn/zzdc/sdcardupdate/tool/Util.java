package com.foxconn.zzdc.sdcardupdate.tool;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.util.Log;

import com.foxconn.zzdc.sdcardupdate.BuildConfig;
import com.foxconn.zzdc.sdcardupdate.update.UpdateHandler;

import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;

import static android.content.Context.CONNECTIVITY_SERVICE;
import static java.lang.Thread.sleep;

public class Util {
    private static final String TAG = "Util";

    public static String getHttpResponse(String downloadURL) {
        Log.d(TAG, "getHttpResponse: downloadURL=" + downloadURL);
        HttpURLConnection connection = null;
        try {
            URL url = new URL(downloadURL);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);
            connection.setDoInput(true);
            connection.setDoOutput(true);

            InputStream inputStream = connection.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }

            return response.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    public static void checkUpdate(Context context, UpdateHandler handler) {
        OTAApplication app = (OTAApplication) context.getApplicationContext();
        new Thread(() -> {
            String checkOTAUrl = BuildConfig.OTA_SERVER_URL + "?current=" +
                    Build.VERSION.INCREMENTAL + "&sn=" + Build.getSerial();
            String response = getHttpResponse(checkOTAUrl);
            Log.d(TAG, "checkUpdate: response=" + response);
            boolean updatable = false;
            if (response != null) {
                JSONObject jsonResponse;
                try {
                    sleep(500);
                    jsonResponse = new JSONObject(response);
                    updatable = jsonResponse.getString("updatable").equals("true");
                    app.setUpdatable(true);
                    if (updatable) {
                        app.setNewVersion(jsonResponse.getString("newVersion"));
                        app.setDownloadUrl(jsonResponse.getString("url"));
                        app.setReleaseNote(jsonResponse.getString("releaseNote").
                                replace("\\n", "\n"));
                        app.setOta(jsonResponse.getString("ota"));
                    }
                    handler.run(updatable);
                } catch (Exception e) {
                    Log.d(TAG, "checkUpdate: " + e.getStackTrace());
                    e.printStackTrace();
                }
            } else {
                handler.run(updatable);
            }
        }).start();
    }

    public static boolean isNetworkConnected(Context context) {
        ConnectivityManager manager = (ConnectivityManager) context.getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo info = manager.getActiveNetworkInfo();
        if (info != null) {
            return info.isAvailable();
        }
        return false;
    }

    public static boolean copyFile(String srcPath, String dstPath) {
        File src = new File(srcPath);
        File dst = new File(dstPath);

        if (dst.exists() && dst.isFile()) {
            final String dstMD5 = fileToMD5(dstPath);
            final String srcMD5 = fileToMD5(srcPath);
            if (dstMD5.equals(srcMD5)) {
                Log.d(TAG, dstPath + " is the same with " + srcPath);
                return true;
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
            return false;
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
                return false;
            }
            return true;
        }
    }

    public static String fileToMD5(String filepath) {
        InputStream inputStream = null;
        try {
            inputStream = new FileInputStream(filepath);
            byte[] buffer = new byte[1024];
            MessageDigest digest = MessageDigest.getInstance("MD5");
            int numRead = 0;
            while (numRead != -1) {
                numRead = inputStream.read(buffer);
                if (numRead > 0) {
                    digest.update(buffer, 0, numRead);
                }
            }
            byte[] md5Bytes = digest.digest();
            return covertHashToString(md5Bytes);
        } catch (Exception e) {
            Log.e(TAG, e.toString());
            return null;
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    Log.d(TAG, e.toString());
                }
            }
        }
    }

    private static String covertHashToString(byte[] md5Bytes) {
        String returnVal = "";
        for (int i = 0; i < md5Bytes.length; i++) {
            returnVal += Integer.toString((md5Bytes[i] & 0xff) + 0x100, 16).substring(1);
        }

        return returnVal.toUpperCase();
    }
}

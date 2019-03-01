package com.foxconn.zzdc.sdcardupdate.download;

import android.os.AsyncTask;
import android.os.Environment;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class DownloadTask extends AsyncTask<String, Integer, Integer> {
    public static final int SUCCEED = 0;
    public static final int FAILED = 1;
    public static final int PAUSED = 2;
    public static final int CANCELED = 3;

    private DownloadListener mListener;
    private boolean mIsCanceled = false;
    private boolean mIsPaused = false;
    private int mLastProgress;

    public static String filePath;

    public DownloadTask(DownloadListener listener) {
        mListener = listener;
    }

    @Override
    protected Integer doInBackground(String... params) {
        InputStream is = null;
        RandomAccessFile savedFile = null;
        File file = null;
        try {
            long downloadedLength = 0;
            String downloadUrl = params[0];
            String fileName = downloadUrl.substring(downloadUrl.lastIndexOf("=") + 1);
            String directory = Environment.getExternalStorageDirectory().getPath();

            file = new File(directory + "/" + fileName);
            filePath = file.getAbsolutePath();

            if (file.exists()) {
                downloadedLength = file.length();
            }

            long contentLength = getContentLength(downloadUrl);
            if (contentLength == 0) {
                return FAILED;
            } else if (contentLength == downloadedLength) {
                return SUCCEED;
            }

            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    .addHeader("RANGE", "bytes=" + downloadedLength + "-")
                    .url(downloadUrl)
                    .build();

            Response response = client.newCall(request).execute();

            if (response != null) {
                is = response.body().byteStream();
                savedFile = new RandomAccessFile(file, "rw");
                savedFile.seek(downloadedLength);

                byte[] b = new byte[4096];
                int total = 0;
                int len;
                while ((len = is.read(b)) != -1) {
                    if (mIsCanceled) {
                        return CANCELED;
                    } else if (mIsPaused) {
                        return PAUSED;
                    } else {
                        total += len;
                        savedFile.write(b, 0, len);

                        int progress = (int) ((total + downloadedLength) * 100 / contentLength);
                        publishProgress(progress);
                    }
                }

                response.body().close();
                return SUCCEED;
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (is != null) {
                    is.close();
                }

                if (savedFile != null) {
                    savedFile.close();
                }

                if (mIsCanceled && file != null) {
                    file.delete();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return FAILED;
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        if (values[0] > mLastProgress) {
            mListener.onProgress(values[0]);
            mLastProgress = values[0];
        }
    }

    public void pauseDownload() {
        mIsPaused = true;
    }

    public void cancelDownload() {
        mIsCanceled = true;
    }

    @Override
    protected void onPostExecute(Integer status) {
        switch (status) {
            case SUCCEED:
                mListener.onSuccess();
                break;
            case FAILED:
                mListener.onFailed();
                break;
            case PAUSED:
                mListener.onPaused();
                break;
            case CANCELED:
                mListener.onCanceled();
            default:
                break;
        }
    }

    private long getContentLength(String downloadUrl) throws IOException {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(downloadUrl).build();
        Response response = client.newCall(request).execute();
        if (response != null && response.isSuccessful()) {
            long contentLength = response.body().contentLength();
            response.close();
            return contentLength;
        }
        return 0;
    }
}

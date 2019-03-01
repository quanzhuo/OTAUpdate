package com.foxconn.zzdc.sdcardupdate.download;

import android.Manifest;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.foxconn.zzdc.sdcardupdate.R;
import com.foxconn.zzdc.sdcardupdate.tool.OTAApplication;
import com.foxconn.zzdc.sdcardupdate.tool.ProgressButton;

import java.io.File;

import static com.foxconn.zzdc.sdcardupdate.download.DownloadStatus.FINISHED;
import static com.foxconn.zzdc.sdcardupdate.download.DownloadStatus.NOT_START;

public class DownloadDetailsActivity extends AppCompatActivity {
    private static final String TAG = "DownloadDetailsActivity";

    private OTAApplication mApp;

    private ProgressButton progressButton;
    private File mOTAPackage;

    private DownloadService.DownloadBinder mDownloadBinder;
    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mDownloadBinder = (DownloadService.DownloadBinder) service;
            Log.d(TAG, "onServiceConnected: status=" + mDownloadBinder.getStatus());
            if (mApp.getPercent() == 100) {
                mDownloadBinder.setStatus(FINISHED);
            }

            DownloadService downloadService = mDownloadBinder.getService();
            downloadService.setUpdateUI(new UpdateUI() {

                @Override
                public void updateProgress(int progress) {
                    progressButton.setProgress(progress);
                    if (progress == 100) {
                        progressButton.setText(R.string.install);
                    }
                }
            });

            switch (mDownloadBinder.getStatus()) {
                case DOWNLOADING:
//                    progressButton.setText(R.string.pause);
                    break;
                case PAUSED:
                    progressButton.setProgress(mDownloadBinder.getProgress());
                    progressButton.setText(R.string.download);
                    break;
                case FINISHED:
                    if (mOTAPackage.exists()) {
                        progressButton.setText(R.string.install);
                    } else {
                        progressButton.setText(R.string.download);
                        mDownloadBinder.setStatus(NOT_START);
                    }
                    break;
                case NOT_START:
                    break;
                default:
                    break;
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_download_details);

        mApp = (OTAApplication) getApplication();
        Intent intent = getIntent();

        if (mApp.getDownloadUrl() == null) {
            mApp.setOta(intent.getStringExtra("ota"));
            mApp.setReleaseNote(intent.getStringExtra("releaseNote"));
            mApp.setDownloadUrl(intent.getStringExtra("url"));
            mApp.setNewVersion(intent.getStringExtra("newVersion"));
            mApp.setPercent(intent.getIntExtra("percent", 0));
        }

        Log.d(TAG, "onCreate: mApp=" + mApp);

        if (ContextCompat.checkSelfPermission(DownloadDetailsActivity.this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(DownloadDetailsActivity.this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        }

        TextView textViewReleaseNote = findViewById(R.id.textReleaseNote);
        textViewReleaseNote.setText(mApp.getReleaseNote());

        progressButton = findViewById(R.id.pb);
        progressButton.setTag(0);
        int percent = mApp.getPercent();
        if (percent > 0 && percent < 100) {
            progressButton.setProgress(percent);
        }

        String url = mApp.getDownloadUrl();
        String otaFileName = url.substring(url.lastIndexOf("=") + 1);
        mOTAPackage = new File(Environment.getExternalStorageDirectory().getPath() + "/", otaFileName);

        progressButton.setOnClickListener(view -> {
            progressButton.setTag(1);
            if (mDownloadBinder == null) {
                return;
            }

            switch (mDownloadBinder.getStatus()) {
                case FINISHED:
                    Intent service = new Intent();
                    service.setClassName("com.evenwell.OTAUpdate",
                            "com.evenwell.OTAUpdate.OTAService");
                    service.putExtra("EXTRA_ACTION", "ACTION_CHECK_UPDATEZIP_SERVICE");
                    startService(service);
                    break;
                case DOWNLOADING:
                    mDownloadBinder.pauseDownload();
                    progressButton.setText(R.string.download);
                    break;
                case NOT_START:
                case PAUSED:
                    mDownloadBinder.startDownload(url);
                    break;
                default:
                    break;
            }
        });

        progressButton.setOnLongClickListener(View -> {

            if (mOTAPackage.exists()) {
                mOTAPackage.delete();
                Toast.makeText(DownloadDetailsActivity.this, "deleted succeed",
                        Toast.LENGTH_LONG).show();
            }

            return false;
        });

        Intent service = new Intent(this, DownloadService.class);
        startService(service);
        bindService(service, mServiceConnection, BIND_AUTO_CREATE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "please grant permission", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        unbindService(mServiceConnection);
    }
}

package com.foxconn.zzdc.sdcardupdate.download;

import android.Manifest;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
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
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.foxconn.zzdc.sdcardupdate.R;
import com.foxconn.zzdc.sdcardupdate.UpdateUI;

import java.io.File;

import static com.foxconn.zzdc.sdcardupdate.download.DownloadStatus.NOT_START;

public class DownloadDetailsActivity extends Activity {

    private TextView mTextViewReleaseNote;
    private ProgressBar mProgressBar;
    private Button mButton;
    private File mOTAPackage;

    private DownloadService.DownloadBinder mDownloadBinder;
    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mDownloadBinder = (DownloadService.DownloadBinder) service;

            DownloadService downloadService = mDownloadBinder.getService();
            downloadService.setUpdateUI(new UpdateUI() {
                @Override
                public void updateButton() {
                    mButton.setText(R.string.install);
                }

                @Override
                public void updateProgress(int progress) {
                    mProgressBar.setVisibility(View.VISIBLE);
                    mProgressBar.setProgress(progress);
                }
            });

            switch (mDownloadBinder.getStatus()) {
                case DOWNLOADING:
                    mButton.setText(R.string.pause);
                    break;
                case PAUSED:
                    mProgressBar.setVisibility(View.VISIBLE);
                    mProgressBar.setProgress(mDownloadBinder.getProgress());
                    mButton.setText(R.string.download);
                    break;
                case FINISHED:
                    if (mOTAPackage.exists()) {
                        mButton.setText(R.string.install);
                    } else {
                        mButton.setText(R.string.download);
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

        mTextViewReleaseNote = findViewById(R.id.textReleaseNote);
        mProgressBar = findViewById(R.id.progressBar);
        mButton = findViewById(R.id.button);

        Intent intent = getIntent();
        String url = intent.getStringExtra("url");
        mOTAPackage = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS).getPath(), url.substring(url.lastIndexOf('/')));
        String releaseNote = intent.getStringExtra("releaseNote");
        int percent = intent.getIntExtra("percent", 0);
        String ota = intent.getStringExtra("ota");

        mTextViewReleaseNote.setText(releaseNote);
        mProgressBar.setProgress(percent);
        if (percent > 0) {
            mProgressBar.setVisibility(View.VISIBLE);
        }
        mButton.setOnClickListener(view -> {
            if (mDownloadBinder == null) {
                return;
            }

            switch (mDownloadBinder.getStatus()) {
                case FINISHED:
                    //TODO: install the ota package here
                    //intent.setClassName("com.evenwell.OTAUpdate", "com.evenwell.OTAUpdate.OTAService");
                    Intent service = new Intent();
                    service.setClassName("com.evenwell.OTAUpdate",
                            "com.evenwell.OTAUpdate.OTAService");
                    startService(service);
                    Toast.makeText(DownloadDetailsActivity.this, "Install OTA",
                            Toast.LENGTH_LONG).show();
                    break;
                case DOWNLOADING:
                    mDownloadBinder.pauseDownload();
                    mButton.setText(R.string.download);
                    break;
                case NOT_START:
                case PAUSED:
                    mProgressBar.setVisibility(TextView.VISIBLE);
                    mDownloadBinder.startDownload(url);
                    mButton.setText(R.string.pause);
                    break;
                default:
                    break;
            }
        });
        mButton.setOnLongClickListener(View -> {
            String directory = Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS).getPath();
            String fileName = url.substring(url.lastIndexOf("/"));
            File file = new File(directory, fileName);

            if (file.exists()) {
                file.delete();
                Toast.makeText(DownloadDetailsActivity.this, "deleted succeed",
                        Toast.LENGTH_LONG).show();
            }

            return false;
        });

        Intent service = DownloadService.newIntent(this, releaseNote, ota, url, percent);
        startService(service);
        bindService(service, mServiceConnection, BIND_AUTO_CREATE);

        if (ContextCompat.checkSelfPermission(DownloadDetailsActivity.this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(DownloadDetailsActivity.this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        }
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

    public static Intent newIntent(Context context,
                                   String releaseNote,
                                   String downloadURL,
                                   String ota,
                                   int percent) {
        Intent intent = new Intent(context, DownloadDetailsActivity.class);
        intent.putExtra("releaseNote", releaseNote);
        intent.putExtra("url", downloadURL);
        intent.putExtra("ota", ota);
        intent.putExtra("percent", percent);

        return intent;
    }
}

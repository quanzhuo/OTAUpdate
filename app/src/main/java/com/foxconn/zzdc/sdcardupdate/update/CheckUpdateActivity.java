package com.foxconn.zzdc.sdcardupdate.update;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Paint;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.foxconn.zzdc.sdcardupdate.R;
import com.foxconn.zzdc.sdcardupdate.autoupdate.SettingActivity;
import com.foxconn.zzdc.sdcardupdate.download.DownloadDetailsActivity;
import com.foxconn.zzdc.sdcardupdate.tool.OTAApplication;

import static com.foxconn.zzdc.sdcardupdate.tool.Util.checkUpdate;
import static com.foxconn.zzdc.sdcardupdate.tool.Util.isNetworkConnected;

public class CheckUpdateActivity extends AppCompatActivity {

    private static final int READ_SN = 1;
    private static final int WRITE_STORAGE = 2;

    public static String sCheckOTAUrl;
    public static String sSN;

    private TextView mTextViewNewVersion;
    private TextView mTextViewCurVersion;
    private Button mButtonCheckUpdate;
    private ProgressBar mProgressBar;
    private static final String TAG = "CheckUpdateActivity";

    private OTAApplication app;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_check_update);

        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        app = (OTAApplication) getApplication();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE)
                == PackageManager.PERMISSION_GRANTED) {
            sSN = Build.getSerial();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_PHONE_STATE,}, READ_SN);
        }

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,}, WRITE_STORAGE);
        }

        mTextViewCurVersion = findViewById(R.id.currentVersion);
        mTextViewCurVersion.setText(Build.VERSION.INCREMENTAL);

        mTextViewNewVersion = findViewById(R.id.newVersion);
        mTextViewNewVersion.setOnClickListener(view -> {
            Intent intent = new Intent(CheckUpdateActivity.this, DownloadDetailsActivity.class);
            startActivity(intent);
        });

        mProgressBar = findViewById(R.id.progressCircular);
        UpdateHandler uiHandler = updatable -> {
            runOnUiThread(() -> {
                if (updatable) {
                    mTextViewNewVersion.setTextColor(getColor(R.color.colorAccent));
                    mTextViewNewVersion.getPaint().setFlags(Paint.UNDERLINE_TEXT_FLAG);
                    mTextViewNewVersion.setText(app.getNewVersion());
                } else {
                    mTextViewNewVersion.setEnabled(false);
                    mTextViewNewVersion.setText(R.string.no_new_version);
                }
                mProgressBar.setVisibility(View.GONE);
            });
        };

        if (isNetworkConnected(this)) {
            checkUpdate(this, uiHandler);
        } else {
            mTextViewNewVersion.setText(R.string.network_unavaliable);
        }

        mButtonCheckUpdate = findViewById(R.id.buttonCheckUpdate);
        mButtonCheckUpdate.setOnClickListener(view -> {
            mProgressBar.setVisibility(View.VISIBLE);
            if (isNetworkConnected(CheckUpdateActivity.this)) {
                checkUpdate(CheckUpdateActivity.this, uiHandler);
            } else {
                Toast.makeText(CheckUpdateActivity.this,
                        R.string.network_unavaliable, Toast.LENGTH_LONG).show();
                mProgressBar.setVisibility(View.GONE);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.settings:
                Intent intent = new Intent(this, SettingActivity.class);
                startActivity(intent);
                break;

            case android.R.id.home:
                finish();

            default:
        }
        return true;
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == READ_SN) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                sSN = Build.getSerial();
            } else {
                Toast.makeText(this, R.string.no_permission_toast, Toast.LENGTH_LONG).show();
            }
        } else if (requestCode == WRITE_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_DENIED) {
                Toast.makeText(this, R.string.no_permission_toast, Toast.LENGTH_LONG).show();
                android.os.Process.killProcess(android.os.Process.myPid());
                System.exit(0);
            }
        }
    }
}

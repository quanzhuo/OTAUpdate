package com.foxconn.zzdc.sdcardupdate;

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
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.foxconn.zzdc.sdcardupdate.autoupdate.SettingActivity;
import com.foxconn.zzdc.sdcardupdate.download.DownloadDetailsActivity;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import static java.lang.Thread.sleep;

public class CheckUpdateActivity extends AppCompatActivity {

    private static final int READ_SN = 1;

    public static String mNewVersion;
    public static String mDownloadURL;
    public static String mReleaseNote;
    public static String mOTA;
    public static String checkOTAUrl;

    public static CheckUpdateActivity sActivity;

    private TextView mTextViewNewVersion;
    private TextView mTextViewCurVersion;
    private Button mButtonCheckUpdate;
    private ProgressBar mProgressBar;
    private String mSN;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_check_update);

        sActivity = this;

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE)
                == PackageManager.PERMISSION_GRANTED) {
            mSN = Build.getSerial();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_PHONE_STATE,}, READ_SN);
        }

        checkOTAUrl = BuildConfig.OTA_SERVER_URL + "?current=" +
                Build.VERSION.INCREMENTAL + "&sn=" + mSN;

        mTextViewCurVersion = findViewById(R.id.currentVersion);
        mTextViewCurVersion.setText(Build.VERSION.INCREMENTAL);

        mTextViewNewVersion = findViewById(R.id.newVersion);
        mTextViewNewVersion.setOnClickListener(view -> {
            Intent intent = DownloadDetailsActivity.newIntent(CheckUpdateActivity.this,
                    mReleaseNote, mDownloadURL, mOTA, 0);
            startActivity(intent);
        });

        mProgressBar = findViewById(R.id.progressCircular);
        UpdateHandler uiHandler = updatable -> {
            runOnUiThread(() -> {
                if (updatable) {
                    mTextViewNewVersion.setTextColor(getColor(R.color.colorAccent));
                    mTextViewNewVersion.getPaint().setFlags(Paint.UNDERLINE_TEXT_FLAG);
                } else {
                    mTextViewNewVersion.setEnabled(false);
                    mNewVersion = getString(R.string.no_new_version);
                }

                mProgressBar.setVisibility(View.GONE);
                mTextViewNewVersion.setText(mNewVersion);
            });
        };
        checkUpdate(uiHandler);

        mButtonCheckUpdate = findViewById(R.id.buttonCheckUpdate);
        mButtonCheckUpdate.setOnClickListener(view -> {
            mProgressBar.setVisibility(View.VISIBLE);
            checkUpdate(uiHandler);
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
            default:
        }
        return true;
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == READ_SN) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                mSN = Build.getSerial();
            } else {
                Toast.makeText(this, R.string.no_permission_toast, Toast.LENGTH_LONG).show();
            }
        }
    }

    public static String getHttpResponse(String downloadURL) {
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

    public static void checkUpdate(UpdateHandler handler) {
        new Thread(() -> {
            String response = getHttpResponse(checkOTAUrl);
            JSONObject jsonResponse;
            boolean updatable;

            try {
                sleep(500);
                jsonResponse = new JSONObject(response);
                updatable = jsonResponse.getString("updatable").equals("true");
                if (updatable) {
                    mNewVersion = jsonResponse.getString("newVersion");
                    mDownloadURL = jsonResponse.getString("url");
                    mReleaseNote = jsonResponse.getString("releaseNote");
                    mOTA = jsonResponse.getString("ota");
                }

                Log.d("quanzhuo", "newver: " + mNewVersion + ", nDownURL: " + mDownloadURL +
                        ", release: " + mReleaseNote + ", mOTA: " + mOTA);

                handler.run(updatable);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}

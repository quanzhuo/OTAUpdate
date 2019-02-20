package com.foxconn.zzdc.sdcardupdate.autoupdate;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.CompoundButton;
import android.widget.Switch;

import com.foxconn.zzdc.sdcardupdate.R;

import java.util.Calendar;

import static com.foxconn.zzdc.sdcardupdate.CheckUpdateActivity.checkUpdate;

public class SettingActivity extends AppCompatActivity {

    private Switch switch_Download;
    private Switch switch_Install;

    public static final String AUTO_DOWNLOAD = "auto_download";
    public static final String AUTO_INSTALL = "auto_install";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);

        switch_Download = findViewById(R.id.sw_autoDownload);
        switch_Install = findViewById(R.id.sw_autoInstall);

        SharedPreferences pref = getSharedPreferences(getString(R.string.preference_file_name),
                MODE_PRIVATE);

        switch_Download.setChecked(pref.getBoolean(AUTO_DOWNLOAD, true));
        switch_Install.setChecked(pref.getBoolean(AUTO_INSTALL, true));


        SharedPreferences.Editor editor = pref.edit();

        switch_Download.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {

                    editor.putBoolean(AUTO_DOWNLOAD, true);

                    int value = getBatteryLevel();
                    Log.d("SettingActivity", "onCheckedChanged: BatteryLevel:" + value);

                    Boolean wifiState = getWifiState();
                    Log.d("SettingActivity", "onCheckedChanged: wifiState:" + wifiState);

                    if (value >= 30 && (wifiState = true)) {
                        Intent service = new Intent(SettingActivity.this, AutoUpdateService.class);
                        startService(service);
                    }

                } else {
                    editor.putBoolean(AUTO_DOWNLOAD, false);

                }
                editor.commit();

            }
        });

        switch_Install.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {

                    editor.putBoolean(AUTO_INSTALL, true);

                    int value = getBatteryLevel();
                    Log.d("SettingActivity", "onCheckedChanged: BatteryLevel:" + value);

                    Calendar calendar = Calendar.getInstance();
                    int hour = calendar.get(Calendar.HOUR_OF_DAY);
                    int minute = calendar.get(Calendar.MINUTE);
                    Log.d("SettingActivity", "onCheckedChanged: " + hour + ":" + minute);

                    if (value >= 50 && (hour <= 5)) {

                    }
                } else {
                    editor.putBoolean(AUTO_INSTALL, false);

                }
                editor.commit();

            }
        });
    }

    public int getBatteryLevel() {

        Intent batteryInfoIntent = getApplicationContext().registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        int level = batteryInfoIntent.getIntExtra("level", 0);
        return level;
    }

    public boolean getWifiState() {

        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        @SuppressLint("MissingPermission") NetworkInfo wifiNetworkInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        if (wifiNetworkInfo.isConnected()) {
            return true;
        }
        return false;
    }
}

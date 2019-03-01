package com.foxconn.zzdc.sdcardupdate.tool;

import android.app.Application;

public class OTAApplication extends Application {
    private boolean updatable;
    private String newVersion;
    private String downloadUrl;
    private String releaseNote;
    private String ota;
    private int percent;

    public boolean isUpdatable() {
        return updatable;
    }

    public void setUpdatable(boolean updatable) {
        this.updatable = updatable;
    }

    public String getNewVersion() {
        return newVersion;
    }

    public void setNewVersion(String newVersion) {
        this.newVersion = newVersion;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }

    public String getReleaseNote() {
        return releaseNote;
    }

    public void setReleaseNote(String releaseNote) {
        this.releaseNote = releaseNote;
    }

    public String getOta() {
        return ota;
    }

    public void setOta(String ota) {
        this.ota = ota;
    }

    public int getPercent() {
        return percent;
    }

    public void setPercent(int percent) {
        this.percent = percent;
    }

    @Override
    public String toString() {
        return "OTAApplication{" +
                "updatable=" + updatable +
                ", newVersion='" + newVersion + '\'' +
                ", downloadUrl='" + downloadUrl + '\'' +
                ", releaseNote='" + releaseNote + '\'' +
                ", ota='" + ota + '\'' +
                ", percent=" + percent +
                '}';
    }
}

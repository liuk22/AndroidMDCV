package com.example.kaich.nvmotiondetection;

import android.util.DisplayMetrics;

public class PhotoTools {

    public static final int RESULT_LOAD_VIDEO = 0;
    public static final String ANALZYED_VIDEO_FILE_TAG = "_analyzedNVMD";

    public static final String FOURCC = "H265";
    public static final String FILE_EXTENSION = ".mp4"; //Don't know if these work yet

    private DisplayMetrics displayMetrics;

    public PhotoTools(DisplayMetrics displayMetrics){
        this.displayMetrics = displayMetrics;

    }

    public int dpToPx(int dp){
        return Math.round(dp * displayMetrics.density);
    }

    public int pxToDp(int px){
        return Math.round(px / displayMetrics.density);
    }
}

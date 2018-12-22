package com.example.kaich.nvmotiondetection;

import android.util.DisplayMetrics;

public class PhotoTools {

    public static final int RESULT_LOAD_VIDEO = 0;

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

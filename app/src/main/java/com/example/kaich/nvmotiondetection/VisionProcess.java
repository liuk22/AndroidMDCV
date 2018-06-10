package com.example.kaich.nvmotiondetection;

import android.view.Surface;

import org.opencv.core.Mat;

public class VisionProcess {
    /*
    take a look at these HSV values for a dark room
    Scalar(H, S, V)
    Scalar(0, 0, 8)
    to
    Scalar(180, 255, 197)
     */
    private int xRes;
    private int yRes;

    public VisionProcess(int xRes, int yRes) {
        this.xRes = xRes;
        this.yRes = yRes;

    }



}

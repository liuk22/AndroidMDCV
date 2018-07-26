package com.example.kaich.nvmotiondetection;

import android.view.Surface;

import org.opencv.core.Mat;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;

import java.io.File;

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

    private int brightness;

    private Mat mat;

    public VisionProcess(int xRes, int yRes) {
        this.xRes = xRes;
        this.yRes = yRes;
    }

    private File analyze(File file){

        return file;
    }

    private void imageAnalysis(){

    }

    private void videoAnalysis(int cameraId){ //no idea what format cameraId for Android
        VideoCapture videoCapture = new VideoCapture(cameraId);
        //check if it was opened

        while(true){ //inject Thread.sleep or whatever necessary to allow for frame skips, performance etc.
            Mat frame = new Mat();
            videoCapture.read(frame);

        }
        //algorithm should start by turning RGB2GRAY
        //get a "frame delta"
        //threshold it
        //bounding rect
    }

    private void setBrightness(int brightness){

    }





}

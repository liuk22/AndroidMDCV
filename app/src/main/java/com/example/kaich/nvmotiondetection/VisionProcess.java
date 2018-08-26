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

    private int xRes = 1280; //default max values
    private int yRes = 720;
    private int fps = 30;

    private int brightness;

    private VideoCapture vc;
    private File file;
    private String absoluteFilePath;

    public VisionProcess(File file){
        this.file = file;
        absoluteFilePath = file.getAbsolutePath();
        //determine
    }

    private File analyze(){
        //should return replacement for file input - image or video
    }

    private void imageAnalysis(){
        //only gamma correction? cannot do motion detection with single frame post-capture
    }

    private void videoAnalysis(){
        //read in File as video
        //check if it was opened
        VideoCapture videoCapture = new VideoCapture(absoluteFilePath); //Android only uses MJPG fourcc

        while(true){ //inject Thread.sleep or whatever necessary to allow for frame skips, performance etc.
            Mat frame = new Mat();
            videoCapture.read(frame);

        }
        //algorithm should start by turning RGB2GRAY
        //get a "frame delta"
        //threshold it
        //bounding rect

        //end by applying histogram and then gamma correction
    }

    public void setBrightness(int brightness){
        this.brightness = brightness;
    }

    public int getXRes(){
        return xRes;
    }

    public int getYRes(){
        return yRes;
    }

    public int getFPS(){
        return fps;
    }





}

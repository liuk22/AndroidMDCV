package com.example.kaich.nvmotiondetection;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.VideoWriter;

import java.io.File;
import java.util.ArrayList;

public class VisionProcess {

    /*
    take a look at these HSV values for a dark room
    Scalar(H, S, V)
    Scalar(0, 0, 8)
    to
    Scalar(180, 255, 197)
     */

    public static String FOURCC = "H264";
    public static String FILE_EXTENSION = ".avi"; //Don't know if these work yet
    public static String ANALYZED_NAME_TAG = "analyzedNVMD";

    private int frameWidth = 1280; //must be modified depending on CameraDevice
    private int frameHeight = 720;
    private int FPS = 15;


    private int brightness;
    private int resetReferenceFactor = 2;
    private int gaussianPixelIntensity = 21;
    private int deltaFrameThreshold = 32;
    private int minContourArea = 600;

    private VideoCapture videoCapture = new VideoCapture();
    private VideoWriter videoWriter = new VideoWriter();

    private String currentReadPath;

    public VisionProcess(){
        //TASK consider instance differences across devices, etc
    }

    private void analyze(File inputFile){

        //TASK return analyzed video
        currentReadPath = inputFile.getAbsolutePath(); //TASK does this work properly? 

        videoWriter.open(currentReadPath + ANALYZED_NAME_TAG + FILE_EXTENSION, VideoWriter.fourcc(FOURCC.charAt(0),FOURCC.charAt(1), FOURCC.charAt(2), FOURCC.charAt(3)),
                FPS, new Size(frameWidth, frameHeight));

        Mat referenceFrame = null;

        videoCapture.open(currentReadPath);

        int resetReferenceCount = 0;
        while(true) {
            if (referenceFrame == null || resetReferenceCount%(FPS/resetReferenceFactor) == 0){
                referenceFrame = new Mat();
                if(!videoCapture.read(referenceFrame)) {
                    break;
                }

                Imgproc.cvtColor(referenceFrame, referenceFrame, Imgproc.COLOR_BGR2GRAY);
                Imgproc.GaussianBlur(referenceFrame, referenceFrame, new Size(gaussianPixelIntensity, gaussianPixelIntensity), 0); // check on these numbers

            }else {

                Mat currentFrame = new Mat();
                if(!videoCapture.read(currentFrame)) {
                    break;
                }
                Mat colorFrame = currentFrame.clone();

                Imgproc.cvtColor(currentFrame, currentFrame, Imgproc.COLOR_BGR2GRAY);
                Imgproc.GaussianBlur(currentFrame, currentFrame, new Size(gaussianPixelIntensity, gaussianPixelIntensity), 0);

                Mat deltaFrame = new Mat();
                Core.absdiff(referenceFrame, currentFrame, deltaFrame);
                Imgproc.threshold(deltaFrame, deltaFrame, deltaFrameThreshold, 255, Imgproc.THRESH_BINARY);
                Imgproc.dilate(deltaFrame, deltaFrame, new Mat());

                ArrayList<MatOfPoint> contours = new ArrayList<MatOfPoint>();
                Mat hierarchy = new Mat(); //empty hierarchy for simple stationary detection. Consider application differently
                //for instability
                Imgproc.findContours(deltaFrame, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

                for (int i = 0; i < contours.size(); i++) {
                    if(Imgproc.contourArea(contours.get(i)) < minContourArea) {
                        continue;
                    }
                    Rect boundingRect = Imgproc.boundingRect(contours.get(i));
                    Imgproc.rectangle(colorFrame, new Point(boundingRect.x, boundingRect.y),
                            new Point(boundingRect.x + boundingRect.width, boundingRect.y + boundingRect.height), new Scalar(0, 255, 0), 5);
                    //color picking for the rectangle color is based on destination Mat color space (BGR)
                }

                videoWriter.write(colorFrame);

            }
            resetReferenceCount += 1;
        }
    }


    public void setBrightness(int brightness){
        this.brightness = brightness;
    }

    public int getBrightness(){
        return brightness;
    }

    public void setFrameWidth(int frameWidth){
        this.frameWidth = frameWidth;
    }

    public int getFrameWidth(){
        return frameWidth;
    }

    public void setFrameHeight(int frameHeight){
        this.frameHeight = frameHeight;
    }

    public int getFrameHeight(){
        return frameHeight;
    }

    public void setFPS(int FPS){
        this.FPS = FPS;
    }

    public int getFPS(){
        return FPS;
    }

}

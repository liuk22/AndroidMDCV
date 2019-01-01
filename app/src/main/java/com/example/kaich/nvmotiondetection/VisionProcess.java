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



    private int frameWidth = 1280; //must be modified depending on CameraDevice
    private int frameHeight = 720;
    private int FPS = 15;

    private int brightness;
    private int resetReferenceFactor = 2;
    private double gammaCorrectionConstant = 4.0;
    private int gaussianPixelIntensity = 21;
    private int deltaFrameThreshold = 32;
    private int minContourArea = 600;

    private VideoCapture videoCapture = new VideoCapture();
    private VideoWriter videoWriter = new VideoWriter();

    public VisionProcess(){
        //TASK consider instance differences across devices, etc
    }

    public void analyzeAndWrite(String readPath, String writePath){ //TASK how will this work with including the file name?

        videoWriter.open(writePath,
                VideoWriter.fourcc(PhotoTools.FOURCC.charAt(0),PhotoTools.FOURCC.charAt(1), PhotoTools.FOURCC.charAt(2), PhotoTools.FOURCC.charAt(3)),
                FPS,
                new Size(frameWidth, frameHeight));

        Mat referenceFrame = null;

        videoCapture.open(readPath);

        int resetReferenceCount = 0;
        while(true) {
            if (referenceFrame == null || resetReferenceCount%(FPS/resetReferenceFactor) == 0){
                referenceFrame = new Mat();
                if(!videoCapture.read(referenceFrame)) {
                    break;
                }

                Imgproc.cvtColor(referenceFrame, referenceFrame, Imgproc.COLOR_BGR2GRAY);
                Imgproc.equalizeHist(referenceFrame, referenceFrame); //increases contrast maybe too much
                correctGamma(referenceFrame, gammaCorrectionConstant);
                Imgproc.GaussianBlur(referenceFrame, referenceFrame, new Size(gaussianPixelIntensity, gaussianPixelIntensity), 0); // check on these numbers

            }else {

                Mat currentFrame = new Mat();
                if(!videoCapture.read(currentFrame)) {
                    break;
                }
                Mat colorFrame = currentFrame.clone();

                Imgproc.cvtColor(currentFrame, currentFrame, Imgproc.COLOR_BGR2GRAY);
                Imgproc.equalizeHist(currentFrame, currentFrame); //increases contrast maybe too much
                correctGamma(currentFrame, gammaCorrectionConstant);
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

    private void correctGamma(Mat image, double gamma){
        //gamma corrects each pixel in the image. Currently for grayscale (1 channel == intensity) image

        double inverseGamma = 1.0/gamma;
        int[] lookupTable = new int[256];
        for(int i = 0; i < lookupTable.length; i++) {
            lookupTable[i] = (int)(Math.pow((i / 255.0), inverseGamma) * 255);
        }
        for(int row = 0; row < image.height(); row++) {
            for(int col = 0; col < image.width(); col++) {
                byte[] imageData = {(byte)lookupTable[(int) image.get(row, col)[0]], 0, 0};
                image.put(row, col, imageData);	//image.get returns an array of the channel values
            }
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

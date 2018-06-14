package com.example.kaich.nvmotiondetection;


import android.content.Context;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.v7.app.AppCompatActivity;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.support.v7.widget.Toolbar;
import android.widget.VideoView;
import android.util.Size;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    //HSI
    private CameraDevice mCameraDevice;
    //UI
    private Toolbar mTopToolbar;
    private SeekBar mSeekBar;
    private SeekBar.OnSeekBarChangeListener mOnSeekBarChangeListener = new SeekBar.OnSeekBarChangeListener(){
        @Override
        public void onProgressChanged(SeekBar seekbar, int progress, boolean b){

        }
        @Override
        public void onStartTrackingTouch(SeekBar seekbar){

        }
        @Override
        public void onStopTrackingTouch(SeekBar seekbar){

        }
    };
    private ImageButton mSunBrightness;
    private ImageButton.OnClickListener mOnSunBrightnessClickListnener = new ImageButton.OnClickListener() {
        @Override
        public void onClick(View view) {

        }
    };
    private ImageButton mMoonBrightness;
    private ImageButton.OnClickListener mOnMoonBrightnessClickListener = new ImageButton.OnClickListener(){
        @Override
        public void onClick(View view){

        }
    };
    private TextureView mTextureView;
    private Toolbar mBottomToolbar;
    private ImageButton mCameraButton;
    private ImageButton.OnClickListener mOnCameraClickListener = new ImageButton.OnClickListener(){
        @Override
        public void onClick(View view){

        }
    };
    private ImageButton mRecordTypeButton;
    private ImageButton.OnClickListener mOnRecordTypeClickListener = new ImageButton.OnClickListener(){
        @Override
        public void onClick(View view){

        }
    };
    private ImageButton mSwitchCameraButton;
    private ImageButton.OnClickListener mOnSwitchCameraClickListener = new ImageButton.OnClickListener(){
        @Override
        public void onClick(View view){

        }
    };

    //analytical
    private VisionProcess mVisionProcess;

    //file saving
    private File mFile;
    private Handler mBackgroundHandler;
    private HandlerThread mBackgroundThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        System.loadLibrary("opencv_java3"); //do NOT use Core.NATIVE_LIBRARY_NAME, OpenCV is already treated as local in jni

        mTopToolbar = findViewById(R.id.topToolbar);
        mSeekBar = (SeekBar) findViewById(R.id.seekBar);
        mSeekBar.setOnSeekBarChangeListener((mOnSeekBarChangeListener));
        mSunBrightness = findViewById(R.id.sunBrightness);
        mSunBrightness.setOnClickListener(mOnSunBrightnessClickListnener);
        mMoonBrightness = findViewById(R.id.moonBrightness);
        mMoonBrightness.setOnClickListener(mOnMoonBrightnessClickListener);

        mTextureView = findViewById(R.id.textureView);

        mBottomToolbar = findViewById(R.id.bottomToolbar);
        mCameraButton = findViewById(R.id.cameraButton);
        mCameraButton.setOnClickListener(mOnCameraClickListener);
        mRecordTypeButton = findViewById(R.id.recordTypeButton);
        mRecordTypeButton.setOnClickListener(mOnRecordTypeClickListener);
        mSwitchCameraButton = findViewById(R.id.switchCameraButton);
        mSwitchCameraButton.setOnClickListener(mOnSwitchCameraClickListener);

    }

    private void openCamera(){

    }

    private void takePicture(){
        if(mCameraDevice == null) {
            return;
        }
        CameraManager manager = (CameraManager)getSystemService(Context.CAMERA_SERVICE);
        try{
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(mCameraDevice.getId());
            Size[] jpegSizes = null;
            if(characteristics != null){
                jpegSizes = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP).getOutputSizes(ImageFormat.JPEG);
            }else{ //set default image size here
                jpegSizes[0] = new Size(1280, 720); //max OpenCV offers
            }
            ImageReader reader = ImageReader.newInstance(jpegSizes[0].getWidth(), jpegSizes[0].getHeight(), ImageFormat.JPEG, 1);
            List<Surface> outputSurface = new ArrayList<>(2);
            outputSurface.add(reader.getSurface());
            outputSurface.add(new Surface(mTextureView.getSurfaceTexture()));

        }catch(CameraAccessException e){
            e.printStackTrace();
        }


    }

    private void takeVideo(){

    }


}

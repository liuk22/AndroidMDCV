package com.example.kaich.nvmotiondetection;


import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.support.v7.widget.Toolbar;
import android.widget.Toast;
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

    //permissions
    private static final int MY_PERMISSIONS_REQUEST_CAMERA = 500;
    private static final int MY_PERMISSIONS_REQUEST_STORAGE = 600;

    //analytical
    private VisionProcess VisionProcess;

    //file storage
    private File File;
    private Handler BackgroundHandler;
    private HandlerThread BackgroundThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        System.loadLibrary("opencv_java3"); //do NOT use Core.NATIVE_LIBRARY_NAME, OpenCV is already treated as local in jni

        mTopToolbar = findViewById(R.id.topToolbar);
        mSeekBar = findViewById(R.id.seekBar);
        mSunBrightness = findViewById(R.id.sunBrightness);
        mMoonBrightness = findViewById(R.id.moonBrightness);


        mTextureView = findViewById(R.id.textureView);
        mBottomToolbar = findViewById(R.id.bottomToolbar);
        mCameraButton = findViewById(R.id.cameraButton);
        mRecordTypeButton = findViewById(R.id.recordTypeButton);
        mSwitchCameraButton = findViewById(R.id.switchCameraButton);

        //listeners not set until permissions granted

        if(ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA},MY_PERMISSIONS_REQUEST_CAMERA);
        }else if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, MY_PERMISSIONS_REQUEST_STORAGE);
        }else{
            //all permissions already granted
            openCamera();
        }

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

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        switch(requestCode){
            case MY_PERMISSIONS_REQUEST_CAMERA: //consecutive permission requests
                if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){

                    if(ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
                        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, MY_PERMISSIONS_REQUEST_STORAGE);
                    }else{
                        //permission already granted
                    }

                }else{
                    Toast.makeText(this, "This application cannot function without camera permissions", 3);
                }
                return;
            case MY_PERMISSIONS_REQUEST_STORAGE: //can also feed from if user exited after first permission
                if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){

                    //all permissions granted, enable functionality

                    mSeekBar.setOnSeekBarChangeListener((mOnSeekBarChangeListener));
                    mSunBrightness.setOnClickListener(mOnSunBrightnessClickListnener);
                    mMoonBrightness.setOnClickListener(mOnMoonBrightnessClickListener);

                    mCameraButton.setOnClickListener(mOnCameraClickListener);
                    mRecordTypeButton.setOnClickListener(mOnRecordTypeClickListener);
                    mSwitchCameraButton.setOnClickListener(mOnSwitchCameraClickListener);


                }else{
                    Toast.makeText(this, "This application cannot function without storage permissions", 3);
                }
                return;
        }

    }
}

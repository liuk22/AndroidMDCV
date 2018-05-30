package com.example.kaich.nvmotiondetection;

import android.graphics.Camera;
import android.hardware.camera2.CameraManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.widget.SeekBar;
import android.widget.Toolbar;
import android.widget.VideoView;

public class MainActivity extends AppCompatActivity {

    private HardwareMap mHardWareMap;
    private CameraManager mCameraManager;
    private Camera mCamera;

    private Toolbar mTopToolbar;
    private Toolbar mBottomToolbar;
    private SeekBar mSeekBar;
    private SeekBar.OnSeekBarChangeListener mOnSeekBarChangeListener
            = new SeekBar.OnSeekBarChangeListener(){
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
    private VideoView mVideoView;

    private VisionProcess mVisionProcess;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mHardWareMap = new HardwareMap();
        //mCameraManager, camera

        mTopToolbar = (Toolbar) findViewById(R.id.topToolbar);
        mSeekBar = (SeekBar) findViewById(R.id.seekBar);
        mSeekBar.setOnSeekBarChangeListener((mOnSeekBarChangeListener));

        mVideoView = (VideoView) findViewById(R.id.videoView);

        mBottomToolbar = (Toolbar) findViewById(R.id.bottomToolbar);

        System.loadLibrary("opencv_java3"); //do NOT use Core.NATIVE_LIBRARY_NAME, OpenCV is already treated as local in jni


    }

}

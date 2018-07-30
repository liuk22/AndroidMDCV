package com.example.kaich.nvmotiondetection;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.support.v7.widget.Toolbar;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    //UI
    private Toolbar mTopToolbar;
    private SeekBar mSeekBar; //range of 5
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
            mSeekBar.incrementProgressBy(-1);
        }
    };
    private ImageButton mMoonBrightness;
    private ImageButton.OnClickListener mOnMoonBrightnessClickListener = new ImageButton.OnClickListener(){
        @Override
        public void onClick(View view){
            mSeekBar.incrementProgressBy(1);
        }
    };

    private TextureView mTextureView;
    private TextureView.SurfaceTextureListener mTextureViewSurfaceListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            updateCamera(MY_CAMERA_INDEX);
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {

        }
    };

    private Toolbar mBottomToolbar;
    private ImageButton mCameraButton;
    private ImageButton.OnClickListener mOnCameraClickListener = new ImageButton.OnClickListener(){
        @Override
        public void onClick(View view){
            if(MY_RECORDING_TYPE){
                takePicture();
            }else{
                takeVideo();
            }
        }
    };
    private ImageButton mRecordTypeButton;
    private ImageButton.OnClickListener mOnRecordTypeClickListener = new ImageButton.OnClickListener(){
        @Override
        public void onClick(View view){
            MY_RECORDING_TYPE ^= true;
            if(MY_RECORDING_TYPE){
                mRecordTypeButton.setBackgroundResource(R.drawable.ic_photo_mode_48dp);
            }else{
                mRecordTypeButton.setBackgroundResource(R.drawable.ic_video_mode_48dp);
            }
        }
    };
    private ImageButton mSwitchCameraButton;
    private ImageButton.OnClickListener mOnSwitchCameraClickListener = new ImageButton.OnClickListener(){
        @Override
        public void onClick(View view){
            if(MY_CAMERA_INDEX == 0){
                updateCamera(1);
            }else{
                updateCamera(0);
            }
        }
    };

    //permissions
    private static final int MY_PERMISSIONS_REQUEST_CAMERA = 500;
    private static final int MY_PERMISSIONS_REQUEST_STORAGE = 600;

    //camera
    private boolean MY_RECORDING_TYPE = true; //true for photo, false for video
    private int MY_CAMERA_INDEX = 0; //0 is front facing
    private CameraDevice cameraDevice;
    private ImageReader imageReader;
    private Size imageDimension;
    private CaptureRequest.Builder captureRequestBuilder;
    private CameraCaptureSession cameraCaptureSession;
    private CameraDevice.StateCallback stateCallBack = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            cameraDevice = camera;
            assert mTextureView.isAvailable();
            openCameraAndPreview();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            cameraDevice.close();
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            cameraDevice.close();
            cameraDevice = null;
        }
    };

    //vision
    private VisionProcess visionProcess = new VisionProcess();

    //file storage
    private File file;
    private Handler backgroundHandler;
    private HandlerThread backgroundThread;

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

        if(ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA},MY_PERMISSIONS_REQUEST_CAMERA);
        }else if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, MY_PERMISSIONS_REQUEST_STORAGE);
        }else{
            //all permissions already granted

            mSeekBar.setOnSeekBarChangeListener((mOnSeekBarChangeListener));
            mSunBrightness.setOnClickListener(mOnSunBrightnessClickListnener);
            mMoonBrightness.setOnClickListener(mOnMoonBrightnessClickListener);

            mTextureView = findViewById(R.id.textureView);
            mTextureView.setSurfaceTextureListener(mTextureViewSurfaceListener);

            mCameraButton.setOnClickListener(mOnCameraClickListener);
            mRecordTypeButton.setOnClickListener(mOnRecordTypeClickListener);
            mSwitchCameraButton.setOnClickListener(mOnSwitchCameraClickListener);

        }
    }

    private void openCameraAndPreview(){
        try {
            SurfaceTexture surfaceTexture = mTextureView.getSurfaceTexture();
            surfaceTexture.setDefaultBufferSize(imageDimension.getWidth(), imageDimension.getHeight());//null???
            Surface surface = new Surface(surfaceTexture);
            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            captureRequestBuilder.addTarget(surface);
            cameraDevice.createCaptureSession(Arrays.asList(surface), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    if (cameraDevice == null) {
                        return;
                    }
                    cameraCaptureSession = session;
                    updatePreview();
                }
                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession){ //apparently this is supposed to be "Changed" ...?
                    Toast toast = Toast.makeText(getApplicationContext(), "Failed to configure camera", Toast.LENGTH_SHORT);
                    toast.show();
                }
            }, null);
        }catch(Exception e){
            Log.e(null,Log.getStackTraceString(e), e);
        }
    }

    private void updateCamera(int cameraIndex){ //cameraIndex = 0 is main camera, 1 should be rear facing
        MY_CAMERA_INDEX = cameraIndex;
        CameraManager cameraManager = (CameraManager)getSystemService(Context.CAMERA_SERVICE);
        try{
            String cameraId = cameraManager.getCameraIdList()[cameraIndex];
            CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            assert map != null;
            imageDimension = map.getOutputSizes(SurfaceTexture.class)[0];
            cameraManager.openCamera(cameraId, stateCallBack, null); //permissions have already been previously checked in onCreate()

        }catch(Exception e){
            Log.e(null, Log.getStackTraceString(e), e);
        }
    }

    private void updatePreview(){
        if(cameraDevice == null){
            Toast toast = Toast.makeText(this,"Error: CameraDevice not found", Toast.LENGTH_SHORT);
            toast.show();
            toast.cancel();
        }
        captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO);
        try{
            cameraCaptureSession.setRepeatingRequest(captureRequestBuilder.build(),null, backgroundHandler);
        }catch(Exception e){
            Log.e(null, Log.getStackTraceString(e), e);
        }
    }

    private void takePicture(){
        if(cameraDevice == null){
            return;
        }
        CameraManager cameraManager = (CameraManager)getSystemService(Context.CAMERA_SERVICE);
        try{
            CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraDevice.getId());
            Size[] jpegSizes = null;
            if(characteristics != null){
                jpegSizes = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP).getOutputSizes(ImageFormat.JPEG);
            }
            //default size
            int width = visionProcess.getxRes();
            int height = visionProcess.getyRes();
            if(jpegSizes != null && jpegSizes.length > 0){
                width = jpegSizes[0].getWidth();
                height = jpegSizes[0].getHeight();
            }
            ImageReader reader = ImageReader.newInstance(width, height, ImageFormat.JPEG, 1);
            ArrayList<Surface> outputSurface = new ArrayList<>(2);
            outputSurface.add(reader.getSurface());
            outputSurface.add(new Surface(mTextureView.getSurfaceTexture()));

            final CaptureRequest.Builder captureRequestBuilder = cameraDevice.createCaptureRequest(cameraDevice.TEMPLATE_STILL_CAPTURE);
            captureRequestBuilder.addTarget(reader.getSurface());
            captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO );

            //should bother checking orientation?
            file = new File(Environment.getExternalStorageDirectory() + "/" + UUID.randomUUID().toString() + ".jpg");
            ImageReader.OnImageAvailableListener imageReaderListener = new ImageReader.OnImageAvailableListener(){
                @Override
                public void onImageAvailable(ImageReader imageReader){
                    Image image = null;
                    try{
                        image = imageReader.acquireLatestImage();
                        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                        byte[] bytes = new byte[buffer.capacity()];
                        buffer.get(bytes);
                        save(bytes);
                    }catch(Exception e){
                        Log.e(null, Log.getStackTraceString(e), e);
                    }
                    finally{
                        if(image != null){
                            image.close();
                        }
                    }
                }

                public void save(byte[] bytes) throws IOException{
                    OutputStream outputStream = null;
                    try{
                        outputStream = new FileOutputStream(file);
                        outputStream.write(bytes);

                    }finally{
                        if(outputStream != null){
                            outputStream.close();
                        }
                    }
                }
            };

            reader.setOnImageAvailableListener(imageReaderListener, backgroundHandler);
            final CameraCaptureSession.CaptureCallback captureCallbackListener = new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
                    super.onCaptureCompleted(session, request, result);
                    Toast toast = Toast.makeText(MainActivity.this, "Saved" + file, Toast.LENGTH_SHORT);
                    toast.show();
                }
            };

            cameraDevice.createCaptureSession(outputSurface, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    try{
                        cameraCaptureSession.capture(captureRequestBuilder.build(), captureCallbackListener, backgroundHandler);
                    }catch(Exception e){
                        Log.e(null, Log.getStackTraceString(e), e);
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {

                }
            }, backgroundHandler);

        }catch(CameraAccessException e){
            Log.e(null, Log.getStackTraceString(e), e);
        }
    }

    private void takeVideo(){

    }

    private void closeCamera(){

    }

    private void backgroundThread(){

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        switch(requestCode){
            case MY_PERMISSIONS_REQUEST_CAMERA: //consecutive permission requests
                if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){

                    if(ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
                        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, MY_PERMISSIONS_REQUEST_STORAGE);
                    }else{
                        //permission already granted

                        mSeekBar.setOnSeekBarChangeListener((mOnSeekBarChangeListener));
                        mSunBrightness.setOnClickListener(mOnSunBrightnessClickListnener);
                        mMoonBrightness.setOnClickListener(mOnMoonBrightnessClickListener);

                        mTextureView = findViewById(R.id.textureView);
                        mTextureView.setSurfaceTextureListener(mTextureViewSurfaceListener);

                        mCameraButton.setOnClickListener(mOnCameraClickListener);
                        mRecordTypeButton.setOnClickListener(mOnRecordTypeClickListener);
                        mSwitchCameraButton.setOnClickListener(mOnSwitchCameraClickListener);

                    }

                }else{
                    Toast toast = Toast.makeText(getApplicationContext(), "This application cannot function without camera permissions", Toast.LENGTH_SHORT);
                    toast.show();
                }
                return;
            case MY_PERMISSIONS_REQUEST_STORAGE: //can also feed from if user exited after first permission
                if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){

                    //all permissions granted, enable functionality

                    mSeekBar.setOnSeekBarChangeListener((mOnSeekBarChangeListener));
                    mSunBrightness.setOnClickListener(mOnSunBrightnessClickListnener);
                    mMoonBrightness.setOnClickListener(mOnMoonBrightnessClickListener);

                    mTextureView = findViewById(R.id.textureView);
                    mTextureView.setSurfaceTextureListener(mTextureViewSurfaceListener);

                    mCameraButton.setOnClickListener(mOnCameraClickListener);
                    mRecordTypeButton.setOnClickListener(mOnRecordTypeClickListener);
                    mSwitchCameraButton.setOnClickListener(mOnSwitchCameraClickListener);

                }else{
                    Toast toast = Toast.makeText(getApplicationContext(), "This application cannot function without storage permissions", Toast.LENGTH_SHORT);
                    toast.show();
                }
                return;
        }

    }

}

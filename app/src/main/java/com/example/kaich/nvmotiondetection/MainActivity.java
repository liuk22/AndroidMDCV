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
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.provider.MediaStore;
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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;

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
                takeVideo(isRecordingVideo);
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
    private static final String[] MY_PERMISSIONS = {Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO};
    private static final int MY_PERMISSIONS_CONSTANT = 1; //this is arbitrary

    //camera
    private int MY_CAMERA_INDEX = 0; //0 is front facing
    private boolean MY_RECORDING_TYPE = true; //true for photo, false for video
    private boolean isRecordingVideo = false;
    private CameraDevice cameraDevice;
    private CaptureRequest.Builder captureRequestBuilder;
    private CameraCaptureSession cameraCaptureSession;
    private CameraDevice.StateCallback stateCallBack = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            cameraDevice = camera;
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
    private MediaRecorder mediaRecorder;
    private String nextVideoAbsolutePath;
    private ImageReader imageReader;
    private Size imageDimension;
    private Size videoDimension;
    private Size previewDimension;
    private Integer sensorOrientation;


    //vision
    private VisionProcess visionProcess = new VisionProcess();

    //file storage
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
        requestPermissions();
    }

    @Override
    protected void onResume(){
        super.onResume();
        Log.e(null, "onResume() called");
        startBackgroundThread();
        if(mTextureView.isAvailable()){
            updateCamera(MY_CAMERA_INDEX);
        }else{
            mTextureView.setSurfaceTextureListener(mTextureViewSurfaceListener);
        }
    }

    @Override
    protected void onPause(){
        Log.e(null, "onPause() called");
        stopBackgroundThread();
        super.onPause();
    }

    private void openCameraAndPreview(){
        try {
            SurfaceTexture surfaceTexture = mTextureView.getSurfaceTexture();
            surfaceTexture.setDefaultBufferSize(imageDimension.getWidth(), imageDimension.getHeight());
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
        }catch(CameraAccessException e){
            Log.e(null,"openCameraAndPreview() failed", e);
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
        }catch(CameraAccessException e){
            Log.e(null, "updatePreview() failed", e);
        }
    }

    private void closePreview(){
        if(cameraCaptureSession != null){
            cameraCaptureSession.close();
            cameraCaptureSession = null;
        }
    }

    private void updateCamera(int cameraIndex){ //cameraIndex = 0 is main camera, 1 should be rear facing
        MY_CAMERA_INDEX = cameraIndex;
        CameraManager cameraManager = (CameraManager)getSystemService(Context.CAMERA_SERVICE);
        try{
            String cameraId = cameraManager.getCameraIdList()[cameraIndex];
            CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

            imageDimension = map.getOutputSizes(SurfaceTexture.class)[0];
            sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);

            //TODO: SET PREVIEWSIZE here
            for(Size size: map.getOutputSizes(MediaRecorder.class)){
                if(size.getWidth() == size.getHeight() * 4 / 3 && size.getWidth() <= 1080){ //3 by 4 aspect ratio, 1080p is limit for MediaRecorder
                    //TODO: CHECK COMPATIBILITY WITH CURRENT LAYOUTS/PREVIEW
                    videoDimension = size;
                    break;
                }
            }

            cameraManager.openCamera(cameraId, stateCallBack, null); //permissions have already been previously checked in onCreate()

        }catch(CameraAccessException e){
            Log.e(null, "updateCamera(int cameraIndex) failed", e);
        }
    }

    private void closeCamera(){

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
            int width = visionProcess.getXRes();
            int height = visionProcess.getYRes();
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

            final File file = new File(Environment.getExternalStorageDirectory() + "/" + UUID.randomUUID().toString() + ".jpg");
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
                    }catch(FileNotFoundException e){
                        Log.e(null, "onImageAvailable(ImageReader imageReader) failed", e);
                    }catch(IOException e){
                        Log.e(null, "onImageReader(ImageReader imageReader) failed", e);
                    }finally{
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

            cameraDevice.createCaptureSession(outputSurface, new CameraCaptureSession.StateCallback() { //TODO: TAKEPICTURE() BREAKS HERE
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    try{
                        session.capture(captureRequestBuilder.build(), captureCallbackListener, backgroundHandler);
                    }catch(CameraAccessException e){
                        Log.e(null,"onConfigured(@NonNull CameraCaptureSession session) failed", e);
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                    Log.e(null, "onConfigureFailed(@NonNull CameraCaptureSession session) triggered");
                }
            }, backgroundHandler);

        }catch(CameraAccessException e){
            Log.e(null, "takePicture() failed", e);
        }
    }

    private void takeVideo(boolean isRecordingVideo){
        if(isRecordingVideo){

        }else{
            if(cameraDevice == null){
                return;
            }
            try{
                closePreview();
                initMediaRecorder();
                SurfaceTexture surfaceTexture = mTextureView.getSurfaceTexture();
                //choose a suitable size where params are at least as large as desired, but smallest out of them
                List<Size> desiredSizes =
                for(Size )

                surfaceTexture.setDefaultBufferSize();
            }catch(CameraAccessException | IOException e){
                Log.e(null, "takeVideo(false) failed", e);
            }
        }
    }

    private void initMediaRecorder() throws IOException{

        //getActivity?

        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        if(nextVideoAbsolutePath == null || nextVideoAbsolutePath.isEmpty()){
            nextVideoAbsolutePath = getVideoFilePath(this)
        }
        mediaRecorder.setOutputFile(nextVideoAbsolutePath);
        mediaRecorder.setVideoEncodingBitRate(1000000); //why this number?
        mediaRecorder.setVideoFrameRate(visionProcess.getFPS());
        mediaRecorder.setVideoSize(videoDimension.getWidth(), videoDimension.getHeight());
        mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264); //TODO: WHAT IS THIS?
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        int rotation = getWindowManager().getDefaultDisplay().getRotation(); //get rotation?

        //TODO: ADD SENSORORIENTATION SWITCH STATEMENT?

        mediaRecorder.prepare();

    }

    protected void startBackgroundThread(){
        backgroundThread = new HandlerThread("Camera Background");
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
    }

    protected void stopBackgroundThread(){
        backgroundThread.quitSafely();
        try{
            backgroundThread.join();
            backgroundThread = null;
            backgroundHandler = null;
        }catch(InterruptedException e){
            Log.e(null, "stopBackgroundThread() failed", e);
        }
    }

    private String getVideoFilePath(Context context){
        final File dir = context.getExternalFilesDir(null);
        return (dir == null ? "" : (dir.getAbsolutePath() + "/")) + System.currentTimeMillis() + ".mp4";
    }

    private void requestPermissions(){
        ArrayList<String> neededPermissions = new ArrayList<String>();
        for(String permission: MY_PERMISSIONS){
            if(ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED){
                neededPermissions.add(permission);
            }
        }

        if(neededPermissions.size() == 0){

            mSeekBar.setOnSeekBarChangeListener(mOnSeekBarChangeListener);
            mMoonBrightness.setOnClickListener(mOnMoonBrightnessClickListener);
            mSunBrightness.setOnClickListener(mOnSunBrightnessClickListnener);

            mTextureView.setSurfaceTextureListener(mTextureViewSurfaceListener);

            mCameraButton.setOnClickListener(mOnCameraClickListener);
            mRecordTypeButton.setOnClickListener(mOnRecordTypeClickListener);
            mSwitchCameraButton.setOnClickListener(mOnSwitchCameraClickListener);
        }else {
            String[] neededPermissionsArray = neededPermissions.toArray(new String[0]);
            ActivityCompat.requestPermissions(this, neededPermissionsArray, MY_PERMISSIONS_CONSTANT);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        boolean cleared = true;
        for(int i = 0; i < grantResults.length; i++) {
            if (grantResults[i] != PERMISSION_GRANTED) {
                cleared = false;
                break;
            }
        }
        if(cleared){ //set listeners once permissions granted
            mSeekBar.setOnSeekBarChangeListener(mOnSeekBarChangeListener);
            mMoonBrightness.setOnClickListener(mOnMoonBrightnessClickListener);
            mSunBrightness.setOnClickListener(mOnSunBrightnessClickListnener);

            mTextureView.setSurfaceTextureListener(mTextureViewSurfaceListener);

            mCameraButton.setOnClickListener(mOnCameraClickListener);
            mRecordTypeButton.setOnClickListener(mOnRecordTypeClickListener);
            mSwitchCameraButton.setOnClickListener(mOnSwitchCameraClickListener);
        }else{
            Toast toast = Toast.makeText(this,"Permissions not granted, app needs all permissions in order to run", Toast.LENGTH_SHORT);
            toast.show();
            toast.cancel();
        }

    }

}

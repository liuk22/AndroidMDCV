package com.example.kaich.nvmotiondetection;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.CharArrayBuffer;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.MediaController;
import android.widget.Toast;
import android.widget.VideoView;


import java.io.File;
import java.util.ArrayList;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;

public class MainActivity extends AppCompatActivity {

    //UI
    private VideoView mVideoView;
    private MediaController mediaController;
    private BottomNavigationView mBottomNavigationView;
    private LinearLayout mLinearLayoutNavigation;
    private Button mChooseVideoBtn;
    private Button mAnalyzeVideoBtn;


    //permissions
    private static final String[] MY_PERMISSIONS = {Manifest.permission.WRITE_EXTERNAL_STORAGE};
    private static final int MY_PERMISSIONS_CONSTANT = 1; //this is arbitrary

    //vision, graphics tools
    private DisplayMetrics metrics = new DisplayMetrics();
    private PhotoTools photoTools;
    private VisionProcess visionProcess;

    //file storage
    private Uri currentVideoURI;
    private String currentVideoPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        photoTools = new PhotoTools(metrics);
        System.loadLibrary("opencv_java3"); //do NOT use Core.NATIVE_LIBRARY_NAME, OpenCV is already treated as local in jni
        visionProcess = new VisionProcess();

        //wait until permissions granted, then enable functionality
        requestPermissions();
    }


    private void requestPermissions(){
        ArrayList<String> neededPermissions = new ArrayList<String>();
        for(String permission: MY_PERMISSIONS){
            if(ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED){
                neededPermissions.add(permission);
            }
        }

        if(neededPermissions.size() == 0){//initialize functionality, all permissions already granted
            initializeFunctionality();

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
        if(cleared){//initialize functionality, all permissions have been granted
            initializeFunctionality();

        }else{
            Toast toast = Toast.makeText(this,"Permissions not granted, app needs all permissions in order to run", Toast.LENGTH_SHORT);
            toast.show();
            toast.cancel();
        }

    }

    private void initializeFunctionality(){
        mVideoView = findViewById(R.id.videoView);
        mediaController = new MediaController(this);
        mVideoView.setMediaController(mediaController);
        mediaController.setAnchorView(mVideoView);

        mBottomNavigationView = findViewById(R.id.bottomNavigationView);
        mLinearLayoutNavigation = findViewById(R.id.linearLayoutNavigation);
        mChooseVideoBtn = findViewById(R.id.chooseVideoBtn);
        mChooseVideoBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(intent, photoTools.RESULT_LOAD_VIDEO);
            }
        });

        mAnalyzeVideoBtn = findViewById(R.id.analyzeVideoBtn);
        mAnalyzeVideoBtn.setEnabled(false);
        mAnalyzeVideoBtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                if(currentVideoURI != null && !(currentVideoURI.getPath().contains(PhotoTools.ANALZYED_VIDEO_FILE_TAG))){
                    //TASK handle this functionality

                    int count1 = 0; //cut out the .mp4 extension
                    for(int i = 0; i < currentVideoPath.length(); i++){
                        if(currentVideoPath.charAt(i) == '/'){
                            count1 = i;
                        }
                    }
                    int count2 = currentVideoPath.length() - 1;
                    while(currentVideoPath.charAt(count2) != '/') {
                        count2 -= 1;
                    }
                    String writePath = currentVideoPath.substring(0, count2);
                    String fileName = currentVideoPath.substring(count1, currentVideoPath.length() - 4);
                    File file = null;
                    try{
                        file = new File(writePath + fileName
                                + PhotoTools.ANALZYED_VIDEO_FILE_TAG + PhotoTools.FILE_EXTENSION);
                        file.mkdirs();
                    } catch (Exception e){
                        Log.e("tttttt", e.getMessage());
                    }
                    visionProcess.analyzeAndWrite(currentVideoPath, file.getAbsolutePath());

                    mVideoView.setVideoURI(Uri.fromFile(file));

                    Log.e("tttttt", file.getAbsolutePath());

                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == photoTools.RESULT_LOAD_VIDEO && resultCode == RESULT_OK  && !(data.equals(null))){
            currentVideoURI = data.getData();

            String[] filePathColumn = {MediaStore.Video.Media.DATA};
            Cursor cursor = getContentResolver().query(currentVideoURI, filePathColumn, null, null, null);
            cursor.moveToFirst();
            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            currentVideoPath = cursor.getString(columnIndex); //TASK what is the format of currentVideoPath String?
            cursor.close();

            mVideoView.setVideoURI(currentVideoURI);

            mVideoView.start();
            mAnalyzeVideoBtn.setEnabled(true);

        }
    }
}

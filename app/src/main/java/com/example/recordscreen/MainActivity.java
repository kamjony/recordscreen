package com.example.recordscreen;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity{

  // "GET ME SOME"

    private static final String TAG = "MainActivity";
    private static final int REQUEST_CODE = 1000;
    private static final int REQUEST_PERMISSION_KEY = 1;
    private static final int DISPLAY_WIDTH = 720;
    private static final int DISPLAY_HEIGHT = 1280;
    FloatingActionButton fab;
    private int screenDensity;
    private MediaProjectionManager mediaProjectionManager;
    private MediaProjection mediaProjection;
    private VirtualDisplay virtualDisplay;
    private MediaProjection.Callback mediaProjectionCallback;
    private MediaRecorder mediaRecorder;
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    private boolean isRecording = false;
    private CustomAdapter customAdapter;
    private ArrayList<Video> videos = null;
    private static final int CODE_DRAW_OVER_OTHER_APP_PERMISSION = 2084;

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        String [] PERMISSIONS = {
                Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO
        };
        if (!hasPermissions(this, PERMISSIONS)) {
            ActivityCompat.requestPermissions(this, PERMISSIONS, REQUEST_PERMISSION_KEY);
        }

        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        screenDensity = displayMetrics.densityDpi;

        mediaProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);

        final ListView listView = (ListView) findViewById(R.id.listView);
        customAdapter = new CustomAdapter(MainActivity.this, getData());
        listView.setAdapter(customAdapter);

        fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
               toggleScreenShare();

            }
        });
    }

    public static boolean hasPermissions(Context context, String... permissions) {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    private void minimizeApp(){
        Toast.makeText(MainActivity.this,"Screen Recording in progress", Toast.LENGTH_LONG).show();
        Intent startMain = new Intent(Intent.ACTION_MAIN);
        startMain.addCategory(Intent.CATEGORY_HOME);
        startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(startMain);
    }


    private ArrayList<Video> getData(){
        videos = new ArrayList<>();
        File folder = new File(Environment.getExternalStorageDirectory(), "ScreenCapture");
        Video v;
        if (folder.exists()){
            File[] files = folder.listFiles();

            for (int i=0; i<files.length; i++){
                File file=files[i];

                v=new Video();
                v.setName(file.getName());
                v.setUri(Uri.fromFile(file));
                videos.add(v);
            }
        }

        return videos;
    }

    public void recordBtnReload() {
        if (isRecording) {
            fab.setImageResource(R.drawable.stop);
        } else {
            fab.setImageResource(R.drawable.play);
        }
    }

    public void toggleScreenShare() {
        if (!isRecording) {
            Toast.makeText(MainActivity.this,"Recording starting in 3secs.",Toast.LENGTH_LONG).show();
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    // TODO Auto-generated method stub
                    initRecorder();
                    shareScreen();
                }
            }, 3000);

        } else {
            Toast.makeText(MainActivity.this,"Recording stopped",Toast.LENGTH_SHORT).show();
            mediaRecorder.stop();
            mediaRecorder.reset();
            mediaRecorder.release();
            mediaRecorder = null;
            stopScreenSharing();

        }
    }

    public void stopScreenSharing() {
        if (virtualDisplay == null){
            return;
        }
        virtualDisplay.release();
        destroyMediaProjection();
        isRecording = false;
        recordBtnReload();
        customAdapter.updateVideosList(getData());

    }

    public void destroyMediaProjection() {
        if (mediaProjection != null){
            mediaProjection.unregisterCallback(mediaProjectionCallback);
            mediaProjection.stop();
            mediaProjection = null;
        }
        Log.i(TAG, "MediaProjection is stopped for now");
    }

    public void shareScreen() {
        if (mediaProjection == null) {
            startActivityForResult(mediaProjectionManager.createScreenCaptureIntent(), REQUEST_CODE);
            return;
        }
        virtualDisplay = createVirtualDisplay();
        mediaRecorder.start();
        isRecording = true;
        recordBtnReload();
        Log.i(TAG, "ShareScreen started for now");
    }

    public void initRecorder() {
        try{
            mediaRecorder = new MediaRecorder();
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);

            mediaRecorder.setVideoSize(DISPLAY_WIDTH, DISPLAY_HEIGHT);
            mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            mediaRecorder.setVideoEncodingBitRate(512 * 1000);
            mediaRecorder.setVideoFrameRate(16);
            mediaRecorder.setVideoEncodingBitRate(3000000);

            File folder = new File(Environment.getExternalStorageDirectory(), "ScreenCapture");
            boolean success = true;
            if (!folder.exists()){
                success = folder.mkdir();
            }
            String filepath;
            if (success) {
                String videoName = ("Capture_" + System.currentTimeMillis() + ".mp4");
                filepath = folder + File.separator + videoName;
                Log.i(TAG, "File created");
            } else {
                Toast.makeText(this, "Failed to create Recordings directory", Toast.LENGTH_SHORT).show();
                return;
            }

            mediaRecorder.setOutputFile(filepath);

            int rotation = getWindowManager().getDefaultDisplay().getRotation();
            int orientation = ORIENTATIONS.get(rotation + 90);
            mediaRecorder.setOrientationHint(orientation);
            mediaRecorder.prepare();
            Log.i(TAG, "initRecorder");
        } catch (IOException e){
            e.printStackTrace();
        }
    }

    public VirtualDisplay createVirtualDisplay() {
        Log.i(TAG, "Created Virtual Display");
        return mediaProjection.createVirtualDisplay(TAG, DISPLAY_WIDTH, DISPLAY_HEIGHT, screenDensity,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, mediaRecorder.getSurface(), null, null);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode != REQUEST_CODE){
            Log.e(TAG, "Unknown request code:" + requestCode);
            return;
        }
        if (resultCode != RESULT_OK){
            Toast.makeText(this, "Permission denied", Toast.LENGTH_LONG).show();
            isRecording = false;
            recordBtnReload();
            return;
        }
        mediaProjectionCallback = new MediaProjection.Callback() {
            @Override
            public void onStop() {
                if (isRecording) {
                    isRecording = false;
                    recordBtnReload();
                    mediaRecorder.stop();
                    mediaRecorder.reset();
                    mediaRecorder.release();
                    mediaRecorder = null;
                }
                mediaProjection = null;
                stopScreenSharing();
            }
        };

        mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data);
        mediaProjection.registerCallback(mediaProjectionCallback, null);
        virtualDisplay = createVirtualDisplay();
        mediaRecorder.start();
        isRecording = true;
        recordBtnReload();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode){
            case REQUEST_PERMISSION_KEY:
                if ((grantResults.length > 0) && (grantResults[0] + grantResults[1]) == PackageManager.PERMISSION_GRANTED) {
                    toggleScreenShare();
                } else {
                    isRecording = false;
                    recordBtnReload();
                    Snackbar.make(findViewById(android.R.id.content),
                            "Please Enable Mic and Storage Permissions", Snackbar.LENGTH_INDEFINITE).setAction("ENABLE", new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent intent = new Intent();
                            intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                            intent.addCategory(Intent.CATEGORY_DEFAULT);
                            intent.setData(Uri.parse("package:" + getPackageName()));
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                            intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                            startActivity(intent);
                        }
                    }).show();

                }
                return;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        destroyMediaProjection();
    }

    @Override
    public void onBackPressed() {
        if (isRecording) {
            Snackbar.make(findViewById(android.R.id.content), "Wanna Stop recording and exit?",
                    Snackbar.LENGTH_INDEFINITE).setAction("Stop",
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            mediaRecorder.stop();
                            mediaRecorder.reset();
                            Log.v(TAG, "Stopping Recording");
                            stopScreenSharing();
                            finish();
                        }
                    }).show();
        } else {
            finish();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mediaRecorder != null) {
            mediaRecorder.release();
            mediaRecorder = null;
        }
    }

}

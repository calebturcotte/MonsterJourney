package com.application.monsterjourney;

import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.view.View;
import android.webkit.URLUtil;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class VideoPlayer extends AppCompatActivity {
    /**
     * Our video player class
     */
    private static final String VIDEO_SAMPLE = "mj";
    boolean isplaying;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.video_view);
        SharedPreferences settings = getSharedPreferences(MainActivity.PREFS_NAME, 0);
        isplaying = settings.getBoolean("isplaying",isplaying);
        VideoView mVideoView = findViewById(R.id.videoview);
        Uri videoUri = getMedia(VIDEO_SAMPLE);
        mVideoView.setVideoURI(videoUri);

        mVideoView.setOnCompletionListener(mediaPlayer -> {
            //confirmWindow.dismiss();
//            if(!isplaying){
//                music.start();
//            }
            finish();
        });
        mVideoView.setOnPreparedListener(mediaPlayer -> {
            if(isplaying){
                mediaPlayer.setVolume(0f,0f);
            }
            mVideoView.start();
        });
//        mVideoView.setOnClickListener((view) -> {
////            confirmWindow.dismiss();
////            if(!isplaying){
////                music.start();
////            }
//            finish();
//        });
        mVideoView.requestFocus();
    }

    /**
     * fetch the Uri of the media
     * @param mediaName name of the media
     * @return Uri of the media
     */
    private Uri getMedia(String mediaName) {
        if (URLUtil.isValidUrl(mediaName)) {
            // media name is an external URL
            return Uri.parse(mediaName);
        } else { // media name is a raw resource embedded in the app
            return Uri.parse("android.resource://" + getPackageName() +
                    "/raw/" + mediaName);
        }
    }

    /**
     * close view
     * @param v videoview clicked
     */
    public void close(View v){
        finish();
    }
}

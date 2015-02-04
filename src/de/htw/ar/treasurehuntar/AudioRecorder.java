package de.htw.ar.treasurehuntar;

import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.media.MediaRecorder;
import android.media.MediaPlayer;

import java.io.IOException;

/**
 * Created by Eric Haller on 02.02.2015.
 */

public class AudioRecorder extends Activity
{
    private static final String LOG_TAG = "AudioRecorder";
    private static String mFileName = null;

    private MediaRecorder mRecorder = null;

    private MediaPlayer   mPlayer = null;

    private boolean isRecording = false;
    private boolean isPlaying = false;

    protected void record(boolean start) {
        if (start) {
            if (!isRecording) {
                startRecording();
                isRecording = true;
            }

        } else {
            if (isRecording) {
                stopRecording();
                isRecording = false;
            }
        }
    }

    protected void play(boolean start) {
        if (start) {
            if (!isPlaying) {
                startPlaying();
                isPlaying = true;
            }
        } else {
            if (isPlaying) {
                stopPlaying();
                isPlaying = false;
            }
        }
    }

    private void startPlaying() {
        mPlayer = new MediaPlayer();
        try {
            mPlayer.setDataSource(mFileName);
            mPlayer.prepare();
            mPlayer.start();
        } catch (IOException e) {
            Log.e(LOG_TAG, "prepare() failed");
        }
    }

    private void stopPlaying() {
        mPlayer.release();
        mPlayer = null;
    }

    private void startRecording() {
        mRecorder = new MediaRecorder();
        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mRecorder.setOutputFile(mFileName);
        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

        try {
            mRecorder.prepare();
        } catch (IOException e) {
            Log.e(LOG_TAG, "prepare() failed");
        }

        mRecorder.start();
    }

    private void stopRecording() {
        mRecorder.stop();
        mRecorder.release();
        mRecorder = null;
    }

    protected String getPath() {
        return mFileName;
    }

    public AudioRecorder() {
        mFileName = Environment.getExternalStorageDirectory().getAbsolutePath();
        mFileName += "/AudioRecorder.3gp";
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mRecorder != null) {
            mRecorder.release();
            mRecorder = null;
        }

        if (mPlayer != null) {
            mPlayer.release();
            mPlayer = null;
        }
    }
}
package de.htw.ar.treasurehuntar;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.media.MediaRecorder;
import android.media.MediaPlayer;
import android.view.MotionEvent;
import android.widget.LinearLayout;
import android.widget.Toast;
import com.google.android.glass.touchpad.Gesture;
import com.google.android.glass.touchpad.GestureDetector;

import java.io.IOException;

/**
 * Created by Eric Haller on 02.02.2015.
 */

public class AudioRecorder extends Activity {
    private GestureDetector mGestureDetector;
    private static final String LOG_TAG = "AudioRecorder";
    private static String mFileName = null;

    private MediaRecorder mRecorder = null;

    private MediaPlayer mPlayer = null;

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

    /**
     * Send generic motion events to the gesture detector
     *
     * @param event
     * @return
     */
    @Override
    public boolean onGenericMotionEvent(MotionEvent event) {
        return mGestureDetector != null && mGestureDetector.onMotionEvent(event);
    }

    private GestureDetector createGestureDetector(Context context) {
        GestureDetector gestureDetector = new GestureDetector(context);
        //Create a base listener for generic gestures
        gestureDetector.setBaseListener(new GestureDetector.BaseListener() {
            @Override
            public boolean onGesture(Gesture gesture) {
                if (gesture == Gesture.TAP) {
                    Log.i("gesture", "Tap");
                    startRecording();
                    return true;
                } else if (gesture == Gesture.SWIPE_RIGHT) {
                    stopRecording();
                    return true;
                } else if (gesture == Gesture.SWIPE_LEFT) {
                    startPlaying();
                    return true;
                }
                return false;
            }
        });

        return gestureDetector;
    }

    private void startPlaying() {
        Log.i("recorder", "start playing");
        mPlayer = new MediaPlayer();
        mPlayer.setOnCompletionListener(new MediaPlayer.
                OnCompletionListener() {
            public void onCompletion(MediaPlayer mp) {
                mPlayer.release();
                mPlayer = null;
            }
        });
        try {
            mPlayer.setDataSource(mFileName);
            mPlayer.prepare();
            mPlayer.start();
        } catch (IOException e) {
            Log.e("start", "prepare() failed");
        }
    }

    private void stopPlaying() {
        mPlayer.release();
        mPlayer = null;
    }

    private void startRecording() {
        Log.i("recorder", "start recording");
        mRecorder = new MediaRecorder();
        mRecorder.setMaxDuration(5000);
        // the settings below are important for the capture
        // and playback to work in Glass
        mRecorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.DEFAULT);
        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        mRecorder.setOutputFile(mFileName);
        try {
            mRecorder.prepare();
        } catch (IOException e) {
            Log.e("Recorder", "prepare() failed");
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

    @Override
    public void onCreate(Bundle icicle) {
        mFileName = Environment.getExternalStorageDirectory().getAbsolutePath();
        mFileName += "/AudioRecorder.3gp";
        mGestureDetector = createGestureDetector(this);

        LinearLayout ll = new LinearLayout(this);
        //ll.addView(mRecordButton,
        //        new LinearLayout.LayoutParams(
        //                ViewGroup.LayoutParams.WRAP_CONTENT,
        //                ViewGroup.LayoutParams.WRAP_CONTENT,
        //                0));
        setContentView(ll);
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

    private void finishWithResult()
    {
        Bundle conData = new Bundle();
        conData.putString("audioPath", mFileName);
        Intent intent = new Intent();
        intent.putExtras(conData);
        setResult(RESULT_OK, intent);
        finish();
    }
}
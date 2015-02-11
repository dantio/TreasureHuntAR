package de.htw.ar.treasurehuntar;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
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
    private static String mImagePath = null;

    private TextView mTimer;

    private MediaRecorder mRecorder = null;

    private MediaPlayer mPlayer = null;

    private boolean isRecording = false;
    private boolean isPlaying = false;

    private boolean isRecorded = false;
    private boolean isFinished = false;

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
                    if (!isRecording && !isFinished && !isRecorded) {
                        startRecording();
                    } else if (!isPlaying && isRecorded) {
                        startPlaying();
                        isFinished = true;
                    } else if (isFinished) {
                        finishWithResult();
                    }
                    return true;
                } else if (gesture == Gesture.SWIPE_DOWN) {
                    if (isRecording) {
                        stopRecording();
                    } else if (isPlaying) {
                        stopPlaying();
                    } else if(isFinished) {
                        finish();
                    }
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
            isPlaying = true;
        } catch (IOException e) {
            Log.e("start", "prepare() failed");
        }
    }

    private void stopPlaying() {
        mPlayer.release();
        mPlayer = null;
        isPlaying = false;
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

        new CountDownTimer(6000, 1000) {

            public void onTick(long millisUntilFinished) {
                mTimer.setText("noch " + millisUntilFinished / 1000 + " sek");
            }

            public void onFinish() {
                Toast.makeText(AudioRecorder.this, R.string.record_finish, Toast.LENGTH_SHORT).show();
                mTimer.setText("[TAP] um die Botschaft abzuspielen");
                stopRecording();
            }
        }.start();
    }

    private void stopRecording() {
        mRecorder.stop();
        mRecorder.release();
        mRecorder = null;
        isRecorded = true;
    }

    @Override
    public void onCreate(Bundle icicle) {
        mFileName = Environment.getExternalStorageDirectory().getAbsolutePath();
        mGestureDetector = createGestureDetector(this);

        Intent intent = getIntent();
        mImagePath = intent.getStringExtra("imgPath");
        String fileName = mImagePath.substring(mImagePath.lastIndexOf("/"), mImagePath.lastIndexOf("."));
        mFileName += fileName;

        Log.i("record", mImagePath);
        Log.i("record", mFileName);

        Toast.makeText(AudioRecorder.this, R.string.record_hint, Toast.LENGTH_SHORT).show();

        mTimer = new TextView(this);
        mTimer.setText("[TAP] um die Aufnahme zu starten");

        ImageView imageView = new ImageView(this);
        imageView.setBackgroundResource(R.drawable.record);

        RelativeLayout rl = new RelativeLayout(this);
        rl.setBackground(Drawable.createFromPath(mImagePath));

        rl.addView(imageView);

        // Add Timer
        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 300, 0, 0);
        mTimer.setLayoutParams(lp);
        rl.addView(mTimer);

        setContentView(rl);
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

    private void finishWithResult() {
        Bundle conData = new Bundle();
        conData.putString("audioPath", mFileName);
        conData.putString("imgPath", mImagePath);

        Intent intent = new Intent();
        intent.putExtras(conData);
        setResult(RESULT_OK, intent);
        finish();
    }
}
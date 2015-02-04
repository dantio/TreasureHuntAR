package de.htw.ar.treasurehuntar;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.SensorManager;
import android.location.LocationListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.Toast;
import com.google.android.glass.content.Intents;
import com.google.android.glass.touchpad.Gesture;
import com.google.android.glass.touchpad.GestureDetector;
import com.wikitude.architect.ArchitectView.ArchitectUrlListener;
import com.wikitude.architect.ArchitectView.SensorAccuracyChangeListener;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Schätze verstecken
 */
public class CachingActivity extends AbstractArchitectActivity {

    // take picture logic
    private static final int TAKE_PICTURE_REQUEST = 1;
    private static final int TAKE_AUDIO_REQUEST = 2;

    private final static String POST_IMAGE_URL = "http://vegapunk.de:9999/cache64";
    private final static String POST_AUDIO_URL = "http://vegapunk.de:9999/audio";

    /**
     * extras key for activity title, usually static and set in Manifest.xml
     */
    protected static final String EXTRAS_KEY_ACTIVITY_TITLE_STRING = "activityTitle";

    /**
     * extras key for architect-url to load, usually already known upfront, can be relative folder to assets (myWorld.html --> assets/myWorld.html is loaded) or web-url ("http://myserver.com/myWorld.html"). Note that argument passing is only possible via web-url
     */
    protected static final String EXTRAS_KEY_ACTIVITY_ARCHITECT_WORLD_URL = "activityArchitectWorldUrl";

    /**
     * last time the calibration toast was shown, this avoids too many toast shown when compass needs calibration
     */
    private long lastCalibrationToastShownTimeMillis = System
            .currentTimeMillis();

    private GestureDetector mGestureDetector;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        mGestureDetector = createGestureDetector(this);

        super.onCreate(savedInstanceState);
    }

    @Override
    public String getARchitectWorldPath() {
        return getIntent().getExtras().getString(
                EXTRAS_KEY_ACTIVITY_ARCHITECT_WORLD_URL);
    }

    @Override
    public String getActivityTitle() {
        return (getIntent().getExtras() != null && getIntent().getExtras().get(
                EXTRAS_KEY_ACTIVITY_TITLE_STRING) != null) ? getIntent()
                .getExtras().getString(EXTRAS_KEY_ACTIVITY_TITLE_STRING)
                : "Test-World";
    }

    @Override
    public int getContentViewId() {
        return R.layout.main_layout;
    }

    @Override
    public int getArchitectViewId() {
        return R.id.architectView;
    }

    @Override
    public SensorAccuracyChangeListener getSensorAccuracyListener() {
        return new SensorAccuracyChangeListener() {
            @Override
            public void onCompassAccuracyChanged(int accuracy) {
                /* UNRELIABLE = 0, LOW = 1, MEDIUM = 2, HIGH = 3 */
                if (accuracy < SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM
                        && CachingActivity.this != null && !CachingActivity.this
                        .isFinishing() && System.currentTimeMillis()
                        - CachingActivity.this.lastCalibrationToastShownTimeMillis
                        > 5 * 1000) {
                    Toast.makeText(CachingActivity.this,
                            R.string.compass_accuracy_low, Toast.LENGTH_LONG)
                            .show();
                    CachingActivity.this.lastCalibrationToastShownTimeMillis = System
                            .currentTimeMillis();
                }
            }
        };
    }

    @Override
    public ArchitectUrlListener getUrlListener() {
        return new ArchitectUrlListener() {

            @Override
            public boolean urlWasInvoked(String uriString) {
                // by default: no action applied when url was invoked
                return false;
            }
        };
    }

    @Override
    public LocationProvider getLocationProvider(
            final LocationListener locationListener) {
        return new LocationProvider(this, locationListener);
    }

    @Override
    public float getInitialCullingDistanceMeters() {
        // you need to adjust this in case your POIs are more than 50km away from user here while loading or in JS code (compare 'AR.context.scene.cullingDistance')
        return 50;
    }

    private GestureDetector createGestureDetector(Context context) {
        GestureDetector gestureDetector = new GestureDetector(context);
        //Create a base listener for generic gestures
        gestureDetector.setBaseListener(new GestureDetector.BaseListener() {
            @Override
            public boolean onGesture(Gesture gesture) {
                if (gesture == Gesture.TAP) {
                    Log.i("gesture", "Tap");
                    takePicture();
                    return true;
                } else if (gesture == Gesture.SWIPE_RIGHT) {
                    startRecording();
                    return true;
                }
                return false;
            }
        });

        return gestureDetector;
    }

    private void startRecording() {
        Intent intent = new Intent(this, AudioRecorder.class);
        startActivityForResult(intent, TAKE_AUDIO_REQUEST);
    }

    private void takePicture() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(intent, TAKE_PICTURE_REQUEST);
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


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case TAKE_PICTURE_REQUEST:
                    String thumbnailPath = data.getStringExtra(Intents.EXTRA_THUMBNAIL_FILE_PATH);
                    new SendCache(thumbnailPath).execute(POST_IMAGE_URL);
                    break;

                case TAKE_AUDIO_REQUEST:
                    String videoPath = data.getStringExtra(Intents.EXTRA_VIDEO_FILE_PATH);
                    new SendAudioCache(videoPath).execute(POST_AUDIO_URL);
                    break;
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }


   

    /**
     * Send file to server
     */
    class SendFileCache extends AsyncTask<String, Void, Void> {

        private String imagePath;
        private String audioPath;

        HttpURLConnection mUrlConnection;

        String mResult;

        @Override
        protected Void doInBackground(String... urls) {
            imagePath = urls[0];
            audioPath = urls[1];

            try {
                int serverResponseCode = 0;
                File imageFile = new File(imagePath);
                File audioFile = new File(audioPath);
                DataOutputStream dos = null;
                String lineEnd = "\r\n";
                String twoHyphens = "--";
                String boundary = "*****";
                int bytesRead, bytesAvailable, bufferSize;
                byte[] buffer;
                int maxBufferSize = 1024 * 1024;
                int REQUEST_SUCCESS_CODE = 200;
                FileInputStream fileInputStream = new FileInputStream(imageFile);
                URL url = new URL(POST_AUDIO_URL);
                mUrlConnection = (HttpURLConnection) url.openConnection();
                mUrlConnection.setRequestMethod("POST");
                mUrlConnection.setRequestProperty("description", "Treasure-"); // TODO: Voice Recognition Text einfuegen
                mUrlConnection.setRequestProperty("latitude", String.valueOf(lastKnownLocation.getLatitude()));
                mUrlConnection.setRequestProperty("longitude", String.valueOf(lastKnownLocation.getLongitude()));
                mUrlConnection.setRequestProperty("altitude",String.valueOf(lastKnownLocation.getAltitude()));
                // write ImageData to Request
                mUrlConnection.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);
                mUrlConnection.setRequestProperty("imageData", imageFile.getName());
                dos = new DataOutputStream(mUrlConnection.getOutputStream());
                dos.writeBytes(twoHyphens + boundary + lineEnd);
                dos.writeBytes("Content-Disposition: form-data; name=Filedata;filename=" + imageFile.getName() +
                        lineEnd);
                dos.writeBytes(lineEnd);
                bytesAvailable = fileInputStream.available();
                bufferSize = Math.min(bytesAvailable, maxBufferSize);
                buffer = new byte[bufferSize];
                bytesRead = fileInputStream.read(buffer, 0, bufferSize);
                while (bytesRead > 0) {
                    dos.write(buffer, 0, bufferSize);
                    bytesAvailable = fileInputStream.available();
                    bufferSize = Math.min(bytesAvailable, maxBufferSize);
                    bytesRead = fileInputStream.read(buffer, 0, bufferSize);
                }
                dos.writeBytes(lineEnd);
                dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);
                fileInputStream.close();
                dos.close();

                // write Audio Data to Request
                fileInputStream = new FileInputStream(audioFile);
                mUrlConnection.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);
                mUrlConnection.setRequestProperty("audioData", audioFile.getName());
                dos = new DataOutputStream(mUrlConnection.getOutputStream());
                dos.writeBytes(twoHyphens + boundary + lineEnd);
                dos.writeBytes("Content-Disposition: form-data; name=Filedata;filename=" + audioFile.getName() +
                        lineEnd);
                dos.writeBytes(lineEnd);
                bytesAvailable = fileInputStream.available();
                bufferSize = Math.min(bytesAvailable, maxBufferSize);
                buffer = new byte[bufferSize];
                bytesRead = fileInputStream.read(buffer, 0, bufferSize);
                while (bytesRead > 0) {
                    dos.write(buffer, 0, bufferSize);
                    bytesAvailable = fileInputStream.available();
                    bufferSize = Math.min(bytesAvailable, maxBufferSize);
                    bytesRead = fileInputStream.read(buffer, 0, bufferSize);
                }
                dos.writeBytes(lineEnd);
                dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);
                fileInputStream.close();
                dos.close();

                // Responses from the server (code and message)
                serverResponseCode = mUrlConnection.getResponseCode();
                String serverResponseMessage = mUrlConnection.getResponseMessage();
                if (serverResponseCode == REQUEST_SUCCESS_CODE) {
                    InputStream is = mUrlConnection.getInputStream();
                    int ch;
                    StringBuffer b = new StringBuffer();
                    while ((ch = is.read()) != -1) {
                        b.append((char) ch);
                    }
                    final String uploadedFilename = b.toString();
                    mResult = "uploaded file at http://www.morkout.com/glass/uploads/" + uploadedFilename;
                    is.close();
                }
            } catch (Exception e) {}
            return null;
        }
    }
}


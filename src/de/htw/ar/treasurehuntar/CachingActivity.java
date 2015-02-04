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
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;

import java.io.*;
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
                }
                return false;
            }
        });

        return gestureDetector;
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
                // First take a picure
                case TAKE_PICTURE_REQUEST:
                    String thumbnailPath = data.getStringExtra(Intents.EXTRA_THUMBNAIL_FILE_PATH);
                    Intent intent = new Intent(CachingActivity.this, AudioRecorder.class);
                    intent.putExtra("imgPath", thumbnailPath);
                    startActivityForResult(intent, TAKE_AUDIO_REQUEST);
                    break;

                // Record audio and send to server
                case TAKE_AUDIO_REQUEST:
                    String audioPath = data.getStringExtra("audioPath");
                    String imgPath = data.getStringExtra("imgPath");
                    new SendAudioCache(audioPath).execute(POST_AUDIO_URL);
                    break;
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }


    /**
     * Send picture to server
     */
    class SendCache extends AsyncTask<String, Void, Integer> {

        private final String picturePath;

        SendCache(String picturePath) {
            this.picturePath = picturePath;
        }

        @Override
        protected Integer doInBackground(String... urls) {
            int waitMax = 5;
            File pictureFile = new File(picturePath);
            while (!pictureFile.exists() && waitMax > 0) {
                try {
                    waitMax--;
                    Thread.sleep(5000);
                    pictureFile = new File(picturePath);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            if (!pictureFile.exists()) {
                // Could not write picture
                Log.e("picture", "doesnt exists");
                return null;
            }

            HttpClient httpclient = new DefaultHttpClient();
            HttpPost httppost = new HttpPost(urls[0]);
            try {
                Bitmap fileToSend = BitmapFactory.decodeFile(pictureFile.getPath());
                List<NameValuePair> nameValuePairs = new ArrayList<>();
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                fileToSend.compress(Bitmap.CompressFormat.JPEG, 90, stream);
                byte[] byte_arr = stream.toByteArray();
                String image_str = Base64.encodeToString(byte_arr, Base64.DEFAULT);

                nameValuePairs.add(new BasicNameValuePair("description", "Treasure-"));
                nameValuePairs.add(new BasicNameValuePair("latitude", String.valueOf(lastKnownLocation.getLatitude())));
                nameValuePairs.add(new BasicNameValuePair("longitude", String.valueOf(lastKnownLocation.getLongitude())));
                nameValuePairs.add(new BasicNameValuePair("altitude", String.valueOf(lastKnownLocation.getAltitude())));
                nameValuePairs.add(new BasicNameValuePair("file", image_str));
                httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

                // Execute HTTP Post Request
                HttpResponse response = httpclient.execute(httppost);

                return response.getStatusLine().getStatusCode();
            } catch (ClientProtocolException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Integer statusCode) {
            if (statusCode != HttpStatus.SC_OK) {
                // Bild konnte nicht hochgeladen werden
                return;
            }

            Intent intent = new Intent(CachingActivity.this, AudioRecorder.class);
            intent.putExtra("imgPath", picturePath);
            startActivityForResult(intent, TAKE_AUDIO_REQUEST);
        }
    }

    /**
     * Send audio to server
     */
    class SendAudioCache extends AsyncTask<String, Void, Void> {

        private final String audioPath;

        SendAudioCache(String audioPath) {
            this.audioPath = audioPath;
        }

        @Override
        protected Void doInBackground(String... urls) {
            int waitMax = 5;
            File audioFile = new File(audioPath);
            while (!audioFile.exists() && waitMax > 0) {
                try {
                    waitMax--;
                    Thread.sleep(5000);
                    audioFile = new File(audioPath);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            if (!audioFile.exists()) {
                // Could not write audio
                Log.e("audio", "doesnt exists");
                return null;
            }

            HttpClient httpclient = new DefaultHttpClient();
            HttpPost httppost = new HttpPost(urls[0]);
            try {
                List<NameValuePair> nameValuePairs = new ArrayList<>();
                String base64String = encodeFileToBase64Binary(audioPath);

                nameValuePairs.add(new BasicNameValuePair("file", base64String));
                httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

                // Execute HTTP Post Request
                HttpResponse response = httpclient.execute(httppost);
                //HttpEntity entity = response.getEntity();
                //String data = EntityUtils.toString(entity);
                //return new JSONArray(data);
            } catch (ClientProtocolException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        private String encodeFileToBase64Binary(String fileName)
                throws IOException {

            File file = new File(fileName);
            byte[] bytes = loadFile(file);
            return Base64.encodeToString(bytes, Base64.DEFAULT);
        }

        private byte[] loadFile(File file) throws IOException {
            InputStream is = new FileInputStream(file);

            long length = file.length();
            if (length > Integer.MAX_VALUE) {
                // File is too large
                return null;
            }
            byte[] bytes = new byte[(int) length];
            int offset = 0;
            int numRead = 0;
            while (offset < bytes.length
                    && (numRead = is.read(bytes, offset, bytes.length - offset)) >= 0) {
                offset += numRead;
            }

            if (offset < bytes.length) {
                throw new IOException("Could not completely read file " + file.getName());
            }

            is.close();
            return bytes;
        }
    }
}


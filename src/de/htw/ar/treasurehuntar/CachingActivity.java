package de.htw.ar.treasurehuntar;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.SensorManager;
import android.location.LocationListener;
import android.os.Bundle;
import android.os.FileObserver;
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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Schätze verstecken
 */
public class CachingActivity extends AbstractArchitectActivity {

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
                    // do something on tap
                    Log.i("gesture", "Tap");
                    takePicture();
                    return true;
                }
                return false;
            }
        });

        return gestureDetector;
    }

    /*
 * Send generic motion events to the gesture detector
 */
    @Override
    public boolean onGenericMotionEvent(MotionEvent event) {
        if (mGestureDetector != null) {
            return mGestureDetector.onMotionEvent(event);
        }
        return false;
    }
    // take picture logic
    private static final int TAKE_PICTURE_REQUEST = 1;

    private void takePicture() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(intent, TAKE_PICTURE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == TAKE_PICTURE_REQUEST && resultCode == RESULT_OK) {
            String thumbnailPath = data.getStringExtra(Intents.EXTRA_THUMBNAIL_FILE_PATH);
            String picturePath = data.getStringExtra(Intents.EXTRA_PICTURE_FILE_PATH);

            //processPictureWhenReady(picturePath);
            // TODO: Show the thumbnail to the user while the full picture is being
            // processed.

            sendCache sC = new sendCache();
            //hier noch das bitmap mitschicken und in doInBackground einbinden
            try {
                sC.execute("http://vegapunk.de:9999/cache64").get();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }

        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    private void processPictureWhenReady(final String picturePath) {
        final File pictureFile = new File(picturePath);

        if (pictureFile.exists()) {
            // The picture is ready; process it.
            //http://vegapunk.de:9999/cache
            sendPostRequest(pictureFile);

        } else {
            // The file does not exist yet. Before starting the file observer, you
            // can update your UI to let the user know that the application is
            // waiting for the picture (for example, by displaying the thumbnail
            // image and a progress indicator).

            final File parentDirectory = pictureFile.getParentFile();
            FileObserver observer = new FileObserver(parentDirectory.getPath(),
                    FileObserver.CLOSE_WRITE | FileObserver.MOVED_TO) {
                // Protect against additional pending events after CLOSE_WRITE
                // or MOVED_TO is handled.
                private boolean isFileWritten;

                @Override
                public void onEvent(int event, String path) {
                    if (!isFileWritten) {
                        // For safety, make sure that the file that was created in
                        // the directory is actually the one that we're expecting.
                        File affectedFile = new File(parentDirectory, path);
                        isFileWritten = affectedFile.equals(pictureFile);

                        if (isFileWritten) {
                            stopWatching();

                            // Now that the file is ready, recursively call
                            // processPictureWhenReady again (on the UI thread).
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    processPictureWhenReady(picturePath);
                                }
                            });
                        }
                    }
                }
            };
            observer.startWatching();
        }
    }

    private HttpResponse sendPostRequest(File fileToSend) {
        // Create a new HttpClient and Post Header
        HttpClient httpclient = new DefaultHttpClient();
        HttpPost httppost = new HttpPost("http://vegapunk.de:9999/test");

        try {
            // Add your data
            List<NameValuePair> nameValuePairs = new ArrayList<>(2);
            Calendar c = Calendar.getInstance();
            int date = c.get(Calendar.DATE);

            Bitmap bitmap = BitmapFactory.decodeFile(fileToSend.getPath());
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 90, stream); //compress to which format you want.
            byte [] byte_arr = stream.toByteArray();
            String image_str = Base64.encodeToString(byte_arr, Base64.DEFAULT);

            nameValuePairs.add(new BasicNameValuePair("description", "Treasure-"+date));
            nameValuePairs.add(new BasicNameValuePair("latitude", "30"));
            nameValuePairs.add(new BasicNameValuePair("longitude", "30"));
            nameValuePairs.add(new BasicNameValuePair("altitude", "30"));
            nameValuePairs.add(new BasicNameValuePair("file", image_str));
            httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

            // Execute HTTP Post Request
             HttpResponse response = httpclient.execute(httppost);
            return response;
        } catch (ClientProtocolException e) {
            // TODO Auto-generated catch block
        } catch (IOException e) {
            // TODO Auto-generated catch block
        }
        return null;
    }

    class sendCache extends AsyncTask<String, Void, Void> {
        @Override
        protected Void doInBackground(String... urls) {

            HttpClient httpclient = new DefaultHttpClient();
            HttpPost httppost = new HttpPost(urls[0]);
            try {
                //ambesten hier ein bitmap übergeben einen pfad oder sowas
                Drawable myDrawable = getResources().getDrawable(R.drawable.logo); //achtung das muss geändert werden == R.drawable.logo
                Bitmap myLogo = ((BitmapDrawable) myDrawable).getBitmap();

                List<NameValuePair> nameValuePairs = new ArrayList<>(2);
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                myLogo.compress(Bitmap.CompressFormat.PNG, 90, stream);
                byte [] byte_arr = stream.toByteArray();
                String image_str = Base64.encodeToString(byte_arr, Base64.DEFAULT);

                nameValuePairs.add(new BasicNameValuePair("description", "Treasure-"));
                nameValuePairs.add(new BasicNameValuePair("latitude", "30"));
                nameValuePairs.add(new BasicNameValuePair("longitude", "30"));
                nameValuePairs.add(new BasicNameValuePair("altitude", "30"));
                nameValuePairs.add(new BasicNameValuePair("file", image_str));
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
    }

}

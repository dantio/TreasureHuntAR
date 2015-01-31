package de.htw.ar.treasurehuntar;

import android.content.Context;
import android.content.res.Resources;
import android.hardware.SensorManager;
import android.location.LocationListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.Toast;
import com.google.android.glass.touchpad.Gesture;
import com.google.android.glass.touchpad.GestureDetector;
import com.wikitude.architect.ArchitectView.ArchitectUrlListener;
import com.wikitude.architect.ArchitectView.SensorAccuracyChangeListener;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Random;

/**
 * Schätze finden
 */
public class HuntingActivity extends AbstractArchitectActivity {

    protected JSONArray poiData;
    protected boolean isLoading = false;

    // User go for magnifier
    public static final String ACTION_START_HUNTING_MAGNIFIER = "startHuntingMagnifier";
    public static final String ACTION_STOP_HUNTING_TREASURE = "stopHuntingMagnifier";
    public static final String ACTION_START_HUNTING_TREASURE = "startHuntingTreasure";

    public boolean isHuntingMagnifier = false;
    public boolean isHuntingTreasure = false;
    public int huntingTreasureId = -1;

    /**
     * radius in m
     */
    public static final int MAX_RADIUS = 2000;

    /**
     * max tresures
     */
    public static final int MAX_TRESURES = 5;

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

    private GestureDetector createGestureDetector(Context context) {
        GestureDetector gestureDetector = new GestureDetector(context);
        //Create a base listener for generic gestures
        gestureDetector.setBaseListener(new GestureDetector.BaseListener() {
            @Override
            public boolean onGesture(Gesture gesture) {
                if (gesture == Gesture.TAP) {
                    // do something on tap
                    Log.i("gesture", "Tap");
                    callJavaScript("TreasureHuntAR.startHunting");
                    return true;
                } else if (gesture == Gesture.SWIPE_DOWN) {
                    if (isHuntingMagnifier) {
                        callJavaScript("TreasureHuntAR.stopHuntingMagnifier");
                        return true;
                    } else if (isHuntingTreasure) {
                        return true;
                    }

                    finish();
                    return false;
                } else if (gesture == Gesture.SWIPE_RIGHT) {
                    // do something on right (forward) swipe
                    return true;
                } else if (gesture == Gesture.SWIPE_LEFT) {
                    // do something on left (backwards) swipe
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
        return mGestureDetector != null && mGestureDetector.onMotionEvent(event);
    }

    @Override
    public SensorAccuracyChangeListener getSensorAccuracyListener() {
        return new SensorAccuracyChangeListener() {
            @Override
            public void onCompassAccuracyChanged(int accuracy) {
                /* UNRELIABLE = 0, LOW = 1, MEDIUM = 2, HIGH = 3 */
                if (accuracy < SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM
                        && HuntingActivity.this != null && !HuntingActivity.this
                        .isFinishing() && System.currentTimeMillis()
                        - HuntingActivity.this.lastCalibrationToastShownTimeMillis
                        > 5 * 1000) {
                    Toast.makeText(HuntingActivity.this,
                            R.string.compass_accuracy_low, Toast.LENGTH_LONG)
                            .show();
                    HuntingActivity.this.lastCalibrationToastShownTimeMillis = System
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

                // TODO Uri uri = new Uri("uriString");
                // starts with "architectsdk://"
                String action = uriString.substring("architectsdk://".length());
                if (action.startsWith(ACTION_START_HUNTING_MAGNIFIER)) {
                    isHuntingMagnifier = true;
                } else if (action.startsWith(ACTION_START_HUNTING_TREASURE)) {
                    isHuntingTreasure = true;
                    String idString = action.substring(
                            ACTION_START_HUNTING_TREASURE.length() + "?id="
                                    .length());
                    int treasureId = Integer.parseInt(idString);
                    loadTreasureData(treasureId);
                } else if (action.startsWith(ACTION_STOP_HUNTING_TREASURE)) {
                    isHuntingMagnifier = false;
                }

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
        return MAX_RADIUS;
    }


    @Override
    protected void onPostCreate(final Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        loadData();
    }

    protected void loadData() {
        if (!isLoading) {
            new LoadCaches().execute("http://www.vegapunk.de:9999/caches");
        }
    }

    /**
     * Get target image and sound from server
     *
     * @param treasureId
     */
    private void loadTreasureData(int treasureId) {
        Log.i("load", "treasure sound and image tracking");
    }

    /**
     * loads poiInformation and returns them as JSONArray. Ensure attributeNames of JSON POIs are well known in JavaScript, so you can parse them easily
     *
     * @return POI information in JSONArray
     */
    public JSONArray getPoiInformation(final JSONArray js) {

        final JSONArray pois = new JSONArray();

        // ensure these attributes are also used in JavaScript when extracting POI data
        final String ATTR_ID = "id";
        final String ATTR_NAME = "name";
        final String ATTR_RESOURCE = "res";
        final String ATTR_DESCRIPTION = "description";
        final String ATTR_LATITUDE = "latitude";
        final String ATTR_LONGITUDE = "longitude";
        final String ATTR_ALTITUDE = "altitude";

        //get example
        //[{"id":1,"description":"bla bla bla","picture":"bild.jpg","latitude":34.43443,"longitude":43.54355,"altitude":43.545,"target":"http://s3-eu-west-1.amazonaws.com/web-api-hosting/jwtc/54afd1bccb34cdd16d3f67f6/20150131/JOs5iFUz/target-collections.wtc"}]

        for (int i = 0; i < js.length(); i++) {

            try {
                final HashMap<String, String> poiInformation = new HashMap<>();
                // Id
                poiInformation.put(ATTR_ID, js.getJSONObject(i).getString("id"));

                // Name
                poiInformation.put(ATTR_NAME, "POI#" + js.getJSONObject(i).getString("id"));
                // Image e. g. (treasure, hint)
                poiInformation.put(ATTR_RESOURCE, "img/magnifier.png");
                // Description
                poiInformation.put(ATTR_DESCRIPTION, js.getJSONObject(i).getString("description"));

                //latitude
                poiInformation.put(ATTR_LATITUDE, js.getJSONObject(i).getString("latitude"));
                //longitude
                poiInformation.put(ATTR_LONGITUDE, js.getJSONObject(i).getString("longitude"));
                //altitude
                poiInformation.put(ATTR_ALTITUDE, js.getJSONObject(i).getString("altitude"));

                pois.put(new JSONObject(poiInformation));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        return pois;
    }

    class LoadCaches extends AsyncTask<String, Void, JSONArray> {
        final int WAIT_FOR_LOCATION_STEP_MS = 5000;

        @Override
        protected void onPreExecute() {

            if (HuntingActivity.this.isLoading) {
                Log.i("LoadCaches", "Task already started");
                cancel(true);
            }

            if (HuntingActivity.this.lastKnownLocation == null
                    && !HuntingActivity.this.isFinishing()) {
                Toast.makeText(
                        HuntingActivity.this,
                        R.string.location_fetching, Toast.LENGTH_SHORT)
                        .show();
            }

            HuntingActivity.this.isLoading = true;
        }

        @Override
        protected JSONArray doInBackground(String... urls) {

            // wait till we have good location
            while (
                    HuntingActivity.this.lastKnownLocation == null
                            && !HuntingActivity.this.isFinishing()) {
                try {
                    Thread.sleep(WAIT_FOR_LOCATION_STEP_MS);
                } catch (InterruptedException e) {
                    cancel(true);
                    return null;
                }
            }

            try {
                HttpGet httpGet = new HttpGet(urls[0]);
                HttpClient httpclient = new DefaultHttpClient();
                HttpResponse response = httpclient.execute(httpGet);
                int status = response.getStatusLine().getStatusCode();

                if (status == 200) {
                    HttpEntity entity = response.getEntity();
                    String data = EntityUtils.toString(entity);
                    return new JSONArray(data);
                }

            } catch (IOException | JSONException ex) {
                ex.printStackTrace();
            }

            return null;
        }

        protected void onPostExecute(JSONArray result) {
            if (result != null) {
                try {
                    HuntingActivity.this.poiData = new JSONArray(result);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                HuntingActivity.this.poiData = getPoiInformation(result);
                HuntingActivity.this
                        .callJavaScript("TreasureHuntAR.hunting", new String[]{
                                HuntingActivity.this.poiData
                                        .toString()});

                Resources res = getResources();
                Toast.makeText(
                        HuntingActivity.this,
                        String.format(res.getString(R.string.found_treaures), result.length()), Toast.LENGTH_SHORT)
                        .show();

            } else {
                Toast.makeText(
                        HuntingActivity.this,
                        R.string.no_treaures, Toast.LENGTH_SHORT)
                        .show();
            }

            HuntingActivity.this.isLoading = false;
        }
    }

    /**
     * helper for creation of dummy places.
     *
     * @param lat center latitude
     * @param lon center longitude
     * @return lat/lon values in given position's vicinity
     */
    private static double[] getRandomLatLonNearby(final double lat,
                                                  final double lon) {
        return new double[]{lat + Math.random() / 5 - 0.1,
                lon + Math.random() / 5 - 0.1};
    }

    private static double[] getRandomLatLonNearby(double lat, double lon,
                                                  int radius) {
        Random random = new Random();

        // Convert radius from meters to degrees
        double radiusInDegrees = radius / 111000f;

        double u = random.nextDouble();
        double v = random.nextDouble();
        double w = radiusInDegrees * Math.sqrt(u);
        double t = 2 * Math.PI * v;
        double x = w * Math.cos(t);
        double y = w * Math.sin(t);

        // Adjust the x-coordinate for the shrinking of the east-west distances
        double new_x = x / Math.cos(lon);

        double foundLongitude = new_x + lon;
        double foundLatitude = y + lat;

        return new double[]{foundLatitude, foundLongitude};
    }

}

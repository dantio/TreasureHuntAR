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
import java.util.Map;
import java.util.Random;

/**
 * Schätze finden
 */
public class HuntingActivity extends AbstractArchitectActivity {

    protected JSONArray poiData;

    protected boolean isLoadingCaches = false;
    protected boolean isLoadingTarget = false;

    // Endpoints http://ericwuendisch.de/restnode/server/uploads/
    public static final String ALL_CACHES = "http://www.vegapunk.de:9999/caches";
    public static final String UPLOAD_PATH = "http://ericwuendisch.de/restnode/server/uploads/";

    // User go for magnifier
    public static final String ACTION_START_HUNTING_MAGNIFIER = "startHuntingMagnifier";
    public static final String ACTION_STOP_HUNTING_MAGNIFIER = "stopHuntingMagnifier";

    // User is in magnifier action range and start hunting treasure
    public static final String ACTION_START_HUNTING_TREASURE = "startHuntingTreasure";
    public static final String ACTION_STOP_HUNTING_TREASURE = "stopHuntingTreasure";

    // State
    public boolean isHuntingMagnifier = false;
    public boolean isHuntingTreasure = false;

    // Treasure to hunt
    public int huntingTreasureId = -1;

    /**
     * radius in m
     */
    public static final int MAX_RADIUS = 2000;

    /**
     * max tresures
     */
    public static final int MIN_TRESURES = 1;

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
        gestureDetector.setBaseListener(new GestureDetector.BaseListener() {
            @Override
            public boolean onGesture(Gesture gesture) {
                switch (gesture) {
                    case TAP:
                        if (!isHuntingMagnifier && !isHuntingTreasure) {
                            Log.i("gesture", "Tap");
                            callJavaScript("TreasureHuntAR." + ACTION_START_HUNTING_MAGNIFIER);
                        }
                        return true;
                    case SWIPE_DOWN:
                        Log.i("gesture", "Swipe Down");
                        if (isHuntingMagnifier) {
                            Log.i("gesture", ACTION_STOP_HUNTING_MAGNIFIER);
                            callJavaScript("TreasureHuntAR." + ACTION_STOP_HUNTING_MAGNIFIER);
                            isHuntingMagnifier = false;
                            return true;
                        } else if (isHuntingTreasure) {
                            Log.i("gesture", ACTION_STOP_HUNTING_TREASURE);
                            callJavaScript("TreasureHuntAR." + ACTION_STOP_HUNTING_TREASURE);
                            isHuntingTreasure = false;
                            return true;
                        } else {
                            finish();
                            return false;
                        }

                    case SWIPE_RIGHT: // TODO select next magnifier
                        return true;

                    case SWIPE_LEFT: // TODO select prev magnifier
                        return true;
                }

                return false;
            }
        });

        return gestureDetector;
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
                    Log.i(ACTION_START_HUNTING_TREASURE, "" + treasureId);

                }

                return false;
            }
        };
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
        loadCache();
        //loadTreasureData(0);
    }

    protected void loadCache() {
        if (!isLoadingCaches) {
            Log.i("load", "Load all caches");
            new LoadCaches().execute(ALL_CACHES);
        }
    }

    /**
     * loads poiInformation and returns them as JSONArray. Ensure attributeNames of JSON POIs are well known in JavaScript, so you can parse them easily
     *
     * @return POI information in JSONArray
     */
    public JSONArray getPoiInformation(final JSONArray js, int minTreasures) {

        final JSONArray pois = new JSONArray();

        // equals "AR.CONST.UNKNOWN_ALTITUDE" in JavaScript (compare AR.GeoLocation specification)
        // Use "AR.CONST.UNKNOWN_ALTITUDE" to tell ARchitect that altitude of places should be on user level.
        // Be aware to handle altitude properly in locationManager in case you use valid POI altitude
        // value (e.g. pass altitude only if GPS accuracy is <7m).
        final float UNKNOWN_ALTITUDE = -32768f;

        // ensure these attributes are also used in JavaScript when extracting POI data
        final String ATTR_ID = "id";
        final String ATTR_NAME = "name";
        final String ATTR_DESCRIPTION = "description";
        final String ATTR_LATITUDE = "latitude";
        final String ATTR_LONGITUDE = "longitude";
        final String ATTR_ALTITUDE = "altitude";
        final String ATTR_PICTURE = "picture";
        final String TARGET = "target";

        if (js != null) {
            for (int i = 0; i < js.length(); i++) {
                try {
                    JSONObject obj = js.getJSONObject(i);
                    String target = obj.getString("target");
                    // This is important
                    if (target == null) {
                        continue;
                    }
                    final HashMap<String, String> poiInformation = new HashMap<>();
                    poiInformation.put(ATTR_ID, obj.getString("id"));
                    poiInformation.put(ATTR_NAME, "POI#" + obj.getString("id"));
                    poiInformation.put(ATTR_DESCRIPTION, obj.getString("description"));
                    poiInformation.put(ATTR_LATITUDE, obj.getString("latitude"));
                    poiInformation.put(ATTR_LONGITUDE, obj.getString("longitude"));
                    poiInformation.put(ATTR_ALTITUDE, obj.getString("altitude"));
                    poiInformation.put(ATTR_PICTURE, UPLOAD_PATH + "/" + obj.getString("picture"));

                    poiInformation.put(TARGET, target);

                    pois.put(new JSONObject(poiInformation));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }

        // fill with random treasures
        for (int i = pois.length(); i < minTreasures; i++) {
            final HashMap<String, String> poiInformation = new HashMap<>();
            poiInformation.put(ATTR_ID, String.valueOf(i));
            poiInformation.put(ATTR_NAME, "POI#" + i);
            poiInformation.put(ATTR_DESCRIPTION, "This is the description of POI#" + i);

            double[] poiLocationLatLon = getRandomLatLonNearby(
                    lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude(),
                    MAX_RADIUS);

            poiInformation.put(ATTR_LATITUDE, String.valueOf(poiLocationLatLon[0]));
            poiInformation.put(ATTR_LONGITUDE, String.valueOf(poiLocationLatLon[1]));
            poiInformation.put(ATTR_ALTITUDE, String.valueOf(UNKNOWN_ALTITUDE));
            poiInformation.put(TARGET, "http://s3-eu-west-1.amazonaws.com/web-api-hosting/jwtc/54a6a99e160a69d26dc51ad4/20150204/NCvpvFfo/target-collections.wtc");
            poiInformation.put(ATTR_PICTURE, "http://ericwuendisch.de/restnode/server/uploads/b2QK17zHWmn37ivU9gTzoIBXgwy7KZD9.jpg");
            pois.put(new JSONObject(poiInformation));
        }

        return pois;
    }

    /**
     * Load nearby caches
     */
    class LoadCaches extends AsyncTask<String, Void, JSONArray> {
        final int WAIT_FOR_LOCATION_STEP_MS = 5000;

        @Override
        protected void onPreExecute() {

            if (isLoadingCaches) {
                Log.i("LoadCaches", "Task already started");
                cancel(true);
            }

            if (lastKnownLocation == null && !isFinishing()) {
                Toast.makeText(
                        HuntingActivity.this,
                        R.string.location_fetching, Toast.LENGTH_SHORT)
                        .show();
            }

            isLoadingCaches = true;
        }

        @Override
        protected JSONArray doInBackground(String... urls) {

            // wait till we have good location
            while (
                    lastKnownLocation == null && !isFinishing()) {
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
                Log.e("LoadCaches", "No Server conncection");
            }

            return null;
        }

        protected void onPostExecute(JSONArray result) {

            if (result == null) {
                Toast.makeText(
                        HuntingActivity.this,
                        R.string.no_treaures, Toast.LENGTH_SHORT)
                        .show();
            } else {
                Resources res = getResources();
                Toast.makeText(
                        HuntingActivity.this,
                        String.format(res.getString(R.string.found_treaures), result.length()), Toast.LENGTH_SHORT)
                        .show();
            }

            poiData = getPoiInformation(result, MIN_TRESURES);
            callJavaScript("TreasureHuntAR.hunting", new String[]{
                    poiData
                            .toString()});


            isLoadingCaches = false;
        }
    }

    /**
     * helper for creation of dummy places.
     *
     * @param lat center latitude
     * @param lon center longitude
     * @return lat/lon values in given position's vicinity
     */
    private static double[] getRandomLatLonNearby(double lat, double lon, int radius) {
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

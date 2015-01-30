package de.htw.ar.treasurehuntar;

import android.content.Context;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.Toast;
import com.google.android.glass.touchpad.Gesture;
import com.google.android.glass.touchpad.GestureDetector;
import com.wikitude.architect.ArchitectView.ArchitectUrlListener;
import com.wikitude.architect.ArchitectView.SensorAccuracyChangeListener;
import org.json.JSONArray;
import org.json.JSONObject;

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
        if (mGestureDetector != null) {
            return mGestureDetector.onMotionEvent(event);
        }
        return false;
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

    final Runnable loadData = new Runnable() {

        @Override
        public void run() {

            HuntingActivity.this.isLoading = true;

            final int WAIT_FOR_LOCATION_STEP_MS = 5000;

            while (
                    HuntingActivity.this.lastKnownLocation == null
                            && !HuntingActivity.this.isFinishing()) {

                HuntingActivity.this
                        .runOnUiThread(new Runnable() {

                            @Override
                            public void run() {
                                Toast.makeText(
                                        HuntingActivity.this,
                                        R.string.location_fetching, Toast.LENGTH_SHORT)
                                        .show();
                            }
                        });

                try {
                    Thread.sleep(WAIT_FOR_LOCATION_STEP_MS);
                } catch (InterruptedException e) {
                    break;
                }
            }

            if (HuntingActivity.this.lastKnownLocation != null
                    && !HuntingActivity.this.isFinishing()) {
                // TODO: you may replace this dummy implementation and instead load POI information e.g. from your database
                HuntingActivity.this.poiData = HuntingActivity
                        .getPoiInformation(
                                HuntingActivity.this.lastKnownLocation,
                                MAX_TRESURES);

                HuntingActivity.this
                        .callJavaScript("TreasureHuntAR.hunting", new String[]{
                                HuntingActivity.this.poiData
                                        .toString()});
            }

            HuntingActivity.this.isLoading = false;
        }
    };

    @Override
    protected void onPostCreate(final Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        loadData();
    }

    protected void loadData() {
        if (!isLoading) {
            final Thread t = new Thread(loadData);
            t.start();
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
     * @param userLocation   the location of the user
     * @param numberOfPlaces number of places to load (at max)
     * @return POI information in JSONArray
     */
    public static JSONArray getPoiInformation(final Location userLocation,
                                              final int numberOfPlaces) {

        if (userLocation == null) {
            return null;
        }

        final JSONArray pois = new JSONArray();

        // ensure these attributes are also used in JavaScript when extracting POI data
        final String ATTR_ID = "id";
        final String ATTR_NAME = "name";
        final String ATTR_RESOURCE = "res";
        final String ATTR_DESCRIPTION = "description";
        final String ATTR_LATITUDE = "latitude";
        final String ATTR_LONGITUDE = "longitude";
        final String ATTR_ALTITUDE = "altitude";

        // Http.get(/glass).done(function(){})
        for (int i = 0; i < numberOfPlaces; i++) {
            final HashMap<String, String> poiInformation = new HashMap<>();
            // Id
            poiInformation.put(ATTR_ID, String.valueOf(i));
            // Name
            poiInformation.put(ATTR_NAME, "POI#" + i);
            // Image e. g. (treasure, hint)
            poiInformation.put(ATTR_RESOURCE, "img/magnifier.png");
            // Description
            poiInformation
                    .put(ATTR_DESCRIPTION, "This is the description of POI#" + i);

            double[] poiLocationLatLon = getRandomLatLonNearby(
                    userLocation.getLatitude(), userLocation.getLongitude(),
                    MAX_RADIUS);

            poiInformation
                    .put(ATTR_LATITUDE, String.valueOf(poiLocationLatLon[0]));
            poiInformation
                    .put(ATTR_LONGITUDE, String.valueOf(poiLocationLatLon[1]));

            final float UNKNOWN_ALTITUDE = -32768f;  // equals "AR.CONST.UNKNOWN_ALTITUDE" in JavaScript (compare AR.GeoLocation specification)
            // Use "AR.CONST.UNKNOWN_ALTITUDE" to tell ARchitect that altitude of places should be on user level. Be aware to handle altitude properly in locationManager in case you use valid POI altitude value (e.g. pass altitude only if GPS accuracy is <7m).
            poiInformation.put(ATTR_ALTITUDE, String.valueOf(UNKNOWN_ALTITUDE));
            pois.put(new JSONObject(poiInformation));
        }

        return pois;
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

package de.htw.ar.treasurehuntar;

import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.widget.Toast;
import com.wikitude.architect.ArchitectView.ArchitectUrlListener;
import com.wikitude.architect.ArchitectView.SensorAccuracyChangeListener;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;

/**
 * Find treasures
 */
public class DiscoverActivity extends AbstractArchitectActivity {

    protected JSONArray poiData;
    protected boolean isLoading = false;

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
    public SensorAccuracyChangeListener getSensorAccuracyListener() {
        return new SensorAccuracyChangeListener() {
            @Override
            public void onCompassAccuracyChanged(int accuracy) {
                /* UNRELIABLE = 0, LOW = 1, MEDIUM = 2, HIGH = 3 */
                if (accuracy < SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM
                    && DiscoverActivity.this != null && !DiscoverActivity.this
                    .isFinishing() && System.currentTimeMillis()
                    - DiscoverActivity.this.lastCalibrationToastShownTimeMillis
                    > 5 * 1000) {
                    Toast.makeText(DiscoverActivity.this,
                        R.string.compass_accuracy_low, Toast.LENGTH_LONG)
                        .show();
                    DiscoverActivity.this.lastCalibrationToastShownTimeMillis = System
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
        return CULLING_DISTANCE_DEFAULT_METERS;
    }

    final Runnable loadData = new Runnable() {

        @Override
        public void run() {

            DiscoverActivity.this.isLoading = true;

            final int WAIT_FOR_LOCATION_STEP_MS = 2000;

            while (
                DiscoverActivity.this.lastKnownLocation == null
                    && !DiscoverActivity.this.isFinishing()) {

                DiscoverActivity.this
                    .runOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                            Toast.makeText(
                                DiscoverActivity.this,
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

            if (DiscoverActivity.this.lastKnownLocation != null
                && !DiscoverActivity.this.isFinishing()) {
                // TODO: you may replace this dummy implementation and instead load POI information e.g. from your database
                DiscoverActivity.this.poiData = DiscoverActivity
                    .getPoiInformation(
                        DiscoverActivity.this.lastKnownLocation,
                        20);
                DiscoverActivity.this
                    .callJavaScript("World.loadPoisFromJsonData", new String[] {
                        DiscoverActivity.this.poiData
                            .toString() });
            }

            DiscoverActivity.this.isLoading = false;
        }
    };

    protected void loadData() {
        if (!isLoading) {
            final Thread t = new Thread(loadData);
            t.start();
        }
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
        final String ATTR_DESCRIPTION = "description";
        final String ATTR_LATITUDE = "latitude";
        final String ATTR_LONGITUDE = "longitude";
        final String ATTR_ALTITUDE = "altitude";

        for (int i = 1; i <= numberOfPlaces; i++) {
            final HashMap<String, String> poiInformation = new HashMap<String, String>();
            poiInformation.put(ATTR_ID, String.valueOf(i));
            poiInformation.put(ATTR_NAME, "POI#" + i);
            poiInformation
                .put(ATTR_DESCRIPTION, "This is the description of POI#" + i);
            double[] poiLocationLatLon = getRandomLatLonNearby(
                userLocation.getLatitude(), userLocation.getLongitude());
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
        return new double[] { lat + Math.random() / 5 - 0.1,
            lon + Math.random() / 5 - 0.1 };
    }

}

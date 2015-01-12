package de.htw.ar.treasurehuntar;

import android.hardware.SensorManager;
import android.location.LocationListener;
import android.widget.Toast;
import com.wikitude.architect.ArchitectView.ArchitectUrlListener;
import com.wikitude.architect.ArchitectView.SensorAccuracyChangeListener;

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

}

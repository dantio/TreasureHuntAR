package de.htw.ar.treasurehuntar;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.pm.ApplicationInfo;
import android.location.Location;
import android.location.LocationListener;
import android.media.AudioManager;
import android.opengl.GLES20;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;
import android.webkit.WebView;
import android.widget.Toast;
import com.wikitude.architect.ArchitectView;
import com.wikitude.architect.ArchitectView.ArchitectConfig;
import com.wikitude.architect.ArchitectView.ArchitectUrlListener;
import com.wikitude.architect.ArchitectView.SensorAccuracyChangeListener;

import java.io.IOException;

/**
 * Abstract activity which handles live-cycle events.
 * Feel free to extend from this activity when setting up your own AR-Activity
 */
public abstract class AbstractArchitectActivity extends Activity {

    /**
     * extras key for architect-url to load, usually already known upfront, can be relative folder to assets (myWorld.html --> assets/myWorld.html is loaded) or web-url ("http://myserver.com/myWorld.html"). Note that argument passing is only possible via web-url
     */
    protected static final String EXTRAS_KEY_ACTIVITY_ARCHITECT_WORLD_URL = "activityArchitectWorldUrl";

    /**
     * extras key for activity title, usually static and set in Manifest.xml
     */
    protected static final String EXTRAS_KEY_ACTIVITY_TITLE_STRING = "activityTitle";

    /**
     * 1km = architectView's default cullingDistance, return this value in "getInitialCullingDistanceMeters()" to not change cullingDistance.
     */
    public static final int CULLING_DISTANCE_DEFAULT_METERS = 1 * 1000;

    /**
     * holds the Wikitude SDK AR-View, this is where camera, markers, compass, 3D models etc. are rendered
     */
    protected ArchitectView architectView;

    /**
     * sensor accuracy listener in case you want to display calibration hints
     */
    protected SensorAccuracyChangeListener sensorAccuracyListener;

    /**
     * last known location of the user, used internally for content-loading after user location was fetched
     */
    protected Location lastKnownLocation;

    /**
     * sample location strategy, you may implement a more sophisticated approach too
     */
    protected LocationProvider locationProvider;

    /**
     * location listener receives location updates and must forward them to the architectView
     */
    protected LocationListener locationListener;

    /**
     * urlListener handling "document.location= 'architectsdk://...' " calls in JavaScript"
     */
    protected ArchitectUrlListener urlListener;

    /**
     * sets maximum distance to render places. In case your places are more than 50km away from the user you must adjust this value (compare 'AR.context.scene.cullingDistance').
     * Return ArchitectViewHolder.CULLING_DISTANCE_DEFAULT_METERS to not change default behavior (50km range) or any positive float to set cullingDistance on architectView start.
     *
     * @return
     */
    public abstract float getInitialCullingDistanceMeters();

    /**
     * Called when the activity is first created.
     */
    @SuppressLint("NewApi")
    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // pressing volume up/down should cause music volume changes
        this.setVolumeControlStream(AudioManager.STREAM_MUSIC);

        // set samples content view
        this.setContentView(this.getContentViewId());

        this.setTitle(this.getActivityTitle());

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        Log.i("architect", "create");

        //
        // this enables remote debugging of a WebView on Android 4.4+ when debugging = true in AndroidManifest.xml
        // If you get a compile time error here, ensure to have SDK 19+ used in your ADT/Eclipse.
        // You may even delete this block in case you don't need remote debugging or don't have an Android 4.4+ device in place.
        // Details: https://developers.google.com/chrome-developer-tools/docs/remote-debugging
        //
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            if (0
                != (getApplicationInfo().flags &= ApplicationInfo.FLAG_DEBUGGABLE)) {
                WebView.setWebContentsDebuggingEnabled(true);
            }
        }

        // set AR-view for life-cycle notifications etc.
        this.architectView = (ArchitectView) this
            .findViewById(this.getArchitectViewId());

        // pass SDK key if you have one, this one is only valid for this package identifier and must not be used somewhere else
        final ArchitectConfig config = new ArchitectConfig(
            this.getWikitudeSDKLicenseKey());

        try {
            // first mandatory life-cycle notification
            this.architectView.onCreate(config);
        } catch (RuntimeException rex) {
            this.architectView = null;
            Toast.makeText(getApplicationContext(),
                "can't create Architect View", Toast.LENGTH_SHORT).show();
            Log.e(this.getClass().getName(),
                "Exception in ArchitectView.onCreate()", rex);
        }

        // set accuracy listener if implemented, you may e.g. show calibration prompt for compass using this listener
        this.sensorAccuracyListener = this.getSensorAccuracyListener();

        // set urlListener, any calls made in JS like "document.location = 'architectsdk://foo?bar=123'" is forwarded to this listener, use this to interact between JS and native Android activity/fragment
        this.urlListener = this.getUrlListener();

        // register valid urlListener in architectView, ensure this is set before content is loaded to not miss any event
        if (this.urlListener != null && this.architectView != null) {
            this.architectView.registerUrlListener(this.getUrlListener());
        }

        // listener passed over to locationProvider, any location update is handled here
        this.locationListener = new LocationListener() {

            @Override
            public void onStatusChanged(String provider, int status,
                Bundle extras) {
            }

            @Override
            public void onProviderEnabled(String provider) {
            }

            @Override
            public void onProviderDisabled(String provider) {
            }

            @Override
            public void onLocationChanged(final Location location) {
                // forward location updates fired by LocationProvider to architectView, you can set lat/lon from any location-strategy
                if (location != null) {
                    Log.i("location", location.toString());
                    // sore last location as member, in case it is needed somewhere (in e.g. your adjusted project)
                    AbstractArchitectActivity.this.lastKnownLocation = location;
                    if (AbstractArchitectActivity.this.architectView
                        != null) {
                        // check if location has altitude at certain accuracy level & call right architect method (the one with altitude information)
                        if (location.hasAltitude() && location.hasAccuracy()
                            && location.getAccuracy() < 7) {
                            AbstractArchitectActivity.this.architectView
                                .setLocation(location.getLatitude(),
                                    location.getLongitude(),
                                    location.getAltitude(),
                                    location.getAccuracy());
                        } else {
                            AbstractArchitectActivity.this.architectView
                                .setLocation(location.getLatitude(),
                                    location.getLongitude(),
                                    location.hasAccuracy() ?
                                        location.getAccuracy() :
                                        1000);
                        }
                    }
                }
            }
        };

        // locationProvider used to fetch user position
        this.locationProvider = getLocationProvider(this.locationListener);
    }

    @Override
    protected void onPostCreate(final Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        if (this.architectView != null) {

            // call mandatory live-cycle method of architectView
            this.architectView.onPostCreate();

            try {
                // load content via url in architectView, ensure '<script src="architect://architect.js"></script>' is part of this HTML file, have a look at wikitude.com's developer section for API references
                this.architectView.load(this.getARchitectWorldPath());

                if (this.getInitialCullingDistanceMeters()
                    != CULLING_DISTANCE_DEFAULT_METERS) {
                    // set the culling distance - meaning: the maximum distance to render geo-content
                    this.architectView.setCullingDistance(
                        this.getInitialCullingDistanceMeters());
                }

            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // call mandatory live-cycle method of architectView
        if (this.architectView != null) {
            this.architectView.onResume();

            // register accuracy listener in architectView, if set
            if (this.sensorAccuracyListener != null) {
                this.architectView.registerSensorAccuracyChangeListener(
                    this.sensorAccuracyListener);
            }
        }

        // tell locationProvider to resume, usually location is then (again) fetched, so the GPS indicator appears in status bar
        if (this.locationProvider != null) {
            this.locationProvider.onResume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        // call mandatory live-cycle method of architectView
        if (this.architectView != null) {
            this.architectView.onPause();

            // unregister accuracy listener in architectView, if set
            if (this.sensorAccuracyListener != null) {
                this.architectView.unregisterSensorAccuracyChangeListener(
                    this.sensorAccuracyListener);
            }
        }

        // tell locationProvider to pause, usually location is then no longer fetched, so the GPS indicator disappears in status bar
        if (this.locationProvider != null) {
            this.locationProvider.onPause();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // call mandatory live-cycle method of architectView
        if (this.architectView != null) {
            this.architectView.onDestroy();
        }
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if (this.architectView != null) {
            this.architectView.onLowMemory();
        }
    }

    /**
     * title shown in activity
     *
     * @return
     */
    public String getActivityTitle() {
        return (getIntent().getExtras() != null && getIntent().getExtras().get(
            EXTRAS_KEY_ACTIVITY_TITLE_STRING) != null) ? getIntent()
            .getExtras().getString(EXTRAS_KEY_ACTIVITY_TITLE_STRING)
            : "Test-World";
    }

    /**
     * path to the architect-file (AR-Experience HTML) to launch
     *
     * @return
     */
    public String getARchitectWorldPath() {
        return getIntent().getExtras().getString(
            EXTRAS_KEY_ACTIVITY_ARCHITECT_WORLD_URL);
    }

    /**
     * url listener fired once e.g. 'document.location = "architectsdk://foo?bar=123"' is called in JS
     *
     * @return
     */
    public abstract ArchitectUrlListener getUrlListener();

    /**
     * @return layout id of your layout.xml that holds an ARchitect View, e.g. R.layout.camview
     */
    public int getContentViewId() {
        return R.layout.main_layout;
    }

    /**
     * @return Wikitude SDK license key, checkout www.wikitude.com for details
     */
    public String getWikitudeSDKLicenseKey() {
        return Constants.WIKITUDE_SDK_KEY;
    }

    /**
     * @return layout-id of architectView, e.g. R.id.architectView
     */
    public int getArchitectViewId() {
        return R.id.architectView;
    }

    /**
     * @return Implementation of a Location
     */
    public abstract LocationProvider getLocationProvider(
        final LocationListener locationListener);

    /**
     * @return Implementation of Sensor-Accuracy-Listener. That way you can e.g. show prompt to calibrate compass
     */
    public abstract ArchitectView.SensorAccuracyChangeListener getSensorAccuracyListener();

    /**
     * helper to check if video-drawables are supported by this device. recommended to check before launching ARchitect Worlds with videodrawables
     *
     * @return true if AR.VideoDrawables are supported, false if fallback rendering would apply (= show video fullscreen)
     */
    public static final boolean isVideoDrawablesSupported() {
        String extensions = GLES20.glGetString(GLES20.GL_EXTENSIONS);
        return extensions != null && extensions
            .contains("GL_OES_EGL_image_external")
            && android.os.Build.VERSION.SDK_INT >= 14;
    }

    /**
     * call JacaScript in architectView
     *
     * @param methodName
     * @param arguments
     */
    protected void callJavaScript(final String methodName,
        final String[] arguments) {
        final StringBuilder argumentsString = new StringBuilder("");
        for (int i = 0; i < arguments.length; i++) {
            argumentsString.append(arguments[i]);
            if (i < arguments.length - 1) {
                argumentsString.append(", ");
            }
        }

        if (this.architectView != null) {
            final String js = (methodName + "( " + argumentsString.toString()
                + " );");
            this.architectView.callJavascript(js);
        }
    }

}
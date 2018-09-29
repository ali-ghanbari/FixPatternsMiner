package com.mapzen.support;

import com.mapzen.R;
import com.mapzen.activity.BaseActivity;
import com.mapzen.android.gson.Feature;
import com.mapzen.android.gson.Geometry;
import com.mapzen.android.gson.Properties;
import com.mapzen.entity.SimpleFeature;
import com.mapzen.fragment.MapFragment;
import com.mapzen.osrm.Instruction;
import com.mapzen.util.DatabaseHelper;

import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.oscim.android.MapView;
import org.robolectric.shadows.ShadowLocationManager;
import org.robolectric.tester.android.view.TestMenu;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationManager;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentManager;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executor;

import static android.content.Context.LOCATION_SERVICE;
import static android.location.LocationManager.GPS_PROVIDER;
import static com.mapzen.entity.SimpleFeature.ADMIN1_ABBR;
import static com.mapzen.entity.SimpleFeature.ADMIN1_NAME;
import static com.mapzen.entity.SimpleFeature.NAME;
import static org.robolectric.Robolectric.application;
import static org.robolectric.Robolectric.buildActivity;
import static org.robolectric.Robolectric.shadowOf;

public final class TestHelper {

    public static final String MOCK_ROUTE_JSON = TestHelper.getFixture("basic_route");
    public static final String MOCK_NO_ROUTE_JSON = TestHelper.getFixture("no_route_found");
    public static final String MOCK_NY_TO_VT = TestHelper.getFixture("ny_to_vermount");
    public static final String MOCK_AROUND_THE_BLOCK = TestHelper.getFixture("around_the_block");

    private TestHelper() {
    }

    public static TestBaseActivity initBaseActivity() {
        return initBaseActivityWithMenu(new TestMenu());
    }

    public static TestBaseActivity initBaseActivityWithMenu(TestMenu menu) {
        TestHelper.initLastKnownLocation();
        TestBaseActivity activity = buildActivity(TestBaseActivity.class)
                .create()
                .start()
                .resume()
                .visible()
                .get();
        activity.onCreateOptionsMenu(menu);
        activity.registerMapView(new MapView(activity));
        activity.setDebugDataExecutor(new ImmediateExecutor());
        return activity;
    }

    public static MapFragment initMapFragment(BaseActivity activity) {
        FragmentManager manager = activity.getSupportFragmentManager();
        MapFragment mapFragment = (MapFragment) manager.findFragmentById(R.id.map_fragment);
        mapFragment.setAct(activity);
        mapFragment.onStart();
        return mapFragment;
    }

    public static Location getTestLocation(double lat, double lng) {
        Location location = new Location("testing");
        location.setLatitude(lat);
        location.setLongitude(lng);
        return location;
    }

    public static Location getTestLocation(String provider, float lat, float lng, long time) {
        Location location = new Location(provider);
        location.setLatitude(lat);
        location.setLongitude(lng);
        location.setTime(time);
        return location;
    }

    public static Instruction getTestInstruction(double lat, double lng) throws Exception {
        String raw = "        [\n" +
                "            \"10\",\n" + // turn instruction
                "            \"19th Street\",\n" + // way
                "            160,\n" + // length in meters
                "            0,\n" + // position?
                "            0,\n" + // time in seconds
                "            \"160m\",\n" + // length with unit
                "            \"SE\",\n" + //earth direction
                "            128\n" + // azimuth
                "        ]\n";
        Instruction instruction = new Instruction(new JSONArray(raw));
        instruction.setLocation(getTestLocation(lat, lng));
        return instruction;
    }


    public static SimpleFeature getTestSimpleFeature() {
        SimpleFeature simpleFeature = new SimpleFeature();
        simpleFeature.setLat(1.0);
        simpleFeature.setLon(1.0);
        simpleFeature.setProperty(NAME, "Test SimpleFeature");
        simpleFeature.setProperty(ADMIN1_NAME, "New York");
        simpleFeature.setProperty(ADMIN1_ABBR, "NY");
        return simpleFeature;
    }

    public static Feature getTestFeature() {
        Feature feature = new Feature();
        Properties properties = new Properties();
        properties.setName("test");
        properties.setAdmin0_name("test");
        properties.setAdmin0_abbr("test");
        feature.setProperties(properties);
        Geometry geometry = new Geometry();
        List<Double> coordinates = new ArrayList<Double>();
        coordinates.add(0.0);
        coordinates.add(0.0);
        geometry.setCoordinates(coordinates);
        feature.setGeometry(geometry);
        return feature;
    }

    public static String getFixture(String name) {
        String basedir = System.getProperty("user.dir");
        File file = new File(basedir + "/src/test/fixtures/" + name + ".fixture");
        String fixture = "";
        try {
            fixture = FileUtils.readFileToString(file, "UTF-8");
        } catch (Exception e) {
            fixture = "not found";
        }
        return fixture;
    }

    public static void enableDebugMode(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor prefEditor = prefs.edit();
        prefEditor.putBoolean(context.getString(R.string.settings_key_debug), true);
        prefEditor.commit();
    }

    public static void initLastKnownLocation() {
        LocationManager locationManager = (LocationManager)
                application.getSystemService(LOCATION_SERVICE);
        ShadowLocationManager shadowLocationManager = shadowOf(locationManager);
        shadowLocationManager.setLastKnownLocation(GPS_PROVIDER, new Location(GPS_PROVIDER));
    }

    public static void populateDatabase(BaseActivity act) throws Exception {
        populateRoutesTable(act);
        populateLocationsTable(act);
        populateRoutesGeometryTable(act);
        populateLogEntriesTable(act);
    }

    private static void populateLogEntriesTable(BaseActivity act) {
        ContentValues logValues = new ContentValues();
        logValues.put(DatabaseHelper.COLUMN_TAG, "tag");
        logValues.put(DatabaseHelper.COLUMN_MSG, "log message");
        logValues.put(DatabaseHelper.COLUMN_TABLE_ID, UUID.randomUUID().toString());
        act.getDb().insert(DatabaseHelper.TABLE_LOG_ENTRIES, null, logValues);
    }

    private static void populateRoutesGeometryTable(BaseActivity act) {
        String routeId = UUID.randomUUID().toString();
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_TABLE_ID, UUID.randomUUID().toString());
        values.put(DatabaseHelper.COLUMN_ROUTE_ID, routeId);
        values.put(DatabaseHelper.COLUMN_POSITION, 0);
        values.put(DatabaseHelper.COLUMN_LAT, 0);
        values.put(DatabaseHelper.COLUMN_LNG, 0);
        act.getDb().insert(DatabaseHelper.TABLE_ROUTE_GEOMETRY, null, values);
    }

    public static void populateRoutesTable(BaseActivity act) {
        ContentValues insertValues = new ContentValues();
        String routeId = UUID.randomUUID().toString();
        insertValues.put(DatabaseHelper.COLUMN_TABLE_ID, routeId);
        insertValues.put(DatabaseHelper.COLUMN_RAW, "blabla");
        act.getDb().insert(DatabaseHelper.TABLE_ROUTES, null, insertValues);
    }

    private static void populateLocationsTable(BaseActivity act) throws Exception {
        act.getDb().insert(DatabaseHelper.TABLE_LOCATIONS, null,
                DatabaseHelper.valuesForLocationCorrection(TestHelper.getTestLocation(0.0, 0.0),
                        getTestLocation(1.0, 1.0), getTestInstruction(0.0, 0.0), "random-id"));
    }

    public static class TestLocation extends Location {
        private double distance;
        private double bearing;
        private double rLat, rLng;
        private Location originalLocation;
        private int R = 6371;

        public TestLocation(String provider) {
            super(provider);
        }

        private TestLocation(Builder builder) {
            super("fake tester");
            this.distance = builder.distance / R;
            this.bearing = Math.toRadians(builder.bearing);
            this.originalLocation = builder.originalLocation;
            this.rLat = Math.toRadians(originalLocation.getLatitude());
            this.rLng = Math.toRadians(originalLocation.getLongitude());
            this.setLatitude(Math.toDegrees(getFarAwayLat()));
            this.setLongitude(Math.toDegrees(getFarAwayLng()));
        }

        private double getFarAwayLat() {
            double lat = Math.asin(Math.sin(rLat)
                    * Math.cos(distance / R)
                    + Math.cos(rLat * Math.sin(distance / R))
                    * Math.cos(bearing));
            return lat;
        }

        private double getFarAwayLng() {
            double lng = rLng + Math.atan2(
                    Math.sin(bearing)
                            * Math.sin(distance)
                            * Math.cos(rLat)
                    ,
                    Math.cos(distance)
                            - Math.sin(rLat)
                            * Math.sin(getFarAwayLat())
            );

            return lng;
        }

        public static class Builder {
            private double distance;
            private double bearing;
            private Location originalLocation;

            public Builder(Location location) {
                this.originalLocation = location;
            }

            public Builder setDistance(double distance) {
                this.distance = distance;
                return this;
            }

            public Builder setBearing(double bearing) {
                this.bearing = bearing;
                return this;
            }

            public TestLocation build() {
                return new TestLocation(this);
            }
        }
    }

    public static class ImmediateExecutor implements Executor {
        @Override
        public void execute(Runnable command) {
            command.run();
        }
    }
}

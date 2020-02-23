package es.gpsou.vehiclealarm;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import androidx.fragment.app.FragmentActivity;
import android.util.Log;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

public class MonitorMapActivity extends FragmentActivity implements OnMapReadyCallback {

    private static GoogleMap mMap=null;
    private static Marker mMarker=null;
    private static Circle circle=null;

    private static MonitorMapActivity.UpdateUI updateUI=null;
    private static IntentFilter broadcastFilter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_monitor_map);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        broadcastFilter = new IntentFilter();
        broadcastFilter.addAction(Globals.UPDATE_UI_INTENT_ACTION);

        if(updateUI==null)
            updateUI=new MonitorMapActivity.UpdateUI();

    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        if(mMap==null)
            return;

        processIntent(intent);
    }

    @Override
    protected void onStart() {
        super.onStart();

        registerReceiver(updateUI, broadcastFilter);
        Log.d(Globals.TAG, "Broadcast registrado");
    }

    @Override
    protected void onStop() {
        super.onStop();

        unregisterReceiver(updateUI);
        Log.d(Globals.TAG, "Broadcast desregistrado");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if(circle!=null)
            circle.remove();

        circle=null;
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMarker=mMap.addMarker(new MarkerOptions()
                .icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_car))
                .anchor(0.5f, 0.5f)
                .position(new LatLng(0, 0))
                .title(getString(R.string.monitor_map_marker_tittle)));

        processIntent(getIntent());
    }

    private void processIntent(Intent intent) {
        Bundle bundle=null;
        LatLng latLong=null;
        long ts=0;

        if(intent!=null)
            bundle=intent.getBundleExtra(Globals.BUNDLE_KEY_LOCATION);

        if(bundle!=null) {
            latLong = new LatLng(Double.parseDouble(bundle.getString(Globals.BUNDLE_LATITUDE)),
                    Double.parseDouble(bundle.getString(Globals.BUNDLE_LONGITUDE)));
            ts=Long.parseLong(bundle.getString(Globals.BUNDLE_TIMESTAMP));
        } else {
            VehicleDeviceStatus vds=Globals.vehicleDeviceStatus;
            latLong = new LatLng(vds.latitude, vds.longitude);
            ts=vds.locationTs;

            if(vds.parkingStatus > 0 && circle == null) {
                circle = mMap.addCircle(new CircleOptions()
                        .center(new LatLng(vds.latitude_park, vds.longitude_park))
                        .radius(vds.parkingStatus)
                        .strokeColor(Color.RED)
                        .fillColor(Color.TRANSPARENT));
            }
        }
        mMarker.setPosition(latLong);
        mMarker.setSnippet(getString(R.string.monitor_map_marker_snippet)+" "+MonitorActivity.tsToStr(ts));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLong, 17.0f));
    }

    public class UpdateUI extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            int op=intent.getIntExtra(Globals.P2P_OP, 0);

            setResultCode(Activity.RESULT_OK);

            VehicleDeviceStatus vds=Globals.vehicleDeviceStatus;

            switch (op) {
                case Globals.SENSOR_ALARM:
                    break;
                case Globals.GEOFENCE_TRANSITION:
                    if(circle!=null) {
                        circle.remove();
                        circle = null;
                    }
                    break;
                case Globals.GET_LOCATION_RESULT:
                case Globals.LOCATION_UPDATE:
                    LatLng latLong = new LatLng(vds.latitude, vds.longitude);
                    long ts=vds.locationTs;

                    mMarker.setPosition(latLong);
                    mMarker.setSnippet(getString(R.string.monitor_map_marker_snippet)+" "+MonitorActivity.tsToStr(ts));
                    mMap.animateCamera(CameraUpdateFactory.newLatLng(latLong));

                    if(mMarker.isInfoWindowShown())
                    {
                        mMarker.hideInfoWindow();
                        mMarker.showInfoWindow();
                    }
                    break;
                case Globals.PARK:
                    circle = mMap.addCircle(new CircleOptions()
                            .center(new LatLng(vds.latitude_park, vds.longitude_park))
                            .radius(vds.parkingStatus)
                            .strokeColor(Color.RED)
                            .fillColor(Color.TRANSPARENT));
            }
        }
    }
}

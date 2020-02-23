package es.gpsou.vehiclealarm;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.BatteryManager;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.firebase.functions.FirebaseFunctions;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import static android.content.Context.NOTIFICATION_SERVICE;

/**
 * Created by Pedro on 15/04/2017.
 */

public class LocationSupport implements
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    public static long parkDetectionPeriod;

    public static final String RESULT_OK = "Ok";
    public static final String PERMISSION_ERROR = "PermissionError";
    public static final String ABSENT_DATA = "AbsentData";
    public static final String GOOGLE_API_CONNECTION_ERROR = "GoogleApiConnectionError";

    private static LocationSupport instance=null;
    private static Context context;

    private static boolean pendingSingleLocation = false;
    private static boolean parkDetectionRequest = false;
    private static boolean trackingActive = false;
    private static boolean geofenceActive = false;

    private PendingIntent geofenceIntent=null;
    private PendingIntent locUpdateIntent=null;
    private PendingIntent savedLocUpdateIntent=null;

    private Location prevLocation=null;
    private Location mostRecentLocation=null;
    private Location parkLocation=null;

    private static GoogleApiClient mGoogleApiClient = null;

    public static LocationSupport getLocationSupport() {
        if (instance==null)
            instance = new LocationSupport();

        return instance;
    }

    public static void init(Context c) {
        context=c;

        SharedPreferences settings=context.getSharedPreferences(Globals.CONFIGURACION, 0);
        try {
            parkDetectionPeriod=Long.parseLong(settings.getString(context.getString(R.string.settings_location_time), "300"));
        } catch(NumberFormatException e) {
            parkDetectionPeriod=300;
        }
    }

    public void opGetLocation(Context c) {
        if (context == null)
            init(c);

        pendingSingleLocation = true;
        startLocation();
    }

    public void opActivateTracking(Context c) {
        if (context == null)
            init(c);

        trackingActive = true;
        startLocation();
    }

    public void opDeactivateTracking(Context c) {

        if (context == null)
            init(c);

        trackingActive = false;

        if(locUpdateIntent==null) {
            Intent intent = new Intent(context, LocationEventsService.class);
            savedLocUpdateIntent = PendingIntent.getService(context, Globals.LOCATION_UPDATE_INTENT_REQUEST_CODE, intent, PendingIntent.FLAG_CANCEL_CURRENT);
        }

        if(mGoogleApiClient!=null) {
            if(mGoogleApiClient.isConnected()) {
                LocationServices.FusedLocationApi.removeLocationUpdates(
                        mGoogleApiClient, locUpdateIntent);
                locUpdateIntent=null;
                if(geofenceIntent==null)
                    mGoogleApiClient.disconnect();
            }
        }
    }

    public void opActivateGeofence(Context c) {
        if (context == null)
            init(c);

        geofenceActive=true;

        startLocation();
    }

    public void opDeactivateGeofence(Context c) {
        if (context == null)
            init(c);
        geofenceActive = false;

        ArrayList<String> requestIds=new ArrayList<>(1);
        requestIds.add(Globals.GEOFENCE_ID);

        if(mGoogleApiClient!=null) {
            if(mGoogleApiClient.isConnected()) {
                LocationServices.GeofencingApi.removeGeofences(
                        mGoogleApiClient,
                        requestIds
                ).setResultCallback(new ResultCallback<Status>() {
                    @Override
                    public void onResult(@NonNull Status status) {
                        if(status.isSuccess()) {
                            Log.d(Globals.TAG, "Geofence desactivada");
                            CancelNotification();

                            if(geofenceIntent!=null)
                                geofenceIntent.cancel();
                            geofenceIntent=null;
                            if(locUpdateIntent==null)
                                mGoogleApiClient.disconnect();
                        } else {
                            Log.d(Globals.TAG, "ERROR al desactivar geofence");
                        }
                    }
                }); // Result processed in onResult().

            }
        }
    }

    public void locationUpdated(Context c, Location location) {
        if(context==null)
            init(c);

        mostRecentLocation=location;

        returnLocation(location, Globals.P2P_OP_LOCATION_UPDATE, false);
    }

    public void geofenceTransitionEvent(Context c, GeofencingEvent e) {
        if(context==null)
            init(c);

        double latitude=0.0d, longitude=0.0d;
        long ts=System.currentTimeMillis();

        Location location=e.getTriggeringLocation();
        if(location!=null) {
            latitude = location.getLatitude();
            longitude = location.getLongitude();
            ts = location.getTime();

            if (mostRecentLocation == null)
                mostRecentLocation = location;
            else if (mostRecentLocation.getTime() < ts)
                mostRecentLocation = location;
        }

        opDeactivateGeofence(context);

        opActivateTracking(context);
        ParkDetectionIntentService.startParkDetection(context);

        int batteryPct=getBatteryLevel();

        SharedPreferences settings=context.getSharedPreferences(Globals.CONFIGURACION, 0);
        String to=settings.getString(Globals.REMOTE_FB_REGISTRATION_ID, null);

/*        FirebaseMessaging fm = FirebaseMessaging.getInstance();
        String id = Integer.toString(Globals.msgId.incrementAndGet());
        fm.send(new RemoteMessage.Builder(to)
                .setMessageId(id)
                .addData(Globals.P2P_DEST, Globals.P2P_DEST_MONITOR)
                .addData(Globals.P2P_OP, Globals.P2P_OP_GEOFENCING_ALERT)
                .addData(Globals.P2P_LATITUDE, String.valueOf(latitude))
                .addData(Globals.P2P_LONGITUDE, String.valueOf(longitude))
                .addData(Globals.P2P_TIMESTAMP, String.valueOf(ts))
                .addData(Globals.P2P_BATTERY, String.valueOf(batteryPct))
                .setTtl(3600)
                .build());
*/
        FirebaseFunctions mFunctions = FirebaseFunctions.getInstance("europe-west1");
        JSONObject data=new JSONObject();
        try {
            data.put(Globals.P2P_TO, to);
            data.put(Globals.P2P_TTL, "3600");
            data.put(Globals.P2P_DEST, Globals.P2P_DEST_MONITOR);
            data.put(Globals.P2P_OP, Globals.P2P_OP_GEOFENCING_ALERT);
            data.put(Globals.P2P_LATITUDE, String.valueOf(latitude));
            data.put(Globals.P2P_LONGITUDE, String.valueOf(longitude));
            data.put(Globals.P2P_TIMESTAMP, String.valueOf(ts));
            data.put(Globals.P2P_BATTERY, String.valueOf(batteryPct));
        } catch (JSONException ex) {
            return;
        }
        mFunctions.getHttpsCallable("sendMessage").call(data);

    }

    public void parkDetectionCheck(Context c) {
        if(context==null)
            init(c);

        pendingSingleLocation=true;
        parkDetectionRequest =true;
        startLocation();
    }

    private void startLocation() {
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(context)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
            mGoogleApiClient.connect();
        } else if (mGoogleApiClient.isConnected()) {
            if (pendingSingleLocation)
                returnSingleLocation();
            if(trackingActive)
                startLocationUpdates();
            if(geofenceActive)
                buildGeofence();
        } else if (!mGoogleApiClient.isConnecting()) {
            mGoogleApiClient.connect();
        }
    }

    private void returnSingleLocation() {
        Location mLastLocation = null;
        boolean permissionFailed = false;

        if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionFailed=true;
        } else {
            mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
                    mGoogleApiClient);
        }

        if(mostRecentLocation!=null) {
            if(mLastLocation.getTime() < mostRecentLocation.getTime())
                mLastLocation=mostRecentLocation;
        }
        mostRecentLocation=mLastLocation;

        pendingSingleLocation=false;

        if(parkDetectionRequest)
            parkDetection(mLastLocation);
        else
            returnLocation(mLastLocation, Globals.P2P_OP_GET_LOCATION_RESULT, permissionFailed);

    }

    private void parkDetection(Location loc) {
        parkDetectionRequest=false;

        if(loc!=null) {
            if(prevLocation!=null) {
                if(loc.distanceTo(prevLocation) < Globals.PARK_RADIUS &&
                        loc.getTime() - prevLocation.getTime() > parkDetectionPeriod/2) {
                    Log.d(Globals.TAG, "Parada detectada");

                    parkLocation=loc;

                    ParkDetectionIntentService.cancelParkDetection(context);

                    opActivateGeofence(context);

                    returnLocation(loc, Globals.P2P_OP_PARK, false);
                }
            }
            prevLocation=loc;
        }
    }

    private void returnLocation(Location location, String operation, boolean permissionFailled) {
/*        FirebaseMessaging fm = FirebaseMessaging.getInstance(); */
        FirebaseFunctions mFunctions = FirebaseFunctions.getInstance("europe-west1");
        SharedPreferences settings=context.getSharedPreferences(Globals.CONFIGURACION, 0);
        String to=settings.getString(Globals.REMOTE_FB_REGISTRATION_ID, null);

        int batteryPct=getBatteryLevel();
        int parkingStatus=getParkingStatus();

//        String operation=updateLocation?Globals.P2P_OP_LOCATION_UPDATE:Globals.P2P_OP_GET_LOCATION_RESULT;

/*        String id = Integer.toString(Globals.msgId.incrementAndGet());
        RemoteMessage.Builder mRemoteMessage=new RemoteMessage.Builder(to);
        mRemoteMessage.setMessageId(id)
                .addData(Globals.P2P_DEST, Globals.P2P_DEST_MONITOR)
                .addData(Globals.P2P_OP, operation)
                .setTtl(3600);
*/
        JSONObject data=new JSONObject();
        try {
            data.put(Globals.P2P_TO, to);
            data.put(Globals.P2P_TTL, "3600");
            data.put(Globals.P2P_DEST, Globals.P2P_DEST_MONITOR);
            data.put(Globals.P2P_OP, operation);


            if(permissionFailled) {
                /*            mRemoteMessage.addData(Globals.P2P_RESULT, PERMISSION_ERROR); */
                data.put(Globals.P2P_RESULT, PERMISSION_ERROR);
                trackingActive=false;
            } else if (location != null || prevLocation != null) {
                if(location==null)
                    location=prevLocation;

/*            mRemoteMessage.addData(Globals.P2P_RESULT, RESULT_OK);
            mRemoteMessage.addData(Globals.P2P_LATITUDE, String.valueOf(location.getLatitude()));
            mRemoteMessage.addData(Globals.P2P_LONGITUDE, String.valueOf(location.getLongitude()));
            mRemoteMessage.addData(Globals.P2P_BATTERY, String.valueOf(batteryPct));
            mRemoteMessage.addData(Globals.P2P_PARKING, String.valueOf(parkingStatus));
            mRemoteMessage.addData(Globals.P2P_TIMESTAMP, String.valueOf(location.getTime()));
*/
                data.put(Globals.P2P_RESULT, RESULT_OK);
                data.put(Globals.P2P_LATITUDE, String.valueOf(location.getLatitude()));
                data.put(Globals.P2P_LONGITUDE, String.valueOf(location.getLongitude()));
                data.put(Globals.P2P_BATTERY, String.valueOf(batteryPct));
                data.put(Globals.P2P_PARKING, String.valueOf(parkingStatus));
                data.put(Globals.P2P_TIMESTAMP, String.valueOf(location.getTime()));
                if(parkingStatus > 0) {
/*                mRemoteMessage.addData(Globals.P2P_LATITUDE_PARK, String.valueOf(parkLocation.getLatitude()));
                mRemoteMessage.addData(Globals.P2P_LONGITUDE_PARK, String.valueOf(parkLocation.getLongitude()));
*/
                    data.put(Globals.P2P_LATITUDE_PARK, String.valueOf(parkLocation.getLatitude()));
                    data.put(Globals.P2P_LONGITUDE_PARK, String.valueOf(parkLocation.getLongitude()));

                }
            } else {
/*            mRemoteMessage.addData(Globals.P2P_RESULT, ABSENT_DATA);
            mRemoteMessage.addData(Globals.P2P_BATTERY, String.valueOf(batteryPct));
*/
                data.put(Globals.P2P_RESULT, ABSENT_DATA);
                data.put(Globals.P2P_BATTERY, String.valueOf(batteryPct));
            }
        } catch (JSONException e) {
            return;
        }
/*        fm.send(mRemoteMessage.build()); */
        mFunctions.getHttpsCallable("sendMessage").call(data);
    }

    private void buildGeofence() {
        if(geofenceIntent!=null)
            return;

        final Location mLastLocation=mostRecentLocation;

        if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            SharedPreferences settings=context.getSharedPreferences(Globals.CONFIGURACION, 0);
            float radius=(float)settings.getInt(context.getString(R.string.settings_location_radius), 150);

//            mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
//                    mGoogleApiClient);

            Geofence mGeofence=new Geofence.Builder()
                    .setRequestId(Globals.GEOFENCE_ID)
                    .setCircularRegion(
                            mLastLocation.getLatitude(),
                            mLastLocation.getLongitude(),
                            radius
                    )
                    .setExpirationDuration(Geofence.NEVER_EXPIRE)
                    .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_EXIT)
                    .build();

            GeofencingRequest mGeofencingRequest=new GeofencingRequest.Builder()
                    .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_EXIT)
                    .addGeofence(mGeofence)
                    .build();


            Intent intent = new Intent(context, LocationEventsService.class);
            geofenceIntent=PendingIntent.getService(context, Globals.GEOFENCE_INTENT_REQUEST_CODE, intent, PendingIntent.
                    FLAG_UPDATE_CURRENT);

            LocationServices.GeofencingApi.addGeofences(
                    mGoogleApiClient,
                    mGeofencingRequest,
                    geofenceIntent
            ).setResultCallback(new ResultCallback<Status>() {
                @Override
                public void onResult(@NonNull Status status) {
                    if(status.isSuccess()) {
                        Log.d(Globals.TAG, "Geofence activada");
                        ShowNotification(mLastLocation);
                    } else {
                        Log.d(Globals.TAG, "ERROR al activar geofence");
                        cancelGeofence();
                    }
                }
            });

        }
    }

    static void CancelNotification() {
        int mNotificationId = 001;

        NotificationManager mNotifyMgr =
                (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
        mNotifyMgr.cancel(mNotificationId);
    }

    static void ShowNotification(Location loc) {
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(context)
                        .setSmallIcon(R.drawable.ic_stat_ic_notification)
                        .setContentTitle("Geofence")
                        .setContentText("Geofence activa");

        Intent resultIntent = new Intent(context, MapActivity.class);
        resultIntent.putExtra("location", loc);

        PendingIntent resultPendingIntent =
                PendingIntent.getActivity(
                        context,
                        0,
                        resultIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );

        mBuilder.setContentIntent(resultPendingIntent);

        int mNotificationId = 001;
        NotificationManager mNotifyMgr =
                (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
        mNotifyMgr.notify(mNotificationId, mBuilder.build());
    }

    private void cancelGeofence() {
        geofenceActive=false;

        if(geofenceIntent!=null)
            geofenceIntent.cancel();
        geofenceIntent=null;

        if(locUpdateIntent==null)
            mGoogleApiClient.disconnect();
    }

    private void startLocationUpdates() {
        if(locUpdateIntent!=null)
            return;

        if(savedLocUpdateIntent==null) {
            Intent intent = new Intent(context, LocationEventsService.class);
            savedLocUpdateIntent = PendingIntent.getService(context, Globals.LOCATION_UPDATE_INTENT_REQUEST_CODE, intent, PendingIntent.
                    FLAG_UPDATE_CURRENT);
        }
        locUpdateIntent=savedLocUpdateIntent;

        final LocationRequest mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(Globals.LOCATION_UPDATE_INTERVAL);
        mLocationRequest.setFastestInterval(Globals.LOCATION_UPDATE_INTERVAL);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(mLocationRequest);

        PendingResult<LocationSettingsResult> result =
                LocationServices.SettingsApi.checkLocationSettings(mGoogleApiClient,
                        builder.build());

        result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
            @Override
            public void onResult(LocationSettingsResult result) {
                final Status status = result.getStatus();
                switch (status.getStatusCode()) {
                    case LocationSettingsStatusCodes.SUCCESS:
                        try {
                            LocationServices.FusedLocationApi.requestLocationUpdates(
                                    mGoogleApiClient, mLocationRequest, locUpdateIntent);

                            returnLocation(null, Globals.P2P_OP_LOCATION_UPDATE, false);
                        } catch (SecurityException e) {
                            cancelLocationUpdates();
                        }
                        break;
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        PendingIntent pI = status.getResolution();
                        mGoogleApiClient.getContext().startActivity(new Intent(mGoogleApiClient.getContext(), VehicleActivity.class)
                                .putExtra(Globals.RESOLUTION_REQUIRED, pI).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                        cancelLocationUpdates();
                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        // Location settings are not satisfied. However, we have no way
                        // to fix the settings so we won't show the dialog.
                        cancelLocationUpdates();

                        break;
                    default:
                        cancelLocationUpdates();
                }
            }
        });
    }

    private void cancelLocationUpdates() {
        trackingActive=false;
        locUpdateIntent=null;
        returnLocation(null, Globals.P2P_OP_LOCATION_UPDATE, true);
        if(geofenceIntent==null)
            mGoogleApiClient.disconnect();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.d(Globals.TAG, "OnConnected triggered");

        if(pendingSingleLocation)
            returnSingleLocation();

        if(trackingActive) {
            startLocationUpdates();
            return;
        }
        if(geofenceActive) {
            buildGeofence();
            return;
        }
        mGoogleApiClient.disconnect();

    }

    public int getBatteryLevel() {
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = context.registerReceiver(null, ifilter);
        int batteryPct=-1;
        if(batteryStatus!=null) {
            int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            batteryPct = (int) (100.0f * level / (float) scale);
        }
        return(batteryPct);
    }

    public int getParkingStatus() {
        if(geofenceActive) {
            SharedPreferences settings=context.getSharedPreferences(Globals.CONFIGURACION, 0);
            int radius=settings.getInt(context.getString(R.string.settings_location_radius), 0);
            return radius;
        } else
            return 0;
    }

//    @Override
//    public void onLocationChanged(Location location) {
//        returnLocation(location, true, false);
//    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(Globals.TAG, "OnConnectionSuspended triggered");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.d(Globals.TAG, "OnConnectionFailed triggered");
/*        FirebaseMessaging fm = FirebaseMessaging.getInstance(); */
        FirebaseFunctions mFunctions = FirebaseFunctions.getInstance("europe-west1");
        SharedPreferences settings=context.getSharedPreferences(Globals.CONFIGURACION, 0);
        String to=settings.getString(Globals.REMOTE_FB_REGISTRATION_ID, null);

/*        String id = Integer.toString(Globals.msgId.incrementAndGet());
        fm.send(new RemoteMessage.Builder(to)
                .setMessageId(id)
                .addData(Globals.P2P_DEST, Globals.P2P_DEST_MONITOR)
                .addData(Globals.P2P_OP, Globals.P2P_OP_GET_LOCATION_RESULT)
                .addData(Globals.P2P_RESULT, GOOGLE_API_CONNECTION_ERROR)
                .setTtl(3600)
                .build());
*/
        JSONObject data=new JSONObject();
        try {
            data.put(Globals.P2P_TO, to);
            data.put(Globals.P2P_TTL, "3600");
            data.put(Globals.P2P_DEST, Globals.P2P_DEST_MONITOR);
            data.put(Globals.P2P_OP, Globals.P2P_OP_GET_LOCATION_RESULT);
            data.put(Globals.P2P_RESULT, GOOGLE_API_CONNECTION_ERROR);
        } catch (JSONException e) {
            return;
        }
        mFunctions.getHttpsCallable("sendMessage").call(data);
    }

}

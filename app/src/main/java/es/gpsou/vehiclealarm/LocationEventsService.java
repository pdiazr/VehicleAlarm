package es.gpsou.vehiclealarm;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;
import com.google.android.gms.location.LocationResult;


public class LocationEventsService extends IntentService {

    public LocationEventsService() {
        super("LocationEventsService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            LocationResult locationResult=LocationResult.extractResult(intent);
            if(locationResult!=null) {
                LocationSupport ls=LocationSupport.getLocationSupport();
                ls.locationUpdated(getApplicationContext(), locationResult.getLastLocation());
                return;
            }

            GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
            if (!geofencingEvent.hasError()) {

                int geofenceTransition = geofencingEvent.getGeofenceTransition();

                if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {
                    LocationSupport ls=LocationSupport.getLocationSupport();
                    ls.geofenceTransitionEvent(getApplicationContext(), geofencingEvent);
                } else {
                    Log.d(Globals.TAG, "Geofence transition invalid");
                }
            }
        }
    }


}

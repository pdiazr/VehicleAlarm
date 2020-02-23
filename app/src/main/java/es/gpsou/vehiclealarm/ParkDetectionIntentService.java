package es.gpsou.vehiclealarm;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.util.Log;

import static es.gpsou.vehiclealarm.Globals.TAG;


public class ParkDetectionIntentService extends IntentService {

    static PendingIntent alarmIntent=null;

    public ParkDetectionIntentService() {
        super("ParkDetectionIntentService");

        Log.d(TAG, "NEW ParkDetectionIntentService service created!!!!!");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.d(TAG, "Stop detection checking");

        if (intent != null) {
            LocationSupport ls=LocationSupport.getLocationSupport();
            ls.parkDetectionCheck(getApplicationContext());
        }
    }

    public static void startParkDetection(Context context) {
        long parkDetectionPeriod;

        if(alarmIntent==null) {
            SharedPreferences settings=context.getSharedPreferences(Globals.CONFIGURACION, 0);
            try {
                parkDetectionPeriod = Long.parseLong(settings.getString(context.getString(R.string.settings_location_time), "300"));
            }catch(NumberFormatException e) {
                parkDetectionPeriod=300;
            }

            AlarmManager alarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

            Intent intent = new Intent(context, ParkDetectionIntentService.class);
            alarmIntent = PendingIntent.getService(context, 0, intent, 0);

            alarmMgr.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    SystemClock.elapsedRealtime() + 1000 * parkDetectionPeriod,
                    1000 * parkDetectionPeriod, alarmIntent);
        }
    }

    public static void cancelParkDetection(Context context) {
        AlarmManager alarmMgr=(AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        if (alarmMgr!= null && alarmIntent != null) {
            alarmMgr.cancel(alarmIntent);
        }
        alarmIntent=null;
    }
}

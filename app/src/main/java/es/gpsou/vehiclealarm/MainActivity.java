package es.gpsou.vehiclealarm;

import android.content.Intent;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Intent intent=null;

        PreferenceManager.setDefaultValues(this, Globals.CONFIGURACION, MODE_PRIVATE, R.xml.vehicle_preferences, false);
        SharedPreferences settings=getSharedPreferences(Globals.CONFIGURACION, 0);


//        Log.d(Globals.TAG, RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_NOTIFICATION).toString());
//        Log.d(Globals.TAG, RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_ALARM).toString());

        if(settings.getString(getString(R.string.settings_sensor_tone),null)==null) {

            Uri tone=null;
            SharedPreferences.Editor editor = settings.edit();

            tone=RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_NOTIFICATION);
            if(tone != null)
                editor.putString(getString(R.string.settings_sensor_tone), tone.toString());

            tone=RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_ALARM);
            if(tone != null)
                editor.putString(getString(R.string.settings_location_tone), tone.toString());

            editor.apply();
        }

        String appMode=settings.getString(Globals.APP_MODE, Globals.NULL);

        LocationSupport.getLocationSupport().init(getApplicationContext());

        Globals.PACKAGE_NAME=getApplication().getPackageName();

        if (appMode.compareTo(Globals.IN_VEHICLE_MODE)==0) {
            intent=new Intent(this, VehicleActivity.class);
        } else if(appMode.compareTo(Globals.MONITORING_MODE)==0) {
            intent=new Intent(this, MonitorActivity.class);
        } else {
            intent = new Intent(this, ConfigureActivity.class);
        }
        startActivity(intent);
    }
}

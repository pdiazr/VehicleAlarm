package es.gpsou.vehiclealarm;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Intent intent=null;

        PreferenceManager.setDefaultValues(this, Globals.CONFIGURACION, MODE_PRIVATE, R.xml.vehicle_preferences, false);
        SharedPreferences settings=getSharedPreferences(Globals.CONFIGURACION, 0);


        createNotificationChannel(getString(R.string.channel_id_event), getString(R.string.channel_name_event), getString(R.string.channel_description_event));
        createNotificationChannel(getString(R.string.channel_id_task), getString(R.string.channel_name_tasl), getString(R.string.channel_description_task));
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

    private void createNotificationChannel(String CHANNEL_ID, String channel_name, String channel_description) {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = channel_name;
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(channel_description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

}

package es.gpsou.vehiclealarm;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;

/**
 * Created by Pedro on 28/05/2017.
 */

public class PlayAlert extends Thread implements MediaPlayer.OnCompletionListener {
    Uri uri=null;
    Context context=null;
    int type=0;
    static MediaPlayer alarmMp;

    PlayAlert(Context c) {
        uri=null;
        context=c;
    }

    PlayAlert(Context c, int t) {
        context=c;
        type=t;

        SharedPreferences settings=context.getSharedPreferences(Globals.CONFIGURACION, 0);

        switch (t) {
            case RingtoneManager.TYPE_NOTIFICATION:
                uri=Uri.parse(settings.getString(context.getString(R.string.settings_sensor_tone), ""));
                break;
            case RingtoneManager.TYPE_ALARM:
                uri=Uri.parse(settings.getString(context.getString(R.string.settings_location_tone), ""));
        }
    }

    @Override
    public void run() {

        if(uri==null)
            uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        MediaPlayer mp=null;

        if(uri != null)
            mp = MediaPlayer.create(context.getApplicationContext(), uri);
        else
            mp = MediaPlayer.create(context.getApplicationContext(), R.raw.defaulttone);

        mp.setOnCompletionListener(this);

        if(type==RingtoneManager.TYPE_ALARM) {
            if(alarmMp!=null)
                alarmMp.stop();
            alarmMp=mp;
        }

        mp.start();

    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        if(type==RingtoneManager.TYPE_ALARM)
            alarmMp=null;

        mp.release();
    }

    public static void stopAlarm() {
        if(alarmMp!=null) {
            alarmMp.stop();
            alarmMp = null;
        }
    }
}
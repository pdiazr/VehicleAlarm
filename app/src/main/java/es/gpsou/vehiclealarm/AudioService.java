package es.gpsou.vehiclealarm;

import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import androidx.core.app.NotificationCompat;


public class AudioService extends IntentService {
    public static final String ACTION_SEND = "es.gpsou.vehiclealarm.action.SEND";
    public static final String ACTION_RECEIVE = "es.gpsou.vehiclealarm.action.RECEIVE";

    public static final String REMOTE_RECEIVED = "es.gpsou.vehiclealarm.extra.REMOTE_RECEIVED";

    public AudioService() {
        super("AudioService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            String remoteReceived=intent.getStringExtra(AudioService.REMOTE_RECEIVED);

            if(remoteReceived!=null) {
                final String action = intent.getAction();
                Intent appIntent=null;

                SharedPreferences settings=getSharedPreferences(Globals.CONFIGURACION, 0);
                String appMode=settings.getString(Globals.APP_MODE, Globals.NULL);
                if (appMode.compareTo(Globals.IN_VEHICLE_MODE)==0) {
                    appIntent=new Intent(this, VehicleActivity.class);
                } else if(appMode.compareTo(Globals.MONITORING_MODE)==0) {
                    appIntent=new Intent(this, MonitorActivity.class);
                } else {
                    return;
                }

                PendingIntent pI=PendingIntent.getActivity(this, 1, appIntent, 0);

                NotificationCompat.Builder mBuilder =
                        new NotificationCompat.Builder(getApplicationContext())
                                .setSmallIcon(R.drawable.ic_audio_on)
                                .setContentTitle("VehicleAlarm")
                                .setContentText("Canal de audio activo")
                                .setContentIntent(pI);

                startForeground(Globals.AUDIO_ACTIVATED, mBuilder.build());

                UDPLinkMgr mUDPLinkMgr = UDPLinkMgr.getInstance();
                if (ACTION_SEND.equals(action)) {
                    mUDPLinkMgr.sendAudio(remoteReceived);
                } else if (ACTION_RECEIVE.equals(action)) {
                    mUDPLinkMgr.receiveAudio(this, remoteReceived);
                }

                stopForeground(true);
            }
        }
    }
}

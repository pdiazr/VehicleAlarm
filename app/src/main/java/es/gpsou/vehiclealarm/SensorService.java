package es.gpsou.vehiclealarm;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.SystemClock;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Date;

import static android.app.PendingIntent.getService;

public class SensorService extends Service {

    private final static String ALARM_RESTART_SERVICE_DIED="AlarmRestartServiceDied";
    private final IBinder mBinder = new LocalBinder();

    private static boolean sensorMonitoringActivated=false;
    private static SensorEventListener sensorEventListener=null;
    private static WakeSoC wakeSoC=null;
    private static PowerManager.WakeLock wakeLock=null;
    private static SensorManager mSensorManager;
    private static Sensor mSensor;
    private static float lastX=0, lastY=0, lastZ=0;
    private static float sensitivity=100.0f;
    private static long sendTs;

    public SensorService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if(sensorMonitoringActivated) {
            sensorMonitoringActivated=false;

            stopForeground(true);

            try {
                getApplicationContext().unregisterReceiver(wakeSoC);
                Log.d(Globals.TAG, "Unregistering wakeSoC receiver");
            } catch (IllegalArgumentException e) {
                Log.d(Globals.TAG, "Broadcast receiver not registered");
            }

            mSensorManager.unregisterListener(sensorEventListener);
            if (wakeLock.isHeld()) {
                wakeLock.release();
            }

            Log.d(Globals.TAG, "Sensor de movimiento desactivado");

        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if(intent!=null && intent.getBooleanExtra(Globals.ACTIVATE_SENSOR, false)) {
            if (intent.getBooleanExtra(ALARM_RESTART_SERVICE_DIED, false)) {
                if (sensorMonitoringActivated) {
                    Log.w(Globals.TAG, "SensorService already running");
                    return START_STICKY;
                }

                Log.w(Globals.TAG, "Reinicio forzado del SensorService");
            }

            sensorMonitoringActivated = true;

            mSensorManager = (SensorManager) getApplicationContext().getSystemService(SENSOR_SERVICE);
            mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

            if (sensorEventListener == null)
                sensorEventListener = new SensortEventListener();

            PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
            if (wakeLock == null)
                wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, Globals.TAG);

            if (wakeSoC == null)
                wakeSoC = new WakeSoC();


            SharedPreferences settings = this.getSharedPreferences(Globals.CONFIGURACION, 0);
            sensitivity = (float) Math.pow(2.0d, ((double) settings.getInt(this.getString(R.string.settings_sensor_sensitivity), 100) - 30) / 10.0f);

            IntentFilter filter = new IntentFilter();
            filter.addAction("android.intent.action.SCREEN_OFF");
            getApplicationContext().registerReceiver(wakeSoC, filter, "com.google.android.c2dm.permission.SEND", null);

            wakeLock.acquire();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                mSensorManager.registerListener(sensorEventListener, mSensor, SensorManager.SENSOR_DELAY_NORMAL, 5000000);
            } else {
                mSensorManager.registerListener(sensorEventListener, mSensor, SensorManager.SENSOR_DELAY_NORMAL);
            }
            sendTs = new Date().getTime();

            Log.d(Globals.TAG, "Sensor de movimiento activado");
        }


        intent=new Intent(this, VehicleActivity.class);
        PendingIntent pI=PendingIntent.getActivity(this, 1, intent, 0);

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(getApplicationContext())
                        .setSmallIcon(R.drawable.ic_stat_ic_notification    )
                        .setContentTitle("VehicleAlarm")
                        .setContentText("Monitorización activada")
                        .setContentIntent(pI);

        startForeground(Globals.MONITORING_ACTIVATED, mBuilder.build());

/*            intent=new Intent(this, SensorService.class);
            PendingIntent pi=getService(this, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
            AlarmManager am = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
            am.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime()+1800000L, pi);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent=new Intent(getApplicationContext(), SensorService.class);
                startService(intent);
            }
        }, 2000); */

        // If we get killed, after returning from here, restart
        return START_STICKY;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);

        Log.w(Globals.TAG, "Sensor service is dieing");

        Intent intent=new Intent(this, SensorService.class);
        intent.putExtra(ALARM_RESTART_SERVICE_DIED, true);
        intent.putExtra(Globals.ACTIVATE_SENSOR, sensorMonitoringActivated);
        PendingIntent pi=getService(this, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
        AlarmManager am = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
        am.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + 5000, pi);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public boolean isSensorMonitoringActivated() {
        return sensorMonitoringActivated;
    }

    private class SensortEventListener implements SensorEventListener {
        @Override
        public void onSensorChanged(SensorEvent event) {

            float accX, accY, accZ;
            accX = event.values[0];
            accY = event.values[1];
            accZ = event.values[2];

            float diff = Math.abs(accX - lastX) + Math.abs(accY - lastY) + Math.abs(accZ - lastZ);
            lastX = accX;
            lastY = accY;
            lastZ = accZ;

            long ts = new Date().getTime();
            if ((diff > sensitivity) && (ts - sendTs > Globals.SENSOR_UPDATE_TIME)) {
                Log.d(Globals.TAG, "Envío de alarma de movimiento al monitor: " + String.valueOf(diff) + "-" + sensitivity);

                sendTs = ts;
                FirebaseMessaging fm = FirebaseMessaging.getInstance();
                SharedPreferences settings = getSharedPreferences(Globals.CONFIGURACION, 0);
                String to = settings.getString(Globals.FB_GROUP_ID, null);

                String id = Integer.toString(Globals.msgId.incrementAndGet());
                fm.send(new RemoteMessage.Builder(to)
                        .setMessageId(id)
                        .addData(Globals.P2P_DEST, Globals.P2P_DEST_MONITOR)
                        .addData(Globals.P2P_OP, Globals.P2P_OP_SENSOR_ALARM)
                        .addData(Globals.P2P_TIMESTAMP, String.valueOf(new Date().getTime()))
                        .setTtl(3600)
                        .build());
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    }

    public class WakeSoC extends BroadcastReceiver {
        public WakeSoC() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(Globals.TAG, "Screen off broadcast message received");

            final long SCREEN_OFF_RECEIVER_DELAY = 1000;

            Runnable runnable = new Runnable() {
                public void run() {
                    Log.d(Globals.TAG, "Reseteando sensor Listener.");
                    mSensorManager.unregisterListener(sensorEventListener);
                    mSensorManager.registerListener(sensorEventListener, mSensor, SensorManager.SENSOR_DELAY_NORMAL);
                }
            };

            new Handler().postDelayed(runnable, SCREEN_OFF_RECEIVER_DELAY);

        }
    }

    public class LocalBinder extends Binder {
        SensorService getService() {
            return SensorService.this;
        }
    }

}


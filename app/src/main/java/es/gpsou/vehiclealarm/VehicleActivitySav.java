package es.gpsou.vehiclealarm;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.graphics.drawable.TransitionDrawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.firebase.functions.FirebaseFunctions;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

public class VehicleActivitySav extends AppCompatActivity {
    private static final String STATE="STATE";

    private static SensorEventListener sel=null;
    private static wakeSoC ws=null;
    private static PowerManager.WakeLock wl=null;
    private static SensorManager mSensorManager;
    private static Sensor mSensor;
    private static float lastX=0, lastY=0, lastZ=0;
    private static boolean monitoringActivated=false;
    private static boolean sensorActivated=false;
    private static long sendTs;
    private static float sensitivity=100.0f;
    private static Menu menu=null;
    private static LocationSupport ls=null;

    @Override
    public boolean onCreateOptionsMenu(Menu m) {
        menu=m;

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.vehicle_menu, menu);

        if(monitoringActivated) {
            MenuItem item = menu.findItem(R.id.vechicle_menu_conf);
            item.setEnabled(false);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent=null;
        final SharedPreferences settings=getSharedPreferences(Globals.CONFIGURACION, 0);

        switch (item.getItemId()) {
            case R.id.vechicle_menu_conf:
                intent=new Intent(this, VehicleSettingsActivity.class);
                startActivity(intent);
                return true;
            case R.id.vehicle_menu_reset:
                AlertDialog.Builder builder = new AlertDialog.Builder(this);

                builder.setMessage(R.string.vehicle_dialog_reset);

                builder.setPositiveButton(R.string.vehicle_dialog_reset_ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        if(monitoringActivated)
                            switchMonitoring(null);

                        SharedPreferences.Editor editor=settings.edit();
                        editor.putString(Globals.APP_MODE, "");
                        editor.apply();
                        Intent intent=new Intent(VehicleActivitySav.this, MainActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                    }
                });
                builder.setNegativeButton(R.string.vehicle_dialog_reset_cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        return;
                    }
                });
                AlertDialog dialog = builder.create();
                dialog.show();

                return true;
            case R.id.vehicle_menu_test:
                String remoteFirebaseId=settings.getString(Globals.REMOTE_FB_REGISTRATION_ID, null);

                String to = remoteFirebaseId;
/*                FirebaseMessaging fm = FirebaseMessaging.getInstance();
                String id = Integer.toString(Globals.msgId.incrementAndGet());
                fm.send(new RemoteMessage.Builder(to)
                        .setMessageId(id)
                        .addData(Globals.P2P_DEST, Globals.P2P_DEST_MONITOR)
                        .addData(Globals.P2P_OP, Globals.P2P_OP_TEST)
                        .addData(Globals.P2P_TXT, getString(R.string.vehicle_test_string))
                        .setTtl(3600)
                        .build());
*/
                FirebaseFunctions mFunctions = FirebaseFunctions.getInstance("europe-west1");
                JSONObject data=new JSONObject();
                try {
                    data.put(Globals.P2P_TO, to);
                    data.put(Globals.P2P_TTL, "3600");
                    data.put(Globals.P2P_DEST, Globals.P2P_DEST_MONITOR);
                    data.put(Globals.P2P_OP, Globals.P2P_OP_TEST);
                    data.put(Globals.P2P_TXT, getString(R.string.vehicle_test_string));
                } catch (JSONException e) {
                    return true;
                }
                mFunctions.getHttpsCallable("sendMessage").call(data);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vehicle);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mSensorManager = (SensorManager) getApplicationContext().getSystemService(SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        ls=LocationSupport.getLocationSupport();

        if(savedInstanceState!=null) {
            monitoringActivated=savedInstanceState.getBoolean(STATE);
            Log.d(Globals.TAG, "Estado de monitorización: "+ monitoringActivated);

            ImageView image = (ImageView) findViewById(R.id.imageView);
            TransitionDrawable drawable = (TransitionDrawable) image.getDrawable();
            if(monitoringActivated) {
                drawable.startTransition(0);
            } else {
                drawable.resetTransition();
            }
        }

        if(sel==null)
            sel=new sensortEventListener();

        PowerManager pm=(PowerManager)getSystemService(POWER_SERVICE);
        if(wl==null)
            wl=pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, this.getLocalClassName());

        if(ws==null)
            ws=new wakeSoC();

        checkLocResolutionRequest(getIntent());
    }



    @Override
    protected void onSaveInstanceState(Bundle outState) {
        Log.d(Globals.TAG, "Salvando estado del VehicleActivity");
        outState.putBoolean(STATE, monitoringActivated);

        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        checkLocResolutionRequest(intent);

    }

    @Override
    public void onBackPressed() {
        if(monitoringActivated) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);

            builder.setMessage(R.string.vehicle_dialog_back);

            builder.setPositiveButton(R.string.vehicle_dialog_back_ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    switchMonitoring(null);
                    VehicleActivitySav.super.onBackPressed();
                }
            });
            builder.setNegativeButton(R.string.vehicle_dialog_back_cancel, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    return;
                }
            });
            AlertDialog dialog = builder.create();
            dialog.show();
        } else {
            super.onBackPressed();
        }
    }

    private void checkLocResolutionRequest(Intent intent) {
        PendingIntent pI = (PendingIntent) (intent.getParcelableExtra(Globals.RESOLUTION_REQUIRED));
        if(pI!=null) {
            try {
                startIntentSenderForResult(pI.getIntentSender(), 1, null, 0, 0, 0);
            } catch (IntentSender.SendIntentException e) {
                e.printStackTrace();
            }
        }
    }

    public void switchMonitoring(View v) {
        ImageView image = (ImageView) findViewById(R.id.imageView);
        TransitionDrawable drawable = (TransitionDrawable) image.getDrawable();
        MenuItem item=menu.findItem(R.id.vechicle_menu_conf);

        if(monitoringActivated) {
            try {
                getApplicationContext().unregisterReceiver(ws);
                Log.d(Globals.TAG, "Unregistering receiver");
            } catch (IllegalArgumentException e) {
                Log.d(Globals.TAG, "Broadcast receiver not registered");
            }

            mSensorManager.unregisterListener(sel);
            if(wl.isHeld()) {
                wl.release();
            }

            ParkDetectionIntentService.cancelParkDetection(getApplicationContext());
            ls.opDeactivateGeofence(this);
            ls.opDeactivateTracking(this);

            item.setEnabled(true);
            drawable.reverseTransition(500);
        } else {
            SharedPreferences settings=this.getSharedPreferences(Globals.CONFIGURACION, 0);
            sensitivity=(float) Math.pow(2.0d, ((double)settings.getInt(this.getString(R.string.settings_sensor_sensitivity), 100)-30)/10.0f);

            sensorActivated=settings.getBoolean(getString(R.string.settings_sensor_active), false);
            if(sensorActivated) {
                IntentFilter filter = new IntentFilter();
                filter.addAction("android.intent.action.SCREEN_OFF");
                getApplicationContext().registerReceiver(ws, filter, "com.google.android.c2dm.permission.SEND", null);

                mSensorManager.registerListener(sel, mSensor, SensorManager.SENSOR_DELAY_NORMAL);
                wl.acquire();
                sendTs = new Date().getTime();
            }

            if(settings.getBoolean(this.getString(R.string.settings_location_active), false)) {
                ParkDetectionIntentService.startParkDetection(getApplicationContext());
                Log.d(Globals.TAG, "Alarma de detección de parada activada");
            }

            item.setEnabled(false);
            drawable.startTransition(500);
        }
        monitoringActivated=!monitoringActivated;
    }



    private class sensortEventListener implements SensorEventListener {
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
                SharedPreferences settings = getSharedPreferences(Globals.CONFIGURACION, 0);
                String to = settings.getString(Globals.REMOTE_FB_REGISTRATION_ID, null);

/*                FirebaseMessaging fm = FirebaseMessaging.getInstance();
                String id = Integer.toString(Globals.msgId.incrementAndGet());
                fm.send(new RemoteMessage.Builder(to)
                        .setMessageId(id)
                        .addData(Globals.P2P_DEST, Globals.P2P_DEST_MONITOR)
                        .addData(Globals.P2P_OP, Globals.P2P_OP_SENSOR_ALARM)
                        .addData(Globals.P2P_TIMESTAMP, String.valueOf(new Date().getTime()))
                        .setTtl(3600)
                        .build());
*/
                FirebaseFunctions mFunctions = FirebaseFunctions.getInstance("europe-west1");
                JSONObject data=new JSONObject();
                try {
                    data.put(Globals.P2P_TO, to);
                    data.put(Globals.P2P_TTL, "3600");
                    data.put(Globals.P2P_DEST, Globals.P2P_DEST_MONITOR);
                    data.put(Globals.P2P_OP, Globals.P2P_OP_SENSOR_ALARM);
                    data.put(Globals.P2P_TIMESTAMP, String.valueOf(new Date().getTime()));
                } catch (JSONException e) {
                    return;
                }
                mFunctions.getHttpsCallable("sendMessage").call(data);
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    }

    public class wakeSoC extends BroadcastReceiver {
        public wakeSoC() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(Globals.TAG, "Screen off broadcast message received");
            if(monitoringActivated && sensorActivated) {
                final long SCREEN_OFF_RECEIVER_DELAY=1000;

                Runnable runnable = new Runnable() {
                    public void run() {
                        Log.d(Globals.TAG, "Reseteando sensor Listener.");
                        mSensorManager.unregisterListener(sel);
                        mSensorManager.registerListener(sel, mSensor, SensorManager.SENSOR_DELAY_NORMAL);
                    }
                };

                new Handler().postDelayed(runnable, SCREEN_OFF_RECEIVER_DELAY);
            }
        }
    }
}

package es.gpsou.vehiclealarm;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.drawable.TransitionDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.RemoteMessage;

public class VehicleActivity extends AppCompatActivity {

    private static boolean monitoringActivated=false;
    private static boolean sensorActivated=false;
    private static Menu menu=null;
    private static LocationSupport ls=null;
    public static boolean dummyBound=false;

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
                    public void onClick(DialogInterface dialog, int dialogId) {
                        if(monitoringActivated)
                            switchMonitoring(null);

                        SharedPreferences.Editor editor=settings.edit();
                        editor.putString(Globals.APP_MODE, Globals.NULL);
                        editor.apply();

                        FirebaseMessaging fm = FirebaseMessaging.getInstance();
                        String groupId=settings.getString(Globals.FB_GROUP_ID, null);
                        String registrationId=settings.getString(Globals.FB_REGISTRATION_ID, null);
                        String to = groupId;
                        String id = Integer.toString(Globals.msgId.incrementAndGet());
                        fm.send(new RemoteMessage.Builder(to)
                                .setMessageId(id)
                                .addData(Globals.P2P_DEST, Globals.P2P_DEST_MONITOR)
                                .addData(Globals.P2P_OP, Globals.P2P_OP_REMOVE_REGISTRATION_ID)
                                .addData(Globals.P2P_FB_REGISTRATION_ID, registrationId)
                                .setTtl(3600)
                                .build());

                        Intent intent=new Intent(VehicleActivity.this, MainActivity.class);
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
                FirebaseMessaging fm = FirebaseMessaging.getInstance();

                String groupId=settings.getString(Globals.FB_GROUP_ID, null);

                String to = groupId;
                String id = Integer.toString(Globals.msgId.incrementAndGet());
                fm.send(new RemoteMessage.Builder(to)
                        .setMessageId(id)
                        .addData(Globals.P2P_DEST, Globals.P2P_DEST_MONITOR)
                        .addData(Globals.P2P_OP, Globals.P2P_OP_TEST)
                        .addData(Globals.P2P_TXT, getString(R.string.vehicle_test_string))
                        .setTtl(3600)
                        .build());

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

        ls=LocationSupport.getLocationSupport();

        Intent intent = new Intent(this, SensorService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);

        checkIntentPayload(getIntent());
    }


    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        checkIntentPayload(intent);

    }

    @Override
    public void onBackPressed() {
        if(monitoringActivated) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);

            builder.setMessage(R.string.vehicle_dialog_back);

            builder.setPositiveButton(R.string.vehicle_dialog_back_ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    switchMonitoring(null);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                        finishAffinity();
                    }
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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                finishAffinity();
            }
        }
    }

    private void checkIntentPayload(Intent intent) {
        if(intent.getBooleanExtra(Globals.SWITCH_MONITORING, false)) {
            switchMonitoring(null);
            return;
        }

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
        MenuItem item = null;

        if(menu != null)
            item=menu.findItem(R.id.vechicle_menu_conf);

        if(monitoringActivated) {
            Intent intent=new Intent(this, SensorService.class);
            stopService(intent);
//            if(dummyBound)
//                unbindService(dummyConnection);

            ParkDetectionIntentService.cancelParkDetection(getApplicationContext());
            ls.opDeactivateGeofence(this);
            ls.opDeactivateTracking(this);

            drawable.reverseTransition(500);
        } else {
            SharedPreferences settings=this.getSharedPreferences(Globals.CONFIGURACION, 0);
            sensorActivated=settings.getBoolean(getString(R.string.settings_sensor_active), false);

            Intent intent=new Intent(this, SensorService.class);
            intent.putExtra(Globals.ACTIVATE_SENSOR, sensorActivated);
            startService(intent);
//                bindService(intent, dummyConnection, Context.BIND_AUTO_CREATE | Context.BIND_IMPORTANT);


            if(settings.getBoolean(this.getString(R.string.settings_location_active), false)) {
                ParkDetectionIntentService.startParkDetection(getApplicationContext());
                Log.d(Globals.TAG, "Alarma de detección de parada activada");
            }

            drawable.startTransition(500);
        }

        if(item != null)
            item.setEnabled(monitoringActivated);

        monitoringActivated=!monitoringActivated;

        SharedPreferences settings=getSharedPreferences(Globals.CONFIGURACION, 0);
        FirebaseMessaging fm = FirebaseMessaging.getInstance();

        String groupId=settings.getString(Globals.FB_GROUP_ID, null);

        String to = groupId;
        String id = Integer.toString(Globals.msgId.incrementAndGet());
        fm.send(new RemoteMessage.Builder(to)
                .setMessageId(id)
                .addData(Globals.P2P_DEST, Globals.P2P_DEST_MONITOR)
                .addData(Globals.P2P_OP, Globals.P2P_OP_SWITCH_MONITORING)
                .addData(Globals.P2P_MONITORING_ACTIVATED, monitoringActivated?Globals.TRUE:Globals.FALSE)
                .setTtl(3600)
                .build());
    }

    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            SensorService.LocalBinder binder = (SensorService.LocalBinder) service;
            SensorService mService = binder.getService();
//            monitoringActivated=mService.isSensorMonitoringActivated();

            ImageView image = (ImageView) findViewById(R.id.imageView);
            TransitionDrawable drawable = (TransitionDrawable) image.getDrawable();
            if(monitoringActivated) {
                drawable.startTransition(0);
            } else {
                drawable.resetTransition();
            }

            unbindService(this);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {

        }
    };

    private ServiceConnection dummyConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            dummyBound=true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            dummyBound=false;
        }
    };
}

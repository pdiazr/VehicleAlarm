package es.gpsou.vehiclealarm;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.RemoteMessage;

import java.text.SimpleDateFormat;
import java.util.Date;

public class MonitorActivity extends AppCompatActivity {

    private static final long DAY_DURATION=24 * 3600 * 1000;
    private static final String HOUR_FORMAT="HH:mm:ss";
    private static final String DAY_FORMAT="dd MMM";
    private UpdateUI updateUI=null;
    private static IntentFilter broadcastFilter;
    private static long cancelTrackingTs=0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_monitor);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent=new Intent(MonitorActivity.this, MonitorMapActivity.class);
                startActivity(intent);
            }
        });


        broadcastFilter = new IntentFilter();
        broadcastFilter.addAction(Globals.UPDATE_UI_INTENT_ACTION);

        updateUI=new UpdateUI();

        WebView webView=(WebView)findViewById(R.id.webView);
        webView.setBackgroundColor(Color.TRANSPARENT);
        webView.loadUrl("file:///android_asset/siren.html");

        VehicleDeviceStatus vds=Globals.vehicleDeviceStatus;
        if(vds.deadThreadFlag) {
            vds.deadThreadFlag=false;

            Log.d(Globals.TAG, "############################ RESTORE STATE ###########################");

            SharedPreferences settings=getSharedPreferences(Globals.SAVED_STATUS, 0);
            vds.batteryLevel=settings.getInt("BATTERY_LEVEL", 0);
            vds.geofenceAlarm=settings.getBoolean("GEOFENCE_ALARM", false);
            vds.geofenceAlarmTs=settings.getLong("GEOFENCE_ALARM_TS", 0);
            vds.latitude=settings.getFloat("LATITUDE", 0);
            vds.latitude_park=settings.getFloat("LATITUDE_PARK", 0);
            vds.locationTs=settings.getLong("LOCATION_TS", 0);
            vds.longitude=settings.getFloat("LONGITUDE", 0);
            vds.longitude_park=settings.getFloat("LONGITUDE_PARK", 0);
            vds.parkingStatus=settings.getInt("PARKING_STATUS", 0);
            vds.sensorAlarm=settings.getBoolean("SENSOR_ALARM", false);
            vds.sensorAlarmTs=settings.getLong("SENSOR_ALARM_TS", 0);
            vds.trackingActive=settings.getBoolean("TRACKING_ACTIVE", false);
        }
    }

    @Override
    public void onBackPressed() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            finishAffinity();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        Log.d(Globals.TAG, "##################### SAVING STATE ON ACTIVITY ####################");

        SharedPreferences settings=getSharedPreferences(Globals.SAVED_STATUS, 0);
        SharedPreferences.Editor editor = settings.edit();

        VehicleDeviceStatus vds=Globals.vehicleDeviceStatus;

        editor.putInt("BATTERY_LEVEL", vds.batteryLevel);
        editor.putBoolean("GEOFENCE_ALARM", vds.geofenceAlarm);
        editor.putLong("GEOFENCE_ALARM_TS", vds.geofenceAlarmTs);
        editor.putFloat("LATITUDE", (float) vds.latitude);
        editor.putFloat("LATITUDE_PARK", (float) vds.latitude_park);
        editor.putLong("LOCATION_TS", vds.locationTs);
        editor.putFloat("LONGITUDE", (float) vds.longitude);
        editor.putFloat("LONGITUDE_PARK", (float) vds.longitude_park);
        editor.putInt("PARKING_STATUS", vds.parkingStatus);
        editor.putBoolean("SENSOR_ALARM", vds.sensorAlarm);
        editor.putLong("SENSOR_ALARM_TS", vds.sensorAlarmTs);
        editor.putBoolean("TRACKING_ACTIVE", vds.trackingActive);
        editor.apply();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.monitor_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent=null;
        final SharedPreferences settings=getSharedPreferences(Globals.CONFIGURACION, 0);
        FirebaseMessaging fm = FirebaseMessaging.getInstance();
        String groupId, to, id;

        switch (item.getItemId()) {
            case R.id.monitor_menu_refresh:
                groupId=settings.getString(Globals.FB_GROUP_ID, null);

                to = groupId;
                id = Integer.toString(Globals.msgId.incrementAndGet());
                fm.send(new RemoteMessage.Builder(to)
                        .setMessageId(id)
                        .addData(Globals.P2P_DEST, Globals.P2P_DEST_IN_VEHICLE)
                        .addData(Globals.P2P_OP, Globals.P2P_OP_GET_LOCATION)
                        .setTtl(3600)
                        .build());

                Toast.makeText(getApplicationContext(), getString(R.string.monitor_getLocation_toast), Toast.LENGTH_SHORT).show();
                return true;
            case R.id.monitor_menu_tracking:
                fm = FirebaseMessaging.getInstance();

                groupId=settings.getString(Globals.FB_GROUP_ID, null);

                to = groupId;
                id = Integer.toString(Globals.msgId.incrementAndGet());
                fm.send(new RemoteMessage.Builder(to)
                        .setMessageId(id)
                        .addData(Globals.P2P_DEST, Globals.P2P_DEST_IN_VEHICLE)
                        .addData(Globals.P2P_OP, Globals.P2P_OP_ACTIVATE_TRACKING)
                        .setTtl(3600)
                        .build());

                Toast.makeText(getApplicationContext(), getString(R.string.monitor_activateTracking_toast), Toast.LENGTH_SHORT).show();
                return true;
            case R.id.monitor_menu_conf:
                intent=new Intent(this, MonitorSettingsActivity.class);
                startActivity(intent);
                return true;
            case R.id.monitor_menu_reset:
                AlertDialog.Builder builder = new AlertDialog.Builder(this);

                builder.setMessage(R.string.monitor_dialog_reset);

                builder.setPositiveButton(R.string.monitor_dialog_reset_ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        new Thread() {
                            @Override
                            public void run() {
                                try {
                                    FBGroupManager.removeFirebaseId(MonitorActivity.this, settings.getString(Globals.REMOTE_FB_REGISTRATION_ID, null));
                                    FBGroupManager.removeFirebaseId(MonitorActivity.this, settings.getString(Globals.FB_REGISTRATION_ID, null));
                                } catch(Exception e) {
                                    e.printStackTrace();
                                }

                                Log.d(Globals.TAG, "##################### RESETTING STATE ON ACTIVITY ####################");

                                SharedPreferences settings=getSharedPreferences(Globals.SAVED_STATUS, 0);
                                SharedPreferences.Editor editor = settings.edit();

                                VehicleDeviceStatus vds=new VehicleDeviceStatus();
                                Globals.vehicleDeviceStatus=vds;

                                editor.putInt("BATTERY_LEVEL", vds.batteryLevel);
                                editor.putBoolean("GEOFENCE_ALARM", vds.geofenceAlarm);
                                editor.putLong("GEOFENCE_ALARM_TS", vds.geofenceAlarmTs);
                                editor.putFloat("LATITUDE", (float) vds.latitude);
                                editor.putFloat("LATITUDE_PARK", (float) vds.latitude_park);
                                editor.putLong("LOCATION_TS", vds.locationTs);
                                editor.putFloat("LONGITUDE", (float) vds.longitude);
                                editor.putFloat("LONGITUDE_PARK", (float) vds.longitude_park);
                                editor.putInt("PARKING_STATUS", vds.parkingStatus);
                                editor.putBoolean("SENSOR_ALARM", vds.sensorAlarm);
                                editor.putLong("SENSOR_ALARM_TS", vds.sensorAlarmTs);
                                editor.putBoolean("TRACKING_ACTIVE", vds.trackingActive);
                                editor.apply();
                            }
                        }.start();

                        SharedPreferences.Editor editor=settings.edit();
                        editor.putString(Globals.APP_MODE, Globals.NULL);
                        editor.putString(Globals.FB_GROUP_ID, null);
                        editor.apply();

                        Intent intent=new Intent(MonitorActivity.this, MainActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                    }
                });
                builder.setNegativeButton(R.string.monitor_dialog_reset_cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        return;
                    }
                });
                AlertDialog dialog = builder.create();
                dialog.show();

                return true;
            case R.id.monitor_menu_test:
                groupId=settings.getString(Globals.FB_GROUP_ID, null);

                to = groupId;
                id = Integer.toString(Globals.msgId.incrementAndGet());
                fm.send(new RemoteMessage.Builder(to)
                        .setMessageId(id)
                        .addData(Globals.P2P_DEST, Globals.P2P_DEST_IN_VEHICLE)
                        .addData(Globals.P2P_OP, Globals.P2P_OP_TEST)
                        .addData(Globals.P2P_TXT, getString(R.string.monitor_test_string))
                        .setTtl(3600)
                        .build());

                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        registerReceiver(updateUI, broadcastFilter);

        repaint();
    }

    @Override
    protected void onStop() {
        super.onStop();

        unregisterReceiver(updateUI);
    }

    public void showMap(View v) {
        Intent intent=new Intent(MonitorActivity.this, MonitorMapActivity.class);
        startActivity(intent);
    }

    public void deActivateTracking(View v) {
        FirebaseMessaging fm = FirebaseMessaging.getInstance();

        SharedPreferences settings=getSharedPreferences(Globals.CONFIGURACION, 0);
        String groupId=settings.getString(Globals.FB_GROUP_ID, null);

        String to = groupId;
        String id = Integer.toString(Globals.msgId.incrementAndGet());
        fm.send(new RemoteMessage.Builder(to)
                .setMessageId(id)
                .addData(Globals.P2P_DEST, Globals.P2P_DEST_IN_VEHICLE)
                .addData(Globals.P2P_OP, Globals.P2P_OP_DEACTIVATE_TRACKING)
                .setTtl(3600)
                .build());

        VehicleDeviceStatus vds=Globals.vehicleDeviceStatus;
        vds.trackingActive=false;

        ViewGroup layout=(ViewGroup)findViewById(R.id.tracking);
        layout.setVisibility(View.GONE);
        cancelTrackingTs=System.currentTimeMillis();

        NotificationManager mNotifyMgr =
                (NotificationManager) getApplicationContext().getSystemService(NOTIFICATION_SERVICE);
        mNotifyMgr.cancel(Globals.LOCATION_UPDATE);
    }

    public void clearSensorAlarm(View v) {
        VehicleDeviceStatus vds=Globals.vehicleDeviceStatus;
        vds.sensorAlarm=false;

        ViewGroup layout=(ViewGroup)findViewById(R.id.sensorAlarm);
        layout.setVisibility(View.GONE);

        NotificationManager mNotifyMgr =
        (NotificationManager) getApplicationContext().getSystemService(NOTIFICATION_SERVICE);
        mNotifyMgr.cancel(Globals.SENSOR_ALARM);
    }

    public void clearGeofenceAlarm(View v) {
        VehicleDeviceStatus vds=Globals.vehicleDeviceStatus;
        vds.geofenceAlarm=false;

        ViewGroup layout=(ViewGroup)findViewById(R.id.geofenceAlarm);
        layout.setVisibility(View.GONE);

        NotificationManager mNotifyMgr =
                (NotificationManager) getApplicationContext().getSystemService(NOTIFICATION_SERVICE);
        mNotifyMgr.cancel(Globals.GEOFENCE_TRANSITION);

        PlayAlert.stopAlarm();
    }

    public void parkReset(View v) {
        FirebaseMessaging fm = FirebaseMessaging.getInstance();

        SharedPreferences settings=getSharedPreferences(Globals.CONFIGURACION, 0);
        String groupId=settings.getString(Globals.FB_GROUP_ID, null);

        String to = groupId;
        String id = Integer.toString(Globals.msgId.incrementAndGet());
        fm.send(new RemoteMessage.Builder(to)
                .setMessageId(id)
                .addData(Globals.P2P_DEST, Globals.P2P_DEST_IN_VEHICLE)
                .addData(Globals.P2P_OP, Globals.P2P_OP_PARK_RESET)
                .setTtl(3600)
                .build());

        ImageView parking=(ImageView)findViewById(R.id.parking);
        parking.setVisibility(View.INVISIBLE);

        VehicleDeviceStatus vds=Globals.vehicleDeviceStatus;
        vds.parkingStatus=0;
    }

    public static String tsToStr(long ts) {
        SimpleDateFormat hourFormatter = new SimpleDateFormat(HOUR_FORMAT);
        SimpleDateFormat dayFormatter = new SimpleDateFormat(DAY_FORMAT);
        if (System.currentTimeMillis() - ts < DAY_DURATION)
            return(hourFormatter.format(new Date(ts)));
        else
            return(dayFormatter.format(new Date(ts)));
    }

    private void setLocationUI(boolean animate) {
        VehicleDeviceStatus vds=Globals.vehicleDeviceStatus;
        TextView txt=(TextView)findViewById(R.id.locationTime);

        if(vds.locationTs!=0)
            txt.setText(tsToStr(vds.locationTs));

        if(animate) {
            txt.setScaleX(3);
            txt.setScaleY(3);
            txt.setAlpha(0);
            txt.setTextColor(0xFF0000FF);
            txt.animate().scaleX(1).scaleY(1).alpha(1).setInterpolator(new DecelerateInterpolator(2)).setDuration(1000);
/*        ObjectAnimator anim1=ObjectAnimator.ofFloat(txt, "ScaleX", 3, 1);
        ObjectAnimator anim2=ObjectAnimator.ofFloat(txt, "ScaleY", 3, 1);
        ObjectAnimator anim3=ObjectAnimator.ofInt(txt, "TextColor", 0xFFFF0000, 0xFF000000);
        AnimatorSet anim=new AnimatorSet();
        anim.playTogether(anim1, anim2, anim3);
        anim.setDuration(5000);
        anim.start(); */
        }

        txt=(TextView)findViewById(R.id.batteryLevel);
        if(vds.batteryLevel!=-1)
            txt.setText(String.valueOf(vds.batteryLevel)+"%");

/*        if(animate) {
            txt.setScaleX(3);
            txt.setScaleY(3);
            txt.setAlpha(0);
            txt.setTextColor(0xFF0000FF);
            txt.animate().scaleX(1).scaleY(1).alpha(1).setInterpolator(new DecelerateInterpolator(2)).setDuration(1000);
        } */


        ImageView parking=(ImageView)findViewById(R.id.parking);
        parking.setVisibility(vds.parkingStatus == 0?View.INVISIBLE:View.VISIBLE);
    }

    private void setSensorUI() {
        VehicleDeviceStatus vds=Globals.vehicleDeviceStatus;
        TextView txt=(TextView)findViewById(R.id.sensorTime);
        Log.d(Globals.TAG, "SensorAlarmTs: "+vds.sensorAlarmTs);
        if(vds.sensorAlarmTs!=0)
            txt.setText(tsToStr(vds.sensorAlarmTs));

        ViewGroup layout=(ViewGroup) findViewById(R.id.sensorAlarm);
        Log.d(Globals.TAG, "SensorAlarmTs: "+vds.sensorAlarm);
        if(vds.sensorAlarm)
            layout.setVisibility(View.VISIBLE);
        else
            layout.setVisibility(View.GONE);
    }

    private void setGeofenceUI() {
        VehicleDeviceStatus vds=Globals.vehicleDeviceStatus;
        TextView txt=(TextView)findViewById(R.id.geofenceTime);
        if(vds.geofenceAlarmTs!=0)
            txt.setText(tsToStr(vds.geofenceAlarmTs));

        ViewGroup layout=(ViewGroup) findViewById(R.id.geofenceAlarm);
        if(vds.geofenceAlarm)
            layout.setVisibility(View.VISIBLE);
        else
            layout.setVisibility(View.GONE);
    }

    private boolean setTrackingUI() {
        VehicleDeviceStatus vds=Globals.vehicleDeviceStatus;

        ViewGroup layout=(ViewGroup) findViewById(R.id.tracking);
        if(vds.trackingActive) {
            if(System.currentTimeMillis() - cancelTrackingTs > Globals.LOCATION_UPDATE_INTERVAL)
                layout.setVisibility(View.VISIBLE);
            else
                return false;
        } else {
            layout.setVisibility(View.GONE);
        }
        return true;
    }

    private void setAudioUI() {
        VehicleDeviceStatus vds=Globals.vehicleDeviceStatus;
        ImageView imageView=(ImageView) findViewById(R.id.audioIcon);
        if(vds.audioOn)
            imageView.setImageResource(R.drawable.ic_audio_on);
        else
            imageView.setImageResource(R.drawable.ic_audio_off);
    }

    private void repaint() {
        setLocationUI(false);
        setSensorUI();
        setGeofenceUI();
        setTrackingUI();
        setAudioUI();
    }

    public class UpdateUI extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            int op=intent.getIntExtra(Globals.P2P_OP, 0);


            switch (op) {
                case Globals.SENSOR_ALARM:
                    setSensorUI();
                    break;
                case Globals.GEOFENCE_TRANSITION:
                    setGeofenceUI();
                    setLocationUI(true);
                    break;
                case Globals.LOCATION_UPDATE:
                    setLocationUI(true);
                    if(setTrackingUI()) {
                        return;
                    }
                    break;
                case Globals.GET_LOCATION_RESULT:
                case Globals.PARK:
                    setLocationUI(true);
                    break;
                case Globals.HIDE_LOCATION_UPDATE:
                    setTrackingUI();
                    break;
                case Globals.AUDIO:
                    setAudioUI();
            }

            setResultCode(Activity.RESULT_OK);
        }
    }


    public void activateAudio(View v) {
        if(!Globals.vehicleDeviceStatus.audioOn) {
            SharedPreferences settings = getSharedPreferences(Globals.CONFIGURACION, 0);
            final String stunServer = settings.getString(getString(R.string.settings_turn_server), "");

            new Thread() {
                @Override
                public void run() {

                    UDPLinkMgr mUDPLinkMgr = UDPLinkMgr.getInstance();

                    String SDPstr=mUDPLinkMgr.getSDPString(stunServer);

                    if(SDPstr==null) {
                        Handler mainHandler = new Handler(getMainLooper());

                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(getApplicationContext(), R.string.monitor_errorAudio_toast, Toast.LENGTH_SHORT).show();
                            }
                        });

                        return;
                    }

                    if(SDPstr.length()==0)
                        return;

                    Handler mainHandler = new Handler(getMainLooper());

                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(), R.string.monitor_activateAudio_toast, Toast.LENGTH_SHORT).show();
                        }
                    });

                    SharedPreferences settings = getSharedPreferences(Globals.CONFIGURACION, 0);
                    String to = settings.getString(Globals.FB_GROUP_ID, null);

                    FirebaseMessaging fm = FirebaseMessaging.getInstance();
                    String id = Integer.toString(Globals.msgId.incrementAndGet());
                    RemoteMessage.Builder mRemoteMessage = new RemoteMessage.Builder(to);
                    fm.send(mRemoteMessage.setMessageId(id)
                            .addData(Globals.P2P_DEST, Globals.P2P_DEST_IN_VEHICLE)
                            .addData(Globals.P2P_OP, Globals.P2P_OP_AUDIO_REQ)
                            .addData(Globals.P2P_SDP, SDPstr)
                            .addData(Globals.P2P_STUN_SERVER, stunServer)
                            .setTtl(3600)
                            .build());
                }
            }.start();
        } else {
/*            ImageView imageView=(ImageView)findViewById(R.id.audioIcon);
            imageView.setImageResource(R.drawable.ic_audio_off); */

            UDPLinkMgr mUDPLinkMgr=UDPLinkMgr.getInstance();
            mUDPLinkMgr.stopAudio();

            SharedPreferences settings = getSharedPreferences(Globals.CONFIGURACION, 0);
            String to = settings.getString(Globals.FB_GROUP_ID, null);

            FirebaseMessaging fm = FirebaseMessaging.getInstance();
            String id = Integer.toString(Globals.msgId.incrementAndGet());
            RemoteMessage.Builder mRemoteMessage = new RemoteMessage.Builder(to);
            fm.send(mRemoteMessage.setMessageId(id)
                    .addData(Globals.P2P_DEST, Globals.P2P_DEST_IN_VEHICLE)
                    .addData(Globals.P2P_OP, Globals.P2P_OP_STOP_AUDIO)
                    .setTtl(3600)
                    .build());
        }
    }
}

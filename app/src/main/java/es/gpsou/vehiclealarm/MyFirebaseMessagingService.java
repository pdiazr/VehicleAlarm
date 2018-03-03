package es.gpsou.vehiclealarm;

import android.app.Activity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;
import android.widget.Toast;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

/**
 * Created by Pedro on 04/02/2017.
 */
// public class MyFirebaseMessagingService extends FirebaseMessagingService implements
//        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {

    public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static LocationSupport ls=null;

    static Intent broadcastIntent=null;

    static Handler handler=null;
    static Runnable hideUpdates=null;

    public MyFirebaseMessagingService() {
        super();
        Log.d(Globals.TAG, "New MyFirebaseMessagingService instance");
        ls=LocationSupport.getLocationSupport();

        if(broadcastIntent==null) {
            broadcastIntent=new Intent(Globals.UPDATE_UI_INTENT_ACTION);
            broadcastIntent.setPackage(Globals.PACKAGE_NAME);
        }
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        if (remoteMessage.getData().size() > 0) {
            final JSONObject data = new JSONObject(remoteMessage.getData());
            Log.d(Globals.TAG, data.toString());

            try {
                String dest = data.getString(Globals.P2P_DEST);
                String op = data.getString(Globals.P2P_OP);

                SharedPreferences settings=getSharedPreferences(Globals.CONFIGURACION, 0);
                String mode=settings.getString(Globals.APP_MODE, Globals.NULL);

                if (op.compareTo(Globals.P2P_OP_TEST) == 0) {
                    final String txt = data.getString(Globals.P2P_TXT);
                    Handler mainHandler = new Handler(getMainLooper());

                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(), txt, Toast.LENGTH_SHORT).show();
                        }
                    });
                    new PlayAlert(getApplicationContext()).start();
                }

                if (dest.compareTo(Globals.P2P_DEST_IN_VEHICLE) == 0) {

                    if(mode.compareTo(Globals.IN_VEHICLE_MODE) == 0) {
                        if (op.compareTo(Globals.P2P_OP_GET_LOCATION) == 0) {
                            ls.opGetLocation(getApplicationContext());
                        } else if (op.compareTo(Globals.P2P_OP_ACTIVATE_TRACKING) == 0) {
                            ls.opActivateTracking(getApplicationContext());
                        } else if (op.compareTo(Globals.P2P_OP_DEACTIVATE_TRACKING) == 0) {
                            ls.opDeactivateTracking(getApplicationContext());
                        } else if (op.compareTo(Globals.P2P_OP_PARK_RESET) == 0) {
                            ls.opDeactivateGeofence(getApplicationContext());
                            ParkDetectionIntentService.startParkDetection(getApplicationContext());
                        } else if(op.compareTo(Globals.P2P_OP_SET_GROUP_ID)==0) {
                            String groupId=data.getString(Globals.P2P_GROUP_ID);

                            FirebaseMessaging fm = FirebaseMessaging.getInstance();
                            String operation=Globals.P2P_OP_SET_GROUP_ID_RESULT;
                            String id = Integer.toString(Globals.msgId.incrementAndGet());
                            RemoteMessage.Builder mRemoteMessage=new RemoteMessage.Builder(groupId);
                            fm.send(mRemoteMessage.setMessageId(id)
                                    .addData(Globals.P2P_DEST, Globals.P2P_DEST_MONITOR)
                                    .addData(Globals.P2P_OP, operation)
                                    .setTtl(3600)
                                    .build());
                        } else if(op.compareTo(Globals.P2P_OP_AUDIO_REQ)==0) {
                            String remoteReceived=data.getString(Globals.P2P_SDP);
                            String stunServer=data.getString(Globals.P2P_STUN_SERVER);

                            UDPLinkMgr mUDPLinkMgr = UDPLinkMgr.getInstance();

                            String SDPstr=mUDPLinkMgr.getSDPString(stunServer, null, null);

                            if(SDPstr==null)
                                SDPstr=Globals.NULL;
                            else if(SDPstr.length()>0) {
                                Intent intent=new Intent(this, AudioService.class);
                                intent.setAction(AudioService.ACTION_SEND);
                                intent.putExtra(AudioService.REMOTE_RECEIVED, remoteReceived);
                                startService(intent);
                            }

                            String to=settings.getString(Globals.FB_GROUP_ID, null);
                            FirebaseMessaging fm = FirebaseMessaging.getInstance();
                            String id = Integer.toString(Globals.msgId.incrementAndGet());
                            RemoteMessage.Builder mRemoteMessage=new RemoteMessage.Builder(to);
                            fm.send(mRemoteMessage.setMessageId(id)
                                    .addData(Globals.P2P_DEST, Globals.P2P_DEST_MONITOR)
                                    .addData(Globals.P2P_OP, Globals.P2P_OP_AUDIO_RESP)
                                    .addData(Globals.P2P_SDP, SDPstr)
                                    .setTtl(3600)
                                    .build());

                        } else if(op.compareTo(Globals.P2P_OP_STOP_AUDIO)==0) {
                            UDPLinkMgr mUDPLinkMgr=UDPLinkMgr.getInstance();
                            mUDPLinkMgr.stopAudio();
                        } else if(op.compareTo(Globals.P2P_OP_SWITCH_MONITORING)==0) {
                            startActivity(new Intent(this, VehicleActivity.class)
                                    .putExtra(Globals.SWITCH_MONITORING, true).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                        }
                    } else if (op.compareTo(Globals.P2P_OP_SET_GROUP_ID) == 0 && mode.compareTo(Globals.NULL) == 0) {
                        SharedPreferences.Editor editor = settings.edit();
                        String groupId=data.getString(Globals.P2P_GROUP_ID);
                        editor.putString(Globals.FB_GROUP_ID, groupId);
                        editor.putString(Globals.APP_MODE, Globals.IN_VEHICLE_MODE);
                        editor.apply();

                        FirebaseMessaging fm = FirebaseMessaging.getInstance();
                        String operation=Globals.P2P_OP_SET_GROUP_ID_RESULT;
                        String id = Integer.toString(Globals.msgId.incrementAndGet());
                        RemoteMessage.Builder mRemoteMessage=new RemoteMessage.Builder(groupId);
                        fm.send(mRemoteMessage.setMessageId(id)
                                .addData(Globals.P2P_DEST, Globals.P2P_DEST_MONITOR)
                                .addData(Globals.P2P_OP, operation)
                                .setTtl(3600)
                                .build());

                        Intent intent = new Intent(this, VehicleActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(intent);

                        Handler mainHandler = new Handler(getMainLooper());

                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(getApplicationContext(), R.string.MFMS_successful_pairing, Toast.LENGTH_SHORT).show();
                            }
                        });

                        BtServerActivity.btListening=false;
                    }
                } else if (dest.compareTo(Globals.P2P_DEST_MONITOR) == 0) {
                    VehicleDeviceStatus vds=Globals.vehicleDeviceStatus;


                    if(mode.compareTo(Globals.MONITORING_MODE) == 0) {

                        if (op.compareTo(Globals.P2P_OP_SENSOR_ALARM) == 0) {
                            if(!BTProximity.getInstance(getApplicationContext()).isVehicleNearCheck()) {
                                vds.sensorAlarm = true;
                                vds.sensorAlarmTs = Long.parseLong(data.getString(Globals.P2P_TIMESTAMP));

                                broadcastIntent.putExtra(Globals.P2P_OP, Globals.SENSOR_ALARM);
                                sendOrderedBroadcast(broadcastIntent, null, new BroadcastReceiver() {
                                    @Override
                                    public void onReceive(Context context, Intent intent) {
                                        int result = getResultCode();

                                        if (result == Activity.RESULT_CANCELED) {
                                            try {
                                                ShowNotification(Globals.SENSOR_ALARM, data.getString(Globals.P2P_TIMESTAMP));
                                            } catch (JSONException e) {
                                                Log.d(Globals.TAG, "Excepción en parseo de estructura JSON");
                                            }
                                        }
                                    }
                                }, null, Activity.RESULT_CANCELED, null, null);

                                Handler mainHandler = new Handler(getMainLooper());
                                mainHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(getApplicationContext(), "Sensor Alarm", Toast.LENGTH_SHORT).show();
                                    }
                                });
                                new PlayAlert(getApplicationContext(), RingtoneManager.TYPE_NOTIFICATION).start();
                            }
                        } else if (op.compareTo(Globals.P2P_OP_GEOFENCING_ALERT) == 0) {
                            if(!BTProximity.getInstance(getApplicationContext()).isVehicleNearCheck()) {
                                vds.geofenceAlarm = true;
                                vds.geofenceAlarmTs = Long.parseLong(data.getString(Globals.P2P_TIMESTAMP));
                                vds.batteryLevel = Integer.parseInt(data.getString(Globals.P2P_BATTERY));
                                vds.parkingStatus = 0;
                                double latitude = Double.parseDouble(data.getString(Globals.P2P_LATITUDE));
                                double longitude = Double.parseDouble(data.getString(Globals.P2P_LONGITUDE));
                                if (latitude != 0.0d || longitude != 0.0d) {
                                    vds.latitude = latitude;
                                    vds.longitude = longitude;
                                    vds.locationTs = vds.geofenceAlarmTs;
                                }

                                broadcastIntent.putExtra(Globals.P2P_OP, Globals.GEOFENCE_TRANSITION);
                                sendOrderedBroadcast(broadcastIntent, null, new BroadcastReceiver() {
                                    @Override
                                    public void onReceive(Context context, Intent intent) {
                                        int result = getResultCode();

                                        if (result == Activity.RESULT_CANCELED) {
                                            try {
                                                ShowNotification(Globals.GEOFENCE_TRANSITION, data.getString(Globals.P2P_TIMESTAMP));
                                            } catch (JSONException e) {
                                                Log.d(Globals.TAG, "Excepción en parseo de estructura JSON");
                                            }
                                        }
                                    }
                                }, null, Activity.RESULT_CANCELED, null, null);

                                Handler mainHandler = new Handler(getMainLooper());
                                mainHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(getApplicationContext(), "GEOFENCE ALARM", Toast.LENGTH_SHORT).show();
                                    }
                                });
                                new PlayAlert(getApplicationContext(), RingtoneManager.TYPE_ALARM).start();
                            }
                        } else if (op.compareTo(Globals.P2P_OP_GET_LOCATION_RESULT) == 0) {

                            final String result = data.getString(Globals.P2P_RESULT);
                            if (result.compareTo(LocationSupport.RESULT_OK) == 0) {
                                vds.latitude = Double.parseDouble(data.getString(Globals.P2P_LATITUDE));
                                vds.longitude = Double.parseDouble(data.getString(Globals.P2P_LONGITUDE));
                                vds.batteryLevel = Integer.parseInt(data.getString(Globals.P2P_BATTERY));
                                vds.parkingStatus = Integer.parseInt(data.getString(Globals.P2P_PARKING));
                                vds.locationTs = Long.parseLong(data.getString(Globals.P2P_TIMESTAMP));
                                try {
                                    vds.latitude_park=Double.parseDouble(data.getString(Globals.P2P_LATITUDE_PARK));
                                    vds.longitude_park=Double.parseDouble(data.getString(Globals.P2P_LONGITUDE_PARK));
                                } catch(JSONException e) {

                                }

                                broadcastIntent.putExtra(Globals.P2P_OP, Globals.GET_LOCATION_RESULT);
                                sendOrderedBroadcast(broadcastIntent, null, new BroadcastReceiver() {
                                    @Override
                                    public void onReceive(Context context, Intent intent) {
                                        int result = getResultCode();

                                        if (result == Activity.RESULT_CANCELED) {
                                            try {
                                                ShowNotification(Globals.GET_LOCATION_RESULT, data.getString(Globals.P2P_TIMESTAMP));
                                            } catch (JSONException e) {
                                                Log.d(Globals.TAG, "Excepción en parseo de estructura JSON");
                                            }
                                        }
                                    }
                                }, null, Activity.RESULT_CANCELED, null, null);

                            } else {
                                Handler mainHandler = new Handler(getMainLooper());

                                mainHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(getApplicationContext(), result, Toast.LENGTH_SHORT).show();
                                    }
                                });

                            }
                        } else if (op.compareTo(Globals.P2P_OP_LOCATION_UPDATE) == 0) {

                            final String result = data.getString(Globals.P2P_RESULT);
                            if (result.compareTo(LocationSupport.RESULT_OK) == 0 ||
                                    result.compareTo(LocationSupport.ABSENT_DATA) == 0) {

                                vds.trackingActive = true;

                                if (result.compareTo(LocationSupport.RESULT_OK) == 0) {

                                    vds.latitude = Double.parseDouble(data.getString(Globals.P2P_LATITUDE));
                                    vds.longitude = Double.parseDouble(data.getString(Globals.P2P_LONGITUDE));
                                    vds.locationTs = Long.parseLong(data.getString(Globals.P2P_TIMESTAMP));
                                    vds.parkingStatus = Integer.parseInt(data.getString(Globals.P2P_PARKING));
                                    vds.batteryLevel = Integer.parseInt(data.getString(Globals.P2P_BATTERY));

                                    final String msg = "UPDATE:\nLatitud: " + data.getString(Globals.P2P_LATITUDE) + "\nLongitud: " + data.getString(Globals.P2P_LONGITUDE);
                                    Handler mainHandler = new Handler(getMainLooper());

                                    mainHandler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                }

                                broadcastIntent.putExtra(Globals.P2P_OP, Globals.LOCATION_UPDATE);

                                sendOrderedBroadcast(broadcastIntent, null, new BroadcastReceiver() {
                                    @Override
                                    public void onReceive(Context context, Intent intent) {
                                        int result = getResultCode();

                                        if (result == Activity.RESULT_CANCELED)
                                            ShowNotification(Globals.LOCATION_UPDATE, null);
                                    }
                                }, null, Activity.RESULT_CANCELED, null, null);

                                hideOnNoUpdates();
                            } else if (result.compareTo(LocationSupport.PERMISSION_ERROR) == 0) {
                                Handler mainHandler = new Handler(getMainLooper());

                                mainHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(getApplicationContext(), getString(R.string.monitor_loc_permission_error), Toast.LENGTH_LONG).show();
                                    }
                                });
                            } else {
                                Handler mainHandler = new Handler(getMainLooper());

                                mainHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(getApplicationContext(), "UPDATE\n" + result, Toast.LENGTH_SHORT).show();
                                    }
                                });

                            }
                        } else if (op.compareTo(Globals.P2P_OP_PARK) == 0) {

                            final String result = data.getString(Globals.P2P_RESULT);
                            if (result.compareTo(LocationSupport.RESULT_OK) == 0) {
                                vds.latitude = Double.parseDouble(data.getString(Globals.P2P_LATITUDE));
                                vds.longitude = Double.parseDouble(data.getString(Globals.P2P_LONGITUDE));
                                vds.locationTs = Long.parseLong(data.getString(Globals.P2P_TIMESTAMP));
                                vds.latitude_park = Double.parseDouble(data.getString(Globals.P2P_LATITUDE_PARK));
                                vds.longitude_park = Double.parseDouble(data.getString(Globals.P2P_LONGITUDE_PARK));
                                vds.batteryLevel = Integer.parseInt(data.getString(Globals.P2P_BATTERY));
                                vds.parkingStatus = Integer.parseInt(data.getString(Globals.P2P_PARKING));

                                broadcastIntent.putExtra(Globals.P2P_OP, Globals.PARK);
                                sendOrderedBroadcast(broadcastIntent, null);

                            } else {
                                Handler mainHandler = new Handler(getMainLooper());

                                mainHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(getApplicationContext(), result, Toast.LENGTH_SHORT).show();
                                    }
                                });

                            }
                        } else if(op.compareTo(Globals.P2P_OP_REPLACE_REGISTRATION_ID)==0) {
                            try {
                                FBGroupManager.replaceFirebaseId(getApplicationContext(), data.getString(Globals.P2P_OLD_FB_REGISTRATION_ID), data.getString(Globals.P2P_NEW_FB_REGISTRATION_ID));
                            } catch(Exception e) {
                                e.printStackTrace();
                            }
                        } else if(op.compareTo(Globals.P2P_OP_REMOVE_REGISTRATION_ID)==0) {
                            try {
                                FBGroupManager.removeFirebaseId(getApplicationContext(), data.getString(Globals.P2P_FB_REGISTRATION_ID));
                            } catch(Exception e) {
                                e.printStackTrace();
                            }
                        } else if(op.compareTo(Globals.P2P_OP_AUDIO_RESP)==0) {
                            String remoteReceived=data.getString(Globals.P2P_SDP);

                            UDPLinkMgr mUDPLingMgr = UDPLinkMgr.getInstance();

                            if(remoteReceived.compareTo(Globals.NULL)==0) {
                                mUDPLingMgr.stopAudio();

                                Handler mainHandler = new Handler(getMainLooper());

                                mainHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(getApplicationContext(), R.string.MFMS_audio_error, Toast.LENGTH_SHORT).show();
                                    }
                                });
                            } else if(remoteReceived.length()>0){
                                Intent intent=new Intent(this, AudioService.class);
                                intent.setAction(AudioService.ACTION_RECEIVE);
                                intent.putExtra(AudioService.REMOTE_RECEIVED, remoteReceived);
                                startService(intent);
                            }
                        } else if(op.compareTo(Globals.P2P_OP_SWITCH_MONITORING) == 0) {
                            String monitoringActivated=data.getString(Globals.P2P_MONITORING_ACTIVATED);
                            if(monitoringActivated.compareTo(Globals.TRUE) == 0)
                                vds.monitoringActivated=true;
                            else if(monitoringActivated.compareTo(Globals.FALSE) == 0)
                                vds.monitoringActivated=false;

                            broadcastIntent.putExtra(Globals.P2P_OP, Globals.MONITORING);
                            sendOrderedBroadcast(broadcastIntent, null);
                        }
                    } else if (op.compareTo(Globals.P2P_OP_SET_GROUP_ID_RESULT) == 0 && mode.compareTo(Globals.NULL) == 0) {
                        SharedPreferences.Editor editor=settings.edit();
                        editor.putString(Globals.APP_MODE, Globals.MONITORING_MODE);
                        editor.apply();

                        Intent intent = new Intent(this, MonitorActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(intent);

                        Handler mainHandler = new Handler(getMainLooper());

                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(getApplicationContext(), R.string.MFMS_successful_pairing, Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                } else
                    return;
            } catch (JSONException e) {
                return;
            }
        }

        // Check if message contains a notification payload.
        if (remoteMessage.getNotification() != null) {
            new PlayAlert(getApplicationContext()).start();
        }
    }

    @Override
    public void onSendError(String s, Exception e) {
        super.onSendError(s, e);
    }

    @Override
    public void onMessageSent(String s) {
        super.onMessageSent(s);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        SharedPreferences settings=getSharedPreferences(Globals.CONFIGURACION, 0);
        String appMode=settings.getString(Globals.APP_MODE, Globals.NULL);

        if(appMode.compareTo(Globals.MONITORING_MODE) == 0) {
            Log.d(Globals.TAG, "##################### SAVING STATE ####################");

            settings=getSharedPreferences(Globals.SAVED_STATUS, 0);
            SharedPreferences.Editor editor = settings.edit();

            VehicleDeviceStatus vds=Globals.vehicleDeviceStatus;

            if(vds.batteryLevel!=-1)
                editor.putInt("BATTERY_LEVEL", vds.batteryLevel);
            if(vds.geofenceAlarm)
                editor.putBoolean("GEOFENCE_ALARM", vds.geofenceAlarm);
            if(vds.geofenceAlarmTs!=0)
                editor.putLong("GEOFENCE_ALARM_TS", vds.geofenceAlarmTs);
            if(vds.latitude!=0)
                editor.putFloat("LATITUDE", (float) vds.latitude);
            if(vds.latitude_park!=0)
                editor.putFloat("LATITUDE_PARK", (float) vds.latitude_park);
            if(vds.locationTs!=0)
                editor.putLong("LOCATION_TS", vds.locationTs);
            if(vds.longitude!=0)
                editor.putFloat("LONGITUDE", (float) vds.longitude);
            if(vds.longitude_park!=0)
                editor.putFloat("LONGITUDE_PARK", (float) vds.longitude_park);
            if(vds.parkingStatus!=0)
                editor.putInt("PARKING_STATUS", vds.parkingStatus);
            if(vds.sensorAlarm)
                editor.putBoolean("SENSOR_ALARM", vds.sensorAlarm);
            if(vds.sensorAlarmTs!=0)
                editor.putLong("SENSOR_ALARM_TS", vds.sensorAlarmTs);
            if(vds.trackingActive)
                editor.putBoolean("TRACKING_ACTIVE", vds.trackingActive);
            editor.apply();
        }
    }

    void ShowNotification(int type, String timestampLong) {
        String title=null;
        String content=null;
        Intent intent=null;
        int iconId=0;
        boolean autoCancel=true;
        int argb=0, onMs=0, offMs=0;
        Date ts=null;
        PendingIntent pI=null;
        TaskStackBuilder stackBuilder=null;


        if(timestampLong!=null)
            ts=new Date(Long.parseLong(timestampLong));

        switch(type) {
            case Globals.SENSOR_ALARM:
                title="SENSOR MOVIMIENTO";
                content="Se ha detectado una vibración";
                intent=new Intent(this, MonitorActivity.class);
                pI=PendingIntent.getActivity(this, 1, intent, 0);
                iconId=R.drawable.ic_vibrate;
                argb=0xFF0000FF;
                onMs=1000;
                offMs=0;
                break;
            case Globals.GEOFENCE_TRANSITION:
                title="ALERTA DE UBICACIÓN";
                content="El vehículo se ha peusto en marcha";
                intent=new Intent(this, MonitorActivity.class);
                pI=PendingIntent.getActivity(this, 1, intent, 0);
                iconId=R.drawable.ic_stat_siren;
                argb=0xFFFF0000;
                onMs=50;
                offMs=50;
                break;
            case Globals.GET_LOCATION_RESULT:
                title="LOCALIZACIÓN";
                content="Recibida localización del vehículo";
                iconId=R.drawable.ic_stat_ic_notification;
                intent=new Intent(getApplicationContext(), MonitorMapActivity.class);
                stackBuilder = TaskStackBuilder.create(getApplicationContext());
                stackBuilder.addParentStack(MonitorMapActivity.class);
                stackBuilder.addNextIntent(intent);
                pI = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
                break;
            case Globals.LOCATION_UPDATE:
                title="SEGUIMIENTO ACTIVADO";
                content="Recibiendo actualizaciones de posición del vehículo...";
                iconId=R.mipmap.ic_red_icon;
                autoCancel=false;
                ts=null;
                intent=new Intent(getApplicationContext(), MonitorMapActivity.class);
                stackBuilder = TaskStackBuilder.create(getApplicationContext());
                stackBuilder.addParentStack(MonitorMapActivity.class);
                stackBuilder.addNextIntent(intent);
                pI = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
                break;
        }

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(getApplicationContext())
                        .setSmallIcon(iconId)
                        .setContentTitle(title)
                        .setContentText(content)
                        .setContentIntent(pI)
                        .setAutoCancel(autoCancel);
        if(ts!=null)
            mBuilder.setSubText(ts.toString());

        if(argb!=0)
            mBuilder.setLights(argb, onMs, offMs);

        int mNotificationId = type;
        NotificationManager mNotifyMgr =
                (NotificationManager) getApplicationContext().getSystemService(NOTIFICATION_SERVICE);
        mNotifyMgr.notify(mNotificationId, mBuilder.build());
    }

    private void hideOnNoUpdates() {
        if(handler==null)
            handler=new Handler(Looper.getMainLooper());

        if(hideUpdates==null) {
            hideUpdates=new Runnable() {
                @Override
                public void run() {
                    VehicleDeviceStatus vds=Globals.vehicleDeviceStatus;
                    vds.trackingActive=false;

                    NotificationManager mNotifyMgr =
                            (NotificationManager) getApplicationContext().getSystemService(NOTIFICATION_SERVICE);
                    mNotifyMgr.cancel(Globals.LOCATION_UPDATE);

                    broadcastIntent.putExtra(Globals.P2P_OP, Globals.HIDE_LOCATION_UPDATE);
                    sendOrderedBroadcast(broadcastIntent, null, null, null, Activity.RESULT_CANCELED, null, null);
                }
            };
        }

        handler.removeCallbacks(hideUpdates);
        handler.postDelayed(hideUpdates, 10 * Globals.LOCATION_UPDATE_INTERVAL);
    }
}

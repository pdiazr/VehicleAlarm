package es.gpsou.vehiclealarm;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.io.IOException;
import java.util.Date;
import java.util.UUID;

/**
 * Created by Pedro on 12/12/2017.
 */

public class BTProximity {
    public static final long STATUS_VALIDITY_TIME=300000;
    public static final String DUMMY_BT_MAC="02:00:00:00:00:00";

    private static final BTProximity ourInstance = new BTProximity();
    private static Context context=null;

    private static long lastProximityCheck=0;
    private static boolean nearVehicle=false;

    public static BTProximity getInstance(Context c) {
        context=c;

        return ourInstance;
    }

    private BTProximity() {
    }

    public static void configureTimeout() {

        new Thread() {
            @Override
            public void run() {
                long startTime = new Date().getTime();
                long diff = 10000;

                BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                if (mBluetoothAdapter.isEnabled()) {
                    BluetoothDevice mDevice = mBluetoothAdapter.getRemoteDevice(DUMMY_BT_MAC);

                    try {
                        BluetoothSocket mmSocket = mDevice.createRfcommSocketToServiceRecord(UUID.fromString(Globals.BT_UUID));

                        if (mmSocket != null) {
                            mBluetoothAdapter.cancelDiscovery();
                            mmSocket.connect();
                        }
                    } catch (IOException e) {
                        diff = new Date().getTime() - startTime;
                    }

                    SharedPreferences settings = context.getSharedPreferences(Globals.CONFIGURACION, 0);
                    SharedPreferences.Editor editor = settings.edit();
                    editor.putLong(Globals.BT_CONNECT_TIMEOUT, (long)(diff * 0.75));
                    editor.apply();
                }

            }
        }.start();

    }

    public static boolean isVehicleNearCheck() {
        SharedPreferences settings = context.getSharedPreferences(Globals.CONFIGURACION, 0);

        if(settings.getBoolean(context.getString(R.string.settings_proximity), false)) {
            BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (mBluetoothAdapter.isEnabled()) {
                long maxTimeout=settings.getLong(Globals.BT_CONNECT_TIMEOUT, 0);

                if(maxTimeout==0) {
                    Log.d(Globals.TAG, "Detector de proximidad: recalculamos timeout");
                    configureTimeout();
                    return(false);
                }

                long startTime = new Date().getTime();
                if(startTime - lastProximityCheck < STATUS_VALIDITY_TIME) {
                    Log.d(Globals.TAG, "Detector de proximidad: último valor registrado válido todavía");
                    return(nearVehicle);
                }

                Log.d(Globals.TAG, "Detector de proximidad: nuevo chequeo");
                lastProximityCheck=startTime;

                String remoteBtMAC=settings.getString(Globals.REMOTE_BT_MAC, DUMMY_BT_MAC);
                BluetoothDevice mDevice = mBluetoothAdapter.getRemoteDevice(remoteBtMAC);
                try {
                    BluetoothSocket mmSocket = mDevice.createRfcommSocketToServiceRecord(UUID.fromString(Globals.BT_UUID));

                    if (mmSocket != null) {
                        mBluetoothAdapter.cancelDiscovery();
                        mmSocket.connect();
                    }
                    return(false);
                } catch (IOException e) {
                    if(new Date().getTime() - startTime < maxTimeout) {
                        Log.d(Globals.TAG, "Detector de proximidad: dispositivo detectado:" + Long.toString(new Date().getTime() - startTime) + " - " + remoteBtMAC);
                        nearVehicle=true;
                    } else {
                        Log.d(Globals.TAG, "Detector de proximidad: dispositivo no detectado");
                        nearVehicle=false;
                    }
                }
                return(nearVehicle);
            } else
                return (false);
        } else
            return(false);
    }

}

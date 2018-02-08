package es.gpsou.vehiclealarm;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.Toast;

import com.google.firebase.iid.FirebaseInstanceId;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.UUID;

public class BtClientActivity extends AppCompatActivity {

    private static BluetoothAdapter mBluetoothAdapter=null;
    private static ArrayList<BluetoothDevice> devices=new ArrayList<>();
    private static ArrayAdapter<BluetoothDevice> itemsAdapter=null;
    private static BluetoothSocket mmSocket=null;
    ConnectThread connectThread=null;
    ProgressDialog ringProgressDialog = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bt_client);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mBluetoothAdapter.cancelDiscovery();
                itemsAdapter.clear();
                startBtDiscovering();
            }
        });
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();


        ListView listView = (ListView) findViewById(R.id.listView);
/*        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                mBluetoothAdapter.cancelDiscovery();
                connectThread=new ConnectThread(devices.get(position));
                connectThread.start();
            }
        }); */

        itemsAdapter = new ArrayAdapter<BluetoothDevice>(this, android.R.layout.simple_list_item_1, devices) {
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                LayoutInflater inflater = (LayoutInflater) BtClientActivity.this
                        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                View rowView = inflater.inflate(R.layout.simple_list_item_1, parent, false);
                RadioButton radioButton = (RadioButton) rowView.findViewById(R.id.text1);
                radioButton.setText(devices.get(position).getName());
                radioButton.setTag(position);
                radioButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        final RadioButton radioButton=(RadioButton)v;

                        mBluetoothAdapter.cancelDiscovery();

                        ringProgressDialog = ProgressDialog.show(BtClientActivity.this, getString(R.string.bt_monitor_dialog_title), getString(R.string.bt_monitor_dialog_info), true);
                        //you usually don't want the user to stop the current process, and this will make sure of that
                        ringProgressDialog.setCancelable(true);
                        ringProgressDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                            @Override
                            public void onDismiss(DialogInterface dialog) {

                                PairingThread.active=false;
                                closeSocket();

//                                runOnUiThread(new Runnable() {
//                                    @Override
//                                    public void run() {
                                        radioButton.setChecked(false);
//                                    }
//                                });
                            }
                        });
                        ringProgressDialog.show();

                        connectThread = new ConnectThread(devices.get((Integer) v.getTag()));
                        connectThread.start();
                    }
                });
                return rowView;
            }
        };

        listView.setAdapter(itemsAdapter);

    }

    @Override
    protected void onStart() {
        super.onStart();

        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mReceiver, filter);

        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, 0);
        } else {
            startBtDiscovering();
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(resultCode!=RESULT_CANCELED)
            startBtDiscovering();

    }

    // Create a BroadcastReceiver for ACTION_FOUND.
    public static final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {

                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                String deviceName = device.getName();
                String deviceHardwareAddress = device.getAddress(); // MAC address

                for(int i=0; i<itemsAdapter.getCount(); i++) {
                    if(itemsAdapter.getItem(i).getAddress().compareTo(deviceHardwareAddress)==0)
                        return;
                }
                itemsAdapter.add(device);
            }
        }
    };

    private void startBtDiscovering() {
//        itemsAdapter.clear();
        // Register for broadcasts when a device is discovered.
        if(mBluetoothAdapter!=null)
            mBluetoothAdapter.startDiscovery();

    }

    @Override
    protected void onStop() {
        super.onStop();

        unregisterReceiver(mReceiver);
        mBluetoothAdapter.cancelDiscovery();

        PairingThread.active=false;
    }

    synchronized void closeSocket() {
        if(mmSocket!=null) {
            try {
                mmSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private class ConnectThread extends Thread {
        BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device) {
            mmDevice=device;
        }

        public void run() {
            closeSocket();

            try {
                // Get a BluetoothSocket to bind with the given BluetoothDevice.
                // MY_UUID is the app's UUID string, also used in the server code.
                mmSocket = mmDevice.createRfcommSocketToServiceRecord(UUID.fromString(Globals.BT_UUID));

                Log.d(Globals.TAG, "Conectamos con: "+ mmDevice.getAddress());
                if(mmSocket!=null) {

                    // Cancel discovery because it otherwise slows down the connection.
                    mBluetoothAdapter.cancelDiscovery();

                    // Connect to the remote device through the socket. This call blocks
                    // until it succeeds or throws an exception.
                    mmSocket.connect();

                    // The connection attempt succeeded. Perform work associated with
                    // the connection in a separate thread.
                    manageMyConnectedSocket(mmSocket);

                }
            } catch (IOException connectException) {

                ringProgressDialog.dismiss();
                Handler mainHandler = new Handler(getMainLooper());
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), R.string.bt_monitor_pairing_error, Toast.LENGTH_SHORT).show();
                    }
                });
            }

            closeSocket();
        }


        private void manageMyConnectedSocket(BluetoothSocket socket) throws IOException {
            InputStream mmInputStream = null;
            OutputStream mmOutputStream = null;

            String inString=null;
            String outString=null;

            // Get the input and output streams; using temp objects because
            // member streams are final.
            mmInputStream = socket.getInputStream();
            mmOutputStream = socket.getOutputStream();

            BufferedReader reader=new BufferedReader(new InputStreamReader(mmInputStream));

            inString = reader.readLine();

            Log.d(Globals.TAG, "BT message: " + inString);

            SharedPreferences settings=getSharedPreferences(Globals.CONFIGURACION, 0);
            String remoteBtMac=null;
            String remoteFirebaseId=null;

            try {
                JSONObject receivedData = new JSONObject(inString);
                remoteBtMac=receivedData.getString(Globals.NFCBT_BT_MAC);
                remoteFirebaseId=receivedData.getString(Globals.NFCBT_FIREBASE_ID);
            } catch(JSONException e) {
                outString=Globals.NFCBT_ERROR_STRING;
            }

            if(outString==null) {
                String btMac = settings.getString(Globals.BT_MAC, "NULL");
                String firebaseId = FirebaseInstanceId.getInstance().getToken();

                SharedPreferences.Editor editor = settings.edit();
                editor.putString(Globals.REMOTE_BT_MAC, remoteBtMac);
                editor.putString(Globals.REMOTE_FB_REGISTRATION_ID, remoteFirebaseId);
                editor.putString(Globals.FB_REGISTRATION_ID, firebaseId);
                editor.apply();

                JSONObject data = new JSONObject();
                try {
                    data.put(Globals.NFCBT_BT_MAC, btMac);
                    data.put(Globals.NFCBT_FIREBASE_ID, firebaseId);
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
                outString=data.toString();

            }

            mmOutputStream.write(outString.getBytes(Charset.forName("UTF-8")));
            mmOutputStream.write("\n".getBytes());

            mmOutputStream.close();
            mmInputStream.close();

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    itemsAdapter.clear();
                }
            });

            PairingThread pairingThread=new PairingThread(BtClientActivity.this);
            pairingThread.start();

            while(pairingThread.isAlive()) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            ringProgressDialog.dismiss();
        }

    }

}


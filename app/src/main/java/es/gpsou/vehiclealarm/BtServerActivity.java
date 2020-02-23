package es.gpsou.vehiclealarm;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.firebase.iid.FirebaseInstanceId;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.UUID;

public class BtServerActivity extends AppCompatActivity {

    private static BluetoothAdapter mBluetoothAdapter=null;
    private BluetoothServerSocket mmServerSocket;
    public static boolean btListening=false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bt_server);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        TextView textView=(TextView)findViewById(R.id.textViewBtName);
        textView.setText(mBluetoothAdapter.getName());
    }

    @Override
    protected void onStart() {
        super.onStart();

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            public void run() {
                // acciones que se ejecutan tras los milisegundos
                Intent intent=new Intent(BtServerActivity.this, BtServerActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
            }
        }, 10);


/*        if (mBluetoothAdapter != null) {

            Intent discoverableIntent =
                    new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivityForResult(discoverableIntent, 0);
        } else {
            Log.d(Globals.TAG, "Device does not support Bluetooth");
        } */
    }


    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        if (mBluetoothAdapter != null) {

            if(mBluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
                Intent discoverableIntent =
                        new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 60);
                startActivityForResult(discoverableIntent, 0);
            } else {
                startListening();
            }
        } else {
            Log.d(Globals.TAG, "Device does not support Bluetooth");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(resultCode==RESULT_CANCELED) {
            ImageView imageView=(ImageView)findViewById(R.id.imageBT);
            imageView.setAlpha(0.25f);

            TextView textView=(TextView)findViewById(R.id.textViewBtName);
            textView.setText("");

            textView=(TextView)findViewById(R.id.textView);
            textView.setText("");
        } else {
            startListening();
        }
    }

    synchronized void closeServerSocket() {
        if (mmServerSocket != null) {
            try {
                mmServerSocket.close();
            } catch (IOException e) {
                Log.d(Globals.TAG, "Could not close the server socket", e);
            }
        }
    }

    private void startListening() {
        btListening = true;

        closeServerSocket();

        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                BluetoothSocket socket = null;

                try {
                    mmServerSocket = mBluetoothAdapter.listenUsingRfcommWithServiceRecord(Globals.TAG, UUID.fromString(Globals.BT_UUID));

                    if(mmServerSocket!=null) {
                        // Keep listening until exception occurs or a socket is returned.
                        while (btListening) {

                            socket = mmServerSocket.accept();

                            if (socket != null) {
                                // A connection was accepted. Perform work associated with
                                // the connection in a separate thread.
                                manageMyConnectedSocket(socket);
                                socket.close();
                            }
                        }
                    }

                } catch (IOException e) {
                    Log.d(Globals.TAG, "No se ha establecido conexión del socket server");
                }

                closeServerSocket();

                return null;
            }
        }.execute();

    }



    @Override
    protected void onStop() {
        super.onStop();

        btListening=false;
        closeServerSocket();
    }


    private void manageMyConnectedSocket(BluetoothSocket socket) {

        InputStream mmInputStream = null;
        OutputStream mmOutputStream = null;

        String inString=null;

        // Get the input and output streams; using temp objects because
        // member streams are final.
        try {
            mmInputStream = socket.getInputStream();
            mmOutputStream = socket.getOutputStream();


            SharedPreferences settings = getSharedPreferences(Globals.CONFIGURACION, 0);
            String btMac = settings.getString(Globals.BT_MAC, "NULL");
            String firebaseId = FirebaseInstanceId.getInstance().getToken();

            JSONObject data = new JSONObject();
            try {
                data.put(Globals.NFCBT_BT_MAC, btMac);
                data.put(Globals.NFCBT_FIREBASE_ID, firebaseId);
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }

            Log.d(Globals.TAG, "BT send message: " + data.toString());

            mmOutputStream.write(data.toString().getBytes(Charset.forName("UTF-8")));
            mmOutputStream.write("\n".getBytes());

            BufferedReader reader = new BufferedReader(new InputStreamReader(mmInputStream));


            inString = reader.readLine();

            if (inString != null) {
                Log.d(Globals.TAG, "BT message received: " + inString);

                String remoteBtMac = null;
                String remoteFirebaseId = null;

                try {
                    JSONObject receivedData = new JSONObject(inString);
                    remoteBtMac = receivedData.getString(Globals.NFCBT_BT_MAC);
                    remoteFirebaseId = receivedData.getString(Globals.NFCBT_FIREBASE_ID);
                    SharedPreferences.Editor editor = settings.edit();
                    editor.putString(Globals.REMOTE_BT_MAC, remoteBtMac);
                    editor.putString(Globals.REMOTE_FB_REGISTRATION_ID, remoteFirebaseId);
                    editor.putString(Globals.FB_REGISTRATION_ID, firebaseId);
                    editor.apply();
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            LinearLayout linearLayout = (LinearLayout) findViewById(R.id.layoutBtServer);

                            linearLayout.setAlpha(0.0f);
                            linearLayout.animate().alpha(1.0f).setDuration(1000);

                        }
                    });
                }

            }

            mmOutputStream.close();
            mmInputStream.close();
        } catch (IOException e) {
            Log.d(Globals.TAG, "Error manejando la conexión Bluetooth", e);
        }
    }

}

package es.gpsou.vehiclealarm;

import android.bluetooth.BluetoothAdapter;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.iid.FirebaseInstanceId;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.charset.Charset;

@RequiresApi(api = Build.VERSION_CODES.KITKAT)
public class NfcVehicleActivity extends AppCompatActivity implements NfcAdapter.ReaderCallback {

    private static final byte[] CLA_INS_P1_P2 = { 0x00, (byte)0xA4, 0x04, 0x00 };
    private static byte[] AID_ANDROID = null;
    NfcAdapter nfcAdapter=null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nfc_vehicle);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        if(BluetoothAdapter.getDefaultAdapter()!=null) {
            fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(NfcVehicleActivity.this, BtServerActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                    startActivity(intent);
                }
            });
        } else
            fab.setVisibility(View.INVISIBLE);

        AID_ANDROID=hexStringToByteArray(getString(R.string.aid));

        nfcAdapter=NfcAdapter.getDefaultAdapter(this);

        if(nfcAdapter==null) {
            Intent intent = new Intent(this, BtServerActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            startActivity(intent);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        try {
            if(!nfcAdapter.isEnabled()) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);

                builder.setMessage(R.string.nfc_dialog_activate);

                builder.setPositiveButton(R.string.nfc_dialog_activate_ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int dialogId) {
                        startActivity(new Intent(Settings.ACTION_NFC_SETTINGS));
                    }
                });
                builder.setNegativeButton(R.string.nfc_dialog_activate_cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        Intent intent = new Intent(NfcVehicleActivity.this, BtServerActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                        startActivity(intent);
                    }
                });
                builder.setCancelable(false);
                AlertDialog dialog = builder.create();
                dialog.show();
            }

            nfcAdapter.enableReaderMode(this, this, NfcAdapter.FLAG_READER_NFC_A | NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK,
                    null);
        } catch(Exception e) {
            Intent intent = new Intent(this, BtServerActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            startActivity(intent);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if(nfcAdapter != null)
            nfcAdapter.disableReaderMode(this);
    }

    @Override
    public void onTagDiscovered(Tag tag) {
        byte[] response;
        IsoDep isoDep = IsoDep.get(tag);

        try {
            isoDep.connect();
            response=isoDep.transceive(createSelectAidApdu(AID_ANDROID));
            if(new String(response, Charset.forName("UTF-8")).compareTo(Globals.NFC_SYNC_STRING)==0) {
                SharedPreferences settings=getSharedPreferences(Globals.CONFIGURACION, 0);
                String btMac=settings.getString(Globals.BT_MAC, "NULL");
                String firebaseId=settings.getString(Globals.FB_REGISTRATION_ID, "NULL");
//                String firebaseId=FirebaseInstanceId.getInstance().getToken();

                JSONObject data=new JSONObject();
                try {
                    data.put(Globals.NFCBT_BT_MAC, btMac);
                    data.put(Globals.NFCBT_FIREBASE_ID, firebaseId);
                } catch(JSONException e) {
                    throw new RuntimeException(e);
                }

                response = isoDep.transceive(data.toString().getBytes(Charset.forName("UTF-8")));


                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        LinearLayout linearLayout=(LinearLayout)findViewById(R.id.content_nfc_vehicle);

                        linearLayout.setAlpha(0.0f);
                        linearLayout.animate().alpha(1.0f).setDuration(1000);

                    }
                });

                String remoteBtMac=null;
                String remoteFirebaseId=null;
                try {
                    JSONObject receivedData = new JSONObject(new String(response));
                    remoteBtMac=receivedData.getString(Globals.NFCBT_BT_MAC);
                    remoteFirebaseId=receivedData.getString(Globals.NFCBT_FIREBASE_ID);
                    SharedPreferences.Editor editor=settings.edit();
                    editor.putString(Globals.REMOTE_BT_MAC, remoteBtMac);
                    editor.putString(Globals.REMOTE_FB_REGISTRATION_ID, remoteFirebaseId);
                    editor.putString(Globals.FB_REGISTRATION_ID, firebaseId);
                    editor.apply();
                } catch(JSONException e) {
                    e.printStackTrace();
                }
            }
            isoDep.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private byte[] createSelectAidApdu(byte[] aid) {
        byte[] result = new byte[6 + aid.length];
        System.arraycopy(CLA_INS_P1_P2, 0, result, 0, CLA_INS_P1_P2.length);
        result[4] = (byte)aid.length;
        System.arraycopy(aid, 0, result, 5, aid.length);
        result[result.length - 1] = 0;
        return result;
    }

    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }
}

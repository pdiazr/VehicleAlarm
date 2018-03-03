package es.gpsou.vehiclealarm;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.DialogInterface;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.provider.Settings;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;

public class NfcMonitorActivity extends AppCompatActivity {

    ProgressDialog ringProgressDialog=null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nfc_monitor);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        if(BluetoothAdapter.getDefaultAdapter()!=null) {
            fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(NfcMonitorActivity.this, BtClientActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                    startActivity(intent);
                }
            });
        } else
            fab.setVisibility(View.INVISIBLE);
    }

    @Override
    protected void onStart() {
        super.onStart();

        NfcAdapter nfcAdapter=NfcAdapter.getDefaultAdapter(this);
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
                    Intent intent = new Intent(NfcMonitorActivity.this, BtClientActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                    startActivity(intent);
                }
            });
            builder.setCancelable(false);
            AlertDialog dialog = builder.create();
            dialog.show();
        }

        HCEService.active=true;
    }

    @Override
    protected void onStop() {
        super.onStop();

        HCEService.active=false;

        PairingThread.active=false;

        if(ringProgressDialog!=null)
            ringProgressDialog.dismiss();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        ringProgressDialog = ProgressDialog.show(this, getString(R.string.nfc_monitor_dialog_title), getString(R.string.nfc_monitor_dialog_info), true);
        //you usually don't want the user to stop the current process, and this will make sure of that
        ringProgressDialog.setCancelable(true);
        ringProgressDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                PairingThread.active=false;
            }
        });
        ringProgressDialog.show();
    }
}

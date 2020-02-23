package es.gpsou.vehiclealarm;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

import java.util.ArrayList;
import java.util.List;

import static android.content.Intent.FLAG_ACTIVITY_NO_ANIMATION;

public class ConfigureActivity extends AppCompatActivity {
    private static final int ACCOUNT_PERMISSION=1;
    private static final int LOCATION_PERMISSION=2;
    private static final int MY_PERMISSIONS_REQUEST_ACCOUNTS = 3;
    private static final int REQUEST_GOOGLE_PLAY_SERVICES=9000;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_configure);

        String btMac=getBluetoothMac(this);

        SharedPreferences settings=getSharedPreferences(Globals.CONFIGURACION, 0);
        SharedPreferences.Editor editor=settings.edit();
        editor.putString(Globals.BT_MAC, btMac);
        editor.apply();
    }

    @Override
    protected void onStart() {
        super.onStart();

        PreferenceManager.setDefaultValues(this, Globals.CONFIGURACION, MODE_PRIVATE, R.xml.vehicle_preferences, false);
        SharedPreferences settings=getSharedPreferences(Globals.CONFIGURACION, 0);

        String appMode=settings.getString(Globals.APP_MODE, Globals.NULL);

        if (appMode.compareTo(Globals.IN_VEHICLE_MODE)==0 ||
            appMode.compareTo(Globals.MONITORING_MODE)==0) {
            finish();
        }

        View v=findViewById(R.id.imageMonitor);
        v.setTranslationY(0.0f);
        v.setAlpha(1.0f);
        v.setVisibility(View.VISIBLE);
        v=findViewById(R.id.imageVehicle);
        v.setTranslationY(0);
        v.setAlpha(1.0f);
        v.setVisibility(View.VISIBLE);
        v=findViewById(R.id.separator);
        v.setVisibility(View.VISIBLE);

        BTProximity.getInstance(ConfigureActivity.this).configureTimeout();

    }

    public void configureVehicle(View v) {
//        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            GoogleApiAvailability API=GoogleApiAvailability.getInstance();
            int resultCode=API.isGooglePlayServicesAvailable(this);
            if(resultCode == ConnectionResult.SUCCESS) {

                int permissionLocation=ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
                int permissionRec=ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO);

                List<String> listPermissionsNeeded = new ArrayList<>();
                if (permissionLocation != PackageManager.PERMISSION_GRANTED) {
                    listPermissionsNeeded.add(Manifest.permission.ACCESS_FINE_LOCATION);
                }
                if (permissionRec != PackageManager.PERMISSION_GRANTED) {
                    listPermissionsNeeded.add(Manifest.permission.RECORD_AUDIO);
                }

                if (!listPermissionsNeeded.isEmpty()) {
                    ActivityCompat.requestPermissions(this,
                            listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]), MY_PERMISSIONS_REQUEST_ACCOUNTS);
                } else {
                    startConfigureVehicle();
                }

/*                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION);
                } else {
                    startConfigureVehicle();
                } */
            } else if (resultCode==ConnectionResult.SERVICE_MISSING ||
                    resultCode==ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED ||
                    resultCode==ConnectionResult.SERVICE_DISABLED){
                API.getErrorDialog(this, resultCode, REQUEST_GOOGLE_PLAY_SERVICES).show();
            } else {
                Toast.makeText(this, R.string.conf_google_api_not_available,
                        Toast.LENGTH_LONG).show();
            }
//        } else {
//            Toast.makeText(this, "Emparejamiento vía Bluetooth no disponible actualmente",
//                    Toast.LENGTH_LONG).show();
//        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode==ACCOUNT_PERMISSION) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startConfigureMonitor();
            } else {
                Toast.makeText(this, R.string.conf_account_permission_reject,
                        Toast.LENGTH_LONG).show();
            }
            return;
        } else if (requestCode==LOCATION_PERMISSION) {
            if (grantResults.length > 0
                    && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, R.string.conf_location_permission_reject,
                        Toast.LENGTH_LONG).show();

            }
            startConfigureVehicle();
        } else if (requestCode==MY_PERMISSIONS_REQUEST_ACCOUNTS) {
            if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, R.string.conf_location_permission_reject,
                        Toast.LENGTH_LONG).show();
            }
            startConfigureVehicle();
        }
    }

    public void configureMonitor(View v) {
/*            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.GET_ACCOUNTS) != PackageManager.PERMISSION_GRANTED) {

                if(ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.GET_ACCOUNTS)) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);

                    builder.setMessage(R.string.conf_dialog_msg);

                    builder.setPositiveButton(R.string.conf_dialog_ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            ActivityCompat.requestPermissions(ConfigureActivity.this, new String[] {Manifest.permission.GET_ACCOUNTS}, 1);
                        }
                    });
                    AlertDialog dialog = builder.create();
                    dialog.show();
                } else {
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.GET_ACCOUNTS}, ACCOUNT_PERMISSION);
                }
            } else {
                startConfigureMonitor();
            }
*/
        startConfigureMonitor();

    }

    private void startConfigureVehicle() {
      /*                    final View androidCarView = findViewById(R.id.imageVehicle);
                    Intent intent = new Intent(this, NfcVehicleActivity.class);
                    // create the transition animation - the images in the layouts
                    // of both activities are defined with android:transitionName="robot"
                    ActivityOptions options = ActivityOptions
                            .makeSceneTransitionAnimation(this, androidCarView, "car");
                    // start the new activity
                    startActivity(intent, options.toBundle()); */

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            View hide = findViewById(R.id.separator);
            hide.setVisibility(View.INVISIBLE);
            hide = findViewById(R.id.imageMonitor);
            hide.setVisibility(View.INVISIBLE);
            View v = findViewById(R.id.imageVehicle);

            v.setTranslationY(0);
            v.animate().translationYBy(v.getHeight() / 2).alpha(0.5f).setDuration(500).withEndAction(new Runnable() {
                @Override
                public void run() {
                    Intent intent = new Intent(ConfigureActivity.this, NfcVehicleActivity.class);
                    intent.setFlags(FLAG_ACTIVITY_NO_ANIMATION);
                    startActivity(intent);
                }
            });
        } else {
            Intent intent = new Intent(ConfigureActivity.this, BtServerActivity.class);
            startActivity(intent);
        }
    }

    private void startConfigureMonitor() {
/*        final Account account=FBGroupManager.getAccount(this);

        if(account==null) {
            Toast.makeText(this, R.string.conf_account_not_available,
                    Toast.LENGTH_LONG).show();
        } else {
//            PairingThread.account=account;

            SharedPreferences settings=getSharedPreferences(Globals.CONFIGURACION, 0);

            SharedPreferences.Editor editor=settings.edit();
            editor.putString(Globals.GOOGLE_ACCOUNT, account.name);
            editor.apply();
*/
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                View hide = findViewById(R.id.separator);
                hide.setVisibility(View.INVISIBLE);
                hide = findViewById(R.id.imageVehicle);
                hide.setVisibility(View.INVISIBLE);
                View v = findViewById(R.id.imageMonitor);

                v.setTranslationY(0);
                v.animate().translationYBy(-v.getHeight() / 2).alpha(0.5f).setDuration(500).withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        Intent intent = new Intent(ConfigureActivity.this, NfcMonitorActivity.class);
                        intent.setFlags(FLAG_ACTIVITY_NO_ANIMATION);
                        startActivity(intent);
                    }
                });
            } else {
                Intent intent = new Intent(this, BtClientActivity.class);
                startActivity(intent);
            }
/*        } */
    }

    private String getBluetoothMac(final Context context) {

        String result = null;
        if (context.checkCallingOrSelfPermission(Manifest.permission.BLUETOOTH)
                == PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // Hardware ID are restricted in Android 6+
                // https://developer.android.com/about/versions/marshmallow/android-6.0-changes.html#behavior-hardware-id
                // Getting bluetooth mac via reflection for devices with Android 6+
                result = android.provider.Settings.Secure.getString(context.getContentResolver(),
                        "bluetooth_address");
            } else {
                BluetoothAdapter bta = BluetoothAdapter.getDefaultAdapter();
                result = bta != null ? bta.getAddress() : null;
            }
        }
        return result;
    }

}

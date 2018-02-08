package es.gpsou.vehiclealarm;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

public class VehicleSettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Display the fragment as the main content.
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new VehicleSettingsFragment())
                .commit();
    }
}

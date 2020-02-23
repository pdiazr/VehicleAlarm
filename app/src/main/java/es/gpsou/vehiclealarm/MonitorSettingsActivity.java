package es.gpsou.vehiclealarm;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

public class MonitorSettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Display the fragment as the main content.
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new MonitorSettingsFragment())
                .commit();
    }

}

package es.gpsou.vehiclealarm;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;


public class VehicleSettingsFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getPreferenceManager().setSharedPreferencesName(Globals.CONFIGURACION);

        addPreferencesFromResource(R.xml.vehicle_preferences);

        Preference preference = null;
        SharedPreferences settings=getActivity().getSharedPreferences(Globals.CONFIGURACION, 0);

        preference = findPreference(getString(R.string.settings_sensor_sensitivity));
        preference.setTitle(getString(R.string.settings_sensor_sensitivity_title) + settings.getInt(getString(R.string.settings_sensor_sensitivity), 0));

        preference = findPreference(getString(R.string.settings_location_radius));
        preference.setTitle(getString(R.string.settings_location_radius_title) + settings.getInt(getString(R.string.settings_location_radius), 0) + " m");
    }

    @Override
    public void onStart() {

        super.onStart();

        SharedPreferences settings=getActivity().getSharedPreferences(Globals.CONFIGURACION, 0);
        settings.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onStop() {

        super.onStop();

        SharedPreferences settings=getActivity().getSharedPreferences(Globals.CONFIGURACION, 0);
        settings.unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

        if (key.equals(getString(R.string.settings_sensor_sensitivity))) {
            Preference preference = findPreference(key);
            preference.setTitle(getString(R.string.settings_sensor_sensitivity_title) + sharedPreferences.getInt(key, 0));
        } else if (key.equals(getString(R.string.settings_location_radius))) {
            Preference preference = findPreference(key);
            preference.setTitle(getString(R.string.settings_location_radius_title) + sharedPreferences.getInt(key, 0) + " m");
        }
    }
}

package es.gpsou.vehiclealarm;

import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;


public class MonitorSettingsFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String TITLES="titles";
    private static final String URIS="uris";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getPreferenceManager().setSharedPreferencesName(Globals.CONFIGURACION);

        addPreferencesFromResource(R.xml.monitor_preferences);

        Preference preference = null;
        SharedPreferences settings=getActivity().getSharedPreferences(Globals.CONFIGURACION, 0);

        preference = findPreference(getString(R.string.settings_sensor_tone));
        preference.setSummary(RingtoneManager.getRingtone(getActivity(), Uri.parse(settings.getString(getString(R.string.settings_sensor_tone), ""))).getTitle(getActivity()));

        preference = findPreference(getString(R.string.settings_location_tone));
        preference.setSummary(RingtoneManager.getRingtone(getActivity(), Uri.parse(settings.getString(getString(R.string.settings_location_tone), ""))).getTitle(getActivity()));

        preference = findPreference(getString(R.string.settings_turn_server));
        preference.setSummary(settings.getString(getString(R.string.settings_turn_server), ""));

        preference = findPreference(getString(R.string.settings_turn_username));
        preference.setSummary(settings.getString(getString(R.string.settings_turn_username), ""));
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

        if(key.equals(getString(R.string.settings_sensor_tone))) {
            Preference preference = findPreference(key);
            preference.setSummary(RingtoneManager.getRingtone(getActivity(), Uri.parse(sharedPreferences.getString(key, ""))).getTitle(getActivity()));
        } else if (key.equals(getString(R.string.settings_location_tone))) {
            Preference preference = findPreference(key);
            preference.setSummary(RingtoneManager.getRingtone(getActivity(), Uri.parse(sharedPreferences.getString(key, ""))).getTitle(getActivity()));
        } else if (key.equals(getString(R.string.settings_turn_server))) {
            Preference preference = findPreference(key);
            preference.setSummary(RingtoneManager.getRingtone(getActivity(), Uri.parse(sharedPreferences.getString(key, ""))).getTitle(getActivity()));
        } else if (key.equals(getString(R.string.settings_turn_username))) {
            Preference preference = findPreference(key);
            preference.setSummary(RingtoneManager.getRingtone(getActivity(), Uri.parse(sharedPreferences.getString(key, ""))).getTitle(getActivity()));
        }
    }
}

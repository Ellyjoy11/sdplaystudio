package com.elena.sdplay;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;

public class SettingsFragment extends PreferenceFragment implements
		OnSharedPreferenceChangeListener {

	private final String[] keys = { "rows", "rnd_rows", "cycles", "top_count",
			"medium_file", "large_file", "buff_size" };

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.pref_general);
		setSummary();
	}

	public void onPause() {
		super.onPause();
		Context context = getActivity();
		PreferenceManager.getDefaultSharedPreferences(context)
				.unregisterOnSharedPreferenceChangeListener(this);
	}

	public void onResume() {
		super.onResume();
		setSummary();
		Context context = getActivity();
		PreferenceManager.getDefaultSharedPreferences(context)
				.registerOnSharedPreferenceChangeListener(this);
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		setSummary();
	}

	private void setSummary() {

		SharedPreferences userPref = PreferenceManager
				.getDefaultSharedPreferences(getActivity());
		for (int i = 0; i < 7; i++) {
			String key_string = keys[i];
			Preference pref = (Preference) findPreference(key_string);
			String value = userPref.getString(key_string, "5");
			pref.setSummary(value);
		}
	}

}

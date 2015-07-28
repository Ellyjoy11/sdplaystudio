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

	private final String[] keys = { "rows", "rnd_rows", "top_count", //"cycles" - for read all cycles
			"medium_file", "large_file", "buff_size", "journal" };
    SharedPreferences userPref;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        userPref = PreferenceManager
                .getDefaultSharedPreferences(getActivity());
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

		for (int i = 0; i < keys.length; i++) {
			String key_string = keys[i];
			Preference pref = (Preference) findPreference(key_string);
			String value = userPref.getString(key_string, "5");
			pref.setSummary(value);
		}
		Preference prefThreads = (Preference) findPreference("threads");
		//SharedPreferences.Editor editor = userPref.edit();
		String val = Integer.toString(MainActivity.defThreads);
		//if (!userPref.getString("threads", "undef").equals("undef")){
		    val = userPref.getString("threads", "1");
		//}
		//editor.putString("threads", val);
		//editor.commit();
		prefThreads.setSummary(val);
	}

}

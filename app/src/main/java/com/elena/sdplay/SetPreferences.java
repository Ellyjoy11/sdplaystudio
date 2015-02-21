package com.elena.sdplay;

import android.app.Activity;
import android.os.Bundle;

public class SetPreferences extends Activity {
	public final String MYPREFS = "my shared prefs";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Display the preference fragment as the main content.
		getFragmentManager().beginTransaction()
				.replace(android.R.id.content, new SettingsFragment()).commit();
	}
}

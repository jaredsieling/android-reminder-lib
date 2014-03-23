package org.ohmage.reminders.notif;

import android.os.Bundle;
import android.preference.PreferenceActivity;

import org.ohmage.reminders.R;
public class NotifSettingsActivity extends PreferenceActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		addPreferencesFromResource(R.xml.notification_settings);
	}
}

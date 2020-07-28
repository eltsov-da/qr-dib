package com.appindesign.dib;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

public class Preferences extends PreferenceActivity
{
	OnSharedPreferenceChangeListener listener;
	
	@Override
	public void onCreate( final Bundle savedInstanceState )
	{
		super.onCreate( savedInstanceState );
		addPreferencesFromResource( R.xml.preferences );		
		populateSummaries();
		
		listener = new OnSharedPreferenceChangeListener()
		{
			public void onSharedPreferenceChanged( SharedPreferences prefs, String key )
			{
				populateSummaries();
			}
		};		
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences( this );
		preferences.registerOnSharedPreferenceChangeListener( listener );	
	}
	
	public void populateSummaries()
	{
		String sDefaultValue;
		Preference preference;
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences( this );
		
		//Competitor surname
		preference = getPreferenceScreen().findPreference( "prefkey_surname" );
		sDefaultValue = getString( R.string.prefdef_surname );
		preference.setSummary( preferences.getString( "prefkey_surname", sDefaultValue ) );
		
		//Competitor forename
		preference = getPreferenceScreen().findPreference( "prefkey_forename" );
		sDefaultValue = getString( R.string.prefdef_forename );
		preference.setSummary( preferences.getString( "prefkey_forename", sDefaultValue ) );
		
		//Club
		preference = getPreferenceScreen().findPreference( "prefkey_club" );
		sDefaultValue = getString( R.string.prefdef_club );
		preference.setSummary( preferences.getString( "prefkey_club", sDefaultValue ) );
		
		//Class
		preference = getPreferenceScreen().findPreference( "prefkey_class" );
		sDefaultValue = getString( R.string.prefdef_class );
		preference.setSummary( preferences.getString( "prefkey_class", sDefaultValue ) );
		
		//Identifier
		preference = getPreferenceScreen().findPreference( "prefkey_id" );
		sDefaultValue = getString( R.string.prefdef_id );
		preference.setSummary( preferences.getString( "prefkey_id", sDefaultValue ) );

		//Site
		preference = getPreferenceScreen().findPreference( "prefkey_site" );
		sDefaultValue = getString( R.string.prefdef_site );
		preference.setSummary( preferences.getString( "prefkey_site", sDefaultValue ) );
	}
}
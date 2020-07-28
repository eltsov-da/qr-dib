package com.appindesign.diblibrary;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.preference.PreferenceManager;

public class Competitor {
	
	private String forename;
	private String surname;
	private String classification;
	private String club;
	private String identifier;
	
	public Competitor( Context context ) 
	{
		SharedPreferences defSharedPrefs = PreferenceManager.getDefaultSharedPreferences( context );
		
		forename = defSharedPrefs.getString( "prefkey_forename", context.getString(R.string.prefdef_forename) );
		surname = defSharedPrefs.getString( "prefkey_surname", context.getString(R.string.prefdef_surname) ); 
		classification = defSharedPrefs.getString( "prefkey_class", context.getString(R.string.prefdef_class) );
		club = defSharedPrefs.getString( "prefkey_club", context.getString(R.string.prefdef_club) );
		identifier = defSharedPrefs.getString( "prefkey_id", context.getString(R.string.prefdef_id) );	
	}
	
	public Competitor ( Cursor cursor )
	{		
		forename = cursor.getString( Msg.FORENAME );
		surname = cursor.getString( Msg.SURNAME );
		classification = cursor.getString( Msg.CLASSIFICATION );
		club = cursor.getString( Msg.CLUB );
		identifier = cursor.getString( Msg.IDENTIFIER );
	}
	
	public String getForename() {
		return forename;
	}

	public String getSurname() {
		return surname;
	}

	public String getClassification() {
		return classification;
	}

	public String getClub() {
		return club;
	}

	public String getIdentifier() {
		return identifier;
	}
	
	/**
	 * 
	 * @return forename + space + surname
	 */
	public String getFullName() {
		return ( forename + " " + surname );
	}	
}

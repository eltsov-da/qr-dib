package com.appindesign.dib;

import java.util.ArrayList;

import com.appindesign.diblibrary.Msg;
import com.appindesign.diblibrary.Punch;

import android.content.Context;
import android.database.Cursor;

public class Dibber
{
	private ArrayList<Punch> dibs;
	
	Dibber( Context context )
	{
		final int nCODE_NDX = 1;
		final int nTIMESTAMP_NDX = 2;
		
		Punch oPunch;		
		Cursor cursor = context.getContentResolver().query( LocalStore.CONTENT_URI, null, null, null, null );

		dibs = new ArrayList<Punch>();
		
		if ( cursor.moveToFirst() ) 
		{
			do 
			{
				String[] sDibArray;
				String f1;
				sDibArray=cursor.getString( nCODE_NDX ).split( ":" );
				if(sDibArray.length==1)
				{
					f1="0000000000";
				}
				else
				{
					f1=sDibArray[1];
				}
				oPunch = new Punch( sDibArray[0], cursor.getLong( nTIMESTAMP_NDX ),f1 );
				dibs.add( oPunch );
			} 
			while ( cursor.moveToNext() );	
		}
		cursor.close();
	}
	
	public String getCode( int i )
	{
		if ( dibs != null && i < dibs.size() )
			return dibs.get(i).getCode();
		else
			return ( "" );
	}

	public Long getTimestamp( int i )
	{
		if ( dibs != null && i < dibs.size() )
			return dibs.get(i).getTimestamp();
		else
			return ( Msg.INVALID_TIME );
	}
	
	public int getNumberOfDibs()
	{	
		if ( dibs != null )
			return dibs.size();
		else
			return 0;
	}
	
	/**
	 * Returns the position of a code in the Dibber's list of codes.
	 * @param sCode The code being sought.
	 * @param nStartingAt The position the search starts at.
	 * @return The code's position or -1 if not found.
	 */	
	public int getIndex( String sCode, int nStartingAt )
	{
		boolean bFound = false;
		int nIndex = -1;
		int i = nStartingAt;
		
		while ( dibs != null && !bFound && ( i < dibs.size() ) )
		{
			if ( dibs.get(i).getCode().equalsIgnoreCase( sCode ) )
			{
				bFound = true;
				nIndex = i;
			}
			else i++;
		}
		return nIndex;
	}
	
	public void removePunch( int i )
	{
		if ( dibs != null && i < dibs.size() )
			dibs.remove(i);
	}
}
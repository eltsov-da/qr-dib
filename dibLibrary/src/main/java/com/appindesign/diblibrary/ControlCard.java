package com.appindesign.diblibrary;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TimeZone;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.preference.PreferenceManager;

public class ControlCard 
{
	private String name;
	private String length;
	private ArrayList<Punch> dibs;
	private String shortMsgStatus;
	private Long shortMsgTime;

	/**
	 * Constructs a ControlCard from the course data in shared preferences. 
	 * The data is read from a tag. 
	 * The tag format is [<CRS>]<STR>[<CTRL>*]<FIN>[<NAME>][<LENGTH>]
	 * @param context The main activity...needed to use the activity's getString method.
	 */
	public ControlCard( Context context )
	{
		//Declarations
		boolean isValid = false;
		
		final int nNAME_NDX = 1;	//The position of course name relative to FIN.
		final int nLENGTH_NDX = 2; 	//The position of course length relative to FIN.		
		int nStrIndex = -1;			//The position of STR in the course fields.
		int nFinIndex = -1; 		//The position of FIN in the course fields.
		
		SharedPreferences defaultSharedPreferences;
		String sCourse;
		List<String> sCourseFields;
		List<String> sDibFields;
		Punch dib;
		
		//Get the course data from the shared preferences.		
		defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences( context );
		sCourse = defaultSharedPreferences.getString( "prefkey_course_data", "" );
		sCourseFields = Arrays.asList( sCourse.split( "," ) );
		
		//Trim white space from course fields.
		for ( int i=0; i < sCourseFields.size(); i++ ) {
			sCourseFields.set( i, sCourseFields.get(i).trim() );
		}

		if(sCourseFields.get(0).length()>4)
		{
			for ( int i=0; i < sCourseFields.size(); i++ ) {
		  		sDibFields = Arrays.asList( sCourseFields.get(i).split( ":" ) );
				if(sDibFields.get(0).toString().equals(context.getString(R.string.str)))
				{
					nStrIndex =i;
				}
				if(sDibFields.get(0).toString().equals(context.getString(R.string.fin)))
				{
					nFinIndex =i;
				}

			}

		}
		else {
			//Check the validity of the course.
			nStrIndex = sCourseFields.indexOf(context.getString(R.string.str));
			nFinIndex = sCourseFields.indexOf(context.getString(R.string.fin));

		}
		isValid = (nStrIndex == 0 && nFinIndex > nStrIndex) ? true : false;
		dibs = new ArrayList<Punch>();
		if ( isValid )
		{
			//ControlCard Name
			if ( sCourseFields.size() > nFinIndex + nNAME_NDX )
				{ name = sCourseFields.get( nFinIndex + nNAME_NDX ).toString(); }
			else
				{ name = context.getString( R.string.prefdef_course_name ); }
			
			//ControlCard Length
			try {
			if ( sCourseFields.size() > nFinIndex + nLENGTH_NDX )
				{	length = sCourseFields.get( nFinIndex + nLENGTH_NDX ).toString();
					//Convert length to metres.
					if ( length.indexOf("km") > -1 ) {
						 length = length.substring( 0, length.indexOf("km") );
						 length = String.valueOf( (int) Math.round( 1000*Float.parseFloat( length ) ) );
					 }
				 	if ( length.indexOf("m") > -1 ) {
						 length = length.substring( 0, length.indexOf("m") );
						 length = String.valueOf( (int) Math.round( Float.parseFloat( length ) ) );
				 	}
				 	length = String.valueOf( (int) Math.round( Float.parseFloat( length ) ) );
			 	}
			else
				{	length = context.getString( R.string.prefdef_course_length ); };				
			}
			catch ( NumberFormatException e ) {
				length = context.getString( R.string.prefdef_course_length) ;
			}
			
			//dibs
			for ( int i=nStrIndex; i<=nFinIndex; i++ )
			{
				String f1;
				sDibFields = Arrays.asList( sCourseFields.get(i).split( ":" ) );
				if(sDibFields.size()==1)
				 {
					f1="0000000000";
				 }
				else
				{
					f1=sDibFields.get(1).toString();
				}
				dib = new Punch( sDibFields.get(0), Msg.INVALID_TIME,f1 );
				dibs.add( dib );
			}
		}
		else
		{
			name = context.getString( R.string.prefdef_course_name );
			length = context.getString( R.string.prefdef_course_length );
			dibs = null;
		}
		
		//shortMsgStatus and shortMsgTime
		shortMsgStatus = "";
		shortMsgTime = 0l;	
	}
	
	public ControlCard( Cursor cursor )
	{
		Punch dib;
		String sSplits; String[] sSplitsArray;
		String sControls; String[] sControlsArray;	 String[] sDibArray;
		
		name = cursor.getString( Msg.NAME );
		length = cursor.getString( Msg.LENGTH );
		
		//Check if there are codes and splits.

		sControls = cursor.getString( Msg.CODES );
		sSplits = cursor.getString( Msg.TIMESTAMPS );
		
		if ( sControls !=null && sSplits != null )
		{
			sControlsArray = sControls.split( Msg.SPLIT_DELIMITER );
			sSplitsArray = sSplits.split( Msg.SPLIT_DELIMITER );
			long lBaseTimestamp = Long.valueOf( sSplitsArray[0] );
			
			dibs = new ArrayList<Punch>();
			sDibArray=sControlsArray[0].split( ":" );
			String f1;
			if(sDibArray.length==1)
			{
				f1="0000000000";
			}
			else
			{
				f1=sDibArray[1];
			}
			dib = new Punch( sDibArray[0], Long.valueOf( sSplitsArray[0] )*Msg.TIME_DIVISOR,f1 );
			dibs.add( dib );
			for ( int i=1; i < sSplitsArray.length; i++ )
			{
				sDibArray=sControlsArray[i].split( ":" );
				if(sDibArray.length==1)

				{
					f1="0000000000";
				}
				else
				{f1=sDibArray[1];}


				dib = new Punch( sDibArray[0], ( Long.valueOf( sSplitsArray[i] ) + lBaseTimestamp ) * Msg.TIME_DIVISOR,f1 );
				dibs.add( dib );
			}			
		}
		
		shortMsgStatus = cursor.getString( Msg.STATUS );
		if ( cursor.isNull( Msg.TIME) ) {	             
			try {
				SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
		        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
				shortMsgTime = sdf.parse( cursor.getString( Msg.OLD_TIME ) ).getTime();} 
			catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();}
		}
		else {
			shortMsgTime = cursor.getLong( Msg.TIME )*Msg.TIME_DIVISOR;
		}
	}

	public String getName() {
		return name;
	}

	public String getLength() {
		return length;
	}
	public String getInid( int i ) {
		if ( dibs != null && i >= 0 && i < dibs.size() )
			return dibs.get(i).getIntid();
		else
			return "";
	}
	public String getCode( int i ) {
		if ( dibs != null && i >= 0 && i < dibs.size() )
			return dibs.get(i).getCode();
		else
			return "";
	}
	public String getCode( String intId ) {
		for(int i=0;i<dibs.size();i++)
		{
			if(dibs.get(i).getIntid().equalsIgnoreCase(intId))
			return (dibs.get(i).getCode().toString());
		}
			return "";
	}

	/**
	 * Counts the number of dibs excluding STR and FIN.
	 * @return Number of dibs minus two, or 0 if the course was invalid.
	 */
	public int getNumberOfControls() {
		if ( dibs != null && dibs.size() >= 2 )
			return ( dibs.size()-2 );
		else
			return 0;
	}
	
	public long getTimestamp( int i )
	{
		if ( dibs != null && i >= 0 && i < dibs.size() )
			return dibs.get(i).getTimestamp();
		else
			return ( Msg.INVALID_TIME );
	}
	
	public void setTimestamp( int i, long timestamp )
	{
		if ( dibs != null && i >=0 && i < dibs.size() )
			dibs.get(i).setTimestamp( timestamp );
	}
	
	/**
	 * Assumes the first dib is STR. This is safe as the ControlCard constructor loops from STR.
	 * @return Returns Msg.INVALID_TIME for an invalid course.
	 */
	public long getStartTimestamp()
	{
		if ( dibs != null )
			return getTimestamp(0);
		else
			return Msg.INVALID_TIME;
	}
	
	/**
	 * Assumes the finish dib is FIN - this is safe as the CTOR loops from 
	 * STR to FIN dib in constructing its list of dibs.
	 * 
	 * @return Returns Msg.INVALID_TIME for an invalid course.
	 */
	public long getFinishTimestamp()
	{
		if ( dibs != null )
			return getTimestamp( dibs.size() -1 );
		else
			return Msg.INVALID_TIME;
	}

	/**
	 * Calculates an elapsed time if you have visited STR and dib "i".
	 * 
	 * @param i The dib you want the elapsed time for.
	 * @return Elapsed time in milli-seconds, or Msg.INVALID_TIME if you missed the dib or this
	 * is your first control.
	 */
	public long getElapsedTime( int i )
	{
		long elapsedTime = Msg.INVALID_TIME;

		if ( !( getTimestamp(i) == Msg.INVALID_TIME ) && !( getStartTimestamp() == Msg.INVALID_TIME ) )
		{	//See comment about split time calculation for explanation of the nTimeDivider.
			elapsedTime =  1000 * ( getTimestamp(i)/1000 - getStartTimestamp()/1000 );
		}			
		return elapsedTime;
	}
	
	/**
	 * Calculates a split time if you have visited dib "i" and the preceding dib - note 
	 * split time here is the DIFFERENCE between the timestamps of the two dibs. If you want 
	 * the timestamp of the split at a dib use getTimestamp(i).
	 * 
	 * @param i The dib you want the split time for.
	 * @return Split time in milli-seconds, or Msg.INVALID_TIME if you missed either dib or this 
	 * is your first control.
	 */
	public long getSplitTime( int i )
	{
		long lSplitTime = Msg.INVALID_TIME;

		if ( !( getTimestamp( i ) == Msg.INVALID_TIME ) && !( getTimestamp( i-1 ) == Msg.INVALID_TIME ) )
		{	//The strange looking procedure with the 1000 is to round the times to the nearest second
			//before calculating the split. This means the displayed timestamp accords with the splits.
			lSplitTime = 1000 * ( getTimestamp( i )/1000 - getTimestamp( i-1 )/1000 );
		}
		
		return ( lSplitTime );
	}	
	
	public long getCourseTime()
	{
		if ( dibs == null )
			return shortMsgTime;
		else 
			return ( Msg.TIME_DIVISOR*Math.max( getFinishTimestamp()/Msg.TIME_DIVISOR - getStartTimestamp()/Msg.TIME_DIVISOR, 0l ) );
	}
	
	/**
	 * If the course was invalid this will return dns.
	 * @param context
	 * @return dns, dnf, mp, ok (if more than one invalid condition applies the order dns has 
	 * highest precedence, mp lowest precedence).
	 */
	public String getResultStatus( Context context )
	{
		boolean bCourseStarted = false;
		boolean bCourseFinished = false;
		String sCourseStatus = context.getString( R.string.ok );

		//If the course was not valid you will have a null list of controls and will see get "dns" from this.
		if ( dibs == null )
		{
			return shortMsgStatus;
		}
		else
		{
			for ( int i=0; i < dibs.size(); i++ )
			{			
				//At some point you need to see a STR with a non-zero time stamp;
				if ( getCode(i).equals( context.getString( R.string.str ) ) && getTimestamp(i) != Msg.INVALID_TIME )
					bCourseStarted = true;

				//At some point you need to see a FIN with a non-zero time stamp.
				if ( getCode(i).equals( context.getString( R.string.fin ) ) && getTimestamp(i) != Msg.INVALID_TIME )
					bCourseFinished = true;
				
				if ( ( getTimestamp(i) == Msg.INVALID_TIME ) ) sCourseStatus = context.getString( R.string.mp );
			}
			if ( !bCourseFinished ) sCourseStatus = context.getString( R.string.dnf ); //dnf has precedence over mp
			if ( !bCourseStarted ) sCourseStatus = context.getString( R.string.dns ); //dns has precedence over dnf
			return sCourseStatus;		
		}
	}		
}
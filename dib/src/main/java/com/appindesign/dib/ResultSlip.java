package com.appindesign.dib;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.TimeZone;

import android.content.Context;
import android.util.Log;

import com.appindesign.diblibrary.ControlCard;
import com.appindesign.diblibrary.Msg;

public class ResultSlip {	
	
	private ArrayList<HashMap<String, String>> legResultsList;
	
	public ResultSlip( ControlCard course, Dibber dibber, Context context )
	{
		//Time formats.
		final SimpleDateFormat sdf_Local_HH_mm_ss = new SimpleDateFormat( "HH:mm:ss", Locale.getDefault() );
		final SimpleDateFormat sdf_UTC_HH_mm_ss = new SimpleDateFormat( "HH:mm:ss" );
		sdf_UTC_HH_mm_ss.setTimeZone( TimeZone.getTimeZone( "UTC" ) );
		
		//Map for result line.
		HashMap<String,String> legResult = new HashMap<String, String>();	
		
		//Create an array of result lines.
		legResultsList = new ArrayList<HashMap<String, String>>();
		if(!course.getName().toUpperCase().startsWith("РОГЕЙН")) {
			//Add headings to result slip.
			legResult.put("leg", context.getString(R.string.leg));
			legResult.put("code", context.getString(R.string.code));
			legResult.put("timestamp", context.getString(R.string.timestamp));
			legResult.put("splitTimestamp", context.getString(R.string.splitTime));
			legResult.put("elapsedTimestamp", context.getString(R.string.elapsedTime));
			legResultsList.add(legResult);

			//Add the controls from the course to the result slip.
			for (int i = 0; i < course.getNumberOfControls() + 2; i++) {
				legResult = new HashMap<String, String>();

				//Control Index
				legResult.put("leg", "(" + i + ")");
				if (i == 0) legResult.put("leg", context.getString(R.string.start));
				if (i == course.getNumberOfControls() + 1)
					legResult.put("leg", context.getString(R.string.finish));

				//Code
				legResult.put("code", course.getCode(i));

				//Timestamp
				if (course.getTimestamp(i) == Msg.INVALID_TIME)
					legResult.put("timestamp", context.getString(R.string.mp));
				else
					legResult.put("timestamp", sdf_Local_HH_mm_ss.format(course.getTimestamp(i)));

				//Split
				if (course.getSplitTime(i) == Msg.INVALID_TIME)
					legResult.put("splitTimestamp", context.getString(R.string.nullTime));
				else
					legResult.put("splitTimestamp", sdf_UTC_HH_mm_ss.format(course.getSplitTime(i)));
				if (i == 0) legResult.put("splitTimestamp", "");

				//Elapsed
				if (course.getElapsedTime(i) == Msg.INVALID_TIME)
					legResult.put("elapsedTimestamp", context.getString(R.string.nullTime));
				else
					legResult.put("elapsedTimestamp", sdf_UTC_HH_mm_ss.format(course.getElapsedTime(i)));
				if (i == 0) legResult.put("elapsedTimestamp", "");
				legResultsList.add(legResult);
			}

			//Add extra controls on the dibber to the result slip.
			for (int i = 0; i < dibber.getNumberOfDibs(); i++) {
				legResult = new HashMap<String, String>();
				legResult.put("leg", "*");
				legResult.put("code", dibber.getCode(i));
				legResult.put("timestamp", sdf_Local_HH_mm_ss.format(dibber.getTimestamp(i)));
				legResultsList.add(legResult);
			}
		}
		 else {
			long lStartTime = Msg.INVALID_TIME;
			long lPreTime = Msg.INVALID_TIME;
			long lFinTime = Msg.INVALID_TIME;

			legResult.put("leg", context.getString(R.string.leg));
			legResult.put("code", context.getString(R.string.code));
			legResult.put("timestamp", context.getString(R.string.timestamp));
			legResult.put("splitTimestamp", context.getString(R.string.splitTime));
			legResult.put("elapsedTimestamp", context.getString(R.string.elapsedTime));
			legResultsList.add(legResult);

			for (int i = 0; i < dibber.getNumberOfDibs(); i++)
			{
				legResult = new HashMap<String, String>();
				if (dibber.getCode(i).equalsIgnoreCase("STR")) {
					legResult.put("leg", "Старт");
					lStartTime=dibber.getTimestamp(i);
					lPreTime=dibber.getTimestamp(i);
				} else {
					if (dibber.getCode(i).equalsIgnoreCase("FIN")) {
						legResult.put("leg", "Финиш");
					lFinTime=dibber.getTimestamp(i);
					}
					else {
						legResult.put("leg", String.valueOf(i));
					}

				}


				legResult.put("code", dibber.getCode(i));
				legResult.put("timestamp", sdf_Local_HH_mm_ss.format(dibber.getTimestamp(i)));
				legResult.put("elapsedTimestamp", sdf_UTC_HH_mm_ss.format(dibber.getTimestamp(i)-lStartTime));
				legResult.put("splitTimestamp", sdf_UTC_HH_mm_ss.format(dibber.getTimestamp(i)-lPreTime));
				lPreTime=dibber.getTimestamp(i);

				legResultsList.add(legResult);
			}
			if(lFinTime>0) {
				long ct;
				try{
					ct=Integer.parseInt(course.getLength());
				}
				catch(NumberFormatException nfe)
				{
					ct=0;
				}
				long lLate =(lFinTime-lStartTime)-ct*1000;
				if(lLate>0)
				{
					legResult = new HashMap<String, String>();
					legResult.put("splitTimestamp", sdf_UTC_HH_mm_ss.format(lLate));
					legResult.put("leg", "Опозд.");
					legResultsList.add(legResult);

				}

			}
		}
	}

	public ArrayList<HashMap<String, String>> getResultSlipData() {
		return legResultsList;
	}
}
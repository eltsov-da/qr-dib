package com.appindesign.dib;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import com.appindesign.diblibrary.Competitor;
import com.appindesign.diblibrary.ControlCard;
import com.appindesign.diblibrary.Msg;

import android.nfc.NdefMessage;
import android.nfc.NfcAdapter;
import android.nfc.tech.NfcF;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.preference.PreferenceManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentFilter.MalformedMimeTypeException;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.support.v7.app.ActionBarActivity;
import android.telephony.SmsManager;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import javax.net.ssl.HttpsURLConnection;


public class Dib extends ActionBarActivity {

	final int mnQR_SCAN_ACTIVITY = 0;

	//Wakelock, NfcAdapter and SharedPreferences.
	WakeLock mWakeLock;	
	NfcAdapter mNfcAdapter;
	SharedPreferences mDefaultSharedPrefs;
	
	//Pending intent variables
	PendingIntent mNfcPendingIntent;
	IntentFilter[] mIntentFiltersArray;
	String[][] mNfcTechListsArray;
	
	Boolean isDibView = true;





	//----------Lifecycle methods----------
	// onCreate( Bundle savedInstanceState )
	// onResume()
	// onPause()
	
	/**
	 * Creates wakelock, prepares NFC (if Android v >= 14), forces into QR mode if Android v < 14.
	 */
	@Override
	protected void onCreate( Bundle savedInstanceState ) 
	{
		super.onCreate( savedInstanceState );
		setContentView( R.layout.main );

		mDefaultSharedPrefs = PreferenceManager.getDefaultSharedPreferences( this );
		
		createWakeLock();
		
		//If version >= 14 register NFC intent, otherwise force into QR mode.
		if ( Build.VERSION.SDK_INT >= 14 && getPackageManager().hasSystemFeature("android.hardware.nfc") )
		{
			prepareNfc();
			onNewIntent( getIntent() );
		}
		else //Force into QR mode if using earlier than 14.
		{
			SharedPreferences.Editor editor = mDefaultSharedPrefs.edit();
			editor.putBoolean( "prefkey_qrmode", true );
			editor.commit();
		}
		EditText editText = (EditText) findViewById(R.id.cp_id);


		editText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
											   @Override
											   public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
												   if (event == null) {
													   if (actionId == EditorInfo.IME_ACTION_DONE) ;
														   // Capture soft enters in a singleLine EditText that is the last EditText.
													   else if (actionId == EditorInfo.IME_ACTION_NEXT) ;
														   // Capture soft enters in other singleLine EditTexts
													   else return false;  // Let system handle all other null KeyEvents
												   } else if (actionId == EditorInfo.IME_NULL) {
													   // Capture most soft enters in multi-line EditTexts and all hard enters.
													   // They supply a zero actionId and a valid KeyEvent rather than
													   // a non-zero actionId and a null event like the previous cases.
													   if (event.getAction() == KeyEvent.ACTION_DOWN) ;
														   // We capture the event when key is first pressed.
													   else return true;   // We consume the event when the key is released.
												   } else return false;
												   readTagCommand(v.getText().toString());
												   v.setText("");
												   populateStaticViews();
												   showDibViews();

												   return true;   // Consume the event
											   }

/*
		editText.setOnEditorActionListener(new EditText.OnEditorActionListener()
				{
			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				boolean handled = false;
				if (actionId == EditorInfo.IME_ACTION_SEND) {
		//			sendMessage();

					readTagCommand(v.getText().toString());
					v.setText("");
					populateStaticViews();
					showDibViews();

					handled = true;
				}
				return handled;
			}
*/

										   }
		);




	}	
	
	@Override
	public void onRestoreInstanceState( Bundle savedInstanceState )
	{
		super.onRestoreInstanceState( savedInstanceState );
		isDibView = savedInstanceState.getBoolean( "isDibView", false );
	}
	
	@Override
	public void onSaveInstanceState( Bundle outState )
	{
		outState.putBoolean( "isDibView", isDibView );
		super.onSaveInstanceState( outState );
	}

	/**
	 * Enables foreground dispatch, acquires wakelock, populates static views.
	 */
	@Override
	protected void onResume()
	{
		super.onResume();
		if ( mNfcAdapter != null ) mNfcAdapter.enableForegroundDispatch
			( this, mNfcPendingIntent, mIntentFiltersArray, mNfcTechListsArray );
		
		//If wakelock preference is to keep awake then acquire a wakelock.
		if ( mDefaultSharedPrefs.getBoolean( "prefkey_wakelock", false ) )
		{
			if ( !mWakeLock.isHeld() ) mWakeLock.acquire();
		}
		else 
		{
			if ( mWakeLock.isHeld() ) mWakeLock.release();
		}
		
		//Populate course and competitor views.
		populateStaticViews();
		
		if ( isDibView ) showDibViews(); else displayResultSlip();
	}

	/**
	 * Disables foreground dispatch, releases wakelock.
	 */
	@Override
	protected void onPause()
	{
		super.onPause();		
		if ( mNfcAdapter != null ) mNfcAdapter.disableForegroundDispatch( this );
		if ( mWakeLock.isHeld() ) mWakeLock.release();
	}
	
	//-------------------Menu functions-------------------------
	// onCreateOptionsMenu( Menu menu )
	// onMenuItemSelected( int featureID, MenuItem item )
	// about()	
	
	@Override 
	public boolean onCreateOptionsMenu( Menu menu ) 
	{
		getMenuInflater().inflate( R.menu.options, menu );
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected( MenuItem item )
	{
		switch( item.getItemId() )
		{
			case R.id.menu_result:
				displayResultSlip();
				return true;
			case R.id.menu_dibscreen:
				showDibViews();
				return true;
			case R.id.menu_preferences:
				startActivity( new Intent( this, Preferences.class ) );
				return true;
			case R.id.menu_about:
				about();
				return true;

			case R.id.menu_send:
			send();
			return true;
		}
		return false;
	}
	
	private void about()
	{
		AlertDialog.Builder adbAbout = new AlertDialog.Builder(this);
		TextView tvWebsiteLink = new TextView(this);
		PackageManager pm = getPackageManager();
		PackageInfo pi = null;
		String sTitle = "";
		
		//Title & Version
		sTitle = getString( R.string.app_name ) + " " + getString( R.string.version );
		try { 
			pi = pm.getPackageInfo( this.getPackageName(), 0  );
			sTitle += pi.versionName; 
		}			
		catch ( NameNotFoundException e ) {}
		adbAbout.setTitle( sTitle );
		
		//Icon & Message
		adbAbout.setIcon( R.drawable.ic_dib );
		adbAbout.setMessage( getString( R.string.copyright ) );
		
		//Buttons
		adbAbout.setCancelable( true );
		adbAbout.setPositiveButton( getString( R.string.ok ), new DialogInterface.OnClickListener()
		{
			public void onClick( DialogInterface dialog, int whichButton ) { dialog.cancel(); }
		});
		
		//Website link.
		tvWebsiteLink.setMovementMethod( LinkMovementMethod.getInstance() );
		tvWebsiteLink.setText( R.string.website );
		tvWebsiteLink.setGravity( Gravity.CENTER | Gravity.CENTER );
		adbAbout.setView( tvWebsiteLink );
		
		adbAbout.show();
	}	
//send ----------------------------------

	private <RequestQueue> void send()
	{
		Competitor competitor = new Competitor( this );
		ControlCard controlCard = new ControlCard ( this );
		Dibber dibber = new Dibber ( this );
		String smsMessage = "";
		String rUrl ="http://www.northernwind.spb.ru/qrdib/load";

		markCourse( controlCard, dibber );
		smsMessage=makeLongResultMessage( competitor, controlCard);

		SharedPreferences defSharedPrefs = PreferenceManager.getDefaultSharedPreferences( this );

		rUrl = defSharedPrefs.getString( "prefkey_site", this.getString(com.appindesign.dib.R.string.prefdef_site) );

		new CallAPI(this).execute(rUrl,smsMessage);
	//	xxx.doInBackground("http://www.northernwind.spb.ru/qrdib/load/","qwerty");
	}

	//---------------------------Event Functions--------------------------------
	// onDib( View view )
	// onActivityResult( int requestCode, int resultCode, Intent intent )
	// onNewIntent( Intent intent )

	/**
	 * If QR mode is enabled, then start a scan when the "dib" button is pressed.
	 * @param view
	 */
	public void onDib( View view )
	{		
		if ( mDefaultSharedPrefs.getBoolean( "prefkey_qrmode", false ) ) //The dib button is only used in QR mode.		
		{			
			Intent intent = new Intent( "com.google.zxing.client.android.SCAN" );
			List<ResolveInfo> list = getPackageManager().queryIntentActivities
				( intent, PackageManager.MATCH_DEFAULT_ONLY );    
			if( list.size() > 0 ) // zxing is available
			{
				intent.setPackage( "com.google.zxing.client.android" );
				intent.putExtra( "MODE", "SCAN_MODE" );
				startActivityForResult( intent, mnQR_SCAN_ACTIVITY );
			}
			else // zxing is not available
			{
				makeToast( getString( R.string.noZxing ) );
			}
		}
		else
		{
			makeToast( getString( R.string.notQrMode ) );
		}
	}

	/**
	 * If the activity result is mnQR_SCAN_ACTIVITY, shows the dib views, reads the tag data and reads the tag command.
	 */
	// Act on the result of a QR scan.
	public void onActivityResult( int requestCode, int resultCode, Intent intent )
	{
		switch( requestCode )
		{
			case( mnQR_SCAN_ACTIVITY ):
				if ( resultCode == RESULT_OK )
				{	
					String sTagData = "";
					sTagData = ( intent.getStringExtra( "SCAN_RESULT" ) );
					showDibViews();
					readTagCommand( sTagData );		
				}
				else
				{
					Toast.makeText( this, getString( R.string.notScanned ), Toast.LENGTH_LONG ).show();
				}
				break;
		}
	}

	/**
	 * If the intent action was NDEF_DISCOVERED, shows the dib views, reads the tag data and reads the tag command.
	 */
	@Override
	protected void onNewIntent( Intent intent ) 
	{
		super.onNewIntent(intent);
		
		if ( intent.getAction().equalsIgnoreCase( "android.nfc.action.NDEF_DISCOVERED" ) )
		{
			String sTagData = "";
			showDibViews();
			sTagData = readNfcTag( intent );
			readTagCommand( sTagData );
		}
	}
	
	//-----------Control Reading Functions-----------------
	// processTagData( String sTagData )
	// storeDib( String sControlCode )
	// radioControl( String sPhoneNumber, String sControlCode )
	// getNfcTagData( Intent intent )	
	
	private void readTagCommand( String sTagData )
	// See what type of control you have and deal with it accordingly.
	{
		//Declarations and initialisations
		String[] sTagDataArray = sTagData.split( "," );
		String sCommand = getString( R.string.err );
		SharedPreferences.Editor editor = mDefaultSharedPrefs.edit();
		Boolean bIsCourseControl = true;
		//makeToast( "in readTagCommand" );
		//The command code is the first string of the control data.
		sCommand = sTagDataArray[0].trim();
		
		//Consider a legacy STR control, containing course data.
		if ( sCommand.equalsIgnoreCase( getString( R.string.str ) ) && sTagDataArray.length > 1 )
		{	
			//Clear the Local Store and then store the course data in the preferences.	
			this.getContentResolver().delete( LocalStore.CONTENT_URI, null, null );
			editor.putString( "prefkey_course_data", sTagData );
			editor.commit();
		}
		
		//Consider CRS.
		if ( sCommand.equalsIgnoreCase( getString( R.string.crs ) ) )
		{	
			bIsCourseControl = false;
			//Clear the Local Store and then store the course data in the preferences.	
			this.getContentResolver().delete( LocalStore.CONTENT_URI, null, null );
			editor.putString( "prefkey_course_data", sTagData.substring( sTagData.indexOf(",") + 1 ) );
			editor.commit();
//			makeToast( "edit ok");
		}		
		
		if ( sCommand.equalsIgnoreCase( getString( R.string.dwn ) ) )
		{
			bIsCourseControl = false;
			downloadResult( sTagDataArray );
		}
		
		if ( sCommand.equalsIgnoreCase( getString( R.string.rad ) ) )
		{
			//On a RADio tag element [1] contains the control code and [2] the telephone number. 
			bIsCourseControl = true;
			radioControl( sTagDataArray[1].trim(), sTagDataArray[2].trim() );
		}
			
		//The non-course controls are CRS, DWN and RAD.
		if ( bIsCourseControl ) 
		{
				String sCourse;
				List<String> sCourseFields;

				ControlCard controlCard = new ControlCard ( this );
				Dibber dibber = new Dibber( this );
				markCourse( controlCard, dibber );
			//	if(dibber.getIndex(sCommand.toUpperCase(),0)>=0)
			//		{
					if (controlCard.getName().toUpperCase().startsWith("РОГЕЙН")|| controlCard.getName().toUpperCase().endsWith("SECURE")) {
	//					if(!(sCommand.equalsIgnoreCase( "STR" )|| sCommand.equalsIgnoreCase("FIN" ))) {
						List<String> sDibFields;
						sDibFields = Arrays.asList( sCommand.toUpperCase().split( ":" ) );
						if(sDibFields.size()>1)
						{
							if(controlCard.getCode(sDibFields.get(1).toString()).equalsIgnoreCase(sDibFields.get(0).toString())) {
								if(controlCard.getFinishTimestamp()<0) {
									if(controlCard.getfirstTimestamp(sDibFields.get(0).toString())<0) {
										storeDib(sCommand.toUpperCase());
									}
								}
							}
						}
	//						}
						}
				else {
						storeDib(sCommand.toUpperCase());
					}
//			Toast.makeText( this, sCommand, Toast.LENGTH_LONG ).show();
		}
				
		//Deal with a finish control.
		if ( sCommand.equalsIgnoreCase( getString( R.string.fin ) ) ) 
			{ displayResultSlip(); }
	}

	/**
	 * Stores the control code and the timestamp in the Local Store.
	 * @param sCode The control code.
	 */
	private void storeDib( String sCode )
	{
		//Store the control code and system time in milliseconds.

		ContentValues contentValues = new ContentValues();
		if(sCode.length()<5) {
			contentValues.put("col_1", sCode);
		}
		else
		{
			ControlCard controlCard = new ControlCard ( this );
			String tCode=controlCard.getCode(sCode);
	//		contentValues.put("col_6", sCode);
			contentValues.put("col_1", tCode);
			if(tCode.length()==0)
			{
				contentValues.put("col_1", sCode);
			}

		}
		contentValues.put( "col_2", System.currentTimeMillis() );
		this.getContentResolver().insert( LocalStore.CONTENT_URI, contentValues );				
	}

	/**
	 * Texts the course name, control code and competitor name.
	 * @param sPhoneNumber The phone number the text is sent to.
	 * @param sControlCode The control code.
	 */
	void radioControl( String sControlCode, String sPhoneNumber )
	{
		Competitor competitor = new Competitor( this );
		ControlCard controlCard = new ControlCard( this );
		SmsManager sms = SmsManager.getDefault();
		sms.sendTextMessage( sPhoneNumber, null, 
				competitor.getFullName() + ", " +
				sControlCode + ", " +
				controlCard.getName(), null, null );		
	}
	
	private String readNfcTag( Intent intent )
	{
		//Declarations
		Parcelable[] ndefMessagesArray = intent.getParcelableArrayExtra( NfcAdapter.EXTRA_NDEF_MESSAGES );
		NdefMessage ndefMessage;
		byte[] ndefMessagePayload;
		String sTagData = getString( R.string.err );

		if ( ndefMessagesArray != null )
		{
			ndefMessage = (NdefMessage) ndefMessagesArray[0];
			ndefMessagePayload = ndefMessage.getRecords()[0].getPayload();
			sTagData = new String( ndefMessagePayload );
		}
		
		return( sTagData );
	}
	
	//-----------Functions to Communicate Results----------	
	// downloadResult( String[] sControlCode )
	// markCourse( Course course, Dibber dibber )
	// getShortMessage( Competitor competitor, Course course )
	// getLongMessage( Competitor competitor, Course course )
	// displayResultSlip()

	/**
	 * This method is called on reading a DWN tag, it reads the DWN parameters and either sends 
	 * a short result by text or a long result by QR.
	 * @param downloadInstruction If this is QR a long result is downloaded by QR code, otherwise it is 
	 * interpreted as a telephone number to text a short result to.
	 */
	private void downloadResult( String[] downloadInstruction )
	{
		Competitor competitor = new Competitor( this );
		ControlCard controlCard = new ControlCard ( this );
		Dibber dibber = new Dibber ( this );
		String smsMessage = "";
		String sPhoneNumber = "";		
		
		markCourse( controlCard, dibber );
		
		//First check there is a download parameter.
		if ( downloadInstruction.length > 1 ) 
		{
			//Find download mode.
			if ( downloadInstruction[1].equals( "QR" ) ) 
			{
				Intent intent = new Intent( "com.google.zxing.client.android.ENCODE" );
		        intent.putExtra( "ENCODE_TYPE", "TEXT_TYPE" );  
		        intent.putExtra( "ENCODE_DATA", makeLongResultMessage( competitor, controlCard ) );  
		        startActivity( intent ); 
			}
			else 
			{
				sPhoneNumber = downloadInstruction[1];	
				SmsManager sms = SmsManager.getDefault();
				//If this is a test send the full result by SMS.
				if ( downloadInstruction.length == 3 && downloadInstruction[2].equals("TST") ) {
					smsMessage = makeLongResultMessage( competitor, controlCard ); }
				else
					smsMessage = makeShortResultMessage( competitor, controlCard );
				sms.sendTextMessage( sPhoneNumber, null, smsMessage, null, null );
			}				
		}
		else
		{
			makeToast( getString( R.string.noDownloadParameter ) );
		}
	}

	/*
	*
	 * Goes through the course looking on the dibber for the course controls - if it finds a control it 
	 * marks the dibber timestamp onto the course, otherwise it marks the timestamp as Msg.INVALID_TIME.
	 * @param course
	 * @param dibber
	 */
	void markCourse( ControlCard controlCard, Dibber dibber )
	{
		String sCode = "";
		int nIndexOnDibber = -1;
		int nStartingAt = 0;
		
		for ( int i=0; i<controlCard.getNumberOfControls()+2; i++ )
		{
			sCode = controlCard.getCode(i);

			if (controlCard.getName().toUpperCase().startsWith("РОГЕЙН")) {
				nStartingAt = 0;
			}
			nIndexOnDibber = dibber.getIndex( sCode, nStartingAt );
			if ( nIndexOnDibber >= 0 )
			{
				controlCard.setTimestamp( i, dibber.getTimestamp( nIndexOnDibber ) );
				dibber.removePunch( nIndexOnDibber );
				nStartingAt = nIndexOnDibber;
			}
			else
			{
				controlCard.setTimestamp( i, Msg.INVALID_TIME );
			}
		}
	}
	
	String makeShortResultMessage( Competitor competitor, ControlCard controlCard )
	{
		String sMessage = "";
		String[] sMessageArray;
		sMessageArray = new String[Msg.LENGTH+1];
		final SimpleDateFormat sdf_UTC_HH_mm_ss = new SimpleDateFormat( "HH:mm:ss" );
		sdf_UTC_HH_mm_ss.setTimeZone( TimeZone.getTimeZone( "UTC" ) );

		sMessageArray[Msg.CODEWORD] = Msg.CODE_WORD;
		
		sMessageArray[Msg.FORENAME] = competitor.getForename();
		sMessageArray[Msg.SURNAME] = competitor.getSurname();
		sMessageArray[Msg.CLUB] = competitor.getClub();
		sMessageArray[Msg.CLASSIFICATION] = competitor.getClassification();
		sMessageArray[Msg.IDENTIFIER] = competitor.getIdentifier();	
		sMessageArray[Msg.NAME] = controlCard.getName();
		sMessageArray[Msg.STATUS] = controlCard.getResultStatus( this );
		sMessageArray[Msg.OLD_TIME] = sdf_UTC_HH_mm_ss.format( controlCard.getCourseTime() );
		sMessageArray[Msg.LENGTH] = controlCard.getLength();
		
		sMessage = TextUtils.join( Msg.DELIMITER, sMessageArray );
		return sMessage;
	}
	
	/**
	 * Creates a result message with a list of controls and split timestamps, compressing the timestamps
	 * by removing the start time from all bar the STR timestamp.
	 * @param competitor
	 * @param controlCard
	 * @return Result message.
	 */
	String makeLongResultMessage( Competitor competitor, ControlCard controlCard )
	{
		String sMessage = "";
		String[] sMessageArray = new String[Msg.TIMESTAMPS+1];
		String[] sCodesArray;// = new String[ controlCard.getNumberOfControls()+2 ];
		String[] sSplitTimestampsArray;// = new String[ controlCard.getNumberOfControls()+2 ];
		long lStartTimestamp = controlCard.getTimestamp(0)/Msg.TIME_DIVISOR;
		int i = 0;
		
		final SimpleDateFormat sdf_UTC_HH_mm_ss = new SimpleDateFormat( "HH:mm:ss" );
		sdf_UTC_HH_mm_ss.setTimeZone( TimeZone.getTimeZone( "UTC" ) );
if(!controlCard.getName().toUpperCase().startsWith("РОГЕЙН")) {
	sCodesArray = new String[ controlCard.getNumberOfControls()+2 ];
	sSplitTimestampsArray = new String[ controlCard.getNumberOfControls()+2 ];
	sMessageArray[Msg.CODEWORD] = Msg.CODE_WORD;
	sMessageArray[Msg.FORENAME] = competitor.getForename();
	sMessageArray[Msg.SURNAME] = competitor.getSurname();
	sMessageArray[Msg.CLUB] = competitor.getClub();
	sMessageArray[Msg.CLASSIFICATION] = competitor.getClassification();
	sMessageArray[Msg.IDENTIFIER] = competitor.getIdentifier();
	sMessageArray[Msg.NAME] = controlCard.getName();
	sMessageArray[Msg.STATUS] = controlCard.getResultStatus(this);
	sMessageArray[Msg.OLD_TIME] = sdf_UTC_HH_mm_ss.format(controlCard.getCourseTime());
	sMessageArray[Msg.LENGTH] = controlCard.getLength();

	//Additional data provided in long message...
	sMessageArray[Msg.TIME] = Long.toString(controlCard.getCourseTime() / Msg.TIME_DIVISOR);
	sMessageArray[Msg.CODES] = "";
	sMessageArray[Msg.TIMESTAMPS] = "";

	//Populate the codes and their timestamps.
	for (i = 0; i < controlCard.getNumberOfControls() + 2; i++) {
		sCodesArray[i] = controlCard.getCode(i);
		sSplitTimestampsArray[i] = Long.toString(controlCard.getTimestamp(i) / Msg.TIME_DIVISOR - lStartTimestamp);
	}
	sSplitTimestampsArray[0] = Long.toString(lStartTimestamp);
}
else
{	long lStartTime = Msg.INVALID_TIME;
	long lPreTime = Msg.INVALID_TIME;
	long lFinTime = Msg.INVALID_TIME;
	Dibber dibber = new Dibber ( this );
	sCodesArray = new String[ dibber.getNumberOfDibs()];
	sSplitTimestampsArray = new String[ dibber.getNumberOfDibs()];
	sMessageArray[Msg.CODEWORD] = Msg.CODE_WORD;
	sMessageArray[Msg.FORENAME] = competitor.getForename();
	sMessageArray[Msg.SURNAME] = competitor.getSurname();
	sMessageArray[Msg.CLUB] = competitor.getClub();
	sMessageArray[Msg.CLASSIFICATION] = competitor.getClassification();
	sMessageArray[Msg.IDENTIFIER] = competitor.getIdentifier();
	sMessageArray[Msg.NAME] = controlCard.getName();

	sMessageArray[Msg.STATUS] = getRogainStatus(dibber);
	sMessageArray[Msg.OLD_TIME] = sdf_UTC_HH_mm_ss.format(controlCard.getCourseTime());
	sMessageArray[Msg.LENGTH] = controlCard.getLength();

	//Additional data provided in long message...

	sMessageArray[Msg.CODES] = "";
	sMessageArray[Msg.TIMESTAMPS] = "";
	for (i = 0; i < dibber.getNumberOfDibs(); i++) {
		if (dibber.getCode(i).equalsIgnoreCase("STR")) {
			lStartTime = dibber.getTimestamp(i);
//			lPreTime = dibber.getTimestamp(i);
		} else {
			if (dibber.getCode(i).equalsIgnoreCase("FIN")) {
				lFinTime = dibber.getTimestamp(i);
			}
		}
		sCodesArray[i] = dibber.getCode(i);
		sSplitTimestampsArray[i] = Long.toString((dibber.getTimestamp(i) - lStartTime)/ Msg.TIME_DIVISOR );

	}
	sMessageArray[Msg.TIME] = Long.toString((lFinTime-lStartTime) / Msg.TIME_DIVISOR);
	sSplitTimestampsArray[0] = Long.toString(lStartTime/1000);

	}
		sMessageArray[Msg.CODES] = TextUtils.join( Msg.SPLIT_DELIMITER, sCodesArray );
		sMessageArray[Msg.TIMESTAMPS] = TextUtils.join( Msg.SPLIT_DELIMITER, sSplitTimestampsArray );
		
		sMessage = TextUtils.join( Msg.DELIMITER, sMessageArray );
		return sMessage;		
	}

	/**
	 * This creates a ResultSlip object and uses it to get result slip data which it then displays.
	 */
	private String getRogainStatus(Dibber dibber) {
		String rs = "н/c";
		for (int i = 0; i < dibber.getNumberOfDibs(); i++) {
			if (dibber.getCode(i).equalsIgnoreCase("STR")) {
				rs = "н/ф";
			}
			if (dibber.getCode(i).equalsIgnoreCase("FIN")) {
				if (rs.equalsIgnoreCase("н/ф")) {
					rs = "OK";
				} else
					rs = "н/с";
			}
		}
		return (rs);
	}

	private void displayResultSlip()
	{
		TextView tvCourseStatus = (TextView) findViewById( R.id.tvCourseStatus );		
		ListView resultSlipView = (ListView) findViewById( R.id.resultListView );

		//Adapter variables.
		final String[] resultSlipColumnTags = getResources().getStringArray( R.array.resultSlipColumnTags );
		final int[] nRESULT_SLIP_COLUMN_IDS = { R.id.tvLeg, R.id.tvCode, R.id.tvTimestamp, R.id.tvSplitTime, R.id.tvElapsedTime	};		
		SimpleAdapter resultSlipAdapter;		
		
		ControlCard controlCard = new ControlCard( this );
		Dibber dibber = new Dibber( this );
		if (!controlCard.getName().toUpperCase().startsWith("РОГЕЙН")) {
			markCourse(controlCard, dibber);
			tvCourseStatus.setText( controlCard.getResultStatus( this ) );
		}
		else
		{
			tvCourseStatus.setText(getRogainStatus(dibber));
		}
		//Display course status.		

		
		//Display result lines.
		ResultSlip resultSlip = new ResultSlip( controlCard, dibber, this );		
		resultSlipAdapter = new SimpleAdapter(this, resultSlip.getResultSlipData(), R.layout.result_leg, resultSlipColumnTags , nRESULT_SLIP_COLUMN_IDS);
		resultSlipView.setAdapter(resultSlipAdapter);
		
		//Show the result views.
		showResultViews();
	}

	//--------------Utility functions---------------------
	// makeToast( String sMsg )
	// logDebug( String sMsg )
	// showDibView()
	// showResultView()
	// populateViews()
	
	private void makeToast( String sMessage )
	{
		Toast.makeText( this, sMessage, Toast.LENGTH_LONG ).show();		
	}	
	
	private void logDebug( String sMessage )
	{
		boolean mbDebug = true;
		if ( mbDebug ) Log.d( "dib", sMessage );
	}

	private void showDibViews()
	{
		ImageButton mbtnDib;
		ListView resultListView;
		TextView tvCourseStatus;
		
		mbtnDib = (ImageButton) findViewById( R.id.btnDib );
		resultListView = (ListView) findViewById( R.id.resultListView );
		tvCourseStatus = (TextView) findViewById( R.id.tvCourseStatus );
		
		mbtnDib.setVisibility( View.VISIBLE );
		resultListView.setVisibility( View.GONE );
		tvCourseStatus.setVisibility( View.GONE );
		
		isDibView = true;
	}
	
	private void showResultViews()
	{
		ImageButton mbtnDib;
		ListView resultListView;
		TextView tvCourseStatus;
		
		mbtnDib = (ImageButton) findViewById( R.id.btnDib );
		resultListView = (ListView) findViewById( R.id.resultListView );
		tvCourseStatus = (TextView) findViewById( R.id.tvCourseStatus );
		
		mbtnDib.setVisibility( View.GONE );
		resultListView.setVisibility( View.VISIBLE );
		tvCourseStatus.setVisibility( View.VISIBLE );
		
		isDibView = false;
	}

	/**
	 * Populates the competitor and course views at the top of the dib screen.
	 */
	private void populateStaticViews()
	{
		//Set text for competitor views.
		Competitor competitor = new Competitor( this );
		
		TextView tvCompetitor = (TextView) findViewById( R.id.tvCompetitor );		
		TextView tvClub = (TextView) findViewById( R.id.tvClub );		
		TextView tvIdentifier = (TextView) findViewById( R.id.tvIdentifier );		
		TextView tvClassification = (TextView) findViewById( R.id.tvClassification );
		
		tvCompetitor.setText( competitor.getFullName() );
		tvClub.setText( competitor.getClub() );
		tvIdentifier.setText( competitor.getIdentifier() );
		tvClassification.setText( competitor.getClassification() );
		
		//Set text for course views.
		ControlCard controlCard = new ControlCard( this );
		
		TextView tvCourseName = (TextView) findViewById( R.id.tvCourseName );
		TextView tvCourseLength = (TextView) findViewById( R.id.tvCourseLength );
		TextView tvNumberOfControls = (TextView) findViewById( R.id.tvNumberOfControls );
		
		tvCourseName.setText( controlCard.getName() );
		if(controlCard.getName().toUpperCase().startsWith("РОГЕЙН"))
		{
			Dibber dibber = new Dibber( this );
			markCourse( controlCard, dibber );
			tvCourseLength.setText("очки:"+controlCard.getScore().toString());
			tvNumberOfControls.setText( "(" +controlCard.getTakenNumberOfControls()+"/"+ controlCard.getNumberOfControls() + " " + getString( R.string.controls ) + ")" );
		}
		else {
			tvCourseLength.setText(controlCard.getLength() + getString(R.string.metres));
			tvNumberOfControls.setText( "(" + controlCard.getNumberOfControls() + " " + getString( R.string.controls ) + ")" );
		}


	}
	
	//-------------Standing Item Creation-------------------
	// prepareNfc()
	// createWakeLock()

	/**
	 * Prepares the parameters for the NFC adapter.
	 */
	private void prepareNfc()
	{			
		//A. Get the NFC adapter.
		//Now prepare the parameters for onResume's call to foreground dispatch.
		//	B. Create a pending intent.
		//	C. Create an array of intent filters.
		//	D. Create an array of tech filters.
		//	E. In onResume use these three data to inform the foreground dispatch system.
		
		//Step A. Get the adapter for the NFC hardware.
		mNfcAdapter = NfcAdapter.getDefaultAdapter( this );
		if ( mNfcAdapter == null ) makeToast( getString( R.string.noNfc ) );
		
		//Step B. Create a pending intent.
		//Invoking a pending intent is analogous to calling Context.startActivity(Intent).
		//Parameter 1: The context in which the pending intent should start the activity.
		//Parameter 2: (Currently not used) Request code for the sender.
		//Parameter 3: The intent of the activity to be launched.
		//Parameter 4: Flags
		mNfcPendingIntent = PendingIntent.getActivity
		(
			this, 0, new Intent( this, getClass() ).addFlags( Intent.FLAG_ACTIVITY_NEW_TASK ), 0 
		);
		
		//Step C. Create an array of intent filters.
		//Remember an intent filter has an ACTION and DATA so you need to specify both of these.
		//1. Construct an IntentFilter object.
		//2. Add the desired ACTION.
		//3. Add the desired DATA (the mimeType(s) you want to detect).
		//4. Repeat for any more filters you want to add.
		//5. Add the filters to a filter array.
		IntentFilter ndef = new IntentFilter();
		ndef.addAction( NfcAdapter.ACTION_NDEF_DISCOVERED );
		try { ndef.addDataType( "application/vnd.com.appindesign.dib" ); }
		catch ( MalformedMimeTypeException e ) { throw new RuntimeException( "fail", e ); }	
		mIntentFiltersArray = new IntentFilter[] { ndef };

		//Step D. Create an array of technology filters - in this case just one - NFC-F.
		mNfcTechListsArray = new String[][] { { NfcF.class.getName() } };
		//You could add other techs e.g. Other.class.getName() and add them to your techlists array.
	}
	
	private void createWakeLock()
	{		
		//Create a wakelock instance.
		final String sWAKELOCK_TAG = "com.appindesign.dib.wakelock";
		PowerManager powerManager = (PowerManager) getSystemService( POWER_SERVICE );
		mWakeLock = powerManager.newWakeLock( PowerManager.SCREEN_DIM_WAKE_LOCK, sWAKELOCK_TAG );
		mWakeLock.setReferenceCounted(false);
	}
}


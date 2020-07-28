package com.appindesign.diblibrary;

/**
 * Defines the fields in the message passing from dib to dibHq which 
 * coincide with the column numbers in dibHq's local store and the index to 
 * dib's msSort array - plus the Msg delimiter and time divisor.
 */

public class Msg
{	
	//The dib to dibHq CODEWORD.
	public final static int CODEWORD = 0;
	
	//Competitor fields.
	public final static int FORENAME = 1;
	public final static int SURNAME = 2;
	public final static int CLUB = 3;
	public final static int CLASSIFICATION = 4;
	public final static int IDENTIFIER = 5;
	
	//ControlCard header information + (indented) convenience info for short message.
	public final static int NAME = 6;
		public final static int STATUS = 7;
		public final static int OLD_TIME = 8;
	public final static int LENGTH = 9;
	
	//ControlCard codes and splits
	public final static int TIME = 10;
	public final static int CODES = 11;
	public final static int TIMESTAMPS = 12;

	//Delimiter and time divisor
	public final static String DELIMITER = ",";
	public final static String SPLIT_DELIMITER = ";";
	public final static int TIME_DIVISOR = 1000;
	
	//Codeword
	public final static String CODE_WORD = "dibSMS";
	
	//Invalid time
	public final static long INVALID_TIME = -1000l;
}

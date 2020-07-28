package com.appindesign.diblibrary;

public class Punch 
{
	private String code;
	private String intid;
	private long timestamp;


	public Punch( String code, long timestamp, String intid ) {
		this.code = code;
		this.timestamp = timestamp;
		this.intid=intid;
	}

	public String getCode() {
		return code;
	}

	public void setCode( String code ) {
		this.code = code;
	}
	public String getIntid() {
		return intid;
	}

	public void setIntid( String code ) {
		this.intid = intid;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp( long time ) {
		this.timestamp = time;
	}
}
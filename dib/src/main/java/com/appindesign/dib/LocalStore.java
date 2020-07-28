package com.appindesign.dib;

	import android.content.ContentProvider;
	import android.content.ContentUris;
	import android.content.ContentValues;
	import android.content.Context;
	import android.content.UriMatcher;
	import android.database.Cursor;
	import android.database.SQLException;
	import android.database.sqlite.SQLiteDatabase;
	import android.database.sqlite.SQLiteOpenHelper;
	import android.database.sqlite.SQLiteQueryBuilder;
	import android.net.Uri;
	import android.text.TextUtils;
	import android.util.Log;

	public class LocalStore extends ContentProvider 
	{
	    public static final String PROVIDER_NAME = "com.appindesign.dib.LocalStore";

	    private static final int ALL_RECORDS = 1;
	    private static final int ONE_RECORD = 2;    	        
	    
	    public static final Uri CONTENT_URI = Uri.parse("content://"+ PROVIDER_NAME + "/records");
	    private static final UriMatcher uriMatcher;
	    static
	    {
	        uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
	        uriMatcher.addURI(PROVIDER_NAME, "records", ALL_RECORDS);
	        uriMatcher.addURI(PROVIDER_NAME, "records/#", ONE_RECORD);        
	    }

	    //---Database definition
	    private SQLiteDatabase oLocalStoreDB;
	    private static final String DATABASE_NAME = "localStoreDB";
	    private static final String DATABASE_TABLE = "localStoreTable";
	    private static final int DATABASE_VERSION = 1;
	    public static final String _ID = "_id";
	    private static final String DATABASE_CREATE =
	    	"create table " + DATABASE_TABLE + 
	            " ( " 
	                + _ID + "  integer primary key autoincrement, "
	                + "col_1 text, "
	                + "col_2 text, "
	                + "col_3 text, "
	                + "col_4 text, "
	                + "col_5 text" /*", "
					+ "col_6 text"*/
	                + 
	            " );";

	    //---Database class providing database creation/upgrade methods
	    private static class DatabaseHelper extends SQLiteOpenHelper 
	    {
	        DatabaseHelper(Context context) 
	        {
	            super(context, DATABASE_NAME, null, DATABASE_VERSION);
	        }

	        @Override
	        public void onCreate(SQLiteDatabase db) 
	        {
	            db.execSQL(DATABASE_CREATE);
	        }

	        @Override
	        public void onUpgrade(SQLiteDatabase db, int oldVersion, 
	        int newVersion) 
	        {
	            Log.w("Content provider database", 
	                  "Upgrading database from version " + 
	                  oldVersion + " to " + newVersion + 
	                  ", which will destroy all old data");
	            db.execSQL( "DROP TABLE IF EXISTS " + DATABASE_TABLE );
	            onCreate(db);
	        }
	    } 

	    //---getType method of ContentProvider
	    @Override
	    public String getType(Uri uri) {
	        switch (uriMatcher.match(uri))
	        {
	            case ALL_RECORDS:
	                return "vnd.android.cursor.dir/vnd.com.appindesign.records ";
	            case ONE_RECORD:                
	                return "vnd.android.cursor.item/vnd.com.appdesign.records ";
	            default:
	                throw new IllegalArgumentException("Unsupported URI: " + uri);        
	        } 
	    }

	    //---onCreate method of ContentProvider
	    @Override
	    public boolean onCreate() 
	    {
	        Context context = getContext();
	        DatabaseHelper dbHelper = new DatabaseHelper(context);
	        oLocalStoreDB = dbHelper.getWritableDatabase();
	        return (oLocalStoreDB == null)? false:true;
	    }	    

	    //---Insert method of ContentProvider
	    @Override
	    public Uri insert(Uri uri, ContentValues values) 
	    {
	        long rowID = oLocalStoreDB.insert( DATABASE_TABLE, "", values );        
	        
	        if (rowID>0) //---if added successfully---
	        {
	            Uri _uri = ContentUris.withAppendedId(CONTENT_URI, rowID);
	            getContext().getContentResolver().notifyChange(_uri, null);    
	            return _uri;                
	        }        
	        throw new SQLException("Failed to insert row into " + uri);
	    }

	    //---Delete method of ContentProvider
	    @Override
	    public int delete(Uri uri, String selection, String[] selectionArgs) 
	    {
	        int count=0;

	        switch (uriMatcher.match(uri))
	        {
	            case ALL_RECORDS: //Delete whole table
	                count = oLocalStoreDB.delete
	                        (
	                            DATABASE_TABLE,
	                            selection, 
	                            selectionArgs
	                         );
	                break;
	            case ONE_RECORD:
	                String id = uri.getPathSegments().get(1);
	                count = oLocalStoreDB.delete
	                        (
	                             DATABASE_TABLE,                        
	                             _ID + " = " + id + (!TextUtils.isEmpty(selection) ? " AND (" + selection + ')' : ""), 
	                             selectionArgs
	                         );
	                break;
	            default: throw new IllegalArgumentException("Unknown URI " + uri);    
	        }
	        
	        getContext().getContentResolver().notifyChange(uri, null);
	        return count;
	    }

	    //---Update method of ContentProvider
	    @Override
	    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) 
	    {
	        int count = 0;
	        switch (uriMatcher.match(uri))
	        {
	            case ALL_RECORDS:
	                count = oLocalStoreDB.update(
	                        DATABASE_TABLE, 
	                        values,
	                        selection, 
	                        selectionArgs);
	                break;
	            case ONE_RECORD:                
	                count = oLocalStoreDB.update(
	                        DATABASE_TABLE, 
	                        values,
	                        _ID + " = " + uri.getPathSegments().get(1) + 
	                        (!TextUtils.isEmpty(selection) ? " AND (" + 
	                            selection + ')' : ""), 
	                        selectionArgs);
	                break;
	            default: throw new IllegalArgumentException("Unknown URI " + uri);    
	        }       
	        getContext().getContentResolver().notifyChange(uri, null);
	        return count;
	    }
	    
	    //---Query method of ContentProvider
	    @Override
	    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) 
	    {
	        SQLiteQueryBuilder sqlBuilder = new SQLiteQueryBuilder();
	        sqlBuilder.setTables(DATABASE_TABLE);
	    
	        if (uriMatcher.match(uri) == ONE_RECORD)
	            //---if getting a particular book---
	            sqlBuilder.appendWhere(_ID + " = " + uri.getPathSegments().get(1));  
	        
	        if ( sortOrder == null || sortOrder == "" ) sortOrder = _ID;
	    
	        Cursor c = sqlBuilder.query(
	                oLocalStoreDB, 
	                projection, 
	                selection, // null gives all rows
	                selectionArgs, // replace "?" in "selection" with selectionArgs values
	                null, // groupBy - null means not grouped
	                null, // having - null means all groups
	                sortOrder);

	        //---register to watch a content URI for changes---
	        c.setNotificationUri(getContext().getContentResolver(), uri);
	        return c;
	    }
}

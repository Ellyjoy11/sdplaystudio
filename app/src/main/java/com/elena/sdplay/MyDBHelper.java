package com.elena.sdplay;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class MyDBHelper extends SQLiteOpenHelper {
	// If you change the database schema, you must increment the database
	// version.
	public static final int DATABASE_VERSION = 1;
	public static final String DATABASE_NAME = "MyDBHelper.db";
	public final String MY_TABLE = "My_table";
	public final String REC_ID = "_id";
	public final String REC_TYPE = "_type";
	public final String REC_NAME = "_name";
	public final String REC_COLOR = "_color";
	// creation SQLite statement
	private final String DATABASE_CREATE = "create table " + MY_TABLE + "("
			+ REC_ID + " integer primary key autoincrement, " + REC_TYPE
			+ " integer, " + REC_COLOR + " integer, " + REC_NAME
			+ " text not null);";

	public MyDBHelper(Context context, String sdpath) {
		super(context, sdpath + DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(DATABASE_CREATE);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// This database is only a cache for online data, so its upgrade policy
		// is
		// to simply to discard the data and start over
		db.execSQL("DROP TABLE IF EXISTS " + MY_TABLE);
		onCreate(db);
	}

	public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		onUpgrade(db, oldVersion, newVersion);
	}

}
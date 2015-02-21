package com.elena.sdplay;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class MyResDBHelper extends SQLiteOpenHelper {
	// If you change the database schema, you must increment the database
	// version.
	public static final int RES_DATABASE_VERSION = 1;
	public static final String RES_DATABASE_NAME = "MyResDBHelper.db";
	public final String RES_TABLE = "Res_table";
	public final String RES_ID = "_id";
	public final String RES_BUILD_ID = "_build_id";
	public final String RES_FS_TYPE = "_fs_type";
	public final String RES_OEMID = "_oemid";
	public final String RES_DETAILS = "_details";
	public final String RES_MANFID = "_manfid";
	public final String RES_SERIAL = "_serial";
	public final String RES_OPER_MODE = "_oper_mode";
	public final String RES_NAME = "_name";
	public final String RES_NICKNAME = "_nickname";
	public final String RES_W_SPEED = "_w_speed";
	public final String RES_R_SPEED = "_r_speed";
	public final String RES_RW_SPEED = "_rw_speed";
	public final String RES_RR_SPEED = "_rr_speed";
	public final String RES_TOTAL_SCORE = "_db_total_score";

	public final String FS_C_SPEED = "_c_speed";
	public final String FS_L_SPEED = "_l_speed";
	public final String FS_RS_SPEED = "_rs_speed";
	public final String FS_WM_SPEED = "_wm_speed";
	public final String FS_RM_SPEED = "_rm_speed";
	public final String FS_WL_SPEED = "_wl_speed";
	public final String FS_RL_SPEED = "_rl_speed";
	public final String FS_IOPS = "_iops_speed";
	public final String FS_D_SPEED = "_d_speed";
	public final String FS_SM_SCORE = "_sm_score";
	public final String FS_M_SCORE = "_m_score";
	public final String FS_L_SCORE = "_l_score";
	public final String FS_TOTAL_SCORE = "_fs_total_score";

	public final String SUMMARY_SCORE = "_summary_score";

	// creation SQLite statement
	private final String DATABASE_CREATE = "create table " + RES_TABLE + "("
			+ RES_ID + " integer primary key autoincrement, " + RES_OEMID
			+ " text, " + RES_MANFID + " text, " + RES_DETAILS + " text, "
			+ RES_BUILD_ID + " text, " + RES_FS_TYPE + " text, " + RES_NICKNAME
			+ " text, " + RES_W_SPEED + " text, " + RES_R_SPEED + " text, "
			+ RES_RW_SPEED + " text, " + RES_SERIAL + " text, " + RES_OPER_MODE
			+ " text, " + RES_TOTAL_SCORE + " integer, " + RES_NAME + " text, "
			+ RES_RR_SPEED + " text, " + FS_C_SPEED + " text, " + FS_L_SPEED
			+ " text, " + FS_RS_SPEED + " text, " + FS_WM_SPEED + " text, "
			+ FS_WL_SPEED + " text, " + FS_RL_SPEED + " text, " + FS_IOPS
			+ " text, " + FS_D_SPEED + " text, " + FS_TOTAL_SCORE
			+ " integer, " + SUMMARY_SCORE + " integer, " + FS_SM_SCORE
			+ " text, " + FS_M_SCORE + " text, " + FS_L_SCORE + " text, "
			+ FS_RM_SPEED + " text);";

	public MyResDBHelper(Context context, String intPath) {
		super(context, intPath + RES_DATABASE_NAME, null, RES_DATABASE_VERSION);
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
		db.execSQL("DROP TABLE IF EXISTS " + RES_TABLE);
		onCreate(db);
	}

	public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		onUpgrade(db, oldVersion, newVersion);
	}

}
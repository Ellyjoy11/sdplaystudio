package com.elena.sdplay;

import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.MergeCursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

import java.util.ArrayList;

public class SummaryView extends Activity implements ActionBar.TabListener {
	private String intPath;
	private boolean isWritten;
	private boolean isFsDone;
	private Cursor c, c1, res_c;
	private Cursor[] res = new Cursor[2];
	private int TOP_COUNT;// = 5;
	private int CHANGE_COLOR;
	private int REF_COUNT;
	private ArrayList<Integer> REF_COLORS;
	private String model_name, device_name, manufacturer, product_name;
	private boolean showCurrent;
	public boolean showRefs;
	public boolean showAll;
	private Menu menu;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		manufacturer = Build.MANUFACTURER;
		model_name = Build.MODEL;
		// product_name = Build.BRAND;
		device_name = Build.DEVICE;
		// setTitle("Test Summary for " + manufacturer + " " + device_name
		// + " [" + model_name + "]");
		setContentView(R.layout.activity_summary_view);
		Intent intent = getIntent();
		intPath = intent.getStringExtra(MainActivity.INT_PATH);

		// ///tabs/////
		// setup action bar for tabs
		ActionBar actionBar = getActionBar();
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
		actionBar.setDisplayShowTitleEnabled(true);
		// actionBar.setTitle("Test Summary for " + manufacturer + " "
		// + device_name + " [" + model_name + "]");
		actionBar.setTitle(manufacturer + " " + device_name + " [" + model_name
				+ "] ");
		// actionBar.setDisplayShowHomeEnabled(true);

		Tab tab0 = actionBar.newTab().setText("ALL").setTabListener(this);
		actionBar.addTab(tab0, 0, true);

		Tab tab1 = actionBar.newTab().setText("DB").setTabListener(this);

		actionBar.addTab(tab1, 1, false);

		Tab tab2 = actionBar.newTab().setText("FS").setTabListener(this);

		actionBar.addTab(tab2, 2, false);
		// actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
		actionBar.show();

		// actionBar.selectTab(actionBar.getTabAt(0));
		// /////end of tabs//////////

	}

	// extend the SimpleCursorAdapter to create a custom class where we
	// can override the getView to change the row colors
	private class MyCursorAdapter extends SimpleCursorAdapter {

		@SuppressWarnings("deprecation")
		public MyCursorAdapter(Context context, int layout, Cursor c,
				String[] from, int[] to) {
			super(context, layout, c, from, to);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {

			// get reference to the row
			View view = super.getView(position, convertView, parent);

			if (REF_COLORS.contains(position)) {
				view.setBackgroundColor(Color.rgb(255, 255, 255));
			} else if (position == CHANGE_COLOR) {
				view.setBackgroundColor(Color.rgb(32, 178, 170));

			} else {
				view.setBackgroundColor(Color.rgb(176, 226, 255));
			}
			return view;
		}

	}

	@Override
	public void onResume() {
		super.onResume();
		// get user options from shared preferences
		SharedPreferences userPref = PreferenceManager
				.getDefaultSharedPreferences(this);
		TOP_COUNT = Integer.parseInt(userPref.getString("top_count", "10"));
        showRefs = userPref.getBoolean("show_refs", true);
        showAll = userPref.getBoolean("show_all", false);
		// isWritten = intent.getBooleanExtra(BenchStart.CURR_RES_EXIST, false);
		isWritten = BenchStart.isWritten;
		isFsDone = BenchStart.isFsDone;
		// ViewFlipper viewTopRes = (ViewFlipper)
		// findViewById(R.id.viewFlipperTop);

		getTopRes();
		showResTable();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		this.menu = menu;
		// menu.getItem(R.id.action_show_top).setIcon(R.drawable.ic_menu_gallery);
		return true;
	}

	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.action_show_top) {
			// ViewFlipper viewTopRes = (ViewFlipper)
			// findViewById(R.id.viewFlipperTop);
			// MenuItem switchButton = menu.findItem(R.id.action_show_top);
			// getTopRes();
			// viewTopRes.showNext();
			getTopRes();
			showResTable();
			return true;
		}

		if (item.getItemId() == R.id.action_settings) {
			Intent intent = new Intent(this, SetPreferences.class);
			startActivity(intent);
			return true;
		}
		if (item.getItemId() == R.id.about) {
			AlertDialog.Builder builder = new AlertDialog.Builder(this);

            builder.setMessage(MainActivity.ABOUT_TITLE).setTitle(
                    MainActivity.ABOUT_VERSION);

			builder.setPositiveButton("OK",
					new DialogInterface.OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							dialog.dismiss();
						}
					});

			AlertDialog dialog = builder.create();
			dialog.show();
			return true;
		} else {
			return super.onOptionsItemSelected(item);
		}

	}

	public void getTopRes() {
		SharedPreferences userPref = PreferenceManager
				.getDefaultSharedPreferences(this);
		TOP_COUNT = Integer.parseInt(userPref.getString("top_count", "10"));
		if (showAll) {
			CHANGE_COLOR = 10000;
		} else {
			CHANGE_COLOR = TOP_COUNT;
		}
		REF_COLORS = new ArrayList<Integer>();
		String[] ref_res = getResources().getStringArray(
				R.array.reference_results);
		REF_COUNT = ref_res.length;
		// Read results database/////
		MyResDBHelper myResDB = new MyResDBHelper(getBaseContext(), intPath);

		SQLiteDatabase res_db = myResDB.getReadableDatabase();

		// Define a projection that specifies which columns from the
		// database
		// you will actually use after this query.
		String whereClause;
		String[] whereArgs;
		String sortOrder;
		String rowsCount;

		if (!showRefs) {
			whereClause = myResDB.RES_ID + " > ? AND " + myResDB.FS_TOTAL_SCORE
					+ " > ? AND " + myResDB.RES_W_SPEED + " > ?";
			whereArgs = new String[] { String.valueOf(REF_COUNT), "0.00",
					"0.00" };
			// Log.d("SDPlay", "where defined");
		} else {
			whereClause = myResDB.FS_TOTAL_SCORE + " > ? AND "
					+ myResDB.RES_W_SPEED + " > ?";
			whereArgs = new String[] { "0.00", "0.00" };
			// whereClause = null;
			// whereArgs = null;
			// Log.d("SDPlay", "get all");
		}

		String[] projection = { myResDB.RES_ID, myResDB.RES_DETAILS,
				myResDB.RES_NOTES, myResDB.RES_BUILD_ID,
				myResDB.RES_W_SPEED, myResDB.FS_L_SCORE, myResDB.FS_IOPS_SCORE,
				"ROUND(" + myResDB.SUMMARY_SCORE + ",2)", myResDB.RES_FS_TYPE,
                myResDB.RES_DEV_SIZE };

		sortOrder = myResDB.SUMMARY_SCORE + " DESC";
		if (!showAll) {
			rowsCount = "0, " + TOP_COUNT;
		} else {
			rowsCount = null;
		}

		res_db.beginTransaction();
		c = res_db.query(myResDB.RES_TABLE, // The table to query
				projection, // The columns to return
				whereClause, // The columns for the WHERE clause (null
				// means getting ALL records here)
				whereArgs, // The values for the WHERE clause
				null, // don't group the rows
				null, // don't filter by row groups
				sortOrder, // sort by Total score
				rowsCount // limit results number to 5
				);
		if (c.getCount() > 0) {
			c.moveToFirst();

		}
		// Log.d("SDPlay","1st item is " + c.getString(1) + "cursor count " +
		// c.getCount());

		String sortOrder1 = myResDB.RES_ID + " DESC";
		c1 = res_db.query(myResDB.RES_TABLE, // The table to query
				projection, // The columns to return
				whereClause, // The columns for the WHERE clause (null
				// means getting ALL records here)
				whereArgs, // The values for the WHERE clause
				null, // don't group the rows
				null, // don't filter by row groups
				sortOrder1, // sort by Total score
				"0, 1" // limit results number to 1
		);
		if (c1.getCount() > 0) {
			// Log.d("SDLog", "c1 is not empty");
			c1.moveToFirst();
		}
		if (isWritten && isFsDone) {
			showCurrent = true;
			c.moveToFirst();

			c1.moveToFirst();
			// Log.d("SDPlay", "isWritten and isFsDone true");
			while (!c.isAfterLast()) {
				// Log.d("SDPlay LOG",
				// "check next cursor if it's same as current "
				// + c.getPosition());
				if (c1.getInt(0) == c.getInt(0)) {
					CHANGE_COLOR = c.getPosition();
					showCurrent = false;
					// Log.d("SDPlay LOG", "show current false " +
					// CHANGE_COLOR);
					res_c = c;
					// c.moveToFirst();
					break;
				}
				c.moveToNext();
			}

		} else {
			showCurrent = false;
		}

		// /////////////////////////////////////
		c.moveToFirst();
		while (!c.isAfterLast()) {
			// Log.d("SDPlay", "check refs " + REF_COUNT);
			if (c.getInt(0) <= REF_COUNT) {
				REF_COLORS.add(c.getPosition());
				// showCurrent = false;
				// Log.d("SDPlay", "position" + c.getPosition());
				// res_c = c;
				// c.moveToFirst();
				// break;
			}
			// Log.d("SDPlay", "is written 3");
			c.moveToNext();
		}
		// /////////////////////////////////////

		if (showCurrent) {
			// Log.d("SDPlay", "show current true");
			c.moveToFirst();
			c1.moveToFirst();
			res[0] = c;
			res[1] = c1;
			res_c = new MergeCursor(res);
			// Log.d("SDPlay", "merge done");
			// if (c.getCount() < TOP_COUNT) {
			CHANGE_COLOR = TOP_COUNT;
			// }
		} else {
			// c.moveToFirst();
			res_c = c;
		}
		res_db.setTransactionSuccessful();
		res_db.endTransaction();
		res_db.close();
		// cursors are closed in onDestroy()

	}

	public void showResTable() {

		ListView summaryRes = (ListView) findViewById(R.id.summaryRes);

		// String[] from = res_c.getColumnNames();
		String[] from = { res_c.getColumnName(1), res_c.getColumnName(2),
				res_c.getColumnName(3), res_c.getColumnName(4),
				res_c.getColumnName(5), res_c.getColumnName(6),
				res_c.getColumnName(7), res_c.getColumnName(8),
                res_c.getColumnName(9)};
		// int[] to = { R.id.text0, R.id.text1, R.id.text2, R.id.text3,
		// R.id.text4, R.id.text5, R.id.text6, R.id.text7, R.id.text8 };
		int[] to = { R.id.text1sum, R.id.text2sum, R.id.text3sum,
				R.id.text4sum, R.id.text5sum, R.id.text6sum, R.id.text7sum,
				R.id.text_fssum, R.id.text_sizesum };

		MyCursorAdapter adapter = new MyCursorAdapter(this,
				R.layout.results_columns_summary, res_c, from, to);
		// Log.d("SDPlay", "before adapter");
		summaryRes.setAdapter(adapter);
		// Log.d("SDPlay", "after adapter");
	}

	public void onDestroy() {
		c.close();
		c1.close();
		res_c.close();
		super.onDestroy();
	}

	public void onChangeMedia(View view) {
		Intent intent = new Intent(getApplicationContext(), MainActivity.class);
		startActivity(intent);
	}

	public void onBackClick(View view) {
		super.onBackPressed();
	}

	public void onHideClick(View view) {
		SharedPreferences userPref = PreferenceManager
				.getDefaultSharedPreferences(this);
		TOP_COUNT = Integer.parseInt(userPref.getString("top_count", "10"));
		showRefs = !showRefs;
        SharedPreferences.Editor editor = userPref.edit();
        editor.putBoolean("show_refs", showRefs);
        editor.commit();
		// ViewFlipper viewTopRes = (ViewFlipper)
		// findViewById(R.id.viewFlipperTop);

		getTopRes();
		showResTable();
	}

	public void onShowAllClick(View view) {
		SharedPreferences userPref = PreferenceManager
				.getDefaultSharedPreferences(this);
		TOP_COUNT = Integer.parseInt(userPref.getString("top_count", "10"));
		showAll = !showAll;
        SharedPreferences.Editor editor = userPref.edit();
        editor.putBoolean("show_all", showAll);
        editor.commit();
		// ViewFlipper viewTopRes = (ViewFlipper)
		// findViewById(R.id.viewFlipperTop);

		getTopRes();
		showResTable();
	}

	@Override
	public void onTabReselected(Tab tab, FragmentTransaction arg1) {
		// TODO Auto-generated method stub
	}

	@Override
	public void onTabSelected(Tab tab, FragmentTransaction arg1) {
		// TODO Auto-generated method stub
		int mDisplayMode = tab.getPosition();
		// Log.d("SDPlay", "SummView display mode is " + mDisplayMode);
		displayViewByTab(mDisplayMode);
	}

	@Override
	public void onTabUnselected(Tab arg0, FragmentTransaction arg1) {
		// TODO Auto-generated method stub

	}

	public void displayViewByTab(int displayMode) {
		int mDisplayMode = displayMode;
		ActionBar actionBar = getActionBar();
		switch (mDisplayMode) {
		case 0:
			break;
		case 1:
			// actionBar.selectTab(actionBar.getTabAt(1));
			Intent intent1 = new Intent(this, TopResults.class);
			intent1.putExtra(MainActivity.INT_PATH, intPath);
			// finish();
			startActivity(intent1);
			break;
		case 2:
			// actionBar.selectTab(actionBar.getTabAt(2));
			Intent intent2 = new Intent(this, ShowTopFsRes.class);
			intent2.putExtra(MainActivity.INT_PATH, intPath);
			// finish();
			startActivity(intent2);
			break;
		default:
			break;
		}
	}

	// /end of class///////

}

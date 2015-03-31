package com.elena.sdplay;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.StatFs;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import com.csvreader.CsvWriter;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executor;

//import com.csvreader.CsvWriter;

//import java.nio.file.Files;

@SuppressLint("NewApi")
public class BenchStart extends Activity {

	private String journalMode = "default"; // "DELETE", "TRUNCATE",
													// "OFF", "WAL"
	private Cursor ccc;
    private final boolean LOG_ON = true;
	private final String TAG = "SDPlayDebug";
    private String device_name;
	private String sdPath;
	private String intPath;
	private String nickname = "";
	private String oemid = "";
	private String manfid = "";
	private String name = "";
	private String serial = "";
	private String card_oper_mode = "";
	private String full_details = "";
	private String fs_type = "";
	private boolean ALL = false;
	private int ROWS;// = 1000;
	private int RND_ROWS;// =1000;
	private int READ_CYCLES;// =100; // how many cycles of read to do to get
							// average speed
	// storage space
	double totalSpace;
	double freeSpace;
    long devSize, devSizeM;
    private long eMmcSize;
	// timing values
	long millis1, millis2, millis3, millis4, millis5, millis6, millis7,
			millis8;
	// long randTime, randTime1, randTime2;
	long diff_w, diff_r, diff_rnd_io, diff_rnd_read;
	double ws, rs, rndws, rndrs;
	private long currentId;

	// ///////////for fs//////////////////
	long diff_create, diff_list, diff_small_read, diff_medium_write,
			diff_medium_read, diff_large_write, diff_large_read, diff_del,
			diff_iops;
	double create_fs, list_fs, small_read_rate, medium_write_rate,
			medium_read_rate, large_write_rate, large_read_rate, del_f,
			iops_rate, iops_read_rate;
	public int MEDIUM_SIZE;
	public int LARGE_SIZE;
	public int IOPS_SIZE; // 1536; // in MB
	public int BUFFER_SIZE; // MB
	public int BUFF_IOPS_WRITE; // 256KB
	private int f_count;
	private int d_count;
	private int small_f_count;
	private int medium_f_count;
	private int large_f_count;
	private int oper_count;
	private int oper_read_count;
	ArrayList<String> list_d;// = new ArrayList<String>();
	ArrayList<String> list_f;// = new ArrayList<String>();
	Random random;// = new Random();
	byte[] bytesToWrite;// = new byte[32];
	byte[] bytesToWriteIops;
	byte[] bytesForIops;
	byte[] bytesToWriteMedium;
	byte[] bytesToWriteLarge;
	private int buffIopsSize;
	private long iopsFileSize;
	private int bufferSize;
	private int smallSize;
	private int mediumSize;
	private long largeSize;
	String largeSizeToPrint;
	double spaceForFsTest;
	double MAGIC_NUMBER = 100; // 100MB and must be > 1
	// ///////////////////////////////

	String tmp;
	private String build_id;
	private String build_type;

	// declare the progress dialog as a member field of activity
	ProgressDialog mProgressDialog;
	TextView textView, textView11;
	Executor executor = AsyncTask.SERIAL_EXECUTOR;

	public final static String CURR_RES_EXIST = "com.elena.sdplay.CURR_RES_EXIST";
	private boolean usb_drive_selected;
	private boolean userdata_selected;
	private boolean custom_drive_selected;
	public static boolean isWritten;
	public static boolean isFsDone;
//    private boolean wasCancelled;
    //public static boolean isFsTestStarted;
	private int isSaved = 0;
	private BroadcastReceiver SDCardStateChangeListener;
	final String MEDIA_REMOVED = Intent.ACTION_MEDIA_REMOVED;
	final String MEDIA_UNMOUNTED = Intent.ACTION_MEDIA_UNMOUNTED;
	final String MEDIA_BAD_REMOVAL = Intent.ACTION_MEDIA_BAD_REMOVAL;
	final String MEDIA_EJECT = Intent.ACTION_MEDIA_SCANNER_FINISHED;
	final String MEDIA_EJECT1 = Intent.ACTION_MEDIA_EJECT;

	final String MEDIA_MOUNTED = Intent.ACTION_MEDIA_MOUNTED;

	final String USB_DEVICE_DETACHED = "android.hardware.usb.action.USB_DEVICE_DETACHED";
	final String USB_DEVICE_ATTACHED = "android.hardware.usb.action.USB_DEVICE_ATTACHED";
	IntentFilter filter;
	SDCardStateChangeListener listener;

	// public synchronized SQLiteDatabase db;

	// directIO JNI lib
	public native ByteBuffer directAllocate(int size);
	public native void directFree(ByteBuffer buffer);

	public native int directOpen(String path, int mode);
	public native int directClose(int fd);
	public native int directSeek(int fd, long offset);
	public native int directRead(int fd, ByteBuffer buffer, int size);
	public native int directWrite(int fd, ByteBuffer buffer, int size);

    public native int directIOPSr(String path, int mode, int bsize);
    public native int directIOPSw(String path, int mode, int bsize);

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// get user options from shared preferences

		// load JNI lib
		System.loadLibrary("directIO");

		setContentView(R.layout.activity_bench_start);

		Intent intent = getIntent();
		// //////////////////
		sdPath = intent.getStringExtra(MainActivity.SD_PATH);

		intPath = intent.getStringExtra(MainActivity.INT_PATH);

		usb_drive_selected = intent.getBooleanExtra(
				MainActivity.USB_DRIVE_SELECTED, false);
		custom_drive_selected = intent.getBooleanExtra(
				MainActivity.CUSTOM_SELECTED, false);
		userdata_selected = intent.getBooleanExtra(
				MainActivity.USERDATA_SELECTED, false);

        device_name = Build.DEVICE;

		textView = (TextView) findViewById(R.id.text1);
		textView11 = (TextView) findViewById(R.id.text11);
		// tmp = "Testing this storage:\n" + sdPath + "\n";
	}

	@Override
	public void onResume() {
		super.onResume();

        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setMessage("");
        mProgressDialog.setIndeterminate(true);
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        mProgressDialog.setCancelable(true);
        mProgressDialog.setCanceledOnTouchOutside(false);

		// this.registerSDCardStateChangeListener();
		MainActivity.calling_activity = "Bench";
        ALL = false;
        eMmcSize = 0;
        devSize = 0;
		fs_type = "";
		filter = new IntentFilter();
		// filter.addAction(MEDIA_REMOVED);
		// filter.addAction(MEDIA_UNMOUNTED);
		// filter.addAction(MEDIA_BAD_REMOVAL);
		filter.addAction(MEDIA_EJECT);
		// filter.addAction(MEDIA_EJECT1);
		filter.addAction(MEDIA_MOUNTED);
		filter.addAction(USB_DEVICE_ATTACHED);
		filter.addAction(USB_DEVICE_DETACHED);
		filter.addDataScheme("file");
		listener = new SDCardStateChangeListener();
		this.registerReceiver(listener, filter);

		SharedPreferences userPref = PreferenceManager
				.getDefaultSharedPreferences(this);

		ViewFlipper viewToShow = (ViewFlipper) findViewById(R.id.viewFlipper);
		if (userPref.getBoolean("advanced", true)) {
			viewToShow.setDisplayedChild(0);
			// textView11.setText(tmp);
		} else {
			viewToShow.setDisplayedChild(1);
			// textView.setText(tmp);
		}
        //getEmmcSize();
		if (checkPath()) {
			getStoragesDetails();
		} else {
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setMessage(
					"Selected storage seems to be not available anymore :(\n"
							+ "Please change media for test.").setTitle(
					"Oops...");

			builder.setPositiveButton("OK",
					new DialogInterface.OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							Intent intent = new Intent(getApplicationContext(),
									MainActivity.class);
							startActivity(intent);
						}
					});

			AlertDialog dialog = builder.create();
			dialog.show();
		}

		// get user options from shared preferences
        journalMode = userPref.getString("journal", "default");
        if (LOG_ON) {
            Log.d(TAG, "DB journal mode is " + journalMode);
        }
		ROWS = Integer.parseInt(userPref.getString("rows", "1000"));
		RND_ROWS = Integer.parseInt(userPref.getString("rnd_rows", "1000"));
		READ_CYCLES = Integer.parseInt(userPref.getString("cycles", "100"));
		MEDIUM_SIZE = Integer.parseInt(userPref.getString("medium_file", "10"));
		// TODO check medium size and add dialog if size > 20MB

		if (MEDIUM_SIZE > 20) {
			MEDIUM_SIZE = 20;
		}
		// LARGE_SIZE = Integer.parseInt(userPref.getString("large_file",
		// "1024"));
		// BUFFER_SIZE = Integer.parseInt(userPref.getString("buff_size", "2"));

            String largeSizeToParse = userPref.getString("large_file", "1G");
            largeSizeToPrint = largeSizeToParse;
            String bufferSizeToParse = userPref.getString("buff_size", "2048K");

            String pattern1 = "^(\\d+)\\s*[K|k]+.*";
            String pattern2 = "^(\\d+)\\s*[M|m]+.*";
            String pattern3 = "^(\\d+)\\s*";
            String pattern4 = "^(\\d+)\\s*[G|g]+.*";
            String pattern5 = "^(\\d+\\.\\d+)\\s*[G|g]+.*";

            // parse buffer size, if no letter - MB by default
            if (bufferSizeToParse.matches(pattern1)) {
                // Kb
                bufferSize = Integer.parseInt(bufferSizeToParse.replaceAll(
                        pattern1, "$1")) * 1024;
            } else if (bufferSizeToParse.matches(pattern2)) {
                // Mb
                bufferSize = Integer.parseInt(bufferSizeToParse.replaceAll(
                        pattern2, "$1")) * 1024 * 1024;
            } else if (bufferSizeToParse.matches(pattern3)) {
                // Kb
                bufferSize = Integer.parseInt(bufferSizeToParse.replaceAll(
                        pattern3, "$1")) * 1024;

            } else {
                // TODO: dialog
                bufferSize = 2 * 1024 * 1024; // default - 2Mb
            }
            // Log.d("SDPlay", "Buffer size parsed: " + bufferSize);

            // parse large file size, if no letter - MB by default
            if (largeSizeToParse.matches(pattern2)) {
                // MB
                LARGE_SIZE = Integer.parseInt(largeSizeToParse.replaceAll(pattern2,
                        "$1"));
            } else if (largeSizeToParse.matches(pattern4)) {
                // GB
                LARGE_SIZE = Integer.parseInt(largeSizeToParse.replaceAll(pattern4,
                        "$1")) * 1024;
            } else if (largeSizeToParse.matches(pattern5)) {
                // GB
                LARGE_SIZE = (int) Math.round(Double.parseDouble(largeSizeToParse
                        .replaceAll(pattern5, "$1")) * 1024);
            } else if (largeSizeToParse.matches(pattern3)) {
                // MB
                LARGE_SIZE = Integer.parseInt(largeSizeToParse.replaceAll(pattern3,
                        "$1"));
                largeSizeToPrint = largeSizeToParse + "MB";
            } else {
                // TODO: dialog
                LARGE_SIZE = 1 * 1024; // default - 1GB, LARGE_SIZE value is always
                // stored in MB
            }
            // Log.d("SDPlay", "Large size in MB: " + LARGE_SIZE);
            // Log.d("SDPlay", "Large size for printing: " + largeSizeToPrint);

            IOPS_SIZE = 100; // 1536; // in Mb
            // BUFF_IOPS_WRITE = 256; // 256Kb
            buffIopsSize = 4 * 1024;
            iopsFileSize = IOPS_SIZE * 1024 * 1024;
            smallSize = 1024;
            bytesToWrite = new byte[smallSize];// = new byte[32];
            // bytesToWriteIops = new byte[BUFF_IOPS_WRITE * 1024];
            bytesForIops = new byte[buffIopsSize];
            mediumSize = MEDIUM_SIZE * 1024 * 1024;
            bytesToWriteMedium = new byte[mediumSize];
            // bufferSize = BUFFER_SIZE * 1024 * 1024;
            largeSize = LARGE_SIZE * 1024 * 1024;
            bytesToWriteLarge = new byte[bufferSize];

            spaceForFsTest = LARGE_SIZE * 2 + MEDIUM_SIZE * 50 + IOPS_SIZE
                    + MAGIC_NUMBER;
            // spaceForFsTest is in MB, freeSpace - in GB

            Log.d("SDPlay", "need space: " + spaceForFsTest / 1024 + "Gb");
            Log.d("SDPlay", "free space: " + freeSpace + "Gb");

            if ((spaceForFsTest / 1024) > freeSpace) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage(
                        "There is no enough space for FS test\n"
                                + "Please change test files sizes in settings.")
                        .setTitle("Oops...");

                builder.setPositiveButton("OK",
                        new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Intent intent = new Intent(getApplicationContext(),
                                        SetPreferences.class);
                                startActivity(intent);
                            }
                        });
                builder.setNegativeButton("Ignore",
                        new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });

                AlertDialog dialog = builder.create();
                dialog.show();

            }

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.action_show_top) {
			Intent intent = new Intent(this, SummaryView.class);
			intent.putExtra(MainActivity.INT_PATH, intPath);
			startActivity(intent);
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
/*
	public void onReadAllClick(View view) {
		final ReadAllRecords readAll = new ReadAllRecords(this);
		readAll.executeOnExecutor(executor, 100, 0, 1);
		mProgressDialog
				.setOnCancelListener(new DialogInterface.OnCancelListener() {
					@Override
					public void onCancel(DialogInterface dialog) {

						readAll.cancel(true);
					}
				});
		// SQLiteDatabase.releaseMemory();
	}
*/
public void onDeleteAllClick(View view) {
    final DeleteAllRecords deleteAll = new DeleteAllRecords(this);
    deleteAll.executeOnExecutor(executor, 100, 0, 1);
    mProgressDialog
            .setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {

                    deleteAll.cancel(true);
                }
            });
}

	public void onAllTestClick(View view) {
		ALL = true;
		EditText nickName = (EditText) findViewById(R.id.nickname1);
		EditText details = (EditText) findViewById(R.id.details1);
		nickname = nickName.getText().toString();
		full_details = details.getText().toString();
		if (LOG_ON) {
			Log.d(TAG, "Starting full test...");
		}
		onWriteClick(view);
	}

	public void onRndReadClick(View view) {
		final RndRead rndRead = new RndRead(this);
		rndRead.executeOnExecutor(executor, 100, 0, 1);
		mProgressDialog
				.setOnCancelListener(new DialogInterface.OnCancelListener() {
					@Override
					public void onCancel(DialogInterface dialog) {

                        rndRead.cancel(true);
					}
				});
		// SQLiteDatabase.releaseMemory();
	}

	public void onWriteClick(View view) {
		if (checkPath()) {
			isWritten = false;
			ws = 0;
			rs = 0;
			rndws = 0;
			rndrs = 0;
            /////delete previous fs test values as well
            isFsDone = false;

            create_fs = 0;
            list_fs = 0;
            medium_write_rate = 0;
            medium_read_rate = 0;
            large_write_rate = 0;
            large_read_rate = 0;
            ////////////////////
            tmp = "";
			textView.setText(tmp);
			textView11.setText(tmp);
			if (!ALL) {
				EditText nickName = (EditText) findViewById(R.id.nickname);
				EditText details = (EditText) findViewById(R.id.details);
				nickname = nickName.getText().toString();
				full_details = details.getText().toString();
			}
			if (LOG_ON) {
				Log.d(TAG, "DB testing is started...");
			}
			final WriteNewRecords writeNew = new WriteNewRecords(this);
			writeNew.executeOnExecutor(executor, 100, 0, 1);
			mProgressDialog
					.setOnCancelListener(new DialogInterface.OnCancelListener() {
						@Override
						public void onCancel(DialogInterface dialog) {

                            writeNew.cancel(true);
						}
					});
			// SQLiteDatabase.releaseMemory();

		} else {
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setMessage(
					"Selected storage seems to be not available anymore :(\n"
							+ "Please change media for test.").setTitle(
					"Oops...");

			builder.setPositiveButton("OK",
					new DialogInterface.OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							Intent intent = new Intent(getApplicationContext(),
									MainActivity.class);
							startActivity(intent);
						}
					});

			AlertDialog dialog = builder.create();
			dialog.show();
		}

	}

	public void onRndIOClick(View view) {
		final RandomUpdate rndWrite = new RandomUpdate(this);
		rndWrite.executeOnExecutor(executor, 100, 0, 1);
		mProgressDialog
				.setOnCancelListener(new DialogInterface.OnCancelListener() {
					@Override
					public void onCancel(DialogInterface dialog) {

                        rndWrite.cancel(true);
					}
				});
		// SQLiteDatabase.releaseMemory();
	}

	// ///////////////////////////////////
	// progress bar for writing
	private class WriteNewRecords extends AsyncTask<Integer, Integer, Integer> {

		private Context context;
		private PowerManager.WakeLock mWakeLockW;

		public WriteNewRecords(Context context) {
			this.context = context;
		}

		protected Integer doInBackground(Integer... params) {
			MyDBHelper myDB = new MyDBHelper(getBaseContext(), sdPath);
			// Gets the data repository in write mode
			SQLiteDatabase db = myDB.getWritableDatabase();

			// clean test table and reset auto-increment key
			db.execSQL("DELETE FROM 'My_table'");
			db.execSQL("DELETE FROM SQLITE_SEQUENCE WHERE NAME = 'My_table'");
            if (!journalMode.equals("default")) {
                ccc = db.rawQuery("PRAGMA journal_mode = " + journalMode
                        + "; PRAGMA synchronous = FULL;", null);
                ccc.moveToNext();
                ccc.getString(0);
                ccc.close();
            }

			// Create a new map of values, where column names are the keys
			ContentValues values = new ContentValues();
			// long newRowId;

			Random rand = new Random();
			// randTime = 0;
			// get time at this moment
			millis1 = SystemClock.elapsedRealtime();

			// Random rand = new Random();
			for (int j = 1; j <= ROWS; j++) {
				if (isCancelled()) {
					mWakeLockW.release();
                 //   wasCancelled = true;
					db.close();
					return 0;
				} else {
					db.beginTransaction();
					try {
						// randTime1 = SystemClock.elapsedRealtime();
						values.put(
								myDB.REC_NAME,
								"Random string: "
										+ Long.toString(rand.nextLong()));
						values.put(myDB.REC_COLOR, rand.nextInt(256));
						values.put(myDB.REC_TYPE, j);
						// randTime2 = SystemClock.elapsedRealtime() -
						// randTime1;
						// randTime += randTime2;
						// Insert the new row, returning the primary key value
						// of the new row
						/* newRowId = */db.insert(myDB.MY_TABLE, null, values);
						db.setTransactionSuccessful();
					} finally {
						db.endTransaction();
					}
					publishProgress(j * mProgressDialog.getMax() / ROWS);
				}
			}
			// SQLiteDatabase.releaseMemory();
			// get time at this moment
			millis2 = SystemClock.elapsedRealtime();
			// Log.d("SDPlay", "random time " + randTime + "ms");
			// writing time in ms
			diff_w = millis2 - millis1;
			db.close();
			return 1;
			// When finished, return the resulting 1, this will cause the
			// Activity to call onPostExecute()
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();

			// take CPU lock to prevent CPU from going off if the user
			// presses the power button during download
			PowerManager pm = (PowerManager) getApplicationContext()
					.getSystemService(Context.POWER_SERVICE);
			mWakeLockW = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
					getClass().getName());
			mWakeLockW.acquire();

			mProgressDialog.setIndeterminate(false);
			mProgressDialog.setMax(ROWS);
			mProgressDialog.setMessage("Database creation is in progress...");
			mProgressDialog.show();
		}

		@Override
		protected void onProgressUpdate(Integer... progress) {
			super.onProgressUpdate(progress);
			mProgressDialog.setProgress(progress[0]);
		}

		@Override
		protected void onPostExecute(Integer result) {
			mWakeLockW.release();
			mProgressDialog.dismiss();
			if (result != 1) {
				Toast.makeText(getBaseContext(), "DB creation error",
						Toast.LENGTH_LONG).show();
			} else {
				//tmp += "\nTotal rows created: " + ROWS;
				ws = ROWS * 1000.0 / diff_w;
				tmp += "\nWrite speed " + String.format("%.2f", ws)
						+ " records/s";
				Toast.makeText(getBaseContext(),
						"DB has been created successfully", Toast.LENGTH_SHORT)
						.show();
			}
			if (ALL) {
				textView11.setText(tmp);
			} else {
				textView.setText(tmp);
			}
			onRndReadClick(findViewById(android.R.id.content).getRootView());

		}
	}

	// end of write progress
	// bar/////////////////////////////////////////////////

    /*
	// ///////////////////////////////////
	// progress bar for reading
	private class ReadAllRecords extends AsyncTask<Integer, Integer, Integer> {

		int rows_count = 0;
		private Context context;
		private PowerManager.WakeLock mWakeLockR;

		public ReadAllRecords(Context context) {
			this.context = context;
		}

		protected Integer doInBackground(Integer... params) {
			// open database in read mode
			MyDBHelper myDB = new MyDBHelper(getBaseContext(), sdPath);
			diff_r = 0;
			// take timestamp
			millis3 = SystemClock.elapsedRealtime();
			List<String> recs = new ArrayList<String>();
			Cursor c;
			SQLiteDatabase db = myDB.getReadableDatabase();
			Cursor ccc = db.rawQuery("PRAGMA journal_mode = " + journalMode
					+ "; PRAGMA synchronous = FULL;", null);
			ccc.moveToNext();
			ccc.getString(0);

			for (int g = 0; g < READ_CYCLES; g++) {
				if (isCancelled()) {
					mWakeLockR.release();
				//	wasCancelled = true;
					db.close();
					return 0;
				} else {

					// Define a projection that specifies which columns from the
					// database
					// you will actually use after this query.
					// Here ALL columns are listed
					// SELECT * FROM MY_TABLE
					String[] projection = { myDB.REC_ID, myDB.REC_TYPE,
							myDB.REC_NAME, myDB.REC_COLOR };

					db.beginTransaction();
					publishProgress(0);
					c = db.query(myDB.MY_TABLE, // The table to query
												// projection,
							null, // The columns to return
							null, // The columns for the WHERE clause (null
							// means getting ALL records here)
							null, // The values for the WHERE clause
							null, // don't group the rows
							null, // don't filter by row groups
							null // don't sort
					);
					try {
						rows_count = c.getCount();
						// Record all selected rows into array
						c.moveToFirst();
						while (!c.isAfterLast()) {
							recs.add(c.getInt(0) + "..." + c.getInt(1) + "..."
									+ c.getString(2) + "..." + c.getInt(3)
									+ "\n");
							c.moveToNext();
						}
						db.setTransactionSuccessful();
					} finally {
						db.endTransaction();
						c.close();
					}
				}

			}

			// ////////end of original read all code/////////////////////
			// /////////////////////////////////////
			// timestamp after selection
			millis4 = SystemClock.elapsedRealtime();
			// Time spent on reading all db rows
			diff_r = millis4 - millis3;
			ccc.close();
			db.close();
			publishProgress(100);
			return 1;
			// When finished, return the resulting 1, this will cause the
			// Activity to call onPostExecute()
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			// take CPU lock to prevent CPU from going off if the user
			// presses the power button during download
			PowerManager pm = (PowerManager) getApplicationContext()
					.getSystemService(Context.POWER_SERVICE);
			mWakeLockR = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
					getClass().getName());
			mWakeLockR.acquire();

			mProgressDialog.setIndeterminate(true);
			mProgressDialog.setMessage("Database reading is in progress...");
			mProgressDialog.setMax(1);
			mProgressDialog.show();
		}

		@Override
		protected void onProgressUpdate(Integer... progress) {
			super.onProgressUpdate(progress);
			if (progress[0] != 100) {
				mProgressDialog.setIndeterminate(true);
			} else {
				mProgressDialog.setIndeterminate(false);
				mProgressDialog.setProgress(progress[0]);
			}
		}

		@Override
		protected void onPostExecute(Integer result) {
			mWakeLockR.release();
			mProgressDialog.dismiss();
			if (result != 1)
				Toast.makeText(getBaseContext(), "DB read error",
						Toast.LENGTH_LONG).show();
			else {
				tmp += "\n" + rows_count + " records have been read "
						+ READ_CYCLES + " times";
				rs = rows_count * READ_CYCLES * 1000.0 / diff_r;
				tmp += "\nAverage read speed " + String.format("%.2f", rs)
						+ " records/s";
				// textView.setText(tmp);
				Toast.makeText(getBaseContext(),
						"All records have been read successfully",
						Toast.LENGTH_SHORT).show();
				isWritten = true;
			}
			isSaved = saveRes();
			if (LOG_ON) {
				Log.d(TAG, "saveRes() has been called and return " + isSaved);
				Log.d(TAG, "Before calling ALL value is " + ALL
						+ "; DB test done is " + isWritten);
			}
			if (!ALL && (isSaved == 1)) {
				showTopRes();
			}
			if (ALL) {
				onFsTestClick(findViewById(android.R.id.content).getRootView());
				textView11.setText(tmp);
				// ALL = false;
			} else {
				textView.setText(tmp);
			}
		}
	}

	// end of read progress bar/////////////////////////////////////////////////
    */

	// ///////////////////////////////////
	// progress bar for random update
	private class RandomUpdate extends AsyncTask<Integer, Integer, Integer> {

		int rows_updated = 0;
		private Context context;
		private PowerManager.WakeLock mWakeLockRW;

		public RandomUpdate(Context context) {
			this.context = context;
		}

		protected Integer doInBackground(Integer... params) {
			MyDBHelper myDB = new MyDBHelper(getBaseContext(), sdPath);
			SQLiteDatabase db = myDB.getWritableDatabase();
            if (!journalMode.equals("default")) {
                ccc = db.rawQuery("PRAGMA journal_mode = " + journalMode
                        + "; PRAGMA synchronous = FULL;", null);
                ccc.moveToNext();
                ccc.getString(0);
                ccc.close();
            }

			ContentValues values = new ContentValues();
			Random rand = new Random();
			// get time at this moment
			millis5 = SystemClock.elapsedRealtime();
			for (int j = 0; j < RND_ROWS; j++) {
				if (isCancelled()) {
					mWakeLockRW.release();
                 //   wasCancelled = true;
					db.close();
					return 0;
				} else {
					db.beginTransaction();
					try {
						values.put(
								myDB.REC_NAME,
								"Updated record: "
										+ Long.toString(rand.nextLong()));
						values.put(myDB.REC_COLOR, rand.nextInt(128));
						String whereClause = myDB.REC_ID + " = ? ";
						String[] whereArgs = new String[] { String
								.valueOf(1 + rand.nextInt(ROWS)) };
						rows_updated += db.update(myDB.MY_TABLE, values,
								whereClause, whereArgs);

						db.setTransactionSuccessful();
					} finally {
						db.endTransaction();
						publishProgress(j * mProgressDialog.getMax() / RND_ROWS);
					}
				}
			}

			// get time at this moment
			millis6 = SystemClock.elapsedRealtime();
			// Time spent for random selection and update records
			diff_rnd_io = millis6 - millis5;

			db.close();
			return 1;
			// When finished, return the resulting 1, this will cause the
			// Activity to call onPostExecute()
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			// take CPU lock to prevent CPU from going off if the user
			// presses the power button during download
			PowerManager pm = (PowerManager) context
					.getSystemService(Context.POWER_SERVICE);
			mWakeLockRW = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
					getClass().getName());
			mWakeLockRW.acquire();

			mProgressDialog.setIndeterminate(false);
			mProgressDialog.setMax(RND_ROWS);
			mProgressDialog.setMessage("Random write is in progress...");
			mProgressDialog.show();
		}

		@Override
		protected void onProgressUpdate(Integer... progress) {
			super.onProgressUpdate(progress);
			mProgressDialog.setProgress(progress[0]);
		}

		@Override
		protected void onPostExecute(Integer result) {
			mWakeLockRW.release();
			mProgressDialog.dismiss();
			if (result != 1)
				Toast.makeText(getBaseContext(), "DB access error",
						Toast.LENGTH_LONG).show();
			else {
				//tmp += "\nWritten records count: " + rows_updated;
				rndws = rows_updated * 1000.0 / diff_rnd_io;
				tmp += "\nRandom write speed " + String.format("%.2f", rndws)
						+ " records/s";
				// if (ALL == 1) {
				// tmp += "\n\n\nTest is finished :)";
				// }
				// textView.setText(tmp);
				Toast.makeText(getBaseContext(),
						rows_updated + " records have been written",
						Toast.LENGTH_SHORT).show();
			}
			if (ALL) {
				// onRndReadClick(findViewById(android.R.id.content).getRootView());
				// onReadAllClick(findViewById(android.R.id.content).getRootView());
				textView11.setText(tmp);
			} else {
				textView.setText(tmp);
			}
			//onReadAllClick(findViewById(android.R.id.content).getRootView());
            onDeleteAllClick(findViewById(android.R.id.content).getRootView());
		}

	}

	// end of random write progress
	// bar/////////////////////////////////////////////////

	// ///////////////////////////////////
	// progress bar for random read
	private class RndRead extends AsyncTask<Integer, Integer, Integer> {

		private Context context;
		private PowerManager.WakeLock mWakeLockRR;

		public RndRead(Context context) {
			this.context = context;
		}

		protected Integer doInBackground(Integer... params) {
			MyDBHelper myDB = new MyDBHelper(getBaseContext(), sdPath);
			SQLiteDatabase db = myDB.getReadableDatabase();
            if (!journalMode.equals("default")) {
                ccc = db.rawQuery("PRAGMA journal_mode = " + journalMode
                        + "; PRAGMA synchronous = FULL;", null);
                ccc.moveToNext();
                ccc.getString(0);
                ccc.close();
            }

			List<String> recs = new ArrayList<String>();
			Random rand = new Random();
			// get time at this moment
			millis7 = SystemClock.elapsedRealtime();
			for (int j = 0; j < RND_ROWS; j++) {
				if (isCancelled()) {
					mWakeLockRR.release();
                //    wasCancelled = true;
					db.close();
					return 0;
				} else {
					db.beginTransaction();
					// Define a projection that specifies which columns from the
					// database
					// you will actually use after this query.
					String[] projection = { myDB.REC_ID, myDB.REC_TYPE,
							myDB.REC_NAME, myDB.REC_COLOR };

					// How you want the results sorted in the resulting Cursor
					String sortOrder = myDB.REC_ID + " ASC";

					String whereClause = myDB.REC_ID + " = ? ";
					String[] whereArgs = new String[] { String.valueOf(1 + rand
							.nextInt(ROWS)) };

					Cursor c = db.query(myDB.MY_TABLE, // The table to query
							projection, // The columns to return
							whereClause, // The columns for the WHERE clause
							whereArgs, // The values for the WHERE clause
							null, // don't group the rows
							null, // don't filter by row groups
							sortOrder // The sort order
							);
					try {
						c.moveToFirst();
						while (!c.isAfterLast()) {
							recs.add(c.getInt(0) + "..." + c.getInt(1) + "..."
									+ c.getString(2) + "..." + c.getInt(3)
									+ "\n");
							c.moveToNext();
						}
						db.setTransactionSuccessful();
					} finally {
						db.endTransaction();
						c.close();
					}
					publishProgress(j * mProgressDialog.getMax() / RND_ROWS);

				}
			}
			// get time at this moment
			millis8 = SystemClock.elapsedRealtime();
			// Time spent for random selection
			diff_rnd_read = millis8 - millis7;
			// //////////////////////////////////////////////////////

			db.close();
			return 1;
			// When finished, return the resulting 1, this will cause the
			// Activity to call onPostExecute()
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			// take CPU lock to prevent CPU from going off if the user
			// presses the power button during download
			PowerManager pm = (PowerManager) context
					.getSystemService(Context.POWER_SERVICE);
			mWakeLockRR = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
					getClass().getName());
			mWakeLockRR.acquire();

			mProgressDialog.setIndeterminate(false);
			mProgressDialog.setMax(RND_ROWS);
			mProgressDialog.setMessage("Random read is in progress...");
			mProgressDialog.show();
		}

		@Override
		protected void onProgressUpdate(Integer... progress) {
			super.onProgressUpdate(progress);
			mProgressDialog.setProgress(progress[0]);
		}

		@Override
		protected void onPostExecute(Integer result) {
			mWakeLockRR.release();
			mProgressDialog.dismiss();
			if (result != 1)
				Toast.makeText(getBaseContext(), "DB access error",
						Toast.LENGTH_LONG).show();
			else {
				//tmp += "\n" + RND_ROWS + " random records have been read";
				rndrs = RND_ROWS * 1000.0 / diff_rnd_read;
				tmp += "\nRandom read speed " + String.format("%.2f", rndrs)
						+ " records/s";
				// textView.setText(tmp);
				Toast.makeText(getBaseContext(),
						RND_ROWS + " random records have been read",
						Toast.LENGTH_SHORT).show();
			}
			if (ALL) {
				// onRndIOClick(findViewById(android.R.id.content).getRootView());
				textView11.setText(tmp);
				// ALL = false;
			} else {
				textView.setText(tmp);
			}
			onRndIOClick(findViewById(android.R.id.content).getRootView());
			// isSaved = saveRes();
			// if (!ALL && (isSaved == 1)) {
			// showTopRes();
			// }
		}

	}

	// end of progress bar/////////////////////////////////////////////////

    // ///////////////////////////////////
    // progress bar for delete all records
    private class DeleteAllRecords extends AsyncTask<Integer, Integer, Integer> {

        int rows_count = 0;
        private Context context;
        private PowerManager.WakeLock mWakeLockD;

        public DeleteAllRecords(Context context) {
            this.context = context;
        }

        protected Integer doInBackground(Integer... params) {
            // open database in read mode
            MyDBHelper myDB = new MyDBHelper(getBaseContext(), sdPath);
            diff_r = 0;
            // take timestamp
            millis3 = SystemClock.elapsedRealtime();
            List<String> recs = new ArrayList<String>();
            Cursor c;
            SQLiteDatabase db = myDB.getWritableDatabase();
            if (!journalMode.equals("default")) {
                ccc = db.rawQuery("PRAGMA journal_mode = " + journalMode
                        + "; PRAGMA synchronous = FULL;", null);
                ccc.moveToNext();
                ccc.getString(0);
                ccc.close();
            }

                if (isCancelled()) {
                    mWakeLockD.release();
                //    wasCancelled = true;
                    db.close();
                    return 0;
                } else {

                    db.beginTransaction();
                    publishProgress(0);
                    String whereClause = myDB.REC_ID + " = ? ";
                    for (int i=1; i <= Math.round(ROWS/2); i++) {
                        String[] whereArgs1 = new String[]{String
                                .valueOf(i)};
                        String[] whereArgs2 = new String[]{String
                                .valueOf(Math.round(ROWS/2) + i)};
                        rows_count += db.delete(myDB.MY_TABLE,
                                whereClause, whereArgs1);
                        rows_count += db.delete(myDB.MY_TABLE,
                                whereClause, whereArgs2);
                        //rows_count = db.delete(myDB.MY_TABLE, null, null);
                        publishProgress(2 * i * mProgressDialog.getMax() / ROWS);
                    }
                    db.setTransactionSuccessful();
                    db.endTransaction();
                }

            // timestamp after deletion
            millis4 = SystemClock.elapsedRealtime();
            // Time spent on reading all db rows
            diff_r = millis4 - millis3;
            db.close();

            return 1;
            // When finished, return the resulting 1, this will cause the
            // Activity to call onPostExecute()
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            // take CPU lock to prevent CPU from going off if the user
            // presses the power button during download
            PowerManager pm = (PowerManager) getApplicationContext()
                    .getSystemService(Context.POWER_SERVICE);
            mWakeLockD = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                    getClass().getName());
            mWakeLockD.acquire();

            mProgressDialog.setIndeterminate(false);
            mProgressDialog.setMessage("Database records deletion is in progress...");
            mProgressDialog.setMax(ROWS);
            mProgressDialog.show();
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            super.onProgressUpdate(progress);
            mProgressDialog.setProgress(progress[0]);
        }

        @Override
        protected void onPostExecute(Integer result) {
            mWakeLockD.release();
            mProgressDialog.dismiss();
            if (result != 1)
                Toast.makeText(getBaseContext(), "DB deletion error",
                        Toast.LENGTH_LONG).show();
            else {
                //tmp += "\n" + rows_count + " records have been deleted";
                rs = rows_count * 1000.0 / diff_r;
                tmp += "\nAverage deletion speed " + String.format("%.2f", rs)
                        + " records/s";
                // textView.setText(tmp);
                Toast.makeText(getBaseContext(),
                        "All records have been deleted successfully",
                        Toast.LENGTH_SHORT).show();
                isWritten = true;
            }
            isSaved = saveRes();
            if (LOG_ON) {
                Log.d(TAG, "saveRes() has been called and return " + isSaved);
                Log.d(TAG, "Before calling ALL value is " + ALL
                        + "; DB test done is " + isWritten);
            }
            if (!ALL && (isSaved == 1)) {
                exportResToCsv();
                showTopRes();
            }
            if (ALL) {
                onFsTestClick(findViewById(android.R.id.content).getRootView());
                textView11.setText(tmp);
                // ALL = false;
            } else {
                textView.setText(tmp);
            }
        }
    }

    // end of delete progress bar/////////////////////////////////////////////////

	public int saveRes() {
		// Save current results to database
		if ((ALL && isWritten && isFsDone) || (!ALL && isWritten)
				|| (!ALL && isFsDone)) {
			build_id = Build.ID;
			build_type = Build.TYPE;
			MyResDBHelper myResDB = new MyResDBHelper(getBaseContext(), intPath);
			SQLiteDatabase res_db = myResDB.getWritableDatabase();
			// Create a new map of values, where column names are the keys
			ContentValues values = new ContentValues();
			// long newResRowId;
			res_db.beginTransaction();
			try {

				if (!(oemid.isEmpty())) {
					values.put(myResDB.RES_OEMID, oemid);
				}
				if (!(manfid.isEmpty())) {
					values.put(myResDB.RES_MANFID, manfid);
				}
				if (!(name.isEmpty())) {
					values.put(myResDB.RES_NAME, name);
				}

				values.put(myResDB.RES_DETAILS, full_details);
                values.put(myResDB.RES_DEV_SIZE, devSize + " GB");
				values.put(myResDB.RES_SERIAL, serial);
				//if (!(card_oper_mode.isEmpty())) {
				//	values.put(myResDB.RES_OPER_MODE, card_oper_mode);
				//}
				values.put(myResDB.RES_NOTES, nickname);
				values.put(myResDB.RES_BUILD_ID, build_id + "/" + build_type);
                if (MainActivity.isEncrypted) {
                    //String enc = "\u0364\u1DE0\u0368";
                    values.put(myResDB.RES_FS_TYPE, fs_type + " (enc)");
                } else {
                    values.put(myResDB.RES_FS_TYPE, fs_type);
                }
                values.put(myResDB.RES_JOURNAL, journalMode);
				values.put(myResDB.RES_W_SPEED, String.format("%.2f", ws));
				values.put(myResDB.RES_D_SPEED, String.format("%.2f", rs));
				values.put(myResDB.RES_RW_SPEED, String.format("%.2f", rndws));
				values.put(myResDB.RES_RR_SPEED, String.format("%.2f", rndrs));
				// values.put(myResDB.RES_TOTAL_SCORE, String.format("%.2f", (ws
				// * 1000 + rs + rndws * 1000 + rndrs * 100) / 400));
				double totalDbScore = (ws * 1000 + rs + rndws * 1000 + rndrs * 100) / 400;
				values.put(myResDB.RES_TOTAL_SCORE, totalDbScore);
				// fs part
				values.put(myResDB.FS_C_SPEED, String.format("%.2f", create_fs));
				values.put(myResDB.FS_L_SPEED, String.format("%.2f", list_fs));
				values.put(myResDB.FS_RS_SPEED,
						String.format("%.2f", small_read_rate));
				values.put(myResDB.FS_WM_SPEED,
						String.format("%.2f", medium_write_rate));
				values.put(myResDB.FS_RM_SPEED,
						String.format("%.2f", medium_read_rate));
				values.put(myResDB.FS_WL_SPEED,
						String.format("%.2f", large_write_rate));
				values.put(myResDB.FS_RL_SPEED,
						String.format("%.2f", large_read_rate));
                values.put(myResDB.FS_IOPS_W, String.format("%.2f", iops_rate));
                values.put(myResDB.FS_IOPS_R, String.format("%.2f", iops_read_rate));
				values.put(myResDB.FS_IOPS_SCORE, String.format("%.2f", iops_rate)
						+ " / " + String.format("%.2f", iops_read_rate));
				values.put(myResDB.FS_D_SPEED, String.format("%.2f", del_f));
				double totalFsScore = (create_fs * 10 + list_fs
						+ small_read_rate * 10 + medium_write_rate * 100
						+ medium_read_rate * 100 + large_write_rate * 100
						+ large_read_rate * 100 + del_f * 10 + iops_rate * 10 + iops_read_rate) / 100;
				values.put(myResDB.FS_TOTAL_SCORE, totalFsScore);
				values.put(
						myResDB.FS_SM_SCORE,
						String.format("%.2f", (create_fs * 10 + list_fs
								+ small_read_rate * 10 + del_f * 10) / 40));
				values.put(myResDB.FS_M_SCORE,
						String.format("%.2f", medium_write_rate) + " / "
								+ String.format("%.2f", medium_read_rate));
				values.put(myResDB.FS_L_SCORE,
						String.format("%.2f", large_write_rate) + " / "
								+ String.format("%.2f", large_read_rate));
				if (ALL) {
					values.put(myResDB.SUMMARY_SCORE,
							(totalDbScore + totalFsScore) / 2);
				} else {
					values.put(myResDB.SUMMARY_SCORE, 0);
				}
				if (LOG_ON) {
					Log.d(TAG, "From save method: ALL is " + ALL);
					Log.d(TAG, "DB tests are done: " + isWritten);
					Log.d(TAG, "FS tests are done: " + isFsDone);
					Log.d(TAG, "DB Score saved: " + totalDbScore);
					Log.d(TAG, "FS Score saved: " + totalFsScore);
					Log.d(TAG,
							"DB Update rate saved: "
									+ String.format("%.2f", rndws));
					Log.d(TAG,
							"Large file metrics saved: "
									+ String.format("%.2f", large_write_rate)
									+ " / "
									+ String.format("%.2f", large_read_rate));
					Log.d(TAG, "From save method: Saving is completed");
				}
				// Insert the new row
				currentId = res_db.insert(myResDB.RES_TABLE, null, values);
				res_db.setTransactionSuccessful();
			} finally {
				res_db.endTransaction();
				// isWritten = true;
			}
			res_db.close();

			return 1;
		} else {
			if (LOG_ON) {
				Log.d(TAG, "From save method: Nothing to save yet");
			}
			return 0;
		}

	}

    public int exportResToCsv() {
        //File dbFile = getDatabasePath("YourDatabase.db");

        MyResDBHelper myResDB = new MyResDBHelper(getBaseContext(), intPath);
        SQLiteDatabase db = myResDB.getReadableDatabase();
        File file = null;
        //new File("/sdcard1/sdplay_results").mkdirs();

        new File("sdcard" + File.separator + "sdplay_results").mkdirs();
        File exportDir = new File("sdcard" + File.separator + "sdplay_results");
        file = new File(exportDir.getAbsolutePath(), device_name + "-" + Build.SERIAL + "-SDPlay.csv");


        try
        {
            if (file.exists()) {
                file.delete();
            }
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
                Log.d(TAG, "Export to csv is skipped due to file not exist");
                Toast.makeText(getBaseContext(), "Export to csv has been skipped: file does not exist",
                        Toast.LENGTH_LONG).show();
                return 0;
            }
                Log.d(TAG, "File to export results is: " + file.getAbsolutePath());

            //com.csvreader.CsvWriter csvWrite = new com.csvreader.CsvWriter(new FileWriter(file, true), ',');

            CsvWriter csvWrite = new CsvWriter(new FileWriter(file, true), ',');
            Cursor curCSV;
            //Cursor curCSV = db.rawQuery("SELECT * FROM RES_TABLE", null);

            ////////////////
            String[] ref_res = getResources().getStringArray(
                    R.array.reference_results);
            int REF_COUNT = ref_res.length;
            String whereClause = myResDB.RES_ID + " > ?";
            String[] whereArgs = new String[] {String.valueOf(REF_COUNT)};

            String[] projection = { myResDB.RES_DETAILS,
                    myResDB.RES_DEV_SIZE, myResDB.RES_SERIAL,
                    myResDB.RES_BUILD_ID, myResDB.RES_FS_TYPE, myResDB.RES_NOTES,
                    myResDB.RES_JOURNAL,
                    myResDB.RES_W_SPEED, myResDB.RES_RW_SPEED, myResDB.RES_RR_SPEED,
                    myResDB.RES_D_SPEED, myResDB.RES_TOTAL_SCORE,
                    myResDB.FS_C_SPEED, myResDB.FS_L_SPEED, myResDB.FS_RS_SPEED,
                    myResDB.FS_WM_SPEED, myResDB.FS_RM_SPEED,
                    myResDB.FS_WL_SPEED, myResDB.FS_RL_SPEED,
                    myResDB.FS_IOPS_W, myResDB.FS_IOPS_R,
                    myResDB.FS_D_SPEED, myResDB.FS_TOTAL_SCORE,
                    myResDB.SUMMARY_SCORE };

            curCSV = db.query(myResDB.RES_TABLE, // The table to query
                    projection, // The columns to return
                    whereClause, // The columns for the WHERE clause (null
                    // means getting ALL records here)
                    whereArgs, // The values for the WHERE clause
                    null, // don't group the rows
                    null, // don't filter by row groups
                    null, // sort by Total score
                    null // limit results number to 5
            );
            //////////////

            csvWrite.writeRecord(curCSV.getColumnNames());
            curCSV.moveToFirst();
/*
            while (!curCSV.isAfterLast())  {

                String arrStr[] = { curCSV.getString(0), curCSV.getString(1),

                        curCSV.getString(2), curCSV.getString(3),
                        curCSV.getString(4), curCSV.getString(5),
                        curCSV.getString(6), curCSV.getString(7),
                        curCSV.getString(8), curCSV.getString(9),
                        curCSV.getString(10), curCSV.getString(11),
                        curCSV.getString(12), curCSV.getString(13),
                        curCSV.getString(14), curCSV.getString(15),
                        curCSV.getString(16), curCSV.getString(17),
                        curCSV.getString(18), curCSV.getString(19),
                        curCSV.getString(20), curCSV.getString(21),
                        curCSV.getString(22), curCSV.getString(23),
                        curCSV.getString(24), curCSV.getString(25),
                        curCSV.getString(26), curCSV.getString(27),
                        curCSV.getString(28), curCSV.getString(29),
                        curCSV.getString(30), curCSV.getString(31)
                };
                */

            while (!curCSV.isAfterLast())  {

                String arrStr[] = { curCSV.getString(0), curCSV.getString(1),

                        curCSV.getString(2), curCSV.getString(3),
                        curCSV.getString(4), curCSV.getString(5),
                        curCSV.getString(6), curCSV.getString(7),
                        curCSV.getString(8), curCSV.getString(9),
                        curCSV.getString(10), curCSV.getString(11),
                        curCSV.getString(12), curCSV.getString(13),
                        curCSV.getString(14), curCSV.getString(15),
                        curCSV.getString(16), curCSV.getString(17),
                        curCSV.getString(18), curCSV.getString(19),
                        curCSV.getString(20), curCSV.getString(21),
                        curCSV.getString(22), curCSV.getString(23)

                };
                //Log.d(TAG, "notes " + curCSV.getString(5));
                csvWrite.writeRecord(arrStr);
                curCSV.moveToNext();

            }
            csvWrite.close();
            curCSV.close();
        }
        catch (IOException e)   {
            Log.e("Export has failed... ", e.getMessage(), e);
        }

        Toast.makeText(getBaseContext(), "Results are exported to: " + file.getAbsolutePath(),
                Toast.LENGTH_LONG).show();
        db.close();
        return 1;
    }

	public void showTopRes() {
		Intent intent = new Intent(getApplicationContext(), TopResults.class);
		intent.putExtra(MainActivity.INT_PATH, intPath);
		// intent.putExtra(CURR_RES_EXIST, isWritten);
		startActivity(intent);
	}

	public void showTopFsRes() {
		Intent intent = new Intent(getApplicationContext(), ShowTopFsRes.class);
		intent.putExtra(MainActivity.INT_PATH, intPath);
		// intent.putExtra(CURR_RES_EXIST, isWritten);
		startActivity(intent);
	}

	public void showSummary() {
		Intent intent = new Intent(getApplicationContext(), SummaryView.class);
		intent.putExtra(MainActivity.INT_PATH, intPath);
		// intent.putExtra(CURR_RES_EXIST, isWritten);
		startActivity(intent);
	}

	public boolean checkPath() {
        new File(sdPath).mkdirs();
		File f = new File(sdPath);
		if (f.exists() && f.isDirectory()) {
			return true;
		} else {
			return false;
		}
	}

	@SuppressWarnings("deprecation")
	public void getStoragesDetails() {

		StatFs stat = new StatFs(sdPath);
		long blockSize;
		long totalBlocks;
		long freeBlocks;
		int currentApiVersion = android.os.Build.VERSION.SDK_INT;
		if (currentApiVersion < 19) {
			blockSize = stat.getFreeBlocks();
			totalBlocks = stat.getBlockCount();
			freeBlocks = stat.getFreeBlocks();
		} else {
			blockSize = stat.getBlockSizeLong();
			totalBlocks = stat.getBlockCountLong();
			freeBlocks = stat.getFreeBlocksLong();
		}
		totalSpace = totalBlocks * blockSize / (1024.0 * 1024 * 1024);
		freeSpace = freeBlocks * blockSize / (1024.0 * 1024 * 1024);
        if (sdPath.equals(intPath) || userdata_selected || custom_drive_selected) {
            getEmmcSize();
            devSize = eMmcSize;
        } else {
            devSize = roundUp2((long) totalSpace);
            if (devSize == 0) {
                devSizeM = roundUp2((long) totalBlocks * blockSize / (1024 * 1024));
            }
        }
		String textShow = "Total user space: " + String.format("%.2f", totalSpace)
				+ " GB\nFree user space: " + String.format("%.2f", freeSpace)
				+ " GB\n";
        TextView dev_size = (TextView) findViewById(R.id.dev_size);
		TextView total_space = (TextView) findViewById(R.id.total_space);
        TextView free_space = (TextView) findViewById(R.id.free_space);
		EditText details = (EditText) findViewById(R.id.details);
		EditText nickText = (EditText) findViewById(R.id.nickname);
        TextView dev_size1 = (TextView) findViewById(R.id.dev_size1);
        TextView total_space1 = (TextView) findViewById(R.id.total_space1);
        TextView free_space1 = (TextView) findViewById(R.id.free_space1);
		EditText details1 = (EditText) findViewById(R.id.details1);
		EditText nickText1 = (EditText) findViewById(R.id.nickname1);
		ViewFlipper viewToShow = (ViewFlipper) findViewById(R.id.viewFlipper);
		String line;

		// ////////get mount output///////////////////
		String mount_out = "";

		// ////////read usb drive parameters
		if (usb_drive_selected) {
			MainActivity.calling_activity = "BenchUsb";
			fs_type = MainActivity.usbFsType;

			UsbManager usbManager = (UsbManager) getSystemService(USB_SERVICE);
			HashMap<String, UsbDevice> devicelist = usbManager.getDeviceList();

			Iterator<UsbDevice> deviceIterator = devicelist.values().iterator();
			// TODO if (devicelist.size() > 1)
			while (deviceIterator.hasNext()) {
				UsbDevice usbDevice = deviceIterator.next();
				// Find usb drive manufacturer name in table
				String[] manfs = getResources().getStringArray(
						R.array.usb_drives);
				String[][] manf_names = new String[manfs.length][2];
				for (int i = 0; i < manfs.length; i++) {
					manf_names[i] = manfs[i].split("\\|");
				}
				for (int j = 0; j < manfs.length; j++) {
					if (manf_names[j][1].substring(2).contains(
							Integer.toHexString(usbDevice.getVendorId()))) {
						full_details = manf_names[j][0];
						break;
					}
				}
				if (full_details == "") {
					full_details = "Unknown USB Drive. VendorID: 0x"
							+ Integer.toHexString(usbDevice.getVendorId());
				}
				// ////////////////////////////////////
			}
			// ///////////////////////////////
		} else {
			if (sdPath.equals(intPath) || userdata_selected
					|| custom_drive_selected) {
				serial = Build.SERIAL;
				MainActivity.calling_activity = "BenchInt";
				// /////////parse mount////////////////
				// for custom case//////////
				if (custom_drive_selected) {
					if (MainActivity.customFsType.isEmpty()) {
						mount_out = MainActivity.getMountOutput();
						String rootDirPattern = "(/\\w+)/*.*";
						String customPath = sdPath;
						String rootCustom = "";
						if (customPath.matches(rootDirPattern)) {
							rootCustom = customPath.replaceAll(rootDirPattern,
									"$1");
							// Log.d("SDPlay", "root for custom path "
							// + rootCustom);
						}
						File cust_r = new File(rootCustom);
						String custFsTypePattern = "";
						String startPattern1 = "/storage/emulated.*";
						String startPattern2 = "/data.*";
						String startPattern3 = "/storage/sdcard1.*";
						if (customPath.matches(startPattern1)) {
							fs_type = MainActivity.intFsType;
						} else if (customPath.matches(startPattern2)) {
							fs_type = MainActivity.userdataFsType;
						} else if (customPath.matches(startPattern3)) {
							fs_type = MainActivity.extFsType;
						} else {
							try {
								if (isSymlink(cust_r)) {
									// Log.d("SDPlay",
									// "custom path is symlink - true");
									// TODO replace rootCustom with symlink
									// target
									custFsTypePattern = ".*"
											+ cust_r.getCanonicalPath()
											+ "\\s+(\\w*).*";
								} else {
									// Log.d("SDPlay",
									// "custom path is symlink - false");
									custFsTypePattern = ".*" + rootCustom
											+ "\\s+(\\w*).*";
								}
								if (mount_out.matches(custFsTypePattern)) {
									fs_type = mount_out.replaceAll(
											custFsTypePattern, "$1");
									// Log.d("SDPlay", "custom fs type is "
									// + fs_type);
								}
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
					} else {
						fs_type = MainActivity.customFsType;
					}
				} else {
					// for internal case
					if (userdata_selected) {
						fs_type = MainActivity.userdataFsType;
					} else {
						fs_type = MainActivity.intFsType;
					}
					// pattern0 = ".*/storage/emulated/\\w+\\s+(\\w+)\\s+.*";
					// pattern1 = ".*/userdata\\s+/\\w*\\s+(\\w+).*";
					// if (sdPath.contains("storage/emulated") &&
					// mount_out.matches(pattern0) && !userdata_selected) {
					// fs_type = mount_out.replaceAll(pattern0, "$1") + "/";
					// }
					// if (mount_out.matches(pattern1)) {
					// fs_type += mount_out.replaceAll(pattern1, "$1");
					// }
				}
				// //////end of parse mount/////////////
			} else {
				MainActivity.calling_activity = "BenchExt";
				fs_type = MainActivity.extFsType;
				// ////////parse mount///////////////////
				// for sdcard case
				// pattern0 = ".*/storage/sdcard1\\s+(\\w+)\\s+.*";
				// pattern1 = ".*/mnt/media_rw/sdcard1\\s+(\\w+).*";
				// if (sdPath.contains("storage/sdcard1") &&
				// mount_out.matches(pattern0)) {
				// fs_type = mount_out.replaceAll(pattern0, "$1") + "/";
				// }
				// if (mount_out.matches(pattern1)) {
				// fs_type += mount_out.replaceAll(pattern1, "$1");
				// }
				// /////end of parse mount///////////////
				try {
					File fff = new File("/sys/block/mmcblk1/device/serial");
					if (fff.exists()) {
						BufferedReader in_serial = new BufferedReader(
								new FileReader(
										"/sys/block/mmcblk1/device/serial"));
						while ((line = in_serial.readLine()) != null) {
							serial = line;
						}
						in_serial.close();
					}
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			// try to search serial in ResDB
			// if found -> skip reading params
			// display details for that serial from ResDB
			MyResDBHelper myResDB = new MyResDBHelper(getBaseContext(), intPath);
			SQLiteDatabase res_db = myResDB.getReadableDatabase();

			// List<String> recs = new ArrayList<String>();
			Cursor c;
			// Define a projection that specifies which columns from the
			// database
			// you will actually use after this query.
			String[] projection = { myResDB.RES_DETAILS, myResDB.RES_NOTES };
			String whereClause = myResDB.RES_SERIAL + " = ? ";
			String[] whereArgs = new String[] { serial };
			String sortOrder = myResDB.RES_ID + " DESC";

			res_db.beginTransaction();
			c = res_db.query(myResDB.RES_TABLE, // The table to query
					projection, // The columns to return
					whereClause, // The columns for the WHERE clause (null
					// means getting ALL records here)
					whereArgs, // The values for the WHERE args clause
					null, // don't group the rows
					null, // don't filter by row groups
					sortOrder // sort order
					);

			// Record all selected rows into array
			if (c.getCount() > 0) {
				c.moveToFirst();
				full_details = c.getString(0);
				// nickname = c.getString(1);
				c.close();
			}
			res_db.setTransactionSuccessful();
			res_db.endTransaction();
			// res_db.close();

			res_db.close();
		}

		if (!(full_details.isEmpty()) && !(full_details.contains("Unknown"))) {
			if (viewToShow.getDisplayedChild() == 0) {
				if (devSize == 0) {
                    dev_size.setText(devSizeM + " MB");
                } else {
                    dev_size.setText(devSize + " GB");
                }
                total_space.setText(String.format("%.2f", totalSpace) + " GB");
                free_space.setText(String.format("%.2f", freeSpace) + " GB");
				details.setText(full_details);
				// nickText.setText(nickname);
			} else {
                if (devSize == 0) {
                    dev_size1.setText(devSizeM + " MB");
                } else {
                    dev_size1.setText(devSize + " GB");
                }
                total_space1.setText(String.format("%.2f", totalSpace) + " GB");
                free_space1.setText(String.format("%.2f", freeSpace) + " GB");
				details1.setText(full_details);
				// nickText1.setText(nickname);
			}
		} else {

			if (sdPath.equals(intPath) || userdata_selected
					|| custom_drive_selected) {
				// Read internal parameters if internal memory is selected for
				// test
				try {
					File iff = new File("/sys/block/mmcblk0/device/manfid");
					if (iff.exists()) {
						BufferedReader in_manfid = new BufferedReader(
								new FileReader(
										"/sys/block/mmcblk0/device/manfid"));
						while ((line = in_manfid.readLine()) != null) {
							manfid = line;
						}
						BufferedReader in_oemid = new BufferedReader(
								new FileReader(
										"/sys/block/mmcblk0/device/oemid"));
						while ((line = in_oemid.readLine()) != null) {
							oemid = line;
						}
						BufferedReader in_name = new BufferedReader(
								new FileReader("/sys/block/mmcblk0/device/name"));
						while ((line = in_name.readLine()) != null) {
							name = line;
						}
						in_manfid.close();
						in_oemid.close();
						in_name.close();
					}
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				// Find memory manufacturer name in table
				String[] manfs = getResources().getStringArray(
						R.array.internal_memories);
				String[][] manf_names = new String[manfs.length][2];
				for (int i = 0; i < manfs.length; i++) {
					manf_names[i] = manfs[i].split("\\|");
				}
				for (int j = 0; j < manfs.length; j++) {
					if (manfid.substring(2).contains(
							manf_names[j][1].substring(2))) {
						full_details += manf_names[j][0];
						break;
					}
				}
				if (full_details == "" || full_details.contains("Unknown")) {
					full_details = "Unknown Vendor " + name;
				} else {
					full_details += " " + name;
				}
			} else {
				// Read card parameters if SD card is selected for test
				String oemid_hex = "";
				try {
					File fff = new File("/sys/block/mmcblk1/device/oemid");
					if (fff.exists()) {// && !(sdPath.equals(intPath))) {
						BufferedReader in_oemid = new BufferedReader(
								new FileReader(
										"/sys/block/mmcblk1/device/oemid"));
						while ((line = in_oemid.readLine()) != null) {
							oemid_hex = line.substring(2);
						}
						// parse oemid

						if (oemid_hex != "") {
							StringBuilder parsed_oemid = new StringBuilder();
							for (int j = 0; j < oemid_hex.length(); j += 2) {
								parsed_oemid.append((char) Integer.parseInt(
										oemid_hex.substring(j, j + 2), 16));
							}
							oemid = parsed_oemid.toString();
						}

						// /////////////////////

						BufferedReader in_manfid = new BufferedReader(
								new FileReader(
										"/sys/block/mmcblk1/device/manfid"));
						while ((line = in_manfid.readLine()) != null) {
							manfid = line;
						}
						BufferedReader in_name = new BufferedReader(
								new FileReader("/sys/block/mmcblk1/device/name"));
						while ((line = in_name.readLine()) != null) {
							name = line;
						}
						in_manfid.close();
						in_oemid.close();
						in_name.close();
					}
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				// /disable read oper_mode//////////
				/*
				 * try { File dbgfs = new File("/sys/kernel/debug/mmc1/ios"); if
				 * (dbgfs.exists()) {// && !(sdPath.equals(intPath))) { // do
				 * smth on SD card here BufferedReader in_oper_mode = new
				 * BufferedReader( new
				 * FileReader("/sys/kernel/debug/mmc1/ios")); while ((line =
				 * in_oper_mode.readLine()) != null) { card_oper_mode = line; }
				 * in_oper_mode.close(); // TODO: timing spec from oper_mode } }
				 * catch (IOException e) { e.printStackTrace(); }
				 */
				// end of disable read oper_mode///////////////
				//
				// Find card manufacturer name in table

				String[] card_manfs = getResources().getStringArray(
						R.array.known_sdcards);
				// card_full_details = Integer.toString(card_manfs.length);
				String[][] card_manf_names = new String[card_manfs.length][4];
				for (int i = 0; i < card_manfs.length; i++) {
					card_manf_names[i] = card_manfs[i].split("\\|");
				}

				for (int j = 0; j < card_manfs.length; j++) {
                    if ((oemid.equals(card_manf_names[j][1]))
                            && (manfid.substring(2)
                            .contains(card_manf_names[j][2]
                                    .substring(2)))
                            && (name.equals(card_manf_names[j][3]))) {
                        full_details = card_manf_names[j][0];
                        break;
                    }
                }

				if (full_details == "") {
					full_details = "Uknown SD card";
				}
			}
			if (viewToShow.getDisplayedChild() == 0) {
                if (devSize == 0) {
                    dev_size.setText(devSizeM + " MB");
                } else {
                    dev_size.setText(devSize + " GB");
                }
                total_space.setText(String.format("%.2f", totalSpace) + " GB");
                free_space.setText(String.format("%.2f", freeSpace) + " GB");
				details.setText(full_details);
				// nickText.setText(nickname);
			} else {
                if (devSize == 0) {
                    dev_size1.setText(devSizeM + " MB");
                } else {
                    dev_size1.setText(devSize + " GB");
                }
                total_space1.setText(String.format("%.2f", totalSpace) + " GB");
                free_space1.setText(String.format("%.2f", freeSpace) + " GB");
				details1.setText(full_details);
				// nickText1.setText(nickname);
			}
		}
	}

	public void onBackClick(View view) {
		super.onBackPressed();
	}

	public void onSettingsClick(View view) {
		Intent intent = new Intent(this, SetPreferences.class);
		startActivity(intent);
	}

	public void onPause() {
		this.unregisterReceiver(listener);
		super.onPause();
	}

    public void onStop() {
        if (LOG_ON) {
            Log.d(TAG, "onStop() is called");
        }
        if ((ALL && isWritten && isFsDone) || (!ALL && isWritten)
                || (!ALL && isFsDone)
                ) {
            if (!sdPath.equals(intPath) && !userdata_selected) {
                if(LOG_ON) {
                    Log.d(TAG, "Trying to delete...; ALL = " + ALL + "; isWritten = " + isWritten + "; isFsDone = " + isFsDone);
                }
                cleanAppDirsOnExternalStorage();
            }
            ALL = false;
        }
        super.onStop();
    }

	// ////////////////FS Test part/////////////////////////////////////////////
	public void onFsTestClick(View view) {
		if (checkPath()) {

            ///////if only fs is running, erase previous DB test values///////
            if (!ALL) {
                isWritten = false;
                ws = 0;
                rs = 0;
                rndws = 0;
                rndrs = 0;
            }
            ///////////////
			isFsDone = false;

			create_fs = 0;
			list_fs = 0;
			medium_write_rate = 0;
			medium_read_rate = 0;
			large_write_rate = 0;
			large_read_rate = 0;
			del_f = 0;
			f_count = 0;
			small_f_count = 0;
			medium_f_count = 0;
			large_f_count = 0;
			random = new Random();
			if (new File(sdPath + File.separator + "fstest_folders").mkdirs()) {
				Log.d("SDPlay", "path for fs test is ok");
			}
            /*
            if (new File(sdPath + File.separator + "fs_iops_folder").mkdirs()) {
                Log.d("SDPlay", "path for iops test is ok");
            }
            */
			if (LOG_ON) {
				Log.d(TAG, "Fs testing is started...");
			}
			// new File(sdPath + File.separator + "fsiops_folders").mkdirs();
			// bytesToWrite = new byte[1024];

			// random.nextBytes(bytesToWrite);
			// Now we have 1024 random bytes in bytesToWrite array

			if (!ALL) {
				EditText nickName = (EditText) findViewById(R.id.nickname);
				EditText details = (EditText) findViewById(R.id.details);
				nickname = nickName.getText().toString();
				full_details = details.getText().toString();
				tmp = "";
				textView.setText(tmp);
				textView11.setText(tmp);
			} else {
				// isWritten = true;
			}
			final CreateFS createFs = new CreateFS(this);
			// createFs.cancel(false);
			createFs.executeOnExecutor(executor, 100, 0, 1);
			mProgressDialog
					.setOnCancelListener(new DialogInterface.OnCancelListener() {
						@Override
						public void onCancel(DialogInterface dialog) {

							createFs.cancel(true);
						}
					});
		} else {
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setMessage(
					"Selected storage seems to be not available anymore :(\n"
							+ "Please change media for test.").setTitle(
					"Oops...");

			builder.setPositiveButton("OK",
					new DialogInterface.OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							Intent intent = new Intent(getApplicationContext(),
									MainActivity.class);
							startActivity(intent);
						}
					});

			AlertDialog dialog = builder.create();
			dialog.show();
		}

	}

	// ////////// progress bar for create fs ///////////////////
	private class CreateFS extends AsyncTask<Integer, Integer, Integer> {

		private Context context;
		private PowerManager.WakeLock mWakeLockCrFS;

		public CreateFS(Context context) {
			this.context = context;
		}

		protected Integer doInBackground(Integer... params) {
			// TODO clean fs before test
			// get timestamp
			publishProgress(0);

			// Log.d("SDPlay", "start iops write");
			writeRndAccessFile();

			random.nextBytes(bytesToWrite);

			millis1 = SystemClock.elapsedRealtime();
			// Log.d("SDPlay", "starting write");

			for (int j = 1; j <= 10; j++) {
				if (new File(sdPath + File.separator + "fstest_folders"
						+ File.separator + String.valueOf(j)).mkdirs()) {
					// Log.d("SDPlay", "inside");

					for (int k = 1; k <= 10; k++) {
						if (isCancelled()) {
							mWakeLockCrFS.release();
                         //   wasCancelled = true;
							Log.d("SDPLay", "Dialog cancelled");
							return 0;
						} else {
							if (new File(sdPath + File.separator
									+ "fstest_folders" + File.separator
									+ String.valueOf(j) + File.separator
									+ String.valueOf(k)).mkdirs()) {
								for (int l = 0; l < 100; l++) {
									// Log.d("SDPlay", "deep inside");
									String path = sdPath + File.separator
											+ "fstest_folders" + File.separator
											+ String.valueOf(j)
											+ File.separator
											+ String.valueOf(k)
											+ File.separator + "small_f"
											+ String.valueOf(l) + ".txt";
									File newSparseFile = null;
									try {
										new File(path).delete();
										newSparseFile = new File(path);
									} catch (final Exception e) {
										Log.d("SDPlay",
												"error while creating file:"
														+ e);
									} finally {
										if (newSparseFile != null) {
											// try {
											small_f_count++;
											// Use the utility static class
											// BinaryFileUtils
											BinaryFileUtils helper = new BinaryFileUtils();
											helper.write(bytesToWrite,
													newSparseFile);
										}
									}
									publishProgress((j * 1000 + k * 100 + l)
											* mProgressDialog.getMax() / 10000);
								}
							}
						}
					}
				}
			}
			millis2 = SystemClock.elapsedRealtime();
			// Log.d("SDPlay", "finish write");
			// writing time in ms
			diff_create = millis2 - millis1;
			return 1;
			// When finished, return the resulting 1, this will cause the
			// Activity to call onPostExecute()
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			// take CPU lock to prevent CPU from going off if the user
			// presses the power button during download
			cleanFs();
			PowerManager pm = (PowerManager) getApplicationContext()
					.getSystemService(Context.POWER_SERVICE);
			mWakeLockCrFS = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
					getClass().getName());
			mWakeLockCrFS.acquire();
			mProgressDialog.setIndeterminate(false);
			mProgressDialog.setMax(10000);
			mProgressDialog.setMessage("FS creation is in progress...");
			mProgressDialog.show();

		}

		@Override
		protected void onProgressUpdate(Integer... progress) {
			super.onProgressUpdate(progress);
			mProgressDialog.setProgress(progress[0]);

		}

		@Override
		protected void onPostExecute(Integer result) {
			mWakeLockCrFS.release();
			mProgressDialog.dismiss();
			// Log.d("SDPlay", "dialog dismissed");
			if (result != 1) {
				Toast.makeText(getBaseContext(), "FS creation error",
						Toast.LENGTH_LONG).show();
			} else {
				create_fs = small_f_count * 1000.0 / diff_create;
				tmp += "\nWrite speed " + String.format("%.2f", create_fs)
						+ " files/s\n";
				Toast.makeText(getBaseContext(),
						"FS has been created successfully", Toast.LENGTH_SHORT)
						.show();
			}
			if (ALL) {
				// onReadAllClick(findViewById(android.R.id.content).getRootView());
				// TODO
				textView11.setText(tmp);
			} else {
				textView.setText(tmp);
			}
			doListFs(findViewById(android.R.id.content).getRootView());
		}
	}

	// end of create fs progress
	// bar/////////////////////////////////////////////////

	public void doListFs(View view) {
		list_d = new ArrayList<String>();
		list_f = new ArrayList<String>();
		final ListFS listFs = new ListFS(this);
		listFs.executeOnExecutor(executor, 100, 0, 1);
		f_count = 0;
		d_count = 0;

		mProgressDialog
				.setOnCancelListener(new DialogInterface.OnCancelListener() {
					@Override
					public void onCancel(DialogInterface dialog) {

						listFs.cancel(true);
					}
				});
	}

	// ////////// progress bar for list fs ///////////////////
	private class ListFS extends AsyncTask<Integer, Integer, Integer> {

		private Context context;
		private PowerManager.WakeLock mWakeLockLFS;

		public ListFS(Context context) {
			this.context = context;
		}

		protected Integer doInBackground(Integer... params) {
			// get timestamp
			millis3 = SystemClock.elapsedRealtime();

			try {
				File fstest = new File(sdPath + File.separator
						+ "fstest_folders");
				if (fstest.exists() && fstest.isDirectory()) {
					// Log.d("SDPlay", "step 1");
					f_count = walk(fstest.getAbsolutePath(), 1);
					publishProgress(0);
				}
			} catch (Exception e) {
				Log.d("SDPlay", "exception " + e);
			}
			millis4 = SystemClock.elapsedRealtime();
			// writing time in ms
			diff_list = millis4 - millis3;
			publishProgress(100);
			return 1;
			// When finished, return the resulting 1, this will cause the
			// Activity to call onPostExecute()
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			// take CPU lock to prevent CPU from going off if the user
			// presses the power button during download
			PowerManager pm = (PowerManager) getApplicationContext()
					.getSystemService(Context.POWER_SERVICE);
			mWakeLockLFS = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
					getClass().getName());
			mWakeLockLFS.acquire();

			mProgressDialog.setIndeterminate(true);
			mProgressDialog.setMax(1);
			mProgressDialog.setMessage("Building file list is in progress...");
			mProgressDialog.show();
		}

		@Override
		protected void onProgressUpdate(Integer... progress) {
			super.onProgressUpdate(progress);
			if (progress[0] != 100) {
				mProgressDialog.setIndeterminate(true);
			} else {
				mProgressDialog.setIndeterminate(false);
				mProgressDialog.setProgress(progress[0]);
			}
		}

		@Override
		protected void onPostExecute(Integer result) {
			mWakeLockLFS.release();
			mProgressDialog.dismiss();
			if (result != 1) {
				Toast.makeText(getBaseContext(), "FS list building error",
						Toast.LENGTH_LONG).show();
			} else {
				list_fs = f_count * 1000.0 / diff_list;
				tmp += "List building speed " + String.format("%.2f", list_fs)
						+ " files/s\n";
				Toast.makeText(getBaseContext(),
						"Files are listed successfully ", Toast.LENGTH_SHORT)
						.show();
			}
			if (ALL) {
				// onReadAllClick(findViewById(android.R.id.content).getRootView());
				// TODO
				textView11.setText(tmp);
			} else {
				textView.setText(tmp);
			}
			writeMediumFiles(findViewById(android.R.id.content).getRootView());
			// readSmallFiles(findViewById(android.R.id.content).getRootView());
			// cleanFs();
		}
	}

	// end of list fs progress
	// bar/////////////////////////////////////////////////

	// ////////////read all small files////////////////////////
	public void readSmallFiles(View view) {
		// cleanFs();
		// list_d = new ArrayList<String>();
		// list_f = new ArrayList<String>();
		final ReadAllSmallFiles readSmallFiles = new ReadAllSmallFiles(this);
		readSmallFiles.executeOnExecutor(executor, 100, 0, 1);
		small_f_count = 0;
		// d_count = 0;

		mProgressDialog
				.setOnCancelListener(new DialogInterface.OnCancelListener() {
					@Override
					public void onCancel(DialogInterface dialog) {

						readSmallFiles.cancel(true);
					}
				});
	}

	// ////////// progress bar for read all small files ///////////////////
	private class ReadAllSmallFiles extends
			AsyncTask<Integer, Integer, Integer> {

		private Context context;
		private PowerManager.WakeLock mWakeLockLFS;

		public ReadAllSmallFiles(Context context) {
			this.context = context;
		}

		protected Integer doInBackground(Integer... params) {

			millis5 = SystemClock.elapsedRealtime();
			int progress = 0;
			try {
				for (String path : list_f) {
					File fileToRead = new File(path);
					BinaryFileUtils helper = new BinaryFileUtils();
					// byte[] data = new byte[1024];
					byte[] data = null;
					data = helper.read(fileToRead);
					if (data.length > 0) {
						progress++;
						small_f_count++;
						publishProgress(progress * mProgressDialog.getMax()
								/ 10000);
					}
				}
			} catch (Exception e) {
				Log.d("SDPlay", "exception " + e);
			}
			millis6 = SystemClock.elapsedRealtime();
			// writing time in ms
			diff_small_read = millis6 - millis5;
			// Log.d("SDPlay", "finish reading files");
			// f_count = list_f.size();

			return 1;
			// When finished, return the resulting 1, this will cause the
			// Activity to call onPostExecute()
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			// take CPU lock to prevent CPU from going off if the user
			// presses the power button during download
			PowerManager pm = (PowerManager) getApplicationContext()
					.getSystemService(Context.POWER_SERVICE);
			mWakeLockLFS = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
					getClass().getName());
			mWakeLockLFS.acquire();

			mProgressDialog.setIndeterminate(false);
			mProgressDialog.setMax(10000);
			mProgressDialog.setMessage("Small files reading is in progress...");
			mProgressDialog.show();
		}

		@Override
		protected void onProgressUpdate(Integer... progress) {
			super.onProgressUpdate(progress);
			mProgressDialog.setProgress(progress[0]);

		}

		@Override
		protected void onPostExecute(Integer result) {
			mWakeLockLFS.release();
			mProgressDialog.dismiss();
			if (result != 1) {
				Toast.makeText(getBaseContext(), "File reading error",
						Toast.LENGTH_LONG).show();
			} else {
				// tmp += "\nTotal files read: " + f_count + "\n";
				small_read_rate = small_f_count * 1000.0 / diff_small_read;
				tmp += "Small files reading speed "
						+ String.format("%.2f", small_read_rate) + " files/s\n";
				Toast.makeText(getBaseContext(),
						"Files are read successfully ", Toast.LENGTH_SHORT)
						.show();
			}
			if (ALL) {
				// onReadAllClick(findViewById(android.R.id.content).getRootView());
				// TODO
				textView11.setText(tmp);
			} else {
				textView.setText(tmp);
			}
			// cleanFs();
			readMediumFiles(findViewById(android.R.id.content).getRootView());
			// writeMediumFiles(findViewById(android.R.id.content).getRootView());
		}
	}

	// end of list all small progress
	// bar////////////////////////////////////////////

	// //////////////write medium files/////////////////////////
	public void writeMediumFiles(View view) {
		medium_f_count = 0;
		random = new Random();

		final WriteMediumFiles writeMediumFiles = new WriteMediumFiles(this);
		writeMediumFiles.executeOnExecutor(executor, 100, 0, 1);
		f_count = 0;
		// d_count = 0;

		mProgressDialog
				.setOnCancelListener(new DialogInterface.OnCancelListener() {
					@Override
					public void onCancel(DialogInterface dialog) {

						writeMediumFiles.cancel(true);
					}
				});
	}

	private class WriteMediumFiles extends AsyncTask<Integer, Integer, Integer> {

		private Context context;
		private PowerManager.WakeLock mWakeLockCrFS;

		public WriteMediumFiles(Context context) {
			this.context = context;
		}

		protected Integer doInBackground(Integer... params) {
			// get timestamp
			millis7 = SystemClock.elapsedRealtime();
			publishProgress(0);
			random.nextBytes(bytesToWriteMedium);
			for (int j = 1; j <= 10; j++) {
				File dir_lvl1_path = new File(sdPath + File.separator
						+ "fstest_folders" + File.separator + String.valueOf(j));
				if (dir_lvl1_path.exists() && dir_lvl1_path.isDirectory()) {
					if (isCancelled()) {
						mWakeLockCrFS.release();
                     //   wasCancelled = true;
						return 0;
					} else {
						for (int k = 1; k <= 5; k++) {
							String path = dir_lvl1_path + File.separator
									+ "medium_f" + String.valueOf(j) + "_"
									+ String.valueOf(k) + ".txt";
							File newMediumFile = null;
							try {
								new File(path).delete();
								newMediumFile = new File(path);
							} catch (final Exception e) {
								Log.d("SDPlay", "error while creating file:"
										+ e);
							} finally {
								if (newMediumFile != null) {
									BinaryFileUtils helper = new BinaryFileUtils();
									helper.write(bytesToWriteMedium,
											newMediumFile);
									medium_f_count++;
								}
							}
							publishProgress((j * 5 + k)
									* mProgressDialog.getMax() / 50);
						}

					}

				}
			}

			millis8 = SystemClock.elapsedRealtime();
			// writing time in ms
			diff_medium_write = millis8 - millis7;
			return 1;
			// When finished, return the resulting 1, this will cause the
			// Activity to call onPostExecute()
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			// take CPU lock to prevent CPU from going off if the user
			// presses the power button during download
			PowerManager pm = (PowerManager) getApplicationContext()
					.getSystemService(Context.POWER_SERVICE);
			mWakeLockCrFS = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
					getClass().getName());
			mWakeLockCrFS.acquire();

			mProgressDialog.setIndeterminate(false);
			mProgressDialog.setMax(50);
			mProgressDialog.setMessage(MEDIUM_SIZE
					+ "MB files writing is in progress...");
			mProgressDialog.show();
		}

		@Override
		protected void onProgressUpdate(Integer... progress) {
			super.onProgressUpdate(progress);
			mProgressDialog.setProgress(progress[0]);
		}

		@Override
		protected void onPostExecute(Integer result) {
			mWakeLockCrFS.release();
			mProgressDialog.dismiss();
			// Log.d("SDPlay", "dialog dismissed");
			if (result != 1) {
				Toast.makeText(getBaseContext(), "File writing error",
						Toast.LENGTH_LONG).show();
			} else {
				// tmp += "\nTotal files written:  " + f_count + "\n";
				medium_write_rate = medium_f_count * MEDIUM_SIZE * 1000.0
						/ diff_medium_write;
				tmp += MEDIUM_SIZE + "MB files writing speed "
						+ String.format("%.2f", medium_write_rate) + " MB/s\n";
				Toast.makeText(getBaseContext(),
						MEDIUM_SIZE + "MB files written successfully",
						Toast.LENGTH_SHORT).show();
			}
			if (ALL) {
				// onReadAllClick(findViewById(android.R.id.content).getRootView());
				// TODO
				textView11.setText(tmp);
			} else {
				textView.setText(tmp);
			}
			writeLargeFiles(findViewById(android.R.id.content).getRootView());
			// readMediumFiles(findViewById(android.R.id.content).getRootView());
		}
	}

	// /////////end of write medium files

	// ////////////read all medium files////////////////////////
	public void readMediumFiles(View view) {
		// cleanFs();
		list_d = new ArrayList<String>();
		list_f = new ArrayList<String>();
		final ReadAllMediumFiles readMediumFiles = new ReadAllMediumFiles(this);
		readMediumFiles.executeOnExecutor(executor, 100, 0, 1);
		medium_f_count = 0;
		f_count = 0;

		mProgressDialog
				.setOnCancelListener(new DialogInterface.OnCancelListener() {
					@Override
					public void onCancel(DialogInterface dialog) {

						readMediumFiles.cancel(true);
					}
				});
	}

	// ////////// progress bar for read all small files ///////////////////
	private class ReadAllMediumFiles extends
			AsyncTask<Integer, Integer, Integer> {

		private Context context;
		private PowerManager.WakeLock mWakeLockLFS;

		public ReadAllMediumFiles(Context context) {
			this.context = context;
		}

		// ArrayList<String> list = new ArrayList<String>();

		protected Integer doInBackground(Integer... params) {
			// get timestamp
			publishProgress(0);
			try {
				File fstest = new File(sdPath + File.separator
						+ "fstest_folders");
				if (fstest.exists() && fstest.isDirectory()) {
					// Log.d("SDPlay", "step 1");
					f_count = walk(fstest.getAbsolutePath(), 3);
				}
			} catch (Exception e) {
				Log.d("SDPlay", "exception " + e);
			}
			millis1 = SystemClock.elapsedRealtime();
			int progress = 0;

			try {
				// publishProgress(0);
				for (String path : list_f) {
					File fileToRead = new File(path);
					// Log.d("SDPlay", "read file: " + path);
					BinaryFileUtils helper = new BinaryFileUtils();
					byte[] data = null;
					data = helper.read(fileToRead);
					// Log.d("SDPlay", "length of read file: " + data.length);
					if (data.length > 0) {
						progress++;
						medium_f_count++;
						publishProgress(progress * mProgressDialog.getMax()
								/ 50);
					}
				}
			} catch (Exception e) {
				Log.d("SDPlay", "exception " + e);
			}
			millis2 = SystemClock.elapsedRealtime();
			// writing time in ms
			diff_medium_read = millis2 - millis1;
			// Log.d("SDPlay", "finish reading files");
			// f_count = list_f.size();

			return 1;
			// When finished, return the resulting 1, this will cause the
			// Activity to call onPostExecute()
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			// take CPU lock to prevent CPU from going off if the user
			// presses the power button during download
			PowerManager pm = (PowerManager) getApplicationContext()
					.getSystemService(Context.POWER_SERVICE);
			mWakeLockLFS = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
					getClass().getName());
			mWakeLockLFS.acquire();

			mProgressDialog.setIndeterminate(false);
			mProgressDialog.setMax(50);
			mProgressDialog.setMessage(MEDIUM_SIZE
					+ "MB files reading is in progress...");
			mProgressDialog.show();
		}

		@Override
		protected void onProgressUpdate(Integer... progress) {
			super.onProgressUpdate(progress);
			mProgressDialog.setProgress(progress[0]);

		}

		@Override
		protected void onPostExecute(Integer result) {
			mWakeLockLFS.release();
			mProgressDialog.dismiss();
			if (result != 1) {
				Toast.makeText(getBaseContext(), "File reading error",
						Toast.LENGTH_LONG).show();
			} else {
				// tmp += "\nTotal files read: " + f_count + "\n";
				medium_read_rate = medium_f_count * MEDIUM_SIZE * 1000.0
						/ diff_medium_read;
				tmp += MEDIUM_SIZE + "MB files reading speed "
						+ String.format("%.2f", medium_read_rate) + " MB/s\n";
				Toast.makeText(getBaseContext(),
						"Files are read successfully ", Toast.LENGTH_SHORT)
						.show();
			}
			if (ALL) {
				// onReadAllClick(findViewById(android.R.id.content).getRootView());
				// TODO
				textView11.setText(tmp);
			} else {
				textView.setText(tmp);
			}
			// cleanFs();
			readLargeFiles(findViewById(android.R.id.content).getRootView());
			// writeLargeFiles(findViewById(android.R.id.content).getRootView());
		}
	}

	// /////end of read all medium files//////////////////

	// //////////////write large files/////////////////////////
	public void writeLargeFiles(View view) {
		large_f_count = 0;
		random = new Random();
		final WriteLargeFiles writeLargeFiles = new WriteLargeFiles(this);
		writeLargeFiles.executeOnExecutor(executor, 100, 0, 1);
		f_count = 0;
		// d_count = 0;

		mProgressDialog
				.setOnCancelListener(new DialogInterface.OnCancelListener() {
					@Override
					public void onCancel(DialogInterface dialog) {

						writeLargeFiles.cancel(true);
					}
				});
	}

	private class WriteLargeFiles extends AsyncTask<Integer, Integer, Integer> {

		private Context context;
		private PowerManager.WakeLock mWakeLockCrFS;

		public WriteLargeFiles(Context context) {
			this.context = context;
		}

		protected Integer doInBackground(Integer... params) {
			// TODO clean fs before test
			// get timestamp
			millis3 = SystemClock.elapsedRealtime();
			publishProgress(0);
			for (int j = 1; j <= 2; j++) {
				File dir_lvl0_path = new File(sdPath + File.separator
						+ "fstest_folders");
				if (dir_lvl0_path.exists() && dir_lvl0_path.isDirectory()) {
					if (isCancelled()) {
						mWakeLockCrFS.release();
                    //    wasCancelled = true;
						return 0;
					} else {
						String path = dir_lvl0_path + File.separator
								+ "large_f" + String.valueOf(j) + ".txt";
						// Log.d("SDPlay", "path for file:" + path);
						File newLargeFile = null;
						try {
							new File(path).delete();
							newLargeFile = new File(path);
						} catch (final Exception e) {
							Log.d("SDPlay", "error while creating file:" + e);
						} finally {
							if (newLargeFile != null) {
								// Use the utility static class
								// BinaryFileUtils
								BinaryFileUtils helper = new BinaryFileUtils();
								helper.writeLarge(bytesToWriteLarge,
										newLargeFile, largeSize);
								if (newLargeFile.length() > 0) {
									large_f_count++;
								}
								// Log.d("SDPlay",
								// "length:" + newLargeFile.length());
							}
						}
					}
					// publishProgress(j * mProgressDialog.getMax() / 2);
				}
			}
			publishProgress(100);
			millis4 = SystemClock.elapsedRealtime();
			// writing time in ms
			diff_large_write = millis4 - millis3;
			return 1;
			// When finished, return the resulting 1, this will cause the
			// Activity to call onPostExecute()
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			// take CPU lock to prevent CPU from going off if the user
			// presses the power button during download
			PowerManager pm = (PowerManager) getApplicationContext()
					.getSystemService(Context.POWER_SERVICE);
			mWakeLockCrFS = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
					getClass().getName());
			mWakeLockCrFS.acquire();

			mProgressDialog.setIndeterminate(true);
			mProgressDialog.setMax(1);
			mProgressDialog.setMessage(largeSizeToPrint
					+ " files writing is in progress...");
			mProgressDialog.show();
		}

		@Override
		protected void onProgressUpdate(Integer... progress) {
			super.onProgressUpdate(progress);
			if (progress[0] != 100) {
				mProgressDialog.setIndeterminate(true);
			} else {
				mProgressDialog.setIndeterminate(false);
				mProgressDialog.setProgress(progress[0]);
			}
		}

		@Override
		protected void onPostExecute(Integer result) {
			mWakeLockCrFS.release();
			mProgressDialog.dismiss();
			// Log.d("SDPlay", "dialog dismissed");
			if (result != 1) {
				Toast.makeText(getBaseContext(), "File writing error",
						Toast.LENGTH_LONG).show();
			} else {
				// tmp += "\nTotal files written:  " + f_count + "\n";
				large_write_rate = large_f_count * LARGE_SIZE * 1000.0
						/ diff_large_write;
				tmp += largeSizeToPrint + " file writing speed "
						+ String.format("%.2f", large_write_rate) + " MB/s\n";
				Toast.makeText(getBaseContext(),
						largeSizeToPrint + " files written successfully",
						Toast.LENGTH_SHORT).show();
			}
			if (ALL) {
				// onReadAllClick(findViewById(android.R.id.content).getRootView());
				// TODO
				textView11.setText(tmp);
			} else {
				textView.setText(tmp);
			}
			runIOPSRead(findViewById(android.R.id.content).getRootView());
			// readSmallFiles(findViewById(android.R.id.content).getRootView());
			// readLargeFiles(findViewById(android.R.id.content).getRootView());
			// cleanFs();
		}
	}

	// /////////end of write large files

	// ////////////read all large files////////////////////////
	public void readLargeFiles(View view) {
		list_d = new ArrayList<String>();
		list_f = new ArrayList<String>();
		final ReadAllLargeFiles readLargeFiles = new ReadAllLargeFiles(this);
		readLargeFiles.executeOnExecutor(executor, 100, 0, 1);
		large_f_count = 0;
		f_count = 0;

		mProgressDialog
				.setOnCancelListener(new DialogInterface.OnCancelListener() {
					@Override
					public void onCancel(DialogInterface dialog) {

						readLargeFiles.cancel(true);
					}
				});
	}

	// ////////// progress bar for read all large files ///////////////////
	private class ReadAllLargeFiles extends
			AsyncTask<Integer, Integer, Integer> {

		private Context context;
		private PowerManager.WakeLock mWakeLockLFS;

		public ReadAllLargeFiles(Context context) {
			this.context = context;
		}

		protected Integer doInBackground(Integer... params) {
			// get timestamp
			publishProgress(0);
			try {
				File fstest = new File(sdPath + File.separator
						+ "fstest_folders");
				if (fstest.exists() && fstest.isDirectory()) {
					// Log.d("SDPlay", "step 1");
					f_count = walk(fstest.getAbsolutePath(), 4);
				}
			} catch (Exception e) {
				Log.d("SDPlay", "exception " + e);
			}
			millis5 = SystemClock.elapsedRealtime();
			// int progress = 0;
			publishProgress(0);
			try {
				for (String path : list_f) {
					File fileToRead = new File(path);
					BinaryFileUtils helper = new BinaryFileUtils();
					int data_length = 0;
					data_length = helper.readLarge(fileToRead);
					// Log.d("SDPlay", "length of read file: " + data_length);
					if (data_length > 0) {
						// progress++;
						large_f_count++;
						// publishProgress(progress * mProgressDialog.getMax() /
						// 2);
					}
				}
			} catch (Exception e) {
				Log.d("SDPlay", "exception " + e);
			}
			publishProgress(100);
			millis6 = SystemClock.elapsedRealtime();
			// writing time in ms
			diff_large_read = millis6 - millis5;
			// Log.d("SDPlay", "finish reading files");
			// f_count = list_f.size();

			return 1;
			// When finished, return the resulting 1, this will cause the
			// Activity to call onPostExecute()
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			// take CPU lock to prevent CPU from going off if the user
			// presses the power button during download
			PowerManager pm = (PowerManager) getApplicationContext()
					.getSystemService(Context.POWER_SERVICE);
			mWakeLockLFS = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
					getClass().getName());
			mWakeLockLFS.acquire();

			mProgressDialog.setIndeterminate(true);
			mProgressDialog.setMax(1);
			mProgressDialog.setMessage(largeSizeToPrint
					+ " files reading is in progress...");
			mProgressDialog.show();
		}

		@Override
		protected void onProgressUpdate(Integer... progress) {
			super.onProgressUpdate(progress);
			if (progress[0] != 100) {
				mProgressDialog.setIndeterminate(true);
			} else {
				mProgressDialog.setIndeterminate(false);
				mProgressDialog.setProgress(progress[0]);
			}

		}

		@Override
		protected void onPostExecute(Integer result) {
			mWakeLockLFS.release();
			mProgressDialog.dismiss();
			if (result != 1) {
				Toast.makeText(getBaseContext(), "File reading error",
						Toast.LENGTH_LONG).show();
			} else {
				// tmp += "\nTotal files read: " + f_count + "\n";
				large_read_rate = large_f_count * LARGE_SIZE * 1000.0
						/ diff_large_read;
				tmp += largeSizeToPrint + " files reading speed "
						+ String.format("%.2f", large_read_rate) + " MB/s\n";
				Toast.makeText(getBaseContext(),
						"Files are read successfully ", Toast.LENGTH_SHORT)
						.show();
			}
			if (ALL) {
				// onReadAllClick(findViewById(android.R.id.content).getRootView());
				// TODO
				textView11.setText(tmp);
			} else {
				textView.setText(tmp);
			}
			// cleanFs();
			// isFsDone = true;
			// deleteAllFiles(findViewById(android.R.id.content).getRootView());
			runIOPS(findViewById(android.R.id.content).getRootView());
		}
	}

	// ////////end of read all large files//////////////////

	// //////////////IOPS test/////////////////////////
	public void runIOPS(View view) {
		oper_count = 0;
		random = new Random();

		final RunIOPSTest runIOPSTest = new RunIOPSTest(this);
		runIOPSTest.executeOnExecutor(executor, 100, 0, 1);

		mProgressDialog
				.setOnCancelListener(new DialogInterface.OnCancelListener() {
					@Override
					public void onCancel(DialogInterface dialog) {

						runIOPSTest.cancel(true);
					}
				});
	}

	private class RunIOPSTest extends AsyncTask<Integer, Integer, Integer> {

		private Context context;
		private PowerManager.WakeLock mWakeLockCrFS;

		public RunIOPSTest(Context context) {
			this.context = context;
		}

		protected Integer doInBackground(Integer... params) {
			// get timestamp
			// millis3 = SystemClock.elapsedRealtime();
			publishProgress(0);

			File dir_lvl0_path = new File(sdPath + File.separator
					+ "fstest_folders");
			if (dir_lvl0_path.exists() && dir_lvl0_path.isDirectory()) {
				if (isCancelled()) {
					mWakeLockCrFS.release();
                //    wasCancelled = true;
					return 0;
				} else {
					String path = dir_lvl0_path + File.separator + "iops_f"
							+ ".txt";

					random.nextBytes(bytesForIops);

					millis3 = SystemClock.elapsedRealtime();
                    oper_count = directIOPSw(path, 0, buffIopsSize);

                    /*
					oper_count = 0;
					int fd = directOpen(path, 0);
					ByteBuffer buff = directAllocate(buffIopsSize);
					buff.put(bytesForIops, 0, buffIopsSize);
					long byte_count = 0;
					for (int k = 0; k < iopsFileSize / 2; k += buffIopsSize) {
                        int r = 0;
						if (directSeek(fd, k) < 0) break;
						if ((r = directWrite(fd, buff, buffIopsSize)) < 0) {
                            Log.d("SDPlay", "directIO position @ " + k);
                            break;
                        }
                        byte_count += r;
                        oper_count++;
						if (directSeek(fd, iopsFileSize / 2 + k) < 0) break;
						if ((r = directWrite(fd, buff, buffIopsSize)) < 0) {
                            Log.d("SDPlay", "directIO position @ " + (iopsFileSize / 2 + k));
                            break;
                        }
                        byte_count += r;
                        oper_count++;
					}
					directFree(buff);
					directClose(fd);
					*/
					/*
					 * RandomAccessFile newSparseFile = new RandomAccessFile(
					 * path, "rwd"); // s = O_SYNC flag
					 * 
					 * for (int k = 0; k < newSparseFile.length() / 2 ; k +=
					 * buffIopsSize) { newSparseFile.seek(k);
					 * newSparseFile.write(bytesForIops, 0, buffIopsSize);
					 * oper_count++; newSparseFile.seek(newSparseFile.length() /
					 * 2 + k); newSparseFile.write(bytesForIops, 0,
					 * buffIopsSize); oper_count++; } newSparseFile.close();
					 */
					// Log.d("SDPlay", "iops update operations count:"
					// + oper_count);

				}

			}

			// //////end of iops/////////////
			publishProgress(100);

			millis4 = SystemClock.elapsedRealtime();
			// writing time in ms
			diff_iops = millis4 - millis3;
			// Log.d("SDPlay", "iops update time:" + diff_iops);
			return 1;
			// When finished, return the resulting 1, this will cause the
			// Activity to call onPostExecute()
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			// take CPU lock to prevent CPU from going off if the user
			// presses the power button during download
			PowerManager pm = (PowerManager) getApplicationContext()
					.getSystemService(Context.POWER_SERVICE);
			mWakeLockCrFS = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
					getClass().getName());
			mWakeLockCrFS.acquire();

			mProgressDialog.setIndeterminate(true);
			mProgressDialog.setMax(1);
			mProgressDialog.setMessage("IOPS updates test is in progress...");
			mProgressDialog.show();
		}

		@Override
		protected void onProgressUpdate(Integer... progress) {
			super.onProgressUpdate(progress);
			if (progress[0] != 100) {
				mProgressDialog.setIndeterminate(true);
			} else {
				mProgressDialog.setIndeterminate(false);
				mProgressDialog.setProgress(progress[0]);
			}

		}

		@Override
		protected void onPostExecute(Integer result) {
			mWakeLockCrFS.release();
			mProgressDialog.dismiss();
			// Log.d("SDPlay", "dialog dismissed");
			if (result != 1) {
				Toast.makeText(getBaseContext(), "IOPS test error",
						Toast.LENGTH_LONG).show();
			} else {
				iops_rate = oper_count * 1000.0 / diff_iops;
				tmp += "IOPS updates rate " + String.format("%.2f", iops_rate)
						+ " operations/s\n";
				Toast.makeText(getBaseContext(),
						"IOPS update test is done successfully",
						Toast.LENGTH_SHORT).show();
			}
			if (ALL) {
				// onReadAllClick(findViewById(android.R.id.content).getRootView());
				// TODO
				textView11.setText(tmp);
			} else {
				textView.setText(tmp);
			}
			// readSmallFiles(findViewById(android.R.id.content).getRootView());
			// readLargeFiles(findViewById(android.R.id.content).getRootView());
			// cleanFs();

			deleteAllFiles(findViewById(android.R.id.content).getRootView());

			// readSmallFiles(findViewById(android.R.id.content).getRootView());
		}
	}

	// /////////end of IOPS test/////////////////////////////

	// //////////////IOPS read test/////////////////////////
	public void runIOPSRead(View view) {
		random = new Random();

		final RunIOPSReadTest runIOPSReadTest = new RunIOPSReadTest(this);
		runIOPSReadTest.executeOnExecutor(executor, 100, 0, 1);

		mProgressDialog
				.setOnCancelListener(new DialogInterface.OnCancelListener() {
					@Override
					public void onCancel(DialogInterface dialog) {

						runIOPSReadTest.cancel(true);
					}
				});
	}

	private class RunIOPSReadTest extends AsyncTask<Integer, Integer, Integer> {

		private Context context;
		private PowerManager.WakeLock mWakeLockCrFS;

		public RunIOPSReadTest(Context context) {
			this.context = context;
		}

		protected Integer doInBackground(Integer... params) {
			// get timestamp
			// millis3 = SystemClock.elapsedRealtime();
			publishProgress(0);

			File dir_lvl0_path = new File(sdPath + File.separator
					+ "fstest_folders");
			if (dir_lvl0_path.exists() && dir_lvl0_path.isDirectory()) {
				if (isCancelled()) {
					mWakeLockCrFS.release();
                //    wasCancelled = true;
					return 0;
				} else {
					String path = dir_lvl0_path + File.separator + "iops_f"
							+ ".txt";

					millis3 = SystemClock.elapsedRealtime();

                    oper_read_count = directIOPSr(path, 0, buffIopsSize);
                    /*
					int fd = directOpen(path, 0);
					ByteBuffer buff = directAllocate(buffIopsSize);

					oper_read_count = 0;
					long byte_count = 0;
					for (int k = 0; k < iopsFileSize / 2; k += buffIopsSize) {
                        int r = 0;
						if (directSeek(fd, k) < 0) break;
						if ((r = directRead(fd, buff, buffIopsSize)) < 0) {
                            Log.d("SDPlay", "directIO position @ " + k);
                            break;
                        }
                        byte_count += r;
                        oper_read_count++;
						if (directSeek(fd, iopsFileSize / 2 + k) < 0) break;
						if ((r = directRead(fd, buff, buffIopsSize)) < 0) {
                            Log.d("SDPlay", "directIO position @ " + (iopsFileSize / 2 + k));
                            break;
                        }
                        byte_count += r;
                        oper_read_count++;
					}
					directFree(buff);
					directClose(fd);
					*/
					// Log.d("SDPlay", "iops read operations count:" +
					// oper_count
					// + ", bytes: " + byte_count);

				}

			}

			// //////end of iops read/////////////
			publishProgress(100);

			millis4 = SystemClock.elapsedRealtime();
			// writing time in ms
			diff_iops = millis4 - millis3;
			// Log.d("SDPlay", "iops read time:" + diff_iops);
			return 1;
			// When finished, return the resulting 1, this will cause the
			// Activity to call onPostExecute()
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			// take CPU lock to prevent CPU from going off if the user
			// presses the power button during download
			PowerManager pm = (PowerManager) getApplicationContext()
					.getSystemService(Context.POWER_SERVICE);
			mWakeLockCrFS = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
					getClass().getName());
			mWakeLockCrFS.acquire();

			mProgressDialog.setIndeterminate(true);
			mProgressDialog.setMax(1);
			mProgressDialog.setMessage("IOPS read test is in progress...");
			mProgressDialog.show();
		}

		@Override
		protected void onProgressUpdate(Integer... progress) {
			super.onProgressUpdate(progress);
			if (progress[0] != 100) {
				mProgressDialog.setIndeterminate(true);
			} else {
				mProgressDialog.setIndeterminate(false);
				mProgressDialog.setProgress(progress[0]);
			}

		}

		@Override
		protected void onPostExecute(Integer result) {
			mWakeLockCrFS.release();
			mProgressDialog.dismiss();
			// Log.d("SDPlay", "dialog dismissed");
			if (result != 1) {
				Toast.makeText(getBaseContext(), "IOPS read test error",
						Toast.LENGTH_LONG).show();
			} else {
				// tmp += "\nTotal files written:  " + f_count + "\n";
				iops_read_rate = oper_read_count * 1000.0 / diff_iops;
				tmp += "IOPS read rate "
						+ String.format("%.2f", iops_read_rate)
						+ " operations/s\n";
				Toast.makeText(getBaseContext(),
						"IOPS read test is done successfully",
						Toast.LENGTH_SHORT).show();
			}
			if (ALL) {
				// onReadAllClick(findViewById(android.R.id.content).getRootView());
				// TODO
				textView11.setText(tmp);
			} else {
				textView.setText(tmp);
			}
			readSmallFiles(findViewById(android.R.id.content).getRootView());
			// readLargeFiles(findViewById(android.R.id.content).getRootView());
			// cleanFs();
			// deleteAllFiles(findViewById(android.R.id.content).getRootView());
		}
	}

	// /////////end of IOPS read test/////////////////////////////

	// ////////////delete all test fs files////////////////////////
	public void deleteAllFiles(View view) {
		list_d = new ArrayList<String>();
		list_f = new ArrayList<String>();
		final DeleteAllFiles deleteAllFiles = new DeleteAllFiles(this);
		deleteAllFiles.executeOnExecutor(executor, 100, 0, 1);

		mProgressDialog
				.setOnCancelListener(new DialogInterface.OnCancelListener() {
					@Override
					public void onCancel(DialogInterface dialog) {

						deleteAllFiles.cancel(true);
					}
				});
	}

	// ////////// progress bar for read all large files ///////////////////
	private class DeleteAllFiles extends AsyncTask<Integer, Integer, Integer> {

		private Context context;
		private PowerManager.WakeLock mWakeLockLFS;

		public DeleteAllFiles(Context context) {
			this.context = context;
		}

		// ArrayList<String> list = new ArrayList<String>();

		protected Integer doInBackground(Integer... params) {
			// get timestamp
			millis5 = SystemClock.elapsedRealtime();
			int progress = 0;
			f_count = cleanFs();
			progress = 100;
			publishProgress(progress);
			millis6 = SystemClock.elapsedRealtime();
			// writing time in ms
			diff_del = millis6 - millis5;
			// Log.d("SDPlay", "finish delete");
			// f_count = list_f.size();
			return 1;
			// When finished, return the resulting 1, this will cause the
			// Activity to call onPostExecute()
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			// take CPU lock to prevent CPU from going off if the user
			// presses the power button during download
			PowerManager pm = (PowerManager) getApplicationContext()
					.getSystemService(Context.POWER_SERVICE);
			mWakeLockLFS = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
					getClass().getName());
			mWakeLockLFS.acquire();

			mProgressDialog.setIndeterminate(true);
			mProgressDialog.setMax(1);
			mProgressDialog.setMessage("Deleting all test fs structure...");
			mProgressDialog.show();
		}

		@Override
		protected void onProgressUpdate(Integer... progress) {
			super.onProgressUpdate(progress);
			if (progress[0] != 100) {
				mProgressDialog.setIndeterminate(true);
			} else {
				mProgressDialog.setIndeterminate(false);
				mProgressDialog.setProgress(progress[0]);
			}

		}

		@Override
		protected void onPostExecute(Integer result) {
			mWakeLockLFS.release();
			mProgressDialog.dismiss();
			if (result != 1) {
				Toast.makeText(getBaseContext(), "File deleting error",
						Toast.LENGTH_LONG).show();
			} else {
				// tmp += "\nTotal files read: " + f_count + "\n";
				del_f = f_count * 1000.0 / diff_del;
				tmp += "Files delete speed " + String.format("%.2f", del_f)
						+ " files/s\n";
				Toast.makeText(getBaseContext(),
						"Files are deleted successfully ", Toast.LENGTH_SHORT)
						.show();
			}

			// cleanFs();
			isFsDone = true;

			saveRes();
			if (LOG_ON) {
				Log.d(TAG, "saveRes() has been called and FS test is done");
				Log.d(TAG, "Before calling ALL value is " + ALL
						+ "; DB test done is " + isWritten);
			}
		//	wasCancelled = false;
            exportResToCsv();
			if (ALL) {
				// onReadAllClick(findViewById(android.R.id.content).getRootView());
				// TODO
				textView11.setText(tmp);
				//ALL = false;
				showSummary();
			} else {
				textView.setText(tmp);
				showTopFsRes();
			}
			// writeMediumFiles(findViewById(android.R.id.content).getRootView());
		}
	}

	// ////////end of delete all test fs files//////////////////

	public int cleanFs() {
		int files_deleted = 0;
		try {
			File fstest = new File(sdPath + File.separator + "fstest_folders");
			if (fstest.exists() && fstest.isDirectory()) {
                if (LOG_ON) {
                    Log.d(TAG, "trying to delete fs...");
                }
				files_deleted = walk(fstest.getAbsolutePath(), 2);
			}
		} catch (Exception e) {
			Log.d(TAG, "Nothing to clean here: " + e);
		}
		return files_deleted;
	}

	// //////recursively go into folders
	public int walk(String path, int mode) {
		// modes: 1 - list, 2 - delete
		// ArrayList<String> list_d = new ArrayList<String>();
		// ArrayList<String> list_f = new ArrayList<String>();
		File root = new File(path);
		File[] list = root.listFiles();

		if (list == null)
			return 0;

		for (File f : list) {
			if (f.isDirectory()) {
				walk(f.getAbsolutePath(), mode);
				switch (mode) {
				case 1:
					list_d.add(f.getAbsolutePath());
					// Log.d("SDPlay", "Dir:" + f.getAbsoluteFile());
					break;
				case 2:

                    f.delete();
					break;
				case 3:
					break;
				case 4:
					break;
				default:
					break;
				}

			} else {
				switch (mode) {
				case 1:
					if (f.getAbsolutePath().contains("/small")) {
						// Log.d("SDPlay", "File medium: " +
						// f.getAbsoluteFile());
						list_f.add(f.getAbsolutePath());
					}
					break;
				case 2:
					// f.delete();
					list_f.add(f.getAbsolutePath());
					f.delete();
					break;
				case 3:
					if (f.getAbsolutePath().contains("/medium")) {
						// Log.d("SDPlay", "File medium: " +
						// f.getAbsoluteFile());
						list_f.add(f.getAbsolutePath());
					}
					break;
				case 4:
					if (f.getAbsolutePath().contains("/large")) {
						// Log.d("SDPlay", "File large: " +
						// f.getAbsoluteFile());
						list_f.add(f.getAbsolutePath());
					}
					break;
				default:
					break;
				}

			}
		}
		f_count = list_f.size();
		d_count = list_d.size();
		return f_count;
	}

	// //////////end of recursive reading folder//////////

	// ////////////finish of FS test part//////////////////////////////////////

	// ////////file methods///////////////
	public class BinaryFileUtils {

		public byte[] read(File file) {
			byte[] data = null;
			try {
				FileInputStream fis = new FileInputStream(file);
				BufferedInputStream bis = new BufferedInputStream(fis);
				data = new byte[bis.available()];
				// Log.d("SDPlay", "size "+bis.available());
				// data = new byte[1024];
				try {
					bis.read(data);
				} finally {
					bis.close();
					fis.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			return data;
		}

		public void write(byte[] data, File file) {
			try {
				FileOutputStream fos = new FileOutputStream(file);
				BufferedOutputStream bos = new BufferedOutputStream(fos);
				try {
					bos.write(data);
				} finally {
					bos.close();
					fos.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		public void writeLarge(byte[] data, File file, long fileSize) {
			BufferedOutputStream stream = null;
			try {
				stream = new BufferedOutputStream(new FileOutputStream(file));
			} catch (FileNotFoundException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

			byte[] buffer = new byte[bufferSize];
			random.nextBytes(buffer);
			for (int i = 0; i < fileSize / bufferSize; i++) {
				try {
					// random.nextBytes(buffer);
					stream.write(buffer, 0, bufferSize);
					// Log.d("SDPlay", "file size: " + i);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			if (stream != null)
				try {
					stream.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		}

		public int readLarge(File file) {
			BufferedInputStream stream = null;
			int b_count = 0;
			try {
				stream = new BufferedInputStream(new FileInputStream(file));
			} catch (FileNotFoundException e2) {
				// TODO Auto-generated catch block
				e2.printStackTrace();
			}
			;
			byte[] buffer = new byte[bufferSize];
			int actual = 0;
			while (actual != -1) {
				try {
					actual = stream.read(buffer, 0, bufferSize);
					b_count += actual;
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			// Log.d("SDPlay", "read large file size: " + b_count);
			if (stream != null)
				try {
					stream.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			return b_count;
		}

	}

	// ///////end of file methods///////////////

	public void writeRndAccessFile() {
		File dir_lvl0_path = new File(sdPath + File.separator
				+ "fstest_folders");
		if (dir_lvl0_path.exists() && dir_lvl0_path.isDirectory()) {
			String path = dir_lvl0_path + File.separator + "iops_f" + ".txt";
			Log.d("SDPlay", "path for iops file:" + path);
			BinaryFileUtils helper = new BinaryFileUtils();
			File newSparseFile = new File(path);
			helper.writeLarge(bytesToWriteLarge, newSparseFile, iopsFileSize);
			Log.d("SDPlay", "iops length:" + newSparseFile.length());
		}
	}

	public static boolean isSymlink(File file) throws IOException {
		File canon;
		if (file.getParent() == null) {
			canon = file;
		} else {
			File canonDir = file.getParentFile().getCanonicalFile();
			canon = new File(canonDir, file.getName());
		}
		return !canon.getCanonicalFile().equals(canon.getAbsoluteFile());
	}


    private void getEmmcSize() {
        String sizeToRound = "0";
        try {
            File fff = new File("/sys/block/mmcblk0/size");
            String line;

            if (fff.exists()) {
                BufferedReader in_emmc_size = new BufferedReader(
                        new FileReader(
                                "/sys/block/mmcblk0/size"));
                while ((line = in_emmc_size.readLine()) != null) {
                    sizeToRound = line;
                    Log.d(TAG, "read from sysfs: " + sizeToRound);
                }
                in_emmc_size.close();
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        long tmp = Long.parseLong(sizeToRound);
        eMmcSize = roundUp2(tmp/(2*1024*1024));
        if (LOG_ON) {
            Log.d(TAG, "emmc size is " + eMmcSize + "GB");
        }
    }


    private long roundUp2(long v)
    {
        long i, j;
        for (i=v, j=0; i>0; i>>=1) {
            v = i;
            j++;
        }
            return v << j;
    }


    public int cleanAppDirsOnExternalStorage() {
        int files_deleted = 0;
        String pathToDelete = "";
        String patternToClean = "(/.+/com.elena.sdplay)/.+";
        if (sdPath.matches(patternToClean)) {
            pathToDelete = sdPath.replaceAll(patternToClean, "$1");
            if (LOG_ON) {
                Log.d(TAG, "path to delete: " + pathToDelete);
            }
        }
        try {
            File appdirs = new File(pathToDelete);
            if (appdirs.exists() && appdirs.isDirectory()) {
                if (LOG_ON) {
                    Log.d(TAG, "trying to delete temporary dirs on external storage...");
                }
                files_deleted = walk(appdirs.getAbsolutePath(), 2);
            }
            appdirs.delete();
        } catch (Exception e) {
            Log.d(TAG, "Nothing to clean here: " + e);
        }

        return files_deleted;
    }

	public void onDestroy() {
        //mProgressDialog.dismiss();
        if (LOG_ON){
            Log.d(TAG, "onDestroy() has been called");
        }
        cleanFs();
        if (!sdPath.equals(intPath) && !userdata_selected) {
            cleanAppDirsOnExternalStorage();
        }
		super.onDestroy();
	}
	// class finish
}

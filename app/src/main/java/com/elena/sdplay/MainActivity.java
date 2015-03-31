package com.elena.sdplay;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

@SuppressLint({ "NewApi", "SdCardPath" })
public class MainActivity extends Activity {

	private static final String TAG = "SDPlayDebug";
    public static final boolean LOG_ON = false;
    public static final String ABOUT_TITLE = "\u00a9 2014-2015 Elena Last, Igor Kovalenko";
    public static String ABOUT_VERSION;
	public final static String SD_PATH = "com.elena.sdplay.SD_PATH";
	private String sdPath = "";
	public final static String INT_PATH = "com.elena.sdplay.INT_PATH";
	private String intPath = "";
	public String mount_out; // output of mount command
	private int usb_drive = 0;
	public final static String USB_DRIVE_SELECTED = "com.elena.sdplay.USB_DRIVE_SELECTED";
	private boolean usb_drive_selected = false;
	public final static String CUSTOM_SELECTED = "com.elena.sdplay.CUSTOM_SELECTED";
	private boolean custom_drive_selected = false;
	private String usb_drive_path;
	private int is_userdata = 0;
	private int isCustom = 1;
    public static boolean isEncrypted = false;
	String custom_path;
	EditText customPathEntered;
	String customPathVerified;
	public static String userdata_path;
	public final static String USERDATA_SELECTED = "com.elena.sdplay.USERDATA_SELECTED";
	private boolean userdata_selected = false;
	private File[] file_list;

	// public final static String INT_FS_TYPE = "com.elena.sdplay.INT_FS_TYPE";
	// public final static String EXT_FS_TYPE = "com.elena.sdplay.EXT_FS_TYPE";
	// public final static String USERDATA_FS_TYPE =
	// "com.elena.sdplay.USERDATA_FS_TYPE";
	// public final static String USB_FS_TYPE = "com.elena.sdplay.USB_FS_TYPE";
	public static String intFsType;
	public static String extFsType;
	public static String userdataFsType;
	public static String usbFsType;
	public static String customFsType;
	// private BroadcastReceiver SDCardStateChangeListener;

	public static int REF_COUNT;

	public static String calling_activity = "Main";
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
	public int currentApiVersion;
	public static String appVersion;

	// private boolean isCustomChecked;

	// RadioButton[] radiobutton;

	// RadioGroup pick;
	// private final int PICK_PATH = 12345;

	// private ArrayList<File> full_file_list;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		PreferenceManager.setDefaultValues(this, R.xml.pref_general, false);

		try {
			appVersion = this.getPackageManager().getPackageInfo(
					this.getPackageName(), 0).versionName;
		} catch (NameNotFoundException e) {
			Log.d(TAG, "App version not found " + e.getMessage());
		}

        ABOUT_VERSION = "SD Play v." + appVersion;

		currentApiVersion = android.os.Build.VERSION.SDK_INT;
		if (currentApiVersion < 19) {
			file_list = new File[2];
			file_list[0] = getExternalFilesDir(null);
		} else {
			file_list = getExternalFilesDirs(null);
            if (LOG_ON) {
                Log.d(TAG, "file_list length " + file_list.length);
            }
		}
		intPath = file_list[0].toString();
        if (LOG_ON) {
            Log.d(TAG, "internal path: " + intPath);
        }

		// check if reference results are added into DB already
		// if not yet - add them
		addReferenceRes();

	}

	@Override
	public void onResume() {
		super.onResume();
		calling_activity = "Main";
		mount_out = getMountOutput();

		usb_drive = 0;
		usb_drive_selected = false;
		is_userdata = 0;
		userdata_selected = false;

		BenchStart.isWritten = false;
		BenchStart.isFsDone = false;

		prepareScreen();
		getStorageOptions();

		// register sd card state change listener - unregister it in onPause!!!
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
	}

	public void prepareScreen() {
		if (currentApiVersion < 19) {
			file_list = new File[2];
			file_list[0] = getExternalFilesDir(null);
			// check if ext sd card exists////////////////
			File ext_f = new File("/storage/sdcard1");
			if (ext_f.exists() && ext_f.isDirectory()) {
				new File(ext_f.getAbsolutePath()
						+ "/Android/data/com.elena.sdplay/files").mkdirs();
				File ext_f2 = new File(ext_f.getAbsolutePath()
						+ "/Android/data/com.elena.sdplay/files");
				if (ext_f2.exists() && ext_f2.isDirectory()) {
					file_list[1] = ext_f2;
				}
			}
			// //////end for sd card////////////////
		} else {
			file_list = getExternalFilesDirs(null);
			// Log.d("SDPlay", "file_list length from prepareScreen "
			// + file_list.length);
		}
		RadioGroup pick = (RadioGroup) findViewById(R.id.pick_path);

		pick.clearCheck(); // This is VERY IMPORTANT STEP!!!
		pick.removeAllViews();
		// //////////get userdata as storage///////////////

		// ///////////parse mount////////////////
		// for usb case
		String pattern0 = ".*\\S+/userdata\\s+(/data)\\s+.*"; // (/\\w+)
		String patternEncrypted = ".*\\S+/dm-\\d+\\s+(/data)\\s+.*"; // (/\\w+)
		String userdata_test_path = "";
		// pattern1 = ".*/mnt/media_rw/usbdisk\\S*\\s+\\w*.*";
		if (mount_out.matches(pattern0)) {
			userdata_test_path = mount_out.replaceAll(pattern0, "$1");
            isEncrypted = false;
            if (LOG_ON) {
                Log.d("SDPlay", "userdata is named " + userdata_test_path);
            }
		} else if (mount_out.matches(patternEncrypted)) {
			userdata_test_path = mount_out.replaceAll(patternEncrypted, "$1");
            isEncrypted = true;
            if (LOG_ON) {
                Log.d(TAG, "userdata is encrypted " + userdata_test_path);
            }
		} else {
            if (LOG_ON) {
                Log.d(TAG, "userdata path does not match");
            }
		}

		// /////////end of parse mount///////////
		File userdata_f = new File(userdata_test_path);
		if (userdata_f.exists() && userdata_f.isDirectory()) {
			new File(userdata_f.getAbsolutePath()
					+ "/data/com.elena.sdplay/files").mkdirs();
			File userdata_f2 = new File(userdata_f.getAbsolutePath()
					+ "/data/com.elena.sdplay/files");
			if (userdata_f2.exists() && userdata_f2.isDirectory()) {
				is_userdata = 1;
				userdata_path = userdata_f2.getAbsolutePath();
			}
		}

		// ////////end for userdata part

        String usb_test_path = "";
			// ///////////parse mount////////////////
			// for usb case
			pattern0 = ".*(/storage/usbdisk\\d*\\S*)\\s+.*";
			// pattern1 = ".*/mnt/media_rw/usbdisk\\S*\\s+\\w*.*";
			if (mount_out.matches(pattern0)) {
				usb_test_path = mount_out.replaceAll(pattern0, "$1");
                if (LOG_ON) {
                    Log.d(TAG, "usb path: " + usb_test_path);
                }
			} else {
                if (LOG_ON) {
                    Log.d(TAG, "usb pattern does not match");
                }
            }

			// /////////end of parse mount///////////
			File usb_f = new File(usb_test_path);
            if (LOG_ON) {
                Log.d(TAG, "usb exist? and isDirectory? " + usb_f.exists() + "; " + usb_f.isDirectory());
            }
			if (usb_f.exists() && usb_f.isDirectory()) {
                //try {
                if (LOG_ON) {
                    Log.d(TAG, "usb is writable: " + usb_f.canWrite());
                }
                    new File(usb_f.getAbsolutePath()
                            + File.separator + "apps" + File.separator +
                            "com.elena.sdplay" + File.separator + "files").mkdirs();
                    File usb_f2 = new File(usb_f.getAbsolutePath() + File.separator + "apps" +
                            File.separator +
                            "com.elena.sdplay" + File.separator + "files");

                if (LOG_ON) {
                    Log.d(TAG, "usb full path exist? and isDirectory? " + usb_f2.exists() + "; " + usb_f2.isDirectory());
                }
                if (usb_f2.exists() && usb_f2.isDirectory()) {
                    usb_drive = 1;
                    usb_drive_path = usb_f2.getAbsolutePath();
                    if (LOG_ON) {
                        Log.d(TAG, "usb path for test is " + usb_drive_path);
                    }

                }

            }

	}

	public void getStorageOptions() {
		String textShow = "";

		getFsTypes();

		RadioButton[] radiobutton = new RadioButton[file_list.length
				+ usb_drive + is_userdata + isCustom];
        if (LOG_ON) {
            Log.d("SDPlay", "buttons " + file_list.length + usb_drive +
                    is_userdata + isCustom);
        }
		RadioGroup pick = (RadioGroup) findViewById(R.id.pick_path);

		for (int i = 0; i < file_list.length; i++) {
			if (file_list[i] != null) {

                radiobutton[i] = new RadioButton(this);
                radiobutton[i].setId(i);

				if (i == 0) {
					textShow = "Internal Memory" + " [" + intFsType + "]";
                    if (isEncrypted) {
                        radiobutton[i].setText(Html.fromHtml(textShow + "<sup><small>enc</small></sup>"));
                    } else {
                        radiobutton[i].setText(textShow);
                    }
				} else if (i == 1) {
					textShow = "External SD Card" + " [" + extFsType + "]";
                    radiobutton[i].setText(textShow);
                    if (LOG_ON) {
                        Log.d(TAG, "External button, i=1");
                    }
				} else {
					textShow = file_list[i].toString();
                    radiobutton[i].setText(textShow);
				}

				pick.addView(radiobutton[i]);
				sdPath = file_list[i].toString();
			}
		}
		if (is_userdata == 1) {
			radiobutton[file_list.length] = new RadioButton(this);
            if (isEncrypted) {
                radiobutton[file_list.length].setText(Html.fromHtml("/userdata" + " ["
                        + userdataFsType + "]<sup><small>enc</small></sup>"));
            } else {
                radiobutton[file_list.length].setText("/userdata" + " ["
                        + userdataFsType + "]");
            }

			radiobutton[file_list.length].setId(file_list.length);
			pick.addView(radiobutton[file_list.length]);
			// Log.d("SDPlay", "Userdata path added");
		}
		if (usb_drive == 1) {
			radiobutton[file_list.length + is_userdata] = new RadioButton(this);
			radiobutton[file_list.length + is_userdata].setText("USB Drive"
					+ " [" + usbFsType + "]");
			radiobutton[file_list.length + is_userdata].setId(file_list.length
					+ is_userdata);
			pick.addView(radiobutton[file_list.length + is_userdata]);
			// Log.d("SDPlay", "Usb path added");
		}
		if (isCustom == 1 && usb_drive == 0) {
			radiobutton[file_list.length + isCustom] = new RadioButton(this);
			radiobutton[file_list.length + isCustom].setText("Custom storage");
			radiobutton[file_list.length + isCustom].setId(file_list.length
					+ isCustom);
			pick.addView(radiobutton[file_list.length + isCustom]);
			// Log.d("SDPlay", "Custom path added, no usb");
		}
		if (isCustom == 1 && usb_drive == 1) {
			radiobutton[file_list.length + is_userdata + isCustom] = new RadioButton(
					this);
			radiobutton[file_list.length + is_userdata + isCustom]
					.setText("Custom storage");
			radiobutton[file_list.length + is_userdata + isCustom]
					.setId(file_list.length + is_userdata + isCustom);
			pick.addView(radiobutton[file_list.length + is_userdata + isCustom]);
			// Log.d("SDPlay", "Custom path added + usb");
		}
		// textToShow.setText(textShow);
		SharedPreferences userPref = PreferenceManager
				.getDefaultSharedPreferences(this);
		String tmp = userPref.getString("customPathFs",
				"Custom path isn't defined yet");
		TextView customPath = (TextView) findViewById(R.id.custom_path);
		customPath.setText(tmp);
        if (!tmp.contains("Custom path isn't defined yet")) {
            checkCustomPath(userPref.getString("customPath",
                    ""), false);
        }
		addListenerOnStart();
	}

	public void addListenerOnStart() {

		final RadioGroup pick_path = (RadioGroup) findViewById(R.id.pick_path);
		Button btnStart = (Button) findViewById(R.id.buttonStart);
		btnStart.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// get selected radio button from radioGroup
				int selectedId = pick_path.getCheckedRadioButtonId();

				if (selectedId != -1) {
					if (selectedId == file_list.length && is_userdata == 0) {
						sdPath = usb_drive_path;
						usb_drive_selected = true;
                        userdata_selected = false;
                        custom_drive_selected = false;
					} else if (selectedId == file_list.length
							&& is_userdata == 1) {
						sdPath = userdata_path;
						userdata_selected = true;
                        usb_drive_selected = false;
                        custom_drive_selected = false;
					} else if ((selectedId == (file_list.length + is_userdata))
							&& is_userdata == 1 && usb_drive == 1) {
						sdPath = usb_drive_path;
						usb_drive_selected = true;
                        userdata_selected = false;
                        custom_drive_selected = false;
					} else if (((selectedId == (file_list.length + is_userdata)) && usb_drive == 0)
							|| ((selectedId == (file_list.length + is_userdata + usb_drive)) && usb_drive == 1)) {
						sdPath = customPathVerified;
						custom_drive_selected = true;
                        usb_drive_selected = false;
                        userdata_selected = false;

					} else {
						sdPath = file_list[selectedId].toString();
						usb_drive_selected = false;
						userdata_selected = false;
						custom_drive_selected = false;
					}
					if (sdPath.isEmpty()) {
						Toast.makeText(getApplicationContext(),
								"Please enter valid path for test",
								Toast.LENGTH_SHORT).show();
					} else {
                        if (LOG_ON) {
                            Log.d(TAG, "Selected storage is " + sdPath + "; button checked " + selectedId);
                        }
						Intent intent = new Intent(getApplicationContext(),
								BenchStart.class);
						intent.putExtra(SD_PATH, sdPath);
						intent.putExtra(INT_PATH, intPath);
						intent.putExtra(USB_DRIVE_SELECTED, usb_drive_selected);
						intent.putExtra(USERDATA_SELECTED, userdata_selected);
						intent.putExtra(CUSTOM_SELECTED, custom_drive_selected);
						// intent.putExtra(INT_FS_TYPE, intFsType);
						// intent.putExtra(EXT_FS_TYPE, extFsType);
						// intent.putExtra(USERDATA_FS_TYPE, userdataFsType);
						// intent.putExtra(USB_FS_TYPE, usbFsType);

						Toast.makeText(getApplicationContext(), sdPath,
								Toast.LENGTH_SHORT).show();
						startActivity(intent);
					}
				} else {
					Toast.makeText(getApplicationContext(),
							"Please select storage for test",
							Toast.LENGTH_SHORT).show();
				}
			}

		});

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

			builder.setMessage(ABOUT_TITLE).setTitle(
					ABOUT_VERSION);

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

	public void onRefreshClick(View view) {
		finish();
		startActivity(getIntent());
	}

	public void onCustomEditClick(View view) {

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Edit custom path");

		final EditText input = new EditText(MainActivity.this);
		LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT,
				LinearLayout.LayoutParams.MATCH_PARENT);
		input.setLayoutParams(lp);
		final SharedPreferences userPref = PreferenceManager
				.getDefaultSharedPreferences(this);
		String tmp = userPref.getString("customPath", "");

		input.setText(tmp);
		builder.setView(input);

		builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				String tmpString = input.getText().toString();
				if (checkCustomPath(tmpString, true) != 0) {
					final String tmpFs = userPref.getString("customPathFs",
							"Path isn't defined yet");
					TextView customPath = (TextView) findViewById(R.id.custom_path);
					customPath.setText(tmpFs);
				}
			}
		});
		builder.setNegativeButton("Cancel",
				new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}
				});

		AlertDialog dialog = builder.create();
		dialog.show();

	}

	private void addReferenceRes() {
		SharedPreferences userPref = PreferenceManager
				.getDefaultSharedPreferences(this);

		// //////Add reference results to Results database
		if (!userPref.getBoolean("isRef", false)) {
			// TODO
			// Toast.makeText(getApplicationContext(), "this works!!!!!!!!",
			// Toast.LENGTH_SHORT).show();
			MyResDBHelper myResDB = new MyResDBHelper(getBaseContext(), intPath);
			SQLiteDatabase res_db = myResDB.getWritableDatabase();
			// Create a new map of values, where column names are the keys
			ContentValues values = new ContentValues();

			String[] ref_res = getResources().getStringArray(
					R.array.reference_results);
			REF_COUNT = ref_res.length;
			// card_full_details = Integer.toString(card_manfs.length);
			String[][] ref_results = new String[ref_res.length][30];
			for (int i = 0; i < REF_COUNT; i++) {
				ref_results[i] = ref_res[i].split("\\|");
				res_db.beginTransaction();
				try {

				values.put(myResDB.RES_OEMID, ref_results[i][0]);
				values.put(myResDB.RES_MANFID, ref_results[i][1]);
				values.put(myResDB.RES_NAME, ref_results[i][2]);
			    values.put(myResDB.RES_DETAILS, ref_results[i][3]);

                values.put(myResDB.RES_DEV_SIZE, ref_results[i][4]);
				values.put(myResDB.RES_SERIAL, ref_results[i][5]);
				values.put(myResDB.RES_BUILD_ID, ref_results[i][6]);
                values.put(myResDB.RES_FS_TYPE, ref_results[i][7]);

				values.put(myResDB.RES_NOTES, ref_results[i][8]);
                    //TODO: rework references order when new refs are ready
                //values.put(myResDB.RES_JOURNAL, ref_results[i][30]);
				values.put(myResDB.RES_W_SPEED, ref_results[i][9]);
				values.put(myResDB.RES_RW_SPEED, ref_results[i][10]);
				values.put(myResDB.RES_RR_SPEED, ref_results[i][11]);
				values.put(myResDB.RES_D_SPEED, ref_results[i][12]);
				values.put(myResDB.RES_TOTAL_SCORE, ref_results[i][13]);

				values.put(myResDB.FS_C_SPEED, ref_results[i][14]);
				values.put(myResDB.FS_L_SPEED, ref_results[i][15]);
				values.put(myResDB.FS_RS_SPEED, ref_results[i][16]);
				values.put(myResDB.FS_WM_SPEED, ref_results[i][17]);
				values.put(myResDB.FS_RM_SPEED, ref_results[i][18]);

				values.put(myResDB.FS_WL_SPEED, ref_results[i][19]);
				values.put(myResDB.FS_RL_SPEED, ref_results[i][20]);
                values.put(myResDB.FS_IOPS_W, ref_results[i][21]);
                values.put(myResDB.FS_IOPS_R, ref_results[i][22]);

                values.put(myResDB.FS_D_SPEED, ref_results[i][23]);
                values.put(myResDB.FS_TOTAL_SCORE, ref_results[i][24]);
                values.put(myResDB.SUMMARY_SCORE,ref_results[i][25]);

				values.put(myResDB.FS_SM_SCORE, ref_results[i][26]);
				values.put(myResDB.FS_M_SCORE, ref_results[i][27]);
				values.put(myResDB.FS_L_SCORE,ref_results[i][28]);
				values.put(myResDB.FS_IOPS_SCORE, ref_results[i][29]);

				res_db.insert(myResDB.RES_TABLE, null, values);
				res_db.setTransactionSuccessful();
				} finally {
					res_db.endTransaction();
				}
			}
			res_db.close();
			// if added successfully set Value to true - to not add them anymore
			Editor editor = userPref.edit();
			editor.putBoolean("isRef", true);
			editor.commit();
		} else {
			// Toast.makeText(getApplicationContext(), "does not work!!!!!!!!",
			// Toast.LENGTH_SHORT).show();
		}
		// //////////////////////////

	}

	public static String getMountOutput() {
		String mount_out = "";
		String line;
		try {
			Process pr = Runtime.getRuntime().exec("mount");
			BufferedReader in = new BufferedReader(new InputStreamReader(
					pr.getInputStream()));
			while ((line = in.readLine()) != null) {
				mount_out += "mount: " + line;
			}
			in.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return mount_out;
	}

	public void getFsTypes() {
		String intPattern0, intPattern1;
		String extPattern0, extPattern1;
		String usbPattern0, usbPattern1;
		// for internal case

		intPattern0 = ".*/storage/emulated/\\w+\\s+(\\w+)\\s+.*";
		// intPattern1 = ".*/userdata\\s+/\\w*\\s+(\\w+).*";
		intPattern1 = ".*\\s+/data\\s+(\\w+).*";
		if (intPath.contains("storage/emulated")
				&& mount_out.matches(intPattern0)) {
			intFsType = mount_out.replaceAll(intPattern0, "$1") + "/";
		}
		if (mount_out.matches(intPattern1)) {
			intFsType += mount_out.replaceAll(intPattern1, "$1");
			userdataFsType = mount_out.replaceAll(intPattern1, "$1");
		}
		// //////end of internal case/////////////
		// for sdcard case
		extPattern0 = ".*/storage/sdcard1\\s+(\\w+)\\s+.*";
		extPattern1 = ".*/mnt/media_rw/sdcard1\\s+(\\w+).*";
		if (mount_out.matches(extPattern0)) {
			extFsType = mount_out.replaceAll(extPattern0, "$1") + "/";
		}
		if (mount_out.matches(extPattern1)) {
			extFsType += mount_out.replaceAll(extPattern1, "$1");
		}
		// /////end of external case///////////////
		// for usb case
		usbPattern0 = ".*/storage/usbdisk\\S*\\s+(\\w+)\\s+.*";
		// usbPattern2 = ".*/storage/usbdisk\\S*\\s+(\\w+).*";
		usbPattern1 = ".*/mnt/media_rw/usbdisk\\S*\\s+(\\w+).*";
		if (mount_out.matches(usbPattern0)) {
			usbFsType = mount_out.replaceAll(usbPattern0, "$1") + "/";
			if (mount_out.matches(usbPattern1)) {
				usbFsType += mount_out.replaceAll(usbPattern1, "$1");
			}
		}
		// else if (mount_out.matches(pattern2)) {
		// fs_type = mount_out.replaceAll(pattern2, "$1");
		// }

		// /////////end of usb case///////////
	}

	public int checkCustomPath(String pathToCheck, boolean addFsType) {
		// isCustomChecked = false;
		// mount_out = MainActivity.getMountOutput();
		customPathVerified = "";
		String rootDirPattern = "(/\\w+)/*.*";
		String customPath = pathToCheck;

		if (customPath.isEmpty()) {
			Toast.makeText(getApplicationContext(),
					"Please enter path for test", Toast.LENGTH_SHORT).show();
			return 0;
		}

		String rootCustom = "";
		if (customPath.matches(rootDirPattern)) {
			rootCustom = customPath.replaceAll(rootDirPattern, "$1");
            if (LOG_ON) {
                Log.d(TAG, "root for custom path " + rootCustom);
            }
		}
		File cust_r = new File(rootCustom);

		// new File(customPath).mkdirs();
		File cust_f = new File(customPath);
		if (!cust_f.exists() || !cust_f.isDirectory()) {
			new File(customPath).mkdirs();
		}
		cust_f.setExecutable(true);
		cust_f.setReadable(true);
		cust_f.setWritable(true);

		if (!cust_r.exists() || !cust_r.isDirectory()) {
			Toast.makeText(getApplicationContext(),
					"Custom path doesn't exist", Toast.LENGTH_SHORT).show();
            if (LOG_ON) {
                Log.d(TAG, "oops - custom path does not exist!");
            }
			return 0;
		} else if (!cust_r.canExecute() || !cust_r.canWrite()) {
			Toast.makeText(getApplicationContext(),
					"You have no enough permissions for that storage!",
					Toast.LENGTH_SHORT).show();
            if (LOG_ON) {
                Log.d(TAG, "permissions for root of custom path FAIL");
            }
			return 0;
		}

		if (cust_f.canExecute() && cust_f.canRead() && cust_f.canWrite()) {

			new File(cust_f.getAbsolutePath()
					+ "/Android/com.elena.sdplay/files").mkdirs();

			File custom_f = new File(cust_f.getAbsolutePath()
					+ "/Android/com.elena.sdplay/files");
			if (custom_f.exists() && custom_f.isDirectory()) {

				customPathVerified = custom_f.getAbsolutePath();
                if (LOG_ON) {
                    Log.d(TAG, "custom path exists: " + customPathVerified);
                }
			}
            if (LOG_ON) {
                Log.d(TAG,
                 "permissions for root of custom OK: " + cust_f.canExecute() + "; "
                 + cust_f.canRead() + "; "+ cust_f.canWrite());
            }
		} else {
			Toast.makeText(getApplicationContext(),
					"Custom path doesn't exist", Toast.LENGTH_SHORT).show();
            if (LOG_ON) {
                Log.d(TAG, "custom path does not exist: " + cust_f.canExecute() + "; "
                        + cust_f.canRead() + "; " + cust_f.canWrite());
            }
			return 0;
		}

		// ////////
		String custFsTypePattern = "";
		String startPattern1 = "/storage/emulated.*";
		String startPattern2 = "/data.*";
		String startPattern3 = "/storage/sdcard1.*";
		if (customPath.matches(startPattern1)) {
			customFsType = MainActivity.intFsType;
		} else if (customPath.matches(startPattern2)) {
			customFsType = MainActivity.userdataFsType;
		} else if (customPath.matches(startPattern3)) {
			customFsType = MainActivity.extFsType;
		} else {
			try {
				if (BenchStart.isSymlink(cust_r)) {
					// Log.d("SDPlay", "custom path is symlink - true");
					custFsTypePattern = ".*" + cust_r.getCanonicalPath()
							+ "\\s+(\\w*).*";
				} else {
					// Log.d("SDPlay", "custom path is symlink - false");
					custFsTypePattern = ".*" + rootCustom + "\\s+(\\w*).*";
				}

				if (mount_out.matches(custFsTypePattern)) {
					customFsType = mount_out
							.replaceAll(custFsTypePattern, "$1");
					// Log.d("SDPlay", "custom fs type is " + customFsType);
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
        if (addFsType) {
            SharedPreferences userPref = PreferenceManager
                    .getDefaultSharedPreferences(this);
            Editor editor = userPref.edit();
            editor.putString("customPathFs", pathToCheck + " [" + customFsType
                    + "]");
            editor.putString("customPath", pathToCheck);
            editor.commit();
        }
		return 1;
		// isCustomChecked = true;
	}

	public void onPause() {
		this.unregisterReceiver(listener);
		super.onPause();
	}
	// ///////////final of Main Activity////////////////
}

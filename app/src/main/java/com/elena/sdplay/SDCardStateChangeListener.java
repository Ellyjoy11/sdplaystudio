package com.elena.sdplay;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

public class SDCardStateChangeListener extends BroadcastReceiver {
	// final String MEDIA_REMOVED = Intent.ACTION_MEDIA_REMOVED;
	final String MEDIA_UNMOUNTED = Intent.ACTION_MEDIA_UNMOUNTED;
	final String MEDIA_BAD_REMOVAL = Intent.ACTION_MEDIA_BAD_REMOVAL;
	final String MEDIA_EJECT = Intent.ACTION_MEDIA_SCANNER_FINISHED;
	final String MEDIA_EJECT1 = Intent.ACTION_MEDIA_EJECT;

	final String MEDIA_MOUNTED = Intent.ACTION_MEDIA_MOUNTED;

	final String USB_DEVICE_DETACHED = "android.hardware.usb.action.USB_DEVICE_DETACHED";
	final String USB_DEVICE_ATTACHED = "android.hardware.usb.action.USB_DEVICE_ATTACHED";
	private boolean isRefreshNeed = false;

	public SDCardStateChangeListener() {
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		// TODO: This method is called when the BroadcastReceiver is receiving
		// an Intent broadcast.
		// throw new UnsupportedOperationException("Not yet implemented");
		// isRefreshNeed = false;
		String action = intent.getAction();
		Log.d("SDPlay", "event: " + action);
		if (MainActivity.calling_activity.equalsIgnoreCase("Main")) {
			isRefreshNeed = true;
			Log.d("SDPlay", "set true");
		}
		if (action.equalsIgnoreCase(MEDIA_EJECT)
				&& MainActivity.calling_activity.equalsIgnoreCase("BenchExt")) {
			// || action.equalsIgnoreCase(MEDIA_BAD_REMOVAL)
			// || action.equalsIgnoreCase(MEDIA_EJECT)) {
			Log.d("SDPlay", "receive event");
			Toast.makeText(
					context,
					"SD Card has been removed. Updating list...\n"
							+ MainActivity.calling_activity, Toast.LENGTH_SHORT)
					.show();
			isRefreshNeed = true;
			// Intent back_intent = new Intent(context, MainActivity.class);
			// context.startActivity(back_intent);
		}
		if (action.equalsIgnoreCase(MEDIA_MOUNTED)
				|| action.equalsIgnoreCase(USB_DEVICE_ATTACHED)) {
			Toast.makeText(context,
					"New media has been inserted. Updating list...",
					Toast.LENGTH_SHORT).show();
		}
		if ((action.equalsIgnoreCase(USB_DEVICE_DETACHED) || action
				.equalsIgnoreCase(MEDIA_EJECT))
				&& MainActivity.calling_activity.equalsIgnoreCase("BenchUsb")) {
			Toast.makeText(context,
					"USB Drive state has been removed. Updating list...",
					Toast.LENGTH_SHORT).show();
			isRefreshNeed = true;
			// Intent back_intent = new Intent(context, MainActivity.class);
			// context.startActivity(back_intent);
		}
		if (isRefreshNeed) {
			Intent back_intent = new Intent(context, MainActivity.class);
			context.startActivity(back_intent);
		}
	}

};

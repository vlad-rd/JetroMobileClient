package com.jetro.mobileclient.ui;

import com.freerdp.freerdpcore.application.NetworkStateReceiver;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class NetworkChangeReceiver extends NetworkStateReceiver {
	
	private static final String TAG = NetworkChangeReceiver.class
			.getSimpleName();
	
	@Override
	public void onReceive(Context context, Intent intent) {
		Log.d(TAG, "NetworkChangeReceiver#onReceive(...) ENTER");
		
		if (!super.isConnectedTo3G(context)) {
//			Intent intentone = new Intent(context.getApplicationContext(),
//					ConnectionsListActivity.class);
//			intentone.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK
//					| Intent.FLAG_ACTIVITY_CLEAR_TOP
//					| Intent.FLAG_ACTIVITY_NEW_TASK);
//			context.startActivity(intentone);
		}
	}

	public boolean isConnected(Context context, Intent intent) {
		return super.isConnectedTo3G(context);
	}

}
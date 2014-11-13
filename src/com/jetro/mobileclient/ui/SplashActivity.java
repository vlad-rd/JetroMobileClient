package com.jetro.mobileclient.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import com.freerdp.freerdpcore.sharedobjects.utils.Constants;
import com.jetro.mobileclient.R;
import com.jetro.mobileclient.ui.activities.ConnectionActivity;
import com.jetro.mobileclient.ui.activities.ConnectionsListActivity;

public class SplashActivity extends BaseActivity {
	
	private static final String TAG = SplashActivity.class.getSimpleName();
	
	private static final int SPLASH_SCREEN_TIMEOUT = 2000;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.d(TAG, "onCreate(...) ENTER");
		super.onCreate(savedInstanceState);

		setContentView(R.layout.splash_activity_layout);

		new Handler().postDelayed(new Runnable() {
			@Override
			public void run() {
				Intent intent;
				if (connectionsDB.isDBEmpty()) {
					intent = new Intent(SplashActivity.this, ConnectionActivity.class);
					intent.putExtra(Constants.MODE, ConnectionActivityMode.AddConnection);
				} else {					
					intent = new Intent(SplashActivity.this, ConnectionsListActivity.class);
				}
				startActivity(intent);
				finish();
			}
		}, SPLASH_SCREEN_TIMEOUT);
	}
	
	
}

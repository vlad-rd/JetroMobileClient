package com.jetro.mobileclient.ui.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import com.jetro.mobileclient.R;
import com.jetro.mobileclient.config.Config;
import com.jetro.mobileclient.repository.ConnectionsDB;

public class SplashActivity extends Activity {
	
	private static final String TAG = SplashActivity.class.getSimpleName();
	
	private static final int SPLASH_SCREEN_TIMEOUT = 2000;

	private ConnectionsDB mConnectionsDB;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.d(TAG, TAG + "#onCreate(...) ENTER");
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_splash);
		
		mConnectionsDB = ConnectionsDB.getInstance(getApplicationContext());

		new Handler().postDelayed(new Runnable() {
			@Override
			public void run() {
				Intent intent;
				if (mConnectionsDB.isDBEmpty()) {
					intent = new Intent(SplashActivity.this, ConnectionActivity.class);
					intent.putExtra(
							Config.Extras.EXTRA_CONNECTION_ACTIVITY_STATE,
							ConnectionActivity.State.ADD_CONNECTION);
				} else {					
					intent = new Intent(SplashActivity.this, ConnectionsListActivity.class);
				}
				startActivity(intent);
				finish();
			}
		}, SPLASH_SCREEN_TIMEOUT);
	}
	
	
}

package com.jetro.mobileclient.repository;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

// TODO: check if needed, if not delete it
public class AppsDB {
	
	private static final String TAG = AppsDB.class.getSimpleName();
	
	private static final String RUNNING_APPS_DB = "running_apps_db";
	
	private static SharedPreferences runningApps;
	
	private static AppsDB instance;
	
	private AppsDB(Context context) {
		Log.d(TAG, "AppsDB#AppsDB(...) ENTER");
		
		runningApps = context.getSharedPreferences(RUNNING_APPS_DB, Context.MODE_PRIVATE);
	}
	
	public static AppsDB getInstance(Context context) {
		Log.d(TAG, "AppsDB#getInstance(...) ENTER");
		
		if (instance == null) {
			instance = new AppsDB(context);
		}
		
		return instance;
	}

}

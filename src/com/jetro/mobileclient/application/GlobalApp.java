package com.jetro.mobileclient.application;

import android.content.Context;
import android.util.Log;


public class GlobalApp extends com.freerdp.freerdpcore.application.GlobalApp {
	
	private static final String TAG = GlobalApp.class.getSimpleName();
	
	private static Context mAppContext = null;

	@Override
	public void onCreate() {
		Log.d(TAG, TAG + "#onCreate(...) ENTER");
		super.onCreate();
		
		mAppContext = getApplicationContext();
	}
	
	public static Context getContext() {
		return mAppContext;
	}

}

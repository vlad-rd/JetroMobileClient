/**
 * 
 */
package com.jetro.mobileclient.config;

import java.io.File;

import android.os.Environment;

import com.jetro.mobileclient.application.GlobalApp;

/**
 * @author ran.h
 *
 */
public class Config {
	
	private Config() {
	}
	
	public static final String APP_PACKAGE = "com.jetro.mobileclient";
	
	public static final String APP_NAME = "Jetro";
	
	public static class Paths {
		
		public static final String DIR_STORAGE_INTERNAL = GlobalApp
				.getContext()
				.getDir("data", 0).getPath();
		
		public static final String DIR_EXTERNAL_INTERNAL = Environment
				.getExternalStorageDirectory().getPath()
				+ File.separator
				+ APP_NAME
				+ File.separator
				+ "data"
				+ File.separator;
		
		public static final String DIR_IMAGES = DIR_EXTERNAL_INTERNAL + "images" + File.separator;
		
		public static final String FILE_LOGIN_IMAGE = DIR_EXTERNAL_INTERNAL + DIR_IMAGES + "login_image";
		
	}
	
	public static class Prefs {
		
		private static final String PREF_KEY_PREFIX = APP_PACKAGE + ".PREF_KEY_";
		
//		public static final String PREFS_USER_CREDENTIALS = APP_PACKAGE + ".PREFS_USER_CREDENTIALS";
		
//		public static final String PREF_KEY_USER_NAME = PREF_KEY_PREFIX + "USER_NAME";
		
//		public static final String PREF_KEY_PASSWORD = PREF_KEY_PREFIX + "PASSWORD";
		
//		public static final String PREF_KEY_DOMAIN = PREF_KEY_PREFIX + "DOMAIN";
		
	}
	
	public static class Extras {
		
		public static final String EXTRA_CONNECTION_ACTIVITY_STATE = APP_PACKAGE + ".EXTRA_CONNECTION_ACTIVITY_STATE";
		
		public static final String EXTRA_RESET_PASSWORD_ACTIVITY_STATE = APP_PACKAGE + ".EXTRA_RESET_PASSWORD_ACTIVITY_STATE";
		
		public static final String EXTRA_CONNECTION = APP_PACKAGE + ".EXTRA_CONNECTION";
		
		public static final String EXTRA_TYPE = APP_PACKAGE + ".EXTRA_TYPE";
		
	}
	
	public static final class Actions {
		
		public static final String ACTION_CONNECTIVITY_CHANGE = "android.net.conn.CONNECTIVITY_CHANGE";
		
	}

}

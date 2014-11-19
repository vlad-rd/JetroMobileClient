/**
 * 
 */
package com.jetro.mobileclient.utils;

/**
 * @author ran.h
 *
 */
public class Config {
	
	private Config() {
	}
	
	public static final String APP_PACKAGE = "com.jetro.mobileclient";
	
	public static class Prefs {
		
		private static final String PREF_KEY_PREFIX = APP_PACKAGE + ".PREF_KEY_";
		
//		public static final String PREFS_USER_CREDENTIALS = APP_PACKAGE + ".PREFS_USER_CREDENTIALS";
		
//		public static final String PREF_KEY_USER_NAME = PREF_KEY_PREFIX + "USER_NAME";
		
//		public static final String PREF_KEY_PASSWORD = PREF_KEY_PREFIX + "PASSWORD";
		
//		public static final String PREF_KEY_DOMAIN = PREF_KEY_PREFIX + "DOMAIN";
		
	}
	
	public static class Extras {
		
		public static final String EXTRA_CONNECTION_ACTIVITY_STATE = APP_PACKAGE + ".EXTRA_CONNECTION_ACTIVITY_STATE";
		
		public static final String EXTRA_CONNECTION = APP_PACKAGE + ".EXTRA_HOST";
		
		public static final String EXTRA_IS_WAN = APP_PACKAGE + ".EXTRA_CONNECTION_MODE";
		
		public static final String EXTRA_TYPE = APP_PACKAGE + ".type";
		
	}
	
	public static final class Actions {
		
		public static final String ACTION_CONNECTIVITY_CHANGE = "android.net.conn.CONNECTIVITY_CHANGE";
		
	}

}

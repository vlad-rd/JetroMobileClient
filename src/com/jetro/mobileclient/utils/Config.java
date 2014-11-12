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
	
	public static class Extras {
		
		public static final String EXTRA_CONNECTION_ACTIVITY_STATE = APP_PACKAGE + ".EXTRA_CONNECTION_ACTIVITY_STATE";
		
		public static final String EXTRA_CONNECTIONS_POINTS = APP_PACKAGE + ".EXTRA_CONNECTIONS_POINTS";
		
		public static final String EXTRA_TYPE = APP_PACKAGE + ".type";
		
	}
	
	public static final class Actions {
		
		public static final String ACTION_CONNECTIVITY_CHANGE = "android.net.conn.CONNECTIVITY_CHANGE";
		
	}

}

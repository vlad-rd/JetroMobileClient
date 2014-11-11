/**
 * 
 */
package com.jetro.mobileclient.utils;

import android.content.Context;
import android.net.ConnectivityManager;

/**
 * @author ran.h
 *
 */
public class ConnectivityUtils {
	
	private ConnectivityUtils() {
	}
	
	public static boolean isNetworkConnected(Context context) {
		ConnectivityManager cm = (ConnectivityManager) context
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		return  cm != null && cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().isConnected();
	}
	
}

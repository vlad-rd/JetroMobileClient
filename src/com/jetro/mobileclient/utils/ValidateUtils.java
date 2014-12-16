/**
 * 
 */
package com.jetro.mobileclient.utils;

import android.text.TextUtils;

/**
 * @author ran.h
 *
 */
public class ValidateUtils {
	
	private ValidateUtils() {
	}
	
	public static boolean isEmailValid(String email) {
		return TextUtils.isEmpty(email)
				|| android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
	}
	
	public static boolean isIpValid(String ip) {
		return !TextUtils.isEmpty(ip)
				&& android.util.Patterns.IP_ADDRESS.matcher(ip).matches();
	}
	
	public static boolean isPortValid(String port) {
		return !TextUtils.isEmpty(port)
				&& TextUtils.isDigitsOnly(port)
				&& Integer.valueOf(port) <= 65535;
	}

}

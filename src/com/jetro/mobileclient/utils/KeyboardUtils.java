/**
 * 
 */
package com.jetro.mobileclient.utils;

import android.content.Context;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

/**
 * @author ran.h
 *
 */
public class KeyboardUtils {
	
	private KeyboardUtils() {
	}
	
	public static void show(Context context, View view, int showFlags) {
		InputMethodManager imm = (InputMethodManager) context
				.getSystemService(Context.INPUT_METHOD_SERVICE);
		if (imm != null) {
			imm.showSoftInput(view, showFlags);
		}
	}
	
	public static void hide(Context context, View view, int hideFlags) {
		InputMethodManager imm = (InputMethodManager) context
				.getSystemService(Context.INPUT_METHOD_SERVICE);
		if (imm != null) {
			imm.hideSoftInputFromWindow(view.getWindowToken(), hideFlags);
		}
	}

}

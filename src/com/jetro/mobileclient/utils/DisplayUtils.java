/**
 * 
 */
package com.jetro.mobileclient.utils;

import android.app.Activity;
import android.util.DisplayMetrics;
import android.view.Display;

/**
 * @author ran.h
 *
 */
public class DisplayUtils {
	
	private DisplayUtils() {
	}
	
	public static DisplayMetrics getDisplayMetrics(Activity activity) {
		DisplayMetrics metrics = new DisplayMetrics();
		Display display = activity.getWindowManager().getDefaultDisplay();
		display.getMetrics(metrics);
		return metrics;
	}

}

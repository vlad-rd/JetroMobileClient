/**
 * 
 */
package com.jetro.mobileclient.utils;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.jetro.mobileclient.application.GlobalApp;

/**
 * @author ran.h
 *
 */
public class FilesUtils {
	
	private static final String TAG = FilesUtils.class.getSimpleName();
	
	private static final int NOT_FOUND = -1;
	
	private FilesUtils() {
	}
	
	public static String getFileName(String filePath, String delimeter) {
		if (filePath == null || delimeter == null) {
			return null;
		}
		int index = filePath.lastIndexOf(delimeter);
		if (index == NOT_FOUND) {
			return filePath;
		}
		return filePath.substring(index+1);
	}
	
	public static Bitmap readImage(String filename) {
		FileInputStream openFileInput = null;
		try {
			openFileInput = GlobalApp.getContext().openFileInput(filename);
			return BitmapFactory.decodeStream(openFileInput);
		} catch (FileNotFoundException e) {
			Log.e(TAG, "ERROR: ", e);
		} finally {
			if (openFileInput != null) {
				try {
					openFileInput.close();
				} catch (IOException e) {
				}
			}
		}
		return null;
	}
	
	public static void writeImage(String filename, Bitmap image) {
		FileOutputStream openFileOutput = null;
		try {
			openFileOutput = GlobalApp.getContext().openFileOutput(filename, Context.MODE_PRIVATE);
			image.compress(Bitmap.CompressFormat.PNG, 100, openFileOutput);
		} catch (FileNotFoundException e) {
			Log.e(TAG, "ERROR: ", e);
		} finally {
			if (openFileOutput != null) {
				try {
					openFileOutput.close();
				} catch (IOException e) {
				}
			}
		}
	}

}

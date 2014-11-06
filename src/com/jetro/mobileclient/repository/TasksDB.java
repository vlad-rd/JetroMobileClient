package com.jetro.mobileclient.repository;

import com.freerdp.freerdpcore.sharedobjects.Task;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

// TODO: check if needed;
public class TasksDB {
	
	private static final String TAG = TasksDB.class.getSimpleName();
	
	private static final String TASKS_DB = "tasks_db";
	
	private static SharedPreferences tasks;
	
	private static TasksDB instance;
	
	private TasksDB(Context context) {
		Log.d(TAG, "AppsDB#AppsDB(...) ENTER");
		
		tasks = context.getSharedPreferences(TASKS_DB, Context.MODE_PRIVATE);
	}
	
	public static TasksDB getInstance(Context context) {
		Log.d(TAG, "AppsDB#getInstance(...) ENTER");
		
		if (instance == null) {
			instance = new TasksDB(context);
		}
		
		return instance;
	}

}

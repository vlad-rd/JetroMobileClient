/**
 * 
 */
package com.jetro.mobileclient.model.helpers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * @author ran.h
 *
 */
public class GsonHelper {
	
	private static Gson gson = null;
	
	private GsonHelper() {
		initGson();
	}
	
	private static final class SingletonHolder {
		private static final GsonHelper INSTANCE = new GsonHelper(); 
	}
	
	public static GsonHelper getInstance() {
		return SingletonHolder.INSTANCE;
	}
	
	public Gson getGson() {
		return gson;
	}
	
	private void initGson() {
		gson = new GsonBuilder().setPrettyPrinting().create();
	}

}

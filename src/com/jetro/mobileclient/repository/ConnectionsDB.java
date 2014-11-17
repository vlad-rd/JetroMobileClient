package com.jetro.mobileclient.repository;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.jetro.mobileclient.model.beans.Host;

public class ConnectionsDB {
	
	private static final String TAG = ConnectionsDB.class.getSimpleName();

	private final static String HOSTS_DB = "hosts_db";
	private final static String HOSTS_NAMES_LIST = "hosts_names_list";
	
	private SharedPreferences hosts;
	private Gson gson;
	
	private static ConnectionsDB instance;

	private ConnectionsDB(Context context) {
		Log.d(TAG, TAG + "#ConnectionsDB(...) ENTER");
		
		hosts = context.getSharedPreferences(HOSTS_DB, Context.MODE_PRIVATE);
		gson = new Gson();
	}

	public static ConnectionsDB getInstance(Context context) {
		Log.d(TAG, TAG + "#getInstance(...) ENTER");
		
		if (instance == null) {
			instance = new ConnectionsDB(context);
		}

		return instance;
	}

	public void saveHost(Host host) {
		Log.d(TAG, TAG + "#saveHost(...) ENTER");
		Log.i(TAG, TAG + "#saveHost(...) host: " + host);
		
		String hostName = host.getHostName();
		
		Editor edit = hosts.edit();
		// Save the host as JSON
		edit.putString(hostName, host.toString());
		// Save the host name
		Set<String> hostsNamesSet = getHostsNamesList();
		hostsNamesSet.add(hostName);
		edit.putStringSet(HOSTS_NAMES_LIST, hostsNamesSet);
		edit.commit();
	}
	
	public Host getHost(String hostName) {
		Log.d(TAG, TAG + "#getHost(...) ENTER");
		
		String hostJson = hosts.getString(hostName, null);
		try {
			return gson.fromJson(hostJson, Host.class);
		} catch (JsonSyntaxException e) {
			Log.e(TAG, "ERROR: ", e);
		}
		return null;
	}
	
	public boolean hasHosts() {
		Log.d(TAG, TAG + "#hasHosts(...) ENTER");
		
		return !getHostsNamesList().isEmpty();
	}
	
	public List<Host> getAllHosts() {
		Log.d(TAG, TAG + "#getAllHosts(...) ENTER");
		
		ArrayList<Host> hosts = new ArrayList<Host>();
		
		Set<String> hostsNamesSet = getHostsNamesList();
		for (String hostName: hostsNamesSet) {
			hosts.add( getHost(hostName) );
		}
		
		return hosts;
	}

	public void deleteHost(String hostName) {
		Log.d(TAG, TAG + "#deleteHost(...) ENTER");
		
		// Deletes host from hosts
		Editor edit = hosts.edit();
		edit.remove(hostName);
		edit.commit();
		// Deletes host name from hosts names
		Set<String> hosts = getHostsNamesList();
		hosts.remove(hostName);
		edit.putStringSet(HOSTS_NAMES_LIST, hosts);
		edit.commit();
	}

	public boolean isDBEmpty() {
		Log.d(TAG, "ConnectionsDB#isDBEmpty(...) ENTER");
		
		return instance != null && hosts != null
				&& getHostsNamesList().size() == 0;		
	}
	
	private Set<String> getHostsNamesList() {
		Log.d(TAG, TAG + "#getHostsNamesList(...) ENTER");
		
		return hosts.getStringSet(HOSTS_NAMES_LIST, new HashSet<String>());
	}
}

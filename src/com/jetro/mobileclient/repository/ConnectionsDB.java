package com.jetro.mobileclient.repository;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.text.TextUtils;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.jetro.mobileclient.model.beans.Connection;
import com.jetro.mobileclient.model.helpers.GsonHelper;

public class ConnectionsDB {
	
	private static final String TAG = ConnectionsDB.class.getSimpleName();

	private final static String CONNECTIONS_DB = "connections_db";
	private final static String CONNECTIONS_NAMES = "connections_names";
	
	private SharedPreferences connections;
	
	private static ConnectionsDB instance;

	private ConnectionsDB(Context context) {
		Log.d(TAG, TAG + "#ConnectionsDB(...) ENTER");
		
		connections = context.getSharedPreferences(CONNECTIONS_DB, Context.MODE_PRIVATE);
	}

	public static ConnectionsDB getInstance(Context context) {
		Log.d(TAG, TAG + "#getInstance(...) ENTER");
		
		if (instance == null) {
			instance = new ConnectionsDB(context);
		}

		return instance;
	}
	
	public boolean isDBEmpty() {
		Log.d(TAG, "ConnectionsDB#isDBEmpty(...) ENTER");
		
		return instance != null && connections != null
				&& getConnectionsNames().size() == 0;		
	}
	
	public boolean hasConnections() {
		Log.d(TAG, TAG + "#hasConnections(...) ENTER");
		
		return !getConnectionsNames().isEmpty();
	}

	public void saveConnection(Connection connection) {
		Log.d(TAG, TAG + "#saveConnection(...) ENTER");
		Log.i(TAG, TAG + "#saveConnection(...) connection: " + connection);
		
		String connectionName = connection.getName();
		
		Editor edit = connections.edit();
		// Saves the connection as JSON
		edit.putString(connectionName, connection.toString());
		// Saves the connection name
		Set<String> connectionsNames = getConnectionsNames();
		connectionsNames.add(connectionName);
		edit.putStringSet(CONNECTIONS_NAMES, connectionsNames);
		edit.commit();
	}
	
	public void deleteConnection(String connectionName) {
		Log.d(TAG, TAG + "#deleteConnection(...) ENTER");
		
		// Validates parameter
		if (TextUtils.isEmpty(connectionName)) {
			return;
		}
		
		// Deletes connection name from connections names
		Set<String> connectionsNames = getConnectionsNames();
		connectionsNames.remove(connectionName);
		
		// Deletes connection from connections
		Editor edit = connections.edit();
		edit.remove(connectionName);
		edit.putStringSet(CONNECTIONS_NAMES, connectionsNames);
		edit.commit();
	}
	
	public Connection getConnection(String connectionName) {
		Log.d(TAG, TAG + "#getConnection(...) ENTER");
		
		String connectionJson = connections.getString(connectionName, null);
		try {
			Gson gson = GsonHelper.getInstance().getGson();
			return gson.fromJson(connectionJson, Connection.class);
		} catch (JsonSyntaxException e) {
			Log.e(TAG, "ERROR: ", e);
		}
		return null;
	}
	
	public List<Connection> getAllConnections() {
		Log.d(TAG, TAG + "#getAllConnections(...) ENTER");
		
		ArrayList<Connection> connections = new ArrayList<Connection>();
		
		Set<String> connectionsNames = getConnectionsNames();
		for (String connectionName: connectionsNames) {
			Connection connection = getConnection(connectionName);
			connections.add(connection);
		}
		
		return connections;
	}
	
	private Set<String> getConnectionsNames() {
		Log.d(TAG, TAG + "#getConnectionsNamesList(...) ENTER");
		
		return connections.getStringSet(CONNECTIONS_NAMES, new HashSet<String>());
	}
}

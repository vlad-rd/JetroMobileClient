package com.jetro.mobileclient.repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.text.TextUtils;
import android.util.Log;

import com.freerdp.freerdpcore.sharedobjects.ConnectionPoint;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class ConnectionsDB {
	
	private static final String TAG = ConnectionsDB.class.getSimpleName();

	private final static String CONNECTIONS_DB = "con_db";
	private final static String HOSTS_NAME_LIST = "host_names_list";
	private static SharedPreferences connections;
	public static ConnectionsDB instance;

	private ConnectionsDB(Context context) {
		Log.d(TAG, "ConnectionsDB#ConnectionsDB(...) ENTER");
		
		connections = context.getSharedPreferences(CONNECTIONS_DB,
				Context.MODE_PRIVATE);
	}

	public static ConnectionsDB getConnectionsDB(Context context) {
		Log.d(TAG, "ConnectionsDB#getConnectionsDB(...) ENTER");
		
		if (instance == null) {
			instance = new ConnectionsDB(context);
		}

		return instance;
	}

	public static void saveNewConnection(String hostName, ConnectionPoint[] cps) {
		Log.d(TAG, "ConnectionsDB#saveNewConnection(...) ENTER");

		for (int i = 0; i < cps.length; i++) {
			Log.i(TAG, "ConnectionsDB#saveNewConnection(...) connection point: " + cps[i].getIP() + ":" + cps[i].getPort());
		}

		Editor edit = connections.edit();

		edit.putString(hostName,
				convertConnectionPointToString(hostName, cps));
		edit.commit();
		addHostToHostsNamesList(hostName);
	}

	private static void addHostToHostsNamesList(String hostName) {
		Log.d(TAG, "ConnectionsDB#addHostToHostsNamesList(...) ENTER");
		
		Set<String> list = getHostsNamesList();
		list.add(hostName);
		Editor edit = connections.edit();
		edit.putStringSet(HOSTS_NAME_LIST, list);
		edit.commit();
	}

	private static Set<String> getHostsNamesList() {
		Log.d(TAG, "ConnectionsDB#getHostsNamesList(...) ENTER");
		
		return connections.getStringSet(HOSTS_NAME_LIST, new HashSet<String>());
	}

	/**
	 * convert the connection points to json structure and save it ad string
	 * 
	 * @param cp
	 *            - the connection point object
	 * @return - json structure strings representation
	 */
	public static String convertConnectionPointToString(String hostName, ConnectionPoint[] cp) {
		Log.d(TAG, "ConnectionsDB#convertConnectionPointToString(...) ENTER");

		JsonObject toString = new JsonObject();
		JsonArray array = new JsonArray();
		for (int i = 0; i < cp.length; i++) {
			JsonObject o = new JsonObject();
			o.addProperty("WAN", cp[i].isWAN());
			o.addProperty("IP", cp[i].getIP());
			o.addProperty("Port", cp[i].getPort());
			o.addProperty("Name", hostName);
			o.addProperty("SSL", cp[i].isSSL());
			o.addProperty("UserName", cp[i].getUserName());
			o.addProperty("Domain", cp[i].getDomain());
			o.addProperty("ConnectionMode", cp[i].getConnectionMode());

			array.add(o);
		}
		toString.add(hostName, array);
		
		Log.i(TAG, "ConnectionsDB#convertConnectionPointToString(...) connection point json: " + toString);

		return toString.toString();
	}

	public static HashMap<String, ArrayList<ConnectionPoint>> getConnectionsPoints(String hostName) {
		Log.d(TAG, "ConnectionsDB#getConnectionPoint(...) ENTER");

		HashMap<String, ArrayList<ConnectionPoint>> map = null;
		ArrayList<ConnectionPoint> cps = new ArrayList<ConnectionPoint>();

		String cpsStr = connections.getString(hostName, null);

		if (!TextUtils.isEmpty(cpsStr)) {
			try {
				JSONObject jsonObject = new JSONObject(cpsStr);
				JSONArray jsonArray = jsonObject.getJSONArray(hostName);
				for (int i = 0; i < jsonArray.length(); i++) {
					Gson g = new Gson();
					ConnectionPoint cp = g.fromJson(jsonArray.get(i).toString(), ConnectionPoint.class);
					cps.add(cp);
				}
			} catch (JSONException e) {
				e.printStackTrace();
			}
			map = new HashMap<String, ArrayList<ConnectionPoint>>();
			map.put(hostName, cps);
		}
		
		return map;
	}

	public static ArrayList<HashMap<String, ArrayList<ConnectionPoint>>> getAllSavedConnections() {
		Log.d(TAG, "ConnectionsDB#getAllSavedConnections(...) ENTER");
		
		ArrayList<HashMap<String, ArrayList<ConnectionPoint>>> allPoints = new ArrayList<HashMap<String, ArrayList<ConnectionPoint>>>();
		Set<String> names = getHostsNamesList();
		for (String host : names) {
			allPoints.add(getConnectionsPoints(host)); 
		}
		return allPoints;
	}

	public static void saveCredentialsForExistingConnection(String hostName, String userName, String domain) {
		Log.d(TAG, "ConnectionsDB#saveCredentialsForExistingConnection(...) ENTER");

		HashMap<String, ArrayList<ConnectionPoint>> cps = getConnectionsPoints(hostName);
		if (cps != null) {
			for (int i = 0; i < cps.get(hostName).size(); i++) {
				ConnectionPoint cp = cps.get(hostName).get(i);
				cp.setUserName(userName);
				cp.setDomain(domain);
			}
			saveNewConnection(hostName, toArray(cps.get(hostName)));
		}
	}

	public static void deleteConnectionPoint(String hostName) {
		Log.d(TAG, "ConnectionsDB#deleteConnectionPoint(...) ENTER");
		
		Editor edit = connections.edit();
		edit.remove(hostName);
		edit.commit();
		
		Set<String> hosts = connections.getStringSet(HOSTS_NAME_LIST, null);
		if (hosts != null && hosts.size() > 0) {
			hosts.remove(hostName);
		}
		edit.putStringSet(HOSTS_NAME_LIST, hosts);
		edit.commit();
	}

	private static ConnectionPoint[] toArray(ArrayList<ConnectionPoint> cps) {
		Log.d(TAG, "ConnectionsDB#toArray(...) ENTER");
		
		ConnectionPoint[] array = new ConnectionPoint[cps.size()];
		for (int i = 0; i < array.length; i++) {
			array[i] = cps.get(i);
		}
		return array;
	}

	public static boolean isDBEmpty() {
		Log.d(TAG, "ConnectionsDB#isDBEmpty(...) ENTER");
		
		Log.i(TAG, "ConnectionsDB#isDBEmpty(...) is empty " + (getHostsNamesList().size() == 0));
		
		return getHostsNamesList().size() == 0;		
	}
}

/**
 * 
 */
package com.jetro.mobileclient.model.beans;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import com.google.gson.Gson;
import com.jetro.mobileclient.model.helpers.GsonHelper;
import com.jetro.protocol.Protocols.Controller.ConnectionPoint;

/**
 * @author ran.h
 *
 */
public class Host implements Serializable {
	
	private static final long serialVersionUID = 1188781886078062688L;

	private String hostName;
	
	private String userName;
	
	private String password;
	
	private String domain;
	
	private Set<ConnectionPoint> LANs = new HashSet<ConnectionPoint>();
	
	private Set<ConnectionPoint> WANs = new HashSet<ConnectionPoint>();

	public Host() {
		super();
	}

	public String getHostName() {
		return hostName;
	}

	public void setHostName(String hostName) {
		this.hostName = hostName;
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getDomain() {
		return domain;
	}

	public void setDomain(String domain) {
		this.domain = domain;
	}

	public Set<ConnectionPoint> getLANs() {
		return LANs;
	}

	public void setLANs(Set<ConnectionPoint> lANs) {
		LANs = lANs;
	}

	public Set<ConnectionPoint> getWANs() {
		return WANs;
	}

	public void setWANs(Set<ConnectionPoint> wANs) {
		WANs = wANs;
	}

	public Set<ConnectionPoint> getConnectionPoints() {
		final Set<ConnectionPoint> cps = new HashSet<ConnectionPoint>();
		cps.addAll(getLANs());
		cps.addAll(getWANs());
		return cps;
	}
	
	public boolean addConnectionPoint(ConnectionPoint connectionPoint) {
		boolean isWan = connectionPoint.WAN;
		if (isWan) {
			return WANs.add(connectionPoint);
		} else {
			return LANs.add(connectionPoint);
		}
	}
	
	public boolean removeConnectionPoint(ConnectionPoint connectionPoint) {
		boolean isWan = connectionPoint.WAN;
		if (isWan) {
			return WANs.remove(connectionPoint);
		} else {
			return LANs.remove(connectionPoint);
		}
	}

	@Override
	public String toString() {
		Gson gson = GsonHelper.getInstance().getGson();
		return gson.toJson(this);
//		JsonObject hostJsonObject = new JsonObject();
//		hostJsonObject.addProperty("hostName", hostName);
//		hostJsonObject.addProperty("userName", userName);
//		hostJsonObject.addProperty("password", password);
//		hostJsonObject.addProperty("domain", domain);
//		hostJsonObject.addProperty("LANs", getConnectionPoints(LANs));
//		hostJsonObject.addProperty("WANs", getConnectionPoints(WANs));
//		return hostJsonObject.toString();
	}
	
//	private String getConnectionPoints(Set<ConnectionPoint> connectionPoints) {
//		JsonArray connectionsJsonArray = new JsonArray();
//		for (ConnectionPoint cp : connectionPoints) {
//			JsonObject cpJsonObject = new JsonObject();
//			cpJsonObject.addProperty("IP", cp.IP);
//			cpJsonObject.addProperty("Port", cp.Port);
//			cpJsonObject.addProperty("WAN", cp.WAN);
//			cpJsonObject.addProperty("SSL", cp.SSL);
//			connectionsJsonArray.add(cpJsonObject);
//		}
//		return connectionsJsonArray.toString();
//	}
	
}

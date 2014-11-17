/**
 * 
 */
package com.jetro.mobileclient.model.beans;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.jetro.protocol.Protocols.Controller.ConnectionPoint;

/**
 * @author ran.h
 *
 */
public class Host implements Serializable {
	
	private String hostName;
	
	private String userName;
	
	private String password;
	
	private String domain;
	
	private Set<ConnectionPoint> lanConnectionPoints = new HashSet<ConnectionPoint>();
	
	private Set<ConnectionPoint> wanConnectionPoints = new HashSet<ConnectionPoint>();

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

	public Set<ConnectionPoint> getLanConnectionPoints() {
		return lanConnectionPoints;
	}

	public void setLanConnectionPoints(Set<ConnectionPoint> lanConnectionPoints) {
		this.lanConnectionPoints = lanConnectionPoints;
	}

	public Set<ConnectionPoint> getWanConnectionPoints() {
		return wanConnectionPoints;
	}

	public void setWanConnectionPoints(Set<ConnectionPoint> wanConnectionPoints) {
		this.wanConnectionPoints = wanConnectionPoints;
	}
	
	public Set<ConnectionPoint> getConnectionPoints() {
		final Set<ConnectionPoint> cps = new HashSet<ConnectionPoint>();
		cps.addAll(getLanConnectionPoints());
		cps.addAll(getWanConnectionPoints());
		return cps;
	}
	
	public boolean addConnectionPoint(ConnectionPoint connectionPoint) {
		boolean isWan = connectionPoint.WAN;
		if (isWan) {
			return wanConnectionPoints.add(connectionPoint);
		} else {
			return lanConnectionPoints.add(connectionPoint);
		}
	}
	
	public boolean removeConnectionPoint(ConnectionPoint connectionPoint) {
		boolean isWan = connectionPoint.WAN;
		if (isWan) {
			return wanConnectionPoints.remove(connectionPoint);
		} else {
			return lanConnectionPoints.remove(connectionPoint);
		}
	}

	@Override
	public String toString() {
		JsonObject hostJsonObject = new JsonObject();
		hostJsonObject.addProperty("Name", hostName);
		hostJsonObject.addProperty("UserName", userName);
		hostJsonObject.addProperty("Password", password);
		hostJsonObject.addProperty("Domain", domain);
		hostJsonObject.addProperty("LANs", getConnectionPoints(lanConnectionPoints));
		hostJsonObject.addProperty("WANs", getConnectionPoints(wanConnectionPoints));
		return hostJsonObject.toString();
	}
	
	private String getConnectionPoints(Set<ConnectionPoint> connectionPoints) {
		JsonArray connectionsJsonArray = new JsonArray();
		for (ConnectionPoint cp : connectionPoints) {
			JsonObject cpJsonObject = new JsonObject();
			cpJsonObject.addProperty("IP", cp.IP);
			cpJsonObject.addProperty("Port", cp.Port);
			cpJsonObject.addProperty("WAN", cp.WAN);
			cpJsonObject.addProperty("SSL", cp.SSL);
			connectionsJsonArray.add(cpJsonObject);
		}
		return connectionsJsonArray.toString();
	}
	
}

/**
 * 
 */
package com.jetro.mobileclient.utils;

import java.util.HashSet;
import java.util.Set;

import android.content.Context;
import android.util.Log;

import com.jetro.mobileclient.model.beans.Connection;
import com.jetro.mobileclient.repository.ConnectionsDB;
import com.jetro.protocol.Core.IConnectionCreationSubscriber;
import com.jetro.protocol.Core.IMessageSubscriber;
import com.jetro.protocol.Core.Net.ClientChannel;
import com.jetro.protocol.Protocols.Controller.ConnectionPoint;
import com.jetro.protocol.Protocols.Controller.ConnectionPoint.ConnectionModeType;

/**
 * @author ran.h
 *
 */
public class ClientChannelUtils {
	
	private static final String TAG = ClientChannelUtils.class.getSimpleName();
	
	public static final int TIMES_TO_TRY = 3;

	private ClientChannelUtils() {
	}
	
	public static boolean connect(Context context, Connection connection,
			ConnectionModeType connectionMode, int timesToTry) {
		Log.d(TAG, TAG + "#connect(...) ENTER");
		
		boolean isCreated = false;
		
		Set<ConnectionPoint> connectionPoints = new HashSet<ConnectionPoint>();
		switch (connectionMode) {
		case DIRECT:
			connectionPoints = connection.getLANs();
			break;
		case SSL:
			connectionPoints = connection.getWANs();
			break;
		}
		
		for (ConnectionPoint cp : connectionPoints) {
			int tries = 0;
			do {
				Log.i(TAG, TAG + "#connect(...) Connecting try #" + tries);
				Log.i(TAG, TAG + "#connect(...) Connecting to HOST IP: " + cp.IP);
				Log.i(TAG, TAG + "#connect(...) Connecting to HOST PORT: " + cp.Port);
				Log.i(TAG, TAG + "#connect(...) Connecting to CONNECTION MODE: " + cp.ConnectionMode);
				try {
					switch (connectionMode) {
					case DIRECT:					
						isCreated = ClientChannel.Create(cp.IP, cp.Port, ClientChannel.TIME_OUT, null);
						break;
					case SSL:
						isCreated = ClientChannel.CreateSSL(cp.IP, cp.Port, ClientChannel.TIME_OUT, null);
						break;
					}
				} catch (Exception e) {
					Log.e(TAG, "ERROR: ", e);
				}
			} while(!isCreated && ++tries < timesToTry);
			if (isCreated) {
				// Saves this connection point as last used one
				connection.setLastConnectionPoint(cp);
				ConnectionsDB.getInstance(context).saveConnection(connection);
				break;
			}
		}
		
		return isCreated;
	}
	
	public static boolean createClientChannel(
			String hostIp, int hostPort,
			ConnectionModeType connectionMode,
			IConnectionCreationSubscriber connectionCreationSubscriber) {
		boolean isCreated = false;
		try {
			switch (connectionMode) {
			case DIRECT:
				isCreated = ClientChannel.Create(hostIp, hostPort, ClientChannel.TIME_OUT, connectionCreationSubscriber);
				break;
			case SSL:
				isCreated = ClientChannel.CreateSSL(hostIp, hostPort, ClientChannel.TIME_OUT, connectionCreationSubscriber);
				break;
			}
		} catch (InterruptedException e) {
			Log.e(TAG, "ERROR: ", e);
		}
		return isCreated;
	}
	
	public static void stopClientChannel(ClientChannel clientChannel, IMessageSubscriber messageSubscriber) {
		// free client channel
		if (clientChannel != null) {
			clientChannel.RemoveListener(messageSubscriber);
			clientChannel.Stop(null);
			clientChannel = null;
		}
	}

}

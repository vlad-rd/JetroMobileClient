/**
 * 
 */
package com.jetro.mobileclient.utils;

import java.util.Set;

import android.util.Log;

import com.jetro.mobileclient.model.beans.Connection;
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
	
	public static boolean connect(Connection connection, ConnectionModeType connectionMode, int timesToTry) {
		Log.d(TAG, TAG + "#connect(...) ENTER");
		
		boolean isCreated = false;
		
		try {
			switch (connectionMode) {
			case DIRECT:
				Set<ConnectionPoint> lans = connection.getLANs();
				for (ConnectionPoint lan : lans) {
					int tries = 0;
					do {
						Log.i(TAG, TAG + "#connect(...) Connecting try #" + tries);
						Log.i(TAG, TAG + "#connect(...) Connecting to HOST IP: " + lan.IP);
						Log.i(TAG, TAG + "#connect(...) Connecting to HOST PORT: " + lan.Port);
						Log.i(TAG, TAG + "#connect(...) Connecting to CONNECTION MODE: " + lan.ConnectionMode);
						isCreated = ClientChannel.Create(lan.IP, lan.Port, ClientChannel.TIME_OUT, null);
					} while(!isCreated && ++tries < timesToTry);
					if (isCreated) {
						break;
					}
				}
				break;
			case SSL:
				Set<ConnectionPoint> wans = connection.getWANs();
				for (ConnectionPoint wan : wans) {
					int tries = 0;
					do {
						Log.i(TAG, TAG + "#connect(...) Connecting try #" + tries);
						Log.i(TAG, TAG + "#connect(...) Connecting to HOST IP: " + wan.IP);
						Log.i(TAG, TAG + "#connect(...) Connecting to HOST PORT: " + wan.Port);
						Log.i(TAG, TAG + "#connect(...) Connecting to CONNECTION MODE: " + wan.ConnectionMode);
						isCreated = ClientChannel.CreateSSL(wan.IP, wan.Port, ClientChannel.TIME_OUT, null);
					} while(!isCreated && ++tries < timesToTry);
					if (isCreated) {
						break;
					}
				}
				break;
			}
		} catch (InterruptedException e) {
			Log.e(TAG, "ERROR: ", e);
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

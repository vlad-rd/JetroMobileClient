/**
 * 
 */
package com.jetro.mobileclient.utils;

import android.util.Log;

import com.jetro.protocol.Core.IConnectionCreationSubscriber;
import com.jetro.protocol.Core.IMessageSubscriber;
import com.jetro.protocol.Core.Net.ClientChannel;
import com.jetro.protocol.Protocols.Controller.ConnectionPoint.ConnectionModeType;

/**
 * @author ran.h
 *
 */
public class ClientChannelUtils {
	
	private static final String TAG = ClientChannelUtils.class.getSimpleName();

	private ClientChannelUtils() {
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

/**
 * 
 */
package com.jetro.mobileclient.utils;

import JProtocol.Core.BaseMsg;
import JProtocol.Core.IMessageSubscriber;
import JProtocol.Core.Net.ClientChannel;
import android.os.AsyncTask;

/**
 * @author ran.h
 *
 */
// TODO: check if needed
public class SendReceiveAsyncTask extends AsyncTask<Params<BaseMsg, Integer, IMessageSubscriber>, Void, BaseMsg> {

	@Override
	protected BaseMsg doInBackground(Params<BaseMsg, Integer, IMessageSubscriber>... params) {
		// Validates arguments
		if (params == null) {
			return null;
		}
		if (params[0].msg == null || params[0].timeout < 0) {
			return null;
		}
		
		ClientChannel channel = ClientChannel.getInstance();
		return channel.SendReceive(params[0].msg, params[0].timeout);
	}

	@Override
	protected void onPostExecute(BaseMsg result) {
	}

}

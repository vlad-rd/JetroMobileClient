package com.jetro.mobileclient.ui;

import com.freerdp.freerdpcore.sharedobjects.utils.Constants;
import com.jetro.mobileclient.repository.ConnectionsDB;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.PopupMenu;

public class BaseActivity extends Activity {
	
	private static final String TAG = BaseActivity.class.getSimpleName();
	
	PopupMenu popupMenu;
	protected ConnectionsDB connectionsDB;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.d(TAG, "onCreate(...) ENTER");
		super.onCreate(savedInstanceState);
		
		connectionsDB = ConnectionsDB.getConnectionsDB(this);
	}

	public void showPopup() {
		Log.d(TAG, "showPopup(...) ENTER");
		
		new AlertDialog.Builder(this)
				.setMessage(Constants.POPUP_EXIT_MESSAGE)
				.setCancelable(false)
				.setPositiveButton(Constants.YES_MESSAGE,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								BaseActivity.this.finish();
							}
						}).setNegativeButton(Constants.NO_MESSAGE, null).show();

	}

	public void open(View view) {
		Log.d(TAG, "open(...) ENTER");
	}

	/**
	 * indicates on the current activity mode.
	 * 
	 * @author idanb
	 * 
	 */
	protected enum ConnectionActivityMode {

		AddConnection(0), Login(1), ResetPassword(2), ViewConnection(3), About(4);

		ConnectionActivityMode(int i) {
			this.type = i;
		}

		private int type;

		public int getNumericType() {
			return type;
		}
	}
}

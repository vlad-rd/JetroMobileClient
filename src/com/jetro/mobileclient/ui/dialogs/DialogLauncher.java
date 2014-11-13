/**
 * 
 */
package com.jetro.mobileclient.ui.dialogs;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;

import com.jetro.mobileclient.R;

/**
 * @author ran.h
 *
 */
public class DialogLauncher {
	
	private DialogLauncher() {
	}
	
	public static void launchCancelLoginDialog(Activity activity,
			DialogInterface.OnClickListener buttonClickListener) {
		new AlertDialog.Builder(activity)
			.setMessage(R.string.dialog_cancel_login_message)
			.setCancelable(false)
			.setPositiveButton(R.string.dialog_cancel_login_positive_text, buttonClickListener)
			.setNegativeButton(R.string.dialog_cancel_login_positive_text, buttonClickListener)
			.show();
	}
	
	public static void launchExitConnectionsListDialog(Activity activity,
			DialogInterface.OnClickListener buttonClickListener) {
		new AlertDialog.Builder(activity)
			.setMessage(R.string.dialog_exit_connections_list_message)
			.setCancelable(false)
			.setPositiveButton(R.string.dialog_exit_connections_list_positive_text, buttonClickListener)
			.setNegativeButton(R.string.dialog_exit_connections_list_negative_text, buttonClickListener)
			.show();
	}

}

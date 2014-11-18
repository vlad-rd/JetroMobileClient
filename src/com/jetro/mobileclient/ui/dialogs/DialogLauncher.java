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
			.setCancelable(false)
			.setMessage(R.string.dialog_cancel_login_message)
			.setPositiveButton(R.string.dialog_cancel_login_positive_text, buttonClickListener)
			.setNegativeButton(R.string.dialog_cancel_login_negative_text, buttonClickListener)
			.show();
	}
	
	public static void launchExitConnectionsListDialog(Activity activity,
			DialogInterface.OnClickListener buttonClickListener) {
		new AlertDialog.Builder(activity)
			.setCancelable(false)
			.setMessage(R.string.dialog_exit_connections_list_message)
			.setPositiveButton(R.string.dialog_exit_connections_list_positive_text, buttonClickListener)
			.setNegativeButton(R.string.dialog_exit_connections_list_negative_text, buttonClickListener)
			.show();
	}
	
	public static void launchDeleteConnectionDialog(Activity activity,
			DialogInterface.OnClickListener buttonClickListener) {
		new AlertDialog.Builder(activity)
				.setCancelable(false)
				.setMessage(R.string.dialog_delete_connection_message)
				.setPositiveButton(R.string.dialog_delete_connection_positive_text, buttonClickListener)
				.setNegativeButton(R.string.dialog_delete_connection_negative_text, buttonClickListener)
				.show();
	}
	
	public static void launchNetworkConnectionIssueDialog(Activity activity,
			DialogInterface.OnClickListener buttonClickListener) {
		new AlertDialog.Builder(activity)
				.setCancelable(false)
				.setMessage(R.string.dialog_network_connection_issue_message)
				.setPositiveButton(R.string.dialog_network_connection_issue_positive_text, buttonClickListener)
				.show();
	}

}

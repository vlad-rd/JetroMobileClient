/**
 * 
 */
package com.jetro.mobileclient.ui.dialogs;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;

import com.jetro.mobileclient.R;

/**
 * @author ran.h
 *
 */
public class DialogLauncher {
	
	private DialogLauncher() {
	}
	
	public static void launchAppExitDialog(final Activity activity) {
		new AlertDialog.Builder(activity)
			.setCancelable(false)
			.setMessage(R.string.dialog_app_exit_message)
			.setPositiveButton(R.string.dialog_app_exit_positive_btn, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					Intent intent = new Intent(Intent.ACTION_MAIN);
					intent.addCategory(Intent.CATEGORY_HOME);
					intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					activity.startActivity(intent);
				}
			})
			.setNegativeButton(R.string.dialog_app_exit_negative_btn, null)
			.show();
	}
	
	public static void launchNoInternetConnection(Activity activity) {
		new AlertDialog.Builder(activity)
			.setCancelable(false)
			.setMessage(R.string.dialog_no_internet_connection_message)
			.setNegativeButton(R.string.dialog_no_internet_connection_negative_btn, null)
			.show();
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
	
	public static void launchConnectionTypesDialog(Activity activity,
			DialogInterface.OnClickListener buttonClickListener) {
		new AlertDialog.Builder(activity)
				.setCancelable(false)
				.setMessage(R.string.dialog_connection_types_message)
				.setPositiveButton(R.string.dialog_connection_types_positive_text, buttonClickListener)
				.setNegativeButton(R.string.dialog_connection_types_negative_text, buttonClickListener)
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
	
	public static void launchServerErrorTwoButtonsDialog(Activity activity,
			String message,
			DialogInterface.OnClickListener buttonClickListener) {
		new AlertDialog.Builder(activity)
		.setCancelable(false)
		.setMessage(message)
		.setPositiveButton(R.string.dialog_server_error_positive_text, buttonClickListener)
		.setNegativeButton(R.string.dialog_server_error_negative_text, buttonClickListener)
		.show();
	}
	
	public static void launchServerErrorOneButtonDialog(Activity activity,
			String message,
			DialogInterface.OnClickListener buttonClickListener) {
		new AlertDialog.Builder(activity)
		.setCancelable(false)
		.setMessage(message)
		.setPositiveButton(R.string.dialog_server_error_normal_text, buttonClickListener)
		.show();
	}

}

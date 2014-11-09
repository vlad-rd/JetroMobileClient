package com.jetro.mobileclient.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import com.freerdp.freerdpcore.domain.BookmarkBase.ScreenSettings;
import com.freerdp.freerdpcore.domain.ManualBookmark;
import com.freerdp.freerdpcore.presentation.SessionActivity;
import com.freerdp.freerdpcore.sharedobjects.Application;
import com.freerdp.freerdpcore.sharedobjects.ISocketListener;
import com.freerdp.freerdpcore.sharedobjects.SocketManager;
import com.freerdp.freerdpcore.sharedobjects.Task;
import com.freerdp.freerdpcore.sharedobjects.controller_messages.GetTsMsg;
import com.freerdp.freerdpcore.sharedobjects.controller_messages.GetTsMsg.GetTsMsgResponse;
import com.freerdp.freerdpcore.sharedobjects.controller_messages.LogoutMsg.LogoutMsgResponse;
import com.freerdp.freerdpcore.sharedobjects.controller_messages.MyApplicationsMsg;
import com.freerdp.freerdpcore.sharedobjects.controller_messages.MyApplicationsMsg.MyAppsResponse;
import com.freerdp.freerdpcore.sharedobjects.controller_messages.ShowTaskListMsg.ShowTaskListMsgResponse;
import com.freerdp.freerdpcore.sharedobjects.protocol.BaseMsg;
import com.freerdp.freerdpcore.sharedobjects.protocol.BaseResponse;
import com.freerdp.freerdpcore.sharedobjects.protocol.MessagesValues;
import com.freerdp.freerdpcore.sharedobjects.utils.Constants;
import com.jetro.mobileclient.R;
import com.jetro.mobileclient.application.GlobalApp;

public class NewDesktopActivity extends BaseActivity implements ISocketListener {

	private static final String TAG = NewDesktopActivity.class.getSimpleName();
	
	private SocketManager socketManager;
	private String ticket;
	private ApplicationsGridAdapter appsAdapter;
	private GridView appsGrid;
	private ImageView _off;
	private ImageView _home;
	private boolean isChecked = false;
	
	private String selectedAppId;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.d(TAG, "onCreate(...) ENTER");
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.new_desktop_activity_layout);
		
		socketManager = GlobalApp.getSocketManager(this);

		appsGrid = (GridView) findViewById(R.id.applicationsGrid);

		_off = (ImageView) findViewById(R.id.disconnectBtn);

		_home = (ImageView) findViewById(R.id.homeBtn);

		// exit from the app
		_off.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (!getStatusDontShowDialog())
					initExitDialog();
				else {
					finish();
				}
			}
		});

		_home.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				showPopupExit(Constants.ARE_YOU_SURE, Constants.CANCEL, Constants.EXIT);
			}
		});
		
		ticket = GlobalApp.getSessionTicket();
		
		sendMyApplicationsMsg(ticket);
	}

	@Override
	protected void onResume() {
		Log.d(TAG, "onResume(...) ENTER");
		super.onResume();
	}
	
	@Override
	public void onBackPressed() {
		Log.d(TAG, "onBackPressed(...) ENTER");
		
		showPopupExit(Constants.POPUP_EXIT_MESSAGE, Constants.YES_MESSAGE, Constants.NO_MESSAGE);
	}
	
	@Override
	protected void onDestroy() {
		Log.d(TAG, "onDestroy(...) ENTER");
		
		super.onDestroy();
		socketManager.closeSocket();
	}

	@Override
	public void OnSocketCreated() {
		Log.d(TAG, "OnSocketCreated(...) ENTER");
	}

	@Override
	public void OnMessageReceived(BaseMsg msg) {
		Log.d(TAG, "OnMessageReceived(...) ENTER");
		
		if (msg == null || msg.getJsonResponse() == null) {
			OnIOError("Message is null");
			return;
		}

		if (msg.getJsonResponse().getDescription() != null) {
		}
		
		switch (msg.extraHeader.MsgClassID) {
		case MessagesValues.ClassID.MyApplicationsMsg:
			MyAppsResponse myAppsResponse = (MyAppsResponse) msg.getJsonResponse();
			appsAdapter = new ApplicationsGridAdapter(myAppsResponse.getApplications());
			appsGrid.setAdapter(appsAdapter);
			appsGrid.setOnItemClickListener(new OnItemClickListener() {
				@Override
				public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
					Application selectedApp = (Application) appsAdapter.getItem(position);
					selectedAppId = selectedApp.getID();
					Log.w(TAG, "Selected App Name: " + selectedApp.getName());
					Log.w(TAG, "Selected App Id: " + selectedAppId);
					sendGetTsMsg(ticket);
				}
			});
			break;
		case MessagesValues.ClassID.ShowTaskListMsg:
			ShowTaskListMsgResponse showTaskListMsgResponse = (ShowTaskListMsgResponse) msg.getJsonResponse();
			Task[] tasks = showTaskListMsgResponse.getTasks();
		case MessagesValues.ClassID.GetTsMsg:
			GetTsMsgResponse getTsResponse = (GetTsMsgResponse) msg.getJsonResponse();
			startJetroSessionActivity(getTsResponse.getAddress(), getTsResponse.getPort(), selectedAppId);
			break;
		case MessagesValues.ClassID.LogoutMsg:
			LogoutMsgResponse logoutMsgRespons = (LogoutMsgResponse) msg.getJsonResponse();
			break;
		case MessagesValues.ClassID.Error:
			BaseResponse baseResponse = (BaseResponse) msg.getJsonResponse();
			break;
		}
	}

	@Override
	public void OnIOError(String exception) {
		Log.d(TAG, "OnIOError(...) ENTER");
		
		finish();
	}
	
	private void sendMyApplicationsMsg(String ticket) {
		Log.d(TAG, "sendMyApplicationsMsg(...) ENTER");
		
		MyApplicationsMsg msg = new MyApplicationsMsg(ticket);
		Log.i(TAG, "Message: " + msg);
		socketManager.sendMessage(msg);
	}
	
	private void sendGetTsMsg(String ticket) {
		Log.d(TAG, "NewDesktopActivity#sendGetTsMsg(...) ENTER");
		
		socketManager.sendMessage(new GetTsMsg(ticket));
	}
	
	private class ApplicationsGridAdapter extends BaseAdapter {

		Application[] apps;
		LayoutInflater inflater;

		public ApplicationsGridAdapter(Application[] apps) {
			this.apps = apps;
			inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}

		@Override
		public int getCount() {
			return this.apps.length;
		}

		@Override
		public Object getItem(int position) {
			return this.apps[position];
		}

		@Override
		public long getItemId(int position) {
			return 0;
		}

		@Override
		public View getView(final int position, View convertView, ViewGroup parent) {
			convertView = inflater.inflate(R.layout.grid_item_layout, null);

			((TextView) convertView.findViewById(R.id.applicationName)).setText(apps[position].getName());

			proccessApplicationIcon(
					((ImageView) convertView.findViewById(R.id.applicationIcon)),
					apps[position].getIcon());
			
			return convertView;
		}
		
		private void proccessApplicationIcon(final ImageView iv, final byte[] iconAsBytes) {
			new AsyncTask<Void, Void, Bitmap>() {

				@Override
				protected Bitmap doInBackground(Void... params) {
					Bitmap result = null;
					try {
						result = BitmapFactory.decodeByteArray(iconAsBytes, 0, iconAsBytes.length);
					} catch (Exception e) {
						Log.e(TAG, "ERROR: ", e);
					}
					return result;
				}

				@Override
				protected void onPostExecute(Bitmap result) {
					iv.setImageBitmap(result);
				};
			}.execute();
		}
	}
	
	private void startJetroSessionActivity(String host, int port, String selectedAppId) {
		Intent intent = new Intent(NewDesktopActivity.this, JetroSessionActivity.class);
		
		ScreenSettings screen_settings = new ScreenSettings();
		screen_settings.setResolution(ScreenSettings.FITSCREEN);
		ManualBookmark bookmark = new ManualBookmark();
		bookmark.setHostname(host);
		bookmark.setPort(port);
		bookmark.setScreenSettings(screen_settings);
		
		Bundle extras = new Bundle();
		extras.putParcelable(SessionActivity.PARAM_JETRO_REFERENCE, bookmark);
		extras.putString("applicationId", selectedAppId);
		intent.putExtras(extras);
		startActivity(intent);
	}

	private void showPopupExit(String st, String but1, String but2) {
		Log.d(TAG, "showPopupExit(...) ENTER");
		
		new AlertDialog.Builder(this)
			.setMessage(st)
			.setCancelable(false)
			.setPositiveButton(but1, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						NewDesktopActivity.this.finish();
					}
				})
			.setNegativeButton(but2, null)
			.show();
	}

	/**
	 * Represent a dialog when exit from the homescreen activity
	 */
	private void initExitDialog() {
		Log.d(TAG, "initExitDialog(...) ENTER");
		
		String MyPREFERENCES = "ExitPopup";
		SharedPreferences sharedpreferences;

		ConnectivityManager manager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
		sharedpreferences = getSharedPreferences(MyPREFERENCES, Context.MODE_PRIVATE);
		final Editor editor = sharedpreferences.edit();

		LayoutInflater inflater = LayoutInflater.from(NewDesktopActivity.this);
		final View dialogView = inflater.inflate(R.layout.home_screen_exit_dialog, null);

		AlertDialog.Builder builder = new AlertDialog.Builder(NewDesktopActivity.this);
		builder.setView(dialogView);
		final AlertDialog alertDialog = builder.create();
		alertDialog.show();

		final CheckBox check = (CheckBox) dialogView.findViewById(R.id.checkBox1);
		check.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				isChecked = check.isChecked();
				// status = isChecked;
				// Do something here.
			}
		});

		Button cancelButton = (Button) dialogView.findViewById(R.id.cancel_button);
		// if decline button is clicked, close the custom dialog
		cancelButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				alertDialog.cancel();
			}
		});
		Button exit = (Button) dialogView.findViewById(R.id.exit_dialog_button_home_screen);
		// add listener to button
		exit.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				if (isChecked) {
					saveStatusDontShowDialog();
				}
				Intent intent = new Intent(NewDesktopActivity.this, ConnectionsListActivity.class);
				startActivity(intent);
				finish();
			}
		});
	}

	private void saveStatusDontShowDialog() {
		Log.d(TAG, "saveStatusDontShowDialog(...) ENTER");
		
		SharedPreferences pref = getApplicationContext().getSharedPreferences("status", MODE_PRIVATE);
		Editor editor = pref.edit();
		editor.putBoolean("status", isChecked);
		editor.commit();
	}

	private boolean getStatusDontShowDialog() {
		Log.d(TAG, "getStatusDontShowDialog(...) ENTER");
		
		SharedPreferences pref = getApplicationContext().getSharedPreferences("status", MODE_PRIVATE);
		return pref.getBoolean("status", false);
	}

}
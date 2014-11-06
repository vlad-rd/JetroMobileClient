/*
   Android Session Activity

   Copyright 2013 Thincast Technologies GmbH, Author: Martin Fleisz

   This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
   If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
*/

package com.jetro.mobileclient.ui;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ZoomControls;

import com.freerdp.freerdpcore.application.GlobalApp;
import com.freerdp.freerdpcore.application.GlobalSettings;
import com.freerdp.freerdpcore.application.SessionState;
import com.freerdp.freerdpcore.domain.BookmarkBase;
import com.freerdp.freerdpcore.domain.BookmarkBase.ScreenSettings;
import com.freerdp.freerdpcore.domain.ConnectionReference;
import com.freerdp.freerdpcore.domain.ManualBookmark;
import com.freerdp.freerdpcore.presentation.ScrollView2D;
import com.freerdp.freerdpcore.services.LibFreeRDP;
import com.freerdp.freerdpcore.sharedobjects.Application;
import com.freerdp.freerdpcore.sharedobjects.ISocketListener;
import com.freerdp.freerdpcore.sharedobjects.SocketManager;
import com.freerdp.freerdpcore.sharedobjects.Task;
import com.freerdp.freerdpcore.sharedobjects.controller_messages.GetTsMsg;
import com.freerdp.freerdpcore.sharedobjects.controller_messages.GetTsMsg.GetTsMsgResponse;
import com.freerdp.freerdpcore.sharedobjects.controller_messages.LogoutMsg.LogoutMsgResponse;
import com.freerdp.freerdpcore.sharedobjects.controller_messages.MyApplicationsMsg;
import com.freerdp.freerdpcore.sharedobjects.controller_messages.MyApplicationsMsg.MyAppsResponse;
import com.freerdp.freerdpcore.sharedobjects.controller_messages.ShowKeyBoardMsg.ShowKeyBoardMsgResponse;
import com.freerdp.freerdpcore.sharedobjects.controller_messages.ShowTaskListMsg.ShowTaskListMsgResponse;
import com.freerdp.freerdpcore.sharedobjects.controller_messages.ShowWindowMsg.ShowWindowMsgResponse;
import com.freerdp.freerdpcore.sharedobjects.controller_messages.StartApplicationMsg;
import com.freerdp.freerdpcore.sharedobjects.controller_messages.StartApplicationMsg.StartApplicationMsgResponse;
import com.freerdp.freerdpcore.sharedobjects.controller_messages.WindowCreatedMsg.WindowCreatedMsgResponse;
import com.freerdp.freerdpcore.sharedobjects.protocol.BaseMsg;
import com.freerdp.freerdpcore.sharedobjects.protocol.BaseResponse;
import com.freerdp.freerdpcore.sharedobjects.protocol.MessagesValues;
import com.freerdp.freerdpcore.sharedobjects.utils.Constants;
import com.freerdp.freerdpcore.utils.ClipboardManagerProxy;
import com.freerdp.freerdpcore.utils.KeyboardMapper;
import com.jetro.mobileclient.R;

public class SessionFragmentActivity extends FragmentActivity 
	implements ISocketListener, DesktopFragment.Listener {

	/**
	 * @author ran.h
	 *
	 */
	private class LibFreeRDPBroadcastReceiver extends BroadcastReceiver
	{		
		@Override
		public void onReceive(Context context, Intent intent) {
			// still got a valid session?
			if (session == null)
				return;
			
			// is this event for the current session?			
			if (session.getInstance() != intent.getExtras().getInt(GlobalApp.EVENT_PARAM, -1))
				return;
			
			switch(intent.getExtras().getInt(GlobalApp.EVENT_TYPE, -1))
			{
				case GlobalApp.FREERDP_EVENT_CONNECTION_SUCCESS:
					OnConnectionSuccess(context);
					break;

				case GlobalApp.FREERDP_EVENT_CONNECTION_FAILURE:
					OnConnectionFailure(context);
					break;
				case GlobalApp.FREERDP_EVENT_DISCONNECTED:
					OnDisconnected(context);
					break;
			}
		}
		
		private void OnConnectionSuccess(Context context)
		{
			Log.v(TAG, "OnConnectionSuccess");

			// bind session
			bindSession();

			if(progressDialog != null)
			{
				progressDialog.dismiss();
				progressDialog = null;
			}
			
			// add hostname to history if quick connect was used
			Bundle bundle = getIntent().getExtras();
			if(bundle != null && bundle.containsKey(PARAM_CONNECTION_REFERENCE))
			{		
				if(ConnectionReference.isHostnameReference(bundle.getString(PARAM_CONNECTION_REFERENCE)))
				{
					assert session.getBookmark().getType() == BookmarkBase.TYPE_MANUAL;
					String item = session.getBookmark().<ManualBookmark>get().getHostname();
					if(!GlobalApp.getQuickConnectHistoryGateway().historyItemExists(item))
						GlobalApp.getQuickConnectHistoryGateway().addHistoryItem(item);					
				}
			}
		}
		
		private void OnConnectionFailure(Context context)
		{
			Log.v(TAG, "OnConnectionFailure");
			
			// remove pending move events
//			uiHandler.removeMessages(UIHandler.SEND_MOVE_EVENT);

			if(progressDialog != null)
			{
				progressDialog.dismiss();
				progressDialog = null;
			}

			// post error message on UI thread
//			if (!connectCancelledByUser)
//				uiHandler.sendMessage(Message.obtain(null, UIHandler.DISPLAY_TOAST, getResources().getText(com.freerdp.freerdpcore.R.string.error_connection_failure)));
			
			closeSessionActivity(RESULT_CANCELED);
		}

		private void OnDisconnected(Context context)
		{
			Log.v(TAG, "OnDisconnected");

			// remove pending move events
//			uiHandler.removeMessages(UIHandler.SEND_MOVE_EVENT);

			if(progressDialog != null)
			{
				progressDialog.dismiss();
				progressDialog = null;
			}
			
			session.setUIEventListener(null);
			closeSessionActivity(RESULT_OK);
		}
	}
	
	public static final String PARAM_CONNECTION_REFERENCE = "conRef";
	public static final String PARAM_INSTANCE = "instance";
	public static final String PARAM_JETRO_REFERENCE = "jetro";
	
	private static final float ZOOMING_STEP = 0.5f;
	private static final int ZOOMCONTROLS_AUTOHIDE_TIMEOUT = 4000;

	// timeout between subsequent scrolling requests when the touch-pointer is at the edge of the session view
	private static final int SCROLLING_TIMEOUT = 50;
	private static final int SCROLLING_DISTANCE = 20;

	private Bitmap bitmap;
	private SessionState session;
	private ProgressDialog progressDialog;
	private KeyboardView keyboardView;
	private KeyboardView modifiersKeyboardView;
	private ZoomControls zoomControls;
	protected KeyboardMapper keyboardMapper; 
	
	private Keyboard specialkeysKeyboard;	
	private Keyboard numpadKeyboard;	
	private Keyboard cursorKeyboard;	
	private Keyboard modifiersKeyboard;

	private AlertDialog dlgVerifyCertificate;
	private AlertDialog dlgUserCredentials;
	private View userCredView;
	
	private boolean autoScrollTouchPointer = GlobalSettings.getAutoScrollTouchPointer();
	private boolean connectCancelledByUser = false;
	private boolean sessionRunning = false;
	private boolean toggleMouseButtons = false;

	private LibFreeRDPBroadcastReceiver libFreeRDPBroadcastReceiver;

	private static final String TAG = "FreeRDP.SessionActivity";

	private ScrollView2D scrollView;

	// keyboard visibility flags
	protected boolean sysKeyboardVisible = false;
	protected boolean extKeyboardVisible = false;

	// variables for delayed move event sending
	private static final int MAX_DISCARDED_MOVE_EVENTS = 3;
	private static final int SEND_MOVE_EVENT_TIMEOUT = 150;
	
	private int discardedMoveEvents = 0;
	
	private ClipboardManagerProxy mClipboardManager;
	
	private boolean callbackDialogResult;
	
	private void createDialogs()
	{
		// build verify certificate dialog
		dlgVerifyCertificate = new AlertDialog.Builder(this)
		.setTitle(com.freerdp.freerdpcore.R.string.dlg_title_verify_certificate)
		.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {				
			@Override
			public void onClick(DialogInterface dialog, int which) {
				callbackDialogResult = true;
				synchronized(dialog)
				{
					dialog.notify();					
				}
			}
		})
		.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				callbackDialogResult = false;
				connectCancelledByUser = true;
				synchronized(dialog)
				{
					dialog.notify();					
				}
			}
		})
		.setCancelable(false)
		.create();
		
		// build the dialog
		userCredView = getLayoutInflater().inflate(com.freerdp.freerdpcore.R.layout.credentials, null, true);		
		dlgUserCredentials = new AlertDialog.Builder(this)
		.setView(userCredView)
		.setTitle(com.freerdp.freerdpcore.R.string.dlg_title_credentials)
		.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {				
			@Override
			public void onClick(DialogInterface dialog, int which) {
				callbackDialogResult = true;
				synchronized(dialog)
				{
					dialog.notify();					
				}
			}
		})
		.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				callbackDialogResult = false;
				connectCancelledByUser = true;
				synchronized(dialog)
				{
					dialog.notify();		
				}
			}
		})
		.setCancelable(false)
		.create();		
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState)
	{				
		super.onCreate(savedInstanceState);

		// show status bar or make fullscreen?
		if(GlobalSettings.getHideStatusBar())
			getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
		this.setContentView(R.layout.session_fragment_activity_layout);

		Log.v(TAG, "Session.onCreate");
	
		libFreeRDPBroadcastReceiver = new LibFreeRDPBroadcastReceiver();

		createDialogs();

		// register freerdp events broadcast receiver
		IntentFilter filter = new IntentFilter();
		filter.addAction(GlobalApp.ACTION_EVENT_FREERDP);
		registerReceiver(libFreeRDPBroadcastReceiver, filter);

		
        socketManager = GlobalApp.getSocketManager(this);
        ticket = GlobalApp.getSessionTicket();
		sendMyApplicationsMsg(ticket);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		Log.v(TAG, "Session.onDestroy");

		// Cancel running disconnect timers.
		GlobalApp.cancelDisconnectTimer();

		// Disconnect all remaining sessions.
		Collection<SessionState> sessions = GlobalApp.getSessions();
		for (SessionState session : sessions)
			LibFreeRDP.disconnect(session.getInstance());

		// unregister freerdp events broadcast receiver
		unregisterReceiver(libFreeRDPBroadcastReceiver);

		// free session
		GlobalApp.freeSession(session.getInstance());
		session = null;
	}
		
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
	    super.onConfigurationChanged(newConfig);
	    	    
	    // reload keyboard resources (changed from landscape)
		modifiersKeyboard = new Keyboard(getApplicationContext(), R.xml.modifiers_keyboard);
		specialkeysKeyboard = new Keyboard(getApplicationContext(), R.xml.specialkeys_keyboard);
		numpadKeyboard = new Keyboard(getApplicationContext(), R.xml.numpad_keyboard);
		cursorKeyboard = new Keyboard(getApplicationContext(), R.xml.cursor_keyboard);

		// apply loaded keyboards
		keyboardView.setKeyboard(specialkeysKeyboard);
		modifiersKeyboardView.setKeyboard(modifiersKeyboard);
	}
	
	private void processIntent(Intent intent)
	{
		// get either session instance or create one from a bookmark
		Bundle bundle = intent.getExtras();
		if(bundle.containsKey(PARAM_INSTANCE))
		{
			int inst = bundle.getInt(PARAM_INSTANCE);
			session = GlobalApp.getSession(inst);
			bitmap = session.getSurface().getBitmap();
			bindSession();
		}
		else if (bundle.containsKey(PARAM_JETRO_REFERENCE))
		{
			ManualBookmark bookmark = null;
			
			bookmark = bundle.getParcelable(PARAM_JETRO_REFERENCE);
			
			if(bookmark != null)
				connect(bookmark);
			else
				closeSessionActivity(RESULT_CANCELED);
		}
		else if(bundle.containsKey(PARAM_CONNECTION_REFERENCE))
		{
			BookmarkBase bookmark = null;
			String refStr = bundle.getString(PARAM_CONNECTION_REFERENCE);
			if(ConnectionReference.isHostnameReference(refStr))
			{
				bookmark = new ManualBookmark();
				bookmark.<ManualBookmark>get().setHostname(ConnectionReference.getHostname(refStr));			
			}
			else if(ConnectionReference.isBookmarkReference(refStr))
			{
				if(ConnectionReference.isManualBookmarkReference(refStr))
					bookmark = GlobalApp.getManualBookmarkGateway().findById(ConnectionReference.getManualBookmarkId(refStr));
				else 
					assert false;								
			}
			
			if(bookmark != null)			
				connect(bookmark);	
			else
				closeSessionActivity(RESULT_CANCELED);
		}
		else
		{
			// no session found - exit
			closeSessionActivity(RESULT_CANCELED);		
		}
	}
	
	private void connect(final BookmarkBase bookmark)
	{
		TaskFragment taskFragment = (TaskFragment) getSupportFragmentManager().findFragmentById(R.id.task_fragment);
		if (taskFragment != null) {
			session = GlobalApp.createSession(bookmark);
			session.setUIEventListener(taskFragment);
		}
		
		int screen_width = getWindow().getDecorView().getWidth();
		int screen_height = getWindow().getDecorView().getHeight();
		
		// set writeable data directory
		LibFreeRDP.setDataDirectory(session.getInstance(), getFilesDir().toString());
		
		BookmarkBase.ScreenSettings screenSettings = session.getBookmark().getActiveScreenSettings();
		Log.v(TAG, "Screen Resolution: " + screenSettings.getResolutionString());	
		if (screenSettings.isAutomatic())
		{
			if((getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_LARGE)
			{
				// large screen device i.e. tablet: simply use screen info				
				screenSettings.setHeight(screen_height);				
				screenSettings.setWidth(screen_width);
			}
			else
			{
				// small screen device i.e. phone:
				// Automatic uses the largest side length of the screen and makes a 16:10 resolution setting out of it 				
				int screenMax = (screen_width > screen_height) ? screen_width : screen_height;			
				screenSettings.setHeight(screenMax);				
				screenSettings.setWidth((int)((float)screenMax * 1.6f));
			}			
		}
		if (screenSettings.isFitScreen()) {
			screenSettings.setHeight(screen_height);
			screenSettings.setWidth(screen_width);
		}
		
		progressDialog = new ProgressDialog(this);
		progressDialog.setTitle(bookmark.getLabel());
		progressDialog.setMessage(getResources().getText(R.string.dlg_msg_connecting));
		progressDialog.setButton(ProgressDialog.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				connectCancelledByUser = true;
				//LibFreeRDP.cancelConnection(session.getInstance());
				finish();
			}
		});
		progressDialog.setCancelable(false);
		progressDialog.show();
		
		// connection thread
		Thread thread = new Thread(new Runnable() {
			public void run() {
				
				// first make sure that the destination is reachable
				// "ping" it with timeout of 3 seconds
				Socket socket = null;
				boolean reachable = false;
				try {
					int socketTimeout = 3000;
				    socket = new Socket();
				    ManualBookmark mbm = (ManualBookmark)bookmark;
				    socket.connect(new InetSocketAddress(mbm.getHostname(), mbm.getPort()), 
				    		socketTimeout);
				    
				    // this will speed up the disconnection time when we close this socket
				    socket.setSoTimeout(socketTimeout);
				    
				    // okay: RDP destination is reachable
				    reachable = true;
				    
				} catch (UnknownHostException e) {
				} catch (IOException e) { } 
				finally 
				{
					// close the pinging socket
			    	try {
			    		if (socket != null)
			    			socket.close(); 
				    }   
			    	catch (IOException e) {}
			    	
			    	// now perform the actual RDP connection
			    	// provided that the destination is reachable
				    if (reachable)
				    {
				    	session.connect();
				    }
				    else
				    {
				    	// else: stop the progress bar and go back to prev activity
				    	runOnUiThread(new Runnable() {
							public void run() {
								if (progressDialog != null && progressDialog.isShowing())
									progressDialog.cancel();
								finish();
							}
						});
				    }
				}
			}
		});
		thread.start();
	}
	
	// binds the current session to the activity by wiring it up with the sessionView and updating all internal objects accordingly
	private void bindSession() {
		Log.v("SessionActivity", "bindSession called");
		
		TaskFragment taskFragment = (TaskFragment) getSupportFragmentManager().findFragmentById(R.id.task_fragment);
		taskFragment.onBindSession(session);
	}
	
	private void closeSessionActivity(int resultCode) {
		// Go back to home activity (and send intent data back to home)
		setResult(resultCode, getIntent());
		finish();
	}
	
	// update the state of our modifier keys
	private void updateModifierKeyStates() {
		// check if any key is in the keycodes list
		
		List<Keyboard.Key> keys = modifiersKeyboard.getKeys();
		for(Iterator<Keyboard.Key> it = keys.iterator(); it.hasNext(); )
		{
			// if the key is a sticky key - just set it to off  
			Keyboard.Key curKey = it.next();
			if(curKey.sticky)
			{
				switch(keyboardMapper.getModifierState(curKey.codes[0]))
				{
					case KeyboardMapper.KEYSTATE_ON:
						curKey.on = true;
						curKey.pressed = false;
						break;
				
					case KeyboardMapper.KEYSTATE_OFF:
						curKey.on = false;
						curKey.pressed = false;
						break;

					case KeyboardMapper.KEYSTATE_LOCKED:
						curKey.on = true;
						curKey.pressed = true;
						break;
				}				
			}
		}

		// refresh image
		modifiersKeyboardView.invalidateAllKeys();
	}
	
	@Override
	public void onBackPressed() {
		// hide keyboards (if any visible) or send alt+f4 to the session
		if(sysKeyboardVisible || extKeyboardVisible) {
			TaskFragment taskFragment = (TaskFragment) getSupportFragmentManager().findFragmentById(R.id.task_fragment);
			taskFragment.showKeyboard(false, false);
		} else {
			//keyboardMapper.sendAltF4();
			showPopupExit(Constants.POPUP_EXIT_MESSAGE, Constants.YES_MESSAGE, Constants.NO_MESSAGE);
			super.onBackPressed();
		}
	}
	
	// android keyboard input handling
	// We always use the unicode value to process input from the android keyboard except if key modifiers
	// (like Win, Alt, Ctrl) are activated. In this case we will send the virtual key code to allow key
	// combinations (like Win + E to open the explorer). 
	@Override
	public boolean onKeyDown(int keycode, KeyEvent event) {
		return keyboardMapper.processAndroidKeyEvent(event);
	}
	
	@Override
	public boolean onKeyUp(int keycode, KeyEvent event) {
		return keyboardMapper.processAndroidKeyEvent(event);	
	}

	// onKeyMultiple is called for input of some special characters like umlauts and some symbol characters
	@Override
	public boolean onKeyMultiple(int keyCode, int repeatCount, KeyEvent event) {
		return keyboardMapper.processAndroidKeyEvent(event);
	}
	
	
	
	
	
	
	/*
	 * Added By Ran Haveshush
	 */
	
	private SocketManager socketManager;
	private String ticket;
	private boolean isChecked = false;
	
	// Current RDP data
	// TODO: wrap all this data in an rdp object
	private String selectedAppId;
	private int processId;
	private Task task;
	private int hwnd;
	
	public SessionState getSession() {
		return session;
	}
	
	private void showPopupExit(String st, String but1, String but2) {
		Log.d(TAG, "showPopupExit(...) ENTER");
		
		new AlertDialog.Builder(this)
			.setMessage(st)
			.setCancelable(false)
			.setPositiveButton(but1, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						SessionFragmentActivity.this.finish();
					}
				})
			.setNegativeButton(but2, null)
			.show();
	}
	
	private void sendMyApplicationsMsg(String ticket) {
		Log.d(TAG, "sendMyApplicationsMsg(...) ENTER");
		
		MyApplicationsMsg msg = new MyApplicationsMsg(ticket);
		Log.i(TAG, "Message: " + msg);
		socketManager.sendMessage(msg);
	}
	
	private void sendGetTsMsg(String ticket) {
		Log.d(TAG, "DesktopActivity#sendGetTsMsg(...) ENTER");
		
		socketManager.sendMessage(new GetTsMsg(ticket));
	}
	
	private void sendStartApplicationMsg(String appToStartId) {
		Log.d(TAG, "JetroSessionActivity#sendStartApplicationMsg(...) ENTER");
		
		socketManager.sendMessage(new StartApplicationMsg(appToStartId));
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
			DesktopFragment desktopFragment = (DesktopFragment) getSupportFragmentManager().findFragmentById(R.id.desktop_fragment);
			desktopFragment.refreshApplications(myAppsResponse.getApplications());
			break;
		case MessagesValues.ClassID.GetTsMsg:
			GetTsMsgResponse getTsResponse = (GetTsMsgResponse) msg.getJsonResponse();
			
			Fragment taskFragment = getSupportFragmentManager().findFragmentById(R.id.task_fragment) ;
			getSupportFragmentManager().beginTransaction().show(taskFragment).commit();
			
			// TODO: open RDP session against the server
			ScreenSettings screen_settings = new ScreenSettings();
			screen_settings.setResolution(ScreenSettings.FITSCREEN);
			ManualBookmark bookmark = new ManualBookmark();
			bookmark.setHostname(getTsResponse.getAddress());
			bookmark.setPort(getTsResponse.getPort());
			bookmark.setScreenSettings(screen_settings);
			connect(bookmark);
			break;
		case MessagesValues.ClassID.SessionReadyMsg:
			sendStartApplicationMsg(selectedAppId);
			break;
		case MessagesValues.ClassID.StartApplicationMsg:
			StartApplicationMsgResponse startApplicationMsgResponse = (StartApplicationMsgResponse) msg.getJsonResponse();
			processId = startApplicationMsgResponse.getPID();
			break;
		case MessagesValues.ClassID.WindowCreatedMsg:
			WindowCreatedMsgResponse windowCreatedMsgResponse = (WindowCreatedMsgResponse) msg.getJsonResponse();
			task = windowCreatedMsgResponse.getTask();
			break;
		case MessagesValues.ClassID.ShowWindowMsg:
			ShowWindowMsgResponse showWindowMsgResponse = (ShowWindowMsgResponse) msg.getJsonResponse();
			hwnd = showWindowMsgResponse.getHWND();
			processId = showWindowMsgResponse.getPID();
			break;
		case MessagesValues.ClassID.ShowTaskListMsg:
			ShowTaskListMsgResponse showTaskListMsgResponse = (ShowTaskListMsgResponse) msg.getJsonResponse();
			int activeHWND = showTaskListMsgResponse.getActiveHWND();
			Task[] tasks = showTaskListMsgResponse.getTasks();
			break;
		case MessagesValues.ClassID.ShowKeyBoardMsg:
			ShowKeyBoardMsgResponse showKeyBoardMsgResponse = (ShowKeyBoardMsgResponse) msg.getJsonResponse();
//			TaskFragment taskFragment = (TaskFragment) getSupportFragmentManager().findFragmentByTag(FRAGMENT_TAG_TASK);
//			if (showKeyBoardMsgResponse.isShow()) {
//				taskFragment.showKeyboard(true, false);
//			} else {
//				taskFragment.showKeyboard(false, false);
//			}
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

		LayoutInflater inflater = LayoutInflater.from(SessionFragmentActivity.this);
		final View dialogView = inflater.inflate(R.layout.home_screen_exit_dialog, null);

		AlertDialog.Builder builder = new AlertDialog.Builder(SessionFragmentActivity.this);
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
				Intent intent = new Intent(SessionFragmentActivity.this, ConnectionsListActivity.class);
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

	@Override
	public void onSelectedApplication(Application selectedApp) {
		Log.w(TAG, "Selected App Name: " + selectedApp.getName());
		Log.w(TAG, "Selected App Id: " + selectedApp.getID());
		
		sendGetTsMsg(ticket);
	}

}

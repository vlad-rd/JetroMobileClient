/*
   Android Session Activity

   Copyright 2013 Thincast Technologies GmbH, Author: Martin Fleisz

   This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
   If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
*/

package com.jetro.mobileclient.ui.activities;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ZoomControls;

import com.freerdp.freerdpcore.application.GlobalApp;
import com.freerdp.freerdpcore.application.GlobalSettings;
import com.freerdp.freerdpcore.application.SessionState;
import com.freerdp.freerdpcore.domain.BookmarkBase;
import com.freerdp.freerdpcore.domain.BookmarkBase.ScreenSettings;
import com.freerdp.freerdpcore.domain.ConnectionReference;
import com.freerdp.freerdpcore.domain.ManualBookmark;
import com.freerdp.freerdpcore.presentation.ScrollView2D;
import com.freerdp.freerdpcore.presentation.TouchPointerView;
import com.freerdp.freerdpcore.services.LibFreeRDP;
import com.freerdp.freerdpcore.utils.ClipboardManagerProxy;
import com.freerdp.freerdpcore.utils.KeyboardMapper;
import com.freerdp.freerdpcore.utils.Mouse;
import com.jetro.mobileclient.R;
import com.jetro.mobileclient.application.ActiveTasks;
import com.jetro.mobileclient.config.Config;
import com.jetro.mobileclient.model.beans.Connection;
import com.jetro.mobileclient.ui.adapters.TasksAdapter;
import com.jetro.mobileclient.ui.widgets.SessionView;
import com.jetro.protocol.Core.BaseMsg;
import com.jetro.protocol.Core.ClassID;
import com.jetro.protocol.Core.IMessageSubscriber;
import com.jetro.protocol.Core.Net.ClientChannel;
import com.jetro.protocol.Protocols.Controller.Application;
import com.jetro.protocol.Protocols.Controller.ApplicationIconMsg;
import com.jetro.protocol.Protocols.Controller.GetTsMsg;
import com.jetro.protocol.Protocols.Controller.LogoutMsg;
import com.jetro.protocol.Protocols.Controller.MyApplicationsMsg;
import com.jetro.protocol.Protocols.Generic.ErrorMsg;
import com.jetro.protocol.Protocols.TsSession.SessionReadyMsg;
import com.jetro.protocol.Protocols.TsSession.ShowKeyBoardMsg;
import com.jetro.protocol.Protocols.TsSession.ShowTaskListMsg;
import com.jetro.protocol.Protocols.TsSession.ShowWindowMsg;
import com.jetro.protocol.Protocols.TsSession.StartApplicationMsg;
import com.jetro.protocol.Protocols.TsSession.Window;
import com.jetro.protocol.Protocols.TsSession.WindowChangedMsg;
import com.jetro.protocol.Protocols.TsSession.WindowCreatedMsg;
import com.jetro.protocol.Protocols.TsSession.WindowDestroyedMsg;


public class SessionActivity extends Activity
	implements LibFreeRDP.UIEventListener, KeyboardView.OnKeyboardActionListener, ScrollView2D.ScrollView2DListener, 
			   KeyboardMapper.KeyProcessingListener, SessionView.SessionViewListener, TouchPointerView.TouchPointerListener,
			   ClipboardManagerProxy.OnClipboardChangedListener,
			   IMessageSubscriber
{
	
	private static final String TAG = SessionActivity.class.getSimpleName();
	
	private class UIHandler extends Handler {
		
		public static final int REFRESH_SESSIONVIEW = 1;
		public static final int DISPLAY_TOAST = 2;
		public static final int HIDE_ZOOMCONTROLS = 3;
		public static final int SEND_MOVE_EVENT = 4;
		public static final int SHOW_DIALOG = 5;
		public static final int GRAPHICS_CHANGED = 6;
		public static final int SCROLLING_REQUESTED = 7;
		
		UIHandler() {
			super();
		}
		
		@Override
		public void handleMessage(Message msg) {
			switch(msg.what)
			{
				case GRAPHICS_CHANGED:
				{
					sessionView.onSurfaceChange(session);
					scrollView.requestLayout();
					break;
				}
				case REFRESH_SESSIONVIEW:
				{
					sessionView.invalidateRegion();
					break;
				}
				case DISPLAY_TOAST:
				{
					Toast errorToast = Toast.makeText(getApplicationContext(), msg.obj.toString(), Toast.LENGTH_LONG);
					errorToast.show();
					break;
				}
				case HIDE_ZOOMCONTROLS:
				{
					zoomControls.hide();
					break;
				}
				case SEND_MOVE_EVENT:
				{
			    	LibFreeRDP.sendCursorEvent(session.getInstance(), msg.arg1, msg.arg2, Mouse.getMoveEvent());		
					break;
				}
				case SHOW_DIALOG:
				{	
					// create and show the dialog					
					((Dialog)msg.obj).show();
					break;
				}
				case SCROLLING_REQUESTED:
				{
		    		int scrollX = 0;
		    		int scrollY = 0;    		
		    		float[] pointerPos = touchPointerView.getPointerPosition();

		    		if (pointerPos[0] > (screen_width - touchPointerView.getPointerWidth()))
		    			scrollX = SCROLLING_DISTANCE;
		    		else if (pointerPos[0] < 0)
		    			scrollX = -SCROLLING_DISTANCE;
		    		
		    		if (pointerPos[1] > (screen_height - touchPointerView.getPointerHeight()))
		    			scrollY = SCROLLING_DISTANCE;
		    		else if (pointerPos[1] < 0)
		    			scrollY = -SCROLLING_DISTANCE;
		    		
		    		scrollView.scrollBy(scrollX, scrollY);
		    		
		    		// see if we reached the min/max scroll positions
		    		if (scrollView.getScrollX() == 0 || scrollView.getScrollX() == (sessionView.getWidth() - scrollView.getWidth()))
		    			scrollX = 0;	    		
		    		if (scrollView.getScrollY() == 0 || scrollView.getScrollY() == (sessionView.getHeight() - scrollView.getHeight()))
		    			scrollY = 0;
		    		
		    		if (scrollX != 0 || scrollY != 0)
		        		uiHandler.sendEmptyMessageDelayed(SCROLLING_REQUESTED, SCROLLING_TIMEOUT);
		    		else
		    			Log.v(TAG, "Stopping auto-scroll");
		    		break;
				}
			}
		}
	}	

	private class PinchZoomListener extends ScaleGestureDetector.SimpleOnScaleGestureListener
	{
		private float scaleFactor = 1.0f;

		@Override
		public boolean onScaleBegin(ScaleGestureDetector detector) {
			scrollView.setScrollEnabled(false);
			return true;
		}
		
		@Override
		public boolean onScale(ScaleGestureDetector detector) {
		
			// calc scale factor
			scaleFactor *= detector.getScaleFactor();
			scaleFactor = Math.max(SessionView.MIN_SCALE_FACTOR, Math.min(scaleFactor, SessionView.MAX_SCALE_FACTOR));
			sessionView.setZoom(scaleFactor);

			if(!sessionView.isAtMinZoom() && !sessionView.isAtMaxZoom())
			{
				// transform scroll origin to the new zoom space  
				float transOriginX = scrollView.getScrollX() * detector.getScaleFactor();
				float transOriginY = scrollView.getScrollY() * detector.getScaleFactor();
				
				// transform center point to the zoomed space  
				float transCenterX = (scrollView.getScrollX() + detector.getFocusX()) * detector.getScaleFactor();
				float transCenterY = (scrollView.getScrollY() + detector.getFocusY()) * detector.getScaleFactor();
				
				// scroll by the difference between the distance of the transformed center/origin point and their old distance (focusX/Y)
				scrollView.scrollBy((int)((transCenterX - transOriginX) - detector.getFocusX()), (int)((transCenterY - transOriginY) - detector.getFocusY()));				
			}
			
			return true;			
		}
		
		@Override
		public void onScaleEnd(ScaleGestureDetector de)
		{
			scrollView.setScrollEnabled(true);		
		}
	}

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
			
			// add host name to history if quick connect was used
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
			uiHandler.removeMessages(UIHandler.SEND_MOVE_EVENT);

			if(progressDialog != null)
			{
				progressDialog.dismiss();
				progressDialog = null;
			}

			// post error message on UI thread
			if (!connectCancelledByUser)
				uiHandler.sendMessage(Message.obtain(null, UIHandler.DISPLAY_TOAST, getResources().getText(R.string.error_connection_failure)));
			
			closeSessionActivity(RESULT_CANCELED);
		}

		private void OnDisconnected(Context context)
		{
			Log.v(TAG, "OnDisconnected");

			// remove pending move events
			uiHandler.removeMessages(UIHandler.SEND_MOVE_EVENT);

			if(progressDialog != null)
			{
				progressDialog.dismiss();
				progressDialog = null;
			}
			
			session.setUIEventListener(null);
			closeSessionActivity(RESULT_OK);
		}
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
			return position;
		}

		@Override
		public View getView(final int position, View convertView, ViewGroup parent) {
			Application currApp = apps[position];

			convertView = inflater.inflate(R.layout.grid_item_app, null);
			ProgressBar appIconLoding = (ProgressBar) convertView.findViewById(R.id.progress_loading);
			ImageView appIcon = (ImageView) convertView.findViewById(R.id.app_icon);
			ImageView appActiveIndicator = (ImageView) convertView.findViewById(R.id.app_active_indicator);
			TextView appName = (TextView) convertView.findViewById(R.id.app_name);
			
			appName.setText(currApp.Name);
			if (currApp.Icon != null && currApp.Icon.length > 0) {
				appIconLoding.setVisibility(View.GONE);
				proccessAppIconInBackground(appIcon, currApp.Icon);
			} else {
				appIconLoding.setVisibility(View.VISIBLE);
			}
			if (currApp.IsActive) {
				appActiveIndicator.setVisibility(View.VISIBLE);
			} else {
				appActiveIndicator.setVisibility(View.INVISIBLE);
			}

			return convertView;
		}
		
		public Application getItem(String appId) {
			for (Application app : apps) {
				if (app.ID.equals(appId)) {
					return app;
				}
			}
			return null;
		}

		private void proccessAppIconInBackground(final ImageView imageView, final byte[] iconAsBytes) {
			new AsyncTask<Void, Void, Bitmap>() {
				@Override
				protected Bitmap doInBackground(Void... params) {
					Bitmap bitmap = null;
					try {
						bitmap = BitmapFactory.decodeByteArray(iconAsBytes, 0, iconAsBytes.length);
					} catch (Exception e) {
						Log.e(TAG, "ERROR: ", e);
					}
					return bitmap;
				}

				@Override
				protected void onPostExecute(Bitmap result) {
					imageView.setImageBitmap(result);
				};
			}.execute();
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
	private View activityRootView;
	protected SessionView sessionView;
	protected TouchPointerView touchPointerView;
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
	
	private UIHandler uiHandler;

	private int screen_width;
	private int screen_height;
	
	private boolean autoScrollTouchPointer = GlobalSettings.getAutoScrollTouchPointer();
	private boolean connectCancelledByUser = false;
	private boolean sessionRunning = false;
	private boolean toggleMouseButtons = false;

	private LibFreeRDPBroadcastReceiver libFreeRDPBroadcastReceiver;

	private ScrollView2D scrollView;

	// keyboard visibility flags
	protected boolean sysKeyboardVisible = false;
	protected boolean extKeyboardVisible = false;

	// variables for delayed move event sending
	private static final int MAX_DISCARDED_MOVE_EVENTS = 3;
	private static final int SEND_MOVE_EVENT_TIMEOUT = 150;
	private int discardedMoveEvents = 0;
	
	private ClipboardManagerProxy mClipboardManager;
	
	// Desktop
	private ClientChannel mClientChannel;
	private Connection mConnection;
	private DrawerLayout mDrawerLayout;
	private ViewGroup mDrawerLeft;
	private ActiveTasks mActiveTasks;
	private ApplicationsGridAdapter mAppsAdapter;
	private GridView mAppsGrid;
	private ImageView mRefreshButton;
	private ImageView mHomeButton;
	private ImageView mDissconnectSessionButton;
	private String mSelectedAppId;
	
	// Tasks drawer
	private TasksAdapter mTasksAdapter;
	private ListView mTasksList;
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		Log.d(TAG, TAG + "#onSaveInstanceState(...) ENTER");
		
		// Save the user's current game state
		outState.putSerializable(Config.Extras.EXTRA_CONNECTION, mConnection);
		
		// Always call the superclass so it can save the view hierarchy state
		super.onSaveInstanceState(outState);
	}

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		Log.d(TAG, TAG + "#onCreate(...) ENTER");
		super.onCreate(savedInstanceState);
		
		// Check whether we're recreating a previously destroyed instance
		if (savedInstanceState != null) {
			// Restore value of members from saved state
			mConnection = (Connection) savedInstanceState.getSerializable(Config.Extras.EXTRA_CONNECTION);
		} else {
			// Probably initialize members with default values for a new instance
			Intent intent = getIntent();
			mConnection = (Connection) intent.getSerializableExtra(Config.Extras.EXTRA_CONNECTION);
		}

		// show status bar or make fullscreen?
		if(GlobalSettings.getHideStatusBar())
			getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
		this.setContentView(R.layout.activity_desktop);
		
		// ATTENTION: We use the onGlobalLayout notification to start our session.
		// This is because only then we can know the exact size of our session when using fit screen
		// accounting for any status bars etc. that Android might throws on us. A bit weird looking
		// but this is the only way ...
		activityRootView = findViewById(R.id.session_root_view);
		activityRootView.getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() 
		{
			@Override
			public void onGlobalLayout() {
				screen_width = activityRootView.getWidth();
				screen_height = activityRootView.getHeight();
								
				// start session
//				if(!sessionRunning && getIntent() != null)
//				{
//					processIntent(getIntent());
//					sessionRunning = true;
//				}
			}	
		});
	
		sessionView = (SessionView) findViewById(R.id.sessionView);
		sessionView.setScaleGestureDetector(new ScaleGestureDetector(this, new PinchZoomListener()));
		sessionView.setSessionViewListener(this);
		sessionView.requestFocus();

		touchPointerView = (TouchPointerView) findViewById(R.id.touchPointerView);
		touchPointerView.setTouchPointerListener(this);
		
		keyboardMapper = new KeyboardMapper();
		keyboardMapper.init(this);
		keyboardMapper.reset(this);
		
		modifiersKeyboard = new Keyboard(getApplicationContext(), R.xml.modifiers_keyboard);
		specialkeysKeyboard = new Keyboard(getApplicationContext(), R.xml.specialkeys_keyboard);
		numpadKeyboard = new Keyboard(getApplicationContext(), R.xml.numpad_keyboard);
		cursorKeyboard = new Keyboard(getApplicationContext(), R.xml.cursor_keyboard);
		
		// hide keyboard below the sessionView
		keyboardView = (KeyboardView)findViewById(R.id.extended_keyboard);
		keyboardView.setKeyboard(specialkeysKeyboard);
		keyboardView.setOnKeyboardActionListener(this);

		modifiersKeyboardView = (KeyboardView) findViewById(R.id.extended_keyboard_header);
		modifiersKeyboardView.setKeyboard(modifiersKeyboard);
		modifiersKeyboardView.setOnKeyboardActionListener(this);
			
		scrollView = (ScrollView2D) findViewById(R.id.sessionScrollView);
		scrollView.setScrollViewListener(this);		
		uiHandler = new UIHandler();
		libFreeRDPBroadcastReceiver = new LibFreeRDPBroadcastReceiver();

		zoomControls = (ZoomControls) findViewById(R.id.zoomControls);
		zoomControls.hide();
		zoomControls.setOnZoomInClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				resetZoomControlsAutoHideTimeout();
				zoomControls.setIsZoomInEnabled(sessionView.zoomIn(ZOOMING_STEP));
				zoomControls.setIsZoomOutEnabled(true);
			}
		});
		zoomControls.setOnZoomOutClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				resetZoomControlsAutoHideTimeout();
				zoomControls.setIsZoomOutEnabled(sessionView.zoomOut(ZOOMING_STEP));			
				zoomControls.setIsZoomInEnabled(true);
			}
		});
				
		toggleMouseButtons = false;
		
		// register freerdp events broadcast receiver
		IntentFilter filter = new IntentFilter();
		filter.addAction(GlobalApp.ACTION_EVENT_FREERDP);
		registerReceiver(libFreeRDPBroadcastReceiver, filter);
		
		mClipboardManager = ClipboardManagerProxy.getClipboardManager(this);
        mClipboardManager.addClipboardChangedListener(this);
        
        mActiveTasks = ActiveTasks.getInstance();
        
        // initialize the Desktop widgets
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerLeft = (ViewGroup) findViewById(R.id.left_drawer);
        mAppsGrid = (GridView) findViewById(R.id.desktop_applications_grid);
        mRefreshButton = (ImageView) findViewById(R.id.desktop_header_refresh_button);
        mRefreshButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				sendMyApplicationsMsg(GlobalApp.getSessionTicket());
				sendShowTaskListMsg();
			}
		});
        
        // initialize the Tasks Drawer widgets
        mHomeButton = (ImageView) findViewById(R.id.home_button);
        mHomeButton.setVisibility(View.INVISIBLE);
        mHomeButton.setOnClickListener(new OnClickListener() {
        	@Override
        	public void onClick(View v) {
        		if (activityRootView.isShown()) {
        			activityRootView.setVisibility(View.INVISIBLE);
        			mHomeButton.setVisibility(View.INVISIBLE);
        			mDrawerLayout.closeDrawer(mDrawerLeft);
        		}
        	}
        });
        mDissconnectSessionButton = (ImageView) findViewById(R.id.disconnect_button);
		mDissconnectSessionButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				finish();
			}
		});
		mTasksAdapter = new TasksAdapter(SessionActivity.this, R.layout.list_item_task, new ArrayList<Window>());
		mTasksList = (ListView) findViewById(R.id.tasks_list);
		mTasksList.setAdapter(mTasksAdapter);
		mTasksList.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				Window task = mTasksAdapter.getItem(position);
				sendShowWindowMsg(task.PID, task.HWND);
			}
		});
	}

	@Override
	protected void onStart() {
		super.onStart();
		Log.v(TAG, "Session.onStart");
	}

	@Override
	protected void onRestart() {
		super.onRestart();
		Log.v(TAG, "Session.onRestart");
	}

	@Override
	protected void onResume() {
		Log.v(TAG, "Session.onResume");
		super.onResume();
		
		mClientChannel = ClientChannel.getInstance();
		if (mClientChannel != null) {
			mClientChannel.AddListener(SessionActivity.this);
		}
		
		sendMyApplicationsMsg(GlobalApp.getSessionTicket());
	}

	@Override
	protected void onPause() {
		Log.v(TAG, "Session.onPause");
		super.onPause();
		
		mClientChannel = ClientChannel.getInstance();
		if (mClientChannel != null) {
			mClientChannel.RemoveListener(SessionActivity.this);
		}

		// hide any visible keyboards
		showKeyboard(false, false);
	}

	@Override
	protected void onStop() {
		super.onStop();
		Log.v(TAG, "Session.onStop");
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

		// remove clipboard listener		
		mClipboardManager.removeClipboardboardChangedListener(this);

		// free session
		if (session != null) {
			GlobalApp.freeSession(session.getInstance());
			session = null;
		}
		
		stopClientChannel();
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
		session = GlobalApp.createSession(bookmark);
		session.setUIEventListener(this);
		
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
		session.setUIEventListener(this);
		sessionView.onSurfaceChange(session);
		scrollView.requestLayout();
		keyboardMapper.reset(this);
	}

	// displays either the system or the extended keyboard or non of  them 
	protected void showKeyboard(boolean showSystemKeyboard, boolean showExtendedKeyboard) {
		// no matter what we are doing ... hide the zoom controls
		// TODO: this is not working correctly as hiding the keyboard issues a onScrollChange notification showing the control again ...
		uiHandler.removeMessages(UIHandler.HIDE_ZOOMCONTROLS);
		if(zoomControls.getVisibility() == View.VISIBLE)
			zoomControls.hide();
		
		InputMethodManager mgr;		
		if(showSystemKeyboard)
		{			
			// hide extended keyboard
			keyboardView.setVisibility(View.GONE);

			// show system keyboard
			mgr = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
			if(!mgr.isActive(sessionView))
				Log.e(TAG, "Failed to show system keyboard: SessionView is not the active view!");
			mgr.showSoftInput(sessionView, 0);
		
			// show modifiers keyboard
			modifiersKeyboardView.setVisibility(View.VISIBLE);
		}
		else if(showExtendedKeyboard)
		{
			// hide system keyboard
			mgr = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
			mgr.hideSoftInputFromWindow(sessionView.getWindowToken(), 0);

			// show extended keyboard
			keyboardView.setKeyboard(specialkeysKeyboard);
			keyboardView.setVisibility(View.VISIBLE);
			modifiersKeyboardView.setVisibility(View.VISIBLE);
		}
		else
		{
			// hide both
			mgr = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
			mgr.hideSoftInputFromWindow(sessionView.getWindowToken(), 0);
			keyboardView.setVisibility(View.GONE);
			modifiersKeyboardView.setVisibility(View.GONE);
			
			// clear any active key modifiers)
			keyboardMapper.clearlAllModifiers();
		}
				
		sysKeyboardVisible = showSystemKeyboard; 
		extKeyboardVisible = showExtendedKeyboard;
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

	private void sendDelayedMoveEvent(int x, int y) {
		if(uiHandler.hasMessages(UIHandler.SEND_MOVE_EVENT))
		{			
			uiHandler.removeMessages(UIHandler.SEND_MOVE_EVENT);
			discardedMoveEvents++;
		}
		else
			discardedMoveEvents = 0;

		if(discardedMoveEvents > MAX_DISCARDED_MOVE_EVENTS)		
	    	LibFreeRDP.sendCursorEvent(session.getInstance(), x, y, Mouse.getMoveEvent());
		else
			uiHandler.sendMessageDelayed(Message.obtain(null, UIHandler.SEND_MOVE_EVENT, x, y), SEND_MOVE_EVENT_TIMEOUT);			
	}

	private void cancelDelayedMoveEvent() {
		uiHandler.removeMessages(UIHandler.SEND_MOVE_EVENT);
	}
		
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.session_menu, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// refer to http://tools.android.com/tips/non-constant-fields why we can't use switch/case here ..
		int itemId = item.getItemId();
		
		if (itemId == R.id.session_touch_pointer)
		{
			// toggle touch pointer
			if(touchPointerView.getVisibility() == View.VISIBLE)
			{
				touchPointerView.setVisibility(View.INVISIBLE);
				sessionView.setTouchPointerPadding(0, 0);
			}
			else
			{
				touchPointerView.setVisibility(View.VISIBLE);				
				sessionView.setTouchPointerPadding(touchPointerView.getPointerWidth(), touchPointerView.getPointerHeight());
			}
		}
		else if (itemId == R.id.session_sys_keyboard)
		{
			showKeyboard(!sysKeyboardVisible, false);
		}
		else if (itemId == R.id.session_ext_keyboard)
		{
			showKeyboard(false, !extKeyboardVisible);
		}
		else if (itemId == R.id.session_disconnect)
		{
			showKeyboard(false, false);
			LibFreeRDP.disconnect(session.getInstance());
		}

		return true;
	}
	
	@Override
	public void onBackPressed() {
		// hide keyboards (if any visible) or send alt+f4 to the session
		if(sysKeyboardVisible || extKeyboardVisible) {
			showKeyboard(false, false);
		} else if (activityRootView.isShown()) {
			activityRootView.setVisibility(View.INVISIBLE);
			mHomeButton.setVisibility(View.INVISIBLE);
		} else {
			//keyboardMapper.sendAltF4();
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
	
	// ****************************************************************************
	// KeyboardView.KeyboardActionEventListener
	@Override
    public void onKey(int primaryCode, int[] keyCodes) {
		keyboardMapper.processCustomKeyEvent(primaryCode);
    }

	@Override
    public void onText(CharSequence text) {
    }

	@Override
    public void swipeRight() {
    }
    
	@Override
    public void swipeLeft() {
    }

	@Override
    public void swipeDown() {
    }

	@Override
    public void swipeUp() {
    }
    
	@Override
    public void onPress(int primaryCode) {
	}
    
	@Override
    public void onRelease(int primaryCode) {
    }

	// ****************************************************************************
	// KeyboardMapper.KeyProcessingListener implementation
	@Override
	public void processVirtualKey(int virtualKeyCode, boolean down) {
		LibFreeRDP.sendKeyEvent(session.getInstance(), virtualKeyCode, down);	
	}

	@Override
	public void processUnicodeKey(int unicodeKey) {
		LibFreeRDP.sendUnicodeKeyEvent(session.getInstance(), unicodeKey);
	}

	@Override
	public void switchKeyboard(int keyboardType) {
		switch(keyboardType)
		{
			case KeyboardMapper.KEYBOARD_TYPE_FUNCTIONKEYS:
				keyboardView.setKeyboard(specialkeysKeyboard);
				break;

			case KeyboardMapper.KEYBOARD_TYPE_NUMPAD:
				keyboardView.setKeyboard(numpadKeyboard);
				break;

			case KeyboardMapper.KEYBOARD_TYPE_CURSOR:
				keyboardView.setKeyboard(cursorKeyboard);
				break;
				
			default:
				break;
		}
	}

	@Override
	public void modifiersChanged() {
		updateModifierKeyStates();							
	}

	// ****************************************************************************	
	// LibFreeRDP UI event listener implementation	
	@Override
	public void OnSettingsChanged(int width, int height, int bpp) {

		if (bpp > 16)
			bitmap = Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888);
		else
			bitmap = Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.RGB_565);

		session.setSurface(new BitmapDrawable(bitmap));
			
		// check this settings and initial settings - if they are not equal the server doesn't support our settings
		// FIXME: the additional check (settings.getWidth() != width + 1) is for the RDVH bug fix to avoid accidental notifications
		// (refer to android_freerdp.c for more info on this problem)
		BookmarkBase.ScreenSettings settings = session.getBookmark().getActiveScreenSettings();
		if((settings.getWidth() != width && settings.getWidth() != width + 1) || settings.getHeight() != height || settings.getColors() != bpp)
			uiHandler.sendMessage(Message.obtain(null, UIHandler.DISPLAY_TOAST, getResources().getText(R.string.info_capabilities_changed)));		
	}
	
	@Override
	public void OnGraphicsUpdate(int x, int y, int width, int height)
	{
		LibFreeRDP.updateGraphics(session.getInstance(), bitmap, x, y, width, height);
		
		sessionView.addInvalidRegion(new Rect(x, y, x + width, y + height));
		
		/* since sessionView can only be modified from the UI thread
		 * any modifications to it need to be scheduled */

		uiHandler.sendEmptyMessage(UIHandler.REFRESH_SESSIONVIEW);
	}

	@Override
	public void OnGraphicsResize(int width, int height, int bpp)
	{
		// replace bitmap
		if (bpp > 16)
			bitmap = Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888);
		else
			bitmap = Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.RGB_565);
		session.setSurface(new BitmapDrawable(bitmap));
		
		/* since sessionView can only be modified from the UI thread
		 * any modifications to it need to be scheduled */
		uiHandler.sendEmptyMessage(UIHandler.GRAPHICS_CHANGED);
	}
	
	private boolean callbackDialogResult; 
	
//	@Override
//	public boolean OnAuthenticate(StringBuilder username, StringBuilder domain, StringBuilder password) {
//		// this is where the return code of our dialog will be stored
//		callbackDialogResult = false;
//
//		// set text fields
//		((EditText)userCredView.findViewById(R.id.editTextUsername)).setText(username);
//		((EditText)userCredView.findViewById(R.id.editTextDomain)).setText(domain);
//		((EditText)userCredView.findViewById(R.id.editTextPassword)).setText(password);
//		
//		// start dialog in UI thread		
//		uiHandler.sendMessage(Message.obtain(null, UIHandler.SHOW_DIALOG, dlgUserCredentials));
//		
//		// wait for result
//		try
//		{		
//			synchronized(dlgUserCredentials)
//			{
//				dlgUserCredentials.wait();
//			}
//		}
//		catch(InterruptedException e)
//		{			
//		}
//
//		// clear buffers
//		username.setLength(0);
//		domain.setLength(0);
//		password.setLength(0);
//
//		// read back user credentials
//		username.append(((EditText)userCredView.findViewById(R.id.editTextUsername)).getText().toString());
//		domain.append(((EditText)userCredView.findViewById(R.id.editTextDomain)).getText().toString());
//		password.append(((EditText)userCredView.findViewById(R.id.editTextPassword)).getText().toString());
//
//		return callbackDialogResult;
//	}
	
	@Override
	public boolean OnAuthenticate(StringBuilder username, StringBuilder domain, StringBuilder password) {
		Log.d(TAG, "OnAuthenticate(...) ENTER");
		
		// provide the user credentials from host to the RDP authentication step
		username.append(mConnection.getUserName());
		password.append(mConnection.getPassword());
		domain.append(mConnection.getDomain());

		return true;
	}

//	@Override
//	public boolean OnVerifiyCertificate(String subject, String issuer, String fingerprint) {
//
//		// see if global settings says accept all
//		if(GlobalSettings.getAcceptAllCertificates())
//			return true;
//		
//		// this is where the return code of our dialog will be stored
//		callbackDialogResult = false;
//
//		// set message
//		String msg = getResources().getString(R.string.dlg_msg_verify_certificate);
//		msg = msg + "\n\nSubject: " + subject + "\nIssuer: " + issuer + "\nFingerprint: " + fingerprint;
//		dlgVerifyCertificate.setMessage(msg);
//		
//		// start dialog in UI thread		
//		uiHandler.sendMessage(Message.obtain(null, UIHandler.SHOW_DIALOG, dlgVerifyCertificate));
//		
//		// wait for result
//		try
//		{		
//			synchronized(dlgVerifyCertificate)
//			{				
//				dlgVerifyCertificate.wait();
//			}
//		}
//		catch(InterruptedException e)
//		{			
//		}
//		
//		return callbackDialogResult;
//	}
	
	@Override
	public boolean OnVerifiyCertificate(String subject, String issuer, String fingerprint) {
		return true;
	}
	
	@Override
	public void OnRemoteClipboardChanged(String data)
	{
		Log.v(TAG, "OnRemoteClipboardChanged: " + data);
		mClipboardManager.setClipboardData(data);				
    }
	
	// ****************************************************************************	
	// ScrollView2DListener implementation
	private void resetZoomControlsAutoHideTimeout() {
		uiHandler.removeMessages(UIHandler.HIDE_ZOOMCONTROLS);
		uiHandler.sendEmptyMessageDelayed(UIHandler.HIDE_ZOOMCONTROLS, ZOOMCONTROLS_AUTOHIDE_TIMEOUT);		
	}
	
	@Override
	public void onScrollChanged(ScrollView2D scrollView, int x, int y, int oldx, int oldy) {
		zoomControls.setIsZoomInEnabled(!sessionView.isAtMaxZoom());
		zoomControls.setIsZoomOutEnabled(!sessionView.isAtMinZoom());
		if(!GlobalSettings.getHideZoomControls() && zoomControls.getVisibility() != View.VISIBLE)
			zoomControls.show();
		resetZoomControlsAutoHideTimeout();
	}

	// ****************************************************************************
	// SessionView.SessionViewListener
	@Override
	public void onSessionViewBeginTouch()
	{
		scrollView.setScrollEnabled(false);		
	}

	@Override
	public void onSessionViewEndTouch()
	{
		scrollView.setScrollEnabled(true);		
	}

	@Override
	public void onSessionViewLeftTouch(int x, int y, boolean down) {		
		if(!down)
			cancelDelayedMoveEvent();

		LibFreeRDP.sendCursorEvent(session.getInstance(), x, y, toggleMouseButtons ? Mouse.getRightButtonEvent(down) : Mouse.getLeftButtonEvent(down));        			

		if (!down)
			toggleMouseButtons = false;
	}

	public void onSessionViewRightTouch(int x, int y, boolean down) {		
		if (!down)
			toggleMouseButtons = !toggleMouseButtons;
	}

	@Override
	public void onSessionViewMove(int x, int y) {
		sendDelayedMoveEvent(x, y);		
	}
	
	@Override
	public void onSessionViewScroll(boolean down) {		
    	LibFreeRDP.sendCursorEvent(session.getInstance(), 0, 0, Mouse.getScrollEvent(down));        					
	}

	// ****************************************************************************
	// TouchPointerView.TouchPointerListener
	@Override
	public void onTouchPointerClose() {
		touchPointerView.setVisibility(View.INVISIBLE);
		sessionView.setTouchPointerPadding(0, 0);
	}

	private Point mapScreenCoordToSessionCoord(int x, int y) {
		int mappedX = (int)((float)(x + scrollView.getScrollX()) / sessionView.getZoom());
		int mappedY = (int)((float)(y + scrollView.getScrollY()) / sessionView.getZoom());
		if(mappedX > bitmap.getWidth())
			mappedX = bitmap.getWidth();
		if(mappedY > bitmap.getHeight())
			mappedY = bitmap.getHeight();
		return new Point(mappedX, mappedY);  
	}
	
	@Override
	public void onTouchPointerLeftClick(int x, int y, boolean down) {
		Point p = mapScreenCoordToSessionCoord(x, y);
    	LibFreeRDP.sendCursorEvent(session.getInstance(), p.x, p.y, Mouse.getLeftButtonEvent(down));
	}

	@Override
	public void onTouchPointerRightClick(int x, int y, boolean down) {
		Point p = mapScreenCoordToSessionCoord(x, y);
    	LibFreeRDP.sendCursorEvent(session.getInstance(), p.x, p.y, Mouse.getRightButtonEvent(down));        			
	}

	@Override
	public void onTouchPointerMove(int x, int y) {
		Point p = mapScreenCoordToSessionCoord(x, y);
    	LibFreeRDP.sendCursorEvent(session.getInstance(), p.x, p.y, Mouse.getMoveEvent());
    	
    	if (autoScrollTouchPointer && !uiHandler.hasMessages(UIHandler.SCROLLING_REQUESTED))
    	{
			Log.v(TAG, "Starting auto-scroll");
    		uiHandler.sendEmptyMessageDelayed(UIHandler.SCROLLING_REQUESTED, SCROLLING_TIMEOUT);
    	}
	}
	
	@Override
	public void onTouchPointerScroll(boolean down) {
    	LibFreeRDP.sendCursorEvent(session.getInstance(), 0, 0, Mouse.getScrollEvent(down));        					
	}

	@Override
	public void onTouchPointerToggleKeyboard() {
		showKeyboard(!sysKeyboardVisible, false);				
	}

	@Override
	public void onTouchPointerToggleExtKeyboard() {
		showKeyboard(false, !extKeyboardVisible);		
	}
	
	@Override
	public void onTouchPointerResetScrollZoom() {
		sessionView.setZoom(1.0f);
		scrollView.scrollTo(0, 0);
	}

	// ****************************************************************************
	// ClipboardManagerProxy.OnClipboardChangedListener
	@Override
	public void onClipboardChanged(String data) {
		Log.v(TAG, "onClipboardChanged: " + data);
		LibFreeRDP.sendClipboardData(session.getInstance(), data);
	}

	// ****************************************************************************
	// IMessageSubscriber implementation
	@Override
	public void ProcessMsg(BaseMsg msg) {
//		Log.i(TAG, TAG + "#ProcessMsg(...)\n" + msg.getClass().getSimpleName() + "\n" + msg.serializeJson());
		Log.i(TAG, TAG + "#ProcessMsg(...)\n" + msg.getClass().getSimpleName());
		
		if (msg.msgCalssID == ClassID.MyApplicationsMsg.ValueOf()) {
			MyApplicationsMsg myApplicationsMsg = (MyApplicationsMsg) msg;
		
			mAppsAdapter = new ApplicationsGridAdapter(myApplicationsMsg.Applications);
			mAppsGrid.setAdapter(mAppsAdapter);
			mAppsGrid.setOnItemClickListener(new OnItemClickListener() {
				@Override
				public void onItemClick(AdapterView<?> parent, View view,
						int position, long id) {
					Application selectedApp = (Application) mAppsAdapter
							.getItem(position);
					mSelectedAppId = selectedApp.ID;
					Log.w(TAG, "Selected App Name: " + selectedApp.Name);
					Log.w(TAG, "Selected App Id: " + mSelectedAppId);
					if (!sessionRunning) {
						sendGetTsMsg(GlobalApp.getSessionTicket());
					} else {
						sendStartApplicationMsg(mSelectedAppId);
					}
				}
			});
			// Fetches async applications icons
			for (Application app : myApplicationsMsg.Applications) {
				sendApplicationIconMsg(app.ID);
			}
		} else if (msg.msgCalssID == ClassID.ApplicationIconMsg.ValueOf()) {
			ApplicationIconMsg applicationIconMsg = (ApplicationIconMsg) msg;
			Application foundApp = mAppsAdapter.getItem(applicationIconMsg.ID);
			if (foundApp != null) {
				foundApp.Icon = applicationIconMsg.Icon;
				mAppsAdapter.notifyDataSetChanged();
			}
		} else if (msg.msgCalssID == ClassID.GetTsMsg.ValueOf()) {
			GetTsMsg getTsMsg = (GetTsMsg) msg;
			ScreenSettings screen_settings = new ScreenSettings();
			screen_settings.setResolution(ScreenSettings.FITSCREEN);
			ManualBookmark bookmark = new ManualBookmark();
			bookmark.setHostname(getTsMsg.Address);
			bookmark.setPort(getTsMsg.Port);
			bookmark.setScreenSettings(screen_settings);
			connect(bookmark);
			sessionRunning = true;
		} else if (msg.msgCalssID == ClassID.SessionReadyMsg.ValueOf()) {
			SessionReadyMsg sessionReadyMsg = (SessionReadyMsg) msg;
			sendStartApplicationMsg(mSelectedAppId);
		} else if (msg.msgCalssID == ClassID.ShowTaskListMsg.ValueOf()) {
			ShowTaskListMsg showTaskListMsg = (ShowTaskListMsg) msg;
			// If none active tasks, hide window
			int activeHWND = showTaskListMsg.ActiveHWND;
			if (activeHWND == 0) {
				activityRootView.setVisibility(View.INVISIBLE);
			}
			// Update active tasks
			Window[] tasks = showTaskListMsg.Tasks;
			mActiveTasks.clear();
			mActiveTasks.add(tasks);
		} else if (msg.msgCalssID == ClassID.StartApplicationMsg.ValueOf()) {
			StartApplicationMsg startApplicationMsg = (StartApplicationMsg) msg;
		} else if (msg.msgCalssID == ClassID.WindowCreatedMsg.ValueOf()) {
			WindowCreatedMsg windowCreatedMsg = (WindowCreatedMsg) msg;
			Window task = windowCreatedMsg.Task;
			Log.i(TAG, TAG + "#ProcessMsg(...) AppID = " + task.AppID);
			// Update Tasks adapter
			mTasksAdapter.add(task);
			mTasksAdapter.notifyDataSetChanged();
			// Update active Tasks
			
			boolean isAppGotActive = mActiveTasks.add(task);
			// Update Apps adapter
			if (isAppGotActive) {
				Application activeApp = mAppsAdapter.getItem(task.AppID);
				if (activeApp != null) {
					activeApp.IsActive = true;
					mAppsAdapter.notifyDataSetChanged();
				}
			}
		} else if (msg.msgCalssID == ClassID.WindowDestroyedMsg.ValueOf()) {
			WindowDestroyedMsg windowDestroyedMsg = (WindowDestroyedMsg) msg;
			Window task = new Window();
			task.AppID = windowDestroyedMsg.AppID;
			task.HWND = windowDestroyedMsg.HWND;
			// Update Tasks adapter
			mTasksAdapter.remove(task);
			mTasksAdapter.notifyDataSetChanged();
			// Update active Tasks
			boolean isAppGotInactive = mActiveTasks.remove(task);
			// Update Apps adapter
			if (isAppGotInactive) {
				Application activeApp = mAppsAdapter.getItem(task.AppID);
				if (activeApp != null) {
					activeApp.IsActive = false;
					mAppsAdapter.notifyDataSetChanged();
				}
			}
		} else if (msg.msgCalssID == ClassID.WindowChangedMsg.ValueOf()) {
			WindowChangedMsg windowChangedMsg = (WindowChangedMsg) msg;
			Window task = new Window();
			task.HWND = windowChangedMsg.HWND;
			task.Title = windowChangedMsg.Title;
			// Update Tasks adapter
			mTasksAdapter.update(task);
			mTasksAdapter.notifyDataSetChanged();
		} else if (msg.msgCalssID == ClassID.ShowWindowMsg.ValueOf()) {
			ShowWindowMsg showWindowMsg = (ShowWindowMsg) msg;
			activityRootView.setVisibility(View.VISIBLE);
			mHomeButton.setVisibility(View.VISIBLE);
			sessionView.requestFocus();
		} else if (msg.msgCalssID == ClassID.ShowKeyBoardMsg.ValueOf()) {
			ShowKeyBoardMsg showKeyBoardMsg = (ShowKeyBoardMsg) msg;
			if (showKeyBoardMsg.Show) {
				showKeyboard(true, false);
			} else {
				showKeyboard(false, false);
			}
		} else if (msg.msgCalssID == ClassID.LogoutMsg.ValueOf()) {
			LogoutMsg logoutMsg = (LogoutMsg) msg;
		} else if (msg.msgCalssID == ClassID.Error.ValueOf()) {
			ErrorMsg errorMsg = (ErrorMsg) msg;
		}
	}

	@Override
	public void ConnectionIsBroken() {
		Log.d(TAG, TAG + "#ConnectionIsBroken(...) ENTER");
		
		stopClientChannel();
		finish();
	}
	
	private void stopClientChannel() {
		// free client channel
		if (mClientChannel != null) {
			mClientChannel.RemoveListener(SessionActivity.this);
			mClientChannel.Stop();
			mClientChannel = null;
		}
	}
	
	private void sendMyApplicationsMsg(String ticket) {
		Log.d(TAG, TAG + "#sendMyApplicationsMsg(...) ENTER");

		MyApplicationsMsg myApplicationsMsg = new MyApplicationsMsg();
		myApplicationsMsg.Ticket = ticket;
		mClientChannel.SendAsync(myApplicationsMsg);
	}
	
	private void sendApplicationIconMsg(String appId) {
		Log.d(TAG, TAG + "#sendApplicationIconMsg(...) ENTER");
		
		ApplicationIconMsg applicationIconMsg = new ApplicationIconMsg();
		applicationIconMsg.ID = appId;
		mClientChannel.SendAsync(applicationIconMsg);
	}
	
	private void sendShowTaskListMsg() {
		Log.d(TAG, TAG + "#sendShowTaskListMsg(...) ENTER");
		
		ShowTaskListMsg showTaskListMsg = new ShowTaskListMsg();
		mClientChannel.SendAsync(showTaskListMsg);
	}

	private void sendGetTsMsg(String ticket) {
		Log.d(TAG, TAG + "#sendGetTsMsg(...) ENTER");
		
		GetTsMsg getTsMsg = new GetTsMsg();
		getTsMsg.Ticket = ticket;
		mClientChannel.SendAsync(getTsMsg);
	}
	
	private void sendStartApplicationMsg(String appToStartId) {
		Log.d(TAG, TAG + "#sendStartApplicationMsg(...) ENTER");
		
		StartApplicationMsg startApplicationMsg = new StartApplicationMsg();
		startApplicationMsg.ID = appToStartId;
		mClientChannel.SendAsync(startApplicationMsg);
	}
	
	private void sendShowWindowMsg(int pId, int hwnd) {
		Log.d(TAG, TAG + "#sendShowWindowMsg(...) ENTER");
		
		ShowWindowMsg showWindowMsg = new ShowWindowMsg();
		showWindowMsg.PID = pId;
		showWindowMsg.HWND = hwnd;
		mClientChannel.SendAsync(showWindowMsg);
	}
}

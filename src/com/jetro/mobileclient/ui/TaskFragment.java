/**
 * 
 */
package com.jetro.mobileclient.ui;

import java.util.Iterator;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;
import android.widget.ZoomControls;

import com.freerdp.freerdpcore.application.GlobalSettings;
import com.freerdp.freerdpcore.application.SessionState;
import com.freerdp.freerdpcore.domain.BookmarkBase;
import com.freerdp.freerdpcore.presentation.ScrollView2D;
import com.freerdp.freerdpcore.presentation.SessionView;
import com.freerdp.freerdpcore.presentation.TouchPointerView;
import com.freerdp.freerdpcore.services.LibFreeRDP;
import com.freerdp.freerdpcore.utils.ClipboardManagerProxy;
import com.freerdp.freerdpcore.utils.KeyboardMapper;
import com.freerdp.freerdpcore.utils.Mouse;
import com.jetro.mobileclient.R;

/**
 * @author ran.h
 *
 */
public class TaskFragment extends Fragment implements
		LibFreeRDP.UIEventListener, ScrollView2D.ScrollView2DListener,
		KeyboardView.OnKeyboardActionListener,
		KeyboardMapper.KeyProcessingListener, SessionView.SessionViewListener,
		TouchPointerView.TouchPointerListener,
		ClipboardManagerProxy.OnClipboardChangedListener {
	
	private static final String TAG = TaskFragment.class.getSimpleName();
	
	private SessionFragmentActivity mActivity;
	
	private static final float ZOOMING_STEP = 0.5f;
	private static final int ZOOMCONTROLS_AUTOHIDE_TIMEOUT = 4000;
	
	// timeout between subsequent scrolling requests when the touch-pointer is at the edge of the session view
	private static final int SCROLLING_TIMEOUT = 50;
	private static final int SCROLLING_DISTANCE = 20;
	
	private Bitmap bitmap;
	private SessionState session;
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
	
	private ScrollView2D scrollView;
	
	// keyboard visibility flags
	protected boolean sysKeyboardVisible = false;
	protected boolean extKeyboardVisible = false;

	// variables for delayed move event sending
	private static final int MAX_DISCARDED_MOVE_EVENTS = 3;
	private static final int SEND_MOVE_EVENT_TIMEOUT = 150;
	private int discardedMoveEvents = 0;
	
	private ClipboardManagerProxy mClipboardManager;
	
	/**
	 * @author ran.h
	 *
	 */
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
					sessionView.onSurfaceChange(mActivity.getSession());
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
					Toast errorToast = Toast.makeText(mActivity.getApplicationContext(), msg.obj.toString(), Toast.LENGTH_LONG);
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
			    	LibFreeRDP.sendCursorEvent(mActivity.getSession().getInstance(), msg.arg1, msg.arg2, Mouse.getMoveEvent());		
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
	
	/**
	 * @author ran.h
	 *
	 */
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

	@Override
	public void onAttach(Activity activity) {
		Log.d(TAG, "TaskFragment#onAttach(...) ENTER");
		super.onAttach(activity);
		
		try {
			mActivity = (SessionFragmentActivity) activity;
		} catch (Exception e) {
			Log.e(TAG, "ERROR: ", e);
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		Log.d(TAG, "TaskFragment#onCreate(...) ENTER");
		super.onCreate(savedInstanceState);
		
		setHasOptionsMenu(true);
	}



	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		Log.d(TAG, "TaskFragment#onCreateView(...) ENTER");
		
		View rootView = inflater.inflate(R.layout.task_fragment_layout, container, false);
		
		sessionView = (SessionView) rootView.findViewById(R.id.sessionView);
		sessionView.setScaleGestureDetector(new ScaleGestureDetector(mActivity, new PinchZoomListener()));
		sessionView.setSessionViewListener(this);
		sessionView.requestFocus();

		touchPointerView = (TouchPointerView) rootView.findViewById(R.id.touchPointerView);
		touchPointerView.setTouchPointerListener(this);
		
		keyboardMapper = new KeyboardMapper();
		keyboardMapper.init(mActivity);
		keyboardMapper.reset(this);
		
		modifiersKeyboard = new Keyboard(mActivity.getApplicationContext(), R.xml.modifiers_keyboard);
		specialkeysKeyboard = new Keyboard(mActivity.getApplicationContext(), R.xml.specialkeys_keyboard);
		numpadKeyboard = new Keyboard(mActivity.getApplicationContext(), R.xml.numpad_keyboard);
		cursorKeyboard = new Keyboard(mActivity.getApplicationContext(), R.xml.cursor_keyboard);
		
		// hide keyboard below the sessionView
		keyboardView = (KeyboardView)rootView.findViewById(R.id.extended_keyboard);
		keyboardView.setKeyboard(specialkeysKeyboard);
		keyboardView.setOnKeyboardActionListener(this);

		modifiersKeyboardView = (KeyboardView) rootView.findViewById(R.id.extended_keyboard_header);
		modifiersKeyboardView.setKeyboard(modifiersKeyboard);
		modifiersKeyboardView.setOnKeyboardActionListener(this);
			
		scrollView = (ScrollView2D) rootView.findViewById(R.id.sessionScrollView);
		scrollView.setScrollViewListener(this);		
		uiHandler = new UIHandler();

		zoomControls = (ZoomControls) rootView.findViewById(R.id.zoomControls);
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
		
		mClipboardManager = ClipboardManagerProxy.getClipboardManager(mActivity);
        mClipboardManager.addClipboardChangedListener(this);
        
        return rootView;
	}
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		Log.d(TAG, "TaskFragment#onCreateOptionsMenu(...) ENTER");
		
		inflater.inflate(R.menu.session_menu, menu);
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
	public void onPause() {
		Log.d(TAG, "TaskFragment#onPause(...) ENTER");
		super.onPause();
		
		// hide any visible keyboards
		showKeyboard(false, false);
	}

	@Override
	public void onDestroy() {
		Log.d(TAG, "TaskFragment#onDestroy(...) ENTER");
		super.onDestroy();
		
		// remove clipboard listener		
		mClipboardManager.removeClipboardboardChangedListener(this);
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		Log.d(TAG, "TaskFragment#onConfigurationChanged(...) ENTER");
		super.onConfigurationChanged(newConfig);
		
	    // reload keyboard resources (changed from landscape)
		modifiersKeyboard = new Keyboard(mActivity.getApplicationContext(), R.xml.modifiers_keyboard);
		specialkeysKeyboard = new Keyboard(mActivity.getApplicationContext(), R.xml.specialkeys_keyboard);
		numpadKeyboard = new Keyboard(mActivity.getApplicationContext(), R.xml.numpad_keyboard);
		cursorKeyboard = new Keyboard(mActivity.getApplicationContext(), R.xml.cursor_keyboard);

		// apply loaded keyboards
		keyboardView.setKeyboard(specialkeysKeyboard);
		modifiersKeyboardView.setKeyboard(modifiersKeyboard);
	}

	// displays either the system or the extended keyboard or non of  them 
	public void showKeyboard(boolean showSystemKeyboard, boolean showExtendedKeyboard) {
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
			mgr = (InputMethodManager) mActivity.getSystemService(Context.INPUT_METHOD_SERVICE);
			if(!mgr.isActive(sessionView))
				Log.e(TAG, "Failed to show system keyboard: SessionView is not the active view!");
			mgr.showSoftInput(sessionView, 0);
		
			// show modifiers keyboard
			modifiersKeyboardView.setVisibility(View.VISIBLE);
		}
		else if(showExtendedKeyboard)
		{
			// hide system keyboard
			mgr = (InputMethodManager) mActivity.getSystemService(Context.INPUT_METHOD_SERVICE);
			mgr.hideSoftInputFromWindow(sessionView.getWindowToken(), 0);

			// show extended keyboard
			keyboardView.setKeyboard(specialkeysKeyboard);
			keyboardView.setVisibility(View.VISIBLE);
			modifiersKeyboardView.setVisibility(View.VISIBLE);
		}
		else
		{
			// hide both
			mgr = (InputMethodManager) mActivity.getSystemService(Context.INPUT_METHOD_SERVICE);
			mgr.hideSoftInputFromWindow(sessionView.getWindowToken(), 0);
			keyboardView.setVisibility(View.GONE);
			modifiersKeyboardView.setVisibility(View.GONE);
			
			// clear any active key modifiers)
			keyboardMapper.clearlAllModifiers();
		}
				
		sysKeyboardVisible = showSystemKeyboard; 
		extKeyboardVisible = showExtendedKeyboard;
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
			bitmap = Bitmap.createBitmap(width, height, Config.ARGB_8888);
		else
			bitmap = Bitmap.createBitmap(width, height, Config.RGB_565);

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
			bitmap = Bitmap.createBitmap(width, height, Config.ARGB_8888);
		else
			bitmap = Bitmap.createBitmap(width, height, Config.RGB_565);
		session.setSurface(new BitmapDrawable(bitmap));
		
		/* since sessionView can only be modified from the UI thread
		 * any modifications to it need to be scheduled */
		uiHandler.sendEmptyMessage(UIHandler.GRAPHICS_CHANGED);
	}
	
	private boolean callbackDialogResult;
	
	@Override
	public boolean OnAuthenticate(StringBuilder username, StringBuilder domain, StringBuilder password) {
		// this is where the return code of our dialog will be stored
		callbackDialogResult = false;

		// set text fields
		((EditText)userCredView.findViewById(R.id.editTextUsername)).setText(username);
		((EditText)userCredView.findViewById(R.id.editTextDomain)).setText(domain);
		((EditText)userCredView.findViewById(R.id.editTextPassword)).setText(password);
		
		// start dialog in UI thread		
		uiHandler.sendMessage(Message.obtain(null, UIHandler.SHOW_DIALOG, dlgUserCredentials));
		
		// wait for result
		try
		{		
			synchronized(dlgUserCredentials)
			{
				dlgUserCredentials.wait();
			}
		}
		catch(InterruptedException e)
		{			
		}

		// clear buffers
		username.setLength(0);
		domain.setLength(0);
		password.setLength(0);

		// read back user credentials
		username.append(((EditText)userCredView.findViewById(R.id.editTextUsername)).getText().toString());
		domain.append(((EditText)userCredView.findViewById(R.id.editTextDomain)).getText().toString());
		password.append(((EditText)userCredView.findViewById(R.id.editTextPassword)).getText().toString());

		return callbackDialogResult;
	}

	@Override
	public boolean OnVerifiyCertificate(String subject, String issuer, String fingerprint) {

		// see if global settings says accept all
		if(GlobalSettings.getAcceptAllCertificates())
			return true;
		
		// this is where the return code of our dialog will be stored
		callbackDialogResult = false;

		// set message
		String msg = getResources().getString(R.string.dlg_msg_verify_certificate);
		msg = msg + "\n\nSubject: " + subject + "\nIssuer: " + issuer + "\nFingerprint: " + fingerprint;
		dlgVerifyCertificate.setMessage(msg);
		
		// start dialog in UI thread		
		uiHandler.sendMessage(Message.obtain(null, UIHandler.SHOW_DIALOG, dlgVerifyCertificate));
		
		// wait for result
		try
		{		
			synchronized(dlgVerifyCertificate)
			{				
				dlgVerifyCertificate.wait();
			}
		}
		catch(InterruptedException e)
		{			
		}
		
		return callbackDialogResult;
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
	
	
	
	
	
	/*
	 * Added By Ran Haveshush
	 */
	
	public void onBindSession(SessionState session) {
		session.setUIEventListener(this);
		sessionView.onSurfaceChange(session);
		scrollView.requestLayout();
		keyboardMapper.reset(this);
	}
}

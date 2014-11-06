package com.jetro.mobileclient.ui;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.freerdp.freerdpcore.R;
import com.freerdp.freerdpcore.domain.ManualBookmark;
import com.freerdp.freerdpcore.presentation.SessionActivity;
import com.freerdp.freerdpcore.sharedobjects.ISocketListener;
import com.freerdp.freerdpcore.sharedobjects.SocketManager;
import com.freerdp.freerdpcore.sharedobjects.Task;
import com.freerdp.freerdpcore.sharedobjects.controller_messages.ErrorMsg.ErrCodeMsgResponse;
import com.freerdp.freerdpcore.sharedobjects.controller_messages.ShowKeyBoardMsg.ShowKeyBoardMsgResponse;
import com.freerdp.freerdpcore.sharedobjects.controller_messages.ShowTaskListMsg.ShowTaskListMsgResponse;
import com.freerdp.freerdpcore.sharedobjects.controller_messages.ShowWindowMsg.ShowWindowMsgResponse;
import com.freerdp.freerdpcore.sharedobjects.controller_messages.StartApplicationMsg;
import com.freerdp.freerdpcore.sharedobjects.controller_messages.StartApplicationMsg.StartApplicationMsgResponse;
import com.freerdp.freerdpcore.sharedobjects.controller_messages.WindowCreatedMsg.WindowCreatedMsgResponse;
import com.freerdp.freerdpcore.sharedobjects.protocol.BaseMsg;
import com.freerdp.freerdpcore.sharedobjects.protocol.MessagesValues;
import com.jetro.mobileclient.application.GlobalApp;


public class JetroSessionActivity extends SessionActivity implements ISocketListener {
	
	private static final String TAG = JetroSessionActivity.class.getSimpleName();
	
	private SocketManager socketManager;
	
	private View sessionRootView;
	
	// Current RDP data
	// TODO: wrap all this data in an rdp object
	private String host;
	private int port;
	private String appToStartId;
	private int processId;
	private Task task;
	private int hwnd;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		Log.d(TAG, "onCreate(...) ENTER");
		super.onCreate(savedInstanceState);
		
		socketManager = GlobalApp.getSocketManager(this);
		
		Intent intent = getIntent();
		appToStartId = (String) intent.getStringExtra("applicationId");
		ManualBookmark bookmark = (ManualBookmark) intent.getParcelableExtra(SessionActivity.PARAM_JETRO_REFERENCE);
		
		sessionRootView = findViewById(R.id.session_root_view);
	}
	
	@Override
	protected void onResume() {
		Log.d(TAG, "onResume(...) ENTER");
		super.onResume();
	}
	
	@Override
	protected void onPause() {
		Log.d(TAG, "onPause(...) ENTER");
		super.onPause();
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		Log.d(TAG, "onSaveInstanceState(...) ENTER");
		super.onSaveInstanceState(outState);
	}

	@Override
	protected void onRestoreInstanceState(Bundle inState) {
		Log.d(TAG, "onRestoreInstanceState(...) ENTER");
		super.onRestoreInstanceState(inState);
	}
	
	@Override
	public boolean OnAuthenticate(StringBuilder username, StringBuilder domain, StringBuilder password) {
		Log.d(TAG, "OnAuthenticate(...) ENTER");
		
		// provide the user's credentials to the RDP authentication step
		username.append(GlobalApp.GetConnectionPoint().getUserName());
		password.append("Welcome!");
		domain.append(GlobalApp.GetConnectionPoint().getDomain());

		return true;
	}

	@Override
	public void OnSocketCreated() {
		Log.d(TAG, "JetroSessionActivity#OnSocketCreated(...) ENTER");
		
	}

	@Override
	public void OnMessageReceived(BaseMsg msg) {
		Log.d(TAG, "JetroSessionActivity#OnMessageReceived(...) ENTER");
		
		if (msg == null || msg.getJsonResponse() == null) {
			OnIOError("Message is null");
			return;
		}

		if (msg.getJsonResponse().getDescription() != null) {
		}
		
		switch (msg.extraHeader.MsgClassID) {
		case MessagesValues.ClassID.SessionReadyMsg:
			sendStartApplicationMsg(appToStartId);
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
			if (showKeyBoardMsgResponse.isShow()) {
				showKeyboard(true, false);
			} else {
				showKeyboard(false, false);
			}
		case MessagesValues.ClassID.Error:
			ErrCodeMsgResponse errCodeMsgResponse = (ErrCodeMsgResponse) msg.getJsonResponse();
			break;
		default:
			break;
		}
	}

	@Override
	public void OnIOError(String exception) {
		Log.d(TAG, "JetroSessionActivity#OnIOError(...) ENTER");
		
		finish();
	}
	
	private void sendStartApplicationMsg(String appToStartId) {
		Log.d(TAG, "JetroSessionActivity#sendStartApplicationMsg(...) ENTER");
		
		socketManager.sendMessage(new StartApplicationMsg(appToStartId));
	}
	
}

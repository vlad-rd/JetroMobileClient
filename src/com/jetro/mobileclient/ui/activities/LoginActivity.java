/**
 * 
 */
package com.jetro.mobileclient.ui.activities;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings.Secure;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import com.freerdp.freerdpcore.application.GlobalApp;
import com.jetro.mobileclient.R;
import com.jetro.mobileclient.config.Config;
import com.jetro.mobileclient.model.beans.Connection;
import com.jetro.mobileclient.repository.ConnectionsDB;
import com.jetro.mobileclient.ui.activities.base.HeaderActivity;
import com.jetro.mobileclient.ui.dialogs.DialogLauncher;
import com.jetro.mobileclient.utils.FilesUtils;
import com.jetro.protocol.Core.BaseMsg;
import com.jetro.protocol.Core.ClassID;
import com.jetro.protocol.Core.IMessageSubscriber;
import com.jetro.protocol.Core.Net.ClientChannel;
import com.jetro.protocol.Protocols.Controller.ConnectionPoint;
import com.jetro.protocol.Protocols.Controller.LoginMsg;

/**
 * @author ran.h
 *
 */
public class LoginActivity extends HeaderActivity implements IMessageSubscriber {
	
	private static final String TAG = LoginActivity.class.getSimpleName();
	
	private ClientChannel mClientChannel;
	
	private Connection mConnection;
	private boolean mIsWAN;
	
	private View mBaseContentLayout;
	private EditText mUsernameInput;
	private EditText mPasswordInput;
	private EditText mDomainInput;
	private ImageView mLoginImage;
	private TextView mCancelButton;
	private TextView mLoginButton;
	
	private TextWatcher mInputTextWatcher = new TextWatcher() {
		@Override
		public void onTextChanged(CharSequence s, int start, int before, int count) {
		}
		@Override
		public void beforeTextChanged(CharSequence s, int start, int count, int after) {
		}
		@Override
		public void afterTextChanged(Editable s) {
			if (areInputFieldsValid()) {
				mLoginButton.setBackgroundResource(R.drawable.orange_button_selector);
			} else {
				mLoginButton.setBackgroundResource(R.color.button_disabled_color);
			}
		}
	};

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		Log.d(TAG, TAG + "#onSaveInstanceState(...) ENTER");
		
		// Save the user's current game state
		outState.putSerializable(Config.Extras.EXTRA_CONNECTION, mConnection);
		
		// Always call the superclass so it can save the view hierarchy state
		super.onSaveInstanceState(outState);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.d(TAG, TAG + "#onCreate(...) ENTER");
		super.onCreate(savedInstanceState);
		
		// Check whether we're recreating a previously destroyed instance
		if (savedInstanceState != null) {
			// Restore value of members from saved state
			mConnection = (Connection) savedInstanceState.getSerializable(Config.Extras.EXTRA_CONNECTION);
			mIsWAN = savedInstanceState.getBoolean(Config.Extras.EXTRA_IS_WAN);
		} else {
			// Probably initialize members with default values for a new instance
			Intent intent = getIntent();
			mConnection = (Connection) intent.getSerializableExtra(Config.Extras.EXTRA_CONNECTION);
			mIsWAN = intent.getBooleanExtra(Config.Extras.EXTRA_IS_WAN, true);
		}
		
		setHeaderTitleText(R.string.header_title_login);
		mHeaderBackButton.setVisibility(View.VISIBLE);
		
		mBaseContentLayout = setBaseContentView(R.layout.activity_login);
		mUsernameInput = (EditText) mBaseContentLayout.findViewById(R.id.username_input);
		mUsernameInput.addTextChangedListener(mInputTextWatcher);
		mPasswordInput = (EditText) mBaseContentLayout.findViewById(R.id.password_input);
		mPasswordInput.addTextChangedListener(mInputTextWatcher);
		mDomainInput = (EditText) mBaseContentLayout.findViewById(R.id.domain_input);
		mDomainInput.addTextChangedListener(mInputTextWatcher);
		mDomainInput.setOnEditorActionListener(new OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				if (actionId == EditorInfo.IME_ACTION_GO) {
					makeLogin();
					return true;
				}
				return false;
			}
		});
		mLoginImage = (ImageView) mBaseContentLayout.findViewById(R.id.login_screen_image);
		mCancelButton = (TextView) mBaseContentLayout.findViewById(R.id.cancel_button);
		mCancelButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				DialogLauncher.launchCancelLoginDialog(LoginActivity.this,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								if (which == DialogInterface.BUTTON_POSITIVE) {
									Intent intent = new Intent(LoginActivity.this,
											ConnectionsListActivity.class);
									startActivity(intent);
									finish();
								}
							}
						});
			}
		});
		mLoginButton = (TextView) mBaseContentLayout.findViewById(R.id.login_button);
		mLoginButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				makeLogin();
			}
		});
		
		// Gets the user credentials from the host
		if (mConnection != null) {
			mUsernameInput.setText(mConnection.getUserName());
			mPasswordInput.setText(mConnection.getPassword());
			mDomainInput.setText(mConnection.getDomain());
			Bitmap loginImage = FilesUtils.readImage(mConnection.getLoginImageName());
			if (loginImage != null) {				
				mLoginImage.setImageBitmap(loginImage);
				mLoginImage.setVisibility(View.VISIBLE);
			}
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		
		mClientChannel = ClientChannel.getInstance();
		if (mClientChannel != null) {
			mClientChannel.AddListener(LoginActivity.this);
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		
		mClientChannel = ClientChannel.getInstance();
		if (mClientChannel != null) {
			mClientChannel.RemoveListener(LoginActivity.this);
		}
	}

	@Override
	protected void setHeader() {
	}
	
	/**
	 * Validates the connection form input fields.
	 * 
	 * @return
	 */
	private boolean areInputFieldsValid() {
		Log.d(TAG, TAG + "#areInputFieldsValid(...) ENTER");
		
		boolean areInputFieldsValid = true;

		if (TextUtils.isEmpty(mUsernameInput.getText())) {
			mUsernameInput.setError(null);
			areInputFieldsValid = false;
		}

		if (TextUtils.isEmpty(mPasswordInput.getText())) {
			mPasswordInput.setError(null);
			areInputFieldsValid = false;
		}

		if (TextUtils.isEmpty(mDomainInput.getText())) {
			mDomainInput.setError(null);
			areInputFieldsValid = false;
		}

		return areInputFieldsValid;
	}
	
	private void makeLogin() {
		Log.d(TAG, TAG + "#makeLogin(...) ENTER");
		
		startLoadingScreen();
		
		// Gets the user credentials from the login form
		String username = mUsernameInput.getText().toString();
		String password = mPasswordInput.getText().toString();
		String domain = mDomainInput.getText().toString();
		
		// Saves user credentials to connection
		mConnection.setUserName(username);
		mConnection.setPassword(password);
		mConnection.setDomain(domain);
		ConnectionsDB.getInstance(getApplicationContext()).saveConnection(mConnection);
		
		sendLoginMsg();
	}

	@Override
	public void ProcessMsg(BaseMsg msg) {
		Log.i(TAG, TAG + "#ProcessMsg(...)\n" + msg.getClass().getSimpleName() + "\n" + msg.serializeJson());
		
		// Receives LoginMsg
		if (msg.msgCalssID == ClassID.LoginMsg.ValueOf()) {
			LoginMsg loginMsg = (LoginMsg) msg;
			switch (loginMsg.returnCode) {
			case LoginMsg.LOGIN_FAILURE: {
				break;
			}
			case LoginMsg.LOGIN_SUCCESS: {
				GlobalApp.setSessionTicket(loginMsg.Ticket);
				Intent intent = new Intent(LoginActivity.this, SessionActivity.class);
				intent.putExtra(Config.Extras.EXTRA_CONNECTION, mConnection);
				startActivity(intent);
				finish();
				break;
			}
			case LoginMsg.LOGIN_RESET_PASSWORD: {
				Intent intent = new Intent(LoginActivity.this, ResetPasswordActivity.class);
				intent.putExtra(Config.Extras.EXTRA_CONNECTION, mConnection);
				startActivity(intent);
				finish();
				break;
			}
			}
		}
	}

	@Override
	public void ConnectionIsBroken() {
		Log.d(TAG, TAG + "#ConnectionIsBroken(...) ENTER");
		
		// TODO: check this code
		ClientChannel.getInstance().RemoveListener(LoginActivity.this);
		ClientChannel.getInstance().Stop();
		finish();
	}
	
	private void sendLoginMsg() {
		Log.d(TAG, TAG + "#sendLoginMsg(...) ENTER");
		
		// Gets device info
		String deviceModel = Build.MODEL;
		String deviceId = Secure.getString(getContentResolver(), Secure.ANDROID_ID);
		// Sends LoginMsg
		LoginMsg loginMsg = new LoginMsg();
		loginMsg.name = mConnection.getUserName();
		loginMsg.password = mConnection.getPassword();
		loginMsg.domain = mConnection.getDomain();
		loginMsg.deviceModel = deviceModel;
		loginMsg.deviceId = deviceId;
		
		if (mClientChannel == null) {
			Log.i(TAG, TAG + "#sendLoginMsg(...) ClientChannel = " + mClientChannel);
			ConnectionPoint connectionPoint = null;
			if (mIsWAN) {
				connectionPoint = mConnection.getWANs().iterator().next();
			} else {
				connectionPoint = mConnection.getLANs().iterator().next();
			}
			boolean isCreated = ClientChannel.Create(connectionPoint.IP, connectionPoint.Port, ClientChannel.TIME_OUT);
			Log.i(TAG, TAG + "#sendLoginMsg(...) ClientChannel isCreated = " + isCreated);
			if (isCreated) {
				mClientChannel = ClientChannel.getInstance();
				mClientChannel.AddListener(LoginActivity.this);
				mClientChannel.SendReceiveAsync(loginMsg);
				// Saves this connection point as last used one
				mConnection.setLastConnectionPoint(connectionPoint);
				ConnectionsDB.getInstance(getApplicationContext()).saveConnection(mConnection);
			} else {
				stopLoadingScreen();
				DialogLauncher.launchNetworkConnectionIssueDialog(LoginActivity.this, null);
			}
		} else {
			Log.i(TAG, TAG + "#sendLoginMsg(...) ClientChannel = " + mClientChannel);
			mClientChannel.SendAsync(loginMsg);
		}
		
	}
	
}

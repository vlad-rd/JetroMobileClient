/**
 * 
 */
package com.jetro.mobileclient.ui.activities;

import android.content.DialogInterface;
import android.content.Intent;
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
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import com.freerdp.freerdpcore.presentation.SessionActivity;
import com.jetro.mobileclient.R;
import com.jetro.mobileclient.model.beans.Host;
import com.jetro.mobileclient.repository.ConnectionsDB;
import com.jetro.mobileclient.ui.activities.base.HeaderActivity;
import com.jetro.mobileclient.ui.dialogs.DialogLauncher;
import com.jetro.mobileclient.utils.Config;
import com.jetro.protocol.Core.BaseMsg;
import com.jetro.protocol.Core.ClassID;
import com.jetro.protocol.Core.IMessageSubscriber;
import com.jetro.protocol.Core.Net.ClientChannel;
import com.jetro.protocol.Protocols.Controller.LoginMsg;

/**
 * @author ran.h
 *
 */
public class LoginActivity extends HeaderActivity implements IMessageSubscriber {
	
	private static final String TAG = LoginActivity.class.getSimpleName();
	
	private ClientChannel mClientChannel;
	
	private Host mHost;
	private boolean mIsWAN;
	
	private View mBaseContentLayout;
	private EditText mUsernameInput;
	private EditText mPasswordInput;
	private EditText mDomainInput;
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
		// Save the user's current game state
		outState.putSerializable(Config.Extras.EXTRA_HOST, mHost);
		
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
			mHost = (Host) savedInstanceState.getSerializable(Config.Extras.EXTRA_HOST);
			mIsWAN = savedInstanceState.getBoolean(Config.Extras.EXTRA_IS_WAN);
		} else {
			// Probably initialize members with default values for a new instance
			Intent intent = getIntent();
			mHost = (Host) intent.getSerializableExtra(Config.Extras.EXTRA_HOST);
			mIsWAN = intent.getBooleanExtra(Config.Extras.EXTRA_IS_WAN, true);
		}
		
		setHeaderTitleText(R.string.header_title_Login);
		mHeaderBackButton.setVisibility(View.VISIBLE);
		
		mBaseContentLayout = setBaseContentView(R.layout.login_activity_layout);
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
		if (mHost != null) {
			mUsernameInput.setText(mHost.getUserName());
			mPasswordInput.setText(mHost.getPassword());
			mDomainInput.setText(mHost.getDomain());
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
		
		// Saves user credentials to host
		mHost.setUserName(username);
		mHost.setPassword(password);
		mHost.setDomain(domain);
		ConnectionsDB.getInstance(getApplicationContext()).saveHost(mHost);
		
		// Gets device info
		String model = Build.MODEL;
		String deviceId = Secure.getString(getContentResolver(), Secure.ANDROID_ID);
		
		// Sends LoginMsg
		LoginMsg loginMsg = new LoginMsg();
		loginMsg.setName(username);
		loginMsg.setPassword(password);
		loginMsg.setDomain(domain);
		loginMsg.setDeviceModel(model);
		loginMsg.setDeviceId(deviceId);
		mClientChannel.SendReceive(loginMsg, ClientChannel.TIME_OUT);
	}

	@Override
	public void ProcessMsg(BaseMsg msg) {
		Log.i(TAG, TAG + "#ProcessMsg(...)\n" + msg.serializeJson());
		
		// Receives LoginMsg
		if (msg.msgCalssID == ClassID.LoginMsg.ValueOf()) {
			LoginMsg loginMsg = (LoginMsg) msg;
			int returnCode = loginMsg.getReturnCode();
			if (returnCode == LoginMsg.LOGIN_SUCCESS) {
				Intent intent = new Intent(LoginActivity.this, SessionActivity.class);
				startActivity(intent);
				finish();
			}
		}
	}

	@Override
	public void ConnectionIsBroken() {
		// TODO Auto-generated method stub
		
	}
	
}

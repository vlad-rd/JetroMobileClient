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
import com.jetro.mobileclient.ui.activities.ResetPasswordActivity.State;
import com.jetro.mobileclient.ui.activities.base.HeaderActivity;
import com.jetro.mobileclient.ui.dialogs.DialogLauncher;
import com.jetro.mobileclient.utils.ClientChannelUtils;
import com.jetro.mobileclient.utils.FilesUtils;
import com.jetro.mobileclient.utils.KeyboardUtils;
import com.jetro.protocol.Core.BaseMsg;
import com.jetro.protocol.Core.ClassID;
import com.jetro.protocol.Core.IConnectionCreationSubscriber;
import com.jetro.protocol.Core.IMessageSubscriber;
import com.jetro.protocol.Core.Net.ClientChannel;
import com.jetro.protocol.Protocols.Controller.ConnectionPoint;
import com.jetro.protocol.Protocols.Controller.LoginMsg;
import com.jetro.protocol.Protocols.Controller.ConnectionPoint.ConnectionModeType;
import com.jetro.protocol.Protocols.Generic.ErrorMsg;

/**
 * @author ran.h
 *
 */
public class LoginActivity extends HeaderActivity implements IConnectionCreationSubscriber, IMessageSubscriber {
	
	private static final String TAG = LoginActivity.class.getSimpleName();
	
	private ClientChannel mClientChannel;
	
	private ConnectionModeType mConnectionMode;
	private Connection mConnection;
	
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
		outState.putSerializable(Config.Extras.EXTRA_CONNECTION_MODE, mConnectionMode);
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
			mConnectionMode = (ConnectionModeType) savedInstanceState.getSerializable(Config.Extras.EXTRA_CONNECTION_MODE);
			mConnection = (Connection) savedInstanceState.getSerializable(Config.Extras.EXTRA_CONNECTION);
		} else {
			// Probably initialize members with default values for a new instance
			Intent intent = getIntent();
			mConnectionMode = (ConnectionModeType) intent.getSerializableExtra(Config.Extras.EXTRA_CONNECTION_MODE);
			mConnection = (Connection) intent.getSerializableExtra(Config.Extras.EXTRA_CONNECTION);
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
					KeyboardUtils.hide(LoginActivity.this, mDomainInput, 0);
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
									dialog.dismiss();
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
				// Disables the login button
				mLoginButton.setEnabled(false);
				makeLogin();
			}
		});
		
		// Gets the user credentials from the host
		if (mConnection != null) {
			String userName = mConnection.getUserName();
			String password = mConnection.getPassword();
			String domain = mConnection.getDomain();
			
			// TODO: refactor this after test
			if (!TextUtils.isEmpty(userName)) {
				mUsernameInput.setText(userName);
			} else {
				mUsernameInput.setText("android_user");
			}
			if (!TextUtils.isEmpty(password)) {
				mPasswordInput.setText(password);
			} else {
				mPasswordInput.setText("Welcome");
			}
			if (!TextUtils.isEmpty(domain)) {
				mDomainInput.setText(domain);
			} else {
				mDomainInput.setText("jp");
			}
			
			Bitmap loginImage = FilesUtils.readImage(mConnection.getLoginImageName());
			if (loginImage != null) {
				mLoginImage.setImageBitmap(loginImage);
				mLoginImage.setVisibility(View.VISIBLE);
			}
		}
	}

	@Override
	protected void onResume() {
		Log.d(TAG, TAG + "#onResume(...) ENTER");
		super.onResume();
		
		mClientChannel = ClientChannel.getInstance();
		if (mClientChannel != null) {
			mClientChannel.AddListener(LoginActivity.this);
		}
	}

	@Override
	protected void onPause() {
		Log.d(TAG, TAG + "#onPause(...) ENTER");
		super.onPause();
		
		mClientChannel = ClientChannel.getInstance();
		if (mClientChannel != null) {
			mClientChannel.RemoveListener(LoginActivity.this);
		}
	}

	@Override
	protected void onStart() {
		Log.d(TAG, TAG + "#onStart(...) ENTER");
		super.onStart();
		
		mPasswordInput.setText("");
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
		sendLoginMsg();
	}
	
	@Override
	public void ConnectionCreated(boolean result, final String message) {
		Log.d(TAG, TAG + "#ConnectionCreated(...) ENTER");
	
//		runOnUiThread(new Runnable() {
//			@Override
//			public void run() {
//				DialogLauncher.launchServerErrorOneButtonDialog(LoginActivity.this, message, null);
//			}
//		});
	}
	
	@Override
	public void ProcessMsg(BaseMsg msg) {
		Log.i(TAG, TAG + "#ProcessMsg(...)\n" + msg.getClass().getSimpleName() + "\n" + msg.serializeJson());
		
		// Receives LoginMsg
		if (msg.msgCalssID == ClassID.LoginMsg.ValueOf()) {
			LoginMsg loginMsg = (LoginMsg) msg;
			switch (loginMsg.returnCode) {
			case LoginMsg.LOGIN_SUCCESS: {
				GlobalApp.setSessionTicket(loginMsg.Ticket);
				saveUserCredentials();
				launchSessionActivity();
				break;
			}
			case LoginMsg.LOGIN_RESET_PASSWORD: {
				stopLoadingScreen();
				// Enables the login button
				mLoginButton.setEnabled(true);
				
				launchResetPasswordActivity(State.PASSWORD_RESET_REQUIRED);
				break;
			}
			case LoginMsg.LOGIN_OPTIONAL_CHANGE_PASSWORD: {
				stopLoadingScreen();
				
				GlobalApp.setSessionTicket(loginMsg.Ticket);
				saveUserCredentials();
				DialogLauncher.launchOptionalChangePassword(
						LoginActivity.this,
						loginMsg.daysBeforePasswordExpiration,
						new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						// Enables the login button
						mLoginButton.setEnabled(true);
						if (which == DialogInterface.BUTTON_POSITIVE) {
							dialog.dismiss();
							launchResetPasswordActivity(State.PASSWORD_RESET_OPTIONAL);
						} else if (which == DialogInterface.BUTTON_NEGATIVE) {
							dialog.dismiss();
							launchSessionActivity();
						}
					}
				});
				break;
			}
			case LoginMsg.LOGIN_FAILURE: {
				ClientChannelUtils.stopClientChannel(mClientChannel, LoginActivity.this);
				break;
			}
			}
		// Receives ErrorMsg
		} else if (msg.msgCalssID == ClassID.Error.ValueOf()) {
			stopLoadingScreen();
			
			ClientChannelUtils.stopClientChannel(mClientChannel, LoginActivity.this);
			
			ErrorMsg errorMsg = (ErrorMsg) msg;
			switch (errorMsg.Err) {
			case ErrorMsg.ERROR_INVALID_USER_CREDENTIALS:
				DialogLauncher.launchServerErrorTwoButtonsDialog(LoginActivity.this,
						errorMsg.Description,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								if (which == DialogInterface.BUTTON_NEGATIVE) {
									dialog.dismiss();
									finish();
								}
							}
						});
				break;
			case ErrorMsg.ERROR_TIMEOUT:
				DialogLauncher.launchServerErrorTwoButtonsDialog(LoginActivity.this,
						errorMsg.Description,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								// Retry
								if (which == DialogInterface.BUTTON_POSITIVE) {
									dialog.dismiss();
									sendLoginMsg();
								// Cancel
								} else if (which == DialogInterface.BUTTON_NEGATIVE) {
									dialog.dismiss();
									finish();
								}
							}
						});
				break;
			case ErrorMsg.ERROR_UNEXPECTED:
				DialogLauncher.launchServerErrorTwoButtonsDialog(LoginActivity.this,
						errorMsg.Description, new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								if (which == DialogInterface.BUTTON_NEGATIVE) {
									dialog.dismiss();
									finish();
								}
							}
						});
				break;
			}
			
			// Enables the login button
			mLoginButton.setEnabled(true);
		}
	}

	@Override
	public void ConnectionIsBroken() {
		Log.d(TAG, TAG + "#ConnectionIsBroken(...) ENTER");
		
		ClientChannelUtils.stopClientChannel(mClientChannel, LoginActivity.this);
	}
	
	private void saveUserCredentials() {
		// Gets the user credentials from the login form
		String username = mUsernameInput.getText().toString().trim();
		String password = mPasswordInput.getText().toString().trim();
		String domain = mDomainInput.getText().toString().trim();
		
		// Saves user credentials to connection
		mConnection.setUserName(username);
		mConnection.setPassword(password);
		mConnection.setDomain(domain);
		ConnectionsDB.getInstance(getApplicationContext()).saveConnection(mConnection);
	}
	
	private void launchSessionActivity() {
		Intent intent = new Intent(LoginActivity.this, SessionActivity.class);
		intent.putExtra(Config.Extras.EXTRA_CONNECTION, mConnection);
		startActivity(intent);
		finish();
	}
	
	private void launchResetPasswordActivity(ResetPasswordActivity.State activityState) {
		Intent intent = new Intent(LoginActivity.this, ResetPasswordActivity.class);
		intent.putExtra(Config.Extras.EXTRA_RESET_PASSWORD_ACTIVITY_STATE, activityState);
		intent.putExtra(Config.Extras.EXTRA_CONNECTION, mConnection);
		startActivity(intent);
	}
	
	private void sendLoginMsg() {
		Log.d(TAG, TAG + "#sendLoginMsg(...) ENTER");
		
		// Gets device info
		String deviceModel = Build.MODEL;
		String deviceId = Secure.getString(getContentResolver(), Secure.ANDROID_ID);
		// Gets the user credentials from the login form
		String username = mUsernameInput.getText().toString().trim();
		String password = mPasswordInput.getText().toString().trim();
		String domain = mDomainInput.getText().toString().trim();
		// Sends LoginMsg
		LoginMsg loginMsg = new LoginMsg();
		loginMsg.name = username;
		loginMsg.password = password;
		loginMsg.domain = domain;
		loginMsg.deviceModel = deviceModel;
		loginMsg.deviceId = deviceId;
		
		// TODO: client channel connection to host fallback logic
		ConnectionPoint connectionPoint = null;
		switch (mConnectionMode) {
		case SSL:
			connectionPoint = mConnection.getWANs().iterator().next();
			break;
		case DIRECT:
			connectionPoint = mConnection.getLANs().iterator().next();
			break;
		default:
			throw new IllegalStateException(
					"Invalid connection.getPreferedConnectionMode() = "
							+ mConnection.getPreferedConnectionMode());
		}
		
		Log.i(TAG, TAG + "#sendLoginMsg(...) Connecting to HOST IP: " + connectionPoint.IP);
		Log.i(TAG, TAG + "#sendLoginMsg(...) Connecting to HOST PORT: " + connectionPoint.Port);
		Log.i(TAG, TAG + "#sendLoginMsg(...) Connecting to CONNECTION MODE: " + connectionPoint.ConnectionMode);
		
		boolean isCreated = ClientChannelUtils.createClientChannel(
				connectionPoint.IP,
				connectionPoint.Port,
				connectionPoint.ConnectionMode,
				LoginActivity.this);
		Log.i(TAG, TAG + "#sendLoginMsg(...) ClientChannel isCreated = " + isCreated);
		if (isCreated) {
			mClientChannel = ClientChannel.getInstance();
			if (mClientChannel == null) {
				Log.i(TAG, TAG + "#sendLoginMsg(...) mClientChannel = " + mClientChannel);
				stopLoadingScreen();
				launchConnectionIssueDialog();
				return;
			}
			mClientChannel.AddListener(LoginActivity.this);
			mClientChannel.SendAsyncTimeout(loginMsg, ClientChannel.TIME_OUT);
			// Saves this connection point as last used one
			mConnection.setLastConnectionPoint(connectionPoint);
			ConnectionsDB.getInstance(getApplicationContext()).saveConnection(mConnection);
		} else {
			stopLoadingScreen();
			launchConnectionIssueDialog();
		}
	}
	
	private void launchConnectionIssueDialog() {
		DialogLauncher.launchNetworkConnectionIssueDialog(LoginActivity.this, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				// Enables the login button
				mLoginButton.setEnabled(true);
			}
		});
	}
	
}

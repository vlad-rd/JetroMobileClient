/**
 * 
 */
package com.jetro.mobileclient.ui.activities;

import java.util.ArrayList;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
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

import com.freerdp.freerdpcore.sharedobjects.ConnectionPoint;
import com.jetro.mobileclient.R;
import com.jetro.mobileclient.ui.HeaderActivity;
import com.jetro.mobileclient.ui.dialogs.DialogLauncher;
import com.jetro.mobileclient.utils.Config;

/**
 * @author ran.h
 *
 */
public class LoginActivity extends HeaderActivity {
	
	private static final String TAG = LoginActivity.class.getSimpleName();
	
	private ArrayList<ConnectionPoint> mConnectionsPoints;
	
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
		outState.putParcelableArrayList(Config.Extras.EXTRA_CONNECTIONS_POINTS, mConnectionsPoints);
		
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
			mConnectionsPoints = savedInstanceState.getParcelableArrayList(Config.Extras.EXTRA_CONNECTIONS_POINTS);
		} else {
			// Probably initialize members with default values for a new instance
			Intent intent = getIntent();
			mConnectionsPoints = intent.getParcelableArrayListExtra(Config.Extras.EXTRA_CONNECTIONS_POINTS);
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
		
		loadUserCredentials();
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

		if (!TextUtils.isEmpty(mPasswordInput.getText())) {
			mPasswordInput.setError(null);
			areInputFieldsValid = false;
		}

		if (!TextUtils.isEmpty(mDomainInput.getText())) {
			mDomainInput.setError(null);
			areInputFieldsValid = false;
		}

		return areInputFieldsValid;
	}
	
	/**
	 * Saves the user credentials to the preferences
	 * from the user login from.
	 * 
	 * @param username
	 * @param password
	 * @param domain
	 */
	private void saveUserCredentials(String username, String password, String domain) {
		Log.d(TAG, TAG + "#saveUserCredentials(...) ENTER");
		
		SharedPreferences userCredentialsPrefs = getSharedPreferences(Config.Prefs.PREFS_USER_CREDENTIALS, MODE_PRIVATE);
		Editor editor = userCredentialsPrefs.edit();
		editor.putString(Config.Prefs.PREF_KEY_USER_NAME, username);
		editor.putString(Config.Prefs.PREF_KEY_PASSWORD, password);
		editor.putString(Config.Prefs.PREF_KEY_DOMAIN, domain);
		editor.commit();
	}
	
	/**
	 * Loads the user credentials from the preferences
	 * to the user login form.
	 */
	private void loadUserCredentials() {
		Log.d(TAG, TAG + "#loadUserCredentials(...) ENTER");
		
		SharedPreferences userCredentialsPrefs = getSharedPreferences(Config.Prefs.PREFS_USER_CREDENTIALS, MODE_PRIVATE);
		String username = userCredentialsPrefs.getString(Config.Prefs.PREF_KEY_USER_NAME, "");
		String password = userCredentialsPrefs.getString(Config.Prefs.PREF_KEY_PASSWORD, "");
		String domain = userCredentialsPrefs.getString(Config.Prefs.PREF_KEY_DOMAIN, "");
		mUsernameInput.setText(username);
		mPasswordInput.setText(password);
		mDomainInput.setText(domain);
	}
	
	private void makeLogin() {
		Log.d(TAG, TAG + "#makeLogin(...) ENTER");
		
		startLoadingScreen();
		
		// Gets the user credentials from the login form
		String username = mUsernameInput.getText().toString();
		String password = mPasswordInput.getText().toString();
		String domain = mDomainInput.getText().toString();
		
		// TODO: save user credenticals
		saveUserCredentials(username, password, domain);
	}
	
}

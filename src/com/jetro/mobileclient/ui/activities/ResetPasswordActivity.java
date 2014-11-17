/**
 * 
 */
package com.jetro.mobileclient.ui.activities;

import java.util.ArrayList;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import com.freerdp.freerdpcore.sharedobjects.ConnectionPoint;
import com.jetro.mobileclient.R;
import com.jetro.mobileclient.ui.activities.base.HeaderActivity;
import com.jetro.mobileclient.utils.Config;

/**
 * @author ran.h
 *
 */
public class ResetPasswordActivity extends HeaderActivity {

	private static final String TAG = ResetPasswordActivity.class
			.getSimpleName();
	
	private ArrayList<ConnectionPoint> mConnectionsPoints;
	
	private View mBaseContentLayout;
	private EditText mOldPasswordInput;
	private EditText mNewPasswordInput;
	private EditText mConfirmPasswordInput;
	private TextView mResetButton;
	
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
				mResetButton.setBackgroundResource(R.drawable.orange_button_selector);
			} else {
				mResetButton.setBackgroundResource(R.color.button_disabled_color);
			}
		}
	};
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		Log.d(TAG, TAG + "#onSaveInstanceState(...) ENTER");
		
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
			mConnectionsPoints = getIntent().getParcelableArrayListExtra(Config.Extras.EXTRA_CONNECTIONS_POINTS);
		}
		
		setHeaderTitleText(R.string.header_title_ResetPassword);
		mHeaderBackButton.setVisibility(View.VISIBLE);
		
		mBaseContentLayout = setBaseContentView(R.layout.reset_password_activity_layout);
		mOldPasswordInput = (EditText) mBaseContentLayout.findViewById(R.id.old_password_input);
		mOldPasswordInput.addTextChangedListener(mInputTextWatcher);
		mNewPasswordInput = (EditText) mBaseContentLayout.findViewById(R.id.new_password_input);
		mNewPasswordInput.addTextChangedListener(mInputTextWatcher);
		mConfirmPasswordInput = (EditText) mBaseContentLayout.findViewById(R.id.confirm_new_password_input);
		mConfirmPasswordInput.addTextChangedListener(mInputTextWatcher);
		mConfirmPasswordInput.setOnEditorActionListener(new OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				if (actionId == EditorInfo.IME_ACTION_GO) {
					resetPassword();
					return true;
				}
				return false;
			}
		});
		
		loadOldPassword();
	}
	
	@Override
	protected void setHeader() {
		// TODO Auto-generated method stub
		
	}
	
	/**
	 * Validates the connection form input fields.
	 * 
	 * @return
	 */
	private boolean areInputFieldsValid() {
		Log.d(TAG, TAG + "#areInputFieldsValid(...) ENTER");
		
		boolean areInputFieldsValid = true;

		if (TextUtils.isEmpty(mOldPasswordInput.getText())) {
			mOldPasswordInput.setError(null);
			areInputFieldsValid = false;
		}

		if (TextUtils.isEmpty(mNewPasswordInput.getText())) {
			mNewPasswordInput.setError(null);
			areInputFieldsValid = false;
		}

		if (TextUtils.isEmpty(mConfirmPasswordInput.getText())) {
			mConfirmPasswordInput.setError(null);
			areInputFieldsValid = false;
		}

		return areInputFieldsValid;
	}
	
	/**
	 * Loads the user old password from the preferences
	 * to the user reset password form.
	 */
	private void loadOldPassword() {
		Log.d(TAG, TAG + "#loadUserCredentials(...) ENTER");
		
		SharedPreferences userCredentialsPrefs = getSharedPreferences(Config.Prefs.PREFS_USER_CREDENTIALS, MODE_PRIVATE);
		String password = userCredentialsPrefs.getString(Config.Prefs.PREF_KEY_PASSWORD, "");
		mOldPasswordInput.setText(password);
	}
	
	private void resetPassword() {
		Log.d(TAG, TAG + "#resetPassword(...) ENTER");
		
		String oldPassword = mOldPasswordInput.getText().toString().trim();
		String newPassword = mNewPasswordInput.getText().toString().trim();
		
		String userName = mConnectionsPoints.get(0).getUserName();
		String domain = mConnectionsPoints.get(0).getDomain();
	}

}

/**
 * 
 */
package com.jetro.mobileclient.ui.activities;

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

import com.jetro.mobileclient.R;
import com.jetro.mobileclient.config.Config;
import com.jetro.mobileclient.model.beans.Connection;
import com.jetro.mobileclient.ui.activities.base.HeaderActivity;

/**
 * @author ran.h
 *
 */
public class ResetPasswordActivity extends HeaderActivity {

	private static final String TAG = ResetPasswordActivity.class
			.getSimpleName();
	
	private Connection mConnection;
	
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
		} else {
			// Probably initialize members with default values for a new instance
			mConnection = (Connection) getIntent().getSerializableExtra(Config.Extras.EXTRA_CONNECTION);
		}
		
		setHeaderTitleText(R.string.header_title_reset_password);
		mHeaderBackButton.setVisibility(View.VISIBLE);
		
		mBaseContentLayout = setBaseContentView(R.layout.activity_reset_password_layout);
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
		
		// Loads old password from host
		if (mConnection != null) {
			mOldPasswordInput.setText(mConnection.getPassword());
		}
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
	
	private void resetPassword() {
		Log.d(TAG, TAG + "#resetPassword(...) ENTER");
		
		String oldPassword = mOldPasswordInput.getText().toString().trim();
		String newPassword = mNewPasswordInput.getText().toString().trim();
		
		String userName = mConnection.getUserName();
		String domain = mConnection.getDomain();
		
		// TODO: save the new password to host
	}

}

/**
 * 
 */
package com.jetro.mobileclient.ui.activities;

import java.util.ArrayList;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import com.freerdp.freerdpcore.sharedobjects.ConnectionPoint;
import com.jetro.mobileclient.R;
import com.jetro.mobileclient.repository.ConnectionsDB;
import com.jetro.mobileclient.ui.HeaderActivity;
import com.jetro.mobileclient.utils.Config;
import com.jetro.protocol.Core.Net.ClientChannel;

/**
 * @author ran.h
 *
 */
public class ConnectionActivity extends HeaderActivity {
	
	private static final String TAG = ConnectionActivity.class.getSimpleName();
	
	private ArrayList<ConnectionPoint> mConnectionsPoints;
	
	private View mBaseContentLayout;
	private ImageView mHostNameStar;
	private EditText mHostNameInput;
	private ImageView mHostIpStar;
	private EditText mHostIpInput;
	private ImageView mHostPortStar;
	private EditText mHostPortInput;
	private String[] mConnectionsModes;
	private Spinner mConnectionModeSpinner;
	private TextView mConnectionModeLabel;
	private EditText mConnectionModeInput;
	private String mSelectedConnectionMode;
	private ImageView mLoginScreenImage;
	private TextView mConnectButton;
	
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
				mConnectButton.setBackgroundResource(R.drawable.orange_button_selector);
			} else {
				mConnectButton.setBackgroundResource(R.color.button_disabled_color);
			}
		}
	};
	
	/**
	 * This class represents the connection activity state.
	 * Connection activity to add connection.
	 * Connection activity to view connection.
	 * 
	 * @author ran.h
	 *
	 */
	public enum State {
		ADD_CONNECTION,
		VIEW_CONNECTION
	}
	
	private State mState = State.ADD_CONNECTION;
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		// Save the user's current game state
		outState.putSerializable(Config.Extras.EXTRA_CONNECTION_ACTIVITY_STATE, mState);
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
			mState = (State) savedInstanceState.getSerializable(Config.Extras.EXTRA_CONNECTION_ACTIVITY_STATE);
			mConnectionsPoints = savedInstanceState.getParcelableArrayList(Config.Extras.EXTRA_CONNECTIONS_POINTS);
		} else {
			// Probably initialize members with default values for a new instance
			Intent intent = getIntent();
			Bundle extras = intent.getExtras();
			mState = (State) extras.getSerializable(Config.Extras.EXTRA_CONNECTION_ACTIVITY_STATE);
			mConnectionsPoints = getIntent().getParcelableArrayListExtra(Config.Extras.EXTRA_CONNECTIONS_POINTS);
		}
		
		mBaseContentLayout = setBaseContentView(R.layout.new_connection_activit_layout);
		mHostNameInput = (EditText) mBaseContentLayout.findViewById(R.id.host_name_input);
		mHostNameInput.addTextChangedListener(mInputTextWatcher);
		mHostIpInput = (EditText) mBaseContentLayout.findViewById(R.id.host_ip_input);
		mHostIpInput.addTextChangedListener(mInputTextWatcher);
		mHostPortInput = (EditText) mBaseContentLayout.findViewById(R.id.host_port_input);
		mHostPortInput.addTextChangedListener(mInputTextWatcher);
		mHostPortInput.setOnEditorActionListener(new OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				if (actionId == EditorInfo.IME_ACTION_GO) {
					openSocket();
					return true;
				}
				return false;
			}
		});
		mLoginScreenImage = (ImageView) mBaseContentLayout.findViewById(R.id.login_screen_image);
		mConnectButton = (TextView) mBaseContentLayout.findViewById(R.id.connect_button);
		mConnectButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				openSocket();
			}
		});
		
		switch (mState) {
		case ADD_CONNECTION:
			// Sets the header title
			setHeaderTitleText(R.string.header_title_AddConnection);
			boolean hasConnections = ConnectionsDB.getAllSavedConnections().size() != 0;
			if (hasConnections) {
				mHeaderBackButton.setVisibility(View.VISIBLE);
			} else {
				mHeaderBackButton.setVisibility(View.INVISIBLE);
			}
			
			mConnectionsModes = getResources().getStringArray(R.array.connection_mode_options);
			mConnectionModeSpinner = (Spinner) mBaseContentLayout.findViewById(R.id.connection_mode_spinner);
			mConnectionModeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
						@Override
						public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
							mSelectedConnectionMode = mConnectionsModes[position];
						}
						@Override
						public void onNothingSelected(AdapterView<?> parent) {
						}
					});
			
			// TODO: remove this after debug
			mHostNameInput.setText("Test environment");
			mHostIpInput.setText("212.199.106.213");
			mHostPortInput.setText("13000");
			break;
		case VIEW_CONNECTION:
			// Sets the header title
			setHeaderTitleText(R.string.header_title_ViewConnection);
			mHeaderBackButton.setVisibility(View.VISIBLE);
			
			// Hides the required input fields indicators (stars)
			mHostNameStar = (ImageView) mBaseContentLayout.findViewById(R.id.host_name_star);
			mHostIpStar = (ImageView) mBaseContentLayout.findViewById(R.id.host_ip_star);
			mHostPortStar = (ImageView) mBaseContentLayout.findViewById(R.id.host_port_star);
			mHostNameStar.setVisibility(View.INVISIBLE);
			mHostIpStar.setVisibility(View.INVISIBLE);
			mHostPortStar.setVisibility(View.INVISIBLE);
			
			// Shows view connection state specific widgets
			mConnectionModeLabel = (TextView) mBaseContentLayout.findViewById(R.id.connection_mode_label);
			mConnectionModeInput = (EditText) mBaseContentLayout.findViewById(R.id.connection_mode_input);
			mConnectionModeLabel.setVisibility(View.VISIBLE);
			mConnectionModeInput.setVisibility(View.VISIBLE);
			
			// Disables the input fields
			mHostNameInput.setEnabled(false);
			mHostIpInput.setEnabled(false);
			mHostPortInput.setEnabled(false);
			mConnectionModeInput.setEnabled(false);
			
			// Fills the input fields
			ConnectionPoint lastConnectionPoint = mConnectionsPoints.get(0);
			mHostNameInput.setText(lastConnectionPoint.getName());
			mHostIpInput.setText(lastConnectionPoint.getIP());
			mHostPortInput.setText(String.valueOf(lastConnectionPoint.getPort()));
			mConnectionModeInput.setText(lastConnectionPoint.getConnectionMode());
			
			break;
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

		if (TextUtils.isEmpty(mHostNameInput.getText())) {
			mHostNameInput.setError(null);
			areInputFieldsValid = false;
		}

		if (!TextUtils.isEmpty(mHostIpInput.getText())) {
			mHostIpInput.setError(null);
			areInputFieldsValid = false;
		}

		if (!TextUtils.isEmpty(mHostPortInput.getText())) {
			mHostPortInput.setError(null);
			areInputFieldsValid = false;
		}

		return areInputFieldsValid;
	}
	
	/**
	 * Connects to the Cockpit server by open socket.
	 */
	private void openSocket() {
		Log.d(TAG, TAG + "#openSocket(...) ENTER");
		
		startLoadingScreen();
		
		String hostIp = mHostNameInput.getText().toString();
		int hostPort = Integer.valueOf(mHostPortInput.getText().toString());
		
		Log.i(TAG, "ConnectionActivity#openSocket(...) host: " + hostIp);
		Log.i(TAG, "ConnectionActivity#openSocket(...) port: " + hostPort);
		
		ClientChannel clientChannel = ClientChannel.getInstance();
		
//		clientChannel.Connect(hostIp, hostPort);
		
//		DisplayMetrics displayMetrics = DisplayUtils.getDisplayMetrics(ConnectionActivity.this);
//		
//		// Sends CockpitSiteInfoMsg
//		CockpitSiteInfoMsg msgCSI = new CockpitSiteInfoMsg(displayMetrics.widthPixels, displayMetrics.heightPixels);
//		CockpitSiteInfoMsg respCSI = (CockpitSiteInfoMsg) clientChannel.SendReceive(msgCSI, ClientChannel.TIME_OUT);
//		if(respCSI != null) 
//			System.out.println(respCSI.toString());
//		else
//		{
//			System.out.println("null received instead of CockpitSiteInfoMsg");
//			clientChannel.Stop();
//			return;
//		}
	}

}

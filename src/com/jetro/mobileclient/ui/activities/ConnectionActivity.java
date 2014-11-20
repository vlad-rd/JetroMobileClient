/**
 * 
 */
package com.jetro.mobileclient.ui.activities;

import java.io.File;
import java.util.Iterator;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import com.jetro.mobileclient.R;
import com.jetro.mobileclient.config.Config;
import com.jetro.mobileclient.model.beans.Connection;
import com.jetro.mobileclient.repository.ConnectionsDB;
import com.jetro.mobileclient.ui.activities.base.HeaderActivity;
import com.jetro.mobileclient.utils.FilesUtils;
import com.jetro.protocol.Core.BaseMsg;
import com.jetro.protocol.Core.ClassID;
import com.jetro.protocol.Core.IMessageSubscriber;
import com.jetro.protocol.Core.Net.ClientChannel;
import com.jetro.protocol.Protocols.Controller.CockpitSiteInfoMsg;
import com.jetro.protocol.Protocols.Controller.ConnectionPoint;
import com.jetro.protocol.Protocols.Controller.LoginScreenImageMsg;

/**
 * @author ran.h
 *
 */
public class ConnectionActivity extends HeaderActivity implements IMessageSubscriber {
	
	private static final String TAG = ConnectionActivity.class.getSimpleName();
	
	private ClientChannel mClientChannel;
	
	private Connection mConnection;
	
	private View mBaseContentLayout;
	private ImageView mConnectionNameStar;
	private EditText mConnectionNameInput;
	private ImageView mHostIpStar;
	private EditText mHostIpInput;
	private ImageView mHostPortStar;
	private EditText mHostPortInput;
	private String[] mConnectionsModes;
	private ViewGroup mConnectionModeSpinnerWrapper;
	private Spinner mConnectionModeSpinner;
	private EditText mConnectionModeInput;
	private String mSelectedConnectionMode;
	private TextView mCancelButton;
	private View mDividerHorizontal;
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
			mState = (State) savedInstanceState.getSerializable(Config.Extras.EXTRA_CONNECTION_ACTIVITY_STATE);
			mConnection = (Connection) savedInstanceState.getSerializable(Config.Extras.EXTRA_CONNECTION);
		} else {
			// Probably initialize members with default values for a new instance
			Intent intent = getIntent();
			Bundle extras = intent.getExtras();
			mState = (State) extras.getSerializable(Config.Extras.EXTRA_CONNECTION_ACTIVITY_STATE);
			mConnection = (Connection) extras.getSerializable(Config.Extras.EXTRA_CONNECTION);
		}
		
		mConnectionsModes = getResources().getStringArray(R.array.connection_mode_options);
		
		mBaseContentLayout = setBaseContentView(R.layout.activity_new_connection);
		mConnectionNameInput = (EditText) mBaseContentLayout.findViewById(R.id.connection_name_input);
		mConnectionNameInput.addTextChangedListener(mInputTextWatcher);
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
		mConnectionModeInput = (EditText) mBaseContentLayout.findViewById(R.id.connection_mode_input);
		mCancelButton = (TextView) mBaseContentLayout.findViewById(R.id.cancel_button);
		mDividerHorizontal = (View) mBaseContentLayout.findViewById(R.id.divider_horizontal);
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
			boolean hasHosts = ConnectionsDB.getInstance(getApplicationContext()).hasConnections();
			if (hasHosts) {
				mHeaderBackButton.setVisibility(View.VISIBLE);
			} else {
				mHeaderBackButton.setVisibility(View.INVISIBLE);
			}
			
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
			
			// Hides view connection state specific widgets
			mConnectionModeInput.setVisibility(View.GONE);
			// Hides the cancel button
			mCancelButton.setVisibility(View.GONE);
			// Hides the horizontal divider between the buttons
			mDividerHorizontal.setVisibility(View.GONE);
			
			// TODO: remove this after debug
			mConnectionNameInput.setText("Test environment");
			mHostIpInput.setText("212.199.106.213");
			mHostPortInput.setText("13000");
			break;
		case VIEW_CONNECTION:
			// Sets the header title
			setHeaderTitleText(R.string.header_title_ViewConnection);
			mHeaderBackButton.setVisibility(View.VISIBLE);
			
			// Hides the required input fields indicators (stars)
			mConnectionNameStar = (ImageView) mBaseContentLayout.findViewById(R.id.connection_name_star);
			mHostIpStar = (ImageView) mBaseContentLayout.findViewById(R.id.host_ip_star);
			mHostPortStar = (ImageView) mBaseContentLayout.findViewById(R.id.host_port_star);
			mConnectionNameStar.setVisibility(View.INVISIBLE);
			mHostIpStar.setVisibility(View.INVISIBLE);
			mHostPortStar.setVisibility(View.INVISIBLE);
			// Hides the connection mode spinner
			mConnectionModeSpinnerWrapper = (ViewGroup) mBaseContentLayout.findViewById(R.id.connection_mode_spinner_wrapper);
			mConnectionModeSpinnerWrapper.setVisibility(View.GONE);
			// Shows the cancel button
			mCancelButton.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					onBackPressed();
				}
			});
			
			// Disables the input fields
			mConnectionNameInput.setEnabled(false);
			mHostIpInput.setEnabled(false);
			mHostPortInput.setEnabled(false);
			mConnectionModeInput.setEnabled(false);
			
			// Fills the input fields
			mConnectionNameInput.setText(mConnection.getName());
			Iterator<ConnectionPoint> iterator = mConnection.getConnectionPoints().iterator();
			if (iterator.hasNext()) {
				ConnectionPoint lastConnectionPoint = iterator.next();
				mHostIpInput.setText(lastConnectionPoint.IP);
				mHostPortInput.setText(String.valueOf(lastConnectionPoint.Port));
				String connectionModeText = (lastConnectionPoint.SSL) ? mConnectionsModes[1] : mConnectionsModes[0];
				mConnectionModeInput.setText(connectionModeText);
			}
			
			break;
		}
		
	}

	@Override
	protected void onResume() {
		super.onResume();
		
		mClientChannel = ClientChannel.getInstance();
		if (mClientChannel != null) {
			mClientChannel.AddListener(ConnectionActivity.this);
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		
		mClientChannel = ClientChannel.getInstance();
		if (mClientChannel != null) {
			mClientChannel.RemoveListener(ConnectionActivity.this);
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

		if (TextUtils.isEmpty(mConnectionNameInput.getText())) {
			mConnectionNameInput.setError(null);
			areInputFieldsValid = false;
		}

		if (TextUtils.isEmpty(mHostIpInput.getText())) {
			mHostIpInput.setError(null);
			areInputFieldsValid = false;
		}

		if (TextUtils.isEmpty(mHostPortInput.getText())) {
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
		
		String hostIp = mHostIpInput.getText().toString();
		int hostPort = Integer.valueOf(mHostPortInput.getText().toString());
		
		Log.i(TAG, "ConnectionActivity#openSocket(...) host: " + hostIp);
		Log.i(TAG, "ConnectionActivity#openSocket(...) port: " + hostPort);
		
		boolean isCreated = ClientChannel.Create(hostIp, hostPort, ClientChannel.TIME_OUT);
		if (isCreated) {
			mClientChannel = ClientChannel.getInstance();
			mClientChannel.AddListener(ConnectionActivity.this);
			// Sends CockpitSiteInfoMsg
			CockpitSiteInfoMsg msgCSI = new CockpitSiteInfoMsg();
	    	mClientChannel.SendAsync(msgCSI);
	    	startLoadingScreen();
		}
	}

	@Override
	public void ProcessMsg(BaseMsg msg) {
		Log.i(TAG, TAG + "#ProcessMsg(...)\n" + msg.getClass().getSimpleName() + "\n" + msg.serializeJson());
		
		if (msg.msgCalssID == ClassID.CockpitSiteInfoMsg.ValueOf()) {
			CockpitSiteInfoMsg cockpitSiteInfoMsg = (CockpitSiteInfoMsg) msg;
			// Gets the connection points
			ConnectionPoint[] connectionPoints = cockpitSiteInfoMsg.ConnectionPoints;
			// Gets the login screen image
			String loginImageName = FilesUtils.getFileName(cockpitSiteInfoMsg.LoginScreenImage, "\\");
			// Gets the connection name from the input
			String connectionName = mConnectionNameInput.getText().toString();
			// Creates a new host
			mConnection = new Connection();
			mConnection.setName(connectionName);
			mConnection.setLoginImageName(loginImageName);
			for (ConnectionPoint cp : connectionPoints) {
				mConnection.addConnectionPoint(cp);
			}
			ConnectionsDB.getInstance(getApplicationContext()).saveConnection(mConnection);
//			String loginImageFilePath = Config.Paths.DIR_IMAGES + loginImageName;
			Bitmap image = FilesUtils.readImage(loginImageName);
			// If the image not found in client storage,
			// fetch it from the server
			if (image == null) {
				// Sends LoginScreenImageMsg
				LoginScreenImageMsg msgLIS = new LoginScreenImageMsg();
				msgLIS.ImageName = ((CockpitSiteInfoMsg)msg).LoginScreenImage;
				ClientChannel.getInstance().SendAsync(msgLIS);
			} else {
				launchConnectionActivity();
			}
		} else if (msg.msgCalssID == ClassID.LoginScreenImageMsg.ValueOf()) {
			LoginScreenImageMsg loginScreenImageMsg = (LoginScreenImageMsg) msg;
			Bitmap image = BitmapFactory.decodeByteArray(
					loginScreenImageMsg.Image, 0,
					loginScreenImageMsg.Image.length);
			String loginImageName = FilesUtils.getFileName(loginScreenImageMsg.ImageName, "\\");
//			String loginImageFilePath = Config.Paths.DIR_IMAGES + loginImageName;
			FilesUtils.writeImage(loginImageName, image);
			launchConnectionActivity();
		}
	}
	
	@Override
	public void ConnectionIsBroken() {
		// Do nothing
	}
	
	private void launchConnectionActivity() {
		// Launches the login activity
		Intent intent = new Intent(ConnectionActivity.this, LoginActivity.class);
		intent.putExtra(Config.Extras.EXTRA_CONNECTION, mConnection);
		startActivity(intent);
		stopLoadingScreen();
	}

}

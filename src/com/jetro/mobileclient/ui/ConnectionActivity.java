package com.jetro.mobileclient.ui;

import java.util.ArrayList;
import java.util.HashMap;

import JProtocol.Core.BaseMsg;
import JProtocol.Core.Net.ClientChannel;
import JProtocol.Protocols.Controller.CockpitSiteInfoMsg;
import JProtocol.Protocols.Controller.LoginMsg;
import JProtocol.Protocols.Controller.LoginScreenImageMsg;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings.Secure;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout.LayoutParams;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import com.freerdp.freerdpcore.application.NetworkStateReceiver;
import com.freerdp.freerdpcore.sharedobjects.ConnectionPoint;
import com.freerdp.freerdpcore.sharedobjects.utils.Constants;
import com.freerdp.freerdpcore.sharedobjects.utils.FileSystemManagerServices;
import com.freerdp.freerdpcore.sharedobjects.utils.Logger;
import com.freerdp.freerdpcore.sharedobjects.utils.Logger.LogLevel;
import com.jetro.mobileclient.R;
import com.jetro.mobileclient.application.GlobalApp;
import com.jetro.mobileclient.repository.ConnectionsDB;
import com.jetro.mobileclient.utils.Config;

public class ConnectionActivity extends HeaderActivtiy implements TextWatcher {

	private static final String TAG = ConnectionActivity.class.getSimpleName();
	
	private ClientChannel mClientChannel;
	
	private ConnectionActivityMode mActivityMode;
	
	// Connection / Login / Reset Password
	private View mBaseContentLayout;
	// HostName / UserName / Old Password
	private TextView mFirstLabel;
	private EditText mFirstInput;
	// Host / Password / New Password
	private TextView mSecondLabel;
	private EditText mSecondInput;
	// Port / Domain / Confirm Password
	private TextView mThirdLabel;
	private EditText mThirdInput;
	// Connection Mode
	private String[] mConnectionsModes;
	private ArrayAdapter<CharSequence> mConnectionModeSpinnerAdapter;
	private Spinner mConnectionModeSpinner;
	private EditText mConnectionModeInput;
	private TextView mConnectionModeText;
	private String mSelectedConnectionMode = "";
	
	private String mHeaderTitlePrefix;
	
	
	private ArrayList<ConnectionPoint> mConnectionsPoints;

	private static boolean SHOW_DIALOG = false;

	// holds the current button that make the
	// main action at the current activity mode
	private TextView actionBtn;

	private FileSystemManagerServices mFileSysManager;
	private String hostName;

	NetworkChangeReceiver mBroadcastReceiver = new NetworkChangeReceiver();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.d(TAG, "onCreate(...) ENTER");
		super.onCreate(savedInstanceState);
		
		mActivityMode = ConnectionActivityMode.values()[getIntent().getIntExtra(Constants.MODE, 0)];
		Log.i(TAG, TAG + "#onCreate(...) ACTIVITY MODE: " + mActivityMode);
		
		mHeaderTitlePrefix = getString(R.string.header_title_prefix);
		
		mClientChannel = ClientChannel.getInstance();
		mConnectionsPoints = getIntent().getParcelableArrayListExtra(Constants.CONNECTIONS_POINTS);
		mFileSysManager = new FileSystemManagerServices(this, true, true);
		
		// Header widgets
		mHeaderBackButton.setVisibility(View.VISIBLE);
		mHeaderBackButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				onBackPressed();
			}
		});
		// Base Content widgets
		mBaseContentLayout = setBaseContentView(R.layout.connection_activity_layout);
		mFirstLabel = (TextView) mBaseContentLayout.findViewById(R.id.first_label);
		mFirstInput = (EditText) mBaseContentLayout.findViewById(R.id.first_input);
		mSecondLabel = (TextView) mBaseContentLayout.findViewById(R.id.second_label);
		mSecondInput = (EditText) mBaseContentLayout.findViewById(R.id.second_input);
		mThirdLabel = (TextView) mBaseContentLayout.findViewById(R.id.third_label);
		mThirdInput = (EditText) mBaseContentLayout.findViewById(R.id.third_input);
		mConnectionModeInput = (EditText) mBaseContentLayout.findViewById(R.id.connection_mode_input);
		mConnectionModeInput.setVisibility(View.GONE);
		mConnectionModeText = (TextView) mBaseContentLayout.findViewById(R.id.connection_mode_text);
		mConnectionModeText.setVisibility(View.GONE);

		if (mActivityMode == ConnectionActivityMode.Login && mConnectionsPoints != null && !mConnectionsPoints.isEmpty()) {
			ConnectionPoint lastConnectionPoint = mConnectionsPoints.get(0);
			mFirstInput.setText(lastConnectionPoint.getName());
			mSecondInput.setText(lastConnectionPoint.getIP());
			mThirdInput.setText(String.valueOf(lastConnectionPoint.getPort()));
			openSocket();
		} else {
			switch (mActivityMode) {
			case AddConnection:
				setAddConnectionMode();
				break;
			case Login:
				setLoginMode();
				break;
			case ResetPassword:
				setResetPasswdMode();
				break;
			case ViewConnection:
				setViewConnectionMode();
				break;
			case About:
				break;
			}
		}
	}

	@Override
	protected void onStart() {
		Log.d(TAG, "onStart(...) ENTER");
		
		registerReceiver(mBroadcastReceiver, new IntentFilter(Config.Actions.ACTION_CONNECTIVITY_CHANGE));
		super.onStart();
	}

	@Override
	protected void onStop() {
		Log.d(TAG, "onStop(...) ENTER");
		super.onStop();
		
		SHOW_DIALOG = false;
		unregisterReceiver(mBroadcastReceiver);
	}

	/**
	 * set add new connection mode
	 */
	private void setAddConnectionMode() {
		Log.d(TAG, TAG + "#setAddConnectionMode(...) ENTER");
		
		mActivityMode = ConnectionActivityMode.AddConnection;
		
		boolean hasConnections = ConnectionsDB.getAllSavedConnections().size() != 0;
		if (hasConnections) {
			mHeaderBackButton.setVisibility(View.VISIBLE);
		} else {
			mHeaderBackButton.setVisibility(View.INVISIBLE);
		}
		
		// Sets labels
		mFirstLabel.setText(getString(R.string.host_name_lbl));
		mSecondLabel.setText(getString(R.string.host_lbl));
		mThirdLabel.setText(getString(R.string.port_lbl));
		// Sets hints
		mFirstInput.setHint(getString(R.string.host_name_hint));
		mSecondInput.setHint(getString(R.string.host_hint));
		mThirdInput.setHint(getString(R.string.port_hint));
		// TODO: remove this after debug
		mFirstInput.setText("Test environment");
		mSecondInput.setText("212.199.106.213");
		mThirdInput.setText("13000");
		
		mThirdInput.setOnEditorActionListener(new OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				if (actionId == EditorInfo.IME_ACTION_GO) {
					openSocket();
					return true;
				}
				return false;
			}
		});
		mConnectionsModes = getResources().getStringArray(R.array.connection_mode_options);
		mConnectionModeSpinnerAdapter = ArrayAdapter.createFromResource(
				ConnectionActivity.this, R.array.connection_mode_options,
				R.layout.spinner_item_connection_mode);
		mConnectionModeSpinner = (Spinner) findViewById(R.id.connection_mode_spinner);
		mConnectionModeSpinner.setAdapter(mConnectionModeSpinnerAdapter);
		mConnectionModeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
					@Override
					public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
						mSelectedConnectionMode = mConnectionsModes[position];
					}
					@Override
					public void onNothingSelected(AdapterView<?> parent) {
					}
				});

		setActivityButtons(R.string.connect_lbl, 0, 0, 0);
	}

	/**
	 * set login mode
	 */
	private void setLoginMode() {
		Log.d(TAG, "setLoginMode(...) ENTER");

		mActivityMode = ConnectionActivityMode.Login;
		
		mHeaderBackButton.setVisibility(View.VISIBLE);

		mFirstLabel.setText(getString(R.string.user_name_lbl));
		mSecondLabel.setText(getString(R.string.passwd_lbl));
		mThirdLabel.setText(getString(R.string.domain_lbl));

		mBaseContentLayout.findViewById(R.id.spinnerWrapper).setVisibility(View.GONE);

		mSecondInput.setInputType(InputType.TYPE_CLASS_TEXT);
		mThirdInput.setInputType(InputType.TYPE_CLASS_TEXT);

		mFirstInput.setHint(getString(R.string.user_name_lbl));
		mSecondInput.setHint(getString(R.string.passwd_lbl));
		mThirdInput.setHint(getString(R.string.domain_lbl));
		mSecondInput.setInputType(InputType.TYPE_CLASS_TEXT
				| InputType.TYPE_TEXT_VARIATION_PASSWORD);

		mFirstInput.setBackgroundResource(R.drawable.black_border_square);
		mSecondInput.setBackgroundResource(R.drawable.black_border_square);
		mThirdInput.setBackgroundResource(R.drawable.black_border_square);
		mSecondInput.setText("");

		setActivityButtons(R.string.cancle_lbl,
				R.drawable.orange_button_selector, R.string.login_lbl, 0);
		mHeaderBackButton.setVisibility(View.VISIBLE);
		getProfileDetails();

		mThirdInput.setOnEditorActionListener(new OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView v, int actionId,
					KeyEvent event) {
				if (actionId == EditorInfo.IME_ACTION_GO) {
					makeLogin();
					return true;
				}
				return false;
			}
		});

		mFirstInput.setEnabled(true);
		mSecondInput.setEnabled(true);
		mThirdInput.setEnabled(true);

		mFirstInput.setClickable(true);
		mSecondInput.setClickable(true);
		mThirdInput.setClickable(true);

		setInputTextWatcher();
	}

	/**
	 * set reset password mode
	 */
	private void setResetPasswdMode() {
		Log.d(TAG, "setResetPasswdMode(...) ENTER");
		
		Logger.log(LogLevel.INFO,
				"Zacky: ConnectionAvtivity::setResetPasswdModeResetPossible");

		mActivityMode = ConnectionActivityMode.ResetPassword;

		mFirstLabel.setText(getString(R.string.old_passwd_lbl));
		mSecondLabel.setText(getString(R.string.new_passwd_lbl));
		mThirdLabel.setText(getString(R.string.confirm_passwd_lbl));
		mBaseContentLayout.findViewById(R.id.spinnerWrapper).setVisibility(View.GONE);

		mFirstInput.setText(mSecondInput.getText().toString());
		mSecondInput.setText("");
		mThirdInput.setText("");
		mThirdInput.setHint("Confirm new password");
		mSecondInput.setHint("New password");

		mSecondInput.setInputType(InputType.TYPE_CLASS_TEXT
				| InputType.TYPE_TEXT_VARIATION_PASSWORD);
		mThirdInput.setInputType(InputType.TYPE_CLASS_TEXT
				| InputType.TYPE_TEXT_VARIATION_PASSWORD);

		mFirstInput.setBackgroundResource(R.drawable.black_border_square);
		mSecondInput.setBackgroundResource(R.drawable.black_border_square);
		mThirdInput.setBackgroundResource(R.drawable.black_border_square);

		setActivityButtons(R.string.reset_lbl,
				R.drawable.orange_button_selector, 0, 0);

		setButtonAction(actionBtn);

		mFirstInput.setEnabled(true);
		mSecondInput.setEnabled(true);
		mThirdInput.setEnabled(true);

		mFirstInput.setClickable(true);
		mSecondInput.setClickable(true);
		mThirdInput.setClickable(true);
	}

	private void setViewConnectionMode() {
		Log.d(TAG, "setViewConnectionMode(...) ENTER");
		
		mActivityMode = ConnectionActivityMode.ViewConnection;

		mFirstLabel.setText(getString(R.string.host_name_lbl));
		mSecondLabel.setText(getString(R.string.host_lbl));
		mThirdLabel.setText(getString(R.string.port_lbl));
		mBaseContentLayout.findViewById(R.id.spinnerWrapper).setVisibility(View.GONE);

		TextView connectionModeText = (TextView) findViewById(R.id.connection_mode_text);
		connectionModeText.setVisibility(View.VISIBLE);
		EditText edit = ((EditText) findViewById(R.id.connection_mode_input));
		edit.setVisibility(View.VISIBLE);
		edit.setText(mConnectionsPoints.get(0).getConnectionMode());

		mFirstInput.setText(mConnectionsPoints.get(0).getName());
		mSecondInput.setText(mConnectionsPoints.get(0).getIP());
		mThirdInput.setText(String.valueOf(mConnectionsPoints.get(0).getPort()));

		mFirstInput.setEnabled(false);
		mSecondInput.setEnabled(false);
		mThirdInput.setEnabled(false);

		mFirstInput.setClickable(false);
		mSecondInput.setClickable(false);
		mThirdInput.setClickable(false);
		edit.setClickable(false);
		edit.setEnabled(false);

		setActivityButtons(R.string.connect_lbl, 0, 0, 0);

		setButtonAction(actionBtn);
		setHeaderTitleText(R.string.header_title_ViewConnection);
		actionBtn.setBackgroundResource(R.drawable.orange_button_selector);

		findViewById(R.id.first_star).setVisibility(View.INVISIBLE);
		findViewById(R.id.second_star).setVisibility(View.INVISIBLE);
		findViewById(R.id.third_star).setVisibility(View.INVISIBLE);
	}

	/**
	 * set the buttons of this screen according to
	 * {@link ConnectionActivityMode} mode. DONT invoke this method directly,
	 * instead call one of the set mode methods. Pass 0 at the resource id if no
	 * value needed for this param. </br>all params are resource identifiers
	 * from strings.xml
	 * 
	 * @param btn1Lbl
	 * @param btn1Color
	 * @param btn2Lbl
	 * @param btn2Color
	 */
	private void setActivityButtons(int btn1Lbl, int btn1Color, int btn2Lbl, int btn2Color) {
		Log.d(TAG, "setActivityButtons(...) ENTER");
		
		TextView btn1 = (TextView) mBaseContentLayout.findViewById(R.id.left_button);
		TextView btn2 = (TextView) mBaseContentLayout.findViewById(R.id.right_button);

		btn1.setText(btn1Lbl);
		setButtonSelector(btn1, btn1Color);

		if (btn2Lbl != 0) {
			Logger.log(LogLevel.INFO, "");
			btn2.setText(btn2Lbl);
			setButtonSelector(btn2, btn2Color);
			setButtonsWeights(2, btn1, btn2);
			return;
		}

		setButtonsWeights(1, btn1, btn2);
	}

	/**
	 * clear input texts on modes transitions
	 */
	private void clearInputs() {
		Log.d(TAG, "clearInputs(...) ENTER");
		
		removeInputTextWatcher();
		mThirdInput.setText("");
		mSecondInput.setText("");
		mFirstInput.setText("");
		setInputTextWatcher();
	}

	/**
	 * set selector for each button(normal and pressed state)
	 * 
	 * @param btn
	 *            - the button reference
	 * @param resourceId
	 *            - resource id reference for the selector xml. pass 0 for no
	 *            selector, the default state is disabled gray style
	 */
	private void setButtonSelector(TextView btn, int resourceId) {
		Log.d(TAG, "setButtonSelector(...) ENTER");
		
		if (resourceId != 0)
			btn.setBackgroundResource(resourceId);
	}

	/**
	 * set weights (equals to percentage) of the screen width for each button,
	 * depending on the number of buttons that should be on each one of this
	 * {@link ConnectionActivityMode} mode
	 * 
	 * @param buttons
	 *            - buttons resource id references, should be at least on
	 *            reference
	 */
	private void setButtonsWeights(int numOfButtons, TextView... buttons) {
		Log.d(TAG, "setButtonsWeights(...) ENTER");
		
		if (buttons == null || buttons.length == 0)
			return;

		LayoutParams lp = (LayoutParams) buttons[0].getLayoutParams();

		switch (numOfButtons) {

		case 1:// one button
			lp.weight = 2;// take the whole width
			buttons[0].setLayoutParams(lp);
			buttons[1].setVisibility(View.GONE);
			actionBtn = buttons[0];

			break;

		case 2:// two buttons

			lp.weight = (float) 0.99;// split into tow buttons with a gap
			// between
			// them

			buttons[0].setLayoutParams(lp);
			buttons[1].setVisibility(View.VISIBLE);
			buttons[1].setLayoutParams(lp);

			// right side button
			actionBtn = buttons[1];
			buttons[0].setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					showPopupWhenPressCancelInLogin();
				}
			});
			break;

		case 3:// three buttons
				// TODO:add implement if needed
			break;
		}

		// set header title
		try {
			int resourceId = getResources().getIdentifier(mHeaderTitlePrefix + mActivityMode, "string", getPackageName());
			setHeaderTitleText(resourceId);
		} catch (Exception e) {
			setHeaderTitleText(R.string.app_title);
		}

		// clear text
		if (mConnectionsPoints == null)
			clearInputs();
	}

	private void showPopupWhenPressCancelInLogin() {
		Log.d(TAG, "showPopupWhenPressCancelInLogin(...) ENTER");
		
		new AlertDialog.Builder(this)
				.setMessage("Are you sure you want to cancel login?")
				.setCancelable(false)
				.setPositiveButton("Yes",
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								Intent intent = new Intent(
										ConnectionActivity.this,
										ConnectionsListActivity.class);
								startActivity(intent);
								finish();
							}
						}).setNegativeButton("No", null).show();
	}

	private void removeInputTextWatcher() {
		Log.d(TAG, "removeInputTextWatcher(...) ENTER");
		
		mFirstInput.removeTextChangedListener(this);
		mSecondInput.addTextChangedListener(this);
		mThirdInput.addTextChangedListener(this);
	}

	private void setInputTextWatcher() {
		Log.d(TAG, "setInputTextWatcher(...) ENTER");
		
		mFirstInput.addTextChangedListener(this);
		mSecondInput.addTextChangedListener(this);
		mThirdInput.addTextChangedListener(this);
	}

	private void resetButtons() {
		Log.d(TAG, "resetButtons(...) ENTER");
		
		mFirstInput.setTextColor(Color.BLACK);
		mSecondInput.setTextColor(Color.BLACK);
		mThirdInput.setTextColor(Color.BLACK);
		// firstInput.setBackgroundColor(Color.BLACK);

		mFirstInput.setVisibility(View.VISIBLE);
		mSecondInput.setVisibility(View.VISIBLE);
		mThirdInput.setVisibility(View.VISIBLE);
	}

	/**
	 * 
	 * @param button
	 */
	private void setButtonAction(TextView button) {
		Log.d(TAG, "setButtonAction(...) ENTER");
		
		button.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				switch (mActivityMode) {
				case AddConnection:
				case ViewConnection: {
					findViewById(R.id.third_label).setVisibility(View.VISIBLE);
					mFirstInput.setBackgroundResource(R.drawable.gray_border_square);
					mThirdInput.setBackgroundResource(R.drawable.gray_border_square);
					resetButtons();
					openSocket();
					break;
				}
				case ResetPassword:
					doPasswordReset();
					break;
				case Login:
					resetButtons();
					makeLogin();
					break;
				}
			}
		});
	}

	@Override
	protected void setHeader() {
		Log.d(TAG, "setHeader(...) ENTER");
	}

	@Override
	public void OnMessageReceived(BaseMsg msg) {
		Log.d(TAG, "OnMessageReceived(...) ENTER");
		
		InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(mFirstInput.getWindowToken(), 0);

		Log.i(TAG, "ConnectionActivity#OnMessageReceived(...) message id: " + msg.extraHeader.MsgClassID);
		
		try {
			if (msg == null || msg.getJsonResponse() == null) {
				OnIOError("Message is null");
				return;
			}
			if (msg.getJsonResponse().getDescription() != null) {

			}
			// local variable for image name
			String imageName = null;

			switch (msg.extraHeader.MsgClassID) {

			case MessagesValues.ClassID.CockpitSiteInfoMsg:

				CPTInfoResponse cptResponse = (CPTInfoResponse) msg.getJsonResponse();
				
				imageName = cptResponse.getLoginScreenImage();
				Log.i(TAG, "ConnectionActivity#OnMessageReceived(...) image name: " + imageName);

				hostName = mFirstInput.getText().toString().trim();

				if (mConnectionsPoints == null) {
					saveConnectionModeToCP(cptResponse.getConnectionPoints());
					ConnectionsDB.saveNewConnection(hostName,
							cptResponse.getConnectionPoints());
				}

				// if socket created successfully
				// set the screen mode to login
				setLoginMode();
				stopLoadingScreen();

				//setLoginScreenImage(imageName);

				break;

			case MessagesValues.ClassID.LoginMsg:
				
				LoginMsgResponse loginResponse = (LoginMsgResponse) msg.getJsonResponse();
				Log.i(TAG, "ConnectionActivity#OnMessageReceived(...) return code: " + loginResponse.getReturnCode());

				switch (loginResponse.getReturnCode()) {
				
				case MessagesValues.LoginReturnCode.LoginSuccess:
					Intent intent = new Intent(ConnectionActivity.this, SessionActivity.class);
					intent.putExtra(Constants.TICKET, MessagesValues.ClassID.MyApplicationsMsg);
					startActivity(intent);
					stopLoadingScreen();
					finish();
					break;
				
				case MessagesValues.LoginReturnCode.Error:
					break;

				case MessagesValues.LoginReturnCode.ResetPassword:
					setResetPasswdMode();
					stopLoadingScreen();
					break;

				case MessagesValues.LoginReturnCode.PasswordBeforeExpiration:
					// fall through - consider this a successful login
					showDialogChooseChangePassword();
					break;
				}
				break;

			case MessagesValues.ClassID.ResetPasswordMsg:
				Intent intent = new Intent(ConnectionActivity.this, DesktopActivity.class);
				finish();
				startActivity(intent);
				stopLoadingScreen();
				break;

			case MessagesValues.ClassID.Error:
				ErrCodeMsgResponse errCodeMsgResponse = (ErrCodeMsgResponse) msg.getJsonResponse();
				int errCode = errCodeMsgResponse.getErr();
				String errDescription = errCodeMsgResponse.getDescription();
				Log.e(TAG, "ErrorMsg: " + errCode + " - " + errDescription);
				
				switch (errCode) {
				case MessagesValues.ErrCode.LoginFailed:
					showErrorDialog(errCodeMsgResponse.getDescription());
					break;
				}
				stopLoadingScreen();
				break;

			default:
				break;
			}
		} catch (Exception e) {
			Log.e(TAG, "ERROR: ", e);
		}

	}

	private void saveConnectionModeToCP(ConnectionPoint[] cp) {
		Log.d(TAG, "saveConnectionModeToCP(...) ENTER");
		
		for (int i = 0; i < cp.length; i++) {
			ConnectionPoint p = cp[i];
			p.setConnectionMode(mSelectedConnectionMode);
		}
	}

	private void showErrorDialog(String st) {
		Log.d(TAG, "showErrorDialog(...) ENTER");

		new AlertDialog.Builder(this).setMessage(st).setCancelable(false)
				.setPositiveButton("ok", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {

					}
				}).setNegativeButton(null,

				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						// finish();
					}
				}

				).show();

	}

	private void showDialogConenctAgain() {
		Log.d(TAG, "showDialogConenctAgain(...) ENTER");
		
		new AlertDialog.Builder(this)
				.setMessage("Can't connect to server do you want to try again?")
				.setCancelable(false)
				.setPositiveButton("Retry",
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								openSocket();
							}
						}).setNegativeButton("ok",

				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						setAddConnectionMode();
					}
				}

				).show();
	}

	/**
	 * open socket according to user input
	 */
	private void openSocket() {
		Log.d(TAG, "openSocket(...) ENTER");
		
		if (!validateInputs())
			return;

		startLoadingScreen();
		
		String host = mSecondInput.getText().toString();
		Integer port = Integer.valueOf(mThirdInput.getText().toString());
		
		Log.i(TAG, "ConnectionActivity#openSocket(...) host: " + host);
		Log.i(TAG, "ConnectionActivity#openSocket(...) port: " + port);
		
		mClientChannel.Connect(host, port);
		
		Display display = getWindowManager().getDefaultDisplay();
		DisplayMetrics metrics = new DisplayMetrics();
		display.getMetrics(metrics);
		int width = metrics.widthPixels;
		int height = metrics.heightPixels;

		////////// CockpitSiteInfoMsg ////////////////
		CockpitSiteInfoMsg msgCSI = new CockpitSiteInfoMsg(width, height);
		CockpitSiteInfoMsg respCSI = (CockpitSiteInfoMsg) mClientChannel.SendReceive(msgCSI, ClientChannel.TIME_OUT);
		if(respCSI != null) 
			System.out.println(respCSI.toString());
		else
		{
			System.out.println("null received instead of CockpitSiteInfoMsg");
			mClientChannel.Stop();
			return;
		}
		
		///////   LoginScreenImageMsg ////////
		String loginScreenImageName = respCSI.getLoginScreenImage();
		Bitmap loginScreenImage = mFileSysManager.getBitmap(loginScreenImageName);
		if (loginScreenImage == null) {
			LoginScreenImageMsg msgLIS = new LoginScreenImageMsg();
			msgLIS.ImageName = ((CockpitSiteInfoMsg)msgCSI).LoginScreenImage;
			LoginScreenImageMsg respLIS = (LoginScreenImageMsg) mClientChannel.SendReceive(msgLIS, ClientChannel.TIME_OUT);
			if(respLIS != null) {
				System.out.println(respLIS.toString());
				String imageName = respLIS.getImageName();
				byte[] bytes = respLIS.getImage();
				Bitmap bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
				((ImageView) mBaseContentLayout.findViewById(R.id.login_screen_image)).setImageBitmap(bmp);
				mFileSysManager.saveBitmap(imageName, bmp);
			} else {
				System.out.println("null received instead of LoginScreenImageMsg");
				return;
			}
		}
	}

	/**
	 * validate input fields according to screen mode
	 * 
	 * @return - true if all fields valid, false otherwise
	 */
	private boolean validateInputs() {
		Log.d(TAG, "validateInputs(...) ENTER");

		boolean valid = true;

		switch (mActivityMode) {

		case AddConnection:

			String ip = mSecondInput.getText().toString();
			if (!isButtonAvailabile() /* || !(ip.equals(Patterns.IP_ADDRESS)) */) {
				valid = false;
			}

			String port = mThirdInput.getText().toString();
			if (!isButtonAvailabile() || port.length() < 4) {
				valid = false;
			}

			break;

		case ResetPassword:

			Logger.log(LogLevel.INFO,
					"Zacky: ConnectionActivity::validateInputs - ResetPassword");

			if (!isButtonAvailabile()) {
				valid = false;
			} else
				valid = (mSecondInput.getText().toString().trim()
						.equals(mThirdInput.getText().toString().trim()));

			break;

		case Login:

			valid = isButtonAvailabile();

			break;
		}

		Log.i(TAG, "ConnectionActivity#validateInputs(...) isValid " + valid);
		return valid;
	}

	/**
	 * check if all fields are filled with at least 1 character for enabling the
	 * action button
	 * 
	 * @return - true if all the input fields are filled with data, false
	 *         otherwise
	 */
	private boolean isButtonAvailabile() {
		Log.d(TAG, "isButtonAvailabile(...) ENTER");
		
		boolean isAvaliable = true;

		if (mFirstInput.getText().toString().trim().isEmpty()) {
			mFirstInput.setError(null);
			isAvaliable = false;
		}

		if (mSecondInput.getText().toString().trim().isEmpty()) {
			mSecondInput.setError(null);
			isAvaliable = false;
		}

		if (mThirdInput.getText().toString().trim().isEmpty()) {
			mThirdInput.setError(null);
			isAvaliable = false;
		}

		return isAvaliable;
	}

	@Override
	public void afterTextChanged(Editable s) {
	}

	@Override
	public void beforeTextChanged(CharSequence s, int start, int count,
			int after) {
	}

	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count) {
		Log.d(TAG, "onTextChanged(...) ENTER");
		
		if (isButtonAvailabile()) {
			// after entering text at all three input fields, need to set the
			// action button enabled and change the color selector
			actionBtn.setBackgroundResource(R.drawable.orange_button_selector);
			setButtonAction(actionBtn);
		} else {
			actionBtn.setBackgroundResource(R.color.disable_gray);
			actionBtn.setOnClickListener(null);
		}
	}

	/**
	 * save the details of the user in the enternal memory
	 */
	public void saveProfileDetails() {
		Log.d(TAG, "saveProfileDetails(...) ENTER");
		
		SharedPreferences pref = getApplicationContext().getSharedPreferences(
				"MyPref", MODE_PRIVATE);

		Log.i(TAG, "ConnectionActivity#saveProfileDetails(...) connection name: " + mFirstInput.getText().toString());
		Log.i(TAG, "ConnectionActivity#saveProfileDetails(...) domain: " + mThirdInput.getText().toString());

		Editor editor = pref.edit();
		editor.putString("name", mFirstInput.getText().toString());
		editor.putString("domain", mThirdInput.getText().toString());
		editor.commit();
	}

	/**
	 * get the details of the user from the internal memory
	 */
	public void getProfileDetails() {
		Log.d(TAG, "getProfileDetails(...) ENTER");
		
		SharedPreferences pref = getApplicationContext().getSharedPreferences("MyPref", MODE_PRIVATE);
		mFirstInput.setText(pref.getString("name", ""));
		mThirdInput.setText(pref.getString("domain", ""));
	}

	/**
	 * send login message to server
	 */
	private void makeLogin() {
		Log.d(TAG, "makeLogin(...) ENTER");
		
		// check if there is a detalis in the sharedPreference

		if (!validateInputs())
			return;
		
		String userName = "";
		String password = "";
		String domain = "";

		if (mActivityMode == ConnectionActivityMode.Login) {
			userName = mFirstInput.getText().toString().trim();
			password = mSecondInput.getText().toString().trim();
			domain = mThirdInput.getText().toString().trim();
		} else if (mActivityMode == ConnectionActivityMode.ResetPassword) {
			userName = mFirstInput.getText().toString().trim();
			password = mSecondInput.getText().toString().trim();
			domain = mConnectionsPoints.get(0).getDomain();
		}
		
		if (mConnectionsPoints == null) {
			mConnectionsPoints = new ArrayList<ConnectionPoint>();
		}
		
		// save params in connection point
		ConnectionPoint cp = new ConnectionPoint();
		cp.setUserName(userName);
		cp.setPassword(password);
		cp.setDomain(domain);
		cp.setConnectionMode("***");
		mConnectionsPoints.add(cp);

		ConnectionsDB.saveCredentialsForExistingConnection(hostName, userName, domain);

		saveProfileDetails();

		// save the connection point in our global app
		// so it can be used when re-authenticating when connecting to RDP
		GlobalApp.SetConnectionPoint(mConnectionsPoints.get(0));

		startLoadingScreen();
		
		//////////////   LoginMsg   //////
		LoginMsg msgLIN = new LoginMsg();
		msgLIN.name = userName;
		msgLIN.password = password;
		msgLIN.domain = domain;
		msgLIN.deviceModel = Build.MODEL;
		msgLIN.deviceId = Secure.getString(getContentResolver(), Secure.ANDROID_ID);
		BaseMsg respLIN = mClientChannel.SendReceive(msgLIN, ClientChannel.TIME_OUT);
		if(respLIN != null) {
			System.out.println(respLIN.toString());
		} else {
			System.out.println("null received instead of LoginMsg");
			mClientChannel.Stop();
			showDialogConenctAgain();
			stopLoadingScreen();
			return;
		}
	}

	/**
	 * send reset password message to server
	 */
	private void doPasswordReset() {
		Log.d(TAG, "doPasswordReset(...) ENTER");

		if (!validateInputs())
			return;

		String oldPassword = mFirstInput.getText().toString().trim();
		String newPassword = mSecondInput.getText().toString().trim();
		String userName = mConnectionsPoints.get(0).getUserName();
		String domain = mConnectionsPoints.get(0).getDomain();

		Log.i(TAG, "ConnectionActivity#doPasswordReset(...) Send password reset message: "
				+ oldPassword + " - " + newPassword + " - " + domain + " - "
				+ userName);

		socketManager.sendMessage(new ResetPasswordMsg(userName, newPassword,
				oldPassword, domain));
	}

	/**
	 * Represent the adapter of the connectionMode
	 * 
	 * @author Hamody.M
	 * 
	 */
	public class ConnectionsModesAdapter extends ArrayAdapter<String> {

		public ConnectionsModesAdapter(Context context, int textViewResourceId, String[] objects) {
			super(context, textViewResourceId, objects);
		}

		@Override
		public View getDropDownView(int position, View convertView, ViewGroup parent) {
			return getCustomView(position, convertView, parent);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			return getCustomView(position, convertView, parent);
		}

		public View getCustomView(int position, View convertView, ViewGroup parent) {
			LayoutInflater inflater = getLayoutInflater();
			View row = inflater.inflate(, parent, false);
			TextView label = (TextView) row.findViewById(R.id.spinner_item_choose);
			label.setText(strings[position]);

			row.setBackgroundResource(R.drawable.spinner_item_selector);

			return row;
		}
	}

	private void setLoginScreenImage(final String imageName) {
		new AsyncTask<Void, Void, Bitmap>() {
			@Override
			protected Bitmap doInBackground(Void... params) {
				Log.d(TAG, "setLoginScreenImage(...) ENTER");
				
				Bitmap bmp = mFileSysManager.getBitmap(imageName, true);
				if (bmp == null) {
					socketManager
							.sendMessage(new LoginScreenImageMsg(imageName));

				}
				return bmp;
			}

			@Override
			protected void onPostExecute(Bitmap result) {
				Log.d(TAG, "onPostExecute(...) ENTER");
				
				if (result != null) {
					((ImageView) mBaseContentLayout.findViewById(R.id.login_screen_image))
							.setImageBitmap(result);
				}
				super.onPostExecute(result);
			} 
		}.execute();
	}

	private void showDialogChooseChangePassword() {
		Log.d(TAG, "showDialogChooseChangePassword(...) ENTER");
		
		new AlertDialog.Builder(this)
				.setMessage("do you want to change password?")
				.setCancelable(false)
				.setPositiveButton("Change",
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								setResetPasswdMode();
								stopLoadingScreen();
							}
						}).setNegativeButton("Cancel",

				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						finish();
					}
				}

				).show();

	}

	private void showDialogChooseNoInternetConnection() {
		Log.d(TAG, "showDialogChooseNoInternetConnection(...) ENTER");
		
		new AlertDialog.Builder(this)
				.setMessage("there is no internet connection!!")
				.setCancelable(false)
				.setPositiveButton("ok", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						Intent intent = new Intent(ConnectionActivity.this,
								ConnectionsListActivity.class);
						intent.putExtra(Config.Extras.EXTRA_TYPE, "type");
						startActivity(intent);
						finish();

					}
				}).setNegativeButton(null,

				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						finish();
					}
				}

				).show();

	}

	@Override
	public void onBackPressed() {
		Log.d(TAG, "onBackPressed(...) ENTER");
		
		Intent intent;
		switch (mActivityMode) {
		case AddConnection:
			if (!ConnectionsDB.isDBEmpty()) {
				mHeaderBackButton.setVisibility(View.VISIBLE);
				intent = new Intent(ConnectionActivity.this,
						ConnectionsListActivity.class);
				startActivity(intent);
				finish();
			} else {
				new AlertDialog.Builder(this)
						.setMessage(Constants.POPUP_EXIT_MESSAGE)
						.setCancelable(false)
						.setPositiveButton(Constants.YES_MESSAGE,
								new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog,
											int id) {
										finish();
									}
								})
						.setNegativeButton(Constants.NO_MESSAGE, null).show();
			}
			break;
		case Login:
			new AlertDialog.Builder(this)
					.setMessage(Constants.MESSAGE_CANCEL_BUTTON)
					.setCancelable(false)
					.setPositiveButton(Constants.YES_MESSAGE,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {
									mHeaderBackButton.setVisibility(View.VISIBLE);
									Intent intent = new Intent(
											ConnectionActivity.this,
											ConnectionsListActivity.class);
									startActivity(intent);
									finish();
								}
							}).setNegativeButton(Constants.NO_MESSAGE, null)
					.show();
			break;
		case ViewConnection:
			intent = new Intent(ConnectionActivity.this, ConnectionsListActivity.class);
			startActivity(intent);
			finish();
			break;
		case ResetPassword:
			intent = new Intent(ConnectionActivity.this, ConnectionActivity.class);
			intent.putParcelableArrayListExtra(Constants.CONNECTIONS_POINTS, null);
			intent.putExtra(Constants.MODE, ConnectionActivityMode.Login.getNumericType());
			startActivity(intent);
			finish();
			break;
		default:
			break;
		}
	}

	public void startList() {
		Log.d(TAG, "startList(...) ENTER");
		
		Intent intent = new Intent(ConnectionActivity.this, ConnectionsListActivity.class);

		startActivity(intent);
		finish();
	}

	public class NetworkChangeReceiver extends NetworkStateReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			Log.d(TAG, "ConnectionActivity.NetworkChangeReceiver#onReceive(...) ENTER");
			
			if (!isConnectedTo3G(context)) {
				if ((!SHOW_DIALOG)) {
					SHOW_DIALOG = true;
					showDialogChooseNoInternetConnection();
				} else if (!ConnectionsListActivity.IS_FIRST) {
					ConnectionsListActivity.IS_FIRST = true;
					showDialogChooseNoInternetConnection();
				}
			}
		}
	}
	
}
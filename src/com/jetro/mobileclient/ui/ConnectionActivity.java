package com.jetro.mobileclient.ui;

import java.util.ArrayList;
import java.util.HashMap;

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
import com.freerdp.freerdpcore.sharedobjects.ISocketListener;
import com.freerdp.freerdpcore.sharedobjects.SocketManager;
import com.freerdp.freerdpcore.sharedobjects.controller_messages.CockpitSiteInfoMsg;
import com.freerdp.freerdpcore.sharedobjects.controller_messages.CockpitSiteInfoMsg.CPTInfoResponse;
import com.freerdp.freerdpcore.sharedobjects.controller_messages.ErrorMsg.ErrCodeMsgResponse;
import com.freerdp.freerdpcore.sharedobjects.controller_messages.LoginMsg;
import com.freerdp.freerdpcore.sharedobjects.controller_messages.LoginMsg.LoginMsgResponse;
import com.freerdp.freerdpcore.sharedobjects.controller_messages.LoginScreenImageMsg;
import com.freerdp.freerdpcore.sharedobjects.controller_messages.LoginScreenImageMsg.ScreenImageResponse;
import com.freerdp.freerdpcore.sharedobjects.controller_messages.ResetPasswordMsg;
import com.freerdp.freerdpcore.sharedobjects.protocol.BaseMsg;
import com.freerdp.freerdpcore.sharedobjects.protocol.MessagesValues;
import com.freerdp.freerdpcore.sharedobjects.utils.Constants;
import com.freerdp.freerdpcore.sharedobjects.utils.FileSystemManagerServices;
import com.freerdp.freerdpcore.sharedobjects.utils.Logger;
import com.freerdp.freerdpcore.sharedobjects.utils.Logger.LogLevel;
import com.jetro.mobileclient.R;
import com.jetro.mobileclient.application.GlobalApp;
import com.jetro.mobileclient.repository.ConnectionsDB;

public class ConnectionActivity extends HeaderActivtiy implements
		ISocketListener, TextWatcher {

	private static final String TAG = ConnectionActivity.class.getSimpleName();
	
	private View layout;
	private SocketManager socketManager;
	private EditText firstInput, secondInput, thirdInput, connectionMode;
	private ConnectionActivityMode mode;
	private ArrayList<ConnectionPoint> connectionsPoints;
	private boolean ConnectedTo3G = false;
	private String spinner_choose = "";

	private boolean PRESS_BACK_BUTTON = false;

	private static boolean SHOW_DIALOG = false;

	String[] strings = { "Direct", "SSL", "TLS" };

	// holds the current button that make the
	// main action at the current activity mode
	private TextView actionBtn;

	private FileSystemManagerServices fs;
	private String hostName;

	NetworkChangeReceiver broadCastReceiver = new NetworkChangeReceiver();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.d(TAG, "onCreate(...) ENTER");
		super.onCreate(savedInstanceState);
		
		try {
			mode = ConnectionActivityMode.values()[getIntent().getIntExtra(Constants.MODE, 0)];
			Log.i(TAG, "ConnectionActivity#onCreate(...) MODE: " + mode);
			// get the connection point object from connections list activity
			connectionsPoints = getIntent().getParcelableArrayListExtra(Constants.CONNECTIONS_POINTS);
			layout = addActivityLayoutInBaseContainer(R.layout.connection_activity_layout);
			firstInput = (EditText) layout.findViewById(R.id.firstInput);
			secondInput = (EditText) layout.findViewById(R.id.secondInput);
			thirdInput = (EditText) layout.findViewById(R.id.thirdInput);
			fs = new FileSystemManagerServices(this, true, true);
			backBtn = (ImageView) findViewById(R.id.backBtn);
			backBtn.setVisibility(View.VISIBLE);
			hidenConnectionModeDetails();
			backBtn.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					PRESS_BACK_BUTTON = true;
					onBackPressed();
				}
			});

			socketManager = GlobalApp.getSocketManager(this);
			if (connectionsPoints != null && !connectionsPoints.isEmpty()
					&& mode == ConnectionActivityMode.Login) {
				backBtn = (ImageView) findViewById(R.id.backBtn);
				firstInput.setText(connectionsPoints.get(0).getName());
				secondInput.setText(connectionsPoints.get(0).getIP());
				thirdInput.setText(String.valueOf(connectionsPoints.get(0).getPort()));
				openSocket();
			} else {
				switch (mode) {
				case AddConnection:
					if (ConnectionsDB.getAllSavedConnections().size() != 0) {
						backBtn.setVisibility(View.VISIBLE);
					} else {
						backBtn.setVisibility(View.INVISIBLE);
					}
					setAddConnectionMode();
					break;
				case Login:
					backBtn = (ImageView) findViewById(R.id.backBtn);
					backBtn.setVisibility(View.VISIBLE);
					setLoginMode();
					break;
				case ResetPassword:
					backBtn = (ImageView) findViewById(R.id.backBtn);
					setResetPasswdMode();
					break;
				case ViewConnection:
					setViewConnectionMode();
					break;
				case About:
					break;
				}
			}
		} catch (Exception e) {
			Log.e(TAG, "ERROR: ", e);
		}
	}

	@Override
	protected void onStart() {
		Log.d(TAG, "onStart(...) ENTER");
		
		// register recevier
		this.registerReceiver(broadCastReceiver, new IntentFilter(
				"android.net.conn.CONNECTIVITY_CHANGE"));
		super.onStart();
	}

	@Override
	protected void onStop() {
		Log.d(TAG, "onStop(...) ENTER");
		super.onStop();
		
		SHOW_DIALOG = false;
		unregisterReceiver(broadCastReceiver);
	}

	private void setHeaderTextByMode() {
		try {
			int rId = getResources().getIdentifier(mode.toString() + "_header",
					"string", getPackageName());
			setHeaderText(getString(rId));
		} catch (Exception e) {
			setHeaderText("Jetro");
		}
	}

	private void hidenConnectionModeDetails() {
		EditText connectionModeEditText = (EditText) findViewById(R.id.connectionModeEditText);
		connectionModeEditText.setVisibility(View.GONE);

		TextView connectionModeText = (TextView) findViewById(R.id.connectionMode);
		connectionModeText.setVisibility(View.GONE);
	}

	/**
	 * set add new connection mode
	 */
	private void setAddConnectionMode() {
		ArrayList<HashMap<String, ArrayList<ConnectionPoint>>> itemsFromDB = new ArrayList<HashMap<String, ArrayList<ConnectionPoint>>>();

		backBtn = (ImageView) findViewById(R.id.backBtn);
		Logger.log(LogLevel.INFO,
				"Zacky: ConnectionAvtivity::setAddConnectionMode");

		mode = ConnectionActivityMode.AddConnection;

		((TextView) layout.findViewById(R.id.lable1))
				.setText(getString(R.string.host_name_lbl));
		((TextView) layout.findViewById(R.id.lable2))
				.setText(getString(R.string.host_lbl));
		((TextView) layout.findViewById(R.id.lable3))
				.setText(getString(R.string.port_lbl));

		setActivityButtons(R.string.connect_lbl, 0, 0, 0);
		// TODO: remove below lines
		firstInput.setText("Test environment");
		secondInput.setText("212.199.106.213");
		thirdInput.setText("13000");

		firstInput.setHint("Host name here");
		secondInput.setHint("Host details here");
		thirdInput.setHint("Port details here");

		Spinner spinnerConnectionMode = (Spinner) findViewById(R.id.connectionModeSpinner);

		spinnerConnectionMode.setAdapter(new MyAdapter(ConnectionActivity.this,
				R.layout.spinner_iten, strings));

		// get the user choose from the spinner mode
		spinnerConnectionMode
				.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
					public void onItemSelected(AdapterView<?> parent,
							View view, int pos, long id) {
						Object item = parent.getItemAtPosition(pos);
						spinner_choose = (String) item;
						Log.i("spi", "the spiner is " + spinner_choose);
					}

					public void onNothingSelected(AdapterView<?> parent) {
					}
				});

		thirdInput.setOnEditorActionListener(new OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView v, int actionId,
					KeyEvent event) {

				if (actionId == EditorInfo.IME_ACTION_GO) {
					openSocket();
					return true;
				}
				return false;
			}
		});
	}

	/**
	 * set login mode
	 */
	private void setLoginMode() {
		Log.d(TAG, "setLoginMode(...) ENTER");
		
		Logger.log(LogLevel.INFO, "Zacky: ConnectionAvtivity::setLoginMode");

		mode = ConnectionActivityMode.Login;

		((TextView) layout.findViewById(R.id.lable1))
				.setText(getString(R.string.user_name_lbl));
		((TextView) layout.findViewById(R.id.lable2))
				.setText(getString(R.string.passwd_lbl));
		((TextView) layout.findViewById(R.id.lable3))
				.setText(getString(R.string.domain_lbl));
		// getProfileDetails();

		layout.findViewById(R.id.spinnerWrapper).setVisibility(View.GONE);

		secondInput.setInputType(InputType.TYPE_CLASS_TEXT);
		thirdInput.setInputType(InputType.TYPE_CLASS_TEXT);

		firstInput.setHint(getString(R.string.user_name_lbl));
		secondInput.setHint(getString(R.string.passwd_lbl));
		thirdInput.setHint(getString(R.string.domain_lbl));
		secondInput.setInputType(InputType.TYPE_CLASS_TEXT
				| InputType.TYPE_TEXT_VARIATION_PASSWORD);

		firstInput.setBackgroundResource(R.drawable.black_border_square);
		secondInput.setBackgroundResource(R.drawable.black_border_square);
		thirdInput.setBackgroundResource(R.drawable.black_border_square);
		secondInput.setText("");

		setActivityButtons(R.string.cancle_lbl,
				R.drawable.orange_button_selector, R.string.login_lbl, 0);
		backBtn.setVisibility(View.VISIBLE);
		getProfileDetails();

		thirdInput.setOnEditorActionListener(new OnEditorActionListener() {
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

		firstInput.setEnabled(true);
		secondInput.setEnabled(true);
		thirdInput.setEnabled(true);

		firstInput.setClickable(true);
		secondInput.setClickable(true);
		thirdInput.setClickable(true);

		setInputTextWatcher();
	}

	/**
	 * set reset password mode
	 */
	private void setResetPasswdMode() {
		Log.d(TAG, "setResetPasswdMode(...) ENTER");
		
		Logger.log(LogLevel.INFO,
				"Zacky: ConnectionAvtivity::setResetPasswdModeResetPossible");

		mode = ConnectionActivityMode.ResetPassword;

		((TextView) layout.findViewById(R.id.lable1))
				.setText(getString(R.string.old_passwd_lbl));
		((TextView) layout.findViewById(R.id.lable2))
				.setText(getString(R.string.new_passwd_lbl));
		((TextView) layout.findViewById(R.id.lable3))
				.setText(getString(R.string.confirm_passwd_lbl));
		layout.findViewById(R.id.spinnerWrapper).setVisibility(View.GONE);

		firstInput.setText(secondInput.getText().toString());
		secondInput.setText("");
		thirdInput.setText("");
		thirdInput.setHint("Confirm new password");
		secondInput.setHint("New password");

		secondInput.setInputType(InputType.TYPE_CLASS_TEXT
				| InputType.TYPE_TEXT_VARIATION_PASSWORD);
		thirdInput.setInputType(InputType.TYPE_CLASS_TEXT
				| InputType.TYPE_TEXT_VARIATION_PASSWORD);

		firstInput.setBackgroundResource(R.drawable.black_border_square);
		secondInput.setBackgroundResource(R.drawable.black_border_square);
		thirdInput.setBackgroundResource(R.drawable.black_border_square);

		setActivityButtons(R.string.reset_lbl,
				R.drawable.orange_button_selector, 0, 0);

		setButtonAction(actionBtn);

		firstInput.setEnabled(true);
		secondInput.setEnabled(true);
		thirdInput.setEnabled(true);

		firstInput.setClickable(true);
		secondInput.setClickable(true);
		thirdInput.setClickable(true);
	}

	private void setViewConnectionMode() {
		Log.d(TAG, "setViewConnectionMode(...) ENTER");
		
		mode = ConnectionActivityMode.ViewConnection;

		((TextView) layout.findViewById(R.id.lable1))
				.setText(getString(R.string.host_name_lbl));
		((TextView) layout.findViewById(R.id.lable2))
				.setText(getString(R.string.host_lbl));
		((TextView) layout.findViewById(R.id.lable3))
				.setText(getString(R.string.port_lbl));
		layout.findViewById(R.id.spinnerWrapper).setVisibility(View.GONE);

		TextView connectionModeText = (TextView) findViewById(R.id.connectionMode);
		connectionModeText.setVisibility(View.VISIBLE);
		EditText edit = ((EditText) findViewById(R.id.connectionModeEditText));
		edit.setVisibility(View.VISIBLE);
		edit.setText(connectionsPoints.get(0).getConnectionMode());

		firstInput.setText(connectionsPoints.get(0).getName());
		secondInput.setText(connectionsPoints.get(0).getIP());
		thirdInput.setText(String.valueOf(connectionsPoints.get(0).getPort()));

		firstInput.setEnabled(false);
		secondInput.setEnabled(false);
		thirdInput.setEnabled(false);

		firstInput.setClickable(false);
		secondInput.setClickable(false);
		thirdInput.setClickable(false);
		edit.setClickable(false);
		edit.setEnabled(false);

		setActivityButtons(R.string.connect_lbl, 0, 0, 0);

		setButtonAction(actionBtn);
		setHeaderText("Details");
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
	private void setActivityButtons(int btn1Lbl, int btn1Color, int btn2Lbl,
			int btn2Color) {
		Log.d(TAG, "setActivityButtons(...) ENTER");
		
		TextView btn1 = (TextView) layout.findViewById(R.id.leftBtn);
		TextView btn2 = (TextView) layout.findViewById(R.id.rightBtn);

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
		thirdInput.setText("");
		secondInput.setText("");
		firstInput.setText("");
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
		setHeaderTextByMode();

		// clear text
		if (connectionsPoints == null)
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
		
		firstInput.removeTextChangedListener(this);
		secondInput.addTextChangedListener(this);
		thirdInput.addTextChangedListener(this);
	}

	private void setInputTextWatcher() {
		Log.d(TAG, "setInputTextWatcher(...) ENTER");
		
		firstInput.addTextChangedListener(this);
		secondInput.addTextChangedListener(this);
		thirdInput.addTextChangedListener(this);
	}

	private void resetButtons() {
		Log.d(TAG, "resetButtons(...) ENTER");
		
		firstInput.setTextColor(Color.BLACK);
		secondInput.setTextColor(Color.BLACK);
		thirdInput.setTextColor(Color.BLACK);
		// firstInput.setBackgroundColor(Color.BLACK);

		firstInput.setVisibility(View.VISIBLE);
		secondInput.setVisibility(View.VISIBLE);
		thirdInput.setVisibility(View.VISIBLE);
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
				switch (mode) {
				case AddConnection:
				case ViewConnection: {
					findViewById(R.id.lable3).setVisibility(View.VISIBLE);
					firstInput.setBackgroundResource(R.drawable.gray_border_square);
					thirdInput.setBackgroundResource(R.drawable.gray_border_square);
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
	public void OnSocketCreated() {
		Log.d(TAG, "OnSocketCreated(...) ENTER");
		
		Logger.log(LogLevel.INFO, "Zacky: OnMessageReceived::OnSocketCreated");
		// get screen width and height
		Display d = getWindowManager().getDefaultDisplay();
		DisplayMetrics metrics = new DisplayMetrics();
		d.getMetrics(metrics);
		int width = metrics.widthPixels;
		int height = metrics.heightPixels;

		// send cockpit site info message
		socketManager.sendMessage(new CockpitSiteInfoMsg(width, height));
	}

	@Override
	public void OnMessageReceived(BaseMsg msg) {
		Log.d(TAG, "OnMessageReceived(...) ENTER");
		
		InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(firstInput.getWindowToken(), 0);

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

				hostName = firstInput.getText().toString().trim();

				if (connectionsPoints == null) {
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

			case MessagesValues.ClassID.LoginScreenImageMsg:

				ScreenImageResponse imageResponse = (ScreenImageResponse) msg
						.getJsonResponse();

				byte[] bytes = imageResponse.getImage();

				try {
					Bitmap bmp = BitmapFactory.decodeByteArray(bytes, 0,
							bytes.length);
					((ImageView) layout.findViewById(R.id.loginScreenImage))
							.setImageBitmap(bmp);
					fs.saveBitmap(imageName, bmp);
				} catch (Exception e) {
					Log.i(TAG, "ConnectionActivity#OnMessageReceived(...) exception with image cokpit");
					Log.e(TAG, "ERROR: ", e);
				}
				break;
			case MessagesValues.ClassID.LoginMsg:
				
				LoginMsgResponse loginResponse = (LoginMsgResponse) msg.getJsonResponse();
				Log.i(TAG, "ConnectionActivity#OnMessageReceived(...) return code: " + loginResponse.getReturnCode());

				switch (loginResponse.getReturnCode()) {
				
				case MessagesValues.LoginReturnCode.LoginSuccess:
					Intent intent = new Intent(ConnectionActivity.this, DesktopActivity.class);
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
			p.setConnectionMode(spinner_choose);
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
								startLoadingScreen();
								socketManager.openSocket(secondInput.getText()
										.toString(), Integer.valueOf(thirdInput
										.getText().toString()), new Handler());
							}
						}).setNegativeButton("ok",

				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						setAddConnectionMode();
					}
				}

				).show();
	}

	@Override
	public void OnIOError(final String exception) {
		Log.d(TAG, "OnIOError(...) ENTER");
		
		Log.i(TAG, "ConnectionActivity#OnIOError(...) exception: " + exception);
		
		try {
			runOnUiThread(new Runnable() {
				public void run() {
					if ("Retry".equals(exception)) {
						showDialogConenctAgain();
					} else if ("Disconnect".equals(exception)) {
						finish();
					}

					stopLoadingScreen();
				}
			});
		} catch (Exception e) {
			Log.e(TAG, "ERROR: ", e);
		}
	}

	/**
	 * open socket according to user input
	 */
	private void openSocket() {
		Log.d(TAG, "openSocket(...) ENTER");
		
		if (!validateInputs())
			return;

		startLoadingScreen();
		
		String host = secondInput.getText().toString();
		Integer port = Integer.valueOf(thirdInput.getText().toString());
		
		Log.i(TAG, "ConnectionActivity#openSocket(...) host: " + host);
		Log.i(TAG, "ConnectionActivity#openSocket(...) port: " + port);
		
		socketManager.openSocket(host, port, new Handler());
	}

	/**
	 * validate input fields according to screen mode
	 * 
	 * @return - true if all fields valid, false otherwise
	 */
	private boolean validateInputs() {
		Log.d(TAG, "validateInputs(...) ENTER");

		boolean valid = true;

		switch (mode) {

		case AddConnection:

			String ip = secondInput.getText().toString();
			if (!isButtonAvailabile() /* || !(ip.equals(Patterns.IP_ADDRESS)) */) {
				valid = false;
			}

			String port = thirdInput.getText().toString();
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
				valid = (secondInput.getText().toString().trim()
						.equals(thirdInput.getText().toString().trim()));

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

		if (firstInput.getText().toString().trim().isEmpty()) {
			firstInput.setError(null);
			isAvaliable = false;
		}

		if (secondInput.getText().toString().trim().isEmpty()) {
			secondInput.setError(null);
			isAvaliable = false;
		}

		if (thirdInput.getText().toString().trim().isEmpty()) {
			thirdInput.setError(null);
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

		Log.i(TAG, "ConnectionActivity#saveProfileDetails(...) connection name: " + firstInput.getText().toString());
		Log.i(TAG, "ConnectionActivity#saveProfileDetails(...) domain: " + thirdInput.getText().toString());

		Editor editor = pref.edit();
		editor.putString("name", firstInput.getText().toString());
		editor.putString("domain", thirdInput.getText().toString());
		editor.commit();
	}

	/**
	 * get the details of the user from the internal memory
	 */
	public void getProfileDetails() {
		Log.d(TAG, "getProfileDetails(...) ENTER");
		
		SharedPreferences pref = getApplicationContext().getSharedPreferences("MyPref", MODE_PRIVATE);
		firstInput.setText(pref.getString("name", ""));
		thirdInput.setText(pref.getString("domain", ""));
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

		if (mode == ConnectionActivityMode.Login) {
			userName = firstInput.getText().toString().trim();
			password = secondInput.getText().toString().trim();
			domain = thirdInput.getText().toString().trim();
		} else if (mode == ConnectionActivityMode.ResetPassword) {
			userName = firstInput.getText().toString().trim();
			password = secondInput.getText().toString().trim();
			domain = connectionsPoints.get(0).getDomain();
		}
		
		if (connectionsPoints == null) {
			connectionsPoints = new ArrayList<ConnectionPoint>();
		}
		
		// save params in connection point
		ConnectionPoint cp = new ConnectionPoint();
		cp.setUserName(userName);
		cp.setPassword(password);
		cp.setDomain(domain);
		cp.setConnectionMode("***");
		connectionsPoints.add(cp);

		ConnectionsDB.saveCredentialsForExistingConnection(hostName, userName, domain);

		saveProfileDetails();

		// save the connection point in our global app
		// so it can be used when re-authenticating when connecting to RDP
		GlobalApp.SetConnectionPoint(connectionsPoints.get(0));

		startLoadingScreen();

		socketManager.sendMessage(new LoginMsg(userName, password, domain,
				Build.MODEL, Secure.getString(getContentResolver(),
						Secure.ANDROID_ID)));
	}

	/**
	 * send reset password message to server
	 */
	private void doPasswordReset() {
		Log.d(TAG, "doPasswordReset(...) ENTER");

		Logger.log(LogLevel.INFO, "Zacky: ConnectionActivity::doPasswordReset");

		if (!validateInputs())
			return;

		String oldPassword = firstInput.getText().toString().trim();
		String newPassword = secondInput.getText().toString().trim();
		String userName = connectionsPoints.get(0).getUserName();
		String domain = connectionsPoints.get(0).getDomain();

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
	public class MyAdapter extends ArrayAdapter<String> {

		public MyAdapter(Context context, int textViewResourceId,
				String[] objects) {
			super(context, textViewResourceId, objects);
		}

		@Override
		public View getDropDownView(int position, View convertView,
				ViewGroup parent) {
			return getCustomView(position, convertView, parent);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			return getCustomView(position, convertView, parent);
		}

		public View getCustomView(int position, View convertView,
				ViewGroup parent) {

			LayoutInflater inflater = getLayoutInflater();
			View row = inflater.inflate(R.layout.spinner_iten, parent, false);
			TextView label = (TextView) row
					.findViewById(R.id.spinner_item_choose);
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
				
				Bitmap bmp = fs.getBitmap(imageName, true);
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
					((ImageView) layout.findViewById(R.id.loginScreenImage))
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
						intent.putExtra("type", "type");
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
		switch (mode) {
		case AddConnection:
			if (!ConnectionsDB.isDBEmpty()) {
				backBtn.setVisibility(View.VISIBLE);
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
									backBtn.setVisibility(View.VISIBLE);
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
			
			String input = getIntent().getExtras().getString("type");

			Log.i(TAG, "ConnectionActivity.NetworkChangeReceiver#onReceive(...) network type: " + input);

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

		public boolean isConnected(Context context, Intent intent) {
			return isConnectedTo3G(context);
		}
	}
}
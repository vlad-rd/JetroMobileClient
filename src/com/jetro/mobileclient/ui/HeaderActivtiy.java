package com.jetro.mobileclient.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;

import android.graphics.drawable.AnimationDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;

import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.TextView;

import com.freerdp.freerdpcore.sharedobjects.utils.Constants;
import com.freerdp.freerdpcore.sharedobjects.utils.Logger;
import com.jetro.mobileclient.R;

public abstract class HeaderActivtiy extends BaseActivity {

	private final String TAG = getClass().getSimpleName();
	private RelativeLayout baseLayoutConteiner;
	private TextView headerTextview;
	protected ImageView backBtn;
	private ImageView progressImage;
	private AnimationDrawable progressAnimation;
	public static boolean IS_LAST_ACTIVITY = false;
	public static boolean IS_OK = true;
	protected ImageView button1;
	private Context _context = HeaderActivtiy.this;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.d(TAG, "onCreate(...) ENTER");
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.header_activity_layout);
		
		baseLayoutConteiner = (RelativeLayout) findViewById(R.id.baseContainer);
		headerTextview = (TextView) findViewById(R.id.headerText);
		backBtn = (ImageView) findViewById(R.id.backBtn);
		progressImage = (ImageView) findViewById(R.id.progressBarImage);
		initlizedMenuButton();
	}

	public void showPopUp() {
		new AlertDialog.Builder(this)
				.setCancelable(false)
				.setMessage(Constants.POPUP_EXIT_MESSAGE)
				.setPositiveButton(Constants.YES_MESSAGE,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								finish();
								// System.exit(0);
							}
						})
				.setNegativeButton(Constants.NO_MESSAGE, null)
				.show();
	}

	private void initlizedMenuButton() {
		button1 = (ImageView) findViewById(R.id.header_menu_button);
		button1.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				showPopupOptions(button1);
			}
		});
	}

	/**
	 * show the popup with a three options
	 * 
	 * @param button1
	 *            the button of the options in header activity
	 */
	public void showPopupOptions(final ImageView button1) {
		PopupMenu popup = new PopupMenu(HeaderActivtiy.this, button1);
		// Inflating the Popup using xml file
		popup.getMenuInflater().inflate(R.menu.popup_menu, popup.getMenu());
		// registering popup with OnMenuItemClickListener
		popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
			public boolean onMenuItemClick(MenuItem item) {
				if (item.getTitle().equals("Exit")) {
					showPopUp();
				}
				if (item.getTitle().equals("About")) {
					if (!isConnectingToInternet()) {
						showDialogExit();
					} else {
						Intent intent = new Intent(HeaderActivtiy.this, AboutPopupActivity.class);
						intent.putExtra("type", "About");
						startActivity(intent);
					}
				}
				if (item.getTitle().equals("Help")) {
					if (!isConnectingToInternet()) {
						showDialogExit();
					} else {
						Intent intent = new Intent(HeaderActivtiy.this, AboutPopupActivity.class);
						intent.putExtra("type", "Help");
						startActivity(intent);
					}
				}
				return true;
			}
		});
		popup.show();// showing popup menu
	}

	public boolean isConnectingToInternet() {
		ConnectivityManager connectivity = (ConnectivityManager) _context
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		if (connectivity != null) {
			NetworkInfo[] info = connectivity.getAllNetworkInfo();
			if (info != null) {
				for (int i = 0; i < info.length; i++)
					if (info[i].getState() == NetworkInfo.State.CONNECTED) {
						return true;
					}
			}
		}
		return false;
	}

	private void showDialogExit() {
		new AlertDialog.Builder(HeaderActivtiy.this)
				.setMessage("There is no internet connection!")
				.setCancelable(false)
				.setPositiveButton("", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {

						Intent intent = new Intent(HeaderActivtiy.this,
								AboutPopupActivity.class);
						startActivity(intent);

						dialog.dismiss();
					}
				}).setNegativeButton("Done", null).show();
	}

	/**
	 * should be implemented in every activity that extends
	 * {@link HeaderActivtiy} implementation of this method should call
	 * setHeaderTextAndBehavior(OnClickListener backBtnListener,
	 * intheaderStringResourceId) method in header activity
	 */
	protected abstract void setHeader();

	/**
	 * Method for inflating and adding the activity layout in the base container
	 * 
	 * @param layoutResource
	 *            - the wanted layout resource id
	 * @return - the inflated view
	 */
	protected View addActivityLayoutInBaseContainer(int layoutResourceId) {
		try {
			LayoutInflater baseInflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);

			if (layoutResourceId != 0) {
				View v = baseInflater.inflate(layoutResourceId, null);

				if (baseLayoutConteiner != null) {
					baseLayoutConteiner.addView(v, new LayoutParams(
							LayoutParams.MATCH_PARENT,
							LayoutParams.MATCH_PARENT));
				}
				return v;
			}
		} catch (Exception e) {
			Logger.e(e, TAG + " : Error adding layout : "
					+ getResources().getResourceName(layoutResourceId)
					+ ", in base layout");
		}
		return null;
	}

	protected void startLoadingScreen() {

		((ViewGroup) progressImage.getParent()).setVisibility(View.VISIBLE);
		progressImage.setBackgroundResource(R.anim.progress_bar);
		progressAnimation = (AnimationDrawable) progressImage.getBackground();
		progressAnimation.start();
	}

	protected void stopLoadingScreen() {
		new Handler().postDelayed(new Runnable() {
			@Override
			public void run() {
				((ViewGroup) progressImage.getParent())
						.setVisibility(View.GONE);
				progressAnimation.stop();
			}
		}, 1000);
	}

	/**
	 * set header text and back button action
	 * 
	 * @param backBtnListener
	 *            - listener for button action or null for no action
	 */
	protected void setHeaderTextAndBehavior(OnClickListener backBtnListener) {
		backBtn.setOnClickListener(backBtnListener);
	}

	/**
	 * set header text of the screen by resource identifier reference.
	 * 
	 * @param text
	 *            - the string to set
	 */
	protected void setHeaderText(String text) {
		headerTextview.setText(text);
	}
}

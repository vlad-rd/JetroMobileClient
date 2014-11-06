package com.jetro.mobileclient.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.WebView;

import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import com.freerdp.freerdpcore.sharedobjects.utils.Constants;
import com.jetro.mobileclient.R;

public class AboutPopupActivity extends HeaderActivtiy {
	
	private String TAG = AboutPopupActivity.class.getSimpleName();
	ImageView backBtn;
	private Context _context = AboutPopupActivity.this;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.d(TAG, "onCreate(...) ENTER");
		
		super.onCreate(savedInstanceState);
		setContentView(R.layout.about_popup_layout);
		String input = getIntent().getExtras().getString("type");

		TextView headerText = (TextView) findViewById(R.id.option_about_header_textView);
		WebView webView = (WebView) findViewById(R.id.about_webview_popup);
		backBtn = (ImageView) findViewById(R.id.backBtn1);
		backBtn.setBackgroundResource(R.drawable.selector_back_button);
		backBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {

				AboutPopupActivity.this.finish();

			}
		});

		if (input.equals("About")) {
			headerText.setText(Constants.ABOUT);
			webView.loadUrl(Constants.ABOUT_LINK);

		} else {
			headerText.setText(Constants.HELP);
			webView.loadUrl(Constants.HELP_LINK);

		}

	}

	@Override
	protected void setHeader() {
		// TODO Auto-generated method stub

	}

	

	/**
	 * show the popup with a three options
	 * 
	 * @param button1
	 *            the button of the options in header activity
	 */
	public void showPopupOptions(final ImageView button1) {
		PopupMenu popup = new PopupMenu(AboutPopupActivity.this, button1);
		// Inflating the Popup using xml file
		popup.getMenuInflater().inflate(R.menu.popup_menu, popup.getMenu());
		// registering popup with OnMenuItemClickListener
		popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
			public boolean onMenuItemClick(MenuItem item) {
				button1.setBackgroundResource(R.drawable.more);

				if (item.getTitle().equals("Exit")) {
					showPopUp();
				}
				if (item.getTitle().equals("About")) {

					if (!isConnectingToInternet()) {

						showDialogExit();
					} else {
						Intent intent = new Intent(AboutPopupActivity.this,
								AboutPopupActivity.class);
						intent.putExtra("type", "About");
						startActivity(intent);
					}
				}
				if (item.getTitle().equals("Help")) {
					if (!isConnectingToInternet()) {

						showDialogExit();
					} else {
						Intent intent = new Intent(AboutPopupActivity.this,
								AboutPopupActivity.class);
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
			if (info != null)
				for (int i = 0; i < info.length; i++)
					if (info[i].getState() == NetworkInfo.State.CONNECTED) {
						return true;
					}

		}
		return false;
	}

	private void showDialogExit() {
		new AlertDialog.Builder(AboutPopupActivity.this)
				.setMessage("There is no internet connection!")
				.setCancelable(false)
				.setPositiveButton("", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {

						Intent intent = new Intent(AboutPopupActivity.this,
								AboutPopupActivity.class);
						startActivity(intent);

						dialog.dismiss();
					}
				}).setNegativeButton("Done", null).show();
	}

}

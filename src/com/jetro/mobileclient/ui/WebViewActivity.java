package com.jetro.mobileclient.ui;

import android.os.Bundle;
import android.util.Log;
import android.webkit.WebView;

import com.jetro.mobileclient.R;
import com.jetro.mobileclient.config.Config;
import com.jetro.mobileclient.ui.activities.base.HeaderActivity;

public class WebViewActivity extends HeaderActivity {
	
	private static final String TAG = WebViewActivity.class.getSimpleName();
	
	private WebView mWebView;
	
	/**
	 * Represents the type of the web view activity.
	 * 
	 * @author ran.h
	 *
	 */
	public enum Type {
		ABOUT,
		HELP
	}

	private Type mType;

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		Log.d(TAG, TAG + "#onSaveInstanceState(...) ENTER");
		
		// Save the user's current game state
		outState.putSerializable(Config.Extras.EXTRA_TYPE, mType);
		
		// Always call the superclass so it can save the view hierarchy state
		super.onSaveInstanceState(outState);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.d(TAG, TAG + "#onCreate(...) ENTER");
		super.onCreate(savedInstanceState);
		
		setBaseContentView(R.layout.activity_web_view);
		
		// Check whether we're recreating a previously destroyed instance
		if (savedInstanceState != null) {
			// Restore value of members from saved state
			mType = (Type) savedInstanceState.getSerializable(Config.Extras.EXTRA_TYPE);
		} else {
			// Probably initialize members with default values for a new instance
			mType = (Type) getIntent().getSerializableExtra(Config.Extras.EXTRA_TYPE);
		}

		mWebView = (WebView) findViewById(R.id.webview);

		switch (mType) {
		case ABOUT:
			setHeaderTitleText(R.string.header_title_about);
			mWebView.loadUrl(getString(R.string.link_about));
			break;
		case HELP:
			setHeaderTitleText(R.string.header_title_help);
			mWebView.loadUrl(getString(R.string.link_help));			
			break;
		}

	}

	@Override
	protected void setHeader() {
	}

}

package com.jetro.mobileclient.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.jetro.mobileclient.R;
import com.jetro.mobileclient.config.Config;
import com.jetro.mobileclient.ui.activities.base.HeaderActivity;

public class WebViewActivity extends HeaderActivity {
	
	private static final String TAG = WebViewActivity.class.getSimpleName();
	
	private View mBaseContentView;
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
	protected void onNewIntent(Intent intent) {
		Log.d(TAG, TAG + "#onNewIntent(...) ENTER");
		super.onNewIntent(intent);
		
		mType = (Type) intent.getSerializableExtra(Config.Extras.EXTRA_TYPE);
		
		// Check whether we're recreating a previously destroyed instance
		switch (mType) {
		case ABOUT:
			setHeaderTitleText(R.string.header_title_about);
			String aboutUrl = getString(R.string.link_about);
			Log.i(TAG, TAG + "#onCreate(...) about url = " + aboutUrl);
			mWebView.loadUrl(aboutUrl);
			break;
		case HELP:
			setHeaderTitleText(R.string.header_title_help);
			String helpUrl = getString(R.string.link_help);
			Log.i(TAG, TAG + "#onCreate(...) help url = " + helpUrl);
			mWebView.loadUrl(helpUrl);
			break;
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.d(TAG, TAG + "#onCreate(...) ENTER");
		super.onCreate(savedInstanceState);
		
		// Check whether we're recreating a previously destroyed instance
		if (savedInstanceState != null) {
			// Restore value of members from saved state
			mType = (Type) savedInstanceState.getSerializable(Config.Extras.EXTRA_TYPE);
		} else {
			// Probably initialize members with default values for a new instance
			mType = (Type) getIntent().getSerializableExtra(Config.Extras.EXTRA_TYPE);
		}

		mBaseContentView = setBaseContentView(R.layout.activity_web_view);
		mWebView = (WebView) mBaseContentView.findViewById(R.id.webview);
		mWebView.setWebViewClient(new WebViewClient());

		switch (mType) {
		case ABOUT:
			setHeaderTitleText(R.string.header_title_about);
			String aboutUrl = getString(R.string.link_about);
			Log.i(TAG, TAG + "#onCreate(...) about url = " + aboutUrl);
			mWebView.loadUrl(aboutUrl);
			break;
		case HELP:
			setHeaderTitleText(R.string.header_title_help);
			String helpUrl = getString(R.string.link_help);
			Log.i(TAG, TAG + "#onCreate(...) help url = " + helpUrl);
			mWebView.loadUrl(helpUrl);
			break;
		}
	}
	
	@Override
	public void onBackPressed() {
		if (mWebView.canGoBack()) {
			mWebView.goBack();
		} else {
			super.onBackPressed();
		}
	}

	@Override
	protected void setHeader() {
	}

}

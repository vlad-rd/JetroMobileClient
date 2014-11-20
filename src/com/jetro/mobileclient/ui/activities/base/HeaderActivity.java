package com.jetro.mobileclient.ui.activities.base;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.jetro.mobileclient.R;
import com.jetro.mobileclient.config.Config;
import com.jetro.mobileclient.ui.AboutPopupActivity;
import com.jetro.mobileclient.utils.ConnectivityUtils;

public abstract class HeaderActivity extends Activity {

	private final String TAG = HeaderActivity.class.getSimpleName();
	
	protected ImageView mHeaderBackButton;
	private TextView mHeaderTitle;
	protected ImageView mHeaderMenuButton;
	private RelativeLayout mBaseConteiner;
	
	private ViewGroup mProgressBarContainer;
	private ImageView mProgressBarImage;
	
	private AnimationDrawable progressAnimation;
	public static boolean IS_LAST_ACTIVITY = false;
	public static boolean IS_OK = true;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.d(TAG, TAG + "#onCreate(...) ENTER");
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.activity_header);
		
		// Sets Header widgets
		mHeaderBackButton = (ImageView) findViewById(R.id.header_back_button);
		mHeaderBackButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				onBackPressed();
			}
		});
		mHeaderTitle = (TextView) findViewById(R.id.header_title);
		mHeaderMenuButton = (ImageView) findViewById(R.id.header_menu_button);
		mHeaderMenuButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				launchPopupOptions(v);
			}
		});
		// Sets Content widgets
		mBaseConteiner = (RelativeLayout) findViewById(R.id.base_container);
		// Sets Progress Bar widget
		mProgressBarContainer = (ViewGroup) findViewById(R.id.progress_bar_container);
		mProgressBarImage = (ImageView) findViewById(R.id.progress_bar_image);
	}
	
	/**
	 * Shows the header action bar overflow action menu.
	 */
	public void launchPopupOptions(View anchor) {
		Log.d(TAG, TAG + "#launchPopupOptions(...) ENTER");
		
		PopupMenu popup = new PopupMenu(HeaderActivity.this, anchor);
		popup.getMenuInflater().inflate(R.menu.activity_header_menu, popup.getMenu());
		popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
			public boolean onMenuItemClick(MenuItem item) {
				switch (item.getItemId()) {
				case R.id.header_menu_item_about:
				case R.id.header_menu_item_help: {
					boolean isConnected = ConnectivityUtils.isNetworkConnected(HeaderActivity.this);
					if (!isConnected) {
						launchNoInternetConnectionDialog();
					} else {
						String type = null;
						if (item.getItemId() == R.id.header_menu_item_about) {
							type = "About";
						} else if (item.getItemId() == R.id.header_menu_item_help) {
							type = "Help";
						}
						Intent intent = new Intent(HeaderActivity.this, AboutPopupActivity.class);
						intent.putExtra(Config.Extras.EXTRA_TYPE, type);
						startActivity(intent);
					}
					return true;
				}
				case R.id.header_menu_item_exit:
					launchExitDialog();
					return true;
				default:
					return false;
				}
			}
		});
		popup.show();
	}
	
	private void launchNoInternetConnectionDialog() {
		new AlertDialog.Builder(HeaderActivity.this)
				.setCancelable(false)
				.setMessage(R.string.dialog_no_internet_connection_message)
				.setNegativeButton(R.string.dialog_no_internet_connection_negative_btn, null)
				.show();
	}

	public void launchExitDialog() {
		new AlertDialog.Builder(this)
				.setCancelable(false)
				.setMessage(R.string.dialog_exit_message)
				.setPositiveButton(R.string.dialog_exit_positive_btn, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						finish();
					}
				})
				.setNegativeButton(R.string.dialog_exit_negative_btn, null)
				.show();
	}

	/**
	 * should be implemented in every activity that extends
	 * {@link HeaderActivity} implementation of this method should call
	 * setHeaderTextAndBehavior(OnClickListener backBtnListener,
	 * intheaderStringResourceId) method in header activity
	 */
	protected abstract void setHeader();

	/**
	 * Sets the header back button action (behavior).
	 * 
	 * @param backBtnListener
	 *            - listener for button action or null for no action
	 */
	protected void setHeaderBackButtonBehavior(OnClickListener backBtnListener) {
		mHeaderBackButton.setOnClickListener(backBtnListener);
	}

	/**
	 * Sets the header title text.
	 * 
	 * @param stringResourceId
	 *            - the title string resource id
	 */
	protected void setHeaderTitleText(int stringResourceId) {
		mHeaderTitle.setText(stringResourceId);
	}
	
	/**
	 * Inflates and adds the sub activity base content layout to the header
	 * activity base content layout container.
	 * 
	 * @param layoutResource
	 *            - the base content layout resource id
	 * @return - the inflated view
	 */
	protected View setBaseContentView(int layoutResourceId) {
		
		// Validates base content layout resource id
		if (layoutResourceId == 0) {
			Log.w(TAG, TAG + "#setBaseContentView(...) Invalid layoutResourceId = " + layoutResourceId);
			return null;
		}
		// Validates there is base content container
		if (mBaseConteiner == null) {
			Log.w(TAG, TAG + "#setBaseContentView(...) Invalid mBaseConteiner = null");
			return null;
		}
		
		View view = getLayoutInflater().inflate(layoutResourceId, null);
		mBaseConteiner.addView(view);
		return view;
	}

	protected void startLoadingScreen() {
		mProgressBarContainer.setVisibility(View.VISIBLE);
		progressAnimation = (AnimationDrawable) mProgressBarImage.getBackground();
		progressAnimation.start();
	}

	protected void stopLoadingScreen() {
		new Handler().postDelayed(new Runnable() {
			@Override
			public void run() {
				mProgressBarContainer.setVisibility(View.GONE);
				progressAnimation.stop();
			}
		}, 1000);
	}
}

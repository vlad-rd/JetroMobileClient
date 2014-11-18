package com.jetro.mobileclient.ui.activities;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.freerdp.freerdpcore.application.GlobalApp;
import com.jetro.mobileclient.R;
import com.jetro.mobileclient.model.beans.Host;
import com.jetro.mobileclient.repository.ConnectionsDB;
import com.jetro.mobileclient.ui.activities.base.HeaderActivity;
import com.jetro.mobileclient.ui.dialogs.DialogLauncher;
import com.jetro.mobileclient.utils.Config;
import com.jetro.protocol.Protocols.Controller.ConnectionPoint;

public class ConnectionsListActivity extends HeaderActivity {

	private static final String TAG = ConnectionsListActivity.class.getSimpleName();
	
	private ConnectionsDB mConnectionsDB;
	
	private List<Host> mHosts = null;
	
	private View mBaseContentLayout;
	private View mAddNewConnectionButton;
	private ConnectionsListAdapter mConnectionsAdapter;
	private ListView mConnectionsList;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.d(TAG, TAG + "#onCreate(...) ENTER");
		super.onCreate(savedInstanceState);
		
		mConnectionsDB = ConnectionsDB.getInstance(getApplicationContext());

		setHeaderTitleText(R.string.header_title_Connections);
		mHeaderBackButton.setVisibility(View.INVISIBLE);
		
		mBaseContentLayout = setBaseContentView(R.layout.connections_list_activity_layout);
		mAddNewConnectionButton = mBaseContentLayout.findViewById(R.id.add_new_connection_button);
		mAddNewConnectionButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(ConnectionsListActivity.this,
						ConnectionActivity.class);
				intent.putExtra(Config.Extras.EXTRA_CONNECTION_ACTIVITY_STATE,
						ConnectionActivity.State.ADD_CONNECTION);
				startActivity(intent);
				finish();
			}
		});
		mConnectionsList = (ListView) mBaseContentLayout.findViewById(R.id.connections_list);
	}
	
	@Override
	protected void onResume() {
		Log.d(TAG, TAG + "#onResume(...) ENTER");
		super.onResume();
		
		// If there are mHosts, list them
		boolean hasHosts = mConnectionsDB.hasHosts();
		if (hasHosts) {
			mHosts = mConnectionsDB.getAllHosts();
			mConnectionsAdapter = new ConnectionsListAdapter(mHosts);
			mConnectionsList.setAdapter(mConnectionsAdapter);
		// If there are none mHosts, redirect to ConnectionActivity,
		// to add the first connection
		} else {
			Intent intent = new Intent(ConnectionsListActivity.this, ConnectionActivity.class);
			intent.putExtra(Config.Extras.EXTRA_CONNECTION_ACTIVITY_STATE,
					ConnectionActivity.State.ADD_CONNECTION);
			startActivity(intent);
			finish();
		}
	}

	@Override
	public void onBackPressed() {
		Log.d(TAG, TAG + "#onBackPressed(...) ENTER");
		
		DialogLauncher.launchExitConnectionsListDialog(
				ConnectionsListActivity.this,
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						if (which == DialogInterface.BUTTON_POSITIVE) {
							finish();
						}
					}
				});
	}

	@Override
	public void finish() {
		Log.d(TAG, TAG + "#finish(...) ENTER");
		
		GlobalApp.getSocketManager(null).closeSocket();
		Log.i(TAG, TAG + "#finish(...) Terminating the main socket");
		
		super.finish();
	}

	@Override
	protected void setHeader() {
	}

	/**
	 * @author ran.h
	 *
	 */
	private class ConnectionsListAdapter extends BaseAdapter {

		private List<Host> mHosts;
		private LayoutInflater mInflater;

		public ConnectionsListAdapter(List<Host> hosts) {
			mHosts = hosts;
			mInflater = getLayoutInflater();
		}

		@Override
		public int getCount() {
			return mHosts.size();
		}

		@Override
		public Object getItem(int position) {
			return mHosts.get(position);
		}

		@Override
		public long getItemId(int position) {
			return 0;
		}

		@Override
		public View getView(final int position, View convertView, ViewGroup parent) {

			final Host host = mHosts.get(position);
			
			String name = host.getHostName();
			Set<ConnectionPoint> connectionPoints = host.getConnectionPoints();
			
			convertView = mInflater.inflate(R.layout.connection_list_item_layout, parent, false);
			TextView hostName = (TextView) convertView.findViewById(R.id.host_name);
			hostName.setText(name);
			TextView hostIp = (TextView) convertView.findViewById(R.id.host_ip);
			hostIp.setText(Arrays.toString(connectionPoints.toArray()));

			convertView.findViewById(R.id.itemBg).setOnClickListener(
					new OnClickListener() {
						@Override
						public void onClick(View v) {
							launchConnectionTypesDialog(host);
						}
					});

			convertView.findViewById(R.id.itemBg).setOnLongClickListener(
					new OnLongClickListener() {
						@Override
						public boolean onLongClick(View v) {
							launchHostActionsDialog(host);
							return true;
						}
					});

			convertView.findViewById(R.id.delete_button).setOnClickListener(
					new OnClickListener() {
						@Override
						public void onClick(View v) {
							launchHostConnectionDialog(host, position);
						}
					});
			return convertView;
		}
		
		private void launchConnectionTypesDialog(final Host host) {
			Log.d(TAG, TAG + "#launchConnectionTypesDialog(...) ENTER");

			final String[] connectionsTypes = getResources().getStringArray(R.array.connection_types_options);

			final Dialog connectionsTypesDialog = new Dialog(ConnectionsListActivity.this);
			connectionsTypesDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
			connectionsTypesDialog.setContentView(R.layout.dialog_connection_types_list);
			ListView connectionsTypesList = (ListView) connectionsTypesDialog.findViewById(R.id.connection_types_list);
			connectionsTypesList.setOnItemClickListener(new OnItemClickListener() {
				@Override
				public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
					String item = ((TextView) view).getText().toString();
					if (connectionsTypes[0].equals(item)) {
						Intent intent = new Intent(ConnectionsListActivity.this, LoginActivity.class);
						intent.putExtra(Config.Extras.EXTRA_IS_WAN, false);
						intent.putExtra(Config.Extras.EXTRA_HOST, host);
						startActivity(intent);
						finish();
					} else if (connectionsTypes[1].equals(item)) {
						Intent intent = new Intent(ConnectionsListActivity.this, LoginActivity.class);
						intent.putExtra(Config.Extras.EXTRA_IS_WAN, true);
						intent.putExtra(Config.Extras.EXTRA_HOST, host);
						startActivity(intent);
						finish();
					}
					connectionsTypesDialog.dismiss();
				}
			});
			connectionsTypesDialog.show();
		}
		
		private void launchHostActionsDialog(final Host host) {
			Log.d(TAG, TAG + "#launchHostActionsDialog(...) ENTER");

			final Dialog dialog = new Dialog(ConnectionsListActivity.this);
			dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
			dialog.setContentView(R.layout.dialog_connection_actions_list);
			ListView connectionActionsList = (ListView) dialog.findViewById(R.id.connection_actions_list);
			connectionActionsList.setOnItemClickListener(new OnItemClickListener() {
				@Override
				public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

					String[] connectionActionsOptions = getResources().getStringArray(R.array.connection_actions_options);
					String connectionActionOption = ((TextView) view).getText().toString();

					if (connectionActionsOptions[0].equals(connectionActionOption)) {
						Intent intent = new Intent(ConnectionsListActivity.this, LoginActivity.class);
						intent.putExtra(Config.Extras.EXTRA_HOST, host);
						startActivity(intent);
						finish();
					} else if (connectionActionsOptions[1].equals(connectionActionOption)) {
						Intent intent = new Intent(ConnectionsListActivity.this, ConnectionActivity.class);
						intent.putExtra(Config.Extras.EXTRA_CONNECTION_ACTIVITY_STATE, ConnectionActivity.State.VIEW_CONNECTION);
						intent.putExtra(Config.Extras.EXTRA_HOST, host);
						startActivity(intent);
						finish();
					} else if (connectionActionsOptions[2].equals(connectionActionOption)) {
						launchHostConnectionDialog(host, position);
					}

					dialog.dismiss();
				}
			});
			dialog.show();
		}
		
		private void launchHostConnectionDialog(final Host host, final int position) {
			Log.d(TAG, TAG + "#launchDeleteConnectionDialog(...) ENTER");
			
			DialogLauncher.launchDeleteConnectionDialog(
					ConnectionsListActivity.this,
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							mConnectionsDB.deleteHost(host.getHostName());
							mHosts.remove(position);
							if (mConnectionsDB.isDBEmpty()) {
								Intent intent = new Intent(ConnectionsListActivity.this, ConnectionActivity.class);
								intent.putExtra(Config.Extras.EXTRA_CONNECTION_ACTIVITY_STATE, ConnectionActivity.State.ADD_CONNECTION);
								startActivity(intent);
								finish();
							} else {
								notifyDataSetChanged();
							}
							dialog.dismiss();
						}
					});
		}
		
	}
	
}

package com.jetro.mobileclient.ui.activities;

import java.util.List;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.jetro.mobileclient.R;
import com.jetro.mobileclient.config.Config;
import com.jetro.mobileclient.model.beans.Connection;
import com.jetro.mobileclient.repository.ConnectionsDB;
import com.jetro.mobileclient.ui.activities.base.HeaderActivity;
import com.jetro.mobileclient.ui.dialogs.DialogLauncher;

/**
 * @author ran.h
 *
 */
public class ConnectionsListActivity extends HeaderActivity {

	private static final String TAG = ConnectionsListActivity.class.getSimpleName();
	
	private ConnectionsDB mConnectionsDB;
	
	private List<Connection> mConnections;
	
	private View mBaseContentLayout;
	private View mAddNewConnectionButton;
	private ConnectionsListAdapter mConnectionsAdapter;
	private ListView mConnectionsList;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.d(TAG, TAG + "#onCreate(...) ENTER");
		super.onCreate(savedInstanceState);
		
		mConnectionsDB = ConnectionsDB.getInstance(getApplicationContext());

		setHeaderTitleText(R.string.header_title_connections);
		mHeaderBackButton.setVisibility(View.INVISIBLE);
		
		mBaseContentLayout = setBaseContentView(R.layout.activity_connections_list);
		mAddNewConnectionButton = mBaseContentLayout.findViewById(R.id.add_new_connection_button);
		mAddNewConnectionButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(ConnectionsListActivity.this,
						ConnectionActivity.class);
				intent.putExtra(Config.Extras.EXTRA_CONNECTION_ACTIVITY_STATE,
						ConnectionActivity.State.ADD_CONNECTION);
				startActivity(intent);
			}
		});
		mConnectionsList = (ListView) mBaseContentLayout.findViewById(R.id.connections_list);
		mConnectionsList.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				Connection connection = mConnections.get(position);
				Intent intent = new Intent(ConnectionsListActivity.this, LoginActivity.class);
				intent.putExtra(Config.Extras.EXTRA_CONNECTION, connection);
				startActivity(intent);
			}
		});
		mConnectionsList.setOnItemLongClickListener(new OnItemLongClickListener() {
			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
				Connection connection = mConnections.get(position);
				launchConnectionActionsDialog(connection);
				return true;
			}
		});
	}
	
	@Override
	protected void onResume() {
		Log.d(TAG, TAG + "#onResume(...) ENTER");
		super.onResume();
		
		// If there are mConnections, list them
		boolean hasHosts = mConnectionsDB.hasConnections();
		if (hasHosts) {
			mConnections = mConnectionsDB.getAllConnections();
			mConnectionsAdapter = new ConnectionsListAdapter(mConnections);
			mConnectionsList.setAdapter(mConnectionsAdapter);
		// If there are none mConnections, redirect to ConnectionActivity,
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
		
		DialogLauncher.launchAppExitDialog(ConnectionsListActivity.this);
	}

	@Override
	protected void setHeader() {
	}

	/**
	 * @author ran.h
	 *
	 */
	private class ConnectionsListAdapter extends BaseAdapter {

		private List<Connection> mConnections;
		private LayoutInflater mInflater;

		public ConnectionsListAdapter(List<Connection> hosts) {
			mConnections = hosts;
			mInflater = getLayoutInflater();
		}

		@Override
		public int getCount() {
			return mConnections.size();
		}

		@Override
		public Object getItem(int position) {
			return mConnections.get(position);
		}

		@Override
		public long getItemId(int position) {
			return 0;
		}

		@Override
		public View getView(final int position, View convertView, ViewGroup parent) {
			ViewHolder holder;
			
			if (convertView == null) {
				holder = new ViewHolder();
				convertView = mInflater.inflate(R.layout.list_item_connection, parent, false);
				holder.connectionName = (TextView) convertView.findViewById(R.id.connection_name);
				holder.deleteButton = (Button) convertView.findViewById(R.id.delete_button);
				convertView.setTag(holder);
			} else {
				holder = (ViewHolder) convertView.getTag();
			}
			
			final Connection connection = mConnections.get(position);
			String connectionName = connection.getName();
			holder.connectionName.setText(connectionName);
			holder.deleteButton.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					launchDeleteConnectionDialog(connection, position);
				}
			});
			
			return convertView;
		}
		
		private class ViewHolder {
			TextView connectionName;
			Button deleteButton;
		}
		
	}
	
	private void launchConnectionActionsDialog(final Connection connection) {
		Log.d(TAG, TAG + "#launchConnectionActionsDialog(...) ENTER");

		final Dialog dialog = new Dialog(ConnectionsListActivity.this);
		dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		dialog.setContentView(R.layout.dialog_connection_actions_list);
		ListView connectionActionsList = (ListView) dialog.findViewById(R.id.connection_actions_list);
		connectionActionsList.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				String[] connectionActionsOptions = getResources().getStringArray(R.array.connection_actions_options);
				String connectionActionOption = ((TextView) view).getText().toString();

				// Connection option connect
				if (connectionActionsOptions[0].equals(connectionActionOption)) {
					Intent intent = new Intent(ConnectionsListActivity.this, LoginActivity.class);
					intent.putExtra(Config.Extras.EXTRA_CONNECTION, connection);
					startActivity(intent);
				// Connection option details
				} else if (connectionActionsOptions[1].equals(connectionActionOption)) {
					Intent intent = new Intent(ConnectionsListActivity.this, ConnectionActivity.class);
					intent.putExtra(Config.Extras.EXTRA_CONNECTION_ACTIVITY_STATE, ConnectionActivity.State.VIEW_CONNECTION);
					intent.putExtra(Config.Extras.EXTRA_CONNECTION, connection);
					startActivity(intent);
				// Connection option delete
				} else if (connectionActionsOptions[2].equals(connectionActionOption)) {
					launchDeleteConnectionDialog(connection, position);
				}
				dialog.dismiss();
			}
		});
		dialog.show();
	}
	
	private void launchDeleteConnectionDialog(final Connection host, final int position) {
		Log.d(TAG, TAG + "#launchDeleteConnectionDialog(...) ENTER");
		
		DialogLauncher.launchDeleteConnectionDialog(
				ConnectionsListActivity.this,
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						if (which == DialogInterface.BUTTON_POSITIVE) {
							mConnectionsDB.deleteConnection(host.getName());
							mConnections.remove(position);
							if (mConnectionsDB.isDBEmpty()) {
								Intent intent = new Intent(ConnectionsListActivity.this, ConnectionActivity.class);
								intent.putExtra(Config.Extras.EXTRA_CONNECTION_ACTIVITY_STATE, ConnectionActivity.State.ADD_CONNECTION);
								startActivity(intent);
								dialog.dismiss();
								finish();
							} else {
								mConnectionsAdapter.notifyDataSetChanged();
							}
						}
					}
				});
	}
	
}

package com.jetro.mobileclient.ui.activities;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

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
import com.freerdp.freerdpcore.sharedobjects.ConnectionPoint;
import com.jetro.mobileclient.R;
import com.jetro.mobileclient.repository.ConnectionsDB;
import com.jetro.mobileclient.ui.activities.base.HeaderActivity;
import com.jetro.mobileclient.ui.dialogs.DialogLauncher;
import com.jetro.mobileclient.utils.Config;

public class ConnectionsListActivity extends HeaderActivity {

	private static final String TAG = ConnectionsListActivity.class.getSimpleName();
	
	private ConnectionsDB mConnectionsDB;
	
	private ArrayList<HashMap<String, ArrayList<ConnectionPoint>>> itemsFromDB = new ArrayList<HashMap<String, ArrayList<ConnectionPoint>>>();
	private ArrayList<ViewItem> itemsToShow = new ArrayList<ViewItem>();
	
	private View mBaseContentLayout;
	private View mAddNewConnectionButton;
	private ConnectionsListAdapter mConnectionsAdapter;
	private ListView mConnectionsList;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.d(TAG, TAG + "#onCreate(...) ENTER");
		super.onCreate(savedInstanceState);
		
		mConnectionsDB = ConnectionsDB.getInstance(ConnectionsListActivity.this);

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

		itemsFromDB.clear();
		itemsToShow.clear();

		itemsFromDB = mConnectionsDB.getAllSavedConnections();
		
		// If there are none connections, redirect to ConnectionActivity,
		// to add the first connection
		if (itemsFromDB.size() == 0) {
			Intent intent = new Intent(ConnectionsListActivity.this, ConnectionActivity.class);
			intent.putExtra(Config.Extras.EXTRA_CONNECTION_ACTIVITY_STATE,
					ConnectionActivity.State.ADD_CONNECTION);
			startActivity(intent);
			finish();
		} else {
			for (HashMap<String, ArrayList<ConnectionPoint>> item : itemsFromDB) {
				for (String key : item.keySet()) {
					itemsToShow.add(new ViewItem(key, item.get(key)));
					mConnectionsAdapter = new ConnectionsListAdapter(itemsToShow);
					mConnectionsList.setAdapter(mConnectionsAdapter);
				}
			}
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

	private class ViewItem {

		String name;
		ArrayList<ConnectionPoint> cps;

		public ViewItem(String name, ArrayList<ConnectionPoint> cps) {
			this.name = name;
			this.cps = cps;
		}

		public String getName() {
			return name;
		}

		public ArrayList<ConnectionPoint> getConnectionsPoints() {
			return cps;
		}
	}

	private class ConnectionsListAdapter extends BaseAdapter {

		ArrayList<ViewItem> mItems;
		LayoutInflater mInflater;

		public ConnectionsListAdapter(ArrayList<ViewItem> items) {
			mItems = items;
			mInflater = getLayoutInflater();
		}

		@Override
		public int getCount() {
			return mItems.size();
		}

		@Override
		public Object getItem(int position) {
			return mItems.get(position);
		}

		@Override
		public long getItemId(int position) {
			return 0;
		}

		@Override
		public View getView(final int position, View convertView, ViewGroup parent) {

			final ArrayList<ConnectionPoint> cps = mItems.get(position).getConnectionsPoints();
			
			convertView = mInflater.inflate(R.layout.connection_list_item_layout, parent, false);
			TextView hostName = (TextView) convertView.findViewById(R.id.host_name);
			hostName.setText(mItems.get(position).getName());
			TextView hostIp = (TextView) convertView.findViewById(R.id.host_ip);
			hostIp.setText(Arrays.toString(cps.toArray()));
			

			convertView.findViewById(R.id.itemBg).setOnClickListener(
					new OnClickListener() {
						@Override
						public void onClick(View v) {
							launchConnectionTypesDialog(cps);
						}
					});

			convertView.findViewById(R.id.itemBg).setOnLongClickListener(
					new OnLongClickListener() {
						@Override
						public boolean onLongClick(View v) {
							launchConnectionActionsDialog(cps, position);
							return true;
						}
					});

			convertView.findViewById(R.id.delete_button).setOnClickListener(
					new OnClickListener() {
						@Override
						public void onClick(View v) {
							launchDeleteConnectionDialog(cps, position);
						}
					});
			return convertView;
		}
		
		private void launchConnectionTypesDialog(final ArrayList<ConnectionPoint> cps) {
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
						// Filters only LAN connections points
						ArrayList<ConnectionPoint> lanCps = filterConnectionsPoints(cps, false);
						Intent intent = new Intent(ConnectionsListActivity.this, LoginActivity.class);
						intent.putParcelableArrayListExtra(Config.Extras.EXTRA_CONNECTIONS_POINTS, lanCps);
						startActivity(intent);
						finish();
					} else if (connectionsTypes[1].equals(item)) {
						// Filters only WAN connections points
						ArrayList<ConnectionPoint> wanCps = filterConnectionsPoints(cps, true);
						Intent intent = new Intent(ConnectionsListActivity.this, LoginActivity.class);
						intent.putParcelableArrayListExtra(Config.Extras.EXTRA_CONNECTIONS_POINTS, wanCps);
						startActivity(intent);
						finish();
					}
					connectionsTypesDialog.dismiss();
				}
			});
			connectionsTypesDialog.show();
		}
		
		private ArrayList<ConnectionPoint> filterConnectionsPoints(ArrayList<ConnectionPoint> cps, boolean isWAN) {
			ArrayList<ConnectionPoint> filteredCps = new ArrayList<ConnectionPoint>();
			for (ConnectionPoint cp : cps) {
				if (isWAN == cp.isWAN()) {
					filteredCps.add(cp);
				}
			}
			return filteredCps;
		}
		
		private void launchConnectionActionsDialog(final ArrayList<ConnectionPoint> cps, final int position) {
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

					if (connectionActionsOptions[0].equals(connectionActionOption)) {
						Intent intent = new Intent(ConnectionsListActivity.this, LoginActivity.class);
						intent.putParcelableArrayListExtra(Config.Extras.EXTRA_CONNECTIONS_POINTS, cps);
						startActivity(intent);
						finish();
					} else if (connectionActionsOptions[1].equals(connectionActionOption)) {
						Intent intent = new Intent(ConnectionsListActivity.this, ConnectionActivity.class);
						intent.putParcelableArrayListExtra(Config.Extras.EXTRA_CONNECTIONS_POINTS, cps);
						intent.putExtra(Config.Extras.EXTRA_CONNECTION_ACTIVITY_STATE, ConnectionActivity.State.VIEW_CONNECTION);
						startActivity(intent);
						finish();
					} else if (connectionActionsOptions[2].equals(connectionActionOption)) {
						launchDeleteConnectionDialog(cps, position);
					}

					dialog.dismiss();
				}
			});
			dialog.show();
		}
		
		private void launchDeleteConnectionDialog(final ArrayList<ConnectionPoint> cps, final int position) {
			Log.d(TAG, TAG + "#launchDeleteConnectionDialog(...) ENTER");
			
			DialogLauncher.launchDeleteConnectionDialog(
					ConnectionsListActivity.this,
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							for (ConnectionPoint cp : cps) {
								mConnectionsDB.deleteConnectionPoint(cp.getName());
							}
							mItems.remove(position);
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

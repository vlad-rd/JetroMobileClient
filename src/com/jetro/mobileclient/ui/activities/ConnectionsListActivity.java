package com.jetro.mobileclient.ui.activities;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
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
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.freerdp.freerdpcore.application.GlobalApp;
import com.freerdp.freerdpcore.sharedobjects.ConnectionPoint;
import com.freerdp.freerdpcore.sharedobjects.utils.Constants;
import com.jetro.mobileclient.R;
import com.jetro.mobileclient.repository.ConnectionsDB;
import com.jetro.mobileclient.ui.HeaderActivity;
import com.jetro.mobileclient.ui.dialogs.DialogLauncher;
import com.jetro.mobileclient.utils.Config;

public class ConnectionsListActivity extends HeaderActivity {

	private static final String TAG = ConnectionsListActivity.class.getSimpleName();
	
	public static boolean IS_FIRST = false;
	
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

		itemsFromDB = ConnectionsDB.getAllSavedConnections();
		
		// If there are none connections, redirect to ConnectionActivity,
		// to add the first connection
		if (itemsFromDB.size() == 0) {
			IS_FIRST = true;
			mHeaderBackButton.setVisibility(View.INVISIBLE);
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

		ArrayList<ViewItem> items;
		LayoutInflater inflater;

		public ConnectionsListAdapter(ArrayList<ViewItem> items) {
			this.items = items;
			inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}

		@Override
		public int getCount() {
			return this.items.size();
		}

		@Override
		public Object getItem(int position) {
			return this.items.get(position);
		}

		@Override
		public long getItemId(int position) {
			return 0;
		}

		@Override
		public View getView(final int position, View convertView, ViewGroup parent) {			
			convertView = inflater.inflate(R.layout.connection_list_item_layout, null);

			TextView name = (TextView) convertView.findViewById(R.id.connectionHostName);
			TextView ip = (TextView) convertView.findViewById(R.id.connectionIp);
			
			name.setText(this.items.get(position).getName());
			final ArrayList<ConnectionPoint> cps = this.items.get(position).getConnectionsPoints();
			ip.setText(Arrays.toString(cps.toArray()));

			convertView.findViewById(R.id.itemBg).setOnClickListener(
					new OnClickListener() {
						@Override
						public void onClick(View v) {
							showClickOptionsDialog(cps, position);
						}
					});

			convertView.findViewById(R.id.itemBg).setOnLongClickListener(
					new OnLongClickListener() {
						@Override
						public boolean onLongClick(View v) {
							showLongClickOptionsDialog(cps, position);
							return true;
						}
					});

			convertView.findViewById(R.id.deleteBtn).setOnClickListener(
					new OnClickListener() {
						@Override
						public void onClick(View v) {
							showDeleteConnectionDialog(position);
						}
					});
			return convertView;
		}
		
		/**
		 * 
		 * @param cps
		 */
		private void showClickOptionsDialog(final ArrayList<ConnectionPoint> cps, final int positionInList) {
			Log.d(TAG, "ConnectionsListActivity.ConnectionsListAdapter#showLongClickOptionsDialog(...) ENTER");

			final Dialog dialog = new Dialog(ConnectionsListActivity.this);
			dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

			dialog.setContentView(R.layout.dialog_connection_types_list);

			ListView dialogButton = (ListView) dialog.findViewById(R.id.list_connection);
			
			Resources resources = getResources();
			String[] connectionsTypes = resources.getStringArray(R.array.connection_types_options);

			dialogButton.setOnItemClickListener(new OnItemClickListener() {
				@Override
				public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
					String item = ((TextView) view).getText().toString();
					if ("I\'m in the office".equals(item)) {
						// Filters only LAN connections points
						ArrayList<ConnectionPoint> lanCps = filterConnectionsPoints(cps, false);
						startConnectionActivityByMode(lanCps, ConnectionActivityMode.Login);
					} else if ("I\'m away from the office".equals(item)) {
						// Filters only WAN connections points
						ArrayList<ConnectionPoint> wanCps = filterConnectionsPoints(cps, true);
						startConnectionActivityByMode(wanCps, ConnectionActivityMode.Login);
					}
					dialog.dismiss();
				}
			});
			dialog.show();
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

		/**
		 * 
		 * @param cps
		 */
		private void showLongClickOptionsDialog(final ArrayList<ConnectionPoint> cps, final int positionInList) {
			Log.d(TAG, "ConnectionsListActivity.ConnectionsListAdapter#showLongClickOptionsDialog(...) ENTER");

			final Dialog dialog = new Dialog(ConnectionsListActivity.this);
			dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

			dialog.setContentView(R.layout.dialog_connection_actions_list);

			ListView dialogButton = (ListView) dialog
					.findViewById(R.id.list_connection);

			dialogButton.setOnItemClickListener(new OnItemClickListener() {
				@Override
				public void onItemClick(AdapterView<?> parent, View view,
						int position, long id) {

					String item = ((TextView) view).getText().toString();

					if (item.equals("Connect")) {
						ImageView backBtn1 = (ImageView) findViewById(R.id.header_back_button);
						backBtn1.setVisibility(View.VISIBLE);
						startConnectionActivityByMode(cps,
								ConnectionActivityMode.Login);
					}
					if (item.equals("Details")) {
						startConnectionActivityByMode(cps,
								ConnectionActivityMode.ViewConnection);
					}
					if (item.equals("Delete")) {
						showDeleteConnectionDialog(positionInList);
					}

					dialog.dismiss();
				}
			});
			dialog.show();
		}

		/**
		 * TODO: COMMECT - HAMODI
		 * 
		 * @param cps
		 * @param mode
		 */
		private void startConnectionActivityByMode(ArrayList<ConnectionPoint> cps, ConnectionActivityMode mode) {
			Log.d(TAG, "ConnectionsListActivity.ConnectionsListAdapter#startConnectionActivityByMode(...) ENTER");

			Intent intent = new Intent(ConnectionsListActivity.this, ConnectionActivity.class);
			intent.putParcelableArrayListExtra(Config.Extras.EXTRA_CONNECTIONS_POINTS, cps);
			intent.putExtra(Constants.MODE, mode.getNumericType());
			startActivity(intent);
			finish();
		}

		/**
		 * TODO:COOMENT - HAMODI
		 * 
		 * @param cps
		 */
		private void showDeleteConnectionDialog(final int position) {
			Log.d(TAG, "ConnectionsListActivity.ConnectionsListAdapter#showDeleteConnectionDialog(...) ENTER");
			
			final ArrayList<ConnectionPoint> cps = items.get(position).getConnectionsPoints();
			
			new AlertDialog.Builder(ConnectionsListActivity.this)
					.setMessage("Are you sure you want to delete this connection?")
					.setCancelable(false)
					.setPositiveButton("Yes",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {
									// deleting connection point from db, by name
									for (ConnectionPoint cp : cps) {
										ConnectionsDB.deleteConnectionPoint(cp.getName());
									}

									// remove from list
									items.remove(position);

									// check for number of connections in db
									if (ConnectionsDB.isDBEmpty()) {
										startConnectionActivityByMode(
												null,
												ConnectionActivityMode.AddConnection);
										finish();
									} else {
										notifyDataSetChanged();
									}
									dialog.dismiss();
								}
							}).setNegativeButton("No", null).show();
		}
	}
}

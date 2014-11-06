package com.jetro.mobileclient.ui;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import com.freerdp.freerdpcore.sharedobjects.Application;
import com.jetro.mobileclient.R;

public class DesktopFragment extends Fragment {

	private static final String TAG = DesktopFragment.class.getSimpleName();
	
	/**
	 * @author ran.h
	 *
	 */
	public interface Listener {
		
		public void onSelectedApplication(Application selectedApp);
		
	}
	
	private ApplicationsGridAdapter appsAdapter;
	private GridView appsGrid;
	private Listener mListener;
	
	@Override
	public void onAttach(Activity activity) {
		Log.d(TAG, "DesktopFragment#onAttach(...) ENTER");
		super.onAttach(activity);
		
		try {
			mListener = (Listener) activity;
		} catch (ClassCastException e) {
			throw new ClassCastException(activity.getClass().getSimpleName()
					+ " must implement " + Listener.class.getSimpleName());
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		Log.d(TAG, "DesktopFragment#onCreateView(...) ENTER");
		
		View rootView = inflater.inflate(R.layout.desktop_fragment_layout, container, false);
		
		Application[] apps = new Application[0];
		appsAdapter = new ApplicationsGridAdapter(apps);
		appsGrid = (GridView) rootView.findViewById(R.id.applicationsGrid);
		appsGrid.setAdapter(appsAdapter);
		appsGrid.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				Application selectedApp = (Application) appsAdapter.getItem(position);
				mListener.onSelectedApplication(selectedApp);
			}
		});
		
		return rootView;
	}

	@Override
	public void onResume() {
		Log.d(TAG, "onResume(...) ENTER");
		super.onResume();
	}
	
	@Override
	public void onDestroy() {
		Log.d(TAG, "onDestroy(...) ENTER");
		super.onDestroy();
	}
	
	public void refreshApplications(Application[] apps) {
		appsAdapter = new ApplicationsGridAdapter(apps);
		appsGrid.setAdapter(appsAdapter);
		appsGrid.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				Application selectedApp = (Application) appsAdapter.getItem(position);
				mListener.onSelectedApplication(selectedApp);
			}
		});
	}
	
	/**
	 * @author ran.h
	 *
	 */
	private class ApplicationsGridAdapter extends BaseAdapter {

		Application[] apps;
		LayoutInflater inflater;

		public ApplicationsGridAdapter(Application[] apps) {
			this.apps = apps;
			inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}

		@Override
		public int getCount() {
			return this.apps.length;
		}

		@Override
		public Object getItem(int position) {
			return this.apps[position];
		}

		@Override
		public long getItemId(int position) {
			return 0;
		}

		@Override
		public View getView(final int position, View convertView, ViewGroup parent) {
			convertView = inflater.inflate(R.layout.grid_item_layout, null);

			((TextView) convertView.findViewById(R.id.applicationName)).setText(apps[position].getName());

			proccessApplicationIcon(
					((ImageView) convertView.findViewById(R.id.applicationIcon)),
					apps[position].getIcon());
			
			return convertView;
		}
		
		private void proccessApplicationIcon(final ImageView iv, final byte[] iconAsBytes) {
			new AsyncTask<Void, Void, Bitmap>() {

				@Override
				protected Bitmap doInBackground(Void... params) {
					Bitmap result = null;
					try {
						result = BitmapFactory.decodeByteArray(iconAsBytes, 0, iconAsBytes.length);
					} catch (Exception e) {
						Log.e(TAG, "ERROR: ", e);
					}
					return result;
				}

				@Override
				protected void onPostExecute(Bitmap result) {
					iv.setImageBitmap(result);
				};
			}.execute();
		}
	}

}
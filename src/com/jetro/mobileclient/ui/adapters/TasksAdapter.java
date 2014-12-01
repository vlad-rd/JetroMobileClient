/**
 * 
 */
package com.jetro.mobileclient.ui.adapters;

import java.util.List;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.jetro.mobileclient.R;
import com.jetro.protocol.Protocols.TsSession.Window;

/**
 * @author ran.h
 *
 */
public class TasksAdapter extends ArrayAdapter<Window> {
	
	public static final int POSITION_NOT_FOUND = -1;
	
	private LayoutInflater mInflater;
	private int mLayoutResourceId;
	private List<Window> mTasks;
	
	/**
	 * @author ran.h
	 *
	 */
	public interface Listener {
		
		public void onTaskDeleted(Window task);
		
	}
	
	private Listener mListener;
	
	private OnClickListener mOnClickListener = new OnClickListener() {
		@Override
		public void onClick(View view) {
			int position = (Integer) view.getTag();
			Window task = mTasks.get(position);
			if (task != null) {
				mListener.onTaskDeleted(task);
			}
		}
	};

	public TasksAdapter(Context context, int resource, List<Window> tasks, TasksAdapter.Listener listener) {
		super(context, resource, tasks);
		
		mInflater = LayoutInflater.from(context);
		mLayoutResourceId = resource;
		mTasks = tasks;
		mListener = listener;
	}
	
	@Override
	public int getPosition(Window task) {
		return mTasks.indexOf(task);
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder holder;
		
		if (convertView == null) {
			holder = new ViewHolder();
			convertView = mInflater.inflate(mLayoutResourceId, parent, false);
			holder.taskIcon = (ImageView) convertView.findViewById(R.id.task_icon);
			holder.taskTitle = (TextView) convertView.findViewById(R.id.task_title);
			holder.deleteButton = (Button) convertView.findViewById(R.id.delete_button);
			holder.deleteButton.setOnClickListener(mOnClickListener);
			convertView.setTag(holder);
		} else {
			holder = (ViewHolder) convertView.getTag();
		}
		
		Window task = getItem(position);
		byte[] iconBytes = task.Icon;
		Bitmap bitmap = BitmapFactory.decodeByteArray(iconBytes , 0, iconBytes.length);
		holder.taskIcon.setImageBitmap(bitmap);
		holder.taskTitle.setText(task.Title);
		holder.deleteButton.setTag(position);
		
		return convertView;
	}

	public void update(Window task) {
		int indexOf = mTasks.indexOf(task);
		if (indexOf != POSITION_NOT_FOUND) {
			Window foundTask = mTasks.get(indexOf);
			foundTask.Title = task.Title;
		}
	}
	
	private class ViewHolder {
		ImageView taskIcon;
		TextView taskTitle;
		Button deleteButton;
	}

}

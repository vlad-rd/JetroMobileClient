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
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.jetro.mobileclient.R;
import com.jetro.protocol.Protocols.TsSession.Window;

/**
 * @author ran.h
 *
 */
public class TasksAdapter extends ArrayAdapter<Window> {
	
	public static final int NOT_FOUND = -1;
	
	private LayoutInflater mInflater;
	private int mLayoutResourceId;
	private List<Window> mTasks;

	public TasksAdapter(Context context, int resource, List<Window> tasks) {
		super(context, resource, tasks);
		
		mInflater = LayoutInflater.from(context);
		mLayoutResourceId = resource;
		mTasks = tasks;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder holder;
		
		if (convertView == null) {
			holder = new ViewHolder();
			convertView = mInflater.inflate(mLayoutResourceId, parent, false);
			holder.icon = (ImageView) convertView.findViewById(R.id.task_icon);
			holder.title = (TextView) convertView.findViewById(R.id.task_title);
			convertView.setTag(holder);
		} else {
			holder = (ViewHolder) convertView.getTag();
		}
		
		Window task = getItem(position);
		byte[] iconBytes = task.Icon;
		Bitmap bitmap = BitmapFactory.decodeByteArray(iconBytes , 0, iconBytes.length);
		holder.icon.setImageBitmap(bitmap);
		holder.title.setText(task.Title);
		
		return convertView;
	}
	
	public void update(Window task) {
		int indexOf = mTasks.indexOf(task);
		if (indexOf != NOT_FOUND) {
			Window foundTask = mTasks.get(indexOf);
			foundTask.Title = task.Title;
		}
	}
	
	private class ViewHolder {
		ImageView icon;
		TextView title;
	}

}

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

import com.freerdp.freerdpcore.sharedobjects.Task;
import com.jetro.mobileclient.R;

/**
 * @author ran.h
 *
 */
public class TasksAdapter extends ArrayAdapter<Task> {
	
	private LayoutInflater mInflater;
	private int mLayoutResourceId;

	public TasksAdapter(Context context, int resource, List<Task> tasks) {
		super(context, resource, tasks);
		
		mInflater = LayoutInflater.from(context);
		mLayoutResourceId = resource;
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
		
		Task task = getItem(position);
		byte[] iconBytes = task.getIcon();
		Bitmap bitmap = BitmapFactory.decodeByteArray(iconBytes , 0, iconBytes.length);
		holder.icon.setImageBitmap(bitmap);
		holder.title.setText(task.getTitle());
		
		return convertView;
	}
	
	private class ViewHolder {
		ImageView icon;
		TextView title;
	}

}

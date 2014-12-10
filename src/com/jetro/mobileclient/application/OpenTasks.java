/**
 * 
 */
package com.jetro.mobileclient.application;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.jetro.protocol.Protocols.TsSession.Window;

/**
 * @author ran.h
 *
 */
public class OpenTasks {
	
	private final Map<String, Set<Window>> activeTasksMap = new HashMap<String, Set<Window>>();
	
	private OpenTasks() {
	}
	
	private static final class SingletonHolder {
		private static final OpenTasks INSTANCE = new OpenTasks();
	}
	
	public static OpenTasks getInstance() {
		return SingletonHolder.INSTANCE;
	}
	
	/**
	 * Adds an active task to application.
	 * 
	 * @param task - Task to add
	 * @return true if the application just got active (first active task added), otherwise false
	 */
	public boolean add(Window task) {
		Set<Window> activeTasks = activeTasksMap.get(task.AppID);
		if (activeTasks == null) {
			activeTasks = new HashSet<Window>();
			activeTasksMap.put(task.AppID, activeTasks);
		}
		boolean isAdded = activeTasks.add(task);
		return isAdded && activeTasks.size() == 1;
	}

	public Map<String, Set<Window>> add(Window[] tasks) {
		for (Window task : tasks) {
			add(task);
		}
		return activeTasksMap;
	}
	
	/**
	 * Removes an active task from application.
	 * 
	 * @param task - Task to remove
	 * @return true if the application just got inactive (last active task removed), otherwise false
	 */
	public boolean remove(Window task) {
		Set<Window> activeTasks = activeTasksMap.get(task.AppID);
		if (activeTasks != null) {
			boolean isRemoved = activeTasks.remove(task);
			return isRemoved && activeTasks.size() == 0;
		}
		return false;
	}
	
	public Map<String, Set<Window>> remove(Window[] tasks) {
		for (Window task : tasks) {
			remove(task);
		}
		return activeTasksMap;
	}
	
	public void clear() {
		activeTasksMap.clear();
	}
	
	public boolean isAppHasTasks(String appId) {
		Set<Window> activeTasks = activeTasksMap.get(appId);
		return activeTasks != null && !activeTasks.isEmpty();
	}

}

/**
 * 
 */
package com.jetro.mobileclient.utils;

/**
 * @author ran.h
 *
 */
public class Params<BaseMsg, Integer, IMessageSubscriber> {
	
	public BaseMsg msg;
	public int timeout;
	public IMessageSubscriber messageSubscriber;
	
	public Params(BaseMsg msg, int timeout, IMessageSubscriber messageSubscriber) {
		this.msg = msg;
		this.timeout = timeout;
		this.messageSubscriber = messageSubscriber;
	}
	
}

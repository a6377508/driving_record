package com.autonavi.xmgd.utility;

import com.saiwei.recorder.CarRecorder;

import android.util.Log;


/**
 * 对log 方法进行封装 
 * 主要：在 DEBUG 为false的时候，不打log
 * 
 * @author wei.chen
 *
 */
public class Logutil {
	
	public static void i(String tag,String msg){
		if(CarRecorder.DEBUG) Log.i(tag, msg);
	}
	
	public static void d(String tag,String msg){
		if(CarRecorder.DEBUG) Log.d(tag, msg);
	}
	
	public static void e(String tag,String msg){
		if(CarRecorder.DEBUG) Log.e(tag, msg);
	}
	
	public static void e(String tag,String msg,Throwable tr){
		if(CarRecorder.DEBUG) Log.e(tag, msg, tr);
	}
}

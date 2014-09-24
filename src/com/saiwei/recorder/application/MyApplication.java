package com.saiwei.recorder.application;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.autonavi.xmgd.utility.Storage;
import com.autonavi.xmgd.utility.Util;

/**
 * 全局变量
 * @author wei.chen
 *
 */
public class MyApplication extends Application {

	private static final String TAG = "chenwei.MyApplication";
	
	public static SharedPreferences cache_recoder_video = null;
	
	public static final String PREF_CAR_RECORDER = "navigator_recoder";

	@Override
	public void onCreate() {
		super.onCreate();		
		Log.i(TAG, "onCreate()");

		if(cache_recoder_video == null){
			cache_recoder_video = getSharedPreferences(PREF_CAR_RECORDER, Context.MODE_PRIVATE);
		}
		
		//创建文件夹
		Util.createFolder(Storage.DEFAULT_DIRECTORY);
		
		//初始化视频设置属性
		initRecorderSetting(cache_recoder_video);
	}
	
	/**
	 * 初始化 行车记录仪的设置属性
	 */
	private  void initRecorderSetting(SharedPreferences preferences){
		if(preferences == null){
			return;
		}
		boolean  isContain ;
		
		//后台录像    默认:否(不开启)
		isContain = preferences.contains(Recorder_Global.SHARE_PREFERENCE_BACKGROUND_RECORD);
		if(!isContain){
			preferences.edit()
				.putInt(Recorder_Global.SHARE_PREFERENCE_BACKGROUND_RECORD,
						Recorder_Global.DEFAULT_VIDEO_IS_BACKGROUND_RECORD).commit();
		}
		
		//保存视频个数  默认:3个
		isContain = preferences.contains(Recorder_Global.SHARE_PREFERENCE_SAVE_FILE_NUM);
		if(!isContain){
			preferences.edit()
				.putInt(Recorder_Global.SHARE_PREFERENCE_SAVE_FILE_NUM,
						Recorder_Global.DEFAULT_VIDEO_SAVE_FILE_NUM).commit();
		}
		
		//视频质量    【默认:正常】
		isContain = preferences.contains(Recorder_Global.SHARE_PREFERENCE_VIDEO_QUALITY);
		if(!isContain){
			preferences.edit()
				.putInt(Recorder_Global.SHARE_PREFERENCE_VIDEO_QUALITY,
						Recorder_Global.DEFAULT_VIDEO_QUALITY).commit();
		}
		
		//视频录制时长    【默认:10分钟】
		isContain = preferences.contains(Recorder_Global.SHARE_PREFERENCE_VIDEO_RECORD_RATE);
		if(!isContain){
			preferences.edit()
				.putInt(Recorder_Global.SHARE_PREFERENCE_VIDEO_RECORD_RATE, 
						Recorder_Global.DEFAULT_VIDEO_RECORD_RATE).commit();
		}
		
		//视频文件路径  【默认：内卡路径】
		isContain = preferences.contains(Recorder_Global.SHARE_PREFERENCE_VIDEO_FILE_PATH);
		if(!isContain){
			preferences.edit()
				.putInt(Recorder_Global.SHARE_PREFERENCE_VIDEO_FILE_PATH, 
						Recorder_Global.DEFAULT_VIDEO_FILE_PATH).commit();
		}
		
		//视频文件具体路径  【默认：/sdcard/DCIM/Nav】
		isContain = preferences.contains(Recorder_Global.SHARE_PREFERENCE_VIDEO_FILE_PATH_DETAIL);
		if(!isContain){
			preferences.edit()
				.putString(Recorder_Global.SHARE_PREFERENCE_VIDEO_FILE_PATH_DETAIL, 
						Recorder_Global.DEFAULT_VIDEO_FILE_PATH_DETAIL).commit();
		}
	}
	
	/**
	 * 设置默认的文件路径
	 * @param preferences
	 */
	public static void setDefaultFilePath(SharedPreferences preferences){
		if(preferences != null){
			preferences.edit()
				.putInt(Recorder_Global.SHARE_PREFERENCE_VIDEO_FILE_PATH, 
					Recorder_Global.DEFAULT_VIDEO_FILE_PATH).commit();
			
			preferences.edit()
				.putString(Recorder_Global.SHARE_PREFERENCE_VIDEO_FILE_PATH_DETAIL, 
					Recorder_Global.DEFAULT_VIDEO_FILE_PATH_DETAIL).commit();
			
			Recorder_Global.mCurFilePath = Recorder_Global.DEFAULT_VIDEO_FILE_PATH_DETAIL;
		}
	}
	
	
	
	@Override
	public void onLowMemory() {
		super.onLowMemory();
		
		Log.i(TAG, "onLowMemory()");
	}

	@Override
	public void onTerminate() {
		super.onTerminate();
		
		Log.i(TAG, "onTerminate()");
	}

	@Override
	public void onTrimMemory(int level) {
		super.onTrimMemory(level);
		
		Log.i(TAG, "onTrimMemory()");
	}
}

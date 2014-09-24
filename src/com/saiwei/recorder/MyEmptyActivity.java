package com.saiwei.recorder;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.WindowManager;

import com.saiwei.recorder.application.Recorder_Global;

/**
 * 空的Activity,主要用于控制生命周期
 * 
 * @author wei.chen
 */
public class MyEmptyActivity extends Activity {

	private static final String TAG = "chenwei.MyEmptyActivity";
	
	private Intent intent;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
//		setContentView(R.layout.my_empty_activity);

		Log.i(TAG, "onCreate()");

		// 保持屏幕常亮
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
	
//		if(Recorder_Global.isRecording){
//		    BackgroundVideoRecorderLogic.getInstance(getApplication(), getApplicationContext(),THIS(),getResources(),mHandler)
//		        .onStartCommand(Recorder_Global.PLUGIN_ACTION_NAVIGATOR_RECORD_WINDOW_MAXIMIZE);
//		} else {
//		 // 保持屏幕常亮
//	        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
//
//	        mBackgroundVideoRecorderLogic = BackgroundVideoRecorderLogic.getInstance(
//	                getApplication(),
//	                getApplicationContext(),
//	                THIS(),
//	                getResources(),
//	                mHandler);
//	        mBackgroundVideoRecorderLogic.gotoCarRecordView();
//		}
		
		
		if(Recorder_Global.isRecording){
			updateRecorderWindow(Recorder_Global.PLUGIN_ACTION_NAVIGATOR_RECORD_WINDOW_MAXIMIZE);
		} else {
			startRecorderService();
		}
	}

	@Override
	protected void onRestart() {
		super.onRestart();

		Log.i(TAG, "onRestart()");
		
		if(Recorder_Global.isRecording){			
			updateRecorderWindow(
					Recorder_Global.PLUGIN_ACTION_NAVIGATOR_RECORD_WINDOW_MAXIMIZE);
		} else {
			startRecorderService();
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		Log.i(TAG, "onResume()");
		
		//TODO 处理偶发BUG
	}

	@Override
	protected void onPause() {
		super.onPause();

		Log.i(TAG, "onPause() Recorder_Global.isRecording="
				+ Recorder_Global.isRecording);
		
		if (Recorder_Global.isRecording) {
			if (Recorder_Global.isRecordBackground) {
				updateRecorderWindow(
						Recorder_Global.PLUGIN_ACTION_NAVIGATOR_RECORD_WINDOW_MINIMIZE);
			} else {
				stopRecorderService();
			}
		} else {
			stopRecorderService();
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		Log.i(TAG, "onDestroy() isFinishing()="+isFinishing());
		if (isFinishing()) {
			if(!Recorder_Global.isRecordBackground){
				// 关闭录像
				stopService(new Intent(
						Recorder_Global.PLUGIN_ACTION_NAVIGATOR_RECORD));
			}
		}
	}
	
	/**
	 * 更新视频窗口
	 * @param action
	 */
	private void updateRecorderWindow(String action){
		
		if(TextUtils.isEmpty(action)) return ;
		
		intent = new Intent(action);
		startService(intent);
	}
	
	/**
	 * 打开行车记录仪service
	 */
	private void startRecorderService(){
		intent = new Intent(
				Recorder_Global.PLUGIN_ACTION_NAVIGATOR_RECORD);
		startService(intent);
	}
	
	/**
	 * 关闭行车记录仪service
	 */
	private void stopRecorderService(){
		intent = new Intent(
				Recorder_Global.PLUGIN_ACTION_NAVIGATOR_RECORD);
		stopService(intent);
	}
}
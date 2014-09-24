package com.autonavi;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import com.autonavi.xmgd.utility.Util;
import com.saiwei.recorder.MyEmptyActivity;
import com.saiwei.recorder.R;
import com.saiwei.recorder.VideoFileManager;
import com.saiwei.recorder.VideoSetting;

/**
 * 模拟导航Map
 * @author wei.chen
 *
 */
public class Map extends Activity implements OnClickListener {

	private static final String TAG = "chenwei.Map";
	private Context mContext;
	private Button   mBtRecorderSetting;
	/** 视频文件夹 */
	private Button mBtVideoFolder;
	/**
	 * 显示软件版本号
	 */
	private TextView mVersion;
	private android.widget.ImageButton mBtNaviRecorder;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.map);
		
		Log.i(TAG, "onCreate()");
		
		mContext = this;
		init();
	}
	
	private void init() {
		mBtNaviRecorder = (ImageButton) this.findViewById(R.id.bt_navi_recorder);
		mBtNaviRecorder.setOnClickListener(this);
		mBtVideoFolder = (Button) this.findViewById(R.id.bt_video_folder);
		mBtVideoFolder.setOnClickListener(this);
		mBtRecorderSetting = (Button) this.findViewById(R.id.bt_recorder_setting);
		mBtRecorderSetting.setOnClickListener(this);
	
		mVersion = (TextView) this.findViewById(R.id.tv_show_version);
		
		mVersion.setText("版本号： "+Util.getVersion(mContext)+" !!!");

	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		Log.i(TAG, "onDestroy()");
	}
	
	Intent mIntent;
	
	@Override
	public void onClick(View v)  {
		switch (v.getId()) {
		//进入行车记录仪
		case R.id.bt_navi_recorder:
			mIntent = new Intent(Map.this, MyEmptyActivity.class);
			startActivity(mIntent);
			break;
		
		//进入视频文件夹
		case R.id.bt_video_folder:
		    mIntent = new Intent(Map.this,VideoFileManager.class);
            startActivity(mIntent);
		    break;
		
		//进入录像设置界面
		case R.id.bt_recorder_setting:
			mIntent = new Intent(Map.this,VideoSetting.class);
			startActivity(mIntent);
			break;
		default:
			break;
		}
	}	
}

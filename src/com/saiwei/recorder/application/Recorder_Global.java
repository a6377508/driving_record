package com.saiwei.recorder.application;

import com.autonavi.xmgd.utility.Storage;


/**
 * 全局变量
 * 
 * @author wei.chen
 *
 */
public class Recorder_Global {
	
	public static final boolean DEBUG = true;
	
	
	
	/**
	 * 后台录像
	 * 0  表示    不开启
	 * 1 表示    开启
	 */
	public static final String SHARE_PREFERENCE_BACKGROUND_RECORD = "background_record";
	
	/**
	 * 保存视频个数
	 */
	public static final String SHARE_PREFERENCE_SAVE_FILE_NUM = "save_file_num";
	
	/**
	 * 视频质量
	 */
	public static final String SHARE_PREFERENCE_VIDEO_QUALITY = "video_quality";
	
	/**
	 * 自定义：  quality_id  例如： CamcorderProfile.QUALITY_HIGH ， CamcorderProfile.QUALITY_720P
	 */
	public static final String SHARE_PREFERENCE_VIDEO_QUALITY_ID = "video_quality_id";
	
	/**
	 * 视频录制时长
	 */
	public static final String SHARE_PREFERENCE_VIDEO_RECORD_RATE = "record_rate";
	
	/**
	 * 视频文件路径  【内卡路径，外卡路径】
	 */
	public static final String SHARE_PREFERENCE_VIDEO_FILE_PATH = "video_file_path";
	
	/**
	 * 视频文件路径[具体路径]
	 */
	public static final String SHARE_PREFERENCE_VIDEO_FILE_PATH_DETAIL = "video_file_path_detail";
	
	//-----[SharedPreferences]--[end]------------------------
	
	
	
	//--------设置属性的默认值--【start】---------------------------

	/**
	 * 后台录像    默认:否(不开启)
	 */
	public static final int DEFAULT_VIDEO_IS_BACKGROUND_RECORD = 0;
	
	/**
	 * 保存视频个数  默认:3个
	 */
	public static final int DEFAULT_VIDEO_SAVE_FILE_NUM = 3;
	
	/**
	 * 视频质量    默认:正常
	 */
	public static final int DEFAULT_VIDEO_QUALITY = 0;
	
	/**
	 * 自定义：  quality_id  例如： CamcorderProfile.QUALITY_HIGH ， CamcorderProfile.QUALITY_720P
	 * 默认 -1
	 */
	public static final int DEFAULT_VIDEO_QUALITY_ID = -1;
	
	/**
	 * 视频录制时长   默认:10分钟
	 */
	public static final int DEFAULT_VIDEO_RECORD_RATE = 1;
	
	/**
	 * 视频文件路径   默认:内卡路径
	 */
	public static final int DEFAULT_VIDEO_FILE_PATH = 0;
	
	/**
	 * 视频文件路径  [具体路径]  默认  /sdcard/DCIM/Navi/   文件夹下
	 */
	public static final String DEFAULT_VIDEO_FILE_PATH_DETAIL = Storage.DEFAULT_DIRECTORY;
	
	//--------设置属性的默认值--【end】---------------------------
	
	
	/**
	 * 是否正在录像中
	 */
	public static boolean isRecording = false;
	
	/**
	 * 是否处于后台录像中
	 */
	public static boolean isRecordBackground = false;
	
	/**
	 *  行车记录仪service [service action]
	 */
	public static final String PLUGIN_ACTION_NAVIGATOR_MYEMPTYACTIVITY
			= "com.saiwei.action.start.emptyactivity";
	
	
	/**
	 *  行车记录仪service [service action]
	 */
	public static final String PLUGIN_ACTION_NAVIGATOR_RECORD
			= "com.saiwei.action.recorder";
	
	/**
	 *  行车记录仪service : 自己停止自己
	 */
	public static final String PLUGIN_ACTION_NAVIGATOR_RECORD_STOPSELF
			= "com.saiwei.action.recorder.stopself";

	/**
	 * 更新window窗口：  窗口最小化
	 */
	public static final String PLUGIN_ACTION_NAVIGATOR_RECORD_WINDOW_MINIMIZE
				= "com.saiwei.action.recorder..window.minimize";  
	
	/**
	 * 更新window窗口：  窗口最大化
	 */
	public static final String PLUGIN_ACTION_NAVIGATOR_RECORD_WINDOW_MAXIMIZE
				= "com.saiwei.action.recorder..window.maximize";  
	
	/**
	 * 更新window窗口：  自定义窗口大小
	 */
	public static final String PLUGIN_ACTION_NAVIGATOR_RECORD_WINDOW_CUSTOM_SIZE
				= "com.saiwei.action.recorder..window.custom_size";  
	
	/**
	 * 保存记录条数
	 */
	public static final int SAVE_FILE_NUM = 3;
	
	/**
	 * 当前文件夹
	 */
	public static String mCurFilePath = DEFAULT_VIDEO_FILE_PATH_DETAIL;
}

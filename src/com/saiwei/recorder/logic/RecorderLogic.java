package com.saiwei.recorder.logic;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Set;

import android.app.Application;
import android.media.CamcorderProfile;
import android.util.Log;

import com.autonavi.xmgd.utility.FileComparator;
import com.autonavi.xmgd.utility.Logutil;
import com.autonavi.xmgd.utility.Storage;
import com.autonavi.xmgd.utility.Util;
import com.saiwei.recorder.CarRecorder;
import com.saiwei.recorder.MyMediaRecorder;
import com.saiwei.recorder.R;
import com.saiwei.recorder.application.MyApplication;
import com.saiwei.recorder.application.Recorder_Global;


/**
 * 单件， 处理业务逻辑 
 * 
 * @author wei.chen
 *
 */
public class RecorderLogic {
	
	private static final String TAG = "chenwei.RecorderLogic";
	
	private static boolean DEBUG = true;
	
	private static final int QUALITY_NORMAL = 0;		//正常
	private static final int QUALITY_FINE = 1;			//精细
	private static final int QUALITY_HYPERFINE = 2; 	//超精细

	private Application mApplication;
	
	private static RecorderLogic instance = null;
	
	private MyMediaRecorder mMediaRecorder;
	
	public static RecorderLogic getInstance(Application application) {
		
		if (instance == null) {
			instance = new RecorderLogic(application);
		}
		return instance;
	}
	
	
	/**
	 * 构造函数
	 * @param application
	 */
	private RecorderLogic(Application application){
		
		Logutil.i(TAG, "RecorderLogic() 构造函数");
		
		if(application == null){
			return;
		}
		mApplication = application;
		
		deleteTempFile();
	}

	/**
	 * 单件的释放
	 */
	public static void freeInstance(){
		if(instance!=null){
			instance.onDestroy();
			instance= null;
		}
	}
	
	/**
	 * 单件销毁
	 */
	private void onDestroy(){
		if(instance != null){
			checkVideoFiles();
			instance = null;
		}
	}
	
	/**
	 * 删除头像的缓存文件
	 */
	private void deleteTempFile(){
		File dir  = new File(Storage.DEFAULT_DIRECTORY);
		if(!dir.exists()){
			return;
		}
		
		File[] files = dir.listFiles();
		if(files!=null){
			for(int i=0;i<files.length;i++){
				if(files[i].getName().startsWith("navi_")){
					files[i].delete();
				}
			}
		}
	}
	
	private ArrayList<File> mListFiles = null;
	
	/**
	 * 检查DCIM/Camera/Navi里总共有几个视频文件， 只保留定义的保存个数，把多余的删除了
	 */
	public void checkVideoFiles(){
		new Thread(new Runnable() {

			@Override
			public void run() {
				
				File dir = new File(Recorder_Global.mCurFilePath);
				if (!dir.exists()) {
					return;
				}
							
				File[] files = dir.listFiles();
				
				mListFiles = new ArrayList<File>(Arrays.asList(files));
				
				//按时间排序
				Collections.sort(mListFiles, new FileComparator());	
				
				if(mListFiles.size() > Recorder_Global.SAVE_FILE_NUM){
					for(int i=Recorder_Global.SAVE_FILE_NUM;i<mListFiles.size();i++){
						File f = mListFiles.get(i);
						if(!f.exists()){
							continue;
						}
						f.delete();
					}
				}				
			}
		}).start();
	}
	
	/**
	 * 获取录像周期
	 * @return
	 */
	public int getVideoRate(){
		
    	int value = MyApplication.cache_recoder_video
    			.getInt(Recorder_Global.SHARE_PREFERENCE_VIDEO_RECORD_RATE, 
    					Recorder_Global.DEFAULT_VIDEO_RECORD_RATE);
    	String[] strs = mApplication.getResources().getStringArray(R.array.video_record_rate);
    	String str = strs[value];
    	int i = Integer.parseInt(str);
    	
    	Logutil.i(TAG, "录像周期  i="+i);
    	int lVideoRate = i * 60 * 1000;	
    	
    	if(CarRecorder.DEBUG){
    		return (int)(0.5 * 60 * 1000);
    	}
    	
    	return lVideoRate;
	}
	
	/**
	 * 获取视频品质 [包括： 视频分辨率 和 录制质量]
	 * @return
	 */
	public CamcorderProfile getVideoProfile(int[] screensize){
		
		int   tempQualityValue ,tempQualityId = -1;
		
		//录制质量
		tempQualityValue = MyApplication.cache_recoder_video
						.getInt(Recorder_Global.SHARE_PREFERENCE_VIDEO_QUALITY, 
		    					Recorder_Global.DEFAULT_VIDEO_QUALITY);
		
		CamcorderProfile profile = null;
		
		//quality_id 
		tempQualityId =  MyApplication.cache_recoder_video
						.getInt(Recorder_Global.SHARE_PREFERENCE_VIDEO_QUALITY_ID, 
								Recorder_Global.DEFAULT_VIDEO_QUALITY_ID);
		
		
		Log.i(TAG, "tempQualityId="+tempQualityId);
		
		if(tempQualityId == -1){
			tempQualityId = getAdaptScreen(screensize);
			MyApplication.cache_recoder_video.edit()
					.putInt(Recorder_Global.SHARE_PREFERENCE_VIDEO_QUALITY_ID, tempQualityId).commit();
		} 
		
		try {
			profile = CamcorderProfile.get(tempQualityId);
		} catch (Exception e) {
			profile = CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH);
		}
//		Log.i(TAG, "screensize[] = "+screensize[0]+","+screensize[1]);
//		Log.i(TAG, "profile.videoFrameHeight="+profile.videoFrameHeight+","+profile.videoFrameWidth);
		
		editProfile(profile, tempQualityValue);
		
		return profile;
	}
	
	/**
	 * 获取视频文件具体路径
	 * @return
	 */
	public String getVideoFilePathDetail(){
		String filepath = MyApplication.cache_recoder_video
			.getString(Recorder_Global.SHARE_PREFERENCE_VIDEO_FILE_PATH_DETAIL, 
				Recorder_Global.DEFAULT_VIDEO_FILE_PATH_DETAIL);
		//创建文件夹
		Util.createFolder(filepath);
		if(Util.isFileExist(filepath)){
			return filepath;
		} else {		
			MyApplication.setDefaultFilePath(MyApplication.cache_recoder_video);
			return Recorder_Global.DEFAULT_VIDEO_FILE_PATH_DETAIL;
		}
	}
	
	private HashMap<Float, Integer> profiles = new HashMap<Float, Integer>();
	
	/**
	 * 获取自适应屏幕的 QUALITY id
	 * @param screensize
	 * @return
	 */
	private int getAdaptScreen(int[] screensize){
		
		if(screensize == null){
			return CamcorderProfile.QUALITY_HIGH;
		}
		profiles.clear();
		CamcorderProfile profile = null;
		String profile_frame = "";
		float temp = 0;
		
		for(int i=CamcorderProfile.QUALITY_LOW;i<CamcorderProfile.QUALITY_QVGA;i++){
			try {
				profile = CamcorderProfile.get(i);								
				profile_frame = profile.videoFrameHeight+","+profile.videoFrameWidth;
//				Log.i(TAG, "i="+i+" ,profile_frame="+profile_frame);
//				Log.i(TAG, "profile.videoBitRate="+profile.videoBitRate);
				temp = profile.videoFrameHeight*profile.videoFrameWidth;
				
				if(temp>(screensize[0]*screensize[1])){
					continue;
				}
				profiles.put(temp,i);
			} catch (Exception e) {
				
			}
		}
		
		Set<Float> keyset = profiles.keySet();
		Object[] ff = keyset.toArray();
		
		//默认从小到大
		Arrays.sort(ff);
		Float tt= (Float) ff[ff.length-1];
		int quality = profiles.get(tt);
//		Log.i(TAG, "quality="+quality);
		return quality;
	}
	
	
	/**
	 * 调整视频码率 
	 * @param profile  
	 * @param factor
	 */
	private void changeVideoBitRate(CamcorderProfile profile,double factor){
		int temp = profile.videoBitRate;
		profile.videoBitRate = (int) (temp*factor);
	}

	private void editProfile(CamcorderProfile profile  ,int videoQuality){
		
		if(profile == null){
			return ;
		}
		
		switch (videoQuality) {
		case QUALITY_NORMAL:	//正常
			changeVideoBitRate(profile,0.5);
			break;
		case QUALITY_FINE:		//精细
			changeVideoBitRate(profile,0.75);
			break;
		case QUALITY_HYPERFINE:		//超精细
			changeVideoBitRate(profile,1);
			break;

		default:
			break;
		}
	}
	
	/**
	 * 创建文件名
	 * @param dateTaken
	 * @return
	 */
	public String createFileName(long dateTaken) {
	
        Date date = new Date(dateTaken);
        SimpleDateFormat dateFormat = new SimpleDateFormat(
                mApplication.getString(R.string.video_file_name_format));

        return dateFormat.format(date);
    }
}

package com.saiwei.recorder;

import java.util.Iterator;
import java.util.List;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Camera;
import android.hardware.Camera.ErrorCallback;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.Size;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.media.MediaRecorder.OnErrorListener;
import android.media.MediaRecorder.OnInfoListener;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.SystemClock;
import android.provider.Settings;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.RemoteViews;
import android.widget.TextView;
import android.widget.Toast;

import com.autonavi.xmgd.utility.Logutil;
import com.autonavi.xmgd.utility.Storage;
import com.autonavi.xmgd.utility.Util;
import com.saiwei.recorder.application.MyApplication;
import com.saiwei.recorder.application.Recorder_Global;
import com.saiwei.recorder.logic.RecorderLogic;

/**
 * 视频录像Service
 * 
 * @author wei.chen
 */
public class VideoRecordService extends Service 
		implements 	SurfaceHolder.Callback ,
					OnErrorListener ,
					OnInfoListener,
					OnClickListener,
					OnCheckedChangeListener,
					ErrorCallback
		{

	private static final String TAG = "chenwei.VideoRecordService";

	private Context mContext;

	private WindowManager windowManager;
	private SurfaceView surfaceView;
	private SurfaceHolder mSurfaceHolder = null;
	private Camera mCameraDevice = null;
	
	private RecorderLogic mRecorderLogic;

	private boolean mPreviewing = false; // True if preview is started.

	private static final int MIN_HEIGHT = 1;

	private WindowManager.LayoutParams mLayoutParams;

	private View view;

	/** 开始/结束 录像 */
	private Button mBtRecord;

	/** 进入设置界面 */
	private Button mBtSetting;
	/** 进入文件管理器 */
	private Button mBtFileManager;
	
	private CheckBox mCheckBoxIsBackground;
	
	private Button mBtOpenGps;

	/** 当前录制时长 */
	private TextView mCurRecordTime;
	
	/** 按钮点击的间隔时间 */
	private static final long SHUTTER_BUTTON_TIMEOUT = 500L; // 500ms
	
	private static final int CLEAR_SCREEN_DELAY = 4;
	private static final int UPDATE_RECORD_TIME = 5;
	private static final int REMOVE_UPDATE_RECORD_TIME = 6;
    private static final int ENABLE_SHUTTER_BUTTON = 7;
    private static final int SHOW_ERROR = 8;
    
    private Parameters mParameters;
	private int mDesiredPreviewWidth;
	private int mDesiredPreviewHeight;
	
	private boolean mOpenCameraFail = false;
    private boolean mCameraDisabled = false;
    
    private int mDisplayRotation;
    
    /** 当前视频的文件名*/
    private String mVideoFilename ;
    
    /** 屏幕高度*/
    private int screenHight;
    /** 屏幕宽度*/
    private int screenWidth;
    
    private Intent intent = null;
    
	private Handler mHandler = new Handler(){

		@Override
		public void handleMessage(Message msg) {
			
			switch (msg.what) {
			case CLEAR_SCREEN_DELAY:
//				getWindow().clearFlags(
//                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
				break;
			case ENABLE_SHUTTER_BUTTON:
				mBtRecord.setEnabled(true);
				break;
	 		case UPDATE_RECORD_TIME:
				updateRecordingTime();
				break;
			case SHOW_ERROR:
				Toast.makeText(mContext, "需要对该机器进行特殊处理!!!", Toast.LENGTH_SHORT ).show();
				break;
			default:
				break;
			}
		}
	};
	
	@Override
	public void onCreate() {

		Log.i(TAG, "onCreate()");

		mContext = this;

		mRecorderLogic = RecorderLogic.getInstance(getApplication());
		
		Thread startPreviewThread = new Thread(new Runnable() {
            @Override
			public void run() {
                try {
                	mOpenCameraFail = false;
                	mCameraDevice = CameraHolder.instance().open(0);
        			readVideoPreferences();
        			startPreview();
                }  catch (Exception e) {
                	mOpenCameraFail = true;
				}
            }
        });
        startPreviewThread.start();

		// Start foreground service to avoid unexpected kill
		// API : 16
		// Notification notification = new Notification.Builder(this)
		// .setContentTitle("Background Video Recorder")
		// .setContentText("")
		// .setSmallIcon(R.drawable.ic_launcher)
		// .build();

		// Create new SurfaceView, set its size to 1x1, move it to the top left
		// corner and set this service as a callback
		windowManager = (WindowManager) this
				.getSystemService(Context.WINDOW_SERVICE);

		initView();

		/*
		 * LayoutParams layoutParams = new WindowManager.LayoutParams(720, 1080,
		 * WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY,
		 * WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
		 * PixelFormat.TRANSLUCENT);
		 */

		// TODO: 获取屏幕分辨率，同时注意 需要扣除 status bar height 1130
		
		int[] screensize = getScreenSize();
		int statusbarheight = Util.getStatusBarHeight(mContext);
		
		mLayoutParams = new WindowManager.LayoutParams(screensize[0], screensize[1]-statusbarheight, 2007, 8, -3);

		// Make sure preview is started.
        try {
            startPreviewThread.join();
            if (mOpenCameraFail) {
            	Log.i(TAG, "mOpenCameraFail = true");
            } 
        } catch (InterruptedException ex) {
        	
        }
		
		// layoutParams.gravity = Gravity.LEFT | Gravity.TOP;
		windowManager.addView(view, mLayoutParams);
	}

	/**
	 * 初始化界面
	 */
	private void initView() {
		
		screenWidth = getScreenSize()[0];
		screenHight = getScreenSize()[1];
		
		LayoutInflater mInflater = LayoutInflater.from(this);
		view = mInflater.inflate(R.layout.activity_main, null);
		surfaceView = (SurfaceView) view.findViewById(R.id.camera_preview);
		surfaceView.getHolder().addCallback(this);

		mBtRecord = (Button) view.findViewById(R.id.bt_record);
		mBtRecord.setOnClickListener(this);

		mBtSetting = (Button) view.findViewById(R.id.bt_video_setting);
		mBtSetting.setOnClickListener(this);

		mBtFileManager = (Button) view.findViewById(R.id.bt_video_show);
		mBtFileManager.setOnClickListener(this);

		mCheckBoxIsBackground = (CheckBox) view.findViewById(R.id.checkbox_is_background);
		mCheckBoxIsBackground.setOnCheckedChangeListener(this);
		
		initCheckBox();
		
		// mBtVideoBackground = (Button)
		// view.findViewById(R.id.bt_video_background);
		// mBtVideoBackground.setOnClickListener(this);

		mCurRecordTime = (TextView) view.findViewById(R.id.tv_show_time);
		
		mBtOpenGps = (Button) view.findViewById(R.id.bt_open_gps);
		mBtOpenGps.setOnClickListener(this);
	}

	/**
	 * 初始化后台录像属性
	 */
	private void initCheckBox(){
		int value = MyApplication.cache_recoder_video
				.getInt(Recorder_Global.SHARE_PREFERENCE_BACKGROUND_RECORD, 
						Recorder_Global.DEFAULT_VIDEO_IS_BACKGROUND_RECORD);
		
		Log.i(TAG, "initCheckBox() value="+value);
		
		if(value != Recorder_Global.DEFAULT_VIDEO_IS_BACKGROUND_RECORD){
			mCheckBoxIsBackground.setChecked(true);
			Recorder_Global.isRecordBackground = true;
		}
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.i(TAG, "onStartCommand()");

		handleIntent(intent);
		return super.onStartCommand(intent, flags, startId);
	}

	/**
	 * 处理 intent
	 * 
	 * @param intent
	 */
	private void handleIntent(Intent intent) {
		if (intent != null) {
			String action = intent.getAction();

			if (TextUtils.isEmpty(action)) return;
			
			Log.i(TAG, "handleIntent()  action="+action);

			if (action
					.equals(Recorder_Global.PLUGIN_ACTION_NAVIGATOR_RECORD_WINDOW_MINIMIZE)) {
				showNewNotification();
				updateWindow(MIN_HEIGHT, MIN_HEIGHT);
			} else if (action
					.equals(Recorder_Global.PLUGIN_ACTION_NAVIGATOR_RECORD_WINDOW_MAXIMIZE)) {
				removeNewNotification();
				int[] screensize = getScreenSize();
				int statusbarheight = Util.getStatusBarHeight(mContext);
				updateWindow(screensize[0], screensize[1]-statusbarheight);
			} else if (action
					.equals(Recorder_Global.PLUGIN_ACTION_NAVIGATOR_RECORD_WINDOW_CUSTOM_SIZE)) {
				updateWindow(500, 500);
			} else if(action
					.equals(Recorder_Global.PLUGIN_ACTION_NAVIGATOR_RECORD_STOPSELF)){
				stopSelf();
			}
		}
	}

	/**
	 * 开始预览
	 */
	private void startPreview() {
		Logutil.i(TAG, "startPreview()");
		
		mCameraDevice.setErrorCallback(this);

		if (mPreviewing == true) {
			mCameraDevice.stopPreview();
			mPreviewing = false;
		}

		// TODO 暂时先注释
		// mDisplayRotation = Util.getDisplayRotation(this);
		// int orientation = Util.getDisplayOrientation(mDisplayRotation, );

		// TODO 写先死， 待删除
		int orientation = 90;

		Logutil.i(TAG, "orientation = " + orientation);

		mCameraDevice.setDisplayOrientation(orientation); // TODO 90,��д��
		setCameraParameters();
		setPreviewDisplay(mSurfaceHolder);
		try {
			mCameraDevice.startPreview();
		} catch (Throwable ex) {
			closeCamera();
			throw new RuntimeException("startPreview failed", ex);
		}
		mPreviewing = true;
	}

	private void setPreviewDisplay(SurfaceHolder holder) {
		Log.i(TAG, "setPreviewDisplay()");
		try {
			mCameraDevice.setPreviewDisplay(holder);
		} catch (Throwable ex) {
			closeCamera();
			throw new RuntimeException("setPreviewDisplay failed", ex);
		}
	}

	private void setCameraParameters() {

		Logutil.i(TAG, "setCameraParameters()");

		mParameters = mCameraDevice.getParameters();

		 Log.i(TAG,
		 "setCameraParameters() mDesiredPreviewWidth="+mDesiredPreviewWidth+" , mDesiredPreviewHeight="+mDesiredPreviewHeight);

		 mParameters.setPreviewSize(mDesiredPreviewWidth,
		 mDesiredPreviewHeight);

//		mParameters.setPreviewSize(1280, 720);

		/*
		 * 2013.10.30 bug: oppo机器上会出现预览变形问题 原因： 应该是oppo系统工程师在framework层改了东西 解决：
		 * 屏蔽代码
		 */
		// mParameters.setPreviewFrameRate(mProfile.videoFrameRate);

		// Set continuous autofocus.
		/*
		 * 2013.11.13 bug: cc的 s4（SCH-I959）预览没聚焦， 录像崩溃，手机并重启 解决： 下面一段屏蔽，
		 * 厂商定制化引起的
		 * TODO 这段代码很讨厌， 屏蔽了 oppo手机就不能聚焦， 不屏蔽 三星又崩溃
		 */
		/*
		 * List<String> supportedFocus = mParameters.getSupportedFocusModes();
		 * if (isSupported(Parameters.FOCUS_MODE_CONTINUOUS_VIDEO,
		 * supportedFocus)) {
		 * mParameters.setFocusMode(Parameters.FOCUS_MODE_CONTINUOUS_VIDEO); }
		 */

		/*
		 * 2013.10.30 bug: oppo机器上会出现预览变形问题 原因： 应该是oppo系统工程师在framework层改了东西 解决：
		 * 屏蔽代码
		 */
		// mParameters.setRecordingHint(true);

		/*
		 * 2013.8.1 兼容性问题
		 */
		// Enable video stabilization. Convenience methods not available in API
		// level <= 14
		// String vstabSupported =
		// mParameters.get("video-stabilization-supported");
		// if ("true".equals(vstabSupported)) {
		// mParameters.set("video-stabilization", "true");
		// }

		// Set picture size.
		// The logic here is different from the logic in still-mode camera.
		// There we determine the preview size based on the picture size, but
		// here we determine the picture size based on the preview size.
		// List<Size> supported = mParameters.getSupportedPictureSizes();
		// Size optimalSize = Util.getOptimalVideoSnapshotPictureSize(supported,
		// (double) mDesiredPreviewWidth / mDesiredPreviewHeight);
		// Size original = mParameters.getPictureSize();
		// if (!original.equals(optimalSize)) {
		// mParameters.setPictureSize(optimalSize.width, optimalSize.height);
		// }
		// Log.v(TAG, "Video snapshot size is " + optimalSize.width + "x" +
		// optimalSize.height);

		// Set JPEG quality.
		// int jpegQuality =
		// CameraProfile.getJpegEncodingQualityParameter(mCameraId,
		// CameraProfile.QUALITY_HIGH);
		// mParameters.setJpegQuality(jpegQuality);

		mCameraDevice.setParameters(mParameters);
		// Keep preview size up to date.
		mParameters = mCameraDevice.getParameters();
	}

	// Method called right after Surface created (initializing and starting
	// MediaRecorder)
	@Override
	public void surfaceCreated(SurfaceHolder surfaceHolder) {

		Log.i(TAG, "surfaceCreated()");
		
		// readVideoPreferences();
		//
		// mSurfaceHolder = surfaceHolder;
		//
		// try {
		// mCameraDevice = CameraHolder.instance().open(0);
		// } catch (CameraHardwareException e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// Toast.makeText(this, "摄像头初始化失败，请确认摄像头可用性！",
		// Toast.LENGTH_SHORT).show();
		// this.stopSelf();
		// return;
		// }
		// startVideoRecording();
	}

	// Stop recording and remove SurfaceView
	@Override
	public void surfaceChanged(SurfaceHolder surfaceHolder, int format,
			int width, int height) {
		
		Log.i(TAG, "surfaceChanged()");
		
		// mSurfaceHolder = surfaceHolder;

		// Make sure we have a surface in the holder before proceeding.
		if (surfaceHolder.getSurface() == null) {
			Log.i(TAG, "surfaceChanged()  (surfaceHolder.getSurface() == null)=true");
			return;
		}

		Log.i(TAG, "surfaceChanged()  surfaceChanged. w=" + width + ". h=" + height);

		mSurfaceHolder = surfaceHolder;

		// TODO
		// if (mPausing) {
		// // We're pausing, the screen is off and we already stopped
		// // video recording. We don't want to start the camera again
		// // in this case in order to conserve power.
		// // The fact that surfaceChanged is called _after_ an onPause appears
		// // to be legitimate since in that case the lockscreen always returns
		// // to portrait orientation possibly triggering the notification.
		// return;
		// }

		// The mCameraDevice will be null if it is fail to connect to the
		// camera hardware. In this case we will show a dialog and then
		// finish the activity, so it's OK to ignore it.
		if (mCameraDevice == null){
			Log.i(TAG, "surfaceChanged()  (mCameraDevice == null)=true");
			return;
		}
			
		// Set preview display if the surface is being created. Preview was
		// already started. Also restart the preview if display rotation has
		// changed. Sometimes this happens when the device is held in portrait
		// and camera app is opened. Rotation animation takes some time and
		// display rotation in onCreate may not be what we want.
		
		if (mPreviewing && mSurfaceHolder.isCreating()) {
			Log.i(TAG, "surfaceChanged()  mSurfaceHolder.isCreating()  true");
			setPreviewDisplay(mSurfaceHolder);
		} else {
			Log.i(TAG, "surfaceChanged()  mSurfaceHolder.isCreating()  false");
			// stopVideoRecording();
			// startPreview();
		}
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
		Log.i(TAG, "surfaceDestroyed()");
		mSurfaceHolder = null;
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	private int lVideoRate;
	private CamcorderProfile mProfile;
	
	private void readVideoPreferences() {
		//录像周期    	
    	lVideoRate = mRecorderLogic.getVideoRate();
    	
    	//获取视频品质 [包括： 视频分辨率 和 录制质量]
    	mProfile = mRecorderLogic.getVideoProfile(getScreenSize());

    	//获取当前文件路径
    	Recorder_Global.mCurFilePath = mRecorderLogic.getVideoFilePathDetail();
    	
    	getDesiredPreviewSize();
	}

	/**
	 * 获取预览的最佳分辨率
	 */
	private void getDesiredPreviewSize() {
		mParameters = mCameraDevice.getParameters();
		
		List<Size> sizes = mParameters.getSupportedPreviewSizes();
//      Size preferred = mParameters.getPreferredPreviewSizeForVideo();
//      int product = preferred.width * preferred.height;
      int[] preferred =  getScreenSize();
      int product = preferred[0] * preferred[1];
      
      Iterator it = sizes.iterator();
      // Remove the preview sizes that are not preferred.
      while (it.hasNext()) {
          Size size = (Size) it.next();
          if (size.width * size.height > product) {
              it.remove();
          }
      }
      
      Log.i(TAG, "mProfile.quality="+mProfile.quality);
      
      Log.i(TAG, "mProfile.videoFrameWidth="+mProfile.videoFrameWidth+" , mProfile.videoFrameHeight="+mProfile.videoFrameHeight);
      
      Size optimalSize = Util.getOptimalPreviewSize(preferred, sizes,
          (double) mProfile.videoFrameWidth / mProfile.videoFrameHeight);
      mDesiredPreviewWidth = optimalSize.width;
      mDesiredPreviewHeight = optimalSize.height;
      
	}
	
	private MyMediaRecorder mMediaRecorder;
	private boolean mMediaRecorderRecording = false;

	/**
	 * 开始录像
	 */
	private void startVideoRecording() {
		try {
			initializeRecorder();
		} catch (Exception e) {
			stopSelf();
			Toast.makeText(this, "摄像头初始化失败，请确认摄像头可用性！", Toast.LENGTH_SHORT).show();
			return;
		}
		
		long mStorageSpace = Storage.getAvailableSpace();
		if (mStorageSpace < Storage.LOW_STORAGE_THRESHOLD) {
            Log.v(TAG, "Storage issue, ignore the start request");
            Toast.makeText(this, "存储空间不足 ！ ", Toast.LENGTH_SHORT).show();
            return;
        }
		
		if(mMediaRecorder == null)	return;
		
		try {
            mMediaRecorder.start(); // Recording is now started
        } catch (RuntimeException e) {
            Logutil.e(TAG, "Could not start media recorder. ", e);
            releaseMediaRecorder();
            // If start fails, frameworks will not lock the camera for us.
            mCameraDevice.lock();
            return;
        }
		
		mMediaRecorderRecording = true;
		// 通知全局 当前处于 录像开始状态
		Recorder_Global.isRecording = true;
		
		
		mRecordingStartTime = SystemClock.uptimeMillis();
		
		showRecordingUI(true);
		
		updateRecordingTime();
	}
	
	/**
	 * 停止录像
	 */
	private void stopVideoRecording(){
		Logutil.i(TAG, "stopVideoRecording");
        if (mMediaRecorderRecording) {
        	
        	try {
        		mMediaRecorder.setOnErrorListener(null);
            	mMediaRecorder.setOnInfoListener(null);
        		mMediaRecorder.stop();
        		
			} catch (RuntimeException e) {
				Logutil.e(TAG, "stop fail" ,e);
				if (mVideoFilename != null) Util.deleteVideoFile(mVideoFilename);
			}
			mMediaRecorderRecording = false;
			// 通知全局 当前处于录像停止状态
			Recorder_Global.isRecording = false;
			showRecordingUI(false);
		}
		releaseMediaRecorder();
	}
	
	private void closeCamera(){
		if (mCameraDevice == null) {
            Logutil.i(TAG, "already stopped.");
            return;
        }
        CameraHolder.instance().release();
        mCameraDevice = null;
	}
	
	private void initializeRecorder(){
		if (mCameraDevice == null) return;
        
        if (mSurfaceHolder == null) return;
        
        mMediaRecorder = new MyMediaRecorder();
        
        //outputfile
       	long dateTaken = System.currentTimeMillis();
       	String filename = mRecorderLogic.createFileName(dateTaken)+".mp4";
//       	mVideoFilename = Storage.DIRECTORY + '/' + filename;
       	mVideoFilename = Recorder_Global.mCurFilePath + '/' + filename;
        
        // Set maximum file size.
        long requestedSizeLimit = 0;
        long maxFileSize = Storage.getAvailableSpace() - Storage.LOW_STORAGE_THRESHOLD;
		
        //rotation
        int rotation = 90;
        
        mMediaRecorder.initializeRecorder(
        		mCameraDevice, 
        		mSurfaceHolder, 
        		mProfile, 
        		mVideoFilename, 
        		lVideoRate,
        		maxFileSize, 
        		rotation,
        		this,
        		this
        		);
	}
	
	/**
     * 获取屏幕大小 
     * TODO: 前后台都有写，可以合成一个 
     * T[0] width ,,,,
	 * T[1] height  
     */
    private int[] getScreenSize() {
        int[] screensize = new int[2];
        int width  , height;
        if(windowManager == null){
        	windowManager = (WindowManager) this.getSystemService(Context.WINDOW_SERVICE);
        }
        Display mDisplay = windowManager.getDefaultDisplay();
        width = Math.min(mDisplay.getWidth(), mDisplay.getHeight());
        height = Math.max(mDisplay.getWidth(), mDisplay.getHeight());
        screensize[0] =  width;
        screensize[1] = height;
        return screensize;
    }
	
	private void releaseMediaRecorder() {
        Log.v(TAG, "Releasing media recorder.");
        if (mMediaRecorder != null) {
//            cleanupEmptyFile();
            mMediaRecorder.reset();
            mMediaRecorder.release();
            mMediaRecorder = null;
        }
        mVideoFilename = null;
    }

	@Override
	public void onInfo(MediaRecorder mr, int what, int extra) {
		Logutil.i(TAG, "onInfo() what="+what+" , extra="+extra);
		
		switch (what) {
		
		case MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED:
			
			Logutil.i(TAG, "MAX_DURATION");
			
			Toast.makeText(this, "录像时间超过预设，将自动进行下一段录制", Toast.LENGTH_SHORT).show();
        	
        	//保存重新录像 ,进入下一个录制周期
        	stopVideoRecording();
        	startVideoRecording();
        	
        	mRecorderLogic.checkVideoFiles();
			
			break;
		
		case MediaRecorder.MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED:
			
			// Show the toast.
            Toast.makeText(this, "已达到大小上限",
                    Toast.LENGTH_LONG).show();
			
			if(mMediaRecorderRecording){
				//保存重新录像 ,进入下一个录制周期
	        	stopVideoRecording();
	        	mRecorderLogic.checkVideoFiles();
	        	startVideoRecording();
			}
			break;

		default:
			break;
		}	
	}

	@Override
	public void onClick(View view) {
		switch (view.getId()) {
		case R.id.bt_record:
			boolean stop = mMediaRecorderRecording ;
			if(stop){
				stopVideoRecording();
			} else {
				startVideoRecording();
			}
			
			//防止频繁点击
			mBtRecord.setEnabled(false);
			
			mHandler.sendEmptyMessageDelayed(
                    ENABLE_SHUTTER_BUTTON, SHUTTER_BUTTON_TIMEOUT);
			break;
		case R.id.bt_video_setting:	// 进入视频设置界面

		    intent = new Intent(
					mContext,VideoSetting.class);
		    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			startActivity(intent);
			
			break;

		case R.id.bt_video_show:	//进入视频文件管理器
		    
			//进入查看录像文件夹， 先对文件进行一次过滤删除
			mRecorderLogic.checkVideoFiles();
			
			intent = new Intent(
					mContext,VideoFileManager.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			startActivity(intent);
			break;
			
		case R.id.bt_open_gps:
		    
		    if(isOpenGps()){
		        mBtOpenGps.setText("[已打开GPS]");
		    } else {
		        
		        intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
	            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
	            startActivity(intent);
		    }
		    
		    break;
			
		default:
			break;
		}
	}
	
	
	
	@Override
	public void onDestroy() {

		Log.i(TAG, "onDestroy()");

		stopVideoRecording();
		closeCamera();
		
		removeNewNotification();
		
		windowManager.removeView(view);
		//单件销毁
		RecorderLogic.freeInstance();
	}

	/**
	 * @deprecated
	 * 在 status bar 显示 notification 应用场景：后台录像开始的时候
	 */
	@Deprecated
	private void showNotification() {
		//TODO 		
		PendingIntent pendingIntent = PendingIntent.getService(
				mContext,
				0, 
				new Intent(Recorder_Global.PLUGIN_ACTION_NAVIGATOR_RECORD_STOPSELF), 
				0);

		RemoteViews mRemoteViews = new RemoteViews(getPackageName(),
				R.layout.notification_view);
		mRemoteViews.setOnClickPendingIntent(R.id.notification_stop_recorder, pendingIntent);

		Notification notification = new Notification.Builder(this)
				.setContentTitle("Background Video Recorder")
				.setContentText("")
				.setSmallIcon(R.drawable.notification_icon)
				.setContent(mRemoteViews)
				.getNotification();
		startForeground(1234, notification);
	}
	
	private NotificationManager mNotificationManager;
	private NotificationCompat.Builder mBuilder;
	private RemoteViews mRemoteViews;
	private static final int NOTIFICATION_ID = 1234;
	/**
	 * 点击，停止后台录喜
	 */
	private PendingIntent mPendingIntentStopRecord;
	
	/**
	 * 点击，跳转到录像界面
	 */
	private PendingIntent mPendingIntentDisplayRecorder;
	
	/**
	 * 当前是否有显示 notification 信息 
	 */
	private boolean isShowNotification = false;
	
	/**
	 * 显示新版的notification
	 */
	private void showNewNotification(){
		
		mNotificationManager =
				(NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		
		mPendingIntentStopRecord = PendingIntent.getService(
				mContext,
				0, 
				new Intent(Recorder_Global.PLUGIN_ACTION_NAVIGATOR_RECORD_STOPSELF), 
				0);

		mPendingIntentDisplayRecorder =PendingIntent.getActivity(
				mContext, 
				0, 
				new Intent(Recorder_Global.PLUGIN_ACTION_NAVIGATOR_MYEMPTYACTIVITY), 
				0);
		
		mRemoteViews = new RemoteViews(getPackageName(),
				R.layout.notification_view);
		mRemoteViews.setOnClickPendingIntent(R.id.notification_stop_recorder, mPendingIntentStopRecord);
		
		mRemoteViews.setOnClickPendingIntent(R.id.notification_message_layout, mPendingIntentDisplayRecorder);
		
		mBuilder = new NotificationCompat.Builder(this)
				.setContentTitle("Background Video Recorder")
				.setContentText("")
				.setSmallIcon(R.drawable.notification_icon)
				.setOnlyAlertOnce(true)
				.setContent(mRemoteViews)
				.setOngoing(true)
				;
		
//		startForeground(NOTIFICATION_ID, mBuilder.build());
		mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());
		
		isShowNotification = true;
//		mBuilder =
//		        new NotificationCompat.Builder(this)
//		        .setSmallIcon(R.drawable.notification_icon)
//		        .setContentTitle("My notification")
//		        .setContentText("Hello World!");
//		
//		mNotificationManager =
//			    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
//		// mId allows you to update the notification later on.
//		mNotificationManager.notify(1333, mBuilder.build());
	}

	/**
	 * 更新 notification 录像时间
	 * @param str
	 */
	private void updateNewNotification(String str){

		if(mNotificationManager != null && mBuilder != null){
			if(mRemoteViews != null){
				mRemoteViews.setTextViewText(R.id.notification_update_time,str );
			}
			mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());
		}
	}
	
	/**
	 * 移除 notification
	 */
	private void removeNewNotification(){
		if(mNotificationManager!=null){
//			mBuilder.setOngoing(false);
			mNotificationManager.cancel(NOTIFICATION_ID);
			isShowNotification = false;
		}
	}
	
	private long mRecordingStartTime;
	
	/**
	 * 更新UI
	 * @param recording
	 */
	private void showRecordingUI(boolean recording) {
		if(recording){
			mBtRecord.setBackgroundResource(R.drawable.navi_video_stop);
			mCurRecordTime.setVisibility(View.VISIBLE);
		} else {
			mBtRecord.setBackgroundResource(R.drawable.navi_video_start);
			mCurRecordTime.setVisibility(View.GONE);
		}
	}
	
	/**
	 * 更新已录制时长
	 */
	private void updateRecordingTime() {
		if (!mMediaRecorderRecording) return;
		
		long now = SystemClock.uptimeMillis();
        long delta = now - mRecordingStartTime;
        String recordtime ="REC 00:00";
        
        recordtime ="REC "+ Util.millisecondToTimeString(delta, false);
        
        mCurRecordTime.setText(recordtime);
        
        if(isShowNotification){
        	updateNewNotification(recordtime);
        }
        
		mHandler.sendEmptyMessageDelayed(
                UPDATE_RECORD_TIME, 1000);
	}
	
	/**
	 * 更新系统窗口
	 */
	private void updateWindow(int width, int height) {
		Log.i(TAG, "updateWindow()");
		if (windowManager != null) {
			mLayoutParams.width = width;
			mLayoutParams.height = height;
			windowManager.updateViewLayout(view, mLayoutParams);
		}
	}

	@Override
	public void onError(MediaRecorder arg0, int arg1, int arg2) {
		// TODO Auto-generated method stub
	}

	@Override
	public void onError(int error, Camera camera) {
		// TODO Auto-generated method stub
	}

	@Override
	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
		if(buttonView == mCheckBoxIsBackground){
			if(isChecked){
				Log.i(TAG, "启动后台录像");
				Recorder_Global.isRecordBackground = true;
				
				MyApplication.cache_recoder_video.edit().putInt(
						Recorder_Global.SHARE_PREFERENCE_BACKGROUND_RECORD,
						1).commit();
				
			} else {
				Log.i(TAG, "关闭后台录像");
				Recorder_Global.isRecordBackground = false;
				
				MyApplication.cache_recoder_video.edit().putInt(
						Recorder_Global.SHARE_PREFERENCE_BACKGROUND_RECORD,
						0).commit();
			}			
		}
	}
	
	/** 
     * 判断GPS是否开启
     * @param context 
     * @return true 表示开启 
     */  
    private boolean isOpenGps() {  
        android.location.LocationManager locationManager   
                                 = (android.location.LocationManager) getSystemService(Context.LOCATION_SERVICE);  
        // 通过GPS卫星定位，定位级别可以精确到街（通过24颗卫星定位，在室外和空旷的地方定位准确、速度快）  
        boolean gps = locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER);  
        // 通过WLAN或移动网络(3G/2G)确定的位置（也称作AGPS，辅助GPS定位。主要用于在室内或遮盖物（建筑群或茂密的深林等）密集的地方定位）  
//        boolean network = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);  
        if (gps) {      // || network
            return true;  
        }  
        return false;  
    }  
}

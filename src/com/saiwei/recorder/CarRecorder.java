package com.saiwei.recorder;

import java.util.Iterator;
import java.util.List;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Camera;
import android.hardware.Camera.ErrorCallback;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.Size;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.media.MediaRecorder.OnErrorListener;
import android.media.MediaRecorder.OnInfoListener;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.OrientationEventListener;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.autonavi.xmgd.utility.Logutil;
import com.autonavi.xmgd.utility.Storage;
import com.autonavi.xmgd.utility.Util;
import com.saiwei.recorder.application.Recorder_Global;
import com.saiwei.recorder.exception.CameraDisabledException;
import com.saiwei.recorder.exception.CameraHardwareException;
import com.saiwei.recorder.logic.RecorderLogic;

/**
 * 行车记录仪
 * @author wei.chen
 */
public class CarRecorder extends Activity 
		implements 	SurfaceHolder.Callback ,
					ErrorCallback , 
					OnClickListener , 
					OnErrorListener, 
					OnInfoListener{ 

	private static final String TAG = "chenwei.CarRecorder";
	
	public static final boolean DEBUG =true;
	
	private Context mContext;
	protected Camera mCameraDevice;
	
	private boolean mOpenCameraFail = false;
    private boolean mCameraDisabled = false;
    
    private SurfaceHolder mSurfaceHolder = null;
    
    private boolean mPausing = false;
    private boolean mPreviewing = false; // True if preview is started.
    private boolean mMediaRecorderRecording = false;
    
    private CamcorderProfile mProfile;
    
    private int mCameraId;
    
    /**
     * 开始/结束 录像
     */
    private Button mBtRecord;
    
    private Button mBtSetting;
    private Button mBtShow;
    private Button mBtVideoBackground;
    
    /** 测试文本*/
    private TextView mTest;
    
    
    /**
     * 显示录制了多长时间
     */
    private TextView mTVShowTime;	
    
    private long mRecordingStartTime;
    
    private int orientation; // 屏幕方向
    
    private int mDisplayRotation;

	private IntentFilter mFilter;
	private Bundle bundle;
    
	private RecorderLogic mRecorderLogic;
	
	// The degrees of the device rotated clockwise from its natural orientation.
    private int mOrientation = OrientationEventListener.ORIENTATION_UNKNOWN;
	
	private static final long SHUTTER_BUTTON_TIMEOUT = 500L; // 500ms
    
	private static final int CLEAR_SCREEN_DELAY = 4;
	private static final int UPDATE_RECORD_TIME = 5;
	private static final int REMOVE_UPDATE_RECORD_TIME = 6;
    private static final int ENABLE_SHUTTER_BUTTON = 7;
    private static final int SHOW_ERROR = 8;
    private static final int FAIL_CONNECT_TO_CAMERA = 9;
    
    private static final int SCREEN_DELAY = 5 * 1000;
    
    private Parameters mParameters;
    private int mDesiredPreviewWidth;
    private int mDesiredPreviewHeight;
    
	private Handler mHandler = new Handler(){

		@Override
		public void handleMessage(Message msg) {
			
			switch (msg.what) {
			case CLEAR_SCREEN_DELAY:
				getWindow().clearFlags(
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
				break;
			case ENABLE_SHUTTER_BUTTON:
				if(mBtRecord !=null){
					mBtRecord.setEnabled(true);
				}
				
				break;
			case UPDATE_RECORD_TIME:
				updateRecordingTime();
				break;
			case SHOW_ERROR:
				Toast.makeText(mContext, "需要对该机器进行特殊处理!!!", Toast.LENGTH_SHORT ).show();
				break;
				
			case FAIL_CONNECT_TO_CAMERA:
				Toast.makeText(CarRecorder.this, "[FAIL_CONNECT_TO_CAMERA]  "+msg.obj, Toast.LENGTH_LONG).show();
				break;
			default:
				break;
			}
		}
	};
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        Logutil.i(TAG, "onCreate()");
        
        Intent intent = getIntent();
        if(intent != null){
        	
        	//TODO
//        	String action = intent.getAction();
//        	if(action != null){
//        		if(action.equals(Recorder_Global.PLUGIN_FROM_UNITE_ACTION_NAVIGATOR_RECORD_NORMAL_STOP)){
//        			finish();
//        			return;
//        		}
//        	}
        }
        
        mContext = this;
        mCameraId = 0;
        //逻辑类
        mRecorderLogic = RecorderLogic.getInstance(getApplication());
        
        /*
         * To reduce startup time, we start the preview in another thread.
         * We make sure the preview is started at the end of onCreate.
         */
        Thread startPreviewThread = new Thread(new Runnable() {
            @Override
			public void run() {
                try {
                    mCameraDevice = Util.openCamera(CarRecorder.this, mCameraId);
                    readVideoPreferences();
                    
                    Logutil.i(TAG, "camera open");
                    
                    startPreview();
                } catch (CameraHardwareException e) {
                	Message msg = mHandler.obtainMessage();
                	msg.what = FAIL_CONNECT_TO_CAMERA;
                	msg.obj = e.toString();
                	mHandler.sendMessage(msg);
                	Log.i(TAG, "CameraHardwareException() e="+e.toString());
                	
                    mOpenCameraFail = true;
                } catch (CameraDisabledException e) {
                	
                	Toast.makeText(CarRecorder.this, ""+e.toString(), Toast.LENGTH_LONG).show();
                	
                    mCameraDisabled = true;
                } catch (Exception e) {
                	
                	Toast.makeText(CarRecorder.this, ""+e.toString(), Toast.LENGTH_LONG).show();
                	
					finish();
					return;
				}
            }
        });
        startPreviewThread.start();
        
        setContentView(R.layout.activity_main);
        
        
        // don't set mSurfaceHolder here. We have it set ONLY within
        // surfaceCreated / surfaceDestroyed, other parts of the code
        // assume that when it is set, the surface is also set.
        SurfaceView preview = (SurfaceView) findViewById(R.id.camera_preview);
        SurfaceHolder holder = preview.getHolder();
        holder.addCallback(this);
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        
        // Make sure preview is started.
        try {
            startPreviewThread.join();
            if (mOpenCameraFail) {
                Util.showErrorAndFinish(this, R.string.cannot_connect_camera);
                return;
            } else if (mCameraDisabled) {
                Util.showErrorAndFinish(this, R.string.camera_disabled);
                return;
            }
        } catch (InterruptedException ex) {
            // ignore
        }
        
        init();    
    }
    
    @Override
    protected void onRestart() {
    	super.onRestart();
    	
    	Logutil.i(TAG, "onRestart()");
    }
    
    private void init() {

        mBtRecord = (Button) this.findViewById(R.id.bt_record);
        mBtRecord.setOnClickListener(this);
        
        mBtSetting =(Button) this.findViewById(R.id.bt_video_setting);
        mBtSetting.setOnClickListener(this);
        
        mBtShow = (Button) this.findViewById(R.id.bt_video_show);
        mBtShow.setOnClickListener(this);
    
        mBtVideoBackground = (Button) this.findViewById(R.id.bt_video_background);
        mBtVideoBackground.setOnClickListener(this);
        
        mTVShowTime = (TextView) this.findViewById(R.id.tv_show_time);
        
        if(DEBUG){
        	mTest = (TextView) this.findViewById(R.id.video_test);
        	mTest.setVisibility(View.VISIBLE);
        }

		
		//保持屏幕常亮
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }
    
    /**
     * 判断是否竖屏
     * @return
     */
    private boolean isPortrait(){
    	
    	int orientation = getOrientation();
    	if(orientation == 0 || orientation == 2){
    		return true;
    	} else {
    		return false;
    	}
    }
    
	private void setPreviewDisplay(SurfaceHolder holder) {
        try {
            mCameraDevice.setPreviewDisplay(holder);
        } catch (Throwable ex) {
            closeCamera();
            throw new RuntimeException("setPreviewDisplay failed", ex);
        }
    }
    
	/**
	 * 开始预览
	 */
    private void startPreview() {
    	
    	
    	Logutil.i(TAG, "startPreview()");
    	
    	mCameraDevice.setErrorCallback(this);
    	
    	if(mPreviewing == true){
    		mCameraDevice.stopPreview();
    		mPreviewing = false;
    	}
    	    	
    	mDisplayRotation = Util.getDisplayRotation(this);
        int orientation = Util.getDisplayOrientation(mDisplayRotation, mCameraId);

        
        Logutil.i(TAG, "orientation = "+orientation);
        
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
    
    /**
     * 关闭摄像头
     */
    private void closeCamera() {
        Log.v(TAG, "closeCamera");
        if (mCameraDevice == null) {
        	Logutil.i(TAG, "already stopped.");
            return;
        }

        CameraHolder.instance().release();
        mCameraDevice.setErrorCallback(null);
        mCameraDevice = null;
        mPreviewing = false;
    }
    
    @Override
    protected void onResume() {
    	super.onResume();
    	
    	Logutil.i(TAG, "onResume()");
    	
    	mPausing = false;
    	
    	if (!mPreviewing) {
            try {
                mCameraDevice = Util.openCamera(this, mCameraId);
                readVideoPreferences();
//                resizeForPreviewAspectRatio();
                startPreview();
            } catch (CameraHardwareException e) {
                Util.showErrorAndFinish(this, R.string.cannot_connect_camera);
                return;
            } catch (CameraDisabledException e) {
                Util.showErrorAndFinish(this, R.string.camera_disabled);
                return;
            } catch (Exception e){
            	finish();
            	return ;
            }
        }
    	
    	mStorageSpace = Storage.getAvailableSpace();
    }
    
    @Override
    protected void onPause() {
    	super.onPause();
    	
    	Logutil.i(TAG, "onPause()");
    	
    	mPausing = true;
    	
    	finishRecorderAndCloseCamera();

    }

    private void finishRecorderAndCloseCamera() {
    	stopVideoRecording();
    	closeCamera();
	}
    
    private static boolean isSupported(String value, List<String> supported) {
        return supported == null ? false : supported.indexOf(value) >= 0;
    }
    
    private void setCameraParameters() {
    	
    	Logutil.i(TAG, "setCameraParameters()");
    	
        mParameters = mCameraDevice.getParameters();
        
        Log.i(TAG, "setCameraParameters() mDesiredPreviewWidth="+mDesiredPreviewWidth+" , mDesiredPreviewHeight="+mDesiredPreviewHeight);
        
        mParameters.setPreviewSize(mDesiredPreviewWidth, mDesiredPreviewHeight);
        
//        mParameters.setPreviewSize(1280, 720);
        
        /*
         * 2013.10.30 
         * bug: oppo机器上会出现预览变形问题
         * 原因： 应该是oppo系统工程师在framework层改了东西
         * 解决： 屏蔽代码	
         */
//        mParameters.setPreviewFrameRate(mProfile.videoFrameRate);
        
        // Set continuous autofocus.
        /*
         * 2013.11.13
         *  bug: cc的 s4（SCH-I959）预览没聚焦， 录像崩溃，手机并重启
         *  解决： 下面一段屏蔽， 厂商定制化引起的
         */
        /*List<String> supportedFocus = mParameters.getSupportedFocusModes();
        if (isSupported(Parameters.FOCUS_MODE_CONTINUOUS_VIDEO, supportedFocus)) {
            mParameters.setFocusMode(Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
        }*/
        
        /*
         * 2013.10.30 
         * bug: oppo机器上会出现预览变形问题
         * 原因： 应该是oppo系统工程师在framework层改了东西
         * 解决： 屏蔽代码	
         */
//        mParameters.setRecordingHint(true);

        /*
         * 2013.8.1 兼容性问题
         */
        // Enable video stabilization. Convenience methods not available in API
        // level <= 14
//        String vstabSupported = mParameters.get("video-stabilization-supported");
//        if ("true".equals(vstabSupported)) {
//            mParameters.set("video-stabilization", "true");
//        }

        // Set picture size.
        // The logic here is different from the logic in still-mode camera.
        // There we determine the preview size based on the picture size, but
        // here we determine the picture size based on the preview size.
//        List<Size> supported = mParameters.getSupportedPictureSizes();
//        Size optimalSize = Util.getOptimalVideoSnapshotPictureSize(supported,
//                (double) mDesiredPreviewWidth / mDesiredPreviewHeight);
//        Size original = mParameters.getPictureSize();
//        if (!original.equals(optimalSize)) {
//            mParameters.setPictureSize(optimalSize.width, optimalSize.height);
//        }
//        Log.v(TAG, "Video snapshot size is " + optimalSize.width + "x" +
//                optimalSize.height);

        // Set JPEG quality.
//        int jpegQuality = CameraProfile.getJpegEncodingQualityParameter(mCameraId,
//                CameraProfile.QUALITY_HIGH);
//        mParameters.setJpegQuality(jpegQuality);

        mCameraDevice.setParameters(mParameters);
        // Keep preview size up to date.
        mParameters = mCameraDevice.getParameters();
    }
    
    private int lVideoRate;

    private long mStorageSpace;


    private void updateAndShowStorageHint() {
        mStorageSpace = Storage.getAvailableSpace();
//        showStorageHint();
    }

    private void showStorageHint() {
        String errorMessage = null;
//        if (mStorageSpace == Storage.UNAVAILABLE) {
//            errorMessage = getString(R.string.no_storage);
//        } else if (mStorageSpace == Storage.PREPARING) {
//            errorMessage = getString(R.string.preparing_sd);
//        } else if (mStorageSpace == Storage.UNKNOWN_SIZE) {
//            errorMessage = getString(R.string.access_sd_fail);
//        } else if (mStorageSpace < Storage.LOW_STORAGE_THRESHOLD) {
//            errorMessage = getString(R.string.spaceIsLow_content);
//        }
    }
    
	private void readVideoPreferences() {
		
		Logutil.i(TAG, "readVideoPreferences()");
		
		//录像周期    	
    	lVideoRate = mRecorderLogic.getVideoRate();
		
		//获取视频品质 [包括： 视频分辨率 和 录制质量]
		mProfile = mRecorderLogic.getVideoProfile(getScreenSize());
		
		if(mProfile==null){
			throw new NullPointerException();
		}
		
		getDesiredPreviewSize();
	}
	
	private void getDesiredPreviewSize() {
        mParameters = mCameraDevice.getParameters();
//        if (mParameters.getSupportedVideoSizes() == null) {
//            mDesiredPreviewWidth = mProfile.videoFrameWidth;
//            mDesiredPreviewHeight = mProfile.videoFrameHeight;
//        } else {  // Driver supports separates outputs for preview and video.
//            List<Size> sizes = mParameters.getSupportedPreviewSizes();
//
//            /*
//             * 2013.8.5
//             * Bug #70500 (1920*1080)行车记录仪，1.0.002版本调用摄像头时，界面会出现水波纹(GT_9200)
//             * 具体看开发文档
//             */
//            
////            Size preferred = mParameters.getPreferredPreviewSizeForVideo();
////            int product = preferred.width * preferred.height;
//            
//            int[] preferred =  getScreenSize();
////            int product = preferred[0] * preferred[1];
//            
////            int product =  mProfile.videoFrameWidth * mProfile.videoFrameHeight;
//            
////            Log.i(TAG, "mProfile.videoFrameWidth="+mProfile.videoFrameWidth+" , mProfile.videoFrameHeight="+mProfile.videoFrameHeight);
////            Log.i(TAG, "product="+product);
//            
////            Iterator it = sizes.iterator();
////            // Remove the preview sizes that are not preferred.
////            while (it.hasNext()) {
////                Size size = (Size) it.next();
////                if (size.width * size.height > product) {
////                    it.remove();
////                }
////            }
////            Size optimalSize = Util.getOptimalPreviewSize(this, sizes,
////                (double) preferred[0] / preferred[1]);
//            
//            Size optimalSize = getOptimalPreviewSize( sizes,
//            		preferred[0] ,preferred[1]);
//            
//            mDesiredPreviewWidth = optimalSize.width;
//            mDesiredPreviewHeight = optimalSize.height;
            
 
            List<Size> sizes = mParameters.getSupportedPreviewSizes();
//            Size preferred = mParameters.getPreferredPreviewSizeForVideo();
//            int product = preferred.width * preferred.height;
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
            
            Log.i(TAG, "mProfile.videoFrameWidth="+mProfile.videoFrameWidth+" , mProfile.videoFrameHeight="+mProfile.videoFrameHeight);
            
            Size optimalSize = Util.getOptimalPreviewSize(this, sizes,
                (double) mProfile.videoFrameWidth / mProfile.videoFrameHeight);
            mDesiredPreviewWidth = optimalSize.width;
            mDesiredPreviewHeight = optimalSize.height;
//        }
    }
	
	private WindowManager windowManager;
	private Display mDisplay;
	
	/**
	 * 获取屏幕大小
	 */
	private int[] getScreenSize() {
		
		int[] screensize = new int[2];
		int width  , height;
		windowManager = (WindowManager) mContext
				.getSystemService(mContext.WINDOW_SERVICE);
		mDisplay = windowManager.getDefaultDisplay();

		height = Math.min(mDisplay.getWidth(), mDisplay.getHeight());
		width = Math.max(mDisplay.getWidth(), mDisplay.getHeight());
		
		screensize[0] =  width;
		screensize[1] = height;
		
		return screensize;
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		
	}

	private int mSurfaceWidth;
    private int mSurfaceHeight;
	
	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int w,
			int h) {
		// Make sure we have a surface in the holder before proceeding.
        if (holder.getSurface() == null) {
        	Logutil.i(TAG, "holder.getSurface() == null");
            return;
        }

        Log.v(TAG, "surfaceChanged. w=" + w + ". h=" + h);

        mSurfaceHolder = holder;
        mSurfaceWidth = w;
        mSurfaceHeight = h;

        if (mPausing) {
            // We're pausing, the screen is off and we already stopped
            // video recording. We don't want to start the camera again
            // in this case in order to conserve power.
            // The fact that surfaceChanged is called _after_ an onPause appears
            // to be legitimate since in that case the lockscreen always returns
            // to portrait orientation possibly triggering the notification.
            return;
        }

        // The mCameraDevice will be null if it is fail to connect to the
        // camera hardware. In this case we will show a dialog and then
        // finish the activity, so it's OK to ignore it.
        if (mCameraDevice == null) return;

        // Set preview display if the surface is being created. Preview was
        // already started. Also restart the preview if display rotation has
        // changed. Sometimes this happens when the device is held in portrait
        // and camera app is opened. Rotation animation takes some time and
        // display rotation in onCreate may not be what we want.
        if (mPreviewing &&  holder.isCreating()) {
        	
        	Logutil.i(TAG, "surfaceChanged  holder.isCreating()  true");
        	
            setPreviewDisplay(holder);
        } else {
        	
        	Logutil.i(TAG, "surfaceChanged  holder.isCreating()  false");
        	
//            stopVideoRecording();
//            startPreview();
        }
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		mSurfaceHolder = null;
	}

	@Override
	public void onError(int error, Camera camera) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void onClick(View v) {
		
		switch (v.getId()) {
		case R.id.bt_record:
			
			boolean stop = mMediaRecorderRecording ;
			if(stop){
				stopVideoRecording();
			} else {
				startVideoRecording();
			}
			
			mBtRecord.setEnabled(false);
			
			mHandler.sendEmptyMessageDelayed(
                    ENABLE_SHUTTER_BUTTON, SHUTTER_BUTTON_TIMEOUT);
			
			break;
			
		case R.id.bt_video_setting:

			Intent mIntent = new Intent(mContext ,VideoSetting.class);
			startActivity(mIntent);
			break;
		
		case R.id.bt_video_show:
			 //进入查看录像文件夹， 先对文件进行一次过滤删除
			 mRecorderLogic.checkVideoFiles();
			
			 Intent iVideo = new Intent(mContext, VideoFileManager.class);
			 startActivity(iVideo);
			 break;
		
		case R.id.bt_video_background:	//后台录像
		    
		    Toast.makeText(this, "开发中...", Toast.LENGTH_SHORT).show();
		    
//			Recorder_Global.isRecordBackground = true;
//			
//			finishRecorderAndCloseCamera();
//			if(Recorder_Global.isRecordBackground){
//				BackgroundVideoRecorderLogic.getInstance(getApplication()).startRecorder();
//			}
//			finish();
			break;
			
		default:
			break;
		}
	}
	
	private String mVideoFilename;
	private MyMediaRecorder mMediaRecorder;
	
	/**
	 *  初始化录像
	 */
	private void initializeRecorder(){
		
		if (mCameraDevice == null) return;
        
        if (mSurfaceHolder == null) return;
		
		mMediaRecorder = new MyMediaRecorder();
		
		//outputfile
		long dateTaken = System.currentTimeMillis();
        String filename = mRecorderLogic.createFileName(dateTaken)+".mp4";
        mVideoFilename = Recorder_Global.mCurFilePath + '/' + filename;
		
        // Set maximum file size.
        long requestedSizeLimit = 0;
        long maxFileSize = Storage.getAvailableSpace() - Storage.LOW_STORAGE_THRESHOLD;
		
        //rotation
        int rotation = 0;
        boolean isPortrait = isPortrait();
        if(isPortrait){
        	rotation = 90;
        } else {
        	rotation = 0 ;	
        }
        
        try {
        	mMediaRecorder.initializeRecorder(
    				mCameraDevice, 
    				mSurfaceHolder, 
    				mProfile, 
    				mVideoFilename, 
    				lVideoRate,
    				maxFileSize, 
    				rotation,
    				this,
    				this);
		} catch (Exception e) {
			finish();
			Toast.makeText(this, "摄像头初始化失败，请确认摄像头可用性！", Toast.LENGTH_SHORT).show();
		}
	}
	
	/**
	 * 开始录像
	 */
	private void startVideoRecording() {
		initializeRecorder();
		
		mStorageSpace = Storage.getAvailableSpace();
        if (mStorageSpace < Storage.LOW_STORAGE_THRESHOLD) {
            Log.v(TAG, "Storage issue, ignore the start request");
            Toast.makeText(mContext, "存储空间不足 ！ ", Toast.LENGTH_SHORT).show();
            return;
        }
		
		if(mMediaRecorder == null) return;
		
//		pauseAudioPlayback();
		
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
		
		mRecordingStartTime = SystemClock.uptimeMillis();
		
		showRecordingUI(true);
		
		updateRecordingTime();
	}
	
	/**
	 * 停止录像
	 */
	private void stopVideoRecording() {
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
            showRecordingUI(false);
        }
        releaseMediaRecorder();
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
	
	/**
	 * 更新已录制时长
	 */
	private void updateRecordingTime() {
		if (!mMediaRecorderRecording) {
            return;
        }
		
		long now = SystemClock.uptimeMillis();
        long delta = now - mRecordingStartTime;
        String text ;
        
        text = millisecondToTimeString(delta, false);
        
		mTVShowTime.setText("REC "+text);
		
		mHandler.sendEmptyMessageDelayed(
                UPDATE_RECORD_TIME, 1000);
	}
	
	private static String millisecondToTimeString(long milliSeconds, boolean displayCentiSeconds) {
        long seconds = milliSeconds / 1000; // round down to compute seconds
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long remainderMinutes = minutes - (hours * 60);
        long remainderSeconds = seconds - (minutes * 60);

        StringBuilder timeStringBuilder = new StringBuilder();

        // Hours
        if (hours > 0) {
            if (hours < 10) {
                timeStringBuilder.append('0');
            }
            timeStringBuilder.append(hours);

            timeStringBuilder.append(':');
        }

        // Minutes
        if (remainderMinutes < 10) {
            timeStringBuilder.append('0');
        }
        timeStringBuilder.append(remainderMinutes);
        timeStringBuilder.append(':');

        // Seconds
        if (remainderSeconds < 10) {
            timeStringBuilder.append('0');
        }
        timeStringBuilder.append(remainderSeconds);

        // Centi seconds
        if (displayCentiSeconds) {
            timeStringBuilder.append('.');
            long remainderCentiSeconds = (milliSeconds - seconds * 1000) / 10;
            if (remainderCentiSeconds < 10) {
                timeStringBuilder.append('0');
            }
            timeStringBuilder.append(remainderCentiSeconds);
        }

        return timeStringBuilder.toString();
    }

	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		
		Logutil.i(TAG, "onDestroy()");
		
		if(isFinishing() && !Recorder_Global.isRecordBackground){			
			RecorderLogic.freeInstance();
		}
	}
	
	private WindowManager mWindowManager;
	
	/**
	 * 根据不同角度下屏幕是横屏还是竖屏 (暂时不考虑 pad)
	 */
	public int getOrientation() {
		mWindowManager = (WindowManager) mContext
				.getSystemService(Context.WINDOW_SERVICE);
		return mWindowManager.getDefaultDisplay().getOrientation();
	}
	
	private static final int DLG_NAVI_RECORD_EXIT = 0;
	private static final int DLG_NAVI_RECORD_BACKGROUND = 1;
	
	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
//		case DLG_NAVI_RECORD_EXIT:
//			
//        	CustomDialog dlgVideoRecordExit = new CustomDialog(CarRecorder.this,
//					CustomDialog.DIALOG_TYPE_TEXT,
//					new ADialogListener() {
//						   
//						@Override
//						public void onSureClicked(List<String> itemHadChose) {
//							dismissDialog(DLG_NAVI_RECORD_EXIT);
//		                	finish();
//						}
//
//						@Override
//						public void onCancelClicked( ) {
//							
//						}
//					});
//        	dlgVideoRecordExit.setTitleName(getString(R.string.alert_dialog_title));
////			mCustomDialog.setmTitleDrawableId(R.drawable.alert_dialog_icon);
//        	dlgVideoRecordExit.setTextContent(getString(R.string.dialog_message_whether_close_car_recorder));
//        	dlgVideoRecordExit.setBtnSureText(Util.getString(
//					getApplicationContext(), android.R.string.ok));
//        	dlgVideoRecordExit.setBtnCancelText(Util.getString(
//					getApplicationContext(), android.R.string.cancel));
//			return dlgVideoRecordExit;
        
		case DLG_NAVI_RECORD_BACKGROUND:
			
			break;
			
		default:
			break;
		}
		return super.onCreateDialog(id);
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		
		if(keyCode==KeyEvent.KEYCODE_BACK){
			showDialog(DLG_NAVI_RECORD_EXIT);
		}
		return super.onKeyDown(keyCode, event);
	}
	
	
	
	/**
	 * 更新UI
	 * @param recording
	 */
	private void showRecordingUI(boolean recording) {
		if(recording){
			mBtRecord.setBackgroundResource(R.drawable.navi_video_stop);
			mBtVideoBackground.setVisibility(View.VISIBLE);
			mTVShowTime.setVisibility(View.VISIBLE);
		} else {
			mBtRecord.setBackgroundResource(R.drawable.navi_video_start);
			mBtVideoBackground.setVisibility(View.GONE);
			mTVShowTime.setVisibility(View.GONE);
		}
	}

	@Override
	public void onInfo(MediaRecorder mr, int what, int extra) {
		
		Logutil.i(TAG, "onInfo() what="+what+" , extra="+extra);
		
		switch (what) {
		
		case MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED:
			
			Logutil.i(TAG, "MAX_DURATION");
			
//			Tool.writeLog("onInfo()", "将自动进行下一段录制  ,当前时间："+SystemClock.uptimeMillis());
			
			mHandler.removeMessages(UPDATE_RECORD_TIME);
			
			Toast.makeText(mContext, "录像时间超过预设，将自动进行下一段录制", Toast.LENGTH_SHORT).show();
        	
        	//保存重新录像 ,进入下一个录制周期
        	stopVideoRecording();
        	startVideoRecording();
        	
        	mRecorderLogic.checkVideoFiles();
			
			break;
		
		case MediaRecorder.MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED:
			
			if(mMediaRecorderRecording){
				//保存重新录像 ,进入下一个录制周期
	        	stopVideoRecording();
	        	mRecorderLogic.checkVideoFiles();
	        	startVideoRecording();
			}
		
        	// Show the toast.
            Toast.makeText(this, "已达到大小上限",
                    Toast.LENGTH_LONG).show();
			
			break;

		default:
			break;
		}	
	}

	@Override
	public void onError(MediaRecorder mr, int what, int extra) {
		Logutil.e(TAG, "onError()  . what=" + what + ". extra=" + extra);
		
		if(what == MediaRecorder.MEDIA_RECORDER_ERROR_UNKNOWN){
			
		}
	}
}

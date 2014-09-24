package com.autonavi.xmgd.utility;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.os.storage.StorageManager;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.Display;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.widget.Toast;

import com.saiwei.recorder.CameraHolder;
import com.saiwei.recorder.CarRecorder;
import com.saiwei.recorder.R;
import com.saiwei.recorder.exception.CameraDisabledException;
import com.saiwei.recorder.exception.CameraHardwareException;

/**
 * Collection of utility functions used in this package.
 */
public class Util {

	private static final String TAG = "chenwei.Util";
	
	// Orientation hysteresis amount used in rounding, in degrees
    public static final int ORIENTATION_HYSTERESIS = 5;
	
	public static void Assert(boolean cond) {
        if (!cond) {
            throw new AssertionError();
        }
    }
	
	public static android.hardware.Camera openCamera(Activity activity, int cameraId)
            throws CameraHardwareException, CameraDisabledException {
    	
    	Logutil.i(TAG, "openCamera()");
    	
        // Check if device policy has disabled the camera.
/*        DevicePolicyManager dpm = (DevicePolicyManager) activity.getSystemService(
                Context.DEVICE_POLICY_SERVICE);*/
        
//        Logutil.i(TAG, "(dpm.getCameraDisabled(null))="+(dpm.getCameraDisabled(null)));
        
//        if (dpm.getCameraDisabled(null)) {
//            throw new CameraDisabledException();
//        }
        
        try {
            return CameraHolder.instance().open(cameraId);
        } catch (CameraHardwareException e) {
        	
        	Toast.makeText(activity, ""+e.toString(), Toast.LENGTH_LONG).show();
        	
            // In eng build, we throw the exception so that test tool
            // can detect it and report it
            if ("eng".equals(Build.TYPE)) {
                throw new RuntimeException("openCamera failed", e);
            } else {
                throw e;
            }
        } 
    }
	
	
	public static Size getOptimalPreviewSize(int[] screensize,
            List<Size> sizes, double targetRatio) {
        // Use a very small tolerance because we want an exact match.
        final double ASPECT_TOLERANCE = 0.001;
        if (sizes == null) return null;

        Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        // Because of bugs of overlay and layout, we sometimes will try to
        // layout the viewfinder in the portrait orientation and thus get the
        // wrong size of mSurfaceView. When we change the preview size, the
        // new overlay will be created before the old one closed, which causes
        // an exception. For now, just get the screen size

        
        int displayWidth = screensize[0];
        int dislayHeight= screensize[1];
        
//        int targetHeight = Math.min(display.getHeight(), display.getWidth());
        
        int targetHeight = Math.min(dislayHeight, displayWidth);

        if (targetHeight <= 0) {
            // We don't know the size of SurfaceView, use screen height
            targetHeight = dislayHeight;
        }

        // Try to find an size match aspect ratio and size
        for (Size size : sizes) {
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;
            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }

        // Cannot find the one match the aspect ratio. This should not happen.
        // Ignore the requirement.
        if (optimalSize == null) {
            Log.w(TAG, "No preview size match the aspect ratio");
            minDiff = Double.MAX_VALUE;
            for (Size size : sizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }
        return optimalSize;
    }
	
	
	public static Size getOptimalPreviewSize(Activity currentActivity,
            List<Size> sizes, double targetRatio) {
        // Use a very small tolerance because we want an exact match.
        final double ASPECT_TOLERANCE = 0.001;
        if (sizes == null) return null;

        Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        // Because of bugs of overlay and layout, we sometimes will try to
        // layout the viewfinder in the portrait orientation and thus get the
        // wrong size of mSurfaceView. When we change the preview size, the
        // new overlay will be created before the old one closed, which causes
        // an exception. For now, just get the screen size

        Display display = currentActivity.getWindowManager().getDefaultDisplay();
        int targetHeight = Math.min(display.getHeight(), display.getWidth());

        if (targetHeight <= 0) {
            // We don't know the size of SurfaceView, use screen height
            targetHeight = display.getHeight();
        }

        // Try to find an size match aspect ratio and size
        for (Size size : sizes) {
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;
            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }

        // Cannot find the one match the aspect ratio. This should not happen.
        // Ignore the requirement.
        if (optimalSize == null) {
            Log.w(TAG, "No preview size match the aspect ratio");
            minDiff = Double.MAX_VALUE;
            for (Size size : sizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }
        return optimalSize;
    }
	
	// Returns the largest picture size which matches the given aspect ratio.
    public static Size getOptimalVideoSnapshotPictureSize(
            List<Size> sizes, double targetRatio) {
        // Use a very small tolerance because we want an exact match.
        final double ASPECT_TOLERANCE = 0.001;
        if (sizes == null) return null;

        Size optimalSize = null;

        // Try to find a size matches aspect ratio and has the largest width
        for (Size size : sizes) {
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;
            if (optimalSize == null || size.width > optimalSize.width) {
                optimalSize = size;
            }
        }

        // Cannot find one that matches the aspect ratio. This should not happen.
        // Ignore the requirement.
        if (optimalSize == null) {
            Log.w(TAG, "No picture size match the aspect ratio");
            for (Size size : sizes) {
                if (optimalSize == null || size.width > optimalSize.width) {
                    optimalSize = size;
                }
            }
        }
        return optimalSize;
    }
    
    public static void showErrorAndFinish(final Activity activity, int msgId) {
        DialogInterface.OnClickListener buttonListener =
                new DialogInterface.OnClickListener() {
            @Override
			public void onClick(DialogInterface dialog, int which) {
                activity.finish();
            }
        };
        new AlertDialog.Builder(activity)
                .setCancelable(false)
                .setIconAttribute(android.R.attr.alertDialogIcon)
                .setTitle(R.string.camera_error_title)
                .setMessage(msgId)
                .setNeutralButton(android.R.string.ok, buttonListener)
                .show();
    }
    
    public static int getDisplayRotation(Activity activity) {
        int rotation = activity.getWindowManager().getDefaultDisplay()
                .getRotation();
        switch (rotation) {
            case Surface.ROTATION_0: return 0;
            case Surface.ROTATION_90: return 90;
            case Surface.ROTATION_180: return 180;
            case Surface.ROTATION_270: return 270;
        }
        return 0;
    }

    public static int getDisplayOrientation(int degrees, int cameraId) {
        // See android.hardware.Camera.setDisplayOrientation for
        // documentation.
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(cameraId, info);
        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        return result;
    }
    
    /**
     * 获取软件版本号
     * @return
     */
    public static String getVersion(Context context){
    	
    	if(context == null){
    		return "";
    	}
    	
    	PackageInfo info;
    	String versionName = "";
		try {
			info = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
			versionName = info.versionName;  
		} catch (NameNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		
		return versionName;
    }
    
    /**
	 * 创建文件夹
	 * @param path
	 */
	public static void createFolder(String path){
		File mFile = new File(path) ;
		if(!mFile.exists()){
			mFile.mkdirs();
		}
	}
	
	/**
	 * 判断文件是否存在
	 * @param path
	 * @return
	 */
	public static boolean isFileExist(String path){
		File mFile = new File(path);
		if(mFile.exists()){
			return true;
		} else{
			return false;
		}
	}
	
	public static int roundOrientation(int orientation, int orientationHistory) {
        boolean changeOrientation = false;
        if (orientationHistory == OrientationEventListener.ORIENTATION_UNKNOWN) {
            changeOrientation = true;
        } else {
            int dist = Math.abs(orientation - orientationHistory);
            dist = Math.min( dist, 360 - dist );
            changeOrientation = ( dist >= 45 + ORIENTATION_HYSTERESIS );
        }
        if (changeOrientation) {
            return ((orientation + 45) / 90 * 90) % 360;
        }
        return orientationHistory;
    }
	
	/**
	 * 删除视频文件
	 * @param fileName
	 */
	public static void deleteVideoFile(String fileName) {
        Log.v(TAG, "Deleting video " + fileName);
        File f = new File(fileName);
        if (!f.delete()) {
            Log.v(TAG, "Could not delete " + fileName);
        }
    }
	
	public static String millisecondToTimeString(long milliSeconds, boolean displayCentiSeconds) {
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
	
	/**
	 * 写入到sdcard的log，的文件名
	 */
	private static final String LOG_FILE_NAME = "nai_car_record_log.txt";
	
	/**
	 * log写到sdcard
	 * @param title   例如：写  onHttpRequestFinish  写方法名
	 * @param aStream
	 */
	public static void writeLog(String title ,String message){

		if(CarRecorder.DEBUG){
			StringBuilder sb = new StringBuilder("");
			String curTime = DateFormat.format("yyyy-MM-dd kk:mm:ss",
					System.currentTimeMillis()).toString();
			sb.append(curTime)
					.append("---"+title+"---")
					.append(message)
					.append("\n");
			writeLog(LOG_FILE_NAME, sb.toString(), true);
		}
	}
	
	
	/**
	 * 写日志
	 * @param fileName
	 * @param str
	 */
	private static void writeLog(String fileName, String str, boolean append)
	{
		FileOutputStream trace = null;
		try
		{
			String dir = Environment.getExternalStorageDirectory().getAbsolutePath();

			File file = new File(dir + File.separator + fileName);
			trace = new FileOutputStream(file, append);
			trace.write((str+"\r\n").getBytes());
			trace.flush();
		}
		catch(Exception e){}
		finally
		{
			if(trace != null )
			{
				try {
					trace.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}
	

	/**
	 * 加载res/drawable中的图片文件
	 * @param context
	 * @param resId
	 * @return 根据图片资源id返回位图对象，失败返回null
	 */
	public static Bitmap loadImage(Context context,int resId)
	{
		try
		{
			final BitmapFactory.Options mOptions = new BitmapFactory.Options();
		    mOptions.inPurgeable = true;
			return BitmapFactory.decodeResource(context.getResources(),resId,mOptions);
		}
		catch(OutOfMemoryError ome)
		{
			return null;
		}
		catch(Exception e)
		{
			return null;
		}
	}

	/**
	 * 加载res/value/string中的字符串
	 * @param context
	 * @param resid
	 * @return 根据字符串资源id获取字符串，失败返回null
	 */
	public static String getString(Context context, int resid) 
	{
		try
		{
			return context.getResources().getString(resid);
			
		}
		catch(Exception e) 
		{
			return null;
		}
	}
	
	/**
	 * 获取手机状态栏高度
	 * @param context
	 * @return
	 */
    public static int getStatusBarHeight(Context context){
        Class<?> c = null;
        Object obj = null;
        Field field = null;
        int x = 0, statusBarHeight = 0;
        try {
            c = Class.forName("com.android.internal.R$dimen");
            obj = c.newInstance();
            field = c.getField("status_bar_height");
            x = Integer.parseInt(field.get(obj).toString());
            statusBarHeight = context.getResources().getDimensionPixelSize(x);
        } catch (Exception e1) {
            e1.printStackTrace();
        } 
        return statusBarHeight;
    }
    
    /**
	 * 2013.08.17
	 * 增加获取手机存储路径的方法
	 * 
	 * example:
	 *  [0] = /storage/emulated/0
	 *  
	 *  [1] = /storage/extSdCard
	 * 
	 * @param context
	 * @return
	 */
	public static ArrayList<String> enumExternalStroragePath(Context context) {
		ArrayList<String> enumResult = null;
		int currentapiVersion = android.os.Build.VERSION.SDK_INT;
		if (currentapiVersion >= 12) {
			try {
				StorageManager manager = (StorageManager)context.getSystemService(Context.STORAGE_SERVICE);
				/************** StorageManager的方法 ***********************/
				Method getVolumeList = StorageManager.class.getMethod("getVolumeList", null);
				Method getVolumeState = StorageManager.class.getMethod("getVolumeState", String.class);
				Object[] Volumes = (Object[]) getVolumeList.invoke(manager, null);
				String state = null;
				String path = null;
				enumResult = new ArrayList<String>();

				for (Object volume : Volumes) {
					/************** StorageVolume的方法 ***********************/
					Method getPath = volume.getClass().getMethod("getPath", null);
					path = (String) getPath.invoke(volume, null);
					state = (String) getVolumeState.invoke(manager, getPath.invoke(volume, null));
					if (null != path && null != state && state.equals(Environment.MEDIA_MOUNTED)) {
						enumResult.add(path);
					}
				}
				return enumResult;
			} catch (Exception e) {
			}
		}
		
		{
			// 得到存储卡路径
			File sdDir = null;
			boolean sdCardExist = Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED); // 判断sd卡
			// 或可存储空间是否存在
			if (sdCardExist) {
				enumResult = new ArrayList<String>();
				sdDir = Environment.getExternalStorageDirectory();// 获取sd卡或可存储空间的跟目录
				enumResult.add(sdDir.toString() );
				return enumResult;
			}
			return null;
		}
	}
	
	/**
	 * 2013.08.17
	 * 增加获取某一路径的可用空间大小的方法，返回已经格式化的大小文本
	 * 
	 * @param path
	 * @return 当获取磁盘空间异常的话，会返回null
	 */
	public static String getAvailaleSize(String path){

		long size = availableDiskSpace(path);

		return getFormatSize(size);

	}
	
	/**
     * 获取磁盘空间，path为绝对路径
     * @param path
     * @return -1：表示未能正确获取到磁盘空间的大小
     */
    public static long availableDiskSpace(String path)
    {
    	/**
         * stat.getBlockSize()的返回值是int型，
         * stat.getAvailableBlocks()的返回值也是int型，
         * 如果stat.getBlockSize() * (stat.getAvailableBlocks()直接相乘，结果可能溢出，变成负值。
         */
//        long availableCount = stat.getBlockSize() * (stat.getAvailableBlocks());
    	try{
	        StatFs stat = new StatFs(path);
	        long size = stat.getBlockSize();
	        long blocks = stat.getAvailableBlocks();
	        long availableCount = size * blocks;
	        return availableCount;
    	}catch (Exception e) {
			e.printStackTrace();
			return -1;
		}
    }
    
    /**
	 * 2013.08.17
	 * 增加格式化磁盘空间大小的方法
	 * 
	 * @param size
	 * @return 当size == -1 的时候，会返回null
	 */
	public static String getFormatSize(long size){
		if(size == -1)
		{
			return "";
		}
		if(size >= 1 * 1024 * 1024 * 1024) {
			return String.format("%1$.2f", size/(1024 * 1024 * 1024 * 1.0f) ) + "GB";
		}
		else if(size >= 1 * 1024 * 1024 ) {
			return String.format("%1$.2f", size/(1024 * 1024 * 1.0f)) + "MB";
		}
		else if(size >= 1 * 1024 ) {
			return String.format("%1$.2f", size/(1024 * 1.0f) ) + "KB";
		}
		else {
			return String.format("%1$.2f", size/1.0f) + "B";
		}
	}
	
	/** 
     * 判断GPS是否开启
     * @param context 
     * @return true 表示开启 
     */  
    public static boolean isOpenGps(Context context) {  
        
        if(context == null) return false;
        
        android.location.LocationManager locationManager   
                                 = (android.location.LocationManager) context.getSystemService(Context.LOCATION_SERVICE);  
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

package com.saiwei.recorder;

import java.io.IOException;

import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.media.MediaRecorder.OnErrorListener;
import android.media.MediaRecorder.OnInfoListener;
import android.util.Log;
import android.view.SurfaceHolder;

/**
 * 对 MediaRecorder 进行一层包装 
 * @author wei.chen
 *
 */
public class MyMediaRecorder  {
	
	private MediaRecorder mMediaRecorder ;
	private Camera mCameraDevice = null;
	
	/**
	 * 
	 */
	public MyMediaRecorder(){
		mMediaRecorder = new MediaRecorder();
	}
	
	/**
	 * 
	 * @param camera
	 */
	public MyMediaRecorder(Camera camera){
		mCameraDevice = camera;
		mMediaRecorder = new MediaRecorder();
	}
	
	/**
	 * 初始化
	 */
	public void initializeRecorder(
			Camera camera,
			SurfaceHolder surfaceHolder ,
			CamcorderProfile profile,
			String outputfile,
			int max_duration_ms,
			long maxFileSize ,
			int rotation,
			OnErrorListener errorlistener,
			OnInfoListener infolistener
			){
		
//		Tool.writeLog(
//				"initializeRecorder()", 
//				"开始录制： outputfile="+outputfile
//				+" ,max_duration_ms="+max_duration_ms
//				+" ,maxFileSize="+maxFileSize
//				+" ,rotation="+rotation
//				+" ,profile.videoFrameWidth="+profile.videoFrameWidth
//				+" ,profile.videoFrameHeight="+profile.videoFrameHeight);
//		
//        Log.i(TAG, "initializeRecorder()");
		
        // Unlock the camera object before passing it to media recorder.
		camera.unlock();
        mMediaRecorder.setCamera(camera);
		
		mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
        
        //  mProfile.
        mMediaRecorder.setProfile(profile);
        mMediaRecorder.setVideoSize(profile.videoFrameWidth,profile.videoFrameHeight);
        
        mMediaRecorder.setOutputFile(outputfile);
        mMediaRecorder.setPreviewDisplay(surfaceHolder.getSurface());

        
        //set maxduration   
        mMediaRecorder.setMaxDuration(max_duration_ms);
        	
        // Set maximum file size.
        try {
            mMediaRecorder.setMaxFileSize(maxFileSize);
        } catch (RuntimeException exception) {
        	Log.i(TAG, "RuntimeException  mMediaRecorder.setMaxFileSize(maxFileSize); ");
        }
        
        /*
         * 使得，播放视频能正常，不会相差90度
         */
        mMediaRecorder.setOrientationHint(rotation);
        
        try {
            mMediaRecorder.prepare();
        } catch (IOException e) {
        	
        	Log.i(TAG, "IOException  mMediaRecorder.prepare(); ");
        	
            releaseMediaRecorder();
            throw new RuntimeException(e);
        }
		
		mMediaRecorder.setOnErrorListener(errorlistener);
        mMediaRecorder.setOnInfoListener(infolistener);
	}
	
	private static final String TAG = "chenwei.MyMediaRecorder";
	
	/**
	 * 开始录像
	 */
	public void start(){
		if(mMediaRecorder != null){
			mMediaRecorder.start();
		}
	}
	
	/**
	 * 停止录像
	 */
	public void stop() throws RuntimeException{
		if(mMediaRecorder != null){
			mMediaRecorder.stop();
		}
		
	}
	
	
	public void reset(){
		if(mMediaRecorder != null){
			mMediaRecorder.reset();
		}
	}
	
	public void release(){
		if(mMediaRecorder != null){
			mMediaRecorder.release();
		}
	}
	
	/**
	 * 停止录像
	 */
	public void stopVideoRecording() throws RuntimeException{
		
		if(mMediaRecorder != null){
			mMediaRecorder.setOnErrorListener(null);
	    	mMediaRecorder.setOnInfoListener(null);
	    	
	    	//将Exception 抛出
	    	mMediaRecorder.stop();
		}
	}

	public void setOnErrorListener(OnErrorListener listener){
		if(mMediaRecorder != null){
			mMediaRecorder.setOnErrorListener(listener);
		}
	}
	
	public void setOnInfoListener(OnInfoListener listener){
		
		if(mMediaRecorder != null){
			mMediaRecorder.setOnInfoListener(listener);
		}
	}
	
	private void releaseMediaRecorder() {
        if (mMediaRecorder != null) {
            mMediaRecorder.reset();
            mMediaRecorder.release();
            mMediaRecorder = null;
        }
    }
}

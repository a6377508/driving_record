package com.autonavi.xmgd.utility;

import java.io.File;


import android.os.Environment;
import android.os.StatFs;

public class Storage {
	private static final String TAG = "CameraStorage";
	
    public static final String DCIM =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).toString();

    public static final String DEFAULT_DIRECTORY = DCIM + "/Camera/Navi";
    
    public static final long UNAVAILABLE = -1L;
    public static final long PREPARING = -2L;
    public static final long UNKNOWN_SIZE = -3L;
    public static final long LOW_STORAGE_THRESHOLD= 50000000;
    
    public static long getAvailableSpace() {
        String state = Environment.getExternalStorageState();
        Logutil.i(TAG, "External storage state=" + state);
        if (Environment.MEDIA_CHECKING.equals(state)) {
            return PREPARING;
        }
        if (!Environment.MEDIA_MOUNTED.equals(state)) {
            return UNAVAILABLE;
        }

        File dir = new File(DEFAULT_DIRECTORY);
        dir.mkdirs();
        if (!dir.isDirectory() || !dir.canWrite()) {
            return UNAVAILABLE;
        }

        try {
            StatFs stat = new StatFs(DEFAULT_DIRECTORY);
            return stat.getAvailableBlocks() * (long) stat.getBlockSize();
        } catch (Exception e) {
            Logutil.e(TAG, "Fail to access external storage", e);
        }
        return UNKNOWN_SIZE;
    }
}

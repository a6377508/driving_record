package com.autonavi.xmgd.utility;

import java.io.File;
import java.util.Comparator;

/**
 * 比较视频文件最后修改的时间
 * @author wei.chen
 *
 */
public class FileComparator implements Comparator<File> {

	@Override
	public int compare(File o1, File o2) {
		long time1 = o1.lastModified();
		long time2 = o2.lastModified();	
		return (time1 > time2 ? -1 : (time1 == time1 ? 0 : 1));
	}
}

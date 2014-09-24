package com.saiwei.recorder;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.format.DateFormat;
import android.text.format.Formatter;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnCreateContextMenuListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.autonavi.xmgd.utility.FileComparator;
import com.autonavi.xmgd.utility.Logutil;
import com.autonavi.xmgd.view.GDTitle;
import com.saiwei.recorder.application.Recorder_Global;

/**
 * 
 * 视频文件管理器
 * 
 * @author wei.chen
 *
 */
public class VideoFileManager extends Activity {
	
	private static final String TAG = "chenwei.VideoFileManager";
	
	private Context mContext;
	
	private ListView mListView;
	private MyAdapter mAdapter;
	private ArrayList<File> mListFiles = null;
	
	private String fileHeadName = ""; 
	
	/**当前路径 */
	private TextView tvCurPath ;
	
	
	/**
	 * 文件夹路径
	 */
	private String mFileDir;
	
	private Handler mHandler = new Handler(){

		@Override
		public void handleMessage(Message msg) {
			
		}
	};
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.video_file_manager);
		mContext = this;
		init();
	}
	
	/**
	 * 初始化
	 */
	private void init() {
		GDTitle title = (GDTitle) findViewById(R.id.title_simplelist);
		title.setText("录像");
		
		tvCurPath = (TextView) this.findViewById(R.id.file_manager_currentfilepath);
		tvCurPath.setText("当前路径："+Recorder_Global.mCurFilePath);
		
		mListView = (ListView) findViewById(R.id.list_listactivity);
		mAdapter = new MyAdapter(mContext);
		mListView.setAdapter(mAdapter);
		mListView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				File file = getFile(mFileDir, mListFiles.get(position).getName());
				Logutil.i(TAG, "file="+file);
				openFile(file);
			}
		});
		
		mListView.setOnCreateContextMenuListener(new OnCreateContextMenuListener() {
			
			@Override
			public void onCreateContextMenu(ContextMenu menu, View v,
					ContextMenuInfo menuInfo) {
				
				menu.setHeaderTitle("提示");
//				menu.setHeaderIcon(it.getIcon());
				menu.add(0, MENU_DELETE, 0, "删除");
				
			}
		});
//		mFileDir = Storage.DIRECTORY;
		
		mFileDir = Recorder_Global.mCurFilePath;
		
		fileHeadName = getResources().getString(R.string.video_file_name_format).substring(1, 5);
		
		Logutil.i(TAG, "fileHeadName="+fileHeadName);
		
		scannerFile(mFileDir);
	}
	
	private static final int DIALOG_DELETE = 2;
	
	private File mContextFile = null ;
	
	@Override
	protected Dialog onCreateDialog(int id) {
		
		switch (id) {
		case DIALOG_DELETE:
			return new AlertDialog.Builder(this).setTitle("是否删除 ？")
            	.setIcon(android.R.drawable.ic_dialog_alert).setPositiveButton(
					android.R.string.ok, new OnClickListener() {
						
						@Override
						public void onClick(DialogInterface dialog, int which) {
							deleteFile(mContextFile);
						}
						
					}).setNegativeButton(android.R.string.cancel, new OnClickListener() {
						
						@Override
						public void onClick(DialogInterface dialog, int which) {
						}
						
					}).create();

		default:
			return super.onCreateDialog(id);
		}
	}
	
	private static final int MENU_DELETE = Menu.FIRST + 5;
	
	/**
	 * 删除文件
	 * @param file
	 */
	private void deleteFile(File file) {
		if(file.exists()){
			file.delete();
		}
		
		refreshList();
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		
		AdapterContextMenuInfo menuInfo = (AdapterContextMenuInfo) item
				.getMenuInfo();
		
		
		if (mAdapter == null) {
	      	  return false;
	        }
	        
		mContextFile = (File) mAdapter.getItem(menuInfo.position);
		
		switch (item.getItemId()) {
		case MENU_DELETE:
			showDialog(DIALOG_DELETE);
			return true;
		}
		
		return false;
	}
	
	
	/**
	 * 浏览文件
	 */
	private void scannerFile(String filedir){
		
		Logutil.i(TAG, "scannerFile()");
		
		File currentDirectory  = new File(filedir);
		
		if(!currentDirectory.exists()){
			return;
		}
		
//		File[] files = currentDirectory.listFiles();
		
		File[] files = currentDirectory.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String filename) {
				
				return filename.startsWith(fileHeadName);
			}
		});		
	
		mListFiles = new ArrayList<File>(Arrays.asList(files));
	
		//自动删除0.0B大小的文件
		for(int i=0;i<mListFiles.size() ;i++){
			File file = mListFiles.get(i);
			if(file.length() == 0){
				mListFiles.remove(i);
			}
		}
		
		//按时间排序
		Collections.sort(mListFiles, new FileComparator());	
		
		mAdapter.setList(mListFiles);
	}
	
	/**
	 * 打开视频文件
	 * @param aFile
	 */
	private void openFile(File aFile) {
		if (!aFile.exists()) {
			Toast.makeText(this, "文件不存在！", Toast.LENGTH_SHORT).show();
			return;
		}
		
		Intent intent = new Intent(android.content.Intent.ACTION_VIEW);

        Uri data = Uri.fromFile(aFile);
        String type = "video/mp4";
        intent.setDataAndType(data, type);
        
		try {
			startActivity(intent);
		} catch (ActivityNotFoundException e) {
			Toast.makeText(this, "应用程序不可用", Toast.LENGTH_SHORT).show();
		}
	}
	
	public File getFile(String curdir, String file) {
		String separator = "/";
		  if (curdir.endsWith("/")) {
			  separator = "";
		  }
		   File clickedFile = new File(curdir + separator
		                       + file);
		return clickedFile;
	}
	
	/**
	 * 刷新列表
	 */
	private void refreshList() {
		scannerFile(Recorder_Global.mCurFilePath);
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
	}
	
	/**
	 * 自定义适配器
	 * @author wei.chen
	 *
	 */
	private class MyAdapter extends BaseAdapter{

		private LayoutInflater mInflater;
		private ArrayList<File> mList = null;
		private Context mContext;
		
		private Formatter mFormatter ;
		
		
		public MyAdapter(Context context) {
			
			mContext = context;
			
			mFormatter = new Formatter();
			mInflater = LayoutInflater.from(context);
		}
		
		public void setList(ArrayList<File> list) {
			this.mList = list;
			notifyDataSetChanged();
		}

		@Override
		public int getCount() {
			if(mList != null){
				return mList.size();
			}
			return 0;
		}

		@Override
		public Object getItem(int position) {
			
			if(mList != null){
				return mList.get(position);
			}
			return null;
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			
			ViewHolder holder;
			
			if(convertView == null){
				convertView = mInflater.inflate(R.layout.file_manager_list_item, null);
				
				holder = new ViewHolder();
				
				holder.filename 
						= (TextView) convertView.findViewById(R.id.tv_file_name);
				holder.filesize 
						= (TextView) convertView.findViewById(R.id.tv_file_size);
				holder.filedate 
						=  (TextView) convertView.findViewById(R.id.tv_file_date);
							
				
				convertView.setTag(holder);
			} else {
				holder = (ViewHolder) convertView.getTag();
			}
			
			holder.filename.setText(mList.get(position).getName());
			
			long lfiledate = mList.get(position).lastModified();
			String datetime = DateFormat.getDateFormat(mContext).format(new Date(lfiledate));
			
			holder.filedate.setText(datetime);
			
			String sfilesize = mFormatter.formatFileSize(mContext, mList.get(position).length());
			holder.filesize.setText(sfilesize +" , ");
			
			return convertView;
		}
		
		class ViewHolder {
			ImageView icon;
			TextView filename;
			TextView filesize;
			TextView filedate;
		}
	}
}

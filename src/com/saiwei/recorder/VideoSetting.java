package com.saiwei.recorder;

import java.util.ArrayList;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.autonavi.xmgd.utility.Util;
import com.autonavi.xmgd.view.GDSystemConfigMenuItem;
import com.autonavi.xmgd.view.GDTitle;
import com.saiwei.recorder.application.MyApplication;
import com.saiwei.recorder.application.Recorder_Global;

/**
 * 行车记录仪设置界面
 * 
 * @author wei.chen
 *
 */
public class VideoSetting extends Activity {

	private static final String TAG = "chenwei.VideoSetting";
	private static final boolean DEBUG = true;
	private Context mContext;
	private SettingSimpleAdapter mAdapter;
	private ListView mlistView;
	private ArrayList<GDSystemConfigMenuItem> listItems = new ArrayList<GDSystemConfigMenuItem>();
	
	private static final int DLG_VIDEO_QUALITY = 2;	//视频质量
	private static final int DLG_VIDEO_RECORD_RATE = 3;	//视频录制时长
	private static final int DLG_VIDEO_FILE_PATH = 4;	//视频文件路径
	
	private GDSystemConfigMenuItem 
    				videoQuality,
    				videoRecordRate;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		mContext = this;		
		creatMenuItems();
		init();
	}
	
	@Override
	protected Dialog onCreateDialog(int id) {
		
		int checkitem;
		switch (id) {

		//视频质量
		case DLG_VIDEO_QUALITY:
			checkitem = MyApplication.cache_recoder_video.getInt(Recorder_Global.SHARE_PREFERENCE_VIDEO_QUALITY, Recorder_Global.DEFAULT_VIDEO_QUALITY);
			return new AlertDialog.Builder(this)
			.setTitle(getString(R.string.video_setting_quality))
				.setSingleChoiceItems(getResources().getStringArray(R.array.video_quality), checkitem,new  DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						MyApplication.cache_recoder_video.edit()
						.putInt(Recorder_Global.SHARE_PREFERENCE_VIDEO_QUALITY, which).commit();
            	
						updateView();
						
						dialog.dismiss();
					}
				})
				.create();
			
		//视频录制时长
		case DLG_VIDEO_RECORD_RATE:
			
			checkitem = MyApplication.cache_recoder_video.getInt(Recorder_Global.SHARE_PREFERENCE_VIDEO_RECORD_RATE, Recorder_Global.DEFAULT_VIDEO_RECORD_RATE);
			return new AlertDialog.Builder(this)
				.setTitle(getString(R.string.video_setting_record_rate))
				.setSingleChoiceItems(getVideoRateList(), checkitem,new  DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						MyApplication.cache_recoder_video.edit()
						.putInt(Recorder_Global.SHARE_PREFERENCE_VIDEO_RECORD_RATE, which).commit();
            	
						updateView();
						
						dialog.dismiss();
					}
				}).create();
			
			//视频文件路径
			case DLG_VIDEO_FILE_PATH:
				
				checkitem = MyApplication.cache_recoder_video
					.getInt(Recorder_Global.SHARE_PREFERENCE_VIDEO_FILE_PATH, 
							Recorder_Global.DEFAULT_VIDEO_FILE_PATH);
				
				final String[] pathDetails =getVideoFilePathDetail();
				
				
				return new AlertDialog.Builder(this)
					.setTitle(getString(R.string.video_setting_file_path))
					.setSingleChoiceItems(getVideoFilePath(), checkitem,new  DialogInterface.OnClickListener() {
						
						@Override
						public void onClick(DialogInterface dialog, int which) {
							MyApplication.cache_recoder_video.edit()
								.putInt(Recorder_Global.SHARE_PREFERENCE_VIDEO_FILE_PATH, which).commit();
	            	
							Recorder_Global.mCurFilePath = pathDetails[which]+"/DCIM/Camera/Navi";
							
							MyApplication.cache_recoder_video.edit()
								.putString(Recorder_Global.SHARE_PREFERENCE_VIDEO_FILE_PATH_DETAIL,Recorder_Global.mCurFilePath ).commit();
							
							Toast.makeText(VideoSetting.this, "当前文件路径："+Recorder_Global.mCurFilePath, Toast.LENGTH_SHORT).show();
							
							//创建文件夹 ，防止文件夹不存在
							Util.createFolder(Recorder_Global.mCurFilePath);
							
							updateView();
							
							dialog.dismiss();
						}
					}).create();
				
			
		default:
			break;
		}		
		return null;
	}
	
	/**
	 * 创建ListItem
	 */
	private void creatMenuItems() {
		
		GDSystemConfigMenuItem mi;
		int titleid;
		
		// 视频质量
		titleid = R.string.video_setting_quality;
		mi = new GDSystemConfigMenuItem(getString(titleid)) {

			@Override
			public void onItemClick() {
				showDialog(DLG_VIDEO_QUALITY);
			}

			@Override
			public void onItemClickInCarMode() {
				// TODO Auto-generated method stub
			}

			@Override
			public String getSecondTitle() {
				
				
				String[] strs = getResources().getStringArray(R.array.video_quality);
				int value = MyApplication.cache_recoder_video
						.getInt(Recorder_Global.SHARE_PREFERENCE_VIDEO_QUALITY, 
								Recorder_Global.DEFAULT_VIDEO_QUALITY);
				
				return strs[value];
			}
		};
		mi.setTitleId(titleid);
		mi.setIcon(Util.loadImage(this, R.drawable.ic_recorder_setting_video));
		listItems.add(mi);
		videoQuality = mi;
		
		// 视频录制时长
		titleid = R.string.video_setting_record_rate;
		mi = new GDSystemConfigMenuItem(getString(titleid)) {

			@Override
			public void onItemClick() {
				showDialog(DLG_VIDEO_RECORD_RATE);
			}

			@Override
			public void onItemClickInCarMode() {
			}

			@Override
			public String getSecondTitle() {
				
				String[] strs = getResources().getStringArray(R.array.video_record_rate);
				int value = MyApplication.cache_recoder_video
						.getInt(Recorder_Global.SHARE_PREFERENCE_VIDEO_RECORD_RATE, 
								Recorder_Global.DEFAULT_VIDEO_RECORD_RATE);
				return strs[value] + " " + mContext.getResources().getString(R.string.navigator_minute);
			}
		};
		mi.setTitleId(titleid);
		mi.setIcon(Util.loadImage(this, R.drawable.ic_recorder_setting_time));
		listItems.add(mi);
		videoRecordRate = mi;
		
		// 视频文件路径
		titleid = R.string.video_setting_file_path;
		mi = new GDSystemConfigMenuItem(getString(titleid)) {

			@Override
			public void onItemClick() {
				showDialog(DLG_VIDEO_FILE_PATH);
			}

			@Override
			public void onItemClickInCarMode() {
			}

			@Override
			public String getSecondTitle() {
				
				String[] strs = getResources().getStringArray(R.array.video_record_file_path);
				int value = MyApplication.cache_recoder_video
						.getInt(Recorder_Global.SHARE_PREFERENCE_VIDEO_FILE_PATH, 
								Recorder_Global.DEFAULT_VIDEO_FILE_PATH);
				if(isExistSelectPath(value)){
					return strs[value];
				} else {
					
					MyApplication.setDefaultFilePath(MyApplication.cache_recoder_video);
					
					return strs[0];
				}
			}
		};
		mi.setTitleId(titleid);
		mi.setIcon(Util.loadImage(this, R.drawable.ic_recorder_setting_time));
		listItems.add(mi);
		videoRecordRate = mi;
		
		
		// 版本号
//		titleid = R.string.video_setting_close_car_recorder;
//		mi = new GDSystemConfigMenuItem("查看版本号") {
//
//			@Override
//			public void onItemClick() {
//				Toast.makeText(mContext, "版本号 : "+Util.getVersion(mContext), Toast.LENGTH_SHORT).show();
//			}
//
//			@Override
//			public void onItemClickInCarMode() {
//			}
//
//			@Override
//			public String getSecondTitle() {
//				return null;
//			}
//		};
//		mi.setTitleId(titleid);
//		mi.setIcon(Tool.loadImage(mContext, R.drawable.ic_setting_font));
//		listItems.add(mi);
	}
	
	/**
	 * 初始化
	 */
	private void init() {
		setContentView(R.layout.simplelist_activity);
		GDTitle title = (GDTitle) findViewById(R.id.title_simplelist);
		title.setText(R.string.video_setting);		
		
		mlistView = (ListView) findViewById(R.id.list_listactivity);
		mAdapter = new SettingSimpleAdapter(this);
		mlistView.setAdapter(mAdapter);
		mlistView.setFastScrollEnabled(true);
		mlistView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				listItems.get(position).onItemClick();
			}
		});
	}

	@Override
	protected void onResume() {
		super.onResume();
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
	}
	
	private ArrayList<String> paths;
	
	/**
	 * 判断所选择的路径是否存在
	 * @param selectid
	 * @return
	 */
	private boolean isExistSelectPath(int selectid){
		paths = Util.enumExternalStroragePath(this);
		if(paths!=null && paths.size()>0){
			try {
				paths.get(selectid);
				return true;
			} catch (Exception e) {
				return false;
			}
		}
		return false;
	}
	
	/**
	 * 适配器
	 * 
	 * @author wei.chen
	 */
	public class SettingSimpleAdapter extends BaseAdapter {

		private LayoutInflater mInflater;

		public SettingSimpleAdapter(Context context) {
			mInflater = LayoutInflater.from(context);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
		
			ViewHolder1 holder1;
			if (convertView == null) {
				convertView = mInflater.inflate(
						R.layout.system_config_item, null);

				holder1 = new ViewHolder1();
				holder1.text = (TextView) convertView
						.findViewById(R.id.item_text);
				holder1.icon1 = (ImageView) convertView
						.findViewById(R.id.item_icon1);
				holder1.value = (TextView) convertView
						.findViewById(R.id.item_value);
				holder1.icon2 = (ImageView) convertView
						.findViewById(R.id.item_icon2);
				convertView.setTag(holder1);

			} else {
				holder1 = (ViewHolder1) convertView.getTag();
			}

			holder1.text.setText(listItems.get(position).getTitle());
			holder1.icon1.setImageBitmap(listItems.get(position).getIcon());
			holder1.value.setText(listItems.get(position).getSecondTitle());
			holder1.icon2.setImageResource(R.drawable.list_image);

			holder1.text.setEnabled(listItems.get(position).isEnabled());
			holder1.icon1.setEnabled(listItems.get(position).isEnabled());
			holder1.value.setEnabled(listItems.get(position).isEnabled());
			holder1.icon2.setEnabled(listItems.get(position).isEnabled());
			convertView.setEnabled(listItems.get(position).isEnabled());
			return convertView;
		}

		class ViewHolder1 {
			ImageView icon1;
			TextView text;
			TextView value;
			ImageView icon2;
		}

		@Override
		public int getCount() {
			return listItems.size();// itemString.length;
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public Object getItem(int position) {
			return position;
		}
	}

	/**
	 * 更新界面
	 */
	private void updateView(){
		mAdapter.notifyDataSetChanged();
	}
	
	/**
	 * 获取录像周期列表显示
	 * @return
	 */
	public String[] getVideoRateList(){
    	String[] strs = getResources().getStringArray(R.array.video_record_rate);
    	for(int i = 0 ; i < strs.length ; i++){
    		strs[i] = strs[i] + " " +getResources().getString(R.string.navigator_minute);
    	}
    	return strs;
	}
	
	/**
	 * 获取视频文件路径
	 * @return
	 */
	public String[] getVideoFilePath(){
		String[] strs = getResources().getStringArray(R.array.video_record_file_path);
		ArrayList<String> paths = Util.enumExternalStroragePath(this);
		if(paths != null ){
			if(paths.size() >= 2){
				return strs;
			} else {
				return new String[]{
					strs[0]	
				};
			}
		}
		
		return new String[]{
				strs[0]	
			};
	}
	
	/**
	 * 
	 * @return
	 */
	public String[] getVideoFilePathDetail(){
		ArrayList<String> paths = Util.enumExternalStroragePath(this);
		return paths.toArray(new String[paths.size()]);
	}
}

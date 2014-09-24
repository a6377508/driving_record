package com.autonavi.xmgd.view;

import android.app.Activity;
import android.content.Context;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;

import com.saiwei.recorder.R;


/**
 * 标题栏控件(包含左右按钮以及中间标题文本)
 * @author yangyang.qian
 *
 */
public class GDTitle extends FrameLayout {
	
	/** 左边按钮*/
	private ImageButton mRight;
	/** 右边按钮*/
	private ImageButton mLeft;
	/** 文本*/
	private TextView mText;
	
	public GDTitle(Context context) {
		this(context, null);
		init(context);
	}

	public GDTitle(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
		init(context);
	}

	public GDTitle(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(context);
	}
	
	private void init(final Context context) {
		LayoutInflater mInflater = LayoutInflater.from(context);
		View v = mInflater.inflate(R.layout.gdtitle, null);
		
		mLeft = (ImageButton)v.findViewById(R.id.gdtitle_left);
		mRight = (ImageButton)v.findViewById(R.id.gdtitle_right);
		mText = (TextView)v.findViewById(R.id.gdtitle_text);
		
		mLeft.setFocusable(false);
		
		/**
		 * 把右边按钮的点击事件直接转化成系统返回按钮的点击事件
		 */
		mLeft.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				KeyEvent event = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BACK);
				((Activity)context).dispatchKeyEvent(event);
				event = new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BACK);
				((Activity)context).dispatchKeyEvent(event);
			}	
		});
		
		
		addView(v);
	}
	
	public TextView getTitleView() {
		return mText;
	}
	
	public ImageButton getLeftView() {
		return mLeft;
	}

	public ImageButton getRightView() {
		return mRight;
	}
	
	public void setText(String string)
	{
		mText.setText(string);
	}
	
	public void setText(int resid) 
	{
		mText.setText(resid);	
	}
}

package com.autonavi.xmgd.view;

import android.graphics.Bitmap;

/**
 * 通用列表项(包含图标+文本),一般与GDListAdapter一起使用
 * @author yangyang.qian
 *
 */
public abstract class GDMenuItem 
{
	public abstract void onItemClick();    // 此方法外部继承后一定要实现(点击响应)
	public void onItemLongClick(){}
	
	public GDMenuItem()
	{
		enabled = true;
	}
	
	public GDMenuItem(String title)
	{
		enabled = true;
		this.title = title;
	}
	
	public void setTitleId(int resid)
	{
		titleid = resid;
	}
	
	public void setTitle(String title)
	{
		this.title = title;
	}
	
	public void setIcon(Bitmap icon)
	{
		this.icon = icon;
	}
	
	public void setIconId(int resid)
	{
		iconid = resid;
	}
	
	public void setTag(int tag)
	{
		this.tag = tag;
	}
	
	public void setEnabled(boolean enable)
	{
		enabled = enable;
	}
	
	public boolean isEnabled()
	{
		return enabled;
	}
	
	
	public int getTitleId()
	{
		return titleid;
	}
	
	public String getTitle()
	{
		return title;
	}
	
	public Bitmap getIcon()
	{
		return icon;
	}
	
	public int getIconId()
	{
		return iconid;
	}
	
	public int getTag()
	{
		return tag;
	}
	
	public void setObject(Object o)
	{
		obj = o;
	}
	
	public Object getObject()
	{
		return obj;
	}

	
	private String title;
	private Bitmap icon;
	private int iconid;
	private int titleid;
	private boolean enabled;
	protected int tag;
	
	protected Object obj;
	
}

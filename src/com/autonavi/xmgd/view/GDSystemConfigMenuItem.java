package com.autonavi.xmgd.view;

/**
 * 扩展的列表项(系统设置中使用,多了右边一行文本)
 * @author yangyang.qian
 *
 */
public abstract class GDSystemConfigMenuItem extends GDMenuItem {
	
	public GDSystemConfigMenuItem(String title)	{
		super(title);
	}
	
	public GDSystemConfigMenuItem() {
	}
	
	abstract public String getSecondTitle();
	
	abstract public void onItemClickInCarMode();
}

package com.autonavi.xmgd.view;


import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.FontMetrics;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.AbsoluteLayout;
import android.widget.TextView;

/**
 * �����ı���: GDScrollText
 * 
 * @author yangyang.qian
 */

public class GDScrollText extends AbsoluteLayout
{
	private TextView tv;   // ��ʾ�ı���textView
	
	private Context context;   // ��ǰ�ؼ�������context
	
	private int startOffset;   // ƫ��
	
	public GDScrollText(Context context) {
		this(context, null);
	}
	
	public GDScrollText(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}
	
	public GDScrollText(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(context);
	}
	
	private void init(Context c)
	{
		context = c;
		startOffset = 518;
		
		tv = new TextView(context);
		tv.setTextSize(28.0f);    // ����Ĭ�ϴ�С28
		tv.setTextColor(Color.WHITE);
		tv.setSingleLine(true);

		addView(tv);
		
	}
	
	/**
	 * �ڲ�ʹ�õ������ı�����_setText,�����ı��Ƿ�����Լ���������
	 * @param text
	 */
	private void _setText(String text)
	{
		stopAnimation();
		tv.setText(text);
		
		if(text!=null)
		{
			final View target = tv;
	        final View targetParent = (View) target.getParent();
	        
	        int parentw = targetParent.getWidth();
	        int parenth = targetParent.getHeight();
	        int padleft = targetParent.getPaddingLeft();
	        int padright = targetParent.getPaddingRight();
	        int padtop = targetParent.getPaddingTop();
	        int padbottom = targetParent.getPaddingBottom();
	        
			int textWidth = (int)(tv.getPaint().measureText(getText().toString()));
			int textHeight = getFontHeight(getTextSize() )*4/3;
			
			// �ı����ȳ������ĳ���ʱ
			if(textWidth>parentw-padleft-padright)
			{
				tv.setLayoutParams(new AbsoluteLayout.LayoutParams(textWidth,textHeight,0,parenth-padtop-padbottom-textHeight>>1));
				
				// ���ö���
				Animation a = new TranslateAnimation(0.0f,
		                parentw - textWidth - padleft - padright,
		                0.0f, 0.0f);
		        a.setDuration(calDuration(textWidth-parentw+padleft+padright));
		        a.setStartOffset(startOffset);
		        a.setRepeatMode(Animation.REVERSE);
		        a.setRepeatCount(Animation.INFINITE);
		        
		        target.startAnimation(a);
			}
			else
			{
				tv.setLayoutParams(new AbsoluteLayout.LayoutParams(textWidth,textHeight,parentw-padleft-padright-textWidth>>1,parenth-padtop-padbottom-textHeight>>1));
			}
		}
	} 
	
	public int getFontHeight(float fontSize)  
    {  
        Paint paint = new Paint();  
        paint.setTextSize(fontSize);  
        FontMetrics fm = paint.getFontMetrics();  
        return (int)Math.ceil(fm.descent - fm.ascent);  
    }  
	
	/**
	 * ����ʱ����
	 * @param movedis
	 * @return ����ʱ����
	 */
	private int calDuration(int movedis)
	{
		return Math.max(movedis*88,518);
	}
	
	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) 
	{
		super.onSizeChanged(w, h, oldw, oldh);
		_setText(tv.getText().toString());
	}
	
	/**
	 * ����ƫ��
	 * @param startoffset
	 */
	public void setStartOffset(int startoffset)
	{
		this.startOffset = startoffset;
	}
	
	/**
	 * ��ȡ�ı�
	 * @return
	 */
	public String getText()
	{
		return tv.getText().toString();
	}
	
	/**
	 * ���� �����ı�
	 * @param text
	 * ��ͬ���ı���Ҫ�ٴ����ã����⶯�������á�
	 */
	public void setText(String text)
	{
		String oldtext = (String)tv.getText();
		if(text!=null&&oldtext!=null&&oldtext.compareTo(text)==0)
		{
			
		}
		else
		{
			_setText(text);
		}
	}
	
	public float getTextSize()
	{
		return tv.getTextSize();
	}
	
	public void setTextSize(float size)
	{
		tv.setTextSize(size);
	}
	
	public int getTextColor()
	{
		return tv.getTextColors().getDefaultColor();
	}
	
	public void setTextColor(int color)
	{
		tv.setTextColor(color);
	}
	
	public void setShadowLayer(float radius, float dx, float dy, int color)
	{
		tv.setShadowLayer(radius, dx, dy, color);
	}
	
	private void startAnimation()
	{
		
	}
	
	private void stopAnimation()
	{		
		tv.setAnimation(null);
		this.setAnimation(null);
	}

}





















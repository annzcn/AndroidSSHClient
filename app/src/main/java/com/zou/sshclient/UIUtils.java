package com.zou.sshclient;
import android.content.Context;
import android.text.SpannableStringBuilder;
import android.util.TypedValue;
import android.view.WindowManager;

import java.util.ArrayList;

/**
 * 与UI界面相关的工具类
 *
 * @author Nielev
 * @company Oray
 * @version 2016年2月24日  下午5:44:29
 */
public class UIUtils {

	
    /**
     *  根据Unicode编码完美的判断中文汉字和符号
     * @param c
     * @return
     */
    public static boolean isChinese(char c) {
        Character.UnicodeBlock ub = Character.UnicodeBlock.of(c);
        if (ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS || ub == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS
                || ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A || ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B
                || ub == Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION || ub == Character.UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS
                || ub == Character.UnicodeBlock.GENERAL_PUNCTUATION) {
            return true;
        }
        return false;
    }
	
	public static String charsToString(char[] chars){
		StringBuffer buffer = new StringBuffer();
		for (char c : chars) {
			buffer.append(c);
		}
		return buffer.toString();
	}
	
	/**
	 * dp转px
	 * @param dp
	 * @param context
	 * @return
	 */
    public static int dp2px(int dp,Context context) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp,
        		context.getResources().getDisplayMetrics());
    }
    /**
     * 获取屏幕高度
     * @param context
     * @return
     */
    public static int getScreenHeight(Context context){
    	WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
    	return wm.getDefaultDisplay().getHeight();
    }
    /**
     * 获取屏幕宽度
     * @param context
     * @return
     */
    public static int getScreenWidth(Context context){
    	WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
    	return wm.getDefaultDisplay().getWidth();
    }

	public static boolean containBuilder(ArrayList<SpannableStringBuilder> builders, SpannableStringBuilder builder){
		for(int i=0;i<builders.size();i++){
			if(builder.toString().equals(builders.get(i).toString())){
				return true;
			}
		}
		return false;
	}
}
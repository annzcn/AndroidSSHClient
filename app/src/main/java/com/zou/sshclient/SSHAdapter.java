package com.zou.sshclient;

import java.util.ArrayList;
import android.content.Context;
import android.text.SpannableStringBuilder;
import android.text.TextPaint;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class SSHAdapter extends BaseAdapter{
	private static final String TAG = null;
	private ArrayList<SpannableStringBuilder> strs;
	private Context context;
	private TextPaint mPaint;
	private float lineHeight;
	public SSHAdapter(ArrayList<SpannableStringBuilder> strs,Context context){
		this.strs =strs;
		this.context = context;
	}
	
	@Override
	public int getCount() {
		return strs.size();
	}

	@Override
	public Object getItem(int position) {
		return strs.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}
	
	public TextPaint getPaint(){
		return mPaint;
	}
	
	public float getLineHeight(){
		return lineHeight;
	}
	
	public void setArray(ArrayList<SpannableStringBuilder> strs){
		this.strs =strs ;
	}
	
	public ArrayList<SpannableStringBuilder> getArray(){
		return this.strs;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		TextView tv = null;
		SpannableStringBuilder builder = strs.get(position);
		if(convertView ==null){
			convertView = View.inflate(context, R.layout.item_sshview, null);
			tv = (TextView) convertView.findViewById(R.id.tv_ssh);
			convertView.setTag(tv);
		}else{
			tv = (TextView) convertView.getTag();
		}

		for(int i=0;i<builder.length();i++){
			char ch = builder.charAt(i);
			if(UIUtils.isChinese(ch)&&i<=builder.length()-3&&' '==builder.charAt(i+1)){
				builder.delete(i+1, i+2);
			}
		}

		tv.setText(builder);
		mPaint = tv.getPaint();
		convertView.measure(0, 0);
		lineHeight = convertView.getMeasuredHeight();
		return convertView;
	}
}

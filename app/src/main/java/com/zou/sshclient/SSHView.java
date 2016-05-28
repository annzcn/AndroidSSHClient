package com.zou.sshclient;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Message;

import android.text.Spannable;
import android.text.SpannableStringBuilder;

import android.text.TextPaint;

import android.text.style.ForegroundColorSpan;

import android.util.AttributeSet;

import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import android.view.inputmethod.BaseInputConnection;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;

import com.zou.sshclient.terminal.VDUBuffer;
import com.zou.sshclient.terminal.VDUDisplay;
import com.zou.sshclient.terminal.vt320;

/**
 * Created by zou on 2016/3/8.
 */
@SuppressLint("NewApi")
public class SSHView extends FrameLayout implements VDUDisplay {
	private static final String TAG = "SSHView";
	private char[][] disPlayChars;
	private CursorView cursorView;
	public float charWidthEN, charWidthCN, charHeight;
	private OnInputTextListener onInputTextListener;
	public vt320 buffer;
	private int[] color;
	public Paint defaultPaint;
	public int lineCount;
	public static final int WIDTH_COUNT = 40;
	public static final int HEIGHT_COUNT = 23;
	public static final int CHAR_SIZE_EN = 14;
	public static final int CHAR_SIZE_CN = 14;
	public static final int MAX_NOMAL_LINE = 98;
	public int defaultFg = 7;
	public int defaultBg = 0;
	public boolean isConnected;
	public StringBuffer username = new StringBuffer();
	public StringBuffer password = new StringBuffer();
	private String displayusername;
	private String displaypassword;
	private Handler handler;
	protected static final int MSG_DISPLAY_USERNAME = 1;
	protected static final int MSG_REFRESH_LIST = 0;

	private DisplayListView displayListView;
	private SSHAdapter sshAdapter;
	private ArrayList<SpannableStringBuilder> nomalDisplayString;

	private ArrayList<SpannableStringBuilder> viDisplayString;
	
	private ArrayList<SpannableStringBuilder> viSaveString;

	private ArrayList<SpannableStringBuilder> quitVIDisplayString;

	private ArrayList<SpannableStringBuilder> saveDisplayString;

	private boolean isVIMode = false;// 标识进入VI的状态

	private boolean isQuitVI = false;// 标识退出VI的状态

	private String viFilePath;

	private Timer timer;
	private TimerTask task;

	private long fristRefreshTime;
	private Runnable runnable;
	private int saveCount;
	
	private int aboveMaxLineSaveCount;
	
	private CharsetDecoder decoder;
	
	private StringBuffer srcBuffer = new StringBuffer();//维护一份原始数据
	
	public static int BUFFER_SIZE = 4096;
	

	public SSHView(Context context) {
		super(context);
		init();
	}

	public SSHView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	public SSHView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		init();
	}

	private void init() {
		handler = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				switch (msg.what) {
				case MSG_DISPLAY_USERNAME:
					performClick();
					break;
				case MSG_REFRESH_LIST:
					refresh();
					break;
				}

				super.handleMessage(msg);
			}
		};

		color = Colors.defaults;
		buffer = new vt320() {
			@Override
			public void debug(String notice) {

			}

			@Override
			public void write(byte[] b) {
				if (onInputTextListener != null) {
					onInputTextListener.onInputText(new String(b));
				}
			}

			@Override
			public void write(int b) {
				if (onInputTextListener != null) {
					onInputTextListener.onInputText(String.valueOf((char) b));
				}
			}
		};
		buffer.setDisplay(this);
		buffer.setScreenSize(WIDTH_COUNT, HEIGHT_COUNT, true);
		defaultPaint = new Paint();
		defaultPaint.setAntiAlias(true);
		defaultPaint.setTypeface(Typeface.MONOSPACE);
		defaultPaint.setFakeBoldText(true);

		setFocusable(true);
		setFocusableInTouchMode(true);
		requestFocus();

		// 添加textview的父容器
		// addLinearLayout();
		addListView();
		// 添加光标View
		addCursorView();
	}

	//
	// @Override
	// protected void onDraw(Canvas canvas) {
	// int fg, bg;
	// synchronized (buffer) {
	// boolean entireDirty = buffer.update[0];
	// boolean isWideCharacter = false;
	//
	// // walk through all lines in the buffer
	// for (int l = 0; l < buffer.height; l++) {
	//
	// // check if this line is dirty and needs to be repainted
	// // also check for entire-buffer dirty flags
	// if (!entireDirty && !buffer.update[l + 1]) continue;
	//
	// // reset dirty flag for this line
	// buffer.update[l + 1] = false;
	//
	// // walk through all characters in this line
	// for (int c = 0; c < buffer.width; c++) {
	// int addr = 0;
	// int currAttr = buffer.charAttributes[buffer.windowBase + l][c];
	//
	// {
	// int fgcolor = defaultFg;
	//
	// // check if foreground color attribute is set
	// if ((currAttr & VDUBuffer.COLOR_FG) != 0)
	// fgcolor = ((currAttr & VDUBuffer.COLOR_FG) >> VDUBuffer.COLOR_FG_SHIFT) -
	// 1;
	//
	// if (fgcolor < 8 && (currAttr & VDUBuffer.BOLD) != 0)
	// fg = color[fgcolor + 8];
	// else
	// fg = color[fgcolor];
	// }
	//
	// // check if background color attribute is set
	// if ((currAttr & VDUBuffer.COLOR_BG) != 0)
	// bg = color[((currAttr & VDUBuffer.COLOR_BG) >> VDUBuffer.COLOR_BG_SHIFT)
	// - 1];
	// else
	// bg = color[defaultBg];
	//
	// // support character inversion by swapping background and foreground
	// color
	// if ((currAttr & VDUBuffer.INVERT) != 0) {
	// int swapc = bg;
	// bg = fg;
	// fg = swapc;
	// }
	//
	// // set underlined attributes if requested
	// defaultPaint.setUnderlineText((currAttr & VDUBuffer.UNDERLINE) != 0);
	//
	// isWideCharacter = (currAttr & VDUBuffer.FULLWIDTH) != 0;
	//
	// if (isWideCharacter)
	// addr++;
	// else {
	// // determine the amount of continuous characters with the same settings
	// and print them all at once
	// while (c + addr < buffer.width
	// && buffer.charAttributes[buffer.windowBase + l][c + addr] == currAttr) {
	// addr++;
	// }
	// }
	//
	// // Save the current clip region
	// canvas.save(Canvas.CLIP_SAVE_FLAG);
	//
	// // clear this dirty area with background color
	// defaultPaint.setColor(bg);
	// if (isWideCharacter) {
	// canvas.clipRect(c * charWidthEN,
	// l * charHeight,
	// (c + 2) * charWidthEN,
	// (l + 1) * charHeight);
	// } else {
	// canvas.clipRect(c * charWidthEN,
	// l * charHeight,
	// (c + addr) * charWidthEN,
	// (l + 1) * charHeight);
	// }
	// canvas.drawPaint(defaultPaint);
	//
	// // write the text string starting at 'c' for 'addr' number of characters
	// defaultPaint.setColor(fg);
	// if ((currAttr & VDUBuffer.INVISIBLE) == 0)
	// canvas.drawText(buffer.charArray[buffer.windowBase + l], c,
	// addr, c * charWidthEN, (l * charHeight),
	// defaultPaint);
	//
	// // Restore the previous clip region
	// canvas.restore();
	//
	// // advance to the next text block with different characteristics
	// c += addr - 1;
	// if (isWideCharacter)
	// c++;
	// }
	// }
	//
	// // reset entire-buffer flags
	// buffer.update[0] = false;
	// }
	// }

	private void addListView() {
		displayListView = new DisplayListView(getContext());
		LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT,
				LayoutParams.WRAP_CONTENT);
		displayListView.setLayoutParams(lp);
		displayListView.setDividerHeight(0);
		displayListView.setFocusable(false);
		displayListView.setFocusableInTouchMode(false);
		displayListView.setSelector(android.R.color.transparent);
		nomalDisplayString = new ArrayList<SpannableStringBuilder>();
		sshAdapter = new SSHAdapter(nomalDisplayString, getContext());
		displayListView.setAdapter(sshAdapter);

		viDisplayString = new ArrayList<SpannableStringBuilder>();
		
		viSaveString = new ArrayList<SpannableStringBuilder>();

		quitVIDisplayString = new ArrayList<SpannableStringBuilder>();

		saveDisplayString = new ArrayList<SpannableStringBuilder>();
		// setListViewHeightBasedOnChildren(displayListView);

		this.addView(displayListView);
		TextView tv = (TextView) View.inflate(getContext(),
				R.layout.item_sshview, null);
		TextPaint paint = tv.getPaint();
		charWidthEN = paint.measureText("x");
		charWidthCN = paint.measureText("一");
		tv.measure(0, 0);
		charHeight = tv.getMeasuredHeight();
	}

	// private void addLinearLayout() {
	// ll_text_view = new LinearLayout(getContext());
	// ViewGroup.LayoutParams lp = new
	// ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT);
	// ll_text_view.setLayoutParams(lp);
	// ll_text_view.setOrientation(LinearLayout.VERTICAL);
	// this.addView(ll_text_view);
	// }

	private void addNewString(String text) {
		// 设置颜色
		SpannableStringBuilder builder = new SpannableStringBuilder(text);
		nomalDisplayString.add(builder);
		sshAdapter.notifyDataSetChanged();
		lineCount++;
	}

	private void addCursorView() {
		cursorView = new CursorView(getContext());
		cursorView.setSize((int) charWidthEN, (int) charHeight);
		this.addView(cursorView);
	}

	public void addString(String str) {
		synchronized (ALPHA) {
//			Log.i(TAG, "----------------------source string read: ----------------start");
//			for(int i=0;i<str.length();i++){
//				Log.i(TAG, "char : "+String.valueOf(str.charAt(i))+"int : "+(int)str.charAt(i));
//			}
//			Log.i(TAG, "----------------------source string read: ----------------end");
//			char[] s = str.toCharArray();
//			byte[] fullwidths = new byte[s.length];
//			for (int i = 0; i < s.length; i++) {
//				char c = s[i];
//				if (UIUtils.isChinese(c)) {
//					fullwidths[i] = AndroidCharacter.EAST_ASIAN_WIDTH_WIDE;
//				} else {
//					fullwidths[i] = AndroidCharacter.EAST_ASIAN_WIDTH_NARROW;
//				}
//			}
//
//			
//			EastAsianWidth measurer = EastAsianWidth.getInstance();
//			
//			measurer.measure(charArray, 0, offset, wideAttribute, charWidth);
//			buffer.putString(charArray, wideAttribute, 0, charBuffer.position());
//			
//			buffer.putString(s, fullwidths, 0, str.length());
			//TODO
			srcBuffer.append(str);
			if (srcBuffer.toString().contains("[2J")) {
				// 保存进入vi模式之前的字符集合
				isVIMode = true;
//				isQuitVI = false;
//				LogUtil.d(TAG,
//						"----------------saveDisplayString --------------------------start");
//				if (saveDisplayString.size() == 0) {
//					// 第一次进行保存
//					for (int i = 0; i < nomalDisplayString.size(); i++) {
//						saveDisplayString.add(nomalDisplayString.get(i));
//						LogUtil.d(TAG, "saveDisplayString line" + i + " : "
//								+ saveDisplayString.get(i));
//					}
//				} else {
//					saveDisplayString.clear();
//					for (int i = 0; i < quitVIDisplayString.size(); i++) {
//						saveDisplayString.add(quitVIDisplayString.get(i));
//						LogUtil.d(TAG, "saveDisplayString line" + i + " : "
//								+ saveDisplayString.get(i));
//					}
//				}
//				LogUtil.d(TAG,
//						"----------------saveDisplayString --------------------------end");
				srcBuffer.replace(0, srcBuffer.length(), "");
				viDisplayString = new ArrayList<SpannableStringBuilder>();
				sshAdapter.setArray(viDisplayString);
			}

			if (System.currentTimeMillis() - fristRefreshTime > 5
					|| fristRefreshTime == 0) {
				// LogUtil.d(TAG, "refreshing-----------");
				// fristRefreshTime=System.currentTimeMillis();
				// refresh();
				startRefresh();
			}
		}
	}

	public void startRefresh() {
		fristRefreshTime = System.currentTimeMillis();
		// if(listRefreshTimer == null){
		// listRefreshTimer = new Timer();
		// }
		// if(listRefreshTask == null){
		// listRefreshTask = new TimerTask() {
		//
		// @Override
		// public void run() {
		// Message msg = Message.obtain(handler);
		// msg.what = MSG_REFRESH_LIST;
		// msg.sendToTarget();
		// }
		// };
		// }
		// listRefreshTimer.schedule(listRefreshTask, 100);
		if (runnable == null) {
			runnable = new Runnable() {

				@Override
				public void run() {
					try {
						Thread.sleep(5);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					Message msg = Message.obtain(handler);
					msg.what = MSG_REFRESH_LIST;
					msg.sendToTarget();
				}
			};
		}
		ThreadPoolManage.getSinglePool().execute(runnable);
	}

	/**
	 * 刷新数据
	 */
	public void refresh() {
		// displayString.clear();
		// boolean entireDirty = buffer.update[0];
		// for(int l=0; l<buffer.height;l++){
		//
		// String disPlayString = UIUtils.charsToString(buffer.charArray[l+1]);
		// SpannableStringBuilder builder = new
		// SpannableStringBuilder(disPlayString);
		//
		// if(!entireDirty&&!buffer.update[l+1]) continue;
		//
		// buffer.update[l+1] = false;
		//
		// for(int c=0;c<buffer.width;c++){
		// int fgcolor = defaultFg;
		// int currAttr = buffer.charAttributes[l+1][c];
		// if((currAttr & VDUBuffer.COLOR_FG) != 0){
		// fgcolor = ((currAttr & VDUBuffer.COLOR_FG) >>
		// VDUBuffer.COLOR_FG_SHIFT) - 1;
		// }
		// ForegroundColorSpan colorSpan = new
		// ForegroundColorSpan(color[fgcolor]);
		// builder.setSpan(colorSpan, c, c+1,
		// Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		// }
		// displayString.add(l+1, builder);
		// }
		// sshAdapter.notifyDataSetChanged();
		synchronized (ALPHA) {
			disPlayChars = buffer.charArray;
			if (isVIMode) {
				// 如果是VI模式，则只显示最后一屏数据
				viDisplayString.clear();
				
				for (int i = disPlayChars.length - 1 - buffer.height; i < disPlayChars.length - 1; i++) {
					char[] disPlayText = disPlayChars[i];
					String disPlayString = UIUtils.charsToString(disPlayText);
					SpannableStringBuilder builder = new SpannableStringBuilder(
							disPlayString);
					
					if (disPlayString.contains("$")
							&& i == disPlayChars.length - 2) {
						int end = disPlayString.indexOf("$");
						viFilePath = disPlayString.substring(0, end + 1);
						if (viFilePath != null
								&& viFilePath.equals(disPlayString.substring(0,
										end + 1))) {
							// 表示退出VI
							isVIMode = false;
							isQuitVI = true;
							saveCount = disPlayChars.length-2;
//							displayQuitVI();
//							viSaveString.clear();
							
							for(int j=0;j<viDisplayString.size();j++){
								if(!UIUtils.containBuilder(viSaveString, viDisplayString.get(j))){
									viSaveString.add(viDisplayString.get(j));
								}
							}
							
							Log.i(TAG,"----------------viDisplayString ---------------------start-----");
							for(int x=0;x<viDisplayString.size();x++){
								Log.i(TAG,"----------------viDisplayString line"+x+" : "+viDisplayString.get(x));
							}
							Log.i(TAG,"----------------viDisplayString ---------------------end-----");

							Log.i(TAG,"----------------viSaveString ---------------------start-----");
							for(int x=0;x<viSaveString.size();x++){
								Log.i(TAG,"----------------viSaveString line"+x+" : "+viSaveString.get(x));
							}
							Log.i(TAG,"----------------viSaveString ---------------------end-----");
							displayNomal();
							redrawCursor();
							Log.i(TAG,"----------------quitVI --------------------------");
							return;
						}
					}
					
					
					int fgcolor = defaultFg;
					for (int j = 0; j < disPlayText.length; j++) {
						char currchar = disPlayText[j];
						int currAttr = buffer.charAttributes[i][j];
						// check if foreground color attribute is set
						if ((currAttr & VDUBuffer.COLOR_FG) != 0) {
							fgcolor = ((currAttr & VDUBuffer.COLOR_FG) >> VDUBuffer.COLOR_FG_SHIFT) - 1;
							ForegroundColorSpan colorSpan = new ForegroundColorSpan(
									color[fgcolor]);
							builder.setSpan(colorSpan, j, j + 1,
									Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
						}
					}
					Log.i(TAG, "viDisplayString line" + i + " : " + builder);
					viDisplayString.add(builder);
				}
				Log.i(TAG,"----------------viDisplayString --------------------------end");
				sshAdapter.notifyDataSetChanged();
				for(int j=0;j<viDisplayString.size();j++){
					if(!UIUtils.containBuilder(viSaveString, viDisplayString.get(j))){
						viSaveString.add(viDisplayString.get(j));
					}
				}
			} else {
				displayNomal();
			}
			redrawCursor();
		}
	}

	private void displayNomal() {
		synchronized (ALPHA) {
		nomalDisplayString.clear();
		sshAdapter.setArray(nomalDisplayString);
		for (int i = 0; i < disPlayChars.length - 1; i++) {
			char[] disPlayText = disPlayChars[i];
			String disPlayString = UIUtils.charsToString(disPlayText);
			SpannableStringBuilder builder = new SpannableStringBuilder(disPlayString);
			int fgcolor = defaultFg;
			for (int j = 0; j < disPlayText.length; j++) {
				char currchar = disPlayText[j];
				int currAttr = buffer.charAttributes[i][j];
				// check if foreground color attribute is set
				if ((currAttr & VDUBuffer.COLOR_FG) != 0) {
					fgcolor = ((currAttr & VDUBuffer.COLOR_FG) >> VDUBuffer.COLOR_FG_SHIFT) - 1;
					ForegroundColorSpan colorSpan = new ForegroundColorSpan(
							color[fgcolor]);
					builder.setSpan(colorSpan, j, j + 1,
							Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
				}
			}
			nomalDisplayString.add(builder);
		}

			Log.i(TAG,"----------------nomalDisplayString ---------------------before-----start");
		for(int i=0;i<nomalDisplayString.size();i++){
			Log.i(TAG, "nomalDisplayString line" + i + " : " + nomalDisplayString.get(i));
		}
			Log.i(TAG,"----------------nomalDisplayString ---------------------before-----end");
		
		if(viSaveString!=null&&viSaveString.size()!=0){
			Log.i(TAG,"----------------viSaveString --------------------------start");
			for(int i=0;i<viSaveString.size();i++){
				Log.i(TAG, "viSaveString line" + i + " : " + viSaveString.get(i));
			}
			Log.i(TAG,"----------------viSaveString --------------------------end");
			
			
			for(int i=0;i<viSaveString.size();i++){
				for(int j=0;j<nomalDisplayString.size();j++){
					if(viSaveString.get(i).toString().equals(nomalDisplayString.get(j).toString())){
						nomalDisplayString.remove(j);
						j--;
					}
				}
			}
		}

			Log.i(TAG,"----------------nomalDisplayString ---------------------after-----start");
		for(int i=0;i<nomalDisplayString.size();i++){
			Log.i(TAG, "nomalDisplayString line" + i + " : " + nomalDisplayString.get(i));
		}
			Log.i(TAG,"----------------nomalDisplayString ---------------------after-----end");
		
		sshAdapter.notifyDataSetChanged();
		}
	}

	// 显示退出VI后的界面
//	private void displayQuitVI() {
//
//		nomalDisplayString.clear();
//		quitVIDisplayString.clear();
//		sshAdapter.setArray(quitVIDisplayString);
//		for (int i = 0; i < disPlayChars.length - 1; i++) {
//			char[] disPlayText = disPlayChars[i];
//			String disPlayString = UIUtils.charsToString(disPlayText);
//
//			SpannableStringBuilder builder = new SpannableStringBuilder(
//					disPlayString);
//			int fgcolor = defaultFg;
//			for (int j = 0; j < disPlayText.length; j++) {
//				char currchar = disPlayText[j];
//				int currAttr = buffer.charAttributes[i][j];
//				// check if foreground color attribute is set
//				if ((currAttr & VDUBuffer.COLOR_FG) != 0) {
//					fgcolor = ((currAttr & VDUBuffer.COLOR_FG) >> VDUBuffer.COLOR_FG_SHIFT) - 1;
//					ForegroundColorSpan colorSpan = new ForegroundColorSpan(
//							color[fgcolor]);
//					builder.setSpan(colorSpan, j, j + 1,
//							Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
//				}
//
//			}
//			nomalDisplayString.add(builder);
//		}
//		for (int i = 0; i < saveDisplayString.size(); i++) {
//			quitVIDisplayString.add(saveDisplayString.get(i));
//		}
//		Log.i(TAG,
//				"saveCount------"+saveCount);
//		if(nomalDisplayString.size()<MAX_NOMAL_LINE){
//			for (int i = saveCount; i < nomalDisplayString.size(); i++) {
//				quitVIDisplayString.add(nomalDisplayString.get(i));
//			}
//		}else{
//			for(int i=0;i<saveDisplayString.size();i++){
//				if(nomalDisplayString.get(0).equals(saveDisplayString.get(i))){
//					aboveMaxLineSaveCount = i;
//				}
//			}
//			if(aboveMaxLineSaveCount !=0){
//				for (int i = nomalDisplayString.size() - aboveMaxLineSaveCount; i < nomalDisplayString.size(); i++) {
//					quitVIDisplayString.add(nomalDisplayString.get(i));
//				}
//			}else{
//				quitVIDisplayString.clear();
//				for (int i = saveCount; i < nomalDisplayString.size(); i++) {
//					quitVIDisplayString.add(nomalDisplayString.get(i));
//				}
//				saveDisplayString.clear();
//				for (int i=0;i<quitVIDisplayString.size();i++) {
//					saveDisplayString.add(quitVIDisplayString.get(i));
//				}
//			}
//		}
//		Log.i(TAG,
//				"----------------quitVIDisplayString ------saveDisplayString--------------------start");
//		for (int i = 0; i < saveDisplayString.size(); i++) {
//			Log.i(TAG,
//					"saveDisplayString line" + i + " : "
//							+ saveDisplayString.get(i));
//		}
//		Log.i(TAG,
//				"----------------quitVIDisplayString -------saveDisplayString-------------------end");
//
//		Log.i(TAG,
//				"----------------quitVIDisplayString ------nomalDisplayString--------------------start");
//
//		for (int i = 0; i < nomalDisplayString.size(); i++) {
//			Log.i(TAG, "nomalDisplayString line" + i + " : "
//					+ nomalDisplayString.get(i));
//		}
//
//		Log.i(TAG,
//				"----------------quitVIDisplayString -------nomalDisplayString-------------------end");
//
//		Log.i(TAG,
//				"----------------quitVIDisplayString --------------------------start");
//		for (int i = 0; i < quitVIDisplayString.size(); i++) {
//			Log.i(TAG, "quitVIDisplayString line" + i + " : "
//					+ quitVIDisplayString.get(i));
//		}
//		Log.i(TAG,
//				"----------------quitVIDisplayString --------------------------end");
//		sshAdapter.notifyDataSetChanged();
//	}

	/**
	 * 连接成功后重绘光标
	 */
	private void redrawCursor() {
		int cursorX = 0;
		int rowInList = displayListView.getCount() - HEIGHT_COUNT + buffer.getCursorRow();
		SSHAdapter currentAdapter = (SSHAdapter)displayListView.getAdapter();
		SpannableStringBuilder currentStr = currentAdapter.getArray().get(rowInList);
		for (int i = 0; i < buffer.getCursorColumn(); i++) {
			cursorX += charWidthEN;
//			char curChar = buffer.charArray[buffer.getCursorRow()][i];
			char curChar = currentStr.charAt(i);
			// boolean onWideCharacter = (currAttr & VDUBuffer.FULLWIDTH) != 0;
			boolean onWideCharacter = UIUtils.isChinese(curChar);
			if (onWideCharacter) {
				cursorX += (charWidthCN - charWidthEN- charWidthEN);
				Log.i(TAG,"cnCount = "+i);
			}
			Log.i(TAG,"Count = "+i);
		}
		cursorView.setTranslationX(cursorX);
		cursorView
				.setTranslationY(rowInList * charHeight);
		displayListView.measure(0, 0);
		if (displayListView.getMeasuredHeight() - charHeight == cursorView
				.getTranslationY()) {
			handler.post(new Runnable() {
				@Override
				public void run() {
					((ScrollView) SSHView.this.getParent())
							.fullScroll(ScrollView.FOCUS_DOWN);
				}
			});
		} else if (charHeight == cursorView.getTranslationY()) {
			handler.post(new Runnable() {
				@Override
				public void run() {
					((ScrollView) SSHView.this.getParent())
							.fullScroll(ScrollView.FOCUS_UP);
				}
			});
		}

		// cursorView.setTranslationY(buffer.getCursorRow() * charHeight);
		invalidate();
	}

//	/**
//	 * 请输入用户名
//	 *
//	 * @param text
//	 */
//	public void setDisPlayUsername(String text) {
//		displayusername = text;
//		addNewString(displayusername);
//		cursorView.setTranslationX(text.length() * charWidthCN);
//		cursorView.setTranslationY((lineCount - 1) * charHeight);
//		invalidate();
//		timer = new Timer();
//		task = new TimerTask() {
//
//			@Override
//			public void run() {
//				Message msg = Message.obtain(handler);
//				msg.what = MSG_DISPLAY_USERNAME;
//				msg.sendToTarget();
//			}
//		};
//		timer.schedule(task, 500);
//
//	}

//	/**
//	 * 请输入密码
//	 *
//	 * @param text
//	 */
//	public void setDisPlayPassword(String text) {
//		displaypassword = text;
//		addNewString(displaypassword);
//		cursorView.setTranslationX(text.length() * charWidthCN);
//		cursorView.setTranslationY((lineCount - 1) * charHeight);
//		invalidate();
//	}

	
	/**
	 * 判断光标是否在最下面
	 * @return
	 */
	public boolean isCursorDown(){
		if(displayListView.getMeasuredHeight() - charHeight == cursorView
				.getTranslationY()){
			return true;
		}else{
			return false;
		}
	}
	
//	/**
//	 * 输入用户名、密码时调用
//	 *
//	 * @param text
//	 */
//	public void addDisPlayString(String text) {
//		if (lineCount == 1) {
//			cursorView.setTranslationX(displayusername.length() * charWidthCN
//					+ text.length() * charWidthEN);
//			cursorView.setTranslationY((lineCount - 1) * charHeight);
//			nomalDisplayString.set(0, new SpannableStringBuilder(
//					displayusername + text));
//		} else if (lineCount == 2) {
//			cursorView.setTranslationX(displaypassword.length() * charWidthCN
//					+ text.length() * charWidthEN);
//			cursorView.setTranslationY((lineCount - 1) * charHeight);
//			nomalDisplayString.set(1, new SpannableStringBuilder(
//					displaypassword + text));
//		}
//		sshAdapter.notifyDataSetChanged();
//		invalidate();
//	}
//
//	public void deleteDisPlayChar() {
//		if (lineCount == 1) {
//			if (username != null && username.length() > 0) {
//				username.deleteCharAt(username.length() - 1);
//				cursorView.setTranslationX(cursorView.getTranslationX()
//						- charWidthEN);
//				TextView tv = (TextView) displayListView
//						.getChildAt(lineCount - 1);
//				tv.setText(displayusername
//						+ username.subSequence(0, username.length()));
//			}
//		} else if (lineCount == 2) {
//			if (password != null && password.length() > 0) {
//				password.deleteCharAt(password.length() - 1);
//				cursorView.setTranslationX(cursorView.getTranslationX()
//						- charWidthEN);
//				TextView tv = (TextView) displayListView
//						.getChildAt(lineCount - 1);
//				tv.setText(displaypassword
//						+ password.subSequence(0, password.length()));
//			}
//		}
//
//		invalidate();
//	}

	public void clearTextView() {
		if (nomalDisplayString != null) {
			nomalDisplayString.clear();
		}
		lineCount = 0;
		username = new StringBuffer();
		password = new StringBuffer();
	}

	@Override
	public void redraw() {
		// redrawCursor();
	}

	@Override
	public void updateScrollBar() {

	}

	@Override
	public void setVDUBuffer(VDUBuffer buffer) {
		this.buffer = (vt320) buffer;
	}

	@Override
	public VDUBuffer getVDUBuffer() {
		return buffer;
	}

	@Override
	public void setColor(int index, int red, int green, int blue) {
		if (index < color.length && index >= 16)
			color[index] = 0xff000000 | red << 16 | green << 8 | blue;
	}

	@Override
	public void resetColors() {
		color = Colors.defaults;
	}

	public interface OnInputTextListener {
		void onInputText(String text);
	}

	public void setOnInputTextListener(OnInputTextListener onInputTextListener) {
		this.onInputTextListener = onInputTextListener;
	}

//	@Override
//	public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
//		return new MyInputConnection(this, false);
//	}

//	/**
//	 * 自定义InputConnection,监听软键盘输入用
//	 */
//	class MyInputConnection extends BaseInputConnection {
//
//		public MyInputConnection(View targetView, boolean fullEditor) {
//			super(targetView, fullEditor);
//		}
//
//		@Override
//		public boolean commitText(CharSequence text, int newCursorPosition) {
//			if (!isConnected) {
//				if (lineCount == 1) {
//					username.append(text);
//					addDisPlayString(username.toString());
//				} else if (lineCount == 2) {
//					password.append(text);
//					addDisPlayString(password.toString());
//				}
//				return true;
//			} else {
//				return super.commitText(text, newCursorPosition);
//			}
//		}
//	}

	public class DisplayListView extends ListView {

		public DisplayListView(Context context) {
			super(context);
		}

		@Override
		protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
			int mExpandSpec = MeasureSpec.makeMeasureSpec(
					Integer.MAX_VALUE >> 2, MeasureSpec.AT_MOST);
			super.onMeasure(widthMeasureSpec, mExpandSpec);
		}

		@Override
		public boolean onTouchEvent(MotionEvent ev) {
			// TODO Auto-generated method stub
			return false;
		}
	}

	
}

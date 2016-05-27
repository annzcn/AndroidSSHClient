package com.zou.sshclient;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.ScrollView;

import com.jcraft.jsch.ChannelShell;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.zou.sshclient.terminal.vt320;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;

public class MainActivity extends Activity implements View.OnClickListener,SSHView.OnInputTextListener{
    private static final String TAG ="RemoteSSHUI" ;
    private View mView;
    public static final String BUNDLE_SSH_IP = "bundle_ssh_ip";
    public static final String BUNDLE_SSH_PORT = "bundle_ssh_port";
    private String ip;
    private int port;
    private SSHView sshView;
    private Session session = null;
    private ChannelShell channel = null;
    private JSch jsch;
    private Handler handler;
    private InputStream instream;
    private OutputStream outstream;
    public static final int MSG_ADD_TEXT = 0;
    public static final int MSG_CONNECT_FAIL = 1;
    public static final int MSG_CONNECTED = 2;
    private static final int BUFFER_SIZE = 4096;
    private InputMethodManager imm;
    private Button ssh_key_f1,ssh_key_f2,ssh_key_f3,ssh_key_f4,ssh_key_f5,ssh_key_f6,ssh_key_f7,ssh_key_f8,ssh_key_f9,ssh_key_f10,ssh_key_f11,ssh_key_f12;
    private Button ssh_key_esc,ssh_key_alt,ssh_key_ctrl,ssh_key_tab,ssh_key_left,ssh_key_right,ssh_key_up,ssh_key_down,ssh_key_sp_1,ssh_key_sp_2,ssh_key_sp_3,ssh_key_sp_4,ssh_key_sp_5,ssh_key_sp_6,ssh_key_sp_7,ssh_key_sp_8,ssh_key_sp_9,ssh_key_sp_10,ssh_key_sp_11,ssh_key_sp_12,ssh_key_sp_13,ssh_key_sp_14;
    private int ctrl,alt;
    private SSHKeyListener connectedkeyListener;
    private View.OnKeyListener unConnectedKeyListener;
    private AlertDialog.Builder connectFailDialog,inputIPAndPortDialog;
    private RelativeLayout rl_keyboard;
    private CharsetDecoder decoder;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.activity_main);
        initView();
        initData();
        setLisetener();
        inputIpAndPort();
        inputUserNameAndPassword();
        super.onCreate(savedInstanceState);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void inputIpAndPort() {
        final View view = View.inflate(this,R.layout.input_ip_port_layout,null);
        inputIPAndPortDialog = new AlertDialog.Builder(this);
        inputIPAndPortDialog.setCancelable(false);
        inputIPAndPortDialog.setTitle("请输入IP和端口");
        inputIPAndPortDialog.setView(view);
        inputIPAndPortDialog.setNegativeButton("确定", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface arg0, int arg1) {
                EditText et_ip = (EditText) view.findViewById(R.id.et_ip);
                ip = et_ip.getText().toString();
                EditText et_port = (EditText) view.findViewById(R.id.et_port);
                port = Integer.parseInt(et_port.getText().toString());
                sshView.clearTextView();
                sshView.setDisPlayUsername("请输入用户名：");
            }
        });
    }

    /**
     * 输入用户名和密码
     */
    private void inputUserNameAndPassword() {
        sshView.setDisPlayUsername("请输入用户名：");

    }

    /**
     * 开始连接SSH
     */
    private void startConnect(final String username ,final String password) {
        ThreadPoolManage.ThreadPool pool = ThreadPoolManage.getShortPool();
        pool.execute(new Runnable() {

            @Override
            public void run() {
                connect(ip, username, password, port, null, null);
            }
        });
    }

    private void initData(){
        decoder = Charset.forName("utf-8").newDecoder();
//        ip = getArguments().getString(BUNDLE_SSH_IP);
//        port = getArguments().getInt(BUNDLE_SSH_PORT);
        connectedkeyListener = new SSHKeyListener(this,sshView.buffer,"utf-8");
//        connectedkeyListener.setOnClearListener(new SSHKeyListener.OnClearListener() {
//            @Override
//            public void onClear() {
//                clearState();
//            }
//        });

        unConnectedKeyListener = new View.OnKeyListener() {

            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if(KeyEvent.ACTION_UP == event.getAction()){
                    if(event.getKeyCode()>=7&&event.getKeyCode()<=16){
                        //数字键0-9
                        if(sshView.lineCount == 1){
                            sshView.username.append((event.getKeyCode()-7)+"");
                            sshView.addDisPlayString(sshView.username.toString());
                        }else if(sshView.lineCount == 2){
                            sshView.password.append((event.getKeyCode()-7)+"");
                            sshView.addDisPlayString(sshView.password.toString());
                        }
                    }

                    if(event.getKeyCode() == 67){
                        //退格键
                        sshView.deleteDisPlayChar();
                    }
                    if(event.getKeyCode() == 66){
                        //回车键
                        if(sshView.lineCount==1){
                            sshView.setDisPlayPassword("请输入密码：");
                        }else if(sshView.lineCount==2){
                            sshView.setDisPlayPassword("正在连接...");
                            startConnect(sshView.username.toString(),sshView.password.toString());
                        }
                    }
                }
                return false;
            }
        };
        imm = (InputMethodManager) mView.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);

        handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case MSG_ADD_TEXT:

                        sshView.addString((String)msg.obj);
                        break;
                    case MSG_CONNECT_FAIL:
                        connectFailDialog.show();
                        break;
                    case MSG_CONNECTED:
                        sshView.clearTextView();
                        break;
                }
                super.handleMessage(msg);
            }
        };
    }

    private void clearState(){
        if(ctrl==1) {
            clearCtrl();
            showSoftInput();
        }
        if(alt==1) {
            clearAlt();
            showSoftInput();
        }

    }

    private void clearCtrl(){
        ctrl=0;
        ssh_key_ctrl.setBackgroundDrawable(getResources().getDrawable(R.mipmap.keyboard_nomal));
        ssh_key_ctrl.setTextColor(Color.parseColor("#000000"));
    }
    private void clearAlt(){
        alt=0;
        ssh_key_alt.setBackgroundDrawable(getResources().getDrawable(R.mipmap.keyboard_nomal));
        ssh_key_alt.setTextColor(Color.parseColor("#000000"));
    }

    private void initView() {
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        connectFailDialog = new AlertDialog.Builder(this);
        connectFailDialog.setCancelable(false);
        connectFailDialog.setTitle("温馨提示");
        connectFailDialog.setMessage("SSH登录失败");
        connectFailDialog.setPositiveButton("断开", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface arg0, int arg1) {
                onBackPressed();
            }
        });
        connectFailDialog.setNegativeButton("再试一次", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface arg0, int arg1) {
                sshView.clearTextView();
                sshView.setDisPlayUsername("请输入用户名：");
            }
        });
        sshView = (SSHView) mView.findViewById(R.id.ssh_view);
        rl_keyboard = (RelativeLayout) mView.findViewById(R.id.rl_keyboard);
//      ll_ssh_keyboard = (LinearLayout)findViewById(R.id.ll_ssh_keyboard);
        ssh_key_f1 = (Button) mView.findViewById(R.id.ssh_key_f1);
        ssh_key_f2 = (Button) mView.findViewById(R.id.ssh_key_f2);
        ssh_key_f3 = (Button) mView.findViewById(R.id.ssh_key_f3);
        ssh_key_f4 = (Button) mView.findViewById(R.id.ssh_key_f4);
        ssh_key_f5 = (Button) mView.findViewById(R.id.ssh_key_f5);
        ssh_key_f6 = (Button) mView.findViewById(R.id.ssh_key_f6);
        ssh_key_f7 = (Button) mView.findViewById(R.id.ssh_key_f7);
        ssh_key_f8 = (Button) mView.findViewById(R.id.ssh_key_f8);
        ssh_key_f9 = (Button) mView.findViewById(R.id.ssh_key_f9);
        ssh_key_f10 = (Button) mView.findViewById(R.id.ssh_key_f10);
        ssh_key_f11 = (Button) mView.findViewById(R.id.ssh_key_f11);
        ssh_key_f12 = (Button) mView.findViewById(R.id.ssh_key_f12);

        ssh_key_f1.setOnClickListener(this);
        ssh_key_f2.setOnClickListener(this);
        ssh_key_f3.setOnClickListener(this);
        ssh_key_f4.setOnClickListener(this);
        ssh_key_f5.setOnClickListener(this);
        ssh_key_f6.setOnClickListener(this);
        ssh_key_f7.setOnClickListener(this);
        ssh_key_f8.setOnClickListener(this);
        ssh_key_f9.setOnClickListener(this);
        ssh_key_f10.setOnClickListener(this);
        ssh_key_f11.setOnClickListener(this);
        ssh_key_f12.setOnClickListener(this);

        ssh_key_esc = (Button) mView.findViewById(R.id.ssh_key_esc);
        ssh_key_alt = (Button) mView.findViewById(R.id.ssh_key_alt);
        ssh_key_ctrl = (Button) mView.findViewById(R.id.ssh_key_ctrl);
        ssh_key_tab = (Button) mView.findViewById(R.id.ssh_key_tab);
        ssh_key_left = (Button) mView.findViewById(R.id.ssh_key_left);
        ssh_key_right = (Button) mView.findViewById(R.id.ssh_key_right);
        ssh_key_up = (Button) mView.findViewById(R.id.ssh_key_up);
        ssh_key_down = (Button) mView.findViewById(R.id.ssh_key_down);
        ssh_key_sp_1 = (Button) mView.findViewById(R.id.ssh_key_sp_1);
        ssh_key_sp_2 = (Button) mView.findViewById(R.id.ssh_key_sp_2);
        ssh_key_sp_3 = (Button) mView.findViewById(R.id.ssh_key_sp_3);
        ssh_key_sp_4 = (Button) mView.findViewById(R.id.ssh_key_sp_4);
        ssh_key_sp_5 = (Button) mView.findViewById(R.id.ssh_key_sp_5);
        ssh_key_sp_6 = (Button) mView.findViewById(R.id.ssh_key_sp_6);
        ssh_key_sp_7 = (Button) mView.findViewById(R.id.ssh_key_sp_7);
        ssh_key_sp_8 = (Button) mView.findViewById(R.id.ssh_key_sp_8);
        ssh_key_sp_9 = (Button) mView.findViewById(R.id.ssh_key_sp_9);
        ssh_key_sp_10 = (Button) mView.findViewById(R.id.ssh_key_sp_10);
        ssh_key_sp_11 = (Button) mView.findViewById(R.id.ssh_key_sp_11);
        ssh_key_sp_12 = (Button) mView.findViewById(R.id.ssh_key_sp_12);
        ssh_key_sp_13 = (Button) mView.findViewById(R.id.ssh_key_sp_13);
        ssh_key_sp_14 = (Button) mView.findViewById(R.id.ssh_key_sp_14);

        ssh_key_esc.setOnClickListener(this);
        ssh_key_alt.setOnClickListener(this);
        ssh_key_ctrl.setOnClickListener(this);
        ssh_key_tab.setOnClickListener(this);
        ssh_key_left.setOnClickListener(this);
        ssh_key_right.setOnClickListener(this);
        ssh_key_up.setOnClickListener(this);
        ssh_key_down.setOnClickListener(this);
        ssh_key_sp_1.setOnClickListener(this);
        ssh_key_sp_2.setOnClickListener(this);
        ssh_key_sp_3.setOnClickListener(this);
        ssh_key_sp_4.setOnClickListener(this);
        ssh_key_sp_5.setOnClickListener(this);
        ssh_key_sp_6.setOnClickListener(this);
        ssh_key_sp_7.setOnClickListener(this);
        ssh_key_sp_8.setOnClickListener(this);
        ssh_key_sp_9.setOnClickListener(this);
        ssh_key_sp_10.setOnClickListener(this);
        ssh_key_sp_11.setOnClickListener(this);
        ssh_key_sp_12.setOnClickListener(this);
        ssh_key_sp_13.setOnClickListener(this);
        ssh_key_sp_14.setOnClickListener(this);




        ssh_key_esc.setText("Esc");
        ssh_key_alt.setText("Alt");
        ssh_key_ctrl.setText("Ctrl");
        ssh_key_tab.setText("Tab");
        ssh_key_left.setText("←");
        ssh_key_right.setText("→");
        ssh_key_up.setText("↑");
        ssh_key_down.setText("↓");
        ssh_key_sp_1.setText("$");
        ssh_key_sp_2.setText("%");
        ssh_key_sp_3.setText("^");
        ssh_key_sp_4.setText("*");
        ssh_key_sp_5.setText("-");
        ssh_key_sp_6.setText("_");
        ssh_key_sp_7.setText("|");
        ssh_key_sp_8.setText("\\");
        ssh_key_sp_9.setText("/");
        ssh_key_sp_10.setText("<");
        ssh_key_sp_11.setText(">");
        ssh_key_sp_12.setText("#");
        ssh_key_sp_13.setText(":");
        ssh_key_sp_14.setText(".");
    }

    private void setLisetener() {
        mView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                Rect r = new Rect();
                mView.getWindowVisibleDisplayFrame(r);
                rl_keyboard.getHeight();

                int screenHeight = mView.getRootView().getHeight();
                int heightDifference = screenHeight - (r.bottom - r.top);
                if(heightDifference != 0){
                    if(sshView.isCursorDown()){
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                ((ScrollView) sshView.getParent()).fullScroll(ScrollView.FOCUS_DOWN);
                            }
                        });
                    }
                }
            }
        });

        sshView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSoftInput();
            }
        });
        sshView.setOnInputTextListener(this);

        sshView.setOnKeyListener(unConnectedKeyListener);
    }

    //显示软键盘
    public void showSoftInput() {
        imm.showSoftInput(sshView, InputMethodManager.SHOW_FORCED);
    }

    public void hideSoftInput(){
        imm.hideSoftInputFromWindow(sshView.getWindowToken(), 0);
    }

    @Override
    public void onInputText(String text) {
        try {
            sshShell(text);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.ssh_key_f1:
                connectedkeyListener.sendPressedKey(vt320.KEY_F1);
                break;
            case R.id.ssh_key_f2:
                connectedkeyListener.sendPressedKey(vt320.KEY_F2);
                break;
            case R.id.ssh_key_f3:
                connectedkeyListener.sendPressedKey(vt320.KEY_F3);
                break;
            case R.id.ssh_key_f4:
                connectedkeyListener.sendPressedKey(vt320.KEY_F4);
                break;
            case R.id.ssh_key_f5:
                connectedkeyListener.sendPressedKey(vt320.KEY_F5);
                break;
            case R.id.ssh_key_f6:
                connectedkeyListener.sendPressedKey(vt320.KEY_F6);
                break;
            case R.id.ssh_key_f7:
                connectedkeyListener.sendPressedKey(vt320.KEY_F7);
                break;
            case R.id.ssh_key_f8:
                connectedkeyListener.sendPressedKey(vt320.KEY_F8);
                break;
            case R.id.ssh_key_f9:
                connectedkeyListener.sendPressedKey(vt320.KEY_F9);
                break;
            case R.id.ssh_key_f10:
                connectedkeyListener.sendPressedKey(vt320.KEY_F10);
                break;
            case R.id.ssh_key_f11:
                connectedkeyListener.sendPressedKey(vt320.KEY_F11);
                break;
            case R.id.ssh_key_f12:
                connectedkeyListener.sendPressedKey(vt320.KEY_F12);
                break;
            case R.id.ssh_key_esc:
                connectedkeyListener.sendEscape();
                break;
            case R.id.ssh_key_alt:
                if(alt<2){
                    alt++;
                }else{
                    alt=0;
                }
//	                connectedkeyListener.metaPress(SSHKeyListener.OUR_ALT_ON, true);
                switch (alt){
                    case 0:
                        ssh_key_alt.setBackgroundDrawable(getResources().getDrawable(R.mipmap.keyboard_nomal));
                        if(ctrl==0){
                            showSoftInput();
                        }
                        break;
                    case 1:
                        ssh_key_alt.setBackgroundDrawable(getResources().getDrawable(R.mipmap.keyboard_press));
                        hideSoftInput();
                        break;
                    case 2:
                        ssh_key_alt.setBackgroundDrawable(getResources().getDrawable(R.mipmap.keyboard_long_press));
                        hideSoftInput();
                        break;
                }
                break;
            case R.id.ssh_key_ctrl:
                if(ctrl<2){
                    ctrl++;
                }else{
                    ctrl=0;
                }
//	                connectedkeyListener.metaPress(SSHKeyListener.OUR_CTRL_ON, true);
                switch (ctrl){
                    case 0:
                        ssh_key_ctrl.setBackgroundDrawable(getResources().getDrawable(R.mipmap.keyboard_nomal));
                        if(alt==0){
                            showSoftInput();
                        }
                        break;
                    case 1:
                        ssh_key_ctrl.setBackgroundDrawable(getResources().getDrawable(R.mipmap.keyboard_press));
                        hideSoftInput();
                        break;
                    case 2:
                        ssh_key_ctrl.setBackgroundDrawable(getResources().getDrawable(R.mipmap.keyboard_long_press));
                        hideSoftInput();
                        break;
                }
                break;

            case R.id.ssh_key_tab:
                connectedkeyListener.sendTab();
                break;
            case R.id.ssh_key_left:
                connectedkeyListener.sendPressedKey(vt320.KEY_LEFT);
                break;
            case R.id.ssh_key_right:
                connectedkeyListener.sendPressedKey(vt320.KEY_RIGHT);
                break;
            case R.id.ssh_key_up:
                connectedkeyListener.sendPressedKey(vt320.KEY_UP);
                break;
            case R.id.ssh_key_down:
                connectedkeyListener.sendPressedKey(vt320.KEY_DOWN);
                break;

            case R.id.ssh_key_sp_1:
                try {
                    sshShell("$");
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case R.id.ssh_key_sp_2:
                try {
                    sshShell("%");
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case R.id.ssh_key_sp_3:
                try {
                    sshShell("^");
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case R.id.ssh_key_sp_4:
                try {
                    sshShell("*");
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case R.id.ssh_key_sp_5:
                try {
                    sshShell("-");
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case R.id.ssh_key_sp_6:
                try {
                    sshShell("_");
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case R.id.ssh_key_sp_7:
                try {
                    sshShell("|");
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case R.id.ssh_key_sp_8:
                try {
                    sshShell("\\");
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case R.id.ssh_key_sp_9:
                try {
                    sshShell("/");
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case R.id.ssh_key_sp_10:
                try {
                    sshShell("<");
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case R.id.ssh_key_sp_11:
                try {
                    sshShell(">");
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;

            case R.id.ssh_key_sp_12:
                try {
                    sshShell("#");
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;

            case R.id.ssh_key_sp_13:
                try {
                    sshShell(":");
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;

            case R.id.ssh_key_sp_14:
                try {
                    sshShell(".");
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
        }

    }

    private void connect(String ip, String user, String psw
            , int port, String privateKey, String passphrase) {
        try {
            jsch = new JSch();
            //设置密钥和密码
            if (privateKey != null && !"".equals(privateKey)) {
                if (passphrase != null && "".equals(passphrase)) {
                    //设置带口令的密钥
                    jsch.addIdentity(privateKey, passphrase);
                } else {
                    //设置不带口令的密钥
                    jsch.addIdentity(privateKey);
                }
            }

            if (port <= 0) {
                //连接服务器，采用默认端口
                session = jsch.getSession(user, ip);
            } else {
                //采用指定的端口连接服务器
                session = jsch.getSession(user, ip, port);
            }

            //如果服务器连接不上，则抛出异常
            if (session == null) {
                throw new Exception("session is null");
            }
            //设置登陆主机的密码
            session.setPassword(psw);//设置密码
            //设置第一次登陆的时候提示，可选值：(ask | yes | no)
            session.setConfig("StrictHostKeyChecking", "no");
            session.setConfig("PreferredAuthentications", "publickey,keyboard-interactive,password");
            //设置登陆超时时间
            session.connect(30000);
            Message msg =Message.obtain(handler);
            msg.what = MSG_CONNECTED;
            msg.sendToTarget();

            //创建sftp通信通道
            channel = (ChannelShell) session.openChannel("shell");
            channel.setXForwarding(true);
            channel.setAgentForwarding(true);
            channel.setPty(true);
            channel.setPtyType("xterm");
            channel.setPtySize(SSHView.WIDTH_COUNT, SSHView.HEIGHT_COUNT, UIUtils.getScreenWidth(mView.getContext()), UIUtils.getScreenHeight(mView.getContext()));
            //获取输入流和输出流
            channel.connect(1000);
            sshView.isConnected = true;
            instream = channel.getInputStream();

            outstream = channel.getOutputStream();
            sshView.setOnKeyListener(connectedkeyListener);

            try {
                ByteBuffer byteBuffer;
                CharBuffer charBuffer;
                byte[] byteArray;
                char[] charArray;

                byteBuffer = ByteBuffer.allocate(BUFFER_SIZE);
                charBuffer = CharBuffer.allocate(BUFFER_SIZE);

                byte[] wideAttribute = new byte[BUFFER_SIZE];

                byteArray = byteBuffer.array();
                charArray = charBuffer.array();

                CoderResult result;

                int bytesRead = 0;
                byteBuffer.limit(0);
                int bytesToRead;
                int offset;
                EastAsianWidth measurer = EastAsianWidth.getInstance();



//        int len = 0;
//        byte[] bs = new byte[BUFFER_SIZE];
                while (true) {
//            String str = new String(bs, 0, len,"utf-8");
                    bytesToRead = byteBuffer.capacity() - byteBuffer.limit();
                    offset = byteBuffer.arrayOffset() + byteBuffer.limit();
                    bytesRead = instream.read(byteArray, offset, bytesToRead);
                    if(bytesRead>0){
                        byteBuffer.limit(byteBuffer.limit() + bytesRead);

                        synchronized (this) {
                            result = decoder.decode(byteBuffer, charBuffer, false);
                        }

                        if (result.isUnderflow() &&
                                byteBuffer.limit() == byteBuffer.capacity()) {
                            byteBuffer.compact();
                            byteBuffer.limit(byteBuffer.position());
                            byteBuffer.position(0);
                        }

                        offset = charBuffer.position();
                        measurer.measure(charArray, 0, offset, wideAttribute);
                        sshView.buffer.putString(charArray, wideAttribute, 0, charBuffer.position());
                        Message msg1 = Message.obtain(handler);
                        msg1.what = MSG_ADD_TEXT;
                        msg1.obj = String.valueOf(charArray);
//	            msg.arg1 = len;
                        msg1.sendToTarget();
//				buffer.putString(charArray, wideAttribute, 0, charBuffer.position());
                        charBuffer.clear();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

        } catch (Exception e) {
            Message msg = Message.obtain(handler);
            msg.what = MSG_CONNECT_FAIL;
            msg.sendToTarget();
            e.printStackTrace();
        }finally{

        }
    }

    private void sshShell(String cmd) throws Exception {
        synchronized (this) {
            try {
//				   StringBuffer buf = new StringBuffer(cmd);
//				   for(int i =0;i<buf.length();i++){
//					   char ch = buf.charAt(i);
//					   if(UIUtils.isChinese(ch)){
//						   buf.insert(i, ' ');
//						   i++;
//					   }
//				   }
//				   cmd = buf.toString();
                Log.i(TAG, "----------------------source string write: ----------------start");
                Log.i(TAG, "cmd.getBytes "+cmd);
                outstream.write(cmd.getBytes("utf-8"));
                outstream.flush();
                Log.i(TAG, "----------------------source string write: ----------------start");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    @Override
    public void onDestroy() {
        if(jsch!=null){
            try {
                jsch.removeAllIdentity();
                jsch = null;
            } catch (JSchException e1) {
                e1.printStackTrace();
            }
        }
        if(instream!=null){
            try {
                instream.close();
                instream = null;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if(outstream!=null){
            try {
                outstream.close();
                outstream = null;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if(session!=null&&session.isConnected()){
            session.disconnect();
            session = null;
        }
        if(channel!=null&&channel.isConnected()){
            channel.disconnect();
            channel = null;
        }
        System.gc();
        super.onDestroy();
    }
}

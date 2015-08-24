package com.hardwork.fg607.floatingball.service;

/**
 * Created by fg607 on 15-8-20.
 *
 */

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.IBinder;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.hardwork.fg607.floatingball.MainActivity;
import com.hardwork.fg607.floatingball.R;
import com.hardwork.fg607.floatingball.utils.FloatingBallUtils;

import static android.content.SharedPreferences.Editor;

public class FloatingBallService extends Service implements View.OnClickListener,View.OnKeyListener {
    public FloatingBallService() {
    }

    WindowManager wm = null;
    WindowManager.LayoutParams ballWmParams = null;
    private View ballView;
    private View menuView;
    private float mTouchStartX;
    private float mTouchStartY;
    private float x;
    private float y;
    private int oldOffsetX, oldOffsetY,newOffsetX,newOffsetY;
    private int tag ;
    private RelativeLayout menuLayout;
    private Button floatImage;
    private PopupWindow pop;
    private RelativeLayout menuTop;
    private boolean ismoving = false;
    private boolean canmove = false;
    private Notification notification = null;
    private SharedPreferences sp;
    private boolean isAdd;
    private int clickCount;
    private boolean doubleClickCounting;
    private static final long CLICK_SPACING_TIME = 200;

    private static final long LONG_PRESS_TIME = 400;
    private Handler myHandler;
    private LongPressedThread mLongPressedThread;
    private ClickPressedThread mClickPressedThread;
    private long mPreClickTime;

    @Override
    public void onCreate() {
        super.onCreate();
        //加载悬浮球布局
        ballView = LayoutInflater.from(this).inflate(R.layout.floatball, null);
        floatImage = (Button)ballView.findViewById(R.id.float_image);
        tag = 0;
        isAdd = false;
        clickCount = 0;
        doubleClickCounting = false;
        myHandler = new Handler();
        mPreClickTime = 0;
        sp = FloatingBallUtils.getSharedPreferences(this);
        //  setUpFloatMenuView();
        createFloatBallView();

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        String moveAction = intent.getStringExtra("canmove");

        if(moveAction != null) {

            switch (moveAction)
            {
                case "moveallowed":
                    canmove = true;
                    break;
                case "moveforbidden":
                    canmove = false;
                    break;

            }
        }

        String showAction = intent.getStringExtra("ballstate");

        if(showAction != null) {

            switch (showAction)
            {
                case "showball":
                    showFloatBall();
                    break;
                case "closeball":
                    closeFloatBall();
                    break;

            }
        }

        if(notification == null)
        {
            //设置通知栏
            Notification notification = new Notification(R.drawable.ball,
                    getString(R.string.app_name), System.currentTimeMillis());


            // notification.flags=Notification.FLAG_AUTO_CANCEL;用户点击清除能够清除通知


            //点击通知栏返回后台程序，默认新建
            Intent appIntent = new Intent(this, MainActivity.class);//为PlayerActivity组建创建Intent


            //ACTION_MAIN和CATEGORY_LAUNCHER设置启动组建PlayerActivity
            appIntent.setAction(Intent.ACTION_MAIN);
            appIntent.addCategory(Intent.CATEGORY_LAUNCHER);


            //设置启动模式为将后台PlayerActivity前台运行
            appIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);


            PendingIntent pendingintent =PendingIntent.getActivity(this,0,appIntent,0);


            notification.setLatestEventInfo(this, "FloatBall", "Made By fg607",
                    pendingintent);
            startForeground(0x111, notification);//使Service处于前台，避免容易被清除
        }


        flags = START_STICKY;//设置START_STICKY标志，用于Service被杀后重启

        return super.onStartCommand(intent, flags, startId);
    }
/**
 * 窗口菜单初始化
 */
   /* private void setUpFloatMenuView(){
        menuView = LayoutInflater.from(this).inflate(R.layout.floatmenu, null);
        menuLayout = (RelativeLayout)menuView.findViewById(R.id.menu);
        menuTop = (RelativeLayout)menuView.findViewById(R.id.lay_main);
        menuLayout.setOnClickListener(this);
        menuLayout.setOnKeyListener(this);
        menuTop.setOnClickListener(this);
    }*/


public class LongPressedThread implements Runnable{

    @Override

    public void run() {

        //这里处理长按事件

        onFloatBallDoubleClick();
        clickCount = 0;

    }

}

    public class ClickPressedThread implements Runnable{

        @Override

        public void run() {

            //这里处理连续点击事件 clickCount 为连续点击的次数

            if(clickCount == 1)
            {
                //单击
               onFloatBallClick();
            }
            else if (clickCount == 2)
            {
                //双击
                onFloatBallDoubleClick();
            }
            clickCount = 0;


        }

    }

    /**
     * 创建ＦloatBallView，并初始化显示参数
     */
    private void createFloatBallView() {
        wm = (WindowManager) getApplicationContext().getSystemService(Context.WINDOW_SERVICE);
        ballWmParams = new WindowManager.LayoutParams();
        ballWmParams.type = WindowManager.LayoutParams.TYPE_PHONE;
        ballWmParams.flags |= WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        ballWmParams.gravity = Gravity.LEFT | Gravity.TOP;

        ballWmParams.x = sp.getInt("ballWmParamsX",0);
        ballWmParams.y = sp.getInt("ballWmParamsY",0);

      //  ballWmParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
        //ballWmParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
        ballWmParams.width = 120;
        ballWmParams.height = 120;
        ballWmParams.format = PixelFormat.RGBA_8888;


        //注册触碰事件监听器
        floatImage.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                x = event.getX();
                y = event.getY();
                if (tag == 0) {
                    oldOffsetX = ballWmParams.x;
                    oldOffsetY = ballWmParams.y;
                }

                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        clickCount++;
                        mPreClickTime = System.currentTimeMillis();

                        if(mClickPressedThread != null)
                        {
                            myHandler.removeCallbacks(mClickPressedThread);
                        }

                        mLongPressedThread = new LongPressedThread();
                        myHandler.postDelayed(mLongPressedThread,LONG_PRESS_TIME);

                        ismoving = false;
                        mTouchStartX = (int) event.getX();
                        mTouchStartY = (int) event.getY();
                        break;
                    case MotionEvent.ACTION_MOVE:
                        ismoving = true;

                        //取消注册的长按事件

                      //  myHandler.removeCallbacks(mLongPressedThread);
                        tag = 1;
                        ballWmParams.x += (int) (x - mTouchStartX) / 3;// 减小偏移量,防止过度抖动
                        ballWmParams.y += (int) (y - mTouchStartY) / 3;// 减小偏移量,防止过度抖动
                        if (canmove) {
                            updateViewPosition();
                            saveStates("ballWmParamsX", ballWmParams.x);
                            saveStates("ballWmParamsY", ballWmParams.y);
                        }

                        break;
                    case MotionEvent.ACTION_UP:

                        mTouchStartX = mTouchStartY = 0;
                        newOffsetX = ballWmParams.x;
                        newOffsetY = ballWmParams.y;

                        // 点击悬浮球
                        if (Math.abs(oldOffsetX - newOffsetX) <= 20 && Math.abs(oldOffsetY - newOffsetY) <= 20)
                        {

                            if(System.currentTimeMillis() - mPreClickTime <= LONG_PRESS_TIME)
                            {

                                //取消注册的长按事件

                                myHandler.removeCallbacks(mLongPressedThread);

                                mClickPressedThread = new ClickPressedThread();
                                myHandler.postDelayed(mClickPressedThread, CLICK_SPACING_TIME);
                            }

                            onClearOffset();
                        }

                        if (!canmove) {
                            //向上滑动
                            if ((oldOffsetY - newOffsetY) - Math.abs(oldOffsetX - newOffsetX) > 20 && (oldOffsetY - newOffsetY) > 20) {

                                onFloatBallFlipUp();
                            }
                            //向下滑动
                            else if ((newOffsetY - oldOffsetY) - Math.abs(oldOffsetX - newOffsetX) > 20 && (newOffsetY - oldOffsetY) > 20) {
                                onFloatBallFlipDown();
                            }
                            //向左滑动
                            else if ((oldOffsetX - newOffsetX) - Math.abs(oldOffsetY - newOffsetY) > 20 && (oldOffsetX - newOffsetX) > 20) {
                                onFloatBallFlipLeft();
                            }
                            //向右滑动
                            else if ((newOffsetX - oldOffsetX) - Math.abs(oldOffsetY - newOffsetY) > 20 && (newOffsetX - oldOffsetX) > 20) {
                                onFloatBallFlipRight();
                            }
                            onClearOffset();

                        }

                        tag = 0;
                        break;
                }
                //如果拖动则返回false，否则返回true
                if (ismoving == false) {
                    return false;
                } else {
                    return true;
                }
            }

        });

    }

    /**
     * 将状态数据保存在sharepreferences
     * @param name
     * @param number
     */
    public void saveStates(String name,int number)
    {
        Editor editor = sp.edit();
        editor.putInt(name, number);
        editor.commit();

    }

    /**
     * 清空滑动位移量
     */
    private void onClearOffset()
    {
        ballWmParams.x = oldOffsetX;
        ballWmParams.y = oldOffsetY;
    }


    /**
     * 双击悬浮球
     */
    private void onFloatBallDoubleClick(){

        FloatingBallUtils.simulateKey(KeyEvent.KEYCODE_APP_SWITCH);

    }
    /**
     * 点击悬浮球返回
     */

    private  void  onFloatBallClick(){

        FloatingBallUtils.simulateKey(KeyEvent.KEYCODE_BACK);
    }
    /**
     * 悬浮球向上滑动，弹出menu
     */

    private  void  onFloatBallFlipUp(){


        FloatingBallUtils.simulateKey(KeyEvent.KEYCODE_MENU);
    }
    /**
     * 悬浮球向下滑动，回到桌面
     */

    private  void  onFloatBallFlipDown(){


        FloatingBallUtils.simulateKey(KeyEvent.KEYCODE_HOME);
    }
    /**
     * 悬浮球向左滑动，关闭屏幕
     */

    private  void  onFloatBallFlipLeft(){


        FloatingBallUtils.simulateKey(KeyEvent.KEYCODE_POWER);
    }
    /**
     * 悬浮球向右滑动，打开任务面板
     */

    private  void  onFloatBallFlipRight(){


        FloatingBallUtils.simulateKey(KeyEvent.KEYCODE_APP_SWITCH);
    }
    /**
     * 更新view的显示位置
     */
    private void updateViewPosition() {

        wm.updateViewLayout(ballView, ballWmParams);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onClick(View v) {
        /*switch (v.getId()) {
            case R.id.lay_main:
                Toast.makeText(getApplicationContext(), "111", Toast.LENGTH_SHORT).show();
                break;

            default:
                if(pop!=null && pop.isShowing()){
                    pop.dismiss();
                }
                break;
        }*/

    }

    @Override
    public boolean onKey(View v, int keyCode, KeyEvent event) {
        Toast.makeText(getApplicationContext(), "keyCode:"+keyCode, Toast.LENGTH_SHORT).show();
        switch (keyCode) {
            case KeyEvent.KEYCODE_HOME:
                pop.dismiss();
                break;
            default:
                break;
        }
        return true;
    }

    public void closeFloatBall()
    {
        if (isAdd)
        {
            wm.removeView(ballView);
            isAdd = !isAdd;
        }


    }
    public void  showFloatBall()
    {

        if(!isAdd)
        {
            wm.addView(ballView, ballWmParams);
            isAdd = !isAdd;
        }

    }

    @Override
    public void onDestroy() {

        closeFloatBall();

        //销毁时停止前台
        stopForeground(true);
        super.onDestroy();
    }
}

package com.scut.danmaku;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class MainActivity extends AppCompatActivity {
    private FrameLayout mContainer;
    private DanmakuManager mManager;
    private EditText mEtSend;
    private Handler mHandler = new Handler();
    private Runnable mRandomDanmakuTask = new Runnable() {
        @Override
        public void run() {
            Danmaku danmaku = new Danmaku("abcdefgaaaa",80,Danmaku.Mode.scroll,Danmaku.COLOR_RED);
            mManager.send(danmaku);
            mHandler.postDelayed(this, RandomUtil.nextInt(500, 1000));
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        initDanmaku();
//        for(int i=0;i<10;i++) {
//            Danmaku danmaku = new Danmaku("abcdefgaaaa"+i,100,Danmaku.Mode.scroll,Danmaku.COLOR_RED);
//            mManager.send(danmaku);
//        }
        mHandler.post(mRandomDanmakuTask);
    }

    private void initDanmaku() {

        mManager = DanmakuManager.getInstance();
        mManager.init(this, mContainer); // 必须首先调用init方法
        mManager.setMaxDanmakuSize(120); // 设置同屏最大弹幕数

        DanmakuManager.Config config = mManager.getConfig(); // 弹幕相关设置
        boolean isPortrait = ScreenUtil.getScreenHeight() > ScreenUtil.getScreenWidth();
        config.setDurationTop(5000); // 设置顶部弹幕显示时长，默认6秒
        config.setDurationScroll(isPortrait ? 5000 : 10000); // 设置滚动字幕显示时长，默认10秒
        config.setMaxScrollLine(12); // 设置滚动字幕最大行数
        config.setLineHeight(ScreenUtil.autoSize(isPortrait ? 100 : 120)); // 设置行高

    }


    private void initView() {
        mContainer = findViewById(R.id.danmakuContainer);
        mEtSend = findViewById(R.id.etSend);
        mEtSend.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                hideIme();
            }
        });
        mEtSend.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == 100 || actionId == EditorInfo.IME_ACTION_SEND || actionId == EditorInfo.IME_ACTION_DONE) {
                String text =mEtSend.getText().toString();
                Danmaku danmaku = new Danmaku(text,80,Danmaku.Mode.scroll,Danmaku.COLOR_RED);
                mManager.send(danmaku);
                hideIme();
                mEtSend.clearFocus();
                mEtSend.setText("");
                return true;
            }
            return false;
        });
    }


    private void hideIme() {
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(mEtSend.getWindowToken(), 0);
        }
        hideStatusBar();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void hideStatusBar() {
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN);
    }

}
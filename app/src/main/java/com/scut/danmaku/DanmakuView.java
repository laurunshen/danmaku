package com.scut.danmaku;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.util.AttributeSet;

import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.Scroller;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class DanmakuView extends View {

    //弹幕内容
    private Danmaku mDanmaku;

    //弹幕画笔
    private Paint danmakuPaint;


    /**
     * 监听
     */
    private ListenerInfo mListenerInfo;

    private class ListenerInfo {
        private ArrayList<OnEnterListener> mOnEnterListeners;

        private List<OnExitListener> mOnExitListener;
    }

    /**
     * 弹幕进场时的监听
     */
    public interface OnEnterListener {
        void onEnter(DanmakuView view);
    }

    /**
     * 弹幕离场后的监听
     */
    public interface OnExitListener {
        void onExit(DanmakuView view);
    }

    //显示时长 ms
    private int mDuration;

    private Scroller mScroller;

    public DanmakuView(Context context) {
        super(context);
    }

    public DanmakuView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public DanmakuView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    private void init() {
        danmakuPaint = new Paint();
        danmakuPaint.setAntiAlias(true);
        danmakuPaint.setColor(Color.parseColor(mDanmaku.color));
        danmakuPaint.setTextSize(mDanmaku.size);
        danmakuPaint.setStyle(Paint.Style.FILL);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawDanmaku(canvas);
    }


    private void drawDanmaku(Canvas canvas) {
        String text = mDanmaku.text;
        if(mDanmaku.imgBitmap!=null) {
            Bitmap bitmap = mDanmaku.imgBitmap;
            bitmap = getScaleBitmap(bitmap,mDanmaku.size*2/3,mDanmaku.size*2/3);
            canvas.drawBitmap(getCirleBitmap(bitmap), mDanmaku.size/6, (int)danmakuPaint.getTextSize()/3, danmakuPaint);
        }
        canvas.drawText(text, mDanmaku.size, danmakuPaint.getTextSize(), danmakuPaint);
    }


    /*
        设置弹幕内容
         */
    public void setDanmaku(Danmaku danmaku) {
        this.mDanmaku = danmaku;
        init();
    }

    /*
    显示弹幕
     */
    public void show(final ViewGroup parent, int duration) {
        mDuration = duration;
        showScrollDanmaku(parent, duration);

        postDelayed(() -> {
            setVisibility(GONE);
            if (hasOnExitListener()) {
                for (OnExitListener listener : getListenerInfo().mOnExitListener) {
                    listener.onExit(DanmakuView.this);
                }
            }
            parent.removeView(DanmakuView.this);
        }, duration);
    }

    private void showScrollDanmaku(ViewGroup parent, int duration) {
        int screenWidth = ScreenUtil.getScreenWidth();
        int textLength = (int)danmakuPaint.measureText(mDanmaku.text);
        int imgLength =   0;
        if(mDanmaku.imgBitmap!=null) {
            imgLength = mDanmaku.size;
        }
        scrollTo(-screenWidth, 0);
        parent.addView(this);
        smoothScrollTo(textLength+imgLength,0,duration);
    }

    private void smoothScrollTo(int x, int y, int duration) {
        if (mScroller == null) {
            mScroller = new Scroller(getContext(), new LinearInterpolator());

        }
        int sx = getScrollX();
        int sy = getScrollY();
        mScroller.startScroll(sx,sy,x-sx,y-sy,duration);
    }

    @Override
    public void computeScroll() {
        if (mScroller != null && mScroller.computeScrollOffset()) {
            scrollTo(mScroller.getCurrX(), mScroller.getCurrY());
            postInvalidate();
        }
    }

    public Danmaku getDanmaku() {
        return mDanmaku;
    }

    private ListenerInfo getListenerInfo() {
        if(mListenerInfo == null) {
            mListenerInfo = new ListenerInfo();
        }
        return mListenerInfo;
    }

    public void addOnEnterListener(OnEnterListener listener) {
        ListenerInfo info = getListenerInfo();
        if(info.mOnEnterListeners == null) {
            info.mOnEnterListeners = new ArrayList<>();
        }
        if(!info.mOnEnterListeners.contains(listener)) {
            info.mOnEnterListeners.add(listener);
        }
    }

    public void clearOnEnterListeners() {
        ListenerInfo info = getListenerInfo();
        if(info.mOnEnterListeners == null || info.mOnEnterListeners.size() == 0) {
            return;
        }
        info.mOnEnterListeners.clear();
    }

    public void addOnExitListeners(OnExitListener listener) {
        ListenerInfo info = getListenerInfo();
        if (info.mOnExitListener == null) {
            info.mOnExitListener = new CopyOnWriteArrayList<>();
        }
        if (!info.mOnExitListener.contains(listener)) {
            info.mOnExitListener.add(listener);
        }
    }

    public void clearOnExitListeners() {
        ListenerInfo info = getListenerInfo();
        if (info.mOnExitListener == null || info.mOnExitListener.size() == 0) {
            return;
        }
        info.mOnExitListener.clear();
    }

    public boolean hasOnEnterListener() {
        ListenerInfo info = getListenerInfo();
        return info.mOnEnterListeners != null && info.mOnEnterListeners.size() != 0;
    }

    public boolean hasOnExitListener() {
        ListenerInfo info = getListenerInfo();
        return info.mOnExitListener != null && info.mOnExitListener.size() != 0;
    }

    void callExitListener() {
        for (OnExitListener listener : getListenerInfo().mOnExitListener) {
            listener.onExit(this);
        }
    }

    public int getViewLength() {
        int textLength = (int)danmakuPaint.measureText(mDanmaku.text);
        int imgLength = mDanmaku.size;
        return textLength + imgLength;
    }

    /**
     * 恢复初始状态
     */
    public void restore() {
        clearOnEnterListeners();
        clearOnExitListeners();
        setVisibility(VISIBLE);
        setScrollX(0);
        setScrollY(0);
    }


    public static DanmakuView createDanmakuView(Context context, ViewGroup parent) {
        return (DanmakuView) LayoutInflater.from(context)
                .inflate(R.layout.danmaku_view, parent, false);
    }

    public static Bitmap getScaleBitmap(Bitmap bm, int newWidth ,int newHeight){
        // 获得图片的宽高
        int width = bm.getWidth();
        int height = bm.getHeight();
        // 计算缩放比例
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;
        // 取得想要缩放的matrix参数
        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleHeight);
        // 得到新的图片
        Bitmap newbm = Bitmap.createBitmap(bm, 0, 0, width, height, matrix, true);
        return newbm;
    }

    public Bitmap getCirleBitmap(Bitmap bmp) {
        //获取bmp的宽高 小的一个做为圆的直径r
        int w = bmp.getWidth();
        int h = bmp.getHeight();
        int r = Math.min(w, h);

        //创建一个paint
        Paint paint = new Paint();
        paint.setAntiAlias(true);

        //新创建一个Bitmap对象newBitmap 宽高都是r
        Bitmap newBitmap = Bitmap.createBitmap(r, r, Bitmap.Config.ARGB_8888);

        //创建一个使用newBitmap的Canvas对象
        Canvas canvas = new Canvas(newBitmap);

        //canvas画一个圆形
        canvas.drawCircle(r / 2, r / 2, r / 2, paint);

       //然后 paint要设置Xfermode 模式为SRC_IN 显示上层图像（后绘制的一个）的相交部分
       paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));

        //canvas调用drawBitmap直接将bmp对象画在画布上 因为paint设置了Xfermode，所以最终只会显示这个bmp的一部分 也就
        //是bmp的和下层圆形相交的一部分圆形的内容
        canvas.drawBitmap(bmp, 0, 0, paint);

        return newBitmap;
    }
}

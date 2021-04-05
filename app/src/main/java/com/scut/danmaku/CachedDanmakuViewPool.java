package com.scut.danmaku;

import java.util.LinkedList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class CachedDanmakuViewPool implements Pool<DanmakuView> {
    private static final String TAG = "CacheDanmakuViewPool";

    /**
     * 缓存DanmakuView队列。显示已经完毕的DanmakuView会被添加到缓存中进行复用。
     * 在一定的时间过后，没有被访问到的DanmakuView会被回收。
     */
    private LinkedList<DanmakuViewWithExpireTime> mCache = new LinkedList<>();

    //缓存存活时间
    private long mKeepAliveTime;

    //定时清理缓存
    private ScheduledExecutorService mChecker = Executors.newSingleThreadScheduledExecutor();

    /**
     * 最大DanmakuView数量。
     * 这个数量包含了正在显示的DanmakuView和已经显示完毕进入缓存等待复用的DanmakuView之和。
     */
    private int mMaxSize;

    //正在显示的弹幕数量
    private int mInUseSize;

    /**
     * 创建新DanmakuView的Creator
     */
    private ViewCreator<DanmakuView> mCreator;

    CachedDanmakuViewPool(long keepAliveTime, int maxSize, ViewCreator<DanmakuView> creator) {
        mKeepAliveTime = keepAliveTime;
        mMaxSize = maxSize;
        mInUseSize = 0;
        mCreator = creator;
        scheduleCheckUnusedViews();
    }

    /**
     * 每隔一秒检查并清理掉空闲队列中超过一定时间没有被使用的DanmakuView
     */
    private void scheduleCheckUnusedViews() {
        mChecker.scheduleWithFixedDelay(() -> {
            long current = System.currentTimeMillis();
            while (!mCache.isEmpty()) {
                DanmakuViewWithExpireTime first = mCache.getFirst();
                if (current > first.expireTime) {
                    mCache.remove(first);
                } else {
                    break;
                }
            }
        }, 1000, 1000, TimeUnit.MILLISECONDS);
    }

    @Override
    public DanmakuView get() {
        DanmakuView view;
        if (mCache.isEmpty()) { //缓存中没有view
            if (mInUseSize >= mMaxSize) {
                return null;
            }
            view = mCreator.create();
        } else { //缓存中有可用的view
            view = mCache.poll().danmakuView;
        }
        view.addOnExitListeners(new DanmakuView.OnExitListener() {
            @Override
            public void onExit(DanmakuView view) {
                long expire = System.currentTimeMillis() + mKeepAliveTime;
                view.restore();
                DanmakuViewWithExpireTime item = new DanmakuViewWithExpireTime();
                item.danmakuView = view;
                item.expireTime = expire;
                mCache.offer(item);
                mInUseSize--;
            }
        });
        mInUseSize++;
        return view;
    }

    @Override
    public void release() {
        mCache.clear();
    }

    @Override
    public int count() {
        //返回使用中的DanmakuView和缓存中的DanmakuView数量之和
        return mInUseSize + mCache.size();
    }

    @Override
    public void setMaxSize(int max) {
        mMaxSize = max;
    }

    //保存一个DanmakuView和它的过期时间
    private class DanmakuViewWithExpireTime {
        private DanmakuView danmakuView; // 缓存的DanmakuView
        private long expireTime; // 超过这个时间没有被访问的缓存将被丢弃
    }

    public interface ViewCreator<T> {
        T create();
    }

}

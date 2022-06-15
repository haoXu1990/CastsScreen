package com.android.zxkj.dlna.dmr.media;

import android.content.Context;
import android.graphics.Color;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.FrameLayout;
import android.widget.MediaController;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;


import java.io.IOException;
import java.util.Map;

import tv.danmaku.ijk.media.player.IMediaPlayer;
import tv.danmaku.ijk.media.player.IjkMediaPlayer;

public class ZxineVideoView extends FrameLayout {

    private IMediaPlayer mMediaPlayer = null;

    private String mVideoUrl;
    private Map<String,String> mHeader;

    private SurfaceView mSurfaceView;

    private Context mContext;
    private boolean mEnableMediaCodec = false;
    private ZxineVideoListener mListener;

    IMediaPlayer.OnPreparedListener mPreparedListener = new IMediaPlayer.OnPreparedListener() {
        public void onPrepared(IMediaPlayer mp) {
            mListener.onPrepared(mp);
        }
    };

    IMediaPlayer.OnVideoSizeChangedListener mSizeChangedListener =
            (mp, width, height, sarNum, sarDen) -> {

            };

    public ZxineVideoView(@NonNull Context context) {
        this(context, null);
    }

    public ZxineVideoView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }
    public ZxineVideoView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }
    private void init(Context context) {
        mContext = context;
        setBackgroundColor(Color.BLACK);
        createSurfaceView();
    }

    private void createSurfaceView() {
        mSurfaceView = new SurfaceView(mContext);
        mSurfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder surfaceHolder) {
                if (mMediaPlayer != null) {
                    mMediaPlayer.setDisplay(surfaceHolder);
                }
            }

            @Override
            public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder surfaceHolder) {

            }
        });
        LayoutParams layoutParams = new LayoutParams(LayoutParams.MATCH_PARENT
                , LayoutParams.MATCH_PARENT, Gravity.CENTER);
        addView(mSurfaceView,0,layoutParams);
    }

    //创建一个新的player
    private IMediaPlayer createPlayer() {
        IjkMediaPlayer ijkMediaPlayer = new IjkMediaPlayer();
        // 设置 Option 会崩溃, 不知道原因, 感觉是线程问题
        // A/libc: Fatal signal 11 (SIGSEGV), code 2 (SEGV_ACCERR), fault addr 0x7150e86b10 in tid 16752 (Thread-4), pid 16499 (d.zxkj.renderer)
//        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "opensles", 1);
//
//        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "overlay-format", IjkMediaPlayer.SDL_FCC_RV32);
//        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "framedrop", 1);
//        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "start-on-prepared", 0);
//
//        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "http-detect-range-support", 1);
//
        // 环路过滤 0 开启 画面质量高， 解码开销高
        // 48关闭 画面质量差， 解码开销小
//        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_CODEC, "skip_loop_filter", 48);
//        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_CODEC, "min-frames", 100);

        // seek 优化
        // 有些视频在 seekto 的时候会出现画面回跳，这是关键帧的问题，seek 只支持关键帧，出现这种情况是原始视频中的 i 帧比较少
//        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "enable-accurate-seek", 1);

        ijkMediaPlayer.setVolume(1.0f, 1.0f);

        setEnableMediaCodec(ijkMediaPlayer,mEnableMediaCodec);
        return ijkMediaPlayer;
    }

    //设置是否开启硬解码
    private void setEnableMediaCodec(IjkMediaPlayer ijkMediaPlayer, boolean isEnable) {
        int value = isEnable ? 1 : 0;
        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec", value);//开启硬解码
        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec-auto-rotate", value);
        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec-handle-resolution-change", value);
    }

    public void setEnableMediaCodec(boolean isEnable){
        mEnableMediaCodec = isEnable;
    }

    //设置ijkplayer的监听
    private void setListener(IMediaPlayer player){
        player.setOnPreparedListener(mPreparedListener);
        player.setOnVideoSizeChangedListener(mSizeChangedListener);
    }

    /**
     * 设置自己的player回调
     */
    public void setVideoListener(ZxineVideoListener listener){
        mListener = listener;
    }

    //设置播放地址
    public void setPath(String path) {
        setPath(path,null);
    }

    public void setPath(String path,Map<String,String> header){
        mVideoUrl = path;
        mHeader = header;
    }

    //开始加载视频
    public void load() throws IOException {
        if(mMediaPlayer != null){
            mMediaPlayer.stop();
            mMediaPlayer.release();
        }
        mMediaPlayer = createPlayer();
        setListener(mMediaPlayer);
        mMediaPlayer.setDisplay(mSurfaceView.getHolder());
        mMediaPlayer.setDataSource(mContext, Uri.parse(mVideoUrl),mHeader);

        mMediaPlayer.prepareAsync();
    }

    public void start() {
        if (mMediaPlayer != null) {
            mMediaPlayer.start();
        }
    }

    public void release() {
        if (mMediaPlayer != null) {
            mMediaPlayer.reset();
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
    }

    public void pause() {
        if (mMediaPlayer != null) {
            mMediaPlayer.pause();
        }
    }

    public void stop() {
        if (mMediaPlayer != null) {
            mMediaPlayer.stop();
        }
    }


    public void reset() {
        if (mMediaPlayer != null) {
            mMediaPlayer.reset();
        }
    }


    public long getDuration() {
        if (mMediaPlayer != null) {
            return mMediaPlayer.getDuration();
        } else {
            return 0L;
        }
    }


    public long getCurrentPosition() {
        if (mMediaPlayer != null) {
            return mMediaPlayer.getCurrentPosition();
        } else {
            return 0L;
        }
    }


    public void seekTo(long l) {
        if (mMediaPlayer != null) {
            mMediaPlayer.seekTo(l);
        }
    }

    public boolean isPlaying(){
        if(mMediaPlayer != null) {
            return mMediaPlayer.isPlaying();
        }
        return false;
    }
}

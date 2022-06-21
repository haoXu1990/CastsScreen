package com.android.zxkj.dlna.dmr.media;

import android.content.Context;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.net.Uri;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;


import java.io.IOException;
import java.util.Map;

import tv.danmaku.ijk.media.player.IMediaPlayer;
import tv.danmaku.ijk.media.player.IjkMediaPlayer;

public class ZxineVideoView extends SurfaceView {

    private static final String TAG = "ZxineVideoView";

    // 媒体播放器
    private IMediaPlayer mMediaPlayer = null;

    // SurfaceHolder
    private SurfaceHolder mSurfaceHolder;

    private View loaddingView;

    // 视频宽
    private int mVideoHeight;

    // 视频高
    private int mVideoWidth;

    // SurfaceView高
    private int mSurfaceHeight;

    // SurfaceView宽
    private int mSurfaceWidth;

    // 播放地址
    private String mVideoUrl;

    // 是否硬解码
    private boolean mEnableMediaCodec = false;

    // 播放器是否已经准备好
    private boolean isPrepared = false;

    // 当前进度
    private int mCurrentPos;
    // 当前播放视频时长
    private int mDuration = -1;

    // 当前缓冲进度--网络
    private int mCurrentBufferPer;

    private Map<String,String> mHeader;

    private Context mContext;

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
        setFocusable(true);
        setFocusableInTouchMode(true);
        requestFocus();


        getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(@NonNull SurfaceHolder surfaceHolder) {
                mSurfaceHolder = surfaceHolder;
                openVideo();
            }

            @Override
            public void surfaceChanged(@NonNull SurfaceHolder surfaceHolder, int i, int i1, int i2) {
                mSurfaceHeight = i1;
                mSurfaceWidth = i2;
                if (mMediaPlayer != null && isPrepared) {
                    initPosition();
                    mMediaPlayer.start();//开始播放
                    //开启面板
                }
            }

            @Override
            public void surfaceDestroyed(@NonNull SurfaceHolder surfaceHolder) {
                mSurfaceHolder = null;
                release();
            }
        });

    }

    /*
    * 设置指示器
    * */
    public void setLoaddingView(View view) {
        loaddingView = view;
    }

    /**
     * 初始化最初位置
     */
    private void initPosition() {
        if (mCurrentPos != 0) {
            mMediaPlayer.seekTo(mCurrentPos);
            mCurrentPos = 0;
        }
    }

    // 创建一个新的player
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

    // 设置是否开启硬解码
    private void setEnableMediaCodec(IjkMediaPlayer ijkMediaPlayer, boolean isEnable) {
        int value = isEnable ? 1 : 0;
        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec", value);//开启硬解码
        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec-auto-rotate", value);
        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec-handle-resolution-change", value);
    }

    /*
    *  打开播放器
    * */
    private void openVideo() {
        if (mVideoUrl == null || mSurfaceHolder == null) {
            Log.d(TAG, "openVideo: VideoUrl == null || mSurfaceHolder == null");
            return;
        }
        release();
        isPrepared = false;
        showLoaddingView(true);
        mMediaPlayer = createPlayer();
        try {

            mMediaPlayer.setDisplay(mSurfaceHolder);
            mMediaPlayer.setDataSource(mContext, Uri.parse(mVideoUrl),mHeader);
            // 播放时不熄屏
            mMediaPlayer.setScreenOnWhilePlaying(true);
            // 异步准备
            mMediaPlayer.prepareAsync();

            mMediaPlayer.setOnVideoSizeChangedListener((iMediaPlayer, i, i1, i2, i3) -> {
                mVideoWidth = i2;
                mVideoHeight = i3;
                if (mOnSizeChanged != null) {
                    mOnSizeChanged.onSizeChange();
                }
                if (mVideoWidth != 0 && mVideoHeight != 0) {
                    fitVideoSize(mVideoWidth, mVideoHeight, mSurfaceWidth, mSurfaceHeight);
                }
            });

            // 准备监听
            mMediaPlayer.setOnPreparedListener(iMediaPlayer -> {
                isPrepared = true;
                showLoaddingView(false);
                if (mOnPreparedListener != null) {//补偿回调
                    mOnPreparedListener.onPrepared(iMediaPlayer);
                }

                if (mVideoWidth != 0 && mVideoHeight != 0) {
                    //开始初始化
                    initPosition();
                    start();
                }
            });

            mMediaPlayer.setOnCompletionListener(iMediaPlayer -> {
                start();
                if (mOnCompletionListener != null) {
                    mOnCompletionListener.onCompletion(iMediaPlayer);
                }
            });

            mMediaPlayer.setOnErrorListener((iMediaPlayer, i, i1) -> {
                //隐藏面板
                if (mOnErrorListener != null) {
                    mOnErrorListener.onError(iMediaPlayer, i, i1);
                }
                return true;
            });

            mMediaPlayer.setOnInfoListener((iMediaPlayer, i, i1) -> {

                switch (i) {
                    case IMediaPlayer.MEDIA_INFO_BUFFERING_START:
                        showLoaddingView(true);
                        break;
                    case IMediaPlayer.MEDIA_INFO_BUFFERING_END:
                        showLoaddingView(false);
                        break;
                    default:
                        break;
                }
                return false;
            });

            mMediaPlayer.setOnBufferingUpdateListener(new IMediaPlayer.OnBufferingUpdateListener() {
                @Override
                public void onBufferingUpdate(IMediaPlayer iMediaPlayer, int i) {
                    mCurrentBufferPer = i;
                    if (mListener != null) {
                        mListener.onBufferingUpdate(iMediaPlayer, i);
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "openVideo: ", e);
        }

    }

    /*
    *  显示 正在加载指示器
    * */
    public void showLoaddingView(boolean isShow) {
        if (loaddingView != null) {
            loaddingView.setVisibility(isShow ? View.VISIBLE : View.INVISIBLE);
        }
    }

    public void fitVideoSize() {
        float rateY = 1;//高不变
        float rateX = mVideoWidth * 1.f / mVideoHeight;
        fitVideoSize(mVideoWidth, mVideoHeight, mSurfaceWidth, mSurfaceHeight);
    }

    public void fitSize16_9() {
        float rateY = 1;//高不变
        float rate = 16.f / 9;

        float W = rate * mVideoHeight;
        float rateX = W / mVideoWidth;//高不变
        //W:H =16:9 ------ W= 16/9*H

        fitVideoSize(mVideoWidth, mVideoHeight, mSurfaceWidth, mSurfaceHeight);
    }

    public void setEnableMediaCodec(boolean isEnable){
        mEnableMediaCodec = isEnable;
    }

    public void fitVideoSize(
            int videoW, int videoH, int surfaceW, int surfaceH) {


        float ratio = videoW * 1.f / videoH;

        //surfaceW:surfaceH=videoW:videoH --> surfaceW=videoW:videoH*surfaceH

        surfaceW = (int) (ratio * surfaceH);

        //无法直接设置视频尺寸，将计算出的视频尺寸设置到surfaceView 让视频自动填充。
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(surfaceW, surfaceH);
        params.gravity = 0x13;
        setLayoutParams(params);
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
        mCurrentPos = 0;
        openVideo();
        requestLayout();
        invalidate();
    }

    public void setPath(String path,Map<String,String> header){
        mVideoUrl = path;
        mHeader = header;
    }


    public void start() {
        if (mMediaPlayer != null) {
            mMediaPlayer.start();
        }
    }

    public void release() {
        Log.d(TAG, "releasePlayer: ");
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




    //----------------------------------------------------------------
    //------------补偿回调---------------------------
    //----------------------------------------------------------------
    private IMediaPlayer.OnPreparedListener mOnPreparedListener;
    private IMediaPlayer.OnCompletionListener mOnCompletionListener;
    private IMediaPlayer.OnErrorListener mOnErrorListener;

    public void setOnPreparedListener(IMediaPlayer.OnPreparedListener onPreparedListener) {
        mOnPreparedListener = onPreparedListener;
    }

    public void setOnCompletionListener(IMediaPlayer.OnCompletionListener onCompletionListener) {
        mOnCompletionListener = onCompletionListener;
    }

    public void setOnErrorListener(IMediaPlayer.OnErrorListener onErrorListener) {
        mOnErrorListener = onErrorListener;
    }

    public interface OnSizeChanged {
        void onSizeChange();
    }

    private OnSizeChanged mOnSizeChanged;

    public void setOnSizeChanged(OnSizeChanged onSizeChanged) {
        mOnSizeChanged = onSizeChanged;
    }

    public interface OnProgressChanged {
        void onChange(int per_100, int position);
    }

    private OnProgressChanged mOnProgressChanged;

    public void setOnProgressChanged(OnProgressChanged onProgressChanged) {
        mOnProgressChanged = onProgressChanged;
    }
}

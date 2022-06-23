package com.android.zxkj.dlna.dmr;

import android.app.ActivityOptions;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.display.DisplayManager;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.android.zxkj.dlna.dmr.media.ZxineVideoListener;
import com.android.zxkj.dlna.dmr.media.ZxineVideoView;
import com.android.zxkj.dlna.dmr.widget.MyImageView;
import com.shuyu.gsyvideoplayer.GSYVideoManager;
import com.shuyu.gsyvideoplayer.listener.GSYSampleCallBack;
import com.shuyu.gsyvideoplayer.utils.GSYVideoType;
import com.shuyu.gsyvideoplayer.utils.OrientationUtils;
import com.shuyu.gsyvideoplayer.video.StandardGSYVideoPlayer;
//import com.google.android.exoplayer2.ExoPlaybackException;
//import com.google.android.exoplayer2.ExoPlayer;
//import com.google.android.exoplayer2.MediaItem;
//import com.google.android.exoplayer2.Player;
//import com.google.android.exoplayer2.SimpleExoPlayer;
//import com.google.android.exoplayer2.ui.PlayerView;
//import com.shuyu.gsyvideoplayer.listener.VideoAllCallBack;
//import com.shuyu.gsyvideoplayer.video.StandardGSYVideoPlayer;

import org.fourthline.cling.model.types.UnsignedIntegerFourBytes;
import org.fourthline.cling.support.avtransport.lastchange.AVTransportVariable;
import org.fourthline.cling.support.model.Channel;
import org.fourthline.cling.support.model.TransportState;
import org.fourthline.cling.support.renderingcontrol.lastchange.ChannelVolume;
import org.fourthline.cling.support.renderingcontrol.lastchange.RenderingControlVariable;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import tv.danmaku.ijk.media.player.IMediaPlayer;

public class DLNARenderActivity extends AppCompatActivity {
    private static final String KEY_EXTRA_CURRENT_URI = "Renderer.KeyExtra.CurrentUri";
    private static final String TAG = "DLNARenderActivity";
    public static void startActivity(Context context, String currentURI) {
        Intent intent = new Intent(context, DLNARenderActivity.class);
        intent.putExtra(KEY_EXTRA_CURRENT_URI, currentURI);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        context.startActivity(intent);
//        context.startActivity(intent, getOption(context).toBundle());
    }

    static ActivityOptions getOption(Context context) {
        int display = 0;
        ActivityOptions options = ActivityOptions.makeBasic();
        DisplayManager displayManager = (DisplayManager)context.getApplicationContext().getSystemService(Context.DISPLAY_SERVICE);
        Display[] presentationDisplays = displayManager.getDisplays(null);
        if (presentationDisplays.length > 0) {
            Display presentationDisplay = presentationDisplays[presentationDisplays.length - 1];
            display = presentationDisplay.getDisplayId();
        }
        options.setLaunchDisplayId(display);

        return options;
    }
    private final UnsignedIntegerFourBytes INSTANCE_ID = new UnsignedIntegerFourBytes(0);

    private ProgressBar mProgressBar;

    private DLNARendererService mRendererService;

    private ZxineVideoView mVideoView;

    private MyImageView mImageView;

    private StandardGSYVideoPlayer mGSYVideoPlayer;
    private OrientationUtils orientationUtils;

    private final ZxineVideoListener mZxineVideoListener = new ZxineVideoListener() {
        @Override
        public void onPrepared(IMediaPlayer iMediaPlayer) {
            mVideoView.start();
        }

        @Override
        public void onBufferingUpdate(IMediaPlayer iMediaPlayer, int pre) {
            Log.d(TAG, "onBufferingUpdate: " + pre);
        }
    };

    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mRendererService = ((DLNARendererService.RendererServiceBinder) service).getRendererService();
            mRendererService.setRenderControl(new IDLNARenderControl.GSYVideoViewRenderControl(mGSYVideoPlayer));
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mRendererService = null;
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dlna_renderer);

        initCompentGSY();
//        initCompent();
        bindService(new Intent(this, DLNARendererService.class), mServiceConnection, Service.BIND_AUTO_CREATE);
        openMedia(getIntent());
    }

    private void initCompentGSY() {
        mProgressBar = findViewById(R.id.video_progress);
        mProgressBar.setVisibility(View.INVISIBLE);
        mImageView = findViewById(R.id.image_view);
        mImageView.setVisibility(View.INVISIBLE);
        mGSYVideoPlayer = findViewById(R.id.video_view);

        // 关闭全屏动画
        mGSYVideoPlayer.setShowFullAnimation(false);

        // 设置返回按钮
        mGSYVideoPlayer.getBackButton().setVisibility(View.VISIBLE);

        mGSYVideoPlayer.getBackButton().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                finish();
                notifyTransportStateChanged(TransportState.PLAYING);
            }
        });

        mGSYVideoPlayer.setVideoAllCallBack(new GSYSampleCallBack() {
            @Override
            public void onPrepared(String url, Object... objects) {
                super.onPrepared(url, objects);
                orientationUtils.setEnable(true);
                Log.d(TAG, "onPrepared: ");
                notifyTransportStateChanged(TransportState.PLAYING);

                // 开启全屏
                // 有问题： 这里开启全屏后，loadding hud 不会隐藏
//                mGSYVideoPlayer.startWindowFullscreen(DLNARenderActivity.this, true, true);

            }
        });

        mGSYVideoPlayer.getFullscreenButton().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // 开启/退出 全屏
                mGSYVideoPlayer.startWindowFullscreen(DLNARenderActivity.this, true, true);
            }
        });

        // 绘制模式
        GSYVideoType.setRenderType(GSYVideoType.GLSURFACE);
        GSYVideoType.setShowType(GSYVideoType.SCREEN_TYPE_DEFAULT);


        mGSYVideoPlayer.setNeedOrientationUtils(true);
        orientationUtils = new OrientationUtils(this, mGSYVideoPlayer);

        // 手动旋转
//        orientationUtils.resolveByClick();

    }
    private void initCompent() {
        mProgressBar = findViewById(R.id.video_progress);

        mImageView = findViewById(R.id.image_view);
        mImageView.setVisibility(View.INVISIBLE);

        mVideoView = findViewById(R.id.video_view);
        mVideoView.setVideoListener(mZxineVideoListener);
        mVideoView.setOnPreparedListener(new IMediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(IMediaPlayer mp) {
                Log.d(TAG, "onPrepared:  发送开始播放通知");

                notifyTransportStateChanged(TransportState.PLAYING);
                notifyRenderVolumeChanged(10);
            }
        });

        mVideoView.setLoaddingView(mProgressBar);

        // 不开启硬解码
        mVideoView.setEnableMediaCodec(false);
    }


    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        openMedia(intent);
    }

    private void openMedia(Intent intent) {
        Bundle bundle = intent.getExtras();
        if (bundle != null) {
            mProgressBar.setVisibility(View.VISIBLE);
            String currentUri = bundle.getString(KEY_EXTRA_CURRENT_URI);
            Log.d(TAG, "open Media Uir: "+ currentUri);
            // 这个根据Uri 来判断播放音视频，还是播放图片
            if (currentUri.endsWith(".png") || currentUri.endsWith(".jpg")) {
                Log.d(TAG, "open Media Uir: "+ currentUri);
                mImageView.setVisibility(View.VISIBLE);
                mProgressBar.setVisibility(View.INVISIBLE);
                mImageView.setImageURL(currentUri);
            } else {

                mProgressBar.setVisibility(View.INVISIBLE);

                mGSYVideoPlayer.onVideoPause();
                mGSYVideoPlayer.onVideoReset();
                mGSYVideoPlayer.release();

                mGSYVideoPlayer.setUp(currentUri, true, "");
                mGSYVideoPlayer.startPlayLogic();
            }
        } else {
            Toast.makeText(this, "没有找到有效的视频地址，请检查...", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @Override
    protected void onDestroy() {
//        if (mVideoView != null) mVideoView.release();
        if (mGSYVideoPlayer != null) GSYVideoManager.releaseAllVideos();
        if (orientationUtils != null) orientationUtils.releaseListener();
        notifyTransportStateChanged(TransportState.STOPPED);
        unbindService(mServiceConnection);
        super.onDestroy();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        boolean handled = super.onKeyDown(keyCode, event);
        if (mRendererService != null) {
            if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_MUTE) {
                int volume = ((AudioManager) getApplication().getSystemService(Context.AUDIO_SERVICE)).getStreamVolume(AudioManager.STREAM_MUSIC);
                Log.d(TAG, "onKeyDown: " + volume);
                notifyRenderVolumeChanged(volume);
            } else if (keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
//                if (mGSYVideoPlayer != null && mGSYVideoPlayer.isPlaying()) {
//                    mVideoView.pause();
//                    notifyTransportStateChanged(TransportState.PAUSED_PLAYBACK);
//                } else if (mVideoView != null) {
//                    mVideoView.start();
//                    notifyTransportStateChanged(TransportState.PLAYING);
//                }
//                if (mVideoView != null && mVideoView.isPlaying()) {
//                    mVideoView.pause();
//                    notifyTransportStateChanged(TransportState.PAUSED_PLAYBACK);
//                } else if (mVideoView != null) {
//                    mVideoView.start();
//                    notifyTransportStateChanged(TransportState.PLAYING);
//                }
            }
        }
        return handled;
    }

    private void notifyTransportStateChanged(TransportState transportState) {
        if (mRendererService != null) {
            Log.d(TAG, "notifyTransportStateChanged: " + transportState.getValue());
//            mRendererService.
//            mRendererService.getRenderControlManager()
            mRendererService.getAvTransportLastChange()
                    .setEventedValue(
                            INSTANCE_ID,
                            new AVTransportVariable.TransportState(transportState)
                    );
        }
    }

    private void notifyRenderVolumeChanged(int volume) {
        if (mRendererService != null) {
            Log.d(TAG, "notifyRenderVolumeChanged: " + volume);
            mRendererService.getAudioControlLastChange()
                    .setEventedValue(INSTANCE_ID, new RenderingControlVariable.Volume(new ChannelVolume(Channel.Master, volume)));
        }
    }
}

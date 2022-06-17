package com.android.zxkj.dlna.dmr;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.android.zxkj.dlna.dmr.widget.MyImageView;

import org.fourthline.cling.model.types.UnsignedIntegerFourBytes;
import org.fourthline.cling.support.avtransport.lastchange.AVTransportVariable;
import org.fourthline.cling.support.model.Channel;
import org.fourthline.cling.support.model.TransportState;
import org.fourthline.cling.support.renderingcontrol.lastchange.ChannelVolume;
import org.fourthline.cling.support.renderingcontrol.lastchange.RenderingControlVariable;


public class DLNARenderActivity extends AppCompatActivity implements SurfaceHolder.Callback, MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener {
    private static final String KEY_EXTRA_CURRENT_URI = "Renderer.KeyExtra.CurrentUri";
    private static final String TAG = "DLNARenderActivity";
    public static void startActivity(Context context, String currentURI) {
        Intent intent = new Intent(context, DLNARenderActivity.class);
        intent.putExtra(KEY_EXTRA_CURRENT_URI, currentURI);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    private final UnsignedIntegerFourBytes INSTANCE_ID = new UnsignedIntegerFourBytes(0);

    private ProgressBar mProgressBar;
    private DLNARendererService mRendererService;

    private MyImageView mImageView;

    private MediaPlayer mVideoView;
    private SurfaceView mSurfaceView;
    private SurfaceHolder mSurfaceHolder;

    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Toast.makeText(getApplicationContext(), "服务连接成功", Toast.LENGTH_LONG).show();
            mRendererService = ((DLNARendererService.RendererServiceBinder) service).getRendererService();

            mRendererService.setRenderControl(new IDLNARenderControl.VideoViewRenderControl(mVideoView));
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mRendererService = null;
            Toast.makeText(getApplicationContext(), "服务断开连接", Toast.LENGTH_LONG).show();
            finish();
        }
    };


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dlna_renderer);

        initComponent();

        openMedia(getIntent());
    }

    private void initComponent() {
        // 图片
        mImageView = findViewById(R.id.image_view);
        mImageView.setVisibility(View.INVISIBLE);

        // 音视频
        mVideoView = new MediaPlayer();
        mVideoView.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mVideoView.setOnPreparedListener(this);
        mVideoView.setOnCompletionListener(this);
        mVideoView.setOnBufferingUpdateListener(new MediaPlayer.OnBufferingUpdateListener() {
            @Override
            public void onBufferingUpdate(MediaPlayer mediaPlayer, int i) {
                Log.d(TAG, "onBufferingUpdate: " + i);
            }
        });

        mVideoView.setOnVideoSizeChangedListener(new MediaPlayer.OnVideoSizeChangedListener() {
            @Override
            public void onVideoSizeChanged(MediaPlayer mediaPlayer, int i, int i1) {
                Log.d(TAG, "onVideoSizeChanged: " + i + " i1: " + i1);
            }
        });


        // MediaPlayer 需要的 SurfaceView
        mSurfaceView = findViewById(R.id.video_view);
        mSurfaceHolder = mSurfaceView.getHolder();
        mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        mSurfaceHolder.addCallback(this);

        // loading HUD
        mProgressBar = findViewById(R.id.video_progress);
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
            // 投屏的时候目前使用的是AVTransport
            if (currentUri.endsWith(".png") || currentUri.endsWith(".jpg")) {
                Log.d(TAG, "open Image Media Uir: "+ currentUri);
                if (mVideoView != null) {
                    mVideoView.stop();
                }
                mImageView.setVisibility(View.VISIBLE);
                mProgressBar.setVisibility(View.INVISIBLE);
                mSurfaceView.setVisibility(View.GONE);

                mImageView.setImageURL(currentUri);
            } else {
                Log.d(TAG, "open Video Media Uir: "+ currentUri);

                bindService(new Intent(this, DLNARendererService.class), mServiceConnection, Service.BIND_AUTO_CREATE);
                mImageView.setVisibility(View.INVISIBLE);
                mSurfaceView.setVisibility(View.VISIBLE);
                try {
                    mVideoView.setDataSource(currentUri);
                    mVideoView.prepare();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } else {
            Toast.makeText(this, "没有找到有效的视频地址，请检查...", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        if (mVideoView != null) {
            mVideoView.stop();
            mVideoView.release();
            mVideoView = null;
            mSurfaceHolder = null;
            mSurfaceView = null;
        }

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
                notifyRenderVolumeChanged(volume);
            } else if (keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
                if (mVideoView != null && mVideoView.isPlaying()) {
                    mVideoView.pause();
                    notifyTransportStateChanged(TransportState.PAUSED_PLAYBACK);
                } else if (mVideoView != null) {
                    mVideoView.start();
                    notifyTransportStateChanged(TransportState.PLAYING);
                }
            }
        }
        return handled;
    }

    // DMR notify,  有问题，DMC 端无法目前没有收到信息
    private void notifyTransportStateChanged(TransportState transportState) {
        if (mRendererService != null) {
            mRendererService.getAvTransportLastChange()
                    .setEventedValue(INSTANCE_ID, new AVTransportVariable.TransportState(transportState));
        }
    }

    private void notifyRenderVolumeChanged(int volume) {
        if (mRendererService != null) {
            mRendererService.getAudioControlLastChange()
                    .setEventedValue(INSTANCE_ID, new RenderingControlVariable.Volume(new ChannelVolume(Channel.Master, volume)));
        }
    }


    // SurfaceHolder Listener
    @Override
    public void surfaceCreated(@NonNull SurfaceHolder surfaceHolder) {
        mVideoView.setDisplay(surfaceHolder);
    }

    @Override
    public void surfaceChanged(@NonNull SurfaceHolder surfaceHolder, int i, int i1, int i2) {

    }

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder surfaceHolder) {

    }


    // MediaPlayer Listener
    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {
        Log.d(TAG, "onCompletion: ");
    }

    @Override
    public void onPrepared(MediaPlayer mediaPlayer) {
        mediaPlayer.start();
        notifyTransportStateChanged(TransportState.PLAYING);
        mProgressBar.setVisibility(View.INVISIBLE);
    }
}

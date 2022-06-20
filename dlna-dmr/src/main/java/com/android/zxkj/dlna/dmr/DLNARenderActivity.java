package com.android.zxkj.dlna.dmr;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.AudioManager;
import android.media.MediaCodec;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.android.zxkj.dlna.dmr.widget.MyImageView;
import com.android.zxkj.dlna.dmr.widget.VideoView;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.mediacodec.MediaCodecInfo;
import com.google.android.exoplayer2.mediacodec.MediaCodecSelector;
import com.google.android.exoplayer2.mediacodec.MediaCodecUtil;
import com.google.android.exoplayer2.mediacodec.MediaFormatUtil;
import com.google.android.exoplayer2.ui.PlayerView;

import org.eclipse.jetty.util.StringUtil;
import org.fourthline.cling.model.types.UnsignedIntegerFourBytes;
import org.fourthline.cling.support.avtransport.lastchange.AVTransportVariable;
import org.fourthline.cling.support.model.Channel;
import org.fourthline.cling.support.model.TransportAction;
import org.fourthline.cling.support.model.TransportState;
import org.fourthline.cling.support.renderingcontrol.lastchange.ChannelVolume;
import org.fourthline.cling.support.renderingcontrol.lastchange.RenderingControlVariable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class DLNARenderActivity extends AppCompatActivity implements MediaPlayer.OnVideoSizeChangedListener, MediaPlayer.OnBufferingUpdateListener, MediaPlayer.OnCompletionListener, MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener {
    private static final String KEY_EXTRA_CURRENT_URI = "Renderer.KeyExtra.CurrentUri";
    private static final String TAG = "DLNARenderActivity";
    public static void startActivity(Context context, String currentURI) {
        Intent intent = new Intent(context, DLNARenderActivity.class);
        intent.putExtra(KEY_EXTRA_CURRENT_URI, currentURI);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); // start from service content,should add 'FLAG_ACTIVITY_NEW_TASK' flag.
        context.startActivity(intent);
    }

    private final UnsignedIntegerFourBytes INSTANCE_ID = new UnsignedIntegerFourBytes(0);


    private ProgressBar mProgressBar;
    private DLNARendererService mRendererService;
    private MyImageView mImageView;

    private SurfaceHolder mSurfaceHolder;
    private SurfaceView mSurfaceView;
    private MediaPlayer mMediaPlayer;

    private VideoView mVideoView;


    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG, "onServiceConnected: ");
            mRendererService = ((DLNARendererService.RendererServiceBinder) service).getRendererService();
            mRendererService.setRenderControl(new IDLNARenderControl.VideoViewRenderControl(mMediaPlayer));
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, "onServiceDisconnected: ");
            mRendererService = null;
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dlna_renderer);


        mImageView = findViewById(R.id.my_imageView);
        mImageView.setVisibility(View.GONE);
        mProgressBar = findViewById(R.id.video_progress);

        mMediaPlayer = new MediaPlayer();
        mMediaPlayer.setOnPreparedListener(this);
        mMediaPlayer.setOnCompletionListener(this);
        mMediaPlayer.setOnBufferingUpdateListener(this);
        mMediaPlayer.setOnVideoSizeChangedListener(this);
        mMediaPlayer.setOnErrorListener(this);

        mSurfaceView = findViewById(R.id.my_surfaceView);
        mSurfaceHolder = mSurfaceView.getHolder();
        mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        mSurfaceHolder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(@NonNull SurfaceHolder surfaceHolder) {
                Log.d(TAG, "surfaceCreated: " + System.currentTimeMillis());
                mMediaPlayer.setDisplay(mSurfaceView.getHolder());
            }

            @Override
            public void surfaceChanged(@NonNull SurfaceHolder surfaceHolder, int i, int i1, int i2) {
                Log.d(TAG, "surfaceChanged: " + System.currentTimeMillis());
            }

            @Override
            public void surfaceDestroyed(@NonNull SurfaceHolder surfaceHolder) {
                Log.d(TAG, "surfaceDestroyed: " + System.currentTimeMillis());
            }
        });

        bindService(new Intent(this, DLNARendererService.class), mServiceConnection, Service.BIND_AUTO_CREATE);
        // 在这里上报一下当前的状态
        openMedia(getIntent());
    }

    public MediaCodec createBestCodec() throws IOException {
        MediaCodecList list = new MediaCodecList(MediaCodecList.REGULAR_CODECS);
        android.media.MediaCodecInfo[] codecs = list.getCodecInfos();
        for (android.media.MediaCodecInfo codec : codecs) {
            if (!codec.isEncoder()) {
                String name = codec.getName();
                if (name.startsWith("OMX.google") && name.contains("hevc")) {
                    Log.e(TAG,"找到合适的解码器");
                    return MediaCodec.createByCodecName(codec.getName());
                }
            }
        }
        return MediaCodec.createDecoderByType(MediaFormat.MIMETYPE_VIDEO_HEVC);
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

            Log.d(TAG, "open Media Uri: " + currentUri);
            // 暂时没有找到专门的图片渲染事件，这里是用的 AVTranport,
            // 先根据后缀判断以下类型
            if (currentUri.endsWith(".jpg") || currentUri.endsWith(".png")) {
                if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
                    mMediaPlayer.reset();
                }

                mSurfaceView.setVisibility(View.INVISIBLE);

                mImageView.setImageURL(currentUri);
                mImageView.setVisibility(View.VISIBLE);
                mProgressBar.setVisibility(View.INVISIBLE);
            } else {
                if (mSurfaceView.getVisibility() != View.VISIBLE) {
                    mSurfaceView.setVisibility(View.VISIBLE);
                }

                if (mMediaPlayer != null) {
                    Log.d(TAG, "openMedia:  isPlaying, reset");
                    mMediaPlayer.reset();
                }

                mImageView.setVisibility(View.GONE);;

                try {
                    // 准备播放
                    mMediaPlayer.setDataSource(currentUri);
                    mMediaPlayer.prepare();
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
        if (mMediaPlayer != null) mMediaPlayer.release();
        notifyTransportStateChanged(TransportState.STOPPED);
        Log.d(TAG, "onDestroy:  begin unbindService");
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
                if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
                    mMediaPlayer.pause();
                    notifyTransportStateChanged(TransportState.PAUSED_PLAYBACK);
                } else if (mMediaPlayer != null) {
                    mMediaPlayer.start();
                    notifyTransportStateChanged(TransportState.PLAYING);
                }
            }
        }
        return handled;
    }

    private void notifyTransportStateChanged(TransportState transportState) {
        if (mRendererService != null) {

            Log.d(TAG, "notifyTransportStateChanged: ");

            mRendererService.getAvTransportLastChange()
                    .setEventedValue(INSTANCE_ID,
                            new AVTransportVariable.TransportState(transportState),
                            new AVTransportVariable.CurrentTransportActions(
                                    new TransportAction[] { TransportAction.Pause,
                                            TransportAction.Stop, TransportAction.Seek,
                                            TransportAction.Next, TransportAction.Previous })

                    );

//            try {
//                Thread.sleep(1000);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//            mRendererService.getAvTransportLastChange().fire(mRendererService.getPropertyChangeSupport());
        } else  {
            Log.d(TAG, "notifyTransportStateChanged:  mRendererService == null ");
        }
    }

    private void notifyRenderVolumeChanged(int volume) {
        if (mRendererService != null) {
            mRendererService.getAudioControlLastChange()
                    .setEventedValue(INSTANCE_ID,
                            new RenderingControlVariable.Volume(new ChannelVolume(Channel.Master, volume))
                    );
        }
    }

    @Override
    public void onVideoSizeChanged(MediaPlayer mediaPlayer, int i, int i1) {
        Log.d(TAG, "onVideoSizeChanged: i: " + i + " i1: " + i1);
    }

    @Override
    public void onBufferingUpdate(MediaPlayer mediaPlayer, int i) {
        Log.d(TAG, "onBufferingUpdate: " + i);
    }

    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {
        Log.d(TAG, "onCompletion: release MediaPlayer =  " + System.currentTimeMillis());
        mediaPlayer.release();
        finish();
    }

    @Override
    public void onPrepared(MediaPlayer mediaPlayer) {
        Log.d(TAG, "onPrepared: MediaPlayer start  =  " + System.currentTimeMillis());
        mMediaPlayer.start();
        mProgressBar.setVisibility(View.INVISIBLE);
        notifyTransportStateChanged(TransportState.PLAYING);
    }


    @Override
    public boolean onError(MediaPlayer mediaPlayer, int i, int i1) {
        return true;
    }
}

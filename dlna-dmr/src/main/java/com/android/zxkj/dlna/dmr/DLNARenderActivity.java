package com.android.zxkj.dlna.dmr;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
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
    }

    private final UnsignedIntegerFourBytes INSTANCE_ID = new UnsignedIntegerFourBytes(0);

    private ProgressBar mProgressBar;

    private DLNARendererService mRendererService;

    private ZxineVideoView mVideoView;

    private MyImageView mImageView;

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
            mRendererService.setRenderControl(new IDLNARenderControl.VideoViewRenderControl(mVideoView));
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

        initCompent();
        bindService(new Intent(this, DLNARendererService.class), mServiceConnection, Service.BIND_AUTO_CREATE);
        openMedia(getIntent());
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
                Log.d(TAG, "open Media Uir: "+ currentUri);

                mVideoView.setPath(currentUri);
            }
        } else {
            Toast.makeText(this, "没有找到有效的视频地址，请检查...", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        if (mVideoView != null) mVideoView.release();
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

    private void notifyTransportStateChanged(TransportState transportState) {
        if (mRendererService != null) {
            Log.d(TAG, "notifyTransportStateChanged: " + transportState.getValue());
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

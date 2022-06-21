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
import com.pili.pldroid.player.AVOptions;
import com.pili.pldroid.player.PLOnErrorListener;
import com.pili.pldroid.player.PLOnPreparedListener;
import com.pili.pldroid.player.widget.PLVideoView;

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

public class DLNARenderActivity extends AppCompatActivity implements PLOnPreparedListener, PLOnErrorListener {
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


    private PLVideoView mVideoView;


    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG, "onServiceConnected: ");
            mRendererService = ((DLNARendererService.RendererServiceBinder) service).getRendererService();
            mRendererService.setRenderControl(new IDLNARenderControl.VideoViewRenderControl(mVideoView));
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


        AVOptions options = new AVOptions();
        options.setInteger(AVOptions.KEY_PREPARE_TIMEOUT, 10 * 1000);
        options.setInteger(AVOptions.KEY_SEEK_MODE, 1);
        options.setInteger(AVOptions.KEY_LIVE_STREAMING,  0);
        options.setInteger(AVOptions.KEY_PREFER_FORMAT, 2);

        options.setInteger(AVOptions.KEY_CACHE_FILE_NAME_ENCODE,  0);

        options.setInteger(AVOptions.KEY_MEDIACODEC, AVOptions.MEDIA_CODEC_SW_DECODE);
        options.setInteger(AVOptions.KEY_CACHE_BUFFER_DURATION, 200);
        options.setInteger(AVOptions.KEY_CACHE_BUFFER_DURATION_SPEED_ADJUST, 0);
        options.setInteger(AVOptions.KEY_START_POSITION, 0);


        mVideoView = findViewById(R.id.video_view);
        mVideoView.setDisplayAspectRatio(PLVideoView.ASPECT_RATIO_ORIGIN);
        mVideoView.setBufferingIndicator(mProgressBar);
        mVideoView.setOnPreparedListener(this);
        mVideoView.setOnErrorListener(this);

        mVideoView.setAVOptions(options);

        bindService(new Intent(this, DLNARendererService.class), mServiceConnection, Service.BIND_AUTO_CREATE);

        // 在这里上报一下当前的状态
        openMedia(getIntent());
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

                mImageView.setImageURL(currentUri);
                mImageView.setVisibility(View.VISIBLE);
                mProgressBar.setVisibility(View.INVISIBLE);
            } else {
                mImageView.setVisibility(View.GONE);
                mVideoView.setVideoPath(currentUri);
//                mVideoView.start();

            }

        } else {
            Toast.makeText(this, "没有找到有效的视频地址，请检查...", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        if (mVideoView != null) mVideoView.stopPlayback();
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

            Log.d(TAG, "notifyTransportStateChanged: ");

            mRendererService.getAvTransportLastChange()
                    .setEventedValue(INSTANCE_ID,
                            new AVTransportVariable.TransportState(transportState),
                            new AVTransportVariable.CurrentTransportActions(
                                    new TransportAction[] { TransportAction.Pause,
                                            TransportAction.Stop, TransportAction.Seek,
                                            TransportAction.Next, TransportAction.Previous })

                    );
//
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
    public void onPrepared(int i) {
        Log.d(TAG, "onPrepared: ");
//        mVideoView.start();
    }

    @Override
    public boolean onError(int i, Object o) {
        Log.d(TAG, "onError: i = " + i);
        return false;
    }
}

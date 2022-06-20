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
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.android.zxkj.dlna.dmr.widget.MyImageView;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.PlaybackException;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.mediacodec.MediaCodecInfo;
import com.google.android.exoplayer2.mediacodec.MediaCodecSelector;
import com.google.android.exoplayer2.mediacodec.MediaCodecUtil;
import com.google.android.exoplayer2.ui.PlayerView;

import org.eclipse.jetty.util.StringUtil;
import org.fourthline.cling.model.types.UnsignedIntegerFourBytes;
import org.fourthline.cling.support.avtransport.lastchange.AVTransportVariable;
import org.fourthline.cling.support.model.Channel;
import org.fourthline.cling.support.model.TransportState;
import org.fourthline.cling.support.renderingcontrol.lastchange.ChannelVolume;
import org.fourthline.cling.support.renderingcontrol.lastchange.RenderingControlVariable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class DLNARenderActivity extends AppCompatActivity {
    private static final String KEY_EXTRA_CURRENT_URI = "Renderer.KeyExtra.CurrentUri";
    private static final String TAG = "DLNARenderActivity";
    public static void startActivity(Context context, String currentURI) {
        Intent intent = new Intent(context, DLNARenderActivity.class);
        intent.putExtra(KEY_EXTRA_CURRENT_URI, currentURI);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); // start from service content,should add 'FLAG_ACTIVITY_NEW_TASK' flag.
        context.startActivity(intent);
    }

    private final UnsignedIntegerFourBytes INSTANCE_ID = new UnsignedIntegerFourBytes(0);

    private PlayerView mVideoView;
    private SimpleExoPlayer mPlayer;
    private ExoPlayer mPlayer1;
    private ProgressBar mProgressBar;
    private DLNARendererService mRendererService;

    private MyImageView mImageView;

    private final Player.Listener mPlayerListenner = new Player.Listener() {
        @Override
        public void onPlayerError(PlaybackException error) {
            Log.d(TAG, error.toString());
            mProgressBar.setVisibility(View.INVISIBLE);
            notifyTransportStateChanged(TransportState.STOPPED);
            finish();
        }

        @Override
        public void onPlaybackStateChanged(int state) {

            switch (state) {
                case ExoPlayer.STATE_IDLE: //播放器已实例化，但尚未准备就绪。
                    break;
                case ExoPlayer.STATE_READY: //播放器可以立即从当前位置开始播放
                    // 播放
                    mPlayer.play();
                    Log.d(TAG, "EXOPlayer start play");
                    mProgressBar.setVisibility(View.INVISIBLE);
                    notifyTransportStateChanged(TransportState.PLAYING);
                    break;
                case ExoPlayer.STATE_ENDED: //播放器已完成媒体播放。
                    notifyTransportStateChanged(TransportState.STOPPED);
                    finish();
                    break;
            }
        }

        @Override
        public void onEvents(Player player, Player.Events events) {
            Log.d(TAG, events.toString());
        }

    };

    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mRendererService = ((DLNARendererService.RendererServiceBinder) service).getRendererService();
            mRendererService.setRenderControl(new IDLNARenderControl.VideoViewRenderControl(mPlayer));
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


        mImageView = findViewById(R.id.my_imageView);
        mImageView.setVisibility(View.GONE);


        mVideoView = findViewById(R.id.video_view);

        DefaultRenderersFactory defaultRenderersFactory = new DefaultRenderersFactory(this);

        defaultRenderersFactory.setMediaCodecSelector(new MediaCodecSelector() {
            @RequiresApi(api = Build.VERSION_CODES.Q)
            @Override
            public List<MediaCodecInfo> getDecoderInfos(String mimeType, boolean requiresSecureDecoder, boolean requiresTunnelingDecoder) throws MediaCodecUtil.DecoderQueryException {

                MediaCodecList list = new MediaCodecList(MediaCodecList.REGULAR_CODECS);


                android.media.MediaCodecInfo[] infos = list.getCodecInfos();

               List<MediaCodecInfo> resultMediaCodecInfo = new ArrayList<>();


                for (android.media.MediaCodecInfo items:  infos) {

                    final String name = items.getName();
                    if (!items.isEncoder()) {
                        if (name.startsWith("OMX.")) {
                            Log.d(TAG,"找到合适的解码器" + name);
                            resultMediaCodecInfo.add(MediaCodecInfo.newInstance("OMX.google.hevc.decoder",
                                    "video/hevc", "video/hevc", null,
                                    items.isHardwareAccelerated(),items.isSoftwareOnly(),items.isVendor(),
                                    false, false ));
                        }
                    }
                }

                return resultMediaCodecInfo;
            }
        });

        mPlayer = new SimpleExoPlayer.Builder(this).setLooper(Looper.getMainLooper()).build();
        mPlayer.setThrowsWhenUsingWrongThread(false);


        mPlayer.addListener(mPlayerListenner);
        mVideoView.setPlayer(mPlayer);


        mProgressBar = findViewById(R.id.video_progress);

        bindService(new Intent(this, DLNARendererService.class), mServiceConnection, Service.BIND_AUTO_CREATE);
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
                if (mPlayer != null && mPlayer.isPlaying()) {
                    mPlayer.stop();
                }

                mVideoView.setVisibility(View.INVISIBLE);

                mImageView.setImageURL(currentUri);
                mImageView.setVisibility(View.VISIBLE);
                mProgressBar.setVisibility(View.INVISIBLE);
            } else {
                if (mVideoView.getVisibility() != View.VISIBLE) {
                    mVideoView.setVisibility(View.VISIBLE);
                }

                if (mPlayer != null && mPlayer.isPlaying()) {
                    mPlayer.stop();
                }

                mImageView.setVisibility(View.GONE);
                // 设置媒体信息
                MediaItem mediaItem =  MediaItem.fromUri(currentUri);

                mPlayer.setMediaItem(mediaItem);
                // 准备播放
                mPlayer.prepare();
            }

        } else {
            Toast.makeText(this, "没有找到有效的视频地址，请检查...", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        if (mPlayer != null) mPlayer.stop();
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
                if (mPlayer != null && mPlayer.isPlaying()) {
                    mPlayer.pause();
                    notifyTransportStateChanged(TransportState.PAUSED_PLAYBACK);
                } else if (mPlayer != null) {
                    mPlayer.play();
                    notifyTransportStateChanged(TransportState.PLAYING);
                }
            }
        }
        return handled;
    }

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
}

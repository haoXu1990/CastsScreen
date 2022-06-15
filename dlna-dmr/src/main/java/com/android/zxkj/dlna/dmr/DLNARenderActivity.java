package com.android.zxkj.dlna.dmr;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.ui.PlayerView;
import com.shuyu.gsyvideoplayer.listener.VideoAllCallBack;
import com.shuyu.gsyvideoplayer.video.StandardGSYVideoPlayer;

import org.fourthline.cling.model.types.UnsignedIntegerFourBytes;
import org.fourthline.cling.support.avtransport.lastchange.AVTransportVariable;
import org.fourthline.cling.support.model.Channel;
import org.fourthline.cling.support.model.TransportState;
import org.fourthline.cling.support.renderingcontrol.lastchange.ChannelVolume;
import org.fourthline.cling.support.renderingcontrol.lastchange.RenderingControlVariable;

public class DLNARenderActivity extends AppCompatActivity {
    private static final String KEY_EXTRA_CURRENT_URI = "Renderer.KeyExtra.CurrentUri";
    private static final String TAG = "DLNARenderActivity";
    public static void startActivity(Context context, String currentURI) {
//        Log.i(TAG, "startActivity", new Exception());
        Intent intent = new Intent(context, DLNARenderActivity.class);
        intent.putExtra(KEY_EXTRA_CURRENT_URI, currentURI);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); // start from service content,should add 'FLAG_ACTIVITY_NEW_TASK' flag.
        context.startActivity(intent);
    }

    private final UnsignedIntegerFourBytes INSTANCE_ID = new UnsignedIntegerFourBytes(0);

//    private PlayerView mVideoView;
//    private SimpleExoPlayer mPlayer;
    private ProgressBar mProgressBar;
    private DLNARendererService mRendererService;

    private StandardGSYVideoPlayer mVideoView;

//    private final Player.EventListener mPlayerListenner = new Player.EventListener() {
//        @Override
//        public void onPlayerError(ExoPlaybackException error) {
//            Player.EventListener.super.onPlayerError(error);
//            Log.d(TAG, error.toString());
//            mProgressBar.setVisibility(View.INVISIBLE);
//            notifyTransportStateChanged(TransportState.STOPPED);
//            finish();
//        }
//
//        @Override
//        public void onPlaybackStateChanged(int state) {
//            Player.EventListener.super.onPlaybackStateChanged(state);
//
//            switch (state) {
//                case ExoPlayer.STATE_IDLE: //播放器已实例化，但尚未准备就绪。
//                    break;
//                case ExoPlayer.STATE_READY: //播放器可以立即从当前位置开始播放
//                    Log.d(TAG, "EXOPlayer start play");
//                    mProgressBar.setVisibility(View.INVISIBLE);
//                    notifyTransportStateChanged(TransportState.PLAYING);
//                    break;
//                case ExoPlayer.STATE_ENDED: //播放器已完成媒体播放。
//                    notifyTransportStateChanged(TransportState.STOPPED);
//                    finish();
//                    break;
//            }
//        }
//
//        @Override
//        public void onEvents(Player player, Player.Events events) {
//            Player.EventListener.super.onEvents(player, events);
//
//            Log.d(TAG, events.toString());
//        }
//
//    };

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

        mVideoView = findViewById(R.id.video_view);
        mVideoView.getBackButton().setVisibility(View.VISIBLE);
        mVideoView.setVideoAllCallBack(new VideoAllCallBack() {
            @Override
            public void onStartPrepared(String url, Object... objects) {
                Log.d(TAG, "EXOPlayer start play");
                    mProgressBar.setVisibility(View.INVISIBLE);
                    notifyTransportStateChanged(TransportState.PLAYING);
            }

            @Override
            public void onPrepared(String url, Object... objects) {

            }

            @Override
            public void onClickStartIcon(String url, Object... objects) {
            }

            @Override
            public void onClickStartError(String url, Object... objects) {

            }


            @Override
            public void onClickStop(String url, Object... objects) {

            }

            @Override
            public void onClickStopFullscreen(String url, Object... objects) {

            }

            @Override
            public void onClickResume(String url, Object... objects) {

            }

            @Override
            public void onClickResumeFullscreen(String url, Object... objects) {

            }

            @Override
            public void onClickSeekbar(String url, Object... objects) {

            }

            @Override
            public void onClickSeekbarFullscreen(String url, Object... objects) {

            }

            @Override
            public void onAutoComplete(String url, Object... objects) {

            }

            @Override
            public void onComplete(String url, Object... objects) {

            }

            @Override
            public void onEnterFullscreen(String url, Object... objects) {

            }

            @Override
            public void onQuitFullscreen(String url, Object... objects) {

            }

            @Override
            public void onQuitSmallWidget(String url, Object... objects) {

            }

            @Override
            public void onEnterSmallWidget(String url, Object... objects) {

            }

            @Override
            public void onTouchScreenSeekVolume(String url, Object... objects) {

            }

            @Override
            public void onTouchScreenSeekPosition(String url, Object... objects) {

            }

            @Override
            public void onTouchScreenSeekLight(String url, Object... objects) {

            }

            @Override
            public void onPlayError(String url, Object... objects) {

            }

            @Override
            public void onClickStartThumb(String url, Object... objects) {

            }

            @Override
            public void onClickBlank(String url, Object... objects) {

            }

            @Override
            public void onClickBlankFullscreen(String url, Object... objects) {

            }
        });

        mProgressBar = findViewById(R.id.video_progress);
        bindService(new Intent(this, DLNARendererService.class), mServiceConnection, Service.BIND_AUTO_CREATE);
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
            mVideoView.setUp(currentUri,false,"");
            mVideoView.startPlayLogic();
            // 播放成功以后应该设置 AVTransportController -> mOriginPositionInfo ，重新设置媒体信息的播放总时长

//            mRendererService.createConfiguration()
            // 设置媒体信息
//            MediaItem mediaItem =  MediaItem.fromUri(currentUri);
//            mPlayer.setMediaItem(mediaItem);
//            // 准备播放
//            mPlayer.prepare();
//            // 播放
//            mPlayer.play();
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
                notifyRenderVolumeChanged(volume);
            } else if (keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
                if (mVideoView != null && mVideoView.isInPlayingState()) {
                    mVideoView.onVideoPause();
                    notifyTransportStateChanged(TransportState.PAUSED_PLAYBACK);
                } else if (mVideoView != null) {
                    mVideoView.onVideoResume();
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

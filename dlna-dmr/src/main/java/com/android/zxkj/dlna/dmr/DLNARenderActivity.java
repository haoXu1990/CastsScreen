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
import android.net.Uri;
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
import java.net.URI;
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


    private ProgressBar mProgressBar;
    private DLNARendererService mRendererService;
    private MyImageView mImageView;

//    private SurfaceHolder mSurfaceHolder;
//    private SurfaceView mSurfaceView;
//    private MediaPlayer mMediaPlayer;

    private VideoView mVideoView;


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

        mVideoView = findViewById(R.id.video_view);
        mVideoView.setLoaddingView(mProgressBar);
        mVideoView.setOnBufferingUpdateListener(pre -> Log.d(TAG, "update: " + pre));
        mVideoView.setOnStopListener(() -> {
            Log.d(TAG, "收到了停止播放事件推出界面 ");
            finish();
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

                mImageView.setImageURL(currentUri);
                mImageView.setVisibility(View.VISIBLE);
                mProgressBar.setVisibility(View.INVISIBLE);
            } else {
                mProgressBar.setVisibility(View.INVISIBLE);
                mImageView.setVisibility(View.GONE);
//                String uri1 = "https://www.cnuseful.com/testdown/video/test.mp4";
//                String uri1 = "http://vfx.mtime.cn/Video/2019/03/19/mp4/190319125415785691.mp4";
//                String uri1 = "https://mirror.aarnet.edu.au/pub/TED-talks/911Mothers_2010W-480p.mp4";
//                String uri1 =  "https://sf1-hscdn-tos.pstatp.com/obj/media-fe/xgplayer_doc_video/mp4/xgplayer-demo-720p.mp4";
//                String uri1 = "http://vfx.mtime.cn/Video/2019/03/19/mp4/190319222227698228.mp4";
//                String uri1 =  "https://media.w3.org/2010/05/sintel/trailer.mp4";
//                String uri1 =   "https://upos-sz-mirrorcos.bilivideo.com/upgcxcode/51/22/738282251/738282251-1-208.mp4?e=ig8euxZM2rNcNbhMhzdVhwdlhzKzhwdVhoNvNC8BqJIzNbfq9rVEuxTEnE8L5F6VnEsSTx0vkX8fqJeYTj_lta53NCM=&uipk=5&nbs=1&deadline=1655817045&gen=playurlv2&os=hwbv&oi=1363604970&trid=64e6b7210ffd4959a8a5b6ede4321264T&mid=0&platform=html5&upsig=d16230c6f35b73de5bc0eaca9281ebe6&uparams=e,uipk,nbs,deadline,gen,os,oi,trid,mid,platform&bvc=vod&nettype=0&bw=343833&orderid=0,1&logo=80000000";
                mVideoView.setVideoURI(Uri.parse(currentUri));
            }

        } else {
            Toast.makeText(this, "没有找到有效的视频地址，请检查...", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        if (mVideoView != null) mVideoView.releasePlayer();
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

}

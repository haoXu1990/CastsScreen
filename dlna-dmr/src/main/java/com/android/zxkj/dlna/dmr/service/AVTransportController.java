package com.android.zxkj.dlna.dmr.service;

import android.content.Context;
import android.util.Log;

import com.android.zxkj.dlna.core.utils.CastUtils;
import com.android.zxkj.dlna.dmr.DLNARenderActivity;
import com.android.zxkj.dlna.dmr.IDLNARenderControl;

import org.fourthline.cling.model.ModelUtil;
import org.fourthline.cling.model.types.ErrorCode;
import org.fourthline.cling.model.types.UnsignedIntegerFourBytes;
import org.fourthline.cling.support.avtransport.AVTransportErrorCode;
import org.fourthline.cling.support.avtransport.AVTransportException;
import org.fourthline.cling.support.model.DeviceCapabilities;
import org.fourthline.cling.support.model.MediaInfo;
import org.fourthline.cling.support.model.PositionInfo;
import org.fourthline.cling.support.model.SeekMode;
import org.fourthline.cling.support.model.StorageMedium;
import org.fourthline.cling.support.model.TransportAction;
import org.fourthline.cling.support.model.TransportInfo;
import org.fourthline.cling.support.model.TransportSettings;
import org.fourthline.cling.support.model.TransportState;

import java.net.URI;

// AVTransport 控制器
public class AVTransportController implements IRendererInterface.IAVTransportControl {
    private static final TransportAction[] TRANSPORT_ACTION_STOPPED = new TransportAction[]{TransportAction.Play};
    private static final TransportAction[] TRANSPORT_ACTION_PLAYING = new TransportAction[]{TransportAction.Stop, TransportAction.Pause, TransportAction.Seek};
    private static final TransportAction[] TRANSPORT_ACTION_PAUSE_PLAYBACK = new TransportAction[]{TransportAction.Play, TransportAction.Seek, TransportAction.Stop,TransportAction.Pause};

    private static final String TAG = "AVTransportController";

    private final UnsignedIntegerFourBytes mInstanceId;

    private final Context mApplicationContext;

    // 播放状态信息
    //  这个放在这里不合适啊！！！！！！
    private TransportInfo mTransportInfo = new TransportInfo();

    // 播放设置, 这个暂时不支持，直接传个默认的
    private final TransportSettings mTransportSettings = new TransportSettings();

    // 播放位置信息
    private PositionInfo mOriginPositionInfo = new PositionInfo();

    // 媒体信息
    private MediaInfo mMediaInfo = new MediaInfo();

    // 播放器控制器
    private final IDLNARenderControl mMediaControl;

    public AVTransportController(Context context, IDLNARenderControl control) {
        this(context, new UnsignedIntegerFourBytes(0), control);
    }

    public AVTransportController(Context context, UnsignedIntegerFourBytes instanceId, IDLNARenderControl control) {
        mApplicationContext = context.getApplicationContext();
        mInstanceId = instanceId;
        mMediaControl = control;
    }

    //------------------------
    // 下面是一些播放控制方法
    //------------------------

    public UnsignedIntegerFourBytes getInstanceId() {
        return mInstanceId;
    }

    public synchronized TransportAction[] getCurrentTransportActions() {
        switch (mTransportInfo.getCurrentTransportState()) {
            case PLAYING:
                return TRANSPORT_ACTION_PLAYING;
            case PAUSED_PLAYBACK:
                return TRANSPORT_ACTION_PAUSE_PLAYBACK;
            case STOPPED:
                return TRANSPORT_ACTION_STOPPED;
            default:
                return null;
        }
    }


    @Override
    public DeviceCapabilities getDeviceCapabilities() {
        return new DeviceCapabilities(new StorageMedium[]{StorageMedium.NETWORK});
    }

    @Override
    public MediaInfo getMediaInfo() {
        return mMediaInfo;
    }

    @Override
    public PositionInfo getPositionInfo() {
        final PositionInfo positionInfo = new PositionInfo(mOriginPositionInfo, mMediaControl.getPosition() / 1000, mMediaControl.getDuration()/ 1000 );
        positionInfo.setTrackDuration(ModelUtil.toTimeString(mMediaControl.getDuration()/ 1000));
        return positionInfo;
    }

    @Override
    public TransportInfo getTransportInfo() {
        Log.d(TAG, "getTransportInfo: 上报状态 " + mTransportInfo.getCurrentTransportState().getValue());
//        Log.d(TAG, "getTransportInfo: " , new Exception());
        // TODO: 这里暂时还没搞明白，这个mTransportInfo 的信息应该是需要更新的，但是目前没有发现哪里在更新
        // 当播放状态改变时，应该需要修改
        return mTransportInfo;
    }

    @Override
    public TransportSettings getTransportSettings() {
        return mTransportSettings;
    }

    @Override
    public void setAVTransportURI(String currentURI, String currentURIMetaData) throws AVTransportException {
        try {
            new URI(currentURI);
        } catch (Exception ex) {
            throw new AVTransportException(ErrorCode.INVALID_ARGS, "CurrentURI can not be null or malformed");
        }
        mMediaInfo = new MediaInfo(currentURI, currentURIMetaData, new UnsignedIntegerFourBytes(1), "", StorageMedium.NETWORK);
        mOriginPositionInfo = new PositionInfo(1, currentURIMetaData, currentURI);

        // TODO: 这里可以解析出来投屏过来的媒体类型
        // 根据不同的媒体类型调用不同的渲染 Activity
        String type = "image";
        if (currentURIMetaData.contains("object.item.videoItem")) {
            type = "video";
        } else if (currentURIMetaData.contains("object.item.imageItem")) {
            type = "image";
        } else if (currentURIMetaData.contains("object.item.audioItem")) {
            type = "audio";
        }
        Log.d(TAG, "setAVTransportURI type = " + type);


        // TODO: 如果当前 Renderer 端正在播放的话，还需要发送停止事件，然后释放播放器
        // 这一步思考一下在哪里来处理比较合适
        DLNARenderActivity.startActivity(mApplicationContext, currentURI);
    }

    @Override
    public void setNextAVTransportURI(String nextURI, String nextURIMetaData) {
    }

    @Override
    public void play(String speed) {
        mTransportInfo = new TransportInfo(TransportState.PLAYING);
        mMediaControl.play();
    }

    public void pause() {
        mTransportInfo = new TransportInfo(TransportState.PAUSED_PLAYBACK);
        mMediaControl.pause();
    }


    @Override
    public void seek(String unit, String target) throws AVTransportException {
        SeekMode seekMode = SeekMode.valueOrExceptionOf(unit);
        if (!seekMode.equals(SeekMode.REL_TIME)) {
            throw new AVTransportException(AVTransportErrorCode.SEEKMODE_NOT_SUPPORTED, "Unsupported seek mode: " + unit);
        }
        long position = CastUtils.getIntTime(target);
        mMediaControl.seek(position);
    }

    synchronized public void stop() {
        mTransportInfo = new TransportInfo(TransportState.STOPPED);
        mMediaControl.stop();
    }

    synchronized protected void transportStateChanged(TransportState newState) {
        mTransportInfo = new TransportInfo(newState);
    }


    @Override
    public void previous() {
    }

    @Override
    public void next() {
    }

    @Override
    public void record() {
    }

    @Override
    public void setPlayMode(String newPlayMode) {
    }

    @Override
    public void setRecordQualityMode(String newRecordQualityMode) {
    }
}

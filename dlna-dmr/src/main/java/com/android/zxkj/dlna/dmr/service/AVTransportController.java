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

public class AVTransportController implements IRendererInterface.IAVTransportControl {
    private static final TransportAction[] TRANSPORT_ACTION_STOPPED = new TransportAction[]{TransportAction.Play};
    private static final TransportAction[] TRANSPORT_ACTION_PLAYING = new TransportAction[]{TransportAction.Stop, TransportAction.Pause, TransportAction.Seek};
    private static final TransportAction[] TRANSPORT_ACTION_PAUSE_PLAYBACK = new TransportAction[]{TransportAction.Play, TransportAction.Seek, TransportAction.Stop};
    private static final String TAG = "AVTransportController";

    private final UnsignedIntegerFourBytes mInstanceId;
    private final Context mApplicationContext;
    private TransportInfo mTransportInfo = new TransportInfo();
    private final TransportSettings mTransportSettings = new TransportSettings();
    private PositionInfo mOriginPositionInfo = new PositionInfo();
    private MediaInfo mMediaInfo = new MediaInfo();
    private final IDLNARenderControl mMediaControl;

    public AVTransportController(Context context, IDLNARenderControl control) {
        this(context, new UnsignedIntegerFourBytes(0), control);
    }

    public AVTransportController(Context context, UnsignedIntegerFourBytes instanceId, IDLNARenderControl control) {
        mApplicationContext = context.getApplicationContext();
        mInstanceId = instanceId;
        mMediaControl = control;
    }


    public UnsignedIntegerFourBytes getInstanceId() {
        return mInstanceId;
    }

    public synchronized TransportAction[] getCurrentTransportActions() {
        switch (mTransportInfo.getCurrentTransportState()) {
            case PLAYING:
                return TRANSPORT_ACTION_PLAYING;
            case PAUSED_PLAYBACK:
                return TRANSPORT_ACTION_PAUSE_PLAYBACK;
            default:
                return TRANSPORT_ACTION_STOPPED;
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
        Log.d(TAG, "getTransportInfo: 上报状态");
        mTransportInfo = new TransportInfo(TransportState.PLAYING);
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
        DLNARenderActivity.startActivity(mApplicationContext, currentURI);
    }

    @Override
    public void setNextAVTransportURI(String nextURI, String nextURIMetaData) {
    }

    @Override
    public void play(String speed) {

        mMediaControl.play();
    }

    public void pause() {
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
        mMediaControl.stop();
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

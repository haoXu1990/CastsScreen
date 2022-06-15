package com.android.zxkj.cast_screen;

import android.content.Context;
import android.media.AudioManager;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.zxkj.dlna.core.utils.CastUtils;
import com.android.zxkj.dlna.dmc.DLNACastManager;
import com.android.zxkj.dlna.dmc.control.ICastInterface;

import org.fourthline.cling.model.meta.Device;


public class ControlFragment extends Fragment implements IDisplayDevice, CastFragment.Callback  {


    private static final String TAG = "ControlFragment";
    private TextView mPositionInfo;
    private SeekBar mPositionSeekBar;
    private TextView mVolumeInfo;
    private SeekBar mVolumeSeekBar;
    private TextView mStatusInfo;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_control, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initComponent(view);

        // 视图销毁后一定要记得 unregister
        DLNACastManager.getInstance().registerActionCallbacks(
                // 投屏 EVENT
                new ICastInterface.CastEventListener() {
                    @Override
                    public void onSuccess(String result) {
                        // 投屏开始，处理业务逻辑
                        // 这里只代表服务端处理成功，不是播放段播放成功
                        Toast.makeText(getActivity(), "Cast: " + result, Toast.LENGTH_LONG).show();
                        mPositionMsgHandler.start(0);
                        mVolumeMsgHandler.start(0);
                    }

                    @Override
                    public void onFailed(String errMsg) {
                        // 投屏失败
                        Toast.makeText(getActivity(), errMsg, Toast.LENGTH_SHORT).show();
                    }
                },
                // 播放
                new ICastInterface.PlayEventListener() {
                    @Override
                    public void onSuccess(Void result) {
                        Toast.makeText(getActivity(), "Play", Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onFailed(String errMsg) {
                        Toast.makeText(getActivity(), errMsg, Toast.LENGTH_SHORT).show();
                    }
                },
                // 暂停
                new ICastInterface.PauseEventListener() {
                    @Override
                    public void onSuccess(Void result) {
                        Toast.makeText(getActivity(), "Pause", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFailed(String errMsg) {
                        Toast.makeText(getActivity(), errMsg, Toast.LENGTH_LONG).show();
                    }
                },
                // 停止
                new ICastInterface.StopEventListener() {
                    @Override
                    public void onSuccess(Void result) {
                        Toast.makeText(getActivity(), "Stop", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFailed(String errMsg) {
                        Toast.makeText(getActivity(), errMsg, Toast.LENGTH_LONG).show();
                    }
                },
                // 拖动进度
                new ICastInterface.SeekToEventListener() {
                    @Override
                    public void onSuccess(Long result) {
                        Toast.makeText(getActivity(), "SeekTo: " + CastUtils.getStringTime(result), Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onFailed(String errMsg) {
                        Toast.makeText(getActivity(), errMsg, Toast.LENGTH_LONG).show();
                    }
                }
        );

        // 注册订阅监听
        DLNACastManager.getInstance().registerSubscriptionListener(event -> mStatusInfo.setText(event.getValue()));
    }

    private void initComponent(View view) {
        view.findViewById(R.id.btn_cast).setOnClickListener(v -> new CastFragment().setCallback(this).show(getChildFragmentManager(), "CastFragment"));
        view.findViewById(R.id.btn_cast_pause).setOnClickListener(v -> DLNACastManager.getInstance().pause());
        view.findViewById(R.id.btn_cast_resume).setOnClickListener(v -> DLNACastManager.getInstance().play());
        view.findViewById(R.id.btn_cast_stop).setOnClickListener(v -> DLNACastManager.getInstance().stop());
        view.findViewById(R.id.btn_cast_mute).setOnClickListener(v -> DLNACastManager.getInstance().setMute(true));

        mPositionInfo = view.findViewById(R.id.ctrl_position_info);
        mPositionSeekBar = view.findViewById(R.id.ctrl_seek_position);
        mPositionSeekBar.setOnSeekBarChangeListener(mOnSeekBarChangeListener);
        mVolumeInfo = view.findViewById(R.id.ctrl_volume_info);
        mVolumeSeekBar = view.findViewById(R.id.ctrl_seek_volume);
        mVolumeSeekBar.setOnSeekBarChangeListener(mOnSeekBarChangeListener);
        mStatusInfo = view.findViewById(R.id.ctrl_status_info);
    }

    @Override
    public void onDestroyView() {
        DLNACastManager.getInstance().unregisterActionCallbacks();
        super.onDestroyView();
    }

    private final SeekBar.OnSeekBarChangeListener mOnSeekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int i, boolean b) {

        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            switch (seekBar.getId()) {
                case R.id.ctrl_seek_volume: {
                    DLNACastManager.getInstance().setVolume((int) (seekBar.getProgress() * 100f / seekBar.getMax()));
                    break;
                }
                case R.id.ctrl_seek_position: {
                    if (mDurationMillSeconds > 0) {
                        int position = (int) ((seekBar.getProgress() * 1f / seekBar.getMax()) * mDurationMillSeconds);
                        DLNACastManager.getInstance().seekTo(position);
                    }
                    break;
                }
            }
        }
    };



    private Device<?, ?, ?> mDevice;

    @Override
    public void setCastDevice(Device<?, ?, ?> device) {
        mDevice = device;
        if (device == null) {
            mPositionInfo.setText("");
            mPositionSeekBar.setProgress(0);
            mVolumeInfo.setText("");
            mVolumeSeekBar.setProgress(0);
            mPositionMsgHandler.stop();
            mVolumeMsgHandler.stop();
        }
    }

    private long mDurationMillSeconds = 0;
    private final Runnable mPositionRunnable = () -> {
      if (mDevice == null) return;
      DLNACastManager.getInstance().getPositionInfo(mDevice, ((positionInfo, errMsg) -> {
          if (positionInfo != null) {
              Log.d(TAG, positionInfo.toString());
              mPositionInfo.setText(String.format("%s/%s", positionInfo.getRelTime(), positionInfo.getTrackDuration()));
              if (positionInfo.getTrackDurationSeconds() != 0) {
                  mDurationMillSeconds = positionInfo.getTrackDurationSeconds() * 1000;
                  mPositionSeekBar.setProgress((int) (positionInfo.getTrackElapsedSeconds() * 100 / positionInfo.getTrackDurationSeconds()));
              } else {
                  mPositionSeekBar.setProgress(0);
              }
          } else {
              mPositionInfo.setText(errMsg);
          }
      }));
    };
    private final Runnable mVolumeRunnable = () -> {
        if (mDevice == null) return;
        DLNACastManager.getInstance().getVolumeInfo(mDevice, ((integer, errMsg) -> {
            if (integer != null && getActivity() != null) {
                Log.d(TAG, "getVolumeInfo: " + integer);
                AudioManager audioManager = (AudioManager) getActivity().getSystemService(Context.AUDIO_SERVICE);
                int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
                mVolumeSeekBar.setProgress(integer);
                mVolumeInfo.setText(String.format("%s/%s", (int) (integer / 100f * maxVolume), maxVolume));
            } else {
                mVolumeInfo.setText(errMsg);
            }
        }));
    };


    private final CircleMessageHandler mPositionMsgHandler = new CircleMessageHandler(1000, mPositionRunnable);
    private final CircleMessageHandler mVolumeMsgHandler = new CircleMessageHandler(3000, mVolumeRunnable);

    @Override
    public void onCastUrl(String url) {
        if (mDevice != null) {
            DLNACastManager.getInstance().cast(mDevice, CastObject.CastVideo.newInstance(url, Constants.CAST_ID, Constants.CAST_NAME));
        } else {
            Toast.makeText(getActivity(), "请选择需要投射的设备", Toast.LENGTH_LONG).show();
        }
    }

    private static class CircleMessageHandler extends Handler {
        private static final int MSG = 101;
        private final long duration;
        private final Runnable runnable;

        public CircleMessageHandler(long duration, @NonNull Runnable runnable) {
            super(Looper.getMainLooper());
            this.duration = duration;
            this.runnable = runnable;
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            runnable.run();
            sendEmptyMessageDelayed(MSG, duration);
        }

        public void start(long delay) {
            stop();
            sendEmptyMessageDelayed(MSG, delay);
        }

        public void stop() {
            removeMessages(MSG);
        }
    }

}
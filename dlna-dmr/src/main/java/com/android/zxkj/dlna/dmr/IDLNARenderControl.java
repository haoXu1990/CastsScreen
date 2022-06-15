package com.android.zxkj.dlna.dmr;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.android.exoplayer2.ExoPlayer;
import com.shuyu.gsyvideoplayer.GSYVideoADManager;
import com.shuyu.gsyvideoplayer.GSYVideoManager;
import com.shuyu.gsyvideoplayer.video.StandardGSYVideoPlayer;

public interface IDLNARenderControl {
    void play();

    void pause();

    void seek(long position);

    void stop();

    long getPosition();

    long getDuration();

    // -------------------------------------------------------------------------------------------
    // - VideoView impl
    // -------------------------------------------------------------------------------------------
    final class VideoViewRenderControl implements IDLNARenderControl {

        private final StandardGSYVideoPlayer videoView;
        private static final String TAG = "VideoViewRenderControl";
        public VideoViewRenderControl(StandardGSYVideoPlayer videoView) {
            this.videoView = videoView;
        }

        @Override
        public void play() {
            videoView.onVideoResume();
        }

        @Override
        public void pause() {
            Log.d("TAG", "pause: ");
            videoView.onVideoPause();
        }

        @Override
        public void seek(long position) {
            videoView.seekTo((int) position);
        }

        @Override
        public void stop() {
            videoView.onVideoReset();
        }

        @Override
        public long getPosition() {
            Log.d(TAG, "getPosition: " + videoView.getCurrentPositionWhenPlaying());
            return videoView.getCurrentPositionWhenPlaying();
        }

        @Override
        public long getDuration() {
            Log.d(TAG, "getDuration: " + videoView.getDuration());
            return videoView.getDuration();
        }
    }

    // -------------------------------------------------------------------------------------------
    // - Default impl
    // -------------------------------------------------------------------------------------------
    final class DefaultRenderControl implements IDLNARenderControl {
        public DefaultRenderControl() {
        }

        @Override
        public void play() {
        }

        @Override
        public void pause() {
        }

        @Override
        public void seek(long position) {
        }

        @Override
        public void stop() {
        }

        @Override
        public long getPosition() {
            return 0L;
        }

        @Override
        public long getDuration() {
            return 0L;
        }
    }
}

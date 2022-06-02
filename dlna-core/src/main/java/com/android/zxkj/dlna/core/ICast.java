package com.android.zxkj.dlna.core;

import androidx.annotation.NonNull;

public interface ICast {
    @NonNull
    String getId();

    @NonNull
    String getUri();

    String getName();

    interface ICastVideo extends ICast {
        long getDurationMillSeconds();

        long getSize();

        long getBitrate();
    }

    interface ICastAudio extends ICast {
        long getDurationMillSeconds();

        long getSize();
    }

    interface ICastImage extends ICast {
        long getSize();
    }
}

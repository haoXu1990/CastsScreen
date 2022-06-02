package com.android.zxkj.dlna.dmc;

import org.fourthline.cling.model.meta.Device;

public interface OnDeviceRegistryListener {
    void onDeviceAdded(Device<?, ?, ?> device);

    void onDeviceUpdated(Device<?, ?, ?> device);

    void onDeviceRemoved(Device<?, ?, ?> device);
}

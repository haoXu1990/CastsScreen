package com.android.zxkj.renderer;

import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import com.android.zxkj.dlna.core.utils.DeviceWiFiInfo;
import com.android.zxkj.dlna.dmr.DLNARendererService;
import com.permissionx.guolindev.PermissionX;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        PermissionX.init(this)
                .permissions(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION)
                .request((allGranted, grantedList, deniedList) -> resetWifiInfo());
        Log.d(TAG, "onCreate: ");
        DLNARendererService.startService(this);
    }

    private void resetWifiInfo() {
        ( (TextView)findViewById(R.id.tv_wifi)).setText("WiFi: " + DeviceWiFiInfo.getWiFiInfoSSID(this));
    }
}
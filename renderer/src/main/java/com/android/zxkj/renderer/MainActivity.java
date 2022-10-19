package com.android.zxkj.renderer;

import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.app.ActivityManager;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.android.zxkj.dlna.core.utils.DeviceWiFiInfo;
import com.android.zxkj.dlna.dmr.DLNARendererService;
import com.permissionx.guolindev.PermissionX;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setSupportActionBar(findViewById(R.id.toolbar));

        PermissionX.init(this)
                .permissions(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION)
                .request((allGranted, grantedList, deniedList) -> resetWifiInfo());
        startRendererService();
    }


    private void init() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        |View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        );
    }

    @Override
    protected void onResume() {
        super.onResume();
        startRendererService();
    }

    private void startRendererService() {
        if (!isServiceWorked(this, "DLNARendererService")) {
            Log.d(TAG, "startRendererService: " + "RendererService is not running");
            DLNARendererService.startService(this);
        } else {
            Log.d(TAG, "startRendererService: " + "RendererService is running");
        }
    }

    public static boolean isServiceWorked(Context context, String serviceName) {
        ActivityManager myManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        ArrayList<ActivityManager.RunningServiceInfo> runningService = (ArrayList<ActivityManager.RunningServiceInfo>) myManager.getRunningServices(Integer.MAX_VALUE);
        for (int i = 0; i < runningService.size(); i++) {
            final String serviceName1 = runningService.get(i).service.getClassName();
            Log.d(TAG, "isServiceWorked: " + serviceName1);
            if (runningService.get(i).service.getClassName().contains(serviceName)) {
                return true;
            }
        }
        return false;
    }

    private void resetWifiInfo() {
        getSupportActionBar().setTitle("DLNA Renderer");
        getSupportActionBar().setSubtitle("WiFi: " + DeviceWiFiInfo.getWiFiInfoSSID(this));
    }


}
package com.android.zxkj.cast_screen;

import android.Manifest;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.zxkj.cast_screen.adpter.DeviceAdapter;
import com.android.zxkj.dlna.core.utils.CastUtils;
import com.android.zxkj.dlna.core.utils.DeviceWiFiInfo;
import com.android.zxkj.dlna.dmc.DLNACastManager;
import com.permissionx.guolindev.PermissionX;


public class MainActivity extends AppCompatActivity {

    private DeviceAdapter mDeviceListAdapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        DLNACastManager.getInstance().enableLog();
        initComponent();
        PermissionX.init(this)
                .permissions(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION)
                .request((allGranted, grantedList, deniedList) -> resetToolbar());
    }

    private void resetToolbar() {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("DLNA Cast");
            getSupportActionBar().setSubtitle(DeviceWiFiInfo.getWiFiInfoSSID(this));
        }
    }

    private void initComponent() {
        RecyclerView recyclerView = findViewById(R.id.rv_devices);
        recyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        recyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        recyclerView.setAdapter(mDeviceListAdapter = new DeviceAdapter(this, (castDevice, selected) -> {
            mDeviceListAdapter.setSelectedDevice(selected ? castDevice : null);
        }));
        DLNACastManager.getInstance().registerDeviceListener(mDeviceListAdapter);
//        DLNACastManager.getInstance().search(null, 60);
    }

    @Override
    protected void onStart() {
        super.onStart();
        resetToolbar();
        DLNACastManager.getInstance().bindCastService(this);
    }
}

package com.android.zxkj.cast_screen;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
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

    private Fragment mControlFragment;
    private Fragment mLocalControlFragment;


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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_options, menu);
        return super.onCreateOptionsMenu(menu);
    }

    private void resetToolbar() {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("DLNA Cast");
            getSupportActionBar().setSubtitle(DeviceWiFiInfo.getWiFiInfoSSID(this));
        }
    }

    private void initComponent() {
        setSupportActionBar(findViewById(R.id.toolbar));

        mControlFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_control);
        mLocalControlFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_local_provider);

        RadioGroup typeGroup = findViewById(R.id.cast_type_group);
        typeGroup.setOnCheckedChangeListener(mOnCheckedChangeListener);
        typeGroup.check(R.id.cast_type_ctrl);

        RecyclerView recyclerView = findViewById(R.id.rv_devices);
        recyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        recyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        recyclerView.setAdapter(mDeviceListAdapter = new DeviceAdapter(this, (castDevice, selected) -> {
            mDeviceListAdapter.setSelectedDevice(selected ? castDevice : null);

            ((IDisplayDevice) mControlFragment).setCastDevice(selected ? castDevice: null);

            ((IDisplayDevice) mLocalControlFragment).setCastDevice(selected ? castDevice: null);
        }));

        // 会自动搜索局域网内的设备
        DLNACastManager.getInstance().registerDeviceListener(mDeviceListAdapter);
    }


    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_search_start) {
            Toast.makeText(this, "开始搜索", Toast.LENGTH_SHORT).show();
            DLNACastManager.getInstance().search(null, 60);
        }
        return super.onOptionsItemSelected(item);
    }

    private final RadioGroup.OnCheckedChangeListener mOnCheckedChangeListener = new RadioGroup.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(RadioGroup radioGroup, int i) {
            if (i == R.id.cast_type_ctrl) {
                getSupportFragmentManager().beginTransaction()
                        .show(mControlFragment)
                        .hide(mLocalControlFragment)
                        .commit();
            } else if (i == R.id.cast_type_ctrl_local) {
                getSupportFragmentManager().beginTransaction()
                        .show(mLocalControlFragment)
                        .hide(mControlFragment)
                        .commit();
            }
        }
    };

    @Override
    protected void onStart() {
        super.onStart();
        resetToolbar();
        DLNACastManager.getInstance().bindCastService(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        DLNACastManager.getInstance().unbindCastService(this);
    }

    @Override
    protected void onStop() {
        DLNACastManager.getInstance().unbindCastService(this);
        super.onStop();
    }

}

package com.android.zxkj.cast_screen;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.os.Environment;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.android.zxkj.dlna.core.utils.CastUtils;
import com.android.zxkj.dlna.dmc.DLNACastManager;
import com.android.zxkj.dlna.dms.MediaServer;

import org.fourthline.cling.model.meta.Device;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link LocalControlFragment} factory method to
 * create an instance of this fragment.
 */
public class LocalControlFragment extends Fragment implements IDisplayDevice {

    private static final int REQUEST_CODE_SELECT = 222;
    private TextView mPickupContent;
    private MediaServer mMediaServer;
    private String mCastPathUrl = "";
    private Device<?, ?, ?> mDevice;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_local_control, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initComponent(view);
        mMediaServer = new MediaServer(view.getContext());
        mMediaServer.start();

        DLNACastManager.getInstance().addMediaServer(mMediaServer.getmDevice());
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_SELECT && resultCode == Activity.RESULT_OK && data != null) {
            Uri uri = data.getData();
            String path = CastUtils.parseUri2Path(getActivity(), uri);
            final String transfromStr = tryTransformLocalMediaAddressToLocalHttpServerAddress(mMediaServer.getBaseUrl(),path);
            mCastPathUrl = transfromStr;
            mCastPathUrl = mMediaServer.getBaseUrl() + mCastPathUrl; //path;
            mPickupContent.setText(mCastPathUrl);
            PreferenceManager.getDefaultSharedPreferences(getActivity())
                    .edit()
                    .putString("selectPath", mCastPathUrl)
                    .apply();
        }
    }

    @Override
    public void setCastDevice(Device<?, ?, ?> device) {
        mDevice = device;
    }

    @Override
    public void onDestroyView() {
        DLNACastManager.getInstance().removeMediaServer(mMediaServer.getmDevice());
        mMediaServer.stop();
        super.onDestroyView();
    }

    private void initComponent(View view) {
        String selectPath = PreferenceManager.getDefaultSharedPreferences(getActivity()).getString("selectPath", "");
        mPickupContent = view.findViewById(R.id.local_ctrl_pick_content_text);
        mPickupContent.setText(selectPath);
        mCastPathUrl = selectPath;
        view.findViewById(R.id.local_ctrl_pick_content).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("video/*;audio/*;image/*");
                startActivityForResult(intent, REQUEST_CODE_SELECT);
            }
        });
        view.findViewById(R.id.local_ctrl_cast).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mDevice != null) {
                    DLNACastManager.getInstance().cast(mDevice,
                            CastObject.newInstance(mCastPathUrl, Constants.CAST_ID, Constants.CAST_NAME));

//                    String[] array = mCastPathUrl.split(":");
//                    if (array.length > 1) {
//                        String last = array[1];
//                        if (last.equals("jpg")  || last.equals("png")) {
//
//
//                        }else {
//                            DLNACastManager.getInstance().cast(mDevice,
//                                    CastObject.CastVideo.newInstance(mCastPathUrl, Constants.CAST_ID, Constants.CAST_NAME));
//                        }
//                    }

                }
            }
        });
    }


    private static boolean isLocalMediaAddress(String sourceUrl) {
        return !TextUtils.isEmpty(sourceUrl)
                && !sourceUrl.startsWith("http://")
                && !sourceUrl.startsWith("https://")
                && sourceUrl.startsWith(Environment.getExternalStorageDirectory().getAbsolutePath());
    }
    static String tryTransformLocalMediaAddressToLocalHttpServerAddress(@NonNull String context, @NonNull String sourceUrl) {
        Log.d("LocalControlFragment","tryTransformLocalMediaAddressToLocalHttpServerAddress, sourceUrl: " + sourceUrl);
        if (!isLocalMediaAddress(sourceUrl)) {
            return sourceUrl;
        }
        String newSourceUrl =
                sourceUrl.replace(Environment.getExternalStorageDirectory().getAbsolutePath(), "");
        Log.d("LocalControlFragment","tryTransformLocalMediaAddressToLocalHttpServerAddress ,newSourceUrl : " + newSourceUrl);

        try {
            final String[] urlSplits = newSourceUrl.split("/");
            final String originFileName = urlSplits[urlSplits.length - 1];
            String fileName = originFileName;
            fileName = URLEncoder.encode(fileName, "UTF-8");
            fileName = fileName.replaceAll("\\+", "%20");
            newSourceUrl = newSourceUrl.replace(originFileName, fileName);
            Log.d("LocalControlFragment","tryTransformLocalMediaAddressToLocalHttpServerAddress ,encodeNewSourceUrl : " + newSourceUrl);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        return newSourceUrl;
    }
}
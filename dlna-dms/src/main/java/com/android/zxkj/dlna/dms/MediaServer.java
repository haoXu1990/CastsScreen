package com.android.zxkj.dlna.dms;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import androidx.annotation.Nullable;

import com.android.zxkj.dlna.core.utils.DeviceWiFiInfo;

import org.fourthline.cling.binding.annotations.AnnotationLocalServiceBinder;
import org.fourthline.cling.model.DefaultServiceManager;
import org.fourthline.cling.model.ValidationException;
import org.fourthline.cling.model.meta.DeviceDetails;
import org.fourthline.cling.model.meta.DeviceIdentity;
import org.fourthline.cling.model.meta.Icon;
import org.fourthline.cling.model.meta.LocalDevice;
import org.fourthline.cling.model.meta.LocalService;
import org.fourthline.cling.model.meta.ManufacturerDetails;
import org.fourthline.cling.model.meta.ModelDetails;
import org.fourthline.cling.model.types.DeviceType;
import org.fourthline.cling.model.types.UDADeviceType;
import org.fourthline.cling.model.types.UDN;

import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.UUID;

public final class MediaServer {
    private static final String TAG = "MediaServer";
    private static final String DMS_DESC = "Zxine-MediaServer";
    // 创建设备 identity 时的 盐值
    private static final String ID_SALT = "GNap-MediaServer";
    public static final String TYPE_MEDIA_SERVER = "MediaServer";
    private static final int VERSION = 1;
    private static final int PORT = 9588;


    private LocalDevice mDevice;
    private IResourceServer mResourceServer;
    private String mBaseUrl;

    public MediaServer(Context context) {
        this(context, new IResourceServer.IResourecServerFactory.DefaultResourceServerFactoryImpl(PORT, DeviceWiFiInfo.getWiFiInfoIPAddress(context)));
    }
    public MediaServer(Context context, IResourceServer.IResourecServerFactory factory) {
        String wifiAddress = DeviceWiFiInfo.getWiFiInfoIPAddress(context);
        if (wifiAddress == DeviceWiFiInfo.UNKNOWN) {
            Log.e(TAG, "wifiAddress is unknown ");
        } else {
            mBaseUrl = String.format("http://%s:%s", wifiAddress, factory.getPort());
            ContentFactory.getInstance().setServerUrl(context, mBaseUrl);
            try {
                mDevice = createLocalDevice(context, wifiAddress);
                mResourceServer = factory.getInstance();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void start() {
        if (mResourceServer != null) {
            mResourceServer.startServer();
        }
    }

    public void stop() {
        if (mResourceServer != null) {
            mResourceServer.stopServer();
        }
    }

    @Nullable
    public String getBaseUrl() { return mBaseUrl; }

    @Nullable
    public LocalDevice getmDevice() { return mDevice; }


    protected LocalDevice createLocalDevice(Context context, String ipAddress) throws ValidationException {
        DeviceIdentity identity = new DeviceIdentity(createUniqueSystemIdentifier(ID_SALT, ipAddress));
        DeviceType type = new UDADeviceType(TYPE_MEDIA_SERVER, VERSION);
        DeviceDetails details = new DeviceDetails(String.format("DMS %s", Build.MODEL),
                new ManufacturerDetails(Build.MANUFACTURER),
                new ModelDetails(Build.MODEL, DMS_DESC, "v1", mBaseUrl));

        final LocalService<?> service = new AnnotationLocalServiceBinder().read(ContentDirectoryService.class);
        service.setManager(new DefaultServiceManager(service, ContentDirectoryService.class));

        Icon icon = null;
        try {
            icon = new Icon("image/png", 48,48,32,"msi.png",
                    context.getResources().getAssets().open("ic_launcher.png"));

        } catch (IOException ignored) {
        }

        return new LocalDevice(identity, type, details, icon, service);
    }

    private static UDN createUniqueSystemIdentifier(@SuppressWarnings("SameParameterValue") String salt, String ipAddress) {
        StringBuilder builder = new StringBuilder();
        builder.append(ipAddress);
        builder.append(Build.MODEL);
        builder.append(Build.MANUFACTURER);
        try {
            byte[] hash = MessageDigest.getInstance("MD5").digest(builder.toString().getBytes());
            return new UDN(new UUID(new BigInteger(-1, hash).longValue(),salt.hashCode()));
        } catch (Exception e) {
            return new UDN(e.getMessage() != null ? e.getMessage(): "UNKNOWN ERROR");
        }
    }

}


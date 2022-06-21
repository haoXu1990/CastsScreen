package com.android.zxkj.dlna.core.utils;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;

import androidx.core.app.ActivityCompat;
// 设备 WiFi 信息
public class DeviceWiFiInfo {
    private static final String NETWORK_TYPE_WIFI = "WiFi";
    private static final String NETWORK_TYPE_MOBILE = "Mobile EDGE>";
    private static final String NETWORK_TYPE_OTHERS = "Others>";

    private static final String WIFI_DISABLED = "<disabled>";
    private static final String WIFI_NO_CONNECT = "<not connect>";
    private static final String WIFI_NO_PERMISSION = "<permission deny>";

    public static final String UNKNOWN = "<unknown>";

    /**
     * need permission 'Manifest.permission.ACCESS_FINE_LOCATION' and 'Manifest.permission.ACCESS_WIFI_STATE' if system sdk >= Android O.
     */
    @SuppressLint("MissingPermission")
    public static String getWiFiInfoSSID(Context context) {
        WifiManager wifiManager = getSystemService(context, Context.WIFI_SERVICE);
        if (!wifiManager.isWifiEnabled()) return WIFI_DISABLED;
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        if (wifiInfo == null) return WIFI_NO_CONNECT;
        if (wifiInfo.getSSID().equals(WifiManager.UNKNOWN_SSID)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                        || ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_WIFI_STATE) != PackageManager.PERMISSION_GRANTED) {
                    return WIFI_NO_PERMISSION;
                }
                if (wifiManager.getConfiguredNetworks() != null) {
                    for (WifiConfiguration config : wifiManager.getConfiguredNetworks()) {
                        if (config.networkId == wifiInfo.getNetworkId()) {
                            return config.SSID.replaceAll("\"", "");
                        }
                    }
                }
            } else {
                return WIFI_NO_CONNECT;
            }
            return UNKNOWN;
        } else {
            return wifiInfo.getSSID().replaceAll("\"", "");
        }
    }

    public static String getWiFiInfoIPAddress(Context context) {
        WifiManager wifiManager = getSystemService(context, Context.WIFI_SERVICE);
        if (wifiManager == null || !wifiManager.isWifiEnabled()) return "";
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        if (wifiInfo == null) return UNKNOWN;
        int address = wifiInfo.getIpAddress();
        if (address == 0) return UNKNOWN;
        return (address & 0xFF) + "." + ((address >> 8) & 0xFF) + "." + ((address >> 16) & 0xFF) + "." + (address >> 24 & 0xFF);
    }

    public static String getActiveNetworkInfo(Context context) {
        ConnectivityManager connectivityManager = getSystemService(context, Context.CONNECTIVITY_SERVICE);
        if (connectivityManager == null) return UNKNOWN;
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        if (networkInfo == null) return UNKNOWN;
        if (networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
            return NETWORK_TYPE_WIFI;
        } else if (networkInfo.getType() == ConnectivityManager.TYPE_MOBILE) {
            return NETWORK_TYPE_MOBILE;
        } else {
            return NETWORK_TYPE_OTHERS;
        }
    }

    @SuppressWarnings({"unchecked", "TypeParameterExplicitlyExtendsObject", "SameParameterValue"})
    private static <T extends Object> T getSystemService(Context context, String name) {
        return (T) context.getApplicationContext().getSystemService(name);
    }
}

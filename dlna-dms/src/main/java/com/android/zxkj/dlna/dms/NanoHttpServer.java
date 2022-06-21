package com.android.zxkj.dlna.dms;

import android.os.Environment;

import androidx.annotation.NonNull;

import com.android.zxkj.dlna.webserver.org.nanohttpd.webserver.SimpleWebServer;
import com.orhanobut.logger.Logger;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;


public class NanoHttpServer implements IResourceServer {
    private static final String TAG = "NanoHttpServer";
    private int mPort;
    private String mIP;

    public NanoHttpServer(@NonNull String ip, @NonNull int port) {
        this.mIP = ip;
        this.mPort = port;
    }

    private void run() {
        final String localMediaRootPath = Environment.getExternalStorageDirectory().getAbsolutePath();
        String[] args = {
                "--host",
                mIP,
                "--port",
                String.valueOf(mPort),
                "--dir",
                localMediaRootPath
        };

        SimpleWebServer.startServer(args);
        Logger.i("initLocalLinkService success,localIpAddress : " + mIP +
                ",localVideoRootPath : " + localMediaRootPath);
    }
    @Override
    synchronized public void startServer() {
        try {
            final PipedOutputStream pipedOutputStream = new PipedOutputStream();
            System.setIn(new PipedInputStream(pipedOutputStream));
            new Thread((this::run)).start();
        } catch (IOException e) {
            Logger.i(TAG, e.toString());
        }
    }

    @Override
    public void stopServer() {

    }
}

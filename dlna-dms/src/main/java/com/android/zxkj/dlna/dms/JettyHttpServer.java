package com.android.zxkj.dlna.dms;

import android.util.Log;

import com.orhanobut.logger.Logger;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;

public class JettyHttpServer implements IResourceServer {
    private final Server mServer;
    private static final String TAG = "JettyHttpServer";
    public JettyHttpServer(int port) {
        mServer = new Server(port);
        mServer.setGracefulShutdown(1000);
    }

    synchronized public void startServer() {
        Log.d(TAG, "startServer: ");
        if (!mServer.isStarted() && !mServer.isStarting()) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    ServletContextHandler contextHandler = new ServletContextHandler();
                    contextHandler.setContextPath("/");
                    contextHandler.setInitParameter("org.eclipse.jetty.servlet.Default.gzip", "false");
                    contextHandler.addServlet(ContentResourceServlet.class, "/");
                    mServer.setHandler(contextHandler);
                    Log.d(TAG, "JettyServer start. ");
                    try {
                        mServer.start();
                        // 打印dump时的信息
                        System.out.println(mServer.dump());
                        mServer.join();
                        Log.d(TAG, "run: JettyServer complete.");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }
    }

    synchronized public void stopServer() {
        if (!mServer.isStopped() && !mServer.isStopping()) {
            try {
                Logger.i("JettyServer stop.");
                mServer.stop();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
}

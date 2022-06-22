package com.android.zxkj.dlna.dms;

import androidx.annotation.NonNull;

public interface IResourceServer {

    void startServer();

    void stopServer();


    interface IResourecServerFactory {
        int getPort();
        String getIp();

        IResourceServer getInstance();

        // implement
        final class DefaultResourceServerFactoryImpl implements IResourecServerFactory {
            private final int port;
            private final boolean useJetty;
            private final String ip;

            public DefaultResourceServerFactoryImpl(@NonNull int port,@NonNull String ip) {
                this(port, ip, false);
            }

            public DefaultResourceServerFactoryImpl(@NonNull int port, @NonNull String ip,@NonNull boolean userJetty) {
                this.port = port;
                this.useJetty = userJetty;
                this.ip = ip;
            }

            @Override
            public int getPort() {
                return port;
            }

            @Override
            public String getIp() {
                return ip;
            }

            @Override
            public IResourceServer getInstance() {
                if (useJetty) {
                    return new JettyHttpServer(port);
                }
                return new NanoHttpServer(ip, port);
            }
        }
    }


}

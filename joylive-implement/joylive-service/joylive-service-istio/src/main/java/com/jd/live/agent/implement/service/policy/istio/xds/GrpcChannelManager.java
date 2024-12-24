package com.jd.live.agent.implement.service.policy.istio.xds;

import java.io.File;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLException;

import com.jd.live.agent.implement.service.policy.istio.config.IstioConfig;

import io.grpc.ManagedChannel;
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContext;
import lombok.Getter;

public class GrpcChannelManager implements AutoCloseable {

    private final IstioConfig config;

    @Getter
    private ManagedChannel channel;

    @Getter
    private final Object lock = new Object();

    public GrpcChannelManager(IstioConfig config) {
        this.config = config;
    }

    private ManagedChannel createChannel() throws SSLException {
        NettyChannelBuilder channelBuilder = NettyChannelBuilder.forTarget(config.getIstioAddress());

        if (config.isSslEnabled()) {
            SslContext context = GrpcSslContexts.forClient()
                .trustManager(new File(config.getSslCertPath()))
                .build();
            channelBuilder.sslContext(context);
        }
        return channelBuilder.usePlaintext()
            .keepAliveTime(30, TimeUnit.SECONDS)
            .keepAliveTimeout(10, TimeUnit.SECONDS)
            .keepAliveWithoutCalls(true)
            .build();
    }

    public ManagedChannel resetChannelIfNecessary() throws SSLException {
        if (channel == null || channel.isShutdown() || channel.isTerminated()) {
            synchronized (lock) {
                if (channel == null || channel.isShutdown() || channel.isTerminated()) {
                    channel = createChannel();
                }
            }
        }
        return channel;
    }


    @Override
    public void close() throws Exception {
        if (channel != null) {
            channel.shutdownNow();
            channel = null;
        }
    }
}

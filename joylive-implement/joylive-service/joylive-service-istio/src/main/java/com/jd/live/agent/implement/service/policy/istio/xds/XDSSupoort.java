package com.jd.live.agent.implement.service.policy.istio.xds;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;
import com.jd.live.agent.implement.service.policy.istio.config.IstioConfig;

import io.envoyproxy.envoy.config.core.v3.Node;
import io.envoyproxy.envoy.service.discovery.v3.AggregatedDiscoveryServiceGrpc;
import io.envoyproxy.envoy.service.discovery.v3.DiscoveryRequest;
import io.envoyproxy.envoy.service.discovery.v3.DiscoveryResponse;
import io.grpc.ManagedChannel;
import io.grpc.stub.StreamObserver;

import com.google.protobuf.InvalidProtocolBufferException;

public class XDSSupoort {

    private static final Logger logger = LoggerFactory.getLogger(XDSSupoort.class);


    public static Node buildNode(IstioConfig config) {
        String id = String.format("sidecar~%s~%s.%s~%s",
        getLocalIP(),
        config.getPodName(),
        config.getNamespace(),
        config.getNamespace() + ".svc.cluster.local");

        return io.envoyproxy.envoy.config.core.v3.Node.newBuilder()
            .setId(id)
            .setCluster(config.getClusterName())
            .setMetadata(com.google.protobuf.Struct.newBuilder()
            .putFields("NAMESPACE", com.google.protobuf.Value.newBuilder()
                .setStringValue(config.getNamespace()).build())
            .putFields("ISTIO_VERSION", com.google.protobuf.Value.newBuilder()
                .setStringValue(config.getIstioVersion()).build())
            .build())
        .build();
    }

    public static AggregatedDiscoveryServiceGrpc.AggregatedDiscoveryServiceStub createADSStub(ManagedChannel channel) {
        return AggregatedDiscoveryServiceGrpc.newStub(channel);
    }

    private static String getLocalIP() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            return "127.0.0.1";
        }
    }

    public static <T extends com.google.protobuf.Message> List<T> extractResources(DiscoveryResponse response, Class<T> resourceType) {
        return response.getResourcesList().stream()
            .filter(Objects::nonNull)
            .filter(any -> any.is(resourceType))
            .map(any -> {
                try {
                    return any.<T>unpack(resourceType);
                } catch (InvalidProtocolBufferException e) {
                    logger.error("Failed to unpack resource for type: {}.", resourceType.getSimpleName(), e);
                    return null;
                }
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    public static DiscoveryRequest buildDiscoveryRequest(List<String> resourceNames, Node node, String typeUrl) {
        return DiscoveryRequest.newBuilder()
            .setNode(node)
            .addAllResourceNames(resourceNames == null ? Collections.emptyList() : resourceNames)
            .setTypeUrl(typeUrl)
            .build();
    }

    public static StreamObserver<DiscoveryResponse> buildResponseObserver(Consumer<DiscoveryResponse> responseConsumer, GrpcChannelManager channelManager, CountDownLatch latch) {
        return new StreamObserver<DiscoveryResponse>() {

            private void countdownSafely() {
                if (latch != null) {
                    latch.countDown();
                }
            }

            @Override
            public void onNext(DiscoveryResponse response) {
                responseConsumer.accept(response);
                countdownSafely();
            }

            @Override
            public void onError(Throwable t) {
                logger.error("Stream error: " + t.getMessage());
                countdownSafely();
            }

            @Override
            public void onCompleted() {
                logger.info("Stream completed");
                countdownSafely();
            }
        };
    }
}


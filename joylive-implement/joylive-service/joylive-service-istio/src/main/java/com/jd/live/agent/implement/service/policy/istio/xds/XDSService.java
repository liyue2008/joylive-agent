package com.jd.live.agent.implement.service.policy.istio.xds;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import javax.net.ssl.SSLException;

import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;
import com.jd.live.agent.implement.service.policy.istio.config.IstioConfig;

import io.envoyproxy.envoy.config.core.v3.Node;
import io.envoyproxy.envoy.service.discovery.v3.AggregatedDiscoveryServiceGrpc;
import io.envoyproxy.envoy.service.discovery.v3.DiscoveryRequest;
import io.envoyproxy.envoy.service.discovery.v3.DiscoveryResponse;
import io.grpc.stub.StreamObserver;



public abstract class XDSService<T extends com.google.protobuf.Message> {

    private static final Logger logger = LoggerFactory.getLogger(XDSService.class);

    protected final IstioConfig config;
    protected final GrpcChannelManager channelManager;
    protected final Node node;
    protected AggregatedDiscoveryServiceGrpc.AggregatedDiscoveryServiceStub stub;
    protected StreamObserver<DiscoveryRequest> requestObserver = null;

    private final Object lock = new Object();

    private final ResonseObserver<T> responseObserver;
    

    public XDSService(IstioConfig config, GrpcChannelManager channelManager) {
        this.config = config;
        this.channelManager = channelManager;
        this.node = XDSSupoort.buildNode(config);
        this.responseObserver = new ResonseObserver<>(channelManager, getResourceClass());
    }

    public void start() {
        synchronized (lock) {
            this.stub = XDSSupoort.createADSStub(channelManager.getChannel());
            
        }
    }


    public Future<List<T>> subscribeResources(List<String> resourceNames) {
        
        DiscoveryRequest request = XDSSupoort.buildDiscoveryRequest(resourceNames, node, getResourceTypeUrl());
        ResponseStreamObserverFuture<T> responseStreamObserverFuture = new ResponseStreamObserverFuture<>(getResourceClass(), channelManager);
        StreamObserver<DiscoveryRequest> requestObserver = stub.streamAggregatedResources(responseStreamObserverFuture);
        requestObserver.onNext(request);
        requestObserver.onCompleted();
        return responseStreamObserverFuture;
    }

    public void subscribeResourcesAsync(List<String> resourceNames) {
        DiscoveryRequest request = XDSSupoort.buildDiscoveryRequest(resourceNames, node, getResourceTypeUrl());
        maybeCreateRequestObserverSafely();
        requestObserver.onNext(request);
    }

    private void maybeCreateRequestObserverSafely() {
        if (requestObserver == null) {
            synchronized (lock) {
                if (requestObserver == null) {  
                    requestObserver =
                        stub.streamAggregatedResources(this.responseObserver);
                }
            }
        }
    }

    protected abstract String getResourceTypeUrl();

    protected abstract Class<T> getResourceClass();

    public void addConsumer(Consumer<List<T>> consumer) {
        responseObserver.addConsumer(consumer);
    }

    public void addOnCompleteCallback(Runnable callback) {
        responseObserver.addOnCompleteCallback(callback);
    } 

    public void addOnErrorCallback(Consumer<Throwable> callback) {
        responseObserver.addOnErrorCallback(callback);
    }


    // TODO: 支持ACK
    private static class ResonseObserver<T extends com.google.protobuf.Message> implements StreamObserver<DiscoveryResponse> {

        private final List<Consumer<List<T>>> resourceConsumers = new CopyOnWriteArrayList<>();
        private final GrpcChannelManager channelManager;
        private final Class<T> resourceClass;
        private final List<Runnable> onCompleteCallbacks = new CopyOnWriteArrayList<>();
        private final List<Consumer<Throwable>> onErrorCallbacks = new CopyOnWriteArrayList<>();

        public ResonseObserver(GrpcChannelManager channelManager, Class<T> resourceClass) {

            this.channelManager = channelManager;
            this.resourceClass = resourceClass;

        }
        @Override
        public void onNext(DiscoveryResponse value) {
            resourceConsumers.forEach(consumer -> consumer.accept(XDSSupoort.extractResources(value, resourceClass)));
        }
        @Override
        public void onError(Throwable t) {
            logger.error("Stream error: " + t.getMessage());
            try {
                channelManager.resetChannelIfNecessary();
            } catch (SSLException e) {
                logger.error("Failed to reset channel: ", e);
            }
            onErrorCallbacks.forEach(callback -> callback.accept(t));
        }

        @Override
        public void onCompleted() {
            logger.info("Stream completed");
            onCompleteCallbacks.forEach(callback -> callback.run());
        }

        public void addConsumer(Consumer<List<T>> consumer) {
            resourceConsumers.add(consumer);
        }

        public void addOnCompleteCallback(Runnable callback) {
            onCompleteCallbacks.add(callback);
        } 

        public void addOnErrorCallback(Consumer<Throwable> callback) {
            onErrorCallbacks.add(callback);
        }
    }
    
}


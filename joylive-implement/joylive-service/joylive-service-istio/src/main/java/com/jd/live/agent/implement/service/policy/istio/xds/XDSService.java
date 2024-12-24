package com.jd.live.agent.implement.service.policy.istio.xds;

import java.util.List;
import java.util.concurrent.Future;

import com.jd.live.agent.implement.service.policy.istio.config.IstioConfig;

import io.envoyproxy.envoy.config.core.v3.Node;
import io.envoyproxy.envoy.service.discovery.v3.AggregatedDiscoveryServiceGrpc;
import io.envoyproxy.envoy.service.discovery.v3.DiscoveryRequest;
import io.grpc.stub.StreamObserver;


public abstract class XDSService<T extends com.google.protobuf.Message> {

    protected final IstioConfig config;
    protected final GrpcChannelManager channelManager;
    protected final Node node;
    protected AggregatedDiscoveryServiceGrpc.AggregatedDiscoveryServiceStub stub;
    protected StreamObserver<DiscoveryRequest> requestObserver = null;
    protected ResponseStreamObserverFuture<T> responseStreamObserverFuture = null;
    private final Object lock = new Object();

    public XDSService(IstioConfig config, GrpcChannelManager channelManager) {
        this.config = config;
        this.channelManager = channelManager;
        this.node = XDSSupoort.buildNode(config);
    }

    public void start() {
        synchronized (lock) {
            this.stub = XDSSupoort.createADSStub(channelManager.getChannel());
            this.responseStreamObserverFuture = new ResponseStreamObserverFuture<>(getResourceClass(), channelManager);
        }
    }


    public Future<List<T>> subscribeResources(List<String> resourceNames) {
        
        DiscoveryRequest request = XDSSupoort.buildDiscoveryRequest(resourceNames, node, getResourceTypeUrl());
        maybeCreateRequestObserverSafely();
        requestObserver.onNext(request);
        return responseStreamObserverFuture;
    }

    private void maybeCreateRequestObserverSafely() {
        if (requestObserver == null) {
            synchronized (lock) {
                if (requestObserver == null) {  
                    requestObserver =
                        stub.streamAggregatedResources(responseStreamObserverFuture);
                }
            }
        }
    }
    
    public Future<List<T>> getResourceFutuerFuture() {
        return responseStreamObserverFuture;
    }

    protected abstract String getResourceTypeUrl();

    protected abstract Class<T> getResourceClass();
}

package com.jd.live.agent.xds;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.net.ssl.SSLException;

import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;

import io.envoyproxy.envoy.service.discovery.v3.DiscoveryResponse;
import io.grpc.stub.StreamObserver;

public class ResponseStreamObserverFuture<T extends com.google.protobuf.Message> implements StreamObserver<DiscoveryResponse>, Future<List<T>> {

    private static final Logger logger = LoggerFactory.getLogger(ResponseStreamObserverFuture.class);

    private List<T> resources;

    private final Class<T> resourceClass;

    private final GrpcChannelManager channelManager;

    private final CountDownLatch latch = new CountDownLatch(1);

    private void onComplete() {
        if (!isDone()) {
            latch.countDown();
        }
    }

    public ResponseStreamObserverFuture(Class<T> resourceClass, GrpcChannelManager channelManager) {
        this.resourceClass = resourceClass;
        this.channelManager = channelManager;
    }

    @Override
    public void onNext(DiscoveryResponse response) {

        this.resources = XDSSupport.extractResources(response, resourceClass);
        onComplete();
    }

    @Override
    public void onError(Throwable t) {
        logger.error("Stream error: " + t.getMessage());
        try {
            channelManager.resetChannelIfNecessary();
        } catch (SSLException e) {
            logger.error("Failed to reset channel: ", e);
        }
        onComplete();
    }

    @Override
    public void onCompleted() {
        logger.info("Stream completed");
        onComplete();
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return false;
    }

    @Override
    public List<T> get() throws InterruptedException, ExecutionException {
        latch.await();
        return resources;
    }

    @Override
    public List<T> get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        if (latch.await(timeout, unit)) {
            return resources;
        }
        throw new TimeoutException("Stream observer did not complete within the specified timeout");
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    public boolean isDone() {
        return latch.getCount() == 0;
    }

}

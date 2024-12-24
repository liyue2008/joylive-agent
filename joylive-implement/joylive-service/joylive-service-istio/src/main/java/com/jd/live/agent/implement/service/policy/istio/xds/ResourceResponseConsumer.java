package com.jd.live.agent.implement.service.policy.istio.xds;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import io.envoyproxy.envoy.service.discovery.v3.DiscoveryResponse;
import lombok.Getter;
import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;

public class ResourceResponseConsumer<T extends com.google.protobuf.Message> implements Consumer<DiscoveryResponse> {

    private final static Logger logger = LoggerFactory.getLogger(ResourceResponseConsumer.class);
    private final Class<T> resourceClass;

    private List<T> resources;

    @Getter
    private final CountDownLatch latch;

    public ResourceResponseConsumer(Class<T> resourceClass) {
        this.resourceClass = resourceClass;
        this.latch = new CountDownLatch(1);
    }

    @Override
    public void accept(DiscoveryResponse discoveryResponse) {
        this.resources = XDSSupoort.extractResources(discoveryResponse, resourceClass);
    }

    public List<T> getResources() {
        try {
            latch.await(15, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            logger.error("Interrupted while waiting for response", e);
        }
        return resources;
    }
}

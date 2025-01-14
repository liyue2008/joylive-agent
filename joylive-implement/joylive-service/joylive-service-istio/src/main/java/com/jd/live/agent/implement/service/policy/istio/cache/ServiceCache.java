package com.jd.live.agent.implement.service.policy.istio.cache;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;
import com.jd.live.agent.governance.policy.service.Service;
import com.jd.live.agent.governance.policy.service.ServiceGroup;
import com.jd.live.agent.governance.policy.service.ServiceType;
import com.jd.live.agent.xds.GrpcChannelManager;
import com.jd.live.agent.xds.LDSService;
import com.jd.live.agent.xds.RDSService;
import com.jd.live.agent.xds.XDSConvert;
import com.jd.live.agent.xds.config.IstioConfig;

import io.envoyproxy.envoy.config.route.v3.RouteConfiguration;

public class ServiceCache extends AbstractLazyCache<String /* service name */, Service> implements AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(ServiceCache.class);

    protected final GrpcChannelManager channelManager;
    protected final LDSService ldsService;
    protected final RDSService rdsService;

    public ServiceCache(IstioConfig istioConfig) {
        this.channelManager = new GrpcChannelManager(istioConfig);
        this.ldsService = new LDSService(istioConfig, channelManager);
        this.rdsService = new RDSService(istioConfig, channelManager);
    }

    @Override
    public void close() throws Exception {
        channelManager.close();
    }

    @Override
    protected void loadResourceAsync() {
        try {
            channelManager.resetChannelIfNecessary();
            rdsService.addConsumer(this::updateServiceCache);
            rdsService.addOnErrorCallback(this::onResourceError);
            ldsService.addConsumer(listeners -> rdsService.subscribeResourcesAsync(LDSService.getRdsNames(listeners)));
            ldsService.addOnErrorCallback(this::onResourceError);
            ldsService.subscribeResourcesAsync(Collections.emptyList());
        } catch (Exception e) {
            logger.error("Error loading resources", e);
        }
    }

    protected void updateServiceCache(List<RouteConfiguration> routes) {
        Map<String, Service> serviceCache = new ConcurrentHashMap<>();
        routes.forEach(route -> {
            route.getVirtualHostsList().forEach(virtualHost -> {
                ServiceGroup serviceGroup = XDSConvert.virtualHostToServiceGroup(virtualHost);
                virtualHost.getDomainsList().forEach(domain -> {
                    if (domain.startsWith("*") || domain.endsWith("*")) {
                        logger.info("Ignore unsupported domain: {}", domain);
                    } else {
                        Service service = new Service(domain, ServiceType.HTTP);
                        service.setGroups(Collections.singletonList(serviceGroup));
                        serviceCache.put(domain, service);
                    }
                });
            });
        });

        onResourceReady(serviceCache);
    }

    protected Map<String, Service> getCache() {
        return cache;
    }
}

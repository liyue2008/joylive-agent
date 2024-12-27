package com.jd.live.agent.implement.service.policy.istio.xds;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.net.ssl.SSLException;

import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;
import com.jd.live.agent.implement.service.policy.istio.config.IstioConfig;

import io.envoyproxy.envoy.config.endpoint.v3.ClusterLoadAssignment;
import io.envoyproxy.envoy.config.route.v3.RouteConfiguration;
import io.envoyproxy.envoy.config.route.v3.VirtualHost;




public class XDSLocalCache implements AutoCloseable {

    private final static Logger logger = LoggerFactory.getLogger(XDSLocalCache.class);

    private final IstioConfig istioConfig;

    private final GrpcChannelManager channelManager;

    private final LDSService ldsService;

    private final RDSService rdsService;

    private final EDSService edsService;

    private final CDSService cdsService;

    private Map<String /* domain */, VirtualHost> virtualHostCache = Collections.emptyMap();

    private Map<String /* clusterName */, List<ClusterLoadAssignment>> endpointsCache = Collections.emptyMap();
    public XDSLocalCache(IstioConfig istioConfig) {
        this.istioConfig = istioConfig;
        this.channelManager = new GrpcChannelManager(istioConfig);
        this.ldsService = new LDSService(istioConfig, channelManager);
        this.rdsService = new RDSService(istioConfig, channelManager);
        this.edsService = new EDSService(istioConfig, channelManager);
        this.cdsService = new CDSService(istioConfig, channelManager);
    }

    public void start() {

        try {
            channelManager.resetChannelIfNecessary();
            subscribeRoutes();
            subscribeEndpoints();
        } catch (SSLException e) {
            logger.error("Error starting XDSLocalCache", e);
        }
    }

    private void subscribeRoutes() {
        rdsService.addConsumer(this::updateVirtualHosts);
        ldsService.addConsumer(listeners -> rdsService.subscribeRoutes(LDSService.getRdsNames(listeners)));
        ldsService.subscribeResourcesAsync(Collections.emptyList());
    }

    private void subscribeEndpoints() {
        edsService.addConsumer(this::updateEndpoints);
        cdsService.addConsumer(clusters -> edsService.subscribeEndpoints(CDSService.getClusterNames(clusters)));
        cdsService.subscribeResourcesAsync(Collections.emptyList());
    }

    @Override
    public void close() throws Exception {
        channelManager.close();
    }

    private void updateVirtualHosts(List<RouteConfiguration> routes) {
        Map<String, VirtualHost> newVirtualHostCache = new ConcurrentHashMap<>();
        if (routes == null || routes.isEmpty()) {
            return;
        }
        routes.forEach(route -> {
            route.getVirtualHostsList().forEach(virtualHost -> {
                virtualHost.getDomainsList().forEach(domain -> {
                    if (domain.startsWith("*") || domain.endsWith("*")) {
                        logger.info("Ignore unsupported domain: {}", domain);
                    } else {
                        newVirtualHostCache.put(domain, virtualHost);
                    }
                });
            });
        });
        virtualHostCache = newVirtualHostCache;
    }

    private void updateEndpoints(List<ClusterLoadAssignment> endpoints) {
        Map<String, List<ClusterLoadAssignment>> newEndpointsCache = new HashMap<>();
        endpoints.forEach(endpoint -> {
            newEndpointsCache.computeIfAbsent(endpoint.getClusterName(), k -> new ArrayList<>()).add(endpoint);
        });
        endpointsCache = newEndpointsCache;
    }

    public VirtualHost getVirtualHosts(String domain) {
        return virtualHostCache.get(domain);
    }

    public List<ClusterLoadAssignment> getEndpoints(String clusterName) {
        return endpointsCache.get(clusterName);
    }
}

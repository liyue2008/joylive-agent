package com.jd.live.agent.implement.service.policy.istio.xds;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import javax.net.ssl.SSLException;

import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;
import com.jd.live.agent.implement.service.policy.istio.config.IstioConfig;

import io.envoyproxy.envoy.config.listener.v3.Listener;
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

    private final Map<String /* domain */, VirtualHost> virtualHostCache = new ConcurrentHashMap<>();


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
            ldsService.start();
            rdsService.start();
            edsService.start();
            cdsService.start();

            subscribeRoutes();
        } catch (SSLException e) {
            logger.error("Error starting XDSLocalCache", e);
        }
    }

    private void subscribeRoutes() {
        rdsService.addConsumer(this::updateVirtualHosts);
        ldsService.addConsumer(listeners -> rdsService.subscribeRoutes(LDSService.getRdsNames(listeners)));
        ldsService.subscribeResourcesAsync(Collections.emptyList());
    }
    @Override
    public void close() throws Exception {
        channelManager.close();
    }

    private void updateVirtualHosts(List<RouteConfiguration> routes) {
        if (routes == null || routes.isEmpty()) {
            return;
        }
        routes.forEach(route -> {
            route.getVirtualHostsList().forEach(virtualHost -> {
                virtualHost.getDomainsList().forEach(domain -> {
                    if (domain.startsWith("*") || domain.endsWith("*")) {
                        logger.info("Ignore unsupported domain: {}", domain);
                        
                    } else {
                        virtualHostCache.put(domain, virtualHost);
                    }
                });
            });
        });
    }

    public VirtualHost getVirtualHosts(String domain) {
        return virtualHostCache.get(domain);
    }
}

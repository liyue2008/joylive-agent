package com.jd.live.agent.implement.service.policy.istio.xds;

import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;
import com.jd.live.agent.implement.service.policy.istio.config.IstioConfig;

import io.envoyproxy.envoy.config.route.v3.RouteConfiguration;

public class RDSService extends XDSService<RouteConfiguration> {

    private final static Logger logger = LoggerFactory.getLogger(RDSService.class);

    private final static String RDS_TYPE_URL = "type.googleapis.com/envoy.config.route.v3.RouteConfiguration";

    public RDSService(IstioConfig istioConfig, GrpcChannelManager channelManager) {
        super(istioConfig, channelManager);
    }

    public List<RouteConfiguration> subscribeRoutes(List<String> rdsNames) {
        Future<List<RouteConfiguration>> routeConfigurationFutures = subscribeResources(rdsNames);
        try {
            return routeConfigurationFutures.get(15, TimeUnit.SECONDS);
        } catch (Exception e) {
            logger.error("Error subscribing routes", e);
            return null;
        }
    }

    @Override
    protected String getResourceTypeUrl() {
        return RDS_TYPE_URL;
    }

    @Override
    protected Class<RouteConfiguration> getResourceClass() {
        return RouteConfiguration.class;
    }

}

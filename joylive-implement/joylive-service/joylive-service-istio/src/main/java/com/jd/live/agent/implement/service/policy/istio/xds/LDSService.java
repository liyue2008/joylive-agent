package com.jd.live.agent.implement.service.policy.istio.xds;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;
import com.jd.live.agent.implement.service.policy.istio.config.IstioConfig;

import io.envoyproxy.envoy.config.listener.v3.Listener;
import io.envoyproxy.envoy.extensions.filters.network.http_connection_manager.v3.HttpConnectionManager;
import io.envoyproxy.envoy.extensions.filters.network.http_connection_manager.v3.Rds;


public class LDSService extends XDSService<Listener> {

    private final static Logger logger = LoggerFactory.getLogger(LDSService.class);
    private final static String LDS_TYPE_URL = "type.googleapis.com/envoy.config.listener.v3.Listener";
    private final static String HTTP_CONNECTION_MANAGER_FILTER_NAME = "envoy.filters.network.http_connection_manager";

    public LDSService(IstioConfig config, GrpcChannelManager channelManager) {
        super(config, channelManager);
    }

    public List<Listener> subscribeListeners() {

        Future<List<Listener>> listenerFutures = subscribeResources(null);
        try {
            return listenerFutures.get(15, TimeUnit.SECONDS);
        } catch (Exception e) {
            logger.error("Error subscribing listeners", e);
            return null;    
        }
    }

    public static List<String> getRdsNames(List<Listener> listeners) {
        List<String> rdsNames = new ArrayList<>();

        for (Listener listener : listeners) {
            if(listener.getAddress().getSocketAddress().getPortValue() == 80) {
                // 处理过滤器链
                listener.getFilterChainsList().forEach(filterChain -> {
                    filterChain.getFiltersList().forEach(filter -> {
                        if (HTTP_CONNECTION_MANAGER_FILTER_NAME.equals(filter.getName())) {
                            try {
                                HttpConnectionManager hcm = filter.getTypedConfig().unpack(HttpConnectionManager.class);
                                if (hcm.hasRds()) {
                                    Rds rds = hcm.getRds();
                                    String routeConfigName = rds.getRouteConfigName();
                                    rdsNames.add(routeConfigName);
                                }
                            } catch (Exception e) {
                                logger.error("Error processing HTTP connection manager config", e);
                            }
                        }
                    });
                });
            }
        }
        return rdsNames;
    }

    @Override
    protected String getResourceTypeUrl() {
        return LDS_TYPE_URL;
    }

    @Override
    protected Class<Listener> getResourceClass() {
        return Listener.class;
    }
}

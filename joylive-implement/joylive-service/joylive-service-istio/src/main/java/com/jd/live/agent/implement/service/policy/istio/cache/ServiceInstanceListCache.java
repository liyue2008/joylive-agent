package com.jd.live.agent.implement.service.policy.istio.cache;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.cloud.client.ServiceInstance;

import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;

import com.jd.live.agent.xds.EDSService;
import com.jd.live.agent.xds.GrpcChannelManager;
import com.jd.live.agent.xds.LDSService;
import com.jd.live.agent.xds.RDSService;
import com.jd.live.agent.xds.XDSConvert;
import com.jd.live.agent.xds.config.IstioConfig;

import io.envoyproxy.envoy.config.endpoint.v3.ClusterLoadAssignment;
import io.envoyproxy.envoy.config.endpoint.v3.LocalityLbEndpoints;
import io.envoyproxy.envoy.config.route.v3.RouteConfiguration;

public class ServiceInstanceListCache extends AbstractLazyCache<String /* service name */, List<ServiceInstance>> implements AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(ServiceInstanceListCache.class);


    private final GrpcChannelManager channelManager;

    private final EDSService edsService;

    private final LDSService ldsService;

    private final RDSService rdsService;

    private Map<String /* istio cluster name */, List<String>>  /* service name / domain */ serviceNameMap = new HashMap<>();



    public ServiceInstanceListCache(IstioConfig istioConfig) {
        this.channelManager = new GrpcChannelManager(istioConfig);
        this.edsService = new EDSService(istioConfig, channelManager);
        this.ldsService = new LDSService(istioConfig, channelManager);
        this.rdsService = new RDSService(istioConfig, channelManager);
    }

    @Override
    protected void loadResourceAsync() {
        try {
            channelManager.resetChannelIfNecessary();

            edsService.addConsumer(this::updateEndpoints);
            edsService.addOnErrorCallback(this::onResourceError);
            rdsService.addConsumer(routes -> edsService.subscribeResourcesAsync(RDSService.getEdsNames(routes)));
            rdsService.addConsumer(this::updateServiceNameMap);
            rdsService.addOnErrorCallback(this::onResourceError);
            ldsService.addConsumer(listeners -> rdsService.subscribeResourcesAsync(LDSService.getRdsNames(listeners)));
            ldsService.addOnErrorCallback(this::onResourceError);
            ldsService.subscribeResourcesAsync(Collections.emptyList());
        } catch (Exception e) {
            logger.error("Error loading resources", e);
        }
    }


    private void updateServiceNameMap(List<RouteConfiguration> routes) {

        Map<String, List<String>> serviceNameMap = new HashMap<>();
        if (routes == null || routes.isEmpty()) {
            return;
        }
        routes.forEach(route -> {
            route.getVirtualHostsList().forEach(virtualHost -> {
                virtualHost.getDomainsList().forEach(domain -> {
                    if (domain.startsWith("*") || domain.endsWith("*")) {
                        logger.warn("Ignore unsupported domain: {}", domain);
                        return;
                    }

                    String serviceName = domain;
                    List<String> clusterNames = XDSConvert.getClusterNamesFromVirtualHost(virtualHost);
                    logger.info(domain + " -> " + clusterNames);
                    clusterNames.forEach(clusterName -> {
                        if (serviceNameMap.containsKey(clusterName)) {
                            serviceNameMap.get(clusterName).add(serviceName);
                        } else {
                            serviceNameMap.put(clusterName, new ArrayList<>());
                            serviceNameMap.get(clusterName).add(serviceName);
                        }
                    });

                });
            });
        });
        this.serviceNameMap = serviceNameMap;
    }

    private void updateEndpoints(List<ClusterLoadAssignment> endpoints) {
        Map<String, List<ServiceInstance>> resources = new HashMap<>();

        endpoints.forEach(clusterLoadAssignment -> {
            if (clusterLoadAssignment.getEndpointsCount() > 0) {
                String clusterName = clusterLoadAssignment.getClusterName();
                List<LocalityLbEndpoints> localityLbEndpoints = clusterLoadAssignment.getEndpointsList();
                List<String> serviceNames = serviceNameMap.get(clusterName);
                if (null != serviceNames && !serviceNames.isEmpty()) {
                    for (String serviceName : serviceNames) {
                        List<ServiceInstance> serviceInstances = XDSConvert.endpointsToServiceInstances(localityLbEndpoints, clusterName, serviceName);
                        if (resources.containsKey(serviceName)) {
                            resources.get(serviceName).addAll(serviceInstances);
                        } else {
                            resources.put(serviceName, serviceInstances);
                        }
                    }
                } else {
                    logger.warn("No service name found for cluster {}", clusterName);
                }

            }
        });
        onResourceReady(resources);

    }


    @Override
    public void close() throws Exception {
        channelManager.close();
    }
}

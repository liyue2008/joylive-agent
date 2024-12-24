package com.jd.live.agent.implement.service.policy.istio.xds;

import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;
import com.jd.live.agent.implement.service.policy.istio.config.IstioConfig;

import io.envoyproxy.envoy.config.endpoint.v3.ClusterLoadAssignment;
import io.envoyproxy.envoy.config.listener.v3.Listener;
import io.envoyproxy.envoy.config.route.v3.Route;
import io.envoyproxy.envoy.config.route.v3.RouteConfiguration;
import io.envoyproxy.envoy.config.route.v3.VirtualHost;


import java.util.Collections;
import java.util.List;

import javax.net.ssl.SSLException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


public class LDSServiceTest {

    private static final Logger logger = LoggerFactory.getLogger(LDSServiceTest.class);

    private IstioConfig istioConfig;

    private GrpcChannelManager channelManager;

    private LDSService ldsService;

    private RDSService rdsService;

    private EDSService edsService;

    private CDSService cdsService;

    @BeforeEach
    public void setUp() throws SSLException {
        istioConfig = new IstioConfig();
        istioConfig.setIstioAddress("localhost:15010");
        // istioConfig.setNamespace("envoy-managed");
        channelManager = new GrpcChannelManager(istioConfig);
        channelManager.resetChannelIfNecessary();
        ldsService = new LDSService(istioConfig, channelManager);
        rdsService = new RDSService(istioConfig, channelManager);
        edsService = new EDSService(istioConfig, channelManager);
        cdsService = new CDSService(istioConfig, channelManager);
        ldsService.start();
        rdsService.start();
        edsService.start();
        cdsService.start();
    }

    @Test
    public void testSubscribeListeners() {
        List<Listener> listeners = ldsService.subscribeListeners();

        for (Listener listener : listeners) {
            List<String> rdsNames = LDSService.getRdsNames(Collections.singletonList(listener));
            if (rdsNames == null || rdsNames.isEmpty()) {
                continue;
            }
            System.out.println("--------------------------------");
            System.out.println("Listener: " + listener.getName() + " " + listener.getAddress().getSocketAddress().getAddress() + ":" + listener.getAddress().getSocketAddress().getPortValue());
            
            System.out.println("Rds Name: " + rdsNames.get(0));
            List<RouteConfiguration> routes = rdsService.subscribeRoutes(rdsNames);
            if (routes == null || routes.isEmpty()) {
                System.out.println("No routes found for listener: " + listener.getName());
                continue;
            }
            for (RouteConfiguration route : routes) {
                System.out.println("Route: " + route.getName());
                route.getVirtualHostsList().forEach(virtualHost -> {
                    System.out.println("VirtualHost: " + virtualHost);
                    
                });
            }
            
        }
        
    }

    @Test
    public void testSubscribeEndpoints() {

        List<String> clusterNames = Collections.singletonList("outbound|80|unit2|joylive-demo-kubernetes-provider.envoy-managed.svc.cluster.local");
        List<ClusterLoadAssignment> endpoints = edsService.subscribeEndpoints(clusterNames);
        for (ClusterLoadAssignment endpoint : endpoints) {
            System.out.println(endpoint);
        }
    }

    @Test
    public void testSubscribeRoutes() {
        List<RouteConfiguration> routeConfigs = rdsService.subscribeRoutes(Collections.singletonList("joylive-demo-kubernetes-provider.envoy-managed.svc.cluster.local:80"));
    }

    @Test
    public void testAnalyzeRequest() {
        // 1. 首先获取所有Listener
        List<Listener> listeners = ldsService.subscribeListeners();
        
        String targetHost = "joylive-demo-kubernetes-provider.envoy-managed.svc.cluster.local";
        
        for (Listener listener : listeners) {
            List<String> rdsNames = LDSService.getRdsNames(Collections.singletonList(listener));
            if (rdsNames == null || rdsNames.isEmpty()) {
                continue;
            }
            
            System.out.println("\n分析 Listener: " + listener.getName());
            System.out.println("地址: " + listener.getAddress().getSocketAddress().getAddress() + 
                             ":" + listener.getAddress().getSocketAddress().getPortValue());
            System.out.println("RDS名称: " + rdsNames.get(0));
            
            // 2. 获取该Listener对应的RouteConfiguration
            List<RouteConfiguration> routes = rdsService.subscribeRoutes(rdsNames);
            if (routes == null || routes.isEmpty()) {
                continue;
            }
            
            // 3. 遍历RouteConfiguration中的VirtualHost
            for (RouteConfiguration routeConfig : routes) {
                System.out.println("\n检查RouteConfiguration: " + routeConfig.getName());
                
                for (VirtualHost virtualHost : routeConfig.getVirtualHostsList()) {
                    System.out.println("\nVirtualHost名称: " + virtualHost.getName());
                    System.out.println("域名列表: " + virtualHost.getDomainsList());
                    
                    // 4. 检查域名是否匹配
                    if (virtualHost.getDomainsList().contains(targetHost)) {
                        System.out.println("找到匹配的VirtualHost!");
                        // 5. 打印路由规则
                        for (Route route : virtualHost.getRoutesList()) {
                            System.out.println("路由规则: " + route.getMatch().getPath() + 
                                             " -> " + route.getRoute().getCluster());
                        }
                    }
                }
            }
        }
    }

    @AfterEach
    public void tearDown() throws Exception {
        channelManager.close();
    }
} 

package com.jd.live.agent.implement.service.policy.istio.cache;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.client.ServiceInstance;

import com.jd.live.agent.bootstrap.logger.LoggerFactory;
import com.jd.live.agent.implement.logger.slf4j.SLF4JBridge;
import com.jd.live.agent.xds.config.IstioConfig;
@Disabled
public class ServiceInstanceListCacheTest {
        private ServiceInstanceListCache serviceInstanceListCache;

    @BeforeEach
    void setUp() {
        LoggerFactory.setBridge(new SLF4JBridge());
        IstioConfig istioConfig = new IstioConfig();
        istioConfig.setIstioAddress("localhost:15010");
        serviceInstanceListCache = new ServiceInstanceListCache(istioConfig);
    }

    @Test
    void testGetResource() {
      List<ServiceInstance> instanceList = serviceInstanceListCache.get("joylive-demo-kubernetes-provider.envoy-managed");
      Assertions.assertNotNull(instanceList);
    }

}

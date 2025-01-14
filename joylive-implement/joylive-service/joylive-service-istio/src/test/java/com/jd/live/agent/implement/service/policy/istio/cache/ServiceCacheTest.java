package com.jd.live.agent.implement.service.policy.istio.cache;

import com.jd.live.agent.bootstrap.logger.LoggerFactory;
import com.jd.live.agent.governance.policy.service.Service;
import com.jd.live.agent.implement.logger.slf4j.SLF4JBridge;
import com.jd.live.agent.xds.config.IstioConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
@Disabled
class ServiceCacheTest {


    private ServiceCache serviceCache;

    @BeforeEach
    void setUp() {
        LoggerFactory.setBridge(new SLF4JBridge());
        IstioConfig istioConfig = new IstioConfig();
        istioConfig.setIstioAddress("localhost:15010");
        serviceCache = new ServiceCache(istioConfig);
    }

    @Test
    void testGetResource() {
      Service service = serviceCache.get("joylive-demo-kubernetes-provider.envoy-managed");
      assertNotNull(service);
    }

}

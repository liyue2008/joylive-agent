package com.jd.live.agent.implement.service.policy.istio.xds;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.jd.live.agent.implement.service.policy.istio.config.IstioConfig;

public class XDSLocalCacheTest {

    private XDSLocalCache xdsLocalCache;

    @BeforeEach
    public void setUp() {
        IstioConfig istioConfig = new IstioConfig();
        istioConfig.setIstioAddress("localhost:15010");
        xdsLocalCache = new XDSLocalCache(istioConfig);
        xdsLocalCache.start();
    }

    @AfterEach
    public void tearDown() {
        xdsLocalCache = xdsLocalCache;
    }

    @Test
    public void testUpdate() throws InterruptedException {
        Thread.sleep(1000000);
    }

}

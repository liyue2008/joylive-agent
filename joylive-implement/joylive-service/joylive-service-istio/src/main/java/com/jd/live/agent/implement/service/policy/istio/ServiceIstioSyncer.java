package com.jd.live.agent.implement.service.policy.istio;

import com.jd.live.agent.core.config.SyncConfig;
import com.jd.live.agent.core.extension.annotation.ConditionalOnProperty;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.inject.annotation.Config;
import com.jd.live.agent.core.inject.annotation.Injectable;
import com.jd.live.agent.governance.config.GovernanceConfig;
import com.jd.live.agent.governance.policy.PolicySubscriber;
import com.jd.live.agent.governance.policy.listener.ServiceEvent;
import com.jd.live.agent.governance.policy.service.MergePolicy;
import com.jd.live.agent.governance.policy.service.Service;
import com.jd.live.agent.governance.service.sync.AbstractServiceSyncer;
import com.jd.live.agent.governance.service.sync.SyncKey;
import com.jd.live.agent.governance.service.sync.SyncResponse;
import com.jd.live.agent.governance.service.sync.SyncStatus;
import com.jd.live.agent.governance.service.sync.SyncKey.ServiceKey;
import com.jd.live.agent.governance.service.sync.Syncer;
import com.jd.live.agent.implement.service.policy.istio.cache.ServiceCache;
import com.jd.live.agent.implement.service.policy.istio.config.IstioSyncConfig;
import java.util.concurrent.CompletableFuture;

import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;


@Injectable
@Extension("ServiceIstioSyncer")
@ConditionalOnProperty(name = SyncConfig.SYNC_MICROSERVICE_TYPE, value = "istio")
@ConditionalOnProperty(name = GovernanceConfig.CONFIG_FLOW_CONTROL_ENABLED, matchIfMissing = true)
public class ServiceIstioSyncer extends AbstractServiceSyncer<SyncKey.ServiceKey> {


    private final static Logger logger = LoggerFactory.getLogger(ServiceIstioSyncer.class);

    private static final String NAME = "service-istio-syncer";

    private ServiceCache serviceCache;
    public ServiceIstioSyncer() {
        name = NAME;
    }

    @Override
    protected CompletableFuture<Void> doStart() {
        try {
            serviceCache = new ServiceCache(syncConfig.getIstio());
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
        return super.doStart();
    }

    @Config(SyncConfig.SYNC_MICROSERVICE)
    private IstioSyncConfig syncConfig = new IstioSyncConfig();

    @Override
    protected Syncer<ServiceKey, Service> createSyncer() {
        return subscription -> {
            ServiceKey key = subscription.getKey();
            String serviceName = key.getName();
            SyncResponse<Service> response = getService(serviceName);
            subscription.onUpdate(response);
        };
    }

    private SyncResponse<Service> getService(String serviceName) {

        Service service = serviceCache.get(serviceName);
        if (null != service) {

            return new SyncResponse<>(SyncStatus.SUCCESS, service);
        } else {
            return new SyncResponse<>(SyncStatus.NOT_FOUND, null);
        }
    }

    @Override
    protected ServiceKey createServiceKey(PolicySubscriber subscriber) {
        return new ServiceKey(subscriber);
    }

    @Override
    protected void configure(ServiceEvent event) {
        event.setMergePolicy(MergePolicy.FLOW_CONTROL);
    }

    @Override
    protected SyncConfig getSyncConfig() {
        return syncConfig;
    }


}
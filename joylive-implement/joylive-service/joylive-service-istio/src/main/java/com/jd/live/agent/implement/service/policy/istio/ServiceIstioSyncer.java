package com.jd.live.agent.implement.service.policy.istio;

import com.jd.live.agent.core.config.SyncConfig;
import com.jd.live.agent.core.extension.annotation.ConditionalOnProperty;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.inject.annotation.Injectable;
import com.jd.live.agent.governance.config.GovernanceConfig;
import com.jd.live.agent.governance.policy.PolicySubscriber;
import com.jd.live.agent.governance.policy.listener.ServiceEvent;
import com.jd.live.agent.governance.policy.service.Service;
import com.jd.live.agent.governance.service.sync.AbstractServiceSyncer;
import com.jd.live.agent.governance.service.sync.SyncKey;
import com.jd.live.agent.governance.service.sync.Syncer;

@Injectable
@Extension("ServiceIstioSyncer")
@ConditionalOnProperty(name = SyncConfig.SYNC_MICROSERVICE_TYPE, value = "istio")
@ConditionalOnProperty(name = GovernanceConfig.CONFIG_FLOW_CONTROL_ENABLED, matchIfMissing = true)
public class ServiceIstioSyncer extends AbstractServiceSyncer<SyncKey.ServiceKey> {
    @Override
    protected SyncConfig getSyncConfig() {
        return null;
    }

    @Override
    protected Syncer<SyncKey.ServiceKey, Service> createSyncer() {
        return null;
    }

    @Override
    protected SyncKey.ServiceKey createServiceKey(PolicySubscriber subscriber) {
        return null;
    }

    @Override
    protected void configure(ServiceEvent event) {

    }
}

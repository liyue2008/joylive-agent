package com.jd.live.agent.implement.service.policy.istio;

import com.jd.live.agent.core.config.ConfigWatcher;
import com.jd.live.agent.core.config.SyncConfig;
import com.jd.live.agent.core.extension.annotation.ConditionalOnProperty;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.inject.annotation.Config;
import com.jd.live.agent.core.inject.annotation.Injectable;
import com.jd.live.agent.governance.config.GovernanceConfig;
import com.jd.live.agent.governance.policy.service.Service;
import com.jd.live.agent.governance.policy.service.ServiceGroup;
import com.jd.live.agent.governance.policy.service.ServicePolicy;
import com.jd.live.agent.governance.policy.service.ServiceType;
import com.jd.live.agent.governance.policy.service.route.RoutePolicy;
import com.jd.live.agent.governance.rule.OpType;
import com.jd.live.agent.governance.rule.tag.TagCondition;
import com.jd.live.agent.governance.rule.tag.TagDestination;
import com.jd.live.agent.governance.rule.tag.TagRule;
import com.jd.live.agent.governance.service.sync.AbstractSyncer;
import com.jd.live.agent.governance.service.sync.Subscription;
import com.jd.live.agent.governance.service.sync.SyncKey;
import com.jd.live.agent.governance.service.sync.SyncKey.ServiceKey;
import com.jd.live.agent.governance.service.sync.Syncer;
import com.jd.live.agent.implement.service.policy.istio.config.IstioSyncConfig;

import io.envoyproxy.envoy.config.route.v3.Route;
import io.envoyproxy.envoy.config.route.v3.RouteMatch;
import io.envoyproxy.envoy.config.route.v3.VirtualHost;
import io.envoyproxy.envoy.type.matcher.v3.StringMatcher;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;


@Injectable
@Extension("ServiceIstioSyncer")
@ConditionalOnProperty(name = SyncConfig.SYNC_MICROSERVICE_TYPE, value = "istio")
@ConditionalOnProperty(name = GovernanceConfig.CONFIG_FLOW_CONTROL_ENABLED, matchIfMissing = true)
public class ServiceIstioSyncer extends AbstractSyncer<SyncKey.ServiceKey, Service> {


    private final static Logger logger = LoggerFactory.getLogger(ServiceIstioSyncer.class);

    public ServiceIstioSyncer() {
        name = "service-istio-syncer";
    }

    @Config(SyncConfig.SYNC_MICROSERVICE)
    private IstioSyncConfig syncConfig;

    @Override
    public String getType() {
        return ConfigWatcher.TYPE_SERVICE_SPACE;
    }

    @Override
    protected SyncConfig getSyncConfig() {
        return this.syncConfig;
    }

    @Override
    protected Syncer<ServiceKey, Service> createSyncer() {
        return new Syncer<ServiceKey,Service>() {

            @Override
            public void sync(Subscription<ServiceKey, Service> subscription) {
                ServiceKey key = subscription.getKey();
                subscription.onUpdate(null);
                
                // 这里获取并构建Service                
            }
            
        };
    }

    @Override
    protected void startSync() throws Exception {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'startSync'");
    }


}
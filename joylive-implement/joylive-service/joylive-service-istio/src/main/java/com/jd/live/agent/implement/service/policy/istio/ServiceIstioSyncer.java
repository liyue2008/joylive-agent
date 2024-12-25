package com.jd.live.agent.implement.service.policy.istio;

import com.jd.live.agent.core.config.ConfigWatcher;
import com.jd.live.agent.core.config.SyncConfig;
import com.jd.live.agent.core.extension.annotation.ConditionalOnProperty;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.inject.annotation.Config;
import com.jd.live.agent.core.inject.annotation.Injectable;
import com.jd.live.agent.governance.config.GovernanceConfig;
import com.jd.live.agent.governance.policy.service.Service;
import com.jd.live.agent.governance.service.sync.AbstractSyncer;
import com.jd.live.agent.governance.service.sync.Subscription;
import com.jd.live.agent.governance.service.sync.SyncKey;
import com.jd.live.agent.governance.service.sync.SyncResponse;
import com.jd.live.agent.governance.service.sync.SyncStatus;
import com.jd.live.agent.governance.service.sync.SyncKey.ServiceKey;
import com.jd.live.agent.governance.service.sync.Syncer;
import com.jd.live.agent.implement.service.policy.istio.config.IstioSyncConfig;
import com.jd.live.agent.implement.service.policy.istio.xds.XDSConvert;
import com.jd.live.agent.implement.service.policy.istio.xds.XDSLocalCache;

import io.envoyproxy.envoy.config.route.v3.VirtualHost;

import java.util.List;

import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;


@Injectable
@Extension("ServiceIstioSyncer")
@ConditionalOnProperty(name = SyncConfig.SYNC_MICROSERVICE_TYPE, value = "istio")
@ConditionalOnProperty(name = GovernanceConfig.CONFIG_FLOW_CONTROL_ENABLED, matchIfMissing = true)
public class ServiceIstioSyncer extends AbstractSyncer<SyncKey.ServiceKey, Service> {


    private final static Logger logger = LoggerFactory.getLogger(ServiceIstioSyncer.class);

    private XDSLocalCache xdsLocalCache;

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
                String domain = key.getName();
                VirtualHost virtualHost = xdsLocalCache.getVirtualHosts(domain);
                if (null != virtualHost) {
                    List<Service> services = XDSConvert.virtualHostToServices(virtualHost);
                    Service service  = services.stream().filter(s -> s.getName().equals(domain)).findFirst().orElse(null);
                    subscription.onUpdate(new SyncResponse<Service>(SyncStatus.SUCCESS, service));
                } else {
                    subscription.onUpdate(new SyncResponse<Service>(SyncStatus.NOT_FOUND, null, "virtualHost is null", null));
                }             
            }  
        };
    }

    @Override
    protected void startSync() throws Exception {
        this.xdsLocalCache = new XDSLocalCache(syncConfig.getIstio());
        this.xdsLocalCache.start();
    }

    @Override
    protected void stopSync() {
        try {
            this.xdsLocalCache.close();
        } catch (Exception e) {
            logger.error("Error closing XDSLocalCache", e);
        }
        super.stopSync();
    }


}
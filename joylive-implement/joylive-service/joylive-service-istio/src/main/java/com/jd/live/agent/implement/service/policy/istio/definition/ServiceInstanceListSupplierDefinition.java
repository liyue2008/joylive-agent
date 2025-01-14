package com.jd.live.agent.implement.service.policy.istio.definition;

import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;
import com.jd.live.agent.core.bytekit.matcher.MatcherBuilder;
import com.jd.live.agent.core.config.SyncConfig;
import com.jd.live.agent.core.extension.annotation.ConditionalOnProperty;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.inject.annotation.Config;
import com.jd.live.agent.core.inject.annotation.Inject;
import com.jd.live.agent.core.inject.annotation.Injectable;
import com.jd.live.agent.core.plugin.definition.InterceptorDefinition;
import com.jd.live.agent.core.plugin.definition.InterceptorDefinitionAdapter;
import com.jd.live.agent.core.plugin.definition.PluginDefinitionAdapter;
import com.jd.live.agent.governance.config.GovernanceConfig;
import com.jd.live.agent.governance.invoke.InvocationContext;
import com.jd.live.agent.implement.service.policy.istio.config.IstioSyncConfig;
import com.jd.live.agent.implement.service.policy.istio.interceptor.ServiceInstanceListSupplierInterceptor;

@Injectable
@Extension(value = "ServiceInstanceListSupplierPluginDefinitionIstio")
@ConditionalOnProperty(name = SyncConfig.SYNC_MICROSERVICE_TYPE, value = "istio")
@ConditionalOnProperty(name = GovernanceConfig.CONFIG_FLOW_CONTROL_ENABLED, matchIfMissing = true)
public class ServiceInstanceListSupplierDefinition extends PluginDefinitionAdapter {

    private static final Logger logger = LoggerFactory.getLogger(ServiceInstanceListSupplierDefinition.class);

    protected static final String TYPE_SERVICE_INSTANCE_LIST_SUPPLIER = "org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier";

    private static final String METHOD_GET = "get";

    private static final String[] ARGUMENTS_GET = new String[]{
            "org.springframework.cloud.client.loadbalancer.Request"
    };

    @Inject(InvocationContext.COMPONENT_INVOCATION_CONTEXT)
    private InvocationContext context;

    @Config(SyncConfig.SYNC_MICROSERVICE)
    private IstioSyncConfig syncConfig = new IstioSyncConfig();

    public ServiceInstanceListSupplierDefinition() {

        this.matcher = () -> MatcherBuilder.isSubTypeOf(TYPE_SERVICE_INSTANCE_LIST_SUPPLIER);
        this.interceptors = new InterceptorDefinition[]{
                new InterceptorDefinitionAdapter(
                        MatcherBuilder.named(METHOD_GET).
                                and(MatcherBuilder.arguments(ARGUMENTS_GET)),
                        () -> new ServiceInstanceListSupplierInterceptor(context, syncConfig.getIstio())
                )
        };
    }

}

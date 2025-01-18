package com.jd.live.agent.implement.service.policy.istio.interceptor;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;

import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.bootstrap.bytekit.context.MethodContext;
import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;
import com.jd.live.agent.core.plugin.definition.InterceptorAdaptor;
import com.jd.live.agent.governance.invoke.InvocationContext;
import com.jd.live.agent.implement.service.policy.istio.cache.ServiceInstanceListCache;
import com.jd.live.agent.xds.config.IstioConfig;

import reactor.core.publisher.Flux;

public class ServiceInstanceListSupplierInterceptor extends InterceptorAdaptor {

    private static final Logger logger = LoggerFactory.getLogger(ServiceInstanceListSupplierInterceptor.class);
    private final InvocationContext context;
    private final ServiceInstanceListCache serviceInstanceListCache;

    public ServiceInstanceListSupplierInterceptor(InvocationContext context, IstioConfig istioConfig) {
        this.context = context;
        this.serviceInstanceListCache = new ServiceInstanceListCache(istioConfig);
    }

    @Override
    public void onEnter(ExecutableContext ctx) {
        MethodContext mc = (MethodContext) ctx;
        ServiceInstanceListSupplier target = (ServiceInstanceListSupplier) ctx.getTarget();
        String targetServiceName = target.getServiceId();

        if (serviceInstanceListCache.exists(targetServiceName)) {
            List<ServiceInstance> instances = getInstances(targetServiceName);
            logger.info("{} instances from cache: {}.", targetServiceName,
                instances.stream().map(instance -> instance.getHost() + ":" + instance.getPort() + "/" + instance.getMetadata().get("subset")).collect(Collectors.toList()));
            mc.skipWithResult(Flux.just());
        }
    }

    private List<ServiceInstance> getInstances(String serviceName) {
        return serviceInstanceListCache.get(serviceName);
    }
}

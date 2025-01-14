package com.jd.live.agent.implement.service.policy.istio.interceptor;

import java.util.List;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;

import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.bootstrap.bytekit.context.MethodContext;
import com.jd.live.agent.core.plugin.definition.InterceptorAdaptor;
import com.jd.live.agent.governance.invoke.InvocationContext;
import com.jd.live.agent.implement.service.policy.istio.cache.ServiceInstanceListCache;
import com.jd.live.agent.xds.config.IstioConfig;

import reactor.core.publisher.Flux;

public class ServiceInstanceListSupplierInterceptor extends InterceptorAdaptor {

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
            mc.skipWithResult(Flux.just(getInstances(targetServiceName)));
        }
    }

    private List<ServiceInstance> getInstances(String serviceName) {
        return serviceInstanceListCache.get(serviceName);
    }
}

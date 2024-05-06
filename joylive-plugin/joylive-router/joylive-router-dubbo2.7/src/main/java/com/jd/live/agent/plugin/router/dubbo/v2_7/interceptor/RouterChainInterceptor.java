/*
 * Copyright © ${year} ${owner} (${email})
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jd.live.agent.plugin.router.dubbo.v2_7.interceptor;

import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.bootstrap.bytekit.context.MethodContext;
import com.jd.live.agent.bootstrap.exception.RejectException;
import com.jd.live.agent.governance.interceptor.AbstractInterceptor.AbstractRouteInterceptor;
import com.jd.live.agent.governance.invoke.InvocationContext;
import com.jd.live.agent.governance.invoke.filter.RouteFilter;
import com.jd.live.agent.governance.invoke.filter.RouteFilterChain;
import com.jd.live.agent.plugin.router.dubbo.v2_7.instance.DubboEndpoint;
import com.jd.live.agent.plugin.router.dubbo.v2_7.request.DubboRequest.DubboOutboundRequest;
import com.jd.live.agent.plugin.router.dubbo.v2_7.request.invoke.DubboInvocation.DubboOutboundInvocation;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.cluster.RouterChain;

import java.util.List;
import java.util.stream.Collectors;

/**
 * RouterChainInterceptor
 */
public class RouterChainInterceptor extends AbstractRouteInterceptor<DubboOutboundRequest, DubboOutboundInvocation> {

    public RouterChainInterceptor(InvocationContext context, List<RouteFilter> filters) {
        super(context, filters);
    }

    /**
     * Enhanced logic after method execution<br>
     * <p>
     *
     * @param ctx ExecutableContext
     * @see RouterChain#route(URL, Invocation)
     */
    @SuppressWarnings("unchecked")
    @Override
    public void onSuccess(ExecutableContext ctx) {
        MethodContext mc = (MethodContext) ctx;
        Object[] arguments = mc.getArguments();
        Invocation invocation = (Invocation) arguments[1];
        List<Invoker<?>> invokers = (List<Invoker<?>>) mc.getResult();
        List<DubboEndpoint<?>> instances = invokers.stream().map(DubboEndpoint::new).collect(Collectors.toList());
        DubboOutboundRequest request = new DubboOutboundRequest(invocation);
        try {
            DubboOutboundInvocation outboundInvocation = routing(request, instances);
            List<DubboEndpoint<?>> endpoints = (List<DubboEndpoint<?>>) outboundInvocation.getEndpoints();
            mc.setResult(endpoints.stream().map(DubboEndpoint::getInvoker).collect(Collectors.toList()));
        } catch (RejectException e) {
            mc.setThrowable(new RpcException(RpcException.FORBIDDEN_EXCEPTION, e.getMessage()));
        }
    }

    @Override
    protected void routing(DubboOutboundInvocation invocation) {
        new RouteFilterChain.Chain(routeFilters).filter(invocation);
    }

    @Override
    protected DubboOutboundInvocation createOutlet(DubboOutboundRequest request) {
        return new DubboOutboundInvocation(request, context);
    }
}
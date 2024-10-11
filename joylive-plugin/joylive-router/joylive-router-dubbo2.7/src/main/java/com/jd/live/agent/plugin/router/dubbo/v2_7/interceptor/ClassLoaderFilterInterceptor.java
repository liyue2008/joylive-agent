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
import com.jd.live.agent.core.plugin.definition.InterceptorAdaptor;
import com.jd.live.agent.governance.invoke.InvocationContext;
import com.jd.live.agent.plugin.router.dubbo.v2_7.request.DubboRequest.DubboInboundRequest;
import com.jd.live.agent.plugin.router.dubbo.v2_7.request.invoke.DubboInvocation.DubboInboundInvocation;
import org.apache.dubbo.rpc.AppResponse;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.filter.ClassLoaderFilter;

import static org.apache.dubbo.rpc.cluster.support.Dubbo27InboundThrower.THROWER;

/**
 * ClassLoaderFilterInterceptor
 */
public class ClassLoaderFilterInterceptor extends InterceptorAdaptor {

    private final InvocationContext context;

    public ClassLoaderFilterInterceptor(InvocationContext context) {
        this.context = context;
    }

    /**
     * Enhanced logic before method execution<br>
     * <p>
     *
     * @param ctx ExecutableContext
     * @see ClassLoaderFilter#invoke(Invoker, Invocation)
     */
    @Override
    public void onEnter(ExecutableContext ctx) {
        MethodContext mc = (MethodContext) ctx;
        Object[] arguments = mc.getArguments();
        Invocation invocation = (Invocation) arguments[1];
        DubboInboundRequest request = new DubboInboundRequest(invocation);
        if (!request.isSystem()) {
            try {
                context.inbound(new DubboInboundInvocation(request, context));
            } catch (Throwable e) {
                Result result = new AppResponse(THROWER.createException(e, request));
                mc.setResult(result);
                mc.setSkip(true);
            }
        }
    }
}

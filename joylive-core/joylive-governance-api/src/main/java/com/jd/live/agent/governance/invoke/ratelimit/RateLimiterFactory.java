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
package com.jd.live.agent.governance.invoke.ratelimit;

import com.jd.live.agent.core.extension.annotation.Extensible;
import com.jd.live.agent.governance.policy.PolicySupplier;
import com.jd.live.agent.governance.policy.service.Service;
import com.jd.live.agent.governance.policy.service.limit.RateLimitPolicy;

import java.util.function.Function;

/**
 * A factory interface for creating {@link RateLimiter} instances.
 * Implementations of this interface are responsible for providing
 * rate limiters based on the given policy and policy supplier.
 */
@Extensible("RateLimiterFactory")
public interface RateLimiterFactory {

    /**
     * Retrieves a {@link RateLimiter} based on the provided {@link RateLimitPolicy}
     * and {@link PolicySupplier}.
     *
     * @param policy         The rate limit policy that defines the rate limiting rules.
     * @param serviceFunc    A function that provides service.
     * @return A {@link RateLimiter} instance configured according to the provided policy.
     */
    RateLimiter get(RateLimitPolicy policy, Function<String, Service> serviceFunc);
}


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
package com.jd.live.agent.governance.invoke.circuitbreak;

import com.jd.live.agent.governance.policy.service.circuitbreaker.CircuitBreakerPolicy;

import java.util.concurrent.TimeUnit;

/**
 * CircuitBreaker
 *
 * @since 1.1.0
 */
public interface CircuitBreaker {

    /**
     * Try to get a permit return the result
     *
     * @return permission
     */
    default boolean acquire() {
        return true;
    }

    /**
     * Records a failed call. This method must be invoked when a call failed.
     *
     * @param duration     The elapsed time duration of the call
     * @param durationUnit The duration unit
     * @param throwable    The throwable which must be recorded
     */
    void onError(long duration, TimeUnit durationUnit, Throwable throwable);

    /**
     * Records a successful call. This method must be invoked when a call was
     * successful.
     *
     * @param duration     The elapsed time duration of the call
     * @param durationUnit The duration unit
     */
    void onSuccess(long duration, TimeUnit durationUnit);

    /**
     * This method must be invoked when a call returned a result
     * and the result predicate should decide if the call was successful or not.
     *
     * @param duration     The elapsed time duration of the call
     * @param durationUnit The duration unit
     * @param result       The result of the protected function
     */
    void onResult(long duration, TimeUnit durationUnit, Object result);

    /**
     * Register a listener to watch state change event.
     *
     * @param listener State change listener
     */
    void registerListener(CircuitBreakerStateListener listener);

    /**
     * Get circuit-breaker policy
     *
     * @return policy
     */
    CircuitBreakerPolicy getPolicy();
}
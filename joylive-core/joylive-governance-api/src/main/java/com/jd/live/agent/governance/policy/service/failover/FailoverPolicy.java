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
package com.jd.live.agent.governance.policy.service.failover;

import com.jd.live.agent.governance.policy.PolicyInherit.PolicyInheritWithId;
import com.jd.live.agent.governance.policy.service.annotation.Consumer;
import lombok.Getter;
import lombok.Setter;

/**
 * Defines a failover policy that specifies the behavior of a system or component in the event of a failure.
 * This includes how many retry attempts should be made and the timeout for each attempt before considering
 * the operation as failed. The policy is designed to improve system resilience by allowing temporary failures
 * to be retried, potentially recovering from transient issues without affecting the overall system availability.
 * <p>
 * The {@code FailoverPolicy} class implements {@link PolicyInheritWithId} interface, enabling it to inherit
 * or supplement its configuration from another failover policy. This feature is useful for dynamically adjusting
 * policy parameters in response to changes in system behavior or requirements.
 * </p>
 *
 * @since 1.0.0
 */
@Getter
@Setter
@Consumer
public class FailoverPolicy implements PolicyInheritWithId<FailoverPolicy> {

    /**
     * The unique identifier of the failover policy. This ID can be used to reference and manage the policy
     * within a system.
     */
    private Long id;

    /**
     * The number of retry attempts that should be made in case of a failure. This parameter allows the system
     * to attempt to recover from transient failures by retrying the failed operation.
     */
    private Integer retry;

    /**
     * The timeout in milliseconds for each retry attempt. This parameter specifies how long the system should
     * wait for an operation to complete before timing out and potentially retrying the operation, according
     * to the retry policy.
     */
    private Integer timeoutInMilliseconds;

    @Override
    public void supplement(FailoverPolicy source) {
        if (source == null) {
            return;
        }
        if (retry == null) {
            retry = source.retry;
        }
        if (timeoutInMilliseconds == null) {
            timeoutInMilliseconds = source.timeoutInMilliseconds;
        }
    }
}
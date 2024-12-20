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
package com.jd.live.agent.governance.util;

import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;
import com.jd.live.agent.governance.exception.ErrorCause;
import com.jd.live.agent.governance.exception.ErrorPolicy;
import com.jd.live.agent.governance.exception.ErrorPredicate;
import com.jd.live.agent.governance.policy.service.exception.CodeParser;
import com.jd.live.agent.governance.policy.service.exception.CodePolicy;
import com.jd.live.agent.governance.request.ServiceRequest.OutboundRequest;
import com.jd.live.agent.governance.response.ServiceResponse;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

import static com.jd.live.agent.governance.exception.ErrorCause.cause;

/**
 * Utility class for Predicates.
 */
public class Predicates {

    private static final Logger logger = LoggerFactory.getLogger(Predicates.class);

    private static final String DUBBO_APACHE_METADATA_SERVICE = "org.apache.dubbo.metadata.MetadataService";

    private static final String DUBBO_APACHE_REGISTRY_SERVICE = "org.apache.dubbo.registry.RegistryService";

    private static final String DUBBO_APACHE_MONITOR_SERVICE = "org.apache.dubbo.monitor.MonitorService";

    private static final String DUBBO_ALIBABA_REGISTRY_SERVICE = "com.alibaba.dubbo.registry.RegistryService";

    private static final String DUBBO_ALIBABA_MONITOR_SERVICE = "com.alibaba.dubbo.monitor.MonitorService";

    private static final Set<String> DUBBO_SYSTEM_SERVICES = new HashSet<>(Arrays.asList(
            DUBBO_APACHE_METADATA_SERVICE, DUBBO_APACHE_REGISTRY_SERVICE, DUBBO_APACHE_MONITOR_SERVICE,
            DUBBO_ALIBABA_REGISTRY_SERVICE, DUBBO_ALIBABA_MONITOR_SERVICE));

    /**
     * Checks if the given service name is a system service for Dubbo.
     *
     * @param serviceName the name of the service to check.
     * @return true if the service name is a system service for Dubbo, false otherwise.
     */
    public static boolean isDubboSystemService(String serviceName) {
        return serviceName != null && DUBBO_SYSTEM_SERVICES.contains(serviceName);
    }

    /**
     * Checks if the given ServiceResponse matches the specified ErrorPolicy.
     *
     * @param policy    the ErrorPolicy to check against
     * @param request   the OutboundRequest associated with the ServiceResponse
     * @param response  the ServiceResponse to check
     * @param predicate the ErrorPredicate to use for error cause matching
     * @param factory   the Function to use for parsing error codes
     * @return true if the ServiceResponse matches the ErrorPolicy, false otherwise
     */
    public static boolean isError(ErrorPolicy policy,
                                  OutboundRequest request,
                                  ServiceResponse response,
                                  ErrorPredicate predicate,
                                  Function<String, CodeParser> factory) {
        if (policy.containsErrorCode(response.getCode())) {
            return true;
        }
        ErrorCause cause = cause(response.getError(), request.getErrorFunction(), predicate);
        if (cause != null) {
            if (cause.match(policy)) {
                return true;
            } else if (cause.getCause() != null) {
                // throw exception
                return false;
            }
        }
        if (response.match(policy)) {
            String code = parseCode(policy.getCodePolicy(), response.getResult(), factory);
            return policy.containsErrorCode(code);
        }
        return false;
    }

    /**
     * Parses the error code from the given result object using the specified code policy.
     *
     * @param policy  the code policy to use for parsing
     * @param result  the result object to parse
     * @param factory the code parser factory
     * @return the parsed error code, or null if no code could be parsed
     */
    private static String parseCode(CodePolicy policy, Object result, Function<String, CodeParser> factory) {
        String code = null;
        if (policy != null && result != null) {
            String errorParser = policy.getParser();
            String errorExpression = policy.getExpression();
            if (errorParser != null && !errorParser.isEmpty() && errorExpression != null && !errorExpression.isEmpty()) {
                CodeParser parser = factory.apply(errorParser);
                if (parser != null) {
                    try {
                        code = parser.getCode(errorExpression, result);
                    } catch (Throwable e) {
                        logger.error(e.getMessage(), e);
                    }
                }
            }
        }
        return code;
    }

}

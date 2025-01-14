package com.jd.live.agent.xds;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;

import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;
import com.jd.live.agent.governance.policy.service.Service;
import com.jd.live.agent.governance.policy.service.ServiceGroup;
import com.jd.live.agent.governance.policy.service.ServicePolicy;
import com.jd.live.agent.governance.policy.service.ServiceType;
import com.jd.live.agent.governance.policy.service.route.RoutePolicy;
import com.jd.live.agent.governance.rule.OpType;
import com.jd.live.agent.governance.rule.tag.TagCondition;
import com.jd.live.agent.governance.rule.tag.TagDestination;
import com.jd.live.agent.governance.rule.tag.TagRule;

import io.envoyproxy.envoy.config.endpoint.v3.LocalityLbEndpoints;
import io.envoyproxy.envoy.config.route.v3.Route;
import io.envoyproxy.envoy.config.route.v3.RouteAction;
import io.envoyproxy.envoy.config.route.v3.RouteMatch;
import io.envoyproxy.envoy.config.route.v3.VirtualHost;
import io.envoyproxy.envoy.type.matcher.v3.StringMatcher;

public class XDSConvert {

    private static final Logger logger = LoggerFactory.getLogger(XDSConvert.class);


    public static List<String> getClusterNamesFromVirtualHost(VirtualHost virtualHost) {
        return virtualHost.getRoutesList().stream()
            .map(Route::getRoute).filter(Objects::nonNull)
            .map(RouteAction::getCluster).filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    public static List<Service> virtualHostToServices(VirtualHost virtualHost) {
        List<Service> services = new ArrayList<>();
        virtualHost.getDomainsList().forEach(domain -> {
            if (domain.startsWith("*") || domain.endsWith("*")) {
                logger.warn("Ignore unsupported domain: {}", domain);
                return;
            }
            Service service = new Service(domain, ServiceType.HTTP);

            ServiceGroup serviceGroup = virtualHostToServiceGroup(virtualHost);
            service.setGroups(Collections.singletonList(serviceGroup));
            services.add(service);
        });
        return services;
    }

    public static ServiceGroup virtualHostToServiceGroup(VirtualHost virtualHost) {
        ServicePolicy servicePolicy = new ServicePolicy();
        servicePolicy.setRoutePolicies(routesToRoutePolicies(virtualHost.getRoutesList()));
        return new ServiceGroup("default", true, servicePolicy);
    }

    private static List<RoutePolicy> routesToRoutePolicies(List<Route> routes) {
        List<RoutePolicy> routePolicies = new ArrayList<>();
        for (Route route : routes) {

            TagDestination tagDestination = routeToTagDestination(route);
            List<TagCondition> tagConditions = routeMatchToTagConditions(route.getMatch());
            if (null != tagConditions && null != tagDestination) {
                RoutePolicy routePolicy = new RoutePolicy();
                routePolicy.setName(route.getName());
                routePolicy.setOrder(routes.indexOf(route));
                TagRule tagRule = new TagRule(tagConditions, Collections.singletonList(tagDestination));
                routePolicy.setTagRules(Collections.singletonList(tagRule));
                routePolicies.add(routePolicy);
            } else {
                logger.warn("Ignore unsupported route: {}", route);
            }
        }
        return routePolicies;
    }

    private static TagDestination routeToTagDestination(Route route) {
        if (route == null) {
            return null;
        }
        String destinationCluster = null;
        if (route.getRoute() != null) {
            destinationCluster = route.getRoute().getCluster();
            if (null != destinationCluster) {
              return new TagDestination(
                Collections.singletonList(new TagCondition("subset", Collections.singletonList(destinationCluster), OpType.EQUAL)), 100);
            }
        }
        return null;
    }

    private static List<TagCondition> routeMatchToTagConditions(RouteMatch match)  {
        if (match == null) {
            return null;
        }
        if (match.getHeadersList().isEmpty()) {
            return Collections.emptyList();
        }

        List<TagCondition> tagConditions = new ArrayList<>();
        match.getHeadersList().forEach(headerMatcher -> {
            TagCondition tagCondition = stringMatcherToTagCondition(headerMatcher.getName(), headerMatcher.getStringMatch(), "header");
            if (null != tagCondition) {
                tagConditions.add(tagCondition);
            } else {
                logger.warn("Ignore unsupported header matcher: {}", headerMatcher);
            }
        });

        match.getQueryParametersList().forEach(queryParameter -> {
            TagCondition tagCondition = stringMatcherToTagCondition(queryParameter.getName(), queryParameter.getStringMatch(), "query");
            if (null != tagCondition) {
                tagConditions.add(tagCondition);
            } else {
                logger.warn("Ignore unsupported query parameter: {}", queryParameter);
            }
        });
        return tagConditions;

    }

    private static TagCondition stringMatcherToTagCondition(String key, StringMatcher stringMatcher, String type) {
        if (null != stringMatcher) {
            if (null != stringMatcher.getExact()) {
                return new TagCondition(key, Collections.singletonList(stringMatcher.getExact()), OpType.EQUAL, type);
            } else if (null != stringMatcher.getPrefix()) {
                return new TagCondition(key, Collections.singletonList(stringMatcher.getPrefix()), OpType.PREFIX, type);
            } else if (null != stringMatcher.getSafeRegex()) {
                return new TagCondition(key, Collections.singletonList(stringMatcher.getSafeRegex().getRegex()), OpType.REGULAR, type);
            }
        }
        return null;
    }

    public static List<ServiceInstance> endpointsToServiceInstances(List<LocalityLbEndpoints> endpoints,
            String clusterName, String serviceName) {
        List<ServiceInstance> serviceInstances = new ArrayList<>();
        for (LocalityLbEndpoints localityLbEndpoints : endpoints) {
            serviceInstances.addAll(endpointToServiceInstances(localityLbEndpoints, clusterName, serviceName));
        }
        return serviceInstances;

    }

    public static List<ServiceInstance> endpointToServiceInstances(LocalityLbEndpoints localityLbEndpoints,
        String clusterName, String serviceName) {
        List<ServiceInstance> serviceInstances = new ArrayList<>();

        localityLbEndpoints.getLbEndpointsList().forEach(lbEndpoint -> {
            String address = lbEndpoint.getEndpoint().getAddress().getSocketAddress().getAddress();
            int port = lbEndpoint.getEndpoint().getAddress().getSocketAddress().getPortValue();
            serviceInstances.add(new DefaultServiceInstance(serviceName + "/" + address + ":" + port, serviceName, address, port, false,
                    Collections.singletonMap("subset", clusterName)));
        });
        return serviceInstances;
    }

}

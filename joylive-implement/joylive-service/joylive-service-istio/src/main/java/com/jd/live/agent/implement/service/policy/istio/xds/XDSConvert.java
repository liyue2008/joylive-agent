package com.jd.live.agent.implement.service.policy.istio.xds;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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

import io.envoyproxy.envoy.config.route.v3.Route;
import io.envoyproxy.envoy.config.route.v3.RouteMatch;
import io.envoyproxy.envoy.config.route.v3.VirtualHost;
import io.envoyproxy.envoy.type.matcher.v3.StringMatcher;

public class XDSConvert {

    private static final Logger logger = LoggerFactory.getLogger(XDSConvert.class);
    
    public List<Service> virtualHostToServices(VirtualHost virtualHost) {
        List<Service> services = new ArrayList<>();
        virtualHost.getDomainsList().forEach(domain -> {
            if (domain.startsWith("*") || domain.endsWith("*")) {
                logger.warn("Ignore unsupported domain: {}", domain);
                return;
            }
            Service service = new Service(domain, ServiceType.HTTP);

            ServicePolicy servicePolicy = new ServicePolicy();
            servicePolicy.setRoutePolicies(routesToRoutePolicies(virtualHost.getRoutesList()));
            
            ServiceGroup serviceGroup = new ServiceGroup("default", true, servicePolicy);
            service.setGroups(Collections.singletonList(serviceGroup));
            services.add(service);
        });
        return services;
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
}

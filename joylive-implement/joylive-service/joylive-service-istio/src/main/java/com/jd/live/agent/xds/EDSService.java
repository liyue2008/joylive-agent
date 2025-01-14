package com.jd.live.agent.xds;

import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;
import com.jd.live.agent.xds.config.IstioConfig;

import io.envoyproxy.envoy.config.endpoint.v3.ClusterLoadAssignment;

public class EDSService extends XDSService<ClusterLoadAssignment> {

    private final static Logger logger = LoggerFactory.getLogger(EDSService.class);

    private final static String EDS_TYPE_URL = "type.googleapis.com/envoy.config.endpoint.v3.ClusterLoadAssignment";

    public EDSService(IstioConfig istioConfig, GrpcChannelManager channelManager) {
        super(istioConfig, channelManager);
    }

    public List<ClusterLoadAssignment> subscribeEndpoints(List<String> clusterNames) {
        Future<List<ClusterLoadAssignment>> clusterLoadAssignmentFutures = subscribeResources(clusterNames);
        try {
            return clusterLoadAssignmentFutures.get(15, TimeUnit.SECONDS);
        } catch (Exception e) {
            logger.error("Error subscribing endpoints", e);
            return null;
        }
    }

    @Override
    protected String getResourceTypeUrl() {
        return EDS_TYPE_URL;
    }

    @Override
    protected Class<ClusterLoadAssignment> getResourceClass() {
        return ClusterLoadAssignment.class;
    }
}

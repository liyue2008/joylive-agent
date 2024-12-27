package com.jd.live.agent.implement.service.policy.istio.xds;

import com.jd.live.agent.implement.service.policy.istio.config.IstioConfig;
import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import io.envoyproxy.envoy.config.cluster.v3.Cluster;
public class CDSService extends XDSService<Cluster> {

    private final static Logger logger = LoggerFactory.getLogger(CDSService.class);
    private final static String CDS_TYPE_URL = "type.googleapis.com/envoy.config.cluster.v3.Cluster";

    private List<Cluster> clusters;

    public CDSService(IstioConfig istioConfig, GrpcChannelManager channelManager) {
        super(istioConfig, channelManager);
    }

    @Override
    protected Class<Cluster> getResourceClass() {
        return Cluster.class;
    }

    public List<Cluster> subscribeClusters() {
        Future<List<Cluster>> clusterFutures = subscribeResources(null);
        try {
            return clusterFutures.get(15, TimeUnit.SECONDS);
        } catch (Exception e) {
            logger.error("Error subscribing clusters", e);
            return null;
        }
    }

    public List<String> getClusterNames() {
        return clusters == null ? null : clusters.stream()
            .filter(cluster -> cluster.getType() == Cluster.DiscoveryType.EDS)
            .map(Cluster::getName).collect(Collectors.toList());
    }


    @Override
    protected String getResourceTypeUrl() {
        return CDS_TYPE_URL;
    }

    public static List<String> getClusterNames(List<Cluster> clusters) {
        return clusters.stream()
            .filter(cluster -> cluster.getType() == Cluster.DiscoveryType.EDS)
            .map(Cluster::getName).collect(Collectors.toList());
    }
}

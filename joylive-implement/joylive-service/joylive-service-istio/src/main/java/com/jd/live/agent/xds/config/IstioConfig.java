package com.jd.live.agent.xds.config;


import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class IstioConfig {

    private String istioAddress = "istio-ingressgateway.istio-system.svc.cluster.local:15012";
    private boolean sslEnabled = false;
    private String sslCertPath = "/etc/istio/certs/cert-chain.pem";
    private String clusterName = "joy-live";
    private String podName = "joy-live";
    private String namespace = "default";
    private String istioVersion = "1.21.0";
}

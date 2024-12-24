package com.jd.live.agent.implement.service.policy.istio.config;

import com.jd.live.agent.core.config.SyncConfig;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class IstioSyncConfig extends SyncConfig {

    private IstioConfig istio = new IstioConfig();
}

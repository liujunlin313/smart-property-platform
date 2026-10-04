package com.smartproperty.platform.common.logging;

import org.slf4j.MDC;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/** Deployment stays stable across a manual release; Docker supplied HOSTNAME identifies one container instance. */
@Component
public class RuntimeIdentity {
    private final String deploymentId;
    private final String instanceId;

    public RuntimeIdentity(Environment environment) {
        this.deploymentId = environment.getProperty("app.deployment-id", "D-UNTRACKED");
        this.instanceId = environment.getProperty("HOSTNAME", "instance-unknown");
    }

    public void putIntoMdc() {
        MDC.put("deploymentId", deploymentId);
        MDC.put("instanceId", instanceId);
    }

    public void clearFromMdc() {
        MDC.remove("deploymentId");
        MDC.remove("instanceId");
    }
}

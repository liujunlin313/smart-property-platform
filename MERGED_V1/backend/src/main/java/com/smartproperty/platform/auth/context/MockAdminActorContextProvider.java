package com.smartproperty.platform.auth.context;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Set;

/** Stage 1-only identity. No account row is created or changed by this provider. */
@Component
@ConditionalOnProperty(name = "app.stage1.mock-admin-enabled", havingValue = "true")
public class MockAdminActorContextProvider implements ActorContextProvider {
    private static final ActorContext MOCK_ADMIN = new ActorContext(
            null, null, "mock-super-admin", "ADMIN", Set.of("SUPER_ADMIN"), null);

    @Override
    public ActorContext currentActor() {
        return MOCK_ADMIN;
    }
}

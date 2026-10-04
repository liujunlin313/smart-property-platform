package com.smartproperty.platform.auth.context;

/** Boundary reserved for the future Spring Security SecurityContext implementation. */
public interface ActorContextProvider {
    ActorContext currentActor();
}

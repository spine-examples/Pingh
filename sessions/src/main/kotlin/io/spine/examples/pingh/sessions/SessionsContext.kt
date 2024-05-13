package io.spine.examples.pingh.sessions

import io.spine.server.BoundedContext
import io.spine.server.BoundedContextBuilder

public class SessionsContext {

    public companion object {
        public const val NAME: String = "sessions"
    }

    public fun newBuilder(): BoundedContextBuilder =
        BoundedContext.singleTenant(NAME)
            .add(UserSessionRepository())
}

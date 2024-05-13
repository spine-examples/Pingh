package io.spine.examples.pingh.sessions

import com.google.errorprone.annotations.OverridingMethodsMustInvokeSuper
import io.spine.examples.pingh.sessions.event.UserLoggedIn
import io.spine.examples.pingh.sessions.event.UserLoggedOut
import io.spine.server.procman.ProcessManagerRepository
import io.spine.server.route.EventRoute.withId
import io.spine.server.route.EventRouting

/**
 * The repository for managing [UserSessionProcess] instances.
 */
public class UserSessionRepository :
    ProcessManagerRepository<SessionId, UserSessionProcess, UserSession>() {

    @OverridingMethodsMustInvokeSuper
    override fun setupEventRouting(routing: EventRouting<SessionId>) {
        super.setupEventRouting(routing)
        routing
            .route(UserLoggedIn::class.java) { event, context ->
                withId(event.id)
            }
            .route(UserLoggedOut::class.java) { event, context ->
                withId(event.id)
            }
    }
}

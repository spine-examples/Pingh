package io.spine.examples.pingh.sessions

import io.spine.base.EventMessage
import io.spine.base.Time
import io.spine.examples.pingh.github.Username
import io.spine.testing.TestValues.randomString
import io.spine.testing.server.EventSubject
import io.spine.testing.server.blackbox.ContextAwareTest
import org.junit.jupiter.api.BeforeEach

abstract class SessionSpec : ContextAwareTest() {

    private lateinit var sessionId: SessionId

    @BeforeEach
    fun generateIdentifiers() {
        sessionId = with(SessionId.newBuilder()) {
            username = with(Username.newBuilder()) {
                value = randomString()
                vBuild()
            }
            whenCreated = Time.currentTime()
            vBuild()
        }
    }

    protected fun session(): SessionId = sessionId

    protected fun <T : EventMessage> assertEvents(eventClass: Class<T>): EventSubject =
        context().assertEvents()
            .withType(eventClass)
}

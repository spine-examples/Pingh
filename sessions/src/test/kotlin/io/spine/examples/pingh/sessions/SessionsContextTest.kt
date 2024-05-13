package io.spine.examples.pingh.sessions

import io.spine.examples.pingh.github.PersonalAccessToken
import io.spine.examples.pingh.github.Username
import io.spine.examples.pingh.sessions.command.LogUserIn
import io.spine.examples.pingh.sessions.event.UserLoggedIn
import io.spine.server.BoundedContextBuilder
import io.spine.testing.server.blackbox.ContextAwareTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("Sessions Context should")
class SessionsContextTest : ContextAwareTest() {

    override fun contextBuilder(): BoundedContextBuilder =
        SessionsContext.newBuilder()

    @Nested
    @DisplayName("handle the `LogUserIn` command")
    inner class LogUserInCommand {

        @BeforeEach
        fun sendCommand() {
            val command = LogUserIn.newBuilder()
                .setId(
                    SessionId.newBuilder()
                        .setUsername(
                            Username.newBuilder()
                                .setValue("aaa")
                                .vBuild()
                        )
                        .vBuild()
                )
                .vBuild()
            context().receivesCommand(command)
        }

        @Test
        @DisplayName("emitting 'UserLoggedIn` event")
        fun event() {
            val expected = UserLoggedIn.newBuilder()
                .setId(SessionId.newBuilder()
                    .setUsername(
                        Username.newBuilder()
                            .setValue("aaa")
                            .vBuild()
                    )
                    .vBuild())
                .setToken(PersonalAccessToken.newBuilder().setValue("").vBuild())
                .vBuild()
            context().assertEvent(expected)
        }
    }
}

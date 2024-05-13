package io.spine.examples.pingh.sessions

import com.google.protobuf.Timestamp
import io.spine.examples.pingh.github.PersonalAccessToken
import io.spine.examples.pingh.sessions.command.LogUserIn
import io.spine.examples.pingh.sessions.command.LogUserOut
import io.spine.examples.pingh.sessions.event.UserLoggedIn
import io.spine.examples.pingh.sessions.event.UserLoggedOut
import io.spine.examples.pingh.sessions.rejection.UserCannotLoggedIn
import io.spine.server.command.Assign
import io.spine.server.procman.ProcessManager
import java.time.Instant

public class UserSessionProcess :
    ProcessManager<SessionId, UserSession, UserSession.Builder>() {

    @Assign
    @Throws(UserCannotLoggedIn::class)
    public fun handle(command: LogUserIn): UserLoggedIn {

        initState(command)

        val token = ""

        return UserLoggedIn.newBuilder()
            .setId(command.id)
            .setToken(
                PersonalAccessToken.newBuilder()
                    .setValue(token)
                    .vBuild()
            )
            .vBuild()
    }

    private fun initState(command: LogUserIn) {

        val now = Instant.now()

        builder()
            .setId(command.id)
            .setWhenLoggedIn(
                Timestamp.newBuilder()
                    .setSeconds(now.epochSecond)
                    .setNanos(now.nano)
                    .build()
            )
    }

    @Assign
    public fun handle(command: LogUserOut): UserLoggedOut {

        return UserLoggedOut.newBuilder()
            .setId(command.id)
            .vBuild()
    }
}

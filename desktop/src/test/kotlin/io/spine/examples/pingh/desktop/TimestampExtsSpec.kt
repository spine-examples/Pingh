package io.spine.examples.pingh.desktop

import com.google.protobuf.util.Timestamps
import io.kotest.matchers.shouldBe
import java.time.ZoneId
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("`Timestamp` extensions should")
internal class TimestampExtsSpec {

    @Test
    internal fun `consider the time zone when converting time to a string`() {
        val time = Timestamps.parse("2024-10-09T10:00:00Z")
        val expected = "09 Oct 12:00"
        val timeZone = ZoneId.of("+02:00")
        time.toDatetime(timeZone) shouldBe expected
    }
}

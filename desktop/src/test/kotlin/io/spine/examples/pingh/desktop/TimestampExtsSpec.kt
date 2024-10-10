package io.spine.examples.pingh.desktop

import com.google.protobuf.util.Timestamps
import io.kotest.matchers.shouldBe
import java.time.ZoneId
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("`Timestamp` extensions should")
internal class TimestampExtsSpec {

    @Test
    internal fun `consider the time difference when converting UTC time to local time`() {
        val time = Timestamps.parse("2024-10-09T10:00:00Z")
        val expected = "09 Oct 13:00"
        val timeZone = ZoneId.of("+03:00")
        time.toDatetime(timeZone) shouldBe expected
    }

    @Test
    internal fun `consider the date difference when converting UTC time to local time`() {
        val time = Timestamps.parse("2024-10-09T03:12:43Z")
        val expected = "08 Oct 23:12"
        val timeZone = ZoneId.of("America/New_York")
        time.toDatetime(timeZone) shouldBe expected
    }

    @Test
    internal fun `consider the daylight saving time when converting UTC time to local time`() {
        val time = Timestamps.parse("2024-06-09T15:00:00Z")
        val expected = "09 Jun 16:00"
        val timeZone = ZoneId.of("Europe/London")
        time.toDatetime(timeZone) shouldBe expected
    }
}

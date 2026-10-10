package com.wallpapercropfixer.data.history

import com.wallpapercropfixer.domain.model.BackgroundFillMode
import com.wallpapercropfixer.domain.model.CropMode
import com.wallpapercropfixer.domain.model.WallpaperHistoryEntry
import com.wallpapercropfixer.domain.model.WallpaperTarget
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Base64

/**
 * Round-trip tests for the pure history codec. [encode]/[decode] are top-level internal
 * functions, so these run as plain JVM tests with no Android [android.content.Context].
 */
class WallpaperHistoryJsonTest {

    private fun entry(
        id: Long,
        renderedAtEpochMs: Long,
        target: WallpaperTarget,
        cropMode: CropMode,
        fillMode: BackgroundFillMode,
        filePath: String,
        widthPx: Int,
        heightPx: Int
    ) = WallpaperHistoryEntry(id, renderedAtEpochMs, target, cropMode, fillMode, filePath, widthPx, heightPx)

    private fun base64(value: String) = Base64.getEncoder().encodeToString(value.toByteArray(Charsets.UTF_8))

    // (a)
    @Test fun `empty list round-trips`() {
        assertEquals("", encode(emptyList()))
        assertEquals(emptyList<WallpaperHistoryEntry>(), decode(encode(emptyList())))
    }

    // (b)
    @Test fun `multiple entries preserve order and all fields`() {
        val entries = listOf(
            entry(1L, 1_700_000_000_000L, WallpaperTarget.HOME, CropMode.SAFE_FIT, BackgroundFillMode.BLUR, "/a/b/c.png", 1080, 2400),
            entry(2L, 1_700_000_100_000L, WallpaperTarget.LOCK, CropMode.BALANCED, BackgroundFillMode.SOLID, "/d/e/f.jpg", 720, 1280),
            entry(3L, 1_700_000_200_000L, WallpaperTarget.BOTH, CropMode.FILL, BackgroundFillMode.GRADIENT, "/g/h.png", 1440, 3200)
        )
        assertEquals(entries, decode(encode(entries)))
    }

    // (c)
    @Test fun `filePath containing separator and newline round-trips exactly`() {
        val trickyPath = "content://media/external/images/media/1|weird\nwith|both"
        val entries = listOf(
            entry(42L, 1_700_000_000_000L, WallpaperTarget.BOTH, CropMode.FILL, BackgroundFillMode.GRADIENT, trickyPath, 100, 200)
        )
        val decoded = decode(encode(entries))
        assertEquals(entries, decoded)
        assertEquals(trickyPath, decoded.single().filePath)
    }

    // (d)
    @Test fun `decode of null and empty returns empty list`() {
        assertEquals(emptyList<WallpaperHistoryEntry>(), decode(null))
        assertEquals(emptyList<WallpaperHistoryEntry>(), decode(""))
    }

    // (e)
    @Test fun `malformed line is skipped without throwing`() {
        val good = entry(1L, 10L, WallpaperTarget.HOME, CropMode.SAFE_FIT, BackgroundFillMode.BLUR, "/ok.png", 10, 20)
        val tooFewFields = "1|2|HOME|SAFE_FIT|BLUR"
        val badId = listOf("notANumber", "10", "HOME", "SAFE_FIT", "BLUR", base64("/x.png"), "10", "20").joinToString("|")
        val badBase64 = listOf("5", "10", "HOME", "SAFE_FIT", "BLUR", "!!!not-base64!!!", "10", "20").joinToString("|")
        val raw = listOf(tooFewFields, encode(listOf(good)), badId, badBase64).joinToString(separator = "\n")

        assertEquals(listOf(good), decode(raw))
    }

    // (f)
    @Test fun `unknown enum name is skipped`() {
        val good = entry(7L, 70L, WallpaperTarget.LOCK, CropMode.BALANCED, BackgroundFillMode.SOLID, "/good.png", 30, 40)
        val unknownTarget = listOf("1", "10", "FUTURE_TARGET", "SAFE_FIT", "BLUR", base64("/a.png"), "1", "2").joinToString("|")
        val unknownCrop = listOf("2", "10", "HOME", "REMOVED_MODE", "BLUR", base64("/b.png"), "1", "2").joinToString("|")
        val unknownFill = listOf("3", "10", "HOME", "SAFE_FIT", "RAINBOW", base64("/c.png"), "1", "2").joinToString("|")
        val raw = listOf(unknownTarget, encode(listOf(good)), unknownCrop, unknownFill).joinToString(separator = "\n")

        val decoded = decode(raw)
        assertEquals(listOf(good), decoded)
        assertTrue(decoded.single().target == WallpaperTarget.LOCK)
    }
}

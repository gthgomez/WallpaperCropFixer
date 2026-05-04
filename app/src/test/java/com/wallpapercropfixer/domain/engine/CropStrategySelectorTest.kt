package com.wallpapercropfixer.domain.engine

import com.wallpapercropfixer.domain.model.CropMode
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CropStrategySelectorTest {

    private val selector = CropStrategySelector()

    @Test
    fun `SAFE_FIT pads when removal exceeds threshold`() {
        assertTrue(selector.shouldUsePadding(CropMode.SAFE_FIT, 0.25f))
    }

    @Test
    fun `SAFE_FIT does not pad when removal is below threshold`() {
        assertFalse(selector.shouldUsePadding(CropMode.SAFE_FIT, 0.10f))
    }

    @Test
    fun `BALANCED pads only when removal exceeds double threshold`() {
        assertFalse(selector.shouldUsePadding(CropMode.BALANCED, 0.25f))
        assertTrue(selector.shouldUsePadding(CropMode.BALANCED, 0.45f))
    }

    @Test
    fun `FILL never pads regardless of removal fraction`() {
        assertFalse(selector.shouldUsePadding(CropMode.FILL, 0.0f))
        assertFalse(selector.shouldUsePadding(CropMode.FILL, 0.99f))
    }
}

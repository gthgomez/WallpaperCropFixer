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
    fun `SAFE_FIT pads when faces are clipped even if removal is low`() {
        assertTrue(selector.shouldUsePadding(CropMode.SAFE_FIT, 0.05f, hasClippedFaces = true))
    }

    @Test
    fun `FILL never pads regardless of removal fraction`() {
        assertFalse(selector.shouldUsePadding(CropMode.FILL, 0.0f))
        assertFalse(selector.shouldUsePadding(CropMode.FILL, 0.99f))
    }

    @Test
    fun `selectCropRect returns full source rect when padding in SAFE_FIT`() {
        val standard = com.wallpapercropfixer.domain.model.CropRect(100f, 0f, 900f, 1000f)
        val full = com.wallpapercropfixer.domain.model.CropRect(0f, 0f, 1000f, 1000f)
        val selected = selector.selectCropRect(CropMode.SAFE_FIT, standard, full, usePadding = true)
        org.junit.Assert.assertEquals(full, selected)
    }

    @Test
    fun `selectCropRect returns standard crop rect when not padding or in other modes`() {
        val standard = com.wallpapercropfixer.domain.model.CropRect(100f, 0f, 900f, 1000f)
        val full = com.wallpapercropfixer.domain.model.CropRect(0f, 0f, 1000f, 1000f)
        val selectedSafeFitNoPad = selector.selectCropRect(CropMode.SAFE_FIT, standard, full, usePadding = false)
        val selectedFill = selector.selectCropRect(CropMode.FILL, standard, full, usePadding = false)
        org.junit.Assert.assertEquals(standard, selectedSafeFitNoPad)
        org.junit.Assert.assertEquals(standard, selectedFill)
    }
}

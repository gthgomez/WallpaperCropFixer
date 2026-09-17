package com.wallpapercropfixer.domain.engine

import com.wallpapercropfixer.domain.model.CropMode
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CropStrategySelectorTest {

    private val selector = CropStrategySelector()

    @Test
    fun `SAFE_FIT pads when removal is non-zero to guarantee whole photo preservation`() {
        assertTrue(selector.shouldUsePadding(CropMode.SAFE_FIT, 0.25f))
        assertTrue(selector.shouldUsePadding(CropMode.SAFE_FIT, 0.05f))
    }

    @Test
    fun `SAFE_FIT does not pad when removal is zero`() {
        assertFalse(selector.shouldUsePadding(CropMode.SAFE_FIT, 0.0f))
    }

    @Test
    fun `BALANCED pads when removal exceeds 35 percent crop budget`() {
        assertFalse(selector.shouldUsePadding(CropMode.BALANCED, 0.25f))
        assertTrue(selector.shouldUsePadding(CropMode.BALANCED, 0.36f))
    }

    @Test
    fun `SAFE_FIT pads when faces are clipped even if removal is zero`() {
        assertTrue(selector.shouldUsePadding(CropMode.SAFE_FIT, 0.0f, hasClippedFaces = true))
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

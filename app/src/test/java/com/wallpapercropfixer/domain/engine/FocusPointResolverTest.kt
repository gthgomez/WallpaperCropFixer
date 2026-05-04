package com.wallpapercropfixer.domain.engine

import com.wallpapercropfixer.core.math.CropMath
import com.wallpapercropfixer.domain.model.FaceBounds
import com.wallpapercropfixer.domain.model.FocusPoint
import com.wallpapercropfixer.domain.model.SubjectAnalysis
import org.junit.Assert.assertEquals
import org.junit.Test

class FocusPointResolverTest {

    private val resolver = FocusPointResolver()
    private val tolerance = 0.001f

    @Test
    fun `manual focus point takes priority over everything`() {
        val manual = FocusPoint(0.3f, 0.7f)
        val analysis = SubjectAnalysis(
            faces = listOf(FaceBounds(0f, 0f, 100f, 100f)),
            suggestedFocusPoint = FocusPoint(0.5f, 0.5f)
        )
        val result = resolver.resolve(
            manual = manual,
            faceAwareEnabled = true,
            subjectAnalysis = analysis,
            sourceWidth = 1000,
            sourceHeight = 1000
        )
        assertEquals(0.3f, result.xNormalized, tolerance)
        assertEquals(0.7f, result.yNormalized, tolerance)
    }

    @Test
    fun `face suggested focus used when face-aware enabled and no manual`() {
        val suggested = FocusPoint(0.4f, 0.3f)
        val analysis = SubjectAnalysis(
            faces = listOf(FaceBounds(0f, 0f, 100f, 100f)),
            suggestedFocusPoint = suggested
        )
        val result = resolver.resolve(
            manual = null,
            faceAwareEnabled = true,
            subjectAnalysis = analysis,
            sourceWidth = 1000,
            sourceHeight = 1000
        )
        assertEquals(0.4f, result.xNormalized, tolerance)
        assertEquals(0.3f, result.yNormalized, tolerance)
    }

    @Test
    fun `face cluster center computed when no suggested focus`() {
        val analysis = SubjectAnalysis(
            faces = listOf(
                FaceBounds(100f, 100f, 200f, 200f),
                FaceBounds(800f, 100f, 900f, 200f)
            ),
            suggestedFocusPoint = null
        )
        val result = resolver.resolve(
            manual = null,
            faceAwareEnabled = true,
            subjectAnalysis = analysis,
            sourceWidth = 1000,
            sourceHeight = 1000
        )
        // union: left=100, top=100, right=900, bottom=200
        // center x = (100+900)/2 = 500 → 0.5, center y = (100+200)/2 = 150 → 0.15
        assertEquals(0.5f, result.xNormalized, tolerance)
        assertEquals(0.15f, result.yNormalized, tolerance)
    }

    @Test
    fun `defaults to image center when face-aware disabled`() {
        val analysis = SubjectAnalysis(
            faces = listOf(FaceBounds(0f, 0f, 100f, 100f)),
            suggestedFocusPoint = FocusPoint(0.1f, 0.1f)
        )
        val result = resolver.resolve(
            manual = null,
            faceAwareEnabled = false,
            subjectAnalysis = analysis,
            sourceWidth = 1000,
            sourceHeight = 1000
        )
        assertEquals(CropMath.CENTER_FOCUS.xNormalized, result.xNormalized, tolerance)
        assertEquals(CropMath.CENTER_FOCUS.yNormalized, result.yNormalized, tolerance)
    }

    @Test
    fun `defaults to image center when no analysis`() {
        val result = resolver.resolve(
            manual = null,
            faceAwareEnabled = true,
            subjectAnalysis = null,
            sourceWidth = 1000,
            sourceHeight = 1000
        )
        assertEquals(0.5f, result.xNormalized, tolerance)
        assertEquals(0.5f, result.yNormalized, tolerance)
    }
}

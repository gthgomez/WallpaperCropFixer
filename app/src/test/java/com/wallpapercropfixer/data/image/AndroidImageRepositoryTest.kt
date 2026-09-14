package com.wallpapercropfixer.data.image

import android.graphics.Bitmap
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.io.File
import java.io.FileOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AndroidImageRepositoryTest {

    @Test
    fun `readImageMeta reads bounds from plain cache file path`() = runTest {
        val context = RuntimeEnvironment.getApplication()
        val sourceFile = File(context.cacheDir, "wcf_pick_test.jpg")
        val bitmap = Bitmap.createBitmap(12, 8, Bitmap.Config.ARGB_8888)

        FileOutputStream(sourceFile).use { output ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, output)
        }

        val meta = AndroidImageRepository(context).readImageMeta(sourceFile.absolutePath)

        assertEquals(sourceFile.absolutePath, meta.uri)
        assertEquals(12, meta.width)
        assertEquals(8, meta.height)
    }

    @Test
    fun `decodeBitmapSampled downsamples large bitmap`() = runTest {
        val context = RuntimeEnvironment.getApplication()
        val sourceFile = File(context.cacheDir, "wcf_large_test.jpg")
        val bitmap = Bitmap.createBitmap(1200, 800, Bitmap.Config.ARGB_8888)

        FileOutputStream(sourceFile).use { output ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, output)
        }

        val decoded = AndroidImageRepository(context).decodeBitmapSampled(sourceFile.absolutePath, 300, 200)

        assertTrue(decoded.width <= 600)
        assertTrue(decoded.height <= 400)
    }

    @Test
    fun `computeSampleSize keeps decoded dimensions within the decode budget`() {
        val context = RuntimeEnvironment.getApplication()
        val repo = AndroidImageRepository(context)

        // 9000x9000 against a 3225x4096 budget used to stop at sampleSize 2 and
        // decode 4500x4500 (~20 MP); the halving must continue until it fits.
        assertEquals(4, repo.computeSampleSize(9000, 9000, 3225, 4096))
        assertTrue(9000 / repo.computeSampleSize(9000, 9000, 3225, 4096) <= 3225)
        assertTrue(9000 / repo.computeSampleSize(9000, 9000, 3225, 4096) <= 4096)

        // Sources already within budget are never downsampled (no upscaling).
        assertEquals(1, repo.computeSampleSize(800, 600, 1080, 2400))

        // Exact-fit boundaries stay at the previously chosen sample size.
        assertEquals(4, repo.computeSampleSize(1200, 800, 300, 200))
    }
}

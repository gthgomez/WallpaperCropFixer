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
        assertTrue(meta.width > 0)
        assertTrue(meta.height > 0)
    }
}

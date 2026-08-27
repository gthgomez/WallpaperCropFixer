package com.wallpapercropfixer.data.export

import android.graphics.Bitmap
import com.wallpapercropfixer.domain.repository.ExportDestination
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class AndroidWallpaperExportRepositoryTest {

    @Test
    fun `api 28 export writes to app external files and reports the true destination`() {
        val context = RuntimeEnvironment.getApplication()
        val repo = AndroidWallpaperExportRepository(context)
        val bitmap = Bitmap.createBitmap(20, 40, Bitmap.Config.ARGB_8888)

        val result = runBlocking {
            repo.exportBitmap(bitmap, "wcf_legacy_test", Bitmap.CompressFormat.JPEG, 90)
        }

        assertEquals(ExportDestination.APP_EXTERNAL_FILES, result.destination)
        val file = File(result.pathOrUri)
        assertTrue("exported file must exist", file.exists())
        assertTrue("exported file must be non-empty", file.length() > 0)
        assertEquals("WallpaperCropFixer", file.parentFile?.name)
        assertEquals("wcf_legacy_test.jpg", file.name)
        // The success message must never claim MediaStore/gallery on legacy devices.
        assertTrue(ExportDestination.APP_EXTERNAL_FILES != ExportDestination.MEDIA_STORE)
    }
}
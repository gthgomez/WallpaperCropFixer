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

    @Test
    fun `api 28 same-second re-save does not overwrite the previous export`() {
        val context = RuntimeEnvironment.getApplication()
        val repo = AndroidWallpaperExportRepository(context)
        val bitmap = Bitmap.createBitmap(20, 40, Bitmap.Config.ARGB_8888)

        val first = runBlocking {
            repo.exportBitmap(bitmap, "wcf_collision_test", Bitmap.CompressFormat.JPEG, 90)
        }
        val second = runBlocking {
            repo.exportBitmap(bitmap, "wcf_collision_test", Bitmap.CompressFormat.JPEG, 90)
        }

        assertEquals("wcf_collision_test.jpg", first.displayName)
        assertEquals("the colliding re-save must shift to a -1 suffix",
            "wcf_collision_test-1.jpg", second.displayName)
        assertTrue(first.pathOrUri != second.pathOrUri)
        // Both exports must remain intact on disk.
        assertTrue("first export must survive the re-save", File(first.pathOrUri).exists())
        assertTrue("first export must be non-empty", File(first.pathOrUri).length() > 0)
        assertTrue("second export must exist", File(second.pathOrUri).exists())
        assertTrue("second export must be non-empty", File(second.pathOrUri).length() > 0)
    }
}
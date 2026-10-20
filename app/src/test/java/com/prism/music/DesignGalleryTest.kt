package com.prism.music

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.prism.music.data.prefs.AppSettings
import com.prism.music.data.prefs.ThemeMode
import com.prism.music.ui.components.DesignGalleryContent
import com.prism.music.ui.theme.PrismTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * Screenshots the Design gallery (every shared component of the 1.7 redesign, in each state) on the PC, as the app's
 * theme draws it: dark and light, with the default accent and with a hard one (pale yellow), onto
 * app/build/design-previews/. Look at them after changing a component.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w412dp-h3000dp-xxhdpi")
class DesignGalleryTest {
    @get:Rule val compose = createComposeRule()

    /** [glass]: as the app draws it, on a background (the Spectrum fields here, standing in for the ambient frost). */
    private fun shoot(name: String, settings: AppSettings, glass: Boolean = true) {
        compose.setContent {
            PrismTheme(settings, artworkColor = null) {
                Box(Modifier.width(412.dp).background(MaterialTheme.colorScheme.background)) {
                    if (glass) com.prism.music.ui.theme.BackdropLayer(
                        com.prism.music.ui.theme.Backdrop.SPECTRUM, null, 0.35f, motion = false, modifier = Modifier.matchParentSize(),
                    )
                    androidx.compose.runtime.CompositionLocalProvider(com.prism.music.ui.components.LocalFrosted provides glass) { DesignGalleryContent() }
                }
            }
        }
        compose.waitForIdle()
        val bmp = compose.onRoot().captureToImage().asAndroidBitmap()
        val dir = File("build/design-previews").apply { mkdirs() }
        File(dir, "$name.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test fun dark() = shoot("gallery-dark", AppSettings(themeMode = ThemeMode.DARK))
    @Test fun light() = shoot("gallery-light", AppSettings(themeMode = ThemeMode.LIGHT))
    @Test fun lightPaleAccent() = shoot("gallery-light-yellow", AppSettings(themeMode = ThemeMode.LIGHT, accent = 0xFFFFE066.toInt()))
    @Test fun darkPlainPage() = shoot("gallery-dark-plain", AppSettings(themeMode = ThemeMode.DARK), glass = false)
}

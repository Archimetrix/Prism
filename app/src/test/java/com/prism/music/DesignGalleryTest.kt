package com.prism.music

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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

    // 1.7.2 color settings.
    @Test fun neonLightDark() = shoot("gallery-dark-neon", AppSettings(themeMode = ThemeMode.DARK, spectrumPreset = "NEON"), glass = false)
    @Test fun sunsetBandsLight() = shoot("gallery-light-sunset-bands", AppSettings(themeMode = ThemeMode.LIGHT, spectrumPreset = "SUNSET", spectrumBands = true), glass = false)
    @Test fun ownLightAndTint() = shoot(
        "gallery-dark-own-tinted",
        AppSettings(
            themeMode = ThemeMode.DARK, spectrumPreset = "CUSTOM", spectrumStops = listOf(0xFF00C2FF.toInt(), 0xFFFFFFFF.toInt(), 0xFFFF2E93.toInt()),
            surfaceTint = com.prism.music.data.prefs.SurfaceTint.CUSTOM, surfaceColor = 0xFF2EC27E.toInt(), surfaceTintStrength = 2.5f,
        ),
        glass = false,
    )
    @Test fun exactAccentHighContrastLight() = shoot(
        "gallery-light-exact-high",
        AppSettings(themeMode = ThemeMode.LIGHT, accent = 0xFFFF6B35.toInt(), accentExact = true, contrast = com.prism.music.data.prefs.ContrastLevel.HIGH),
        glass = false,
    )

    /** The color wheel and the spectrum presets, dark and light. */
    @Test fun colorPickerDark() = colorPicker(true)
    @Test fun colorPickerLight() = colorPicker(false)

    private fun colorPicker(dark: Boolean) {
        run {
            compose.setContent {
                PrismTheme(AppSettings(themeMode = if (dark) ThemeMode.DARK else ThemeMode.LIGHT), artworkColor = null) {
                    androidx.compose.foundation.layout.Column(
                        Modifier.width(412.dp).background(MaterialTheme.colorScheme.background),
                        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp),
                    ) {
                        com.prism.music.ui.components.ColorWheel(215f, 0.75f, 0.9f, Modifier.width(412.dp)) { _, _, _ -> }
                        com.prism.music.ui.theme.SpectrumPresets.all.forEach { p ->
                            val l = com.prism.music.ui.theme.SpectrumPresets.light(p.key, emptyList(), false)
                            androidx.compose.foundation.layout.Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                androidx.compose.material3.Text(p.label, Modifier.width(130.dp).padding(8.dp), style = MaterialTheme.typography.labelMedium)
                                Box(
                                    Modifier.width(180.dp).height(10.dp).background(
                                        androidx.compose.ui.graphics.Brush.horizontalGradient(*(if (dark) l.colors else l.deep).toTypedArray()),
                                        androidx.compose.foundation.shape.CircleShape,
                                    ),
                                )
                                l.hues.forEach { h -> Box(Modifier.padding(2.dp).size(12.dp).background(h)) }
                            }
                        }
                    }
                }
            }
            compose.waitForIdle()
            val bmp = compose.onRoot().captureToImage().asAndroidBitmap()
            val dir = File("build/design-previews").apply { mkdirs() }
            File(dir, "color-picker-${if (dark) "dark" else "light"}.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
    }
}

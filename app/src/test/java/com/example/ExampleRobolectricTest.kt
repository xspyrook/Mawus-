package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    private lateinit var context: Context
    private lateinit var settings: OrbSettings

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("orb_controller_prefs", Context.MODE_PRIVATE).edit().clear().commit()
        settings = OrbSettings.getInstance(context)
        settings.updateConfig(OrbConfig())
    }

    @Test
    fun `read string from context matches app name`() {
        val appName = context.getString(R.string.app_name)
        assertEquals("Orb Controller", appName)
    }

    @Test
    fun `verify orb settings default configuration`() {
        val config = settings.config.value

        assertEquals(56, config.bubbleSizeDp)
        assertTrue(config.snapToEdge)
        assertTrue(config.hapticEnabled)
        assertEquals(0, config.accentColorIndex)
        assertNotNull(config.accentColor)
    }

    @Test
    fun `verify orb settings update config`() {
        val updated = settings.config.value.copy(
            bubbleSizeDp = 64,
            hapticEnabled = false,
            accentColorIndex = 1
        )
        settings.updateConfig(updated)

        val result = settings.config.value
        assertEquals(64, result.bubbleSizeDp)
        assertFalse(result.hapticEnabled)
        assertEquals(1, result.accentColorIndex)
    }

    @Test
    fun `verify orb accessibility service state when unbound`() {
        assertFalse(OrbAccessibilityService.isRunning())
    }
}

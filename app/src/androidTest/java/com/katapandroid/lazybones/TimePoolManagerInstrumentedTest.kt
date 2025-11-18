package com.katapandroid.lazybones

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.katapandroid.lazybones.data.SettingsRepository
import com.katapandroid.lazybones.data.TimePoolManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TimePoolManagerInstrumentedTest {

    private lateinit var context: Context
    private lateinit var settings: SettingsRepository
    private lateinit var manager: TimePoolManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("lazybones_settings", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
        settings = SettingsRepository(context)
        settings.setPoolStartMinutes(480) // 08:00
        settings.setPoolEndMinutes(600)   // 10:00
        manager = TimePoolManager(settings)
    }

    @Test
    fun `validatePoolSettings rejects inverted range`() {
        val (valid, message) = manager.validatePoolSettings(600, 480)
        assertFalse(valid)
        assertEquals("Конец должен быть позже начала", message)
    }

    @Test
    fun `validatePoolSettings rejects duration over 12 hours`() {
        val (valid, message) = manager.validatePoolSettings(360, 1100)
        assertFalse(valid)
        assertEquals("Длительность пула не более 12 часов", message)
    }

    @Test
    fun `current pool range respects configured minutes`() {
        val (start, end) = manager.getCurrentPoolRange()
        val calendar = java.util.Calendar.getInstance().apply { time = start }
        assertEquals(8, calendar.get(java.util.Calendar.HOUR_OF_DAY))
        val endCal = java.util.Calendar.getInstance().apply { time = end }
        assertEquals(10, endCal.get(java.util.Calendar.HOUR_OF_DAY))
    }

}


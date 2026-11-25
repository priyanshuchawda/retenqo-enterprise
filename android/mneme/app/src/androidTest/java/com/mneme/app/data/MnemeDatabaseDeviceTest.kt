package com.mneme.app.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MnemeDatabaseDeviceTest {
    @Test
    fun openingDatabaseEnablesSecureDelete() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = MnemeDatabase.get(context).openHelper.writableDatabase
        database.query("PRAGMA secure_delete").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(1, cursor.getInt(0))
        }
    }
}

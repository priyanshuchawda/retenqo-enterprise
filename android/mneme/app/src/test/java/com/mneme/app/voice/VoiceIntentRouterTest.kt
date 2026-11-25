package com.mneme.app.voice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceIntentRouterTest {
    @Test
    fun routesRemember() {
        val i = VoiceIntentRouter.route("remember: buy milk tomorrow")
        assertTrue(i is VoiceIntent.Remember)
        assertEquals("buy milk tomorrow", (i as VoiceIntent.Remember).text)
    }

    @Test
    fun routesAlarm() {
        val i = VoiceIntentRouter.route("set an alarm at 7:30 am")
        assertTrue(i is VoiceIntent.SetAlarm)
        val a = i as VoiceIntent.SetAlarm
        assertEquals(7, a.hour)
        assertEquals(30, a.minute)
    }

    @Test
    fun routesQuery() {
        val i = VoiceIntentRouter.route("when is the lab due")
        assertTrue(i is VoiceIntent.Query)
    }

    @Test
    fun routesWhatsDue() {
        val i = VoiceIntentRouter.route("what's due today")
        assertTrue(i is VoiceIntent.WhatsDue)
    }

    @Test
    fun routesPhoneActions() {
        assertTrue(VoiceIntentRouter.route("what can you do with my phone") is VoiceIntent.Capabilities)
        assertTrue(VoiceIntentRouter.route("check my email") is VoiceIntent.CheckEmail)
        assertTrue(VoiceIntentRouter.route("read my WhatsApp notifications") is VoiceIntent.ReadNotifications)
        assertTrue(VoiceIntentRouter.route("add dentist appointment to my calendar") is VoiceIntent.AddCalendar)
        assertTrue(VoiceIntentRouter.route("make a task buy groceries") is VoiceIntent.CreateTask)
    }
}

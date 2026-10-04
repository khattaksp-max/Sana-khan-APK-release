package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.ChatMessageEntity
import com.example.data.db.MemoryEntity
import com.example.data.model.ActionCardData
import com.example.data.model.ActionType
import com.example.data.model.ChatMessage
import com.example.data.model.CompanionMode
import com.example.data.model.LanguageOption
import com.example.data.model.SanaVoice
import com.example.data.model.VoiceState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context verifies SANA app name`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("SANA", appName)
    }

    @Test
    fun `verify voice options exist and contain Kore and Aoede`() {
        val kore = SanaVoice.KORE
        val aoede = SanaVoice.AOEDE

        assertEquals("Kore", kore.voiceId)
        assertEquals("Aoede", aoede.voiceId)
        assertTrue(kore.description.contains("Warm", ignoreCase = true))
        assertTrue(aoede.description.contains("Luminous", ignoreCase = true))
    }

    @Test
    fun `verify companion modes contain all required personalities`() {
        val modes = CompanionMode.values()
        assertEquals(4, modes.size)
        assertTrue(modes.contains(CompanionMode.ASSISTANT))
        assertTrue(modes.contains(CompanionMode.FRIEND))
        assertTrue(modes.contains(CompanionMode.COMPANION))
        assertTrue(modes.contains(CompanionMode.ROMANTIC))
    }

    @Test
    fun `verify language options contain auto and multilingual options`() {
        val langs = LanguageOption.values()
        assertEquals(4, langs.size)
        assertTrue(langs.contains(LanguageOption.AUTO))
        assertTrue(langs.contains(LanguageOption.ENGLISH))
        assertTrue(langs.contains(LanguageOption.URDU))
        assertTrue(langs.contains(LanguageOption.ROMAN_URDU))
    }

    @Test
    fun `verify chat message entity conversions`() {
        val chatMessage = ChatMessage(
            id = 42L,
            isUser = false,
            text = "Hello! SANA is here.",
            actionCard = ActionCardData(
                type = ActionType.WHATSAPP_SEND,
                title = "Send WhatsApp to Ali",
                description = "I'll call you later",
                target = "Ali",
                extra = "I'll call you later"
            ),
            mode = CompanionMode.COMPANION
        )

        val entity = ChatMessageEntity.fromChatMessage(chatMessage)
        assertEquals(false, entity.isUser)
        assertEquals("Hello! SANA is here.", entity.text)
        assertEquals(CompanionMode.COMPANION.name, entity.mode)
        assertEquals(ActionType.WHATSAPP_SEND.name, entity.actionType)

        val converted = entity.toChatMessage()
        assertEquals(chatMessage.text, converted.text)
        assertEquals(chatMessage.mode, converted.mode)
        assertNotNull(converted.actionCard)
        assertEquals(ActionType.WHATSAPP_SEND, converted.actionCard?.type)
        assertEquals("Ali", converted.actionCard?.target)
    }

    @Test
    fun `verify memory entity data integrity`() {
        val memory = MemoryEntity(
            category = "Preference",
            content = "User loves black coffee with cardamom"
        )
        assertEquals("Preference", memory.category)
        assertTrue(memory.content.contains("cardamom"))
    }
}

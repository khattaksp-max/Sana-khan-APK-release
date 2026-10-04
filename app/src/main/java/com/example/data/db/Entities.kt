package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.ActionCardData
import com.example.data.model.ActionType
import com.example.data.model.ChatMessage
import com.example.data.model.CompanionMode

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val isUser: Boolean,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val mode: String = CompanionMode.ASSISTANT.name,
    val actionType: String? = null,
    val actionTitle: String? = null,
    val actionTarget: String? = null,
    val actionExtra: String? = null
) {
    fun toChatMessage(): ChatMessage {
        val actionCard = if (actionType != null && actionTitle != null && actionTarget != null) {
            try {
                ActionCardData(
                    type = ActionType.valueOf(actionType),
                    title = actionTitle,
                    description = actionExtra ?: "",
                    target = actionTarget,
                    extra = actionExtra
                )
            } catch (e: Exception) {
                null
            }
        } else null

        val companionMode = try {
            CompanionMode.valueOf(mode)
        } catch (e: Exception) {
            CompanionMode.ASSISTANT
        }

        return ChatMessage(
            id = id,
            isUser = isUser,
            text = text,
            timestamp = timestamp,
            actionCard = actionCard,
            mode = companionMode
        )
    }

    companion object {
        fun fromChatMessage(msg: ChatMessage): ChatMessageEntity {
            return ChatMessageEntity(
                isUser = msg.isUser,
                text = msg.text,
                timestamp = msg.timestamp,
                mode = msg.mode.name,
                actionType = msg.actionCard?.type?.name,
                actionTitle = msg.actionCard?.title,
                actionTarget = msg.actionCard?.target,
                actionExtra = msg.actionCard?.description
            )
        }
    }
}

@Entity(tableName = "sana_memories")
data class MemoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val category: String, // e.g. "Preference", "Fact", "Contact", "Relationship"
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)

package com.example.healthchatbot.models

data class ChatMessage(
    val text: String,
    val isUser: Boolean,
    val isSystem: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)
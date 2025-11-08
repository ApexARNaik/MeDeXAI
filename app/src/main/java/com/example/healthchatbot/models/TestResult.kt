package com.example.healthchatbot.models

data class TestResult(
    val testName: String,
    val value: String,
    val unit: String = "",
    val referenceRange: String = ""
)
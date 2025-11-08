package com.example.healthchatbot

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.healthchatbot.models.ChatMessage
import com.example.healthchatbot.models.TestResult
import com.runanywhere.sdk.public.RunAnywhere
import com.runanywhere.sdk.models.RunAnywhereGenerationOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class HealthChatViewModel : ViewModel() {

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _modelStatus = MutableStateFlow("Initializing...")
    val modelStatus: StateFlow<String> = _modelStatus.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private var extractedData = mutableListOf<TestResult>()
    private val pdfExtractor = PDFExtractor()

    init {
        // Optionally, add an initial message
        viewModelScope.launch {
            addSystemMessage("Welcome to Health Chat! Please initialize the SDK and load a model to begin using real AI.")
        }
    }

    suspend fun sendMessage(text: String) {
        val currentModel = RunAnywhere.currentModel
        if (currentModel == null) {
            _error.value = "Please load a model first to use real AI responses"
            return
        }

        addMessage(ChatMessage(text = text, isUser = true))

        _isLoading.value = true

        try {
            val context = buildMedicalContext()
            val prompt = buildPrompt(text, context)
            val response = generateResponse(prompt)

            addMessage(ChatMessage(text = response, isUser = false))

        } catch (e: Exception) {
            _error.value = "Failed to generate AI response: ${e.message}"
            addMessage(ChatMessage(
                text = "Sorry, I encountered an error generating a response. Please try again.",
                isUser = false
            ))
        } finally {
            _isLoading.value = false
        }
    }

    private suspend fun generateResponse(prompt: String): String {
        return withContext(Dispatchers.IO) {
            try {
                val tokens = mutableListOf<String>()

                // Use REAL RunAnywhere SDK for AI generation
                RunAnywhere.generateStream(
                    prompt = prompt,
                    options = RunAnywhereGenerationOptions(
                        maxTokens = 300,
                        temperature = 0.7f
                    )
                ).collect { token ->
                    tokens.add(token)
                }

                val response = tokens.joinToString("")

                if (response.isBlank()) {
                    "I apologize, but I couldn't generate a response. Please ensure the AI model is properly loaded and try again."
                } else {
                    response
                }

            } catch (e: Exception) {
                "I apologize, but I'm having trouble processing your request with the AI model. Error: ${e.message}"
            }
        }
    }

    private fun buildPrompt(userQuery: String, context: String): String {
        return """You are a knowledgeable health assistant analyzing medical reports and answering health questions.

Medical Data Available:
$context

User Question: $userQuery

Please provide a clear, helpful response based on the medical data available. If you don't have relevant data for the specific question, provide general health guidance while noting the limitation. Keep responses professional, informative, and remind users to consult healthcare providers for medical decisions.

Response:"""
    }

    private fun buildMedicalContext(): String {
        if (extractedData.isEmpty()) {
            return "No medical reports have been uploaded yet. General health guidance can be provided."
        }

        val contextBuilder = StringBuilder()
        contextBuilder.append("Patient's Medical Test Results:\n")

        extractedData.forEach { result ->
            contextBuilder.append("- ${result.testName}: ${result.value}\n")
        }

        return contextBuilder.toString()
    }

    suspend fun processPdf(context: Context, uri: Uri) {
        _isLoading.value = true
        _modelStatus.value = "Processing PDF..."

        try {
            val results = withContext(Dispatchers.IO) {
                pdfExtractor.extractFromPdf(context, uri)
            }

            if (results.isEmpty()) {
                _error.value = "No test results found in PDF"
            } else {
                extractedData.addAll(results)
                addSystemMessage(
                    "✓ Successfully extracted ${results.size} test results from your PDF.\n" +
                            "You can now ask questions about your medical data and get AI-powered insights!"
                )
            }

        } catch (e: Exception) {
            _error.value = "Failed to process PDF: ${e.message}"
        } finally {
            _isLoading.value = false
            // Update model status
            val currentModel = RunAnywhere.currentModel
            _modelStatus.value = if (currentModel != null) {
                "Real AI Model ready ✓ | ${extractedData.size} tests loaded"
            } else {
                "Model not loaded | ${extractedData.size} tests loaded"
            }
        }
    }

    private fun addMessage(message: ChatMessage) {
        _messages.value = _messages.value + message
    }

    private fun addSystemMessage(text: String) {
        addMessage(ChatMessage(text = text, isUser = false, isSystem = true))
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            try {
                RunAnywhere.unloadModel()
            } catch (e: Exception) {
                // Ignore cleanup errors
            }
        }
    }
}
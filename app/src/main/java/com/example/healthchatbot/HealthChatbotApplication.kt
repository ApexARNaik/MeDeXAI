package com.example.healthchatbot

import android.app.Application
import android.util.Log

class HealthChatbotApplication : Application() {

    companion object {
        // These are now controlled by MainActivity, not here.
        @Volatile
        var isSDKInitialized = false

        @Volatile
        var initializationError: String? = null

        // Prevent multiple initialization attempts
        @Volatile
        var isInitializing = false
    }

    override fun onCreate() {
        super.onCreate()
        Log.d("HealthChatbot", "Application onCreate() called")

        // Set up uncaught exception handler to prevent silent crashes
        Thread.setDefaultUncaughtExceptionHandler { thread, exception ->
            Log.e("HealthChatbot", "Uncaught exception in thread ${thread.name}", exception)

            // Reset initialization state on crash
            isSDKInitialized = false
            isInitializing = false
            initializationError = "Application crashed: ${exception.localizedMessage}"

            // Call the default handler to maintain normal crash behavior
            Thread.getDefaultUncaughtExceptionHandler()?.uncaughtException(thread, exception)
        }
    }
}
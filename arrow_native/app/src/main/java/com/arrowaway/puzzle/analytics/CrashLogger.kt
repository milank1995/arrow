package com.arrowaway.puzzle.analytics

import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics

/**
 * Production Crash & Exception Logging facade for Arrow Away.
 * Bridges to Firebase Crashlytics with custom keys, breadcrumbs, and non-fatal logging.
 */
object CrashLogger {

    private const val TAG = "ArrowAwayCrashLogger"
    private var crashlytics: FirebaseCrashlytics? = null

    init {
        try {
            crashlytics = FirebaseCrashlytics.getInstance()
        } catch (e: Exception) {
            Log.w(TAG, "Crashlytics not initialized or running in mock environment: ${e.message}")
        }
    }

    fun log(message: String) {
        try {
            crashlytics?.log(message)
            Log.i(TAG, "[Breadcrumb] $message")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to log breadcrumb: ${e.message}")
        }
    }

    fun setCustomKey(key: String, value: Any) {
        try {
            when (value) {
                is Boolean -> crashlytics?.setCustomKey(key, value)
                is Int -> crashlytics?.setCustomKey(key, value)
                is Long -> crashlytics?.setCustomKey(key, value)
                is Float -> crashlytics?.setCustomKey(key, value)
                is Double -> crashlytics?.setCustomKey(key, value)
                else -> crashlytics?.setCustomKey(key, value.toString())
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to set custom key $key: ${e.message}")
        }
    }

    fun recordException(
        throwable: Throwable,
        location: String? = null,
        customAttributes: Map<String, Any> = emptyMap()
    ) {
        try {
            if (location != null) {
                setCustomKey("error_location", location)
            }
            customAttributes.forEach { (k, v) ->
                setCustomKey(k, v)
            }
            crashlytics?.recordException(throwable)
            Log.e(TAG, "[NonFatalException at ${location ?: "Unknown"}] ${throwable.message}", throwable)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to record non-fatal exception: ${e.message}")
        }
    }

    fun setUserId(userId: String) {
        try {
            crashlytics?.setUserId(userId)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to set userId: ${e.message}")
        }
    }
}

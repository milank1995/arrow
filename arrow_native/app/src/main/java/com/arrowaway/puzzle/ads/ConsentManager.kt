package com.arrowaway.puzzle.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.arrowaway.puzzle.BuildConfig
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.FormError
import com.google.android.ump.UserMessagingPlatform

/**
 * Google User Messaging Platform (UMP) wrapper.
 *
 * Shows the GDPR / US-state privacy messages configured in AdMob > Privacy & messaging
 * when the user's region requires them, and tells the rest of the app whether ads may be requested.
 * Firebase Analytics and the Mobile Ads SDK read the resulting IAB TCF / GPP consent strings automatically.
 */
object ConsentManager {

    private const val TAG = "ArrowAwayConsent"

    /**
     * Debug builds only: set to true to simulate a user in the EEA so the consent form always appears.
     * Has no effect in release builds.
     */
    private const val DEBUG_FORCE_EEA = false

    /**
     * Debug builds only: hashed device IDs that may use the debug geography above.
     * The UMP SDK prints this ID in Logcat ("Use new ConsentDebugSettings.Builder().addTestDeviceHashedId(...)").
     */
    private val DEBUG_TEST_DEVICE_HASHED_IDS: List<String> = emptyList()

    private fun consentInformation(context: Context): ConsentInformation =
        UserMessagingPlatform.getConsentInformation(context)

    /** True when the user has given (or does not need to give) consent and ads may be requested. */
    fun canRequestAds(context: Context): Boolean = consentInformation(context).canRequestAds()

    /** True when the user must be offered a way to change their privacy choices (e.g. EEA/UK users). */
    fun isPrivacyOptionsRequired(context: Context): Boolean =
        consentInformation(context).privacyOptionsRequirementStatus ==
            ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED

    /**
     * Refreshes consent status and shows the consent form if it is required and not yet answered.
     * [onComplete] is always called exactly once on the main thread, with a [FormError] if anything failed.
     * Even on failure, [canRequestAds] reflects the last known consent, so callers should check it.
     */
    fun gatherConsent(activity: Activity, onComplete: (FormError?) -> Unit) {
        val paramsBuilder = ConsentRequestParameters.Builder()
        if (BuildConfig.DEBUG && DEBUG_FORCE_EEA) {
            val debugSettings = ConsentDebugSettings.Builder(activity)
                .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
                .apply { DEBUG_TEST_DEVICE_HASHED_IDS.forEach { addTestDeviceHashedId(it) } }
                .build()
            paramsBuilder.setConsentDebugSettings(debugSettings)
        }

        val info = consentInformation(activity)
        info.requestConsentInfoUpdate(
            activity,
            paramsBuilder.build(),
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { formError ->
                    if (formError != null) {
                        Log.w(TAG, "Consent form error ${formError.errorCode}: ${formError.message}")
                    }
                    Log.i(TAG, "Consent gathered. canRequestAds=${info.canRequestAds()}")
                    onComplete(formError)
                }
            },
            { requestError ->
                Log.w(TAG, "Consent info update failed ${requestError.errorCode}: ${requestError.message}")
                onComplete(requestError)
            }
        )
    }

    /** Opens the privacy options form so the user can review or change their choices. */
    fun showPrivacyOptionsForm(activity: Activity, onDismissed: (FormError?) -> Unit = {}) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { formError ->
            if (formError != null) {
                Log.w(TAG, "Privacy options form error ${formError.errorCode}: ${formError.message}")
            }
            onDismissed(formError)
        }
    }

    /** Debug builds only: clears stored consent so the form appears again on next launch. */
    fun resetForTesting(context: Context) {
        if (BuildConfig.DEBUG) consentInformation(context).reset()
    }
}

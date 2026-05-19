package com.nimmaguru.app

import android.app.Application
import com.nimmaguru.app.BuildConfig
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import dagger.hilt.android.HiltAndroidApp

/**
 * Application class for Nimma Guru.
 *
 * - Hilt code generation entrypoint (P-DI-04).
 * - Installs the Firebase App Check provider (P0.5):
 *     - Debug build: DebugAppCheckProviderFactory (gives a debug token,
 *       which you must register at Firebase Console → App Check → Debug
 *       tokens). Without that step, App-Check-enforced services will
 *       reject debug calls.
 *     - Release build: PlayIntegrityAppCheckProviderFactory.
 */
@HiltAndroidApp
class NimmaGuruApp : Application() {

    override fun onCreate() {
        super.onCreate()
        installAppCheck()
    }

    private fun installAppCheck() {
        val factory = if (BuildConfig.DEBUG) {
            DebugAppCheckProviderFactory.getInstance()
        } else {
            PlayIntegrityAppCheckProviderFactory.getInstance()
        }
        FirebaseAppCheck.getInstance().installAppCheckProviderFactory(factory)
    }
}

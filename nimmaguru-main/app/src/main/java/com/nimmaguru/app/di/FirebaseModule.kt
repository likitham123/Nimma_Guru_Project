package com.nimmaguru.app.di

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.persistentCacheSettings
import com.google.firebase.ktx.Firebase
import com.google.firebase.vertexai.FirebaseVertexAI
import com.google.firebase.vertexai.vertexAI
import com.google.firebase.vertexai.GenerativeModel
import com.google.firebase.vertexai.type.generationConfig
import com.nimmaguru.app.core.common.Constants
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module providing Firebase service instances.
 *
 * R-DI-01: External classes use @Provides.
 * R-DI-02: Firebase instances are @Singleton.
 * R-DI-03: Installed in SingletonComponent.
 * P-DI-02: Has @InstallIn annotation.
 */
@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = Firebase.auth

    @Provides
    @Singleton
    fun provideFirestore(): FirebaseFirestore {
        val firestore = Firebase.firestore
        // P-NICHE-05: Set cache size limit to prevent offline queue from growing forever
        val settings = com.google.firebase.firestore.firestoreSettings {
            setLocalCacheSettings(persistentCacheSettings {
                setSizeBytes(Constants.FIRESTORE_CACHE_SIZE_BYTES)
            })
        }
        firestore.firestoreSettings = settings
        return firestore
    }

    @Provides
    @Singleton
    fun provideFirebaseVertexAI(): FirebaseVertexAI = com.google.firebase.Firebase.vertexAI

    @Provides
    @Singleton
    fun provideGenerativeModel(vertexAI: FirebaseVertexAI): GenerativeModel {
        return vertexAI.generativeModel(
            modelName = "gemini-2.5-flash",
            generationConfig = generationConfig {
                temperature = 0.7f
                maxOutputTokens = 150
            }
        )
    }
}

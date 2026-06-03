package com.nimmaguru.app.feature.profile.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.nimmaguru.app.core.common.Constants
import com.nimmaguru.app.core.model.Guru
import com.nimmaguru.app.feature.profile.domain.repository.GuruRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository implementation for Guru profile operations.
 *
 * R-FB-05: All Firebase SDK calls in data layer only.
 * R-ARCH-03: Single Source of Truth.
 *
 * Wall-of-Fame ranking (H11/H12): every write computes and persists
 * `fameScore = (appreciationCount * 2) + totalSessions + (avgRating * 5)`
 * so [getTopGurus] / [observeTopGurus] can `orderBy("fameScore", DESC)`.
 *
 * Storage uploads: Firebase Storage is OFF on the Spark plan, so the
 * profile flow embeds compressed JPEG bytes as a base64 data URL in
 * [Guru.photoUrl]. [GuruAvatar] handles both http(s) and data URLs.
 */
@Singleton
class GuruRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
) : GuruRepository {

    private companion object {
        const val TAG = "NimmaGuruGuruRepo"
    }

    private val gurusCollection = firestore.collection(Constants.COLLECTION_GURUS)

    override suspend fun getGuru(guruId: String): Result<Guru> {
        return try {
            val doc = gurusCollection.document(guruId).get().await()
            val guru = doc.toObject(Guru::class.java)?.copy(id = doc.id)
            if (guru != null) Result.success(guru)
            else Result.failure(Exception("Guru not found"))
        } catch (e: Exception) {
            Log.e(TAG, "getGuru failed", e)
            Result.failure(e)
        }
    }

    override fun observeGuru(guruId: String): Flow<Guru?> = callbackFlow {
        val listener = gurusCollection.document(guruId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "observeGuru error", error)
                    trySend(null)
                    return@addSnapshotListener
                }
                val guru = snapshot?.toObject(Guru::class.java)?.copy(id = snapshot.id)
                trySend(guru)
            }
        awaitClose { listener.remove() }
    }

    override fun observeTopGurus(limit: Int): Flow<List<Guru>> = callbackFlow {
        val listener = gurusCollection
            .whereEqualTo(Constants.FIELD_IS_PUBLIC, true)
            .orderBy(Constants.FIELD_FAME_SCORE, Query.Direction.DESCENDING)
            .limit(limit.toLong())
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "observeTopGurus error: ${error.message}", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val gurus = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Guru::class.java)?.copy(id = doc.id)
                }.orEmpty()
                trySend(gurus)
            }
        awaitClose { listener.remove() }
    }

    override suspend fun createOrUpdateGuru(guru: Guru): Result<String> {
        return try {
            val uid = auth.currentUser?.uid
                ?: return Result.failure(Exception("Not authenticated"))

            // Recompute fameScore from the persisted counters on the existing doc
            // (counters are mutated by the appreciation/session flows, not here).
            val existingDoc = gurusCollection.document(uid).get().await()
            val isNew = !existingDoc.exists()

            val totalSessions = if (isNew) 0L else existingDoc.getLong("totalSessions") ?: 0L
            val appreciationCount = if (isNew) 0L else existingDoc.getLong("appreciationCount") ?: 0L
            val avgRating = if (isNew) 0.0 else (existingDoc.getDouble("avgRating") ?: 0.0)
            val fameScore = (appreciationCount * 2.0) + totalSessions + (avgRating * 5.0)

            val data = hashMapOf<String, Any>(
                "ownerId" to uid,
                "nameEn" to guru.nameEn,
                "nameKn" to guru.nameKn,
                "village" to guru.village,
                "district" to guru.district,
                "skills" to guru.skills,
                "availability" to guru.availability,
                "bioEn" to guru.bioEn,
                "bioKn" to guru.bioKn,
                "photoUrl" to guru.photoUrl,
                "isPublic" to guru.isPublic,
                "langPref" to guru.langPref,
                Constants.FIELD_FAME_SCORE to fameScore,
                "updatedAt" to System.currentTimeMillis(),
            )

            if (isNew) {
                data["createdAt"] = System.currentTimeMillis()
                data["totalSessions"] = 0L
                data["totalStudents"] = 0L
                data["appreciationCount"] = 0L
                data["avgRating"] = 0.0
            }

            gurusCollection.document(uid).set(data, SetOptions.merge()).await()
            Result.success(uid)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getTopGurus(limit: Int): Result<List<Guru>> {
        return try {
            val snapshot = gurusCollection
                .whereEqualTo(Constants.FIELD_IS_PUBLIC, true)
                .orderBy(Constants.FIELD_FAME_SCORE, Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .get()
                .await()

            val gurus = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Guru::class.java)?.copy(id = doc.id)
            }
            Result.success(gurus)
        } catch (e: Exception) {
            Log.e(TAG, "getTopGurus failed", e)
            Result.success(emptyList())
        }
    }
}

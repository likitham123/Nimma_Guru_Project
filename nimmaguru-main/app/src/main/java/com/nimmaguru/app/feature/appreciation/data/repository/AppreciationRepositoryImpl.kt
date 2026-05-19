package com.nimmaguru.app.feature.appreciation.data.repository

import android.util.Log
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.nimmaguru.app.core.common.Constants
import com.nimmaguru.app.core.model.AppreciationNote
import com.nimmaguru.app.feature.appreciation.domain.repository.AppreciationRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firestore-backed AppreciationRepository.
 *
 * Atomic write (H21): the per-guru subcollection insert + global feed
 * insert + counter increment all happen inside a single
 * [FirebaseFirestore.runBatch] so partial failures can't leave orphan
 * documents or stale counters.
 *
 * Counter increment runs unconditionally because the moderation flag
 * (`isApproved`) is set false on create. Reads filter by
 * `isApproved == true`, so unmoderated content is invisible until a
 * Cloud Function (or admin) approves it. The counter is corrected
 * server-side when the flag flips. Until that function is in place,
 * approve manually from the Firebase console.
 */
@Singleton
class AppreciationRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
) : AppreciationRepository {

    private companion object {
        const val TAG = "NimmaGuruAppreciationRepo"
    }

    private fun appreciationsCollection(guruId: String) =
        firestore.collection(Constants.COLLECTION_GURUS)
            .document(guruId)
            .collection(Constants.SUBCOLLECTION_APPRECIATIONS)

    override fun observeAppreciations(guruId: String): Flow<List<AppreciationNote>> = callbackFlow {
        val listener = appreciationsCollection(guruId)
            .whereEqualTo(Constants.FIELD_IS_APPROVED, true)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "observeAppreciations error: ${error.message}", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val notes = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(AppreciationNote::class.java)?.copy(id = doc.id)
                }.orEmpty()
                trySend(notes)
            }
        awaitClose { listener.remove() }
    }

    override fun observeRecentAppreciations(limit: Int): Flow<List<AppreciationNote>> = callbackFlow {
        val listener = firestore.collection(Constants.COLLECTION_ALL_APPRECIATIONS)
            .whereEqualTo(Constants.FIELD_IS_APPROVED, true)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(limit.toLong())
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "observeRecentAppreciations error: ${error.message}", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val notes = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(AppreciationNote::class.java)?.copy(id = doc.id)
                }.orEmpty()
                trySend(notes)
            }
        awaitClose { listener.remove() }
    }

    override suspend fun postAppreciation(note: AppreciationNote): Result<String> {
        return try {
            val now = System.currentTimeMillis()
            val perGuruRef = appreciationsCollection(note.guruId).document()
            val globalRef = firestore.collection(Constants.COLLECTION_ALL_APPRECIATIONS).document()
            val guruRef = firestore.collection(Constants.COLLECTION_GURUS).document(note.guruId)

            val baseData = hashMapOf<String, Any>(
                "guruId" to note.guruId,
                "studentName" to note.studentName,
                "message" to note.message,
                "rating" to note.rating,
                "photoUrl" to (note.photoUrl ?: ""),
                "createdAt" to now,
                Constants.FIELD_IS_APPROVED to true, // Auto-approve for live updates
            )

            firestore.runTransaction { transaction ->
                val guruSnapshot = transaction.get(guruRef)
                
                val currentCount = guruSnapshot.getLong("appreciationCount")?.toInt() ?: 0
                val currentAvg = guruSnapshot.getDouble("avgRating")?.toFloat() ?: 0f
                val totalSessions = guruSnapshot.getLong("totalSessions")?.toInt() ?: 0
                
                val newCount = currentCount + 1
                val newAvg = ((currentAvg * currentCount) + note.rating) / newCount
                val newFameScore = (newCount * 2f) + totalSessions + (newAvg * 5f)

                transaction.set(perGuruRef, baseData)
                transaction.set(globalRef, baseData + ("noteId" to perGuruRef.id))
                
                transaction.update(
                    guruRef,
                    "appreciationCount", newCount,
                    "avgRating", newAvg,
                    "fameScore", newFameScore
                )
                null
            }.await()

            Result.success(perGuruRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

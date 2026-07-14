
package com.dan1eidtj.mayas

import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

interface CallRepository {
    suspend fun createOutgoingCall(
        callerId: String,
        receiverId: String,
        type: CallType
    ): Result<CallSession>

    fun observeCall(callId: String): Flow<CallSession?>

    fun observeIncomingCalls(currentUserId: String): Flow<CallSession?>

    suspend fun updateCallState(callId: String, state: CallState): Result<Unit>

    suspend fun updateOffer(callId: String, offer: String): Result<Unit>

    suspend fun updateAnswer(callId: String, answer: String): Result<Unit>

    suspend fun deleteCall(callId: String): Result<Unit>
    suspend fun addIceCandidate(callId: String, role: CallParticipantRole, candidate: IceCandidateData): Result<Unit>
    fun observeIceCandidates(callId: String, fromRole: CallParticipantRole): Flow<IceCandidateData>
}

class FirestoreCallRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : CallRepository {

    private val callsCollection get() = firestore.collection(CallSession.COLLECTION_NAME)

    private fun candidatesCollection(callId: String, role: CallParticipantRole) =
        callsCollection.document(callId).collection("candidates_${role.name.lowercase()}")

    override suspend fun addIceCandidate(
        callId: String,
        role: CallParticipantRole,
        candidate: IceCandidateData
    ): Result<Unit> = runCatching {
        candidatesCollection(callId, role).add(
            mapOf(
                "sdpMid" to candidate.sdpMid,
                "sdpMLineIndex" to candidate.sdpMLineIndex,
                "sdp" to candidate.sdp
            )
        ).await()
        Unit
    }

    override fun observeIceCandidates(
        callId: String,
        fromRole: CallParticipantRole
    ): Flow<IceCandidateData> = callbackFlow {
        val registration = candidatesCollection(callId, fromRole)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                snapshot?.documentChanges
                    ?.filter { it.type == DocumentChange.Type.ADDED }
                    ?.forEach { change ->
                        val doc = change.document
                        trySend(
                            IceCandidateData(
                                sdpMid = doc.getString("sdpMid"),
                                sdpMLineIndex = (doc.getLong("sdpMLineIndex") ?: 0L).toInt(),
                                sdp = doc.getString("sdp").orEmpty()
                            )
                        )
                    }
            }
        awaitClose { registration.remove() }
    }

    override suspend fun createOutgoingCall(
        callerId: String,
        receiverId: String,
        type: CallType
    ): Result<CallSession> = runCatching {
        val callId = UUID.randomUUID().toString()
        val session = CallSession(
            callId = callId,
            callerId = callerId,
            receiverId = receiverId,
            state = CallState.OUTGOING,
            type = type,
            createdAt = System.currentTimeMillis()
        )
        callsCollection.document(callId)
            .set(session.toFirestoreMap())
            .await()
        session
    }

    override fun observeCall(callId: String): Flow<CallSession?> = callbackFlow {
        val registration = callsCollection.document(callId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.let { CallSession.fromDocument(it) })
            }
        awaitClose { registration.remove() }
    }

    override fun observeIncomingCalls(currentUserId: String): Flow<CallSession?> = callbackFlow {
        val registration = callsCollection
            .whereEqualTo("receiverId", currentUserId)
            .whereEqualTo("state", CallState.OUTGOING.name)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(1)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val document = snapshot?.documents?.firstOrNull()
                trySend(document?.let { CallSession.fromDocument(it) })
            }
        awaitClose { registration.remove() }
    }

    override suspend fun updateCallState(callId: String, state: CallState): Result<Unit> =
        runCatching {
            callsCollection.document(callId)
                .update("state", state.name)
                .await()
        }

    override suspend fun updateOffer(callId: String, offer: String): Result<Unit> =
        runCatching {
            callsCollection.document(callId)
                .update("offer", offer)
                .await()
        }

    override suspend fun updateAnswer(callId: String, answer: String): Result<Unit> =
        runCatching {
            callsCollection.document(callId)
                .update("answer", answer)
                .await()
        }

    override suspend fun deleteCall(callId: String): Result<Unit> = runCatching {
        callsCollection.document(callId)
            .delete()
            .await()
    }
}
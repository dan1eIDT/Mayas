package com.dan1eidtj.data

import android.util.Log
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Source
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await


data class MuteState(
    val mutedChatIds: Set<String> = emptySet(),
    val massActive: Boolean = false,
    val exceptions: Set<String> = emptySet(),
    val covered: Set<String> = emptySet(),
    val includeNew: Boolean = true
) {
    fun isMuted(chatId: String): Boolean {
        if (chatId in mutedChatIds) return true
        if (!massActive || chatId in exceptions) return false
        return includeNew || chatId in covered
    }

    companion object {
        const val F_MUTED = "mutedChatIds"
        const val F_ACTIVE = "muteAllActive"
        const val F_EXCEPTIONS = "muteAllExceptions"
        const val F_COVERED = "muteAllCovered"
        const val F_INCLUDE_NEW = "muteAllIncludeNew"

        private fun DocumentSnapshot.stringSet(field: String): Set<String> =
            (get(field) as? List<*>)?.filterIsInstance<String>()?.toSet().orEmpty()

        fun from(doc: DocumentSnapshot?): MuteState {
            if (doc == null || !doc.exists()) return MuteState()
            return MuteState(
                mutedChatIds = doc.stringSet(F_MUTED),
                massActive = doc.getBoolean(F_ACTIVE) ?: false,
                exceptions = doc.stringSet(F_EXCEPTIONS),
                covered = doc.stringSet(F_COVERED),
                includeNew = doc.getBoolean(F_INCLUDE_NEW) ?: true
            )
        }
    }
}


object ChatMuteActions {
    private const val TAG = "ChatMute"

    private fun userRef(uid: String) =
        FirebaseFirestore.getInstance().collection("users").document(uid)

    private fun write(uid: String, data: Map<String, Any>, what: String) {
        userRef(uid).set(data, SetOptions.merge())
            .addOnFailureListener { e -> Log.e(TAG, "Не удалось записать: $what", e) }
    }

    fun enableMassMute(uid: String, currentChatIds: Collection<String>) {
        write(
            uid,
            mapOf(
                MuteState.F_ACTIVE to true,
                MuteState.F_EXCEPTIONS to emptyList<String>(),
                MuteState.F_COVERED to currentChatIds.distinct()
            ),
            "включение массового режима"
        )
    }


    suspend fun enableMassMuteFromServer(uid: String): Boolean {
        val ids = try {
            FirebaseFirestore.getInstance()
                .collection("userChats").document(uid).collection("chats")
                .get(Source.SERVER).await()
                .documents.map { it.id }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Не удалось получить список чатов для массового режима", e)
            return false
        }
        enableMassMute(uid, ids)
        return true
    }

    fun disableMassMute(uid: String) {
        write(
            uid,
            mapOf(
                MuteState.F_ACTIVE to false,
                MuteState.F_EXCEPTIONS to FieldValue.delete(),
                MuteState.F_COVERED to FieldValue.delete()
            ),
            "выключение массового режима"
        )
    }

    fun setChatsMuted(uid: String, chatIds: Collection<String>, muted: Boolean, massActive: Boolean) {
        val ids = chatIds.distinct().toTypedArray()
        if (ids.isEmpty()) return
        val data = mutableMapOf<String, Any>()
        if (muted) {
            data[MuteState.F_MUTED] = FieldValue.arrayUnion(*ids)
            if (massActive) data[MuteState.F_EXCEPTIONS] = FieldValue.arrayRemove(*ids)
        } else {
            data[MuteState.F_MUTED] = FieldValue.arrayRemove(*ids)
            if (massActive) data[MuteState.F_EXCEPTIONS] = FieldValue.arrayUnion(*ids)
        }
        write(uid, data, "мьют чатов")
    }

    fun setMuteNewChatsInMassMode(uid: String, include: Boolean) {
        write(uid, mapOf(MuteState.F_INCLUDE_NEW to include), "настройка новых чатов в режиме")
    }
}

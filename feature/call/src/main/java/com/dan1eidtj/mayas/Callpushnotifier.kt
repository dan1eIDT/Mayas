package com.dan1eidtj.mayas

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.URL
import javax.net.ssl.HttpsURLConnection

sealed class PushNotifyResult {
    data object Success : PushNotifyResult()

    data class Error(
        val message: String
    ) : PushNotifyResult()
}

/**
 * Дёргает
 */
class CallPushNotifier {

    companion object {

        private const val MAX_ATTEMPTS = 3
        private const val RETRY_DELAY_MS = 800L
        private const val CONNECT_TIMEOUT_MS = 15_000
        private const val READ_TIMEOUT_MS = 15_000
    }

    suspend fun notifyIncomingCall(session: CallSession): PushNotifyResult {
        return withContext(Dispatchers.IO) {

            val receiverToken = fetchFcmToken(session.receiverId)

            if (receiverToken.isNullOrEmpty()) {
                Log.e("CallPushNotifier", "Нет FCM токена для receiverId=${session.receiverId}")
                return@withContext PushNotifyResult.Error(
                    "У собеседника нет активного подключения к уведомлениям"
                )
            }

            var lastError: PushNotifyResult.Error? = null

            for (attempt in 1..MAX_ATTEMPTS) {
                val forceTokenRefresh = lastError?.message?.contains("401") == true

                val idToken = fetchIdToken(forceRefresh = forceTokenRefresh)
                if (idToken.isNullOrEmpty()) {
                    return@withContext PushNotifyResult.Error(
                        "Не удалось подтвердить личность для отправки звонка. Попробуйте перезайти в приложение."
                    )
                }

                val result = trySendPush(session, receiverToken, idToken)

                if (result is PushNotifyResult.Success) {
                    return@withContext result
                }

                lastError = result as PushNotifyResult.Error
                Log.w("CallPushNotifier", "Попытка $attempt/$MAX_ATTEMPTS неудачна: ${result.message}")

                if (attempt < MAX_ATTEMPTS) {
                    delay(RETRY_DELAY_MS * attempt)
                }
            }

            lastError ?: PushNotifyResult.Error("Ошибка отправки уведомления о звонке")
        }
    }

    private suspend fun trySendPush(
        session: CallSession,
        receiverToken: String,
        idToken: String
    ): PushNotifyResult {
        return try {
            val url = URL(Configtebeblat.functionUrl)
            val connection = url.openConnection() as HttpsURLConnection

            connection.apply {
                requestMethod = "POST"
                doOutput = true
                doInput = true
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                setRequestProperty("Authorization", "Bearer $idToken")
            }

            val body = JSONObject().apply {
                put("token", receiverToken)
                put("callId", session.callId)
                put("callerId", session.callerId)
                put("callType", session.type.name)
            }

            Log.d("CallPushNotifier", "Connecting...")
            connection.connect()
            Log.d("CallPushNotifier", "Connected!")

            connection.outputStream.use {
                it.write(body.toString().toByteArray(Charsets.UTF_8))
                it.flush()
            }

            val responseCode = connection.responseCode

            if (responseCode !in 200..299) {
                val errorBody = connection.errorStream?.bufferedReader()?.readText()
                Log.e("CallPushNotifier", "HTTP $responseCode: $errorBody")
                connection.disconnect()

                return if (responseCode == 401) {
                    PushNotifyResult.Error("401 Unauthorized")
                } else {
                    PushNotifyResult.Error("Сервер уведомлений недоступен")
                }
            }

            connection.disconnect()
            Log.d("CallPushNotifier", "Push отправлен")
            PushNotifyResult.Success

        } catch (e: SocketTimeoutException) {
            Log.e("CallPushNotifier", "Таймаут соединения с сервером уведомлений", e)
            PushNotifyResult.Error(
                "Не удалось доставить уведомление о звонке. Возможно, собеседник не получит вызов."
            )
        } catch (e: IOException) {
            Log.e("CallPushNotifier", "Сетевая ошибка при отправке пуша", e)
            PushNotifyResult.Error("Сервер уведомлений недоступен")
        } catch (e: Exception) {
            Log.e("CallPushNotifier", "Push notify failed", e)
            PushNotifyResult.Error("Ошибка отправки уведомления о звонке")
        }
    }

    private suspend fun fetchIdToken(forceRefresh: Boolean): String? {
        return try {
            FirebaseAuth.getInstance().currentUser
                ?.getIdToken(forceRefresh)
                ?.await()
                ?.token
        } catch (e: Exception) {
            Log.e("CallPushNotifier", "Не удалось получить ID-токен", e)
            null
        }
    }

    private suspend fun fetchFcmToken(receiverId: String): String? {
        return try {
            val document = FirebaseFirestore.getInstance()
                .collection("users")
                .document(receiverId)
                .get()
                .await()

            document.getString("fcmToken")
        } catch (e: Exception) {
            Log.e("CallPushNotifier", "Не удалось получить FCM токен из Firestore", e)
            null
        }
    }
}
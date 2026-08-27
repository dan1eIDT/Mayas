package com.dan1eidtj.data

import android.content.Context
import android.provider.ContactsContract
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/** Один контакт "как есть" из телефонной книги устройства, ещё до нормализации. */
data class DeviceContact(
    val name: String,
    val rawPhone: String
)

/** Контакт, для которого нашёлся зарегистрированный юзер Маяса. */
data class MatchedContact(
    val uid: String,
    val deviceName: String,
    val name: String,
    val username: String,
    val avatarUrl: String,
    val phone: String
)

/** Контакт из телефонной книги, для которого юзера Маяса не нашли (номер не совпал ни с кем). */
data class UnregisteredContact(
    val deviceName: String,
    val rawPhone: String
)

/**
 * Результат синка: кто из твоих контактов уже в Маяс (с номером, добавленным
 * в профиле), а кого ещё нет — можно позвать через "Пригласить".
 */
data class ContactsSyncResult(
    val onMayas: List<MatchedContact>,
    val notOnMayas: List<UnregisteredContact>
)

object ContactsRepository {

    /**
     * Читает все контакты с номерами из адресной книги устройства.
     * ВАЖНО: разрешение READ_CONTACTS должно быть уже выдано —
     * эта функция сама разрешение не запрашивает и не проверяет.
     * Дергать только из background-потока (см. syncDeviceContacts).
     */
    fun readDeviceContacts(context: Context): List<DeviceContact> {
        val result = mutableListOf<DeviceContact>()
        val resolver = context.contentResolver
        val cursor = resolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            ),
            null, null, null
        )

        cursor?.use {
            val nameIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numberIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            while (it.moveToNext()) {
                val name = if (nameIdx >= 0) it.getString(nameIdx) ?: "" else ""
                val number = if (numberIdx >= 0) it.getString(numberIdx) ?: "" else ""
                if (number.isNotBlank()) {
                    result.add(DeviceContact(name = name, rawPhone = number))
                }
            }
        }
        return result
    }

    /**
     * Матчит нормализованные номера контактов с зарегистрированными юзерами
     * через phoneIndex/{номер} — по одному точечному get() на каждый номер,
     * запущенных параллельно. Это НЕ list-запрос, поэтому работает даже при
     * жёстком "allow list: if false" на коллекции phoneIndex (см. firestore.rules) —
     * иначе пришлось бы делать whereIn по номерам, а это уже позволило бы
     * перебором вытащить вообще все существующие номера в базе.
     * Дороже по числу запросов, чем один whereIn, но безопаснее — и синк
     * контактов не тот сценарий, где это критично по деньгам/скорости.
     */
    suspend fun findRegisteredUsers(normalizedPhones: List<String>): List<MatchedContact> {
        if (normalizedPhones.isEmpty()) return emptyList()
        val db = FirebaseFirestore.getInstance()
        val distinctPhones = normalizedPhones.distinct()

        val phoneToUid: Map<String, String> = coroutineScope {
            distinctPhones.map { phone ->
                async {
                    val doc = db.collection("phoneIndex").document(phone).get().await()
                    phone to doc.getString("uid")
                }
            }.awaitAll()
        }.filter { it.second != null }.associate { it.first to it.second!! }

        if (phoneToUid.isEmpty()) return emptyList()

        val uidToPhone = phoneToUid.entries.associate { it.value to it.key }
        val result = mutableListOf<MatchedContact>()

        // users уже читается всеми signed-in юзерами по общему правилу (не связано
        // с номерами телефонов), так что тут whereIn по documentId — не новая дыра.
        uidToPhone.keys.toList().chunked(10).forEach { batch ->
            val snap = db.collection("users")
                .whereIn(FieldPath.documentId(), batch)
                .get()
                .await()

            snap.documents.forEach { doc ->
                val phone = uidToPhone[doc.id] ?: return@forEach
                result.add(
                    MatchedContact(
                        uid = doc.id,
                        deviceName = "",
                        name = doc.getString("name") ?: doc.getString("username") ?: "Без имени",
                        username = doc.getString("username") ?: "",
                        avatarUrl = doc.getString("avatarUrl") ?: "",
                        phone = phone
                    )
                )
            }
        }
        return result
    }

    /**
     * Полный цикл: читает контакты (в IO-потоке), нормализует номера через
     * PhoneUtils и матчит их с зарегистрированными юзерами. Возвращает ОБА
     * списка — и тех, кто уже в Маяс, и тех, кого позвать (номер не совпал
     * ни с одним юзером — либо не зареган вообще, либо зареган без номера).
     *
     * Важно: "не найден" здесь означает буквально "в поле phone Firestore
     * нет такого номера". Если юзер зарегистрирован, но не добавил номер в
     * профиль (см. ProfileScreen), он попадёт в notOnMayas, даже если
     * реально пользуется приложением — по номеру его найти невозможно.
     */
    suspend fun syncDeviceContacts(context: Context, myUid: String): ContactsSyncResult {
        return withContext(Dispatchers.IO) {
            val deviceContacts = readDeviceContacts(context)

            // Карта "нормализованный номер -> имя в телефонной книге",
            // чтобы после матчинга подставить знакомое пользователю имя контакта.
            val phoneToDeviceName = linkedMapOf<String, String>()
            deviceContacts.forEach { contact ->
                val normalized = PhoneUtils.normalize(contact.rawPhone) ?: return@forEach
                if (!phoneToDeviceName.containsKey(normalized)) {
                    phoneToDeviceName[normalized] = contact.name
                }
            }

            val matched = findRegisteredUsers(phoneToDeviceName.keys.toList())
                .filter { it.uid != myUid }
                .map { it.copy(deviceName = phoneToDeviceName[it.phone] ?: it.name) }

            val matchedPhones = matched.map { it.phone }.toSet()
            val unregistered = phoneToDeviceName
                .filterKeys { it !in matchedPhones }
                .map { (phone, name) -> UnregisteredContact(deviceName = name, rawPhone = phone) }

            ContactsSyncResult(onMayas = matched, notOnMayas = unregistered)
        }
    }
}

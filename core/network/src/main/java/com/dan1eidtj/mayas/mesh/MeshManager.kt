/* Copyright (C) 2026 ProjectIDT */
package com.dan1eidtj.mayas.mesh

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.nearby.Nearby
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Source
import com.google.android.gms.nearby.connection.AdvertisingOptions
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.ConnectionsClient
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo
import com.google.android.gms.nearby.connection.DiscoveryOptions
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import com.google.android.gms.nearby.connection.Strategy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.security.SecureRandom
import android.util.Base64
import java.util.UUID

object MeshManager {
    private const val TAG = "MeshManager"
    private const val SERVICE_ID = "com.dan1eidtj.mayas.mesh"
    private const val NAME_PREFIX = "mayas1"
    private const val MAX_TTL = 5
    private const val MAX_MESSAGES = 200
    private const val MAX_PENDING_DIRECT_MESSAGES = 100
    private const val MAX_PENDING_CONNECTIONS = 8
    private const val MAX_IDENTITY_HANDSHAKE_ATTEMPTS = 5
    private const val MAX_TEXT = 1000
    private const val SEEN_LIMIT = 500
    private const val PING_INTERVAL_MS = 8_000L
    private const val BROADCAST = "*"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val lock = Any()

    private lateinit var appContext: Context
    private lateinit var prefs: MeshPrefs
    private lateinit var client: ConnectionsClient
    private var initialized = false
    private var running = false
    private var pingJob: Job? = null
    private var pendingDirectRetryJob: Job? = null
    private var retryJob: Job? = null
    private var retryAttempt = 0
    @Volatile private var advertisedUid: String? = null
    @Volatile private var blockedUserIds: Set<String> = emptySet()
    @Volatile private var knownContactUids: Set<String> = emptySet()
    private var bluetoothReceiverRegistered = false
    private val registeredIdentityKeys = HashMap<String, String>()
    private val registeringIdentityKeys = HashSet<String>()
    private val pendingIdentityChallenges = HashMap<String, String>()
    private val identityHandshakeAttempts = HashMap<String, Int>()

    private val seen = object : LinkedHashMap<String, Boolean>(SEEN_LIMIT, 0.75f, false) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Boolean>?): Boolean = size > SEEN_LIMIT
    }
    private val pendingPeerNames = HashMap<String, ParsedEndpoint>()
    private val pendingDirectMessages = LinkedHashMap<String, MeshPendingDirectMessage>()

    private val _state = MutableStateFlow(MeshUiState())
    val state: StateFlow<MeshUiState> = _state.asStateFlow()

    fun init(context: Context) {
        synchronized(lock) {
            if (initialized) return
            appContext = context.applicationContext
            prefs = MeshPrefs(appContext)
            client = Nearby.getConnectionsClient(appContext)
            initialized = true
        }
        val restoredMessages = prefs.loadMessages()
        synchronized(lock) {
            restoredMessages.forEach { seen[it.id] = true }
            pendingDirectMessages.clear()
            prefs.loadPendingDirectMessages().forEach { pendingDirectMessages[it.id] = it }
        }
        _state.update {
            it.copy(
                enabled = prefs.enabled,
                autoEnable = prefs.autoEnable,
                ownId = prefs.ownId,
                displayName = prefs.displayName,
                messages = restoredMessages
            )
        }
        registerNetworkCallback()
        registerBluetoothReceiver()
        reevaluate()
    }

    fun setEnabled(enabled: Boolean) {
        if (!initialized) return
        prefs.enabled = enabled
        _state.update { it.copy(enabled = enabled) }
        reevaluate()
    }

    fun setAutoEnable(auto: Boolean) {
        if (!initialized) return
        prefs.autoEnable = auto
        _state.update { it.copy(autoEnable = auto) }
        reevaluate()
    }

    fun setDisplayName(name: String) {
        if (!initialized) return
        prefs.displayName = name
        _state.update { it.copy(displayName = prefs.displayName) }
        if (running) {
            stopInternal()
            reevaluate()
        }
    }

    fun onPermissionsChanged() {
        if (initialized) reevaluate()
    }

    fun setKnownContactUids(uids: Set<String>) {
        knownContactUids = uids.asSequence()
            .filter { it.isNotBlank() && it.length <= 128 }
            .take(5000)
            .toSet()
    }

    private fun isBluetoothEnabled(): Boolean =
        (appContext.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter?.isEnabled == true

    private fun registerBluetoothReceiver() {
        if (bluetoothReceiverRegistered) return
        bluetoothReceiverRegistered = true
        ContextCompat.registerReceiver(
            appContext,
            object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    if (intent.action == BluetoothAdapter.ACTION_STATE_CHANGED) reevaluate()
                }
            },
            IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    /** Generate/register the current device key after Firebase authentication changes. */
    fun refreshIdentity() {
        if (!initialized) return
        val uid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
        if (running && advertisedUid != uid) {
            stopInternal()
            advertisedUid = null
            reevaluate()
        }
        if (uid.isBlank()) return

        val meshId = prefs.ownId
        val registrationId = "$uid:$meshId"
        val shouldRegister = synchronized(lock) {
            if (registeredIdentityKeys.containsKey(registrationId) || registrationId in registeringIdentityKeys) {
                false
            } else {
                registeringIdentityKeys += registrationId
                true
            }
        }
        if (!shouldRegister) return

        // Android Keystore key generation may be slow on first use; never do it on
        // the UI thread from an AuthVM snapshot callback.
        scope.launch(Dispatchers.IO) {
            val publicKey = runCatching { MeshIdentityCrypto.publicKeyBase64(uid, meshId) }
                .onFailure { Log.e(TAG, "Unable to create Mesh device identity key", it) }
                .getOrNull()
            if (publicKey == null || FirebaseAuth.getInstance().currentUser?.uid != uid) {
                synchronized(lock) { registeringIdentityKeys.remove(registrationId) }
                return@launch
            }

            val userRef = FirebaseFirestore.getInstance().collection("users").document(uid)
            fun finishRegistration(success: Boolean) {
                synchronized(lock) {
                    registeringIdentityKeys.remove(registrationId)
                    if (success) registeredIdentityKeys[registrationId] = publicKey
                }
                if (success) {
                    prefs.saveTrustedMeshPublicKey(uid, meshId, publicKey)
                    requestIdentityVerificationForPeers()
                }
            }
            userRef.get(Source.SERVER)
                .addOnSuccessListener { snapshot ->
                    if (!snapshot.exists()) {
                        finishRegistration(false)
                        Log.w(TAG, "Cannot register Mesh key: user profile does not exist")
                        return@addOnSuccessListener
                    }
                    val existing = (snapshot.get("meshDeviceKeys") as? Map<*, *>)?.get(meshId) as? String
                    if (existing == publicKey) {
                        finishRegistration(true)
                    } else {
                        userRef.update(FieldPath.of("meshDeviceKeys", meshId), publicKey)
                            .addOnSuccessListener { finishRegistration(true) }
                            .addOnFailureListener { error ->
                                finishRegistration(false)
                                Log.w(TAG, "Mesh key registration was rejected by Firebase rules", error)
                            }
                    }
                }
                .addOnFailureListener { error ->
                    finishRegistration(false)
                    Log.d(TAG, "Mesh public key registration unavailable; direct identity stays unverified", error)
                }
        }
    }

    fun setBlockedUserIds(userIds: Set<String>) {
        val blocked = userIds.asSequence().filter { it.isNotBlank() }.take(5000).toSet()
        blockedUserIds = blocked
        val removed = synchronized(lock) {
            val ids = pendingDirectMessages.values.filter { it.dstUid in blocked }.map { it.id }
            if (ids.isNotEmpty()) {
                ids.forEach { pendingDirectMessages.remove(it) }
                persistPendingDirectMessagesLocked()
            }
            ids
        }
        if (removed.isNotEmpty()) {
            markDirectMessagesFailed(removed)
        }
    }

    fun clearMessages() {
        _state.update { it.copy(messages = emptyList()) }
        persistMessageHistory()
    }

    fun hasDirectPeer(mayasUid: String): Boolean =
        mayasUid.isNotBlank() && _state.value.isActive &&
            _state.value.peers.any { it.mayasUid == mayasUid && it.identityVerified }

    private fun persistPendingDirectMessagesLocked() {
        runCatching { prefs.savePendingDirectMessages(pendingDirectMessages.values.toList()) }
            .onFailure { Log.w(TAG, "Unable to persist pending Mesh messages", it) }
    }

    private fun queuePendingDirectMessage(message: MeshPendingDirectMessage): Boolean {
        synchronized(lock) {
            if (!pendingDirectMessages.containsKey(message.id) &&
                pendingDirectMessages.size >= MAX_PENDING_DIRECT_MESSAGES
            ) return false
            pendingDirectMessages[message.id] = message
            persistPendingDirectMessagesLocked()
        }
        return true
    }

    private fun removePendingDirectMessage(messageId: String) {
        synchronized(lock) {
            if (pendingDirectMessages.remove(messageId) != null) persistPendingDirectMessagesLocked()
        }
    }

    private fun markDirectMessagesFailed(messageIds: Collection<String>) {
        if (messageIds.isEmpty()) return
        val ids = messageIds.toSet()
        _state.update { current ->
            current.copy(
                messages = current.messages.map { message ->
                    if (message.id in ids && message.privateChat && message.outgoing && !message.delivered) {
                        message.copy(deliveryFailed = true)
                    } else message
                }
            )
        }
        persistMessageHistory()
    }

    private fun ensurePendingDirectRetryLoop() {
        synchronized(lock) {
            if (pendingDirectRetryJob?.isActive == true) return
            pendingDirectRetryJob = scope.launch {
                while (isActive) {
                    if (!running || !_state.value.isActive) {
                        synchronized(lock) { pendingDirectRetryJob = null }
                        return@launch
                    }
                    retryPendingDirectMessages()
                    val remaining = synchronized(lock) {
                        if (pendingDirectMessages.isEmpty()) {
                            pendingDirectRetryJob = null
                            false
                        } else true
                    }
                    if (!remaining) return@launch
                    delay(2_000L)
                }
            }
        }
    }

    private fun retryPendingDirectMessages(forcePeer: MeshPeer? = null) {
        if (!initialized || !running || !_state.value.isActive) return
        val now = System.currentTimeMillis()
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
        if (currentUid.isBlank()) return
        val pending = synchronized(lock) { pendingDirectMessages.values.toList() }
        pending.forEach { item ->
            if (item.dstUid in blockedUserIds) {
                removePendingDirectMessage(item.id)
                markDirectMessagesFailed(listOf(item.id))
                return@forEach
            }
            val queuedPacket = runCatching { JSONObject(item.packetJson) }.getOrNull()
            if (queuedPacket == null || queuedPacket.optString("srcUid") != currentUid) {
                removePendingDirectMessage(item.id)
                markDirectMessagesFailed(listOf(item.id))
                return@forEach
            }
            val peer = _state.value.peers.firstOrNull {
                it.mayasUid == item.dstUid && it.meshId == item.dstMeshId && it.identityVerified &&
                    (forcePeer == null || it.endpointId == forcePeer.endpointId)
            } ?: return@forEach
            if (forcePeer == null && !MeshRetryPolicy.shouldRetry(now, item.lastAttemptAt, item.attemptCount)) return@forEach
            val updated = synchronized(lock) {
                val latest = pendingDirectMessages[item.id] ?: return@synchronized null
                if (latest.attemptCount != item.attemptCount || latest.lastAttemptAt != item.lastAttemptAt) {
                    return@synchronized null
                }
                latest.copy(
                    attemptCount = (latest.attemptCount + 1).coerceAtMost(6),
                    lastAttemptAt = now
                ).also {
                    pendingDirectMessages[item.id] = it
                    persistPendingDirectMessagesLocked()
                }
            } ?: return@forEach
            val packet = runCatching { JSONObject(updated.packetJson) }.getOrNull()
            if (packet == null) {
                removePendingDirectMessage(item.id)
                markDirectMessagesFailed(listOf(item.id))
                Log.w(TAG, "Discarding malformed pending Mesh message ${item.id}")
            } else {
                sendPacket(peer.endpointId, packet)
            }
        }
    }

    fun sendDirectMessage(
        mayasUid: String,
        text: String,
        replyText: String? = null,
        replyName: String? = null
    ): Boolean {
        val clean = text.trim().take(MAX_TEXT)
        val senderUid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
        if (!initialized || !running || !_state.value.isActive || clean.isEmpty() ||
            senderUid.isBlank() || mayasUid.isBlank() || mayasUid == senderUid ||
            mayasUid in blockedUserIds
        ) return false

        val peer = _state.value.peers.firstOrNull { it.mayasUid == mayasUid && it.identityVerified } ?: return false
        val timestamp = System.currentTimeMillis()
        val messageId = UUID.randomUUID().toString()
        val packet = JSONObject().apply {
            put("t", "msg")
            put("id", messageId)
            put("src", prefs.ownId)
            put("srcUid", senderUid)
            put("srcName", prefs.displayName)
            put("dst", peer.meshId)
            put("dstUid", mayasUid)
            put("privateChat", true)
            put("ttl", 1)
            put("hops", 0)
            put("ts", timestamp)
            put("text", clean)
            replyText?.take(500)?.takeIf { it.isNotBlank() }?.let { put("replyToText", it) }
            replyName?.take(80)?.takeIf { it.isNotBlank() }?.let { put("replyToName", it) }
        }
        val queued = MeshPendingDirectMessage(
            id = messageId,
            dstUid = mayasUid,
            dstMeshId = peer.meshId,
            packetJson = packet.toString(),
            createdAt = timestamp
        )
        if (!queuePendingDirectMessage(queued)) return false
        synchronized(lock) { seen[messageId] = true }
        // Persist the outgoing history before the first payload is sent. The retry
        // queue is already durable, so the message can be resent after a process restart.
        appendMessage(
            MeshMessage(
                id = messageId,
                srcId = prefs.ownId,
                srcName = prefs.displayName,
                dst = peer.meshId,
                text = clean,
                timestamp = timestamp,
                outgoing = true,
                hops = 0,
                srcUid = senderUid,
                dstUid = mayasUid,
                privateChat = true,
                identityVerified = peer.identityVerified,
                replyToText = replyText?.take(500)?.takeIf { it.isNotBlank() },
                replyToName = replyName?.take(80)?.takeIf { it.isNotBlank() }
            ),
            sent = true
        )
        retryPendingDirectMessages(forcePeer = peer)
        ensurePendingDirectRetryLoop()
        return true
    }

    private fun appendMessage(message: MeshMessage, sent: Boolean = false, received: Boolean = false) {
        _state.update { current ->
            current.copy(
                messages = (current.messages + message).takeLast(MAX_MESSAGES),
                sentCount = current.sentCount + if (sent) 1 else 0,
                receivedCount = current.receivedCount + if (received) 1 else 0
            )
        }
        persistMessageHistory()
    }

    private fun persistMessageHistory() {
        if (!initialized) return
        runCatching { prefs.saveMessages(_state.value.messages) }
            .onFailure { Log.w(TAG, "Unable to persist local Mesh message history", it) }
    }

    fun sendMessage(dst: String, text: String): Boolean {
        val clean = text.trim().take(MAX_TEXT)
        if (!initialized || clean.isEmpty() || !running) return false
        val peers = _state.value.peers
        if (peers.isEmpty()) return false

        val target = if (dst.isBlank()) BROADCAST else dst
        val packet = JSONObject().apply {
            put("t", "msg")
            put("id", UUID.randomUUID().toString())
            put("src", prefs.ownId)
            put("srcUid", FirebaseAuth.getInstance().currentUser?.uid.orEmpty())
            put("srcName", prefs.displayName)
            put("dst", target)
            put("ttl", MAX_TTL)
            put("hops", 0)
            put("ts", System.currentTimeMillis())
            put("text", clean)
        }
        synchronized(lock) { seen[packet.getString("id")] = true }
        peers.forEach { sendPacket(it.endpointId, packet) }

        val message = MeshMessage(
            id = packet.getString("id"),
            srcId = prefs.ownId,
            srcName = prefs.displayName,
            dst = target,
            text = clean,
            timestamp = packet.getLong("ts"),
            outgoing = true,
            hops = 0,
            srcUid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
        )
        appendMessage(message, sent = true)
        return true
    }

    private fun registerNetworkCallback() {
        val cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        _state.update { it.copy(hasInternet = hasInternet(cm)) }
        try {
            cm.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) = updateInternet(cm)
                override fun onLost(network: Network) = updateInternet(cm)
                override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) = updateInternet(cm)
            })
        } catch (e: Exception) {
            Log.w(TAG, "registerDefaultNetworkCallback failed", e)
        }
    }

    private fun hasInternet(cm: ConnectivityManager): Boolean {
        val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    private fun updateInternet(cm: ConnectivityManager) {
        val online = hasInternet(cm)
        if (_state.value.hasInternet != online) {
            _state.update { it.copy(hasInternet = online) }
            reevaluate()
            if (online) refreshIdentity()
        }
    }

    @Synchronized
    private fun reevaluate() {
        if (!initialized) return
        val s = _state.value
        val reason = when {
            s.enabled -> MeshReason.MANUAL
            s.autoEnable && !s.hasInternet -> MeshReason.AUTO_OFFLINE
            else -> MeshReason.NONE
        }
        if (reason == MeshReason.NONE) {
            cancelRetry(resetAttempt = true)
            if (running) stopInternal()
            _state.update { it.copy(status = MeshStatus.OFF, reason = MeshReason.NONE, lastError = null) }
            return
        }
        if (!MeshPermissions.hasAll(appContext)) {
            cancelRetry(resetAttempt = true)
            if (running) stopInternal()
            _state.update { it.copy(status = MeshStatus.NEEDS_PERMISSION, reason = reason) }
            return
        }
        if (!isBluetoothEnabled()) {
            cancelRetry(resetAttempt = true)
            if (running) stopInternal()
            _state.update { it.copy(status = MeshStatus.NEEDS_BLUETOOTH, reason = reason, lastError = null) }
            return
        }
        if (running) {
            _state.update { it.copy(reason = reason) }
            return
        }
        cancelRetry()
        startInternal(reason)
    }

    private fun localEndpointName(): String {
        val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
        return "$NAME_PREFIX|${prefs.ownId}|$uid|${prefs.displayName.replace("|", " ")}"
    }

    private fun startInternal(reason: MeshReason) {
        advertisedUid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
        running = true
        refreshIdentity()
        _state.update { it.copy(status = MeshStatus.STARTING, reason = reason, lastError = null) }

        val advertising = AdvertisingOptions.Builder().setStrategy(Strategy.P2P_CLUSTER).setLowPower(true).build()
        val discovery = DiscoveryOptions.Builder().setStrategy(Strategy.P2P_CLUSTER).setLowPower(true).build()

        try {
            client.startAdvertising(localEndpointName(), SERVICE_ID, connectionCallback, advertising)
                .addOnSuccessListener {
                    if (!running) {
                        runCatching { client.stopAdvertising() }
                        return@addOnSuccessListener
                    }
                    try {
                        client.startDiscovery(SERVICE_ID, discoveryCallback, discovery)
                            .addOnSuccessListener {
                                if (!running) {
                                    runCatching { client.stopDiscovery() }
                                    return@addOnSuccessListener
                                }
                                cancelRetry(resetAttempt = true)
                                _state.update { it.copy(status = MeshStatus.ACTIVE, lastError = null) }
                                startPingLoop()
                            }
                            .addOnFailureListener { fail(it) }
                    } catch (e: Exception) {
                        fail(e)
                    }
                }
                .addOnFailureListener { fail(it) }
        } catch (e: Exception) {
            fail(e)
        }
    }

    private fun fail(e: Exception) {
        if (!running) return
        Log.e(TAG, "Mesh start failed", e)
        stopInternal()
        val desiredReason = when {
            _state.value.enabled -> MeshReason.MANUAL
            _state.value.autoEnable && !_state.value.hasInternet -> MeshReason.AUTO_OFFLINE
            else -> MeshReason.NONE
        }
        val hasPermissions = MeshPermissions.hasAll(appContext)
        val bluetoothOn = isBluetoothEnabled()
        val nextStatus = when {
            desiredReason != MeshReason.NONE && !hasPermissions -> MeshStatus.NEEDS_PERMISSION
            desiredReason != MeshReason.NONE && !bluetoothOn -> MeshStatus.NEEDS_BLUETOOTH
            desiredReason != MeshReason.NONE -> MeshStatus.RECONNECTING
            else -> MeshStatus.ERROR
        }
        _state.update {
            it.copy(
                status = nextStatus,
                reason = desiredReason,
                lastError = e.message ?: e.javaClass.simpleName
            )
        }
        if (desiredReason != MeshReason.NONE && hasPermissions && bluetoothOn) scheduleRetry()
    }

    private fun scheduleRetry() {
        synchronized(lock) {
            if (!initialized || retryJob?.isActive == true) return
            val delayMs = MeshRetryPolicy.delayMs(retryAttempt)
            retryAttempt = MeshRetryPolicy.nextAttempt(retryAttempt)
            retryJob = scope.launch {
                delay(delayMs)
                synchronized(lock) { retryJob = null }
                reevaluate()
            }
        }
    }

    private fun cancelRetry(resetAttempt: Boolean = false) {
        synchronized(lock) {
            retryJob?.cancel()
            retryJob = null
            if (resetAttempt) retryAttempt = 0
        }
    }

    private fun stopInternal() {
        running = false
        pingJob?.cancel()
        pingJob = null
        try {
            client.stopAdvertising()
            client.stopDiscovery()
            client.stopAllEndpoints()
        } catch (e: Exception) {
            Log.w(TAG, "stop failed", e)
        }
        synchronized(lock) {
            pendingPeerNames.clear()
            pendingIdentityChallenges.clear()
            identityHandshakeAttempts.clear()
        }
        _state.update { it.copy(discovered = emptyList(), pendingConnections = emptyList(), peers = emptyList()) }
    }

    private fun startPingLoop() {
        pingJob?.cancel()
        if (synchronized(lock) { pendingDirectMessages.isNotEmpty() }) ensurePendingDirectRetryLoop()
        pingJob = scope.launch {
            while (isActive) {
                val packet = JSONObject().apply {
                    put("t", "ping")
                    put("ts", System.currentTimeMillis())
                }
                _state.value.peers.forEach { sendPacket(it.endpointId, packet) }
                delay(PING_INTERVAL_MS)
            }
        }
    }

    private data class ParsedEndpoint(val meshId: String, val mayasUid: String?, val name: String)

    private fun parseEndpointName(raw: String): ParsedEndpoint? {
        val parts = raw.split("|", limit = 4)
        if (parts.size != 4 || parts[0] != NAME_PREFIX || parts[1].length != 8) return null
        return ParsedEndpoint(meshId = parts[1], mayasUid = parts[2].ifBlank { null }, name = parts[3])
    }

    private val discoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            val parsed = parseEndpointName(info.endpointName) ?: return
            if (parsed.meshId == prefs.ownId) return
            _state.update {
                it.copy(discovered = it.discovered.filter { d -> d.endpointId != endpointId } +
                    MeshDiscovered(endpointId, parsed.meshId, parsed.name, parsed.mayasUid))
            }
            if (prefs.ownId < parsed.meshId && _state.value.peers.none { it.meshId == parsed.meshId }) {
                client.requestConnection(localEndpointName(), endpointId, connectionCallback)
                    .addOnFailureListener { Log.w(TAG, "requestConnection failed: ${it.message}") }
            }
        }

        override fun onEndpointLost(endpointId: String) {
            _state.update { it.copy(discovered = it.discovered.filter { d -> d.endpointId != endpointId }) }
        }
    }

    fun acceptPendingConnection(endpointId: String) {
        val pending = _state.value.pendingConnections.firstOrNull { it.endpointId == endpointId } ?: return
        if (pending.acceptedByUser) return
        _state.update { current ->
            current.copy(pendingConnections = current.pendingConnections.map { item ->
                if (item.endpointId == endpointId) item.copy(acceptedByUser = true) else item
            })
        }
        client.acceptConnection(endpointId, payloadCallback)
            .addOnFailureListener { error ->
                Log.w(TAG, "acceptConnection failed: ${error.message}")
                synchronized(lock) { pendingPeerNames.remove(endpointId) }
                removePendingConnection(endpointId)
            }
    }

    fun rejectPendingConnection(endpointId: String) {
        synchronized(lock) { pendingPeerNames.remove(endpointId) }
        removePendingConnection(endpointId)
        client.rejectConnection(endpointId)
            .addOnFailureListener { error -> Log.w(TAG, "rejectConnection failed: ${error.message}") }
    }

    private fun removePendingConnection(endpointId: String) {
        _state.update { current ->
            current.copy(pendingConnections = current.pendingConnections.filterNot { it.endpointId == endpointId })
        }
    }

    private val connectionCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
            val parsed = parseEndpointName(info.endpointName)
            if (parsed == null || parsed.meshId == prefs.ownId) {
                client.rejectConnection(endpointId)
                return
            }
            val shouldReject = synchronized(lock) {
                if (pendingPeerNames.size >= MAX_PENDING_CONNECTIONS) {
                    true
                } else {
                    pendingPeerNames[endpointId] = parsed
                    false
                }
            }
            if (shouldReject) {
                client.rejectConnection(endpointId)
                return
            }
            val pending = MeshPendingConnection(
                endpointId = endpointId,
                meshId = parsed.meshId,
                name = parsed.name,
                mayasUid = parsed.mayasUid,
                authenticationDigits = info.authenticationDigits,
                incoming = info.isIncomingConnection
            )
            _state.update { current ->
                current.copy(pendingConnections = current.pendingConnections
                    .filterNot { it.endpointId == endpointId } + pending)
            }
            val knownContact = parsed.mayasUid?.let { it in knownContactUids && it !in blockedUserIds } == true
            if (knownContact) acceptPendingConnection(endpointId)
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            removePendingConnection(endpointId)
            val parsed = synchronized(lock) { pendingPeerNames.remove(endpointId) }
            if (result.status.isSuccess && parsed != null) {
                _state.update {
                    it.copy(
                        peers = it.peers.filter { p -> p.endpointId != endpointId } +
                            MeshPeer(endpointId, parsed.meshId, parsed.name, parsed.mayasUid),
                        discovered = it.discovered.filter { d -> d.endpointId != endpointId }
                    )
                }
                val hello = JSONObject().apply {
                    put("t", "hello")
                    put("src", prefs.ownId)
                    put("srcName", prefs.displayName)
                }
                sendPacket(endpointId, hello)
                synchronized(lock) { identityHandshakeAttempts[endpointId] = 0 }
                beginIdentityHandshake(endpointId)
            }
        }

        override fun onDisconnected(endpointId: String) {
            synchronized(lock) {
                pendingPeerNames.remove(endpointId)
                pendingIdentityChallenges.remove(endpointId)
                identityHandshakeAttempts.remove(endpointId)
            }
            removePendingConnection(endpointId)
            _state.update { it.copy(peers = it.peers.filter { p -> p.endpointId != endpointId }) }
        }
    }

    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            val bytes = payload.asBytes() ?: return
            if (bytes.size > 8192) return
            val packet = try {
                JSONObject(String(bytes, Charsets.UTF_8))
            } catch (e: Exception) {
                return
            }
            handlePacket(endpointId, packet)
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {}
    }

    private fun sendPacket(endpointId: String, packet: JSONObject) {
        client.sendPayload(endpointId, Payload.fromBytes(packet.toString().toByteArray(Charsets.UTF_8)))
            .addOnFailureListener { e ->
                Log.w(TAG, "sendPayload failed: ${e.message}")
                val apiException = e as? com.google.android.gms.common.api.ApiException
                if (apiException?.statusCode == com.google.android.gms.nearby.connection.ConnectionsStatusCodes.STATUS_ENDPOINT_UNKNOWN) {
                    _state.update { it.copy(peers = it.peers.filter { p -> p.endpointId != endpointId }) }
                }
            }
    }

    private fun handlePacket(fromEndpoint: String, packet: JSONObject) {
        when (packet.optString("t")) {
            "hello" -> Unit
            "ping" -> {
                val pong = JSONObject().apply {
                    put("t", "pong")
                    put("ts", packet.optLong("ts"))
                }
                sendPacket(fromEndpoint, pong)
            }
            "pong" -> {
                val rtt = System.currentTimeMillis() - packet.optLong("ts")
                if (rtt in 0..60_000) {
                    _state.update {
                        it.copy(peers = it.peers.map { p -> if (p.endpointId == fromEndpoint) p.copy(pingMs = rtt) else p })
                    }
                }
            }
            "mesh_ack" -> handleDirectMessageAck(fromEndpoint, packet)
            "mesh_identity_challenge" -> handleIdentityChallenge(fromEndpoint, packet)
            "mesh_identity_proof" -> handleIdentityProof(fromEndpoint, packet)
            "msg" -> handleMessage(fromEndpoint, packet)
        }
    }

    private fun requestIdentityVerificationForPeers() {
        val endpointIds = _state.value.peers
            .filter { !it.identityVerified && !it.mayasUid.isNullOrBlank() }
            .map { it.endpointId }
        endpointIds.forEach(::beginIdentityHandshake)
    }

    private fun beginIdentityHandshake(endpointId: String) {
        val peer = _state.value.peers.firstOrNull { it.endpointId == endpointId } ?: return
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
        val peerUid = peer.mayasUid.orEmpty()
        if (currentUid.isBlank() || peerUid.isBlank() || peerUid in blockedUserIds) return
        refreshIdentity()
        val attempt = synchronized(lock) {
            val next = (identityHandshakeAttempts[endpointId] ?: 0) + 1
            identityHandshakeAttempts[endpointId] = next
            next
        }
        if (attempt > MAX_IDENTITY_HANDSHAKE_ATTEMPTS) {
            synchronized(lock) { pendingIdentityChallenges.remove(endpointId) }
            return
        }
        val nonceBytes = ByteArray(32).also { SecureRandom().nextBytes(it) }
        val nonce = Base64.encodeToString(nonceBytes, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
        synchronized(lock) { pendingIdentityChallenges[endpointId] = nonce }
        sendPacket(endpointId, JSONObject().apply {
            put("t", "mesh_identity_challenge")
            put("src", prefs.ownId)
            put("srcUid", currentUid)
            put("dst", peer.meshId)
            put("dstUid", peerUid)
            put("nonce", nonce)
        })
        // Retry lost challenges/proofs as well as missing-key reads. A newer challenge
        // replaces this nonce, preventing an older timeout from starting a parallel retry.
        scope.launch {
            delay(4_000L)
            val stillPending = synchronized(lock) {
                pendingIdentityChallenges[endpointId] == nonce &&
                    (identityHandshakeAttempts[endpointId] ?: 0) < MAX_IDENTITY_HANDSHAKE_ATTEMPTS
            }
            if (stillPending && _state.value.peers.any { it.endpointId == endpointId && !it.identityVerified }) {
                beginIdentityHandshake(endpointId)
            }
        }
    }

    private fun handleIdentityChallenge(endpointId: String, packet: JSONObject) {
        val peer = _state.value.peers.firstOrNull { it.endpointId == endpointId } ?: return
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
        val peerUid = peer.mayasUid.orEmpty()
        val nonce = packet.optString("nonce")
        if (currentUid.isBlank() || peerUid.isBlank() || peerUid in blockedUserIds ||
            packet.optString("src") != peer.meshId || packet.optString("srcUid") != peerUid ||
            packet.optString("dst") != prefs.ownId || packet.optString("dstUid") != currentUid ||
            nonce.length !in 40..64 || !nonce.all { it.isLetterOrDigit() || it == '-' || it == '_' }
        ) return

        val transcript = MeshIdentityCrypto.transcript(
            nonce = nonce,
            srcUid = currentUid,
            srcMeshId = prefs.ownId,
            dstUid = peerUid,
            dstMeshId = peer.meshId
        )
        scope.launch(Dispatchers.IO) {
            val signature = runCatching { MeshIdentityCrypto.sign(currentUid, prefs.ownId, transcript) }
                .onFailure { Log.w(TAG, "Unable to sign Mesh identity proof", it) }
                .getOrNull() ?: return@launch
            if (FirebaseAuth.getInstance().currentUser?.uid != currentUid ||
                _state.value.peers.none { it.endpointId == endpointId && it.meshId == peer.meshId }) return@launch
            sendPacket(endpointId, JSONObject().apply {
                put("t", "mesh_identity_proof")
                put("challenge", nonce)
                put("src", prefs.ownId)
                put("srcUid", currentUid)
                put("dst", peer.meshId)
                put("dstUid", peerUid)
                put("signature", signature)
            })
        }
    }

    private fun handleIdentityProof(endpointId: String, packet: JSONObject) {
        val peer = _state.value.peers.firstOrNull { it.endpointId == endpointId } ?: return
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
        val peerUid = peer.mayasUid.orEmpty()
        val expectedNonce = synchronized(lock) { pendingIdentityChallenges[endpointId] } ?: return
        val challenge = packet.optString("challenge")
        if (currentUid.isBlank() || peerUid.isBlank() || peerUid in blockedUserIds ||
            challenge != expectedNonce || packet.optString("src") != peer.meshId ||
            packet.optString("srcUid") != peerUid || packet.optString("dst") != prefs.ownId ||
            packet.optString("dstUid") != currentUid
        ) return

        val transcript = MeshIdentityCrypto.transcript(
            nonce = challenge,
            srcUid = peerUid,
            srcMeshId = peer.meshId,
            dstUid = currentUid,
            dstMeshId = prefs.ownId
        )
        val signature = packet.optString("signature")
        withTrustedPeerPublicKey(peerUid, peer.meshId) { publicKey ->
            if (publicKey == null) {
                synchronized(lock) { pendingIdentityChallenges.remove(endpointId) }
                val shouldRetry = synchronized(lock) { (identityHandshakeAttempts[endpointId] ?: 0) < MAX_IDENTITY_HANDSHAKE_ATTEMPTS }
                if (shouldRetry) scope.launch {
                    delay(1_500L)
                    if (_state.value.peers.any { it.endpointId == endpointId && !it.identityVerified }) {
                        beginIdentityHandshake(endpointId)
                    }
                }
                return@withTrustedPeerPublicKey
            }
            scope.launch(Dispatchers.IO) {
                val valid = MeshIdentityCrypto.verify(publicKey, transcript, signature)
                synchronized(lock) { pendingIdentityChallenges.remove(endpointId) }
                if (FirebaseAuth.getInstance().currentUser?.uid != currentUid ||
                    _state.value.peers.none { it.endpointId == endpointId && it.mayasUid == peerUid && it.meshId == peer.meshId }) {
                    return@launch
                }
                if (!valid) {
                    _state.update { current -> current.copy(peers = current.peers.map { p ->
                        if (p.endpointId == endpointId) p.copy(identityVerified = false) else p
                    }) }
                    Log.w(TAG, "Mesh identity proof rejected for ${peer.meshId}")
                    return@launch
                }
                var verifiedPeer: MeshPeer? = null
                _state.update { current ->
                    current.copy(peers = current.peers.map { p ->
                        if (p.endpointId == endpointId && p.mayasUid == peerUid && p.meshId == peer.meshId) {
                            p.copy(identityVerified = true).also { verifiedPeer = it }
                        } else p
                    })
                }
                Log.i(TAG, "Mesh device identity verified against Firebase key registry: $peerUid/${peer.meshId}")
                verifiedPeer?.let {
                    retryPendingDirectMessages(forcePeer = it)
                    ensurePendingDirectRetryLoop()
                }
            }
        }
    }

    private fun withTrustedPeerPublicKey(uid: String, meshId: String, callback: (String?) -> Unit) {
        val cached = prefs.loadTrustedMeshPublicKey(uid, meshId)
        if (!_state.value.hasInternet) {
            callback(cached?.takeIf(MeshIdentityCrypto::isValidPublicKey))
            return
        }
        FirebaseFirestore.getInstance().collection("users").document(uid).get(Source.SERVER)
            .addOnSuccessListener { snapshot ->
                val key = (snapshot.get("meshDeviceKeys") as? Map<*, *>)?.get(meshId) as? String
                if (snapshot.exists() && key != null && MeshIdentityCrypto.isValidPublicKey(key)) {
                    prefs.saveTrustedMeshPublicKey(uid, meshId, key)
                    callback(key)
                } else {
                    // A successful server read without this device key is a revocation/missing
                    // registration, so do not silently accept an older local copy.
                    prefs.removeTrustedMeshPublicKey(uid, meshId)
                    callback(null)
                }
            }
            .addOnFailureListener { error ->
                val transientNetworkFailure = error is FirebaseFirestoreException &&
                    error.code in setOf(
                        FirebaseFirestoreException.Code.UNAVAILABLE,
                        FirebaseFirestoreException.Code.DEADLINE_EXCEEDED
                    )
                callback(if (transientNetworkFailure) cached?.takeIf(MeshIdentityCrypto::isValidPublicKey) else null)
            }
    }

    private fun handleDirectMessageAck(fromEndpoint: String, packet: JSONObject) {
        val peer = _state.value.peers.firstOrNull { it.endpointId == fromEndpoint && it.identityVerified } ?: return
        val acknowledgedId = packet.optString("ackFor")
        val senderId = packet.optString("src")
        val senderUid = packet.optString("srcUid")
        if (acknowledgedId.isBlank() || acknowledgedId.length > 80 || senderId != peer.meshId ||
            peer.mayasUid.isNullOrBlank() || senderUid != peer.mayasUid
        ) return
        val queuedMessage = synchronized(lock) { pendingDirectMessages[acknowledgedId] }
        val hasPendingMessage = _state.value.messages.any {
            it.id == acknowledgedId && it.privateChat && it.outgoing && it.dstUid == peer.mayasUid && !it.delivered
        } || (queuedMessage != null && queuedMessage.dstUid == peer.mayasUid && queuedMessage.dstMeshId == peer.meshId)
        if (!hasPendingMessage) return
        _state.update { current ->
            current.copy(
                messages = current.messages.map { message ->
                    if (message.id == acknowledgedId && message.privateChat && message.outgoing &&
                        message.dstUid == peer.mayasUid
                    ) message.copy(
                        delivered = true,
                        deliveryFailed = false,
                        deliveredAt = System.currentTimeMillis(),
                        attempts = queuedMessage?.attemptCount ?: message.attempts
                    ) else message
                }
            )
        }
        persistMessageHistory()
        removePendingDirectMessage(acknowledgedId)
    }

    private fun sendDirectAcknowledgement(endpointId: String, messageId: String) {
        val acknowledgement = JSONObject().apply {
            put("t", "mesh_ack")
            put("ackFor", messageId)
            put("src", prefs.ownId)
            put("srcUid", FirebaseAuth.getInstance().currentUser?.uid.orEmpty())
        }
        sendPacket(endpointId, acknowledgement)
    }

    private fun handleMessage(fromEndpoint: String, packet: JSONObject) {
        val id = packet.optString("id")
        val src = packet.optString("src")
        val dst = packet.optString("dst")
        val rawText = packet.optString("text")
        val text = rawText.take(MAX_TEXT)
        val ttl = packet.optInt("ttl", 0).coerceIn(0, MAX_TTL)
        val hopsFromPacket = packet.optInt("hops", 0)
        val privateChat = packet.optBoolean("privateChat", false)
        val packetSrcUid = packet.optString("srcUid").take(128)
        val packetDstUid = packet.optString("dstUid").take(128)
        val peer = _state.value.peers.firstOrNull { it.endpointId == fromEndpoint } ?: return

        // Reject malformed or oversized payloads before they enter the relay queue.
        if (id.isBlank() || id.length > 80 || text.isBlank() || rawText.length > MAX_TEXT ||
            src.length != 8 || !src.all { it in '0'..'9' || it in 'a'..'f' } ||
            (dst != BROADCAST && (dst.length != 8 || !dst.all { it in '0'..'9' || it in 'a'..'f' })) ||
            ttl !in 1..MAX_TTL || hopsFromPacket !in 0..MAX_TTL || src == prefs.ownId ||
            packetSrcUid.length > 128 || packetDstUid.length > 128
        ) return

        // A direct packet must identify the connected peer as its source. Relayed
        // packets identify the immediate relay in `via`; accept legacy relays without
        // that field for compatibility with older Mayas Mesh clients.
        val via = packet.optString("via")
        if (hopsFromPacket == 0 && src != peer.meshId) return
        if (hopsFromPacket > 0 && via.isNotBlank() && via != peer.meshId) return

        if (privateChat) {
            val currentUid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
            // Local chat messages require a direct, connected Mayas account and are never relayed.
            if (!peer.identityVerified || dst == BROADCAST || dst != prefs.ownId || ttl != 1 || hopsFromPacket != 0 ||
                peer.mayasUid.isNullOrBlank() || packetSrcUid != peer.mayasUid ||
                packetDstUid.isBlank() || packetDstUid != currentUid ||
                packetSrcUid in blockedUserIds
            ) return
        }

        val forMe = dst == prefs.ownId || dst == BROADCAST
        val fresh = synchronized(lock) {
            if (seen.containsKey(id)) false else { seen[id] = true; true }
        }
        if (!fresh) {
            // A recipient may have saved the message but the sender could have lost
            // its ACK. Re-ack duplicates instead of causing endless retransmissions.
            if (privateChat && dst == prefs.ownId) sendDirectAcknowledgement(fromEndpoint, id)
            return
        }

        val hops = hopsFromPacket + 1
        if (forMe) {
            val message = MeshMessage(
                id = id,
                srcId = src,
                srcName = packet.optString("srcName").take(24),
                dst = dst,
                text = text,
                timestamp = packet.optLong("ts", System.currentTimeMillis()),
                outgoing = false,
                hops = hops,
                srcUid = packetSrcUid.ifBlank { peer.mayasUid.orEmpty() },
                dstUid = packetDstUid,
                privateChat = privateChat,
                identityVerified = privateChat && peer.identityVerified,
                replyToText = packet.optString("replyToText").take(500).takeIf { it.isNotBlank() },
                replyToName = packet.optString("replyToName").take(80).takeIf { it.isNotBlank() },
                receivedAt = System.currentTimeMillis()
            )
            appendMessage(message, received = true)
            if (privateChat) sendDirectAcknowledgement(fromEndpoint, id)
        }

        if (!privateChat && dst != prefs.ownId && ttl > 1) {
            val forward = JSONObject(packet.toString()).apply {
                put("ttl", ttl - 1)
                put("hops", hops)
                put("via", prefs.ownId)
            }
            val targets = _state.value.peers.filter { it.endpointId != fromEndpoint }
            targets.forEach { sendPacket(it.endpointId, forward) }
            if (targets.isNotEmpty()) _state.update { it.copy(relayedCount = it.relayedCount + 1) }
        }
    }
}

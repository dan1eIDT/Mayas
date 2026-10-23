/* Copyright (C) 2026 ProjectIDT */
package com.dan1eidtj.mayas.mesh

enum class MeshStatus { OFF, NEEDS_PERMISSION, NEEDS_BLUETOOTH, STARTING, RECONNECTING, ACTIVE, ERROR }

enum class MeshReason { NONE, MANUAL, AUTO_OFFLINE }

data class MeshPendingConnection(
    val endpointId: String,
    val meshId: String,
    val name: String,
    val mayasUid: String?,
    val authenticationDigits: String,
    val incoming: Boolean,
    val acceptedByUser: Boolean = false
)

data class MeshPeer(
    val endpointId: String,
    val meshId: String,
    val name: String,
    val mayasUid: String? = null,
    val pingMs: Long? = null,
    val connectedAt: Long = System.currentTimeMillis(),
    val identityVerified: Boolean = false
)

data class MeshDiscovered(
    val endpointId: String,
    val meshId: String,
    val name: String,
    val mayasUid: String? = null
)

data class MeshPendingDirectMessage(
    val id: String,
    val dstUid: String,
    val dstMeshId: String,
    val packetJson: String,
    val createdAt: Long,
    val attemptCount: Int = 0,
    val lastAttemptAt: Long = 0L
)

data class MeshMessage(
    val id: String,
    val srcId: String,
    val srcName: String,
    val dst: String,
    val text: String,
    val timestamp: Long,
    val outgoing: Boolean,
    val hops: Int,
    val srcUid: String = "",
    val dstUid: String = "",
    val privateChat: Boolean = false,
    val identityVerified: Boolean = false,
    val delivered: Boolean = false,
    val deliveryFailed: Boolean = false,
    val replyToText: String? = null,
    val replyToName: String? = null,
    val receivedAt: Long = 0L,
    val deliveredAt: Long = 0L,
    val attempts: Int = 0
)

data class MeshUiState(
    val enabled: Boolean = false,
    val autoEnable: Boolean = true,
    val status: MeshStatus = MeshStatus.OFF,
    val reason: MeshReason = MeshReason.NONE,
    val hasInternet: Boolean = true,
    val ownId: String = "",
    val displayName: String = "",
    val discovered: List<MeshDiscovered> = emptyList(),
    val pendingConnections: List<MeshPendingConnection> = emptyList(),
    val peers: List<MeshPeer> = emptyList(),
    val messages: List<MeshMessage> = emptyList(),
    val sentCount: Int = 0,
    val receivedCount: Int = 0,
    val relayedCount: Int = 0,
    val lastError: String? = null
) {
    val isActive: Boolean get() = status == MeshStatus.ACTIVE
    val averagePingMs: Long? get() = peers.mapNotNull { it.pingMs }.takeIf { it.isNotEmpty() }?.average()?.toLong()
}

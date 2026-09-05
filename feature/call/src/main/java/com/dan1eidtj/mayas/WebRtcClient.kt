/* Copyright (C) 2026 ProjectIDT */
package com.dan1eidtj.mayas

import android.content.Context
import org.webrtc.AudioSource
import org.webrtc.AudioTrack
import org.webrtc.DefaultVideoDecoderFactory
import org.webrtc.DefaultVideoEncoderFactory
import org.webrtc.EglBase
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription


interface WebRtcClient {

    fun init(listener: Listener)


    fun startLocalAudio()


    fun createOffer()


    fun createAnswer(remoteOfferSdp: String)


    fun setRemoteAnswer(remoteAnswerSdp: String)


    fun addRemoteIceCandidate(candidate: IceCandidateData)


    fun setMuted(muted: Boolean)

    fun close()

    interface Listener {

        fun onLocalIceCandidate(candidate: IceCandidateData)


        fun onLocalOfferCreated(sdp: String)


        fun onLocalAnswerCreated(sdp: String)


        fun onRemoteDescriptionSet()


        fun onIceConnected()


        fun onIceFailed()

        fun onError(message: String)
    }
}

class WebRtcClientImpl(
    private val appContext: Context
) : WebRtcClient {

    private val iceServers = listOf(
        PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer()
    )

    private var eglBase: EglBase? = null
    private var peerConnectionFactory: PeerConnectionFactory? = null
    private var peerConnection: PeerConnection? = null
    private var localAudioSource: AudioSource? = null
    private var localAudioTrack: AudioTrack? = null
    private var listener: WebRtcClient.Listener? = null

    private val closeLock = Any()

    override fun init(listener: WebRtcClient.Listener) {
        close()

        this.listener = listener

        val initOptions = PeerConnectionFactory.InitializationOptions
            .builder(appContext)
            .createInitializationOptions()
        PeerConnectionFactory.initialize(initOptions)

        val eglBaseInstance = EglBase.create()
        eglBase = eglBaseInstance

        peerConnectionFactory = PeerConnectionFactory.builder()
            .setVideoEncoderFactory(DefaultVideoEncoderFactory(eglBaseInstance.eglBaseContext, true, true))
            .setVideoDecoderFactory(DefaultVideoDecoderFactory(eglBaseInstance.eglBaseContext))
            .createPeerConnectionFactory()

        val rtcConfig = PeerConnection.RTCConfiguration(iceServers).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
        }

        peerConnection = peerConnectionFactory?.createPeerConnection(
            rtcConfig,
            object : PeerConnection.Observer {
                override fun onIceCandidate(candidate: IceCandidate) {
                    listener.onLocalIceCandidate(
                        IceCandidateData(candidate.sdpMid, candidate.sdpMLineIndex, candidate.sdp)
                    )
                }

                override fun onIceConnectionChange(newState: PeerConnection.IceConnectionState) {
                    when (newState) {
                        PeerConnection.IceConnectionState.CONNECTED -> listener.onIceConnected()



                        PeerConnection.IceConnectionState.FAILED -> listener.onIceFailed()
                        else -> Unit
                    }
                }

                override fun onAddStream(stream: MediaStream) {



                }

                override fun onSignalingChange(newState: PeerConnection.SignalingState) = Unit
                override fun onIceConnectionReceivingChange(receiving: Boolean) = Unit
                override fun onIceGatheringChange(newState: PeerConnection.IceGatheringState) = Unit
                override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>) = Unit
                override fun onRemoveStream(stream: MediaStream) = Unit
                override fun onDataChannel(dataChannel: org.webrtc.DataChannel) = Unit
                override fun onRenegotiationNeeded() = Unit
                override fun onAddTrack(
                    receiver: org.webrtc.RtpReceiver,
                    streams: Array<out MediaStream>
                ) {






                }
            }
        )
    }

    override fun startLocalAudio() {
        val factory = peerConnectionFactory ?: return
        val audioConstraints = MediaConstraints()
        val source = factory.createAudioSource(audioConstraints)
        val track = factory.createAudioTrack(LOCAL_AUDIO_TRACK_ID, source)
        localAudioSource = source
        localAudioTrack = track
        peerConnection?.addTrack(track, listOf(LOCAL_STREAM_ID))
    }

    override fun createOffer() {
        val connection = peerConnection ?: return

        val constraints = MediaConstraints()

        connection.createOffer(
            object : SdpObserverAdapter() {

                override fun onCreateSuccess(sdp: SessionDescription?) {
                    if (sdp == null) {
                        listener?.onError("createOffer returned null SDP")
                        return
                    }
                    connection.setLocalDescription(
                        object : SdpObserverAdapter() {
                            override fun onSetSuccess() {
                                listener?.onLocalOfferCreated(
                                    sdp.description
                                )
                            }
                            override fun onSetFailure(error: String?) {
                                listener?.onError(
                                    "setLocalDescription OFFER failed: $error"
                                )
                            }
                        },
                        sdp
                    )
                }
                override fun onCreateFailure(error: String?) {
                    listener?.onError(
                        "createOffer failed: $error"
                    )
                }
            },
            constraints
        )
    }

    override fun createAnswer(remoteOfferSdp: String) {
        val connection = peerConnection ?: return


        connection.setRemoteDescription(
            object : SdpObserverAdapter() {
                override fun onSetSuccess() {
                    listener?.onRemoteDescriptionSet()
                    val constraints = MediaConstraints()
                    connection.createAnswer(
                        object : SdpObserverAdapter() {
                            override fun onCreateSuccess(
                                sdp: SessionDescription?
                            ) {
                                if (sdp == null) {
                                    listener?.onError(
                                        "createAnswer returned null SDP"
                                    )
                                    return
                                }
                                connection.setLocalDescription(
                                    object : SdpObserverAdapter() {

                                        override fun onSetSuccess() {

                                            listener?.onLocalAnswerCreated(
                                                sdp.description
                                            )
                                        }
                                        override fun onSetFailure(
                                            error: String?
                                        ) {
                                            listener?.onError(
                                                "setLocalDescription ANSWER failed: $error"
                                            )
                                        }
                                    },
                                    sdp
                                )
                            }
                            override fun onCreateFailure(
                                error: String?
                            ) {
                                listener?.onError(
                                    "createAnswer failed: $error"
                                )
                            }
                        },
                        constraints
                    )
                }
                override fun onSetFailure(
                    error: String?
                ) {
                    listener?.onError(
                        "setRemoteDescription OFFER failed: $error")
                }
            },
            SessionDescription(
                SessionDescription.Type.OFFER,
                remoteOfferSdp
            )
        )
    }

    override fun setRemoteAnswer(remoteAnswerSdp: String) {
        peerConnection?.setRemoteDescription(
            object : SdpObserverAdapter() {
                override fun onSetSuccess() {
                    listener?.onRemoteDescriptionSet()
                }
                override fun onSetFailure(error: String?) {
                    listener?.onError("setRemoteDescription(answer) failed: $error")
                }
            },
            SessionDescription(SessionDescription.Type.ANSWER, remoteAnswerSdp)
        )
    }

    override fun addRemoteIceCandidate(candidate: IceCandidateData) {
        peerConnection?.addIceCandidate(
            IceCandidate(candidate.sdpMid, candidate.sdpMLineIndex, candidate.sdp)
        )
    }

    override fun setMuted(muted: Boolean) {
        localAudioTrack?.setEnabled(!muted)
    }

    override fun close() {
        synchronized(closeLock) {


            if (peerConnectionFactory == null && peerConnection == null && localAudioTrack == null) {
                return
            }

            runCatching { peerConnection?.close() }
                .onFailure { android.util.Log.w(TAG, "peerConnection.close() failed", it) }
            runCatching { peerConnection?.dispose() }
                .onFailure { android.util.Log.w(TAG, "peerConnection.dispose() failed", it) }
            peerConnection = null



            runCatching { localAudioTrack?.dispose() }
                .onFailure { android.util.Log.w(TAG, "localAudioTrack.dispose() failed", it) }
            localAudioTrack = null

            runCatching { localAudioSource?.dispose() }
                .onFailure { android.util.Log.w(TAG, "localAudioSource.dispose() failed", it) }
            localAudioSource = null




            runCatching { peerConnectionFactory?.dispose() }
                .onFailure { android.util.Log.w(TAG, "peerConnectionFactory.dispose() failed", it) }
            peerConnectionFactory = null

            runCatching { eglBase?.release() }
                .onFailure { android.util.Log.w(TAG, "eglBase.release() failed", it) }
            eglBase = null

            listener = null
        }
    }


    private open class SdpObserverAdapter : SdpObserver {
        override fun onCreateSuccess(sdp: SessionDescription?) = Unit
        override fun onSetSuccess() = Unit
        override fun onCreateFailure(error: String?) = Unit
        override fun onSetFailure(error: String?) = Unit
    }

    private companion object {
        const val TAG = "WebRtcClientImpl"
        const val LOCAL_AUDIO_TRACK_ID = "mayas-local-audio"
        const val LOCAL_STREAM_ID = "mayas-local-stream"
    }
}

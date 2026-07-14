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

/**
 * Контракт для WebRTC-слоя. CallManager видит только эти методы и Listener —
 * ничего не знает про PeerConnection, SDP, ICE candidate объекты из org.webrtc.
 */
interface WebRtcClient {

    fun init(listener: Listener)

    /** Создаёт локальный аудио-трек (микрофон) и добавляет его в PeerConnection. */
    fun startLocalAudio()

    /** Инициирует создание SDP offer (вызывает вызывающая сторона). */
    fun createOffer()

    /**
     * Принимает удалённый offer, выставляет его как remote description и создаёт answer
     * (вызывается принимающей стороной после acceptCall()).
     */
    fun createAnswer(remoteOfferSdp: String)

    /** Выставляет полученный answer как remote description (вызывает звонящая сторона). */
    fun setRemoteAnswer(remoteAnswerSdp: String)

    /** Добавляет ICE-кандидата, полученного от собеседника через Firestore. */
    fun addRemoteIceCandidate(candidate: IceCandidateData)

    /** true — заглушить исходящий звук (микрофон), false — вернуть звук. */
    fun setMuted(muted: Boolean)

    fun close()

    interface Listener {
        /** Локальный ICE-кандидат готов — нужно отправить его собеседнику через Firestore. */
        fun onLocalIceCandidate(candidate: IceCandidateData)

        /** Локальный offer создан и уже выставлен как local description — нужно записать в Firestore. */
        fun onLocalOfferCreated(sdp: String)

        /** Локальный answer создан и уже выставлен как local description — нужно записать в Firestore. */
        fun onLocalAnswerCreated(sdp: String)

        /**
         * Remote description (offer или answer) успешно выставлен. До этого момента
         * добавлять ICE-кандидаты от собеседника небезопасно — их нужно копить в очереди.
         */
        fun onRemoteDescriptionSet()

        /** ICE-соединение установлено — можно считать звонок реально CONNECTED. */
        fun onIceConnected()

        /** ICE-соединение окончательно не удалось (FAILED) — временный DISCONNECTED сюда не попадает. */
        fun onIceFailed()

        fun onError(message: String)
    }
}

class WebRtcClientImpl(
    private val appContext: Context
) : WebRtcClient {

    // STUN нужен для NAT traversal между двумя телефонами в разных сетях.
    // Для сценариев за симметричным NAT/корпоративным файрволом STUN не хватит —
    // потребуется свой TURN-сервер (например, coturn). Это уже инфраструктурная
    // задача, отдельная от кода клиента.
    private val iceServers = listOf(
        PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer()
    )

    private var eglBase: EglBase? = null
    private var peerConnectionFactory: PeerConnectionFactory? = null
    private var peerConnection: PeerConnection? = null
    private var localAudioSource: AudioSource? = null
    private var localAudioTrack: AudioTrack? = null
    private var listener: WebRtcClient.Listener? = null

    // close() может прилететь параллельно с двух разных корутин CallManager'а
    // (локальный endCall() и снапшот-листенер, среагировавший на удаление
    // документа звонка практически в то же мгновение — оба висят на
    // managerScope с Dispatchers.IO, то есть на РАЗНЫХ потоках). Без этого лока
    // оба потока могли одновременно попасть в close(), один из них словить
    // исключение на уже disposed нативном объекте (например, второй dispose()
    // localAudioSource) и прерваться ДО peerConnectionFactory?.dispose() —
    // а именно dispose() фабрики реально останавливает захват с микрофона.
    // Итог был: звонок выглядит завершённым, а AudioRecord всё ещё открыт.
    private val closeLock = Any()

    override fun init(listener: WebRtcClient.Listener) {
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
                        // FAILED — действительно тупик, соединение не восстановится само.
                        // DISCONNECTED может быть временным (кратковременная потеря сети) и часто
                        // восстанавливается сам по себе — не рвём звонок сразу на нём.
                        PeerConnection.IceConnectionState.FAILED -> listener.onIceFailed()
                        else -> Unit
                    }
                }

                override fun onAddStream(stream: MediaStream) {
                    // При sdpSemantics = UNIFIED_PLAN этот колбэк ненадёжен (это legacy API
                    // из Plan B) — реальный удалённый трек приходит через onAddTrack ниже.
                    // Оставлен пустым намеренно, не убираем — часть интерфейса Observer.
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
                    // Это актуальный колбэк для получения удалённого трека под
                    // UNIFIED_PLAN (onAddStream выше — legacy Plan B, ненадёжен здесь).
                    // Аудио-трек проигрывается через системный аудио-вывод автоматически
                    // самим WebRTC-движком, вручную подключать AudioTrack не нужно —
                    // но именно сюда, а не в onAddStream, стоит добавлять любую будущую
                    // логику, завязанную на появление удалённого трека (видео-рендер и т.п.).
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
            // Уже закрыто (например, второй параллельный вызов) — выходим тихо,
            // без повторного дёргивания нативных dispose().
            if (peerConnectionFactory == null && peerConnection == null && localAudioTrack == null) {
                return
            }

            // Каждый шаг обёрнут в runCatching намеренно: раньше исключение на любом
            // одном dispose() (например, при гонке с другим потоком) прерывало весь
            // close() и peerConnectionFactory?.dispose() ниже — единственный вызов,
            // который реально останавливает захват с микрофона — просто не выполнялся.
            // Микрофон оставался открытым уже после того, как звонок считался завершённым.
            runCatching { peerConnection?.close() }
                .onFailure { android.util.Log.w(TAG, "peerConnection.close() failed", it) }
            peerConnection = null

            // Трек раньше вообще не диспозился, только зануляется ссылка — отдельная
            // утечка нативного ресурса, чинится тем же патчем.
            runCatching { localAudioTrack?.dispose() }
                .onFailure { android.util.Log.w(TAG, "localAudioTrack.dispose() failed", it) }
            localAudioTrack = null

            runCatching { localAudioSource?.dispose() }
                .onFailure { android.util.Log.w(TAG, "localAudioSource.dispose() failed", it) }
            localAudioSource = null

            // Именно этот dispose() останавливает нативный аудио-движок WebRTC
            // (в т.ч. AudioRecord/захват с микрофона) — должен выполниться всегда,
            // даже если что-то выше упало.
            runCatching { peerConnectionFactory?.dispose() }
                .onFailure { android.util.Log.w(TAG, "peerConnectionFactory.dispose() failed", it) }
            peerConnectionFactory = null

            runCatching { eglBase?.release() }
                .onFailure { android.util.Log.w(TAG, "eglBase.release() failed", it) }
            eglBase = null

            listener = null
        }
    }

    /** SdpObserver с пустыми реализациями по умолчанию — переопределяем только нужное. */
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

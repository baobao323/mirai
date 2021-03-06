package net.mamoe.mirai.utils.io

import io.ktor.network.sockets.Socket
import io.ktor.network.sockets.aSocket
import io.ktor.network.sockets.openReadChannel
import io.ktor.network.sockets.openWriteChannel
import io.ktor.util.KtorExperimentalAPI
import kotlinx.coroutines.io.readAvailable
import kotlinx.io.core.ByteReadPacket
import kotlinx.io.core.Closeable
import kotlinx.io.pool.useInstance
import net.mamoe.mirai.utils.MiraiInternalAPI

/**
 * 多平台适配的 TCP Socket.
 */
@MiraiInternalAPI
actual class PlatformSocket : Closeable {
    @UseExperimental(KtorExperimentalAPI::class)
    lateinit var channel: Socket

    @UseExperimental(KtorExperimentalAPI::class)
    actual val isOpen: Boolean
        get() = channel.socketContext.isActive

    override fun close() = channel.dispose()

    @PublishedApi
    internal val writeChannel = channel.openWriteChannel(true)
    @PublishedApi
    internal val readChannel = channel.openReadChannel()

    /**
     * @throws SendPacketInternalException
     */
    actual suspend inline fun send(packet: ByteReadPacket) {
        writeChannel.writePacket(packet)
    }

    /**
     * @throws ReadPacketInternalException
     */
    actual suspend inline fun read(): ByteReadPacket {
        // do not use readChannel.readRemaining() !!! this function never returns
        ByteArrayPool.useInstance { buffer ->
            val count = readChannel.readAvailable(buffer)
            return buffer.toReadPacket(0, count)
        }
    }

    @UseExperimental(KtorExperimentalAPI::class)
    actual suspend fun connect(serverHost: String, serverPort: Int) {
        channel = aSocket(io.ktor.network.selector.ActorSelectorManager(kotlinx.coroutines.Dispatchers.IO)).tcp().connect(serverHost, serverPort)
    }
}
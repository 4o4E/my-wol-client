package top.e404.mywol

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.NetworkInterface
import java.util.Collections

private const val WOL_PORT = 9
private val INTERFACE_LIST = listOf("wlan", "eth", "tun")

@OptIn(ExperimentalStdlibApi::class)
suspend fun sendMagicPacket(ip: String?, mac: String) {
    // 12个F + 重复16次的主机MAC地址
    val command = "${"F".repeat(12)}${mac.replace(":", "").repeat(16)}".hexToByteArray()

    withContext(Dispatchers.IO) {
        DatagramSocket().use {
            it.send(DatagramPacket(command, command.size, InetAddress.getByName(ip), WOL_PORT))
            getBroadcastAddress().forEach { address ->
                it.send(DatagramPacket(command, command.size, address, WOL_PORT))
            }
        }
    }
}

fun getBroadcastAddress() = runCatching { NetworkInterface.getNetworkInterfaces() }
    .getOrElse { Collections.emptyEnumeration() }
    .asSequence()
    .filter { networkInterface ->
        INTERFACE_LIST.any { networkInterface.name.startsWith(it) }
    }
    .flatMap { it.interfaceAddresses }
    .mapNotNull { it.broadcast }
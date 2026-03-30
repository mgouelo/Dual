package fr.iutvannes.dual.infrastructure

import java.net.NetworkInterface
import java.net.Inet4Address

/**
 * A utility tool that allows you to retrieve the tablet's local IP address.
 */
object Utils {

    /**
     * Returns the tablet's local IP address
     * Returns null if no network connection is active --> localhost is used in this case
     * Allows you to then construct the URL of the type http://ip:8080/
     */
    fun getLocalIpAddress(): String? {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            for (iface in interfaces) {
                if (!iface.isUp || iface.isLoopback) {
                    continue
                }
                for (addr in iface.inetAddresses) {
                    if (addr is Inet4Address && !addr.isLoopbackAddress) {
                        val ip = addr.hostAddress
                        if (!ip.startsWith("169.")) { // This helps prevent phantom IP addresses starting with 169.x.x.x
                            return ip
                        }
                    }
                }
            }
        } catch (_: Exception) { }
        return null
    }
}

/**
 * To ensure the teacher's tablet always has the same IP address when creating a session, you will need to modify:
 * the Raspberry Pi's network configuration file to associate a MAC address with a single IP address.
 */

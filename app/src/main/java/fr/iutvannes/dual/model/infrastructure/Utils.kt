package fr.iutvannes.dual.infrastructure

import java.net.NetworkInterface
import java.net.Inet4Address

object Utils {

    /**
     * Renvoie l’adresse IP locale de la tablette
     * Retourne null si aucune connexion réseau active --> on part sur localhost dans ce cas
     * Permet de construire ensuite l'url du type http://ip:8080/
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
                        if (!ip.startsWith("169.")) { // permet d'éviter les ip fantômes commencant par 169.x.x.x
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
 * Pour que la tablette prof ait toujours la même adresse IP lors de la création de la séance, il faudra modifier
 * le fichier de configuration réseau du Raspberry Pi afin d'associé une adresse MAC <-> à une unique adresse IP.
 */

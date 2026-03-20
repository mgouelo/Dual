package fr.iutvannes.dual.controller.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import fr.iutvannes.dual.infrastructure.server.KtorServer
import fr.iutvannes.dual.infrastructure.Utils

/**
 * The ViewModel is responsible for maintaining and sharing the Ktor server state (active or inactive?)
 * between the different app fragments.
 * Ensures the persistence of this state during configuration changes
 * (tablet rotation, navigation between fragments) and acts as an intermediary between the UI and the server.
 *
 * @see KtorServer
 * @see Utils
 */
class SessionViewModel : ViewModel() {

    /* Variable indicating whether the Ktor server is active or not */
    private val _running = MutableStateFlow(false)

    /* Variable indicating whether the Ktor server is active or not, without the possibility of modification */
    val running: StateFlow<Boolean> = _running

    /* Variable used to determine the Ktor server URL */
    private val _url = MutableStateFlow<String?>(null)

    /* Variable used to determine the URL of the Ktor server without the possibility of modification */
    val url: StateFlow<String?> = _url

    /**
     * Start the Ktor server
     * @param context Application context
     */
    fun startSession(context: Context) {
        KtorServer.start(context.applicationContext)
        val ip = Utils.getLocalIpAddress() ?: "127.0.0.1"
        val url = "http://$ip:8080/"
        _url.value = url
        _running.value = true
    }

    /**
     * Stop the Ktor server
     */
    fun stopSession() {
        if (!_running.value) return
        KtorServer.stop()
        _running.value = false
        _url.value = null
    }
}
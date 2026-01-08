package fr.iutvannes.dual.controller.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import fr.iutvannes.dual.infrastructure.server.KtorServer
import fr.iutvannes.dual.infrastructure.Utils

/**
 * ViewModel responsable de conserver et de partager l’état du serveur Ktor (actif ou non ?)
 * entre les différents fragments de l’app.
 * Assure la persistance de cet état lors des changements de configuration
 * (rotation de la tablette, navigation entre les fragments) et sert d’intermédiaire entre UI et serveur.
 */
class SessionViewModel : ViewModel() {
    private val _running = MutableStateFlow(false)
    val running: StateFlow<Boolean> = _running

    private val _url = MutableStateFlow<String?>(null)
    val url: StateFlow<String?> = _url

    /**
     * Démarre le serveur Ktor
     */
    fun startSession(context: Context) {
        KtorServer.start(context.applicationContext)
        val ip = Utils.getLocalIpAddress() ?: "127.0.0.1"
        val url = "http://$ip:8080/"
        _url.value = url
        _running.value = true
    }

    /**
     * Arrête le serveur Ktor
     */
    fun stopSession() {
        if (!_running.value) return
        KtorServer.stop()
        _running.value = false
        _url.value = null
    }
}
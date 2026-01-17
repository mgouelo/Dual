package fr.iutvannes.dual.infrastructure.server

import android.content.Context
import android.util.Log
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.cio.*

import io.ktor.server.plugins.calllogging.*
import io.ktor.server.plugins.defaultheaders.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.compression.*
import io.ktor.server.plugins.contentnegotiation.*

import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * Event test eleve -> server
 */
@Serializable
data class EventDTO(
    val type: String,
    val studentId: String? = null,
    val payload: JsonObject? = null
)

/**
 * Démarre/arrête le serveur Ktor (HTTP) et installe les plugins json, logs, cors...
 * Injecte les dépendances dans les routes
 */
object KtorServer {
    private var engine: EmbeddedServer<*, *>? = null
    private lateinit var appContext: Context

    fun start(context: Context, port: Int = 8080, wait: Boolean = false) {
        if (engine != null) {
            return
        }
        appContext = context.applicationContext
        engine = embeddedServer(CIO, host = "0.0.0.0", port = port) {
            module(appContext)
        }.also { it.start(wait = wait) }
    }

    fun stop() {
        engine?.stop()
        engine = null
    }
}

// helper MIME
private fun contentTypeFor(path: String): ContentType = when (path.substringAfterLast('.', "")) {
    "html" -> ContentType.Text.Html
    "css"  -> ContentType("text", "css")
    "js"   -> ContentType.Application.JavaScript
    "png"  -> ContentType.Image.PNG
    "jpg", "jpeg" -> ContentType.Image.JPEG
    "svg"  -> ContentType.Image.SVG
    "gif"  -> ContentType.Image.GIF
    "ico"  -> ContentType("image", "x-icon")
    else   -> ContentType.Application.OctetStream
}

/**
 * Modules Ktor
 */
fun Application.module(appContext: Context) {

    install(DefaultHeaders)
    install(CallLogging)
    install(ContentNegotiation) { json() }
    install(CORS) {
        anyHost() // restreins en prod
        allowHeader(HttpHeaders.ContentType)
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
    }
    install(Compression) { gzip() }

    // bus d'évènement pour le temps réel
    val liveBus = MutableSharedFlow<EventDTO>(extraBufferCapacity = 64)

    routing {
        get("/ping") { call.respond(mapOf("status" to "ok")) }

        // URL à mettre dans le QR
        get("/qr-url") {
            val host = call.request.host()
            val port = call.request.port()
            val base = "http://$host${if (port in listOf(80, 443)) "" else ":$port"}"
            call.respond(mapOf("join" to base))
        }

        //Route pour envoyer toutes les classes existantes
        get("/api/classes/all") {
            val classes = DatabaseProvider.db.classeDao().getAllClasses()
            val nomsClasses = classes.map { it.nom }
            call.respond(nomsClasses)
        }

        //Route pour envoyer les élèves d'UNE classe précise
        get("/api/eleves/par-classe/{nomClasse}") {
            val nomClasse = call.parameters["nomClasse"] ?: ""
            val eleves = DatabaseProvider.db.EleveDao().getElevesByClasse(nomClasse)
            val nomsComplets = eleves.map { "${it.prenom} ${it.nom.uppercase()}" }
            call.respond(nomsComplets)
        }

        // Reçoit les événements des élèves
        post("/event") {
            val evt = runCatching { call.receive<EventDTO>() }.getOrElse {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "invalid_body")); return@post
            }
            liveBus.tryEmit(evt)
            call.respond(HttpStatusCode.Accepted, mapOf("status" to "received"))
        }

        // SSE manuel on fait sans module SSE
        get("/live") {
            call.response.cacheControl(CacheControl.NoCache(null))
            call.respondTextWriter(contentType = ContentType.Text.EventStream) {
                fun sendSse(data: String) { write("data: $data\n\n"); flush() }
                sendSse("""{"type":"connected","payload":{"msg":"ready"}}""")
                liveBus.collect { evt ->
                    val payload = evt.payload ?: JsonObject(mapOf("ts" to JsonPrimitive(System.currentTimeMillis())))
                    val sid = evt.studentId ?: ""
                    sendSse("""{"type":"${evt.type}","studentId":"$sid","payload":$payload}""")
                }
            }
        }

        get("/") {
            val bytes = appContext.assets.open("eleve/index.html").use { it.readBytes() }
            call.respondBytes(bytes, contentType = ContentType.Text.Html)
        }

        get("/{path...}") {
            val segments = call.parameters.getAll("path") ?: emptyList()
            val rest = segments.joinToString("/")
            val p = "eleve/$rest"

            runCatching {
                appContext.assets.open(p).use { it.readBytes() }
            }.onSuccess { bytes ->
                call.respondBytes(bytes, contentType = contentTypeFor(p))
            }.onFailure {
                call.respond(HttpStatusCode.NotFound, "Fichier introuvable: $p")
            }
        }
    }
}

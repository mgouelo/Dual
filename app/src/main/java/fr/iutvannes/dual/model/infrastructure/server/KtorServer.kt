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
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Event test student -> server
 */
@Serializable
data class EventDTO(
    val type: String,
    val studentId: String? = null,
    val payload: JsonObject? = null
)

/**
 * Starts/stops the Ktor server (HTTP) and installs the JSON, logs, CORS plugins...
 * Injects dependencies into routes
 */
object KtorServer {

    /* Variable for the server engine */
    private var engine: EmbeddedServer<*, *>? = null

    /* Variable for the application context */
    private lateinit var appContext: Context

    /**
     * Starts the server
     *
     * @param context the application context
     * @param port the port to listen on
     * @param wait if true, the server will block the current thread
     */
    fun start(context: Context, port: Int = 8080, wait: Boolean = false) {
        if (engine != null) {
            return
        }
        appContext = context.applicationContext
        engine = embeddedServer(CIO, host = "0.0.0.0", port = port) {
            module(appContext)
        }.also { it.start(wait = wait) }
    }

    /**
     * Stops the server
     */
    fun stop() {
        engine?.stop()
        engine = null
    }
}

// helper MIME
/**
 * Returns the content type for the given path
 *
 * @param path the path to analyze
 * @return the content type
 */
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
 *
 * @param appContext the application context
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

    // Event bus for real time
    val liveBus = MutableSharedFlow<EventDTO>(extraBufferCapacity = 64)

    routing {
        // Route to check if the server is running
        get("/ping") { call.respond(mapOf("status" to "ok")) }

        // URL to put in the QR code
        get("/qr-url") {
            val host = call.request.host()
            val port = call.request.port()
            val base = "http://$host${if (port in listOf(80, 443)) "" else ":$port"}"
            call.respond(mapOf("join" to base))
        }

        // Route to send all existing classes
        get("/api/classes/all") {
            val classes = DatabaseProvider.db.classeDao().getAllClasses()
            val nomsClasses = classes.map { it.nom }
            call.respond(nomsClasses)
        }

        // Route to send students from ONE specific class
        get("/api/eleves/par-classe/{nomClasse}") {
            val nomClasse = call.parameters["nomClasse"] ?: ""
            val eleves = DatabaseProvider.db.EleveDao().getElevesByClasse(nomClasse)
            val nomsComplets = eleves.map { "${it.prenom} ${it.nom.uppercase()}" }
            call.respond(nomsComplets)
        }

        // Receives student events
        post("/event") {
            try {
                // We receive the raw text to avoid Serializer errors
                val body = call.receiveText()
                Log.d("KtorServer", "Texte brut reçu : $body")

                // Manual analysis of the JSON
                val jsonParser = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
                val jsonElement = jsonParser.parseToJsonElement(body).jsonObject

                val type = jsonElement["type"]?.jsonPrimitive?.content ?: ""
                val studentId = jsonElement["studentId"]?.jsonPrimitive?.content ?: ""

                // Business logic: We process the shot
                if (type == "TIR_RESULTAT_6EME") {
                    val payload = jsonElement["payload"]?.jsonObject
                    val scoreRaw = payload?.get("total")?.jsonPrimitive?.content ?: "0"
                    val scoreInt = scoreRaw.toIntOrNull() ?: 0

                    // We separate First Name and Last Name
                    val parts = studentId.split(" ")
                    val prenom = parts.getOrNull(0) ?: ""
                    val nom = parts.getOrNull(1) ?: ""

                    // Database insertion
                    val eleve = DatabaseProvider.db.EleveDao().findByName(prenom, nom.uppercase())
                    if (eleve != null) {
                        val nouveauResultat = fr.iutvannes.dual.model.persistence.Resultat(
                            id_eleve = eleve.id_eleve,
                            id_seance = 1,
                            cibles_touchees = scoreInt,
                            temp_course = 0F
                        )
                        DatabaseProvider.db.resultatDao().insert(nouveauResultat)
                        Log.i("KtorServer", "RÉUSSITE : $studentId enregistré avec score $scoreInt")
                    } else {
                        Log.e("KtorServer", "ÉLÈVE NON TROUVÉ en BDD : $prenom $nom")
                    }
                }

                call.respond(HttpStatusCode.Accepted, mapOf("status" to "OK"))

            } catch (e: Exception) {
                Log.e("KtorServer", "Erreur critique route event : ${e.message}")
                call.respond(HttpStatusCode.InternalServerError, mapOf("status" to "error"))
            }
        }

        // Route to send real time events
        get("/") {
            val bytes = appContext.assets.open("eleve/index.html").use { it.readBytes() }
            call.respondBytes(bytes, contentType = ContentType.Text.Html)
        }

        // Route to send real time events
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

        // CSV export route for the teacher
        get("/api/admin/export") {
            try {
                // Retrieving data from the Room database
                val resultats = DatabaseProvider.db.resultatDao().getAllResultats()

                // CSV Content Construction
                val csv = StringBuilder("prenom;nom;genre;cibles_touchees\n")

                resultats.forEach { res ->
                    val eleve = DatabaseProvider.db.EleveDao().getEleveById(res.id_eleve)
                    if (eleve != null) {
                        val prenom = eleve.prenom
                        val nom = eleve.nom.uppercase()
                        val genre = eleve.genre
                        val score = res.cibles_touchees

                        csv.append("$prenom;$nom;$genre;$score\n")
                    }
                }

                // Configuring Headers to Trigger Download
                call.response.header(
                    HttpHeaders.ContentDisposition,
                    ContentDisposition.Attachment.withParameter(
                        ContentDisposition.Parameters.FileName, "resultats_biathlon.csv"
                    ).toString()
                )

                // Sending the reply
                call.respondText(csv.toString(), ContentType.Text.CSV)

            } catch (e: Exception) {
                Log.e("KtorServer", "Erreur Export CSV: ${e.message}")
                call.respond(HttpStatusCode.InternalServerError, "Erreur lors de la génération du fichier")
            }
        }
    }
}

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
import kotlinx.coroutines.Dispatchers

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Event test eleve -> server
 */
@Serializable
data class EventDTO(
    val type: String,
    val studentId: String? = null,
    val payload: JsonObject? = null
)
@Serializable
data class VmaUpdate(val id: Int, val vma: Float)

@Serializable
data class EleveDTO(
    val id_eleve: Int,
    val nomComplet: String,
    val genre: String,
    val vma: Float
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

        // Route pour envoyer les élèves d'une classe donnée (paramètre dans l'URL)
        get("/api/eleves/par-classe/{nomClasse}") {
            val nom = call.parameters["nomClasse"] ?: ""
            Log.d("KtorDebug", "Requête reçue pour la classe : $nom") // Log de début

            try {
                val eleves = withContext(Dispatchers.IO) {
                    DatabaseProvider.db.EleveDao().getElevesByClasse(nom)
                }

                Log.d("KtorDebug", "Nombre d'élèves trouvés en BDD : ${eleves.size}") // Vérifie si la BDD est vide

                val dataEleves = eleves.map {
                    Log.d("KtorDebug", "Traitement de : ${it.prenom} (VMA: ${it.vma})") // Vérifie les valeurs individuelles
                    EleveDTO(
                        id_eleve = it.id_eleve,
                        nomComplet = "${it.prenom} ${it.nom.uppercase()}",
                        genre = it.genre,
                        vma = it.vma
                    )
                }
                call.respond(dataEleves)
            } catch (e: Exception) {
                Log.e("KtorServer", "Erreur critique route event : ${e.message}")
                call.respond(HttpStatusCode.InternalServerError)
            }
        }

        post("/api/eleves/update-vma") {
            val req = call.receive<VmaUpdate>() // Ktor convertit le JSON direct en objet

            val rows = DatabaseProvider.db.EleveDao().updateVma(req.id, req.vma)

            if (rows > 0) {
                call.respond(HttpStatusCode.OK)
            } else {
                call.respond(HttpStatusCode.NotFound)
            }
        }

        //Reçoit les événements des élèves
        post("/event") {
            try {
                //On reçoit le texte brut pour éviter les erreurs de Serializer
                val body = call.receiveText()
                Log.d("KtorServer", "Texte brut reçu : $body")

                //Analyse manuelle du JSON
                val jsonParser = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
                val jsonElement = jsonParser.parseToJsonElement(body).jsonObject

                val type = jsonElement["type"]?.jsonPrimitive?.content ?: ""
                val studentId = jsonElement["studentId"]?.jsonPrimitive?.content ?: ""

                //Logique métier : On traite le tir
                if (type == "TIR_RESULTAT_6EME") {
                    val payload = jsonElement["payload"]?.jsonObject
                    val scoreRaw = payload?.get("total")?.jsonPrimitive?.content ?: "0"
                    val scoreInt = scoreRaw.toIntOrNull() ?: 0

                    //On sépare Prénom et Nom
                    val parts = studentId.split(" ")
                    val prenom = parts.getOrNull(0) ?: ""
                    val nom = parts.getOrNull(1) ?: ""

                    //Insertion bdd
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

        // Route d'export CSV pour le professeur
        get("/api/admin/export") {
            try {
                //Récupération des données depuis la base Room
                val resultats = DatabaseProvider.db.resultatDao().getAllResultats()

                //Construction du contenu CSV
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

                //Configuration des Headers pour déclencher le téléchargement
                call.response.header(
                    HttpHeaders.ContentDisposition,
                    ContentDisposition.Attachment.withParameter(
                        ContentDisposition.Parameters.FileName, "resultats_biathlon.csv"
                    ).toString()
                )

                //Envoi de la réponse
                call.respondText(csv.toString(), ContentType.Text.CSV)

            } catch (e: Exception) {
                Log.e("KtorServer", "Erreur Export CSV: ${e.message}")
                call.respond(HttpStatusCode.InternalServerError, "Erreur lors de la génération du fichier")
            }
        }
    }
}


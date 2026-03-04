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
 * Event test student -> server
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
    val vma: Float?
)

/**
 * Starts/stops the Ktor server (HTTP) and installs the JSON, logs, CORS plugins...
 * Injects dependencies into routes
 */
object KtorServer {

    var idSeanceActuelle: Int = 0

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

                val parts = studentId.split(" ")
                val prenom = parts.getOrNull(0) ?: ""
                val nom = parts.getOrNull(1) ?: ""

                when (type) {
                    "TIR_RESULTAT_6EME" -> {
                        val payload = jsonElement["payload"]?.jsonObject
                        val scoreRaw = payload?.get("total")?.jsonPrimitive?.content ?: "0"
                        val scoreInt = scoreRaw.toIntOrNull() ?: 0

                        val eleve = DatabaseProvider.db.EleveDao().findByName(prenom, nom.uppercase())
                        if (eleve != null) {
                            val nouveauResultat = fr.iutvannes.dual.model.persistence.Resultat(
                                id_eleve = eleve.id_eleve,
                                id_seance = KtorServer.idSeanceActuelle,
                                cibles_touchees = scoreInt,
                                temp_course = 0F
                            )
                            DatabaseProvider.db.resultatDao().insert(nouveauResultat)
                            Log.i("KtorServer", "RÉUSSITE : $studentId enregistré avec score $scoreInt")
                            call.respond(HttpStatusCode.Accepted, mapOf("status" to "OK"))
                        } else {
                            Log.e("KtorServer", "ÉLÈVE NON TROUVÉ : $prenom $nom")
                            call.respond(HttpStatusCode.NotFound, mapOf("error" to "Eleve non trouvé"))
                        }
                    }

                    "VMA_RESULTAT" -> {
                        val payload = jsonElement["payload"]?.jsonObject
                        val vmaValue = payload?.get("vma")?.jsonPrimitive?.content?.toFloatOrNull() ?: 0f

                        val eleve = DatabaseProvider.db.EleveDao().findByName(prenom, nom.uppercase())
                        if (eleve != null) {
                            //Mise à jour de la VMA sur la fiche de l'élève
                            eleve.vma = vmaValue
                            DatabaseProvider.db.EleveDao().update(eleve)

                            //On crée une ligne dans la table Resultat liée à l'idSeanceActuelle
                            val marquageResultat = fr.iutvannes.dual.model.persistence.Resultat(
                                id_eleve = eleve.id_eleve,
                                id_seance = KtorServer.idSeanceActuelle,
                                vma = vmaValue, //On stocke la VMA ici pour l'historique de la séance
                                cibles_touchees = 0, //Pas de tir en Test VMA
                                temp_course = 0F
                            )
                            DatabaseProvider.db.resultatDao().insert(marquageResultat)

                            Log.i("KtorServer", "Test VMA enregistré : $studentId -> $vmaValue km/h")
                            call.respond(HttpStatusCode.Accepted, mapOf("status" to "VMA_OK"))
                        } else {
                            Log.e("KtorServer", "ÉLÈVE NON TROUVÉ : $prenom $nom")
                            call.respond(HttpStatusCode.NotFound, mapOf("error" to "Eleve non trouvé"))
                        }
                    }

                    else -> {
                        Log.w("KtorServer", "Type d'événement inconnu : $type")
                        call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Unknown type"))
                    }
                }

            } catch (e: Exception) {
                Log.e("KtorServer", "Erreur critique route event : ${e.message}")
                // On vérifie si une réponse n'a pas déjà été envoyée avant d'envoyer l'erreur
                if (!call.response.isCommitted) {
                    call.respond(HttpStatusCode.InternalServerError, mapOf("status" to "error"))
                }
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
                val idActuel = KtorServer.idSeanceActuelle
                val seance = DatabaseProvider.db.seanceDao().getSeanceById(idActuel)

                if (seance == null) {
                    call.respond(HttpStatusCode.NotFound, "Aucune séance active.")
                    return@get
                }

                //Nom du fichier dynamique
                val dateClean = seance.date.replace("/", "-").replace(":", "h").replace(" ", "_")
                val nomFichier = "Bilan_${seance.type}_${seance.classe}_$dateClean.csv"

                //Récupération des résultats
                val resultats = DatabaseProvider.db.resultatDao().getBySeance(idActuel)
                val csv = StringBuilder()

                //Personnalisation du contenu selon le type de séance
                when (seance.type) {
                    "Épreuve Finale" -> {
                        csv.append("BILAN ÉVALUATION FINALE - CLASSE : ${seance.classe}\n")
                        csv.append("Date;${seance.date}\n\n")
                        csv.append("Nom;Prenom;Genre;Cibles;VMA;Note Finale;Classement\n") //Colonnes complètes

                        resultats.forEach { res ->
                            val eleve = DatabaseProvider.db.EleveDao().getEleveById(res.id_eleve)
                            if (eleve != null) {
                                csv.append("${eleve.nom.uppercase()};${eleve.prenom};${eleve.genre};${res.cibles_touchees};${eleve.vma};${res.note_finale};${res.classement}\n")
                            }
                        }
                    }
                    "Test VMA" -> {
                        csv.append("RÉSULTATS TEST VMA - CLASSE : ${seance.classe}\n")
                        csv.append("Date;${seance.date}\n\n")
                        csv.append("Nom;Prenom;VMA (km/h)\n") //Uniquement l'essentiel pour le test VMA

                        resultats.forEach { res ->
                            val eleve = DatabaseProvider.db.EleveDao().getEleveById(res.id_eleve)
                            if (eleve != null) {
                                csv.append("${eleve.nom.uppercase()};${eleve.prenom};${eleve.vma}\n")
                            }
                        }
                    }
                    else -> { //Mode Entraînement par défaut
                        csv.append("SUIVI ENTRAÎNEMENT - CLASSE : ${seance.classe}\n")
                        csv.append("Date;${seance.date}\n\n")
                        csv.append("Nom;Prenom;Cibles Touchees;VMA\n")

                        resultats.forEach { res ->
                            val eleve = DatabaseProvider.db.EleveDao().getEleveById(res.id_eleve)
                            if (eleve != null) {
                                csv.append("${eleve.nom.uppercase()};${eleve.prenom};${res.cibles_touchees};${eleve.vma}\n")
                            }
                        }
                    }
                }

                // Configuring Headers to Trigger Download
                call.response.header(
                    HttpHeaders.ContentDisposition,
                    ContentDisposition.Attachment.withParameter(
                        ContentDisposition.Parameters.FileName, nomFichier
                    ).toString()
                )

                // Sending the reply
                call.respondText(csv.toString(), ContentType.Text.CSV)

            } catch (e: Exception) {
                Log.e("KtorServer", "Erreur Export: ${e.message}")
                call.respond(HttpStatusCode.InternalServerError, "Erreur génération CSV")
            }
        }
    }
}
package fr.iutvannes.dual.infrastructure.server

import android.content.Context
import android.util.Log
import fr.iutvannes.dual.model.persistence.Resultat
import fr.iutvannes.dual.model.persistence.Seance
import fr.iutvannes.dual.model.utils.DatabaseProvider
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
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.double
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

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
    val vma: Float?,
    val vma_distance: Int?,
    val vma_badge: String?,
    val vma_parcours: String?
)

/**
 * Données du biathlon
 */
@Serializable
data class BiathlonRequest(
    val prenom: String,
    val nom: String,
    val distance: Double,
    val nbTours: Int,
    val nbTirsReussi: List<Int>,
    val tempsAuPasDeTir: List<Int>,
    val tempsAuTour: List<Int>
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

    val ressentis = mutableMapOf<Int, Triple<String, String, String>>()

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

        get("/api/seance/active") {
            val idActuel = KtorServer.idSeanceActuelle
            if (idActuel == 0) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "Aucune séance active"))
                return@get
            }
            val seance = withContext(Dispatchers.IO) {
                DatabaseProvider.db.seanceDao().getSeanceById(idActuel)
            }
            if (seance != null) {
                call.respond(mapOf(
                    "classe" to seance.classe,
                    "type" to seance.type
                ))
            } else {
                call.respond(HttpStatusCode.NotFound)
            }
        }

        // Route to send all existing classes
        get("/api/classes/all") {
            val classes = withContext(Dispatchers.IO) {
                DatabaseProvider.db.classeDao().getAllClasses()
            }
            val nomsClasses = classes.map { it.nom }
            call.respond(nomsClasses)
        }

        // Route to send students from ONE specific class
        get("/api/eleves/par-classe/{nomClasse}") {
            val nom = call.parameters["nomClasse"] ?: ""
            try {
                val eleves = withContext(Dispatchers.IO) {
                    DatabaseProvider.db.EleveDao().getElevesByClasse(nom)
                }
                val dataEleves = eleves.map {
                    EleveDTO(
                        id_eleve = it.id_eleve,
                        nomComplet = "${it.prenom} ${it.nom.uppercase()}",
                        genre = it.genre,
                        vma = it.vma,
                        vma_distance = it.vma?.let { vma ->
                            when {
                                vma <= 10f -> 250
                                vma <= 11f -> 275
                                vma <= 12f -> 300
                                vma <= 13f -> 325
                                vma <= 14f -> 350
                                vma <= 15f -> 375
                                else       -> 400
                            }
                        },
                        vma_badge = it.vma?.let { vma ->
                            when {
                                vma <= 10f -> "bg-jaune"
                                vma <= 11f -> "bg-vert"
                                vma <= 12f -> "bg-bleu"
                                vma <= 13f -> "bg-bleu"
                                vma <= 14f -> "bg-rouge"
                                vma <= 15f -> "bg-rouge"
                                else       -> "bg-noir"
                            }
                        },
                        vma_parcours = it.vma?.let { vma ->
                            when {
                                vma <= 10f -> "Coupelles Jaunes (250m)"
                                vma <= 11f -> "Plots Verts (275m)"
                                vma <= 12f -> "Coupelles Bleues (300m)"
                                vma <= 13f -> "Plots Bleus (325m)"
                                vma <= 14f -> "Coupelles Rouges (350m)"
                                vma <= 15f -> "Plots Rouges (375m)"
                                else       -> "Grand Tour (400m)"
                            }
                        }
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

            val rows = withContext(Dispatchers.IO) {
                DatabaseProvider.db.EleveDao().updateVma(req.id, req.vma)
            }

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
                    "RESULTAT_EPREUVE_FINALE" -> {
                        val payload = jsonElement["payload"]?.jsonObject
                        val noteFinale = payload?.get("note_finale")?.jsonPrimitive?.content?.toFloatOrNull() ?: 0f
                        val cibles = payload?.get("cibles_touchees")?.jsonPrimitive?.content?.toIntOrNull() ?: 0
                        val vmaRealisee = payload?.get("vma_realisee")?.jsonPrimitive?.content?.toFloatOrNull() ?: 0f
                        val nbTours = payload?.get("nb_tours")?.jsonPrimitive?.content?.toIntOrNull() ?: 0
                        val ecartRegul = payload?.get("ecart_max_course")?.jsonPrimitive?.content?.toIntOrNull() ?: 0
                        val ressentiIntensite = payload?.get("ressenti_intensite")?.jsonPrimitive?.content ?: ""
                        val ressentiDurer = payload?.get("ressenti_durer")?.jsonPrimitive?.content ?: ""
                        val ressentiLucidite = payload?.get("ressenti_lucidite")?.jsonPrimitive?.content ?: ""
                        val tempsA = payload?.get("temps_A")?.jsonPrimitive?.content?.toIntOrNull() ?: 0
                        val tempsB = payload?.get("temps_B")?.jsonPrimitive?.content?.toIntOrNull() ?: 0
                        val tempsC = payload?.get("temps_C")?.jsonPrimitive?.content?.toIntOrNull() ?: 0
                        val tempsD = payload?.get("temps_D")?.jsonPrimitive?.content?.toIntOrNull() ?: 0
                        val tempsE = payload?.get("temps_E")?.jsonPrimitive?.content?.toIntOrNull() ?: 0
                        val tir1 = payload?.get("tir1")?.jsonPrimitive?.content?.toIntOrNull() ?: 0
                        val tir2 = payload?.get("tir2")?.jsonPrimitive?.content?.toIntOrNull() ?: 0
                        val noteIntensite = payload?.get("note_intensite")?.jsonPrimitive?.content?.toFloatOrNull() ?: 0f
                        val noteEfficience = payload?.get("note_efficience")?.jsonPrimitive?.content?.toFloatOrNull() ?: 0f
                        val noteVma = payload?.get("note_vma")?.jsonPrimitive?.content?.toFloatOrNull() ?: 0f
                        val toursArray = payload?.get("tours")?.jsonArray

                        val eleve = withContext(Dispatchers.IO) {
                            DatabaseProvider.db.EleveDao().findByName(prenom, nom.uppercase())
                        }

                        Log.d("KtorServer", "Recherche élève: prenom='$prenom' nom='${nom.uppercase()}' → trouvé: ${eleve != null}")

                        if (eleve != null) {
                            val resultatEpreuve = Resultat(
                                id_eleve = eleve.id_eleve,
                                id_seance = KtorServer.idSeanceActuelle,
                                cibles_touchees = cibles,
                                temp_course = vmaRealisee,
                                note_finale = noteFinale,
                                nbTours = nbTours,
                                ecart_max_course = ecartRegul,
                                temps_A = tempsA,
                                temps_B = tempsB,
                                temps_C = tempsC,
                                temps_D = tempsD,
                                temps_E = tempsE,
                                tir1 = tir1,
                                tir2 = tir2,
                                note_intensite = noteIntensite,
                                note_efficience = noteEfficience,
                                note_vma = noteVma,
                                ressenti_intensite = ressentiIntensite,
                                ressenti_durer     = ressentiDurer,
                                ressenti_lucidite  = ressentiLucidite
                            )

                            withContext(Dispatchers.IO) {
                                // 1. On cherche si l'élève a DEJA une note pour cette séance
                                val existant = DatabaseProvider.db.resultatDao().getResultatByEleveEtSeance(eleve.id_eleve, KtorServer.idSeanceActuelle)

                                if (existant != null) {
                                    // S'il a déjà une note, on LA MET A JOUR (on écrase pour garder la dernière tentative)
                                    existant.cibles_touchees    = cibles
                                    existant.temp_course        = vmaRealisee
                                    existant.note_finale        = noteFinale
                                    existant.nbTours            = nbTours        // Indispensable pour la 6ème !
                                    existant.ecart_max_course   = ecartRegul     // Indispensable pour la 6ème !
                                    existant.temps_A            = tempsA
                                    existant.temps_B            = tempsB
                                    existant.temps_C            = tempsC
                                    existant.temps_D            = tempsD
                                    existant.temps_E            = tempsE
                                    existant.tir1               = tir1
                                    existant.tir2               = tir2
                                    existant.note_intensite     = noteIntensite
                                    existant.note_efficience    = noteEfficience
                                    existant.note_vma           = noteVma
                                    existant.ressenti_intensite = ressentiIntensite
                                    existant.ressenti_durer     = ressentiDurer
                                    existant.ressenti_lucidite  = ressentiLucidite
                                    DatabaseProvider.db.resultatDao().update(existant)
                                } else {
                                    // Sinon, c'est sa première tentative, on l'insère.
                                    DatabaseProvider.db.resultatDao().insert(resultatEpreuve)
                                }

                                // 2. Gestion des temps par tours (Tableaux de détails)
                                if (toursArray != null) {
                                    // Si l'élève avait déjà couru, on efface ses anciens tours pour ne pas fausser le graphique/historique
                                    if (existant != null) {
                                        val anciennesCourses = DatabaseProvider.db.courseDao()
                                            .getCoursesBySeanceEtEleve(KtorServer.idSeanceActuelle, eleve.id_eleve)
                                        anciennesCourses.forEach { courseAvecTours ->
                                            courseAvecTours.liste_tours.forEach { tour ->
                                                DatabaseProvider.db.tourCourseDao().delete(tour)
                                            }
                                            DatabaseProvider.db.courseDao().delete(courseAvecTours.course)
                                        }
                                    }

                                    // On insère la nouvelle course toute propre
                                    val course = fr.iutvannes.dual.model.persistence.Course(
                                        id_seance     = KtorServer.idSeanceActuelle,
                                        id_eleve      = eleve.id_eleve,
                                        distance_tour = 0.0
                                    )
                                    val courseId = DatabaseProvider.db.courseDao().insert(course).toInt()

                                    toursArray.forEachIndexed { index, tourEl ->
                                        val tourObj = tourEl.jsonObject
                                        val numero  = tourObj["numero"]?.jsonPrimitive?.content?.toIntOrNull() ?: (index + 1)
                                        val tempsMs = tourObj["temps_ms"]?.jsonPrimitive?.content?.toLongOrNull() ?: 0L

                                        DatabaseProvider.db.tourCourseDao().insert(
                                            fr.iutvannes.dual.model.persistence.TourCourse(
                                                id_course   = courseId,
                                                numero_tour = numero,
                                                temps_ms    = tempsMs
                                            )
                                        )
                                    }
                                }
                            }

                            KtorServer.ressentis[eleve.id_eleve] = Triple(ressentiIntensite, ressentiDurer, ressentiLucidite)

                            call.respond(HttpStatusCode.Accepted, mapOf("status" to "OK"))
                        } else {
                            Log.e("KtorServer", "ÉLÈVE NON TROUVÉ : prenom='$prenom' nom='${nom.uppercase()}'")
                            call.respond(HttpStatusCode.NotFound, mapOf("error" to "Eleve non trouve"))
                        }
                    }
                    "TIR_RESULTAT_6EME" -> {
                        val payload = jsonElement["payload"]?.jsonObject
                        val scoreInt = payload?.get("total")?.jsonPrimitive?.content?.toIntOrNull() ?: 0

                        val eleve = withContext(Dispatchers.IO) {
                            DatabaseProvider.db.EleveDao().findByName(prenom, nom.uppercase())
                        }

                        if (eleve != null) {
                            val nouveauResultat = fr.iutvannes.dual.model.persistence.Resultat(
                                id_eleve = eleve.id_eleve,
                                id_seance = KtorServer.idSeanceActuelle,
                                cibles_touchees = scoreInt,
                                temp_course = 0F
                            )
                            withContext(Dispatchers.IO) {
                                DatabaseProvider.db.resultatDao().insert(nouveauResultat)
                            }
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

                        val eleve = withContext(Dispatchers.IO) {
                            DatabaseProvider.db.EleveDao().findByName(prenom, nom.uppercase())
                        }
                        if (eleve != null) {
                            withContext(Dispatchers.IO) {
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
                            }
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

        post("/api/biathlon") {

            try {

                val req = call.receive<BiathlonRequest>()
                val prenom = req.prenom
                val nom = req.nom
                val nbTirsReussi = req.nbTirsReussi
                val tempsAuPasDeTir = req.tempsAuPasDeTir
                val tempsAuTour = req.tempsAuTour
                val distance = req.distance


                val eleve = withContext(Dispatchers.IO) {
                    DatabaseProvider.db.EleveDao().findByName(prenom, nom.uppercase())
                }

                if (eleve == null) {
                    call.respond(HttpStatusCode.NotFound, "Élève introuvable")
                    return@post
                }

                val seanceId = KtorServer.idSeanceActuelle

                if (seanceId == 0) {
                    call.respond(HttpStatusCode.BadRequest, "Aucune séance active")
                    return@post
                }

                withContext(Dispatchers.IO) {

                    // ----- TIR -----
                    val tir = fr.iutvannes.dual.model.persistence.Tir(
                        id_seance = seanceId,
                        id_eleve = eleve.id_eleve
                    )

                    val tirId = DatabaseProvider.db
                        .tirDao()
                        .insert(tir)
                        .toInt()

                    nbTirsReussi.forEachIndexed { index, nbReussi ->
                        val salve = fr.iutvannes.dual.model.persistence.SalveTir(
                            id_tir = tirId,
                            numero_passage = index + 1,
                            nb_tir_reussi = nbReussi,
                            temps_au_pas_de_tir_ms = tempsAuPasDeTir.getOrElse(index) { 0 }.toLong()
                        )

                        DatabaseProvider.db.salveTirDao().insert(salve)
                    }

                    // ----- COURSE -----
                    val course = fr.iutvannes.dual.model.persistence.Course(
                        id_seance = seanceId,
                        id_eleve = eleve.id_eleve,
                        distance_tour = distance
                    )

                    val courseId = DatabaseProvider.db
                        .courseDao()
                        .insert(course)
                        .toInt()

                    tempsAuTour.forEachIndexed { index, temps ->
                        val tour = fr.iutvannes.dual.model.persistence.TourCourse(
                            id_course = courseId,
                            numero_tour = index + 1,
                            temps_ms = temps.toLong()
                        )

                        DatabaseProvider.db.tourCourseDao().insert(tour)
                    }

                    // ----- RÉSULTAT -----
                    val totalCibles = nbTirsReussi.sum()
                    val existant = DatabaseProvider.db.resultatDao().getResultatByEleveEtSeance(
                        eleve.id_eleve, seanceId
                    )
                    if (existant != null) {
                        existant.cibles_touchees += totalCibles
                        DatabaseProvider.db.resultatDao().update(existant)
                    } else {
                        val marquage = fr.iutvannes.dual.model.persistence.Resultat(
                            id_eleve        = eleve.id_eleve,
                            id_seance       = seanceId,
                            cibles_touchees = totalCibles,
                            temp_course     = 0F
                        )
                        DatabaseProvider.db.resultatDao().insert(marquage)
                    }
                }

                call.respond(HttpStatusCode.Created)

            } catch (e: Exception) {
                e.printStackTrace()
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf(
                        "error" to (e.message ?: "Erreur inconnue"),
                        "type" to e::class.simpleName
                    )
                )
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
                val idParam = call.request.queryParameters["seanceId"]?.toIntOrNull()
                val idActuel = idParam ?: KtorServer.idSeanceActuelle
                val seance = withContext(Dispatchers.IO) {
                    DatabaseProvider.db.seanceDao().getSeanceById(idActuel)
                }


                if (seance == null) {
                    call.respond(HttpStatusCode.NotFound, "Aucune séance active.")
                    return@get
                }

                //Nom du fichier dynamique
                val dateClean = seance.date.replace("/", "-").replace(":", "h").replace(" ", "_")
                val nomFichier = "Bilan_${seance.type}_${seance.classe}_$dateClean.csv"

                //Récupération des résultats
                val resultats = withContext(Dispatchers.IO) {
                    DatabaseProvider.db.resultatDao().getBySeance(idActuel)
                }
                val csv = StringBuilder()

                //Personnalisation du contenu selon le type de séance
                when (seance.type) {

                    "Test VMA" -> {
                        csv.append("Test VMA - ${seance.classe} - ${seance.date}\n\n")
                        csv.append("Nom;Prénom;VMA (km/h)\n")

                        resultats.forEach { res ->
                            val eleve = withContext(Dispatchers.IO) {
                                DatabaseProvider.db.EleveDao().getEleveById(res.id_eleve)
                            }
                            if (eleve != null) {
                                val vma = eleve.vma?.let { String.format("%.1f", it) } ?: "-"
                                csv.append("${eleve.nom.uppercase()};${eleve.prenom};$vma\n")
                            }
                        }
                    }

                    "Épreuve Finale" -> {
                        val is4eme = resultats.all { it.ecart_max_course == 0 && it.nbTours == 6 }

                        if (is4eme) {
                            csv.append("Épreuve Finale 4ème - ${seance.classe} - ${seance.date}\n\n")
                            csv.append("Nom;Prénom;VMA ref (km/h);Vitesse épreuve (km/h);% VMA;Cibles touchées (/10);Note /12\n")

                            resultats.forEach { res ->
                                val eleve = withContext(Dispatchers.IO) {
                                    DatabaseProvider.db.EleveDao().getEleveById(res.id_eleve)
                                }
                                if (eleve != null) {
                                    val vmaRef = eleve.vma?.let { String.format("%.1f", it) } ?: "-"
                                    val vitesse = String.format("%.2f", res.temp_course)
                                    val pctVma = if ((eleve.vma ?: 0f) > 0f)
                                        String.format("%.0f%%", (res.temp_course / eleve.vma!!) * 100)
                                    else "-"
                                    val note = String.format("%.2f", res.note_finale)
                                    csv.append("${eleve.nom.uppercase()};${eleve.prenom};$vmaRef;$vitesse;$pctVma;${res.cibles_touchees};$note\n")
                                }
                            }
                        } else {
                            csv.append("Épreuve Finale 6ème - ${seance.classe} - ${seance.date}\n\n")
                            csv.append("Nom;Prénom;VMA (km/h);Nb tours;Écart max (s);Cibles touchées;Note /15\n")

                            resultats.forEach { res ->
                                val eleve = withContext(Dispatchers.IO) {
                                    DatabaseProvider.db.EleveDao().getEleveById(res.id_eleve)
                                }
                                if (eleve != null) {
                                    val vma = eleve.vma?.let { String.format("%.1f", it) } ?: "-"
                                    val note = String.format("%.2f", res.note_finale)
                                    csv.append("${eleve.nom.uppercase()};${eleve.prenom};$vma;${res.nbTours};${res.ecart_max_course};${res.cibles_touchees};$note\n")
                                }
                            }
                        }
                    }

                    else -> {
                        csv.append("Entraînement - ${seance.classe} - ${seance.date}\n\n")
                        csv.append("Nom;Prénom;Cibles touchées;VMA (km/h)\n")

                        resultats.forEach { res ->
                            val eleve = withContext(Dispatchers.IO) {
                                DatabaseProvider.db.EleveDao().getEleveById(res.id_eleve)
                            }
                            if (eleve != null) {
                                val vma = eleve.vma?.let { String.format("%.1f", it) } ?: "-"
                                csv.append("${eleve.nom.uppercase()};${eleve.prenom};${res.cibles_touchees};$vma\n")
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

        // Route pour récupérer l'historique complet d'un élève (Épreuves + Entraînements)
        get("/api/eleves/historique/{id}") {
            val idEleveStr = call.parameters["id"]
            val idEleve = idEleveStr?.toIntOrNull()

            if (idEleve == null) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID invalide"))
                return@get
            }

            try {
                val historiqueAEnvoyer = mutableListOf<JsonObject>()

                withContext(Dispatchers.IO) {
                    val db = DatabaseProvider.db
                    val eleve = db.EleveDao().getEleveById(idEleve)
                    val vmaRef = eleve?.vma ?: 10f

                    // On récupère TOUTES les séances pour pouvoir faire le tri (Épreuve vs Entraînement)
                    val toutesLesSeances = db.seanceDao().getAllSeances()

                    // ---------------------------------------------------------
                    // PARTIE 1 : RÉCUPÉRATION DES ÉPREUVES (Table Resultat)
                    // ---------------------------------------------------------
                    val resultatsDB = db.resultatDao().getByEleve(idEleve)

                    resultatsDB.forEach { res ->
                        val seanceAssociee = toutesLesSeances.find { it.id_seance == res.id_seance }
                        val typeSeanceOriginal = seanceAssociee?.type ?: "Inconnu"

                        // On ignore les entraînements qui auraient "accidentellement" laissé une trace dans Resultat
                        if (typeSeanceOriginal == "Entraînement") return@forEach

                        val isVma = typeSeanceOriginal == "Test VMA" || (res.vma > 0f && res.cibles_touchees == 0)
                        val is4eme = seanceAssociee?.classe?.contains("4") == true

                        val typeEpreuve = when {
                            isVma -> "Test VMA"
                            is4eme -> "Épreuve Finale 4ème"
                            else -> "Épreuve Finale 6ème"
                        }

                        val dateStrFormatee = seanceAssociee?.date ?: "Inconnue"

                        val jsonRes = buildJsonObject {
                            put("type", typeEpreuve)
                            put("dateStr", dateStrFormatee)
                            put("dateObj", res.id_resultat.toLong()) // Tri JS
                            put("noteFinale", res.note_finale)

                            if (!isVma) {
                                put("bilan", buildJsonObject {
                                    if (is4eme) {
                                        val pourcentageVma = if (vmaRef > 0) (res.temp_course / vmaRef) * 100 else 0f
                                        val tempsTirSec = (res.temps_B - res.temps_A) + (res.temps_D - res.temps_C)

                                        put("vitesseVal", kotlin.math.round(pourcentageVma).toInt())
                                        put("vitesseRealiseeKmh", String.format(java.util.Locale.US, "%.1f", res.temp_course))
                                        put("noteVitesse", res.note_intensite)
                                        val medailleVitesse = when {
                                            pourcentageVma >= 105 -> "DIAMANT"
                                            pourcentageVma >= 95  -> "PLATINE"
                                            pourcentageVma >= 85  -> "OR"
                                            pourcentageVma >= 75  -> "ARGENT"
                                            else                  -> "BRONZE"
                                        }

                                        put("medailleVitesse", medailleVitesse)
                                        put("tirVal", res.cibles_touchees)
                                        put("tirTempsMs", tempsTirSec * 1000L) // Envoi en millisecondes pour formatTemps JS
                                        put("noteTir", res.note_efficience)
                                        put("vmaVal", vmaRef)
                                        put("noteVma", res.note_vma)

                                        val genre = eleve?.genre ?: "M"
                                        val medailleVma = if (genre == "M") {
                                            when {
                                                vmaRef >= 13.5f -> "DIAMANT"
                                                vmaRef >= 13f   -> "PLATINE"
                                                vmaRef >= 11.5f -> "OR"
                                                vmaRef >= 10.5f -> "ARGENT"
                                                else            -> "BRONZE"
                                            }
                                        } else { // Filles
                                            when {
                                                vmaRef >= 11.5f -> "DIAMANT"
                                                vmaRef >= 11f   -> "PLATINE"
                                                vmaRef >= 10.5f -> "OR"
                                                vmaRef >= 9.5f  -> "ARGENT"
                                                else            -> "BRONZE"
                                            }
                                        }
                                        put("medailleVma", medailleVma)
                                    } else {
                                        // --- CALCULS ÉPREUVE 6ÈME ---
                                        val nbTours = res.nbTours
                                        val ecartMax = res.ecart_max_course
                                        val totalTir = res.cibles_touchees

                                        // 1. Barème Performance
                                        val notePerf = when {
                                            nbTours >= 7 -> 5f
                                            nbTours == 6 -> 4f
                                            nbTours == 5 -> 3f
                                            nbTours == 4 -> 2f
                                            nbTours == 3 -> 1f
                                            else -> 0.5f
                                        }
                                        val medaillePerf = when {
                                            nbTours >= 8 -> "DIAMANT"
                                            nbTours >= 7 -> "PLATINE"
                                            nbTours >= 6 -> "OR"
                                            nbTours >= 5 -> "ARGENT"
                                            else -> "BRONZE"
                                        }

                                        // 2. Barème Régularité
                                        val noteRegul = when {
                                            ecartMax < 10 -> 5f
                                            ecartMax <= 15 -> 4f
                                            ecartMax <= 20 -> 3f
                                            ecartMax <= 25 -> 2f
                                            else -> 1f
                                        }
                                        val medailleRegul = when {
                                            ecartMax < 10 -> "DIAMANT"
                                            ecartMax <= 15 -> "PLATINE"
                                            ecartMax <= 20 -> "OR"
                                            ecartMax <= 25 -> "ARGENT"
                                            else -> "BRONZE"
                                        }

                                        // 3. Barème Tir
                                        val noteTir = when {
                                            totalTir >= 21 -> 5f
                                            totalTir == 20 -> 4.5f
                                            totalTir == 19 -> 4f
                                            totalTir == 18 -> 3.5f
                                            totalTir == 17 -> 3f
                                            totalTir == 16 -> 2.5f
                                            totalTir == 15 -> 2f
                                            totalTir == 14 -> 1.5f
                                            totalTir == 13 -> 1f
                                            else -> 0.5f
                                        }

                                        val maxTirPossible = if (nbTours > 0) nbTours * 5 else 1
                                        val pourcentageTir = (totalTir.toFloat() / maxTirPossible) * 100
                                        val medailleTir = when {
                                            pourcentageTir >= 85 -> "DIAMANT"
                                            pourcentageTir >= 75 -> "PLATINE"
                                            pourcentageTir >= 65 -> "OR"
                                            pourcentageTir >= 55 -> "ARGENT"
                                            else -> "BRONZE"
                                        }

                                        // Injection dans le JSON
                                        put("nbTours", nbTours)
                                        put("notePerf", notePerf)
                                        put("medaillePerf", medaillePerf)

                                        put("ecartMax", ecartMax)
                                        put("noteRegul", noteRegul)
                                        put("medailleRegul", medailleRegul)

                                        put("totalTir", totalTir)
                                        put("noteTir", noteTir)
                                        put("medailleTir", medailleTir)
                                    }
                                })

                                put("audit", buildJsonObject {
                                    put("intensite", res.ressenti_intensite)
                                    put("durer", res.ressenti_durer)
                                    put("lucidite", res.ressenti_lucidite)
                                })
                            }
                        }
                        historiqueAEnvoyer.add(jsonRes)
                    }

                    // ---------------------------------------------------------
                    // PARTIE 2 : RÉCUPÉRATION DES ENTRAÎNEMENTS (Tables Course et Tir)
                    // ---------------------------------------------------------
                    // On cherche toutes les séances de type "Entraînement"
                    val seancesEntrainement = toutesLesSeances.filter { it.type == "Entraînement" }

                    seancesEntrainement.forEach { seance ->
                        val courses = db.courseDao().getCoursesBySeanceEtEleve(seance.id_seance, idEleve)
                        val tirs = db.tirDao().getTirsBySeanceEtEleve(seance.id_seance, idEleve)

                        val is6eme = seance.classe.contains("6")
                        val baseType = if (is6eme) "Entraînement 6ème" else "Entraînement 4ème"

                        // 1. On crée une carte UNIQUE pour CHAQUE bloc de Course enregistré
                        courses.forEach { courseAvecTours ->
                            if (courseAvecTours.liste_tours.isNotEmpty()) {
                                val jsonCourse = buildJsonObject {
                                    put("type", "$baseType (Course)")
                                    put("dateStr", seance.date)
                                    // Tri temporel basé sur l'ID unique de la course
                                    put("dateObj", (seance.id_seance * 10000L) + courseAvecTours.course.id_course)

                                    put("bilan", buildJsonObject {
                                        val toursTries = courseAvecTours.liste_tours.sortedBy { it.numero_tour }
                                        val arrayTours = kotlinx.serialization.json.buildJsonArray {
                                            toursTries.forEach { tour ->
                                                add(kotlinx.serialization.json.JsonPrimitive(tour.temps_ms))
                                            }
                                        }
                                        put("tours", arrayTours)
                                    })
                                }
                                historiqueAEnvoyer.add(jsonCourse)
                            }
                        }

                        // 2. On crée une carte UNIQUE pour CHAQUE bloc de Tir enregistré
                        tirs.forEach { tirAvecPassages ->
                            if (tirAvecPassages.liste_passages.isNotEmpty()) {
                                val jsonTir = buildJsonObject {
                                    put("type", "$baseType (Tir)")
                                    put("dateStr", seance.date)
                                    // Tri temporel basé sur l'ID unique du tir
                                    put("dateObj", (seance.id_seance * 10000L) + tirAvecPassages.tir.id_tir)

                                    put("bilan", buildJsonObject {
                                        val passagesTries = tirAvecPassages.liste_passages.sortedBy { it.numero_passage }
                                        val arrayTirs = kotlinx.serialization.json.buildJsonArray {
                                            passagesTries.forEach { passage ->
                                                if (is6eme) {
                                                    // 6ème : juste le score
                                                    add(kotlinx.serialization.json.JsonPrimitive(passage.nb_tir_reussi))
                                                } else {
                                                    // 4ème : score + temps
                                                    add(buildJsonObject {
                                                        put("reussites", passage.nb_tir_reussi)
                                                        put("tempsMs", passage.temps_au_pas_de_tir_ms)
                                                    })
                                                }
                                            }
                                        }
                                        put("tirs", arrayTirs)
                                    })
                                }
                                historiqueAEnvoyer.add(jsonTir)
                            }
                        }
                    }
                } // Fin du withContext(Dispatchers.IO)

                call.respond(HttpStatusCode.OK, historiqueAEnvoyer)

            } catch (e: Exception) {
                Log.e("KtorServer", "Erreur Historique: ${e.message}", e)
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Erreur Serveur"))
            }
        }

        // Route pour récupérer la VMA fraîche d'un seul élève
        get("/api/eleves/{id}") {
            val idEleve = call.parameters["id"]?.toIntOrNull()
            if (idEleve == null) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID invalide"))
                return@get
            }

            try {
                // On va chercher l'élève dans la base
                val eleve = withContext(Dispatchers.IO) {
                    DatabaseProvider.db.EleveDao().getEleveById(idEleve)
                }

                if (eleve != null) {
                    // On renvoie sa VMA
                    call.respond(HttpStatusCode.OK, mapOf("vma" to eleve.vma))
                } else {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "Élève introuvable"))
                }
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Erreur Serveur"))
            }
        }
    }
}
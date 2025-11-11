package fr.iutvannes.dual.model.utils

// Import des librairies nécessaires
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

/**
 * EmailService est un objet Kotlin (singleton) utilisé pour envoyer des emails
 * via l'API SendGrid. Il est conçu pour être appelé depuis n'importe quel
 * endroit de l'application (ex : lors d'une réinitialisation de mot de passe).
 */
object EmailService {

    /** Clé API SendGrid (à ne jamais exposer dans un code client en production !)
    Elle permet d'authentifier les requêtes vers le service SendGrid. */
    private const val API_KEY = "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"

    /** Adresse de l'expéditeur qui sera affichée dans le mail reçu */
    private const val SENDER_EMAIL = "biathlon.dual@protonmail.com"

    /** URL officielle de l’API SendGrid pour l’envoi de mails */
    private const val SENDGRID_API_URL = "https://api.sendgrid.com/v3/mail/send"

    /** Client HTTP utilisé pour faire les requêtes réseau */
    private val client = OkHttpClient()

    /** Type MIME pour indiquer que le corps de la requête est au format JSON */
    private val JSON = "application/json; charset=utf-8".toMediaType()

    /**
     * Envoie un email de réinitialisation de mot de passe via SendGrid.
     * @param emailTo : adresse du destinataire (le prof)
     * @param newPassword : mot de passe temporaire généré
     * @return true si l'email a été accepté par SendGrid, false sinon
     */
    suspend fun sendPasswordResetEmail(emailTo: String, newPassword: String): Boolean {
        // withContext(Dispatchers.IO) => exécute ce code sur un thread d'entrée/sortie (I/O)
        // pour ne pas bloquer le thread principal (UI)
        return withContext(Dispatchers.IO) {
            try {
                // --- Construction du JSON attendu par SendGrid ---
                val json = JSONObject().apply {
                    // "personalizations" contient la liste des destinataires
                    put("personalizations", JSONArray().apply {
                        put(JSONObject().apply {
                            // "to" : à qui on envoie le mail
                            put("to", JSONArray().apply {
                                put(JSONObject().apply {
                                    put("email", emailTo)
                                })
                            })
                        })
                    })

                    // "from" : adresse de l'expéditeur (doit être vérifiée chez SendGrid)
                    put("from", JSONObject().apply {
                        put("email", SENDER_EMAIL)
                    })

                    // Sujet du mail
                    put("subject", "Réinitialisation de votre mot de passe")

                    // "content" : contenu du mail (texte brut ici)
                    put("content", JSONArray().apply {
                        put(JSONObject().apply {
                            put("type", "text/plain") // Type de contenu : texte simple
                            put("value", """
                                Bonjour,

                                Votre nouveau mot de passe temporaire est : $newPassword

                                Veuillez le changer dès votre prochaine connexion.

                                Cordialement,
                                L'équipe DUAL
                            """.trimIndent())
                        })
                    })
                }

                // --- Conversion du JSON en corps de requête HTTP ---
                val requestBody = json.toString().toRequestBody(JSON)

                // --- Création de la requête HTTP ---
                val request = Request.Builder()
                    .url(SENDGRID_API_URL) // URL de l’API SendGrid
                    // Authentification via la clé API dans l’en-tête
                    .addHeader("Authorization", "Bearer $API_KEY")
                    // Type de contenu JSON
                    .addHeader("Content-Type", "application/json")
                    // Méthode POST car on envoie des données
                    .post(requestBody)
                    .build()

                // --- Exécution de la requête ---
                val response = client.newCall(request).execute()

                // --- Vérification du résultat ---
                // SendGrid renvoie 202 si l'email a été "accepté"
                println("SendGrid Response: ${response.code} - ${response.message}")
                println("Response body: ${response.body?.string()}")

                // On renvoie true si tout s'est bien passé (code HTTP 2xx)
                response.isSuccessful
            } catch (e: Exception) {
                // En cas d'erreur réseau ou JSON, on affiche la trace et on retourne false
                e.printStackTrace()
                false
            }
        }
    }
}

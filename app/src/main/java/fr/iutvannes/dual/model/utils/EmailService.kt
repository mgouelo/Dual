package fr.iutvannes.dual.model.utils

import fr.iutvannes.dual.BuildConfig
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
 * via l'API Brevo (anciennement Sendinblue).
 * Il est conçu pour être appelé depuis n'importe quel endroit de l'application
 * (ex : réinitialisation de mot de passe).
 */
object EmailService {

    /** Clé API Brevo (à ne jamais exposer dans un code client en production !) */
    private const val API_KEY = "MY_API_KEY"
    /** Adresse de l'expéditeur vérifiée sur Brevo */
    private const val SENDER_EMAIL = "biathlon.dual@outlook.fr"

    /** URL de l’API Brevo pour l’envoi d’emails transactionnels */
    private const val BREVO_API_URL = "https://api.brevo.com/v3/smtp/email"

    /** Client HTTP utilisé pour faire les requêtes réseau */
    private val client = OkHttpClient()

    /** Type MIME pour indiquer que le corps de la requête est au format JSON */
    private val JSON = "application/json; charset=utf-8".toMediaType()

    /**
     * Envoie un email de réinitialisation de mot de passe via Brevo.
     * @param emailTo : adresse du destinataire
     * @param newPassword : mot de passe temporaire généré
     * @return true si l'email a été accepté par Brevo, false sinon
     */
    suspend fun sendPasswordResetEmail(emailTo: String, newPassword: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // --- Construction du JSON attendu par Brevo ---
                val json = JSONObject().apply {
                    // "sender" : expéditeur vérifié
                    put("sender", JSONObject().apply { put("email", SENDER_EMAIL) })

                    // "to" : liste des destinataires
                    put("to", JSONArray().apply {
                        put(JSONObject().apply { put("email", emailTo) })
                    })

                    // Sujet de l'email
                    put("subject", "Réinitialisation de votre mot de passe")

                    // Contenu texte simple de l'email
                    put("textContent", """
                        Bonjour,

                        Votre nouveau mot de passe temporaire est : $newPassword

                        Veuillez le changer dès votre prochaine connexion.

                        Cordialement,
                        L'équipe DUAL
                    """.trimIndent())
                }

                // --- Conversion du JSON en corps de requête HTTP ---
                val requestBody = json.toString().toRequestBody(JSON)

                // --- Création de la requête HTTP ---
                val request = Request.Builder()
                    .url(BREVO_API_URL) // URL de l’API Brevo
                    // Authentification via la clé API Brevo
                    .addHeader("api-key", API_KEY)
                    // Type de contenu JSON
                    .addHeader("accept", "application/json")
                    .addHeader("Content-Type", "application/json")
                    // Méthode POST pour envoyer le mail
                    .post(requestBody)
                    .build()

                // --- Exécution de la requête ---
                val response = client.newCall(request).execute()

                // --- Vérification du résultat ---
                println("Brevo Response: ${response.code} - ${response.message}")
                println("Response body: ${response.body?.string()}")

                // Retourne true si code HTTP 2xx
                response.isSuccessful
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }
}

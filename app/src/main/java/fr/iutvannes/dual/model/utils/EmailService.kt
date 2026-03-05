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
import android.util.Base64
import fr.iutvannes.dual.model.persistence.Resultat
import okhttp3.MediaType.Companion.toMediaTypeOrNull

/**
 * EmailService is a Kotlin object (singleton) used to send emails
 * via the Brevo API (formerly Sendinblue).
 * It is designed to be called from anywhere in the application
 * (e.g., password reset).
 */
object EmailService {

    /** Brevo API key (never expose it in production client code!)*/
    private const val API_KEY = BuildConfig.MY_API_KEY
    /** Sender's address verified on Brevo */
    private const val SENDER_EMAIL = "biathlon.dual@outlook.fr"

    /** Brevo API URL for sending transactional emails */
    private const val BREVO_API_URL = "https://api.brevo.com/v3/smtp/email"

    /** HTTP client used to make network requests */
    private val client = OkHttpClient()

    /** MIME type to indicate that the request body is in JSON format. */
    private val JSON = "application/json; charset=utf-8".toMediaType()

    /**
     * Sends a password reset email via Brevo.
     * @param emailTo : recipient address
     * @param newPassword : generated temporary password
     * @return true if the email was accepted by Brevo, false otherwise
     */
    suspend fun sendPasswordResetEmail(emailTo: String, newPassword: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // --- Construction du JSON attendu par Brevo ---
                val json = JSONObject().apply {
                    // "sender" : expéditeur vérifié
                    put("sender", JSONObject().apply { put("email", SENDER_EMAIL) })

                    // "to": list of recipients
                    put("to", JSONArray().apply {
                        put(JSONObject().apply { put("email", emailTo) })
                    })

                    // Email subject
                    put("subject", "Réinitialisation de votre mot de passe")

                    // Plain text content of the email
                    put("textContent", """
                        Bonjour,

                        Votre nouveau mot de passe temporaire est : $newPassword

                        Veuillez le changer dès votre prochaine connexion.

                        Cordialement,
                        L'équipe DUAL
                    """.trimIndent())
                }

                // --- Converting JSON to HTTP Request Body ---
                val requestBody = json.toString().toRequestBody(JSON)

                // --- Creating the HTTP request ---
                val request = Request.Builder()
                    .url(BREVO_API_URL) // Brevo API URL
                    // Authentication via the Brevo API key
                    .addHeader("api-key", API_KEY)
                    // JSON content type
                    .addHeader("accept", "application/json")
                    .addHeader("Content-Type", "application/json")
                    // POST method for sending the email
                    .post(requestBody)
                    .build()

                // --- Query execution ---
                val response = client.newCall(request).execute()

                // --- Result Verification ---
                println("Brevo Response: ${response.code} - ${response.message}")
                println("Response body: ${response.body?.string()}")

                // Returns true if HTTP code 2xx
                response.isSuccessful
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    suspend fun sendExcelExportEmail(emailTo: String, dateSession: String, csvContent: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                //Encodage du contenu CSV en Base64
                val base64Content = android.util.Base64.encodeToString(csvContent.toByteArray(), android.util.Base64.NO_WRAP)

                val json = JSONObject().apply {
                    put("sender", JSONObject().apply { put("email", SENDER_EMAIL) })
                    put("to", JSONArray().apply { put(JSONObject().apply { put("email", emailTo) }) })
                    put("subject", "Bilan DUAL - Séance du $dateSession")
                    put("textContent", "Bonjour,\n\nVotre séance est terminée. Veuillez trouver le bilan en pièce jointe.\n\nCordialement.")

                    //Pièce jointe
                    put("attachment", JSONArray().apply {
                        put(JSONObject().apply {
                            put("content", base64Content)
                            put("name", "bilan_seance_${dateSession.replace("/", "_")}.csv")
                        })
                    })
                }

                val requestBody = json.toString().toRequestBody("application/json".toMediaTypeOrNull())
                val request = Request.Builder()
                    .url(BREVO_API_URL)
                    .addHeader("api-key", API_KEY)
                    .post(requestBody)
                    .build()

                client.newCall(request).execute().isSuccessful
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }
}

// Assurez-vous que le package est correct
package fr.iutvannes.dual.controller.fragments

// Imports nécessaires
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import fr.iutvannes.dual.R
import fr.iutvannes.dual.controller.MainActivity
import fr.iutvannes.dual.model.database.AppDatabase
import fr.iutvannes.dual.model.utils.PasswordUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.core.content.edit
import com.bumptech.glide.Glide
import fr.iutvannes.dual.model.persistence.Prof

/**
 * Fragment to display the user's profile screen.
 * The associated layout is R.layout.fragment_profile.
 *
 * @see AppDatabase
 * @see MainActivity
 * @see PasswordUtils
 * @see R.layout.fragment_profil
 */
class ProfilFragment : Fragment(R.layout.fragment_profil) {

    // We "promise" the compiler that we will initialize this variable before any call
    /* Variable to display the user's profile picture */
    private lateinit var pdp: ImageView

    /* Variable to store the currently connected teacher */
    private var profConnecte: Prof? = null

    // Initializing the callback to manage the new profile picture chosen by the user
    private val pickMedia = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            Log.d("PhotoPicker", "Photo sélectionnée : $uri") // debug

            // We make access to this file permanent so that we have read permission even after restarting the app.
            val flag = Intent.FLAG_GRANT_READ_URI_PERMISSION
            requireContext().contentResolver.takePersistableUriPermission(uri, flag)

            // Display with slide
            Glide.with(this)
                .load(uri)
                .circleCrop()
                .into(pdp)

            profConnecte?.let { prof ->
                sauvegarderPhotoEnBase(uri.toString(), prof)
            }
        } else {
            Log.d("PhotoPicker", "Pas de photo sélectionnée")
        }
    }


    /**
     * This function is called when the fragment view is created.
     * It initializes interactions with views.
     *
     * @param view The fragment view.
     * @param savedInstanceState The fragment's saved data.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Profile picture (PDP)
        pdp = view.findViewById<ImageView>(R.id.profileImage)

        // --- REFERENCES TO TEXT FIELDS ---
        val nomField = view.findViewById<EditText>(R.id.nomField)
        val prenomField = view.findViewById<EditText>(R.id.prenomField)
        val adresseField = view.findViewById<EditText>(R.id.adresseField)
        val mdpField = view.findViewById<EditText>(R.id.mdpField)
        val nouveauMdpField = view.findViewById<EditText>(R.id.nouveau_mdpField)
        val confirmerMdpField = view.findViewById<EditText>(R.id.confirmer_nouveau_mdpField)
        val userProfilTxt = view.findViewById<TextView>(R.id.user_profil_txt)

        // --- BUTTONS ---
        val editButtonProfil = view.findViewById<ImageButton>(R.id.editButtonProfil)
        val editButtonMdp = view.findViewById<ImageButton>(R.id.editButtonMdp)
        val disconnectButton = view.findViewById<Button>(R.id.btnDisconnect)
        val backButton = view.findViewById<ImageButton>(R.id.arrow_back_button)


        // --- BASE CONNECTION ---
        val db = DatabaseProvider.db

        // Retrieve the email securely
        val masterKey = MasterKey.Builder(requireContext())
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        val sharedPrefs = EncryptedSharedPreferences.create(
            requireContext(),
            "loginPrefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

        // Load the information of the connected teacher (from the database)
        // lifectcleScope.launch is used to execute asynchronous code to prevent the UI from freezing when the user changes fragments
        // (the operation is canceled if the fragment is changed)
        lifecycleScope.launch {
            profConnecte = withContext(Dispatchers.IO) {
                // Example: here, we assume that we have stored the email address of the logged-in teacher.
                val email = sharedPrefs.getString("email", null)
                if (email != null) {
                    // If an email address is present, the corresponding teacher is searched in the database.
                    db.profDAO().getProfByEmail(email)
                } else {
                    // Otherwise, we return null (no teachers connected)
                    null
                }
            }

            // If we have found the teacher (!= null), we fill in the fields
            profConnecte?.let { prof ->
                userProfilTxt.setText(prof.prenom)
                nomField.setText(prof.nom)
                prenomField.setText(prof.prenom)
                adresseField.setText(prof.email)

                chargerPhotoProfil(
                    requireContext(),
                    pdp,
                    prof.nom,
                    prof.prenom,
                    prof.photoUri // Room returns null or the path to the pp
                )
            }
        }

        // --- BACK BUTTON HANDLING ---
        // Handling the click on the back button
        backButton.setOnClickListener {
            (activity as? MainActivity)?.showFragment(TableauDeBordFragment(), true, true)
        }

        pdp.setOnClickListener {
            pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) // ouvre la galerie android sur image only
        }

        // --- GESTION DU BOUTON D'ÉDITION DU PROFIL ---
        editButtonProfil.setOnClickListener {
            // We determine whether the fields are currently editable or not.
            val isEditable = !nomField.isEnabled   // If the fields are disabled, we switch to edit mode

            // The fields are disabled or enabled depending on the chosen mode.
            nomField.isEnabled = isEditable
            prenomField.isEnabled = isEditable
            adresseField.isEnabled = isEditable

            // If the fields become disabled after a click (thus exiting edit mode)
            if (!isEditable) {
                // --- SAVE CHANGES ---
                lifecycleScope.launch {  // Launching a coroutine (asynchronous execution without blocking the interface)

                    // We retrieve the stored email address of the logged-in user from the secure SharedPreferences.
                    val email = sharedPrefs.getString("email", null)
                    if (email == null) {
                        // If no email address is found, an error is displayed and the coroutine is stopped.
                        Toast.makeText(requireContext(), "Erreur : aucun utilisateur connecté", Toast.LENGTH_SHORT).show()
                        return@launch
                    }

                    // We retrieve the profile corresponding to this email from the database.
                    val prof = withContext(Dispatchers.IO) { db.profDAO().getProfByEmail(email) }

                    if (prof != null) {
                        // Edit mode disabled → save and restore default colors
                        nomField.setBackgroundResource(0)
                        prenomField.setBackgroundResource(0)
                        adresseField.setBackgroundResource(0)
                        val defaultColor = resources.getColor(android.R.color.black, null)
                        nomField.setTextColor(defaultColor)
                        prenomField.setTextColor(defaultColor)
                        adresseField.setTextColor(defaultColor)

                        // If a teacher was successfully found, the new values entered by the user are retrieved.
                        val newNom = nomField.text.toString().trim()
                        val newPrenom = prenomField.text.toString().trim()
                        val newEmail = adresseField.text.toString().trim()

                        // --- Checking required fields ---
                        if (newNom.isBlank() || newPrenom.isBlank() || newEmail.isBlank()) {
                            // If a field is empty → an error is displayed and the save operation is stopped
                            Toast.makeText(requireContext(), "Tous les champs doivent être remplis", Toast.LENGTH_SHORT).show()
                            return@launch
                        }

                        // --- Email address format check ---
                        val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$".toRegex()
                        if (!newEmail.matches(emailRegex)) {
                            // If the email is not compliant, the user is notified.
                            Toast.makeText(requireContext(), "Email invalide", Toast.LENGTH_SHORT).show()
                            return@launch
                        }

                        // --- Professor's Information Update ---
                        prof.nom = newNom
                        prof.prenom = newPrenom
                        prof.email = newEmail

                        // The new information is saved in the database (in a background thread).
                        withContext(Dispatchers.IO) { db.profDAO().update(prof) }

                        // If the user has changed their email address, the locally stored email address is also updated.
                        sharedPrefs.edit { putString("email", newEmail) }

                        // Visual confirmation message
                        Toast.makeText(requireContext(), "Profil mis à jour", Toast.LENGTH_SHORT).show()
                    } else {
                        // If no teacher was found with this email address (an unusual case)
                        Toast.makeText(requireContext(), "Utilisateur introuvable", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                // If we have just activated edit mode (the fields become editable)
                Toast.makeText(requireContext(), "Mode édition activé", Toast.LENGTH_SHORT).show()
                val highlightColor = resources.getColor(R.color.gris, null)
                nomField.setBackgroundResource(android.R.drawable.edit_text)
                prenomField.setBackgroundResource(android.R.drawable.edit_text)
                adresseField.setBackgroundResource(android.R.drawable.edit_text)
                nomField.setTextColor(highlightColor)
                prenomField.setTextColor(highlightColor)
                adresseField.setTextColor(highlightColor)
            }
        }

        // --- PASSWORD EDIT BUTTON MANAGEMENT ---
        // Password Edit Button Click Management
        editButtonMdp.setOnClickListener {
            // We are toggling the editing state (if it's disabled, we're enabling it)
            val isEditable = !nouveauMdpField.isEnabled

            // Enables or disables both password fields
            mdpField.isEnabled = isEditable
            nouveauMdpField.isEnabled = isEditable
            confirmerMdpField.isEnabled = isEditable

            if (isEditable) {
                // EDITING MODE ENABLED → makes the fields visually distinct
                val highlightColor = resources.getColor(R.color.gris, null)
                mdpField.setBackgroundResource(android.R.drawable.edit_text)
                nouveauMdpField.setBackgroundResource(android.R.drawable.edit_text)
                confirmerMdpField.setBackgroundResource(android.R.drawable.edit_text)
                mdpField.setTextColor(highlightColor)
                nouveauMdpField.setTextColor(highlightColor)
                confirmerMdpField.setTextColor(highlightColor)

                Toast.makeText(requireContext(), "Modification du mot de passe activée", Toast.LENGTH_SHORT).show()
            }
            else {
                // EDIT MODE DISABLED → attempting to save changes

                val mdpActuel = mdpField.text.toString().trim()
                val nouveauMdp = nouveauMdpField.text.toString().trim()
                val confirmer = confirmerMdpField.text.toString().trim()

                // --- Checking for empty fields ---
                if (nouveauMdp.isBlank() || confirmer.isBlank() || mdpActuel.isBlank()) {
                    Toast.makeText(requireContext(), "Veuillez remplir tous les champs", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                // --- Matching Verification ---
                if (nouveauMdp != confirmer) {
                    Toast.makeText(requireContext(), "Les mots de passe ne correspondent pas", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                // --- Minimum password security check ---
                val motDePasseValide = nouveauMdp.length >= 8 &&
                        nouveauMdp.any { it.isUpperCase() } &&
                        nouveauMdp.any { it.isDigit() }

                if (!motDePasseValide) {
                    Toast.makeText(requireContext(),
                        "Le mot de passe doit contenir au moins 8 caractères, une majuscule et un chiffre",
                        Toast.LENGTH_LONG
                    ).show()
                    return@setOnClickListener
                }

                // --- Update the password in the database ---
                lifecycleScope.launch {
                    val email = sharedPrefs.getString("email", null)
                    if (email == null) {
                        Toast.makeText(requireContext(), "Erreur : aucun utilisateur connecté", Toast.LENGTH_SHORT).show()
                        return@launch
                    }

                    val prof = withContext(Dispatchers.IO) { db.profDAO().getProfByEmail(email) }
                    if (prof != null) {
                        // Verifying your current password
                        val mdpCorrect = PasswordUtils.verifyPassword(mdpActuel, prof.password)
                        if (!mdpCorrect) {
                            Toast.makeText(requireContext(), "Mot de passe actuel incorrect !", Toast.LENGTH_SHORT).show()
                            return@launch
                        }

                        // Hashing the password before saving (good security practice)
                        val hashedPassword = PasswordUtils.hashPassword(nouveauMdp)
                        prof.password = hashedPassword

                        // Saving in the database
                        withContext(Dispatchers.IO) { db.profDAO().update(prof) }

                        // Resetting the interface after the update
                        mdpField.text.clear()
                        nouveauMdpField.text.clear()
                        confirmerMdpField.text.clear()
                        mdpField.isEnabled = false
                        nouveauMdpField.isEnabled = false
                        confirmerMdpField.isEnabled = false
                        mdpField.setBackgroundResource(0)
                        nouveauMdpField.setBackgroundResource(0)
                        confirmerMdpField.setBackgroundResource(0)
                        val defaultColor = resources.getColor(android.R.color.black, null)
                        mdpField.setTextColor(defaultColor)
                        nouveauMdpField.setTextColor(defaultColor)
                        confirmerMdpField.setTextColor(defaultColor)

                        Toast.makeText(requireContext(), "Mot de passe mis à jour", Toast.LENGTH_SHORT).show()
                    }
                    else {
                        Toast.makeText(requireContext(), "Utilisateur introuvable", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }


        // --- LOGOUT BUTTON MANAGEMENT ---
        // Logout button click management
        disconnectButton.setOnClickListener {
            // Delete the user from memory (if you are storing a session)
            sharedPrefs.edit { putString("email", "") }
            sharedPrefs.edit { putString("password", "") }
            (activity as? MainActivity)?.showFragment(ConnexionFragment(), false, false)
        }
    }

    /**
     * Initializes the user profile picture
     *
     * @param context The context of the application
     * @param imageView The ImageView to display the profile picture
     * @param nom The last name of the user
     * @param prenom The first name of the user
     * @param photoUri The URI of the user's profile picture
     */
    fun chargerPhotoProfil(context: Context, imageView: ImageView, nom: String, prenom: String, photoUri: String?) {

        //
        val imageACharger: Any = if (photoUri != null) {
            // The user has a personal photo, so we take the URI.
            Uri.parse(photoUri)
        } else {
            // No photo, so we generate the default PFP URL for the API call.
            "https://ui-avatars.com/api/?name=$prenom+$nom&background=random&color=fff&size=128&bold=true"
        }

        // The glide plugin handles the display of the photo.
        Glide.with(context)
            .load(imageACharger)
            .circleCrop() // format circulaire
            .placeholder(R.drawable.pfp) // Image while loadingimage while loading
            .error(R.drawable.pfp)       // Image if error
            .into(imageView)
    }

    /**
     * Allows saving the user's selected profile picture to the database
     *
     * @param uriString The URI of the selected profile picture
     * @param profConnecte The connected teacher
     */
    private fun sauvegarderPhotoEnBase(uriString: String, profConnecte: Prof) {


        // VERIFICATION LOG
        Log.d("DEBUG_PROF", "Tentative de sauvegarde. ID=${profConnecte.id_prof} - URI=$uriString")
        // Retrieve your current user (assuming it's in a variable 'currentUser')
        // Just modify the photoUri field
        val updated = profConnecte.copy(photoUri = uriString)

        // PDP update in a coroutine
        val db = DatabaseProvider.db
        lifecycleScope.launch {
            db.profDAO().update(updated)
            // User feedback
            Toast.makeText(context, "Photo de profil mise à jour !", Toast.LENGTH_SHORT).show()
        }
    }
}
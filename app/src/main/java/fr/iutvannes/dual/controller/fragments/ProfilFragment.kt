// Assurez-vous que le package est correct
package fr.iutvannes.dual.controller.fragments

// Imports nécessaires
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
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

    /**
     * This function is called when the fragment view is created.
     * It initializes interactions with views.
     *
     * @param view The fragment view.
     * @param savedInstanceState The fragment's saved data.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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
            val profConnecte = withContext(Dispatchers.IO) {
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

            // If we have found the teacher, we fill in the fields
            profConnecte?.let {
                userProfilTxt.setText(it.prenom)
                nomField.setText(it.nom)
                prenomField.setText(it.prenom)
                adresseField.setText(it.email)
            }
        }

        // --- BACK BUTTON HANDLING ---
        // Handling the click on the back button
        backButton.setOnClickListener {
            (activity as? MainActivity)?.showFragment(TableauDeBordFragment(), true, true)
        }

        // --- PROFILE EDIT BUTTON MANAGEMENT ---
        // Management of clicks on the profile edit button
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
}
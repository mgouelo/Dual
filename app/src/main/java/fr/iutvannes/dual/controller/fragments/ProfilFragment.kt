// Assurez-vous que le package est correct
package fr.iutvannes.dual.controller.fragments

// Imports nécessaires
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import fr.iutvannes.dual.R
import fr.iutvannes.dual.controller.MainActivity
import fr.iutvannes.dual.model.database.AppDatabase
import fr.iutvannes.dual.model.utils.PasswordUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Fragment pour afficher l'écran de profil de l'utilisateur.
 * Le layout associé est R.layout.fragment_profile.
 */
class ProfilFragment : Fragment(R.layout.fragment_profil) {

    /**
     * Cette fonction est appelée lorsque la vue du fragment est créée.
     * Elle initialise les interactions avec les vues.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // --- RÉFÉRENCES AUX CHAMPS DE TEXTE ---
        val nomField = view.findViewById<EditText>(R.id.nomField)
        val prenomField = view.findViewById<EditText>(R.id.prenomField)
        val adresseField = view.findViewById<EditText>(R.id.adresseField)
        val mdpField = view.findViewById<EditText>(R.id.mdpField)
        val nouveauMdpField = view.findViewById<EditText>(R.id.nouveau_mdpField)
        val confirmerMdpField = view.findViewById<EditText>(R.id.confirmer_nouveau_mdpField)
        val userProfilTxt = view.findViewById<TextView>(R.id.user_profil_txt)

        // --- BOUTONS ---
        val editButtonProfil = view.findViewById<ImageButton>(R.id.editButtonProfil)
        val editButtonMdp = view.findViewById<ImageButton>(R.id.editButtonMdp)
        val disconnectButton = view.findViewById<Button>(R.id.btnDisconnect)
        val backButton = view.findViewById<ImageButton>(R.id.arrow_back_button)


        // --- CONNEXION À LA BASE ---
        val db = Room.databaseBuilder(
            requireContext(),
            AppDatabase::class.java,
            "dual.db"
        ).build()


        // Récupérer l'email de manière sécurisée
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

        // Charger les infos du prof connecté (depuis la base)
        // lifectcleScope.launch est utilisé pour exécuter du code asynchrone permettant de ne pas bloquer l'UI lorsque l'utilisateur change de fragment
        // (l'opération est annulée si on change de fragment)
        lifecycleScope.launch {
            val profConnecte = withContext(Dispatchers.IO) {
                // Exemple : ici, on suppose qu'on a stocké l'email du prof connecté
                val email = sharedPrefs.getString("email", null)
                if (email != null) {
                    // Si un email est présent, on cherche le prof correspondant dans la base
                    db.profDAO().getProfByEmail(email)
                } else {
                    // Sinon, on retourne null (aucun prof connecté)
                    null
                }
            }

            // Si on a trouvé le prof, on remplit les champs
            profConnecte?.let {
                userProfilTxt.setText(it.prenom)
                nomField.setText(it.nom)
                prenomField.setText(it.prenom)
                adresseField.setText(it.email)
            }
        }

        // --- GESTION DU BOUTON RETOUR ---
        backButton.setOnClickListener {
            (activity as? MainActivity)?.showFragment(TableauDeBordFragment(), true, true)
        }

        // --- GESTION DU BOUTON D'ÉDITION DU PROFIL ---
        editButtonProfil.setOnClickListener {
            // On détermine si les champs sont actuellement éditables ou non
            val isEditable = !nomField.isEnabled   // Si les champs sont désactivés, on passe en mode édition

            // On désactive ou active les champs selon le mode choisi
            nomField.isEnabled = isEditable
            prenomField.isEnabled = isEditable
            adresseField.isEnabled = isEditable

            // Si les champs deviennent désactivés après un clic (donc on quitte le mode édition)
            if (!isEditable) {
                // --- SAUVEGARDE DES MODIFICATIONS ---
                lifecycleScope.launch {  // Lancement d’une coroutine (exécution asynchrone sans bloquer l’interface)

                    // On récupère l’email stocké de l’utilisateur connecté dans les SharedPreferences sécurisées
                    val email = sharedPrefs.getString("email", null)
                    if (email == null) {
                        // Si aucun email n’est trouvé, on affiche une erreur et on arrête la coroutine
                        Toast.makeText(requireContext(), "Erreur : aucun utilisateur connecté", Toast.LENGTH_SHORT).show()
                        return@launch
                    }

                    // On récupère depuis la base de données le profil correspondant à cet email
                    val prof = withContext(Dispatchers.IO) { db.profDAO().getProfByEmail(email) }

                    if (prof != null) {
                        // Mode édition désactivé → on sauvegarde et remet les couleurs par défaut
                        nomField.setBackgroundResource(0)
                        prenomField.setBackgroundResource(0)
                        adresseField.setBackgroundResource(0)
                        val defaultColor = resources.getColor(android.R.color.black, null)
                        nomField.setTextColor(defaultColor)
                        prenomField.setTextColor(defaultColor)
                        adresseField.setTextColor(defaultColor)

                        // Si un professeur a bien été trouvé, on récupère les nouvelles valeurs saisies par l’utilisateur
                        val newNom = nomField.text.toString().trim()
                        val newPrenom = prenomField.text.toString().trim()
                        val newEmail = adresseField.text.toString().trim()

                        // --- Vérification des champs obligatoires ---
                        if (newNom.isBlank() || newPrenom.isBlank() || newEmail.isBlank()) {
                            // Si un champ est vide → on affiche une erreur et on arrête la sauvegarde
                            Toast.makeText(requireContext(), "Tous les champs doivent être remplis", Toast.LENGTH_SHORT).show()
                            return@launch
                        }

                        // --- Vérification du format de l’adresse email ---
                        val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$".toRegex()
                        if (!newEmail.matches(emailRegex)) {
                            // Si l’email n’est pas conforme, on le signale à l’utilisateur
                            Toast.makeText(requireContext(), "Email invalide", Toast.LENGTH_SHORT).show()
                            return@launch
                        }

                        // --- Mise à jour des informations du professeur ---
                        prof.nom = newNom
                        prof.prenom = newPrenom
                        prof.email = newEmail

                        // On enregistre les nouvelles informations dans la base de données (dans un thread d’arrière-plan)
                        withContext(Dispatchers.IO) { db.profDAO().update(prof) }

                        // Si l’utilisateur a changé son email, on met aussi à jour l’email stocké localement
                        sharedPrefs.edit().putString("email", newEmail).apply()

                        // Message de confirmation visuel
                        Toast.makeText(requireContext(), "Profil mis à jour", Toast.LENGTH_SHORT).show()
                    } else {
                        // Si aucun professeur n’a été trouvé avec cet email (cas anormal)
                        Toast.makeText(requireContext(), "Utilisateur introuvable", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                // Si on vient d’activer le mode édition (les champs deviennent modifiables)
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

        // --- GESTION DU BOUTON D'ÉDITION DU MOT DE PASSE ---
        editButtonMdp.setOnClickListener {
            // On inverse l’état d’édition (si c’est désactivé, on l’active)
            val isEditable = !nouveauMdpField.isEnabled

            // Active ou désactive les deux champs de mot de passe
            mdpField.isEnabled = isEditable
            nouveauMdpField.isEnabled = isEditable
            confirmerMdpField.isEnabled = isEditable

            if (isEditable) {
                // MODE ÉDITION ACTIVÉ → on rend les champs visuellement distincts
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
                // MODE ÉDITION DÉSACTIVÉ → on tente de sauvegarder les changements

                val mdpActuel = mdpField.text.toString().trim()
                val nouveauMdp = nouveauMdpField.text.toString().trim()
                val confirmer = confirmerMdpField.text.toString().trim()

                // --- Vérification des champs vides ---
                if (nouveauMdp.isBlank() || confirmer.isBlank() || mdpActuel.isBlank()) {
                    Toast.makeText(requireContext(), "Veuillez remplir tous les champs", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                // --- Vérification de la correspondance ---
                if (nouveauMdp != confirmer) {
                    Toast.makeText(requireContext(), "Les mots de passe ne correspondent pas", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                // --- Vérification de la sécurité minimale du mot de passe ---
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

                // --- Mise à jour du mot de passe dans la base ---
                lifecycleScope.launch {
                    val email = sharedPrefs.getString("email", null)
                    if (email == null) {
                        Toast.makeText(requireContext(), "Erreur : aucun utilisateur connecté", Toast.LENGTH_SHORT).show()
                        return@launch
                    }

                    val prof = withContext(Dispatchers.IO) { db.profDAO().getProfByEmail(email) }
                    if (prof != null) {
                        // Verification du mot de passe actuel
                        val mdpCorrect = PasswordUtils.verifyPassword(mdpActuel, prof.password)
                        if (!mdpCorrect) {
                            Toast.makeText(requireContext(), "Mot de passe actuel incorrect !", Toast.LENGTH_SHORT).show()
                            return@launch
                        }

                        // Hashage du mot de passe avant sauvegarde (bonne pratique de sécurité)
                        val hashedPassword = PasswordUtils.hashPassword(nouveauMdp)
                        prof.password = hashedPassword

                        // Sauvegarde dans la base
                        withContext(Dispatchers.IO) { db.profDAO().update(prof) }

                        // Réinitialisation de l’interface après la mise à jour
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


        // --- GESTION DU BOUTON DÉCONNEXION ---
        disconnectButton.setOnClickListener {
            // Supprimer l’utilisateur en mémoire (si tu stockes une session)
            sharedPrefs.edit().putString("email", "").apply()
            sharedPrefs.edit().putString("password", "").apply()
            (activity as? MainActivity)?.showFragment(ConnexionFragment(), false, false)
        }
    }
}
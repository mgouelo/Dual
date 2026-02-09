package fr.iutvannes.dual.controller.fragments

import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import fr.iutvannes.dual.R
import fr.iutvannes.dual.controller.MainActivity
import fr.iutvannes.dual.model.database.AppDatabase
import fr.iutvannes.dual.model.persistence.Prof
import fr.iutvannes.dual.model.utils.PasswordUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Fragment pour la page d'inscription.
 * Affiche un formulaire pour l'inscription d'un nouveau professeur.
 * L'inscription est enregistrée dans la base de données.
 *
 * @see MainActivity
 * @see Prof
 * @see AppDatabase
 * @see PasswordUtils
 * @see R.layout.fragment_creation
 */
class InscriptionFragment : Fragment() {

    /* Variables de visibilté pour le mot de passe */
    private var passwordVisible = false

    /* Variables de visibilté pour le mot de passe de confirmation */
    private var passwordVerifVisible = false

    /**
     * Méthode appelée lors de la création de la vue du fragment.
     *
     * @param inflater L'inflatreur utilisé pour infler le layout du fragment.
     * @param container Le conteneur parent du fragment.
     * @param savedInstanceState Les données sauvegardées du fragment.
     * @return La vue du fragment.
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        // On utilise le layout de l’inscription
        val view = inflater.inflate(R.layout.fragment_creation, container, false)

        // --- RÉCUPÉRATION DES VUES ---
        val nomInput = view.findViewById<EditText>(R.id.Nom)
        val prenomInput = view.findViewById<EditText>(R.id.Prenom)
        val emailInput = view.findViewById<EditText>(R.id.Email)
        val passwordInput = view.findViewById<EditText>(R.id.passwordInput)
        val passwordVerifInput = view.findViewById<EditText>(R.id.passwordVerifInput)
        val oeilIcon = view.findViewById<ImageView>(R.id.oeilIcon)
        val oeilVerifIcon = view.findViewById<ImageView>(R.id.oeilVerifIcon)
        val inscriptionButton = view.findViewById<Button>(R.id.inscriptionButton)
        val connexionLien = view.findViewById<TextView>(R.id.connexionLien)

        // --- BASE DE DONNÉES ROOM ---
        val db = Room.databaseBuilder(
            requireContext(),
            AppDatabase::class.java,
            "dual.db"
        )
            .fallbackToDestructiveMigration() // supprime et recrée la DB si le schéma change
            .build()
        val dao = db.profDAO()

        // --- GESTION DE L’AFFICHAGE DU MOT DE PASSE ---
        // Gestion du clic sur l'icône d'oeil pour afficher/masquer le mot de passe
        oeilIcon.setOnClickListener {
            passwordVisible = !passwordVisible
            passwordInput.inputType = if (passwordVisible)
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            else
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            passwordInput.setSelection(passwordInput.text.length)
        }

        // Gestion du clic sur l'icône d'oeil pour afficher/masquer le mot de passe de confirmation
        oeilVerifIcon.setOnClickListener {
            passwordVerifVisible = !passwordVerifVisible
            passwordVerifInput.inputType = if (passwordVerifVisible)
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            else
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            passwordVerifInput.setSelection(passwordVerifInput.text.length)
        }

        // --- ACTION DU BOUTON D’INSCRIPTION ---
        // Gestion du clic sur le bouton d'inscription
        inscriptionButton.setOnClickListener {
            val nom = nomInput.text.toString().trim()
            val prenom = prenomInput.text.toString().trim()
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()
            val passwordVerif = passwordVerifInput.text.toString().trim()

            // Regex pour email simple
            val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$".toRegex()

            if (nom.isEmpty() || prenom.isEmpty() || email.isEmpty() || password.isEmpty() || passwordVerif.isEmpty()) {
                Toast.makeText(requireContext(), "Tous les champs sont obligatoires", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            } else if (password != passwordVerif) {
                Toast.makeText(requireContext(), "Les mots de passe ne correspondent pas", Toast.LENGTH_SHORT).show()
                passwordInput.setText("")
                passwordVerifInput.setText("")
                return@setOnClickListener
            } else if (!email.matches(emailRegex)) {
                Toast.makeText(requireContext(), "L'email n'est pas valide", Toast.LENGTH_SHORT).show()
                emailInput.setText("")
            } else if (!PasswordUtils.isValid(password) || !PasswordUtils.isValid(passwordVerif)) {
                Toast.makeText(requireContext(), "Le mot de passe doit contenir au moins 8 caractères dont une majuscule un chiffre et un symbole", Toast.LENGTH_SHORT).show()
                passwordInput.setText("")
                passwordVerifInput.setText("")
            } else {
                // --- SAUVEGARDE DANS LA BASE ---
                lifecycleScope.launch {
                    val existingProf = withContext(Dispatchers.IO) {
                        dao.getProfByEmail(email)
                    }

                    if (existingProf != null) {
                        Toast.makeText(requireContext(), "Un professeur avec cet email existe déjà", Toast.LENGTH_SHORT).show()
                        emailInput.setText("")
                    } else {
                        withContext(Dispatchers.IO) {
                            val prof = Prof(
                                nom = nom,
                                prenom = prenom,
                                email = email,
                                password = PasswordUtils.hashPassword(password)
                            )
                            dao.insert(prof)
                        }

                        Toast.makeText(requireContext(), "Inscription réussie", Toast.LENGTH_SHORT).show()

                        // Retour vers la page de connexion
                        (activity as? MainActivity)?.showFragment(ConnexionFragment(), false, false)
                    }
                }
            }
        }

        // --- LIEN "DÉJÀ UN COMPTE ? SE CONNECTER" ---
        // Gestion du clic sur le lien "déjà un compte ? se connecter"
        connexionLien.setOnClickListener {
            (activity as? MainActivity)?.showFragment(ConnexionFragment(), false, false)
        }

        return view
    }
}

package fr.iutvannes.dual.controller.fragments

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import fr.iutvannes.dual.R
import fr.iutvannes.dual.controller.MainActivity
import fr.iutvannes.dual.model.database.AppDatabase
import fr.iutvannes.dual.model.utils.EmailService
import fr.iutvannes.dual.model.utils.PasswordUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

/**
 * Fragment pour gérer la réinitialisation du mot de passe oublié.
 * Le layout associé est R.layout.fragment_forgotten_password.
 */
class ForgottenPasswordFragment : Fragment(R.layout.fragment_forgotten_password) {

    /**
     * Cette fonction est appelée lorsque la vue du fragment est créée.
     * Elle initialise les interactions avec les vues.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Récupération des vues
        val emailInput = view.findViewById<EditText>(R.id.Email)
        val lienButton = view.findViewById<Button>(R.id.lienButton)
        val inscriptionLien = view.findViewById<TextView>(R.id.inscriptionLien)

        // Accès à la base de données
        val db = Room.databaseBuilder(
            requireContext(),
            AppDatabase::class.java,
            "dual.db"
        ).build()

        // Gestion du clic sur le bouton de réinitialisation
        lienButton.setOnClickListener {
            // Verification de l'email
            val email = emailInput.text.toString().trim()
            if (email.isEmpty()) {
                Toast.makeText(requireContext(), "Veuillez entrer votre email", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Lancer une coroutine (code asynchrone) pour les opérations de base de données et d'email
            lifecycleScope.launch {
                // Vérifier si l'email existe dans la base de données
                val prof = withContext(Dispatchers.IO) {
                    db.profDAO().getProfByEmail(email)
                }

                if (prof == null) {
                    Toast.makeText(requireContext(), "Aucun compte associé à cet email", Toast.LENGTH_SHORT).show()
                } else {

                    // Générer un nouveau mot de passe temporaire
                    val newPassword = generateTempPassword()
                    val hashedPassword = PasswordUtils.hashPassword(newPassword)

                    // Mettre à jour en base de données
                    withContext(Dispatchers.IO) {
                        prof.password = hashedPassword
                        db.profDAO().update(prof)
                    }

                    // Envoyer l'email avec le nouveau mot de passe avec EmailService
                    val emailSent = EmailService.sendPasswordResetEmail(email, newPassword)

                    // Informer l'utilisateur du résultat
                    if (emailSent) {
                        Toast.makeText(requireContext(), "Email envoyé avec succès", Toast.LENGTH_LONG).show()
                        (activity as? MainActivity)?.showFragment(ConnexionFragment(), false, false)
                    } else {
                        Toast.makeText(requireContext(), "Erreur lors de l'envoi de l'email", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        // Gestion du clic sur le lien d'inscription
        inscriptionLien.setOnClickListener {
            (activity as? MainActivity)?.showFragment(ConnexionFragment(), false, false)
        }
    }

    /**
     * Génère un mot de passe temporaire aléatoire de 10 caractères.
     */
    private fun generateTempPassword(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        return (1..10)
            .map { chars.random() }
            .joinToString("")
    }
}

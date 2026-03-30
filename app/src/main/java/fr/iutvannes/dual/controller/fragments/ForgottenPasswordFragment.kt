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

/**
 * Fragment for handling forgotten password resets.
 * The associated layout is R.layout.fragment_forgotten_password.
 *
 * @see AppDatabase
 * @see MainActivity
 * @see EmailService
 * @see PasswordUtils
 * @see R.layout.fragment_forgotten_password
 */
class ForgottenPasswordFragment : Fragment(R.layout.fragment_forgotten_password) {

    /**
     * This function is called when the fragment view is created.
     * It initializes interactions with views.
     *
     * @param view The fragment view.
     * @param savedInstanceState The data saved during the activity's state.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Retrieving views
        val emailInput = view.findViewById<EditText>(R.id.Email)
        val lienButton = view.findViewById<Button>(R.id.lienButton)
        val inscriptionLien = view.findViewById<TextView>(R.id.inscriptionLien)

        // Access to the database
        val db = Room.databaseBuilder(
            requireContext(),
            AppDatabase::class.java,
            "dual.db"
        ).build()

        // Managing clicks on the reset button
        lienButton.setOnClickListener {
            // Verification de l'email
            val email = emailInput.text.toString().trim()
            if (email.isEmpty()) {
                Toast.makeText(requireContext(), "Veuillez entrer votre email", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Launch a coroutine (asynchronous code) for database and email operations
            lifecycleScope.launch {
                // Check if the email address exists in the database
                val prof = withContext(Dispatchers.IO) {
                    db.profDAO().getProfByEmail(email)
                }

                if (prof == null) {
                    Toast.makeText(requireContext(), "Aucun compte associé à cet email", Toast.LENGTH_SHORT).show()
                } else {

                    // Generate a new temporary password
                    val newPassword = generateTempPassword()
                    val hashedPassword = PasswordUtils.hashPassword(newPassword)

                    // Update database
                    withContext(Dispatchers.IO) {
                        prof.password = hashedPassword
                        db.profDAO().update(prof)
                    }

                    // Send the email with the new password using EmailService
                    val emailSent = EmailService.sendPasswordResetEmail(email, newPassword)

                    // Inform the user of the result
                    if (emailSent) {
                        Toast.makeText(requireContext(), "Email envoyé avec succès", Toast.LENGTH_LONG).show()
                        (activity as? MainActivity)?.showFragment(ConnexionFragment(), false, false)
                    } else {
                        Toast.makeText(requireContext(), "Erreur lors de l'envoi de l'email", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        // Managing clicks on the registration link
        inscriptionLien.setOnClickListener {
            (activity as? MainActivity)?.showFragment(ConnexionFragment(), false, false)
        }
    }

    /**
     * Generates a random 10-character temporary password.
     *
     * @return The generated temporary password.
     */
    private fun generateTempPassword(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        return (1..10)
            .map { chars.random() }
            .joinToString("")
    }
}

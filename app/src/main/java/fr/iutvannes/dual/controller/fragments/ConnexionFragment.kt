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
import fr.iutvannes.dual.model.utils.PasswordUtils
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.core.content.edit

/**
 * Connection fragment
 * This fragment is used to connect to the application.
 *
 * @see MainActivity
 * @see AppDatabase
 * @see PasswordUtils
 * @see R.layout.fragment_connexion
 */
class ConnexionFragment : Fragment() {

    /* Variable that determines whether the password is visible or not */
    private var passwordVisible = false

    /**
     * Method called when the fragment is created.
     *
     * @param inflater The inflator used to inflate the fragment's layout.
     * @param container The container in which the fragment will be displayed.
     * @param savedInstanceState The saved fragment data.
     * @return The fragment's view.
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val view = inflater.inflate(R.layout.fragment_connexion, container, false)

        val emailInput = view.findViewById<EditText>(R.id.Email)
        val passwordInput = view.findViewById<EditText>(R.id.passwordInput)
        val oeilIcon = view.findViewById<ImageView>(R.id.oeilIcon)
        val connexionButton = view.findViewById<Button>(R.id.connectionButton)
        val inscriptionLien = view.findViewById<TextView>(R.id.inscriptionLien)
        val rememberMe = view.findViewById<CheckBox>(R.id.rememberMeCheckBox)
        val forgottenPassword = view.findViewById<TextView>(R.id.forgottenPassword)


        // Creating a secret key for secure storage of preferences
        val masterKey = MasterKey.Builder(requireContext())
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        // Creating a secure storage object for preferences
        val sharedPref = EncryptedSharedPreferences.create(
            requireContext(),
            "loginPrefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

        // Retrieving login preferences
        val editor = sharedPref.edit()

        // Creating or opening the database
        val db = Room.databaseBuilder(
            requireContext(),
            AppDatabase::class.java,
            "dual.db"
        )
            .fallbackToDestructiveMigration() // Deletes and recreates the database if the schema changes
            .build()

        val dao = db.profDAO()

        val savedEmail = sharedPref.getString("email", "")
        val savedPassword = sharedPref.getString("password", "")
        val isRemembered = sharedPref.getBoolean("rememberMe", false)

        // If connection preferences exist, load them
        if (isRemembered) {
            emailInput.setText(savedEmail)
            passwordInput.setText(savedPassword)
            rememberMe.isChecked = true
        }

        // Displaying the password
        oeilIcon.setOnClickListener {
            passwordVisible = !passwordVisible
            passwordInput.inputType = if (passwordVisible)
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            else
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            passwordInput.setSelection(passwordInput.text.length)
        }

        // Managing clicks on the login button
        connexionButton.setOnClickListener {
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()

            // Simple regex for email
            val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$".toRegex()

            if (email.isEmpty()) {
                Toast.makeText(requireContext(), "Veuillez entrer votre email", Toast.LENGTH_SHORT).show()
            } else if (password.isEmpty()) {
                Toast.makeText(requireContext(), "Veuillez entrer votre mot de passe", Toast.LENGTH_SHORT).show()
            } else if (!email.matches(emailRegex)) {
                Toast.makeText(requireContext(), "L'email n'est pas valide", Toast.LENGTH_SHORT).show()
                emailInput.setText("")
            } else {
                // We launch a coroutine to access the database
                lifecycleScope.launch {
                    val prof = withContext(Dispatchers.IO) {
                        dao.getProfByEmail(email)
                    }

                    if (prof == null) {
                        Toast.makeText(requireContext(), "Cet email n'est pas enregistré", Toast.LENGTH_SHORT).show()
                        emailInput.setText("")
                    } else if (!PasswordUtils.verifyPassword(password, prof.password)) {
                        Toast.makeText(requireContext(), "Mot de passe incorrect", Toast.LENGTH_SHORT).show()
                        passwordInput.setText("")
                    } else {
                        Toast.makeText(requireContext(), "Connexion réussie !", Toast.LENGTH_SHORT).show()
                        if (rememberMe.isChecked) {
                            editor.putString("email", emailInput.text.toString())
                            editor.putString("password", passwordInput.text.toString())
                            editor.putBoolean("rememberMe", true)
                            editor.apply()
                        } else {
                            // We just delete the unnecessary fields, but we keep the email address.
                            editor.remove("password")
                            editor.putBoolean("rememberMe", false)
                            editor.apply()
                        }

                        // Save the email address for the top bar in the dashboard
                        sharedPref.edit { putString("email", email) }
                        // Update the top bar
                        (activity as? MainActivity)?.chargerUtilisateur()

                        (activity as? MainActivity)?.showFragment(TableauDeBordFragment(), true, true)
                    }
                }
            }
        }

        // Managing clicks on the registration link
        inscriptionLien.setOnClickListener {
            (activity as? MainActivity)?.showFragment(InscriptionFragment(), false, false)
        }

        // Managing the click on the forgotten password link
        forgottenPassword.setOnClickListener {
            (activity as? MainActivity)?.showFragment(ForgottenPasswordFragment(), false, false)
        }

        return view
    }
}

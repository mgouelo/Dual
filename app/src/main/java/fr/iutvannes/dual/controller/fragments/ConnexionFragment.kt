package fr.iutvannes.dual.controller.fragments

import android.os.Bundle
import android.text.InputType
import android.util.Log
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
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import fr.iutvannes.dual.model.utils.PasswordUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ConnexionFragment : Fragment() {

    private var passwordVisible = false

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

        val masterKey = MasterKey.Builder(requireContext())
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        val sharedPref = EncryptedSharedPreferences.create(
            requireContext(),
            "loginPrefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
        val editor = sharedPref.edit()

        val db = Room.databaseBuilder(
            requireContext(),
            AppDatabase::class.java,
            "dual.db"
        )
            .fallbackToDestructiveMigration() // supprime et recrée la DB si le schéma change
            .build()

        val dao = db.profDAO()

        val savedEmail = sharedPref.getString("email", "")
        val savedPassword = sharedPref.getString("password", "")
        val isRemembered = sharedPref.getBoolean("rememberMe", false)

        if (isRemembered) {
            emailInput.setText(savedEmail)
            passwordInput.setText(savedPassword)
            rememberMe.isChecked = true
        }

        oeilIcon.setOnClickListener {
            passwordVisible = !passwordVisible
            passwordInput.inputType = if (passwordVisible)
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            else
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            passwordInput.setSelection(passwordInput.text.length)
        }

        connexionButton.setOnClickListener {
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()

            // Regex simple pour email
            val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$".toRegex()

            if (email.isEmpty()) {
                Toast.makeText(requireContext(), "Veuillez entrer votre email", Toast.LENGTH_SHORT).show()
            } else if (password.isEmpty()) {
                Toast.makeText(requireContext(), "Veuillez entrer votre mot de passe", Toast.LENGTH_SHORT).show()
            } else if (!email.matches(emailRegex)) {
                Toast.makeText(requireContext(), "L'email n'est pas valide", Toast.LENGTH_SHORT).show()
                emailInput.setText("")
            } else {
                // On lance une coroutine pour accéder à la DB
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
                            // On efface juste les champs inutiles, mais on garde l'email
                            editor.remove("password")
                            editor.putBoolean("rememberMe", false)
                            editor.apply()
                        }
                        (activity as? MainActivity)?.showFragment(TableauDeBordFragment(), true, true)
                    }
                }
            }
        }

        inscriptionLien.setOnClickListener {
            (activity as? MainActivity)?.showFragment(InscriptionFragment(), false, false)
        }

        forgottenPassword.setOnClickListener {
            (activity as? MainActivity)?.showFragment(ForgottenPasswordFragment(), false, false)
        }

        return view
    }
}

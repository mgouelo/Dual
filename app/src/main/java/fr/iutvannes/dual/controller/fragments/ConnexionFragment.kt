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
 * Fragment pour la connexion
 * Ce fragment est utilisé pour se connecter à l'application.
 *
 * @see MainActivity
 * @see AppDatabase
 * @see PasswordUtils
 * @see R.layout.fragment_connexion
 */
class ConnexionFragment : Fragment() {

    /* Variable permettant de savoir si le mot de passe est visible ou non */
    private var passwordVisible = false

    /**
     * Méthode appelée lorsque le fragment est créé.
     *
     * @param inflater L'inflatreur utilisé pour infler le layout du fragment.
     * @param container Le conteneur dans lequel le fragment sera affiché.
     * @param savedInstanceState Les données sauvegardées du fragment.
     * @return La vue du fragment.
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


        // Création d'une clé secrète pour le stockage sécurisé des préférences
        val masterKey = MasterKey.Builder(requireContext())
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        // Création d'un objet de stockage sécurisé des préférences
        val sharedPref = EncryptedSharedPreferences.create(
            requireContext(),
            "loginPrefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

        // Récupération des préférences de connexion
        val editor = sharedPref.edit()

        // Création ou ouverture de la base de données
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

        // Si les préférences de connexion existent, les charger
        if (isRemembered) {
            emailInput.setText(savedEmail)
            passwordInput.setText(savedPassword)
            rememberMe.isChecked = true
        }

        // Affichage du mot de passe
        oeilIcon.setOnClickListener {
            passwordVisible = !passwordVisible
            passwordInput.inputType = if (passwordVisible)
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            else
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            passwordInput.setSelection(passwordInput.text.length)
        }

        // Gestion du clic sur le bouton de connexion
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

                        // Enregistrer l'email pour la top bar dans le tableau de bord
                        sharedPref.edit { putString("email", email) }
                        // Mettre à jour la top bar
                        (activity as? MainActivity)?.chargerUtilisateur()

                        (activity as? MainActivity)?.showFragment(TableauDeBordFragment(), true, true)
                    }
                }
            }
        }

        // Gestion du clic sur le lien d'inscription
        inscriptionLien.setOnClickListener {
            (activity as? MainActivity)?.showFragment(InscriptionFragment(), false, false)
        }

        // Gestion du clic sur le lien de mot de passe oublié
        forgottenPassword.setOnClickListener {
            (activity as? MainActivity)?.showFragment(ForgottenPasswordFragment(), false, false)
        }

        return view
    }
}

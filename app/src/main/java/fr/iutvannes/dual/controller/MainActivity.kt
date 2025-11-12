package fr.iutvannes.dual.controller

import android.os.Bundle
import android.view.View // Import pour gérer la visibilité (View.VISIBLE, View.GONE)
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import fr.iutvannes.dual.R
import fr.iutvannes.dual.controller.fragments.ConnexionFragment
import fr.iutvannes.dual.controller.fragments.ClassesFragment
import fr.iutvannes.dual.controller.fragments.InscriptionFragment
import fr.iutvannes.dual.controller.fragments.ProfilFragment
import fr.iutvannes.dual.controller.fragments.TableauDeBordFragment
import fr.iutvannes.dual.model.database.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.text.replace

class MainActivity : AppCompatActivity() {

    // Vues pour la barre de navigation et son conteneur
    private lateinit var navBarContainer: ConstraintLayout
    private lateinit var navHomeButton: LinearLayout
    private lateinit var navClassesButton: LinearLayout
    private lateinit var topBarContainer: ConstraintLayout


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Code pour le plein écran...
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val insetsController = WindowInsetsControllerCompat(window, window.decorView)
        insetsController.hide(WindowInsetsCompat.Type.systemBars())
        insetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        setContentView(R.layout.activity_main)

        // --- RÉFÉRENCES AUX CHAMPS DE TEXTE ---
        val prenomLabel = findViewById<TextView>(R.id.prenomLabel)

        // Listener pour le bouton de profil
        val profileButton = findViewById<ImageButton>(R.id.profileImage)
        profileButton.setOnClickListener {
            showFragment(ProfilFragment(), false, false)
        }

        // --- GESTION DE LA NAVIGATION ---

        // Récupérer les vues globales de la barre de navigation
        navBarContainer = findViewById(R.id.bottomNav)
        navHomeButton = findViewById(R.id.nav_home_button)
        navClassesButton = findViewById(R.id.nav_classes_button)

        // --- GESTION DU PROFIL ---
        topBarContainer = findViewById(R.id.topBar)


        // Définir les actions des clics
        navHomeButton.setOnClickListener {
            if (supportFragmentManager.findFragmentById(R.id.fragmentContainer) !is TableauDeBordFragment) {
                showFragment(TableauDeBordFragment(), true, true)
            }
        }
        navClassesButton.setOnClickListener {
            if (supportFragmentManager.findFragmentById(R.id.fragmentContainer) !is ClassesFragment) {
                showFragment(ClassesFragment(), true, true)
            }
        }


        // --- CONNEXION À LA BASE ---
        val db = Room.databaseBuilder(
            this,
            AppDatabase::class.java,
            "dual.db"
        ).build()

        // Récupère les SharedPreferences sécurisées
        val masterKey = MasterKey.Builder(this)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        val sharedPref = EncryptedSharedPreferences.create(
            this,
            "loginPrefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
        val isRemembered = sharedPref.getBoolean("rememberMe", false)

        // Charger les infos du prof connecté (depuis la base)
        // lifectcleScope.launch est utilisé pour exécuter du code asynchrone permettant de ne pas bloquer l'UI lorsque l'utilisateur change de fragment
        // (l'opération est annulée si on change de fragment)
        lifecycleScope.launch {
            val profConnecte = withContext(Dispatchers.IO) {
                // Exemple : ici, on suppose qu'on a stocké l'email du prof connecté
                val email = sharedPref.getString("email", null)
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
                prenomLabel.setText(it.prenom)
            }
        }

        // --- ÉTAT INITIAL ---
        if (savedInstanceState == null) {
            if (isRemembered) {
                showFragment(TableauDeBordFragment(), true, true)
            } else {
                showFragment(ConnexionFragment(), false, false)
            }
        }

    }

    /**
     * Remplace le fragment actuel ET gère la visibilité de la barre de navigation.
     */
    fun showFragment(fragment: Fragment, withNavigation: Boolean, withTopBar: Boolean) {

        // Gérer la visibilité de la barre de navigation
        findViewById<View>(R.id.bottomNav)?.apply {
            visibility = if (withNavigation) View.VISIBLE else View.GONE
        }

        // Gérer la visibilité de la top bar
        findViewById<View>(R.id.topBar)?.apply {
            visibility = if (withTopBar) View.VISIBLE else View.GONE
        }

        // --- Met à jour la sélection du bouton de navigation ---
        when (fragment) {
            is TableauDeBordFragment -> selectNavItem(navHomeButton)
            is ClassesFragment -> selectNavItem(navClassesButton)
            else -> {
                // aucun bouton sélectionné (profil, connexion, etc.)
                navHomeButton.isSelected = false
                navClassesButton.isSelected = false
            }
        }

        // Afficher le fragment
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(
                R.anim.slide_in_right, // entrée
                R.anim.fade_out,       // sortie
                R.anim.fade_in,        // retour
                R.anim.slide_out_right // retour inverse
            )
            .replace(R.id.fragmentContainer, fragment)
            .addToBackStack(null) // Permet le retour avec le bouton retour
            .commit()
    }

    /**
     * Gère la sélection visuelle des boutons (change la couleur).
     * Met les boutons à false et le bouton passé en paramètre à true afin d'éviter les problèmes de couleur.
     * Ensuite on affiche la configuration actuelle avec itemToSelect à true (le bouton selectionner change de couleur).
     */
    private fun selectNavItem(itemToSelect: LinearLayout) {
        navHomeButton.isSelected = false
        navClassesButton.isSelected = false
        itemToSelect.isSelected = true
    }
}



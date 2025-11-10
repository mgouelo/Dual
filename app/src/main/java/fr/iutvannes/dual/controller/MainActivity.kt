package fr.iutvannes.dual.controller

import android.os.Bundle
import android.view.View // Import pour gérer la visibilité (View.VISIBLE, View.GONE)
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.Fragment
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import fr.iutvannes.dual.R
import fr.iutvannes.dual.controller.fragments.ConnexionFragment
import fr.iutvannes.dual.controller.fragments.ClassesFragment
import fr.iutvannes.dual.controller.fragments.InscriptionFragment
import fr.iutvannes.dual.controller.fragments.ProfilFragment
import fr.iutvannes.dual.controller.fragments.TableauDeBordFragment
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



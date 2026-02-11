package fr.iutvannes.dual.controller

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.view.View // Import pour gérer la visibilité (View.VISIBLE, View.GONE)
import android.widget.ImageButton
import android.widget.ImageView
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
import com.bumptech.glide.Glide
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

    /** SharedPreferences pour la connexion cela permet de garder l'email même après un redémarrage */
    private lateinit var sharedPref: SharedPreferences
    /** Base de données */
    private lateinit var db: AppDatabase


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Code pour le plein écran...
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val insetsController = WindowInsetsControllerCompat(window, window.decorView)
        insetsController.hide(WindowInsetsCompat.Type.systemBars())
        insetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        setContentView(R.layout.activity_main)



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

        // Initialiser la base
        DatabaseProvider.init(this)
        db = DatabaseProvider.db

        // Récupère les SharedPreferences sécurisées
        val masterKey = MasterKey.Builder(this)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        sharedPref = EncryptedSharedPreferences.create(
            this,
            "loginPrefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
        val isRemembered = sharedPref.getBoolean("rememberMe", false)

        // Premier essai de chargement
        chargerUtilisateur()

        // Listener pour le bouton de profil
        val profileButton = findViewById<ImageButton>(R.id.profileImage)
        profileButton.setOnClickListener {
            showFragment(ProfilFragment(), false, false)
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

    /**
     * Charge l'utilisateur depuis la base de données et met à jour la top bar.
     */
    fun chargerUtilisateur() {
        val email = sharedPref.getString("email", null)
        val prenomLabel = findViewById<TextView>(R.id.prenomLabel)
        val profileButton = findViewById<ImageButton>(R.id.profileImage)

        if (email != null) {
            db.profDAO().getProfLive(email).observe(this) { prof ->
                if (prof != null) {
                    prenomLabel.text = prof.prenom

                    chargerPhotoProfil(
                        context = this,
                        pdp = profileButton,
                        nom = prof.nom,
                        prenom = prof.prenom,
                        photoUri = prof.photoUri // Room renvoie le chemin ou null
                    )
                } else {
                    prenomLabel.setText(" ")
                    profileButton.setImageResource(R.drawable.pfp)
                }
            }
        }
    }


    fun chargerPhotoProfil(context: Context, pdp: ImageButton, nom: String, prenom: String, photoUri: String?) {

        //
        val imageACharger: Any = if (photoUri != null) {
            // l'utilisateur a une photo perso alors on prend l'uri
            Uri.parse(photoUri)
        } else {
            // pas de photo alors on génère l'url de la pfp par défaut pour l'appel API
            "https://ui-avatars.com/api/?name=$prenom+$nom&background=random&color=fff&size=128&bold=true"
        }

        // Le plugin glide s'occupe de l'affichage de la photo
        Glide.with(context)
            .load(imageACharger)
            .circleCrop() // format circulaire
            .placeholder(R.drawable.pfp) // image pendant le chargement
            .error(R.drawable.pfp)       // image si erreur
            .into(pdp)
    }
}



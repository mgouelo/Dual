package fr.iutvannes.dual.controller

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.view.View // Import pour gérer la visibilité (View.VISIBLE, View.GONE)
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.Fragment
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.bumptech.glide.Glide
import fr.iutvannes.dual.R
import fr.iutvannes.dual.controller.fragments.ConnexionFragment
import fr.iutvannes.dual.controller.fragments.ClassesFragment
import fr.iutvannes.dual.controller.fragments.ProfilFragment
import fr.iutvannes.dual.controller.fragments.TableauDeBordFragment
import fr.iutvannes.dual.model.database.AppDatabase

/**
 * Activité principale de l'application.
 *
 * @see ConnexionFragment
 * @see ClassesFragment
 * @see ProfilFragment
 * @see TableauDeBordFragment
 * @see AppDatabase
 * @see R.layout.activity_main
 */
class MainActivity : AppCompatActivity() {

    // Views for the navigation bar and its container

    /* Variable to manage the navigation bar */
    private lateinit var navBarContainer: ConstraintLayout

    /* Variable to manage the navigation buttons */
    private lateinit var navHomeButton: LinearLayout

    /* Variable to manage the navigation buttons */
    private lateinit var navClassesButton: LinearLayout

    /* Variable to manage the top bar */
    private lateinit var topBarContainer: ConstraintLayout

    /* SharedPreferences for the connection allows you to keep your email even after a restart */
    private lateinit var sharedPref: SharedPreferences

    /* Database */
    private lateinit var db: AppDatabase

    /**
     * Method called when the activity is created.
     *
     * @param savedInstanceState Saved state data
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Code for full screen...
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val insetsController = WindowInsetsControllerCompat(window, window.decorView)
        insetsController.hide(WindowInsetsCompat.Type.systemBars())
        insetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        setContentView(R.layout.activity_main)

        // --- NAVIGATION MANAGEMENT ---
        // Retrieve global views of the navigation bar
        navBarContainer = findViewById(R.id.bottomNav)
        navHomeButton = findViewById(R.id.nav_home_button)
        navClassesButton = findViewById(R.id.nav_classes_button)

        // --- PROFILE MANAGEMENT ---
        topBarContainer = findViewById(R.id.topBar)


        // Define the actions of clicks
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

        // Initialize the database
        DatabaseProvider.init(this)
        db = DatabaseProvider.db

        // Retrieve secure SharedPreferences
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

        // First loading test
        chargerUtilisateur()

        // Listener for the profile button
        val profileButton = findViewById<ImageButton>(R.id.profileImage)
        profileButton.setOnClickListener {
            showFragment(ProfilFragment(), false, false)
        }

        // --- INITIAL STATE ---
        if (savedInstanceState == null) {
            if (isRemembered) {
                showFragment(TableauDeBordFragment(), true, true)
            } else {
                showFragment(ConnexionFragment(), false, false)
            }
        }

    }

    /**
     * Replaces the current fragment AND manages the visibility of the navigation bar.
     *
     * @param fragment The fragment to display
     * @param withNavigation Whether to display the navigation bar or not
     * @param withTopBar Whether to display the top bar or not
     */
    fun showFragment(fragment: Fragment, withNavigation: Boolean, withTopBar: Boolean) {

        // Manage the visibility of the navigation bar
        findViewById<View>(R.id.bottomNav)?.apply {
            visibility = if (withNavigation) View.VISIBLE else View.GONE
        }

        // Manage the visibility of the top bar
        findViewById<View>(R.id.topBar)?.apply {
            visibility = if (withTopBar) View.VISIBLE else View.GONE
        }

        // --- Updates the navigation button selection ---
        when (fragment) {
            is TableauDeBordFragment -> selectNavItem(navHomeButton)
            is ClassesFragment -> selectNavItem(navClassesButton)
            else -> {
                // No button selected (profile, login, etc.)
                navHomeButton.isSelected = false
                navClassesButton.isSelected = false
            }
        }

        // Show fragment
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(
                R.anim.slide_in_right, // entrance
                R.anim.fade_out,       // exit
                R.anim.fade_in,        // back
                R.anim.slide_out_right // reverse back
            )
            .replace(R.id.fragmentContainer, fragment)
            .addToBackStack(null) // Allows return using the return button
            .commit()
    }

    /**
     * Manages the visual selection of buttons (changes the color).
     * Sets the buttons to false and the button passed as a parameter to true to avoid color issues.
     * Then displays the current configuration with itemToSelect set to true (the select button changes color).
     *
     * @param itemToSelect The button to select
     */
    private fun selectNavItem(itemToSelect: LinearLayout) {
        navHomeButton.isSelected = false
        navClassesButton.isSelected = false
        itemToSelect.isSelected = true
    }

    /**
     * Loads the user from the database and updates the top bar.
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

    /**
     * Loads the photo of the user.
     *
     * @param context The context of the activity.
     * @param pdp The ImageView to display the photo.
     * @param nom The user's name.
     * @param prenom The user's first name.
     * @param photoUri The URI of the user's photo.
     */
    fun chargerPhotoProfil(context: Context, pdp: ImageButton, nom: String, prenom: String, photoUri: String?) {

        val imageACharger: Any = if (photoUri != null) {
            // The user has a personal photo, so we take the URI.
            Uri.parse(photoUri)
        } else {
            // No photo, so we generate the default PFP URL for the API call.
            "https://ui-avatars.com/api/?name=$prenom+$nom&background=random&color=fff&size=128&bold=true"
        }

        // The glide plugin handles the display of the photo.
        Glide.with(context)
            .load(imageACharger)
            .circleCrop() // format circulaire
            .placeholder(R.drawable.pfp) // Image while loading
            .error(R.drawable.pfp)       // Image if error
            .into(pdp)
    }
}



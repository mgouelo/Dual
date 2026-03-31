package fr.iutvannes.dual.controller.fragments

// Imports
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import fr.iutvannes.dual.R
import fr.iutvannes.dual.controller.MainActivity
import fr.iutvannes.dual.model.database.AppDatabase
import fr.iutvannes.dual.model.utils.PasswordUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.core.content.edit
import com.bumptech.glide.Glide
import fr.iutvannes.dual.model.persistence.Prof
import fr.iutvannes.dual.model.utils.DatabaseProvider

/**
 * Fragment affichant le profil utilisateur
 * @see AppDatabase
 * @see MainActivity
 * @see PasswordUtils
 * @see R.layout.fragment_profil
 */
class ProfilFragment : Fragment(R.layout.fragment_profil) {

    /* photo de profile de l'utilisateur */
    private lateinit var pdp: ImageView

    /* enregistre l'enseignant actuellement connecté */
    private var profConnecte: Prof? = null

    // initialise le pop up de sléection de la photo
    private val pickMedia = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            Log.d("PhotoPicker", "Photo sélectionnée : $uri")
            val flag = Intent.FLAG_GRANT_READ_URI_PERMISSION
            requireContext().contentResolver.takePersistableUriPermission(uri, flag)

            Glide.with(this)
                .load(uri)
                .circleCrop()
                .into(pdp)

            profConnecte?.let { prof ->
                sauvegarderPhotoEnBase(uri.toString(), prof)
            }
        } else {
            Log.d("PhotoPicker", "Pas de photo sélectionnée")
        }
    }


    /**
     * fonction appelée à la création du fragment
     * initialise les interractions avec la vue
     *
     * @param view la vue
     * @param savedInstanceState donnée sauvegardées par le fragment
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        pdp = view.findViewById<ImageView>(R.id.profileImage)

        // bindings
        val nomField = view.findViewById<EditText>(R.id.nomField)
        val prenomField = view.findViewById<EditText>(R.id.prenomField)
        val adresseField = view.findViewById<EditText>(R.id.adresseField)
        val mdpField = view.findViewById<EditText>(R.id.mdpField)
        val nouveauMdpField = view.findViewById<EditText>(R.id.nouveau_mdpField)
        val confirmerMdpField = view.findViewById<EditText>(R.id.confirmer_nouveau_mdpField)
        val userProfilTxt = view.findViewById<TextView>(R.id.user_profil_txt)

        val editButtonProfil = view.findViewById<ImageButton>(R.id.editButtonProfil)
        val editButtonMdp = view.findViewById<ImageButton>(R.id.editButtonMdp)
        val disconnectButton = view.findViewById<Button>(R.id.btnDisconnect)
        val backButton = view.findViewById<ImageButton>(R.id.arrow_back_button)


        // connexion à la base de données
        val db = DatabaseProvider.db

        val masterKey = MasterKey.Builder(requireContext())
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        val sharedPrefs = EncryptedSharedPreferences.create(
            requireContext(),
            "loginPrefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

        lifecycleScope.launch {
            profConnecte = withContext(Dispatchers.IO) {
                val email = sharedPrefs.getString("email", null)
                if (email != null) {
                    db.profDAO().getProfByEmail(email)
                } else {
                    null
                }
            }

            // si un enseignant est trouvé, on pré-remplis les champs
            profConnecte?.let { prof ->
                userProfilTxt.setText(prof.prenom)
                nomField.setText(prof.nom)
                prenomField.setText(prof.prenom)
                adresseField.setText(prof.email)

                chargerPhotoProfil(
                    requireContext(),
                    pdp,
                    prof.nom,
                    prof.prenom,
                    prof.photoUri // null ou l'uri de la pdp
                )
            }
        }

        // bouton de retour
        backButton.setOnClickListener {
            (activity as? MainActivity)?.showFragment(TableauDeBordFragment(), true, true)
        }

        pdp.setOnClickListener {
            pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) // ouvre la galerie android sur image only
        }

        // mode edition du profil
        editButtonProfil.setOnClickListener {
            val isEditable = !nomField.isEnabled

            // active ou desactive ces champs
            nomField.isEnabled = isEditable
            prenomField.isEnabled = isEditable
            adresseField.isEnabled = isEditable

            if (!isEditable) {
                lifecycleScope.launch {  // lance une coroutine

                    val email = sharedPrefs.getString("email", null)
                    if (email == null) {
                        // si aucune adresse email est trouvée...
                        Toast.makeText(requireContext(), "Erreur : aucun utilisateur connecté", Toast.LENGTH_SHORT).show()
                        return@launch
                    }

                    // recherche le prof correspondant à l'email en base
                    val prof = withContext(Dispatchers.IO) { db.profDAO().getProfByEmail(email) }

                    if (prof != null) {
                        // mode edition desactivé
                        nomField.setBackgroundResource(0)
                        prenomField.setBackgroundResource(0)
                        adresseField.setBackgroundResource(0)
                        val defaultColor = resources.getColor(android.R.color.black, null)
                        nomField.setTextColor(defaultColor)
                        prenomField.setTextColor(defaultColor)
                        adresseField.setTextColor(defaultColor)

                        // si un prof a été trouvé, les valeurs sont sauvegardées
                        val newNom = nomField.text.toString().trim()
                        val newPrenom = prenomField.text.toString().trim()
                        val newEmail = adresseField.text.toString().trim()

                        // vérification des champs obligatoires
                        if (newNom.isBlank() || newPrenom.isBlank() || newEmail.isBlank()) {
                            // si un champs est vide...
                            Toast.makeText(requireContext(), "Tous les champs doivent être remplis", Toast.LENGTH_SHORT).show()
                            return@launch
                        }

                        // regex email
                        val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$".toRegex()
                        if (!newEmail.matches(emailRegex)) {
                            // si l'email est invalide...
                            Toast.makeText(requireContext(), "Email invalide", Toast.LENGTH_SHORT).show()
                            return@launch
                        }

                        // mise à jour des informations
                        prof.nom = newNom
                        prof.prenom = newPrenom
                        prof.email = newEmail

                        // enregistrement des nouvelles informations dans une coroutine
                        withContext(Dispatchers.IO) { db.profDAO().update(prof) }

                        // si changement d'adresse alors on enregistre la nouvelle
                        sharedPrefs.edit { putString("email", newEmail) }

                        // message de confirmation
                        Toast.makeText(requireContext(), "Profil mis à jour", Toast.LENGTH_SHORT).show()
                    } else {
                        // si aucun prof n'a été trouvé avec cet email
                        Toast.makeText(requireContext(), "Utilisateur introuvable", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                // on rend les champs modifiables
                Toast.makeText(requireContext(), "Mode édition activé", Toast.LENGTH_SHORT).show()
                val highlightColor = resources.getColor(R.color.gris, null)
                nomField.setBackgroundResource(android.R.drawable.edit_text)
                prenomField.setBackgroundResource(android.R.drawable.edit_text)
                adresseField.setBackgroundResource(android.R.drawable.edit_text)
                nomField.setTextColor(highlightColor)
                prenomField.setTextColor(highlightColor)
                adresseField.setTextColor(highlightColor)
            }
        }

        // Bouton de modification du mot de passe
        editButtonMdp.setOnClickListener {
            // Inverse l'état du mode édition
            val isEditable = !nouveauMdpField.isEnabled

            // active ou désactive les champs de mot de passe
            mdpField.isEnabled = isEditable
            nouveauMdpField.isEnabled = isEditable
            confirmerMdpField.isEnabled = isEditable

            if (isEditable) {
                // mode édition activé
                val highlightColor = resources.getColor(R.color.gris, null)
                mdpField.setBackgroundResource(android.R.drawable.edit_text)
                nouveauMdpField.setBackgroundResource(android.R.drawable.edit_text)
                confirmerMdpField.setBackgroundResource(android.R.drawable.edit_text)
                mdpField.setTextColor(highlightColor)
                nouveauMdpField.setTextColor(highlightColor)
                confirmerMdpField.setTextColor(highlightColor)

                Toast.makeText(requireContext(), "Modification du mot de passe activée", Toast.LENGTH_SHORT).show()
            }
            else {
                // L'utilisateur quitte le mode édition --> on sauvegarde en base les informations
                val mdpActuel = mdpField.text.toString().trim()
                val nouveauMdp = nouveauMdpField.text.toString().trim()
                val confirmer = confirmerMdpField.text.toString().trim()

                // verification de présence de champs vides
                if (nouveauMdp.isBlank() || confirmer.isBlank() || mdpActuel.isBlank()) {
                    Toast.makeText(requireContext(), "Veuillez remplir tous les champs", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                // verification de l'égalité
                if (nouveauMdp != confirmer) {
                    Toast.makeText(requireContext(), "Les mots de passe ne correspondent pas", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                // vérification minimale du mdp
                val motDePasseValide = nouveauMdp.length >= 8 &&
                        nouveauMdp.any { it.isUpperCase() } &&
                        nouveauMdp.any { it.isDigit() }

                if (!motDePasseValide) {
                    Toast.makeText(requireContext(),
                        "Le mot de passe doit contenir au moins 8 caractères, une majuscule et un chiffre",
                        Toast.LENGTH_LONG
                    ).show()
                    return@setOnClickListener
                }

                // mise à jour du mot de passe en base depuis une coroutine
                lifecycleScope.launch {
                    val email = sharedPrefs.getString("email", null)
                    if (email == null) {
                        Toast.makeText(requireContext(), "Erreur : aucun utilisateur connecté", Toast.LENGTH_SHORT).show()
                        return@launch
                    }

                    val prof = withContext(Dispatchers.IO) { db.profDAO().getProfByEmail(email) }
                    if (prof != null) {
                        // verification du mot de passe actuel
                        val mdpCorrect = PasswordUtils.verifyPassword(mdpActuel, prof.password)
                        if (!mdpCorrect) {
                            Toast.makeText(requireContext(), "Mot de passe actuel incorrect !", Toast.LENGTH_SHORT).show()
                            return@launch
                        }

                        // hash du mot de passe
                        val hashedPassword = PasswordUtils.hashPassword(nouveauMdp)
                        prof.password = hashedPassword

                        // sauvegarde en base
                        withContext(Dispatchers.IO) { db.profDAO().update(prof) }

                        // reconstruction de l'interface après mise à jour
                        mdpField.text.clear()
                        nouveauMdpField.text.clear()
                        confirmerMdpField.text.clear()
                        mdpField.isEnabled = false
                        nouveauMdpField.isEnabled = false
                        confirmerMdpField.isEnabled = false
                        mdpField.setBackgroundResource(0)
                        nouveauMdpField.setBackgroundResource(0)
                        confirmerMdpField.setBackgroundResource(0)
                        val defaultColor = resources.getColor(android.R.color.black, null)
                        mdpField.setTextColor(defaultColor)
                        nouveauMdpField.setTextColor(defaultColor)
                        confirmerMdpField.setTextColor(defaultColor)

                        Toast.makeText(requireContext(), "Mot de passe mis à jour", Toast.LENGTH_SHORT).show()
                    }
                    else {
                        Toast.makeText(requireContext(), "Utilisateur introuvable", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }


        // bouton de déconnexion
        disconnectButton.setOnClickListener {
            // supprime l'utilisateur de la mémoire cache
            sharedPrefs.edit { putString("email", "") }
            sharedPrefs.edit { putString("password", "") }
            (activity as? MainActivity)?.showFragment(ConnexionFragment(), false, false)
        }
    }

    /**
     * Initialise la photo de profil utilisateur
     *
     * @param context contexte de l'app
     * @param imageView imageView qui affichera la photo
     * @param nom nom de famille
     * @param prenom prénom
     * @param photoUri uri de la photo de l'utilisateur
     */
    fun chargerPhotoProfil(context: Context, imageView: ImageView, nom: String, prenom: String, photoUri: String?) {

        val imageACharger: Any = if (photoUri != null) {
            // si l'utilisateur a une photo personnalisé on sauvegarde l'uri
            Uri.parse(photoUri)
        } else {
            // si pas de photo on génère la photo de profil aléatoire
            "https://ui-avatars.com/api/?name=$prenom+$nom&background=random&color=fff&size=128&bold=true"
        }

        // plugin glide pour personnaliser l'import
        Glide.with(context)
            .load(imageACharger)
            .circleCrop() // format circulaire
            .placeholder(R.drawable.pfp) // image affichée pendant le chargement
            .error(R.drawable.pfp)       // l'image affiché si erreur
            .into(imageView)
    }

    /**
     * Sauvegarde la photo sélectionnée en base de donnée
     *
     * @param uriString l'uri de la photo sélectionnée
     * @param profConnecte le prof connecté
     */
    private fun sauvegarderPhotoEnBase(uriString: String, profConnecte: Prof) {


        // log de verification
        Log.d("DEBUG_PROF", "Tentative de sauvegarde. ID=${profConnecte.id_prof} - URI=$uriString")
        val updated = profConnecte.copy(photoUri = uriString)

        // maj de la photo dans la coroutine
        val db = DatabaseProvider.db
        lifecycleScope.launch {
            db.profDAO().update(updated)
            // user feedback
            Toast.makeText(context, "Photo de profil mise à jour !", Toast.LENGTH_SHORT).show()
        }
    }
}
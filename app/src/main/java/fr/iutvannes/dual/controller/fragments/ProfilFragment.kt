// Assurez-vous que le package est correct
package fr.iutvannes.dual.controller.fragments

// Imports nécessaires
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import androidx.fragment.app.Fragment
import fr.iutvannes.dual.controller.MainActivity
import fr.iutvannes.dual.R

/**
 * Fragment pour afficher l'écran de profil de l'utilisateur.
 * Le layout associé est R.layout.fragment_profile.
 */
class ProfilFragment : Fragment(R.layout.fragment_profil) {

    /**
     * Cette fonction est appelée lorsque la vue du fragment est créée.
     * Elle initialise les interactions avec les vues.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        /// Listener pour le bouton profil dans la top bar
        val backButton = view.findViewById<ImageButton>(R.id.arrow_back_button)
        backButton.setOnClickListener {
            // On demande à l'activité parente (MainActivity) d'afficher le fragment du tableau de bord
            // Le 'as?' est une sécurité pour éviter un crash si l'activité n'est pas MainActivity
            (activity as? MainActivity)?.showFragment(TableauDeBordFragment(), true, true)
        }
    }
}
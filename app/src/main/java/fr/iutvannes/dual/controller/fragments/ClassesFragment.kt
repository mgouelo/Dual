package fr.iutvannes.dual.controller.fragments

// Imports nécessaires
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import fr.iutvannes.dual.R
import fr.iutvannes.dual.model.database.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.widget.Button
import fr.iutvannes.dual.controller.MainActivity

/**
 * Fragment pour afficher l'écran de profil de l'utilisateur.
 * Le layout associé est R.layout.fragment_profile.
 */
class ClassesFragment : Fragment(R.layout.fragment_classes) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialisation des vues et des listeners
        val container = view.findViewById<LinearLayout>(R.id.container_classes)

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {

//            //Connexion BDD
//            val db = Room.databaseBuilder(
//                requireContext(),
//                AppDatabase::class.java, "dual.db"
//            ).build()
//
//            val classes = db.EleveDao().getClasses()

            val classes = listOf("6A", "5C", "3F")

            //retour sur le thread principale pour modification de l'interface graphique(UI)
            withContext(Dispatchers.Main) {

                container.removeAllViews()

                if (classes.isEmpty()) {
                    val emptyText = TextView(requireContext()).apply {
                        text = "Aucune classe enregistrée"
                        textSize = 18f
                        setPadding(8, 8, 8, 8)
                    }
                    container.addView(emptyText)
                } else {
                    for (classe in classes) {
                        val button = Button(requireContext()).apply {
                            text = classe
                            textSize = 18f
                            setPadding(8, 8, 8, 8)
                        }

                        button.setOnClickListener {
                            val fragment = ElevesFragment.newInstance(classe)
                            (activity as MainActivity).showFragment(fragment, true, true)

                        }

                        container.addView(button)
                    }
                }
            }
        }
    }
}
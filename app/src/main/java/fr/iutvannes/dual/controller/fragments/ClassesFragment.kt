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
 * Fragment pour afficher la liste des classes.
 * Ainsi que le bouton pour ajouter une nouvelle classe.
 * Le layout utilisé est fragment_classes.xml.
 *
 * @see AppDatabase
 * @see R.layout.fragment_classes
 */
class ClassesFragment : Fragment(R.layout.fragment_classes) {

    /**
     * Méthode appelée lorsque le fragment est créé.
     * Ce fragment est utilisé pour afficher la liste des classes.
     * Ainsi que le bouton pour ajouter une nouvelle classe.
     *
     * @param view La vue du fragment.
     * @param savedInstanceState Les données sauvegardées du fragment.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialisation des vues et des listeners
        val container = view.findViewById<LinearLayout>(R.id.container_classes)

        // Ouvertures d'une coroutine dans le thread IO pour effectuer des tâches en arrière-plan
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {

            //Connexion BDD
            val db = Room.databaseBuilder(
                requireContext(),
                AppDatabase::class.java, "dual.db"
            )
                .fallbackToDestructiveMigration()
                .build()

            val classes = db.classeDao().getClasses()

            val btn_ajoutClasse = view.findViewById<Button>(R.id.ajout_classe)

            // Gestion du clic sur le bouton pour ajouter une nouvelle classe
            btn_ajoutClasse.setOnClickListener {
                val fragment = AjoutClasseFragment()
                (activity as MainActivity).showFragment(fragment, true, true)
            }

            // Retour sur le thread principale pour modification de l'interface graphique(UI)
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

                        // Gestion du clic sur la classe pour afficher la liste des élèves
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
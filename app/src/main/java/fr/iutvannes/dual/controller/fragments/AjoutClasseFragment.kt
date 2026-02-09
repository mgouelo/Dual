package fr.iutvannes.dual.controller.fragments

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import fr.iutvannes.dual.R
import fr.iutvannes.dual.controller.MainActivity
import fr.iutvannes.dual.model.database.AppDatabase
import fr.iutvannes.dual.model.persistence.Classe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Fragment pour ajouter une nouvelle classe.
 * Ce fragment est utilisé pour créer une nouvelle classe dans la base de données en sélectionnant
 * un niveau et une lettre. Lors de la validation, le fragment vérifie si une classe existe ou
 * doit être créé.
 * Des Toasts sont présents pour informer l'utilisateur des différents résultats.
 * Le layout utilisé est fragment_ajout_classe.xml.
 *
 * @see AppDatabase
 * @see MainActivity
 * @see Classe
 * @see R.layout.fragment_ajout_classe
 */
class AjoutClasseFragment: Fragment(R.layout.fragment_ajout_classe) {

    /**
     * Méthode appelée lorsque le fragment est créé.
     * Gère les clics sur les boutons de retour et de validation.
     * Créé une nouvelle classe en fonction du niveau et de la lettre sélectionnées.
     *
     * @param view La vue du fragment.
     * @param savedInstanceState Les données sauvegardées du fragment.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val buttonBack = view.findViewById<ImageButton>(R.id.arrow_back_button)
        val buttonValider = view.findViewById<Button>(R.id.btn_valider)

        val toogleNiveau = view.findViewById<com.google.android.material.button.MaterialButtonToggleGroup>(R.id.toggle_group_niveau)
        val toogleLettre = view.findViewById<com.google.android.material.button.MaterialButtonToggleGroup>(R.id.toggle_group_lettre)


        // Gestion du clic sur le bouton de retour
        buttonBack.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        // Gestion du clic sur le bouton de validation
        buttonValider.setOnClickListener {
            val idNiveau = toogleNiveau.checkedButtonId
            val idLettre = toogleLettre.checkedButtonId

            if (idNiveau == -1 || idLettre == -1) {
                Toast.makeText(
                    requireContext(),
                    "Veuillez sélectionner un niveau et une lettre",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            val niveau = view.findViewById<com.google.android.material.button.MaterialButton>(idNiveau).text.toString()
            val lettre = view.findViewById<com.google.android.material.button.MaterialButton>(idLettre).text.toString()

            val classeNom = "${niveau[0]}$lettre"

            // Ouverture d'une coroutine sur le thread IO pour ajouter la classe à la base de données
            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO){
                // Création ou ouverture de la base de données
                val db = Room.databaseBuilder(
                    requireContext(),
                    AppDatabase::class.java,
                    "dual.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()

                val classe = Classe(
                    nom = classeNom
                )

                val existeDeja = db.classeDao().getClasseByName(classeNom) != null

                // Affichage d'un message si la classe existe déjà par le Thread principal
                withContext(Dispatchers.Main){
                    if (existeDeja) {
                        Toast.makeText(requireContext(), "Cette classe existe déjà", Toast.LENGTH_SHORT).show()
                    }
                }

                if(!existeDeja) {
                    // Ajout de la classe à la base de données
                    db.classeDao().insert(classe)

                    // Affichage d'un message si la classe a été ajoutée par le Thread principal
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "Classe ajoutée", Toast.LENGTH_SHORT).show()

                        val fragment = ClassesFragment()
                        (activity as MainActivity).showFragment(fragment, true, true)
                    }
                }
            }
        }
    }
}
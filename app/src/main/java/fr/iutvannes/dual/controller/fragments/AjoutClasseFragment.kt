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
 * Fragment for adding a new class.
 * This fragment is used to create a new class in the database by selecting
 * a level and a letter. During validation, the fragment checks if a class already exists or
 * needs to be created.
 * Toasts are displayed to inform the user of the different results.
 * The layout used is fragment_ajout_classe.xml.
 *
 * @see AppDatabase
 * @see MainActivity
 * @see Classe
 * @see R.layout.fragment_ajout_classe
 */
class AjoutClasseFragment: Fragment(R.layout.fragment_ajout_classe) {

    /**
     * This method is called when the fragment is created.
     * Handles clicks on the back and submit buttons.
     * Creates a new class based on the selected level and letter.
     *
     * @param view The fragment view.
     * @param savedInstanceState The saved data of the fragment.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val buttonBack = view.findViewById<ImageButton>(R.id.arrow_back_button)
        val buttonValider = view.findViewById<Button>(R.id.btn_valider)

        val toogleNiveau = view.findViewById<com.google.android.material.button.MaterialButtonToggleGroup>(R.id.toggle_group_niveau)
        val toogleLettre = view.findViewById<com.google.android.material.button.MaterialButtonToggleGroup>(R.id.toggle_group_lettre)


        // Managing clicks on the back button
        buttonBack.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        // Managing the click on the validation button
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

            // Opening a coroutine on the I/O thread to add the class to the database
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

                // Displaying a message if the class already exists via the main thread
                withContext(Dispatchers.Main){
                    if (existeDeja) {
                        Toast.makeText(requireContext(), "Cette classe existe déjà", Toast.LENGTH_SHORT).show()
                    }
                }

                if(!existeDeja) {
                    // Adding the class to the database
                    db.classeDao().insert(classe)

                    // A message is displayed if the class was added by the main thread.
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
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

class AjoutClasseFragment: Fragment(R.layout.fragment_ajout_classe) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val buttonBack = view.findViewById<ImageButton>(R.id.arrow_back_button)
        val buttonValider = view.findViewById<Button>(R.id.btn_valider)

        val toogleNiveau = view.findViewById<com.google.android.material.button.MaterialButtonToggleGroup>(R.id.toggle_group_niveau)
        val toogleLettre = view.findViewById<com.google.android.material.button.MaterialButtonToggleGroup>(R.id.toggle_group_lettre)



        buttonBack.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

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

            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO){
                val db = Room.databaseBuilder(
                    requireContext(),
                    AppDatabase::class.java,
                    "dual.db"
                ).build()

                val classe = Classe(
                    nom = classeNom
                )

                val existeDeja = db.classeDao().getClasseByName(classeNom) != null


                withContext(Dispatchers.Main){
                    if (existeDeja) {
                        Toast.makeText(requireContext(), "Cette classe existe déjà", Toast.LENGTH_SHORT).show()
                    }
                }

                if(!existeDeja) {
                    db.classeDao().insert(classe)

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
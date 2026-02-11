package fr.iutvannes.dual.controller.fragments

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import fr.iutvannes.dual.R
import fr.iutvannes.dual.model.persistence.Eleve
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AjoutFragment : Fragment(R.layout.fragment_ajout_eleve) {

    private var classeNom: String? = null
    private var eleveID: Int = -1

    // init db via provider
    val db = DatabaseProvider.db

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        classeNom = arguments?.getString("classeNom")
        eleveID = arguments?.getInt("eleveID", -1) ?: -1
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val inputPrenom = view.findViewById<EditText>(R.id.input_prenom)
        val inputNom = view.findViewById<EditText>(R.id.input_nom)
        val groupSexe = view.findViewById<RadioGroup>(R.id.group_sexe)
        val buttonBack = view.findViewById<ImageButton>(R.id.arrow_back_button)
        val buttonValider = view.findViewById<Button>(R.id.btn_valider)

        if (eleveID != -1) { // id différent de celui par défaut --> élève déjà existant --> mode édition
            buttonValider.text = "Enregistrer les modifications"

            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                val eleveExist = db.EleveDao().getEleveById(eleveID) // sécruité : on vérifie quand même existence de l'élève
                if (eleveExist != null) {
                    withContext(Dispatchers.Main) {
                        inputPrenom.setText(eleveExist.prenom)
                        inputNom.setText(eleveExist.nom)

                        if (eleveExist.genre == "F") {
                            groupSexe.check(R.id.radio_femme)
                        } else if (eleveExist.genre == "M") {
                            groupSexe.check(R.id.radio_homme)
                        }
                    }
                }
            }
        }

        buttonBack.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        buttonValider.setOnClickListener {

            val prenom = inputPrenom.text.toString().trim()
            val nom = inputNom.text.toString().trim()
            val selectedId = groupSexe.checkedRadioButtonId

            // Déterminer le genre en fonction de l'ID
            val genre = when (selectedId) {
                R.id.radio_homme -> "M"
                R.id.radio_femme -> "F"
                else -> "" // Rien n'est coché
            }

            if (prenom.isEmpty() || nom.isEmpty() || genre.isEmpty()) {
                Toast.makeText(requireContext(), "Veuillez remplir tous les champs", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {

                val eleve = Eleve(
                    prenom = prenom,
                    nom = nom,
                    genre = genre,
                    classe = classeNom!!
                )

                if (eleveID != -1) {
                    eleve.id_eleve = eleveID
                    db.EleveDao().update(eleve)

                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "Elève modifié !", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    db.EleveDao().insert(eleve)
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "Elève ajouté !", Toast.LENGTH_SHORT).show()
                    }
                }

                withContext(Dispatchers.Main) {
                    //Retour à ElevesFragment
                    requireActivity().supportFragmentManager.popBackStack()
                }
            }
        }
    }

    companion object {
        fun newInstance(classeNom: String): AjoutFragment {
            val fragment = AjoutFragment()
            val args = Bundle()
            args.putString("classeNom", classeNom)
            fragment.arguments = args
            return fragment
        }

        fun newInstanceForEdit(classeNom: String, eleveID: Int): AjoutFragment {
            val fragment = AjoutFragment()
            val args = Bundle()
            args.putString("classeNom", classeNom)
            args.putInt("eleveID", eleveID)
            fragment.arguments = args
            return fragment
        }
    }
}

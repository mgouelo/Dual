package fr.iutvannes.dual.controller.fragments

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import fr.iutvannes.dual.R
import fr.iutvannes.dual.controller.MainActivity
import fr.iutvannes.dual.model.database.AppDatabase
import fr.iutvannes.dual.model.persistence.Eleve
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AjoutFragment : Fragment(R.layout.fragment_ajout_eleve) {

    private var classeNom: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        classeNom = arguments?.getString("classeNom")
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val inputPrenom = view.findViewById<EditText>(R.id.input_prenom)
        val inputNom = view.findViewById<EditText>(R.id.input_nom)
        val buttonBack = view.findViewById<ImageButton>(R.id.arrow_back_button)
        val buttonValider = view.findViewById<Button>(R.id.btn_valider)

        buttonBack.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        buttonValider.setOnClickListener {

            val prenom = inputPrenom.text.toString().trim()
            val nom = inputNom.text.toString().trim()

            if (prenom.isEmpty() || nom.isEmpty()) {
                Toast.makeText(requireContext(), "Veuillez remplir tous les champs", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {

                val db = Room.databaseBuilder(
                    requireContext(),
                    AppDatabase::class.java,
                    "dual.db"
                ).build()

                val eleve = Eleve(
                    prenom = prenom,
                    nom = nom,
                    classe = classeNom!!
                )

                db.EleveDao().insert(eleve)

                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Élève ajouté", Toast.LENGTH_SHORT).show()

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
    }
}

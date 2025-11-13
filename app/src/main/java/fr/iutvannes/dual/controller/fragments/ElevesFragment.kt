package fr.iutvannes.dual.controller.fragments

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
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
import java.nio.file.Files.size

class ElevesFragment : Fragment(R.layout.fragment_eleves){


    //Variable qui contiendra le nom de la classe (peut accepter un nom null pour éviter les null pointer exception)
    private var classeNom: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //Récupération du nom de la classe appelée depuis ClasseFragment
        classeNom = arguments?.getString("classeNom")
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        val titre = view.findViewById<TextView>(R.id.classe_titre)
        val container = view.findViewById<LinearLayout>(R.id.container_eleves)
        val btnAdd = view.findViewById<Button>(R.id.btn_add_eleve)
        val backButton = view.findViewById<ImageButton>(R.id.arrow_back_button)

        backButton.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        titre.text = "Élèves de $classeNom"

        //Action du bouton
        btnAdd.setOnClickListener {
            // On ouvre le fragment d'ajout
            val fragment = AjoutFragment.newInstance(classeNom!!)
            (activity as MainActivity).showFragment(fragment, true, true)
        }

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {

            val db = Room.databaseBuilder(
                requireContext(),
                AppDatabase::class.java, "dual.db"
            )
                .fallbackToDestructiveMigration()
                .build()


            val eleveDao = db.EleveDao()
            val eleves = db.EleveDao().getElevesByClasse(classeNom!!)

            withContext(Dispatchers.Main) {

                container.removeAllViews()

                if(eleves.isEmpty()){
                    val emptyText = TextView(requireContext()).apply {
                        text = "Aucun élève"
                        textSize = 18f
                    }
                    container.addView(emptyText)
                } else {

                    //Pour chaque élève trouvé
                    for(Eleve in eleves) {
                        val textView = TextView(requireContext()).apply {
                            text = "${Eleve.prenom} ${Eleve.nom}"
                            textSize = 18f
                            setPadding(8, 8, 8, 8)
                        }
                        container.addView(textView)
                    }
                }
            }


        }



    }

    companion object {
        /**
         * Méthode utilitaire pour créer un fragment ElevesFragment
         * en lui passant le nom de la classe à afficher.
         */
        fun newInstance(classeNom: String): ElevesFragment {
            val fragment = ElevesFragment()
            val args = Bundle()
            args.putString("classeNom", classeNom)
            fragment.arguments = args
            return fragment
        }
    }
}
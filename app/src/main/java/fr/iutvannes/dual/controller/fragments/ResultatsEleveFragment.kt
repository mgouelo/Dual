package fr.iutvannes.dual.controller.fragments

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import fr.iutvannes.dual.R
import fr.iutvannes.dual.model.components.GraphView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Affichage des résultats de l'élève
 * Récupération de l'identifiant de l'élève depuis ElevesFragment
 *
 * @see GraphView
 */
class ResultatsEleveFragment : Fragment(R.layout.fragment_resultats_eleve){


    /* Variable qui contiendra l'identifiant de l'élève */
    private var eleveId: Int = -1

    val db = DatabaseProvider.db

    /**
     * Méthode appelée lors de la création du fragment
     * Récupération de l'identifiant de l'élève depuis ElevesFragment
     *
     * @param savedInstanceState Bundle contenant l'état de l'interface
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //Récupération de l'identifiant de l'élève appelée depuis ElevesFragment
        eleveId = arguments?.getInt("eleveId", -1) ?: -1
    }

    /**
     * Méthode appelée lors de la création de la vue du fragment
     * Affichage des résultats de l'élève
     *
     * @param view Vue du fragment
     * @param savedInstanceState Bundle contenant l'état de l'interface
     */
    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val titre = view.findViewById<TextView>(R.id.result_eleve)
        val resultGraph = view.findViewById<GraphView>(R.id.resultGraph)
        val resultExamen = view.findViewById<TextView>(R.id.examenResult)
        val resultTitre = view.findViewById<TextView>(R.id.result_titre)
        val btnTirs = view.findViewById<Button>(R.id.btnTirs)
        val btnCourse = view.findViewById<Button>(R.id.btnCourse)
        val btnExamen = view.findViewById<Button>(R.id.btnExamen)
        val btnBack = view.findViewById<ImageButton>(R.id.arrow_back_button)

        if (eleveId != -1) {

            // Ouverture d'une coroutine dans le thread IO pour effectuer les tâches de base de données
            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                val eleveExist = db.EleveDao().getEleveById(eleveId)
                val resultatExist = db.resultatDao().getResultatsByEleve(eleveId)

                if (eleveExist != null) {
                    val dataTirs = resultatExist.map { resultat ->
                        val temps = resultat.cibles_touchees
                        val date = db.seanceDao()
                            .getSeanceById(resultat.id_seance)
                            ?.date ?: "Inconnu"
                        Pair("Séance du $date", temps.toFloat())
                    }

                    val dataCourse = resultatExist.map { resultat ->
                        val temps = resultat.temp_course
                        val date = db.seanceDao()
                            .getSeanceById(resultat.id_seance)
                            ?.date ?: "Inconnu"
                        Pair("Séance du $date", temps)
                    }

                    withContext(Dispatchers.Main) {
                        titre.text =
                            "Résultats de ${eleveExist.nom.uppercase()} ${eleveExist.prenom}"
                        resultExamen.text = "Sélectionnez une catégorie"

                        // Affichage des résultats de l'élève dans la catégorie "Tirs"
                        btnTirs.setOnClickListener {
                            resultTitre.text = "Progression de tirs"
                            if (dataTirs.isEmpty()) {
                                resultExamen.visibility = View.VISIBLE
                                resultGraph.visibility = View.GONE
                                resultExamen.text = "Pas de données à afficher"
                                return@setOnClickListener
                                // Pas de données à afficher
                            } else {
                                resultExamen.visibility = View.GONE
                                resultGraph.visibility = View.VISIBLE
                                resultGraph.yMax = 30
                                resultGraph.yMin = 0
                                resultGraph.lineColor = ContextCompat.getColor(requireContext(), R.color.vert)
                                resultGraph.data = dataTirs
                                resultGraph.invalidate()
                            }
                        }

                        // Affichage des résultats de l'élève dans la catégorie "Course"
                        btnCourse.setOnClickListener {
                            resultTitre.text = "Progression de course"
                            if (dataCourse.isEmpty()) {
                                resultExamen.visibility = View.VISIBLE
                                resultGraph.visibility = View.GONE
                                resultExamen.text = "Pas de données à afficher"
                                return@setOnClickListener
                                // Pas de données à afficher
                            } else {
                                resultExamen.visibility = View.GONE
                                resultGraph.visibility = View.VISIBLE
                                resultGraph.yMax = 30
                                resultGraph.yMin = 0
                                resultGraph.lineColor = ContextCompat.getColor(requireContext(), R.color.rouge)
                                resultGraph.data = dataCourse
                                resultGraph.invalidate()
                            }
                        }

                        // Affichage des résultats de l'élève à l'examen
                        btnExamen.setOnClickListener {
                            resultTitre.text = "Résultat de l'exament"
                            if (resultatExist.isEmpty() || resultatExist[0].note_finale == 0F) {
                                resultExamen.visibility = View.VISIBLE
                                resultGraph.visibility = View.GONE
                                resultExamen.text = "Pas de données à afficher"
                                return@setOnClickListener
                            } else {
                                resultExamen.visibility = View.VISIBLE
                                resultGraph.visibility = View.GONE
                                resultExamen.text = "Note finale : ${resultatExist[0].note_finale}"
                            }
                        }
                    }
                }
            }

            // Retour à la page précédente
            btnBack.setOnClickListener {
                requireActivity().onBackPressedDispatcher.onBackPressed()
            }
        }
    }

    /**
     * Méthode statique pour créer un fragment ResultatsEleveFragment
     *
     * @return Fragment ResultatsEleveFragment
     */
    companion object {
        /**
         * Méthode utilitaire pour créer un fragment ResultatsEleveFragment
         * en lui passant le nom de la classe à afficher.
         */
        fun newInstance(eleveId: Int): ResultatsEleveFragment {
            val fragment = ResultatsEleveFragment()
            val args = Bundle()
            args.putInt("eleveId", eleveId)
            fragment.arguments = args
            return fragment
        }
    }
}
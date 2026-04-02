package fr.iutvannes.dual.controller.fragments

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import kotlin.math.min
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import fr.iutvannes.dual.R
import fr.iutvannes.dual.model.components.GraphView
import fr.iutvannes.dual.model.utils.DatabaseProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.xmlbeans.impl.xb.xsdschema.TopLevelAttribute
import kotlin.collections.map

/**
 * Displaying student results
 * Retrieving the student ID from ElevesFragment
 *
 * @see GraphView
 */
class ResultatsEleveFragment : Fragment(R.layout.fragment_resultats_eleve){


    /* Variable that stores the student ID */
    private var eleveId: Int = -1

    val db = DatabaseProvider.db

    /**
     * Method called during fragment creation
     * Retrieving the student ID from ElevesFragment
     *
     * @param savedInstanceState Bundle containing the fragment's state
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //Récupération de l'identifiant de l'élève appelée depuis ElevesFragment
        eleveId = arguments?.getInt("eleveId", -1) ?: -1
    }

    /**
     * Method called when the fragment view is created
     *
     * @param view The fragment view
     * @param savedInstanceState The data saved during the activity's state
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
                val tirsExist = db.tirDao().getTousLesTirs(eleveId)
                val coursesExist = db.courseDao().getAllTour(eleveId)

                if (eleveExist != null) {
                    val dataTirs = tirsExist.mapNotNull { tirAvecPassages ->
                        val date = db.seanceDao()
                            .getSeanceById(tirAvecPassages.tir.id_seance)
                            ?.date ?: "Inconnu"

                        val passages = tirAvecPassages.liste_passages
                        if (passages.isEmpty()) return@mapNotNull null

                        val parts = date.split("-")
                        val dateFormatee = if (parts.size == 3) "${parts[2]}/${parts[1]}/${parts[0]}" else date

                        val moyenne = passages.map { it.nb_tir_reussi }.average().toFloat()
                        Pair(dateFormatee, moyenne)
                    }

                    val vmaEleve = eleveExist.vma

                    val dataCourse = coursesExist.mapNotNull { courseAvecTours ->
                        val distance = courseAvecTours.course.distance_tour

                        val date = db.seanceDao()
                            .getSeanceById(courseAvecTours.course.id_seance)
                            ?.date ?: "Inconnu"

                        val toursValides = courseAvecTours.liste_tours
                            .map { it.temps_ms }
                            .filter { it > 0 }

                        if (toursValides.isEmpty()) return@mapNotNull null

                        val moyenneTempsMs = toursValides.average()

                        val calculatedValue = if (moyenneTempsMs > 0 && vmaEleve > 0) {
                            val tempsSecondes = moyenneTempsMs / 1000
                            val vitesse = (distance / tempsSecondes) * 3.6
                            (vitesse / vmaEleve * 100).toFloat()
                        } else return@mapNotNull null

                        val pourcentageVMA = min(130f, calculatedValue)

                        val parts = date.split("-")
                        val dateFormatee = if (parts.size == 3) "${parts[2]}/${parts[1]}/${parts[0]}" else date

                        Pair(dateFormatee, pourcentageVMA)
                    }

                    withContext(Dispatchers.Main) {
                        titre.text =
                            "Résultats de ${eleveExist.nom.uppercase()} ${eleveExist.prenom}"
                        resultExamen.text = "Sélectionnez une catégorie"

                        // Affichage des résultats de l'élève dans la catégorie "Tirs"
                        btnTirs.setOnClickListener {
                            resultTitre.text = "Progression de tirs (moyenne sur 5 tirs)"
                            if (dataTirs.isEmpty()) {
                                resultExamen.visibility = View.VISIBLE
                                resultGraph.visibility = View.GONE
                                resultExamen.text = "Pas de données à afficher"
                                return@setOnClickListener
                                // Pas de données à afficher
                            } else {
                                resultExamen.visibility = View.GONE
                                resultGraph.visibility = View.VISIBLE
                                resultGraph.yLabels = null
                                resultGraph.yMin = 0
                                resultGraph.yMax = 5
                                resultGraph.lineColor = ContextCompat.getColor(requireContext(), R.color.vert)
                                resultGraph.data = dataTirs
                                resultGraph.invalidate()
                            }
                        }

                        // Affichage des résultats de l'élève dans la catégorie "Course"
                        btnCourse.setOnClickListener {
                            resultTitre.text = "Progression de course (% de VMA)"
                            if (dataCourse.isEmpty()) {
                                resultExamen.visibility = View.VISIBLE
                                resultGraph.visibility = View.GONE
                                resultExamen.text = "Pas de données à afficher"
                                return@setOnClickListener
                                // Pas de données à afficher
                            } else {
                                resultExamen.visibility = View.GONE
                                resultGraph.visibility = View.VISIBLE
                                resultGraph.yLabels = listOf(50f, 60f, 70f, 80f, 90f, 100f, 110f, 120f, 130f)
                                resultGraph.lineColor = ContextCompat.getColor(requireContext(), R.color.rouge)
                                resultGraph.data = dataCourse
                                resultGraph.invalidate()
                            }
                        }

                        // Affichage des résultats de l'élève à l'examen
                        btnExamen.setOnClickListener {
                            resultTitre.text = "Résultat de l'examen"
                            if (resultatExist.isEmpty() || resultatExist[0].note_finale == 0F) {
                                resultExamen.visibility = View.VISIBLE
                                resultGraph.visibility = View.GONE
                                resultExamen.text = "Pas de données à afficher"
                                return@setOnClickListener
                            } else {
                                resultExamen.visibility = View.VISIBLE
                                resultGraph.visibility = View.GONE
                                resultExamen.text = "Note finale : ${resultatExist[0].note_finale} / 20"
                                resultExamen.textSize = 48f
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
     * Static method to create a fragment: ResultatsEleveFragment
     *
     * @return Fragment ResultatsEleveFragment
     */
    companion object {
        /**
         * Static method to create a fragment: ResultatsEleveFragment
         *
         * @param eleveId ID of the student
         * @return Fragment ResultatsEleveFragment
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
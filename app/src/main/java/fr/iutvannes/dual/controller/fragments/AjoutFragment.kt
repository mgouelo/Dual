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

/**
 * Fragment for adding or modifying a student.
 * This fragment is used to create a new student or to modify the information of an existing student.
 * By specifying the necessary information.
 * The layout used is that of fragment_ajout_eleve.xml.
 *
 * @see Eleve
 * @see AppDatabase
 * @see R.layout.fragment_ajout_eleve
 */
class AjoutFragment : Fragment(R.layout.fragment_ajout_eleve) {

    /* Variable for storing the class name. */
    private var classeNom: String? = null

    /* Variable used to store the student ID. */
    private var eleveID: Int = -1

    // init db via provider
    val db = DatabaseProvider.db

    /**
     * Method called when the fragment is created.
     * Retrieves the data provided during fragment creation.
     *
     * @param savedInstanceState The saved data of the fragment..
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        classeNom = arguments?.getString("classeNom")
        eleveID = arguments?.getInt("eleveID", -1) ?: -1
    }

    /**
     * Method called when the fragment is created.
     * Handles adding or modifying a student based on the provided data.
     * As well as clicks on the submit and back buttons.
     *
     * @param view The fragment view
     * @param savedInstanceState The saved fragment data.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val inputPrenom = view.findViewById<EditText>(R.id.input_prenom)
        val inputNom = view.findViewById<EditText>(R.id.input_nom)
        val groupSexe = view.findViewById<RadioGroup>(R.id.group_sexe)
        val buttonBack = view.findViewById<ImageButton>(R.id.arrow_back_button)
        val buttonValider = view.findViewById<Button>(R.id.btn_valider)

        if (eleveID != -1) { // ID different from the default one --> student already exists edit mode
            buttonValider.text = "Enregistrer les modifications"

            // Opening a coroutine on the IO thread to perform the student read operation
            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                val eleveExist = db.EleveDao().getEleveById(eleveID) // Security: we still verify the student's existence.
                if (eleveExist != null) {
                    // Return to the main thread to update the user interface.
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

        // Managing clicks on the back button
        buttonBack.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        // Managing the click on the validation button
        buttonValider.setOnClickListener {

            val prenom = inputPrenom.text.toString().trim()
            val nom = inputNom.text.toString().trim()
            val selectedId = groupSexe.checkedRadioButtonId

            // Determine the gender based on the ID
            val genre = when (selectedId) {
                R.id.radio_homme -> "M"
                R.id.radio_femme -> "F"
                else -> "" // Nothing is checked.
            }

            if (prenom.isEmpty() || nom.isEmpty() || genre.isEmpty()) {
                Toast.makeText(requireContext(), "Veuillez remplir tous les champs", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Opening a coroutine on the I/O thread to perform the student insertion or modification operation
            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {

                val eleve = Eleve(
                    prenom = prenom,
                    nom = nom,
                    genre = genre,
                    classe = classeNom!!
                )

                if (eleveID != -1) {
                    eleve.id_eleve = eleveID
                    // Student modification
                    db.EleveDao().update(eleve)

                    // Return to the main thread to display a successful edit message
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "Elève modifié !", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    // Adding a new student
                    db.EleveDao().insert(eleve)
                    // Return to the main thread to display a successful addition message
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "Elève ajouté !", Toast.LENGTH_SHORT).show()
                    }
                }

                withContext(Dispatchers.Main) {
                    // Return to ElevesFragment (the list of students)
                    requireActivity().supportFragmentManager.popBackStack()
                }
            }
        }
    }

    /**
     * A static method to create a new instance of this fragment.
     *
     * @return A new AddFragment object @return A new AddFragment object
     */
    companion object {
        /**
         * Static method to create a new instance of this fragment.
         *
         * @param classeNom The name of the class
         * @return A new AddFragment object
         */
        fun newInstance(classeNom: String): AjoutFragment {
            val fragment = AjoutFragment()
            val args = Bundle()
            args.putString("classeNom", classeNom)
            fragment.arguments = args
            return fragment
        }

        /**
         * A static method to create a new instance of this fragment in edit mode.
         *
         * @param classeNom The name of the class
         * @param eleveID The student ID to modify
         * @return A new AddFragment object in edit mode
         */
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

package fr.iutvannes.dual.controller.fragments

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
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

    /* Variable for the old class name */
    private var oldClasseNom: String? = null // The class's former name if in edit mode; otherwise, in design mode, it's null.

    /**
     * This method is called when the fragment is created.
     *
     * @param savedInstanceState The saved data of the fragment.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // We retrieve the arguments passed via newInstanceForEdit
        oldClasseNom = arguments?.getString("oldName")
    }

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

        // Title, back button, submit button
        val title = view.findViewById<TextView>(R.id.page_title)
        val buttonBack = view.findViewById<ImageButton>(R.id.arrow_back_button)
        val buttonValider = view.findViewById<Button>(R.id.btn_valider)

        // Radio button group for selecting the naming type --> Either the name is a letter (6th grade A) or a word (6th grade Ouessant)
        val radioGroup = view.findViewById<RadioGroup>(R.id.radio_mode_nommage)

        // Toggles are selectors with predefined choices, such as level or letter.
        val toggleNiveau = view.findViewById<com.google.android.material.button.MaterialButtonToggleGroup>(R.id.toggle_group_niveau)
        val toggleLettre = view.findViewById<com.google.android.material.button.MaterialButtonToggleGroup>(R.id.toggle_group_lettre)

        // Input in the case where the class name is a word.
        val inputLibre = view.findViewById<EditText>(R.id.input_nom_libre)

        // Contains the letter-based naming logic
        val containerNommageLettre = view.findViewById<View>(R.id.container_nom_lettre)

        // Contains the naming logic by word
        val containerNommageLibre = view.findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.container_nom_libre)

        // Special case: modifying an existing class
        if (oldClasseNom != null) { // If the old class name is not null, then we modify an existing class.

            // We're changing the title and the button from 'validate' to 'edit' for less ambiguity.
            title.setText("Modifier la classe " + oldClasseNom)
            buttonValider.text = "Modifier"

            // The current class name is divided into two parts separated by a space:
            // Part 1: Level
            // Part 2: Name
            val parts = oldClasseNom!!.split(" ", limit = 2)

            if (parts.size >= 2) {
                val niveauStr = parts[0] // e.g., "6th"
                val suiteStr = parts[1]  // e.g., "A" or "Ouessant"

                // We pre-select the level for greater consistency and better UX
                selectionnerBoutonParTexte(toggleNiveau, niveauStr)

                // We choose the naming method used for the old name.
                // If suiteStr has a size exactly of 1, then it is a simple letter.
                if (suiteStr.length == 1 && suiteStr[0].isLetter()) {
                    containerNommageLettre.visibility = View.VISIBLE
                    containerNommageLibre.visibility = View.GONE
                    radioGroup.check(R.id.radio_nommage_lettre)
                    selectionnerBoutonParTexte(toggleLettre, suiteStr)

                } else { // Otherwise it's a word
                    containerNommageLettre.visibility = View.GONE
                    containerNommageLibre.visibility = View.VISIBLE
                    radioGroup.check(R.id.radio_nommage_libre)
                    inputLibre.setText(suiteStr)

                }
            }
        }

        // Standard case: we create a new class
        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            if (checkedId == R.id.radio_nommage_lettre) {
                // Standard Mode: Buttons are displayed, text is hidden
                containerNommageLettre.visibility = View.VISIBLE
                containerNommageLibre.visibility = View.GONE
            } else {
                // Free Mode: The buttons are hidden, the text is displayed
                containerNommageLettre.visibility = View.GONE
                containerNommageLibre.visibility = View.VISIBLE
            }
        }

        // Managing clicks on the back button
        buttonBack.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        // Managing the click on the validation button
        buttonValider.setOnClickListener {
            var nomFinal = ""
            val idNiveau = toggleNiveau.checkedButtonId

            // No level was selected
            if (idNiveau == -1) {
                Toast.makeText(requireContext(), "Veuillez sélectionner un niveau", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val niveau = view.findViewById<Button>(idNiveau).text.toString()
            // We check which mode is chosen
            if (radioGroup.checkedRadioButtonId == R.id.radio_nommage_lettre) {

                val idLettre = toggleLettre.checkedButtonId
                if (idLettre == -1) { // No letter is selected
                    Toast.makeText(requireContext(), "Veuillez sélectionner une lettre", Toast.LENGTH_LONG).show()
                    return@setOnClickListener
                }

                val lettre = view.findViewById<Button>(idLettre).text.toString()
                nomFinal = "${niveau[0]}e $lettre" // Example: "6A"

            } else { // Otherwise it's in free naming
                nomFinal = inputLibre.text.toString().trim()
                nomFinal = "${niveau[0]}e " + inputLibre.text.toString().trim()

                if (nomFinal.isEmpty()) {
                    Toast.makeText(requireContext(), "Veuillez entrer un nom de classe", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
            }

            // Backup to database
            sauvegarderClasse(nomFinal)
        }
    }

    /**
     * Creates a new instance of the fragment.
     *
     * @return A new instance of the fragment.
     */
    companion object {
        // For a simple creation (no arguments)
        /**
         * Creates a new instance of the fragment.
         *
         * @return A new instance of the fragment.
         */
        fun newInstance(): AjoutClasseFragment {
            return AjoutClasseFragment()
        }

        // For an edit (pass the current name of the class you want to modify)
        /**
         * Creates a new instance of the fragment.
         *
         * @param nomActuel The name of the class you want to modify.
         * @return A new instance of the fragment.
         */
        fun newInstanceForEdit(nomActuel: String): AjoutClasseFragment {
            val fragment = AjoutClasseFragment()
            val args = Bundle()
            args.putString("oldName", nomActuel)
            fragment.arguments = args
            return fragment
        }
    }

    /**
     * Inserts a new class into the database.
     *
     * @param db The database.
     * @param nom The name of the class.
     */
    private fun insererEtQuitter(db: AppDatabase, nom: String) {
        // Opening a coroutine on the I/O thread to add the class to the database
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            db.classeDao().insert(Classe(nom = nom))
            // Displaying a message if the class already exists via the main thread
            withContext(Dispatchers.Main) {
                Toast.makeText(requireContext(), "Classe $nom ajoutée", Toast.LENGTH_SHORT).show() // User feedback toast
                (activity as MainActivity).onBackPressed() // Return to previous screen
            }
        }
    }

    /**
     * Saves the class to the database.
     *
     * @param nouveauNom The name of the class.
     */
    private fun sauvegarderClasse(nouveauNom: String) {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val db = DatabaseProvider.db

            // Case 1: Modification
            if (oldClasseNom != null) {
                if (oldClasseNom == nouveauNom) {
                    // The user hasn't changed anything.
                    withContext(Dispatchers.Main) {
                        requireActivity().onBackPressed()
                    }
                    return@launch
                }

                // We check if the new name is already taken by another class to avoid duplicates.
                val existeDeja = db.classeDao().getClasseByName(nouveauNom) != null
                if (existeDeja) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "Ce nom de classe existe déjà", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                // Everything is OK, we're doing the database update
                db.classeDao().updateNomClasse(oldClasseNom!!, nouveauNom)

                // We also update each student's class in the database with the new name.
                db.EleveDao().updateClasseEleves(oldClasseNom!!, nouveauNom)

                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Classe modifiée", Toast.LENGTH_SHORT).show() // User feedback
                    requireActivity().onBackPressed()
                }

            } else {
                // Case 2: Insertion
                val existe = db.classeDao().getClasseByName(nouveauNom) != null // Returns a boolean to check for existence
                if (existe) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "La classe $nouveauNom existe déjà", Toast.LENGTH_SHORT).show() // User feedback
                    }
                } else { // Does not exist --> no duplicate found
                    insererEtQuitter(db, nouveauNom)
                }
            }
        }
    }

    /**
     * Selects a button in a group based on its text.
     *
     * @param group The group of buttons.
     * @param texte The text of the button to select.
     */
    private fun selectionnerBoutonParTexte(group: com.google.android.material.button.MaterialButtonToggleGroup, texte: String) {
        for (i in 0 until group.childCount) {
            val view = group.getChildAt(i)
            if (view is Button && view.text.toString() == texte) {
                group.check(view.id)
                return
            }
        }
    }
}
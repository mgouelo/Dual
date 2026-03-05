package fr.iutvannes.dual.controller.fragments

// Necessary imports
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import fr.iutvannes.dual.R
import fr.iutvannes.dual.controller.MainActivity
import fr.iutvannes.dual.model.persistence.Classe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Fragment to display the list of classes.
 * As well as the button to add a new class.
 * The layout used is fragment_classes.xml.
 *
 * @see MainActivity
 * @see Classe
 * @see R.layout.fragment_classes
 */
class ClassesFragment : Fragment(R.layout.fragment_classes) {

    /* Variable for the adapter to use all the classes */
    private lateinit var adapter: ClasseAdapter

    /* Variable for the database */
    private val db = DatabaseProvider.db

    /**
     * This method is called when the fragment is created.
     * This fragment is used to display the list of classes.
     * As well as the button to add a new class.
     *
     * @param view The fragment view.
     * @param savedInstanceState The fragment's saved data.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // View retrieval
        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerViewClasses)
        val btnAjout = view.findViewById<Button>(R.id.ajout_classe)
        val tvEmpty = view.findViewById<TextView>(R.id.tvEmpty)

        // Adapter configuration
        adapter = ClasseAdapter(
            items = emptyList(), // Empty list at startup
            onClick = { classe ->
                // Opening view with the list of students
                val fragment = ElevesFragment.newInstance(classe.nom)
                (activity as MainActivity).showFragment(fragment, true, true)
            },
            onEdit = { classe ->
                // Open the added fragment in edit mode (blue button)
                val fragment = AjoutClasseFragment.newInstanceForEdit(classe.nom)
                (activity as MainActivity).showFragment(fragment, true, true)
            },
            onDelete = { classe ->
                // Confirmation before deletion
                afficherConfirmationSuppression(classe)
            }
        )

        // Branch it adapts to the recyclerview which will allow an optimized display of the classes
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(context)

        // Handling the click on the button to add a new class
        btnAjout.setOnClickListener {
            val fragment = AjoutClasseFragment.newInstance()
            (activity as MainActivity).showFragment(fragment, true, true)
        }

        // Loading classes
        chargerClasses()
    }

    /**
     * Loads the classes from the database and displays them in the adapter.
     */
    private fun chargerClasses() {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {

            // We retrieve the list of classes
            val rawClasses = db.classeDao().getClasses()

            // Transforms each Class into a ClassUI + student counter
            val uiList = rawClasses.map { classe ->
                val count = db.EleveDao().countElevesByClasse(classe.nom)
                ClasseUI(classe, count)
            }

            // Update the interface on the main thread
            withContext(Dispatchers.Main) {
                adapter.updateList(uiList)

                // Display in case no class
                val tvEmpty = view?.findViewById<TextView>(R.id.tvEmpty)
                if (uiList.isEmpty()) {
                    tvEmpty?.visibility = View.VISIBLE
                } else {
                    tvEmpty?.visibility = View.GONE
                }
            }
        }
    }

    /**
     * Deletes a class from the database.
     *
     * @param classe The class to delete
     */
    private fun supprimerClasse(classe: Classe) {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {

            // Prior removal of students
            db.EleveDao().deleteElevesByClasse(classe.nom)

            // Deletion of class
            db.classeDao().delete(classe)

            // Reloading the list
            chargerClasses()

            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Classe ${classe.nom} supprimée", Toast.LENGTH_SHORT).show() // feedback utilisateur
            }
        }
    }

    /**
     * Displays a dialog box asking the user to confirm the deletion
     *
     * @param classe The class to delete
     */
    private fun afficherConfirmationSuppression(classe: Classe) {
        com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
            .setTitle("Supprimer la classe ?")
            .setMessage("Attention, vous êtes sur le point de supprimer la classe \"${classe.nom}\" ainsi que tous les élèves qui y sont associés.\n\nCette action est irréversible.")
            .setNegativeButton("Annuler") { dialog, _ ->
                dialog.dismiss()
            }
            .setPositiveButton("Supprimer") { _, _ ->
                supprimerClasse(classe)
            }
            .show()
    }
}
package fr.iutvannes.dual.controller.fragments

// Necessary imports
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import fr.iutvannes.dual.R
import fr.iutvannes.dual.controller.MainActivity
import fr.iutvannes.dual.model.persistence.Classe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import fr.iutvannes.dual.controller.viewmodel.ImportViewModel
import fr.iutvannes.dual.model.utils.DatabaseProvider

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
    private val db get() = DatabaseProvider.db

    private val openDocumentGlobal = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
    ) { uri -> if (uri != null) importerDepuisUriGlobal(uri) }

    private val importViewModel: ImportViewModel by viewModels()

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

        val btnImportGlobal = view.findViewById<Button>(R.id.btn_import_csv_global)
        btnImportGlobal.setOnClickListener {
            openDocumentGlobal.launch(
                arrayOf("text/csv", "text/comma-separated-values", "text/plain", "application/csv")
            )
        }

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
                if (isAdded) Toast.makeText(requireContext(), "Classe ${classe.nom} supprimée", Toast.LENGTH_SHORT).show()
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

    /**
     * Imports a CSV file from a URI
     *
     * @param uri The URI of the file
     */
    private fun importerDepuisUriGlobal(uri: Uri) {
        val context = requireContext()
        val resolver = context.contentResolver
        val mime = resolver.getType(uri)
        val fileName = getFileName(uri) ?: "import"

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val report = withContext(Dispatchers.IO) {
                    resolver.openInputStream(uri)?.use { input ->
                        importViewModel.importer(input, fileName, mime, classeNom = null)
                    }
                }

                if (report != null) {
                    Toast.makeText(
                        context,
                        "${report.created} élève(s) importé(s), ${report.classesCreated} classe(s) créée(s)",
                        Toast.LENGTH_LONG
                    ).show()
                    chargerClasses() // rafraîchit la liste des classes
                }

            } catch (e: IllegalArgumentException) {
                // Intercepte les erreurs de colonnes manquantes (ex: "Colonne 'Prénom' absente")
                Toast.makeText(
                    context,
                    "Format de fichier invalide : ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
                Log.e("ImportCSVGlobal", "Erreur de format CSV", e)

            } catch (e: Exception) {
                // Intercepte toute autre erreur pour éviter le crash
                Toast.makeText(
                    context,
                    "Impossible de lire le fichier.",
                    Toast.LENGTH_SHORT
                ).show()
                Log.e("ImportCSVGlobal", "Erreur inattendue", e)
            }
        }
    }

    /**
     * Gets the file name from the URI
     *
     * @param uri The URI of the file
     */
    private fun getFileName(uri: Uri): String? {
        val cursor = requireContext().contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (it.moveToFirst() && index != -1) return it.getString(index)
        }
        return null
    }
}
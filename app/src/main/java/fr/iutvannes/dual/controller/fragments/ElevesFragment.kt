package fr.iutvannes.dual.controller.fragments

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import fr.iutvannes.dual.R
import fr.iutvannes.dual.controller.MainActivity
import fr.iutvannes.dual.controller.viewmodel.ImportViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.fragment.app.viewModels
import android.net.Uri
import android.provider.OpenableColumns
import androidx.recyclerview.widget.RecyclerView

/**
 * Fragment to display the list of students in a class.
 * As well as the ability to add, edit, and delete students.
 * And the ability to import data from a spreadsheet file.
 *
 * @see ImportViewModel
 * @see MainActivity
 * @see R.layout.fragment_eleves
 */
class ElevesFragment : Fragment(R.layout.fragment_eleves){


    /* Variable used to retrieve the class name */
    private var classeNom: String? = null

    /* Variable allowing the import of data from a spreadsheet file */
    private val importViewModel: ImportViewModel by viewModels()

    /* Variable allowing retrieval of the list of students */
    private lateinit var adapter: ElevesAdapter

    /* Variable enabling the use of the recyclerView */
    private lateinit var recyclerViewEleves: RecyclerView

    /* Variable to display a message if the list is empty */
    private lateinit var tvEmpty: TextView

    /* Variable used to open the file selector */
    private val openDocument = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            importerDepuisUri(uri)
        }
    }

    /**
     * Method called when the fragment is created.
     * Retrieves the name of the class to display.
     *
     * @param savedInstanceState Data retained during a state change
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Retrieving the name of the class called from ClassFragment
        classeNom = arguments?.getString("classeNom")
    }

    /**
     * Method called when the fragment is created.
     * Handles user interactions.
     * Initializes the user interface and listeners.
     *
     * @param view The fragment view
     * @param savedInstanceState The data saved during a state change
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        val titre = view.findViewById<TextView>(R.id.classe_titre)
        val btnAdd = view.findViewById<Button>(R.id.btn_add_eleve)
        val backButton = view.findViewById<ImageButton>(R.id.arrow_back_button)
        val btnImport = view.findViewById<Button>(R.id.btn_import_tableur)
        recyclerViewEleves = view.findViewById(R.id.recyclerViewEleves)
        tvEmpty = view.findViewById(R.id.tvEmpty)
        adapter = ElevesAdapter(
            onEdit = { eleve ->
                val fragment = AjoutFragment.newInstanceForEdit(classeNom!!, eleve.id_eleve)
                (activity as MainActivity).showFragment(fragment, true, true)
            },
            onDelete = { eleve ->
                viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                    val db = DatabaseProvider.db
                    db.EleveDao().delete(eleve.id_eleve)

                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "Élève supprimé", Toast.LENGTH_SHORT).show()
                        chargerEleves()
                    }
                }
            }
        )
        recyclerViewEleves.adapter = adapter
        recyclerViewEleves.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(requireContext())

        // Handling clicks on the back button
        backButton.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        titre.text = "Élèves de $classeNom"

        // Managing clicks on the add button
        btnAdd.setOnClickListener {
            // On ouvre le fragment d'ajout
            val fragment = AjoutFragment.newInstance(classeNom!!)
            (activity as MainActivity).showFragment(fragment, true, true)
        }

        // Managing clicks on the import button
        btnImport.setOnClickListener {
            ouvrirSelectionFichier()
        }

        chargerEleves()
    }

    /**
     * Static method to create a new instance of the fragment.
     *
     * @return A new ElevesFragment object
     */
    companion object {
        /**
         * Utility method to create a StudentFragment
         * by passing it the name of the class to display.
         *
         * @param classeNom The name of the class to display
         *
         * @return A new ElevesFragment object
         */
        fun newInstance(classeNom: String): ElevesFragment {
            val fragment = ElevesFragment()
            val args = Bundle()
            args.putString("classeNom", classeNom)
            fragment.arguments = args
            return fragment
        }
    }

    /**
     * Open the Android file selector
     * + applies a filter to show only .xls / .xlsx / .ods / .csv files
     */
    private fun ouvrirSelectionFichier() {
        openDocument.launch(
            arrayOf(
                "text/csv",
                "text/comma-separated-values",
                "text/plain", // Crucial addition: some CSV files are seen as plain text
                "application/csv",
                "application/vnd.ms-excel",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            )
        )
    }

    /**
     * Analyzes the spreadsheet file's metadata (name + MIME type)
     * Opens an InputStream object + calls the viewModel
     * Dynamically reloads the student list
     *
     * @param uri The file's URI
     */
    private fun importerDepuisUri(uri: Uri) {
        val context = requireContext()
        val resolver = context.contentResolver
        val mime = resolver.getType(uri)
        val fileName = getFileName(uri) ?: "import"

        // Opening a coroutine on the I/O thread to import the data
        viewLifecycleOwner.lifecycleScope.launch {
            val report = withContext(Dispatchers.IO) {
                resolver.openInputStream(uri)?.use { input ->
                    importViewModel.importer(input, fileName, mime, classeNom)
                }
            }

            // Displaying a message based on the import result
            if (report != null) {
                Toast.makeText(
                    context,
                    "Import terminé (${report.created} créés, ${report.errorCount} erreurs)",
                    Toast.LENGTH_LONG
                ).show()

                // student list update
                chargerEleves()
            }
        }
    }

    /**
     * Retrieving the actual file
     *
     * @param uri The file URI
     * @return The file name
     */
    private fun getFileName(uri: Uri): String? {
        val cursor = requireContext().contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (it.moveToFirst() && index != -1) {
                return it.getString(index)
            }
        }
        return null
    }

    /**
     * Executes a room query to retrieve the students from the database.
     */
    private fun chargerEleves() {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {

            val db = DatabaseProvider.db
            val eleves = db.EleveDao().getElevesByClasse(classeNom!!)

            withContext(Dispatchers.Main) {
                adapter.submitList(eleves)

                tvEmpty.visibility = if (eleves.isEmpty()) View.VISIBLE else View.GONE
                recyclerViewEleves.visibility = if (eleves.isEmpty()) View.GONE else View.VISIBLE
            }
        }
    }
}
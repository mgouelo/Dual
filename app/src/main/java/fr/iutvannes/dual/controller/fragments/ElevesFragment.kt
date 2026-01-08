package fr.iutvannes.dual.controller.fragments

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import fr.iutvannes.dual.R
import fr.iutvannes.dual.controller.MainActivity
import fr.iutvannes.dual.model.database.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import fr.iutvannes.dual.controller.viewmodel.ImportViewModel
import androidx.fragment.app.viewModels
import android.net.Uri
import android.provider.OpenableColumns

class ElevesFragment : Fragment(R.layout.fragment_eleves){


    //ariable qui contiendra le nom de la classe
    private var classeNom: String? = null

    // viewModel
    private val importViewModel: ImportViewModel by viewModels()

    // Sélecteur de fichier pour tableur
    private val openDocument = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            importerDepuisUri(uri)
        }
    }

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
        val btnImport = view.findViewById<Button>(R.id.btn_import_tableur)

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

        // bouton import
        btnImport.setOnClickListener {
            ouvrirSelectionFichier()
        }

        chargerEleves(container)
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

    /**
     * Ouvre le sélecteur de fichier android
     * + applique un filtre pour afficher que les fichiers .xls / .xlsx / .ods / .csv
     */
    private fun ouvrirSelectionFichier() {
        openDocument.launch(
            arrayOf(
                "text/csv",
                "application/vnd.ms-excel",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "application/vnd.oasis.opendocument.spreadsheet"
            )
        )
    }

    /**
     * analyse les métadonnées du fichier tableur (nom + type MIME)
     * ouvre un objet InputStream + appel la viewModel
     * recharge dynamiquement la liste d'éleve
     */
    private fun importerDepuisUri(uri: Uri) {
        val context = requireContext()
        val resolver = context.contentResolver
        val mime = resolver.getType(uri)
        val fileName = getFileName(uri) ?: "import"

        viewLifecycleOwner.lifecycleScope.launch {
            val report = withContext(Dispatchers.IO) {
                resolver.openInputStream(uri)?.use { input ->
                    importViewModel.importer(input, fileName, mime, classeNom)
                }
            }

            if (report != null) {
                Toast.makeText(
                    context,
                    "Import terminé (${report.created} créés, ${report.errorCount} erreurs)",
                    Toast.LENGTH_LONG
                ).show()

                // maj de la liste des eleves
                val container = view?.findViewById<LinearLayout>(R.id.container_eleves)
                if (container != null) {
                    chargerEleves(container)
                }
            }
        }
    }

    /**
     * Récupération du réel du fichier
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
     * Exécute une requête room pour récupréer les élèves en DB
     */
    private fun chargerEleves(container: LinearLayout) {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {

            val db = Room.databaseBuilder(
                requireContext(),
                AppDatabase::class.java, "dual.db"
            )
                .fallbackToDestructiveMigration()
                .build()

            val eleves = db.EleveDao().getElevesByClasse(classeNom!!)

            withContext(Dispatchers.Main) {
                container.removeAllViews()

                if (eleves.isEmpty()) {
                    val emptyText = TextView(requireContext()).apply {
                        text = "Aucun élève"
                        textSize = 18f
                    }
                    container.addView(emptyText)
                } else {
                    for (eleve in eleves) {
                        val tv = TextView(requireContext()).apply {
                            text = "${eleve.prenom} ${eleve.nom}"
                            textSize = 18f
                            setPadding(8, 8, 8, 8)
                        }
                        container.addView(tv)
                    }
                }
            }
        }
    }


}
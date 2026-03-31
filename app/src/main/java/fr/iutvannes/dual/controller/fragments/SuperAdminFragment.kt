package fr.iutvannes.dual.controller.fragments

import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.view.View
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import fr.iutvannes.dual.R
import fr.iutvannes.dual.controller.MainActivity
import fr.iutvannes.dual.controller.fragments.ConnexionFragment
import fr.iutvannes.dual.controller.viewmodel.ImportViewModel
import fr.iutvannes.dual.model.persistence.Classe
import fr.iutvannes.dual.model.persistence.Eleve
import fr.iutvannes.dual.model.persistence.Prof
import fr.iutvannes.dual.model.utils.PasswordUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SuperAdminFragment : Fragment(R.layout.fragment_super_admin) {

    private val importViewModel: ImportViewModel by viewModels()
    private var listeClasses: List<String> = emptyList()

    // État de visibilité des mots de passe
    private var passwordVisible = false
    private var passwordConfirmVisible = false

    private val openDocumentEleves = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
    ) { uri -> if (uri != null) importerElevesDepuisUri(uri) }

    private val openDocumentProfs = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
    ) { uri -> if (uri != null) importerProfsDepuisUri(uri) }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val db = DatabaseProvider.db

        Toast.makeText(requireContext(), "Connecté en tant que Superadmin", Toast.LENGTH_LONG).show()

        // ── Bouton retour ─────────────────────────────────────────────────
        view.findViewById<ImageButton>(R.id.btn_back_superadmin).setOnClickListener {
            (activity as? MainActivity)?.showFragment(
                ConnexionFragment(), withNavigation = false, withTopBar = false
            )
        }

        // ── Références vues — section Classe ──────────────────────────────
        val radioModeNommage  = view.findViewById<RadioGroup>(R.id.radio_mode_nommage_admin)
        val toggleNiveau      = view.findViewById<MaterialButtonToggleGroup>(R.id.toggle_groupe_niveau_admin)
        val toggleLettre      = view.findViewById<MaterialButtonToggleGroup>(R.id.toggle_groupe_lettre_admin)
        val containerLettre   = view.findViewById<View>(R.id.container_lettre_admin)
        val containerNomLibre = view.findViewById<TextInputLayout>(R.id.container_nom_libre_admin)
        val inputNomLibre     = view.findViewById<TextInputEditText>(R.id.input_nom_libre_admin)
        val btnAjoutClasse    = view.findViewById<Button>(R.id.btn_ajout_classe)
        val tvMsgClasse       = view.findViewById<TextView>(R.id.tv_msg_classe)

        // ── Références vues — ajout prof manuel ───────────────────────────
        val etProfPrenom    = view.findViewById<EditText>(R.id.et_prof_prenom)
        val etProfNom       = view.findViewById<EditText>(R.id.et_prof_nom)
        val etProfEmail     = view.findViewById<EditText>(R.id.et_prof_email)
        val etProfPassword  = view.findViewById<EditText>(R.id.et_prof_password)
        val etProfConfirm   = view.findViewById<EditText>(R.id.et_prof_password_confirm)
        val btnTogglePass   = view.findViewById<ImageButton>(R.id.btn_toggle_password)
        val btnToggleConfirm = view.findViewById<ImageButton>(R.id.btn_toggle_password_confirm)
        val btnAjoutProf    = view.findViewById<Button>(R.id.btn_ajout_prof)
        val tvMsgProf       = view.findViewById<TextView>(R.id.tv_msg_prof)

        // ── Références vues — import CSV profs ────────────────────────────
        val btnImportProfs = view.findViewById<Button>(R.id.btn_import_profs)
        val tvMsgCsvProfs  = view.findViewById<TextView>(R.id.tv_msg_csv_profs)

        // ── Références vues — ajout élève manuel ──────────────────────────
        val etElevePrenom = view.findViewById<EditText>(R.id.et_eleve_prenom)
        val etEleveNom    = view.findViewById<EditText>(R.id.et_eleve_nom)
        val spinnerGenre  = view.findViewById<Spinner>(R.id.spinner_genre)
        val spinnerClasse = view.findViewById<Spinner>(R.id.spinner_classe_eleve)
        val etEleveVma    = view.findViewById<EditText>(R.id.et_eleve_vma)
        val btnAjoutEleve = view.findViewById<Button>(R.id.btn_ajout_eleve)
        val tvMsgEleve    = view.findViewById<TextView>(R.id.tv_msg_eleve)

        // ── Références vues — import CSV élèves ───────────────────────────
        val etEleveClasseImport = view.findViewById<EditText>(R.id.et_eleve_classe_import)
        val btnImportEleves     = view.findViewById<Button>(R.id.btn_import_eleves)
        val tvMsgCsvEleves      = view.findViewById<TextView>(R.id.tv_msg_csv_eleves)

        view.setTag(R.id.tv_msg_csv_eleves, tvMsgCsvEleves)
        view.setTag(R.id.tv_msg_csv_profs, tvMsgCsvProfs)
        view.setTag(R.id.et_eleve_classe_import, etEleveClasseImport)

        // ── Spinner genre ─────────────────────────────────────────────────
        spinnerGenre.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            listOf("M", "F")
        )

        // ── Helpers ───────────────────────────────────────────────────────
        fun showMsg(tv: TextView, ok: Boolean, text: String) {
            tv.visibility = View.VISIBLE
            tv.setTextColor(if (ok) Color.parseColor("#18A900") else Color.parseColor("#C8232C"))
            tv.text = text
        }

        fun clearFields(vararg fields: EditText) = fields.forEach { it.setText("") }

        // ── Toggle visibilité mot de passe ────────────────────────────────
        btnTogglePass.setOnClickListener {
            passwordVisible = !passwordVisible
            etProfPassword.transformationMethod = if (passwordVisible)
                HideReturnsTransformationMethod.getInstance()
            else
                PasswordTransformationMethod.getInstance()
            btnTogglePass.setImageResource(
                if (passwordVisible) android.R.drawable.ic_menu_view
                else android.R.drawable.ic_secure
            )
            etProfPassword.setSelection(etProfPassword.text?.length ?: 0)
        }

        btnToggleConfirm.setOnClickListener {
            passwordConfirmVisible = !passwordConfirmVisible
            etProfConfirm.transformationMethod = if (passwordConfirmVisible)
                HideReturnsTransformationMethod.getInstance()
            else
                PasswordTransformationMethod.getInstance()
            btnToggleConfirm.setImageResource(
                if (passwordConfirmVisible) android.R.drawable.ic_menu_view
                else android.R.drawable.ic_secure
            )
            etProfConfirm.setSelection(etProfConfirm.text?.length ?: 0)
        }

        // ── Chargement + refresh du spinner classes ───────────────────────
        fun chargerClasses() {
            viewLifecycleOwner.lifecycleScope.launch {
                val classes = withContext(Dispatchers.IO) {
                    db.classeDao().getClasses().map { it.nom }.sorted()
                }
                listeClasses = classes
                spinnerClasse.adapter = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_spinner_dropdown_item,
                    if (classes.isEmpty()) listOf("Aucune classe disponible") else classes
                )
            }
        }

        chargerClasses()

        // ── Basculer entre mode lettre et mode nom libre ──────────────────
        radioModeNommage.setOnCheckedChangeListener { _, checkedId ->
            if (checkedId == R.id.radio_nommage_lettre_admin) {
                containerLettre.visibility   = View.VISIBLE
                containerNomLibre.visibility = View.GONE
            } else {
                containerLettre.visibility   = View.GONE
                containerNomLibre.visibility = View.VISIBLE
            }
        }

        // ── Ajouter une classe ────────────────────────────────────────────
        btnAjoutClasse.setOnClickListener {
            val idNiveau = toggleNiveau.checkedButtonId
            if (idNiveau == View.NO_ID) {
                showMsg(tvMsgClasse, false, "Veuillez sélectionner un niveau")
                return@setOnClickListener
            }
            val niveau = view.findViewById<Button>(idNiveau).text.toString()

            val nomFinal: String
            if (radioModeNommage.checkedRadioButtonId == R.id.radio_nommage_lettre_admin) {
                val idLettre = toggleLettre.checkedButtonId
                if (idLettre == View.NO_ID) {
                    showMsg(tvMsgClasse, false, "Veuillez sélectionner une lettre")
                    return@setOnClickListener
                }
                val lettre = view.findViewById<Button>(idLettre).text.toString()
                nomFinal = "$niveau $lettre"
            } else {
                val libre = inputNomLibre.text?.toString()?.trim().orEmpty()
                if (libre.isBlank()) {
                    showMsg(tvMsgClasse, false, "Veuillez entrer un nom de classe")
                    return@setOnClickListener
                }
                nomFinal = "$niveau $libre"
            }

            viewLifecycleOwner.lifecycleScope.launch {
                val existe = withContext(Dispatchers.IO) {
                    db.classeDao().getClasseByName(nomFinal) != null
                }
                if (existe) {
                    showMsg(tvMsgClasse, false, "La classe \"$nomFinal\" existe déjà")
                } else {
                    withContext(Dispatchers.IO) { db.classeDao().insert(Classe(nom = nomFinal)) }
                    showMsg(tvMsgClasse, true, "Classe \"$nomFinal\" créée")
                    inputNomLibre.setText("")
                    chargerClasses()
                }
            }
        }

        // ── Ajouter un prof ───────────────────────────────────────────────
        btnAjoutProf.setOnClickListener {
            val prenom   = etProfPrenom.text.toString().trim()
            val nom      = etProfNom.text.toString().trim()
            val email    = etProfEmail.text.toString().trim()
            val password = etProfPassword.text.toString().trim()
            val confirm  = etProfConfirm.text.toString().trim()

            if (prenom.isBlank() || nom.isBlank() || email.isBlank() || password.isBlank() || confirm.isBlank()) {
                showMsg(tvMsgProf, false, "Tous les champs sont requis")
                return@setOnClickListener
            }
            if (password != confirm) {
                showMsg(tvMsgProf, false, "Les mots de passe ne correspondent pas")
                return@setOnClickListener
            }

            viewLifecycleOwner.lifecycleScope.launch {
                val hashed = PasswordUtils.hashPassword(password)
                val prof = Prof(nom = nom.uppercase(), prenom = prenom, email = email, password = hashed)
                val id = withContext(Dispatchers.IO) { db.profDAO().insert(prof) }
                if (id > 0) {
                    showMsg(tvMsgProf, true, "Professeur ajouté avec succès")
                    clearFields(etProfPrenom, etProfNom, etProfEmail, etProfPassword, etProfConfirm)
                    // Réinitialiser la visibilité des mots de passe
                    passwordVisible = false
                    passwordConfirmVisible = false
                    etProfPassword.transformationMethod = PasswordTransformationMethod.getInstance()
                    etProfConfirm.transformationMethod = PasswordTransformationMethod.getInstance()
                    btnTogglePass.setImageResource(android.R.drawable.ic_secure)
                    btnToggleConfirm.setImageResource(android.R.drawable.ic_secure)
                } else {
                    showMsg(tvMsgProf, false, "Email déjà utilisé")
                }
            }
        }

        // ── Importer profs CSV ────────────────────────────────────────────
        btnImportProfs.setOnClickListener {
            tvMsgCsvProfs.visibility = View.GONE
            ouvrirSelectionFichier(openDocumentProfs)
        }

        // ── Ajouter un élève ──────────────────────────────────────────────
        btnAjoutEleve.setOnClickListener {
            val prenom = etElevePrenom.text.toString().trim()
            val nom    = etEleveNom.text.toString().trim()
            val genre  = spinnerGenre.selectedItem.toString()
            val vma    = etEleveVma.text.toString().toFloatOrNull() ?: 10f

            if (listeClasses.isEmpty()) {
                showMsg(tvMsgEleve, false, "Créez d'abord une classe")
                return@setOnClickListener
            }
            val classe = spinnerClasse.selectedItem?.toString().orEmpty()

            if (prenom.isBlank() || nom.isBlank()) {
                showMsg(tvMsgEleve, false, "Prénom et nom sont requis")
                return@setOnClickListener
            }
            viewLifecycleOwner.lifecycleScope.launch {
                val eleve = Eleve(nom = nom.uppercase(), prenom = prenom, genre = genre, classe = classe, vma = vma)
                withContext(Dispatchers.IO) { db.EleveDao().insert(eleve) }
                showMsg(tvMsgEleve, true, "$prenom ${nom.uppercase()} ajouté en $classe")
                clearFields(etElevePrenom, etEleveNom, etEleveVma)
            }
        }

        // ── Importer élèves CSV ───────────────────────────────────────────
        btnImportEleves.setOnClickListener {
            tvMsgCsvEleves.visibility = View.GONE
            ouvrirSelectionFichier(openDocumentEleves)
        }
    }

    override fun onResume() {
        super.onResume()
        view?.let {
            val spinner = it.findViewById<Spinner>(R.id.spinner_classe_eleve) ?: return
            viewLifecycleOwner.lifecycleScope.launch {
                val classes = withContext(Dispatchers.IO) {
                    DatabaseProvider.db.classeDao().getClasses().map { c -> c.nom }.sorted()
                }
                listeClasses = classes
                spinner.adapter = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_spinner_dropdown_item,
                    if (classes.isEmpty()) listOf("Aucune classe disponible") else classes
                )
            }
        }
    }

    private fun ouvrirSelectionFichier(
        launcher: androidx.activity.result.ActivityResultLauncher<Array<String>>
    ) {
        launcher.launch(arrayOf("text/csv", "text/comma-separated-values", "text/plain", "application/csv"))
    }

    private fun importerElevesDepuisUri(uri: Uri) {
        val view      = view ?: return
        val tvMsg     = view.getTag(R.id.tv_msg_csv_eleves) as? TextView ?: return
        val etClasse  = view.getTag(R.id.et_eleve_classe_import) as? EditText
        val classeNom = etClasse?.text?.toString()?.trim()?.takeIf { it.isNotBlank() }

        val resolver = requireContext().contentResolver
        val mime     = resolver.getType(uri)
        val fileName = getFileName(uri) ?: "import_eleves.csv"

        viewLifecycleOwner.lifecycleScope.launch {
            val report = withContext(Dispatchers.IO) {
                resolver.openInputStream(uri)?.use { input ->
                    importViewModel.importer(input, fileName, mime, classeNom)
                }
            }
            if (report != null) {
                val msg = buildString {
                    append("${report.created} élève(s) importé(s)")
                    if (report.classesCreated > 0) append(", ${report.classesCreated} classe(s) créée(s)")
                    if (report.skipped > 0) append(", ${report.skipped} ignoré(s)")
                    if (report.errorCount > 0) append(", ${report.errorCount} erreur(s)")
                }
                showMsg(tvMsg, report.created > 0, msg)
                if (report.classesCreated > 0) {
                    val spinner = view.findViewById<Spinner>(R.id.spinner_classe_eleve)
                    val classes = withContext(Dispatchers.IO) {
                        DatabaseProvider.db.classeDao().getClasses().map { it.nom }.sorted()
                    }
                    listeClasses = classes
                    spinner?.adapter = ArrayAdapter(
                        requireContext(),
                        android.R.layout.simple_spinner_dropdown_item,
                        classes
                    )
                }
            } else {
                showMsg(tvMsg, false, "Impossible de lire le fichier")
            }
        }
    }

    private fun importerProfsDepuisUri(uri: Uri) {
        val view  = view ?: return
        val tvMsg = view.getTag(R.id.tv_msg_csv_profs) as? TextView ?: return
        val db    = DatabaseProvider.db

        val resolver = requireContext().contentResolver
        val fileName = getFileName(uri) ?: "import_profs.csv"

        viewLifecycleOwner.lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                try {
                    resolver.openInputStream(uri)?.use { input ->
                        CsvProfReader().read(input).let { drafts ->
                            var created = 0; var skipped = 0; var errors = 0
                            drafts.forEach { d ->
                                if (d.prenom.isBlank() || d.nom.isBlank() ||
                                    d.email.isBlank() || d.password.isBlank()
                                ) { errors++; return@forEach }
                                val hashed = PasswordUtils.hashPassword(d.password)
                                val prof = Prof(
                                    nom      = d.nom.uppercase(),
                                    prenom   = d.prenom,
                                    email    = d.email,
                                    password = hashed
                                )
                                val id = db.profDAO().insert(prof)
                                if (id > 0) created++ else skipped++
                            }
                            Triple(created, skipped, errors)
                        }
                    }
                } catch (e: Exception) { null }
            }
            if (result != null) {
                val (created, skipped, errors) = result
                val msg = buildString {
                    append("$created prof(s) importé(s)")
                    if (skipped > 0) append(", $skipped ignoré(s)")
                    if (errors > 0) append(", $errors erreur(s)")
                }
                showMsg(tvMsg, result.first > 0, msg)
            } else {
                showMsg(tvMsg, false, "Impossible de lire le fichier")
            }
        }
    }

    private fun getFileName(uri: Uri): String? {
        val cursor = requireContext().contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (it.moveToFirst() && index != -1) return it.getString(index)
        }
        return null
    }

    private fun showMsg(tv: TextView, ok: Boolean, text: String) {
        tv.visibility = View.VISIBLE
        tv.setTextColor(if (ok) Color.parseColor("#18A900") else Color.parseColor("#C8232C"))
        tv.text = text
    }
}
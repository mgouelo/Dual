package fr.iutvannes.dual.controller.fragments

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import fr.iutvannes.dual.R
import fr.iutvannes.dual.model.persistence.Eleve
import fr.iutvannes.dual.model.persistence.Resultat
import fr.iutvannes.dual.model.persistence.Seance
import fr.iutvannes.dual.model.utils.DatabaseProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Fragment that displays the results of past sessions.
 *
 * Operation in 3 steps:
 *
 * 1. The teacher chooses a class and a session type (VMA, Training, Final Event)
 * 2. The list of corresponding sessions is displayed
 * 3. Clicking on a session displays the results for each student
 *
 * For training, clicking on a student opens a dialog box with their shooting graph
 * and a table of their running times.
 */
class ResultatsFragment : Fragment(R.layout.fragment_resultats) {

    // ── Filtres sélectionnés par le professeur ────────────────────────────────
    private var classeSelectionnee: String? = null
    private var typeSelectionne: String? = null

    // Séance actuellement affichée (null = on est sur la liste des séances)
    private var seanceAffichee: Seance? = null

    // ── Références aux vues du layout ────────────────────────────────────────
    private lateinit var tvVide: TextView       // message "aucune donnée"
    private lateinit var recycler: RecyclerView // liste des séances ou des élèves
    private lateinit var btnBack: ImageButton   // flèche retour
    private lateinit var tvClasse: TextView     // label classe sélectionnée
    private lateinit var tvType: TextView       // label type sélectionné
    private lateinit var tvTitre: TextView      // titre en haut de la page

    // ── Gestion du bouton retour physique ────────────────────────────────────
    private lateinit var backPressedCallback: OnBackPressedCallback

    // ── Export CSV ───────────────────────────────────────────────────────────
    // Contenu CSV généré, en attente d'être sauvegardé dans un fichier
    private var contenuCsvEnAttente: String? = null

    // Lance le sélecteur de fichier Android pour sauvegarder le CS
    private val exportLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null && contenuCsvEnAttente != null) {
            try {
                requireContext().contentResolver.openOutputStream(uri)?.bufferedWriter()?.use {
                    it.write(contenuCsvEnAttente)
                }
                Toast.makeText(requireContext(), "Fichier sauvegardé !", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Erreur lors de la sauvegarde", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ── Initialisation du fragment ────────────────────────────────────────────

    /**
     * Called to have the fragment instantiate its user interface view.
     *
     * @param view The view returned by onCreateView(LayoutInflater, ViewGroup, Bundle)
     * @param savedInstanceState If non-null, this fragment is being re-constructed
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val cardClasse = view.findViewById<MaterialCardView>(R.id.card_filtre_classe)
        val cardType   = view.findViewById<MaterialCardView>(R.id.card_filtre_type)
        tvClasse = view.findViewById(R.id.tv_classe_selectionnee)
        tvType   = view.findViewById(R.id.tv_type_selectionne)
        tvVide   = view.findViewById(R.id.tv_resultats_vide)
        recycler = view.findViewById(R.id.recycler_resultats)
        btnBack  = view.findViewById(R.id.btn_back_resultats)
        tvTitre  = view.findViewById(R.id.tv_titre_resultats)

        backPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { gererActionRetour() }
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, backPressedCallback)
        btnBack.setOnClickListener { gererActionRetour() }

        cardClasse.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                val classes = withContext(Dispatchers.IO) {
                    DatabaseProvider.db.classeDao().getAllNames()
                }
                val options = listOf("Toutes") + classes
                android.app.AlertDialog.Builder(requireContext())
                    .setTitle("Filtrer par classe")
                    .setItems(options.toTypedArray()) { _, i ->
                        classeSelectionnee = if (i == 0) null else options[i]
                        tvClasse.text = options[i]
                        chargerSeances()
                    }
                    .show()
            }
        }

        cardType.setOnClickListener {
            val options = arrayOf("Tous", "Test VMA", "Entraînement", "Épreuve Finale")
            android.app.AlertDialog.Builder(requireContext())
                .setTitle("Filtrer par type")
                .setItems(options) { _, i ->
                    typeSelectionne = if (i == 0) null else options[i]
                    tvType.text = options[i]
                    chargerSeances()
                }
                .show()
        }

        chargerSeances()
    }

    // ── Étape 1 : Liste des séances ───────────────────────────────────────────

    /**
     * Load the list of sessions for the selected class and type.
     * If no filter is selected, load all sessions.
     */
    private fun chargerSeances() {
        tvTitre.text = "Résultats"
        btnBack.visibility = View.GONE

        viewLifecycleOwner.lifecycleScope.launch {
            val seances = withContext(Dispatchers.IO) {
                DatabaseProvider.db.seanceDao().getAllSeances()
                    .filter { classeSelectionnee == null || it.classe == classeSelectionnee }
                    .filter { typeSelectionne == null || it.type == typeSelectionne }
                    .sortedByDescending { it.id_seance }
            }

            if (seances.isEmpty()) {
                tvVide.text = "Aucune séance trouvée"
                tvVide.visibility   = View.VISIBLE
                recycler.visibility = View.GONE
                return@launch
            }

            tvVide.visibility   = View.GONE
            recycler.visibility = View.VISIBLE

            recycler.adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
                inner class VH(v: View) : RecyclerView.ViewHolder(v)
                override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int) =
                    VH(layoutInflater.inflate(R.layout.item_seance, parent, false))
                override fun getItemCount() = seances.size
                override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
                    val seance = seances[position]
                    val v = holder.itemView
                    v.findViewById<TextView>(R.id.tv_seance_titre).text  = "Séance du ${formaterDate(seance.date)}"
                    v.findViewById<TextView>(R.id.tv_seance_detail).text = "${seance.type} — ${seance.classe}"
                    v.setOnClickListener {
                        val typeSeance = seance.type  // type de la séance pour l'affichage
                        val filtreTypeSauvegarde = typeSelectionne
                        typeSelectionne = typeSeance
                        afficherResultatsSeance(seance)
                    }
                    v.findViewById<MaterialCardView>(R.id.btn_seance_excel).setOnClickListener {
                        genererEtExporterCSV(seance)
                    }
                    v.findViewById<MaterialCardView>(R.id.btn_seance_delete).setOnClickListener {
                        android.app.AlertDialog.Builder(requireContext())
                            .setTitle("Supprimer la séance ?")
                            .setMessage("Attention, vous êtes sur le point de supprimer cette séance ainsi que tous les résultats qui y sont associés.\n\nCette action est irréversible.")
                            .setNegativeButton("Annuler") { dialog, _ -> dialog.dismiss() }
                            .setPositiveButton("Supprimer") { _, _ ->
                                viewLifecycleOwner.lifecycleScope.launch {
                                    withContext(Dispatchers.IO) {
                                        DatabaseProvider.db.seanceDao().delete(seance.id_seance)
                                        DatabaseProvider.db.resultatDao().deleteBySeance(seance.id_seance)
                                    }
                                    chargerSeances()
                                }
                            }
                            .show()
                    }
                }
            }
        }
    }

    /**
     * Manage the back button press.
     */
    private fun gererActionRetour() {
        when {
            seanceAffichee != null -> {
                seanceAffichee = null
                typeSelectionne = null // reset pour ne pas garder le type de la séance
                chargerSeances()
                btnBack.visibility = View.GONE
            }
            else -> {
                backPressedCallback.isEnabled = false
                requireActivity().onBackPressedDispatcher.onBackPressed()
            }
        }
    }

    // ── Étape 2 : Résultats d'une séance ─────────────────────────────────────

    /**
     * Displays the list of students and their results for a given session.
     *
     * For training, shooting and running data are also loaded
     * from the Shooting/SalveShooting and Running/Running tables.
     *
     * @param seance  Session à afficher
     */
    private fun afficherResultatsSeance(seance: Seance) {
        val type = typeSelectionne ?: return
        seanceAffichee = seance
        tvTitre.text = "Séance du ${formaterDate(seance.date)} — ${seance.classe}"
        btnBack.visibility = View.VISIBLE

        viewLifecycleOwner.lifecycleScope.launch {

            /**
             * Internal template that groups all of a student's information for display.
             * The Shooting line and Running line are only filled in for practice.
             */
            data class LigneResultat(
                val eleve: Eleve,
                val resultat: Resultat,
                val ligneTir: String,    // ex: "🎯 Nb séries : 3  •  Moyenne : 4.0/5  •  Meilleur : 5/5"
                val ligneCourse: String  // ex: "🏃 Tours : 4  •  Meilleur : 1'42""
            )

            val lignes = withContext(Dispatchers.IO) {
                DatabaseProvider.db.resultatDao()
                    .getBySeance(seance.id_seance)
                    .mapNotNull { res ->

                        // Récupération de l'élève (null si supprimé entre-temps)
                        val eleve = DatabaseProvider.db.EleveDao()
                            .getEleveById(res.id_eleve) ?: return@mapNotNull null

                        val ligneTir: String
                        val ligneCourse: String

                        if (type == "Entraînement") {
                            // --- Calcul des stats de TIR ---
                            val tirs = DatabaseProvider.db.tirDao()
                                .getTirsBySeanceEtEleve(seance.id_seance, eleve.id_eleve)

                            // Aplatit toutes les salves en une seule liste de scores
                            val tousLesScores = tirs.flatMap { it.liste_passages }.map { it.nb_tir_reussi }
                            val nbSeries = tousLesScores.size
                            val moyenne  = if (nbSeries > 0) tousLesScores.average() else 0.0
                            val meilleur = tousLesScores.maxOrNull() ?: 0

                            // --- Calcul des stats de COURSE ---
                            val courses = DatabaseProvider.db.courseDao()
                                .getCoursesBySeanceEtEleve(seance.id_seance, eleve.id_eleve)

                            val nbTours        = courses.sumOf { it.liste_tours.size }
                            val meilleurTourMs = courses.flatMap { it.liste_tours }.minOfOrNull { it.temps_ms }

                            // Formatage du meilleur temps : 1'42" ou 42"
                            val meilleurTourStr = meilleurTourMs?.let {
                                val sec = (it / 1000).toInt()
                                "%d'%02d\"".format(sec / 60, sec % 60)
                            } ?: "-"

                            ligneTir = if (nbSeries > 0)
                                "🎯 Nb séries : $nbSeries  •  Moyenne : ${"%.1f".format(moyenne)}/5  •  Meilleur : $meilleur/5"
                            else "🎯 Tir : -"

                            ligneCourse = if (nbTours > 0)
                                "🏃 Tours : $nbTours  •  Meilleur : $meilleurTourStr"
                            else "🏃 Course : -"

                        } else {
                            // Pour VMA et Épreuve Finale, pas de stats tir/course détaillées
                            ligneTir    = ""
                            ligneCourse = ""
                        }

                        LigneResultat(eleve, res, ligneTir, ligneCourse)
                    }
                    .sortedWith(compareBy({ it.eleve.nom }, { it.eleve.prenom }))
            }

            // Cas : aucun résultat pour cette séance
            if (lignes.isEmpty()) {
                tvVide.text = "Aucun résultat pour cette séance"
                tvVide.visibility   = View.VISIBLE
                recycler.visibility = View.GONE
                com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Aucun résultat")
                    .setMessage("Aucun résultat pour cette séance. Voulez-vous la supprimer ?")
                    .setNegativeButton("Non") { dialog, _ -> dialog.dismiss() }
                    .setPositiveButton("Supprimer") { _, _ ->
                        viewLifecycleOwner.lifecycleScope.launch {
                            withContext(Dispatchers.IO) {
                                DatabaseProvider.db.seanceDao().delete(seance.id_seance)
                                DatabaseProvider.db.resultatDao().deleteBySeance(seance.id_seance)
                            }
                            chargerSeances()
                        }
                    }
                    .show()
                return@launch
            }

            tvVide.visibility   = View.GONE
            recycler.visibility = View.VISIBLE

            recycler.adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

                /**
                 * ViewHolder for the RecyclerView.
                 *
                 * @param v View of the item
                 * @return ViewHolder for the item
                 */
                inner class VH(v: View) : RecyclerView.ViewHolder(v)

                /**
                 * Called when RecyclerView needs a new ViewHolder of the given type to represent an item.
                 *
                 * @param parent   The ViewGroup into which the new View will be added after it is bound to an adapter position.
                 * @param viewType The view type of the new View.
                 * @return A new ViewHolder that holds a View of the given view type.
                 */
                override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int) =
                    VH(layoutInflater.inflate(R.layout.item_resultat, parent, false))

                /**
                 * Returns the total number of items in the data set held by the adapter.
                 */
                override fun getItemCount() = lignes.size

                /**
                 * Called by RecyclerView to display the data at the specified position.
                 *
                 * @param holder   The ViewHolder which should be updated to represent the contents of the item at the given position in the data set.
                 * @param position The position of the item within the adapter's data set.
                 */
                override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
                    val ligne = lignes[position]
                    val v = holder.itemView

                    // Numéro de classement + nom
                    v.findViewById<TextView>(R.id.tv_rang).text = "${position + 1}"
                    v.findViewById<TextView>(R.id.tv_nom_resultat).text =
                        "${ligne.eleve.prenom} ${ligne.eleve.nom.uppercase()}"

                    // Ligne de détail selon le type de séance
                    val detail = when (type) {
                        "Test VMA" -> {
                            val vma = ligne.eleve.vma?.let { "%.1f km/h".format(it) } ?: "-"
                            "VMA : $vma"
                        }
                        "Épreuve Finale" -> {
                            // Distinction 4ème/6ème par heuristique
                            val is4eme = ligne.resultat.ecart_max_course == 0 && ligne.resultat.nbTours == 6
                            if (is4eme) {
                                val pct = if ((ligne.eleve.vma ?: 0f) > 0f)
                                    "%.0f%%".format((ligne.resultat.temp_course / ligne.eleve.vma!!) * 100)
                                else "-"
                                "Cibles : ${ligne.resultat.cibles_touchees}/10  •  VMA : $pct"
                            } else {
                                "Tours : ${ligne.resultat.nbTours}  •  Cibles : ${ligne.resultat.cibles_touchees}"
                            }
                        }
                        else -> "${ligne.ligneTir}\n${ligne.ligneCourse}" // Entraînement
                    }
                    v.findViewById<TextView>(R.id.tv_detail_resultat).text = detail

                    // Note à droite (cachée pour l'entraînement)
                    val tvNote = v.findViewById<TextView>(R.id.tv_note)
                    when (type) {
                        "Entraînement" -> {
                            tvNote.visibility = View.GONE
                            v.setOnClickListener {
                                afficherDetailEntrainement(
                                    ligne.eleve.id_eleve,
                                    ligne.eleve.prenom,
                                    ligne.eleve.nom,
                                    seance.id_seance
                                )
                            }
                        }
                        "Épreuve Finale" -> {
                            tvNote.visibility = View.VISIBLE
                            tvNote.text = "%.2f".format(ligne.resultat.note_finale)
                            v.setOnClickListener {
                                afficherDetailEpreuveFinaleResultats(
                                    ligne.eleve,
                                    ligne.resultat
                                )
                            }
                        }
                        else -> {
                            tvNote.visibility = View.VISIBLE
                            tvNote.text = ligne.eleve.vma?.let { "%.1f".format(it) } ?: "-"
                            v.isClickable = false
                        }
                    }
                }
            }
        }
    }

    // ── Étape 3 : Détail entraînement (graphe tir + tableau course) ───────────

    /**
     * Ouvre un dialog avec :
     * - Un graphe de ligne montrant les réussites au tir par passage
     * - Un tableau listant les temps de course de chaque tour
     *
     * @param idEleve  ID de l'élève
     * @param prenom   Prénom (pour le titre du dialog)
     * @param nom      Nom (pour le titre du dialog)
     * @param idSeance ID de la séance concernée
     */
    private fun afficherDetailEntrainement(idEleve: Int, prenom: String, nom: String, idSeance: Int) {


        viewLifecycleOwner.lifecycleScope.launch {

            // Chargement des données en arrière-plan
            val (seriesTir, toursCourse) = withContext(Dispatchers.IO) {
                val tirs = DatabaseProvider.db.tirDao().getTirsBySeanceEtEleve(idSeance, idEleve)
                val seriesTir = tirs.flatMapIndexed { tirIdx, tirAvec ->
                    tirAvec.liste_passages.mapIndexed { passageIdx, passage ->
                        "S${tirIdx + 1}.${passageIdx + 1}" to passage.nb_tir_reussi.toFloat()
                    }
                }

                val courses = DatabaseProvider.db.courseDao().getCoursesBySeanceEtEleve(idSeance, idEleve)
                val toursCourse = courses.flatMapIndexed { courseIdx, courseAvec ->
                    courseAvec.liste_tours.mapIndexed { tourIdx, tour ->
                        "T${courseIdx + 1}.${tourIdx + 1}" to (tour.temps_ms / 1000f)
                    }
                }

                seriesTir to toursCourse
            }

            // Construction du dialog (même style que les autres dialogs de l'app)
            val dialog = android.app.Dialog(requireContext())
            dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
            val dialogView = layoutInflater.inflate(R.layout.dialog_lancer_seance, null)
            dialog.setContentView(dialogView)

            dialogView.findViewById<TextView>(R.id.dialog_header_title).visibility = View.GONE
            dialogView.findViewById<TextView>(R.id.dialog_header_title).visibility = View.GONE

            dialog.window?.setBackgroundDrawable(
                android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT)
            )
            dialog.window?.setLayout(
                (resources.displayMetrics.widthPixels * 0.95).toInt(),
                (resources.displayMetrics.heightPixels * 0.85).toInt()
            )

            dialogView.findViewById<TextView>(R.id.dialog_subtitle).text = "$prenom ${nom.uppercase()}"

            // Conteneur scrollable pour les deux sections
            val container  = dialogView.findViewById<android.widget.LinearLayout>(R.id.dialog_choices_container)
            val scrollView = android.widget.ScrollView(requireContext())
            val inner      = android.widget.LinearLayout(requireContext()).apply {
                orientation = android.widget.LinearLayout.VERTICAL
                setPadding(0, 8, 0, 8)
            }
            scrollView.addView(inner)

            // ── Section Tir : graphe ──────────────────────────────────────
            inner.addView(creerTitreSection("🎯 Tir — réussites par passage"))

            if (seriesTir.isEmpty()) {
                inner.addView(creerTexteVide("Aucune donnée de tir"))
            } else {
                // GraphView : composant custom qui dessine un graphe de ligne
                inner.addView(fr.iutvannes.dual.model.components.GraphView(requireContext()).apply {
                    layoutParams = android.widget.LinearLayout.LayoutParams(
                        android.widget.LinearLayout.LayoutParams.MATCH_PARENT, 300
                    )
                    data       = seriesTir
                    yMin       = 0
                    yMax       = 5
                    lineColor  = androidx.core.content.ContextCompat.getColor(requireContext(), R.color.vert)
                    labelColor = android.graphics.Color.WHITE
                    gridColor  = android.graphics.Color.argb(80, 255, 255, 255)
                    setBackgroundColor(android.graphics.Color.TRANSPARENT)
                })
            }

            // Séparateur visuel entre les deux sections
            inner.addView(View(requireContext()).apply {
                layoutParams = android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT, 1
                ).also { it.setMargins(0, 16, 0, 16) }
                setBackgroundColor(android.graphics.Color.argb(60, 255, 255, 255))
            })

            // ── Section Course : tableau ──────────────────────────────────
            inner.addView(creerTitreSection("🏃 Course — temps par tour"))

            if (toursCourse.isEmpty()) {
                inner.addView(creerTexteVide("Aucune donnée de course"))
            } else {
                inner.addView(creerTableauCourse(toursCourse))
            }

            container.addView(scrollView)

            dialogView.findViewById<TextView>(R.id.dialog_cancel).apply {
                text = "Fermer"
                setOnClickListener { dialog.dismiss() }
            }

            dialog.show()
        }
    }

    // ── Helpers de construction du dialog ────────────────────────────────────

    /**
     * Create a TextView with the given text.
     */
    private fun creerTitreSection(texte: String) = TextView(requireContext()).apply {
        text = texte
        setTextColor(android.graphics.Color.WHITE)
        textSize = 14f
        setPadding(8, 16, 8, 8)
    }

    /**
     * Create a TextView with the given text.
     */
    private fun creerTexteVide(texte: String) = TextView(requireContext()).apply {
        text = texte
        setTextColor(android.graphics.Color.argb(150, 255, 255, 255))
        textSize = 13f
        setPadding(8, 4, 8, 8)
    }

    /**
     * Creates a TableLayout with one row per turn.
     * The best turn is highlighted in green with a star ⭐.
     *
     * @param turns List of pairs (label, time in seconds)
     * @return TableLayout with one row per turn
     */
    private fun creerTableauCourse(tours: List<Pair<String, Float>>): android.widget.TableLayout {

        val table = android.widget.TableLayout(requireContext()).apply {
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setColumnStretchable(0, true)
            setColumnStretchable(1, true)
        }

        // Fonction locale : crée une cellule de tableau
        fun makeCell(texte: String, bold: Boolean = false, couleur: Int = android.graphics.Color.WHITE) =
            TextView(requireContext()).apply {
                text = texte
                setTextColor(couleur)
                textSize = 13f
                if (bold) setTypeface(null, android.graphics.Typeface.BOLD)
                setPadding(12, 8, 12, 8)
                gravity = android.view.Gravity.CENTER
            }

        // Ligne d'en-tête
        val rowEntete = android.widget.TableRow(requireContext())
        rowEntete.addView(makeCell("Tour",  bold = true, couleur = android.graphics.Color.argb(180, 255, 255, 255)))
        rowEntete.addView(makeCell("Temps", bold = true, couleur = android.graphics.Color.argb(180, 255, 255, 255)))
        table.addView(rowEntete)

        // Ligne de séparation sous l'en-tête
        table.addView(View(requireContext()).apply {
            layoutParams = android.widget.TableLayout.LayoutParams(
                android.widget.TableLayout.LayoutParams.MATCH_PARENT, 1
            )
            setBackgroundColor(android.graphics.Color.argb(80, 255, 255, 255))
        })

        val meilleurTemps = tours.minOf { it.second }

        tours.forEachIndexed { index, (label, secondes) ->
            // Formatage : 1'42"05 si plus d'une minute, sinon 42"05
            val min = (secondes / 60).toInt()
            val sec = (secondes % 60).toInt()
            val ms  = ((secondes % 1) * 100).toInt()
            val tempsFormate = if (min > 0) "%d'%02d\"%02d".format(min, sec, ms)
            else         "%d\"%02d".format(sec, ms)

            val isMeilleur = secondes == meilleurTemps
            val couleur    = if (isMeilleur) android.graphics.Color.parseColor("#2ECC71")
            else            android.graphics.Color.WHITE

            val row = android.widget.TableRow(requireContext()).apply {
                // Légère alternance de couleur de fond pour la lisibilité
                if (index % 2 == 0) setBackgroundColor(android.graphics.Color.argb(20, 255, 255, 255))
            }
            row.addView(makeCell(label, couleur = couleur))
            row.addView(makeCell(if (isMeilleur) "⭐ $tempsFormate" else tempsFormate, couleur = couleur))
            table.addView(row)
        }

        return table
    }

    // ── Export CSV ───────────────────────────────────────────────────────────

    /**
     * Generates a CSV file with the session results,
     * then opens the Android file selector to save it.
     * The content varies depending on the session type:
     * - VMA Test → Last Name, First Name, VMA
     * - Final Event → different columns depending on whether it's 4th or 6th grade
     * - Training → Last Name, First Name, Targets, VMA
     *
     * @param seance  Session à exporter
     */
    private fun genererEtExporterCSV(seance: Seance) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val resultats = withContext(Dispatchers.IO) {
                    DatabaseProvider.db.resultatDao().getBySeance(seance.id_seance)
                }

                val csv = StringBuilder()

                when (seance.type) {

                    "Test VMA" -> {
                        csv.append("\uFEFF")
                        csv.append("Test VMA - ${seance.classe} - ${seance.date}\n\n")
                        csv.append("Nom;Prénom;VMA (km/h)\n")
                        resultats.forEach { res ->
                            val eleve = withContext(Dispatchers.IO) {
                                DatabaseProvider.db.EleveDao().getEleveById(res.id_eleve)
                            }
                            if (eleve != null) {
                                val vma = eleve.vma?.let { "%.1f".format(it) } ?: "-"
                                csv.append("${eleve.nom.uppercase()};${eleve.prenom};$vma\n")
                            }
                        }
                    }

                    "Épreuve Finale" -> {
                        // Heuristique 4ème : ecart_max = 0 et nbTours = 6 fixe
                        val is4eme = resultats.all { it.ecart_max_course == 0 && it.nbTours == 6 }

                        if (is4eme) {
                            csv.append("\uFEFF")
                            csv.append("Épreuve Finale 4ème - ${seance.classe} - ${seance.date}\n\n")
                            csv.append("Nom;Prénom;VMA ref;Vitesse;% VMA;Course1;Tir1;Course2;Tir2;Course3;Cibles;Note /12\n")
                            resultats.forEach { res ->
                                val eleve = withContext(Dispatchers.IO) { DatabaseProvider.db.EleveDao().getEleveById(res.id_eleve) }
                                if (eleve != null) {
                                    fun fmtSec(sec: Int) = if (sec > 0) "${sec/60}'${"%02d".format(sec%60)}\"" else "-"
                                    val tc1 = fmtSec(res.temps_A)
                                    val tt1 = fmtSec(res.temps_B - res.temps_A)
                                    val tc2 = fmtSec(res.temps_C - res.temps_B)
                                    val tt2 = fmtSec(res.temps_D - res.temps_C)
                                    val tc3 = fmtSec(res.temps_E - res.temps_D)
                                    val vmaRef = eleve.vma?.let { "%.1f".format(it) } ?: "-"
                                    val vitesse = "%.2f".format(res.temp_course)
                                    val pct = if ((eleve.vma ?: 0f) > 0f) "%.0f%%".format((res.temp_course / eleve.vma!!) * 100) else "-"
                                    val note = "%.2f".format(res.note_finale)
                                    csv.append("${eleve.nom.uppercase()};${eleve.prenom};$vmaRef;$vitesse;$pct;$tc1;$tt1;$tc2;$tt2;$tc3;${res.cibles_touchees};$note\n")
                                }
                            }
                        } else {
                            csv.append("\uFEFF")
                            csv.append("Épreuve Finale 6ème - ${seance.classe} - ${seance.date}\n\n")
                            csv.append("Nom;Prénom;VMA (km/h);Nb tours;Écart max (s);Cibles;Note /15\n")
                            resultats.forEach { res ->
                                val eleve = withContext(Dispatchers.IO) {
                                    DatabaseProvider.db.EleveDao().getEleveById(res.id_eleve)
                                }
                                if (eleve != null) {
                                    val vma  = eleve.vma?.let { "%.1f".format(it) } ?: "-"
                                    val note = "%.2f".format(res.note_finale)
                                    csv.append("${eleve.nom.uppercase()};${eleve.prenom};$vma;${res.nbTours};${res.ecart_max_course};${res.cibles_touchees};$note\n")
                                }
                            }
                        }
                    }

                    else -> { // Entraînement
                        csv.append("\uFEFF")
                        csv.append("Entraînement - ${seance.classe} - ${seance.date}\n\n")
                        csv.append("Nom;Prénom;Nb séries tir;Moyenne tir;Meilleur tir;Nb tours;Meilleur tour;VMA (km/h)\n")
                        resultats.forEach { res ->
                            val eleve = withContext(Dispatchers.IO) { DatabaseProvider.db.EleveDao().getEleveById(res.id_eleve) }
                            if (eleve != null) {
                                val tirs = withContext(Dispatchers.IO) { DatabaseProvider.db.tirDao().getTirsBySeanceEtEleve(seance.id_seance, eleve.id_eleve) }
                                val scores = tirs.flatMap { it.liste_passages }.map { it.nb_tir_reussi }
                                val nbSeries = scores.size
                                val moyenne = if (nbSeries > 0) "%.1f".format(scores.average()) else "-"
                                val meilleurTir = scores.maxOrNull()?.toString() ?: "-"

                                val courses = withContext(Dispatchers.IO) { DatabaseProvider.db.courseDao().getCoursesBySeanceEtEleve(seance.id_seance, eleve.id_eleve) }
                                val nbTours = courses.sumOf { it.liste_tours.size }
                                val meilleurMs = courses.flatMap { it.liste_tours }.minOfOrNull { it.temps_ms }
                                val meilleurTour = meilleurMs?.let { val s = (it/1000).toInt(); "%d'%02d\"".format(s/60, s%60) } ?: "-"

                                val vma = eleve.vma?.let { "%.1f".format(it) } ?: "-"
                                csv.append("${eleve.nom.uppercase()};${eleve.prenom};$nbSeries;$moyenne;$meilleurTir;$nbTours;$meilleurTour;$vma\n")
                            }
                        }
                    }
                }

                // Nom du fichier basé sur la date de la séance
                val dateClean  = seance.date.replace("/", "-").replace(":", "h").replace(" ", "_")
                val nomFichier = "Bilan_${seance.type}_${seance.classe}_$dateClean.csv"

                contenuCsvEnAttente = csv.toString()
                exportLauncher.launch(nomFichier) // ouvre le sélecteur de fichier

            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Erreur génération CSV", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Open details of the final event result.
     *
     * @param eleve Eleve à afficher
     * @param res   Résultat à afficher
     */
    private fun afficherDetailEpreuveFinaleResultats(
        eleve: Eleve,
        res: Resultat
    ) {
        viewLifecycleOwner.lifecycleScope.launch {

            val dialog     = android.app.Dialog(requireContext())
            dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
            val dialogView = layoutInflater.inflate(R.layout.dialog_lancer_seance, null)
            dialog.setContentView(dialogView)
            dialogView.findViewById<TextView>(R.id.dialog_header_title).visibility = View.GONE
            dialog.window?.setBackgroundDrawable(
                android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT)
            )
            dialog.window?.setLayout(
                (resources.displayMetrics.widthPixels * 0.95).toInt(),
                (resources.displayMetrics.heightPixels * 0.85).toInt()
            )

            dialogView.findViewById<TextView>(R.id.dialog_subtitle).text =
                "${eleve.prenom} ${eleve.nom.uppercase()}"

            val container  = dialogView.findViewById<android.widget.LinearLayout>(R.id.dialog_choices_container)
            val scrollView = android.widget.ScrollView(requireContext())
            val inner      = android.widget.LinearLayout(requireContext()).apply {
                orientation = android.widget.LinearLayout.VERTICAL
                setPadding(0, 8, 0, 8)
            }
            scrollView.addView(inner)

            val is4eme = res.ecart_max_course == 0 && res.nbTours == 6
            val vmaRef = eleve.vma ?: 0f

            // Tableau principal des résultats
            val table = android.widget.TableLayout(requireContext()).apply {
                layoutParams = android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                )
                setColumnStretchable(0, true)
                setColumnStretchable(1, true)
            }

            /**
             * Make a row to the table.
             *
             * @param label    Label of the row
             * @param valeur   Value of the row
             * @param couleur  Color of the value
             */
            fun makeRow(label: String, valeur: String, couleur: Int = android.graphics.Color.WHITE) {
                val row = android.widget.TableRow(requireContext())
                row.addView(TextView(requireContext()).apply {
                    text = label
                    setTextColor(android.graphics.Color.argb(180, 255, 255, 255))
                    textSize = 13f
                    setPadding(12, 10, 12, 10)
                })
                row.addView(TextView(requireContext()).apply {
                    text = valeur
                    setTextColor(couleur)
                    textSize = 13f
                    setTypeface(null, android.graphics.Typeface.BOLD)
                    setPadding(12, 10, 12, 10)
                    gravity = android.view.Gravity.END
                })
                table.addView(row)
                table.addView(View(requireContext()).apply {
                    layoutParams = android.widget.TableLayout.LayoutParams(
                        android.widget.TableLayout.LayoutParams.MATCH_PARENT, 1
                    )
                    setBackgroundColor(android.graphics.Color.argb(30, 255, 255, 255))
                })
            }

            // VMA de référence toujours affichée en premier
            makeRow(
                "VMA de référence",
                if (vmaRef > 0) "%.1f km/h".format(vmaRef) else "-",
                android.graphics.Color.parseColor("#9BA3E8")
            )

            if (is4eme) {
                val vmaRef = eleve.vma ?: 0f
                val pctVal = if (vmaRef > 0) (res.temp_course / vmaRef) * 100 else 0f

                val parcours = when {
                    vmaRef <= 10f -> "Coupelles Jaunes (250m)"
                    vmaRef <= 11f -> "Plots Verts (275m)"
                    vmaRef <= 12f -> "Coupelles Bleues (300m)"
                    vmaRef <= 13f -> "Plots Bleus (325m)"
                    vmaRef <= 14f -> "Coupelles Rouges (350m)"
                    vmaRef <= 15f -> "Plots Rouges (375m)"
                    else          -> "Grand Tour (400m)"
                }

                /**
                 * Convert dp to px
                 *
                 * @param v Value in dp
                 */
                fun dp(v: Float) = (v * resources.displayMetrics.density).toInt()

                /**
                 * Convert hex to color
                 *
                 * @param hex Hexadecimal color
                 */
                fun color(hex: String) = android.graphics.Color.parseColor(hex)

                val white      = color("#FFFFFF")
                val textPrim   = color("#FFFFFF")
                val textMuted  = color("#9BA3E8")
                val cardBg     = color("#242660")
                val surfaceBg  = color("#1C1E4E")
                val divider    = color("#303586")

                // ── En-tête nom + note finale ─────────────────────────────────────────────
                val headerRow = android.widget.LinearLayout(requireContext()).apply {
                    orientation = android.widget.LinearLayout.HORIZONTAL
                    gravity = android.view.Gravity.CENTER_VERTICAL
                    setPadding(0, 0, 0, dp(16f))
                }

                // Avatar initiales
                val initiales = "${eleve.prenom.firstOrNull() ?: ""}${eleve.nom.firstOrNull() ?: ""}".uppercase()
                headerRow.addView(TextView(requireContext()).apply {
                    text = initiales
                    textSize = 14f
                    setTypeface(null, android.graphics.Typeface.BOLD)
                    setTextColor(color("#9BA3E8"))
                    gravity = android.view.Gravity.CENTER
                    setBackgroundColor(color("#303586"))
                    layoutParams = android.widget.LinearLayout.LayoutParams(dp(40f), dp(40f)).also {
                        it.marginEnd = dp(12f)
                    }
                })

                // Nom + parcours
                val nameCol = android.widget.LinearLayout(requireContext()).apply {
                    orientation = android.widget.LinearLayout.VERTICAL
                    layoutParams = android.widget.LinearLayout.LayoutParams(0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                }
                nameCol.addView(TextView(requireContext()).apply {
                    text = "${eleve.prenom} ${eleve.nom.uppercase()}"
                    textSize = 15f
                    setTypeface(null, android.graphics.Typeface.BOLD)
                    setTextColor(white)
                })
                nameCol.addView(TextView(requireContext()).apply {
                    text = "VMA %.1f km/h · %s".format(vmaRef, parcours)
                    textSize = 12f
                    setTextColor(textMuted)
                })
                headerRow.addView(nameCol)

                // Badge note finale
                val badgeNote = android.widget.LinearLayout(requireContext()).apply {
                    orientation = android.widget.LinearLayout.VERTICAL
                    gravity = android.view.Gravity.CENTER
                    setBackgroundColor(cardBg)
                    setPadding(dp(14f), dp(6f), dp(14f), dp(6f))
                    layoutParams = android.widget.LinearLayout.LayoutParams(
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                    ).also { it.marginStart = dp(12f) }
                }
                badgeNote.addView(TextView(requireContext()).apply {
                    text = "note finale"
                    textSize = 11f
                    setTextColor(textMuted)
                    gravity = android.view.Gravity.CENTER
                })
                badgeNote.addView(TextView(requireContext()).apply {
                    text = "%.2f".format(res.note_finale)
                    textSize = 22f
                    setTypeface(null, android.graphics.Typeface.BOLD)
                    setTextColor(white)
                    gravity = android.view.Gravity.CENTER
                })
                badgeNote.addView(TextView(requireContext()).apply {
                    text = "/ 12"
                    textSize = 12f
                    setTextColor(textMuted)
                    gravity = android.view.Gravity.CENTER
                })
                headerRow.addView(badgeNote)
                inner.addView(headerRow)

                // ── 3 cartes métriques ────────────────────────────────────────────────────
                val metricsRow = android.widget.LinearLayout(requireContext()).apply {
                    orientation = android.widget.LinearLayout.HORIZONTAL
                    setPadding(0, 0, 0, dp(12f))
                }

                /**
                 * Create a metric card with the given parameters.
                 *
                 * @param label Label of the card
                 * @param value Value of the card
                 * @param max Max value of the card
                 * @param sub Subtext of the card
                 * @return android.widget.LinearLayout
                 */
                fun metricCard(label: String, value: String, max: String, sub: String): android.widget.LinearLayout {
                    return android.widget.LinearLayout(requireContext()).apply {
                        orientation = android.widget.LinearLayout.VERTICAL
                        setBackgroundColor(cardBg)
                        setPadding(dp(12f), dp(12f), dp(12f), dp(12f))
                        layoutParams = android.widget.LinearLayout.LayoutParams(0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f).also {
                            it.marginEnd = dp(6f)
                        }
                        addView(TextView(requireContext()).apply {
                            text = label; textSize = 11f; setTextColor(textMuted)
                        })
                        val valRow = android.widget.LinearLayout(requireContext()).apply {
                            orientation = android.widget.LinearLayout.HORIZONTAL
                            gravity = android.view.Gravity.BOTTOM
                        }
                        valRow.addView(TextView(requireContext()).apply {
                            text = value; textSize = 20f
                            setTypeface(null, android.graphics.Typeface.BOLD)
                            setTextColor(white)
                        })
                        valRow.addView(TextView(requireContext()).apply {
                            text = " /$max"; textSize = 12f; setTextColor(textMuted)
                        })
                        addView(valRow)
                        addView(TextView(requireContext()).apply {
                            text = sub; textSize = 11f; setTextColor(textMuted)
                            setPadding(0, dp(4f), 0, 0)
                        })
                    }
                }

                val tempsTirTotal = (res.temps_B - res.temps_A) + (res.temps_D - res.temps_C)
                val minTir = tempsTirTotal / 60; val secTir = tempsTirTotal % 60
                val tempsTirStr = "%d'%02d\"".format(minTir, secTir)

                metricsRow.addView(metricCard("intensité", "%.1f".format(res.note_intensite), "4", "%.1f%% VMA".format(pctVal)))
                metricsRow.addView(metricCard("efficience tir", "%.1f".format(res.note_efficience), "6", "${res.cibles_touchees}/10 · $tempsTirStr"))
                metricsRow.addView(android.widget.LinearLayout(requireContext()).apply {
                    orientation = android.widget.LinearLayout.VERTICAL
                    setBackgroundColor(cardBg)
                    setPadding(dp(12f), dp(12f), dp(12f), dp(12f))
                    layoutParams = android.widget.LinearLayout.LayoutParams(0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    addView(TextView(requireContext()).apply { text = "vma"; textSize = 11f; setTextColor(textMuted) })
                    val valRow = android.widget.LinearLayout(requireContext()).apply {
                        orientation = android.widget.LinearLayout.HORIZONTAL; gravity = android.view.Gravity.BOTTOM
                    }
                    valRow.addView(TextView(requireContext()).apply {
                        text = "%.2f".format(res.note_vma); textSize = 20f
                        setTypeface(null, android.graphics.Typeface.BOLD); setTextColor(white)
                    })
                    valRow.addView(TextView(requireContext()).apply { text = " /2"; textSize = 12f; setTextColor(textMuted) })
                    addView(valRow)
                    addView(TextView(requireContext()).apply {
                        text = "%.1f km/h".format(vmaRef); textSize = 11f; setTextColor(textMuted)
                        setPadding(0, dp(4f), 0, 0)
                    })
                })
                inner.addView(metricsRow)

                // ── Helper tableau ────────────────────────────────────────────────────────
                /**
                 * Create a table with the given parameters.
                 *
                 * @param titre Titre du tableau
                 * @param lignes Liste de paires (label, valeur)
                 */
                fun ajouterTableau(titre: String, lignes: List<Pair<String, String>>) {
                    val card = android.widget.LinearLayout(requireContext()).apply {
                        orientation = android.widget.LinearLayout.VERTICAL
                        setBackgroundColor(surfaceBg)
                        layoutParams = android.widget.LinearLayout.LayoutParams(
                            android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                        ).also { it.bottomMargin = dp(10f) }
                    }
                    card.addView(TextView(requireContext()).apply {
                        text = titre; textSize = 12f; setTextColor(textMuted)
                        setPadding(dp(14f), dp(10f), dp(14f), dp(10f))
                        setBackgroundColor(surfaceBg)
                    })
                    card.addView(View(requireContext()).apply {
                        layoutParams = android.widget.LinearLayout.LayoutParams(
                            android.widget.LinearLayout.LayoutParams.MATCH_PARENT, 1
                        )
                        setBackgroundColor(divider)
                    })
                    lignes.forEachIndexed { i, (label, valeur) ->
                        val row = android.widget.LinearLayout(requireContext()).apply {
                            orientation = android.widget.LinearLayout.HORIZONTAL
                            gravity = android.view.Gravity.CENTER_VERTICAL
                            setPadding(dp(14f), dp(9f), dp(14f), dp(9f))
                            if (i % 2 == 1) setBackgroundColor(cardBg)
                        }
                        row.addView(TextView(requireContext()).apply {
                            text = label; textSize = 13f; setTextColor(textMuted)
                            layoutParams = android.widget.LinearLayout.LayoutParams(
                                0,
                                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                                1f
                            )
                        })
                        row.addView(TextView(requireContext()).apply {
                            text = valeur; textSize = 13f
                            setTypeface(null, android.graphics.Typeface.BOLD)
                            setTextColor(white)
                        })
                        card.addView(row)
                        if (i < lignes.size - 1) {
                            card.addView(View(requireContext()).apply {
                                layoutParams = android.widget.LinearLayout.LayoutParams(
                                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT, 1
                                )
                                setBackgroundColor(divider)
                            })
                        }
                    }

                    inner.addView(card)
                }
                // ── Tableau segments ──────────────────────────────────────────────────────
                /**
                 * Convert seconds to minutes and seconds.
                 *
                 * @param sec Seconds
                 * @return String in the format "mm'ss" or "ss"'
                 */
                fun fmtSec(sec: Int): String {
                    val m = sec / 60; val s = sec % 60
                    return if (m > 0) "%d'%02d\"".format(m, s) else "%d\"".format(s)
                }

                if (res.temps_A > 0) {
                    val tc1 = res.temps_A
                    val tt1 = res.temps_B - res.temps_A
                    val tc2 = res.temps_C - res.temps_B
                    val tt2 = res.temps_D - res.temps_C
                    val tc3 = res.temps_E - res.temps_D

                    val segLignes = mutableListOf<Pair<String, String>>()
                    if (tc1 > 0) segLignes.add("Course 1  (2 tours)" to fmtSec(tc1))
                    if (tt1 > 0) segLignes.add("Tir 1" to fmtSec(tt1))
                    if (tc2 > 0) segLignes.add("Course 2  (2 tours + pén.)" to fmtSec(tc2))
                    if (tt2 > 0) segLignes.add("Tir 2" to fmtSec(tt2))
                    if (tc3 > 0) segLignes.add("Course 3  (2 tours + pén.)" to fmtSec(tc3))

                    ajouterTableau("temps par segment", segLignes)
                }

                // ── Tableau tir ───────────────────────────────────────────────────────────
                ajouterTableau("tir", listOf(
                    "Série 1" to "${res.tir1} / 5",
                    "Série 2" to "${res.tir2} / 5"
                ))
            } else {
                // 6ème — inchangé
                val medailleTours = when {
                    res.nbTours >= 7 -> "💎 DIAMANT"
                    res.nbTours >= 6 -> "🏆 OR"
                    res.nbTours >= 5 -> "🥈 ARGENT"
                    res.nbTours >= 4 -> "🥉 BRONZE"
                    else             -> "—"
                }
                val medailleEcart = when {
                    res.ecart_max_course < 10  -> "💎 DIAMANT"
                    res.ecart_max_course <= 15 -> "🏆 OR"
                    res.ecart_max_course <= 20 -> "🥈 ARGENT"
                    res.ecart_max_course <= 25 -> "🥉 BRONZE"
                    else                       -> "—"
                }
                val medailleTir = when {
                    res.cibles_touchees >= 21 -> "💎 DIAMANT"
                    res.cibles_touchees >= 19 -> "🏆 OR"
                    res.cibles_touchees >= 16 -> "🥈 ARGENT"
                    res.cibles_touchees >= 12 -> "🥉 BRONZE"
                    else                      -> "—"
                }

                makeRow("VMA référence", "%.1f km/h".format(vmaRef),
                    android.graphics.Color.parseColor("#9BA3E8"))
                makeRow("Tours ($medailleTours)", "${res.nbTours} tours")
                makeRow("Régularité ($medailleEcart)", "Écart max : ${res.ecart_max_course} s")
                makeRow("Tir ($medailleTir)", "${res.cibles_touchees} cibles")
                makeRow("🏅 Note finale", "%.2f / 15".format(res.note_finale),
                    android.graphics.Color.parseColor("#F1C40F"))

                inner.addView(table)
            }

            // Section Ressenti
            if (res.ressenti_intensite.isNotEmpty() || res.ressenti_durer.isNotEmpty() || res.ressenti_lucidite.isNotEmpty()) {

                inner.addView(View(requireContext()).apply {
                    layoutParams = android.widget.LinearLayout.LayoutParams(
                        android.widget.LinearLayout.LayoutParams.MATCH_PARENT, 1
                    ).also { it.setMargins(0, 16, 0, 16) }
                    setBackgroundColor(android.graphics.Color.argb(60, 255, 255, 255))
                })

                inner.addView(TextView(requireContext()).apply {
                    text = "💬 Ressenti de l'élève"
                    setTextColor(android.graphics.Color.WHITE)
                    textSize = 14f
                    setPadding(8, 8, 8, 12)
                })

                val emojiMap = mapOf(
                    // Intensité 6ème
                    "Tranquille" to "🟢", "Chaud" to "🟡", "Essoufflé" to "🟠", "À bout" to "🔴",
                    // Intensité 4ème
                    "Contrôlé" to "🟢", "Intense" to "🟡", "Critique" to "🟠", "Saturation" to "🔴",
                    "Peu essoufflé" to "🟢", "Effort soutenu" to "🟡", "Gros souffle" to "🟠", "Épuisé" to "🔴",
                    // Durée 6ème
                    "Lent" to "🐢", "Bien" to "✅", "Vite" to "🥵",
                    // Durée 4ème
                    "Régulier" to "✅", "Économie" to "🐢", "Décroissant" to "📉",
                    "Vitesse stable" to "✅", "Gardé de la réserve" to "🐢", "Fin de course difficile" to "📉",
                    // Lucidité 6ème
                    "Zen" to "🎯", "Bouge" to "⚖️",
                    // Lucidité 4ème
                    "Équilibré" to "⚖️", "Prudent" to "🎯", "Instable" to "🤠",
                    "Rapide et précis" to "⚖️", "Calme et appliqué" to "🎯", "Précipité / Tremblant" to "🤠"
                )

                fun creerBadgeRessenti(valeur: String, questionLabel: String) {
                    val emoji = emojiMap[valeur] ?: "•"
                    val dp = { v: Float -> (v * resources.displayMetrics.density).toInt() }
                    val row = android.widget.LinearLayout(requireContext()).apply {
                        orientation = android.widget.LinearLayout.HORIZONTAL
                        gravity = android.view.Gravity.CENTER_VERTICAL
                        setPadding(dp(4f), dp(6f), dp(4f), dp(6f))
                        layoutParams = android.widget.LinearLayout.LayoutParams(
                            android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                        ).also { it.bottomMargin = dp(8f) }
                    }
                    row.addView(TextView(requireContext()).apply {
                        text = questionLabel
                        setTextColor(android.graphics.Color.argb(160, 255, 255, 255))
                        textSize = 13f
                        layoutParams = android.widget.LinearLayout.LayoutParams(
                            0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f
                        )
                    })
                    row.addView(TextView(requireContext()).apply {
                        text = "$emoji  $valeur"
                        setTextColor(android.graphics.Color.WHITE)
                        textSize = 13f
                        setTypeface(null, android.graphics.Typeface.BOLD)
                        setPadding(dp(14f), dp(8f), dp(14f), dp(8f))
                        background = android.graphics.drawable.GradientDrawable().apply {
                            shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                            cornerRadius = dp(20f).toFloat()
                            setColor(android.graphics.Color.argb(60, 255, 255, 255))
                            setStroke(dp(1f), android.graphics.Color.argb(100, 255, 255, 255))
                        }
                    })
                    inner.addView(row)
                }

                if (res.ressenti_intensite.isNotEmpty()) creerBadgeRessenti(res.ressenti_intensite, "Niveau d'engagement")
                if (res.ressenti_durer.isNotEmpty())     creerBadgeRessenti(res.ressenti_durer,     "Gestion de l'allure")
                if (res.ressenti_lucidite.isNotEmpty())  creerBadgeRessenti(res.ressenti_lucidite,  "Lucidité face aux cibles")
            }

            // On attache le scrollView au container APRÈS avoir tout construit
            container.addView(scrollView)

            dialogView.findViewById<TextView>(R.id.dialog_cancel).apply {
                text = "Fermer"
                setOnClickListener { dialog.dismiss() }
            }

            dialog.show()
        }
    }

    /**
     * Formats the date to a more readable format.
     *
     * @param date Date to format
     * @return Formatted date
     */
    private fun formaterDate(date: String): String {
        return try {
            val sdf = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.FRANCE)
            val d = sdf.parse(date) ?: return date
            val jours = arrayOf("Dimanche", "Lundi", "Mardi", "Mercredi", "Jeudi", "Vendredi", "Samedi")
            val cal = java.util.Calendar.getInstance()
            cal.time = d
            val jour = jours[cal.get(java.util.Calendar.DAY_OF_WEEK) - 1]
            val dateFormatee = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.FRANCE).format(d)
            "$jour $dateFormatee"
        } catch (e: Exception) {
            date.substringBefore(" ")
        }
    }
}
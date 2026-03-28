package fr.iutvannes.dual.controller.fragments

import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import fr.iutvannes.dual.R
import fr.iutvannes.dual.controller.MainActivity
import fr.iutvannes.dual.controller.viewmodel.SessionViewModel
import fr.iutvannes.dual.infrastructure.server.KtorServer
import fr.iutvannes.dual.model.database.AppDatabase
import fr.iutvannes.dual.model.utils.EmailService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.widget.LinearLayout

/**
 * Fragment to display the dashboard.
 *
 * @see SessionViewModel
 * @see AppDatabase
 * @see R.layout.fragment_tableau_de_bord
 */
class TableauDeBordFragment : Fragment(R.layout.fragment_tableau_de_bord) {

    /* Variable to keep track of the initial count */
    private var countInitial = 0

    /* Variable for the SessionViewModel instance */
    private val sessionViewModel: SessionViewModel by activityViewModels()

    private var classeActuelle: String = ""

    /**
     * This function is called when the fragment view is created.
     * It initializes interactions with views.
     *
     * @param view The fragment view.
     * @param savedInstanceState The data saved during the activity's state.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val sessionBtn = view.findViewById<Button>(R.id.launchASession)

        // Managing the click on the session start button
        sessionBtn.setOnClickListener {
            //Si la séance est déjà en cours, on arrête la séance
            if (sessionViewModel.running.value) {
                viewLifecycleOwner.lifecycleScope.launch {
                    val nbResultats = withContext(Dispatchers.IO) {
                        DatabaseProvider.db.resultatDao().countBySeance(KtorServer.idSeanceActuelle)
                    }
                    val nbEleves = withContext(Dispatchers.IO) {
                        DatabaseProvider.db.EleveDao().countElevesByClasse(sessionViewModel.nomClasse.value)
                    }
                    afficherDialogFinSeance(nbResultats, nbEleves)
                }


            //Si la séance n'est pas en cours, on la lance
            } else {
                viewLifecycleOwner.lifecycleScope.launch {
                    val classes = withContext(Dispatchers.IO) {
                        DatabaseProvider.db.classeDao().getAllNames()
                    }
                    afficherDialogChoix(classes)
                }
            }
        }

        val layoutUrl = view.findViewById<View>(R.id.layoutUrl)
        val cardResultats = view.findViewById<View>(R.id.cardResultats)
        val nbResultat = view.findViewById<TextView>(R.id.text_resultats_count)
        val btnExport = view.findViewById<Button>(R.id.btn_download_excel)

        cardResultats.setOnClickListener {
            if (KtorServer.idSeanceActuelle != 0) {
                viewLifecycleOwner.lifecycleScope.launch {
                    val type = sessionViewModel.typeSeance.value
                    val classe = sessionViewModel.nomClasse.value
                    val resultats = withContext(Dispatchers.IO) {
                        DatabaseProvider.db.resultatDao().getBySeance(KtorServer.idSeanceActuelle)
                    }
                    afficherDialogResultatsEnDirect(type, classe, resultats)
                }
            }
        }

        //Opening a coroutine in the I/O thread to count the results
        viewLifecycleOwner.lifecycleScope.launch {
            while (true) {
                if (sessionViewModel.running.value && KtorServer.idSeanceActuelle != 0) {
                    val nbBilans = withContext(Dispatchers.IO) {
                        DatabaseProvider.db.resultatDao().countBySeance(KtorServer.idSeanceActuelle)
                    }
                    nbResultat.text = "$nbBilans"
                }
                kotlinx.coroutines.delay(2000)
            }
        }

        val qrCode = view.findViewById<ImageView>(R.id.qrCodeView)
        qrCode.setBackgroundColor(Color.DKGRAY) // DEBUG
        val sessionUrl = view.findViewById<TextView>(R.id.textUrl)
        // Opening a coroutine in the IO thread to generate the QR code
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            sessionViewModel.url.collect { url ->
                if (url != null) {
                    Toast.makeText(requireContext(), "URL: $url", Toast.LENGTH_LONG).show()
                    qrCode.visibility = View.VISIBLE
                    sessionUrl.visibility = View.VISIBLE
                    nbResultat.visibility = View.VISIBLE
                    btnExport.visibility = View.VISIBLE
                    sessionUrl.text = url
                    qrCode.setImageBitmap(genererQRCode(url))
                }
            }
        }



        //Opening a coroutine in the I/O thread to handle the session start button
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            sessionViewModel.running.collect { running ->
                if (running) {
                    sessionBtn.text = "Arrêter la séance"
                    val couleurBleu = ContextCompat.getColor(requireContext(), R.color.bleu)
                    sessionBtn.backgroundTintList = android.content.res.ColorStateList.valueOf(couleurBleu)
                    qrCode.visibility = View.VISIBLE
                    layoutUrl.visibility = View.VISIBLE
                    cardResultats.visibility = View.VISIBLE
                    btnExport.visibility = View.VISIBLE

                    //Récupếration des infos du ViewModel
                    val classe = sessionViewModel.nomClasse.value
                    val type = sessionViewModel.typeSeance.value
                    val dateAujourdhui = SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE).format(Date())

                    //Mise à jour du texte du bouton avec les données persistantes
                    btnExport.text = "Télécharger les résultats ($type - $classe - $dateAujourdhui)"
                } else {
                    sessionBtn.text = "Lancer une séance"
                    val couleurBleu = ContextCompat.getColor(requireContext(), R.color.bleu)
                    sessionBtn.backgroundTintList = android.content.res.ColorStateList.valueOf(couleurBleu)
                    qrCode.visibility = View.GONE
                    layoutUrl.visibility = View.GONE
                    cardResultats.visibility = View.GONE
                }
            }
        }

        // Managing clicks on the export button
        btnExport.setOnClickListener {
            val currentUrl = sessionViewModel.url.value
            if (currentUrl != null) {
                // We construct the download URL
                val downloadUrl = "$currentUrl/api/admin/export"

                // We open the tablet's browser to start the download
                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(downloadUrl))
                startActivity(intent)
            } else {
                Toast.makeText(requireContext(), "Démarrez une session d'abord", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Generates a QR code from text.
     *
     * @param text The text to encode in the QR code.
     * @return The generated QR code as a Bitmap.
     */
    private fun genererQRCode(text: String): Bitmap {
        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(text, BarcodeFormat.QR_CODE, 512, 512)
        val bmp = Bitmap.createBitmap(512, 512, Bitmap.Config.RGB_565)
        for (x in 0 until 512) {
            for (y in 0 until 512) {
                bmp.setPixel(x, y, if (bitMatrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
            }
        }
        return bmp
    }

    /**
     * Méthode pour lancer une séance.
     * @param type : type de séance (Entraînement ou Évaluation)
     */
    private fun lancerLaSeance(classe: String, type: String) {
        classeActuelle = classe
        viewLifecycleOwner.lifecycleScope.launch {
            val idGenere = withContext(Dispatchers.IO) {
                val nouvelleSeance = fr.iutvannes.dual.model.persistence.Seance(
                    date = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRANCE).format(Date()),
                    id_prof = DatabaseProvider.db.profDAO().getProfId(),
                    type = type,    //"Entraînement" ou "Épreuve Finale"
                    classe = classe
                )
                DatabaseProvider.db.seanceDao().insert(nouvelleSeance)
            }

            KtorServer.idSeanceActuelle = idGenere.toInt()

            val count = withContext(Dispatchers.IO) {
                DatabaseProvider.db.resultatDao().getCount()
            }
            (activity as MainActivity).countInitialSession = count

            sessionViewModel.startSession(requireContext(), classe, type)

            Toast.makeText(requireContext(), "Séance $type ($classe) lancée", Toast.LENGTH_SHORT).show()
        }
    }

    private fun afficherDialogChoix(classes: List<String>) {
        val dialog = android.app.Dialog(requireContext())
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)

        val view = layoutInflater.inflate(R.layout.dialog_lancer_seance, null)
        dialog.setContentView(view)

        dialog.window?.setBackgroundDrawable(
            android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT)
        )
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.88).toInt(),
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        )

        val container = view.findViewById<LinearLayout>(R.id.dialog_choices_container)
        val subtitle = view.findViewById<TextView>(R.id.dialog_subtitle)
        subtitle.text = "Sélectionnez la classe"

        val icons = listOf("🏫", "📚", "🎯", "⭐", "🏆", "📋")

        classes.forEachIndexed { index, nomClasse ->
            val item = layoutInflater.inflate(R.layout.item_dialog_choix, container, false)
            item.findViewById<TextView>(R.id.item_icon).text = icons.getOrElse(index) { "📋" }
            item.findViewById<TextView>(R.id.item_label).text = nomClasse
            item.setOnClickListener { afficherDialogType(dialog, nomClasse) }
            container.addView(item)
        }

        view.findViewById<TextView>(R.id.dialog_cancel).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    /**
     * Méthode pour afficher le type de séance.
     * @param dialog : dialog en cours
     */
    private fun afficherDialogType(dialog: android.app.Dialog, classeChoisie: String) {
        val container = dialog.findViewById<LinearLayout>(R.id.dialog_choices_container)
        val subtitle = dialog.findViewById<TextView>(R.id.dialog_subtitle)

        subtitle.text = classeChoisie
        container?.removeAllViews()

        val types = listOf(
            Triple("🧪", "Test VMA", "#E67E22"),
            Triple("🏃", "Entraînement", "#2980B9"),
            Triple("🏆", "Épreuve Finale", "#27AE60")
        )

        types.forEach { (icon, typeSeance, couleur) ->
            val item = layoutInflater.inflate(R.layout.item_dialog_choix, container, false)
            val iconView = item.findViewById<TextView>(R.id.item_icon)
            iconView.text = icon
            iconView.setBackgroundColor(android.graphics.Color.parseColor(couleur))
            item.findViewById<TextView>(R.id.item_label).text = typeSeance
            item.setOnClickListener {
                dialog.dismiss()
                lancerLaSeance(classeChoisie, typeSeance)
            }
            container?.addView(item)
        }

        dialog.findViewById<TextView>(R.id.dialog_cancel)?.apply {
            text = "← Changer de classe"
            setOnClickListener {
                viewLifecycleOwner.lifecycleScope.launch {
                    val classes = withContext(Dispatchers.IO) {
                        DatabaseProvider.db.classeDao().getAllNames()
                    }
                    afficherDialogChoix(classes)
                    dialog.dismiss()
                }
            }
        }
    }

    private fun afficherDialogFinSeance(nbResultats: Int, nbEleves: Int) {
        val dialog = android.app.Dialog(requireContext())
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)

        val view = layoutInflater.inflate(R.layout.dialog_lancer_seance, null)
        dialog.setContentView(view)

        dialog.window?.setBackgroundDrawable(
            android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT)
        )
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.88).toInt(),
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        )

        // Titre
        view.findViewById<TextView>(R.id.dialog_subtitle).text = "Terminer la séance ?"

        val container = view.findViewById<LinearLayout>(R.id.dialog_choices_container)

        // Carte de stats
        val statsView = layoutInflater.inflate(R.layout.item_dialog_choix, container, false)
        val pct = if (nbEleves > 0) (nbResultats * 100) / nbEleves else 0
        val emoji = when {
            pct >= 100 -> "✅"
            pct >= 50  -> "⏳"
            else       -> "⚠️"
        }
        statsView.findViewById<TextView>(R.id.item_icon).text = emoji
        statsView.findViewById<TextView>(R.id.item_label).apply {
            text = "$nbResultats / $nbEleves élèves\nont soumis leurs résultats"
            setTextColor(android.graphics.Color.WHITE)
        }
        // Pas de flèche cliquable sur la carte stats
        statsView.isClickable = false
        container.addView(statsView)

        // Espace
        val spacer = View(requireContext())
        spacer.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 16
        )
        container.addView(spacer)

        // Bouton Terminer
        val btnTerminer = layoutInflater.inflate(R.layout.item_dialog_choix, container, false)
        btnTerminer.findViewById<TextView>(R.id.item_icon).apply {
            text = "🛑"
            setBackgroundColor(android.graphics.Color.parseColor("#E74C3C"))
        }
        btnTerminer.findViewById<TextView>(R.id.item_label).text = "Oui, terminer la séance"
        btnTerminer.setOnClickListener {
            dialog.dismiss()
            sessionViewModel.stopSession()
            KtorServer.idSeanceActuelle = 0
            Toast.makeText(requireContext(), "Séance terminée", Toast.LENGTH_SHORT).show()
        }
        container.addView(btnTerminer)

        // Annuler
        view.findViewById<TextView>(R.id.dialog_cancel).apply {
            text = "Continuer la séance"
            setOnClickListener { dialog.dismiss() }
        }

        dialog.show()
    }

    private fun afficherDialogResultatsEnDirect(
        type: String,
        classe: String,
        resultats: List<fr.iutvannes.dual.model.persistence.Resultat>
    ) {
        viewLifecycleOwner.lifecycleScope.launch {

            // Charger TOUS les élèves en IO avant d'ouvrir le dialog
            data class LigneResultat(
                val idEleve: Int,
                val prenom: String,
                val nom: String,
                val emoji: String,
                val detail: String
            )

            val lignes = withContext(Dispatchers.IO) {
                resultats.mapNotNull { res ->
                    val eleve = DatabaseProvider.db.EleveDao().getEleveById(res.id_eleve)
                        ?: return@mapNotNull null

                    val (emoji, detail) = when (type) {
                        "Test VMA" -> {
                            val vma = eleve.vma?.let { "%.1f km/h".format(it) } ?: "-"
                            "🧪" to "VMA : $vma"
                        }
                        "Épreuve Finale" -> {
                            val note = "%.2f".format(res.note_finale)
                            val is4eme = res.ecart_max_course == 0 && res.nbTours == 6
                            val detail2 = if (is4eme) {
                                val pct = if ((eleve.vma ?: 0f) > 0f)
                                    "%.0f%%".format((res.temp_course / eleve.vma!!) * 100)
                                else "-"
                                "Note : $note  •  Cibles : ${res.cibles_touchees}/10  •  VMA : $pct"
                            } else {
                                "Note : $note  •  Cibles : ${res.cibles_touchees}  •  Tours : ${res.nbTours}"
                            }
                            "🏆" to detail2
                        }
                        else -> {
                            val idSeance = KtorServer.idSeanceActuelle

                            // Récupération des tirs de l'élève pour cette séance
                            val tirs = DatabaseProvider.db.tirDao()
                                .getTirsBySeanceEtEleve(idSeance, eleve.id_eleve)
                            val totalCibles = tirs.sumOf { t ->
                                t.liste_passages.sumOf { it.nb_tir_reussi }
                            }
                            val nbSeries = tirs.sumOf { it.liste_passages.size }

                            // Récupération des courses de l'élève pour cette séance
                            val courses = DatabaseProvider.db.courseDao()
                                .getCoursesBySeanceEtEleve(idSeance, eleve.id_eleve)
                            val nbTours = courses.sumOf { it.liste_tours.size }
                            val meilleurTourMs = courses
                                .flatMap { it.liste_tours }
                                .minOfOrNull { it.temps_ms }
                            val meilleurTourStr = meilleurTourMs?.let {
                                val sec = (it / 1000).toInt()
                                "%d'%02d\"".format(sec / 60, sec % 60)
                            } ?: "-"

                            val ligneTir = if (nbSeries > 0) "🎯 Tir : $totalCibles / ${nbSeries * 5}" else "🎯 Tir : -"
                            val ligneCourse = if (nbTours > 0) "🏃 Course : $nbTours tours  •  meilleur : $meilleurTourStr" else "🏃 Course : -"

                            "📋" to "$ligneTir\n$ligneCourse"
                        }
                    }

                    LigneResultat(eleve.id_eleve, eleve.prenom, eleve.nom, emoji, detail)
                }
            }

            // Maintenant on construit et affiche le dialog sur le Main thread
            val dialog = android.app.Dialog(requireContext())
            dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)

            val view = layoutInflater.inflate(R.layout.dialog_lancer_seance, null)
            dialog.setContentView(view)

            dialog.window?.setBackgroundDrawable(
                android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT)
            )
            dialog.window?.setLayout(
                (resources.displayMetrics.widthPixels * 0.92).toInt(),
                (resources.displayMetrics.heightPixels * 0.80).toInt()
            )

            view.findViewById<TextView>(R.id.dialog_subtitle).text =
                "${lignes.size} résultat${if (lignes.size > 1) "s" else ""}  —  $classe"

            val container = view.findViewById<LinearLayout>(R.id.dialog_choices_container)
            val scrollView = android.widget.ScrollView(requireContext())
            val innerContainer = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
            }
            scrollView.addView(innerContainer)

            if (lignes.isEmpty()) {
                val vide = layoutInflater.inflate(R.layout.item_dialog_choix, innerContainer, false)
                vide.findViewById<TextView>(R.id.item_icon).text = "⏳"
                vide.findViewById<TextView>(R.id.item_label).text = "En attente des premiers résultats..."
                vide.isClickable = false
                innerContainer.addView(vide)
            } else {
                lignes.forEach { ligne ->
                    val item = layoutInflater.inflate(R.layout.item_dialog_choix, innerContainer, false)
                    item.findViewById<TextView>(R.id.item_icon).text = ligne.emoji
                    item.findViewById<TextView>(R.id.item_label).apply {
                        text = "${ligne.prenom} ${ligne.nom.uppercase()}\n${ligne.detail}"
                        setTextColor(android.graphics.Color.WHITE)
                    }
                    // Cliquable seulement pour l'entraînement
                    if (type == "Entraînement") {
                        item.setOnClickListener {
                            afficherGraphsEleve(ligne.idEleve, ligne.prenom, ligne.nom)
                        }
                    } else {
                        item.isClickable = false
                    }
                    innerContainer.addView(item)
                }
            }

            container.addView(scrollView)

            view.findViewById<TextView>(R.id.dialog_cancel).apply {
                text = "Fermer"
                setOnClickListener { dialog.dismiss() }
            }

            // Rafraîchissement automatique toutes les 3 secondes
            val job = viewLifecycleOwner.lifecycleScope.launch {
                while (true) {
                    kotlinx.coroutines.delay(3000)

                    val nouveauxResultats = withContext(Dispatchers.IO) {
                        DatabaseProvider.db.resultatDao().getBySeance(KtorServer.idSeanceActuelle)
                    }

                    // Recharge uniquement si le nombre a changé
                    if (nouveauxResultats.size != resultats.size) {
                        dialog.dismiss()
                        afficherDialogResultatsEnDirect(type, classe, nouveauxResultats)
                        return@launch
                    }
                }
            }

            // On arrête la coroutine quand le dialog se ferme
            dialog.setOnDismissListener {
                job.cancel()
            }

            dialog.show()
        }
    }

    private fun afficherGraphsEleve(idEleve: Int, prenom: String, nom: String) {
        viewLifecycleOwner.lifecycleScope.launch {

            val idSeance = KtorServer.idSeanceActuelle

            data class DonneesTir(val series: List<Pair<String, Float>>)
            data class DonneesCourse(val tours: List<Pair<String, Float>>)

            val (donneesTir, donneesCourse) = withContext(Dispatchers.IO) {
                // --- TIR : moyenne par salve ---
                val tirs = DatabaseProvider.db.tirDao()
                    .getTirsBySeanceEtEleve(idSeance, idEleve)
                val seriesTir = tirs.flatMapIndexed { tirIdx, tirAvec ->
                    tirAvec.liste_passages.mapIndexed { passageIdx, passage ->
                        "S${tirIdx + 1}.${passageIdx + 1}" to passage.nb_tir_reussi.toFloat()
                    }
                }

                // --- COURSE : temps de chaque tour en secondes ---
                val courses = DatabaseProvider.db.courseDao()
                    .getCoursesBySeanceEtEleve(idSeance, idEleve)
                val toursCourse = courses.flatMapIndexed { courseIdx, courseAvec ->
                    courseAvec.liste_tours.mapIndexed { tourIdx, tour ->
                        "T${courseIdx + 1}.${tourIdx + 1}" to (tour.temps_ms / 1000f)
                    }
                }

                DonneesTir(seriesTir) to DonneesCourse(toursCourse)
            }

            // Construction du dialog
            val dialog = android.app.Dialog(requireContext())
            dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)

            val view = layoutInflater.inflate(R.layout.dialog_lancer_seance, null)
            dialog.setContentView(view)

            dialog.window?.setBackgroundDrawable(
                android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT)
            )
            dialog.window?.setLayout(
                (resources.displayMetrics.widthPixels * 0.95).toInt(),
                (resources.displayMetrics.heightPixels * 0.85).toInt()
            )

            view.findViewById<TextView>(R.id.dialog_subtitle).text =
                "$prenom ${nom.uppercase()}"

            val container = view.findViewById<android.widget.LinearLayout>(R.id.dialog_choices_container)
            val scrollView = android.widget.ScrollView(requireContext())
            val inner = android.widget.LinearLayout(requireContext()).apply {
                orientation = android.widget.LinearLayout.VERTICAL
                setPadding(0, 8, 0, 8)
            }
            scrollView.addView(inner)

            // --- Graphe TIR ---
            val titreTir = TextView(requireContext()).apply {
                text = "🎯 Tir — réussites par passage"
                setTextColor(android.graphics.Color.WHITE)
                textSize = 14f
                setPadding(8, 16, 8, 8)
            }
            inner.addView(titreTir)

            val graphTir = fr.iutvannes.dual.model.components.GraphView(requireContext()).apply {
                layoutParams = android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT, 300
                )
                data = donneesTir.series
                yMin = 0
                yMax = 5
                lineColor = androidx.core.content.ContextCompat.getColor(requireContext(), R.color.vert)
                labelColor = android.graphics.Color.WHITE
                gridColor  = android.graphics.Color.argb(80, 255, 255, 255)
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
            }
            inner.addView(graphTir)

            if (donneesTir.series.isEmpty()) {
                val tv = TextView(requireContext()).apply {
                    text = "Aucune donnée de tir"
                    setTextColor(android.graphics.Color.argb(150, 255, 255, 255))
                    textSize = 13f
                    setPadding(8, 4, 8, 8)
                }
                inner.addView(tv)
            }

            // Séparateur
            val sep = View(requireContext()).apply {
                layoutParams = android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT, 1
                ).also { it.setMargins(0, 16, 0, 16) }
                setBackgroundColor(android.graphics.Color.argb(60, 255, 255, 255))
            }
            inner.addView(sep)

            // --- Tableau COURSE ---
            val titreCourse = TextView(requireContext()).apply {
                text = "🏃 Course — temps par tour"
                setTextColor(android.graphics.Color.WHITE)
                textSize = 14f
                setPadding(8, 8, 8, 8)
            }
            inner.addView(titreCourse)

            if (donneesCourse.tours.isEmpty()) {
                val tv = TextView(requireContext()).apply {
                    text = "Aucune donnée de course"
                    setTextColor(android.graphics.Color.argb(150, 255, 255, 255))
                    textSize = 13f
                    setPadding(8, 4, 8, 8)
                }
                inner.addView(tv)
            } else {
                // En-tête du tableau
                val entete = android.widget.TableLayout(requireContext()).apply {
                    layoutParams = android.widget.LinearLayout.LayoutParams(
                        android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    setColumnStretchable(0, true)
                    setColumnStretchable(1, true)
                }

                fun makeCell(texte: String, bold: Boolean = false, couleur: Int = android.graphics.Color.WHITE): TextView {
                    return TextView(requireContext()).apply {
                        text = texte
                        setTextColor(couleur)
                        textSize = 13f
                        if (bold) setTypeface(null, android.graphics.Typeface.BOLD)
                        setPadding(12, 8, 12, 8)
                        gravity = android.view.Gravity.CENTER
                    }
                }

                // Ligne d'en-tête
                val rowEntete = android.widget.TableRow(requireContext())
                rowEntete.addView(makeCell("Tour", bold = true, couleur = android.graphics.Color.argb(180, 255, 255, 255)))
                rowEntete.addView(makeCell("Temps", bold = true, couleur = android.graphics.Color.argb(180, 255, 255, 255)))
                entete.addView(rowEntete)

                // Séparateur sous l'en-tête
                val sepEntete = View(requireContext()).apply {
                    layoutParams = android.widget.TableLayout.LayoutParams(
                        android.widget.TableLayout.LayoutParams.MATCH_PARENT, 1
                    )
                    setBackgroundColor(android.graphics.Color.argb(80, 255, 255, 255))
                }
                entete.addView(sepEntete)

                // Calcul du meilleur tour pour le mettre en valeur
                val meilleurTemps = donneesCourse.tours.minOf { it.second }

                // Lignes de données
                donneesCourse.tours.forEachIndexed { index, (label, secondes) ->
                    val min = (secondes / 60).toInt()
                    val sec = (secondes % 60).toInt()
                    val ms = ((secondes % 1) * 100).toInt()
                    val tempsFormate = if (min > 0) "%d'%02d\"%02d".format(min, sec, ms)
                    else "%d\"%02d".format(sec, ms)

                    val isMeilleur = secondes == meilleurTemps
                    val couleurLigne = if (isMeilleur)
                        android.graphics.Color.parseColor("#2ECC71")
                    else
                        android.graphics.Color.WHITE

                    val row = android.widget.TableRow(requireContext()).apply {
                        if (index % 2 == 0) setBackgroundColor(android.graphics.Color.argb(20, 255, 255, 255))
                    }
                    row.addView(makeCell(label, couleur = couleurLigne))
                    row.addView(makeCell(if (isMeilleur) "⭐ $tempsFormate" else tempsFormate, couleur = couleurLigne))
                    entete.addView(row)
                }

                inner.addView(entete)
            }

            container.addView(scrollView)

            view.findViewById<TextView>(R.id.dialog_cancel).apply {
                text = "Fermer"
                setOnClickListener { dialog.dismiss() }
            }

            dialog.show()
        }
    }
}

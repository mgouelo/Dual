package fr.iutvannes.dual.controller.fragments

import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.util.Log
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import fr.iutvannes.dual.model.utils.DatabaseProvider

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
            repeatOnLifecycle(Lifecycle.State.STARTED) {
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
        }

        val qrCode = view.findViewById<ImageView>(R.id.qrCodeView)
        qrCode.setBackgroundColor(Color.WHITE)
        val sessionUrl = view.findViewById<TextView>(R.id.textUrl)

        val cardInfoSeance = view.findViewById<View>(R.id.cardInfoSeance)
        val tvSeanceType = view.findViewById<TextView>(R.id.tv_seance_type)
        val tvSeanceClasse = view.findViewById<TextView>(R.id.tv_seance_classe)

        // Opening a coroutine in the IO thread to generate the QR code
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                sessionViewModel.url.collect { url ->
                    if (url != null) {
                        qrCode.visibility = View.VISIBLE
                        sessionUrl.visibility = View.VISIBLE
                        nbResultat.visibility = View.VISIBLE
                        sessionUrl.text = url
                        qrCode.setImageBitmap(genererQRCode(url))
                    }
                }
            }
        }



        //Opening a coroutine in the I/O thread to handle the session start button
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                sessionViewModel.running.collect { running ->
                    if (running) {
                        sessionBtn.text = "Arrêter la séance"
                        val couleurBleu = ContextCompat.getColor(requireContext(), R.color.bleu)
                        sessionBtn.backgroundTintList =
                            android.content.res.ColorStateList.valueOf(couleurBleu)
                        qrCode.visibility = View.VISIBLE
                        layoutUrl.visibility = View.VISIBLE
                        cardResultats.visibility = View.VISIBLE

                        //Récupếration des infos du ViewModel
                        val classe = sessionViewModel.nomClasse.value
                        val type = sessionViewModel.typeSeance.value
                        tvSeanceType.text = type.uppercase()
                        tvSeanceClasse.text = classe
                        cardInfoSeance.visibility = View.VISIBLE

                    } else {
                        sessionBtn.text = "Lancer une séance"
                        val couleurBleu = ContextCompat.getColor(requireContext(), R.color.bleu)
                        sessionBtn.backgroundTintList =
                            android.content.res.ColorStateList.valueOf(couleurBleu)
                        qrCode.visibility = View.GONE
                        layoutUrl.visibility = View.GONE
                        cardResultats.visibility = View.GONE
                        cardInfoSeance.visibility = View.GONE
                    }
                }
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
     * Launches a new session.
     *
     * @param classe The class of the session.
     * @param type The type of the session.
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

    /**
     * Méthod to display the dialog to choose the class.
     *
     * @param classes : list of classes
     */
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

        val icons = listOf("🏫", "📚", "🎯", "⭐", "🏆", "📋", "🎨", "🔬", "🌍", "🎵",
            "🏅", "💡", "🔭", "📐", "🖊️", "🧪", "🗺️", "🎭", "📖", "🏛️")

        classes.forEachIndexed { index, nomClasse ->
            val item = layoutInflater.inflate(R.layout.item_dialog_choix, container, false)
            item.findViewById<TextView>(R.id.item_icon).text = icons[index % icons.size]
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
     * Méthod to display the dialog to choose the type of session.
     *
     * @param classeChoisie : class selected
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

    /**
     * Méthod to display the dialog to end the session.
     *
     * @param nbResultats : number of results
     * @param nbEleves : number of students
     */
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

    /**
     * Méthod to display the dialog to display the results in real time.
     *
     * @param type : type of session
     * @param classe : class of session
     * @param resultats : list of results
     */
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
                    when (type) {
                        "Entraînement" -> item.setOnClickListener {
                            afficherGraphsEleve(ligne.idEleve, ligne.prenom, ligne.nom)
                        }
                        "Épreuve Finale" -> item.setOnClickListener {
                            afficherDetailEpeuveFinale(ligne.idEleve, ligne.prenom, ligne.nom)
                        }
                        else -> item.isClickable = false
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

    /**
     * Méthod to display the dialog to display the graphs.
     *
     * @param idEleve : id of the student
     * @param prenom : first name of the student
     * @param nom : last name of the student
     */
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

    /**
     * Méthod to display the dialog to display the graphs.
     *
     * @param idEleve : id of the student
     * @param prenom : first name of the student
     * @param nom : last name of the student
     */
    private fun afficherDetailEpeuveFinale(idEleve: Int, prenom: String, nom: String) {
        viewLifecycleOwner.lifecycleScope.launch {

            val idSeance = KtorServer.idSeanceActuelle

            data class DetailEpreuve(
                val eleve: fr.iutvannes.dual.model.persistence.Eleve?,
                val resultat: fr.iutvannes.dual.model.persistence.Resultat?
            )

            val detail = withContext(Dispatchers.IO) {
                val eleve = DatabaseProvider.db.EleveDao().getEleveById(idEleve)
                val resultat = DatabaseProvider.db.resultatDao()
                    .getResultatByEleveEtSeance(idEleve, idSeance)
                DetailEpreuve(eleve, resultat)
            }

            val dialog = android.app.Dialog(requireContext())
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

            dialogView.findViewById<TextView>(R.id.dialog_subtitle).text = "$prenom ${nom.uppercase()}"

            val container = dialogView.findViewById<android.widget.LinearLayout>(R.id.dialog_choices_container)
            val scrollView = android.widget.ScrollView(requireContext())
            val inner = android.widget.LinearLayout(requireContext()).apply {
                orientation = android.widget.LinearLayout.VERTICAL
                setPadding(0, 8, 0, 8)
            }
            scrollView.addView(inner)

            val eleve = detail.eleve
            val res = detail.resultat

            if (eleve == null || res == null) {
                inner.addView(TextView(requireContext()).apply {
                    text = "Résultat non encore enregistré"
                    setTextColor(android.graphics.Color.argb(150, 255, 255, 255))
                    textSize = 14f
                    setPadding(8, 16, 8, 8)
                })
            } else {
                val is4eme = res.ecart_max_course == 0 && res.nbTours == 6
                val vmaRef = eleve.vma ?: 0f

                // Tableau des données
                val table = android.widget.TableLayout(requireContext()).apply {
                    layoutParams = android.widget.LinearLayout.LayoutParams(
                        android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    setColumnStretchable(0, true)
                    setColumnStretchable(1, true)
                }

                /**
                 * Méthod to add a row to the table.
                 *
                 * @param label : label of the row
                 * @param valeur : value of the row
                 * @param couleur : color of the row
                 */
                fun makeRow(label: String, valeur: String, couleur: Int = android.graphics.Color.WHITE) {
                    val row = android.widget.TableRow(requireContext())
                    val tvLabel = TextView(requireContext()).apply {
                        text = label
                        setTextColor(android.graphics.Color.argb(180, 255, 255, 255))
                        textSize = 13f
                        setPadding(12, 10, 12, 10)
                    }
                    val tvVal = TextView(requireContext()).apply {
                        text = valeur
                        setTextColor(couleur)
                        textSize = 13f
                        setTypeface(null, android.graphics.Typeface.BOLD)
                        setPadding(12, 10, 12, 10)
                        gravity = android.view.Gravity.END
                    }
                    row.addView(tvLabel)
                    row.addView(tvVal)
                    table.addView(row)

                    // Séparateur fin
                    table.addView(View(requireContext()).apply {
                        layoutParams = android.widget.TableLayout.LayoutParams(
                            android.widget.TableLayout.LayoutParams.MATCH_PARENT, 1
                        )
                        setBackgroundColor(android.graphics.Color.argb(30, 255, 255, 255))
                    })
                }

                // VMA de référence
                makeRow("VMA de référence", if (vmaRef > 0) "%.1f km/h".format(vmaRef) else "-",
                    android.graphics.Color.parseColor("#9BA3E8"))

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
                     * Méthod to convert dp to px.
                     *
                     * @param v : value in dp
                     * @return value in px
                     */
                    fun dp(v: Float) = (v * resources.displayMetrics.density).toInt()

                    /**
                     * Méthod to convert hex to color.
                     *
                     * @param hex : value in hex
                     * @return color
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
                     * Méthod to add a card to the metrics row.
                     *
                     * @param label : label of the card
                     * @param value : value of the card
                     * @param max : max value of the card
                     * @param sub : sub value of the card
                     * @return the card
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
                     * Méthod to add a table to the inner container.
                     *
                     * @param titre : title of the table
                     * @param lignes : list of lines of the table
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
                                android.widget.LinearLayout.LayoutParams.MATCH_PARENT, 1)
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
                                layoutParams = android.widget.LinearLayout.LayoutParams(0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
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
                                        android.widget.LinearLayout.LayoutParams.MATCH_PARENT, 1)
                                    setBackgroundColor(divider)
                                })
                            }
                        }
                        inner.addView(card)
                    }

                    // ── Tableau segments ──────────────────────────────────────────────────────
                    /**
                     * Méthod to convert seconds to minutes and seconds.
                     *
                     * @param sec : seconds
                     * @return minutes and seconds as a string
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
            }

            container.addView(scrollView)
            dialogView.findViewById<TextView>(R.id.dialog_cancel).apply {
                text = "Fermer"
                setOnClickListener { dialog.dismiss() }
            }

            dialog.show()
        }
    }

    /**
     * Méthod to create a title section.
     *
     * @param texte : text of the section
     * @return the section
     */
    private fun creerTitreSection(texte: String) = TextView(requireContext()).apply {
        text = texte
        setTextColor(android.graphics.Color.WHITE)
        textSize = 14f
        setPadding(8, 16, 8, 8)
    }
}

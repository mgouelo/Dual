package fr.iutvannes.dual.controller.fragments

import android.os.Bundle
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ResultatsFragment : Fragment(R.layout.fragment_resultats) {

    private var classeSelectionnee: String? = null
    private var typeSelectionne: String? = null
    private var seanceAffichee: Seance? = null

    // Éléments de vue UI
    private lateinit var tvVide: TextView
    private lateinit var recycler: RecyclerView
    private lateinit var btnBack: ImageButton
    private lateinit var tvClasse: TextView
    private lateinit var tvType: TextView
    private lateinit var tvTitre: TextView

    // Callback pour le bouton retour physique
    private lateinit var backPressedCallback: OnBackPressedCallback

    // Variable pour stocker le CSV généré avant la sauvegarde
    private var contenuCsvEnAttente: String? = null

    // Lanceur natif Android pour créer/sauvegarder un fichier
    private val exportLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null && contenuCsvEnAttente != null) {
            try {
                requireContext().contentResolver.openOutputStream(uri)?.bufferedWriter()?.use {
                    it.write(contenuCsvEnAttente)
                }
                Toast.makeText(requireContext(), "Fichier sauvegardé avec succès !", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Erreur lors de la sauvegarde", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialisation des vues
        val cardClasse = view.findViewById<MaterialCardView>(R.id.card_filtre_classe)
        val cardType   = view.findViewById<MaterialCardView>(R.id.card_filtre_type)
        tvClasse   = view.findViewById(R.id.tv_classe_selectionnee)
        tvType     = view.findViewById(R.id.tv_type_selectionne)
        tvVide     = view.findViewById(R.id.tv_resultats_vide)
        recycler   = view.findViewById(R.id.recycler_resultats)
        btnBack    = view.findViewById(R.id.btn_back_resultats)
        tvTitre    = view.findViewById(R.id.tv_titre_resultats)

        // Initialisation du callback de retour
        backPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                gererActionRetour()
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, backPressedCallback)

        // Clic sur le bouton retour visuel (flèche)
        btnBack.setOnClickListener {
            gererActionRetour()
        }

        // Clic sur la card Classe
        cardClasse.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                val classes = withContext(Dispatchers.IO) {
                    DatabaseProvider.db.classeDao().getAllNames()
                }
                val items = classes.toTypedArray()
                android.app.AlertDialog.Builder(requireContext())
                    .setTitle("Sélectionner une classe")
                    .setItems(items) { _, i ->
                        classeSelectionnee = items[i]
                        tvClasse.text = items[i]
                        chargerSeances()
                    }
                    .show()
            }
        }

        // Clic sur la card Type
        cardType.setOnClickListener {
            val types = arrayOf("Test VMA", "Entraînement", "Épreuve Finale")
            android.app.AlertDialog.Builder(requireContext())
                .setTitle("Type de séance")
                .setItems(types) { _, i ->
                    typeSelectionne = types[i]
                    tvType.text = types[i]
                    chargerSeances()
                }
                .show()
        }
    }

    private fun gererActionRetour() {
        if (seanceAffichee != null) {
            seanceAffichee = null
            chargerSeances()
        } else if (classeSelectionnee != null || typeSelectionne != null) {
            classeSelectionnee = null
            typeSelectionne = null
            tvClasse.text = "Sélectionner ▾"
            tvType.text = "Sélectionner ▾"
            tvTitre.text = "Résultats"
            btnBack.visibility = View.GONE

            tvVide.visibility = View.VISIBLE
            tvVide.text = "Sélectionnez une classe et un type de séance"
            recycler.visibility = View.GONE
        } else {
            backPressedCallback.isEnabled = false
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun chargerSeances() {
        val classe = classeSelectionnee ?: return
        val type   = typeSelectionne   ?: return

        backPressedCallback.isEnabled = true
        btnBack.visibility = View.VISIBLE
        tvTitre.text = "Séances ($classe)"

        viewLifecycleOwner.lifecycleScope.launch {

            val seances = withContext(Dispatchers.IO) {
                DatabaseProvider.db.seanceDao().getSeancesByClasseEtType(classe, type)
            }

            if (seances.isEmpty()) {
                tvVide.visibility = View.VISIBLE
                tvVide.text = "Aucune séance trouvée pour cette sélection"
                recycler.visibility = View.GONE
                return@launch
            }

            tvVide.visibility = View.GONE
            recycler.visibility = View.VISIBLE

            recycler.adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
                inner class VH(v: View) : RecyclerView.ViewHolder(v)

                override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int) =
                    VH(layoutInflater.inflate(R.layout.item_seance, parent, false))

                override fun getItemCount() = seances.size

                override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
                    val seance = seances[position]
                    val v = holder.itemView

                    v.findViewById<TextView>(R.id.tv_seance_rang).text = "${position + 1}"
                    v.findViewById<TextView>(R.id.tv_seance_titre).text =
                        "Séance du ${seance.date.substringBefore(" ")}"
                    v.findViewById<TextView>(R.id.tv_seance_detail).text =
                        "${seance.type} — ${seance.classe}"

                    v.setOnClickListener {
                        afficherResultatsSeance(seance)
                    }

                    // Bouton Excel (Génère et sauvegarde nativement)
                    v.findViewById<MaterialCardView>(R.id.btn_seance_excel).setOnClickListener {
                        genererEtExporterCSV(seance)
                    }

                    // Bouton Supprimer
                    v.findViewById<MaterialCardView>(R.id.btn_seance_delete).setOnClickListener {
                        android.app.AlertDialog.Builder(requireContext())
                            .setTitle("Supprimer la séance ?")
                            .setMessage("Tous les résultats liés seront perdus.")
                            .setPositiveButton("Supprimer") { _, _ ->
                                viewLifecycleOwner.lifecycleScope.launch {
                                    withContext(Dispatchers.IO) {
                                        DatabaseProvider.db.seanceDao().delete(seance.id_seance)
                                        DatabaseProvider.db.resultatDao().deleteBySeance(seance.id_seance)
                                    }
                                    chargerSeances()
                                }
                            }
                            .setNegativeButton("Annuler", null)
                            .show()
                    }
                }
            }
        }
    }

    private fun afficherResultatsSeance(seance: Seance) {
        val type = typeSelectionne ?: return
        seanceAffichee = seance
        tvTitre.text = "Le ${seance.date.substringBefore(" ")}"

        viewLifecycleOwner.lifecycleScope.launch {
            data class LigneResultat(val eleve: Eleve, val resultat: Resultat)

            val lignes = withContext(Dispatchers.IO) {
                DatabaseProvider.db.resultatDao()
                    .getBySeance(seance.id_seance)
                    .mapNotNull { res ->
                        val eleve = DatabaseProvider.db.EleveDao()
                            .getEleveById(res.id_eleve) ?: return@mapNotNull null
                        LigneResultat(eleve, res)
                    }
                    .sortedByDescending { it.resultat.note_finale }
            }

            if (lignes.isEmpty()) {
                tvVide.visibility = View.VISIBLE
                tvVide.text = "Aucun résultat pour cette séance"
                recycler.visibility = View.GONE
                return@launch
            }

            tvVide.visibility = View.GONE
            recycler.visibility = View.VISIBLE

            recycler.adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
                inner class VH(v: View) : RecyclerView.ViewHolder(v)

                override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int) =
                    VH(layoutInflater.inflate(R.layout.item_resultat, parent, false))

                override fun getItemCount() = lignes.size

                override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
                    val ligne = lignes[position]
                    val v = holder.itemView

                    v.findViewById<TextView>(R.id.tv_rang).text = "${position + 1}"
                    v.findViewById<TextView>(R.id.tv_nom_resultat).text =
                        "${ligne.eleve.prenom} ${ligne.eleve.nom.uppercase()}"

                    val detail = when (type) {
                        "Test VMA" -> {
                            val vma = ligne.eleve.vma?.let { "%.1f km/h".format(it) } ?: "-"
                            "VMA : $vma"
                        }
                        "Épreuve Finale" -> {
                            val is4eme = ligne.resultat.ecart_max_course == 0 &&
                                    ligne.resultat.nbTours == 6
                            if (is4eme) {
                                val pct = if ((ligne.eleve.vma ?: 0f) > 0f)
                                    "%.0f%%".format(
                                        (ligne.resultat.temp_course / ligne.eleve.vma!!) * 100
                                    ) else "-"
                                "Cibles : ${ligne.resultat.cibles_touchees}/10  •  VMA : $pct"
                            } else {
                                "Tours : ${ligne.resultat.nbTours}  •  Cibles : ${ligne.resultat.cibles_touchees}"
                            }
                        }
                        else -> "Cibles : ${ligne.resultat.cibles_touchees}"
                    }

                    v.findViewById<TextView>(R.id.tv_detail_resultat).text = detail

                    val noteStr = when (type) {
                        "Test VMA" -> ligne.eleve.vma?.let { "%.1f".format(it) } ?: "-"
                        else -> "%.2f".format(ligne.resultat.note_finale)
                    }
                    v.findViewById<TextView>(R.id.tv_note).text = noteStr
                    v.isClickable = false
                }
            }
        }
    }

    /**
     * Génère le CSV exactement comme Ktor, mais le lance directement
     * dans le gestionnaire de fichiers Android du téléphone
     */
    private fun genererEtExporterCSV(seance: Seance) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val resultats = withContext(Dispatchers.IO) {
                    DatabaseProvider.db.resultatDao().getBySeance(seance.id_seance)
                }

                val csv = java.lang.StringBuilder()

                when (seance.type) {
                    "Test VMA" -> {
                        csv.append("Test VMA - ${seance.classe} - ${seance.date}\n\n")
                        csv.append("Nom;Prénom;VMA (km/h)\n")
                        resultats.forEach { res ->
                            val eleve = withContext(Dispatchers.IO) {
                                DatabaseProvider.db.EleveDao().getEleveById(res.id_eleve)
                            }
                            if (eleve != null) {
                                val vma = eleve.vma?.let { String.format("%.1f", it) } ?: "-"
                                csv.append("${eleve.nom.uppercase()};${eleve.prenom};$vma\n")
                            }
                        }
                    }
                    "Épreuve Finale" -> {
                        val is4eme = resultats.all { it.ecart_max_course == 0 && it.nbTours == 6 }
                        if (is4eme) {
                            csv.append("Épreuve Finale 4ème - ${seance.classe} - ${seance.date}\n\n")
                            csv.append("Nom;Prénom;VMA ref (km/h);Vitesse épreuve (km/h);% VMA;Cibles touchées (/10);Note /12\n")
                            resultats.forEach { res ->
                                val eleve = withContext(Dispatchers.IO) {
                                    DatabaseProvider.db.EleveDao().getEleveById(res.id_eleve)
                                }
                                if (eleve != null) {
                                    val vmaRef = eleve.vma?.let { String.format("%.1f", it) } ?: "-"
                                    val vitesse = String.format("%.2f", res.temp_course)
                                    val pctVma = if ((eleve.vma ?: 0f) > 0f)
                                        String.format("%.0f%%", (res.temp_course / eleve.vma!!) * 100)
                                    else "-"
                                    val note = String.format("%.2f", res.note_finale)
                                    csv.append("${eleve.nom.uppercase()};${eleve.prenom};$vmaRef;$vitesse;$pctVma;${res.cibles_touchees};$note\n")
                                }
                            }
                        } else {
                            csv.append("Épreuve Finale 6ème - ${seance.classe} - ${seance.date}\n\n")
                            csv.append("Nom;Prénom;VMA (km/h);Nb tours;Écart max (s);Cibles touchées;Note /15\n")
                            resultats.forEach { res ->
                                val eleve = withContext(Dispatchers.IO) {
                                    DatabaseProvider.db.EleveDao().getEleveById(res.id_eleve)
                                }
                                if (eleve != null) {
                                    val vma = eleve.vma?.let { String.format("%.1f", it) } ?: "-"
                                    val note = String.format("%.2f", res.note_finale)
                                    csv.append("${eleve.nom.uppercase()};${eleve.prenom};$vma;${res.nbTours};${res.ecart_max_course};${res.cibles_touchees};$note\n")
                                }
                            }
                        }
                    }
                    else -> {
                        csv.append("Entraînement - ${seance.classe} - ${seance.date}\n\n")
                        csv.append("Nom;Prénom;Cibles touchées;VMA (km/h)\n")
                        resultats.forEach { res ->
                            val eleve = withContext(Dispatchers.IO) {
                                DatabaseProvider.db.EleveDao().getEleveById(res.id_eleve)
                            }
                            if (eleve != null) {
                                val vma = eleve.vma?.let { String.format("%.1f", it) } ?: "-"
                                csv.append("${eleve.nom.uppercase()};${eleve.prenom};${res.cibles_touchees};$vma\n")
                            }
                        }
                    }
                }

                // Préparation du nom et lancement de la sauvegarde
                val dateClean = seance.date.replace("/", "-").replace(":", "h").replace(" ", "_")
                val nomFichier = "Bilan_${seance.type}_${seance.classe}_$dateClean.csv"

                contenuCsvEnAttente = csv.toString()
                exportLauncher.launch(nomFichier)

            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Erreur génération CSV", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
package fr.iutvannes.dual.ui.fragments

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.textfield.TextInputEditText
import fr.iutvannes.dual.R
import fr.iutvannes.dual.model.persistence.Eleve
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.core.content.ContextCompat

class EleveProfilFragment : Fragment() {

    private var eleveId: Int = -1
    private lateinit var currentEleve: Eleve

    // Vues
    private lateinit var tvNomPrenom: TextView
    private lateinit var tvClasse: TextView
    private lateinit var etVma: TextInputEditText
    private lateinit var tvParcours: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // On récupère l'ID passé depuis la liste
        arguments?.let {
            eleveId = it.getInt("eleve_id", -1)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.profil_eleve, container, false)

        // Initialisation des vues
        tvNomPrenom = view.findViewById(R.id.tv_profil_nom_prenom)
        tvClasse = view.findViewById(R.id.tv_profil_classe)
        etVma = view.findViewById(R.id.et_profil_vma)
        tvParcours = view.findViewById(R.id.tv_profil_parcours)

        val btnBack = view.findViewById<ImageButton>(R.id.btn_back_profil)
        val btnSave = view.findViewById<Button>(R.id.btn_save_profil)

        // Action bouton retour
        btnBack.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        // Chargement des données
        if (eleveId != -1) {
            chargerEleve()
        }

        // Action quand on tape au clavier dans la case VMA
        etVma.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                mettreAJourAffichageParcours(s.toString())
            }
        })

        // Action bouton Enregistrer
        btnSave.setOnClickListener {
            sauvegarderProfil()
        }

        return view
    }

    private fun chargerEleve() {
        val dao = DatabaseProvider.db.EleveDao()

        lifecycleScope.launch(Dispatchers.IO) {
            val eleve = dao.getEleveById(eleveId)

            withContext(Dispatchers.Main) {
                if (eleve != null) {
                    currentEleve = eleve
                    // remplissage de l'UI
                    tvNomPrenom.text = "${eleve.nom} ${eleve.prenom}"
                    tvClasse.text = "Classe : ${eleve.classe}"

                    // Si l'élève a une VMA on l'affiche, sinon champ vide
                    if (eleve.vma != null) {
                        etVma.setText(eleve.vma.toString())
                        tvParcours.text = eleve.couleur_parcours ?: "À déterminer"
                    } else {
                        etVma.setText("")
                        tvParcours.text = "À déterminer"
                    }
                    couleurParcoursTexte(tvParcours.text.toString())
                }
            }
        }
    }

    private fun mettreAJourAffichageParcours(saisie: String) {
        val vma = saisie.toFloatOrNull()
        if (vma != null) {
            val nouveauParcours = determinerParcours(vma) // on récupère le bon parcours en fonction de la nouvelle VMA
            tvParcours.text = nouveauParcours
        } else {
            tvParcours.text = "À déterminer"
        }
        couleurParcoursTexte(tvParcours.text.toString())
    }

    private fun sauvegarderProfil() {
        val nouvelleVmaString = etVma.text.toString()
        val nouvelleVma = nouvelleVmaString.toFloatOrNull()

        // gestion de l'interface
        if (nouvelleVma == null) {
            // Le champ est vide, on met à jour l'affichage
            if (::currentEleve.isInitialized && currentEleve.vma != null) {
                etVma.hint = "Ancienne valeur : ${currentEleve.vma}"
                tvParcours.text = currentEleve.couleur_parcours ?: "À déterminer"
            } else {
                etVma.hint = "VMA (km/h)"
                tvParcours.text = "À déterminer"
            }

            Toast.makeText(requireContext(), "Aucune modification enregistrée", Toast.LENGTH_SHORT).show()
            requireActivity().onBackPressedDispatcher.onBackPressed() // On retourne à la liste d'élève

            return
        }

        // sauvegarde en base
        // Si le code arrive ici, c'est que nouvelleVma n'est pas null.
        val dao = DatabaseProvider.db.EleveDao()

        lifecycleScope.launch(Dispatchers.IO) {
            val nouveauParcours = determinerParcours(nouvelleVma)

            // Sauvegarde des données
            dao.updateVma(eleveId, nouvelleVma)
            dao.updateParcours(eleveId, nouveauParcours)

            // retour sur l'UI pour faire un feedback utilisateur
            withContext(Dispatchers.Main) {
                Toast.makeText(requireContext(), "VMA et parcours mis à jour", Toast.LENGTH_SHORT).show()
                requireActivity().onBackPressedDispatcher.onBackPressed() // Retour à la liste
            }
        }
    }

    /**
     * Détermine le parcours de l'élève en fonction de sa VMA (float)
     */
    fun determinerParcours(vma: Float): String {
        return when {
            vma < 10.5f -> "Coupelles jaunes"
            vma < 11.5f -> "Plots verts"
            vma < 12.5f -> "Coupelles bleues"
            vma < 13.5f -> "Plots bleus"
            vma < 14.5f -> "Coupelles rouges"
            vma <= 15.0f -> "Plots rouges"
            else -> "Grand tour"
        }
    }

    /**
     * Change la couleur du texte en fonction de la couleur du parcours (Utile que pour l'UI/UX)
     */
    private fun couleurParcoursTexte(nomParcours: String) {
        val couleurId = when (nomParcours) {
            "Coupelles jaunes" -> R.color.jaune
            "Plots verts" -> R.color.vert
            "Coupelles bleues", "Plots bleus" -> R.color.bleu
            "Coupelles rouges", "Plots rouges" -> R.color.rouge
            "Grand tour" -> R.color.noir
            else -> R.color.gris
        }

        // applique la couleur au textview
        tvParcours.setTextColor(ContextCompat.getColor(requireContext(), couleurId))
    }

    companion object {
        /**
         * Crée une nouvelle instance du fragment en lui passant l'ID de l'élève
         */
        fun newInstance(eleveId: Int): EleveProfilFragment {
            val fragment = EleveProfilFragment()
            val args = Bundle()
            args.putInt("eleve_id", eleveId) // on ajoute l'id en paramètre avec le fragment
            fragment.arguments = args
            return fragment
        }
    }
}

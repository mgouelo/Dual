package fr.iutvannes.dual.controller.fragments

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import fr.iutvannes.dual.R
import fr.iutvannes.dual.model.database.AppDatabase
import fr.iutvannes.dual.model.persistence.Classe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Fragment d'ajout de classe
 */
class AjoutClasseFragment: Fragment(R.layout.fragment_ajout_classe) {

    private var oldClasseNom: String? = null // Ancien nom de la classe si on est en mode édition sinon, en mode création c'est à null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // On récupère les arguments passés via newInstanceForEdit
        oldClasseNom = arguments?.getString("oldName")
    }

    /**
     * A la création de la vue...
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Titre, bouton de retour, bouton de validation
        val title = view.findViewById<TextView>(R.id.page_title)
        val buttonBack = view.findViewById<ImageButton>(R.id.arrow_back_button)
        val buttonValider = view.findViewById<Button>(R.id.btn_valider)

        // Groupe de bouton radio pour la sélection du type de nommage
        val radioGroup = view.findViewById<RadioGroup>(R.id.radio_mode_nommage)

        // Les toggles sont les sélecteurs à choix prédéfini comme le niveau ou la lettre
        val toggleNiveau = view.findViewById<com.google.android.material.button.MaterialButtonToggleGroup>(R.id.toggle_group_niveau)
        val toggleLettre = view.findViewById<com.google.android.material.button.MaterialButtonToggleGroup>(R.id.toggle_group_lettre)

        // Input dans le cas où le nom de la classe est un mot
        val inputLibre = view.findViewById<EditText>(R.id.input_nom_libre)

        // Contient la logique de nommage par lettre
        val containerNommageLettre = view.findViewById<View>(R.id.container_nom_lettre)

        // Contient la logique de nommage par mot
        val containerNommageLibre = view.findViewById<View>(R.id.container_nom_libre)


        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            if (checkedId == R.id.radio_nommage_lettre) {
                // Mode Standard : On affiche les boutons, on cache le texte
                containerNommageLettre.visibility = View.VISIBLE
                containerNommageLibre.visibility = View.GONE
            } else {
                // Mode Libre : On cache les boutons, on affiche le texte
                containerNommageLettre.visibility = View.GONE
                containerNommageLibre.visibility = View.VISIBLE
            }
        }

        // On modifie une classe existante (ex: "6A" ou "4Lisbonne") --> Mode édition
        if (oldClasseNom != null) {

            // On change le titre et le bouton de 'valider' à 'modifier' pour moins d'ambiguité
            title.text = "Modifier la classe " + oldClasseNom!!.formatAffichageClasse()
            buttonValider.text = "Modifier"

            // Nouvelle logique de découpage pour le format brut (ex: "6A" ou "6Lisbonne")
            if (oldClasseNom!!.length >= 2 && oldClasseNom!!.first().isDigit()) {
                val premierCaractere = oldClasseNom!!.first() // ex: '6'
                val niveauStr = "${premierCaractere}e"        // ex: "6e" (Pour matcher avec le texte des boutons)
                val suiteStr = oldClasseNom!!.substring(1).trim() // ex: "A" ou "Lisbonne"

                // On préselectionne le niveau pour plus de cohérence + meilleur UX
                selectionnerBoutonParTexte(toggleNiveau, niveauStr)

                // On choisit la méthode de nommage utilisée pour l'ancien nom
                if (suiteStr.length == 1 && suiteStr.first().isLetter()) {
                    // C'est une simple lettre (ex: "A")
                    radioGroup.check(R.id.radio_nommage_lettre)
                    selectionnerBoutonParTexte(toggleLettre, suiteStr)
                } else {
                    // C'est un mot libre (ex: "Lisbonne")
                    radioGroup.check(R.id.radio_nommage_libre)
                    inputLibre.setText(suiteStr)
                }
            }
        }

        buttonBack.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        // 3. Logique du bouton de validation (vérification, validation et écriture en base)
        buttonValider.setOnClickListener {
            val nomFinal: String
            val idNiveau = toggleNiveau.checkedButtonId

            // Aucun niveau n'a été sélectionné
            if (idNiveau == -1) {
                Toast.makeText(requireContext(), "Veuillez sélectionner un niveau", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val niveau = view.findViewById<Button>(idNiveau).text.toString() // ex: "6e"

            // On vérifie quel mode est choisi
            if (radioGroup.checkedRadioButtonId == R.id.radio_nommage_lettre) {

                val idLettre = toggleLettre.checkedButtonId
                if (idLettre == -1) { // aucune lettre n'est sélectionnée
                    Toast.makeText(requireContext(), "Veuillez sélectionner une lettre", Toast.LENGTH_LONG).show()
                    return@setOnClickListener
                }

                val lettre = view.findViewById<Button>(idLettre).text.toString()
                nomFinal = "${niveau[0]}$lettre" // Ex: "6A"

            } else { // sinon c'est en nommage libre
                val motLibre = inputLibre.text.toString().trim()

                if (motLibre.isEmpty()) {
                    Toast.makeText(requireContext(), "Veuillez entrer un nom de classe", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                nomFinal = "${niveau[0]}$motLibre" // ex: "4Lisbonne"
            }

            // sauvegarde en bdd
            sauvegarderClasse(nomFinal)
        }
    }

    companion object {
        // Pour une création simple (pas d'arguments)
        fun newInstance(): AjoutClasseFragment {
            return AjoutClasseFragment()
        }

        // Pour une édition (on passe le nom actuel de la classe que l'on veut modifier)
        fun newInstanceForEdit(nomActuel: String): AjoutClasseFragment {
            val fragment = AjoutClasseFragment()
            val args = Bundle()
            args.putString("oldName", nomActuel)
            fragment.arguments = args
            return fragment
        }
    }

    private fun insererEtQuitter(db: AppDatabase, nom: String) {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            db.classeDao().insert(Classe(nom = nom))
            withContext(Dispatchers.Main) {
                Toast.makeText(requireContext(), "Classe ${nom.formatAffichageClasse()} ajoutée", Toast.LENGTH_SHORT).show() // toast de retour utilisateur
                requireActivity().onBackPressedDispatcher.onBackPressed() // retour a l'écran précédent
            }
        }
    }

    private fun sauvegarderClasse(nouveauNom: String) {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val db = DatabaseProvider.db

            // Cas 1 : modification
            if (oldClasseNom != null) {
                if (oldClasseNom == nouveauNom) {
                    // L'utilisateur n'a rien changé
                    withContext(Dispatchers.Main) {
                        requireActivity().onBackPressedDispatcher.onBackPressed()
                    }
                    return@launch
                }

                // On vérifie si le nouveau nom est déjà pris par une autre classe pour éviter les doublons
                val existeDeja = db.classeDao().getClasseByName(nouveauNom) != null
                if (existeDeja) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "Ce nom de classe existe déjà", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                // Tout est ok on fait la mise à jour en base
                db.classeDao().updateNomClasse(oldClasseNom!!, nouveauNom)

                // On met à jour également la classe de chaque élève en base avec le nouveau nom
                db.EleveDao().updateClasseEleves(oldClasseNom!!, nouveauNom)

                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Classe modifiée", Toast.LENGTH_SHORT).show() // feedback utilisateur
                    requireActivity().onBackPressedDispatcher.onBackPressed()
                }

            } else {
                // Cas 2 : insertion
                val existe = db.classeDao().getClasseByName(nouveauNom) != null // retourne un boolean pour vérifier l'existence
                if (existe) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "La classe ${nouveauNom.formatAffichageClasse()} existe déjà", Toast.LENGTH_SHORT).show() // feedback utilisateur
                    }
                } else { // n'existe pas --> pas de doublon en vu
                    insererEtQuitter(db, nouveauNom)
                }
            }
        }
    }

    /**
     * Cherche un bouton dans le ToggleGroup par son texte et le coche.
     */
    private fun selectionnerBoutonParTexte(group: com.google.android.material.button.MaterialButtonToggleGroup, texte: String) {
        for (i in 0 until group.childCount) {
            val view = group.getChildAt(i)
            if (view is Button && view.text.toString() == texte) {
                group.check(view.id)
                return
            }
        }
    }

    /**
     * Transforme "6A" en "6e A" ou "3Lisbonne" en "3e Lisbonne"
     */
    fun String.formatAffichageClasse(): String {
        // si la chaîne est vide ou a 1 seul caractère, on la renvoie telle quelle
        if (this.length < 2) {
            return this
        }

        // On vérifie que le premier caractère est bien un chiffre
        val premierCaractere = this.first()
        if (!premierCaractere.isDigit()) {
            return this
        }

        // extrait le reste (nom de la classe ou lettre)
        val reste = this.substring(1).trim()

        // formatage
        return "${premierCaractere}e $reste"
    }
}
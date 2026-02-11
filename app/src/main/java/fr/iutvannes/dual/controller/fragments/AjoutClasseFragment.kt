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
import androidx.room.Room
import fr.iutvannes.dual.R
import fr.iutvannes.dual.controller.MainActivity
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

        // Groupe de bouton radio pour la sélection du type de nommage --> Soit le nom est une lettre (6e A) soit un mot (6e Ouessant)
        val radioGroup = view.findViewById<RadioGroup>(R.id.radio_mode_nommage)

        // Les toggles sont les sélecteurs à choix prédéfini comme le niveau ou la lettre
        val toggleNiveau = view.findViewById<com.google.android.material.button.MaterialButtonToggleGroup>(R.id.toggle_group_niveau)
        val toggleLettre = view.findViewById<com.google.android.material.button.MaterialButtonToggleGroup>(R.id.toggle_group_lettre)

        // Input dans le cas où le nom de la classe est un mot
        val inputLibre = view.findViewById<EditText>(R.id.input_nom_libre)

        // Contient la logique de nommage par lettre
        val containerNommageLettre = view.findViewById<View>(R.id.container_nom_lettre)

        // Contient la lgoique de nommage par mot
        val containerNommageLibre = view.findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.container_nom_libre)

        // Cas spécial : on modifie une classe existante
        if (oldClasseNom != null) { // Si l'ancien de nom de classe n'est pas null --> alors on modifie une classe existante

            // On change le titre et le bouton de 'valider' à 'modifier' pour moins d'ambiguité
            title.setText("Modifier la classe " + oldClasseNom)
            buttonValider.text = "Modifier"

            // On découpe le nom actuel de la classe en 2 parties séparées d'un espace :
            // 1ère partie : Niveau'e'
            // 2ème partie : Nom
            val parts = oldClasseNom!!.split(" ", limit = 2)

            if (parts.size >= 2) {
                val niveauStr = parts[0] // ex: "6e"
                val suiteStr = parts[1]  // ex: "A" ou "Ouessant"

                // On préselectionne le niveau pour plus de cohérance + meilleur UX
                selectionnerBoutonParTexte(toggleNiveau, niveauStr)

                // On choisit la méthode de nommage utilisée pour l'ancien nom.
                // Si suiteStr à une taille exactement de 1 alors c'est une simple lettre
                if (suiteStr.length == 1 && suiteStr[0].isLetter()) {
                    containerNommageLettre.visibility = View.VISIBLE
                    containerNommageLibre.visibility = View.GONE
                    radioGroup.check(R.id.radio_nommage_lettre)
                    selectionnerBoutonParTexte(toggleLettre, suiteStr)

                } else { // sinon c'est un mot
                    containerNommageLettre.visibility = View.GONE
                    containerNommageLibre.visibility = View.VISIBLE
                    radioGroup.check(R.id.radio_nommage_libre)
                    inputLibre.setText(suiteStr)

                }
            }
        }

        // Cas standart : on créer une nouvelle classe
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

        buttonBack.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        // logique du bouton de validation (vérification, validation et ecriture en base)
        buttonValider.setOnClickListener {
            var nomFinal = ""
            val idNiveau = toggleNiveau.checkedButtonId

            // Aucun niveau a été sélectionné
            if (idNiveau == -1) {
                Toast.makeText(requireContext(), "Veuillez sélectionner un niveau", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val niveau = view.findViewById<Button>(idNiveau).text.toString()
            // On vérifie quel mode est choisi
            if (radioGroup.checkedRadioButtonId == R.id.radio_nommage_lettre) {

                val idLettre = toggleLettre.checkedButtonId
                if (idLettre == -1) { // aucune lettre est sélectionnée
                    Toast.makeText(requireContext(), "Veuillez sélectionner une lettre", Toast.LENGTH_LONG).show()
                    return@setOnClickListener
                }

                val lettre = view.findViewById<Button>(idLettre).text.toString()
                nomFinal = "${niveau[0]}e $lettre" // Ex: "6A"

            } else { // sinon c'est en nommage libre
                nomFinal = inputLibre.text.toString().trim()
                nomFinal = "${niveau[0]}e " + inputLibre.text.toString().trim()

                if (nomFinal.isEmpty()) {
                    Toast.makeText(requireContext(), "Veuillez entrer un nom de classe", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
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
                Toast.makeText(requireContext(), "Classe $nom ajoutée", Toast.LENGTH_SHORT).show() // toast de retour utilisateur
                (activity as MainActivity).onBackPressed() // retour a l'écran précédent
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
                        requireActivity().onBackPressed()
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
                    requireActivity().onBackPressed()
                }

            } else {
                // Cas 2 : insertion
                val existe = db.classeDao().getClasseByName(nouveauNom) != null // retourne un boolean pour vérifier l'existence
                if (existe) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "La classe $nouveauNom existe déjà", Toast.LENGTH_SHORT).show() // feedback utilisateur
                    }
                } else { // existe pas --> pas de doublon en vu
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
}
package fr.iutvannes.dual.controller.fragments

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import fr.iutvannes.dual.R
import fr.iutvannes.dual.controller.MainActivity
import fr.iutvannes.dual.model.persistence.Classe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ClassesFragment : Fragment(R.layout.fragment_classes) {

    // on déclare l'adapter en varaible de classe pour pouvoir l'utiliser partout
    private lateinit var adapter: ClasseAdapter

    private val db = DatabaseProvider.db

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // recupération des vues
        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerViewClasses)
        val btnAjout = view.findViewById<Button>(R.id.ajout_classe)
        val tvEmpty = view.findViewById<TextView>(R.id.tvEmpty)

        // configuration de l'adapter
        adapter = ClasseAdapter(
            items = emptyList(), // Liste vide au démarrage
            onClick = { classe ->
                // Ouverture de la vue avec la liste des élèves
                val fragment = ElevesFragment.newInstance(classe.nom)
                (activity as MainActivity).showFragment(fragment, true, true)
            },
            onEdit = { classe ->
                // Ouvre le fragment d'ajout en mode édition (bouton bleu)
                val fragment = AjoutClasseFragment.newInstanceForEdit(classe.nom)
                (activity as MainActivity).showFragment(fragment, true, true)
            },
            onDelete = { classe ->
                // confirmation avant suppression
                afficherConfirmationSuppression(classe)
            }
        )

        // Branche l'adapter au recyclerview qui va permettre un affichage optimisé des classes
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(context)

        // Bouton d'ajout
        btnAjout.setOnClickListener {
            val fragment = AjoutClasseFragment.newInstance()
            (activity as MainActivity).showFragment(fragment, true, true)
        }

        // chargement des classes
        chargerClasses()
    }

    private fun chargerClasses() {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {

            // on récupère la liste des classes
            val rawClasses = db.classeDao().getClasses()

            // transforme chaque Classe en ClasseUI + compteur d'élèves
            val uiList = rawClasses.map { classe ->
                val count = db.EleveDao().countElevesByClasse(classe.nom)
                ClasseUI(classe, count)
            }

            // MAJ de l'interface sur le thread principal
            withContext(Dispatchers.Main) {
                adapter.updateList(uiList)

                // affichage au cas où aucune classe
                val tvEmpty = view?.findViewById<TextView>(R.id.tvEmpty)
                if (uiList.isEmpty()) {
                    tvEmpty?.visibility = View.VISIBLE
                } else {
                    tvEmpty?.visibility = View.GONE
                }
            }
        }
    }

    private fun supprimerClasse(classe: Classe) {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {

            // suppression au préalable des élèves
            db.EleveDao().deleteElevesByClasse(classe.nom)

            // suppression de la classe
            db.classeDao().delete(classe)

            // rechargement de la liste
            chargerClasses()

            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Classe ${classe.nom} supprimée", Toast.LENGTH_SHORT).show() // feedback utilisateur
            }
        }
    }

    /**
     * Affiche une boite de dialogue demandant à l'utilisateur de confirmer la suppression
     */
    private fun afficherConfirmationSuppression(classe: Classe) {
        com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
            .setTitle("Supprimer la classe ?")
            .setMessage("Attention, vous êtes sur le point de supprimer la classe \"${classe.nom}\" ainsi que tous les élèves qui y sont associés.\n\nCette action est irréversible.")
            .setNegativeButton("Annuler") { dialog, _ ->
                dialog.dismiss()
            }
            .setPositiveButton("Supprimer") { _, _ ->
                supprimerClasse(classe)
            }
            .show()
    }
}
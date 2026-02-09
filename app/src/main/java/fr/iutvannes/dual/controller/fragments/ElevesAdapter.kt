package fr.iutvannes.dual.controller.fragments

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import fr.iutvannes.dual.model.persistence.Eleve
import fr.iutvannes.dual.R

/**
 * Adapter pour la liste des élèves.
 *
 * @see Eleve
 * @see R.layout.item_eleve
 *
 * @param onEdit Fonction appelée lorsque l'utilisateur clique sur le bouton "Modifier"
 * @param onDelete Fonction appelée lorsque l'utilisateur clique sur le bouton "Supprimer"
 * @return Adapter pour la liste des élèves
 */
class ElevesAdapter(
    private val onEdit: (Eleve) -> Unit,
    private val onDelete: (Eleve) -> Unit
) : RecyclerView.Adapter<ElevesAdapter.VH>() {

    /* Variable contenant la liste des élèves à afficher */
    private var items: List<Eleve> = emptyList()

    /**
     * Envoie la liste des élèves à afficher à l'adapter.
     */
    fun submitList(newItems: List<Eleve>) {
        items = newItems
        notifyDataSetChanged()
    }

    /**
     * Classe interne qui représente un élément de la liste.
     *
     * @param view Vue du layout item_eleve.xml
     * @return ViewHolder contenant la vue de l'élément
     */
    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val tvNomPrenom: TextView = view.findViewById(R.id.tvNomPrenom)
        val tvGenre: TextView = view.findViewById(R.id.tvGenre)
        val btnEdit: View = view.findViewById(R.id.btnEdit)
        val btnDelete: View = view.findViewById(R.id.btnDelete)
    }

    /**
     * Création de la vue d'un élément de la liste.
     *
     * @param parent Vue parent
     * @param viewType Type de vue
     * @return ViewHolder contenant la vue de l'élément
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_eleve, parent, false)
        return VH(view)
    }

    /**
     * Modification de la vue d'un élément de la liste.
     *
     * @param holder ViewHolder contenant la vue de l'élément
     * @param position Position de l'élément dans la liste
     * @return ViewHolder contenant la vue de l'élément
     */
    override fun onBindViewHolder(holder: VH, position: Int) {
        val eleve = items[position]

        // texte du textView dans item_eleve.xml
        holder.tvNomPrenom.text = "${eleve.nom} ${eleve.prenom}"
        holder.tvGenre.text = eleve.genre ?: "—"

        holder.btnEdit.setOnClickListener { onEdit(eleve) }
        holder.btnDelete.setOnClickListener { onDelete(eleve) }
    }

    /**
     * Retourne la taille de la liste.
     *
     * @return Taille de la liste
     */
    override fun getItemCount(): Int = items.size
}

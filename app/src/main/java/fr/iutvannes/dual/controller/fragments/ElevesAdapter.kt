package fr.iutvannes.dual.controller.fragments

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import fr.iutvannes.dual.model.persistence.Eleve
import fr.iutvannes.dual.R
import fr.iutvannes.dual.model.persistence.Classe


class ElevesAdapter(
    private val onClick: (Eleve) -> Unit,
    private val onEdit: (Eleve) -> Unit,
    private val onDelete: (Eleve) -> Unit
) : RecyclerView.Adapter<ElevesAdapter.VH>() {

    private var items: List<Eleve> = emptyList()

    fun submitList(newItems: List<Eleve>) {
        items = newItems
        notifyDataSetChanged()
    }

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val tvNomPrenom: TextView = view.findViewById(R.id.tvNomPrenom)
        val tvGenre: TextView = view.findViewById(R.id.tvGenre)
        val btnEdit: View = view.findViewById(R.id.btnEdit)
        val btnDelete: View = view.findViewById(R.id.btnDelete)
        val card: View = view
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_eleve, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val eleve = items[position]

        // texte du textView dans item_eleve.xml
        holder.tvNomPrenom.text = "${eleve.nom} ${eleve.prenom}"
        holder.tvGenre.text = eleve.genre ?: "—"

        holder.card.setOnClickListener { onClick(eleve) }
        holder.btnEdit.setOnClickListener { onEdit(eleve) }
        holder.btnDelete.setOnClickListener { onDelete(eleve) }
    }

    override fun getItemCount(): Int = items.size
}

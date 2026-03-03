package fr.iutvannes.dual.controller.fragments

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import fr.iutvannes.dual.R
import fr.iutvannes.dual.model.persistence.Classe

data class ClasseUI(val classe: Classe, val nombreEleves: Int)

class ClasseAdapter(
    private var items: List<ClasseUI>,
    private val onClick: (Classe) -> Unit,
    private val onEdit: (Classe) -> Unit,
    private val onDelete: (Classe) -> Unit
) : RecyclerView.Adapter<ClasseAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvNom: TextView = view.findViewById(R.id.tvNomClasse)
        val tvCount: TextView = view.findViewById(R.id.tvCount)
        val btnEdit: View = view.findViewById(R.id.btnEdit)
        val btnDelete: View = view.findViewById(R.id.btnDelete)
        val card: View = view
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_classe, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]

        holder.tvNom.text = item.classe.nom
        holder.tvCount.text = "${item.nombreEleves} élèves"

        holder.card.setOnClickListener { onClick(item.classe) }
        holder.btnEdit.setOnClickListener { onEdit(item.classe) }
        holder.btnDelete.setOnClickListener { onDelete(item.classe) }
    }

    override fun getItemCount() = items.size

    fun updateList(newItems: List<ClasseUI>) {
        items = newItems
        notifyDataSetChanged()
    }
}
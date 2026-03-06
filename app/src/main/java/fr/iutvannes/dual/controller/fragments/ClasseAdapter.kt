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

        holder.tvNom.text = item.classe.nom.formatAffichageClasse() // nom formaté pour un affichage + propre
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
package fr.iutvannes.dual.controller.fragments

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import fr.iutvannes.dual.model.persistence.Eleve
import fr.iutvannes.dual.R

/**
 * Adapter for the student list.
 *
 * @see Eleve
 * @see R.layout.item_eleve
 *
 * @param onEdit Function called when the user clicks the "Edit" button
 * @param onDelete Function called when the user clicks the "Delete" button
 * @return Adapter for the student list
 */
class ElevesAdapter(
    private val onEdit: (Eleve) -> Unit,
    private val onDelete: (Eleve) -> Unit
) : RecyclerView.Adapter<ElevesAdapter.VH>() {

    /* Variable containing the list of students to display */
    private var items: List<Eleve> = emptyList()

    /**
     * Send the list of students to be displayed for adaptation.
     */
    fun submitList(newItems: List<Eleve>) {
        items = newItems
        notifyDataSetChanged()
    }

    /**
     * An internal class that represents an element of the list.
     *
     * @param view View of the item_eleve.xml layout
     * @return ViewHolder containing the view of the element
     */
    inner class VH(view: View) : RecyclerView.ViewHolder(view) {

        /* Variable for the text view displaying the student name */
        val tvNomPrenom: TextView = view.findViewById(R.id.tvNomPrenom)

        /* Variable for the text view displaying the student gender */
        val tvGenre: TextView = view.findViewById(R.id.tvGenre)

        /* Variable for the edit button */
        val btnEdit: View = view.findViewById(R.id.btnEdit)

        /* Variable for the delete button */
        val btnDelete: View = view.findViewById(R.id.btnDelete)
    }

    /**
     * Creating a view of a list item.
     *
     * @param parent Parent view
     * @param viewType View type
     * @return ViewHolder containing the view of the element
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_eleve, parent, false)
        return VH(view)
    }

    /**
     * Modifying the view of a list item.
     *
     * @param holder ViewHolder containing the item's view
     * @param position Item's position in the list
     * @return ViewHolder containing the item's view
     */
    override fun onBindViewHolder(holder: VH, position: Int) {
        val eleve = items[position]

        // Text of the textView in item_eleve.xml
        holder.tvNomPrenom.text = "${eleve.nom} ${eleve.prenom}"
        holder.tvGenre.text = eleve.genre ?: "—"

        holder.btnEdit.setOnClickListener { onEdit(eleve) }
        holder.btnDelete.setOnClickListener { onDelete(eleve) }
    }

    /**
     * Returns the size of the list.
     *
     * @return List Size
     */
    override fun getItemCount(): Int = items.size
}

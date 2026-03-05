package fr.iutvannes.dual.controller.fragments

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import fr.iutvannes.dual.R
import fr.iutvannes.dual.model.persistence.Classe

/**
 * Data class to represent a class with its associated number of students.
 * This class is used to display the list of classes in the UI.
 *
 * @param classe The class to display
 * @param nombreEleves The number of students in the class
 */
data class ClasseUI(val classe: Classe, val nombreEleves: Int)

/**
 * Adapter for the list of classes.
 * This adapter is used to display the list of classes in the UI.
 *
 * @param items The list of classes to display
 * @param onClick The action to perform when a class is clicked
 * @param onEdit The action to perform when the edit button is clicked
 * @param onDelete The action to perform when the delete button is clicked
 * @return A ViewHolder for the list of classes
 */
class ClasseAdapter(
    private var items: List<ClasseUI>,
    private val onClick: (Classe) -> Unit,
    private val onEdit: (Classe) -> Unit,
    private val onDelete: (Classe) -> Unit
) : RecyclerView.Adapter<ClasseAdapter.ViewHolder>() { // Tu as bien précisé ton ViewHolder ici, c'est bien.

    /**
     * ViewHolder for the list of classes.
     * This ViewHolder is used to display the list of classes in the UI.
     *
     * @param view The view to display
     * @return A ViewHolder for the list of classes
     */
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        /* Variable for the text view displaying the class name */
        val tvNom: TextView = view.findViewById(R.id.tvNomClasse)

        /* Variable for the text view displaying the number of students */
        val tvCount: TextView = view.findViewById(R.id.tvCount)

        /* Variable for the edit button */
        val btnEdit: View = view.findViewById(R.id.btnEdit)

        /* Variable for the delete button */
        val btnDelete: View = view.findViewById(R.id.btnDelete)

        /* Variable for the card */
        val card: View = view
    }

    // CORRECTION 1: The return type must be TON ViewHolder
    /**
     * Creates a new ViewHolder for the list of classes.
     *
     * @param parent The parent view
     * @param viewType The type of view
     * @return A ViewHolder for the list of classes
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_classe, parent, false)
        // CORRECTION 2: You must return an instance of YOUR ViewHolder, not the default one
        return ViewHolder(view)
    }

    // CORRECTION 3: The 'holder' argument must be of type 'ViewHolder' (yours), not 'RecyclerView.ViewHolder'
    /**
     * Binds the data to the ViewHolder.
     *
     * @param holder The ViewHolder to bind the data to
     * @param position The position of the item in the list
     */
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]

        // Now, 'holder' knows your variables tvNom, tvCount, etc.
        holder.tvNom.text = item.classe.nom
        holder.tvCount.text = "${item.nombreEleves} élèves"

        holder.card.setOnClickListener { onClick(item.classe) }
        holder.btnEdit.setOnClickListener { onEdit(item.classe) }
        holder.btnDelete.setOnClickListener { onDelete(item.classe) }
    }

    /**
     * Returns the number of items in the list.
     *
     * @return The number of items in the list
     */
    override fun getItemCount() = items.size

    /**
     * Updates the list of items.
     *
     * @param newItems The new list of items
     */
    fun updateList(newItems: List<ClasseUI>) {
        items = newItems
        notifyDataSetChanged()
    }
}
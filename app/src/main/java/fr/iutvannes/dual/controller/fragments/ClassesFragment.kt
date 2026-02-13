package fr.iutvannes.dual.controller.fragments

// Imports nécessaires
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import fr.iutvannes.dual.R
import fr.iutvannes.dual.model.database.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.widget.Button
import fr.iutvannes.dual.controller.MainActivity

/**
 * Fragment to display the list of classes.
 * As well as the button to add a new class.
 * The layout used is fragment_classes.xml.
 *
 * @see AppDatabase
 * @see R.layout.fragment_classes
 */
class ClassesFragment : Fragment(R.layout.fragment_classes) {

    /**
     * This method is called when the fragment is created.
     * This fragment is used to display the list of classes.
     * As well as the button to add a new class.
     *
     * @param view The fragment view.
     * @param savedInstanceState The fragment's saved data.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initializing views and listeners
        val container = view.findViewById<LinearLayout>(R.id.container_classes)

        // Opening a coroutine in the I/O thread to perform background tasks
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {

            //Database connection
            val db = Room.databaseBuilder(
                requireContext(),
                AppDatabase::class.java, "dual.db"
            )
                .fallbackToDestructiveMigration()
                .build()

            val classes = db.classeDao().getClasses()

            val btn_ajoutClasse = view.findViewById<Button>(R.id.ajout_classe)

            // Handling the click on the button to add a new class
            btn_ajoutClasse.setOnClickListener {
                val fragment = AjoutClasseFragment()
                (activity as MainActivity).showFragment(fragment, true, true)
            }

            // Returning to the main thread for modifying the graphical user interface (UI).
            withContext(Dispatchers.Main) {

                container.removeAllViews()

                if (classes.isEmpty()) {
                    val emptyText = TextView(requireContext()).apply {
                        text = "Aucune classe enregistrée"
                        textSize = 18f
                        setPadding(8, 8, 8, 8)
                    }
                    container.addView(emptyText)
                } else {
                    for (classe in classes) {
                        val button = Button(requireContext()).apply {
                            text = classe
                            textSize = 18f
                            setPadding(8, 8, 8, 8)
                        }

                        // Managing clicks on the class to display the student list
                        button.setOnClickListener {
                            val fragment = ElevesFragment.newInstance(classe)
                            (activity as MainActivity).showFragment(fragment, true, true)

                        }

                        container.addView(button)
                    }
                }
            }
        }
    }
}
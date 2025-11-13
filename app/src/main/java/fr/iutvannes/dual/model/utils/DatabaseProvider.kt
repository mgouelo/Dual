import android.content.Context
import androidx.room.Room
import fr.iutvannes.dual.model.database.AppDatabase

/**
 * Singleton permettant d'initialiser la base de données.
 * Cela permet d'éviter d'avoir à le faire dans chaque activité.
 */
object DatabaseProvider {
    lateinit var db: AppDatabase

    fun init(context: Context) {
        if (!::db.isInitialized) {
            db = Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "dual.db"
            ).build()
        }
    }
}

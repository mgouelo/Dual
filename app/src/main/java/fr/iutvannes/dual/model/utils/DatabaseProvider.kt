import android.content.Context
import androidx.room.Room
import fr.iutvannes.dual.model.database.AppDatabase

/**
 * A singleton is used to initialize the database.
 * This avoids having to do it in each activity.
 */
object DatabaseProvider {
    lateinit var db: AppDatabase

    /**
     * Initializes the database.
     *
     * @param context Application context.
     */
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

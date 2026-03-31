package fr.iutvannes.dual.controller.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.room.Room
import fr.iutvannes.dual.model.database.AppDatabase
import fr.iutvannes.dual.model.importation.ImportReport
import fr.iutvannes.dual.model.importation.ImportService
import fr.iutvannes.dual.model.importation.readers.CsvStudentReader
import fr.iutvannes.dual.model.importation.readers.XlsStudentReader
import java.io.InputStream

/**
 * ViewModel pour l'import de fichiers élèves.
 * Passe désormais le [ClasseDAO] à [ImportService] afin que les classes
 * présentes dans le fichier soient créées automatiquement si elles n'existent pas.
 */
class ImportViewModel(application: Application) : AndroidViewModel(application) {

    private val db = Room.databaseBuilder(
        application,
        AppDatabase::class.java,
        "dual.db"
    ).build()

    private val importService = ImportService(
        readers = listOf(
            CsvStudentReader(),
            XlsStudentReader(),
        ),
        eleveDao  = db.EleveDao(),
        classeDao = db.classeDao()
    )

    /**
     * Importe un fichier d'élèves.
     * Crée automatiquement les classes manquantes en base.
     *
     * @param input      flux du fichier (CSV / XLS)
     * @param fileName   nom du fichier (pour détecter l'extension)
     * @param mime       type MIME du fichier
     * @param classeNom  classe de repli si absente du fichier
     * @return rapport d'import
     */
    suspend fun importer(
        input: InputStream,
        fileName: String,
        mime: String?,
        classeNom: String?
    ): ImportReport {
        return importService.import(input, fileName, mime, classeNom)
    }
}
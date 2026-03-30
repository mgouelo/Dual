package fr.iutvannes.dual.controller.viewmodel

import androidx.lifecycle.AndroidViewModel
import android.app.Application
import androidx.room.Room
import fr.iutvannes.dual.model.database.AppDatabase
import fr.iutvannes.dual.model.importation.ImportReport
import fr.iutvannes.dual.model.importation.ImportService
import fr.iutvannes.dual.model.importation.readers.CsvStudentReader
import fr.iutvannes.dual.model.importation.readers.XlsStudentReader
import java.io.InputStream

/**
 * Class allowing the import of data from a file.
 *
 * @see ImportService
 * @see ImportReport
 * @see CsvStudentReader
 * @see XlsStudentReader
 * @see AppDatabase
 */
class ImportViewModel(application: Application) : AndroidViewModel(application) {

    /* Variable used to create or open the database */
    private val db = Room.databaseBuilder(
        application,
        AppDatabase::class.java,
        "dual.db"
    ).build()

    /* Variable used to import data */
    private val importService = ImportService( // The importService object is provided with all the readers and the database so that the import can take place.
        readers = listOf(
            CsvStudentReader(),
            XlsStudentReader(),
        ),
        eleveDao = db.EleveDao()
    )

    /**
     * Method for importing data from a file.
     *
     * @param input [InputStream] containing the data to import
     * @param fileName [String] containing the file name
     * @param mime [String] containing the file type
     * @param classeNom [String] containing the class name
     * @return [ImportReport] containing the import report
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
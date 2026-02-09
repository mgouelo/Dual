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
 * Classe permettant d'importer des données depuis un fichier.
 *
 * @see ImportService
 * @see ImportReport
 * @see CsvStudentReader
 * @see XlsStudentReader
 * @see AppDatabase
 */
class ImportViewModel(application: Application) : AndroidViewModel(application) {

    /* Variable permettant de créer ou d'ouvrir la DB */
    private val db = Room.databaseBuilder(
        application,
        AppDatabase::class.java,
        "dual.db"
    ).build()

    /* Variable permettant d'importer les données */
    private val importService = ImportService( // on fournie à l'objet importService tous les readers et la DB pour que l'import puisse avoir lieu
        readers = listOf(
            CsvStudentReader(),
            XlsStudentReader(),
        ),
        eleveDao = db.EleveDao()
    )

    /**
     * Méthode permettant d'importer des données depuis un fichier.
     *
     * @param input [InputStream] contenant les données à importer
     * @param fileName [String] contenant le nom du fichier
     * @param mime [String] contenant le type de fichier
     * @param classeNom [String] contenant le nom de la classe
     * @return [ImportReport] contenant le rapport de l'import
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